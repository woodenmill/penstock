package io.woodenmill.penstock.backends.kafka

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.IO
import io.woodenmill.penstock.testutils.{Ports, Spec}
import io.woodenmill.penstock.{LoadRunner, Metrics}
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
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

    val runnerFinished = LoadRunner(kafkaBackend).start(() => message, 1.milli, 1)(mat)

    whenReady(runnerFinished) { _ =>
      consumeFirstStringMessageFrom(topic) shouldBe "from-runner"
    }
  }

  it should "expose basic Kafka Producer metrics" in withNewKafkaBackend(bootstrapServer){ backend =>
    val someMessage = new ProducerRecord[Array[Byte], Array[Byte]](topic, "some message".getBytes)
    val metrics = backend.metrics

    backend.send(someMessage)
    backend.send(someMessage)

    eventually {
      metrics.recordSendTotal.unsafeRunSync().value shouldBe 2
      metrics.recordErrorTotal.unsafeRunSync().value shouldBe 0
    }
  }

  it should "allow creating a counter from producer metrics" in {
    val producerMetricIO: IO[Metrics.Counter] = kafkaBackend.metrics.counter(ProducerMetricName("outgoing-byte-total"))

    val producerMetric = producerMetricIO.unsafeRunSync()

    producerMetric.name shouldBe "outgoing-byte-total"
    producerMetric.value should be >= 0L
  }

  it should "allow creating a gauge from producer metrics" in {
    val producerMetricsIO: IO[Metrics.Gauge] = kafkaBackend.metrics.gauge(ProducerMetricName("outgoing-byte-rate"))

    val producerMetric = producerMetricsIO.unsafeRunSync()

    producerMetric.name shouldBe "outgoing-byte-rate"
    producerMetric.value should be >= 0.0
  }

  it should "give the list of supported producer metrics when given counter's name is incorrect" in {
    val incorrectMetric = kafkaBackend.metrics.counter(ProducerMetricName("incorrect-counter-name"))

    whenReady(incorrectMetric.unsafeToFuture().failed) { e =>
      e.getMessage should include("connection-count")
      e.getMessage should include("byte-total")
      e.getMessage should include("request-latency-max")
    }
  }

  it should "give the list of supported producer metrics when given gauge's name is incorrect" in {
    val incorrectMetric = kafkaBackend.metrics.gauge(ProducerMetricName("incorrect-gauge-name"))

    whenReady(incorrectMetric.unsafeToFuture().failed) { e =>
      e.getMessage should include("io-ratio")
      e.getMessage should include("response-rate")
    }
  }

  it should "expose metrics before any message is sent" in withNewKafkaBackend(bootstrapServer){ backend =>
    backend.metrics.recordSendTotal.unsafeRunSync().value shouldBe 0
  }

  "Kafka Backend Readiness" should "return true if Kafka is available" in {
    kafkaBackend.isReady shouldBe true
  }

  it should "return false if Kafka is not available" in withNewKafkaBackend("127.1.2.3:9092") { backend =>
    backend.isReady shouldBe false
  }


  def withNewKafkaBackend(bootstrapServer: String)(f: KafkaBackend => Unit): Unit = {
    val backend = KafkaBackend(bootstrapServer)
    try f(backend)
    finally backend.shutdown()
  }

}