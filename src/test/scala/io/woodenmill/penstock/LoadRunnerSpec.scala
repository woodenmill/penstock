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

    LoadGenerator(backend).generate(() => message, 1.milli, throughput = 100)(mat).unsafeRunSync()

    backend.messages should contain(message)
  }

  it should "accept many messages" in {
    val messages = List("msg1", "msg2", "msg3")
    val backend: MockedBackend[String] = mockedBackend()

    LoadGenerator(backend).generate(() => messages, 1.milli, throughput = 100).unsafeRunSync()

    backend.messages should contain allElementsOf messages
  }

  it should "accept a function that generates messages to send" in {
    val messages = () => List(UUID.randomUUID())
    val backend: MockedBackend[UUID] = mockedBackend()

    LoadGenerator(backend).generate(messages, 3.milli, throughput = 500).unsafeRunSync()

    eventually {
      backend.messages.distinct.size should be >= 2
    }
  }

  it should "throttle the message sending with high rate" in {
    val throughput: Int = 1000
    val backend = mockedBackend[String]()

    LoadGenerator(backend).generate(() => "some message", 1.second, throughput)(mat).unsafeRunSync()

    backend.messages.size shouldBe 1000 +- 200
  }

  it should "throttle the message sending with very low rate" in {
    val throughput: Int = 1
    val backend = mockedBackend[String]()

    LoadGenerator(backend).generate(() => "some message", duration = 1.second, throughput)(mat).unsafeRunSync()

    backend.messages.size shouldBe 1 +- 1
  }

  it should "fail fast if load target is not ready" in {
    val backend = mockedBackend[String](isReady = false)

    assertThrows[IllegalStateException] {
      LoadGenerator(backend).generate(() => "some msg", duration = 1.second, throughput = 1)(mat).unsafeRunSync()
    }
  }

  override protected def afterAll(): Unit = Await.ready(actorSystem.terminate(), atMost = 5.seconds)
}
