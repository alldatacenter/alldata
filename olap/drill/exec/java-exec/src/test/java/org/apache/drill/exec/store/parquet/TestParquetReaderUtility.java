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
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.hadoop.metadata.ParquetMetadata;
import org.apache.parquet.hadoop.ParquetFileReader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.Map;

@Category({ParquetTest.class, UnlikelyTest.class})
public class TestParquetReaderUtility extends BaseTest {

  private static final String path = "src/test/resources/store/parquet/complex/complex.parquet";
  private static ParquetMetadata footer;

  @BeforeClass
  public static void setup() throws IOException {
    Configuration conf = new Configuration();

    footer = ParquetFileReader.readFooter(conf, new Path(path));
  }

  @Test
  public void testSchemaElementsMap() {
    Map<String, SchemaElement> schemaElements = ParquetReaderUtility.getColNameToSchemaElementMapping(footer);
    assertEquals("Schema elements map size must be 14", schemaElements.size(), 14);

    SchemaElement schemaElement = schemaElements.get("`marketing_info`.`camp_id`");
    assertNotNull("Schema element must be not null", schemaElement);
    assertEquals("Schema element must be named 'camp_id'", schemaElement.getName(), "camp_id");

    schemaElement = schemaElements.get("`marketing_info`");
    assertNotNull("Schema element must be not null", schemaElement);
    assertEquals("Schema element name match lookup key", schemaElement.getName(), "marketing_info");
  }

  @Test
  public void testColumnDescriptorMap() {
    Map<String, ColumnDescriptor> colDescMap = ParquetReaderUtility.getColNameToColumnDescriptorMapping(footer);
    assertEquals("Column descriptors map size must be 11", colDescMap.size(), 11);

    assertNotNull("column descriptor lookup must return not null", colDescMap.get("`marketing_info`.`camp_id`"));
    assertNull("column descriptor lookup must return null on GroupType column", colDescMap.get("`marketing_info`"));
  }
}
