import scala.util.Try
import scala.io.Source

object Parser {
  
  def parse(filename: String): List[Line] = parse(filename.split("\\.").init.mkString("."), Source.fromFile(filename))
  def parse(name: String, source: Source): List[Line] = parse(name, source.getLines.toList)
  def parse(name: String, lines: List[String]): List[Line] = {
    val roles = lines.collect {
      case re"\[(.*?)$jvm\].*on node '([^']+)$role'.*" => (jvm, role)
    }.toMap
    val addresses = lines.collect {
      case re"\[(.*?)$jvm\].*Remoting started; listening on address.*?\[([^]]+)$address\].*" => (jvm, address)
      case re"INFO.*Remoting started; listening on address.*?\[([^]]+)$address\].*" => (name, address)
    }.toMap
    lines.flatMap(line => parseLine(name, line, roles, addresses).toOption)
  }
  
    def parseLine(name: String, line: String, roles: Map[String, String], addresses: Map[String, String]): Try[Line] = Try {
      // Perhaps we should append lines that do start with [JVM but don't follow the log pattern otherwise to the message of the previous line?
      line match {
        // testClass still optional until we release sbt-multi-jvm:
        // https://github.com/sbt/sbt-multi-jvm/pull/44
        case re"\[JVM-(\d+)$jvmId(-?)$d([^]]*)$testClass\] \[([^]]*)$level\] \[([^]]*)$timestamp\] \[([^]]*)$thread\] \[([^]]*)$source\] (.*)$message" =>
          Line(jvmId.toInt, testClass, roles(s"JVM-$jvmId$d$testClass"), addresses(s"JVM-$jvmId$d$testClass"), level, parseTimestamp(timestamp), thread, source, message)
        // Console output without multi-jvm:
        case re"(\w+)$level .\[32m(.*?)$timestamp.\[0;39m .\[33m(.*?)$source.\[0;39m  \- (.*)$message" =>
          Line(0, "", name, addresses.getOrElse(name, ""), level, parseConsoleTime(timestamp), "", source, message)
      }
    }

    def parseTimestamp(stamp: String): String = stamp match {
      case re"(\d+)$month/(\d+)$day/(\d+)$year (.*)$time" =>
        Seq(year, month, day).mkString("-") + "T" + time + "Z"
    }

    def parseConsoleTime(time: String): String = {
      // Unfortunately there is only time so we guess the date:
      import java.time._
      val now = LocalDate.now()
      Seq(now.getYear(), now.getMonth().ordinal + 1, now.getDayOfMonth()).mkString("-") + "T" + time + "Z"
    }

    implicit class RegexHelper(val sc: StringContext) extends AnyVal {
      def re: scala.util.matching.Regex = sc.parts.mkString.r
    }

}