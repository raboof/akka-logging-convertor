import java.io.PrintWriter

object MultiJvmToJson extends App {
  if (args.size < 2) {
    println("""Usage:
      sbt "run input1.txt input2.txt outfile.json"
      sbt "run input1.txt input2.txt outfile.shiviz"
      """")
  } else {
    val lines = args.init.flatMap(Parser.parse)
    val target = args.last
    val result = target.split("\\.").last match {
      case "json" => KibanaJsonInterpreter.interpret(lines)
      case "shiviz" => ClusterShiVizInterpreter.interpret(lines)
    }
    new PrintWriter(target) {
      write(result.mkString("\n")); close }
  }
}