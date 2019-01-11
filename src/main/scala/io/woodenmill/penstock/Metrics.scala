package io.woodenmill.penstock

import io.woodenmill.penstock.Metrics.MetricName


object Metrics {
  type MetricName = String

  case class Counter(value: Long, name: MetricName) extends Metric[Long]

  case class Gauge(value: Double, name: MetricName) extends Metric[Double]

}

trait Metric[T] {
  def value: T

  def name: MetricName
}