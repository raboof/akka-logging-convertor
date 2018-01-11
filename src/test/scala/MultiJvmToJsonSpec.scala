import MultiJvmToJson.Line
import org.scalatest.{Matchers, WordSpec}

import scala.util.Success

class MultiJvmToJsonSpec extends WordSpec with Matchers {
  "The multi-jvm jenkins log output parser" should {
    
    val roles = Map("JVM-2-FooSpec" -> "first")
    val addresses = Map("JVM-2-FooSpec" -> "akka.tcp://FooSpec@127.0.0.1:4312")
    
    "parse a simple line" in {
      MultiJvmToJson.parseLine("[JVM-2-FooSpec] [INFO] [01/09/2018 04:07:03.676] [ScalaTest-main] [akka.remote.Remoting] Starting remoting", roles, addresses) should
        be(Success(Line(2, "FooSpec", "first", "akka.tcp://FooSpec@127.0.0.1:4312", "INFO", "2018-01-09T04:07:03.676Z", "ScalaTest-main", "akka.remote.Remoting", "Starting remoting")))
    }
  }
}
