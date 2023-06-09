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
package org.apache.drill.exec.store.parquet;

import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.junit.BeforeClass;
import org.junit.Test;

import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestParquetLimitPushDown extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    dirTestWatcher.copyResourceToRoot(Paths.get("parquet", "multirowgroup.parquet"));
    dirTestWatcher.copyResourceToRoot(Paths.get("parquet", "users"));
    startCluster(builder);
  }

  @Test
  public void testMultipleFiles() throws Exception {
    String query = "select * from dfs.`parquet/users` limit 1";
    QueryBuilder.QuerySummary summary = queryBuilder().sql(query).run();
    assertTrue(summary.succeeded());
    assertEquals(1, summary.recordCount());

    String plan = queryBuilder().sql(query).explainText();
    assertThat(plan, containsString("numRowGroups=1"));
  }

  @Test
  public void testMultipleRowGroups() throws Exception {
    String query = "select * from dfs.`parquet/multirowgroup.parquet` limit 1";
    QueryBuilder.QuerySummary summary = queryBuilder().sql(query).run();
    assertTrue(summary.succeeded());
    assertEquals(1, summary.recordCount());

    String plan = queryBuilder().sql(query).explainText();
    assertThat(plan, containsString("numRowGroups=1"));
  }

  @Test
  public void testLimitZero() throws Exception {
    String query = "select * from dfs.`parquet/users` limit 0";
    QueryBuilder.QuerySummary summary = queryBuilder().sql(query).run();
    assertTrue(summary.succeeded());
    assertEquals(0, summary.recordCount());

    String plan = queryBuilder().sql(query).explainText();
    assertThat(plan, containsString("numRowGroups=1"));
  }

}
