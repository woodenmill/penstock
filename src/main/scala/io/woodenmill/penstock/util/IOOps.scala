package io.woodenmill.penstock.util

import cats.effect.{ContextShift, IO, Timer}
import cats.implicits._

import scala.concurrent.duration.FiniteDuration

object IOOps {

  implicit class IOExtras(io: IO[_]) {
    def repeatEndlessly(delay: FiniteDuration)(implicit t: Timer[IO], cs: ContextShift[IO]): IO[Unit] = {
      val sleep = IO.sleep(delay)
      for {
        _ <- io
        _ <- IO.shift
        _ <- sleep
        _ <- repeatEndlessly(delay)
      } yield ()
    }
  }

  val printIO: String => IO[Unit] = term => IO {
    println(term)
  }


  def parallelAttempt[T](ios: List[IO[T]])(implicit cs: ContextShift[IO]): IO[(List[Throwable], List[T])] = {
    ios
      .map(io => io.attempt)
      .parSequence
      .map(errorsAndSuccesses => errorsAndSuccesses.separate)
  }
}