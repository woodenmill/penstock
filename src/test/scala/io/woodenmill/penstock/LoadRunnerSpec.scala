package io.woodenmill.penstock

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.woodenmill.penstock.testutils.Spec
import io.woodenmill.penstock.testutils.TestBackends._
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class LoadRunnerSpec extends Spec with BeforeAndAfterAll {
  val actorSystem: ActorSystem = ActorSystem("LoadRunnerSpec")
  val mat: ActorMaterializer = ActorMaterializer()(actorSystem)


  "LoadRunner" should "use streaming backend to send a message" in {
    val message = "some message"
    val backend: MockedBackend[String] = mockedBackend()

    val runnerFinished = LoadRunner(message, 1.milli, throughput = 100).run()(backend, mat)

    whenReady(runnerFinished) { _ =>
      backend.messages should contain(message)
    }
  }

  it should "accept many messages" in {
    val messages = Seq("msg1", "msg2", "msg3")
    val backend: MockedBackend[String] = mockedBackend()

    val runnerFinished = LoadRunner(messages, 1.milli, throughput = 100).run()(backend, mat)

    whenReady(runnerFinished) { _ =>
      backend.messages should contain allElementsOf messages
    }
  }

  it should "throttle the message sending with high rate" in {
    val throughput: Int = 1000
    val backend = mockedBackend[String]()

    val runnerFinished = LoadRunner("some message", 1.second, throughput).run()(backend, mat)

    whenReady(runnerFinished) { _ =>
      backend.messages.size shouldBe 1000 +- 200
    }
  }

  it should "throttle the message sending with very low rate" in {
    val throughput: Int = 1
    val backend = mockedBackend[String]()

    val runnerFinished = LoadRunner("some message", duration = 1.second, throughput).run()(backend, mat)

    whenReady(runnerFinished) { _ =>
      backend.messages.size shouldBe 1 +- 1
    }
  }

  override protected def afterAll(): Unit = Await.ready(actorSystem.terminate(), atMost = 5.seconds)
}
