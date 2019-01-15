package io.woodenmill.penstock.metrics.prometheus

import io.woodenmill.penstock.metrics.prometheus.PrometheusClient.{PromData, PromResponse, PromResult}
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric.RawMetric
//Looks good
object ResponseValidator {

  sealed trait DomainValidation extends Exception {
    override def getMessage: String
  }

  case object PromDataHadMoreThanOneResult extends DomainValidation {
    override def getMessage: String = "Prometheus Response had more than one result. Correct Prometheus query"
  }

  case object PromDataHadNoData extends DomainValidation {
    override def getMessage: String = "Prometheus Response had no data. Correct your Prometheus query"
  }

  case object EmptyMetricName extends DomainValidation {
    override def getMessage: String = "Metric name cannot be empty"
  }

  def validateToRawMetric(promResponse: PromResponse, metricName: String): Either[Throwable, RawMetric] = {
    for {
      validMetricName <- validateMetricName(metricName)
      nonEmptyPromData <- validatePromDataNonEmpty(promResponse.data)
      validatedPromData <- validatePromDataHadOneResult(nonEmptyPromData)
    } yield RawMetric(validMetricName, validatedPromData.metricValue)
  }

  private def validateMetricName(name: String): Either[EmptyMetricName.type, String] = {
    Either.cond(
      name.nonEmpty,
      name,
      EmptyMetricName
    )
  }

  private def validatePromDataHadOneResult(promData: PromData): Either[PromDataHadMoreThanOneResult.type, PromResult] =
    Either.cond(
      promData.result.size == 1,
      promData.result.head,
      PromDataHadMoreThanOneResult
    )

  private def validatePromDataNonEmpty(promData: PromData): Either[PromDataHadNoData.type, PromData] =
    Either.cond(
      promData.result.nonEmpty,
      promData,
      PromDataHadNoData
    )
}
