package io.woodenmill.penstock.testutils

import io.woodenmill.penstock.backends.StreamingBackend

import scala.collection.mutable
import scala.concurrent.Future


object TestBackends {
  def doNothing[T](): StreamingBackend[T] = (_: T) => Future.successful(())

  def mockedBackend[T](): MockedBackend[T] = MockedBackend()

  case class MockedBackend[T]() extends StreamingBackend[T] {
    var messages: mutable.Buffer[T] = mutable.Buffer()

    override def send(msg: T): Unit = messages += msg
  }

}
