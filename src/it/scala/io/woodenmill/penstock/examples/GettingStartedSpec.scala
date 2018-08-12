package io.woodenmill.penstock.examples

import java.net.URI

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import io.woodenmill.penstock.Metrics.{Gauge, MetricFactory}
import io.woodenmill.penstock.backends.kafka.{KafkaBackend, KafkaMessage}
import io.woodenmill.penstock.metrics.prometheus.Prometheus.{PromQl, PrometheusConfig}
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric
import io.woodenmill.penstock.{LoadRunner, Metrics}
import org.apache.kafka.common.serialization.{Serializer, StringSerializer}
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.duration._

class GettingStartedSpec extends AsyncFlatSpec with Matchers  {

  implicit val system: ActorSystem = ActorSystem("Getting-Started")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val kafkaBackend: KafkaBackend = KafkaBackend("localhost:9092")
  implicit val promConfig: PrometheusConfig = PrometheusConfig(new URI("localhost:9090"))
  implicit val stringSerializer: Serializer[String] = new StringSerializer()
  val mf: MetricFactory[Gauge] = Metrics.gaugeFactory


  "GettingStarted example" should "send messages to Kafka and use custom Prometheus metric to verify behaviour" in {
    //given
    val topic = "input"
    val testMessage = KafkaMessage(topic, "test message").asRecord()
    val query = PromQl[Gauge](s"""kafka_server_BrokerTopicMetrics_OneMinuteRate{name="MessagesInPerSec",topic="$topic"}""", mf)
    val kafkaTopicMessagesRate = PrometheusMetric[Gauge](query)

    //when
    val loadFinished = LoadRunner(testMessage, duration = 2.minutes, throughput = 200).run()

    //then
    loadFinished.map { _ =>
      kafkaBackend.metrics().recordErrorTotal.value shouldBe 0
      kafkaBackend.metrics().recordSendTotal.value should be (24000L +- 1000L)
      kafkaTopicMessagesRate.value().value shouldBe 200.0 +- 20.0
    }
  }

}
