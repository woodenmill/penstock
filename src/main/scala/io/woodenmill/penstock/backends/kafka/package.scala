package io.woodenmill.penstock.backends

import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer

//TODO: Serialization can be a side effect. example: AvroSerializer. introduce IO
package object kafka {

  def createProducerRecord[K, V](topic: String, key: K, value: V)(implicit keySer: Serializer[K], valSer: Serializer[V]): ProducerRecord[Array[Byte], Array[Byte]] = {
    val valueBytes = valSer.serialize(topic, value)
    val keyBytes = keySer.serialize(topic, key)
    new ProducerRecord(topic, keyBytes, valueBytes)
  }

  def createProducerRecord[K, V](topic: String, value: V)(implicit valSer: Serializer[V]): ProducerRecord[Array[Byte], Array[Byte]] = {
    val valueBytes = valSer.serialize(topic, value)
    new ProducerRecord(topic, valueBytes)
  }

}
