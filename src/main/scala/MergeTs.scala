import java.io.PrintWriter

import spray.json.{JsObject, JsString}

import scala.io.Source
import scala.util.Try

object MergeTs extends App {

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

  new PrintWriter("out.logs") {
    write(logs); close }

  def vc(ts: Int): String =
    hosts.map(host => s""""$host": $ts""").mkString("{", ",", "}")

  def parseTimestamp(logline: String): String = {
    val timestamp = logline.split("32m").tail.head.split(".\\[0").head
    "2018-01-07T" + timestamp + "Z"
  }


}