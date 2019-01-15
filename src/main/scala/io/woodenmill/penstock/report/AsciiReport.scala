package io.woodenmill.penstock.report

import cats.data.NonEmptyList
import cats.effect.{ContextShift, IO}
import io.woodenmill.penstock.Metric
import io.woodenmill.penstock.util.IOOps.parallelAttempt

object AsciiReport {

  type Report = String

  def apply(metricIOs: NonEmptyList[IO[Metric[_]]])(implicit cs: ContextShift[IO]): IO[Report] =
    parallelAttempt(metricIOs.toList).map { case (throwables, metrics) =>
        val metricsReport = AsciiTableFormatter.format(metrics)
        val errorReport = throwables.map(t => s"Error: ${t.getMessage}").mkString("\n")
        s"""
           |$metricsReport
           |$errorReport
        """.stripMargin
      }
}
