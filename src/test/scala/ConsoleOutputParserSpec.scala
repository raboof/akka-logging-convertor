import scala.io.Source
import org.scalatest.{Matchers, WordSpec}

import scala.util.Success

class ConsoleOutputParserSpec extends WordSpec with Matchers {
  "The console log output parser" should {
    
    val roles = Map("JVM-2-FooSpec" -> "first")
    val addresses = Map("gw-1001" -> "akka.tcp://FooSpec@127.0.0.1:4312")
    val date = {
      import java.time._
      val now = LocalDate.now()
      Seq(now.getYear(), now.getMonth().ordinal + 1, now.getDayOfMonth()).mkString("-")
    }

    "parse a single line" in {
      Parser.parseLine("gw-1001", "INFO [32m20:06:43.861[0;39m [33mROOT[0;39m  - Using configurator: play.api.libs.logback.LogbackLoggerConfigurator@4b9df8a", roles, addresses) should
        be(Success(Line(0, "", "gw-1001", "akka.tcp://FooSpec@127.0.0.1:4312", "INFO", s"${date}T20:06:43.861Z", "", "ROOT", "Using configurator: play.api.libs.logback.LogbackLoggerConfigurator@4b9df8a")))
    }
    
    "parse console output" in {
        val source = Source.fromResource("gw-1001.log").getLines().toList
        source.size should be(4726)
        val lines = Parser.parse("gw-1001", source)
        // Successfully parse 1877 lines:
        lines.size should be(4364)
        lines.head should be(Line(0, "", "gw-1001", "akka://myapp@gw-1001.ad1.myvcn.com:2500", "INFO", s"${date}T20:06:43.861Z", "", "ROOT", "Using configurator: play.api.libs.logback.LogbackLoggerConfigurator@4b9df8a"))
    }
  }
}
