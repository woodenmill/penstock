package io.woodenmill.penstock.backends.kafka

import java.util.UUID
import java.util.concurrent.TimeUnit.SECONDS

import cats.effect.IO
import io.woodenmill.penstock.backends.StreamingBackend
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.{Metric, MetricName}
import scala.concurrent.duration._
import scala.collection.JavaConverters._


case class KafkaBackend(bootstrapServers: String) extends StreamingBackend[ProducerRecord[Array[Byte], Array[Byte]]] {
  private val producerClientId = UUID.randomUUID().toString
  private val config: Map[String, AnyRef] = Map(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> bootstrapServers,
    ProducerConfig.CLIENT_ID_CONFIG -> producerClientId
  )
  private val bytesSerializer = new ByteArraySerializer
  private val producer = new KafkaProducer[Array[Byte], Array[Byte]](config.asJava, bytesSerializer, bytesSerializer)

  override def send(msg: ProducerRecord[Array[Byte], Array[Byte]]): Unit = producer.send(msg)

  def shutdown(): Unit = producer.close(5, SECONDS)

  def metrics(): KafkaMetrics = KafkaMetrics(rawMetrics, producerClientId)

  private val rawMetrics: IO[Map[MetricName, Metric]] = IO {producer.metrics().asScala.toMap }

  override def isReady: Boolean = KafkaReadiness.isReady(bootstrapServers, 5.second)
}