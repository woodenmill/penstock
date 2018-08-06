package io.woodenmill.penstock

import akka.Done
import akka.stream._
import akka.stream.scaladsl._
import io.woodenmill.penstock.backends.StreamingBackend

import scala.concurrent._
import scala.concurrent.duration._


object LoadRunner {
  def apply[T](toSend: T, duration: FiniteDuration, throughput: Int): LoadRunner[T] = LoadRunner(Seq(toSend), duration, throughput)
}

case class LoadRunner[T](toSend: Seq[T], duration: FiniteDuration, throughput: Int) {

  def run()(implicit backend: StreamingBackend[T], mat: ActorMaterializer): Future[Done] = {

    val (killSwitch, loadRunnerFinishedFuture) = Source.cycle[T](() => toSend.iterator)
      .throttle(throughput, per = 1.second)
      .viaMat(KillSwitches.single)(Keep.right)
      .toMat(Sink.foreach(backend.send))(Keep.both)
      .run()(mat)

    mat.system.scheduler.scheduleOnce(duration)(killSwitch.shutdown())(mat.executionContext)

    loadRunnerFinishedFuture
  }

}
