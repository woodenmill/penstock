package io.woodenmill.penstock


object Metrics {
  case class Counter(value: Long, name: String) extends Metric[Long]
  case class Gauge(value: Double, name: String) extends Metric[Double]
}

trait Metric[T] {
  def value: T
  def name: String
}