package io.woodenmill.penstock.backends.kafka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.ovoenergy.kafka.serialization.circe._
import io.circe.generic.auto._
import io.woodenmill.penstock.LoadRunner
import io.woodenmill.penstock.testutils.{Ports, Spec}
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{Deserializer, Serializer, StringDeserializer}
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration._

class KafkaBackendSpec extends Spec with EmbeddedKafka with BeforeAndAfterAll {

  val topic: String = "input"
  val kafkaPort: Int = Ports.nextAvailablePort()
  val bootstrapServer: String = s"localhost:$kafkaPort"

  val system = ActorSystem()
  val mat = ActorMaterializer()(system)
  val kafkaBackend: KafkaBackend = KafkaBackend(bootstrapServer)
  implicit val stringDeserializer = new StringDeserializer()
  implicit val kafkaConfig = EmbeddedKafkaConfig(kafkaPort = kafkaPort, zooKeeperPort = Ports.nextAvailablePort())

  override protected def beforeAll(): Unit = {
    EmbeddedKafka.start()(kafkaConfig)
    createCustomTopic(topic)
  }

  override protected def afterAll(): Unit = {
    Await.ready(system.terminate(), 5.seconds)
    kafkaBackend.shutdown()
    EmbeddedKafka.stop()
  }


  "Kafka Backend" should "send a message to Kafka" in {
    val message = new ProducerRecord[Array[Byte], Array[Byte]](topic, "key".getBytes, "value".getBytes)

    kafkaBackend.send(message)

    consumeFirstKeyedMessageFrom[String, String](topic) shouldBe("key", "value")
  }

  it should "integrate with Load Runner" in {
    val message = new ProducerRecord[Array[Byte], Array[Byte]](topic, "from-runner".getBytes)

    val runnerFinished = LoadRunner(message, 1.milli, 1).run()(kafkaBackend, mat)

    whenReady(runnerFinished) { _ =>
      consumeFirstStringMessageFrom(topic) shouldBe "from-runner"
    }
  }

  it should "integrate with custom serializers" in {
    case class User(name: String, property: Double)
    implicit val userSer: Serializer[User] = circeJsonSerializer[User]

    val user = User("Dominic", 34.3)
    val message = KafkaMessage(topic, user).asRecord()

    val runnerFinished = LoadRunner(message, 1.milli, 1).run()(kafkaBackend, mat)

    whenReady(runnerFinished) { _ =>
      implicit val deserializer: Deserializer[User] = circeJsonDeserializer[User]
      consumeFirstMessageFrom[User](topic) shouldBe user
    }
  }

  it should "expose basic Kafka Producer metrics" in withNewKafkaBackend(bootstrapServer){ backend =>
    val someMessage = new ProducerRecord[Array[Byte], Array[Byte]](topic, "some message".getBytes)
    val metrics = backend.metrics()

    backend.send(someMessage)
    backend.send(someMessage)

    eventually {
      metrics.recordSendTotal.unsafeRunSync().value shouldBe 2
      metrics.recordErrorTotal.unsafeRunSync().value shouldBe 0
    }
  }

  it should "expose metrics before any message is sent" in withNewKafkaBackend(bootstrapServer){ backend =>
    backend.metrics().recordSendTotal.unsafeRunSync().value shouldBe 0
  }

  def withNewKafkaBackend(bootstrapServer: String)(f: KafkaBackend => Unit): Unit = {
    val backend = KafkaBackend(bootstrapServer)
    try f(backend)
    finally backend.shutdown()
  }

}