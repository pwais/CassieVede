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

import org.cassievede.CVSessionConfig
import org.cassievede.caricare.Utils
import org.cassievede.caricare.datastream.Datastream
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.Log
import scala.collection.mutable.Queue
import scala.collection.mutable.HashMap
import org.apache.commons.io.IOUtils

/**
 * Generate data from the synthetic characters Street View Text / PLEX project.
 * FMI see http://vision.ucsd.edu/~kai/grocr/
 *
 * This dataset is *not* the Street View Text labeled scenes, but rather
 * the synthetic character data used in the project.  Noted in more detail
 * here: https://github.com/shiaokai/plex/blob/0c96a5662f964ac2f87dea0e931e6bde8f217fdb/matlab/README#L63
 *
 * The zip file contains three directories:
 *  * clfs -- Trained random fern models in matlab format (ignore)
 *  * train -- Training images
 *  * test -- Testing images
 *
 *  Importable image entries have paths like:
 *    test/charHard/-a/I00004.png
 *  where:
 *    - charHard is some sort of release dataset tag
 *    - '-a' is the classname; the negative sign indicates a
 *      lowercase character (note that some filesystems are not
 *      case-sensitive)
 *    - Ixxxx.png is the image name; image numbers start from 0 for each class
 */
object SVTSynthChars extends StandardDataset {

  val log: Log = LogFactory.getLog("SVTSynthChars")

  def datasetNames(conf: CVSessionConfig) =
    List(
      "ucsd.svt.synthchars.test",
      "ucsd.svt.synthchars.train")

  val SVTSynthCharsDist =
    "http://vision.ucsd.edu/~kai/grocr/release/synth_release.zip"
  lazy val sourceFile = Utils.downloadToTemp(SVTSynthCharsDist)

  def stream(conf: CVSessionConfig) : Datastream = new SVTSynthChars()

  class SVTSynthChars extends Datastream {

    var n: Long = 0

    @transient lazy val rootZip = new ZipFile(sourceFile)
    lazy val entrynamesToImport = {
      val q = new Queue[String]
      val iterEntries = rootZip.getEntries()
      while (iterEntries.hasMoreElements()) {
        val curEntry = iterEntries.nextElement()
        if (curEntry.getName().endsWith(".png")) {
          q += curEntry.getName()
        } else {
          log.info(f"Not importing file ${curEntry.getName()}")
        }
      }

      log.info(f"Found ${q.size} importable entries")
      q
    }

    def hasNext(): Boolean = !entrynamesToImport.isEmpty

    def next() : HashMap[String, Any] = {
      val entryname = entrynamesToImport.front

      val r = new HashMap[String, Any]
      r("id") = n
      r("name") = entryname
      r("datasetName") = {
        if (entryname.startsWith("train")) {
          "ucsd.svt.synthchars.train"
        } else if (entryname.startsWith("test")) {
          "ucsd.svt.synthchars.test"
        } else {
          assert(false, f"Entry ${entryname} has undecipherable dataset")
          ""
        }
      }
      r("classnames") = List(getClassname(entryname))
      r("data") = IOUtils.toByteArray(
                    rootZip.getInputStream(
                      rootZip.getEntry(entryname)))

      entrynamesToImport.dequeue()
      n += 1

      return r
    }

    ///
    /// Utils
    ///

    private def getClassname(entryname: String) : String = {
      val toks = entryname.split('/')
      assert(toks.size == 4, f"Don't know how to handle entry ${entryname}")
      val charname = toks(2)
      if (charname.startsWith("-")) {
        // Trim the leading negative sign and return just the lowercase char
        charname.substring(1)
      } else {
        charname
      }
    }
  }
}
