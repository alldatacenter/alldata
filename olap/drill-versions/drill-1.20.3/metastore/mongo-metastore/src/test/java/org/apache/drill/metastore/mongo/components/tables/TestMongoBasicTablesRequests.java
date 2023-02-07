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

import com.clearspring.analytics.util.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.record.SchemaUtil;
import org.apache.drill.metastore.components.tables.BasicTablesTransformer;
import org.apache.drill.metastore.components.tables.MetastoreTableInfo;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.mongo.MongoBaseTest;
import org.apache.drill.metastore.operate.Delete;
import org.apache.drill.metastore.operate.Metadata;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.base.Joiner;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestMongoBasicTablesRequests extends MongoBaseTest {

  @Test
  public void testMetastoreTableInfoExistingTable() {
    MetastoreTableInfo metastoreTableInfo = basicRequests.metastoreTableInfo(nationTableInfo);
    assertTrue(metastoreTableInfo.isExists());
    assertEquals(nationTableInfo, metastoreTableInfo.tableInfo());
    assertEquals(nationTable.lastModifiedTime(), metastoreTableInfo.lastModifiedTime());
    assertEquals(Metadata.UNDEFINED, metastoreTableInfo.metastoreVersion());
  }

  @Test
  public void testDelete() {
    MetastoreTableInfo metastoreTableInfo = basicRequests.metastoreTableInfo(nationTableInfo);
    assertTrue(metastoreTableInfo.isExists());
    tables.modify()
      .delete(Delete.builder()
        .metadataType(MetadataType.TABLE)
        .filter(nationTableInfo.toFilter())
        .build())
      .execute();
    metastoreTableInfo = basicRequests.metastoreTableInfo(nationTableInfo);
    assertFalse(metastoreTableInfo.isExists());

    List<TableMetadataUnit> res =
      tables.read().metadataType(MetadataType.ALL).execute();
    assertFalse(res.isEmpty());
    tables.modify().purge();
    res = tables.read().metadataType(MetadataType.ALL).execute();
    assertTrue(res.isEmpty());

    prepareData(tables);
  }

  @Test
  public void testTableMetastoreSchemaParse() {
    TableMetadataUnit unit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("test")
      .owner("user")
      .tableType("json")
      .metadataType(MetadataType.TABLE.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .location("/tmp/donuts.json")
      .schema("{\"type\":\"tuple_schema\",\"columns\":[" +
        "{\"name\":\"id\",\"type\":\"VARCHAR\",\"mode\":\"OPTIONAL\"}," +
        "{\"name\":\"type\",\"type\":\"VARCHAR\",\"mode\":\"OPTIONAL\"}," +
        "{\"name\":\"name\",\"type\":\"VARCHAR\",\"mode\":\"OPTIONAL\"}," +
        "{\"name\":\"ppu\",\"type\":\"DOUBLE\",\"mode\":\"OPTIONAL\"}," +
        "{\"name\":\"sales\",\"type\":\"BIGINT\",\"mode\":\"OPTIONAL\"}," +
        "{\"name\":\"batters\",\"type\":\"STRUCT<`batter` ARRAY<STRUCT<`id` VARCHAR, `type` VARCHAR>>>\",\"mode\":\"REQUIRED\"}," +
        "{\"name\":\"topping\",\"type\":\"ARRAY<STRUCT<`id` VARCHAR, `type` VARCHAR>>\",\"mode\":\"REPEATED\"}," +
        "{\"name\":\"filling\",\"type\":\"ARRAY<STRUCT<`id` VARCHAR, `type` VARCHAR>>\",\"mode\":\"REPEATED\"}]}")
      .lastModifiedTime(System.currentTimeMillis())
      .columnsStatistics(Maps.newHashMap())
      .metadataStatistics(Lists.newArrayList())
      .partitionKeys(Maps.newHashMap())
      .build();
    List<SchemaPath> schemaPaths =
      SchemaUtil.getSchemaPaths(BasicTablesTransformer.tables(Collections.singletonList(unit)).get(0).getSchema());
    Set<String> schemaSet =
      schemaPaths.stream().map(SchemaPath::toString).collect(Collectors.toSet());
    assertEquals(String.format("Schema path is not parsed correctly:%s",
      Joiner.on(",").join(schemaPaths)), schemaPaths.size(), schemaSet.size());
  }
}
