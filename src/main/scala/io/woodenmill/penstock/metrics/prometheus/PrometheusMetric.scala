package io.woodenmill.penstock.metrics.prometheus

import cats.effect.IO
import io.woodenmill.penstock.Metric
import io.woodenmill.penstock.Metrics.{Counter, Gauge}
import io.woodenmill.penstock.metrics.prometheus.PrometheusClient.{PrometheusConfig, RawMetric}

object PrometheusMetric {

  def apply[M <: Metric[_]](metricName: String, query: PromQl)(implicit config: PrometheusConfig, f: RawMetric => M): IO[M] = {
    PrometheusClient.fetch(config)(metricName, query).map(f)
  }

  implicit val promValueToGauge: RawMetric => Gauge = m => Gauge(m.metricValue, m.metricName)
  implicit val promValueToCounter: RawMetric => Counter = m => Counter(m.metricValue.toLong, m.metricName)
}