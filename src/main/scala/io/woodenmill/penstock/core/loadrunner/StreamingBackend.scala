package io.woodenmill.penstock.core.loadrunner

trait StreamingBackend[T] {
  def send(msg: T): Unit
}
