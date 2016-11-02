package mesosphere.marathon
package state

import mesosphere.UnitTest
import mesosphere.marathon.state.DiscoveryInfo.Port
import mesosphere.marathon.stream.Implicits._
import org.apache.mesos.{ Protos => MesosProtos }

class DiscoveryInfoTest extends UnitTest {

  class Fixture {
    lazy val emptyDiscoveryInfo = DiscoveryInfo()

    lazy val discoveryInfoWithPort = DiscoveryInfo(
      ports = Seq(Port(name = "http", number = 80, protocol = "tcp", labels = Map("VIP_0" -> "192.168.0.1:80")))
    )
    lazy val discoveryInfoWithTwoPorts = DiscoveryInfo(
      ports = Seq(
        Port(name = "dns", number = 53, protocol = "udp"),
        Port(name = "http", number = 80, protocol = "tcp")
      )
    )
    lazy val discoveryInfoWithTwoPorts2 = DiscoveryInfo(
      ports = Seq(
        Port(name = "dnsudp", number = 53, protocol = "udp"),
        Port(name = "dnstcp", number = 53, protocol = "tcp")
      )
    )
  }

  def fixture(): Fixture = new Fixture

  "DiscoveryInfo" should {
    "ToProto default DiscoveryInfo" in {
      val f = fixture()
      val proto = f.emptyDiscoveryInfo.toProto

      proto should be(Protos.DiscoveryInfo.getDefaultInstance)
    }

    "ToProto with one port" in {
      val f = fixture()
      val proto = f.discoveryInfoWithPort.toProto

      val portProto =
        MesosProtos.Port.newBuilder()
          .setName("http")
          .setNumber(80)
          .setProtocol("tcp")
          .setLabels(
            MesosProtos.Labels.newBuilder.addLabels(
              MesosProtos.Label.newBuilder
                .setKey("VIP_0")
                .setValue("192.168.0.1:80")))
          .build()

      proto.getPortsList.head should equal(portProto)
    }

    "ConstructFromProto with default proto" in {
      val f = fixture()

      val defaultProto = Protos.DiscoveryInfo.newBuilder.build
      val result = DiscoveryInfo.fromProto(defaultProto)
      result should equal(f.emptyDiscoveryInfo)
    }

    "ConstructFromProto with port" in {
      val f = fixture()

      val portProto =
        MesosProtos.Port.newBuilder()
          .setName("http")
          .setNumber(80)
          .setProtocol("tcp")
          .setLabels(
            MesosProtos.Labels.newBuilder.addLabels(
              MesosProtos.Label.newBuilder
                .setKey("VIP_0")
                .setValue("192.168.0.1:80")))
          .build()

      val protoWithPort = Protos.DiscoveryInfo.newBuilder
        .addAllPorts(Seq(portProto))
        .build

      val result = DiscoveryInfo.fromProto(protoWithPort)
      result should equal(f.discoveryInfoWithPort)
    }
  }
}
