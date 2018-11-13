package io.woodenmill.penstock

import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.woodenmill.penstock.testutils.Spec
import io.woodenmill.penstock.testutils.TestBackends._
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class LoadRunnerSpec extends Spec with BeforeAndAfterAll {
  val actorSystem: ActorSystem = ActorSystem("LoadRunnerSpec")
  implicit val mat: ActorMaterializer = ActorMaterializer()(actorSystem)


  "LoadRunner" should "use streaming backend to send a message" in {
    val message = "some message"
    val backend: MockedBackend[String] = mockedBackend()

    val runnerFinished = LoadRunner(backend).start(() => message, 1.milli, throughput = 100)(mat)

    whenReady(runnerFinished) { _ =>
      backend.messages should contain(message)
    }
  }

  it should "accept many messages" in {
    val messages = List("msg1", "msg2", "msg3")
    val backend: MockedBackend[String] = mockedBackend()

    val runnerFinished = LoadRunner(backend).start(() => messages, 1.milli, throughput = 100)

    whenReady(runnerFinished) { _ =>
      backend.messages should contain allElementsOf messages
    }
  }

  it should "accept a function that generates messages to send" in {
    val messages = () => List(UUID.randomUUID())
    val backend: MockedBackend[UUID] = mockedBackend()

    LoadRunner(backend).start(messages, 3.milli, throughput = 500)

    eventually {
      backend.messages.distinct.size should be >= 2
    }
  }

  it should "throttle the message sending with high rate" in {
    val throughput: Int = 1000
    val backend = mockedBackend[String]()

    val runnerFinished = LoadRunner(backend).start(() => "some message", 1.second, throughput)(mat)

    whenReady(runnerFinished) { _ =>
      backend.messages.size shouldBe 1000 +- 200
    }
  }

  it should "throttle the message sending with very low rate" in {
    val throughput: Int = 1
    val backend = mockedBackend[String]()

    val runnerFinished = LoadRunner(backend).start(() => "some message", duration = 1.second, throughput)(mat)

    whenReady(runnerFinished) { _ =>
      backend.messages.size shouldBe 1 +- 1
    }
  }

  it should "fail fast if load target is not ready" in {
    val backend = mockedBackend[String](isReady = false)

    val runnerResult = LoadRunner(backend).start(() => "some msg", duration = 1.second, throughput = 1)(mat)

    runnerResult.failed.futureValue shouldBe an[IllegalStateException]
  }

  override protected def afterAll(): Unit = Await.ready(actorSystem.terminate(), atMost = 5.seconds)
}
