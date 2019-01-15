package io.woodenmill.penstock.testutils

import java.io.ByteArrayOutputStream

import cats.effect.{ContextShift, IO, Timer}
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import org.scalatest.{FlatSpec, Matchers, TryValues}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait Spec extends FlatSpec with Matchers with ScalaFutures with Eventually with TryValues {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(timeout = 2.seconds)

  implicit val contextShift: ContextShift[IO] = IO.contextShift(global)
  implicit val timer: Timer[IO] = IO.timer(global)

  def catchConsoleOutput(thunk: => Unit): String = {
    val outCapture = new ByteArrayOutputStream
    Console.withOut(outCapture) {
      thunk
    }
    outCapture.toString
  }
}
