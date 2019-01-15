package io.woodenmill.penstock.report

import cats.data.NonEmptyList
import cats.effect.IO
import io.woodenmill.penstock.Metrics.{Counter, Gauge}
import io.woodenmill.penstock.testutils.Spec

class ConsoleReportSpec extends Spec {

  "Ascii Report" should "print metrics name and value" in {
    val metric = IO(Counter(3, "the name"))

    val report = AsciiReport(NonEmptyList.of(metric)).unsafeRunSync()

    report should include("3")
    report should include("the name")
  }

  it should "allow to pass many metrics" in {
    val first = IO(Counter(44, "counter"))
    val second = IO(Gauge(3.0, "gauge"))

    val report = AsciiReport(NonEmptyList.of(first, second)).unsafeRunSync()

    report should include("counter")
    report should include("gauge")
  }

  it should "not memoize metric values" in {
    val metricIO = stubbedIO(
      () => Counter(12, "counter"),
      () => Counter(13, "counter"))
    val reportIO = AsciiReport(NonEmptyList.of(metricIO))

    reportIO.unsafeRunSync() should include("12")
    reportIO.unsafeRunSync() should include("13")
  }

  it should "include error messages" in {
    val metricA = IO.raiseError(new RuntimeException("message A"))
    val metricB = IO.raiseError(new RuntimeException("message B"))

    val report = AsciiReport(NonEmptyList.of(metricA, metricB)).unsafeRunSync()

    report should include(
      """
        |Error: message A
        |Error: message B
      """.stripMargin)
  }

  def stubbedIO[T](first: () => T, next: () => T*): IO[T] = {
    val stubs = Iterator.continually(first :: next.toList).flatten
    IO(stubs.next().apply())
  }
}
