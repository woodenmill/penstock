package io.woodenmill.penstock.examples

import java.net.URI
import java.util.UUID

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
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class GettingStartedSpec extends FlatSpec with Matchers with ScalaFutures {

  implicit val system: ActorSystem = ActorSystem("Getting-Started")
  implicit val mat: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = system.dispatcher
  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = 5.minutes)

  implicit val kafkaBackend: KafkaBackend = KafkaBackend("localhost:9092")
  implicit val promConfig: PrometheusConfig = PrometheusConfig(new URI("localhost:9090"))
  implicit val stringSerializer: Serializer[String] = new StringSerializer()

  val topic = "input"
  val q = PromQl(s"""kafka_server_BrokerTopicMetrics_OneMinuteRate{name="MessagesInPerSec",topic="$topic"}""")


  "GettingStarted example" should "send messages to Kafka and use custom Prometheus metric to verify behaviour" in {
    //given
    var expectedOutputMessages = List[String]()

    val messageGen = () => {
      val id = UUID.randomUUID().toString
      expectedOutputMessages = id :: expectedOutputMessages
      List(KafkaMessage(topic, s"test message, ID: $id").asRecord())
    }

    val kafkaMessageInRate: IO[Gauge] = PrometheusMetric[Gauge](metricName = "kafka-messages-in-rate", query = q)
    val recordErrorTotal: IO[Counter] = kafkaBackend.metrics().recordErrorTotal
    val recordSendTotal: IO[Counter] = kafkaBackend.metrics().recordSendTotal
    val recordSendRate: IO[Gauge] = kafkaBackend.metrics().recordSendRate

    //when
    val loadFinished = LoadRunner(messageGen, duration = 2.minutes, throughput = 200).run()
    ConsoleReport(kafkaMessageInRate, recordSendRate, recordSendTotal, recordErrorTotal).runEvery(10.seconds)

    //then
    whenReady(loadFinished) { _ =>
      recordErrorTotal.unsafeRunSync().value shouldBe 0
      recordSendTotal.unsafeRunSync().value shouldBe (24000L +- 1000L)
      kafkaMessageInRate.unsafeRunSync().value shouldBe 200.0 +- 20.0
    }
  }

}
