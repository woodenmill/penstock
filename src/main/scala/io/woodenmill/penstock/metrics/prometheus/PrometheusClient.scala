package io.woodenmill.penstock.metrics.prometheus

import java.net.URI
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.woodenmill.penstock.Metrics
import io.woodenmill.penstock.Metrics._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}


object Prometheus {

  case class PrometheusConfig(prometheusUrl: URI)

  object PromQl {
    def apply(queryToParse: String): Try[PromQl] = Success(new PromQl(queryToParse))
  }

  case class PromQl(query: String)
}


case class PrometheusClient(config: Prometheus.PrometheusConfig) {
  private implicit val backend = AsyncHttpClientFutureBackend()

  def fetch(query: Prometheus.PromQl)(implicit ec: ExecutionContext): Future[Metrics.Counter] = {
    val promApi = Uri(config.prometheusUrl).path("/api/v1/query")
    val request = sttp.get(promApi.param("query", query.query))
    request.send().flatMap {
      case r @ Response(Right(body), code, _, _, _) if r.isSuccess =>
        Future.fromTry( MetricExtractor.extract[Counter](body) )
      case Response(Left(body), code, _, _, _) =>
        Future.failed(new RuntimeException(s"Querying Prometheus has failed. Query: $query. Response: status=$code, body=${new String(body)}"))
    }
  }
}
