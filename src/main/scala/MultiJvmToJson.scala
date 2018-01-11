import java.io.PrintWriter

import spray.json._
import DefaultJsonProtocol._

import scala.io.Source
import scala.util.Try

object MultiJvmToJson extends App {

  // It would be nice to be able to recognise which test each line belongs to.
  // https://github.com/sbt/sbt-multi-jvm/pull/44
  case class Line(jvm: Int, testClass: String, role: String, address: String, level: String, `@timestamp`: String, thread: String, source: String, message: String)
  implicit val formatter = jsonFormat9(Line)

  def parseLine(line: String, roles: Map[String, String], addresses: Map[String, String]): Try[Line] = Try {
    // Perhaps we should append lines that do start with [JVM but don't follow the log pattern otherwise to the message of the previous line?
    line match {
      case re"\[JVM-(\d+)$jvmId-?([^]]*)$testClass\] \[([^]]*)$level\] \[([^]]*)$timestamp\] \[([^]]*)$thread\] \[([^]]*)$source\] (.*)$message" =>
        Line(jvmId.toInt, testClass, roles(s"JVM-$jvmId-$testClass"), addresses(s"JVM-$jvmId-$testClass"), level, parseTimestamp(timestamp), thread, source, message)
    }
  }
  
  val lines = Source.fromResource("akka-cluster-tools-multi-jvm-test.log")
    .getLines()
    .toList
  
  val roles = lines.collect {
    case re"\[(.*?)$jvm\].*on node '([^']+)$role'.*" => (jvm, role)
  }.toMap
  val addresses = lines.collect {
    case re"\[(.*?)$jvm\].*Remoting started; listening on addresses :\[([^]]+)$address\]" => (jvm, address)
  }.toMap
  
  val logs = lines
    .flatMap(line => parseLine(line, roles, addresses).toOption)
    .map(_.toJson.compactPrint)
    .mkString("\n")

  new PrintWriter("out.logs") {
    write(logs); close }

  def parseTimestamp(stamp: String): String = stamp match {
    case re"(\d+)$month/(\d+)$day/(\d+)$year (.*)$time" =>
      Seq(year, month, day).mkString("-") + "T" + time + "Z"
  }

  implicit class RegexHelper(val sc: StringContext) extends AnyVal {
    def re: scala.util.matching.Regex = sc.parts.mkString.r
  }

}