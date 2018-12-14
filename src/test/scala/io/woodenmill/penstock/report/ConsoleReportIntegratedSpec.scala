package io.woodenmill.penstock.report

import akka.actor.ActorSystem
import io.woodenmill.penstock.Metrics.Counter
import io.woodenmill.penstock.metrics.prometheus.{PromQl, PrometheusConfig, PrometheusMetric}
import io.woodenmill.penstock.testutils.{PromResponses, PrometheusIntegratedSpec, Spec}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class ConsoleReportIntegratedSpec extends Spec with PrometheusIntegratedSpec {

  implicit val promConfig= PrometheusConfig(prometheusUri)
  implicit val system = ActorSystem()
  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global

  "Console Report" should "print metrics values fetched from Prometheus" in {
    val metric = PrometheusMetric[Counter]("up", PromQl("up"))
    val mockedPrinter = MockedPrinter()
    configurePromStub("up", PromResponses.valid("1478"), 200)

    ConsoleReport(metric).runEvery(10.milli)(system, mockedPrinter)

    eventually {
      mockedPrinter.printed() should include("up")
      mockedPrinter.printed() should include("1478")
    }
  }

  it should "print error message when query is invalid and fetching metrics has filed" in {
    val metric = PrometheusMetric[Counter]("up", PromQl("up"))
    val mockedPrinter = MockedPrinter()
    configurePromStub("up", PromResponses.noDataPoint, 200)

    ConsoleReport(metric).runEvery(10.milli)(system, mockedPrinter)

    eventually {
      mockedPrinter.printed() should include("Error: Prometheus Response had no data. Correct your Prometheus query")
    }
  }

}
