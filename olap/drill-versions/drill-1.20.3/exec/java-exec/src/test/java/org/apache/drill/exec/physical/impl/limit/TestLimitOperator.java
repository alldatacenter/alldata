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
package org.apache.drill.exec.physical.impl.limit;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.physical.config.Limit;
import org.apache.drill.test.PhysicalOpUnitTestBase;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

public class TestLimitOperator extends PhysicalOpUnitTestBase {

  @Rule
  public BaseDirTestWatcher baseDirTestWatcher = new BaseDirTestWatcher();

  // DRILL-6474
  @Test
  public void testLimitIntegrationTest() throws Exception {
    final ClusterFixtureBuilder builder = new ClusterFixtureBuilder(baseDirTestWatcher);

    try (ClusterFixture clusterFixture = builder.build();
         ClientFixture clientFixture = clusterFixture.clientFixture()) {
      clientFixture.testBuilder()
        .sqlQuery("select name_s10 from `mock`.`employees_100000` order by name_s10 offset 100")
        .expectsNumRecords(99900)
        .build()
        .run();
    }
  }

  @Test
  public void testLimitMoreRecords() {
    Limit limitConf = new Limit(null, 0, 10);
    List<String> inputJsonBatches = Lists.newArrayList(
      "[{\"a\": 5, \"b\" : 1 }]",
      "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]");
    legacyOpTestBuilder()
      .physicalOperator(limitConf)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b")
      .baselineValues(5l, 1l)
      .baselineValues(5l, 5l)
      .baselineValues(3l, 8l)
      .go();
  }

  @Test
  public void testLimitLessRecords() {
    Limit limitConf = new Limit(null, 0, 1);
    List<String> inputJsonBatches = Lists.newArrayList(
      "[{\"a\": 5, \"b\" : 1 }]",
      "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]");
    legacyOpTestBuilder()
      .physicalOperator(limitConf)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b")
      .baselineValues(5l, 1l)
      .go();
  }

  @Test
  public void testLimitWithOffset() {
    Limit limitConf = new Limit(null, 2, 3);
    List<String> inputJsonBatches = Lists.newArrayList(
      "[{\"a\": 5, \"b\" : 1 }]",
      "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]");
    legacyOpTestBuilder()
      .physicalOperator(limitConf)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b")
      .baselineValues(3l, 8l)
      .go();
  }

  @Test
  public void testLimitWithNoLastRecord() {
    Limit limitConf = new Limit(null, 1, null);
    List<String> inputJsonBatches = Lists.newArrayList(
      "[{\"a\": 5, \"b\" : 1 }]",
      "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]");
    legacyOpTestBuilder()
      .physicalOperator(limitConf)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b")
      .baselineValues(5l, 5l)
      .baselineValues(3l, 8l)
      .go();
  }

  @Test
  public void testLimitWithNegativeOffset() {
    Limit limitConf = new Limit(null, -1, null);
    List<String> inputJsonBatches = Lists.newArrayList(
      "[{\"a\": 5, \"b\" : 1 }]",
      "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]");
    legacyOpTestBuilder()
      .physicalOperator(limitConf)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b")
      .baselineValues(5l, 1l)
      .baselineValues(5l, 5l)
      .baselineValues(3l, 8l)
      .go();
  }

  @Test
  public void testLimitWithNegativeFirstLast() {
    Limit limitConf = new Limit(null, -1, -1);
    List<String> inputJsonBatches = Lists.newArrayList(
      "[{\"a\": 5, \"b\" : 1 }]",
      "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]");
    legacyOpTestBuilder()
      .physicalOperator(limitConf)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b")
      .expectZeroRows()
      .go();
  }

  @Test
  public void testLimitWithOffsetOutOfRange() {
    Limit limitConf = new Limit(null, 10, 20);
    List<String> inputJsonBatches = Lists.newArrayList(
      "[{\"a\": 5, \"b\" : 1 }]",
      "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]");
    legacyOpTestBuilder()
      .physicalOperator(limitConf)
      .inputDataStreamJson(inputJsonBatches)
      .baselineColumns("a", "b")
      .expectZeroRows()
      .go();
  }
}
