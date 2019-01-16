package io.woodenmill.penstock.metrics.prometheus

import java.net.URL

import scala.concurrent.duration.{FiniteDuration, _}

case class PrometheusConfig(prometheusUrl: URL, connectionTimeout: FiniteDuration = 3.seconds, socketTimeout: FiniteDuration = 1.second)
