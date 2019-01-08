package io.woodenmill.penstock.dsl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.IO
import io.woodenmill.penstock.Metrics.Counter
import io.woodenmill.penstock.dsl.Penstock.PenstockFailedAssertion
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

  it should "accept a message generator that generates one message at a time" in {
    val singleMessageGen = () => "some message"

    "Penstock.load(backend, singleMessageGen, 1.second, 100)" should compile
  }

  it should "allow to specify assertion that should be verified when load finished" in {
    aPenstock
      .metricAssertion(always10)(_ shouldBe 10)
      .run()
  }

  it should "fail if assertion fails" in {
    assertThrows[PenstockFailedAssertion] {
      aPenstock
        .metricAssertion(always10)(_ shouldBe -1)
        .run()
    }
  }

  it should "allow to specify many assertions" in {
    aPenstock
      .metricAssertion(always10)(_ shouldBe 10)
      .metricAssertion(always10)(_ shouldBe 10)
      .run()
  }

  it should "fail if one of the assertions fails" in {
    val scenario = aPenstock
      .metricAssertion(always10)(_ shouldBe 10)
      .metricAssertion(always10)(_ shouldBe -1)

    assertThrows[PenstockFailedAssertion] {
      scenario.run()
    }
  }

  it should "return information about all failed assertions" in {
    val scenario = aPenstock
      .metricAssertion(always10){_ shouldBe -2}
      .metricAssertion(always10)(_ shouldBe -1)

    the [PenstockFailedAssertion] thrownBy {
      scenario.run()
    } should have message "10 was not equal to -1, 10 was not equal to -2"

  }

  it should "print report with all metrics provided for assertions" in {
    pending
  }

  it should "not expose any objects/classes from third party libraries (akka, cats etc.)" in {
    pending
  }

  it should "clean up after scenario" in {
    pending
  }

  it should "stop printing report when LoadGenerator failed" in {
    pending
  }

  val aPenstock = Penstock.load(backend, () => "msg", duration = 1.second, throughput = 1)

  val always10 = IO(Counter(10, "always ten"))
}
