package io.woodenmill.penstock.dsl

import cats.effect.IO
import io.woodenmill.penstock.Metrics.Counter
import io.woodenmill.penstock.dsl.Penstock.FailedAssertion
import io.woodenmill.penstock.testutils.{Spec, TestBackends}
import io.woodenmill.penstock.testutils.TestBackends.mockedBackend

import scala.concurrent.duration._

class DslSpec extends Spec {
  val always10 = IO(Counter(10, "always ten"))
  val aPenstock = Penstock.load(TestBackends.doNothing[String](), () => "msg", duration = 1.second, throughput = 1)

  "Penstock DSL" should "allow to send messages with given throughput" in {
    val throughput: Int = 1500
    val backend = mockedBackend[String]()

    Penstock.load(backend, () => List("msg"), duration = 2.seconds, throughput).run()

    backend.messages.size shouldBe 3000 +- 500
  }

  it should "accept a message generator that generates one message at a time" in {
    val singleMessageGen = () => "some message"

    "Penstock.load(TestBackends.doNothing[String](), singleMessageGen, 1.second, 100)" should compile
  }

  it should "allow to specify assertion" in {
    aPenstock
      .metricAssertion(always10)(_ shouldBe 10)
      .run()
  }

  it should "fail if assertion fails" in {
    assertThrows[FailedAssertion] {
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

    assertThrows[FailedAssertion] {
      scenario.run()
    }
  }

  it should "return information about all failed assertions" in {
    val scenario = aPenstock
      .metricAssertion(always10)(_ shouldBe -2)
      .metricAssertion(always10)(_ shouldBe -1)

    val error = the[FailedAssertion] thrownBy scenario.run()

    error.getMessage should include("10 was not equal to -1")
    error.getMessage should include("10 was not equal to -2")
  }

  it should "check assertions when load finished" in {
    val backend = mockedBackend[String]()
    val totalMessagesMetric = IO(Counter(backend.messages.size, "Total messages sent"))

    Penstock
      .load(backend, () => "msg", duration = 1.second, throughput = 10)
      .metricAssertion(totalMessagesMetric)(_ shouldBe 10)
      .run()
  }

  it should "provide a metric name when assertion fails" in {
    val qualityMetric = IO(Counter(1, "quality"))

    val error = the[FailedAssertion] thrownBy aPenstock.metricAssertion(qualityMetric)(_ shouldBe 100).run()

    error.getMessage should include("quality")
  }

  it should "print report with all metrics provided for assertions" in {
    val metric111 = IO(Counter(111, "always one one one"))
    val metric222 = IO(Counter(222, "always two two two"))

    val output = catchConsoleOutput {
      Penstock
        .load(TestBackends.doNothing[String](), () => "msg", duration = 100.millis, throughput = 1)
        .metricAssertion(metric111)(_ shouldBe 111)
        .metricAssertion(metric222)(_ shouldBe 222)
        .run()
    }

    output should include("always one one one")
    output should include("111")
    output should include("always two two two")
    output should include("222")
  }

  it should "print nothing if no assertion was provided" in {
    val output = catchConsoleOutput {
      Penstock
        .load(TestBackends.doNothing[String](), () => "msg", duration = 100.millis, throughput = 1)
        .run()
    }

    output shouldBe empty
  }
}
