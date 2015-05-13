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

// Must import connector first
import com.datastax.spark._
import com.datastax.spark.connector._
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.cassievede.CVSessionConfig
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.cassievede.caricare.datastream.Datastream
import org.cassievede.caricare.datastream.DatastreamCheckpointer
import scala.concurrent.Future
import scala.collection.mutable.Queue
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

  val log: Log = LogFactory.getLog("DataStreamFactory")

  case class VersionedTask(version: Long, task: Future[Unit])
  val taskQueue: Queue[VersionedTask] = new Queue[VersionedTask]

  lazy val sc = new SparkContext(new SparkConf(true))

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
        f"Using queue depth of ${maxQDepth} " +
        "and chunk size of ${chunkSizeMB} MB")

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

      log.info("Creating a task ...")
      val curChunk = MutableList[HashMap[String, Any]]()


      log.info("... reading records ...")
      var curChunkSizeBytes: Long = 0
      while (data.hasNext && curChunkSizeBytes < chunkSizeMB * 10e6) {
        val r = data.next()
        curChunk += r
        curChunkSizeBytes +=
          r.getOrElse("data", Array[Byte]()).asInstanceOf[Array[Byte]].length
        numRecordsRead += 1
      }
      log.info(
          "... read total " + numRecordsRead + " records " +
          "(current chunk: " + (curChunkSizeBytes / 10e6) + "MB) ...")


      val version =
        if (checkpointer == null) { 1 } else { checkpointer.checkpoint() }

      val task = Future {
        log.info("Loading chunk of " + curChunk.size + " to Cassandra ...")
        sc.parallelize(curChunk)
            .map(
              (r: HashMap[String, Any]) =>
                { CassandraRow.fromMap(r.toMap) })
            .saveToCassandra(
              TableDefs.CVKeyspaceName,
              TableDefs.CVImagesTableName)
        log.info("... done.")
      }
      taskQueue += VersionedTask(version, task)
      log.info("... enqueued task.")
    }
  }

  private def waitUntilQueueHasMaxSize(size: Long) = {
    log.info("... blocking on open tasks ...")
    while (taskQueue.size > size) {
      val entry = taskQueue.dequeue()

      log.info("Blocking on " + entry + "; kill to stop ...")
      Await.result(entry.task, Duration.Inf) // Wait until user kills this process
      log.info("... task complete!")

      if (checkpointer != null) {
        checkpointer.release(entry.version)
        log.info("... released version " + entry.version + '.')
      }
    }
    log.info("... done blocking; queue now has size " + taskQueue.size + "...")
  }
}
