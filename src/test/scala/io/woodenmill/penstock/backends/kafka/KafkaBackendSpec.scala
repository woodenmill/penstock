package io.woodenmill.penstock.backends.kafka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.woodenmill.penstock.LoadRunner
import io.woodenmill.penstock.testutils.{Ports, Spec}
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._
import scala.util.Try

class KafkaBackendSpec extends Spec with EmbeddedKafka with BeforeAndAfterAll {

  val topic: String = "input"
  val kafkaPort: Int = Ports.nextAvailablePort()
  val bootstrapServer: String = s"localhost:$kafkaPort"

  val kafkaBackend: KafkaBackend = KafkaBackend(bootstrapServer)
  implicit val stringDeserializer = new StringDeserializer()
  implicit val kafkaConfig = EmbeddedKafkaConfig(kafkaPort = kafkaPort, zooKeeperPort = Ports.nextAvailablePort())

  override protected def beforeAll(): Unit = {
    EmbeddedKafka.start()(kafkaConfig)
    createCustomTopic(topic)
  }

  override protected def afterAll(): Unit = {
    kafkaBackend.shutdown()
    EmbeddedKafka.stop()
  }


  "Kafka Backend" should "send a message to Kafka" in {
    val message = new ProducerRecord[Array[Byte], Array[Byte]](topic, "key".getBytes, "value".getBytes)

    kafkaBackend.send(message)

    consumeFirstKeyedMessageFrom[String, String](topic) shouldBe("key", "value")
  }

  it should "integrate with Load Runner" in {
    val system = ActorSystem()
    val mat = ActorMaterializer()(system)
    val message = new ProducerRecord[Array[Byte], Array[Byte]](topic, "from-runner".getBytes)

    val runnerFinished = LoadRunner(message, 1.milli, 1).run()(kafkaBackend, mat)

    whenReady(runnerFinished) { _ =>
      consumeFirstStringMessageFrom(topic) shouldBe "from-runner"
    }
  }

  it should "expose basic Kafka Producer metrics" in withNewKafkaBackend(bootstrapServer){ backend =>
    val someMessage = new ProducerRecord[Array[Byte], Array[Byte]](topic, "some message".getBytes)

    backend.send(someMessage)
    backend.send(someMessage)

    eventually {
      val metrics = backend.metrics()
      metrics.recordSendTotal.value shouldBe 2
      metrics.recordErrorTotal.value shouldBe 0
    }
  }

  it should "expose metrics before any message is sent" in withNewKafkaBackend(bootstrapServer){ backend =>
    backend.metrics().recordSendTotal.value shouldBe 0
  }

  def withNewKafkaBackend(bootstrapServer: String)(f: KafkaBackend => Unit): Unit = {
    val backend = KafkaBackend(bootstrapServer)
    Try( f(backend) )
    backend.shutdown()
  }

}