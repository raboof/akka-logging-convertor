import java.io.PrintWriter

import scala.annotation.tailrec
import scala.io.Source
import scala.util.Try

object MergeTs extends App {

  val input = Source.fromResource(s"akka-cluster-tools-multi-jvm-test.log").getLines.toList
    .filter(_.contains("ClusterSingletonManagerLeaveMultiJvmNode"))
    .flatMap(line => parseLine(line).toOption)
  
  val interpreted = input.flatMap(line => interpretLine(line).toOption)
  
  // It would be nice to be able to recognise which test each line belongs to.
  // https://github.com/sbt/sbt-multi-jvm/pull/44
  case class Line(jvm: Int, level: String, `@timestamp`: String, system: String, thread: String, actorPath: String, message: String)

  def parseLine(line: String): Try[Line] = Try {
    // Perhaps we should append lines that do start with [JVM but don't follow the log pattern otherwise to the message of the previous line.
    val Array(jvm, level, timestamp, systemAndThread, rest) = line.split("\\] \\[")
    val system = systemAndThread.split("-")(0)
    val thread = systemAndThread.split("-").tail.mkString
    Line(jvm.drop(5).split("-")(0).toInt, level, parseTimestamp(timestamp), system, thread, rest.split("\\] ")(0), rest.split("\\] ").tail.mkString("] "))
  }
  
  def interpretLine(line: Line): Try[ShiVizLine] = Try {
    val (process, mark, backReference) = line.message match {
      // On each node:
      case re"Cluster Node \[([^]]+)$node\] \- Starting.*" =>
        (line.jvm.toString, Some(Starting(node)), None)
      case re".* Node \[([^]]+)$node\] is JOINING.*" =>
        (line.jvm.toString, Some(NodeStateChange(node, "Joining")), Some(Starting(node)))
      // On each node:
      case re"Member([^)]+)$event\(Member\(address = ([^,]+)$node.*" =>
        val state = event match {
          case "Up" => "Up"
          case "Left" => "Leaving"
          case "Exited" => "Exiting"
          case "Removed" => "Removed"
        }
        (line.jvm.toString, None, Some(NodeStateChange(node, state)))
      // On leader:
      case re".*Leader is moving node \[([^]]+)$node\] to \[([^]]+)$state\].*" => 
        (line.jvm.toString, Some(NodeStateChange(node, state)), if (state == "Up") Some(NodeStateChange(node, "Joining")) else None)
      // On leader:
      case re".*Leader is removing unreachable node \[([^]]+)$node\].*" => 
        (line.jvm.toString, Some(NodeStateChange(node, "Removed")), None)
      // On node that initiates leaving:
      case re".*Marked address \[([^]]+)$node\] as \[Leaving\].*" =>
        (line.jvm.toString, Some(NodeStateChange(node, "Leaving")), None)
      case re"ClusterSingletonManager state change \[(.*)$previousState \-> (.*)$newState\]" =>
        // No need to mark explicitly, but we do want to see those in the output.
        ("CS-" + line.jvm + "-" + line.actorPath.split("/user/")(1), None, None)
      case re"LeaderChanged.*" =>
        (line.jvm.toString, None, None)
    }
    ShiVizLine(process, mark, backReference, line.`@timestamp` + ": " + line.message)
  }
  
  // Could remove tail recursion
  def addOwnClocks(lines: List[ShiVizLine], currentClocks: Map[Process, Int] = Map.empty.withDefaultValue(1)): List[(Int, ShiVizLine)] = lines match {
    case Nil => Nil
    case x :: xs =>
      (currentClocks(x.process), x) :: addOwnClocks(xs, currentClocks.updateWith(x.process, _ + 1))
  }

  sealed trait ShiVizMark
  case class Starting(nodeAddress: String) extends ShiVizMark
  case class NodeStateChange(nodeAddress: String, state: String) extends ShiVizMark

  type Process = String
  case class ShiVizLine(process: Process, mark: Option[ShiVizMark], refersBackTo: Option[ShiVizMark], message: String)

  val names: Map[Process, String] = interpreted.flatMap { line => line.mark match {
    case Some(Starting(address)) => Some((line.process, address.split(":").last))
    case _ => None
  }}.toMap
  
  def name(process: Process): String = process match {
      case re"CS\-([^-]+)$jvmid.*" => 
        "CS-" + name(jvmid)
      case other => 
        names.get(other).getOrElse(other)
  }
  
  val interpretedWithOwnClock = addOwnClocks(interpreted)

  // TODO perhaps check for duplicate marks?
  val marks: Map[ShiVizMark, (Process, Int)] =
    interpretedWithOwnClock.flatMap {
      case (version, line) => line.mark match {
        case None => None
        case Some(mark) => Some(mark -> (line.process, version))
    }}.toMap

  val backrefsCollectedWhileParsing =
    interpretedWithOwnClock.flatMap {
      case (version, line) => line.refersBackTo.flatMap(mark => marks.get(mark).map(pi => (line.process, version) -> pi))
    }.toMap

  val clusterSingletonBackRefs = interpretedWithOwnClock
    .filter { case (_, l) => !l.message.contains("Start ->") }
    .flatMap {
    case (version, line @ ShiVizLine(re"CS-(\d+)$jvmid.*", _, _, _)) =>
      // Very rough heuristic: cluster change before singleton transition might be its cause..
      interpretedWithOwnClock
        .takeWhile { case (_, l) => l != line }
        .filter { case (_, l) => l.process == jvmid }
        .filter { case (_, l) => l.message.contains(": Member")}
        .lastOption
        .map { case (v, l) => (line.process, version) -> (l.process, v) }
    case _ => None
  }.toMap

  val backrefs: Map[(Process, Int), (Process, Int)] = backrefsCollectedWhileParsing ++ clusterSingletonBackRefs

  new PrintWriter("out.logs") {
    write("(?<host>[^|]+)\\|(?<clock>[^|]+)\\|(?<event>.*)\\n\n")
    write("====\n")
    @tailrec
    def writeAll(vectorClocks: Map[Process, Map[Process, Int]], lines: List[(Int, ShiVizLine)]): Unit = lines match {
      case Nil => //done
      case (version, line) :: xs =>
        val updatedClocks = vectorClocks.updateWith(line.process, clocks => {
          val incremented = increment(clocks, line.process)
          backrefs.get((line.process, version)) match {
            case None => incremented
            case Some((line.process, _)) => incremented
            case Some((process, version)) => incremented.updateWith(process, _ => version)
          }
        })
        
        val clock = updatedClocks(line.process).map { case (process, version) => s""" "${name(process)}": $version """ }.mkString("{", ", ", "}")
        write(s"${name(line.process)}|$clock|${line.message}\n")
        writeAll(updatedClocks, xs)
    }
    writeAll(Map.empty.withDefaultValue(Map.empty.withDefaultValue(0)), interpretedWithOwnClock)
    close
  }
  
  def increment(clocks: Map[Process, Int], process: Process) = clocks.updateWith(process, _ + 1)
  
  implicit class PimpedMap[K, V](val map: Map[K, V]) extends AnyVal {
    def updateWith(key: K, f: V => V): Map[K, V] =
      map.updated(key, f(map(key)))
  }
  
  implicit class RegexHelper(val sc: StringContext) extends AnyVal {
    def re: scala.util.matching.Regex = sc.parts.mkString.r
  }
  
  def parseTimestamp(stamp: String): String = {
    val Array(date, time) = stamp.split(" ")
    val Array(month, day, year) = date.split("/")
    Seq(year, month, day).mkString("-") + "T" + time + "Z"
  }

}