package io.woodenmill.penstock.util

import java.util.concurrent.TimeUnit.MILLISECONDS

import scala.concurrent.duration.FiniteDuration

object Mesurements {

  val Zero = FiniteDuration(0, MILLISECONDS)

  def executionTime[A](block: => A): (FiniteDuration, A) = {
    val t0 = System.currentTimeMillis()
    val result = block
    val t1 = System.currentTimeMillis()
    val elapsedTime = FiniteDuration(t1-t0, MILLISECONDS)
    (elapsedTime, result)
  }
}
