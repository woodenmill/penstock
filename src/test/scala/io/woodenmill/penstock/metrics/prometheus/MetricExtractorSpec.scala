package io.woodenmill.penstock.metrics.prometheus

import io.woodenmill.penstock.Metrics.{Counter, _}
import io.woodenmill.penstock.testutils.{PromResponses, Spec}

import scala.util.Success

class MetricExtractorSpec extends Spec {

  "MetricExtractor" should "extract a single metric from Prometheus Api response" in {
    val counter = MetricExtractor.extract[Counter](PromResponses.valid("44"))

    counter shouldBe Success(Counter(44L))
  }

  it should "extract negative values" in {
    val extracted = MetricExtractor.extract[Counter](PromResponses.responseWithNegativeValue)

    extracted shouldBe Success(Counter(-1L))
  }

  it should "return Failure if response is invalid" in {
    val invalidResponse = "Not found"

    val extracted = MetricExtractor.extract[Counter](invalidResponse)

    extracted.failure.exception should have message "Not a valid JSON"
  }

  it should "return Failure if response contains more than 1 metric" in {
    val extracted = MetricExtractor.extract[Counter](PromResponses.multipleMetricsResponse())

    extracted.failure.exception should have message "Prom Query returned more than one metric. Correct your query so it fetch only one metric"
  }

  it should "return Failure if queried metrics does not exist" in {
    val extracted = MetricExtractor.extract[Counter](PromResponses.noDataPoint)

    extracted.failure.exception should have message "Metric does not exist"
  }

  it should "return Failure if metric value is not a String" in {
    val extracted = MetricExtractor.extract[Counter](PromResponses.invalidValue)

    extracted.failure.exception should have message "Invalid response from Prometheus. Unparsable metric value: JInt(3)"
  }

  it should "return Failure if response is invalid and not contain 'result' array section" in {
    val extracted = MetricExtractor.extract[Counter](PromResponses.resultIsNotAnArray)

    extracted.failure.exception should have message "Invalid response from Prometheus. 'result' field must be an array"
  }
}
