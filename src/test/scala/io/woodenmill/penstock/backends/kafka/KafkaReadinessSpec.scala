package io.woodenmill.penstock.backends.kafka

import io.woodenmill.penstock.testutils.Spec

import scala.concurrent.duration._

class KafkaReadinessSpec extends Spec {

  "Kafka readiness" should "be true if there is one broker in a cluster" in {
    val stubbedBrokersFetcher = () => Some(1)

    val isReady = KafkaReadiness.check(stubbedBrokersFetcher, 1.millisecond)

    isReady shouldBe true
  }

  it should "be true if there is more than one broker in a cluster" in {
    val stubbedBrokersFetcher = () => Some(11)

    val isReady = KafkaReadiness.check(stubbedBrokersFetcher, 1.millisecond)

    isReady shouldBe true
  }

  it should "be false if there is no broker in a cluster" in {
    val stubbedBrokersFetcher = () => Some(0)

    val isReady = KafkaReadiness.check(stubbedBrokersFetcher, 1.millisecond)

    isReady shouldBe false
  }

  it should "be false if it cannot connect to cluster" in {
    val stubbedBrokersFetcher = () => None

    val isReady = KafkaReadiness.check(stubbedBrokersFetcher, 1.millisecond)

    isReady shouldBe false
  }
}
