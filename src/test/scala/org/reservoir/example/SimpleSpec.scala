package org.reservoir.example

import java.util.concurrent.TimeUnit

import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.apache.kafka.common.serialization.{ByteArraySerializer, Serializer, StringSerializer}
import org.reservoir.Metric
import org.reservoir.example.Types.{Bytes, Record}
import org.scalatest.{Assertion, FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, _}
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}


class SimpleSpec extends FlatSpec with Matchers with Materializers {

  import ProducerRecordOps._

  implicit val serverBackend: KafkaBackend = KafkaBackend()
  implicit val stringSerializer: StringSerializer = new StringSerializer()
  implicit val bytesSerializer: Serializer[Array[Byte]] = new ByteArraySerializer()

  "Sample application" should "consume messages with throughput of 1000 messages per second" in withMaterializer { implicit mat =>
    //given
    val messagesToSend = Seq(
      ("test" -> "message 1").asRecord,
      ("test" -> "message 2").asRecord
    )

    //when
    val load = LoadRunner(duration = 10.seconds, throughput = 1000, () => messagesToSend).run()

    //then
    val metrics = AppMetrics()

    Report(metrics.all).print(every = 10.second).run()

    Assertions(() => Seq(
      metrics.messagesSendRate.topValue should be > 20000.0)
    ).runWhenFinished(load)
  }
}

//core/load
case class LoadRunner[T](
                    duration: FiniteDuration,
                    throughput: Int,
                    messages: () => Seq[T]
                  )(implicit backend: StreamingBackend[T]) {

  def run()(implicit mat: Materializer): Future[Unit] = {
    runFor(duration) { () =>
      messages().foreach(msg => {
        backend.send(msg)
      })
    }

  }

  private def runFor[T](duration: FiniteDuration)(f: () => T): Future[Unit] = {
    Future {
      val startTime = System.currentTimeMillis()
      while (duration.toMillis > (System.currentTimeMillis() - startTime)) {
        f()
      }
    }
  }
}

//core/report
case class Report(metrics: Set[Metric]) {
  def print(every: FiniteDuration): Report = this

  def run()(implicit mat: Materializer) = ()
}

//core/assertions
case class Assertions(checks: () => Seq[Assertion]) {
  def runWhenFinished(future: Future[Unit]): Unit = {
    Await.ready(future, Duration.Inf)
    checks()
  }
}


case class AppMetrics()(implicit mat: Materializer) {
  val messagesSendRate: Metric = PrometheusMetric("""rate(kafka_server_brokertopicmetrics_messagesin_total{topic="test"}[1m])""")

  val all: Set[Metric] = Set(messagesSendRate)
}

//core/load
trait StreamingBackend[T] {
  def send(msg: T): Unit

  def close(): Unit
}

//backends/kafka
case class KafkaBackend() extends StreamingBackend[Record] {

  import scala.collection.JavaConverters._

  val bytesSerializer = new ByteArraySerializer

  val config: Map[String, AnyRef] = Map(
    ProducerConfig.BOOTSTRAP_SERVERS_CONFIG -> "localhost:9092"
  )

  val producer = new KafkaProducer[Bytes, Bytes](config.asJava, bytesSerializer, bytesSerializer)

  def send(msg: Record): Unit = {
    producer.send(msg)
  }

  def close(): Unit = {
    producer.close(10, TimeUnit.SECONDS)
  }
}

//metrics/prometheus
case class PrometheusMetric(query: String)(implicit mat: Materializer) extends Metric {
  override def topValue: Double = PrometheusClient.getMetric(query)
}

//metrics/prometheus
object PrometheusClient {

  import com.softwaremill.sttp._
  import org.json4s._
  import org.json4s.native.JsonMethods._

  val promApi = uri"http://localhost:9090/api/v1/query"

  implicit val backend = TryHttpURLConnectionBackend()

  def getMetric(query: String): Double = {
    val request = sttp.get(promApi.param("query", query))
    val tryResponse: Try[Response[String]] = request.send()
    tryResponse match {
      case Failure(e) => throw new RuntimeException(e)
      case Success(Response(body: Right[_, String], _, _, _, _)) =>
        val bodyString = body.value
        val bodyJson: JValue = parse(bodyString)

        val metric = ((bodyJson \ "data" \ "result") (0) \ "value") (1)

        val finalValue = metric match {
          case JString(value) => value
          case _ => throw new RuntimeException("wtf?!")
        }

        finalValue.toDouble
    }
  }


}


//core
trait Materializers {
  def withMaterializer(f: Materializer => Unit): Unit = {
    val mat = new Materializer {}
    try {
      f(mat)
    }
    finally {
      mat.close()
    }
  }
}

//core
trait Materializer { //useful wrapper for actor system. every test has a separate actor system
  def init(): Unit = ()

  def close(): Unit = ()
}

//backends/kafka
object Types {
  type Bytes = Array[Byte]
  type Record = ProducerRecord[Bytes, Bytes]
}

//backends/kafka
object ProducerRecordOps {
  implicit class FromTupleConverter[A](t: (String, A)) {
    def asRecord(implicit valueSerializer: Serializer[A]): Record = {
      new Record(t._1, valueSerializer.serialize(t._1, t._2))
    }
  }
}