import scala.io.Source
import org.scalatest.{Matchers, WordSpec}

import scala.util.Success

class ClusterEventsToShiVizSpec extends WordSpec with Matchers {
  "The shiviz interpreter" should {
    
    "parse and interpret the example sbt output" in {
        val source = Source.fromResource("ClusterSingletonManagerLeaveMultiJvmNode.logs").getLines().toList
        val lines = Parser.parse("", source)
        lines.size should be(294)
        val shiviz = ClusterShiVizInterpreter.interpret(lines)
        shiviz.size should be(70)
    }
  }
}
