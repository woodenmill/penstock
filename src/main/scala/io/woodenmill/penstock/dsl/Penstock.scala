package io.woodenmill.penstock.dsl

import akka.stream.ActorMaterializer
import cats.Parallel
import cats.effect.IO
import io.woodenmill.penstock.{LoadGenerator, Metric}
import io.woodenmill.penstock.backends.StreamingBackend

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.FiniteDuration

object Penstock {
  def load[T](backend: StreamingBackend[T], messageGen: () => T, duration: FiniteDuration, throughput: Int): Penstock[T] = {
    new Penstock[T](backend, () => List(messageGen()), duration, throughput)
  }
  def load[T](backend: StreamingBackend[T], messageGen: () => List[T], duration: FiniteDuration, throughput: Int)(implicit d: DummyImplicit): Penstock[T] = {
    new Penstock[T](backend, messageGen, duration, throughput)
  }

}

case class Penstock[T](
                        backend: StreamingBackend[T],
                        messageGenerator: () => List[T],
                        duration: FiniteDuration,
                        throughput: Int,
                        assertions: List[IO[Any]] = List()
                      ) {

  def run()(implicit mat: ActorMaterializer): Unit = {
    import cats.effect.{ContextShift, IO}
    import cats.implicits._
    implicit val ec: ExecutionContext = mat.system.dispatcher
    implicit val cs: ContextShift[IO] = IO.contextShift(ec)

    val scenario = for {
      _ <- LoadGenerator(backend).generate(messageGenerator, duration, throughput)
      _ <- Parallel.parSequence(assertions)
    } yield ()

    scenario.unsafeRunSync()
  }

  def metricAssertion[V](metric: IO[Metric[V]])(f: V => Any): Penstock[T] = {
    val assertion = metric.map(m => f(m.value))
    this.copy(assertions = assertion :: assertions)
  }
}
