package io.woodenmill.penstock.metrics.prometheus

import io.woodenmill.penstock.metrics.prometheus.PrometheusClient.{PromData, PromResponse, PromResult}
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric.RawMetric
import io.woodenmill.penstock.testutils.Spec
import org.scalatest.EitherValues

class ResponseValidatorSpec extends Spec with EitherValues {

  "MetricExtractor" should "extract a single metric from Prometheus Api response" in {
    val promResponse = PromResponse(PromData(Seq(PromResult((5678L, 2.3)))))

    val rawMetric = ResponseValidator.validateToRawMetric(promResponse, "name")

    rawMetric shouldBe Right(RawMetric("name", 2.3))
  }

  it should "extract negative values" in {
    val promResponse = PromResponse(PromData(Seq(PromResult((1234L, -1.0)))))

    val rawMetric = ResponseValidator.validateToRawMetric(promResponse, "test")

    rawMetric shouldBe Right(RawMetric("test", -1.0))
  }

  it should "return error if queried metrics does not exist" in {
    val promResponse = PromResponse(PromData(Seq.empty))

    val rawMetric = ResponseValidator.validateToRawMetric(promResponse, "some name")

    rawMetric.left.value should have message "Prometheus Response had no data. Correct your Prometheus query"
  }

  it should "return error if given metric name is empty" in {
    val metricName = ""
    val promResponse = PromResponse(PromData(Seq.empty))

    val rawMetric = ResponseValidator.validateToRawMetric(promResponse, metricName)

    rawMetric.left.value should have message "Metric name cannot be empty"
  }

}
