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
package org.apache.drill.metastore.components.tables;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.categories.MetastoreTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.MetastoreRegistry;
import org.apache.drill.metastore.config.MetastoreConfigConstants;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.metastore.operate.Delete;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.test.BaseTest;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(MetastoreTest.class)
public abstract class AbstractTablesMetastoreTest extends BaseTest {

  @ClassRule
  public static TemporaryFolder defaultFolder = new TemporaryFolder();

  protected static Tables tables;

  protected static void innerInit(Config config, Class<?> implementationClass) {
    DrillConfig drillConfig = new DrillConfig(config
      .withValue(MetastoreConfigConstants.USE_PROVIDED_CONFIG,
        ConfigValueFactory.fromAnyRef(true))
      .withValue(MetastoreConfigConstants.IMPLEMENTATION_CLASS,
        ConfigValueFactory.fromAnyRef(implementationClass.getName())));
    tables = new MetastoreRegistry(drillConfig).get().tables();
  }

  @After
  public void cleanUp() {
    tables.modify().purge();
  }

  @Test
  public void testWriteReadAllFieldTypes() {
    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name("nation")
      .build();

    TableMetadataUnit tableUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE.name())
      .metadataStatistics(Collections.singletonList("{\"statisticsValue\":2.1," +
        "\"statisticsKind\":{\"name\":\"approx_count_distinct\"}}"))
      .columnsStatistics(Collections.singletonMap("`name`", "{\"statistics\":[{\"statisticsValue\":\"aaa\"," +
        "\"statisticsKind\":{\"exact\":true,\"name\":\"minValue\"}},{\"statisticsValue\":\"zzz\"," +
        "\"statisticsKind\":{\"exact\":true,\"name\":\"maxValue\"}}],\"type\":\"VARCHAR\"}"))
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    TableMetadataUnit rowGroupUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey("1994")
      .metadataIdentifier("1994/Q1/0_0_0.parquet/1")
      .metadataType(MetadataType.ROW_GROUP.name())
      .lastModifiedTime(System.currentTimeMillis())
      .location("/tmp/nation")
      .path("/tmp/nation/1/0_0_0.parquet")
      .rowGroupIndex(1)
      .hostAffinity(Collections.singletonMap("host1", 0.1F))
      .build();

    tables.modify()
      .overwrite(tableUnit, rowGroupUnit)
      .execute();

    List<TableMetadataUnit> tableUnits = tables.read()
      .metadataType(MetadataType.TABLE)
      .filter(tableInfo.toFilter())
      .execute();

    assertEquals(1, tableUnits.size());
    assertEquals(tableUnit, tableUnits.get(0));

    List<TableMetadataUnit> rowGroupUnits = tables.read()
      .metadataType(MetadataType.ROW_GROUP)
      .filter(tableInfo.toFilter())
      .execute();

    assertEquals(1, rowGroupUnits.size());
    assertEquals(rowGroupUnit, rowGroupUnits.get(0));
  }

  @Test
  public void testReadSelectedColumns() {
    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name("nation")
      .build();

    TableMetadataUnit unit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE.name())
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    tables.modify()
      .overwrite(unit)
      .execute();

    List<TableMetadataUnit> units = tables.read()
      .metadataType(MetadataType.ALL)
      .filter(tableInfo.toFilter())
      .columns(MetastoreColumn.TABLE_NAME, MetastoreColumn.METADATA_KEY)
      .execute();

    assertEquals(1, units.size());
    assertEquals(TableMetadataUnit.builder()
      .tableName("nation")
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .build(),
      units.get(0));
  }

  @Test
  public void testReadNoResult() {
    List<TableMetadataUnit> units = tables.read()
      .metadataType(MetadataType.ALL)
      .filter(FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs"))
      .columns(MetastoreColumn.TABLE_NAME, MetastoreColumn.METADATA_KEY)
      .execute();

    assertTrue(units.isEmpty());
  }

  @Test
  public void testReadAbsentColumns() {
    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name("nation")
      .build();

    TableMetadataUnit unit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE.name())
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    tables.modify()
      .overwrite(unit)
      .execute();

    List<TableMetadataUnit> units = tables.read()
      .metadataType(MetadataType.TABLE)
      .filter(tableInfo.toFilter())
      .columns(MetastoreColumn.TABLE_NAME, MetastoreColumn.HOST_AFFINITY)
      .execute();

    assertEquals(1, units.size());
    assertEquals(TableMetadataUnit.builder()
        .tableName("nation")
        .build(),
      units.get(0));

    units = tables.read()
      .metadataType(MetadataType.TABLE)
      .filter(tableInfo.toFilter())
      .columns(MetastoreColumn.HOST_AFFINITY)
      .execute();

    assertEquals(1, units.size());
    assertEquals(TableMetadataUnit.EMPTY_UNIT, units.get(0));
  }

  @Test
  public void testFilterByAbsentColumns() {
    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name("nation")
      .build();

    TableMetadataUnit unit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE.name())
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    tables.modify()
      .overwrite(unit)
      .execute();

    List<TableMetadataUnit> units = tables.read()
      .metadataType(MetadataType.TABLE)
      .filter(FilterExpression.equal(MetastoreColumn.PATH, "abc"))
      .columns(MetastoreColumn.TABLE_NAME)
      .execute();

    assertEquals(0, units.size());

    units = tables.read()
      .metadataType(MetadataType.TABLE)
      .filter(FilterExpression.and(
        FilterExpression.equal(MetastoreColumn.TABLE_NAME, "nation"),
        FilterExpression.isNull(MetastoreColumn.PARTITION_VALUES),
        FilterExpression.notEqual(MetastoreColumn.PATH, "abc"),
        FilterExpression.isNull(MetastoreColumn.COLUMN),
        FilterExpression.notIn(MetastoreColumn.ROW_GROUP_INDEX, 1, 2)))
      .columns(MetastoreColumn.TABLE_NAME)
      .execute();

    assertEquals(TableMetadataUnit.builder()
        .tableName("nation")
        .build(),
      units.get(0));
  }

  @Test
  public void testOverwrite() {
    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name("nation")
      .build();

    TableMetadataUnit initialUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE.name())
      .tableType("parquet")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    tables.modify()
      .overwrite(initialUnit)
      .execute();

    List<TableMetadataUnit> units = tables.read()
      .metadataType(MetadataType.TABLE)
      .filter(tableInfo.toFilter())
      .execute();

    assertEquals(1, units.size());
    assertEquals(initialUnit, units.get(0));

    TableMetadataUnit updatedUnit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE.name())
      .tableType("text")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    tables.modify()
      .overwrite(updatedUnit)
      .execute();

    List<TableMetadataUnit> updatedUnits = tables.read()
      .metadataType(MetadataType.TABLE)
      .filter(tableInfo.toFilter())
      .execute();

    assertEquals(1, updatedUnits.size());
    assertEquals(updatedUnit, updatedUnits.get(0));
  }

  @Test
  public void testOverwriteSeveralUnits() {
    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name("nation")
      .build();

    TableMetadataUnit topLevelSegment = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey("1994")
      .metadataIdentifier("1994")
      .metadataType(MetadataType.SEGMENT.name())
      .location("/tmp/nation/1994")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    TableMetadataUnit firstNestedSegment = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey("1994")
      .metadataIdentifier("1994/Q1")
      .metadataType(MetadataType.SEGMENT.name())
      .location("/tmp/nation/1994/Q1")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    TableMetadataUnit secondNestedSegment = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey("1994")
      .metadataIdentifier("1994/Q2")
      .metadataType(MetadataType.SEGMENT.name())
      .location("/tmp/nation/1994/Q2")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    tables.modify()
      .overwrite(topLevelSegment, firstNestedSegment, secondNestedSegment)
      .execute();

    List<TableMetadataUnit> units = tables.read()
      .metadataType(MetadataType.SEGMENT)
      .filter(tableInfo.toFilter())
      .execute();

    assertEquals(3, units.size());

    tables.modify()
      .overwrite(topLevelSegment, firstNestedSegment)
      .execute();

    List<TableMetadataUnit> updatedUnits = tables.read()
      .metadataType(MetadataType.SEGMENT)
      .filter(tableInfo.toFilter())
      .execute();

    assertEquals(2, updatedUnits.size());

    Set<String> metadataIdentifiers = updatedUnits.stream()
      .map(TableMetadataUnit::metadataIdentifier)
      .collect(Collectors.toSet());

    assertEquals(Sets.newHashSet("1994", "1994/Q1"), metadataIdentifiers);
  }

  @Test
  public void testDelete() {
    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name("nation")
      .build();

    TableMetadataUnit firstUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey("1994")
      .metadataIdentifier("1994")
      .metadataType(MetadataType.SEGMENT.name())
      .location("/tmp/nation/1994")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    TableMetadataUnit secondUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey("1995")
      .metadataIdentifier("1995")
      .metadataType(MetadataType.SEGMENT.name())
      .location("/tmp/nation/1995")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    tables.modify()
      .overwrite(firstUnit, secondUnit)
      .execute();

    List<TableMetadataUnit> units = tables.read()
      .metadataType(MetadataType.SEGMENT)
      .filter(tableInfo.toFilter())
      .execute();

    assertEquals(2, units.size());

    FilterExpression deleteFilter = FilterExpression.and(
      tableInfo.toFilter(),
      FilterExpression.equal(MetastoreColumn.METADATA_KEY, "1994"));

    tables.modify()
      .delete(Delete.builder()
        .metadataType(MetadataType.SEGMENT)
        .filter(deleteFilter)
        .build())
      .execute();

    List<TableMetadataUnit> updatedUnits = tables.read()
      .metadataType(MetadataType.SEGMENT)
      .filter(tableInfo.toFilter())
      .execute();

    assertEquals(1, updatedUnits.size());
    assertEquals(secondUnit, updatedUnits.get(0));
  }

  @Test
  public void testOverwriteDeleteOrder() {
    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name("nation")
      .build();

    TableMetadataUnit initialUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .metadataType(MetadataType.TABLE.name())
      .lastModifiedTime(System.currentTimeMillis())
      .tableType("parquet")
      .build();

    TableMetadataUnit updatedUnit = initialUnit.toBuilder()
      .tableType("text")
      .build();

    // add initial unit
    tables.modify()
      .overwrite(initialUnit)
      .execute();

    // first delete, then overwrite
    tables.modify()
      .delete(Delete.builder()
        .metadataType(MetadataType.TABLE)
        .build())
      .overwrite(updatedUnit)
      .execute();

    // check that unit is present and updated
    List<TableMetadataUnit> resultAfterDeleteOverwrite = tables.read()
      .metadataType(MetadataType.TABLE)
      .execute();

    assertEquals(1, resultAfterDeleteOverwrite.size());
    assertEquals(updatedUnit, resultAfterDeleteOverwrite.get(0));

    // first overwrite, then delete
    tables.modify()
      .overwrite(initialUnit)
      .delete(Delete.builder()
        .metadataType(MetadataType.TABLE)
        .build())
      .execute();

    // check that units are absent
    List<TableMetadataUnit> resultAfterOverwriteDelete = tables.read()
      .metadataType(MetadataType.TABLE)
      .execute();

    assertEquals(0, resultAfterOverwriteDelete.size());
  }

  @Test
  public void testOverwriteAndDeleteInOneTransaction() {
    TableInfo tableInfo = TableInfo.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .name("nation")
      .build();

    TableMetadataUnit firstUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey("1994")
      .metadataIdentifier("1994")
      .metadataType(MetadataType.SEGMENT.name())
      .location("/tmp/nation/1994")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    TableMetadataUnit secondUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey("1995")
      .metadataIdentifier("1995")
      .metadataType(MetadataType.SEGMENT.name())
      .location("/tmp/nation/1995")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    tables.modify()
      .overwrite(firstUnit, secondUnit)
      .execute();

    List<TableMetadataUnit> units = tables.read()
      .metadataType(MetadataType.SEGMENT)
      .filter(tableInfo.toFilter())
      .execute();

    assertEquals(2, units.size());

    FilterExpression deleteFilter = FilterExpression.and(
      tableInfo.toFilter(),
      FilterExpression.equal(MetastoreColumn.METADATA_KEY, "1994"));

    TableMetadataUnit updatedUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataKey("1995")
      .metadataIdentifier("1995")
      .metadataType(MetadataType.SEGMENT.name())
      .location("/user/nation/1995")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    tables.modify()
      .delete(Delete.builder()
        .metadataType(MetadataType.SEGMENT)
        .filter(deleteFilter)
        .build())
      .overwrite(updatedUnit)
      .execute();

    List<TableMetadataUnit> updatedUnits = tables.read()
      .metadataType(MetadataType.SEGMENT)
      .filter(tableInfo.toFilter())
      .execute();

    assertEquals(1, updatedUnits.size());
    assertEquals(updatedUnit, updatedUnits.get(0));
  }

  @Test
  public void testPurge() {
    TableMetadataUnit firstUnit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataKey("dir0")
      .metadataType(MetadataType.TABLE.name())
      .tableType("parquet")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    TableMetadataUnit secondUnit = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataKey("1994")
      .metadataIdentifier("1994")
      .metadataType(MetadataType.SEGMENT.name())
      .location("/tmp/nation/1994")
      .lastModifiedTime(System.currentTimeMillis())
      .build();

    tables.modify()
      .overwrite(firstUnit, secondUnit)
      .execute();

    List<TableMetadataUnit> initialUnits = tables.read()
      .metadataType(MetadataType.ALL)
      .execute();

    assertEquals(2, initialUnits.size());

    tables.modify()
      .purge();

    List<TableMetadataUnit> resultingUnits = tables.read()
      .metadataType(MetadataType.ALL)
      .execute();

    assertTrue(resultingUnits.isEmpty());
  }
}
