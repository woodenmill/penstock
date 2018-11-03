package io.woodenmill.penstock.report

import akka.actor.ActorSystem
import cats.effect.IO
import cats.implicits._
import io.woodenmill.penstock.Metric

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{FiniteDuration, _}


case class ConsoleReport(metricIos: IO[Metric[_]]*) {

  def runEvery(interval: FiniteDuration)(implicit system: ActorSystem, printer: Printer = ConsolePrinter()): Unit = {
    implicit val ec: ExecutionContext = system.dispatcher

    val report = metricIos
      .map(io => io.attempt)
      .toList.parSequence
      .map(throwablesAndMetrics => throwablesAndMetrics.separate)
      .map { case (throwables, metrics) =>
        printer.printLine(AsciiTableFormatter.format(metrics))
        throwables.foreach(t => printer.printLine(s"Error: ${t.getMessage}"))
      }

    system.scheduler.schedule(0.seconds, interval) {
      report.unsafeRunSync()
    }
  }
}

trait Printer {
  def printLine(line: String): Unit
}

case class ConsolePrinter() extends Printer {
  override def printLine(line: String): Unit = println(line)
}