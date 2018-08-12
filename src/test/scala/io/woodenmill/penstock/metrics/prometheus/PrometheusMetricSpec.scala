package io.woodenmill.penstock.metrics.prometheus

import akka.actor.ActorSystem
import io.woodenmill.penstock.Metrics
import io.woodenmill.penstock.Metrics.{Counter, Gauge}
import io.woodenmill.penstock.metrics.prometheus.Prometheus.{PromQl, PrometheusConfig}
import io.woodenmill.penstock.testutils.PromResponses.valid
import io.woodenmill.penstock.testutils.{PrometheusIntegratedSpec, Spec}

class PrometheusMetricSpec extends Spec with PrometheusIntegratedSpec {

  val actorSystem = ActorSystem()
  val promConfig = PrometheusConfig(prometheusUri)

  "PrometheusMetric" should "fetch the value from Prometheus" in {
    configurePromStub("up", valid("7"))

    val prometheusMetric = PrometheusMetric[Counter](PromQl[Counter]("up", Metrics.counterFactory))(promConfig, actorSystem)

    prometheusMetric.value shouldBe Counter(7)
  }

  it should "support Gauge metric" in {
    configurePromStub("messagesRate", valid("198.2876758707929"))

    val prometheusMetric = PrometheusMetric[Gauge](PromQl("messagesRate", Metrics.gaugeFactory))(promConfig, actorSystem)

    prometheusMetric.value().value shouldBe 198.287 +- 0.1
  }

  it should "fetch a metric periodically" in {
    configurePromStub("sum(up)", valid("1"), valid("2"), valid("3"))

    val prometheusMetric = PrometheusMetric[Counter](PromQl("sum(up)", Metrics.counterFactory))(promConfig, actorSystem)

    eventually {
      prometheusMetric.value().value shouldBe 3
    }
  }
}
