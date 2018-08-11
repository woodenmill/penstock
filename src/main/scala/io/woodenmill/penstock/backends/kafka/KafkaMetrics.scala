package io.woodenmill.penstock.backends.kafka

import io.woodenmill.penstock.Metrics
import io.woodenmill.penstock.backends.kafka.KafkaMetrics.recordSendTotalName
import org.apache.kafka.common.{Metric, MetricName}

import scala.collection.JavaConverters._

object KafkaMetrics {
  def recordSendTotalName(clientId: String): MetricName = new MetricName(
    "record-send-total",
    "producer-metrics",
    "",
    Map("client-id"->clientId).asJava
  )
  def recordErrorTotalName(clientId: String): MetricName = new MetricName(
    "record-error-total",
    "producer-metrics",
    "",
    Map("client-id"->clientId).asJava
  )
}

case class KafkaMetrics(rawMetrics: Map[MetricName, Metric], producerId: String) {

  lazy val recordSendTotal: Metrics.Counter = {
    val totalCount = rawMetrics(recordSendTotalName(producerId)).metricValue().asInstanceOf[Double]
    Metrics.Counter(totalCount.toLong)
  }

  lazy val recordErrorTotal: Metrics.Counter = {
    val errorCount = rawMetrics(KafkaMetrics.recordErrorTotalName(producerId)).metricValue().asInstanceOf[Double]
    Metrics.Counter(errorCount.toLong)
  }
}