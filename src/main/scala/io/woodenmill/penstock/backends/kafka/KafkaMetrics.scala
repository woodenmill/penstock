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
  private val timestamp = System.currentTimeMillis()

  lazy val recordSendTotal: Metrics.Counter = {
    val totalCount: Metric = rawMetrics(recordSendTotalName(producerId))
    val name = totalCount.metricName().name()
    val value = totalCount.metricValue().asInstanceOf[Double]
    Metrics.Counter(value.toLong, name, timestamp)
  }

  lazy val recordErrorTotal: Metrics.Counter = {
    val errorCount = rawMetrics(KafkaMetrics.recordErrorTotalName(producerId))
    val name = errorCount.metricName().name()
    val value = errorCount.metricValue().asInstanceOf[Double]
    Metrics.Counter(value.toLong, name, timestamp)
  }
}