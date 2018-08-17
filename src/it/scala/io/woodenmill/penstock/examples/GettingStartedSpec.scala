package io.woodenmill.penstock.examples

import java.net.URI

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.IO
import io.woodenmill.penstock.LoadRunner
import io.woodenmill.penstock.Metrics.Gauge
import io.woodenmill.penstock.backends.kafka.{KafkaBackend, KafkaMessage}
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric._
import io.woodenmill.penstock.metrics.prometheus.{PromQl, PrometheusConfig, PrometheusMetric}
import org.apache.kafka.common.serialization.{Serializer, StringSerializer}
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.duration._

class GettingStartedSpec extends AsyncFlatSpec with Matchers  {

  implicit val system: ActorSystem = ActorSystem("Getting-Started")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val kafkaBackend: KafkaBackend = KafkaBackend("localhost:9092")
  implicit val promConfig: PrometheusConfig = PrometheusConfig(new URI("localhost:9090"))
  implicit val stringSerializer: Serializer[String] = new StringSerializer()

  val topic = "input"
  val q = PromQl(s"""kafka_server_BrokerTopicMetrics_OneMinuteRate{name="MessagesInPerSec",topic="$topic"}""")


  "GettingStarted example" should "send messages to Kafka and use custom Prometheus metric to verify behaviour" in {
    //given
    val testMessage = KafkaMessage(topic, "test message").asRecord()

    val messageRateDefinition: IO[Gauge] = PrometheusMetric[Gauge](metricName ="messages rate", query = q)

    //when
    val loadFinished = LoadRunner(testMessage, duration = 2.minutes, throughput = 200).run()

    //then
    loadFinished.map { _ =>
      kafkaBackend.metrics().recordErrorTotal.value shouldBe 0
      kafkaBackend.metrics().recordSendTotal.value should be (24000L +- 1000L)
      messageRateDefinition.unsafeRunSync().value shouldBe 200.0 +- 20.0
    }
  }

}
