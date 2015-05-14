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

import com.google.common.base.Preconditions.checkNotNull
import java.io.File
import java.net.URI
import org.apache.commons.io.FileUtils
import java.nio.ByteBuffer
import scala.collection.mutable.HashMap
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import com.google.common.io.Files
import java.security.MessageDigest
import com.google.common.hash.Hashing
import com.google.common.base.Charsets

object Utils {

  val log: Log = LogFactory.getLog("Utils")

  lazy val cvTempRoot: File = {
    val f =
      FileUtils.getFile(FileUtils.getTempDirectory(), "CassieVedeUtil")
    FileUtils.forceMkdir(f)
    log.debug(f"CassieVedeUtil temp directory is ${f.toString()}")
    f
  }

  def downloadToTemp(u: String) : File = {
    checkNotNull(u)
    val uri = new URI(u)

    val hashCode =
      Hashing
        .md5()
        .newHasher()
        .putString(uri.toString(), Charsets.UTF_8)
        .hash()
        .toString()
    val dest = FileUtils.getFile(cvTempRoot, hashCode, uri.getFragment())

    if (!dest.exists()) {
      log.info(f"Downloading ${uri} to ${dest.toString()} ... ")
      FileUtils.copyURLToFile(uri.toURL(), dest)
      log.info("... done.")
    }

    dest
  }

}
