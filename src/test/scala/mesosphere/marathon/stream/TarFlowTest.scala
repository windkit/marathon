package mesosphere.marathon
package stream

import akka.util.ByteString
import java.io.{ ByteArrayInputStream, ByteArrayOutputStream }
import mesosphere.{ AkkaTest, UnitTest }

import akka.stream.scaladsl.Source
import org.apache.commons.compress.archivers.tar.{ TarArchiveInputStream, TarArchiveOutputStream, TarConstants }
import scala.annotation.tailrec

class TarFlowTest extends UnitTest with AkkaTest {
  import TarFlow._

  val sampleData = ByteString("daterbase")
  val tarEntries = List(
    TarEntry(
      "1234567890/1234567890/1234567890/1234567890/1234567890/1234567890/1234567890/1234567890/1234567890/1234567890/long.txt",
      ByteString("> 100 char file name. Look out!")),
    TarEntry(
      "/path/to/file-2.txt",
      sampleData),
    TarEntry(
      "/path/to/file.txt",
      (1 to 1024).map(_ => sampleData).reduce(_ ++ _)))

  val tarredBytes =
    Source(tarEntries).
      via(TarFlow.writer).
      runReduce(_ ++ _).
      futureValue

  List(1, 13, 512, Int.MaxValue).foreach { n =>
    s"it can roundtrip tar and untar with ${n} sized byte boundaries" in {
      val untarredItems =
        Source(tarredBytes.grouped(n).toList). // we vary the chunk sizes to make sure we handle boundaries properly
          via(TarFlow.reader).
          runFold(List.empty[TarEntry]) { _ :+ _ }.
          futureValue
      untarredItems.map(_.header.getName) shouldBe tarEntries.map(_.header.getName)
      untarredItems.map(_.data) shouldBe tarEntries.map(_.data)
    }
  }

  "it generates valid tar data that can be read by apache commons TarArchiveInputStream" in {
    val bytes = new ByteArrayInputStream(tarredBytes.toArray)
    val tar = new TarArchiveInputStream(bytes)

    var entries = List.empty[TarEntry]
    @tailrec def readEntries(tar: TarArchiveInputStream, entries: List[TarEntry] = Nil): List[TarEntry] = {
      val entry = tar.getNextTarEntry
      if (entry == null)
        entries
      else {
        val data = Array.ofDim[Byte](entry.getSize.toInt)
        tar.read(data)
        readEntries(tar, entries :+ TarEntry(entry, ByteString(data)))
      }
    }

    val untarredItems = readEntries(tar)
    untarredItems.map(_.header.getName) shouldBe tarEntries.map(_.header.getName)
    untarredItems.map(_.data) shouldBe tarEntries.map(_.data)
  }

  "it reads tar data generated by apache commons TarArchiveOutputStream" in {
    val bos = new ByteArrayOutputStream
    val tarOut = new TarArchiveOutputStream(bos)
    tarOut.setLongFileMode(TarConstants.FORMAT_OLDGNU)
    tarEntries.foreach { entry =>
      tarOut.putArchiveEntry(entry.header)
      tarOut.write(entry.data.toArray, 0, entry.data.size)
      tarOut.closeArchiveEntry()
    }
    tarOut.finish()

    val untarredItems =
      Source(ByteString(bos.toByteArray()).grouped(1024).toList).
        via(TarFlow.reader).
        runFold(List.empty[TarEntry]) { _ :+ _ }.
        futureValue

    untarredItems.map(_.header.getName) shouldBe tarEntries.map(_.header.getName)
    untarredItems.map(_.data) shouldBe tarEntries.map(_.data)
  }
}
