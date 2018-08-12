package io.woodenmill.penstock.metrics.prometheus


import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.{ask, pipe}
import akka.util.Timeout
import io.woodenmill.penstock.Metric
import io.woodenmill.penstock.metrics.prometheus.MetricFetcher.{Fetch, RespondMetric}
import io.woodenmill.penstock.metrics.prometheus.Prometheus.PromQl

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

case class PrometheusMetric[T <: Metric[_]](query: PromQl[T])(implicit config: Prometheus.PrometheusConfig, actorSystem: ActorSystem) {
  private implicit val askTimeout: Timeout = Timeout(5.seconds)
  private val fetcher: ActorRef = actorSystem.actorOf(MetricFetcher.props(query, config))

  def value(): T = Await.result(fetcher.ask(MetricFetcher.Fetch()).mapTo[RespondMetric[T]], 1.second).value
}


object MetricFetcher {
  def props[M <: Metric[_]](query: PromQl[M], config: Prometheus.PrometheusConfig): Props = Props(new MetricFetcher(query, config))

  final case class Fetch()
  final case class RespondMetric[T](value: T)
}

class MetricFetcher[M <: Metric[_]](query: PromQl[M], config: Prometheus.PrometheusConfig) extends Actor with ActorLogging {
  implicit val ec: ExecutionContext = context.dispatcher
  val promClient: PrometheusClient = PrometheusClient(config)

  override def receive: Receive = {
    case Fetch() =>
      val eventualMetric: Future[M] = promClient.fetch[M](query)(ec)
      eventualMetric.map(m => RespondMetric[M](m)) pipeTo sender()
  }

}