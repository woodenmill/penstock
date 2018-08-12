package io.woodenmill.penstock.testutils

import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{FlatSpec, Matchers, TryValues}

import scala.concurrent.duration._

trait Spec extends FlatSpec with Matchers with ScalaFutures with Eventually with TryValues {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 2.seconds)

}
