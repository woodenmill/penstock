package io.woodenmill.penstock.metrics.prometheus

import cats.effect.IO
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.cats.AsyncHttpClientCatsBackend
import io.woodenmill.penstock.metrics.prometheus.PrometheusClient.{PromResponse, backend}
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric.RawMetric
import upickle.default._

import scala.util.{Failure, Success, Try}

//TODO: close backend
//TODO: are all cases covered in that flatmap?

object PrometheusClient {

  case class PromResponse(data: PromData)

  case class PromData(result: Seq[PromResult])

  case class PromResult(value: (Long, Double)) {
    val metricValue: Double = value._2
  }

  implicit val backend: SttpBackend[IO, Nothing] = AsyncHttpClientCatsBackend()
  implicit val promResponseReader: Reader[PromResponse] = macroR
  implicit val promDataReader: Reader[PromData] = macroR
  implicit val promResultReader: Reader[PromResult] = macroR
}

case class PrometheusClient(config: PrometheusConfig) {
  private val promApi = Uri(config.prometheusUrl).path("/api/v1/query")

  def fetch(metricName: String, query: PromQl): IO[RawMetric] = {
    def extractRawMetric(json: String): Either[Throwable, RawMetric] = {
      Try(read[PromResponse](json)) match {
        case Success(promResponse) => ResponseValidator.validateToRawMetric(promResponse, metricName)
        case Failure(exception) => Left(new RuntimeException(s"Prometheus response is invalid. Response: $json", exception))
      }
    }

    sttp
      .get(promApi.param("query", query.query))
      .send()
      .flatMap {
        case r@Response(Right(body), _, _, _, _) if r.isSuccess =>
          IO.fromEither(extractRawMetric(body))
        case Response(Left(body), code, _, _, _) =>
          IO.raiseError(new RuntimeException(s"Querying Prometheus has failed. $query. Response: status=$code, body=${new String(body)}"))
      }
  }

}
