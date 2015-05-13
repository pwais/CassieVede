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

import com.google.common.base.Preconditions.checkNotNull
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.fusesource.leveldbjni.JniDBFactory._
import org.iq80.leveldb.DB
import org.iq80.leveldb.Options
import java.io.File
import org.iq80.leveldb.ReadOptions
import org.apache.commons.lang3.SerializationUtils
import org.iq80.leveldb.CompressionType
import org.iq80.leveldb.DBException

class LocalLeveldbDSCheckpointer(stream :Datastream = null) extends DatastreamCheckpointer  {

  val log: Log = LogFactory.getLog("ChunkLoader")
  var db :DB = null
  var curVersion :Long = 0
  var s :Datastream = stream

  override def finalize() : Unit = { if (db != null) { db.close() } }

  def useLocalPath(loc: File) :Boolean = {
    checkNotNull(loc)
    if (db != null) { db.close() }

    val ldbOpts = new Options()
    ldbOpts.createIfMissing(true)
    ldbOpts.compressionType(CompressionType.SNAPPY)
    ldbOpts.cacheSize(10 * 1048576); // 10MB cache
    db = factory.open(loc, ldbOpts)
    log.debug("Using leveldb at " + loc.toString())

    // If we're not resuming, we're done ...
    if (s != null) {
      curVersion = 0
      return true
    }

    log.info("Resuming from " + loc.toString() + " ... ")

    // ... else find the oldest version ...
    curVersion = Long.MaxValue
    val ldbROpts = new ReadOptions()
    ldbROpts.fillCache(false)
    ldbROpts.verifyChecksums(true)
    val iterReadKeys = db.iterator(ldbROpts)
    while (iterReadKeys.hasNext()) {
      val key =
        SerializationUtils
        .deserialize(iterReadKeys.next().getKey())
        .asInstanceOf[Long]
      curVersion = Math.min(curVersion, key)
    }
    log.info("... oldest version is " + curVersion + " ... ")

    // ... free anything newer ...
    ldbROpts.verifyChecksums(false)
    val iterDel = db.iterator(ldbROpts)
    val batch = db.createWriteBatch()
    var numNewerVersions :Long = 0
    while (iterDel.hasNext()) {
      val keyBytes = iterDel.next().getKey()
      val key = SerializationUtils.deserialize(keyBytes).asInstanceOf[Long]
      if (key > curVersion) {
        batch.delete(keyBytes)
        numNewerVersions += 1
      }
    }
    db.write(batch)
    batch.close()
    log.info("... released " + numNewerVersions + " newer versions ... ")

    // ... and resume from oldest checkpoint
    val sAtV = readVersion(curVersion)
    if (sAtV == null) {
      log.error("Read error; failed to restore from version " + curVersion)
      curVersion = 0
      return false
    }
    s = sAtV
    log.info("... restored from version " + curVersion)
    curVersion += 1 // don't overwrite this version
    return true
  }

  def checkpoint() : Long = {
    writeVersion(curVersion, s)
    log.info("Checkpointed version " + curVersion)
    val written = curVersion
    curVersion += 1
    return written
  }

  def release(v: Long) : Unit = {
    checkNotNull(db)
    db.delete(SerializationUtils.serialize(v))
    log.info("Released version " + curVersion)
  }

  private def readVersion(v: Long) : Datastream = {
    checkNotNull(db)
    try {
      val streamBytes = db.get(SerializationUtils.serialize(v))
      log.debug("Read version " + v)
      return SerializationUtils.deserialize(streamBytes).asInstanceOf[Datastream]
    } catch {
      case e :DBException => {
        log.error("Could not read version " + v)
        return null
      }
    }
  }

  private def writeVersion(v: Long, s: Datastream) = {
    checkNotNull(s)
    checkNotNull(db)
    db.put(
        SerializationUtils.serialize(v),
        SerializationUtils.serialize(s))
    log.debug("Wrote version " + v)
  }
}
