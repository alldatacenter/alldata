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
import org.apache.drill.metastore.iceberg.exceptions.IcebergMetastoreException;
import org.apache.drill.metastore.iceberg.transform.InputDataTransformer;
import org.apache.drill.metastore.iceberg.transform.WriteData;
import org.apache.drill.metastore.metadata.MetadataInfo;
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
import static org.junit.Assert.assertNull;

public class TestTablesInputDataTransformer extends IcebergBaseTest {

  private static Schema metastoreSchema;
  private static Schema partitionSchema;
  private static Map<String, MethodHandle> unitGetters;

  @BeforeClass
  public static void init() {
    metastoreSchema = IcebergTables.SCHEMA.tableSchema();
    partitionSchema = new Schema(IcebergTables.SCHEMA.partitionSpec().partitionType().fields());
    unitGetters = TableMetadataUnit.SCHEMA.unitGetters();
  }

  @Test
  public void testNoData() {
    WriteData writeData = new InputDataTransformer<TableMetadataUnit>(metastoreSchema, partitionSchema, unitGetters)
      .units(Collections.emptyList())
      .execute();

    assertEquals(Collections.emptyList(), writeData.records());
    assertNull(writeData.partition());
  }

  @Test
  public void testValidDataOneRecord() {
    Map<String, String> partitionKeys = new HashMap<>();
    partitionKeys.put("dir0", "2018");
    partitionKeys.put("dir1", "2019");
    List<String> partitionValues = Arrays.asList("a", "b", "c");
    Long lastModifiedTime = System.currentTimeMillis();

    TableMetadataUnit unit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .partitionKeys(partitionKeys)
      .partitionValues(partitionValues)
      .lastModifiedTime(lastModifiedTime)
      .build();

    WriteData writeData = new InputDataTransformer<TableMetadataUnit>(metastoreSchema, partitionSchema, unitGetters)
      .units(Collections.singletonList(unit))
      .execute();

    Record tableRecord = GenericRecord.create(metastoreSchema);
    tableRecord.setField("storagePlugin", "dfs");
    tableRecord.setField("workspace", "tmp");
    tableRecord.setField("tableName", "nation");
    tableRecord.setField("metadataKey", MetadataInfo.GENERAL_INFO_KEY);
    tableRecord.setField("partitionKeys", partitionKeys);
    tableRecord.setField("partitionValues", partitionValues);
    tableRecord.setField("lastModifiedTime", lastModifiedTime);

    Record partitionRecord = GenericRecord.create(partitionSchema);
    partitionRecord.setField("storagePlugin", "dfs");
    partitionRecord.setField("workspace", "tmp");
    partitionRecord.setField("tableName", "nation");
    partitionRecord.setField("metadataKey", MetadataInfo.GENERAL_INFO_KEY);

    assertEquals(Collections.singletonList(tableRecord), writeData.records());
    assertEquals(partitionRecord, writeData.partition());
  }

  @Test
  public void testValidDataSeveralRecords() {
    List<TableMetadataUnit> units = Arrays.asList(
      TableMetadataUnit.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
        .column("a")
        .build(),
      TableMetadataUnit.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
        .column("b")
        .build(),
      TableMetadataUnit.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
        .column("c")
        .build());

    WriteData writeData = new InputDataTransformer<TableMetadataUnit>(metastoreSchema, partitionSchema, unitGetters)
      .units(units)
      .execute();

    Record tableRecord1 = GenericRecord.create(metastoreSchema);
    tableRecord1.setField("storagePlugin", "dfs");
    tableRecord1.setField("workspace", "tmp");
    tableRecord1.setField("tableName", "nation");
    tableRecord1.setField("metadataKey", MetadataInfo.GENERAL_INFO_KEY);
    tableRecord1.setField("column", "a");

    Record tableRecord2 = GenericRecord.create(metastoreSchema);
    tableRecord2.setField("storagePlugin", "dfs");
    tableRecord2.setField("workspace", "tmp");
    tableRecord2.setField("tableName", "nation");
    tableRecord2.setField("metadataKey", MetadataInfo.GENERAL_INFO_KEY);
    tableRecord2.setField("column", "b");

    Record tableRecord3 = GenericRecord.create(metastoreSchema);
    tableRecord3.setField("storagePlugin", "dfs");
    tableRecord3.setField("workspace", "tmp");
    tableRecord3.setField("tableName", "nation");
    tableRecord3.setField("metadataKey", MetadataInfo.GENERAL_INFO_KEY);
    tableRecord3.setField("column", "c");

    Record partitionRecord = GenericRecord.create(partitionSchema);
    partitionRecord.setField("storagePlugin", "dfs");
    partitionRecord.setField("workspace", "tmp");
    partitionRecord.setField("tableName", "nation");
    partitionRecord.setField("metadataKey", MetadataInfo.GENERAL_INFO_KEY);

    assertEquals(Arrays.asList(tableRecord1, tableRecord2, tableRecord3), writeData.records());
    assertEquals(partitionRecord, writeData.partition());
  }

  @Test
  public void testInvalidPartition() {
    TableMetadataUnit unit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .build();

    thrown.expect(IcebergMetastoreException.class);

    new InputDataTransformer<TableMetadataUnit>(metastoreSchema, partitionSchema, unitGetters)
      .units(Collections.singletonList(unit))
      .execute();
  }

  @Test
  public void testNonMatchingPartitionKey() {
    List<TableMetadataUnit> units = Arrays.asList(
      TableMetadataUnit.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("a")
        .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
        .build(),
      TableMetadataUnit.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("b")
        .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
        .build(),
      TableMetadataUnit.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("c")
        .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
        .build());

    thrown.expect(IcebergMetastoreException.class);

    new InputDataTransformer<TableMetadataUnit>(metastoreSchema, partitionSchema, unitGetters)
      .units(units)
      .execute();
  }
}
