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

import org.apache.drill.exec.record.BatchSchemaBuilder;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.ExternalSort;
import org.apache.drill.exec.physical.config.Filter;
import org.apache.drill.exec.physical.config.FlattenPOP;
import org.apache.drill.exec.physical.config.HashAggregate;
import org.apache.drill.exec.physical.config.HashJoinPOP;
import org.apache.drill.exec.physical.config.Limit;
import org.apache.drill.exec.physical.config.MergeJoinPOP;
import org.apache.drill.exec.physical.config.Project;
import org.apache.drill.exec.physical.config.StreamingAggregate;
import org.apache.drill.exec.physical.config.UnionAll;
import org.apache.drill.exec.planner.physical.AggPrelBase;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.store.dfs.DrillFileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestNullInputMiniPlan extends MiniPlanUnitTestBase{
  protected static DrillFileSystem fs;

  public final String SINGLE_EMPTY_JSON = "/scan/emptyInput/emptyJson/empty.json";
  public final String SINGLE_EMPTY_JSON2 = "/scan/emptyInput/emptyJson/empty2.json";
  public final String SINGLE_JSON = "/scan/jsonTbl/1990/1.json";  // {id: 100, name : "John"}
  public final String SINGLE_JSON2 = "/scan/jsonTbl/1991/2.json"; // {id: 1000, name : "Joe"}

  @BeforeClass
  public static void initFS() throws Exception {
    Configuration conf = new Configuration();
    conf.set(FileSystem.FS_DEFAULT_NAME_KEY, FileSystem.DEFAULT_FS);
    fs = new DrillFileSystem(conf);
  }

  /**
   * Test ScanBatch with a single empty json file.
   * @throws Exception
   */
  @Test
  public void testEmptyJsonInput() throws Exception {
    RecordBatch scanBatch = createScanBatchFromJson(SINGLE_EMPTY_JSON);

    new MiniPlanTestBuilder()
        .root(scanBatch)
        .expectNullBatch(true)
        .go();
  }

  /**
   * Test ScanBatch with mixed json files.
   * input is empty, data_file, empty, data_file
   * */
  @Test
  public void testJsonInputMixedWithEmptyFiles1() throws Exception {
    RecordBatch scanBatch = createScanBatchFromJson(SINGLE_EMPTY_JSON, SINGLE_JSON, SINGLE_EMPTY_JSON2, SINGLE_JSON2);

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("id", TypeProtos.MinorType.BIGINT)
        .addNullable("name", TypeProtos.MinorType.VARCHAR);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    new MiniPlanTestBuilder()
        .root(scanBatch)
        .expectSchema(expectedSchema)
        .baselineValues(100L, "John")
        .baselineValues(1000L, "Joe")
        .expectBatchNum(2)
        .go();

  }

  /**
   * Test ScanBatch with mixed json files.
   * input is empty, empty, data_file, data_file
   * */
  @Test
  public void testJsonInputMixedWithEmptyFiles2() throws Exception {
    RecordBatch scanBatch = createScanBatchFromJson(SINGLE_EMPTY_JSON, SINGLE_EMPTY_JSON2, SINGLE_JSON, SINGLE_JSON2);

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("id", TypeProtos.MinorType.BIGINT)
        .addNullable("name", TypeProtos.MinorType.VARCHAR);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    new MiniPlanTestBuilder()
        .root(scanBatch)
        .expectSchema(expectedSchema)
        .baselineValues(100L, "John")
        .baselineValues(1000L, "Joe")
        .expectBatchNum(2)
        .go();
  }

  /**
   * Test ScanBatch with mixed json files.
   * input is empty, data_file, data_file, empty
   * */
  @Test
  public void testJsonInputMixedWithEmptyFiles3() throws Exception {
    RecordBatch scanBatch = createScanBatchFromJson(SINGLE_EMPTY_JSON, SINGLE_JSON, SINGLE_JSON2, SINGLE_EMPTY_JSON2);

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("id", TypeProtos.MinorType.BIGINT)
        .addNullable("name", TypeProtos.MinorType.VARCHAR);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    new MiniPlanTestBuilder()
        .root(scanBatch)
        .expectSchema(expectedSchema)
        .baselineValues(100L, "John")
        .baselineValues(1000L, "Joe")
        .expectBatchNum(2)
        .go();
  }

  /**
   * Test ScanBatch with mixed json files.
   * input is data_file, data_file, empty, empty
   * */
  @Test
  public void testJsonInputMixedWithEmptyFiles4() throws Exception {
    RecordBatch scanBatch = createScanBatchFromJson(SINGLE_JSON, SINGLE_JSON2, SINGLE_EMPTY_JSON2, SINGLE_EMPTY_JSON2);

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("id", TypeProtos.MinorType.BIGINT)
        .addNullable("name", TypeProtos.MinorType.VARCHAR);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    new MiniPlanTestBuilder()
        .root(scanBatch)
        .expectSchema(expectedSchema)
        .baselineValues(100L, "John")
        .baselineValues(1000L, "Joe")
        .expectBatchNum(2)
        .go();
  }

  @Test
  public void testProjectEmpty() throws Exception {
    final PhysicalOperator project = new Project(parseExprs("x+5", "x"), null);
    testSingleInputNullBatchHandling(project);
  }

  @Test
  public void testFilterEmpty() throws Exception {
    final PhysicalOperator filter = new Filter(null, parseExpr("a=5"), 1.0f);
    testSingleInputNullBatchHandling(filter);
  }

  @Test
  public void testHashAggEmpty() throws Exception {
    final PhysicalOperator hashAgg = new HashAggregate(null, AggPrelBase.OperatorPhase.PHASE_1of1, parseExprs("a", "a"), parseExprs("sum(b)", "b_sum"), 1.0f);
    testSingleInputNullBatchHandling(hashAgg);
  }

  @Test
  public void testStreamingAggEmpty() throws Exception {
    final PhysicalOperator hashAgg = new StreamingAggregate(null, parseExprs("a", "a"), parseExprs("sum(b)", "b_sum"));
    testSingleInputNullBatchHandling(hashAgg);
  }

  @Test
  public void testSortEmpty() throws Exception {
    final PhysicalOperator sort = new ExternalSort(null,
        Lists.newArrayList(ordering("b", RelFieldCollation.Direction.ASCENDING, RelFieldCollation.NullDirection.FIRST)), false);
    testSingleInputNullBatchHandling(sort);
  }

  @Test
  public void testLimitEmpty() throws Exception {
    final PhysicalOperator limit = new Limit(null, 10, 5);
    testSingleInputNullBatchHandling(limit);
  }

  @Test
  public void testFlattenEmpty() throws Exception {
    final PhysicalOperator flatten = new FlattenPOP(null, SchemaPath.getSimplePath("col1"));
    testSingleInputNullBatchHandling(flatten);
  }

  @Test
  public void testUnionEmptyBoth() throws Exception {
    final PhysicalOperator unionAll = new UnionAll(Collections.EMPTY_LIST); // Children list is provided through RecordBatch
    testTwoInputNullBatchHandling(unionAll);
  }

  @Test
  public void testHashJoinEmptyBoth() throws Exception {
   final PhysicalOperator join = new HashJoinPOP(null, null, Lists.newArrayList(joinCond("a", "EQUALS", "b")), JoinRelType.INNER, null);
    testTwoInputNullBatchHandling(join);
  }

  @Test
  public void testLeftHashJoinEmptyBoth() throws Exception {
    final PhysicalOperator join = new HashJoinPOP(null, null, Lists.newArrayList(joinCond("a", "EQUALS", "b")), JoinRelType.LEFT, null);
    testTwoInputNullBatchHandling(join);
  }

  @Test
  public void testRightHashJoinEmptyBoth() throws Exception {
    final PhysicalOperator join = new HashJoinPOP(null, null, Lists.newArrayList(joinCond("a", "EQUALS", "b")), JoinRelType.RIGHT, null);
    testTwoInputNullBatchHandling(join);
  }

  @Test
  public void testFullHashJoinEmptyBoth() throws Exception {
    final PhysicalOperator join = new HashJoinPOP(null, null, Lists.newArrayList(joinCond("a", "EQUALS", "b")), JoinRelType.FULL, null);
    testTwoInputNullBatchHandling(join);
  }

  @Test
  public void testMergeJoinEmptyBoth() throws Exception {
    final PhysicalOperator join = new MergeJoinPOP(null, null, Lists.newArrayList(joinCond("a", "EQUALS", "b")), JoinRelType.INNER);
    testTwoInputNullBatchHandling(join);
  }

  @Test
  public void testLeftMergeJoinEmptyBoth() throws Exception {
    final PhysicalOperator join = new MergeJoinPOP(null, null, Lists.newArrayList(joinCond("a", "EQUALS", "b")), JoinRelType.LEFT);
    testTwoInputNullBatchHandling(join);
  }

  @Test
  public void testRightMergeJoinEmptyBoth() throws Exception {
    final PhysicalOperator join = new MergeJoinPOP(null, null, Lists.newArrayList(joinCond("a", "EQUALS", "b")), JoinRelType.RIGHT);
    testTwoInputNullBatchHandling(join);
  }

  @Test
  @Ignore("Full Merge join is not supported.")
  public void testFullMergeJoinEmptyBoth() throws Exception {
    final PhysicalOperator join = new MergeJoinPOP(null, null, Lists.newArrayList(joinCond("a", "EQUALS", "b")), JoinRelType.FULL);
    testTwoInputNullBatchHandling(join);
  }

  @Test
  public void testUnionLeftEmtpy() throws Exception {
    final PhysicalOperator unionAll = new UnionAll(Collections.EMPTY_LIST); // Children list is provided through RecordBatch

    RecordBatch left = createScanBatchFromJson(SINGLE_EMPTY_JSON);

    String file = DrillFileUtils.getResourceAsFile("/tpchmulti/region/01.parquet").toURI().toString();
    List<Path> filePath = Collections.singletonList(new Path(file));

    RecordBatch scanBatch = new ParquetScanBuilder()
        .fileSystem(fs)
        .columnsToRead("R_REGIONKEY")
        .inputPaths(filePath)
        .build();

    RecordBatch projectBatch = new PopBuilder()
        .physicalOperator(new Project(parseExprs("R_REGIONKEY+10", "regionkey"), null))
        .addInput(scanBatch)
        .build();

    RecordBatch unionBatch = new PopBuilder()
        .physicalOperator(unionAll)
        .addInput(left)
        .addInput(projectBatch)
        .build();

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .add("regionkey", TypeProtos.MinorType.BIGINT);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .build();

    new MiniPlanTestBuilder()
        .root(unionBatch)
        .expectSchema(expectedSchema)
        .baselineValues(10L)
        .baselineValues(11L)
        .go();
  }


  @Test
  public void testHashJoinLeftEmpty() throws Exception {
    RecordBatch left = createScanBatchFromJson(SINGLE_EMPTY_JSON);

    List<String> rightJsonBatches = Lists.newArrayList(
        "[{\"a\": 50, \"b\" : 10 }]");

    RecordBatch rightScan = new JsonScanBuilder()
        .jsonBatches(rightJsonBatches)
        .columnsToRead("a", "b")
        .build();

    RecordBatch joinBatch = new PopBuilder()
        .physicalOperator(new HashJoinPOP(null, null, Lists.newArrayList(joinCond("a2", "EQUALS", "a")), JoinRelType.INNER, null))
        .addInput(left)
        .addInput(rightScan)
        .build();

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("a", TypeProtos.MinorType.BIGINT)
        .addNullable("b", TypeProtos.MinorType.BIGINT);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .withSVMode(BatchSchema.SelectionVectorMode.NONE)
        .build();

    new MiniPlanTestBuilder()
        .root(joinBatch)
        .expectSchema(expectedSchema)
        .expectZeroRow(true)
        .go();
  }

  @Test
  public void testHashJoinRightEmpty() throws Exception {
    List<String> leftJsonBatches = Lists.newArrayList(
        "[{\"a\": 50, \"b\" : 10 }]");

    RecordBatch leftScan = new JsonScanBuilder()
        .jsonBatches(leftJsonBatches)
        .columnsToRead("a", "b")
        .build();

    RecordBatch right = createScanBatchFromJson(SINGLE_EMPTY_JSON);

    RecordBatch joinBatch = new PopBuilder()
        .physicalOperator(new HashJoinPOP(null, null, Lists.newArrayList(joinCond("a", "EQUALS", "a2")), JoinRelType.INNER, null))
        .addInput(leftScan)
        .addInput(right)
        .build();

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("a", TypeProtos.MinorType.BIGINT)
        .addNullable("b", TypeProtos.MinorType.BIGINT);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .withSVMode(BatchSchema.SelectionVectorMode.NONE)
        .build();

    new MiniPlanTestBuilder()
        .root(joinBatch)
        .expectSchema(expectedSchema)
        .expectZeroRow(true)
        .go();
  }


  @Test
  public void testLeftHashJoinLeftEmpty() throws Exception {
    RecordBatch left = createScanBatchFromJson(SINGLE_EMPTY_JSON);

    List<String> rightJsonBatches = Lists.newArrayList(
        "[{\"a\": 50, \"b\" : 10 }]");

    RecordBatch rightScan = new JsonScanBuilder()
        .jsonBatches(rightJsonBatches)
        .columnsToRead("a", "b")
        .build();

    RecordBatch joinBatch = new PopBuilder()
        .physicalOperator(new HashJoinPOP(null, null, Lists.newArrayList(joinCond("a2", "EQUALS", "a")), JoinRelType.LEFT, null))
        .addInput(left)
        .addInput(rightScan)
        .build();

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("a", TypeProtos.MinorType.BIGINT)
        .addNullable("b", TypeProtos.MinorType.BIGINT);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .withSVMode(BatchSchema.SelectionVectorMode.NONE)
        .build();

    new MiniPlanTestBuilder()
        .root(joinBatch)
        .expectSchema(expectedSchema)
        .expectZeroRow(true)
        .go();
  }

  @Test
  public void testLeftHashJoinRightEmpty() throws Exception {
    List<String> leftJsonBatches = Lists.newArrayList(
        "[{\"a\": 50, \"b\" : 10 }]");

    RecordBatch leftScan = new JsonScanBuilder()
        .jsonBatches(leftJsonBatches)
        .columnsToRead("a", "b")
        .build();

    RecordBatch right = createScanBatchFromJson(SINGLE_EMPTY_JSON);

    RecordBatch joinBatch = new PopBuilder()
        .physicalOperator(new HashJoinPOP(null, null, Lists.newArrayList(joinCond("a", "EQUALS", "a2")), JoinRelType.LEFT, null))
        .addInput(leftScan)
        .addInput(right)
        .build();

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("a", TypeProtos.MinorType.BIGINT)
        .addNullable("b", TypeProtos.MinorType.BIGINT);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .withSVMode(BatchSchema.SelectionVectorMode.NONE)
        .build();

    new MiniPlanTestBuilder()
        .root(joinBatch)
        .expectSchema(expectedSchema)
        .baselineValues(50L, 10L)
        .go();
  }

  @Test
  public void testUnionFilterAll() throws Exception {
    List<String> leftJsonBatches = Lists.newArrayList(
        "[{\"a\": 5, \"b\" : \"name1\" }]");

    List<String> rightJsonBatches = Lists.newArrayList(
        "[{\"a\": 50, \"b\" : \"name2\" }]");

    RecordBatch leftScan = new JsonScanBuilder()
        .jsonBatches(leftJsonBatches)
        .columnsToRead("a", "b")
        .build();

    RecordBatch leftFilter = new PopBuilder()
        .physicalOperator(new Filter(null, parseExpr("a < 0"), 1.0f))
        .addInput(leftScan)
        .build();

    RecordBatch rightScan = new JsonScanBuilder()
        .jsonBatches(rightJsonBatches)
        .columnsToRead("a", "b")
        .build();

    RecordBatch rightFilter = new PopBuilder()
        .physicalOperator(new Filter(null, parseExpr("a < 0"), 1.0f))
        .addInput(rightScan)
        .build();

    RecordBatch batch = new PopBuilder()
        .physicalOperator(new UnionAll(Collections.emptyList())) // Children list is provided through RecordBatch
        .addInput(leftFilter)
        .addInput(rightFilter)
        .build();

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("a", TypeProtos.MinorType.BIGINT)
        .addNullable("b", TypeProtos.MinorType.VARCHAR);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .withSVMode(BatchSchema.SelectionVectorMode.NONE)
        .build();

    new MiniPlanTestBuilder()
        .root(batch)
        .expectSchema(expectedSchema)
        .expectZeroRow(true)
        .go();
  }

  @Test
  public void testOutputProjectEmpty() throws Exception {
    final PhysicalOperator project = new Project(
        parseExprs(
        "x", "col1",
        "x + 100", "col2",
        "100.0", "col3",
        "cast(nonExist as varchar(100))", "col4"), null, true);

    SchemaBuilder schemaBuilder = new SchemaBuilder()
        .addNullable("col1", TypeProtos.MinorType.INT)
        .addNullable("col2", TypeProtos.MinorType.INT)
        .add("col3", TypeProtos.MinorType.FLOAT8)
        .addNullable("col4", TypeProtos.MinorType.VARCHAR, 100);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
        .withSchemaBuilder(schemaBuilder)
        .withSVMode(BatchSchema.SelectionVectorMode.NONE)
        .build();

    final RecordBatch input = createScanBatchFromJson(SINGLE_EMPTY_JSON);

    RecordBatch batch = new PopBuilder()
        .physicalOperator(project) // Children list is provided through RecordBatch
        .addInput(input)
        .build();

    new MiniPlanTestBuilder()
        .root(batch)
        .expectSchema(expectedSchema)
        .expectZeroRow(true)
        .go();
  }

  /**
   * Given a physical, first construct scan batch from one single empty json, then construct scan batch from
   * multiple empty json files. In both case, verify that the output is a NullBatch.
   * @param pop
   * @throws Exception
   */
  private void testSingleInputNullBatchHandling(PhysicalOperator pop) throws Exception {
    final RecordBatch input = createScanBatchFromJson(SINGLE_EMPTY_JSON);

    RecordBatch batch = new PopBuilder()
        .physicalOperator(pop)
        .addInput(input)
        .build();

    new MiniPlanTestBuilder()
        .root(batch)
        .expectNullBatch(true)
        .go();

    final RecordBatch input2 = createScanBatchFromJson(SINGLE_EMPTY_JSON, SINGLE_EMPTY_JSON2);
    RecordBatch batch2 = new PopBuilder()
        .physicalOperator(pop)
        .addInput(input2)
        .build();

    new MiniPlanTestBuilder()
        .root(batch2)
        .expectNullBatch(true)
        .go();
  }

  private void testTwoInputNullBatchHandling(PhysicalOperator pop) throws Exception {
    RecordBatch left = createScanBatchFromJson(SINGLE_EMPTY_JSON);
    RecordBatch right = createScanBatchFromJson(SINGLE_EMPTY_JSON);

    RecordBatch joinBatch = new PopBuilder()
        .physicalOperator(pop)
        .addInput(left)
        .addInput(right)
        .build();

    new MiniPlanTestBuilder()
        .root(joinBatch)
        .expectNullBatch(true)
        .go();
  }

  private RecordBatch createScanBatchFromJson(String... resourcePaths) throws Exception {
    List<Path> inputPaths = new ArrayList<>();

    for (String resource : resourcePaths) {
      inputPaths.add(new Path(DrillFileUtils.getResourceAsFile(resource).toURI()));
    }

    RecordBatch scanBatch = new JsonScanBuilder()
        .fileSystem(fs)
        .inputPaths(inputPaths)
        .build();

    return scanBatch;
  }

}
