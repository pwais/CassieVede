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

import scala.collection.JavaConversions

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.spark.SparkConf

import com.datastax.driver.core.Row
import com.datastax.driver.core.Session
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.spark.connector.cql.CassandraConnector
import com.datastax.spark.connector.cql.CassandraConnectorConf
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap

object DBUtil {
  def safeSessionExec[T](conf: CVSessionConfig, f: Session => T) : T = {
    CassandraConnector(DBUtil.toSparkConf(conf)).withSessionDo {
      session => f(session)
    }
  }

  def safeDBUtilExec[T](conf: CVSessionConfig, f: DBUtil => T) : T = {
    CassandraConnector(DBUtil.toSparkConf(conf)).withSessionDo {
      session => {
        val d = new DBUtil(session)
        f(d)
      }
    }
  }

  def toSparkConf(conf: CVSessionConfig) : SparkConf = {
//    val toks = conf.cassandra.split(":")
//    val host = if (!toks(0).isEmpty()) { toks(0) } else { "127.0.0.1" }
//    val port = if (toks.size > 1) { toks(0).toInt } else { 9042 }
    val sconf = new SparkConf(loadDefaults = true)
//    sconf.set(
//        CassandraConnectorConf.CassandraConnectionHostProperty, host)
//    sconf.set(
//        CassandraConnectorConf.CassandraConnectionNativePortProperty, "" + port)
    return sconf
  }
}

class DBUtil(session: Session) {

  val log :Log = LogFactory.getLog("DBUtil")

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

  def createDataset(datasetName: String) : Unit = {
    val dToID = getDatasetIdMap()
    if (dToID.containsKey(datasetName)) {
      log.debug(f"Dataset ${datasetName} already exists")
      return
    }

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

