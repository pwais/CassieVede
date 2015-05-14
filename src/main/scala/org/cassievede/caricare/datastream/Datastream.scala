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

import scala.collection.mutable.HashMap
import java.io.File

/**
 * A Datastream is an iterator that generates records following the
 * schema of the cassievede.image table.  Furthermore, a Datastream
 * is *Serializable* such that it can be resumed after a crash.
 * I.e.  the iterator should provide a means of serializing its state.
 */
trait Datastream extends Iterator[HashMap[String, Any]] with Serializable

trait DatastreamCheckpointer {

  // Checkpoint the wrapped Datastream and return a version number
  def checkpoint() : Long

  // Release a version of the wrapped Datastream (i.e. we've successfully
  // inserted records from it
  def release(version: Long) : Unit

  // Save state to (or resume from) this local path.  Return true on success
  def useLocalPath(loc: File) : Boolean

  def currentStream() : Datastream

}
