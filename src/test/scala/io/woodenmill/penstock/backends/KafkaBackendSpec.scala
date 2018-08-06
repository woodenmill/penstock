package io.woodenmill.penstock.backends

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.woodenmill.penstock.backends.kafka.KafkaBackend
import io.woodenmill.penstock.core.loadrunner.LoadRunner
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.duration._

class KafkaBackendSpec extends FlatSpec with Matchers with EmbeddedKafka with BeforeAndAfterAll with ScalaFutures {

  val topic = "input"
  val kafkaPort = 6001
  val kafkaBackend: KafkaBackend = KafkaBackend(s"localhost:$kafkaPort")
  implicit val stringDeserializer = new StringDeserializer()
  implicit val kafkaConfig = EmbeddedKafkaConfig(kafkaPort = kafkaPort)
  implicit val testPatienceConfig: PatienceConfig = PatienceConfig(timeout = 2.seconds)

  override protected def beforeAll(): Unit = {
    EmbeddedKafka.start()(kafkaConfig)
    createCustomTopic(topic)
  }

  override protected def afterAll(): Unit = {
    kafkaBackend.shutdown()
    EmbeddedKafka.stop()
  }


  "Kafka Backend" should "send a message to Kafka" in {
    //given
    val message = new ProducerRecord[Array[Byte], Array[Byte]](topic, "key".getBytes, "value".getBytes)

    //when
    kafkaBackend.send(message)

    //then
    consumeFirstKeyedMessageFrom[String, String](topic) shouldBe("key", "value")
  }

  it should "integrate with Load Runner" in {
    //given
    val system = ActorSystem()
    val mat = ActorMaterializer()(system)
    val message = new ProducerRecord[Array[Byte], Array[Byte]](topic, "from-runner".getBytes)

    //when
    val runnerFinished = LoadRunner(message, 1.milli, 1).run()(kafkaBackend, mat)

    //then
    whenReady(runnerFinished) { _ =>
      consumeFirstStringMessageFrom(topic) shouldBe "from-runner"
    }
  }

}