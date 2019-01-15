package io.woodenmill.penstock.backends

//TODO: send should return IO[Unit]
//TODO: is ready should return IO[Boolean]
//TODO: a new method: close/shutdown is needed
trait StreamingBackend[T] {
  def send(msg: T): Unit
  def isReady: Boolean
}
