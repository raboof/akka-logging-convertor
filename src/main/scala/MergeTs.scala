import java.io.PrintWriter

import scala.io.Source
import scala.util.Try

object MergeTs extends App {

//  println(classOf[App].getResourceAsStream("gw-1001.log").read())
//  println(Thread.currentThread().getContextClassLoader().getResourceAsStream("/gw-1001.log").read())
//  Source.fromResource(s"gw-1001.log").getLines.toList

  val hosts = Seq("gw-1001", "gw-2001", "gw-3001")
  val sources: Seq[(Long, String, String)] =
    hosts.flatMap(name => Source.fromResource(s"$name.log").getLines.toList.flatMap(line => parseTimestamp(line).toOption.toList.map(ts => (ts, name, line))))

  val logs = sources
    .groupBy(_._1)
    .toList
    .sortBy(_._1)
    .zipWithIndex
    .flatMap {
    case ((ts, events), idx) =>
      events.map {
        case (_, host, event) =>
        s"$host|${vc(idx+1)}|$event"
      }
  }.mkString("\n")

  new PrintWriter("out.logs") {
    write("(?<host>[^|]+)\\|(?<clock>[^|]+)\\|(?<event>.*)\\n\n")
    write("====\n")
    write(logs); close }

  def vc(ts: Int): String =
    hosts.map(host => s""""$host": $ts""").mkString("{", ",", "}")

  def parseTimestamp(logline: String): Try[Long] = Try {
    val timestamp = logline.split("32m").tail.head.split(".\\[0").head
    val Array(head, millis) = timestamp.split("\\.")
    val Array(hours, minutes, seconds) = head.split(":")
    millis.toLong + 1000 * (seconds.toLong + 60 * (minutes.toLong + 60 * hours.toLong))
  }


}