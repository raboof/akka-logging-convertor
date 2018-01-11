import scala.io.Source
import org.scalatest.{Matchers, WordSpec}

import scala.util.Success

class MultiJvmToJsonSpec extends WordSpec with Matchers {
  "The multi-jvm jenkins log output parser" should {
    
    val roles = Map("JVM-2-FooSpec" -> "first")
    val addresses = Map("JVM-2-FooSpec" -> "akka.tcp://FooSpec@127.0.0.1:4312")
    
    "parse a simple line" in {
      Parser.parseLine("", "[JVM-2-FooSpec] [INFO] [01/09/2018 04:07:03.676] [ScalaTest-main] [akka.remote.Remoting] Starting remoting", roles, addresses) should
        be(Success(Line(2, "FooSpec", "first", "akka.tcp://FooSpec@127.0.0.1:4312", "INFO", "2018-01-09T04:07:03.676Z", "ScalaTest-main", "akka.remote.Remoting", "Starting remoting")))

      Parser.parseLine("", "[JVM-2] [INFO] [01/09/2018 04:07:03.676] [ScalaTest-main] [akka.remote.Remoting] Starting remoting", Map("JVM-2" -> "first"), Map("JVM-2" -> "akka.tcp://FooSpec@127.0.0.1:4312")) should
        be(Success(Line(2, "", "first", "akka.tcp://FooSpec@127.0.0.1:4312", "INFO", "2018-01-09T04:07:03.676Z", "ScalaTest-main", "akka.remote.Remoting", "Starting remoting")))
    }
  }

  "The kibana json interpreter" should {
    "parse and interpret the example sbt output" in {
        val source = Source.fromResource("akka-cluster-tools-multi-jvm-test.log").getLines().toList
        val lines = Parser.parse("", source)
        // Successfully parse 1877 lines:
        lines.size should be(1877)
        lines should contain(Line(1,"DistributedPubSubMediatorMultiJvmNode1","first","akka.tcp://DistributedPubSubMediatorSpec@localhost:39571","INFO","2018-01-10T11:15:01.083Z","ScalaTest-main","akka.remote.Remoting","Starting remoting"))
    }

    "parse and interpret the example jenkins output" in {
        val source = Source.fromResource("consoleText").getLines().toList
        val lines = Parser.parse("", source)
        lines.head should be(Line(2,"","second","akka.tcp://ClusterShardingLeavingSpec@localhost:36963","INFO","2018-01-09T04:07:03.676Z","ScalaTest-main","akka.remote.Remoting","Starting remoting"))
        lines.size should be(24250)
    }
  }
}
