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
package org.apache.drill.exec.physical.impl.join;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.categories.OperatorTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.exec.physical.config.HashJoinPOP;
import org.apache.drill.exec.work.filter.BloomFilter;
import org.apache.drill.exec.work.filter.BloomFilterDef;
import org.apache.drill.exec.work.filter.RuntimeFilterDef;
import org.apache.drill.test.PhysicalOpUnitTestBase;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

@Category({SlowTest.class, OperatorTest.class})
public class TestHashJoinJPPD extends PhysicalOpUnitTestBase {

  @SuppressWarnings("unchecked")
  @Test
  public void testBroadcastHashJoin1Cond() {
    List<BloomFilterDef> bloomFilterDefs = new ArrayList<>();
    int numBytes = BloomFilter.optimalNumOfBytes(2600, 0.01);
    BloomFilterDef bloomFilterDef = new BloomFilterDef(numBytes, true, "lft", "rgt");
    bloomFilterDefs.add(bloomFilterDef);
    RuntimeFilterDef runtimeFilterDef = new RuntimeFilterDef(true, false, bloomFilterDefs, false, -1);
    HashJoinPOP joinConf = new HashJoinPOP(null, null,
      Lists.newArrayList(joinCond("lft", "EQUALS", "rgt")), JoinRelType.INNER, runtimeFilterDef);
    operatorFixture.getOptionManager().setLocalOption("exec.hashjoin.num_partitions", 4);
    operatorFixture.getOptionManager().setLocalOption("exec.hashjoin.num_rows_in_batch", 64);
    operatorFixture.getOptionManager().setLocalOption("exec.hashjoin.max_batches_in_memory", 8);
    operatorFixture.getOptionManager().setLocalOption("exec.hashjoin.enable.runtime_filter", true);
    // Put some duplicate values
    List<String> leftTable = Lists.newArrayList("[{\"lft\": 0, \"a\" : \"a string\"}]",
      "[{\"lft\": 0, \"a\" : \"a different string\"},{\"lft\": 0, \"a\" : \"yet another\"}]");
    List<String> rightTable = Lists.newArrayList("[{\"rgt\": 0, \"b\" : \"a string\"}]",
      "[{\"rgt\": 0, \"b\" : \"a different string\"},{\"rgt\": 0, \"b\" : \"yet another\"}]");
    int numRows = 2500;
    for ( int cnt = 1; cnt <= numRows; cnt++ ) {
      leftTable.add("[{\"lft\": " + cnt + ", \"a\" : \"a string\"}]");
    }
    legacyOpTestBuilder()
      .physicalOperator(joinConf)
      .inputDataStreamsJson(Lists.newArrayList(leftTable,rightTable))
      .baselineColumns("lft", "a", "b", "rgt")
      .expectedTotalRows(  9 )
      .go();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testBroadcastHashJoin2Cond() {
    List<BloomFilterDef> bloomFilterDefs = new ArrayList<>();
    int numBytes = BloomFilter.optimalNumOfBytes(2600, 0.01);
    BloomFilterDef bloomFilterDef = new BloomFilterDef(numBytes, true, "lft", "rgt");
    BloomFilterDef bloomFilterDef1 = new BloomFilterDef(numBytes, true, "a", "b");
    bloomFilterDefs.add(bloomFilterDef);
    bloomFilterDefs.add(bloomFilterDef1);
    RuntimeFilterDef runtimeFilterDef = new RuntimeFilterDef(true, false, bloomFilterDefs, false, -1);
    HashJoinPOP joinConf = new HashJoinPOP(null, null,
      Lists.newArrayList(joinCond("lft", "EQUALS", "rgt"), joinCond("a", "EQUALS", "b")), JoinRelType.INNER, runtimeFilterDef);
    operatorFixture.getOptionManager().setLocalOption("exec.hashjoin.num_partitions", 4);
    operatorFixture.getOptionManager().setLocalOption("exec.hashjoin.num_rows_in_batch", 128);
    operatorFixture.getOptionManager().setLocalOption("exec.hashjoin.max_batches_in_memory", 8);
    operatorFixture.getOptionManager().setLocalOption("exec.hashjoin.enable.runtime_filter", true);
    // Put some duplicate values
    List<String> leftTable = Lists.newArrayList("[{\"lft\": 0, \"a\" : \"a string\"}]",
      "[{\"lft\": 0, \"a\" : \"a different string\"},{\"lft\": 0, \"a\" : \"yet another\"}]");
    List<String> rightTable = Lists.newArrayList("[{\"rgt\": 0, \"b\" : \"a string\"}]",
      "[{\"rgt\": 0, \"b\" : \"a different string\"},{\"rgt\": 0, \"b\" : \"yet another\"}]");
    int numRows = 2500;
    for ( int cnt = 1; cnt <= numRows; cnt++ ) {
      leftTable.add("[{\"lft\": " + cnt + ", \"a\" : \"a string\"}]");
    }
    legacyOpTestBuilder()
      .physicalOperator(joinConf)
      .inputDataStreamsJson(Lists.newArrayList(leftTable,rightTable))
      .baselineColumns("lft", "a", "b", "rgt")
      .expectedTotalRows(  3 )
      .go();
  }

}
