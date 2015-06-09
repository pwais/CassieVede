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

import java.io.File
import java.nio.ByteBuffer

import scala.collection.mutable.HashMap

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cassievede.CVSessionConfig
import org.cassievede.caricare.ResumableTarStream
import org.cassievede.caricare.datastream.Datastream

import com.google.common.base.Preconditions.checkNotNull

object ImageNet extends StandardDataset {

  def datasetNames(conf: CVSessionConfig) = List("imagenet")

  def stream(conf: CVSessionConfig) =
    new ImageNet(new File(conf.stdDatasetPath))
}


/**
 * Generate a Datastream from a copy of the ImageNet tarball
 * (~1.2TB "fall11_whole.tar") To obtain the tarball, see
 * http://image-net.org/download
 *
 * The ImageNet tarball has the following structure:
 *   * Each file is a tarball named (wnid).tar.  This inner tarball
 *       contains image files of the form (wnid)_(img number).JPEG
 *   * There are 21841 total inner tarballs
 */
class ImageNet(rootTar: File) extends Datastream {
  val log: Log = LogFactory.getLog("ImageNetStream")

  lazy val rootTarStream = new ResumableTarStream(rootTar)

  var n: Long = 0

  @transient var curIter: IterInnerTarEntries = null
  var curIterStartOffset: Long = 0

  @transient var curEntry: TarArchiveEntry = null

  def hasNext(): Boolean = {
    if (!rootTarStream.hasNext()) { return false }

    if (curIter == null) {
      curEntry = rootTarStream.next()
      curIter =
        new IterInnerTarEntries(
              rootTarStream,
              curEntry,
              curIterStartOffset)
    }

    if (!curIter.hasNext()) {
      curIter = null
      curIterStartOffset = 0
      return hasNext()
    }

    return curIter.hasNext()
  }

  def next() : HashMap[String, Any] = {
    val r = curIter.next()
    r("datasetName") = "imagenet"
    r("id") = n

    n += 1

    return r
  }

  ///
  /// Utils
  ///

  class IterInnerTarEntries(
       tarStream: ResumableTarStream,
       curEntry: TarArchiveEntry,
       startOffset: Long)
    extends Datastream {

      private var curInnerEntry: TarArchiveEntry = null

      var wnid :String = ""

      val innerTarStream = {
        checkNotNull(curEntry)
        log.info(f"Reading from entry ${curEntry.getName()}")
        wnid = curEntry.getName().split(".tar")(0)
        new TarArchiveInputStream(tarStream.inStream)
      }

      def hasNext() : Boolean = {
        if (startOffset != 0) {
          log.info(f"Resuming, skipping ${startOffset} bytes")
          innerTarStream.skip(startOffset)
        }
        curInnerEntry = innerTarStream.getNextTarEntry()
        return (curInnerEntry != null)
      }

      def next() : HashMap[String, Any] = {
        val entryBytes =
          ByteBuffer.allocate(curInnerEntry.getSize().asInstanceOf[Int])
        tarStream.inStream.read(entryBytes.array())
        log.debug(f"Read tar entry of ${curInnerEntry.getSize()} bytes")

        val r = new HashMap[String, Any]
        r("name") = curInnerEntry.getName()
        r("classnames") = List(wnid)
        r("data") = entryBytes

        return r
      }
  }
}
