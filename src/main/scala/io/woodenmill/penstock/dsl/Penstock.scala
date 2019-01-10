package io.woodenmill.penstock.dsl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.{ContextShift, IO}
import cats.implicits._
import io.woodenmill.penstock.backends.StreamingBackend
import io.woodenmill.penstock.dsl.Penstock.PenstockFailedAssertion
import io.woodenmill.penstock.{LoadGenerator, Metric}
import scala.concurrent.duration.FiniteDuration

object Penstock {
  def load[T](backend: StreamingBackend[T], messageGen: () => T, duration: FiniteDuration, throughput: Int): Penstock[T] = {
    new Penstock[T](backend, () => List(messageGen()), duration, throughput)
  }

  def load[T](backend: StreamingBackend[T], messageGen: () => List[T], duration: FiniteDuration, throughput: Int)(implicit d: DummyImplicit): Penstock[T] = {
    new Penstock[T](backend, messageGen, duration, throughput)
  }

  class PenstockFailedAssertion(msg: String) extends Exception(msg)

}

class Penstock[T] private(
                           backend: StreamingBackend[T],
                           messageGenerator: () => List[T],
                           duration: FiniteDuration,
                           throughput: Int,
                           assertions: List[IO[Any]] = List()
                         ) {
  def run(): Unit = {
    val system: ActorSystem = ActorSystem()
    implicit val mat: ActorMaterializer = ActorMaterializer()(system)
    implicit val cs: ContextShift[IO] = IO.contextShift(system.dispatcher)

    val assertionsIO = assertions
      .map(_.attempt)
      .parSequence
      .map(_.collect { case Left(e) => e })
      .flatMap {
        case Nil => IO.unit
        case errors => IO.raiseError(new PenstockFailedAssertion(errors.map(_.getMessage).mkString(", ")))
      }

    val scenario = for {
      _ <- LoadGenerator(backend).generate(messageGenerator, duration, throughput)
      _ <- assertionsIO
    } yield ()

    scenario.unsafeRunSync()
  }

  def metricAssertion[V](metric: IO[Metric[V]])(f: V => Any): Penstock[T] = {
    val assertion = metric.map(m => f(m.value))
    this.copy(assertions = assertion :: assertions)
  }

  private def copy(backend: StreamingBackend[T] = this.backend,
                   messageGenerator: () => List[T] = this.messageGenerator,
                   duration: FiniteDuration = this.duration,
                   throughput: Int = this.throughput,
                   assertions: List[IO[Any]] = this.assertions): Penstock[T] = {
    new Penstock[T](backend, messageGenerator, duration, throughput, assertions)
  }
}
