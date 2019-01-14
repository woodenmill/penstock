package io.woodenmill.penstock.util

import cats.effect.IO
import cats.effect.laws.util.TestContext
import io.woodenmill.penstock.testutils.Spec
import io.woodenmill.penstock.util.IOOps._

import scala.concurrent.duration._

class IOOpsSpec extends Spec {

  "repeatEndlessly function" should "repeat given IO" in {
    var counter = 0
    val increase = IO{ counter = counter + 1 }

    increase.repeatEndlessly(0.milli).unsafeToFuture()

    eventually(counter should be > 100)
    eventually(counter should be > 1000)
  }

  it should "repeat execution in given intervals" in {
    val tc = TestContext()
    var counter = 0
    val increase = IO{ counter = counter + 1 }

    increase.repeatEndlessly(10.seconds)(tc.timer, tc.contextShift).unsafeToFuture()

    counter shouldBe 1
    tc.tick(9.seconds)
    counter shouldBe 1
    tc.tick(2.second)
    counter shouldBe 2
    tc.tick(10.seconds)
    counter shouldBe 3
  }

  it should "not blow up the Stack" in {
    val program = IO.race(
      IO.unit.repeatEndlessly(delay = 0.millis),
      IO.sleep(1.second)
    )

    noException should be thrownBy program.unsafeRunSync()
  }

}
