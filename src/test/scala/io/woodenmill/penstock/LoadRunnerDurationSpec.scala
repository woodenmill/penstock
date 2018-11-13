package io.woodenmill.penstock

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.ExplicitlyTriggeredScheduler
import com.typesafe.config.{Config, ConfigFactory}
import io.woodenmill.penstock.backends.StreamingBackend
import io.woodenmill.penstock.testutils.{Spec, TestBackends}
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}

class LoadRunnerDurationSpec extends Spec with BeforeAndAfterAll {

  val config: Config = ConfigFactory.parseString("""akka.scheduler.implementation = "akka.testkit.ExplicitlyTriggeredScheduler"""")
  val system = ActorSystem("manualtime", config)
  val manualTime = system.scheduler.asInstanceOf[ExplicitlyTriggeredScheduler]
  val mat: ActorMaterializer = ActorMaterializer()(system)
  val backend: StreamingBackend[String] = TestBackends.doNothing[String]()


  "Load Runner" should "run as long as configured duration" in {
    val duration: FiniteDuration = 1.hour

    val loadFinished = LoadRunner(backend).start(() => "some message", duration, throughput = 100)(mat)

    loadFinished.isCompleted shouldBe false
    manualTime.timePasses(1.hour)

    eventually {
      loadFinished.isCompleted shouldBe true
    }
  }

  override protected def afterAll(): Unit = Await.ready(system.terminate(), atMost = 5.seconds)
}
