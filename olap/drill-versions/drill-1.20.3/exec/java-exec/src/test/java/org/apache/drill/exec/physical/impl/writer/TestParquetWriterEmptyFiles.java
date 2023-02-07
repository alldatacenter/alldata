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
package org.apache.drill.exec.physical.impl.writer;

import org.apache.commons.io.FileUtils;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.BatchSchemaBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.exec.ExecConstants;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category({ParquetTest.class, UnlikelyTest.class})
public class TestParquetWriterEmptyFiles extends BaseTestQuery {

  @BeforeClass
  public static void initFs() throws Exception {
    updateTestCluster(3, null);
    dirTestWatcher.copyResourceToRoot(Paths.get("schemachange"));
    dirTestWatcher.copyResourceToRoot(Paths.get("parquet", "empty"));
    dirTestWatcher.copyResourceToRoot(Paths.get("parquet", "alltypes_required.parquet"));
  }

  @Test
  public void testWriteEmptyFile() throws Exception {
    final String outputFileName = "testparquetwriteremptyfiles_testwriteemptyfile";
    final File outputFile = FileUtils.getFile(dirTestWatcher.getDfsTestTmpDir(), outputFileName);

    test("CREATE TABLE dfs.tmp.%s AS SELECT * FROM cp.`employee.json` WHERE 1=0", outputFileName);
    assertTrue(outputFile.exists());
  }

  @Test
  public void testWriteEmptyFileWithSchema() throws Exception {
    final String outputFileName = "testparquetwriteremptyfiles_testwriteemptyfilewithschema";

    test("CREATE TABLE dfs.tmp.%s AS select * from dfs.`parquet/alltypes_required.parquet` where `col_int` = 0", outputFileName);

    // Only the last scan scheme is written
    SchemaBuilder schemaBuilder = new SchemaBuilder()
      .add("col_int", TypeProtos.MinorType.INT)
      .add("col_chr", TypeProtos.MinorType.VARCHAR)
      .add("col_vrchr", TypeProtos.MinorType.VARCHAR)
      .add("col_dt", TypeProtos.MinorType.DATE)
      .add("col_tim", TypeProtos.MinorType.TIME)
      .add("col_tmstmp", TypeProtos.MinorType.TIMESTAMP)
      .add("col_flt", TypeProtos.MinorType.FLOAT4)
      .add("col_intrvl_yr", TypeProtos.MinorType.INTERVAL)
      .add("col_intrvl_day", TypeProtos.MinorType.INTERVAL)
      .add("col_bln", TypeProtos.MinorType.BIT);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
      .withSchemaBuilder(schemaBuilder)
      .build();

    testBuilder()
      .unOrdered()
      .sqlQuery("select * from dfs.tmp.%s", outputFileName)
      .schemaBaseLine(expectedSchema)
      .go();
  }

  @Test
  public void testWriteEmptyFileWithEmptySchema() throws Exception {
    final String outputFileName = "testparquetwriteremptyfiles_testwriteemptyfileemptyschema";
    final File outputFile = FileUtils.getFile(dirTestWatcher.getDfsTestTmpDir(), outputFileName);

    test("CREATE TABLE dfs.tmp.%s AS SELECT * FROM cp.`empty.json`", outputFileName);
    assertFalse(outputFile.exists());
  }

  @Test
  public void testWriteEmptySchemaChange() throws Exception {
    final String outputFileName = "testparquetwriteremptyfiles_testwriteemptyschemachange";
    final File outputFile = FileUtils.getFile(dirTestWatcher.getDfsTestTmpDir(), outputFileName);

    test("CREATE TABLE dfs.tmp.%s AS select id, a, b from dfs.`schemachange/multi/*.json` WHERE id = 0", outputFileName);

    // Only the last scan scheme is written
    SchemaBuilder schemaBuilder = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .addNullable("a", TypeProtos.MinorType.BIGINT)
      .addNullable("b", TypeProtos.MinorType.BIT);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
      .withSchemaBuilder(schemaBuilder)
      .build();

    testBuilder()
      .unOrdered()
      .sqlQuery("select * from dfs.tmp.%s", outputFileName)
      .schemaBaseLine(expectedSchema)
      .go();

    // Make sure that only 1 parquet file was created
    assertEquals(1, outputFile.list((dir, name) -> name.endsWith("parquet")).length);
  }

  @Test
  public void testComplexEmptyFileSchema() throws Exception {
    final String outputFileName = "testparquetwriteremptyfiles_testcomplexemptyfileschema";

    test("create table dfs.tmp.%s as select * from dfs.`parquet/empty/complex/empty_complex.parquet`", outputFileName);

    // end_date column is null, so it missing in result schema.
    SchemaBuilder schemaBuilder = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.BIGINT)
      .addNullable("name", TypeProtos.MinorType.VARCHAR)
      .addArray("orders", TypeProtos.MinorType.BIGINT);
    BatchSchema expectedSchema = new BatchSchemaBuilder()
      .withSchemaBuilder(schemaBuilder)
      .build();

    testBuilder()
      .unOrdered()
      .sqlQuery("select * from dfs.tmp.%s", outputFileName)
      .schemaBaseLine(expectedSchema)
      .go();
  }

  @Test
  public void testWriteEmptyFileAfterFlush() throws Exception {
    final String outputFileName = "testparquetwriteremptyfiles_test_write_empty_file_after_flush";
    final File outputFile = FileUtils.getFile(dirTestWatcher.getDfsTestTmpDir(), outputFileName);

    try {
      // this specific value will force a flush just after the final row is written
      // this may cause the creation of a new "empty" parquet file
      test("ALTER SESSION SET `store.parquet.block-size` = 19926");

      final String query = "SELECT * FROM cp.`employee.json` LIMIT 100";
      test("CREATE TABLE dfs.tmp.%s AS %s", outputFileName, query);

      // Make sure that only 1 parquet file was created
      assertEquals(1, outputFile.list((dir, name) -> name.endsWith("parquet")).length);

      // this query will fail if an "empty" file was created
      testBuilder()
        .unOrdered()
        .sqlQuery("SELECT * FROM dfs.tmp.%s", outputFileName)
        .sqlBaselineQuery(query)
        .go();
    } finally {
      // restore the session option
      resetSessionOption(ExecConstants.PARQUET_BLOCK_SIZE);
    }
  }
}
