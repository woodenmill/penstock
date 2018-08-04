package io.woodenmill.penstock.core

import io.woodenmill.penstock.core.TestBackends.MockedBackend
import io.woodenmill.penstock.core.loadrunner.{Clock, LoadRunner, StreamingBackend}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class LoadRunnerSpec extends FlatSpec with Matchers {

  "LoadRunner" should "use streaming backend to send a message" in {
    //given
    implicit val backend: MockedBackend[String] = TestBackends.mockedBackend()
    val message = "some message"

    //when
    LoadRunner(message, 1.milli).run()

    //then
    backend.messages should contain (message)
  }

  it should "accept many messages" in {
    //given
    implicit val backend: MockedBackend[String] = TestBackends.mockedBackend()
    val messages = Seq("msg1", "msg2", "msg3")

    //when
    LoadRunner(messages, 1.milli).run()

    //then
    backend.messages should contain allElementsOf messages
  }

  it should "run as long as configured duration" in {
    //given
    val backend: StreamingBackend[String] = TestBackends.doNothing[String]()
    val fastClock: FakeClock = FakeClock(tick = 1.minute)
    val message = "some message"
    val duration: FiniteDuration = 1.hour

    //when
    LoadRunner(message, duration).run()(backend, fastClock)

    //then
    fastClock.usedFor shouldBe duration
  }

}

case class FakeClock(tick: FiniteDuration) extends Clock {
  private var time: Long = System.currentTimeMillis()
  private var usage: FiniteDuration = Duration.Zero

  override def currentTime(): Long = {
    usage = usage + tick
    time = time + tick.toMillis
    time
  }

  def usedFor(): FiniteDuration = usage - tick
}

object TestBackends {
  def doNothing[T](): StreamingBackend[T] = (_: T) => Future.successful(())

  def mockedBackend[T](): MockedBackend[T] = MockedBackend()

  case class MockedBackend[T]() extends StreamingBackend[T]{
    var messages: mutable.Buffer[T] = mutable.Buffer()

    override def send(msg: T): Future[Unit] = {
      messages += msg
      Future.successful(())
    }
  }
}
