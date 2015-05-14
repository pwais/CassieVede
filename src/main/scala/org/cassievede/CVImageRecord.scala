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
package org.cassievede

import scala.collection.mutable.HashMap
import com.datastax.spark.connector.CassandraRow

object CVImageRecord {

  val colNames =
    List(
      "datasetid",
      "partitionid",
      "id",
      "name",
      "data",
      "classnames",
      "binlabels",
      "tags",
      "width",
      "height",
      "geoid",
      "longitude",
      "latitude",
      "uri",
      "extra")

  def toRow(m: HashMap[String, Any]) : CassandraRow = {
    // Sadly, due to how the Cassandra Connector works,
    // the map must have a key for each colname
    val r = new HashMap[String, Any]
    colNames.foreach {
      c => r(c) = if (m.contains(c)) { m(c) } else { null }
    }
    CassandraRow.fromMap(r.toMap)
  }

}
