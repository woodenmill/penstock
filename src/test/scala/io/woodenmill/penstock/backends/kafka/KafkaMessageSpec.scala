package io.woodenmill.penstock.backends.kafka

import com.ovoenergy.kafka.serialization.circe._
import io.circe.generic.auto._
import io.woodenmill.penstock.testutils.Spec
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{IntegerDeserializer, IntegerSerializer, Serializer, StringSerializer}

class KafkaMessageSpec extends Spec {
  implicit val stringSer: Serializer[String] = new StringSerializer()
  implicit val integerSer: Serializer[Integer] = new IntegerSerializer()

  "KafkaMessageSpec" should "be easily converted to ProducerRecord" in {
    val kafkaMessage = KafkaMessage("topic", "test message")

    val record: ProducerRecord[Array[Byte], Array[Byte]] = kafkaMessage.asRecord()

    record.topic() shouldBe "topic"
    record.value() shouldBe "test message".getBytes
  }

  it should "accept different types as a value, for instance Integer" in {
    val intDeserializer = new IntegerDeserializer()

    val record = KafkaMessage("sometopic", new Integer(42)).asRecord()

    intDeserializer.deserialize(record.topic, record.value) shouldBe 42
  }

  it should "accept a case class as a value" in {
    case class User(age: Int, name: String)
    implicit val userSer: Serializer[User] = circeJsonSerializer[User]
    val userDes = circeJsonDeserializer[User]
    val user = User(56, "Rose")

    val record = KafkaMessage("sometopic", user).asRecord()

    userDes.deserialize(record.topic, record.value()) shouldBe user
  }

  it should "allow to specify a message key" in {
    val kafkaMessage = KafkaMessage("topic", "key", "value")

    val record: ProducerRecord[Array[Byte], Array[Byte]] = kafkaMessage.asRecord()

    record.topic() shouldBe "topic"
    record.key() shouldBe "key".getBytes
    record.value() shouldBe "value".getBytes
  }


}
