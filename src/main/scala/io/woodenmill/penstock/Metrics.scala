package io.woodenmill.penstock


object Metrics {
  case class Counter(value: Long, name: String, time: Long) extends Metric[Long]
  case class Gauge(value: Double, name: String, time: Long) extends Metric[Double]
}

trait Metric[T] {
  def value: T
  def name: String
  def time: Long
}