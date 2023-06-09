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
package org.apache.drill.exec.store.parquet;

import org.apache.drill.categories.ParquetTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.ParquetFileReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;
import java.util.ArrayList;

import java.io.IOException;

/**
 * This test checks correctness of complex column detection in the Parquet file schema.
 */
@Category({ParquetTest.class, UnlikelyTest.class})
public class TestComplexColumnInSchema extends BaseTest {

  /*
  Parquet schema:
    message root {
      optional int64 id;
      optional binary a (UTF8);
      repeated int64 repeated;
      optional binary VariableCase (UTF8);
      optional group nested {
        optional int64 id;
        repeated int64 repeated;
        optional binary VaRiAbLeCaSe (UTF8);
      }
    }

   Data set:
   complex_special_cases.parquet
   {
     "id": 1,
     "a": "some string",
     "repeated": [1, 2],
     "VariableCase": "top level variable case column",
     "nested": {
       "id": 2,
       "repeated": [3, 4],
       "VaRiAbLeCaSe": "nested variable case column"
     }
   }
   */
  private static final String path = "src/test/resources/store/parquet/complex/complex_special_cases.parquet";
  private static ParquetMetadata footer;

  @BeforeClass
  public static void setup() throws IOException {
    Configuration conf = new Configuration();

    footer = ParquetFileReader.readFooter(conf, new Path(path));
  }

  @Test
  public void testGroupTypeColumn() {
    List<SchemaPath> columns = new ArrayList<>();
    columns.add(SchemaPath.getCompoundPath("nested"));
    assertTrue("GroupType column must be detected as complex",
        ParquetReaderUtility.containsComplexColumn(footer, columns));
  }

  @Test
  public void testNestedColumn() {
    List<SchemaPath> columns = new ArrayList<>();
    columns.add(SchemaPath.getCompoundPath("nested", "id"));
    assertTrue("Nested column must be detected as complex",
        ParquetReaderUtility.containsComplexColumn(footer, columns));
  }

  @Test
  public void testCombinedColumns() {
    List<SchemaPath> columns = new ArrayList<>();
    columns.add(SchemaPath.getCompoundPath("id"));
    columns.add(SchemaPath.getCompoundPath("nested", "id"));
    assertTrue("Nested column in the list list must be detected as complex",
        ParquetReaderUtility.containsComplexColumn(footer, columns));
  }

  @Test
  public void testSimpleColumn() {
    List<SchemaPath> columns = new ArrayList<>();
    columns.add(SchemaPath.getCompoundPath("id"));
    assertFalse("No complex column must be detected",
        ParquetReaderUtility.containsComplexColumn(footer, columns));
  }

  @Test
  public void testSimpleColumns() {
    List<SchemaPath> columns = new ArrayList<>();
    columns.add(SchemaPath.getCompoundPath("id"));
    columns.add(SchemaPath.getCompoundPath("a"));
    assertFalse("No complex columns must be detected",
        ParquetReaderUtility.containsComplexColumn(footer, columns));
  }

  @Test
  public void testNonexistentColumn() {
    List<SchemaPath> columns = new ArrayList<>();
    columns.add(SchemaPath.getCompoundPath("nonexistent"));
    assertFalse("No complex column must be detected",
        ParquetReaderUtility.containsComplexColumn(footer, columns));
  }

  @Test
  public void testVariableCaseColumn() {
    List<SchemaPath> columns = new ArrayList<>();
    columns.add(SchemaPath.getCompoundPath("variablecase"));
    assertFalse("No complex column must be detected",
        ParquetReaderUtility.containsComplexColumn(footer, columns));
  }

  @Test
  public void testVariableCaseSchemaPath() {
    List<SchemaPath> columns = new ArrayList<>();
    columns.add(SchemaPath.getCompoundPath("VaRiAbLeCaSe"));
    assertFalse("No complex column must be detected",
        ParquetReaderUtility.containsComplexColumn(footer, columns));
  }

  @Test
  public void testNestedVariableCaseColumn() {
    List<SchemaPath> columns = new ArrayList<>();
    columns.add(SchemaPath.getCompoundPath("nested", "variablecase"));
    assertTrue("Nested variable case column must be detected as complex",
        ParquetReaderUtility.containsComplexColumn(footer, columns));
  }

  @Test
  public void testRepeatedColumn() {
    List<SchemaPath> columns = new ArrayList<>();
    columns.add(SchemaPath.getCompoundPath("repeated"));
    assertTrue("Repeated column must be detected as complex",
        ParquetReaderUtility.containsComplexColumn(footer, columns));
  }

  @Test
  public void testNestedRepeatedColumn() {
    List<SchemaPath> columns = new ArrayList<>();
    columns.add(SchemaPath.getCompoundPath("nested", "repeated"));
    assertTrue("Nested repeated column must be detected as complex",
        ParquetReaderUtility.containsComplexColumn(footer, columns));
  }
}
