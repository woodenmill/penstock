package io.woodenmill.penstock.metrics.prometheus

import cats.effect.IO
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric.RawMetric
import io.woodenmill.penstock.testutils.{PromResponses, PrometheusIntegratedSpec, Spec}


class PrometheusClientSpec extends Spec with PrometheusIntegratedSpec {

  val promClient = PrometheusClient(PrometheusConfig(prometheusUri))

  "Prometheus client" should "fetch metric value from Prometheus" in {
    val query = "up"
    configurePromStub(query, PromResponses.valid("5"))

    val metricIO: IO[RawMetric] = promClient.fetch("up", PromQl(query))

    val m = metricIO.unsafeRunSync()
    m.metricName shouldBe "up"
    m.metricValue shouldBe 5.0
  }

  it should "return error when Prometheus response is not OK" in {
    configurePromStub("up", "Not found", 404)

    val metricIO = promClient.fetch("abc", PromQl("up"))

    whenReady(metricIO.unsafeToFuture().failed) { ex =>
      ex should have message "Not Found"
    }
  }

  it should "return an error when Prometheus response has more than one result" in {
    configurePromStub("test", PromResponses.multipleMetricsResponse())

    val metricIO = promClient.fetch("test", PromQl("test"))

    whenReady(metricIO.unsafeToFuture().failed) { ex =>
      ex should have message "Prometheus Response had more than one result. Correct Prometheus query"
    }
  }

  it should "return an error when Prometheus response structure is invalid" in {
    configurePromStub("invalid-response", PromResponses.resultIsNotAnArray)

    val metricIO = promClient.fetch("test", PromQl("invalid-response"))

    whenReady(metricIO.unsafeToFuture().failed) { ex =>
      ex.getMessage should include("Prometheus response is invalid.")
    }
  }

  it should "return an error when Prometheus response has no data" in {
    configurePromStub("no-data", PromResponses.noDataPoint)

    val metricIO = promClient.fetch("no-data", PromQl("no-data"))

    whenReady(metricIO.unsafeToFuture().failed) { ex =>
      ex should have message "Prometheus Response had no data. Correct your Prometheus query"
    }
  }
}

