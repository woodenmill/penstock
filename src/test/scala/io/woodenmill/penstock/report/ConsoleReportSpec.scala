package io.woodenmill.penstock.report

import akka.actor.ActorSystem
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
    val mockedPrinter = MockedPrinter()

    ConsoleReport(metric).runEvery(10.milliseconds)(system, mockedPrinter)

    eventually {
      mockedPrinter.printed() should include("3")
      mockedPrinter.printed() should include("the name")
    }
  }

  it should "allow to pass many metrics" in {
    val first = IO(Counter(44, "counter", 0L))
    val second = IO(Gauge(3.0, "gauge", 1L))
    val mockedPrinter = MockedPrinter()

    ConsoleReport(first, second).runEvery(10.milliseconds)(system, mockedPrinter)

    eventually {
      mockedPrinter.printed() should include("counter")
      mockedPrinter.printed() should include("gauge")
    }
  }

  it should "fetch metrics value periodically" in {
    val mockedPrinter = MockedPrinter()
    val metricIO = StubIO[Counter]()
      .and(Counter(12, "counter", 0L))
      .and(Counter(13, "counter", 1000L))
      .toIO()

    ConsoleReport(metricIO).runEvery(10.milliseconds)(system, mockedPrinter)

    eventually {
      mockedPrinter.printed() should include("12")
      mockedPrinter.printed() should include("13")
    }
  }

  it should "survive metrics fetching failure" in {
    val mockedPrinter = MockedPrinter()
    val metricIO = StubIO[Counter]()
      .and(() => throw new RuntimeException("error"))
      .and(Counter(7, "now it works", 1000L))
      .toIO()

    ConsoleReport(metricIO).runEvery(10.milliseconds)(system, mockedPrinter)

    eventually {
      mockedPrinter.printed() should include("error")
      mockedPrinter.printed() should include("now it works")
    }
  }
}


case class StubIO[T](input: List[() => T] = List()) {
  private var results: List[() => T] = input

  def and(m: T): StubIO[T] = this.and(() => m)

  def and(m: () => T): StubIO[T] = StubIO(results ::: List(m))

  def toIO(): IO[T] = IO {
    results match {
      case head :: Nil =>
        head()
      case head :: tail =>
        this.results = tail
        head()
      case Nil => ???
    }
  }
}

case class MockedPrinter() extends Printer {
  private val builder = new StringBuilder

  override def printLine(line: String): Unit = builder.append(line).append("\n")

  def printed(): String = builder.toString()
}
