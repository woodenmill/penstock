package io.woodenmill.penstock.metrics.prometheus

import akka.actor.ActorSystem
import io.woodenmill.penstock.Metrics.Counter
import io.woodenmill.penstock.metrics.prometheus.Prometheus.{PromQl, PrometheusConfig}
import io.woodenmill.penstock.testutils.PromResponses.valid
import io.woodenmill.penstock.testutils.PrometheusIntegratedSpec
import org.scalatest.concurrent.Eventually
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

class PrometheusMetricSpec extends FlatSpec with Matchers with PrometheusIntegratedSpec with Eventually {

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(timeout = 2.seconds)

  val actorSystem = ActorSystem()
  val promConfig = PrometheusConfig(prometheusUri)

  "PrometheusMetric" should "fetch the value from Prometheus" in {
    //given
    configurePromStub("up", valid("7"))

    //when
    val prometheusMetric = PrometheusMetric[Counter](PromQl("up").get)(promConfig, actorSystem)

    //then
    prometheusMetric.value shouldBe 7
  }

  it should "fetch a metric periodically" in {
    //given
    configurePromStub("sum(up)", valid("1"), valid("2"), valid("3"))

    //when
    val prometheusMetric = PrometheusMetric[Counter](PromQl("sum(up)").get)(promConfig, actorSystem)

    //then
    eventually {
      prometheusMetric.value() shouldBe 3
    }
  }
}
