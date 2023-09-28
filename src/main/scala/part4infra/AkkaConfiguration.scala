package part4infra

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory

object AkkaConfiguration {

  object SimpleLoggingActor {
    def apply(): Behavior[String] = Behaviors.receive { (context, message) =>
      context.log.info(message)
      Behaviors.same
    }
  }

  // 1 - inline configuration
  def demoInlineConfig(): Unit = {
    val configString: String = {
      // HOCON, superset of json, managed by Lightbend
      """
        |akka {
        | loglevel = "DEBUG"
        |}
        |""".stripMargin
    }
    val config = ConfigFactory.parseString(configString)
    val system = ActorSystem(SimpleLoggingActor(), "ConfigDemo", ConfigFactory.load(config))

    system ! "A message to remember"
    Thread.sleep(1000)
    system.terminate()
  }

  // 2 - config file
  def demoConfigFile(): Unit = {
    val specialConfig = ConfigFactory.load().getConfig("mySpecialConfig2")
    val system = ActorSystem(SimpleLoggingActor(), "ConfigDemoFile", ConfigFactory.load(specialConfig))

    system ! "A message to remember"
    Thread.sleep(1000)
    system.terminate()
  }

  // 3 - a different config in another file
  def demoSeparateConfigFile(): Unit = {
    val separateConfig =
      ConfigFactory.load("secretDir/secretConfiguration.conf")
    print(separateConfig.getString("akka.loglevel"))

  }

  // 4 - different file formats (JSON, properties)
  def demoOtherFileFormats(): Unit = {
    val jsonConfig = ConfigFactory.load("json/jsonConfiguration.json")
    println(s"json config with custom property: ${jsonConfig.getString("aJsonProperty")}")
    println(s"json config with Akka property: ${jsonConfig.getString("akka.loglevel")}")

    val propConfig = ConfigFactory.load("properties/propsConfiguration.properties")
    println(s"properties config with custom property: ${propConfig.getString("mySimpleProperty")}")
    println(s"properties config with Akka property: ${propConfig.getString("akka.loglevel")}")

  }

  def main(args: Array[String]): Unit = {
    // demoInlineConfig()
    // demoConfigFile()
    // demoSeparateConfigFile()
    demoOtherFileFormats()
  }
}
