package io.woodenmill.penstock.metrics.prometheus

import java.net.URL

import cats.effect.IO
import org.apache.http.client.fluent.{Async, Content, Request}
import org.apache.http.concurrent.FutureCallback
import upickle.default._

import scala.concurrent.duration.{FiniteDuration, _}
import scala.util.{Failure, Success, Try}


object PrometheusClient {

  case class PrometheusConfig(prometheusUrl: URL, connectionTimeout: FiniteDuration = 3.seconds, socketTimeout: FiniteDuration = 1.second)

  case class RawMetric(metricName: String, metricValue: Double)

  case class PromResponse(data: PromData)

  case class PromData(result: Seq[PromResult])

  case class PromResult(value: (Long, Double)) {
    val metricValue: Double = value._2
  }

  implicit val promResponseReader: Reader[PromResponse] = macroR
  implicit val promDataReader: Reader[PromData] = macroR
  implicit val promResultReader: Reader[PromResult] = macroR


  def fetch(config: PrometheusConfig)(metricName: String, query: PromQl): IO[RawMetric] = {

    def buildRequest(query: PromQl, config: PrometheusConfig): Request = Request
      .Get(new URL(s"${config.prometheusUrl}/api/v1/query?query=${query.query}").toURI)
      .connectTimeout(config.connectionTimeout.toMillis.toInt)
      .socketTimeout(config.socketTimeout.toMillis.toInt)

    def extractRawMetric(json: String): Either[Throwable, RawMetric] = {
      Try(read[PromResponse](json)) match {
        case Success(promResponse) => ResponseValidator.validateToRawMetric(promResponse, metricName)
        case Failure(exception) => Left(new RuntimeException(s"Prometheus response is invalid. Response: $json", exception))
      }
    }

    def httpClientCallback(callback: Either[Throwable, RawMetric] => Unit) = new FutureCallback[Content] {
      override def completed(content: Content): Unit = callback(extractRawMetric(content.asString()))

      override def failed(ex: Exception): Unit = callback(Left(ex))

      override def cancelled(): Unit = callback(Left(new RuntimeException("Http call to Prometheus has been cancelled")))
    }

    IO.async(callback => Async.newInstance().execute(buildRequest(query, config), httpClientCallback(callback)))
  }
}
