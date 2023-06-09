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
package org.apache.drill.exec.physical.unit;

import static org.apache.drill.test.TestBuilder.mapOf;

import java.util.List;

import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.ComplexToJson;
import org.apache.drill.exec.physical.config.ExternalSort;
import org.apache.drill.exec.physical.config.Filter;
import org.apache.drill.exec.physical.config.HashAggregate;
import org.apache.drill.exec.physical.config.HashJoinPOP;
import org.apache.drill.exec.physical.config.MergeJoinPOP;
import org.apache.drill.exec.physical.config.MergingReceiverPOP;
import org.apache.drill.exec.physical.config.Project;
import org.apache.drill.exec.physical.config.StreamingAggregate;
import org.apache.drill.exec.physical.config.TopN;
import org.apache.drill.exec.physical.config.FlattenPOP;
import org.apache.drill.exec.planner.physical.AggPrelBase;
import org.apache.drill.test.LegacyOperatorTestBuilder;
import org.apache.drill.test.PhysicalOpUnitTestBase;
import org.junit.Ignore;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class BasicPhysicalOpUnitTest extends PhysicalOpUnitTestBase {

  @Test
  public void testSimpleProject() {
    Project projectConf = new Project(parseExprs("x+5", "x"), null);
    List<String> jsonBatches = Lists.newArrayList(
        "[{\"x\": 5 },{\"x\": 10 }]",
        "[{\"x\": 20 },{\"x\": 30 },{\"x\": 40 }]");
    legacyOpTestBuilder()
        .physicalOperator(projectConf)
        .inputDataStreamJson(jsonBatches)
        .baselineColumns("x")
        .baselineValues(10l)
        .baselineValues(15l)
        .baselineValues(25l)
        .baselineValues(35l)
        .baselineValues(45l)
        .go();
  }

  @Test
  public void testProjectComplexOutput() {
    Project projectConf = new Project(parseExprs("convert_from(json_col, 'JSON')", "complex_col"), null);
    List<String> jsonBatches = Lists.newArrayList(
        "[{\"json_col\": \"{ \\\"a\\\" : 1 }\"}]",
        "[{\"json_col\": \"{ \\\"a\\\" : 5 }\"}]");
    legacyOpTestBuilder()
        .physicalOperator(projectConf)
        .inputDataStreamJson(jsonBatches)
        .baselineColumns("complex_col")
        .baselineValues(mapOf("a", 1l))
        .baselineValues(mapOf("a", 5l))
        .go();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSimpleHashJoin() {
    HashJoinPOP joinConf = new HashJoinPOP(null, null, Lists.newArrayList(joinCond("x", "EQUALS", "x1")), JoinRelType.LEFT, null);
    // TODO - figure out where to add validation, column names must be unique, even between the two batches,
    // for all columns, not just the one in the join condition
    // TODO - if any are common between the two, it is failing in the generated setup method in HashJoinProbeGen
    List<String> leftJsonBatches = Lists.newArrayList(
        "[{\"x\": 5, \"a\" : \"a string\"}]",
        "[{\"x\": 5, \"a\" : \"a different string\"},{\"x\": 5, \"a\" : \"meh\"}]");
    List<String> rightJsonBatches = Lists.newArrayList(
        "[{\"x1\": 5, \"a2\" : \"asdf\"}]",
        "[{\"x1\": 6, \"a2\" : \"qwerty\"},{\"x1\": 5, \"a2\" : \"12345\"}]");
    legacyOpTestBuilder()
        .physicalOperator(joinConf)
        .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches))
        .baselineColumns("x", "a", "a2", "x1")
        .baselineValues(5l, "a string", "asdf", 5l)
        .baselineValues(5l, "a string", "12345", 5l)
        .baselineValues(5l, "a different string", "asdf", 5l)
        .baselineValues(5l, "a different string", "12345", 5l)
        .baselineValues(5l, "meh", "asdf", 5l)
        .baselineValues(5l, "meh", "12345", 5l)
        .go();
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testSimpleMergeJoin() {
    MergeJoinPOP joinConf = new MergeJoinPOP(null, null, Lists.newArrayList(joinCond("x", "EQUALS", "x1")), JoinRelType.LEFT);
    // TODO - figure out where to add validation, column names must be unique, even between the two batches,
    // for all columns, not just the one in the join condition
    List<String> leftJsonBatches = Lists.newArrayList(
        "[{\"x\": 5, \"a\" : \"a string\"}]",
        "[{\"x\": 5, \"a\" : \"a different string\"},{\"x\": 5, \"a\" : \"meh\"}]");
    List<String> rightJsonBatches = Lists.newArrayList(
        "[{\"x1\": 5, \"a2\" : \"asdf\"}]",
        "[{\"x1\": 5, \"a2\" : \"12345\"}, {\"x1\": 6, \"a2\" : \"qwerty\"}]");
    legacyOpTestBuilder()
        .physicalOperator(joinConf)
        .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches))
        .baselineColumns("x", "a", "a2", "x1")
        .baselineValues(5l, "a string", "asdf", 5l)
        .baselineValues(5l, "a string", "12345", 5l)
        .baselineValues(5l, "a different string", "asdf", 5l)
        .baselineValues(5l, "a different string", "12345", 5l)
        .baselineValues(5l, "meh", "asdf", 5l)
        .baselineValues(5l, "meh", "12345", 5l)
        .go();
  }

  @Test
  public void testSimpleHashAgg() {
    HashAggregate aggConf = new HashAggregate(null, AggPrelBase.OperatorPhase.PHASE_1of1, parseExprs("a", "a"), parseExprs("sum(b)", "b_sum"), 1.0f);
    List<String> inputJsonBatches = Lists.newArrayList(
        "[{\"a\": 5, \"b\" : 1 }]",
        "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]");
    legacyOpTestBuilder()
        .physicalOperator(aggConf)
        .inputDataStreamJson(inputJsonBatches)
        .baselineColumns("b_sum", "a")
        .baselineValues(6l, 5l)
        .baselineValues(8l, 3l)
        .go();
  }

  @Test
  public void testSimpleStreamAgg() {
    StreamingAggregate aggConf = new StreamingAggregate(null, parseExprs("a", "a"), parseExprs("sum(b)", "b_sum"));
    List<String> inputJsonBatches = Lists.newArrayList(
        "[{\"a\": 5, \"b\" : 1 }]",
        "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]");
    legacyOpTestBuilder()
        .physicalOperator(aggConf)
        .inputDataStreamJson(inputJsonBatches)
        .baselineColumns("b_sum", "a")
        .baselineValues(6l, 5l)
        .baselineValues(8l, 3l)
        .go();
  }

  @Test
  public void testComplexToJson() {
    ComplexToJson complexToJson = new ComplexToJson(null);
    List<String> inputJsonBatches = Lists.newArrayList(
        "[{\"a\": {\"b\" : 1 }}]",
        "[{\"a\": {\"b\" : 5}},{\"a\": {\"b\" : 8}}]");
    legacyOpTestBuilder()
        .physicalOperator(complexToJson)
        .inputDataStreamJson(inputJsonBatches)
        .baselineColumns("a")
        .baselineValues("{\n  \"b\" : 1\n}")
        .baselineValues("{\n  \"b\" : 5\n}")
        .baselineValues("{\n  \"b\" : 8\n}")
        .go();
  }

  @Test
  public void testFilter() {
    Filter filterConf = new Filter(null, parseExpr("a=5"), 1.0f);
    List<String> inputJsonBatches = Lists.newArrayList(
        "[{\"a\": 5, \"b\" : 1 }]",
        "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]",
        "[{\"a\": 40, \"b\" : 3},{\"a\": 13, \"b\" : 100}]");
    legacyOpTestBuilder()
        .physicalOperator(filterConf)
        .inputDataStreamJson(inputJsonBatches)
        .baselineColumns("a", "b")
        .baselineValues(5l, 1l)
        .baselineValues(5l, 5l)
        .go();
  }

  @Test
  public void testFlatten() {
    final PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("b"));
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();

    for (int j = 0; j < 1; j++) {
      batchString.append("[");
      for (int i = 0; i < 1; i++) {
        batchString.append("{\"a\": 5, \"b\" : [5, 6, 7]}");
      }
      batchString.append("]");
      inputJsonBatches.add(batchString.toString());
    }

    LegacyOperatorTestBuilder opTestBuilder = legacyOpTestBuilder()
            .physicalOperator(flatten)
            .inputDataStreamJson(inputJsonBatches)
            .baselineColumns("a", "b")
            .baselineValues(5l, 5l)
            .baselineValues(5l, 6l)
            .baselineValues(5l, 7l);

    opTestBuilder.go();
  }

  @Test
  public void testExternalSort() {
    ExternalSort sortConf = new ExternalSort(null,
        Lists.newArrayList(ordering("b", RelFieldCollation.Direction.ASCENDING, RelFieldCollation.NullDirection.FIRST)), false);
    List<String> inputJsonBatches = Lists.newArrayList(
        "[{\"a\": 5, \"b\" : 1 }]",
        "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]",
        "[{\"a\": 40, \"b\" : 3},{\"a\": 13, \"b\" : 100}]");
    legacyOpTestBuilder()
        .physicalOperator(sortConf)
        .maxAllocation(15_000_000L)
        .inputDataStreamJson(inputJsonBatches)
        .baselineColumns("a", "b")
        .baselineValues(5l, 1l)
        .baselineValues(40l, 3l)
        .baselineValues(5l, 5l)
        .baselineValues(3l, 8l)
        .baselineValues(13l, 100l)
        .go();
  }

  private void externalSortLowMemoryHelper(int batchSize, int numberOfBatches, long initReservation, long maxAllocation) {
    ExternalSort sortConf = new ExternalSort(null,
        Lists.newArrayList(ordering("b", RelFieldCollation.Direction.ASCENDING, RelFieldCollation.NullDirection.FIRST)), false);
    List<String> inputJsonBatches = Lists.newArrayList();
    StringBuilder batchString = new StringBuilder();
    for (int j = 0; j < numberOfBatches; j++) {
      batchString.append("[");
      for (int i = 0; i < batchSize; i++) {
        batchString.append("{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8},");
      }
      batchString.append("{\"a\": 5, \"b\" : 1 }");
      batchString.append("]");
      inputJsonBatches.add(batchString.toString());
    }

    LegacyOperatorTestBuilder opTestBuilder =
        legacyOpTestBuilder()
            .initReservation(initReservation)
            .maxAllocation(maxAllocation)
            .physicalOperator(sortConf)
            .inputDataStreamJson(inputJsonBatches)
            .baselineColumns("a", "b");
    for (int i = 0; i < numberOfBatches; i++) {
      opTestBuilder.baselineValues(5l, 1l);
    }
    for (int i = 0; i < batchSize * numberOfBatches; i++) {
      opTestBuilder.baselineValues(5l, 5l);
    }
    for (int i = 0; i < batchSize * numberOfBatches; i++) {
      opTestBuilder.baselineValues(3l, 8l);
    }
    opTestBuilder.go();
  }

  // TODO - Failing with - org.apache.drill.exec.exception.OutOfMemoryException: Unable to allocate buffer of size 262144 (rounded from 147456) due to memory limit. Current allocation: 16422656
  // look in ExternalSortBatch for this JIRA number, changing this percentage of the allocator limit that is
  // the threshold for spilling (it worked with 0.65 for me) "fixed" the problem but hurt perf, will want
  // to find a better solutions to this problem. When it is fixed this threshold will likely become unnecessary
  @Test
  @Ignore("DRILL-4438")
  public void testExternalSortLowMemory1() {
    externalSortLowMemoryHelper(4960, 100, 10000000, 16500000);
  }

  // TODO- believe this was failing in the scan not the sort, may not require a fix
  @Test
  @Ignore("DRILL-4438")
  public void testExternalSortLowMemory2() {
    externalSortLowMemoryHelper(4960, 100, 10000000, 15000000);
  }

  // TODO - believe this was failing in the scan not the sort, may not require a fix
  @Test
  @Ignore("DRILL-4438")
  public void testExternalSortLowMemory3() {
    externalSortLowMemoryHelper(40960, 10, 10000000, 10000000);
  }

  // TODO - Failing with - org.apache.drill.exec.exception.OutOfMemoryException: Unable to allocate sv2 buffer after repeated attempts
  // see comment above testExternalSortLowMemory1 about TODO left in ExternalSortBatch
  @Test
  @Ignore("DRILL-4438")
  public void testExternalSortLowMemory4() {
    externalSortLowMemoryHelper(15960, 30, 10000000, 14500000);
  }

  @Test
  public void testTopN() {
    TopN sortConf = new TopN(null,
        Lists.newArrayList(ordering("b", RelFieldCollation.Direction.ASCENDING, RelFieldCollation.NullDirection.FIRST)), false, 3);
    List<String> inputJsonBatches = Lists.newArrayList(
        "[{\"a\": 5, \"b\" : 1 }]",
        "[{\"a\": 5, \"b\" : 5},{\"a\": 3, \"b\" : 8}]",
        "[{\"a\": 40, \"b\" : 3},{\"a\": 13, \"b\" : 100}]");
    legacyOpTestBuilder()
        .physicalOperator(sortConf)
        .inputDataStreamJson(inputJsonBatches)
        .baselineColumns("a", "b")
        .baselineValues(5l, 1l)
        .baselineValues(40l, 3l)
        .baselineValues(5l, 5l)
        .go();
  }

  // TODO(DRILL-4439) - doesn't expect incoming batches, uses instead RawFragmentBatch
  // need to figure out how to mock these
  @SuppressWarnings("unchecked")
  @Ignore
  @Test
  public void testSimpleMergingReceiver() {
    MergingReceiverPOP mergeConf = new MergingReceiverPOP(-1, Lists.<MinorFragmentEndpoint>newArrayList(),
        Lists.newArrayList(ordering("x", RelFieldCollation.Direction.ASCENDING, RelFieldCollation.NullDirection.FIRST)), false);
    List<String> leftJsonBatches = Lists.newArrayList(
        "[{\"x\": 5, \"a\" : \"a string\"}]",
        "[{\"x\": 5, \"a\" : \"a different string\"},{\"x\": 5, \"a\" : \"meh\"}]");
    List<String> rightJsonBatches = Lists.newArrayList(
        "[{\"x\": 5, \"a\" : \"asdf\"}]",
        "[{\"x\": 5, \"a\" : \"12345\"}, {\"x\": 6, \"a\" : \"qwerty\"}]");
    legacyOpTestBuilder()
        .physicalOperator(mergeConf)
        .inputDataStreamsJson(Lists.newArrayList(leftJsonBatches, rightJsonBatches))
        .baselineColumns("x", "a")
        .baselineValues(5l, "a string")
        .baselineValues(5l, "a different string")
        .baselineValues(5l, "meh")
        .baselineValues(5l, "asdf")
        .baselineValues(5l, "12345")
        .baselineValues(6l, "qwerty")
        .go();
  }
}
