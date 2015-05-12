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

import com.datastax.driver.core.Session
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.QueryBuilder.eq
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import com.datastax.driver.core.Row
import scala.collection.JavaConversions
import com.datastax.driver.core.Cluster

object DBUtil {
  def createCluster(conf: CVSessionConfig) : Cluster = {
    val toks = conf.cassandra.split(":")
    val host = if (!toks(0).isEmpty()) { toks(0) } else { "127.0.0.1" }
    val port = if (toks.size > 1) { toks(0).toInt } else { 9042 }
    return Cluster.builder().addContactPoint(host).withPort(port).build()
  }
}

class DBUtil(session: Session) {

  def createTables() = {
    session.execute(TableDefs.Keyspace)
    session.execute(TableDefs.DatasetTable)
    session.execute(TableDefs.ImageTable)
  }

  def getDatasetIdMap() : BiMap[String, Int] = {
    val result = session.execute(
        QueryBuilder
          .select()
          .all()
          .from(TableDefs.CVKeyspaceName, TableDefs.CVDatasetTableName))
    val m = HashBiMap.create[String, Int]()
    JavaConversions
      .asScalaIterator[Row](result.iterator)
      .foreach(r => m.put(r.getString("name"), r.getInt("id")))
    return m
  }

  def createDataset(datasetName: String) = {
    val dToID = getDatasetIdMap()
    require(
      !dToID.containsKey(datasetName),
      "Dataset " + datasetName + " already exists!")

    val maxId =
      if (dToID.values().isEmpty()) {
        0
      } else {
        JavaConversions.asScalaSet[Int](dToID.values()).max
      }
    val newId = maxId + 1

    val exp =
      QueryBuilder
      .insertInto(TableDefs.CVKeyspaceName, TableDefs.CVDatasetTableName)
      .value("name", datasetName)
      .value("id", newId)
    session.execute(exp)
  }

  def dropDataset(datasetName: String) = {
    val dToID = getDatasetIdMap()
    val datasetId = dToID.get(datasetName)
    session.execute(
        QueryBuilder
        .delete()
        .all()
        .from(TableDefs.CVKeyspaceName, TableDefs.CVDatasetTableName)
        .where(QueryBuilder.eq("name", datasetName)))
    session.execute(
        QueryBuilder
        .delete()
        .all()
        .from(TableDefs.CVKeyspaceName, TableDefs.CVImagesTableName)
        .where(QueryBuilder.eq("datasetid", datasetId)))
  }

}

