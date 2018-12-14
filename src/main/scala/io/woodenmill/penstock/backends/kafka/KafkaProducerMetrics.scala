package io.woodenmill.penstock.backends.kafka

import cats.effect.IO
import io.woodenmill.penstock.Metrics
import io.woodenmill.penstock.Metrics.{Counter, Gauge}
import io.woodenmill.penstock.backends.kafka.ProducerMetricNames._
import org.apache.kafka.common.Metric

object ProducerMetricNames {
  val RecordSendRateName: ProducerMetricName = ProducerMetricName("record-send-rate")
  val RecordSendTotalName: ProducerMetricName = ProducerMetricName("record-send-total")
  val RecordErrorTotalName: ProducerMetricName = ProducerMetricName("record-error-total")
}

case class ProducerMetricName(value: String)

case class KafkaProducerMetrics(allMetrics: IO[Map[ProducerMetricName, Metric]]) {

  lazy val recordSendTotal: IO[Counter] = counter(RecordSendTotalName)
  lazy val recordErrorTotal: IO[Counter] = counter(RecordErrorTotalName)
  lazy val recordSendRate: IO[Gauge] = gauge(RecordSendRateName)

  def counter(metricName: ProducerMetricName): IO[Counter] = {
    getMetric(allMetrics, metricName)
      .map { m =>
        val value = m.metricValue().asInstanceOf[Double]
        val name = m.metricName().name()
        Metrics.Counter(value.toLong, name)
      }
  }

  def gauge(gaugeName: ProducerMetricName): IO[Gauge] = {
    getMetric(allMetrics, gaugeName)
      .map { m =>
        val value = m.metricValue().asInstanceOf[Double]
        val name = m.metricName().name()
        Metrics.Gauge(value, name)
      }
  }

  private def getMetric(allMetrics: IO[Map[ProducerMetricName, Metric]], metricName: ProducerMetricName): IO[Metric] = {
    allMetrics.flatMap(metrics => metrics.get(metricName) match {
      case Some(producerMetric) => IO.pure(producerMetric)
      case None => IO.raiseError {
        new IllegalArgumentException(s"Metric name: $metricName is incorrect. Supported metric names are: ${metrics.keySet.mkString(", ")}")
      }
    })
  }
}