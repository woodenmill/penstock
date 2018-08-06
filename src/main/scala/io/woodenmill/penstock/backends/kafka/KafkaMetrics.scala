package io.woodenmill.penstock.backends.kafka

import io.woodenmill.penstock.Metrics
import io.woodenmill.penstock.backends.kafka.KafkaMetrics.recordSendTotalName
import org.apache.kafka.common.{Metric, MetricName}

import scala.collection.JavaConverters._

object KafkaMetrics {
  val recordSendTotalName: MetricName = new MetricName("record-send-total", "producer-metrics", "", Map("client-id"->KafkaBackend.producerClientId).asJava)
  val recordErrorTotalName: MetricName = new MetricName("record-error-total", "producer-metrics", "", Map("client-id"->KafkaBackend.producerClientId).asJava)
}

case class KafkaMetrics(rawMetrics: Map[MetricName, Metric]) {

  lazy val recordSendTotal: Metrics.Counter = {
    val totalCount = rawMetrics(recordSendTotalName).metricValue().asInstanceOf[Double]
    Metrics.Counter(totalCount.toLong)
  }

  lazy val recordErrorTotal: Metrics.Counter = {
    val errorCount = rawMetrics(KafkaMetrics.recordErrorTotalName).metricValue().asInstanceOf[Double]
    Metrics.Counter(errorCount.toLong)
  }
}