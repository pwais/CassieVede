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

import org.cassievede.CVSessionConfig
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.cassievede.caricare.datastream.Datastream
import org.cassievede.caricare.datastream.DatastreamCheckpointer
import scala.concurrent.Future
import scala.collection.mutable.Queue
import org.apache.spark.SparkContext
import scala.collection.mutable.HashMap
import scala.collection.mutable.MutableList
import org.cassievede.TableDefs
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration

class LoaderQueue(
    conf :CVSessionConfig,
    data :Datastream,
    checkpointer :DatastreamCheckpointer = null) {

  val log: Logger = LoggerFactory.getLogger("DataStreamFactory")

  case class VersionedTask(version: Long, task: Future[Unit])
  val taskQueue: Queue[VersionedTask] = new Queue[VersionedTask]

  lazy val sc = new SparkContext()

  val maxQDepth: Long = conf.sparkQDepth match {
    case x if x >= 1 => x
    case _ => Math.max(1, sc.getExecutorStorageStatus.length)
  }

  val chunkSizeMB: Long = conf.sparkChunkSize match {
    case x if x >= 1 => x
    case _ =>
      ((Runtime.getRuntime().maxMemory().toDouble / 10e6)
          * (1.0 / maxQDepth.toDouble)).toLong
  }

  var numRecordsRead: Long = 0

  def run() = {
    log.info(
        "Using queue dpeth of " + maxQDepth +
        " and chunk size of " + chunkSizeMB + "MB")

    log.info("Loading data ...")
    while (data.hasNext) {
      fillQueue()
      waitUntilQueueHasMaxSize(maxQDepth - 1)
    }

    // Wait for everything to finish
    waitUntilQueueHasMaxSize(0)
    log.info("... done loading!")
  }

  private def fillQueue() = {
    while (data.hasNext && taskQueue.size < maxQDepth) {

      log.debug("Creating a task ...")
      val curChunk = MutableList[HashMap[String, Any]]()


      log.debug("... reading records ...")
      var curChunkSizeBytes: Long = 0
      while (data.hasNext && curChunkSizeBytes < chunkSizeMB * 10e6) {
        val r = data.next()
        curChunk += r
        curChunkSizeBytes +=
          r.getOrElse("data", Array[Byte]()).asInstanceOf[Array[Byte]].length
        numRecordsRead += 1
      }
      log.debug(
          "... read total " + numRecordsRead + " records " +
          "(current chunk: " + (curChunkSizeBytes / 10e6) + "MB) ...")


      val version =
        if (checkpointer == null) { 1 } else { checkpointer.checkpoint() }
      val task = Future {
        val c = new ChunkLoader()
        c.load(curChunk, TableDefs.CVKeyspaceName, TableDefs.CVImagesTableName)
      }
      taskQueue += VersionedTask(version, task)
      log.debug("... enqueued task.")
    }
  }

  private def waitUntilQueueHasMaxSize(size: Long) = {
    log.info("... blocking on open tasks ...")
    while (taskQueue.size >= size) {
      val entry = taskQueue.dequeue()

      log.debug("Blocking on " + entry + "; kill to stop ...")
      Await.ready(entry.task, Duration.Inf) // Wait until user kills this process
      log.debug("... task complete!")

      if (checkpointer != null) { checkpointer.release(entry.version) }
      log.debug("... released version " + entry.version + '.')
    }
    log.info("... done blocking; queue now has size " + taskQueue.size + "...")
  }
}
