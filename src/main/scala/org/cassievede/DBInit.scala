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

import com.datastax.driver.core.Cluster

object DBUtil {
  def CreateCassie(host: String, port: Int) : Unit = {
    val cluster =
      Cluster.builder().addContactPoint(host).withPort(port).build()

//    if (cluster.describeKeyspace(Defs.CVKeyspaceName)) return



    val session = cluster.newSession()

    val result = session.execute("DESCRIBE KEYSPACE " + Defs.CVKeyspaceName)


//    val session = cluster.connect(Defs.CVKeyspaceName)
  }
}
