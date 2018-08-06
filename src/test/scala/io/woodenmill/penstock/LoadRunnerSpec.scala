package io.woodenmill.penstock

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.woodenmill.penstock.backends.TestBackends._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

class LoadRunnerSpec extends FlatSpec with Matchers with BeforeAndAfterAll with ScalaFutures {
  val actorSystem: ActorSystem = ActorSystem("LoadRunnerSpec")
  val mat: ActorMaterializer = ActorMaterializer()(actorSystem)
  implicit val testPatienceConfig: PatienceConfig = PatienceConfig(timeout = 2.seconds)


  "LoadRunner" should "use streaming backend to send a message" in {
    //given
    val message = "some message"
    val backend: MockedBackend[String] = mockedBackend()

    //when
    val runnerFinished = LoadRunner(message, 1.milli, throughput = 100).run()(backend, mat)

    //then
    whenReady(runnerFinished) { _ =>
      backend.messages should contain(message)
    }
  }

  it should "accept many messages" in {
    //given
    val messages = Seq("msg1", "msg2", "msg3")
    val backend: MockedBackend[String] = mockedBackend()

    //when
    val runnerFinished = LoadRunner(messages, 1.milli, throughput = 100).run()(backend, mat)

    //then
    whenReady(runnerFinished) { _ =>
      backend.messages should contain allElementsOf messages
    }
  }

  it should "throttle the message sending with high rate" in {
    //given
    val throughput: Int = 1000
    val backend = mockedBackend[String]()

    //when
    val runnerFinished = LoadRunner("some message", 1.second, throughput).run()(backend, mat)

    //then
    whenReady(runnerFinished) { _ =>
      backend.messages.size shouldBe 1000 +- 200
    }
  }

  it should "throttle the message sending with very low rate" in {
    //given
    val throughput: Int = 1
    val backend = mockedBackend[String]()

    //when
    val runnerFinished = LoadRunner("some message", duration = 1.second, throughput).run()(backend, mat)

    //then
    whenReady(runnerFinished) { _ =>
      backend.messages.size shouldBe 1 +- 1
    }
  }

  override protected def afterAll(): Unit = Await.ready(actorSystem.terminate(), atMost = 5.seconds)
}
