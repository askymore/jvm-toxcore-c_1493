package im.tox.core.dht.packets.dht

import im.tox.core.ModuleCompanionTest
import im.tox.core.dht.NodeInfo
import im.tox.core.dht.NodeInfoTest._
import org.scalacheck.Arbitrary._
import org.scalacheck.{Arbitrary, Gen}

object NodesResponsePacketTest {

  private val _ = arbNodeInfo // XXX(iphydf): Hack because IDEA doesn't recognise the need for it.

  implicit val arbNodesResponse: Arbitrary[NodesResponsePacket] = {
    Arbitrary(Gen.resultOf[(Seq[NodeInfo], Long), NodesResponsePacket] {
      case (nodes, pingId) =>
        NodesResponsePacket(nodes.take(4), pingId).toOption.get
    })
  }

}

final class NodesResponsePacketTest extends ModuleCompanionTest(NodesResponsePacket) {

  override val arbT = NodesResponsePacketTest.arbNodesResponse

}