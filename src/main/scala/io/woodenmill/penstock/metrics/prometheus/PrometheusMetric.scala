package io.woodenmill.penstock.metrics.prometheus

import cats.effect.IO
import io.woodenmill.penstock.Metric
import io.woodenmill.penstock.Metrics.{Counter, Gauge}
//Looks good
object PrometheusMetric {

  def apply[M <: Metric[_]](metricName: String, query: PromQl)(implicit config: PrometheusConfig, f: RawMetric => M): IO[M] = {
    PrometheusClient(config)
      .fetch(metricName, query)
      .map(f)
  }

  case class RawMetric(metricName: String, metricValue: Double)

  implicit val promValueToGauge: RawMetric => Gauge = m => Gauge(m.metricValue, m.metricName)
  implicit val promValueToCounter: RawMetric => Counter = m => Counter(m.metricValue.toLong, m.metricName)
}