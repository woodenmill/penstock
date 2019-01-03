package io.woodenmill.penstock.dsl

import akka.stream.ActorMaterializer
import cats.effect.IO
import io.woodenmill.penstock.LoadGenerator
import io.woodenmill.penstock.backends.StreamingBackend

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
                        throughput: Int
                      ) {

  def run()(implicit mat: ActorMaterializer): Unit = {
    val load: IO[Unit] = LoadGenerator(backend).generate(messageGenerator, duration, throughput)
    load.unsafeRunSync()
  }
}
