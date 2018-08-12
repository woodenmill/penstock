package io.woodenmill.penstock

import scala.util.Try

object Metrics {
  type MetricFactory[B] = String => Try[B]

  implicit val counterFactory: String => Try[Counter] = v => Try(Counter(v.toLong))
  case class Counter(value: Long) extends Metric[Long]

  implicit val gaugeFactory: String => Try[Gauge] = v => Try(Gauge(v.toDouble))
  case class Gauge(value: Double) extends Metric[Double]

}

trait Metric[T] {
  def value: T
}