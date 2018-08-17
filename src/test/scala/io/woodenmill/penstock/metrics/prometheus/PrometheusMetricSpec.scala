package io.woodenmill.penstock.metrics.prometheus

import io.woodenmill.penstock.Metrics.{Counter, Gauge}
import io.woodenmill.penstock.testutils.PromResponses.valid
import io.woodenmill.penstock.testutils.{PrometheusIntegratedSpec, Spec}
import PrometheusMetric._

class PrometheusMetricSpec extends Spec with PrometheusIntegratedSpec {

  implicit val promConfig: PrometheusConfig = PrometheusConfig(prometheusUri)

  "PrometheusMetric" should "fetch the value from Prometheus" in {
    configurePromStub("up", valid(1234567L, "7.1"))

    val counterMetric = PrometheusMetric[Counter]("up", PromQl("up")).unsafeRunSync()

    counterMetric shouldBe Counter(7, "up", 1234567L)
  }

  it should "support Gauge metric" in {
    configurePromStub("messagesRate", valid(1L, "198.2876758707929"))

    val gaugeMetric = PrometheusMetric[Gauge]("messagesRate", PromQl("messagesRate")).unsafeRunSync()

    gaugeMetric.value shouldBe 198.287 +- 0.1
    gaugeMetric.name shouldBe "messagesRate"
    gaugeMetric.time shouldBe 1L
  }

  it should "fetch a metric periodically" in {
    configurePromStub("sum(up)", valid(1L, "1"), valid(2L, "2"), valid(3L, "3"))

    val counterMetric = PrometheusMetric[Counter]("sum", PromQl("sum(up)"))

    counterMetric.unsafeRunSync().value shouldBe 1
    counterMetric.unsafeRunSync().value shouldBe 2
    counterMetric.unsafeRunSync().value shouldBe 3
  }
}
