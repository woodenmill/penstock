package io.woodenmill.penstock.metrics.prometheus
//TODO: does Penstock really need this case class?
case class PromQl(query: String) {
  override def toString: String = s"query=$query"
}