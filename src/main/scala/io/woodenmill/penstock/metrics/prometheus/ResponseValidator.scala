package io.woodenmill.penstock.metrics.prometheus

import io.woodenmill.penstock.Metrics.MetricName
import io.woodenmill.penstock.metrics.prometheus.PrometheusClient.RawMetric
import upickle.default.{macroR, _}
import scala.util.Try

object ResponseValidator {

  case class PromResponse(data: PromData)

  case class PromData(result: Seq[PromResult])

  case class PromResult(value: (Long, Double)) {
    val metricValue: Double = value._2
  }

  implicit val promResponseReader: Reader[PromResponse] = macroR
  implicit val promDataReader: Reader[PromData] = macroR
  implicit val promResultReader: Reader[PromResult] = macroR

  def validate(responseBody: String, metricName: MetricName): Either[Throwable, RawMetric] = {
    val validationResult = for {
      promResponse <- Try(read[PromResponse](responseBody)).toEither
      validMetricName <- validateMetricName(metricName)
      nonEmptyPromData <- validatePromDataNonEmpty(promResponse.data)
      validatedPromData <- validatePromDataHadOneResult(nonEmptyPromData)
    } yield RawMetric(validMetricName, validatedPromData.metricValue)

    validationResult.left.map(exception => new RuntimeException(s"Prometheus response is invalid. Response: $responseBody. ${exception.getMessage}", exception))
  }

  sealed trait Violation extends Exception {
    override def getMessage: String
  }

  case object PromDataHadMoreThanOneResult extends Violation {
    override def getMessage: String = "Prometheus Response had more than one result. Correct Prometheus query"
  }

  case object PromDataHadNoData extends Violation {
    override def getMessage: String = "Prometheus Response had no data. Correct your Prometheus query"
  }

  case object EmptyMetricName extends Violation {
    override def getMessage: String = "Metric name cannot be empty"
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
