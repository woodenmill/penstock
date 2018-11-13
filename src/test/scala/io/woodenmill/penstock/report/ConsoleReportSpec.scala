package io.woodenmill.penstock.report

import akka.actor.ActorSystem
import cats.data.NonEmptyList
import cats.effect.IO
import io.woodenmill.penstock.Metrics.{Counter, Gauge}
import io.woodenmill.penstock.testutils.Spec

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ConsoleReportSpec extends Spec {

  val system = ActorSystem()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "Report" should "print metrics names values" in {
    val metric = IO(Counter(3, "the name", 1L))

    val report = ConsoleReport.buildReport(NonEmptyList.of(metric)).unsafeRunSync()

    eventually {
      report should include("3")
      report should include("the name")
    }
  }

  it should "allow to pass many metrics" in {
    val first = IO(Counter(44, "counter", 0L))
    val second = IO(Gauge(3.0, "gauge", 1L))

    val report = ConsoleReport.buildReport(NonEmptyList.of(first, second)).unsafeRunSync()

    eventually {
      report should include("counter")
      report should include("gauge")
    }
  }

  it should "fetch metrics value periodically" in {
    val mockedPrinter = MockedPrinter()
    val metricIO = StubIO[Counter](List(
      () => Counter(12, "counter", 0L),
      () => Counter(13, "counter", 1000L)))
      .toIO()

    ConsoleReport(metricIO).runEvery(10.milliseconds)(system, mockedPrinter)

    eventually {
      mockedPrinter.printed() should include("12")
      mockedPrinter.printed() should include("13")
    }
  }

  it should "survive metrics fetching failure" in {
    val mockedPrinter = MockedPrinter()
    val metricIO = StubIO[Counter](List(
      () => throw new RuntimeException("error"),
      () => Counter(7, "now it works", 1000L)))
    .toIO()

    ConsoleReport(metricIO).runEvery(10.milliseconds)(system, mockedPrinter)

    eventually {
      mockedPrinter.printed() should include("error")
      mockedPrinter.printed() should include("now it works")
    }
  }

  it should "not compile if user does not provide at least one metric" in {
    assertDoesNotCompile("ConsoleReport()")
  }
}


case class StubIO[T](responses: List[() => T]) {
  private val stubs = Iterator.continually(responses).flatten

  def toIO(): IO[T] = IO {
    stubs.next().apply()
  }

}

case class MockedPrinter() extends Printer {
  private val builder = new StringBuilder

  override def printLine(line: String): Unit = builder.append(line).append("\n")

  def printed(): String = builder.toString()
}
