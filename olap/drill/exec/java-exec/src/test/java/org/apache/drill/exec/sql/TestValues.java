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
package org.apache.drill.exec.sql;

import org.apache.drill.categories.SqlTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

@Category(SqlTest.class)
public class TestValues extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testSingle() throws Exception {
    assertEquals("A", queryBuilder().sql("values('A')").singletonString());
  }

  @Test
  public void testNamed() throws Exception {
    testBuilder()
      .sqlQuery("select col from (values('A'), ('B')) t(col)")
      .unOrdered()
      .baselineColumns("col")
      .baselineValues("A")
      .baselineValues("B")
      .go();
  }

  @Test
  public void testDistribution() throws Exception {
    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      testBuilder()
        .sqlQuery("select id, name from (values(1, 'A'), (2, 'B')) t(id, name)")
        .unOrdered()
        .baselineColumns("id", "name")
        .baselineValues(1L, "A")
        .baselineValues(2L, "B")
        .go();
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
    }
  }
}
