package part3testing

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt

class EssentialTestingSpec extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  import EssentialTestingSpec._

  "A simple actor" should {
    // test suite

    "send back a duplicated message" in {
      // code for testing
      val simpleActor = testKit.spawn(SimpleActor(), "SimpleActor")  // actor under test
      val probe = testKit.createTestProbe[SimpleProtocol]()  // "inspector"

      simpleActor ! SimpleMessage("Akka", probe.ref)

      // assertions
      probe.expectMessage(SimpleReply("AkkaAkka"))
    }
  }

  "A black hole actor" should {
    "not reply back" in {
      val blackHole = testKit.spawn(BlackHole(), "BlackHole")
      val probe = testKit.createTestProbe[SimpleProtocol]()

      blackHole ! SimpleMessage("Black hole, nothing", probe.ref)
      blackHole ! SimpleMessage("I love Akka", probe.ref)

      probe.expectNoMessage(1.second)
    }
  }

  "A simple actor with a separate test suite" should {
    val simpleActor = testKit.spawn(SimpleActor(), "SimpleActor")
    val probe = testKit.createTestProbe[SimpleProtocol]()

    "uppercase a string" in {
      simpleActor ! UppercaseString("Akka", probe.ref)
      val receiveMessage = probe.expectMessageType[SimpleReply]
      // other assertions
      assert(receiveMessage.contents == receiveMessage.contents.toUpperCase())  // Scala standard assertion
      receiveMessage.contents should be("AKKA") // Scalatest library assertion
    }

    "reply with favorite tech as multiple message" in {
      simpleActor ! FavoriteTech(probe.ref)
      // fetch multiple message
      val replies: Seq[SimpleProtocol] = probe.receiveMessages(2, 1.second)
      val repliesContents: Seq[String] = replies.collect {
        case SimpleReply(contents) => contents
      }

      // assertion
      repliesContents should contain allOf("Scala", "Akka")
    }
  }
}

object EssentialTestingSpec {
  // code under test
  trait SimpleProtocol
  case class SimpleMessage(message: String, replyTo: ActorRef[SimpleProtocol]) extends SimpleProtocol
  case class UppercaseString(message: String, replyTo: ActorRef[SimpleProtocol]) extends SimpleProtocol
  case class FavoriteTech(replyTo: ActorRef[SimpleProtocol]) extends SimpleProtocol
  case class SimpleReply(contents: String) extends SimpleProtocol

  object SimpleActor {
    def apply(): Behavior[SimpleProtocol] = Behaviors.receiveMessage {
      case SimpleMessage(msg, replyTo) =>
        replyTo ! SimpleReply(msg + msg)
        Behaviors.same
      case UppercaseString(msg, replyTo) =>
        replyTo ! SimpleReply(msg.toUpperCase)
        Behaviors.same
      case FavoriteTech(replyTo) =>
        replyTo ! SimpleReply("Scala")
        replyTo ! SimpleReply("Akka")
        Behaviors.same
    }
  }

  object BlackHole {
    def apply(): Behavior[SimpleProtocol] = Behaviors.ignore
  }
}
