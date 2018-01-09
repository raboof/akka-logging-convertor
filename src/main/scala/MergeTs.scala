import java.io.PrintWriter
import java.time.{LocalDate, ZoneOffset}

import spray.json.{JsNumber, JsObject, JsString}

import scala.io.Source
import scala.util.Try

object MergeTs extends App {

//  println(classOf[App].getResourceAsStream("gw-1001.log").read())
//  println(Thread.currentThread().getContextClassLoader().getResourceAsStream("/gw-1001.log").read())
//  Source.fromResource(s"gw-1001.log").getLines.toList

  val hosts = Seq("gw-1001", "gw-2001", "gw-3001")
  val sources = hosts.flatMap(name => Source.fromResource(s"$name.log").getLines.toList.map(event => (name, event)))

  val logs = sources.flatMap {
    case (name, event) => Try(name, event, parseTimestamp(event)).toOption.toList
  }.map {
      case (host, event, timestamp) => {
        val level = event.split(" .\\[32m")(0)
        val Array(logger, message) = event.split("\\[33m")(1).split(".\\[0;39m  ")
        JsObject(
          "@timestamp" -> JsString(timestamp),
          "host" -> JsString(host),
          "event" -> JsString(message),
          "level" -> JsString(level),
          "logger" -> JsString(logger),
        ).compactPrint
      }
    }.mkString("\n")

//  val logs = sources
//    .groupBy(_._1)
//    .toList
//    .sortBy(_._1)
//    .zipWithIndex
//    .flatMap {
//    case ((ts, events), idx) =>
//      events.map {
//        case (_, host, event) =>
//        s"$host|${vc(idx+1)}|$event"
//      }
//  }.mkString("\n")
//
  new PrintWriter("out.logs") {
//    write("(?<host>[^|]+)\\|(?<clock>[^|]+)\\|(?<event>.*)\\n\n")
//    write("====\n")
    write(logs); close }

  def vc(ts: Int): String =
    hosts.map(host => s""""$host": $ts""").mkString("{", ",", "}")

  def parseTimestamp(logline: String): String = {
    val timestamp = logline.split("32m").tail.head.split(".\\[0").head
//    val Array(head, millis) = timestamp.split("\\.")
//    val Array(hours, minutes, seconds) = head.split(":")
//    val epochSecond = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).getEpochSecond
//
//    millis.toLong + 1000 * (epochSecond + seconds.toLong + 60 * (minutes.toLong + 60 * hours.toLong))
    "2018-01-07T" + timestamp + "Z"
  }


}