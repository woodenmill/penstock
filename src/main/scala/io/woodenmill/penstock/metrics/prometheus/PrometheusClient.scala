package io.woodenmill.penstock.metrics.prometheus

import cats.effect.IO
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.{Decoder, HCursor}
import io.woodenmill.penstock.metrics.prometheus.PrometheusClient.PromResponse
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric.RawMetric
import PrometheusClient.backend

object PrometheusClient {
  implicit val backend: SttpBackend[IO, Nothing] = AsyncHttpClientCatsBackend()

  implicit val decodePromValue: Decoder[PromValue] = (c: HCursor) => for {
    t <- c.downArray.as[Double]
    v <- c.downArray.right.as[Double]
  } yield PromValue(t.toLong, v)

  case class PromResponse(data: PromData)
  case class PromData(result: Seq[PromResult])
  case class PromResult(value: PromValue)
  case class PromValue(timestamp: Long, metricValue: Double)
}

case class PrometheusClient(config: PrometheusConfig) {
  private val promApi = Uri(config.prometheusUrl).path("/api/v1/query")

  def fetch(metricName: String, query: PromQl): IO[RawMetric] = {
    sttp
      .get(promApi.param("query", query.query))
      .send()
      .flatMap {
        case r@Response(Right(body), _, _, _, _) if r.isSuccess =>
          IO.fromEither {
            decode[PromResponse](body).flatMap(ResponseValidator.validateToRawMetric(_, metricName))
          }
        case Response(Left(body), code, _, _, _) =>
          IO.raiseError(new RuntimeException(s"Querying Prometheus has failed. $query. Response: status=$code, body=${new String(body)}"))
      }
  }

}
