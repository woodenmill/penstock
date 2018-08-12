package io.woodenmill.penstock.metrics.prometheus

import io.woodenmill.penstock.Metrics
import io.woodenmill.penstock.Metrics.Counter
import io.woodenmill.penstock.metrics.prometheus.Prometheus.{PromQl, PrometheusConfig}
import io.woodenmill.penstock.testutils.{PromResponses, PrometheusIntegratedSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


class PrometheusClientSpec extends FlatSpec with ScalaFutures with PrometheusIntegratedSpec with Matchers {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 2.seconds)

  "Prometheus client" should "fetch metric value from Prometheus" in {
    //given
    val query = "up"
    configurePromStub(query, PromResponses.valid("5"))

    //when
    val metricFuture = PrometheusClient(PrometheusConfig(prometheusUri)).fetch(PromQl[Counter](query, Metrics.counterFactory))

    //then
    whenReady(metricFuture) { metric =>
      metric.value shouldBe 5
    }
  }

  it should "return failed future when Prometheus response is not OK" in {
    //given
    configurePromStub("up", "Not found", 404)

    //when
    val metricFuture = PrometheusClient(PrometheusConfig(prometheusUri)).fetch(PromQl[Counter]("up", Metrics.counterFactory))

    //then
    whenReady(metricFuture.failed) { ex =>
      ex should have message "Querying Prometheus has failed. query=up. Response: status=404, body=Not found"
    }
  }

}

