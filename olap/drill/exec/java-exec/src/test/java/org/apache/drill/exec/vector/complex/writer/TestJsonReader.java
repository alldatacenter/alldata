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
package org.apache.drill.exec.vector.complex.writer;

import static org.apache.drill.test.TestBuilder.listOf;
import static org.apache.drill.test.TestBuilder.mapOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.record.VectorWrapper;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.store.easy.json.JSONRecordReader;
import org.apache.drill.exec.util.JsonStringHashMap;
import org.apache.drill.exec.util.Text;
import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.exec.vector.RepeatedBigIntVector;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;
import org.apache.drill.test.BaseTestQuery;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestJsonReader extends BaseTestQuery {
  private static final Logger logger = LoggerFactory.getLogger(TestJsonReader.class);

  @BeforeClass
  public static void setupTestFiles() {
    dirTestWatcher.copyResourceToRoot(Paths.get("store", "json"));
    dirTestWatcher.copyResourceToRoot(Paths.get("vector","complex", "writer"));
  }

  @Test
  public void testEmptyList() throws Exception {
    final String root = "store/json/emptyLists";

    testBuilder()
        .sqlQuery("select count(a[0]) as ct from dfs.`%s`", root, root)
        .ordered()
        .baselineColumns("ct")
        .baselineValues(6l)
        .build()
        .run();
  }

  @Test
  public void schemaChange() throws Exception {
    // Verifies that the schema change does not cause a
    // crash. A pretty minimal test.
    // TODO: Verify actual results.
    test("select b from dfs.`vector/complex/writer/schemaChange/`");
  }

  @Test
  public void testFieldSelectionBug() throws Exception {
    try {
      testBuilder()
          .sqlQuery("select t.field_4.inner_3 as col_1, t.field_4 as col_2 from cp.`store/json/schema_change_int_to_string.json` t")
          .unOrdered()
          .optionSettingQueriesForTestQuery("alter session set `store.json.all_text_mode` = true")
          .baselineColumns("col_1", "col_2")
          .baselineValues(
              mapOf(),
              mapOf(
                  "inner_1", listOf(),
                  "inner_3", mapOf()))
          .baselineValues(
              mapOf("inner_object_field_1", "2"),
              mapOf(
                  "inner_1", listOf("1", "2", "3"),
                  "inner_2", "3",
                  "inner_3", mapOf("inner_object_field_1", "2")))
          .baselineValues(
              mapOf(),
              mapOf(
                  "inner_1", listOf("4", "5", "6"),
                  "inner_2", "3",
                  "inner_3", mapOf()))
          .go();
    } finally {
      resetSessionOption(ExecConstants.JSON_ALL_TEXT_MODE);
    }
  }

  @Test
  public void testSplitAndTransferFailure() throws Exception {
    final String testVal = "a string";
    testBuilder()
        .sqlQuery("select flatten(config) as flat from cp.`store/json/null_list.json`")
        .ordered()
        .baselineColumns("flat")
        .baselineValues(listOf())
        .baselineValues(listOf(testVal))
        .go();

    test("select flatten(config) as flat from cp.`store/json/null_list_v2.json`");
    testBuilder()
        .sqlQuery("select flatten(config) as flat from cp.`store/json/null_list_v2.json`")
        .ordered()
        .baselineColumns("flat")
        .baselineValues(mapOf("repeated_varchar", listOf()))
        .baselineValues(mapOf("repeated_varchar", listOf(testVal)))
        .go();

    testBuilder()
        .sqlQuery("select flatten(config) as flat from cp.`store/json/null_list_v3.json`")
        .ordered()
        .baselineColumns("flat")
        .baselineValues(mapOf("repeated_map", listOf(mapOf("repeated_varchar", listOf()))))
        .baselineValues(mapOf("repeated_map", listOf(mapOf("repeated_varchar", listOf(testVal)))))
        .go();
  }

  @Test // DRILL-1824
  public void schemaChangeValidate() throws Exception {
    testBuilder()
      .sqlQuery("select b from dfs.`vector/complex/writer/schemaChange/`")
      .unOrdered()
      .baselineColumns("b")
      .baselineValues(null)
      .baselineValues(null)
      .baselineValues(mapOf())
      .baselineValues(mapOf("x", 1L, "y", 2L))
      .build()
      .run();
  }

  public void runTestsOnFile(String filename, UserBitShared.QueryType queryType, String[] queries, long[] rowCounts) throws Exception {
    logger.debug("===================");
    logger.debug("source data in json");
    logger.debug("===================");
    logger.debug(Files.asCharSource(DrillFileUtils.getResourceAsFile(filename), Charsets.UTF_8).read());

    int i = 0;
    for (String query : queries) {
      logger.debug("=====");
      logger.debug("query");
      logger.debug("=====");
      logger.debug(query);
      logger.debug("======");
      logger.debug("result");
      logger.debug("======");
      int rowCount = testRunAndPrint(queryType, query);
      assertEquals(rowCounts[i], rowCount);

      logger.debug("\n");
      i++;
    }
  }

  @Test
  public void testReadCompressed() throws Exception {
    String filepath = "compressed_json.json";
    File f = new File(dirTestWatcher.getRootDir(), filepath);
    PrintWriter out = new PrintWriter(f);
    out.println("{\"a\" :5}");
    out.close();

    gzipIt(f);
    testBuilder()
        .sqlQuery("select * from dfs.`%s.gz`", filepath)
        .unOrdered()
        .baselineColumns("a")
        .baselineValues(5l)
        .build().run();

    // test reading the uncompressed version as well
    testBuilder()
        .sqlQuery("select * from dfs.`%s`", filepath)
        .unOrdered()
        .baselineColumns("a")
        .baselineValues(5l)
        .build().run();
  }

  public static void gzipIt(File sourceFile) throws IOException {

    // modified from: http://www.mkyong.com/java/how-to-compress-a-file-in-gzip-format/
    byte[] buffer = new byte[1024];
    GZIPOutputStream gzos =
        new GZIPOutputStream(new FileOutputStream(sourceFile.getPath() + ".gz"));

    FileInputStream in =
        new FileInputStream(sourceFile);

    int len;
    while ((len = in.read(buffer)) > 0) {
      gzos.write(buffer, 0, len);
    }
    in.close();
    gzos.finish();
    gzos.close();
  }

  @Test
  public void testDrill_1419() throws Exception {
    String[] queries = {"select t.trans_id, t.trans_info.prod_id[0],t.trans_info.prod_id[1] from cp.`store/json/clicks.json` t limit 5"};
    long[] rowCounts = {5};
    String filename = "/store/json/clicks.json";
    runTestsOnFile(filename, UserBitShared.QueryType.SQL, queries, rowCounts);
  }

  @Test
  public void testRepeatedCount() throws Exception {
    test("select repeated_count(str_list) from cp.`store/json/json_basic_repeated_varchar.json`");
    test("select repeated_count(INT_col) from cp.`parquet/alltypes_repeated.json`");
    test("select repeated_count(FLOAT4_col) from cp.`parquet/alltypes_repeated.json`");
    test("select repeated_count(VARCHAR_col) from cp.`parquet/alltypes_repeated.json`");
    test("select repeated_count(BIT_col) from cp.`parquet/alltypes_repeated.json`");
  }

  @Test
  public void testRepeatedContains() throws Exception {
    test("select repeated_contains(str_list, 'asdf') from cp.`store/json/json_basic_repeated_varchar.json`");
    test("select repeated_contains(INT_col, -2147483648) from cp.`parquet/alltypes_repeated.json`");
    test("select repeated_contains(FLOAT4_col, -1000000000000.0) from cp.`parquet/alltypes_repeated.json`");
    test("select repeated_contains(VARCHAR_col, 'qwerty' ) from cp.`parquet/alltypes_repeated.json`");
    test("select repeated_contains(BIT_col, true) from cp.`parquet/alltypes_repeated.json`");
    test("select repeated_contains(BIT_col, false) from cp.`parquet/alltypes_repeated.json`");
  }

  @Test
  public void testSingleColumnRead_vector_fill_bug() throws Exception {
    String[] queries = {"select * from cp.`store/json/single_column_long_file.json`"};
    long[] rowCounts = {13512};
    String filename = "/store/json/single_column_long_file.json";
    runTestsOnFile(filename, UserBitShared.QueryType.SQL, queries, rowCounts);
  }

  @Test
  public void testNonExistentColumnReadAlone() throws Exception {
    String[] queries = {"select non_existent_column from cp.`store/json/single_column_long_file.json`"};
    long[] rowCounts = {13512};
    String filename = "/store/json/single_column_long_file.json";
    runTestsOnFile(filename, UserBitShared.QueryType.SQL, queries, rowCounts);
  }

  @Test
  public void testAllTextMode() throws Exception {
    try {
      alterSession(ExecConstants.JSON_ALL_TEXT_MODE, true);
      String[] queries = {"select * from cp.`store/json/schema_change_int_to_string.json`"};
      long[] rowCounts = {3};
      String filename = "/store/json/schema_change_int_to_string.json";
      runTestsOnFile(filename, UserBitShared.QueryType.SQL, queries, rowCounts);
    } finally {
      resetSessionOption(ExecConstants.JSON_ALL_TEXT_MODE);
    }
  }

  @Test
  public void readComplexWithStar() throws Exception {
    List<QueryDataBatch> results = testSqlWithResults("select * from cp.`store/json/test_complex_read_with_star.json`");
    assertEquals(1, results.size());

    RecordBatchLoader batchLoader = new RecordBatchLoader(getAllocator());
    QueryDataBatch batch = results.get(0);

    assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));
    assertEquals(3, batchLoader.getSchema().getFieldCount());
    testExistentColumns(batchLoader);

    batch.release();
    batchLoader.clear();
  }

  @Test
  public void testNullWhereListExpected() throws Exception {
    try {
      alterSession(ExecConstants.JSON_ALL_TEXT_MODE, true);
      String[] queries = {"select * from cp.`store/json/null_where_list_expected.json`"};
      long[] rowCounts = {3};
      String filename = "/store/json/null_where_list_expected.json";
      runTestsOnFile(filename, UserBitShared.QueryType.SQL, queries, rowCounts);
    }
    finally {
      resetSessionOption(ExecConstants.JSON_ALL_TEXT_MODE);
    }
  }

  @Test
  public void testNullWhereMapExpected() throws Exception {
    try {
      alterSession(ExecConstants.JSON_ALL_TEXT_MODE, true);
      String[] queries = {"select * from cp.`store/json/null_where_map_expected.json`"};
      long[] rowCounts = {3};
      String filename = "/store/json/null_where_map_expected.json";
      runTestsOnFile(filename, UserBitShared.QueryType.SQL, queries, rowCounts);
    }
    finally {
      resetSessionOption(ExecConstants.JSON_ALL_TEXT_MODE);
    }
  }

  @Test
  public void ensureProjectionPushdown() throws Exception {
    try {
      // Tests to make sure that we are correctly eliminating schema changing
      // columns. If completes, means that the projection pushdown was
      // successful.
      test("alter system set `store.json.all_text_mode` = false; "
          + "select  t.field_1, t.field_3.inner_1, t.field_3.inner_2, t.field_4.inner_1 "
          + "from cp.`store/json/schema_change_int_to_string.json` t");
    } finally {
      resetSessionOption(ExecConstants.JSON_ALL_TEXT_MODE);
    }
  }

  // The project pushdown rule is correctly adding the projected columns to the
  // scan, however it is not removing the redundant project operator after the
  // scan, this tests runs a physical plan generated from one of the tests to
  // ensure that the project is filtering out the correct data in the scan alone.
  @Test
  public void testProjectPushdown() throws Exception {
    try {
      String[] queries = {Files.asCharSource(DrillFileUtils.getResourceAsFile(
          "/store/json/project_pushdown_json_physical_plan.json"), Charsets.UTF_8).read()};
      String filename = "/store/json/schema_change_int_to_string.json";
      alterSession(ExecConstants.JSON_ALL_TEXT_MODE, false);
      long[] rowCounts = {3};
      runTestsOnFile(filename, UserBitShared.QueryType.PHYSICAL, queries, rowCounts);

      List<QueryDataBatch> results = testPhysicalWithResults(queries[0]);
      assertEquals(1, results.size());
      // "`field_1`", "`field_3`.`inner_1`", "`field_3`.`inner_2`", "`field_4`.`inner_1`"

      RecordBatchLoader batchLoader = new RecordBatchLoader(getAllocator());
      QueryDataBatch batch = results.get(0);
      assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

      // this used to be five. It is now four. This is because the plan doesn't
      // have a project. Scanners are not responsible for projecting non-existent
      // columns (as long as they project one column)
      //
      // That said, the JSON format plugin does claim it can do project
      // push-down, which means it will ensure columns for any column
      // mentioned in the project list, in a form consistent with the schema
      // path. In this case, `non_existent`.`nested`.`field` appears in
      // the query. But, even more oddly, the missing field is inserted only
      // if all text mode is true, omitted if all text mode is false.
      // Seems overly complex.
      assertEquals(3, batchLoader.getSchema().getFieldCount());
      testExistentColumns(batchLoader);

      batch.release();
      batchLoader.clear();
    } finally {
      resetSessionOption(ExecConstants.JSON_ALL_TEXT_MODE);
    }
  }

  @Test
  public void testJsonDirectoryWithEmptyFile() throws Exception {
    testBuilder()
        .sqlQuery("select * from dfs.`store/json/jsonDirectoryWithEmpyFile`")
        .unOrdered()
        .baselineColumns("a")
        .baselineValues(1l)
        .build()
        .run();
  }

  private void testExistentColumns(RecordBatchLoader batchLoader) throws SchemaChangeException {
    VectorWrapper<?> vw = batchLoader.getValueAccessorById(
        RepeatedBigIntVector.class,
        batchLoader.getValueVectorId(SchemaPath.getCompoundPath("field_1")).getFieldIds()
    );
    assertEquals("[1]", vw.getValueVector().getAccessor().getObject(0).toString());
    assertEquals("[5]", vw.getValueVector().getAccessor().getObject(1).toString());
    assertEquals("[5,10,15]", vw.getValueVector().getAccessor().getObject(2).toString());

    vw = batchLoader.getValueAccessorById(
        IntVector.class,
        batchLoader.getValueVectorId(SchemaPath.getCompoundPath("field_3", "inner_1")).getFieldIds()
    );
    assertNull(vw.getValueVector().getAccessor().getObject(0));
    assertEquals(2l, vw.getValueVector().getAccessor().getObject(1));
    assertEquals(5l, vw.getValueVector().getAccessor().getObject(2));

    vw = batchLoader.getValueAccessorById(
        IntVector.class,
        batchLoader.getValueVectorId(SchemaPath.getCompoundPath("field_3", "inner_2")).getFieldIds()
    );
    assertNull(vw.getValueVector().getAccessor().getObject(0));
    assertNull(vw.getValueVector().getAccessor().getObject(1));
    assertEquals(3l, vw.getValueVector().getAccessor().getObject(2));

    vw = batchLoader.getValueAccessorById(
        RepeatedBigIntVector.class,
        batchLoader.getValueVectorId(SchemaPath.getCompoundPath("field_4", "inner_1")).getFieldIds()
    );
    assertEquals("[]", vw.getValueVector().getAccessor().getObject(0).toString());
    assertEquals("[1,2,3]", vw.getValueVector().getAccessor().getObject(1).toString());
    assertEquals("[4,5,6]", vw.getValueVector().getAccessor().getObject(2).toString());
  }

  @Test
  public void testSelectStarWithUnionType() throws Exception {
    try {
      testBuilder()
              .sqlQuery("select * from cp.`jsoninput/union/a.json`")
              .ordered()
              .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
              .baselineColumns("field1", "field2")
              .baselineValues(
                      1L, 1.2
              )
              .baselineValues(
                      listOf(2L), 1.2
              )
              .baselineValues(
                      mapOf("inner1", 3L, "inner2", 4L), listOf(3L, 4.0, "5")
              )
              .baselineValues(
                      mapOf("inner1", 3L,
                              "inner2", listOf(
                                      mapOf(
                                              "innerInner1", 1L,
                                              "innerInner2",
                                              listOf(
                                                      3L,
                                                      "a"
                                              )
                                      )
                              )
                      ),
                      listOf(
                              mapOf("inner3", 7L),
                              4.0,
                              "5",
                              mapOf("inner4", 9L),
                              listOf(
                                      mapOf(
                                              "inner5", 10L,
                                              "inner6", 11L
                                      ),
                                      mapOf(
                                              "inner5", 12L,
                                              "inner7", 13L
                                      )
                              )
                      )
              ).go();
    } finally {
      resetSessionOption(ExecConstants.ENABLE_UNION_TYPE_KEY);
    }
  }

  @Test
  public void testSelectFromListWithCase() throws Exception {
    try {
      testBuilder()
              .sqlQuery("select a, typeOf(a) `type` from " +
                "(select case when is_list(field2) then field2[4][1].inner7 end a " +
                "from cp.`jsoninput/union/a.json`) where a is not null")
              .ordered()
              .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
              .baselineColumns("a", "type")
              .baselineValues(13L, "BIGINT")
              .go();
    } finally {
      resetSessionOption(ExecConstants.ENABLE_UNION_TYPE_KEY);
    }
  }

  @Test
  public void testTypeCase() throws Exception {
    try {
      testBuilder()
              .sqlQuery("select case when is_bigint(field1) " +
                "then field1 when is_list(field1) then field1[0] " +
                "when is_map(field1) then t.field1.inner1 end f1 from cp.`jsoninput/union/a.json` t")
              .ordered()
              .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
              .baselineColumns("f1")
              .baselineValues(1L)
              .baselineValues(2L)
              .baselineValues(3L)
              .baselineValues(3L)
              .go();
    } finally {
      resetSessionOption(ExecConstants.ENABLE_UNION_TYPE_KEY);
    }
  }

  @Test
  public void testSumWithTypeCase() throws Exception {
    try {
      testBuilder()
              .sqlQuery("select sum(cast(f1 as bigint)) sum_f1 from " +
                "(select case when is_bigint(field1) then field1 " +
                "when is_list(field1) then field1[0] when is_map(field1) then t.field1.inner1 end f1 " +
                "from cp.`jsoninput/union/a.json` t)")
              .ordered()
              .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
              .baselineColumns("sum_f1")
              .baselineValues(9L)
              .go();
    } finally {
      resetSessionOption(ExecConstants.ENABLE_UNION_TYPE_KEY);
    }
  }

  @Test
  public void testUnionExpressionMaterialization() throws Exception {
    try {
      testBuilder()
              .sqlQuery("select a + b c from cp.`jsoninput/union/b.json`")
              .ordered()
              .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
              .baselineColumns("c")
              .baselineValues(3L)
              .baselineValues(7.0)
              .baselineValues(11.0)
              .go();
    } finally {
      resetSessionOption(ExecConstants.ENABLE_UNION_TYPE_KEY);
    }
  }

  @Test
  public void testSumMultipleBatches() throws Exception {
    File table_dir = dirTestWatcher.makeTestTmpSubDir(Paths.get("multi_batch"));
    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(new File(table_dir, "a.json")));
    for (int i = 0; i < 10000; i++) {
      os.write("{ type : \"map\", data : { a : 1 } }\n".getBytes());
      os.write("{ type : \"bigint\", data : 1 }\n".getBytes());
    }
    os.flush();
    os.close();

    try {
      testBuilder()
              .sqlQuery("select sum(cast(case when `type` = 'map' then t.data.a else data end as bigint)) `sum` from dfs.tmp.multi_batch t")
              .ordered()
              .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
              .baselineColumns("sum")
              .baselineValues(20000L)
              .go();
    } finally {
      resetSessionOption(ExecConstants.ENABLE_UNION_TYPE_KEY);
    }
  }

  @Test
  public void testSumFilesWithDifferentSchema() throws Exception {
    File table_dir = dirTestWatcher.makeTestTmpSubDir(Paths.get("multi_file"));
    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(new File(table_dir, "a.json")));
    for (int i = 0; i < 10000; i++) {
      os.write("{ type : \"map\", data : { a : 1 } }\n".getBytes());
    }
    os.flush();
    os.close();
    os = new BufferedOutputStream(new FileOutputStream(new File(table_dir, "b.json")));
    for (int i = 0; i < 10000; i++) {
      os.write("{ type : \"bigint\", data : 1 }\n".getBytes());
    }
    os.flush();
    os.close();

    try {
      testBuilder()
              .sqlQuery("select sum(cast(case when `type` = 'map' then t.data.a else data end as bigint)) `sum` from dfs.tmp.multi_file t")
              .ordered()
              .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
              .baselineColumns("sum")
              .baselineValues(20000L)
              .go();
    } finally {
      resetSessionOption(ExecConstants.ENABLE_UNION_TYPE_KEY);
    }
  }

  @Test
  public void drill_4032() throws Exception {
    File table_dir = dirTestWatcher.makeTestTmpSubDir(Paths.get("drill_4032"));
    table_dir.mkdir();
    BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(new File(table_dir, "a.json")));
    os.write("{\"col1\": \"val1\",\"col2\": null}".getBytes());
    os.write("{\"col1\": \"val1\",\"col2\": {\"col3\":\"abc\", \"col4\":\"xyz\"}}".getBytes());
    os.flush();
    os.close();
    os = new BufferedOutputStream(new FileOutputStream(new File(table_dir, "b.json")));
    os.write("{\"col1\": \"val1\",\"col2\": null}".getBytes());
    os.write("{\"col1\": \"val1\",\"col2\": null}".getBytes());
    os.flush();
    os.close();
    testNoResult("select t.col2.col3 from dfs.tmp.drill_4032 t");
  }

  @Test
  public void drill_4479() throws Exception {
    try {
      File table_dir = dirTestWatcher.makeTestTmpSubDir(Paths.get("drill_4479"));
      table_dir.mkdir();
      BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(new File(table_dir, "mostlynulls.json")));
      // Create an entire batch of null values for 3 columns
      for (int i = 0; i < JSONRecordReader.DEFAULT_ROWS_PER_BATCH; i++) {
        os.write("{\"a\": null, \"b\": null, \"c\": null}".getBytes());
      }
      // Add a row with {bigint,  float, string} values
      os.write("{\"a\": 123456789123, \"b\": 99.999, \"c\": \"Hello World\"}".getBytes());
      os.flush();
      os.close();

      testBuilder()
        .sqlQuery("select c, count(*) as cnt from dfs.tmp.drill_4479 t group by c")
        .ordered()
        .optionSettingQueriesForTestQuery("alter session set `store.json.all_text_mode` = true")
        .baselineColumns("c", "cnt")
        .baselineValues(null, 4096L)
        .baselineValues("Hello World", 1L)
        .go();

      testBuilder()
        .sqlQuery("select a, b, c, count(*) as cnt from dfs.tmp.drill_4479 t group by a, b, c")
        .ordered()
        .optionSettingQueriesForTestQuery("alter session set `store.json.all_text_mode` = true")
        .baselineColumns("a", "b", "c", "cnt")
        .baselineValues(null, null, null, 4096L)
        .baselineValues("123456789123", "99.999", "Hello World", 1L)
        .go();

      testBuilder()
        .sqlQuery("select max(a) as x, max(b) as y, max(c) as z from dfs.tmp.drill_4479 t")
        .ordered()
        .optionSettingQueriesForTestQuery("alter session set `store.json.all_text_mode` = true")
        .baselineColumns("x", "y", "z")
        .baselineValues("123456789123", "99.999", "Hello World")
        .go();

    } finally {
      resetSessionOption(ExecConstants.JSON_ALL_TEXT_MODE);
    }
  }

  @Test
  public void testFlattenEmptyArrayWithAllTextMode() throws Exception {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), "empty_array_all_text_mode.json")))) {
      writer.write("{ \"a\": { \"b\": { \"c\": [] }, \"c\": [] } }");
    }

    try {
      String query = "select flatten(t.a.b.c) as c from dfs.`empty_array_all_text_mode.json` t";

      testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .optionSettingQueriesForTestQuery("alter session set `store.json.all_text_mode` = true")
        .expectsEmptyResultSet()
        .go();

      testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .optionSettingQueriesForTestQuery("alter session set `store.json.all_text_mode` = false")
        .expectsEmptyResultSet()
        .go();

    } finally {
      resetSessionOption(ExecConstants.JSON_ALL_TEXT_MODE);
    }
  }

  @Test
  public void testFlattenEmptyArrayWithUnionType() throws Exception {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), "empty_array.json")))) {
      writer.write("{ \"a\": { \"b\": { \"c\": [] }, \"c\": [] } }");
    }

    try {
      String query = "select flatten(t.a.b.c) as c from dfs.`empty_array.json` t";

      testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
        .expectsEmptyResultSet()
        .go();

      testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type` = true")
        .optionSettingQueriesForTestQuery("alter session set `store.json.all_text_mode` = true")
        .expectsEmptyResultSet()
        .go();

    } finally {
      resetSessionOption(ExecConstants.JSON_ALL_TEXT_MODE);
      resetSessionOption(ExecConstants.ENABLE_UNION_TYPE_KEY);
    }
  }

  @Test // DRILL-5521
  public void testKvgenWithUnionAll() throws Exception {
    String fileName = "map.json";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), fileName)))) {
      writer.write("{\"rk\": \"a\", \"m\": {\"a\":\"1\"}}");
    }

    String query = String.format("select kvgen(m) as res from (select m from dfs.`%s` union all " +
        "select convert_from('{\"a\" : null}' ,'json') as m from (values(1)))", fileName);
    assertEquals("Row count should match", 2, testSql(query));
  }

  @Test // DRILL-4264
  public void testFieldWithDots() throws Exception {
    String fileName = "table.json";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), fileName)))) {
      writer.write("{\"rk.q\": \"a\", \"m\": {\"a.b\":\"1\", \"a\":{\"b\":\"2\"}, \"c\":\"3\"}}");
    }

    testBuilder()
      .sqlQuery("select t.m.`a.b` as a,\n" +
        "t.m.a.b as b,\n" +
        "t.m['a.b'] as c,\n" +
        "t.rk.q as d,\n" +
        "t.`rk.q` as e\n" +
        "from dfs.`%s` t", fileName)
      .unOrdered()
      .baselineColumns("a", "b", "c", "d", "e")
      .baselineValues("1", "2", "1", null, "a")
      .go();
  }

  @Test // DRILL-6020
  public void testUntypedPathWithUnion() throws Exception {
    String fileName = "table.json";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), fileName)))) {
      writer.write("{\"rk\": {\"a\": {\"b\": \"1\"}}}");
      writer.write("{\"rk\": {\"a\": \"2\"}}");
    }

    JsonStringHashMap<String, Text> map = new JsonStringHashMap<>();
    map.put("b", new Text("1"));

    try {
      testBuilder()
        .sqlQuery("select t.rk.a as a from dfs.`%s` t", fileName)
        .ordered()
        .optionSettingQueriesForTestQuery("alter session set `exec.enable_union_type`=true")
        .baselineColumns("a")
        .baselineValues(map)
        .baselineValues("2")
        .go();

    } finally {
      resetSessionOption(ExecConstants.ENABLE_UNION_TYPE_KEY);
    }
  }

  @Test
  public void testConvertFromJson() throws Exception {
    String fileName = "table.tsv";
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), fileName)))) {
      for (int i = 0; i < JSONRecordReader.DEFAULT_ROWS_PER_BATCH; i++) {
        writer.write("{\"id\":\"1\"}\n");
      }
      writer.write("{\"id\":\"2\",\"v\":[\"abc\"]}");
    }

    String sql = "SELECT t.m.id AS id, t.m.v[0] v FROM \n" +
        "(SELECT convert_from(columns[0], 'json') AS m FROM dfs.`%s`) t\n" +
        "where t.m.id='2'";

    testBuilder()
        .sqlQuery(sql, fileName)
        .unOrdered()
        .baselineColumns("id", "v")
        .baselineValues("2", "abc")
        .go();
  }

  @Test // DRILL-7821
  public void testEmptyObjectInference() throws Exception {
    String fileName = "emptyObject.json";

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), fileName)))) {
      writer.write("{\"sample\": [{\"data\": {}},{\"data\": \"\"}]}");
    }

    String sql = "SELECT * from dfs.`%s` t";

    testBuilder()
        .sqlQuery(sql, fileName)
        .ordered()
        .baselineColumns("sample")
        .baselineValues(
            listOf(
                mapOf(
                    "data", mapOf()
                ),
                mapOf(
                    "data", mapOf()
                )
            )
        )
        .go();
  }

  @Test // DRILL-7821
  public void testFilledObjectInference() throws Exception {
    String fileName = "filledObject.json";

    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), fileName)))) {
      writer.write("{\"sample\": [{\"data\": {\"foo\": \"bar\"}},{\"data\": \"\"}]}");
    }

    String sql = "SELECT * from dfs.`%s` t";

    testBuilder()
        .sqlQuery(sql, fileName)
        .ordered()
        .baselineColumns("sample")
        .baselineValues(
            listOf(
                mapOf(
                    "data", mapOf(
                        "foo", "bar"
                    )
                ),
                mapOf(
                    "data", mapOf()
                )
            )
        )
        .go();
  }
}
