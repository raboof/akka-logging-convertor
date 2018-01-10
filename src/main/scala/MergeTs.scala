import java.io.PrintWriter

import scala.annotation.tailrec
import scala.io.Source
import scala.util.Try

object MergeTs extends App {

  val input = Source.fromResource(s"ClusterSingletonManagerLeaveMultiJvm.log").getLines.toList
    .flatMap(line => parseLine(line).toOption)
    
  def parseLine(line: String) = Try {
    val Array(rest, nodeId) = line.split("\\] at \\[")
    val Array(rest2, clocksAndTombs) = rest.split("\\), version = VectorClock\\(")
    val Array(_, gossip) = rest2.split("Updated gossip to \\[Gossip\\(")
    val clocks = clocksAndTombs.split("\\), tombstones =")(0)
    val jsonClocks = clocks.split(", ").map(
      clock => {
        val Array(node, current) = clock.split(" -> ")
        s""" "$node": $current """
    }).mkString("{ ", ", ", " }")
    s"${nodeId.init}|$jsonClocks|$gossip"
  }
  
  new PrintWriter("out.logs") {
    write("(?<host>[^|]+)\\|(?<clock>[^|]+)\\|(?<event>.*)\\n\n")
    write("====\n")
    write(input.mkString("\n"))
    close
  }

}