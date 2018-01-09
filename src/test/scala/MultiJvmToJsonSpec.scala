import MultiJvmToJson.Line
import org.scalatest.{Matchers, WordSpec}

import scala.util.Success

class MultiJvmToJsonSpec extends WordSpec with Matchers {
  "The multi-jvm jenkins log output parser" should {
    "parse a simple line" in {
      MultiJvmToJson.parseLine("[JVM-2] [INFO] [01/09/2018 04:07:03.676] [ScalaTest-main] [akka.remote.Remoting] Starting remoting") should
        be(Success(Line(2, "INFO", "2018-09-01T04:07:03.676Z", "ScalaTest", "main", "akka.remote.Remoting", "Starting remoting")))
    }
  }
}
