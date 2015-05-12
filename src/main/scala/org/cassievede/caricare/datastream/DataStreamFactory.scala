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

import org.cassievede.DBUtil
import org.cassievede.CVSessionConfig
import java.io.File
import java.util.UUID
import scala.collection.immutable.HashSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.cassievede.CVSessionConfig
import scala.collection.mutable.HashMap
import com.google.common.collect.BiMap
import com.datastax.driver.core.Cluster

object DataStreamFactory {

  val log :Logger = LoggerFactory.getLogger("DataStreamFactory")

  def createStream(conf: CVSessionConfig) : Datastream = {

    // Does the user want a ClassnameMappingDirStream?
    var d :Datastream = createClassnameMappingDirStream(conf)

    // Does the user want a standard dataset?
    if (d == null) { d = createStdDataset(conf) }

    if (d == null) {
      log.warn(
        "Failed to deduce a Datastream to build " +
        "(only expected if resuming from cache)")
      return null
    } else {
      return wrapStream(conf, d)
    }
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


  ///
  /// Factory Helpers
  ///

  private def createClassnameMappingDirStream(conf: CVSessionConfig) : Datastream = {
    if (conf.cnameDirs.isEmpty) { return null }

    require(!conf.dataset.isEmpty, "Using --cname-dirs requires --dataset")

    val acceptedExtensions = conf.cnameDirExts match {
      case Seq("") => HashSet[String]()
      case _ => HashSet[String]() ++ conf.cnameDirExts
    }

    val createDS = (f :File) => {
      new ClassnameMappingDirStream(f, acceptedExtensions)
    }

    var iter = createDS(conf.cnameDirs.head)
    for (f <- conf.cnameDirs.tail) { iter ++ createDS(f) }

    log.debug("Created " + conf.cnameDirs.size + " ClassnameMappingDirs")

    // For this stream, all records have the same dataset
    return new FillDatasetField(conf.dataset, iter)
  }

  private def createStdDataset(conf: CVSessionConfig) : Datastream = {
    return null // TODO
  }

  // Wrap `d` in required adapters
  private def wrapStream(conf: CVSessionConfig, d: Datastream) : Datastream = {
    return new SetPartitionId(new DatasetNameToId(conf, d))
  }


  ///
  /// Utils
  ///

  class FillDatasetField(
    datasetName: String,
    s: Datastream) extends Datastream {

    def hasNext(): Boolean = s.hasNext

    def next() : HashMap[String, Any] = {
      val r = s.next()
      r("datasetName") = datasetName
      return r
    }
  }

  class DatasetNameToId(
    conf: CVSessionConfig,
    s: Datastream) extends Datastream {

    val datasetToID: BiMap[String, Int] = {
      var cluster: Cluster = null
      var m: BiMap[String, Int] = null
      try {
        cluster = DBUtil.createCluster(conf)
        val d = new DBUtil(cluster.newSession())
        m = d.getDatasetIdMap()
      } finally {
        cluster.close()
      }
      m
    }

    def hasNext(): Boolean = s.hasNext

    def next() : HashMap[String, Any] = {
      val r = s.next()
      r("datasetid") = datasetToID.get(r("datasetName"))
      r.remove("datasetName")
      return r
    }
  }

  class SetPartitionId(s: Datastream) extends Datastream {

    var n: Long = 0
    var curPartitionid :Int = 0

    def hasNext(): Boolean = s.hasNext

    def next() : HashMap[String, Any] = {
      val r = s.next()

      // Cassandra only allows ~2B values per key
      // http://wiki.apache.org/cassandra/CassandraLimitations
      n += 1
      if (n >= 2e9) {
        n = 0
        curPartitionid += 1
      }

      r("partitionid") = curPartitionid
      return r
    }
  }
}
