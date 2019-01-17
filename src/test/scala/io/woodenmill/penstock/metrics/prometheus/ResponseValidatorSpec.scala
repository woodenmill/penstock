package io.woodenmill.penstock.metrics.prometheus

import io.woodenmill.penstock.metrics.prometheus.PrometheusClient.RawMetric
import io.woodenmill.penstock.metrics.prometheus.ResponseValidator.{PromData, PromResponse, PromResult}
import io.woodenmill.penstock.testutils.Spec
import org.scalatest.EitherValues
import upickle.default._


class ResponseValidatorSpec extends Spec with EitherValues {

  implicit val promResponseReader: Writer[PromResponse] = macroW
  implicit val promDataReader: Writer[PromData] = macroW
  implicit val promResultReader: Writer[PromResult] = macroW

  "MetricExtractor" should "extract a single metric from Prometheus Api response" in {
    val promResponse = write(PromResponse(PromData(Seq(PromResult((5678L, 2.3))))))

    val rawMetric = ResponseValidator.validate(promResponse, "name")

    rawMetric shouldBe Right(RawMetric("name", 2.3))
  }

  it should "extract negative values" in {
    val promResponse = write(PromResponse(PromData(Seq(PromResult((1234L, -1.0))))))

    val rawMetric = ResponseValidator.validate(promResponse, "test")

    rawMetric shouldBe Right(RawMetric("test", -1.0))
  }

  it should "return error if queried metrics does not exist" in {
    val promResponse = write(PromResponse(PromData(Seq.empty)))

    val rawMetric = ResponseValidator.validate(promResponse, "some name")

    rawMetric.left.value.getMessage should include("Prometheus Response had no data. Correct your Prometheus query")
  }

  it should "return error if given metric name is empty" in {
    val metricName = ""
    val promResponse = write(PromResponse(PromData(Seq.empty)))

    val rawMetric = ResponseValidator.validate(promResponse, metricName)

    rawMetric.left.value.getMessage should include("Metric name cannot be empty")
  }

}
