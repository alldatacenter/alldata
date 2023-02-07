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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.record.RecordBatchLoader;
import org.apache.drill.exec.rpc.user.QueryDataBatch;
import org.apache.drill.exec.vector.BigIntVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Files;

public class TestWriter extends BaseTestQuery {
  private static final String ROOT_DIR_REPLACEMENT = "%ROOT_DIR%";
  private static final String TMP_DIR_REPLACEMENT = "%TMP_DIR%";
  private static final String TEST_DIR_REPLACEMENT = "%TEST_DIR%";

  private static final String ALTER_SESSION = String.format("ALTER SESSION SET `%s` = 'csv'", ExecConstants.OUTPUT_FORMAT_OPTION);

  @Test
  public void simpleCsv() throws Exception {
    File testDir = dirTestWatcher.makeRootSubDir(Paths.get("csvtest"));

    String plan = Files.asCharSource(DrillFileUtils.getResourceAsFile("/writer/simple_csv_writer.json"), Charsets.UTF_8).read();
    plan = plan
      .replace(ROOT_DIR_REPLACEMENT, dirTestWatcher.getRootDir().getAbsolutePath())
      .replace(TMP_DIR_REPLACEMENT, dirTestWatcher.getTmpDir().getAbsolutePath())
      .replace(TEST_DIR_REPLACEMENT, testDir.getAbsolutePath());

    List<QueryDataBatch> results = testPhysicalWithResults(plan);

    RecordBatchLoader batchLoader = new RecordBatchLoader(getAllocator());

    QueryDataBatch batch = results.get(0);
    assertTrue(batchLoader.load(batch.getHeader().getDef(), batch.getData()));

    VarCharVector fragmentIdV = (VarCharVector) batchLoader.getValueAccessorById(VarCharVector.class, 0).getValueVector();
    BigIntVector recordWrittenV = (BigIntVector) batchLoader.getValueAccessorById(BigIntVector.class, 1).getValueVector();

    // expected only one row in output
    assertEquals(1, batchLoader.getRecordCount());

    assertEquals("0_0", fragmentIdV.getAccessor().getObject(0).toString());
    assertEquals(132000, recordWrittenV.getAccessor().get(0));

    // now verify csv files are written to disk
    assertTrue(testDir.exists());

    // expect two files
    int count = org.apache.commons.io.FileUtils.listFiles(testDir, new String[]{"csv"}, false).size();
    assertEquals(2, count);

    for (QueryDataBatch b : results) {
      b.release();
    }

    batchLoader.clear();
  }

  @Test
  public void simpleCTAS() throws Exception {
    final String tableName = "simplectas";
    runSQL("Use dfs.tmp");
    runSQL(ALTER_SESSION);

    final String testQuery = String.format("CREATE TABLE %s AS SELECT * FROM cp.`employee.json`", tableName);

    testCTASQueryHelper(testQuery, 1155);
  }

  @Test
  public void complex1CTAS() throws Exception {
    final String tableName = "complex1ctas";
    runSQL("Use dfs.tmp");
    runSQL(ALTER_SESSION);
    final String testQuery = String.format("CREATE TABLE %s AS SELECT first_name, last_name, " +
        "position_id FROM cp.`employee.json`", tableName);

    testCTASQueryHelper(testQuery, 1155);
  }

  @Test
  public void complex2CTAS() throws Exception {
    final String tableName = "complex2ctas";
    runSQL("Use dfs.tmp");
    runSQL(ALTER_SESSION);
    final String testQuery = String.format("CREATE TABLE %s AS SELECT CAST(`birth_date` as Timestamp) FROM " +
        "cp.`employee.json` GROUP BY birth_date", tableName);

    testCTASQueryHelper(testQuery, 52);
  }

  @Test
  public void simpleCTASWithSchemaInTableName() throws Exception {
    final String tableName = "/test/simplectas2";
    runSQL(ALTER_SESSION);
    final String testQuery =
        String.format("CREATE TABLE dfs.tmp.`%s` AS SELECT * FROM cp.`employee.json`",tableName);

    testCTASQueryHelper(testQuery, 1155);
  }

  @Test
  public void simpleParquetDecimal() throws Exception {
    try {
      final String tableName = "simpleparquetdecimal";
      final String testQuery = String.format("CREATE TABLE dfs.tmp.`%s` AS SELECT cast(salary as " +
          "decimal(30,2)) * -1 as salary FROM cp.`employee.json`", tableName);

      // enable decimal
      test(String.format("alter session set `%s` = true", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
      testCTASQueryHelper(testQuery, 1155);

      // disable decimal
    } finally {
      test(String.format("alter session set `%s` = false", PlannerSettings.ENABLE_DECIMAL_DATA_TYPE_KEY));
    }
  }

  private void testCTASQueryHelper(String testQuery, int expectedOutputCount) throws Exception {
    List<QueryDataBatch> results = testSqlWithResults(testQuery);

    RecordBatchLoader batchLoader = new RecordBatchLoader(getAllocator());

    long recordsWritten = 0;
    for (QueryDataBatch batch : results) {
      batchLoader.load(batch.getHeader().getDef(), batch.getData());

      if (batchLoader.getRecordCount() <= 0) {
        continue;
      }

      BigIntVector recordWrittenV = (BigIntVector) batchLoader
        .getValueAccessorById(BigIntVector.class, 1)
        .getValueVector();

      for (int i = 0; i < batchLoader.getRecordCount(); i++) {
        recordsWritten += recordWrittenV.getAccessor().get(i);
      }

      batchLoader.clear();
      batch.release();
    }

    assertEquals(expectedOutputCount, recordsWritten);
  }
}
