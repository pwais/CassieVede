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
package org.cassievede.caricare.datastream

import org.cassievede.CVSessionConfig
import java.io.File
import scala.collection.immutable.HashSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.cassievede.CVSessionConfig
import java.util.UUID

object DataStreamFactory {

  val log :Logger = LoggerFactory.getLogger("DataStreamFactory")

  def createStream(conf: CVSessionConfig) : Datastream = {

    // Handle classname-mapping directories
    if (!conf.cnameDirs.isEmpty) {
      val acceptedExtensions = conf.cnameDirExts match {
        case Seq("") => HashSet[String]()
        case _ => HashSet[String]() ++ conf.cnameDirExts
      }

      val createDS = (f :File) => {
        new ClassnameMappingDir(f, acceptedExtensions)
      }

      var iter = createDS(conf.cnameDirs.head)
      for (f <- conf.cnameDirs.tail) { iter ++ createDS(f) }

      log.debug("Created " + conf.cnameDirs.size + " ClassnameMappingDirs")
      return iter
    }

    // Handle datasets
    var d = conf.dataset match {
      case "TODO" => null
      case _ => null
    }
    return d
  }

  def createCheckpointer(
      conf: CVSessionConfig,
      s: Datastream = null)
          : DatastreamCheckpointer = {

    if (!(conf.resumeCache || conf.cache)) { return null }

    val dbPath =
      new File(
          conf.cacheDir,
          "Leveldb_" + UUID.randomUUID().toString())
    val c = new LocalLeveldbDSCheckpointer(s)
    c.useLocalPath(dbPath)
    return c
  }
}
