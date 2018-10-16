package io.woodenmill.penstock.testutils

import io.woodenmill.penstock.backends.StreamingBackend

import scala.collection.mutable
import scala.concurrent.Future


object TestBackends {
  def doNothing[T](): StreamingBackend[T] = new StreamingBackend[T] {
    override def send(msg: T): Unit = Future.successful(())
    override def isReady: Boolean = true
  }

  def mockedBackend[T](isReady: Boolean = true): MockedBackend[T] = MockedBackend(isReady)

  case class MockedBackend[T](isReady: Boolean) extends StreamingBackend[T] {
    var messages: mutable.Buffer[T] = mutable.Buffer()
    override def send(msg: T): Unit = messages += msg
  }

}
