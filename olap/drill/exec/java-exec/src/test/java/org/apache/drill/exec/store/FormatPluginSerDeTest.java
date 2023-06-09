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
package org.apache.drill.exec.store;

import org.apache.drill.PlanTestBase;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.store.avro.AvroDataGenerator;
import org.junit.Test;

import java.nio.file.Paths;

import static org.apache.drill.exec.util.StoragePluginTestUtils.DFS_TMP_SCHEMA;

public class FormatPluginSerDeTest extends PlanTestBase {

  @Test
  public void testParquet() throws Exception {
    try {
      setSessionOption(ExecConstants.SLICE_TARGET, 1);
      testPhysicalPlanSubmission(
          String.format("select * from table(cp.`%s`(type=>'parquet'))", "parquet/alltypes_required.parquet"),
          String.format("select * from table(cp.`%s`(type=>'parquet', autoCorrectCorruptDates=>false))", "parquet/alltypes_required.parquet"),
          String.format("select * from table(cp.`%s`(type=>'parquet', autoCorrectCorruptDates=>true))", "parquet/alltypes_required.parquet"),
          String.format("select * from table(cp.`%s`(type=>'parquet', autoCorrectCorruptDates=>true, enableStringsSignedMinMax=>false))", "parquet/alltypes_required.parquet"),
          String.format("select * from table(cp.`%s`(type=>'parquet', autoCorrectCorruptDates=>false, enableStringsSignedMinMax=>true))", "parquet/alltypes_required.parquet"));
    } finally {
      resetSessionOption(ExecConstants.SLICE_TARGET);
    }
  }

  @Test
  public void testParquetWithMetadata() throws Exception {
    String tableName = "alltypes_required_with_metadata";
    test("use %s", DFS_TMP_SCHEMA);
    try {
      test("create table %s as select * from cp.`parquet/alltypes_required.parquet`", tableName);
      test("refresh table metadata %s", tableName);
      setSessionOption(ExecConstants.SLICE_TARGET, 1);
      testPhysicalPlanSubmission(
          String.format("select * from table(`%s`(type=>'parquet'))", tableName),
          String.format("select * from table(`%s`(type=>'parquet', autoCorrectCorruptDates=>false))", tableName),
          String.format("select * from table(`%s`(type=>'parquet', autoCorrectCorruptDates=>true))", tableName));
    } finally {
      resetSessionOption(ExecConstants.SLICE_TARGET);
      test("drop table if exists %s", tableName);
    }
  }

  @Test
  public void testAvro() throws Exception {
    AvroDataGenerator dataGenerator = new AvroDataGenerator(dirTestWatcher);
    String file = dataGenerator.generateSimplePrimitiveSchema_NoNullValues(5).getFileName();
    testPhysicalPlanSubmission(
        String.format("select * from dfs.`%s`", file),
        String.format("select * from table(dfs.`%s`(type=>'avro'))", file)
    );
  }

  @Test
  public void testSequenceFile() throws Exception {
    String path = "sequencefiles/simple.seq";
    dirTestWatcher.copyResourceToRoot(Paths.get(path));
    testPhysicalPlanSubmission(
        String.format("select * from dfs.`%s`", path),
        String.format("select * from table(dfs.`%s`(type=>'sequencefile'))", path)
    );
  }

  @Test
  public void testJson() throws Exception {
    testPhysicalPlanSubmission(
        "select * from cp.`donuts.json`",
        "select * from table(cp.`donuts.json`(type=>'json'))"
    );
  }

  @Test
  public void testText() throws Exception {
    String path = "store/text/data/regions.csv";
    dirTestWatcher.copyResourceToRoot(Paths.get(path));
    testPhysicalPlanSubmission(
        String.format("select * from table(dfs.`%s`(type => 'text'))", path),
        String.format("select * from table(dfs.`%s`(type => 'text', extractHeader => false, fieldDelimiter => 'A'))", path)
    );
  }

  @Test
  public void testNamed() throws Exception {
    String path = "store/text/WithQuote.tbl";
    dirTestWatcher.copyResourceToRoot(Paths.get(path));
    String query = String.format("select * from table(dfs.`%s`(type=>'named', name=>'psv'))", path);
    testPhysicalPlanSubmission(query);
  }

  private void testPhysicalPlanSubmission(String...queries) throws Exception {
    for (String query : queries) {
      PlanTestBase.testPhysicalPlanExecutionBasedOnQuery(query);
    }
  }
}
