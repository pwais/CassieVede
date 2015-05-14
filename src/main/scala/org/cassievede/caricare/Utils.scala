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
import java.net.URL

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.TimeoutException
import scala.concurrent.duration.DurationInt

import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.exception.ExceptionUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

import com.google.common.base.Charsets
import com.google.common.base.Preconditions.checkNotNull
import com.google.common.hash.Hashing

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
    val hashCode =
      Hashing
        .md5()
        .newHasher()
        .putString(u, Charsets.UTF_8)
        .hash()
        .toString()
    val fname = u.substring(u.lastIndexOf('/') + 1, u.size)
        // NB FilenameUtils.getBaseName() is actually broken for fname.tar.gz
    val dest = FileUtils.getFile(cvTempRoot, hashCode, fname)

    if (!dest.exists()) {
      // Download the file and log progress every second or so
      log.info(f"Downloading ${u} to ${dest.toString()} ... ")
      val dl = Future { FileUtils.copyURLToFile(new URL(u), dest) }
      var working = true
      while (working) {
        try {
          Await.result(dl, 1 second)
          working = false
        } catch {
          case te: TimeoutException => {
            val sz =
              FileUtils.getFile(dest).length().asInstanceOf[Float] * 1e-6
            log.info(f"... ${dest} downloaded ${sz} MB ...")
          }
          case e: Exception => {
            log.error(
              f"Error while trying to fetch ${u}: ${e} \n" +
              ExceptionUtils.getStackTrace(e))
            working = false
          }
        }
      }
      log.info("... done.")
    }

    dest
  }

}
