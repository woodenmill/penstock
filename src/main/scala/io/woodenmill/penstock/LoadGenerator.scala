package io.woodenmill.penstock

import akka.Done
import akka.actor.Scheduler
import akka.stream._
import akka.stream.scaladsl._
import cats.effect.IO
import io.woodenmill.penstock.backends.StreamingBackend

import scala.concurrent.Future
import scala.concurrent.duration._


//Looks good
case class LoadGenerator[T](backend: StreamingBackend[T]) {

  def generate(toSend: () => T, duration: FiniteDuration, throughput: Int)(implicit mat: ActorMaterializer): IO[Unit] = {
    generate(() => List(toSend()), duration, throughput)
  }

  def generate(toSend: () => List[T], duration: FiniteDuration, throughput: Int)(implicit mat: ActorMaterializer, d: DummyImplicit): IO[Unit] = {
    def raiseErrorWhenNotReady(backend: StreamingBackend[T]): IO[Unit] = IO(backend.isReady).flatMap {
      case false => IO.raiseError(new IllegalStateException("Streaming Backend is not ready. LoadRunner will not start"))
      case true => IO.unit
    }

    def buildStoppableStream(backend: StreamingBackend[T], toSend: () => List[T], throughput: Int, stopAfter: FiniteDuration)(scheduler: Scheduler): RunnableGraph[Future[Done]] = {
      Source.cycle[T](() => toSend().iterator)
        .throttle(throughput, per = 1.second)
        .viaMat(KillSwitches.single)(Keep.right)
        .toMat(Sink.foreach(backend.send))(Keep.both)
        .mapMaterializedValue { case (killSwitch, streamFinished) =>
          scheduler.scheduleOnce(stopAfter)(killSwitch.shutdown())(mat.executionContext)
          streamFinished
        }
    }

    for {
      _ <- raiseErrorWhenNotReady(backend)
      stream = buildStoppableStream(backend, toSend, throughput, duration)(mat.system.scheduler)
      _ <- IO.fromFuture(IO(stream.run()))
    } yield ()
  }

}
