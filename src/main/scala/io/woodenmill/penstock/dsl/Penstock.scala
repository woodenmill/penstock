package io.woodenmill.penstock.dsl

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cats.effect.{ContextShift, IO}
import cats.implicits._
import io.woodenmill.penstock.Metrics.MetricName
import io.woodenmill.penstock.backends.StreamingBackend
import io.woodenmill.penstock.dsl.Penstock.FailedAssertion
import io.woodenmill.penstock.{LoadGenerator, Metric}

import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Try}

object Penstock {
  def load[T](backend: StreamingBackend[T], messageGen: () => T, duration: FiniteDuration, throughput: Int): Penstock[T] = {
    new Penstock[T](backend, () => List(messageGen()), duration, throughput)
  }

  def load[T](backend: StreamingBackend[T], messageGen: () => List[T], duration: FiniteDuration, throughput: Int)(implicit d: DummyImplicit): Penstock[T] = {
    new Penstock[T](backend, messageGen, duration, throughput)
  }

  class FailedAssertion(msg: String) extends Exception(msg)

}

class Penstock[T] private(
                           backend: StreamingBackend[T],
                           messageGenerator: () => List[T],
                           duration: FiniteDuration,
                           throughput: Int,
                           assertions: List[IO[(MetricName, Try[Unit])]] = List()
                         ) {
  def run(): Unit = {
    val system: ActorSystem = ActorSystem()
    implicit val mat: ActorMaterializer = ActorMaterializer()(system)
    implicit val cs: ContextShift[IO] = IO.contextShift(system.dispatcher)

    LoadGenerator(backend).generate(messageGenerator, duration, throughput)
      .flatMap(_ => collectFailed(assertions))
      .flatMap(raiseErrorIfAnyFailed)
      .unsafeRunSync()
  }

  def metricAssertion[V](metric: IO[Metric[V]])(assertion: V => Any): Penstock[T] = {

    val newAssertion: IO[(MetricName, Try[Unit])] = metric.map(m => (m.name, Try(assertion(m.value))))
    this.copy(assertions = newAssertion :: assertions)
  }

  private def collectFailed(assertions: List[IO[(MetricName, Try[Unit])]])(implicit cs: ContextShift[IO]): IO[List[Throwable]] = {
    assertions
      .map(_.attempt)
      .parSequence
      .map(_.collect {
        case Left(e) => e
        case Right((metricName, Failure(e))) => new FailedAssertion(s"$metricName: ${e.getMessage}")
      })
  }

  private def raiseErrorIfAnyFailed(ts: List[Throwable]): IO[Unit] = ts match {
    case Nil => IO.unit
    case errors =>
      val msg = errors.map(_.getMessage).mkString("\n")
      IO.raiseError(new FailedAssertion(msg))
  }

  private def copy(backend: StreamingBackend[T] = this.backend,
                   messageGenerator: () => List[T] = this.messageGenerator,
                   duration: FiniteDuration = this.duration,
                   throughput: Int = this.throughput,
                   assertions: List[IO[(MetricName, Try[Unit])]] = this.assertions): Penstock[T] = {
    new Penstock[T](backend, messageGenerator, duration, throughput, assertions)
  }
}
