package io.woodenmill.penstock.metrics.prometheus

case class PromQl(query: String) {
  override def toString: String = s"query=$query"
}