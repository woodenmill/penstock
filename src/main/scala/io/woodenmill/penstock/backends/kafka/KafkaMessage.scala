package io.woodenmill.penstock.backends.kafka

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer

object KafkaMessage {
  def apply[V](topic: String, value: V): KafkaMessage[V,V] = KafkaMessage[V,V](topic, None, value)
  def apply[K,V](topic: String, key: K, value: V): KafkaMessage[K,V] = KafkaMessage[K,V](topic, Option(key), value)
}

case class KafkaMessage[K,V](topic: String, key: Option[K], value: V) {

  def asRecord()(implicit keySer: Serializer[K], valSer: Serializer[V]): ProducerRecord[Array[Byte], Array[Byte]] = {
    val valueBytes = valSer.serialize(topic, value)
    key match {
      case Some(k) =>
        val keyBytes = keySer.serialize(topic, k)
        new ProducerRecord(topic, keyBytes, valueBytes)
      case None =>
        new ProducerRecord(topic, valueBytes)
    }
  }
}