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
package org.apache.drill.exec.planner.sql.handlers;

import org.apache.drill.categories.SqlTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SqlTest.class)
public class ResetOptionHandlerTest extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher).maxParallelization(1));
  }

  @Test
  public void testReset() throws Exception {
    int defaultValue = Integer.valueOf(client.queryBuilder()
        .sql("SELECT val from sys.options where name = '%s' limit 1", ExecConstants.CODE_GEN_EXP_IN_METHOD_SIZE)
        .singletonString());

    int testValue = defaultValue + 55;

    try {
      client.alterSession(ExecConstants.CODE_GEN_EXP_IN_METHOD_SIZE, testValue);
      client.testBuilder()
          .sqlQuery("select name, val from sys.options where name = '%s'", ExecConstants.CODE_GEN_EXP_IN_METHOD_SIZE)
          .unOrdered()
          .baselineColumns("name", "val")
          .baselineValues(ExecConstants.CODE_GEN_EXP_IN_METHOD_SIZE, String.valueOf(testValue))
          .go();

      client.resetSession(ExecConstants.CODE_GEN_EXP_IN_METHOD_SIZE);
      client.testBuilder()
          .sqlQuery("select name, val from sys.options where name = '%s'", ExecConstants.CODE_GEN_EXP_IN_METHOD_SIZE)
          .unOrdered()
          .baselineColumns("name", "val")
          .baselineValues(ExecConstants.CODE_GEN_EXP_IN_METHOD_SIZE, String.valueOf(defaultValue))
          .go();
    } finally {
      client.resetSession(ExecConstants.CODE_GEN_EXP_IN_METHOD_SIZE);
    }
  }
}
