package part2actors

import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

object ActorsIntro {

  // part 1: behavior
  private val simpleActorBehavior: Behavior[String] = Behaviors.receiveMessage { (message: String) =>
    // do something with the message
    println(s"[simple actor] I have received: $message")

    // new behavior for the NEXT message
    Behaviors.same
  }

  private def demoSimpleActor(): Unit = {
    // part 2: instantiate
    val actorSystem = ActorSystem(SimpleActor_V3(), "FirstActorSystem")

    // part 3: communicate!
    // ! = the "tell" method
    actorSystem ! "I am learning Akka"  // asynchronously send a message

    // part 4: gracefully shut down
    Thread.sleep(1000)
    actorSystem.terminate()
  }

  // refactor
  object SimpleActor {
    def apply(): Behavior[String] = Behaviors.receiveMessage { (message: String) =>
      // do something with the message
      println(s"[simple actor] I have received: $message")

      // new behavior for the NEXT message
      Behaviors.same
    }
  }

  object SimpleActor_V2 {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      // context is a data structure (ActorContext) with access to a variety of APIs
      // simple example: logging
      context.log.info(s"[simple actor] I have received: $message")

      Behaviors.same
    }
  }

  private object SimpleActor_V3 {
    def apply(): Behavior[String] = Behaviors.setup { context =>
      // actor "private" data and methods, behaviors etc
      // YOUR CODE HERE

      // behavior used the FIRST message
      Behaviors.receiveMessage { message =>
        context.log.info(s"[simple actor] I have received: $message")
        Behaviors.same
      }
    }
  }

  /**
   * Exercises
   * 1. Define two "person" actor behaviors, which receive Strings
   *  - "happy", which logs your message, e.g. "I've received ____. That's great!"
   *  - "sad", .... "That sucks."
   *  Test both
   *
   *  2. Change the actor behavior:
   *   - the happy behavior will turn to sad() if it receives "Akka is bad."
   *   - the sad behavior will turn to happy() if it receives "Akka is awesome!"
   *
   *  3. Inspect my code and try to make it better.
   */

  object Person {
    def happy(): Behavior[String]= Behaviors.receive { (context, message) =>
      message match {
        case "Akka is bad." =>
          context.log.info("Don't you say anything bad about Akka!!！")
          sad()
        case _ =>
          context.log.info(s"I've received '$message'. That's great!")
          Behaviors.same
      }
    }

    def sad(): Behavior[String] = Behaviors.receive { (context, message) =>
      message match {
        case "Akka is awesome!" =>
          context.log.info("Happy now!")
          happy()
        case _ =>
          context.log.info(s"I've received '$message'. That's sucks.")
          Behaviors.same
      }
    }

    def apply(): Behavior[String] = happy()
  }

  def testPerson(): Unit = {
    val person = ActorSystem(Person(), "PersonTest")

    person ! "I love the color blue."
    person ! "Akka is bad."
    person ! "I also love the color red."
    person ! "Akka is awesome!"
    person ! "I love Akka."

    Thread.sleep(1000)
    person.terminate()
  }


  // never use Behavior[Any]
  object WeirdActor {
    // wants to receive message of type int AND string
    def apply(): Behavior[Any] = Behaviors.receive{ (context, message) =>
      message match {
        case number: Int =>
          context.log.info(s"I've received an int: $number")
          Behaviors.same
        case string: String =>
          context.log.info(s"I've received a string: $string")
          Behaviors.same
      }

    }
  }

  private def demoWeirdActor(): Unit = {
    import BetterActor._
    //val weirdActor = ActorSystem(WeirdActor(), "WeirdActorDemo")
    val weirdActor = ActorSystem(BetterActor(), "WeirdActorDemo")
    weirdActor ! IntMessage(43)    // ok
    weirdActor ! StringMessage("Akka") // ok
    //weirdActor ! '\t'   // not ok

    Thread.sleep(10000)
    weirdActor.terminate()
  }

  // solution: add wrapper types & type hierarchy (case classes/objects)
  private object BetterActor {

    trait Message
    case class IntMessage(number: Int) extends Message
    case class StringMessage(string: String) extends Message

    def apply(): Behavior[Message] = Behaviors.receive{ (context, message) =>
      message match {
        case IntMessage(number) =>
          context.log.info(s"I've received an int: $number")
          Behaviors.same
        case StringMessage(string) =>
          context.log.info(s"I've received a string: $string")
          Behaviors.same
      }
    }
  }


  def main(args: Array[String]): Unit = {
    // demoSimpleActor()
    // testPerson()
    demoWeirdActor()
  }
}
