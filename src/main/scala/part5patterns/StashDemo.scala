package part5patterns

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import utils._

import scala.concurrent.duration.DurationInt

object StashDemo {

  // an actor with a locked access to a resource
  trait Command
  private case object Open extends Command
  private case object Close extends Command
  private case object Read extends Command
  private case class Write(data: String) extends Command

  private object ResourceActor {

    def apply(): Behavior[Command] = closed("42") // the resource starts as closed with some initial data
    private def closed(data: String): Behavior[Command] = Behaviors.withStash(128) { buffer =>
      Behaviors.receive { (context, message) =>
        message match {
          case Open =>
            context.log.info("Opening Resource")
            buffer.unstashAll(open(data))
          case _ =>
            context.log.info(s"Stashing $message because the resource is closed")
            buffer.stash(message) // buffer is MUTABLE
            Behaviors.same
        }
      }
    }
    private def open(data: String): Behavior[Command] = Behaviors.receive { (context, message) =>
      message match {
        case Read =>
          context.log.info(s"I have read $data")
          Behaviors.same
        case Write(newData) =>
          context.log.info(s"I have written $newData")
          open(newData)
        case Close =>
          context.log.info(s"Closing Resource")
          closed(data)
        case message =>
          context.log.info(s"$message not supported while resource is open")
          Behaviors.same
      }
    }
  }
  def main(args: Array[String]): Unit = {
    val userGuardian = Behaviors.setup[Unit] { context =>
      val resourceActor = context.spawn(ResourceActor(), "resource")

      resourceActor ! Read  // stashed
      resourceActor ! Open  // unstash the Read message after opening
      resourceActor ! Open  // unhandled
      resourceActor ! Write("I love stash") // overwrite
      resourceActor ! Read
      resourceActor ! Read
      resourceActor ! Close
      resourceActor ! Read  // stashed: resource is closed

      Behaviors.empty
    }

    ActorSystem(userGuardian, "DemoStash").withFiniteLifespan(2.seconds)
  }
}
