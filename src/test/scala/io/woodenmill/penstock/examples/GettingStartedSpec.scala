package io.woodenmill.penstock.examples

import java.net.URI

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.woodenmill.penstock.{LoadRunner, Metrics}
import io.woodenmill.penstock.Metrics.{Counter, MetricFactory}
import io.woodenmill.penstock.backends.kafka.KafkaBackend
import io.woodenmill.penstock.metrics.prometheus.Prometheus.{PromQl, PrometheusConfig}
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric
import org.apache.kafka.clients.producer.ProducerRecord
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.duration._

class GettingStartedSpec extends AsyncFlatSpec with Matchers  {


  implicit val system: ActorSystem = ActorSystem("Getting-Started")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val kafkaBackend: KafkaBackend = KafkaBackend("localhost:9092")
  implicit val promConfig: PrometheusConfig = PrometheusConfig(new URI("localhost:9090"))
  val mf: MetricFactory[Counter] = Metrics.counterFactory


  ignore should "send messages to Kafka and use custom Prometheus metric to verify behaviour" in {
    //given
    val topic: String = "input"
    val testMessage = new ProducerRecord[Array[Byte], Array[Byte]](topic, "test message".getBytes)
    val query = PromQl(s"""kafka_server_BrokerTopicMetrics_OneMinuteRate{name="MessagesInPerSec",topic="$topic"}""", mf)
    val kafkaTopicMessagesRate = PrometheusMetric[Counter](query)

    //when
    val loadFinished = LoadRunner(testMessage, duration = 5.minutes, throughput = 200).run()

    //then
    loadFinished.map { _ =>
      kafkaBackend.metrics().recordErrorTotal.value shouldBe 0
      kafkaBackend.metrics().recordSendTotal.value should be (60000L +- 1000L)
      kafkaTopicMessagesRate.value() shouldBe 200L +- 20L
    }
  }

}
