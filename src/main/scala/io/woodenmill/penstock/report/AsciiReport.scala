package io.woodenmill.penstock.report

import cats.data.NonEmptyList
import cats.effect.{ContextShift, IO}
import cats.implicits._
import io.woodenmill.penstock.Metric

object AsciiReport {

  type Report = String

  def apply(metricIOs: NonEmptyList[IO[Metric[_]]])(implicit cs: ContextShift[IO]): IO[Report] =
    metricIOs
      .map(io => io.attempt)
      .parSequence
      .map(throwablesAndMetrics => throwablesAndMetrics.toList.separate)
      .map { case (throwables, metrics) =>
        val metricsReport = AsciiTableFormatter.format(metrics)
        val errorReport = throwables.map(t => s"Error: ${t.getMessage}").mkString("\n")
        s"""
           |$metricsReport
           |$errorReport
        """.stripMargin
      }
}
