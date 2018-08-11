package io.woodenmill.penstock

import scala.util.Try

object Metrics {
  implicit val counterFactory: String => Try[Counter] = v => Try(Counter(v.toLong))
  case class Counter(value: Long) extends Metric[Long]
}

trait Metric[T] {
  def value: T
}