import java.io.PrintWriter

import spray.json._
import DefaultJsonProtocol._

import scala.io.Source
import scala.util.Try

object MultiJvmToJson extends App {

  // It would be nice to be able to recognise which test each line belongs to.
  // https://github.com/sbt/sbt-multi-jvm/pull/44
  case class Line(jvm: Int, level: String, `@timestamp`: String, system: String, thread: String, `class`: String, message: String)

  def parseLine(line: String): Try[Line] = Try {
    // Perhaps we should append lines that do start with [JVM but don't follow the log pattern otherwise to the message of the previous line.
    val Array(jvm, level, timestamp, systemAndThread, rest) = line.split("\\] \\[")
    val Array(system, thread) = systemAndThread.split("-")
    Line(jvm.drop(5).toInt, level, parseTimestamp(timestamp), system, thread, rest.split("\\] ")(0), rest.split("\\] ").tail.mkString("] "))
  }

  implicit val formatter = jsonFormat7(Line)

  val logs = Source.fromResource("consoleText")
    .getLines()
    .flatMap(line => parseLine(line).toOption)
    .map(_.toJson.compactPrint)
    .mkString("\n")

  new PrintWriter("out.logs") {
    write(logs); close }

  def parseTimestamp(stamp: String): String = {
    val Array(date, time) = stamp.split(" ")
    val Array(month, day, year) = date.split("/")
    Seq(year, month, day).mkString("-") + "T" + time + "Z"
  }


}