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
package org.apache.drill.metastore.iceberg.components.tables;

import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.iceberg.IcebergBaseTest;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestTablesOutputDataTransformer extends IcebergBaseTest {

  private static Map<String, MethodHandle> unitSetters;
  private static Schema schema;

  @BeforeClass
  public static void init() {
    unitSetters = TableMetadataUnit.SCHEMA.unitBuilderSetters();
    schema = IcebergTables.SCHEMA.tableSchema();
  }

  @Test
  public void testNoData() {
    List<TableMetadataUnit> actualResult = new TablesOutputDataTransformer(unitSetters)
      .columns("storagePlugin", "workspace", "tableName")
      .records(Collections.emptyList())
      .execute();

    assertEquals(Collections.emptyList(), actualResult);
  }

  @Test
  public void testValidDataOneRecord() {
    Map<String, String> partitionKeys = new HashMap<>();
    partitionKeys.put("dir0", "2018");
    partitionKeys.put("dir1", "2019");
    List<String> partitionValues = Arrays.asList("a", "b", "c");
    Long lastModifiedTime = System.currentTimeMillis();

    Record record = GenericRecord.create(schema);
    record.setField("storagePlugin", "dfs");
    record.setField("workspace", "tmp");
    record.setField("tableName", "nation");
    record.setField("partitionKeys", partitionKeys);
    record.setField("partitionValues", partitionValues);
    record.setField("lastModifiedTime", lastModifiedTime);

    List<TableMetadataUnit> actualResult = new TablesOutputDataTransformer(unitSetters)
      .columns(Arrays.asList("storagePlugin", "workspace", "tableName",
        "partitionKeys", "partitionValues", "lastModifiedTime"))
      .records(Collections.singletonList(record))
      .execute();

    List<TableMetadataUnit> expectedResult = Collections.singletonList(TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .partitionKeys(partitionKeys)
      .partitionValues(partitionValues)
      .lastModifiedTime(lastModifiedTime)
      .build());

    assertEquals(expectedResult, actualResult);
  }

  @Test
  public void testValidDataSeveralRecords() {
    Record record1 = GenericRecord.create(schema);
    record1.setField("tableName", "a");

    Record record2 = GenericRecord.create(schema);
    record2.setField("tableName", "b");

    Record record3 = GenericRecord.create(schema);
    record3.setField("tableName", "c");


    List<TableMetadataUnit> actualResult = new TablesOutputDataTransformer(unitSetters)
      .columns(Collections.singletonList("tableName"))
      .records(Arrays.asList(record1, record2, record3))
      .execute();

    List<TableMetadataUnit> expectedResult = Arrays.asList(
      TableMetadataUnit.builder().tableName("a").build(),
      TableMetadataUnit.builder().tableName("b").build(),
      TableMetadataUnit.builder().tableName("c").build());

    assertEquals(expectedResult, actualResult);
  }

  @Test
  public void testInvalidColumns() {
    Record record = GenericRecord.create(schema);
    record.setField("tableName", "a");

    List<TableMetadataUnit> actualResult = new TablesOutputDataTransformer(unitSetters)
      .records(Collections.singletonList(record))
      .columns(Arrays.asList("a", "b"))
      .execute();

    List<TableMetadataUnit> expectedResult = Collections.singletonList(TableMetadataUnit.builder().build());

    assertEquals(expectedResult, actualResult);
  }
}
