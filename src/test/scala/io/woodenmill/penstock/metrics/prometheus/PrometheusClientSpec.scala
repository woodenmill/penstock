package io.woodenmill.penstock.metrics.prometheus

import io.woodenmill.penstock.Metrics
import io.woodenmill.penstock.Metrics.Counter
import io.woodenmill.penstock.metrics.prometheus.Prometheus.{PromQl, PrometheusConfig}
import io.woodenmill.penstock.testutils.{PromResponses, PrometheusIntegratedSpec, Spec}

import scala.concurrent.ExecutionContext.Implicits.global


class PrometheusClientSpec extends Spec with PrometheusIntegratedSpec {

  "Prometheus client" should "fetch metric value from Prometheus" in {
    val query = "up"
    configurePromStub(query, PromResponses.valid("5"))

    val metricFuture = PrometheusClient(PrometheusConfig(prometheusUri)).fetch(PromQl[Counter](query, Metrics.counterFactory))

    whenReady(metricFuture) { metric =>
      metric.value shouldBe 5
    }
  }

  it should "return failed future when Prometheus response is not OK" in {
    configurePromStub("up", "Not found", 404)

    val metricFuture = PrometheusClient(PrometheusConfig(prometheusUri)).fetch(PromQl[Counter]("up", Metrics.counterFactory))

    whenReady(metricFuture.failed) { ex =>
      ex should have message "Querying Prometheus has failed. query=up. Response: status=404, body=Not found"
    }
  }

}

