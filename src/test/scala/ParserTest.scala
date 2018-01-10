import org.scalatest._

class ParserTest extends WordSpec with Matchers {
  import MergeTs._
  
  "The parsing" should {
    "parse a leader line" in {
      parseLine("""[JVM-1-DistributedPubSubMediatorMultiJvmNode1] [INFO] [01/10/2018 11:15:02.032] [DistributedPubSubMediatorSpec-akka.actor.default-dispatcher-18] [akka.cluster.Cluster(akka://DistributedPubSubMediatorSpec)] Cluster Node [akka.tcp://DistributedPubSubMediatorSpec@localhost:39571] - Leader is moving node [akka.tcp://DistributedPubSubMediatorSpec@localhost:39571] to [Up]""").get
    }
    "parse and interpret 'Cluster node is Starting up'" in {
      val line = parseLine("""[JVM-1-DistributedPubSubMediatorMultiJvmNode1] [INFO] [01/10/2018 11:15:01.229] [ScalaTest-main] [akka.cluster.Cluster(akka://DistributedPubSubMediatorSpec)] Cluster Node [akka.tcp://DistributedPubSubMediatorSpec@localhost:39571] - Starting up...""").get
      val event = interpretLine(line).get
      event.mark.get should be(Starting("akka.tcp://DistributedPubSubMediatorSpec@localhost:39571"))
    }
    "parse and interpret 'Node is JOINING'" in {
      val line = parseLine("""[JVM-1-DistributedPubSubMediatorMultiJvmNode1] [INFO] [01/10/2018 11:15:02.020] [DistributedPubSubMediatorSpec-akka.actor.default-dispatcher-18] [akka.cluster.Cluster(akka://DistributedPubSubMediatorSpec)] Cluster Node [akka.tcp://DistributedPubSubMediatorSpec@localhost:39571] - Node [akka.tcp://DistributedPubSubMediatorSpec@localhost:39571] is JOINING, roles [dc-default]""").get
      val event = interpretLine(line).get
      event.refersBackTo.get should be(Starting("akka.tcp://DistributedPubSubMediatorSpec@localhost:39571"))
    }
  }
}