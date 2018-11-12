package io.woodenmill.penstock.report

import akka.actor.ActorSystem
import cats.effect.IO
import cats.implicits._
import io.woodenmill.penstock.Metric

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, _}

object ConsoleReport {

  type Report = String

  def buildReport(metricIOs: List[IO[Metric[_]]])(implicit ec: ExecutionContext): IO[Report] =
    metricIOs
      .map(io => io.attempt)
      .parSequence
      .map(throwablesAndMetrics => throwablesAndMetrics.separate)
      .map { case (throwables, metrics) =>
        val metricsReport = AsciiTableFormatter.format(metrics)
        val errorReport = throwables.map(t => s"Error: ${t.getMessage}").mkString("\n")
        s"""
           |$metricsReport
           |$errorReport
        """.stripMargin
      }
}

case class ConsoleReport(metricIos: IO[Metric[_]]*) {

  def runEvery(interval: FiniteDuration)(implicit system: ActorSystem, printer: Printer = ConsolePrinter()): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher
    system.scheduler.schedule(0.seconds, interval) {
      ConsoleReport.buildReport(metricIos.toList)
        .map(report => printer.printLine(report))
        .unsafeRunSync()
    }
  }
}

trait Printer {
  def printLine(line: String): Unit
}

case class ConsolePrinter() extends Printer {
  override def printLine(line: String): Unit = println(line)
}