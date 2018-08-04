package io.woodenmill.penstock.core.loadrunner

import scala.concurrent.Future

trait StreamingBackend[T] {
  def send(msg: T): Future[Unit]
}
