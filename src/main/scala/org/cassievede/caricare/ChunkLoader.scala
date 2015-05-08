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

import org.apache.spark.SparkContext
import scala.collection.mutable.HashMap
import com.datastax.spark.connector.CassandraRow
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ChunkLoader {
  val log: Logger = LoggerFactory.getLogger("ChunkLoader")

  lazy val sc = new SparkContext()

  def load(recs: Seq[HashMap[String, Any]], keyspace: String, table: String) = {
    log.info("Loading chunk of " + recs.size + " to Cassandra ...")
    sc.parallelize(recs)
      .map(
        (r: HashMap[String, Any]) => {
            CassandraRow.fromMap(r.asInstanceOf[Map[String, Any]])
          })
      .saveToCassandra(keyspace, table)
    log.info("... done.")
  }
}
