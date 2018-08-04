package io.woodenmill.penstock.core.loadrunner

trait Clock {
  def currentTime(): Long
}

case class SystemClock() extends Clock {
  override def currentTime(): Long = System.currentTimeMillis()
}