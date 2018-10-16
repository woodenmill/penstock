package io.woodenmill.penstock.backends.kafka

import java.util.Properties

import io.woodenmill.penstock.util.Mesurements._
import org.apache.kafka.clients.admin.{AdminClient, AdminClientConfig, DescribeClusterOptions}

import scala.annotation.tailrec
import scala.concurrent.duration._
import scala.util.Try


private[kafka] object KafkaReadiness {
  type NumberOfBrokers = Int
  private val options = new DescribeClusterOptions().timeoutMs(1000)

  def isReady(bootstrapServers: String, timeout: FiniteDuration = 5.second): Boolean = {
    val properties = new Properties { put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers) }
    val adminClient = AdminClient.create(properties)
    val fetchNumberOfBrokers = () => Try( adminClient.describeCluster(options).nodes().get().size()).toOption

    check(fetchNumberOfBrokers, timeout)
  }

  @tailrec
  def check(fetchNumberOfBrokers: () => Option[NumberOfBrokers], timeout: FiniteDuration): Boolean = {
    val (queryTime, maybeNumberOfBrokers) = executionTime { fetchNumberOfBrokers() }

    maybeNumberOfBrokers match {
      case Some(numberOfBrokers) if numberOfBrokers >= 1 => true
      case _ if timeout - queryTime <= Zero => false
      case _ => check(fetchNumberOfBrokers, timeout - queryTime)
    }
  }

}
