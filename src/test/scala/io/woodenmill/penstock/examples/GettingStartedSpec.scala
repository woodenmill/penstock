package io.woodenmill.penstock.examples

import java.net.URI
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.woodenmill.penstock.LoadRunner
import io.woodenmill.penstock.Metrics.Counter
import io.woodenmill.penstock.backends.kafka.KafkaBackend
import io.woodenmill.penstock.metrics.prometheus.Prometheus.{PromQl, PrometheusConfig}
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric
import io.woodenmill.penstock.testutils.{Ports, PromResponses, PrometheusIntegratedSpec}
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration._

class GettingStartedSpec extends AsyncFlatSpec with Matchers with EmbeddedKafka with PrometheusIntegratedSpec {

  val topic: String = "examples"
  val kafkaPort: Int = Ports.nextAvailablePort()

  implicit val kafkaConfig: EmbeddedKafkaConfig = EmbeddedKafkaConfig(kafkaPort = kafkaPort, zooKeeperPort = Ports.nextAvailablePort())

  implicit val kafkaBackend: KafkaBackend = KafkaBackend(s"localhost:$kafkaPort")
  implicit val system: ActorSystem = ActorSystem("Getting-Started")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val promConfig: PrometheusConfig = PrometheusConfig(new URI(s"localhost:$prometheusPort"))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    EmbeddedKafka.start()(kafkaConfig)
    createCustomTopic(topic)
  }


  "Getting Started" should "send messages to Kafka and use custom Prometheus metric to verify behaviour" in {
    //given
    configurePromStub("up", PromResponses.valid("1"))
    val testMessage = new ProducerRecord[Array[Byte], Array[Byte]](topic, "test message".getBytes)
    val customMetric = PrometheusMetric[Counter](PromQl("up").get)

    //when
    val loadFinished = LoadRunner(testMessage, duration = 3.seconds, throughput = 200).run()

    //then
    loadFinished.map { _ =>
      kafkaBackend.metrics().recordErrorTotal.value shouldBe 0
      kafkaBackend.metrics().recordSendTotal.value should be (600L +- 50L)
      customMetric.value() shouldBe 1
    }
  }

  override protected def afterAll(): Unit = {
    Await.ready(system.terminate(), 5.seconds)
    kafkaBackend.shutdown()
    EmbeddedKafka.stop()
    super.afterAll()
  }

}
