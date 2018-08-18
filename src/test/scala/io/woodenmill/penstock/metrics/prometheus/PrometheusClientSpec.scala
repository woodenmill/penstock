package io.woodenmill.penstock.metrics.prometheus

import cats.effect.IO
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric.RawMetric
import io.woodenmill.penstock.testutils.{PromResponses, PrometheusIntegratedSpec, Spec}


class PrometheusClientSpec extends Spec with PrometheusIntegratedSpec {

  val promClient = PrometheusClient(PrometheusConfig(prometheusUri))

  "Prometheus client" should "fetch metric value from Prometheus" in {
    val query = "up"
    configurePromStub(query, PromResponses.valid(1234L, "5"))

    val metricIO: IO[RawMetric] = promClient.fetch("up", PromQl(query))

    val m = metricIO.unsafeRunSync()
    m.metricName shouldBe "up"
    m.metricValue shouldBe 5.0
    m.metricTimestamp shouldBe 1234L
  }

  it should "return error when Prometheus response is not OK" in {
    configurePromStub("up", "Not found", 404)

    val metricIO = promClient.fetch("abc", PromQl("up"))

    whenReady(metricIO.unsafeToFuture().failed) { ex =>
      ex should have message "Querying Prometheus has failed. query=up. Response: status=404, body=Not found"
    }
  }

  it should "return error when Prometheus response has more than one result" in {
    configurePromStub("test", PromResponses.multipleMetricsResponse())

    val metricIO = promClient.fetch("test", PromQl("test"))

    whenReady(metricIO.unsafeToFuture().failed) { ex =>
      ex should have message "Prometheus Response must have exactly one result. Correct PromQL query"
    }
  }
}

