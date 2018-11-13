package io.woodenmill.penstock

import akka.Done
import akka.stream._
import akka.stream.scaladsl._
import io.woodenmill.penstock.backends.StreamingBackend

import scala.concurrent._
import scala.concurrent.duration._


case class LoadRunner[T](backend: StreamingBackend[T]) {

  def start(toSend: () => T, duration: FiniteDuration, throughput: Int)(implicit mat: ActorMaterializer): Future[Done] = {
    start(() => List(toSend()) , duration, throughput)
  }

  def start(toSend: () => List[T], duration: FiniteDuration, throughput: Int)(implicit mat: ActorMaterializer, d: DummyImplicit): Future[Done] = {

    if(backend.isReady) {
      val (killSwitch, loadRunnerFinishedFuture) = Source.cycle[T](() => toSend().iterator)
        .throttle(throughput, per = 1.second)
        .viaMat(KillSwitches.single)(Keep.right)
        .toMat(Sink.foreach(backend.send))(Keep.both)
        .run()(mat)

      mat.system.scheduler.scheduleOnce(duration)(killSwitch.shutdown())(mat.executionContext)

      loadRunnerFinishedFuture
    } else {
      Future.failed(new IllegalStateException("Streaming Backend is not ready. LoadRunner will not start"))
    }
  }

}
