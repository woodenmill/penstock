package io.woodenmill.penstock.backends.kafka

import cats.effect.IO
import io.woodenmill.penstock.Metrics
import io.woodenmill.penstock.Metrics.{Counter, Gauge}
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

  def recordSendRateName(clientId: String): MetricName = new MetricName(
    "record-send-rate",
    "producer-metrics",
    "",
    Map("client-id"->clientId).asJava
  )
}

case class KafkaMetrics(rawMetricsIO: IO[Map[MetricName, Metric]], producerId: String) {

  lazy val recordSendTotal: IO[Counter] = {
    for {
      rawMetrics <- rawMetricsIO
      totalCount = rawMetrics(recordSendTotalName(producerId))
      name = totalCount.metricName().name()
      value = totalCount.metricValue().asInstanceOf[Double]
      timestamp = System.currentTimeMillis()
    } yield Metrics.Counter(value.toLong, name, timestamp)
  }

  lazy val recordErrorTotal: IO[Counter]= {
    for {
      rawMetrics <- rawMetricsIO
      errorCount: Metric = rawMetrics(KafkaMetrics.recordErrorTotalName(producerId))
      name = errorCount.metricName().name()
      value = errorCount.metricValue().asInstanceOf[Double]
      timestamp = System.currentTimeMillis()
    } yield Metrics.Counter(value.toLong, name, timestamp)
  }

  lazy val recordSendRate: IO[Gauge]= {
    for {
      rawMetrics <- rawMetricsIO
      recordSendRate: Metric = rawMetrics(KafkaMetrics.recordSendRateName(producerId))
      name = recordSendRate.metricName().name()
      value = recordSendRate.metricValue().asInstanceOf[Double]
      timestamp = System.currentTimeMillis()
    } yield Metrics.Gauge(value, name, timestamp)
  }
}