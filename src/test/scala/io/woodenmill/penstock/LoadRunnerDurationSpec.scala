package io.woodenmill.penstock

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.ExplicitlyTriggeredScheduler
import com.typesafe.config.{Config, ConfigFactory}
import io.woodenmill.penstock.backends.{StreamingBackend, TestBackends}
import org.scalatest.concurrent.Eventually
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

import scala.concurrent.Await
import scala.concurrent.duration.{FiniteDuration, _}

class LoadRunnerDurationSpec extends FlatSpec with Matchers with Eventually with BeforeAndAfterAll {

  val config: Config = ConfigFactory.parseString("""akka.scheduler.implementation = "akka.testkit.ExplicitlyTriggeredScheduler"""")
  val system = ActorSystem("manualtime", config)
  val manualTime = system.scheduler.asInstanceOf[ExplicitlyTriggeredScheduler]
  val materializer: ActorMaterializer = ActorMaterializer()(system)
  val backend: StreamingBackend[String] = TestBackends.doNothing[String]()


  "Load Runner" should "run as long as configured duration" in {
    //given
    val duration: FiniteDuration = 1.hour

    //when
    val loadFinished = LoadRunner("some message", duration, throughput = 100).run()(backend, materializer)

    //then
    loadFinished.isCompleted shouldBe false
    manualTime.timePasses(1.hour)

    //and
    eventually {
      loadFinished.isCompleted shouldBe true
    }
  }

  override protected def afterAll(): Unit = Await.ready(system.terminate(), atMost = 5.seconds)
}
