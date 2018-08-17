package io.woodenmill.penstock.metrics.prometheus

import io.woodenmill.penstock.metrics.prometheus.PrometheusClient.{PromData, PromResponse, PromResult}
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric.RawMetric

object ResponseValidator {

  sealed trait DomainValidation extends Exception {
    override def getMessage: String
  }

  case object PromDataMustHaveExactlyOneResult extends DomainValidation {
    override def getMessage: String = "Prometheus Response must have exactly one result. Correct PromQL query"
  }

  case object EmptyMetricName extends DomainValidation {
    override def getMessage: String = "Metric name cannot be empty"
  }

  def validateToRawMetric(promResponse: PromResponse, metricName: String): Either[Throwable, RawMetric] = {
    val a: Either[DomainValidation, RawMetric] = for {
      validMetricName <- validateMetricName(metricName)
      validatedPromData <- validatePromData(promResponse.data)
      promValue = validatedPromData.value
    } yield RawMetric(validMetricName, promValue.metricValue, promValue.timestamp)
    a
  }

  private def validateMetricName(name: String): Either[EmptyMetricName.type, String] = {
    Either.cond(
      name.nonEmpty,
      name,
      EmptyMetricName
    )
  }

  private def validatePromData(promData: PromData): Either[PromDataMustHaveExactlyOneResult.type, PromResult] =
    Either.cond(
      promData.result.size == 1,
      promData.result.head,
      PromDataMustHaveExactlyOneResult
    )
}
