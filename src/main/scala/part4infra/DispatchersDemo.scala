package part4infra

import akka.actor.typed.{ActorSystem, Behavior, DispatcherSelector}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory
import utils._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

object DispatchersDemo {

  // Dispatchers are in charge of delivering and handling messages within an actor system

  def demoDispatcherConfig(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val childActorDispatcherDefault = context.spawn(LoggerActor[String](), "childDefault", DispatcherSelector.default())
      val childActorBlocking = context.spawn(LoggerActor[String](), "childBlocking", DispatcherSelector.blocking())
      val childActorInherit = context.spawn(LoggerActor[String](), "childInherit", DispatcherSelector.sameAsParent())
      val childActorConfig = context.spawn(LoggerActor[String](), "childConfig", DispatcherSelector.fromConfig("my-dispatcher"))

      val actors = (1 to 10).map(i => context.spawn(LoggerActor[String](), s"child$i", DispatcherSelector.fromConfig("my-dispatcher")))
      val r = new Random()
      (1 to 1000).foreach(i => actors(r.nextInt(10)) ! s"task$i")

      Behaviors.empty
    }

    ActorSystem(userGuardian, "DemoDispatcher").withFiniteLifespan(2.seconds)
  }

  object DBActor {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      import context.executionContext // this actor's dispatcher
      Future {
        Thread.sleep(1000)
        println(s"Query successful: $message")
      }

      Behaviors.same
    }

  }

  def demoBlockingCalls(): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val loggerActor = context.spawn(LoggerActor[String](), "logger")
      val dbActor = context.spawn(DBActor(), "db", DispatcherSelector.fromConfig("dedicated-blocking-dispatcher"))

      (1 to 100).foreach { i =>
        val message = s"query ${i}"
        dbActor ! message
        loggerActor ! message
      }

      Behaviors.same
    }

    val system = ActorSystem(userGuardian, "DemoBlockingCalls", ConfigFactory.load.getConfig("dispatcher-demo"))
  }

  def main(args: Array[String]): Unit = {
    // demoDispatcherConfig()
    demoBlockingCalls()
  }
}
