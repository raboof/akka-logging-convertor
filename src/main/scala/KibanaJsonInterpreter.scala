import spray.json._
import DefaultJsonProtocol._

object KibanaJsonInterpreter {
  implicit val formatter = jsonFormat9(Line)

  def interpret(lines: Seq[Line]): List[String] = lines.map(_.toJson.compactPrint).toList
}