package io.woodenmill.penstock.backends.kafka

import com.ovoenergy.kafka.serialization.circe._
import io.circe.generic.auto._
import io.woodenmill.penstock.testutils.Spec
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{IntegerDeserializer, IntegerSerializer, Serializer, StringSerializer}

class CreateProducerRecordSpec extends Spec {
  implicit val stringSer: Serializer[String] = new StringSerializer()
  implicit val integerSer: Serializer[Integer] = new IntegerSerializer()

  "KafkaMessageSpec" should "be easily converted to ProducerRecord" in {
    val record: ProducerRecord[Array[Byte], Array[Byte]] = createProducerRecord("topic", "test message")

    record.topic() shouldBe "topic"
    record.value() shouldBe "test message".getBytes
  }

  it should "accept different types as a value, for instance Integer" in {
    val intDeserializer = new IntegerDeserializer()

    val record = createProducerRecord("sometopic", new Integer(42))

    intDeserializer.deserialize(record.topic, record.value) shouldBe 42
  }

  it should "accept a case class as a value" in {
    case class User(age: Int, name: String)
    implicit val userSer: Serializer[User] = circeJsonSerializer[User]
    val userDes = circeJsonDeserializer[User]
    val user = User(56, "Rose")

    val record = createProducerRecord("sometopic", user)

    userDes.deserialize(record.topic, record.value()) shouldBe user
  }

  it should "allow to specify a message key" in {
    val record: ProducerRecord[Array[Byte], Array[Byte]] = createProducerRecord("topic", "key", "value")

    record.topic() shouldBe "topic"
    record.key() shouldBe "key".getBytes
    record.value() shouldBe "value".getBytes
  }
}
