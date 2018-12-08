package io.woodenmill.penstock.backends.kafka

import java.util.concurrent.TimeUnit.SECONDS
import cats.effect.IO
import io.woodenmill.penstock.backends.StreamingBackend
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.ByteArraySerializer
import scala.collection.JavaConverters._
import scala.concurrent.duration._


case class KafkaBackend(bootstrapServers: String) extends StreamingBackend[ProducerRecord[Array[Byte], Array[Byte]]] {
  private val config: Map[String, AnyRef] = Map(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> bootstrapServers)
  private val bytesSerializer = new ByteArraySerializer
  private val producer = new KafkaProducer[Array[Byte], Array[Byte]](config.asJava, bytesSerializer, bytesSerializer)

  lazy val metrics: KafkaProducerMetrics = KafkaProducerMetrics(IO {
    producer.metrics().asScala
      .map { case (metricName, metric) => (ProducerMetricName(metricName.name()), metric) }
      .toMap
  })

  override def send(msg: ProducerRecord[Array[Byte], Array[Byte]]): Unit = producer.send(msg)

  def shutdown(): Unit = producer.close(5, SECONDS)

  override def isReady: Boolean = KafkaReadiness.isReady(bootstrapServers, 5.second)
}