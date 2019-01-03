package io.woodenmill.penstock.dsl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.woodenmill.penstock.testutils.Spec
import io.woodenmill.penstock.testutils.TestBackends.mockedBackend

import scala.concurrent.duration._

class DslSpec extends Spec {

  implicit val mat: ActorMaterializer = ActorMaterializer()(ActorSystem("DslSpec"))
  val backend = mockedBackend[String]()

  "Penstock DSL" should "allow to send messages with given throughput" in {
    val throughput: Int = 1500

    Penstock.load(backend, () => List("msg"), duration = 2.seconds, throughput).run()

    backend.messages.size shouldBe 3000 +- 500
  }

  it should "allow to provide a message generator that generates one message" in {
    val singleMessageGen = () => "some message"

    "Penstock.load(backend, singleMessageGen, 1.second, 100)" should compile
  }

  it should "allow to specify assertion that should be verified when load finished" in {
    pending
  }

  it should "allow to specify many assertions" in {
    pending
  }

  it should "print report with all metrics provided for assertions" in {
    pending
  }

  it should "not expose any objects/classes from third part libraries (akka, cats etc.)" in {
    pending
  }

  it should "clean up after scenario" in {
    pending
  }

  it should "stop printing report if LoadGenerator failed" in {
    pending
  }
}
