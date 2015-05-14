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

import java.io.File
import java.util.UUID

import scala.collection.immutable.HashSet
import scala.collection.mutable.HashMap

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cassievede.CVSessionConfig
import org.cassievede.DBUtil
import org.cassievede.caricare.stddata.CIFAR10
import org.cassievede.caricare.stddata.StandardDataset

import com.google.common.collect.BiMap

object DataStreamFactory {

  val log :Log = LogFactory.getLog("DataStreamFactory")

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
    val success = c.useLocalPath(dbPath)
    require(success, "Failed to set up / resume checkpointer")
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
    val d: StandardDataset = conf.stdDataset match {
      case "cifar10" => CIFAR10
      case _ => {
        log.error(f"Unsupported dataset ${conf.stdDataset}")
        return null
      }
    }

    // Create all induced datasets
    d.datasetNames().foreach {
      name => DBUtil.safeDBUtilExec(conf, dbutil => dbutil.createDataset(name))
    }

    return d.stream()
  }

  // Wrap `d` in required adapters
  private def wrapStream(conf: CVSessionConfig, d: Datastream) : Datastream = {
    return new LogProgress(new SetPartitionId(new DatasetNameToId(conf, d)))
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

    val datasetToID: BiMap[String, Int] =
      DBUtil.safeDBUtilExec(conf, dbutil => dbutil.getDatasetIdMap())

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

  class LogProgress(s: Datastream, interval: Long = 1000) extends Datastream {
    var i: Long = 0
    var b: Long = 0

    def hasNext(): Boolean = s.hasNext

    def next() : HashMap[String, Any] = {
      val r = s.next()
      i += 1
      b += r.getOrElse("data", Array[Byte]()).asInstanceOf[Array[Byte]].length
      if ((i % interval) == 0) {
        log.info(f"Generated ${i} records of approx ${b * 1e-6}MB")
      }
      return r
    }
  }
}
