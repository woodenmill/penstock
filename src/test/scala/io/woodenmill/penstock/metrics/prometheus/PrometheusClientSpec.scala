package io.woodenmill.penstock.metrics.prometheus

import com.softwaremill.sttp._
import io.woodenmill.penstock.metrics.prometheus.Prometheus.{PromQl, PrometheusConfig}
import io.woodenmill.penstock.testutils.{PromResponses, PrometheusIntegratedSpec}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._


class PrometheusClientSpec extends FlatSpec with ScalaFutures with PrometheusIntegratedSpec with Matchers {
  implicit override val patienceConfig: PatienceConfig = PatienceConfig(timeout = 2.seconds)

  "Prometheus client" should "fetch metric value from Prometheus" in {
    //given
    val query = "up"
    configurePromStub(query, PromResponses.valid("5"))

    //when
    val metricFuture = PrometheusClient(PrometheusConfig(uri"localhost:$promPort")).fetch(PromQl(query).get)

    //then
    whenReady(metricFuture) { metric =>
      metric.value shouldBe 5
    }
  }

}

