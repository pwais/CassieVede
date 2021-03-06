/*
 * Copyright 2015 Maintainers of CassieVede
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cassievede.caricare.stddata

import org.cassievede.caricare.datastream.Datastream
import scala.collection.mutable.HashMap
import java.io.File
import org.cassievede.caricare.Utils
import org.cassievede.caricare.ResumableTarStream
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import java.nio.ByteBuffer
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import org.apache.commons.io.output.ByteArrayOutputStream
import javax.imageio.ImageIO
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cassievede.CVSessionConfig

object CIFAR10 extends StandardDataset {

  val log: Log = LogFactory.getLog("CIFAR10")

  // From included batches.meta.txt
  val cidToClassname = Map(
      0 -> "airplane",
      1 -> "automobile",
      2 -> "bird",
      3 -> "cat",
      4 -> "deer",
      5 -> "dog",
      6 -> "frog",
      7 -> "horse",
      8 -> "ship",
      9 -> "truck")

  def datasetNames(conf: CVSessionConfig) : List[String] = {
    List(
      "cifar10.train.1",
      "cifar10.train.2",
      "cifar10.train.3",
      "cifar10.train.4",
      "cifar10.train.5",
      "cifar10.test")
  }

  val cifar10Dist = "http://www.cs.toronto.edu/~kriz/cifar-10-binary.tar.gz"
  lazy val sourceFile = Utils.downloadToTemp(cifar10Dist)

  def stream(conf: CVSessionConfig) : Datastream = new CIFAR10Stream()

  class CIFAR10Stream extends Datastream {

    lazy val tarStream = new ResumableTarStream(sourceFile)
    @transient var curEntry: TarArchiveEntry = null
    @transient var curIterExamples: IterExamples = null

    def hasNext(): Boolean = {

      // Get an iterator over a batch
      if (curIterExamples == null) {
        if (!tarStream.hasNext) { return false }
        curEntry = tarStream.next()
        val entryBytes =
          ByteBuffer.allocate(curEntry.getSize().asInstanceOf[Int])
        tarStream.inStream.read(entryBytes.array())
        log.debug(f"Read tar entry of ${curEntry.getSize()} bytes")

        // Map filename to a dataset name, or skip the entry
        val datasetName = curEntry.getName() match {
          case "cifar-10-batches-bin/data_batch_1.bin" => "cifar10.train.1"
          case "cifar-10-batches-bin/data_batch_2.bin" => "cifar10.train.2"
          case "cifar-10-batches-bin/data_batch_3.bin" => "cifar10.train.3"
          case "cifar-10-batches-bin/data_batch_4.bin" => "cifar10.train.4"
          case "cifar-10-batches-bin/data_batch_5.bin" => "cifar10.train.5"
          case "cifar-10-batches-bin/data_batch_test.bin" => "cifar10.test"
          case _ => {
              log.info(f"Skipping entry in tarfile ${curEntry.getName()}")
              return hasNext()
          }
        }

        curIterExamples = new IterExamples(entryBytes, datasetName)
      } else if (!curIterExamples.hasNext()) {
        curIterExamples = null
        return hasNext()
      }

      return curIterExamples.hasNext()
    }

    def next() : HashMap[String, Any] = curIterExamples.next()

    ///
    /// Utils
    ///

    class IterExamples(b: ByteBuffer, dn: String) extends Datastream {
      var n :Long = 0

      def hasNext() : Boolean = { return b.hasRemaining() }

      def next() : HashMap[String, Any] = {

        // Decode an image and its label.  The image is just an array
        // of non-interleaved RGB
        // FMI see:
        // * http://www.cs.toronto.edu/~kriz/cifar.html
        // * https://github.com/ivan-vasilev/neuralnetworks/blob/master/nn-samples/src/main/java/com/github/neuralnetworks/samples/cifar/CIFARInputProvider.java
        log.debug(f"Reading from buffer pos ${b.position()}")
        val label :Int = b.get() & 0xff // TODO Byte.byte2int(b.get()) ?
        val imageBuffer = b.slice()
        val image = new BufferedImage(32, 32, BufferedImage.TYPE_3BYTE_BGR)
        val px = image
                  .getRaster
                  .getDataBuffer()
                  .asInstanceOf[DataBufferByte]
                  .getData()
        for (i <- 0 until 1024) {
          px((i * 3)) = imageBuffer.get(1024 * 2 + i)
          px((i * 3) + 1) = imageBuffer.get(1024 + i)
          px((i * 3) + 2) = imageBuffer.get(i)
        }
        b.position(b.position() + (32 * 32 * 3))
          // Skip over the imageBuffer bytes we just read

        // Convert RGB raw image to png
        val pngByteStream = new ByteArrayOutputStream()
        ImageIO.write(image, "png", pngByteStream)
        val pngBytes = pngByteStream.toByteArray()

        // Finally, build the record
        val r = new HashMap[String, Any]
        r("id") = n
        r("name") = dn + '.' + n
        r("datasetName") = dn
        r("classnames") = List(cidToClassname(label))
        r("data") = pngBytes

        n += 1

        return r
      }
    }

  }

}
