/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.cassandra;

import org.junit.Test;

public class CassandraPlanTest extends BaseCassandraTest {

  @Test
  public void testProjectPushDown() throws Exception {
    queryBuilder()
        .sql("select n_name, n_nationkey from cassandra.test_keyspace.`nation`")
        .planMatcher()
        .include("cassandra=.*n_name.*n_nationkey")
        .exclude("\\*\\*")
        .match();
  }

  @Test
  public void testFilterPushDown() throws Exception {
    queryBuilder()
        .sql("select n_name, n_nationkey from cassandra.test_keyspace.`nation` where n_nationkey = 0")
        .planMatcher()
        .include("CassandraFilter")
        .match();
  }

  @Test
  public void testFilterPushDownWithJoin() throws Exception {
    String query = "select * from cassandra.test_keyspace.`nation` e\n" +
        "join cassandra.test_keyspace.`nation` s on e.n_name = s.n_name where e.n_nationkey = 'algeria'";

    queryBuilder()
        .sql(query)
        .planMatcher()
        .include("CassandraFilter")
        .match();
  }

  @Test
  public void testLimitPushDown() throws Exception {
    queryBuilder()
        .sql("select n_nationkey from cassandra.test_keyspace.`nation` limit 3")
        .planMatcher()
        .include("CassandraLimit.*fetch")
        .match();
  }
}
