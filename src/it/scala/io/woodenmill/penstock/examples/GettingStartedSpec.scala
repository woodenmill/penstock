package io.woodenmill.penstock.examples

import java.net.URI

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.IO
import io.woodenmill.penstock.LoadRunner
import io.woodenmill.penstock.Metrics.{Counter, Gauge}
import io.woodenmill.penstock.backends.kafka.{KafkaBackend, KafkaMessage}
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric._
import io.woodenmill.penstock.metrics.prometheus.{PromQl, PrometheusConfig, PrometheusMetric}
import io.woodenmill.penstock.report.ConsoleReport
import org.apache.kafka.common.serialization.{Serializer, StringSerializer}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.{Await, ExecutionContext}
import scala.concurrent.duration._

class GettingStartedSpec extends FlatSpec with Matchers {

  implicit val system: ActorSystem = ActorSystem("Getting-Started")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher

  implicit val kafkaBackend: KafkaBackend = KafkaBackend("localhost:9092")
  implicit val promConfig: PrometheusConfig = PrometheusConfig(new URI("localhost:9090"))
  implicit val stringSerializer: Serializer[String] = new StringSerializer()

  val topic = "input"
  val q = PromQl(s"""kafka_server_BrokerTopicMetrics_OneMinuteRate{name="MessagesInPerSec",topic="$topic"}""")


  "GettingStarted example" should "send messages to Kafka and use custom Prometheus metric to verify behaviour" in {
    //given
    val testMessage = KafkaMessage(topic, "test message").asRecord()
    val messageRateDefinition: IO[Gauge] = PrometheusMetric[Gauge](metricName = "messages rate", query = q)
    val recordErrorTotal: IO[Counter] = kafkaBackend.metrics().recordErrorTotal
    val recordSendTotal: IO[Counter] = kafkaBackend.metrics().recordSendTotal

    //when
    val loadFinished = LoadRunner(testMessage, duration = 2.minutes, throughput = 200).run()

    //then
    ConsoleReport(messageRateDefinition, recordSendTotal, recordErrorTotal).runEvery(10.seconds)

    Await.ready(loadFinished, 5.minutes)

    recordErrorTotal.unsafeRunSync().value shouldBe 0
    recordSendTotal.unsafeRunSync().value shouldBe (24000L +- 1000L)
    messageRateDefinition.unsafeRunSync().value shouldBe 200.0 +- 20.0
  }

}
