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
package org.cassievede.caricare

import java.io.File
import com.google.common.base.Preconditions.checkNotNull
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.FileInputStream
import org.apache.commons.io.FilenameUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class ResumableTarStream(tf: File)
  extends Iterator[TarArchiveEntry] with Serializable {

  val log: Log = LogFactory.getLog("CheckpointableTarStream")

  val inStream = {
    checkNotNull(tf)
    log.info(f"Reading from ${tf.getPath()}")
    val ext = FilenameUtils.getExtension(tf.getPath())
    val gz = List("tar.gz", "tgz", "gz", "zip")
    val baseStream = if (gz.contains(ext)) {
      new GzipCompressorInputStream(new FileInputStream(tf))
    } else {
      new FileInputStream(tf)
    }
    new TarArchiveInputStream(baseStream)
  }

  var bytesRead :Long = 0
  @transient var pos :Long = 0
  @transient var mNext :TarArchiveEntry = null

  def hasNext() : Boolean = {
    if (pos < bytesRead) {
      log.info(f"Resuming from ${tf.getPath()}; skipping ${bytesRead} bytes")
      inStream.skip(bytesRead)
      pos = bytesRead
    }
    mNext = inStream.getNextTarEntry()
    return mNext == null
  }

  def next() : TarArchiveEntry = {
    pos += mNext.getSize()
    bytesRead = pos
    mNext
  }

}
