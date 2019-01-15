package io.woodenmill.penstock.examples

import java.net.URI
import java.util.UUID

import cats.effect.IO
import io.woodenmill.penstock.Metrics.{Counter, Gauge}
import io.woodenmill.penstock.backends.kafka._
import io.woodenmill.penstock.dsl.Penstock
import io.woodenmill.penstock.metrics.prometheus.PrometheusMetric._
import io.woodenmill.penstock.metrics.prometheus.{PromQl, PrometheusConfig, PrometheusMetric}
import org.apache.kafka.common.serialization.{Serializer, StringSerializer}
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

class GettingStartedSpec extends FlatSpec with Matchers {

  val kafkaBackend: KafkaBackend = KafkaBackend(bootstrapServers = "localhost:9092")
  implicit val stringSerializer: Serializer[String] = new StringSerializer()
  val messageGen = () => createProducerRecord("input", s"test message, ID: ${UUID.randomUUID()}")

  implicit val promConfig: PrometheusConfig = PrometheusConfig(new URI("localhost:9090"))
  val q = PromQl("""kafka_server_BrokerTopicMetrics_OneMinuteRate{name="MessagesInPerSec",topic="input"}""")
  val kafkaMessageInRateIO: IO[Gauge] = PrometheusMetric[Gauge](metricName = "kafka-messages-in-rate", query = q)
  val recordErrorTotalIO: IO[Counter] = kafkaBackend.metrics.recordErrorTotal
  val recordSendTotalIO: IO[Counter] = kafkaBackend.metrics.recordSendTotal

  "GettingStarted example" should "send messages to Kafka and use custom Prometheus metric to verify behaviour" in {
    Penstock
      .load(kafkaBackend, messageGen, duration = 2.minutes, throughput = 200)
      .metricAssertion(recordErrorTotalIO)(_ shouldBe 0)
      .metricAssertion(recordSendTotalIO)(_ shouldBe (24000L +- 1000L))
      .metricAssertion(kafkaMessageInRateIO)(_ shouldBe 200.0 +- 20.0)
      .run()
  }
}