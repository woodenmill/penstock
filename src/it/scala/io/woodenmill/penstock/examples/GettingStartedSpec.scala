package io.woodenmill.penstock.examples

import java.net.URI
import java.util.UUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.IO
import io.woodenmill.penstock.LoadGenerator
import io.woodenmill.penstock.Metrics.{Counter, Gauge}
import io.woodenmill.penstock.backends.kafka._
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric._
import io.woodenmill.penstock.metrics.prometheus.{PromQl, PrometheusConfig, PrometheusMetric}
import io.woodenmill.penstock.report.ConsoleReport
import org.apache.kafka.common.serialization.{Serializer, StringSerializer}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class GettingStartedSpec extends FlatSpec with Matchers with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem("Getting-Started")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = 5.minutes)

  val kafkaBackend: KafkaBackend = KafkaBackend(bootstrapServers = "localhost:9092")
  val kafkaLoadGenerator = LoadGenerator(backend = kafkaBackend)

  implicit val promConfig: PrometheusConfig = PrometheusConfig(new URI("localhost:9090"))
  implicit val stringSerializer: Serializer[String] = new StringSerializer()

  val topic = "input"
  val q = PromQl(s"""kafka_server_BrokerTopicMetrics_OneMinuteRate{name="MessagesInPerSec",topic="$topic"}""")


  "GettingStarted example" should "send messages to Kafka and use custom Prometheus metric to verify behaviour" in {
    //given
    val messageGen = () => List(createProducerRecord(topic, s"test message, ID: ${UUID.randomUUID()}"))

    val kafkaMessageInRate: IO[Gauge] = PrometheusMetric[Gauge](metricName = "kafka-messages-in-rate", query = q)
    val recordErrorTotal: IO[Counter] = kafkaBackend.metrics.recordErrorTotal
    val recordSendTotal: IO[Counter] = kafkaBackend.metrics.recordSendTotal
    val recordSendRate: IO[Gauge] = kafkaBackend.metrics.recordSendRate
    val report = ConsoleReport(kafkaMessageInRate, recordSendRate, recordSendTotal, recordErrorTotal)

    //when
    val finished = kafkaLoadGenerator.generate(messageGen, duration = 2.minutes, throughput = 200).unsafeToFuture()
    report.runEvery(10.seconds)

    //then
    whenReady(finished) { _ =>
      recordErrorTotal.unsafeRunSync().value shouldBe 0
      recordSendTotal.unsafeRunSync().value shouldBe (24000L +- 1000L)
      kafkaMessageInRate.unsafeRunSync().value shouldBe 200.0 +- 20.0
    }
  }
}
