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
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.mongo.config.MongoConfigConstants;
import org.apache.drill.metastore.mongo.transform.InputDataTransformer;
import org.bson.Document;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TestTablesInputDataTransformer {
  @Test
  public void testNoData() {
    List<Document> documents =
      new InputDataTransformer<TableMetadataUnit>(TableMetadataUnit.SCHEMA.unitGetters())
        .units(Collections.emptyList())
        .execute();
    assertEquals(Collections.emptyList(), documents);
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
      .metadataType(MetadataType.TABLE.name())
      .metadataIdentifier(MetadataInfo.GENERAL_INFO_KEY)
      .partitionKeys(partitionKeys)
      .partitionValues(partitionValues)
      .lastModifiedTime(lastModifiedTime)
      .build();

    InputDataTransformer<TableMetadataUnit> inputDataTransformer =
      new InputDataTransformer<>(TableMetadataUnit.SCHEMA.unitGetters());
    List<Document> documents = inputDataTransformer
      .units(Collections.singletonList(unit))
      .execute();

    Document tableRecord = new Document();
    tableRecord.append("storagePlugin", "dfs");
    tableRecord.append("workspace", "tmp");
    tableRecord.append("tableName", "nation");
    tableRecord.append("metadataType", "TABLE");
    tableRecord.append("metadataIdentifier", MetadataInfo.GENERAL_INFO_KEY);
    assertEquals(tableRecord, documents.get(0).get(MongoConfigConstants.ID));
    assertEquals(partitionKeys, documents.get(0).get("partitionKeys"));
    assertEquals(partitionValues, documents.get(0).get("partitionValues"));
    assertEquals(lastModifiedTime, documents.get(0).get("lastModifiedTime"));
  }

  @Test
  public void testValidDataSeveralRecords() {
    List<TableMetadataUnit> units = Arrays.asList(
      TableMetadataUnit.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataType(MetadataType.TABLE.name())
        .metadataIdentifier(MetadataInfo.GENERAL_INFO_KEY)
        .column("a")
        .build(),
      TableMetadataUnit.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataType(MetadataType.TABLE.name())
        .metadataIdentifier(MetadataInfo.GENERAL_INFO_KEY)
        .column("b")
        .build(),
      TableMetadataUnit.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .tableName("nation")
        .metadataType(MetadataType.TABLE.name())
        .metadataIdentifier(MetadataInfo.GENERAL_INFO_KEY)
        .column("c")
        .build());

    InputDataTransformer<TableMetadataUnit> inputDataTransformer =
      new InputDataTransformer<>(TableMetadataUnit.SCHEMA.unitGetters());
    List<Document> documents = inputDataTransformer
      .units(units)
      .execute();

    Document tableRecord1 = new Document();
    tableRecord1.append("storagePlugin", "dfs");
    tableRecord1.append("workspace", "tmp");
    tableRecord1.append("tableName", "nation");
    tableRecord1.append("metadataType", "TABLE");
    tableRecord1.append("metadataIdentifier", MetadataInfo.GENERAL_INFO_KEY);

    Document tableRecord2 = new Document();
    tableRecord2.append("storagePlugin", "dfs");
    tableRecord2.append("workspace", "tmp");
    tableRecord2.append("tableName", "nation");
    tableRecord2.append("metadataType", "TABLE");
    tableRecord2.append("metadataIdentifier", MetadataInfo.GENERAL_INFO_KEY);

    Document tableRecord3 = new Document();
    tableRecord3.append("storagePlugin", "dfs");
    tableRecord3.append("workspace", "tmp");
    tableRecord3.append("tableName", "nation");
    tableRecord3.append("metadataType", "TABLE");
    tableRecord3.append("metadataIdentifier", MetadataInfo.GENERAL_INFO_KEY);

    assertEquals(tableRecord1, documents.get(0).get(MongoConfigConstants.ID));
    assertEquals(tableRecord2, documents.get(1).get(MongoConfigConstants.ID));
    assertEquals(tableRecord3, documents.get(2).get(MongoConfigConstants.ID));
  }
}
