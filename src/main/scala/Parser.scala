import scala.util.Try
import scala.io.Source

object Parser {
  
  def parse(filename: String): List[Line] = parse(Source.fromFile(filename))
  def parse(source: Source): List[Line] = parse(source.getLines.toList)
  def parse(lines: List[String]): List[Line] = {
    val roles = lines.collect {
      case re"\[(.*?)$jvm\].*on node '([^']+)$role'.*" => (jvm, role)
    }.toMap
    val addresses = lines.collect {
      case re"\[(.*?)$jvm\].*Remoting started; listening on addresses :\[([^]]+)$address\]" => (jvm, address)
    }.toMap
    
    lines.flatMap(line => parseLine(line, roles, addresses).toOption)
  }
  
    def parseLine(line: String, roles: Map[String, String], addresses: Map[String, String]): Try[Line] = Try {
      // Perhaps we should append lines that do start with [JVM but don't follow the log pattern otherwise to the message of the previous line?
      line match {
        // testClass still optional until we release sbt-multi-jvm:
        // https://github.com/sbt/sbt-multi-jvm/pull/44
        case re"\[JVM-(\d+)$jvmId(-?)$d([^]]*)$testClass\] \[([^]]*)$level\] \[([^]]*)$timestamp\] \[([^]]*)$thread\] \[([^]]*)$source\] (.*)$message" =>
          Line(jvmId.toInt, testClass, roles(s"JVM-$jvmId$d$testClass"), addresses(s"JVM-$jvmId$d$testClass"), level, parseTimestamp(timestamp), thread, source, message)
      }
    }
    
    
    def parseTimestamp(stamp: String): String = stamp match {
      case re"(\d+)$month/(\d+)$day/(\d+)$year (.*)$time" =>
        Seq(year, month, day).mkString("-") + "T" + time + "Z"
    }

    implicit class RegexHelper(val sc: StringContext) extends AnyVal {
      def re: scala.util.matching.Regex = sc.parts.mkString.r
    }

}