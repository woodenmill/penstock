package io.woodenmill.penstock.util

import cats.effect.{ContextShift, IO, Timer}

import scala.concurrent.duration.FiniteDuration
//LOOKS GOOD
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

  val printIO: String => IO[Unit] = term => IO { println(term) }
}