package io.woodenmill.penstock.core.loadrunner

import scala.concurrent.duration.FiniteDuration

object LoadRunner {
  def apply[T](toSend: T, duration: FiniteDuration): LoadRunner[T] = LoadRunner(Seq(toSend), duration)
}

case class LoadRunner[T](toSend: Seq[T], duration: FiniteDuration) {

  def run()(implicit backend: StreamingBackend[T], clock: Clock = SystemClock()): Unit = {
    runFor(duration, clock) { () =>
      toSend.foreach(backend.send)
    }
  }

  private def runFor(duration: FiniteDuration, clock: Clock)(f: () => Unit): Unit = {
    val startTime = clock.currentTime()
    while (duration.toMillis > (clock.currentTime() - startTime)) {
      f()
    }
  }

}
