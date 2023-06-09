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
package org.apache.drill.exec.store.enumerable;

import org.apache.drill.exec.store.enumerable.plan.EnumMockPlugin;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;

public class EnumPluginTest extends ClusterTest {

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));

    EnumMockPlugin.EnumMockStoragePluginConfig config = new EnumMockPlugin.EnumMockStoragePluginConfig();
    config.setEnabled(true);
    cluster.defineStoragePlugin("mocked_enum", config);
    cluster.defineStoragePlugin("mocked_enum2", config);
  }

  @Test // DRILL-7972
  public void testIntermConvRuleConvention() throws Exception {
    queryBuilder()
        .sql("select t1.a, t2.a from mocked_enum.mock_enum_table t1 " +
            "join mocked_enum2.mock_enum_table t2 on t1.a=t2.a")
        .planMatcher()
        .include("mocked_enum", "mocked_enum2")
        .match();
  }
}
