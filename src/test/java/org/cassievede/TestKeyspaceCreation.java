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

package org.cassievede;

import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.List;

import org.cassandraunit.CassandraCQLUnit;
import org.cassandraunit.dataset.CQLDataSet;
import org.junit.ClassRule;
import org.junit.Test;

import com.datastax.driver.core.ResultSet;

import org.cassievede.TableDefs;

public class TestKeyspaceCreation {
  
  public static class TableDefsCQLDataSet implements CQLDataSet {

    @Override
    public List<String> getCQLStatements() {
      ArrayList<String> s = new ArrayList<String>();
      s.add(TableDefs.Keyspace());
      s.add(TableDefs.DatasetTable());
      s.add(TableDefs.ImageTable());
      return s;
    }

    @Override
    public String getKeyspaceName() {
      return TableDefs.CVKeyspaceName();
    }

    @Override
    public boolean isKeyspaceCreation() { return false; }

    @Override
    public boolean isKeyspaceDeletion() { return false; }
  }
  
  @ClassRule
  public static CassandraCQLUnit unit =
      new CassandraCQLUnit(new TestKeyspaceCreation.TableDefsCQLDataSet());
  
  @Test
  public void basicTest() {
    ResultSet result =
        unit.session.execute(
            "SELECT * FROM cassievede.image WHERE datasetid=0 and partitionid=0");
    assertFalse(result.iterator().hasNext());
//    String v = result.iterator().next().getString("value");
//    assertEquals("myValue01", v);
//    System.out.println(v);
//    System.out.println(v);
//    System.out.println(v);
//    
//    System.out.println(
//        Cluster.builder().addContactPoint("127.0.0.1").withPort(9142).build()
//        .connect().execute("describe tastKeyspaceName"));
  }
}
