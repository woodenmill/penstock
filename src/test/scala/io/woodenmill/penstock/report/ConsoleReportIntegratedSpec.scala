package io.woodenmill.penstock.report

import cats.data.NonEmptyList
import io.woodenmill.penstock.Metrics.Counter
import io.woodenmill.penstock.metrics.prometheus.PrometheusClient.PrometheusConfig
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric._
import io.woodenmill.penstock.metrics.prometheus.{PromQl, PrometheusMetric}
import io.woodenmill.penstock.testutils.{PromResponses, PrometheusIntegratedSpec, Spec}

class ConsoleReportIntegratedSpec extends Spec with PrometheusIntegratedSpec {

  implicit val promConfig: PrometheusConfig = PrometheusConfig(prometheusUri)

  "Console Report" should "print metrics values fetched from Prometheus" in {
    val metric = PrometheusMetric[Counter]("up", PromQl("up"))
    configurePromStub("up", PromResponses.valid("1478"))

    val report = AsciiReport(NonEmptyList.of(metric)).unsafeRunSync()

    report should include("up")
    report should include("1478")
  }

  it should "print error message when query is invalid and fetching metrics has filed" in {
    val metric = PrometheusMetric[Counter]("up", PromQl("up"))
    configurePromStub("up", PromResponses.noDataPoint)

    val report = AsciiReport(NonEmptyList.of(metric)).unsafeRunSync()

    report should include("Prometheus Response had no data. Correct your Prometheus query")
  }
}
