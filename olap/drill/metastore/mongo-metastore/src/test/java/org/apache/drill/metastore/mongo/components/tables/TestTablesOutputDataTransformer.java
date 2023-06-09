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
package org.apache.drill.metastore.mongo.components.tables;

import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.bson.Document;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.invoke.MethodHandle;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestTablesOutputDataTransformer {

  private static Map<String, MethodHandle> unitSetters;

  @BeforeClass
  public static void init() {
    unitSetters = TableMetadataUnit.SCHEMA.unitBuilderSetters();
  }

  @Test
  public void testNoData() {
    List<TableMetadataUnit> actualResult = new TablesOutputDataTransformer(unitSetters)
      .columns(Arrays.asList("storagePlugin", "workspace", "tableName"))
      .documents(Collections.emptyList())
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

    Document record = new Document();
    record.append("storagePlugin", "dfs");
    record.append("workspace", "tmp");
    record.append("tableName", "nation");
    record.append("partitionKeys", partitionKeys);
    record.append("partitionValues", partitionValues);
    record.append("lastModifiedTime", lastModifiedTime);

    List<TableMetadataUnit> actualResult = new TablesOutputDataTransformer(unitSetters)
      .columns(Arrays.asList("storagePlugin", "workspace", "tableName",
        "partitionKeys", "partitionValues", "lastModifiedTime"))
      .documents(Collections.singletonList(record))
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
    Document record1 = new Document();
    record1.append("tableName", "a");

    Document record2 = new Document();
    record2.append("tableName", "b");

    Document record3 = new Document();
    record3.append("tableName", "c");


    List<TableMetadataUnit> actualResult = new TablesOutputDataTransformer(unitSetters)
      .columns(Collections.singletonList("tableName"))
      .documents(Arrays.asList(record1, record2, record3))
      .execute();

    List<TableMetadataUnit> expectedResult = Arrays.asList(
      TableMetadataUnit.builder().tableName("a").build(),
      TableMetadataUnit.builder().tableName("b").build(),
      TableMetadataUnit.builder().tableName("c").build());

    assertEquals(expectedResult, actualResult);
  }

  @Test
  public void testInvalidColumns() {
    Document record = new Document();
    record.append("tableName", "a");

    List<TableMetadataUnit> actualResult = new TablesOutputDataTransformer(unitSetters)
      .documents(Collections.singletonList(record))
      .columns(Arrays.asList("a", "b"))
      .execute();

    assertEquals(Collections.emptyList(), actualResult);
  }
}
