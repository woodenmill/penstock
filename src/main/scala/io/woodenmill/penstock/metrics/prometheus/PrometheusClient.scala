package io.woodenmill.penstock.metrics.prometheus

import java.net.URL

import cats.effect.IO
import org.apache.http.client.fluent.{Async, Content, Request}
import org.apache.http.concurrent.FutureCallback

import scala.concurrent.duration.{FiniteDuration, _}


object PrometheusClient {

  case class PrometheusConfig(prometheusUrl: URL, connectionTimeout: FiniteDuration = 3.seconds, socketTimeout: FiniteDuration = 1.second)

  case class RawMetric(metricName: String, metricValue: Double)


  def fetch(config: PrometheusConfig)(metricName: String, query: PromQl): IO[RawMetric] = {

    def buildRequest(query: PromQl, config: PrometheusConfig): Request = Request
      .Get(new URL(s"${config.prometheusUrl}/api/v1/query?query=${query.query}").toURI)
      .connectTimeout(config.connectionTimeout.toMillis.toInt)
      .socketTimeout(config.socketTimeout.toMillis.toInt)

    def httpClientCallback(callback: Either[Throwable, RawMetric] => Unit) = new FutureCallback[Content] {
      override def completed(content: Content): Unit = callback(ResponseValidator.validate(content.asString(), metricName))

      override def failed(ex: Exception): Unit = callback(Left(ex))

      override def cancelled(): Unit = callback(Left(new RuntimeException("Http call to Prometheus has been cancelled")))
    }

    IO.async(callback => Async.newInstance().execute(buildRequest(query, config), httpClientCallback(callback)))
  }
}
