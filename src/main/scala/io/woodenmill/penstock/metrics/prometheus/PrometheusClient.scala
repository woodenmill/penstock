package io.woodenmill.penstock.metrics.prometheus

import java.net.URI

import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.future.AsyncHttpClientFutureBackend
import io.woodenmill.penstock.Metric
import io.woodenmill.penstock.Metrics._
import io.woodenmill.penstock.metrics.prometheus.Prometheus.PromQl

import scala.concurrent.{ExecutionContext, Future}


object Prometheus {

  case class PrometheusConfig(prometheusUrl: URI)

  case class PromQl[M <: Metric[_]](query: String, metricFactory: MetricFactory[M]) {
    override def toString: String = s"query=$query"
  }

}


case class PrometheusClient(config: Prometheus.PrometheusConfig) {
  private implicit val backend = AsyncHttpClientFutureBackend()

  def fetch[B <: Metric[_]](query: PromQl[B])(implicit ec: ExecutionContext): Future[B] = {
    val promApi = Uri(config.prometheusUrl).path("/api/v1/query")
    val request = sttp.get(promApi.param("query", query.query))

    request.send().flatMap {
      case r @ Response(Right(body), code, _, _, _) if r.isSuccess =>
        val triedCounter = MetricExtractor.extract[B](body)(query.metricFactory)
        Future.fromTry( triedCounter )
      case Response(Left(body), code, _, _, _) =>
        Future.failed(new RuntimeException(s"Querying Prometheus has failed. $query. Response: status=$code, body=${new String(body)}"))
    }
  }
}
