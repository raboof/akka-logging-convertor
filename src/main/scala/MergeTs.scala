import java.io.PrintWriter

import scala.annotation.tailrec
import scala.io.Source
import scala.util.Try

object MergeTs extends App {

  val hosts = Seq("gw-1001", "gw-2001", "gw-3001")
  val sources = hosts.flatMap(host => Source.fromResource(s"$host.log").getLines.toList.map(event => (host, event)))

  val logs = sources.flatMap {
    case (name, event) => Try(name, event, parseTimestamp(event)).toOption.toList
  }.sortBy(_._3).toList

  new PrintWriter("out.logs") {
    write("(?<host>[^|]+)\\|(?<clock>[^|]+)\\|(?<event>.*)\\n\n")
    write("====\n")
    @tailrec
    def writeAll(logs: List[(String, String, Long)], currentTimestamps: Map[String, Long]): Unit = logs match {
      case Nil => // Done.
      case (host, event, _) :: xs =>
        val nextTimestamps = currentTimestamps.updated(host, currentTimestamps.getOrElse(host, 0L) + 1L)
        write(s"$host|${vc(nextTimestamps)}|$event\n")
        writeAll(xs, nextTimestamps)
    }
    writeAll(logs, Map.empty)
    close
  }

  def vc(ts: Map[String, Long]): String =
    hosts.flatMap(host => ts.get(host).map(t => s""""$host": $t""")).mkString("{", ",", "}")

  def parseTimestamp(logline: String): Long = {
    val timestamp = logline.split("32m").tail.head.split(".\\[0").head
    val Array(head, millis) = timestamp.split("\\.")
    val Array(hours, minutes, seconds) = head.split(":")
    millis.toLong + 1000 * (seconds.toLong + 60 * (minutes.toLong + 60 * hours.toLong))
  }


}