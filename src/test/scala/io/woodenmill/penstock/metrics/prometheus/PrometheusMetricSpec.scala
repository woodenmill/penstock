package io.woodenmill.penstock.metrics.prometheus

import io.woodenmill.penstock.Metrics.{Counter, Gauge}
import io.woodenmill.penstock.testutils.PromResponses.valid
import io.woodenmill.penstock.testutils.{PrometheusIntegratedSpec, Spec}
import PrometheusMetric._

class PrometheusMetricSpec extends Spec with PrometheusIntegratedSpec {

  implicit val promConfig: PrometheusConfig = PrometheusConfig(prometheusUri)

  "PrometheusMetric" should "fetch the value from Prometheus" in {
    configurePromStub("up", valid("7.1"))

    val counterMetric = PrometheusMetric[Counter]("up", PromQl("up")).unsafeRunSync()

    counterMetric shouldBe Counter(7, "up")
  }

  it should "support Gauge metric" in {
    configurePromStub("messagesRate", valid("198.2876758707929"))

    val gaugeMetric = PrometheusMetric[Gauge]("messagesRate", PromQl("messagesRate")).unsafeRunSync()

    gaugeMetric.value shouldBe 198.287 +- 0.1
    gaugeMetric.name shouldBe "messagesRate"
  }

  it should "fetch a metric periodically" in {
    configurePromStub("sum(up)", valid("1"), valid("2"), valid("3"))

    val counterMetric = PrometheusMetric[Counter]("sum", PromQl("sum(up)"))

    counterMetric.unsafeRunSync().value shouldBe 1
    counterMetric.unsafeRunSync().value shouldBe 2
    counterMetric.unsafeRunSync().value shouldBe 3
  }
}
