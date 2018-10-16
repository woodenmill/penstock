package io.woodenmill.penstock.backends

trait StreamingBackend[T] {
  def send(msg: T): Unit
  def isReady: Boolean
}
