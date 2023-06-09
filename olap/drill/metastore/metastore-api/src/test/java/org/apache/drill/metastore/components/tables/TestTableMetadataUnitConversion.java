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

import org.apache.drill.categories.MetastoreTest;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.metastore.metadata.BaseMetadata;
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.metastore.statistics.ColumnStatisticsKind;
import org.apache.drill.metastore.statistics.StatisticsHolder;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.fs.Path;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category(MetastoreTest.class)
public class TestTableMetadataUnitConversion extends BaseTest {

  private static Data data;

  @BeforeClass
  public static void init() {
    data = new Data();
  }

  @Test
  public void testBaseTableMetadata() {
    TableInfo tableInfo = data.fullTableInfo;

    MetadataInfo metadataInfo = MetadataInfo.builder()
      .type(MetadataType.TABLE)
      .key(MetadataInfo.GENERAL_INFO_KEY)
      .build();

    Map<String, String> partitionKeys = new HashMap<>();
    partitionKeys.put("dir0", "2018");
    partitionKeys.put("dir1", "2019");

    // check required fields
    BaseTableMetadata requiredFieldsMetadata = BaseTableMetadata.builder()
      .tableInfo(tableInfo)
      .metadataInfo(metadataInfo)
      .columnsStatistics(data.columnsStatistics)
      .metadataStatistics(data.metadataStatistics)
      .partitionKeys(partitionKeys)
      .build();

    TableMetadataUnit requiredFieldsExpectedUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .owner(tableInfo.owner())
      .tableType(tableInfo.type())
      .metadataType(metadataInfo.type().name())
      .metadataKey(metadataInfo.key())
      .metadataIdentifier(metadataInfo.identifier())
      .columnsStatistics(data.unitColumnsStatistics)
      .metadataStatistics(data.unitMetadataStatistics)
      .lastModifiedTime(BaseMetadata.UNDEFINED_TIME)
      .partitionKeys(partitionKeys)
      .build();

    TableMetadataUnit requiredFieldsUnit = requiredFieldsMetadata.toMetadataUnit();

    assertEquals(requiredFieldsExpectedUnit, requiredFieldsUnit);
    assertNotNull(BaseTableMetadata.builder().metadataUnit(requiredFieldsUnit).build());

    Path location = new Path("/tmp/nation");
    List<SchemaPath> interestingColumns = Arrays.asList(SchemaPath.getSimplePath("a"), SchemaPath.getSimplePath("b"));

    // check all fields
    BaseTableMetadata allFieldsMetadata = BaseTableMetadata.builder()
      .tableInfo(tableInfo)
      .metadataInfo(metadataInfo)
      .schema(data.schema)
      .columnsStatistics(data.columnsStatistics)
      .metadataStatistics(data.metadataStatistics)
      .lastModifiedTime(data.lastModifiedTime)
      .location(location)
      .partitionKeys(partitionKeys)
      .interestingColumns(interestingColumns)
      .build();

    TableMetadataUnit allFieldsExpectedUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .owner(tableInfo.owner())
      .tableType(tableInfo.type())
      .metadataType(metadataInfo.type().name())
      .metadataKey(metadataInfo.key())
      .metadataIdentifier(metadataInfo.identifier())
      .schema(data.unitSchema)
      .columnsStatistics(data.unitColumnsStatistics)
      .metadataStatistics(data.unitMetadataStatistics)
      .lastModifiedTime(data.lastModifiedTime)
      .location(location.toUri().getPath())
      .partitionKeys(partitionKeys)
      .interestingColumns(interestingColumns.stream()
        .map(SchemaPath::toString)
        .collect(Collectors.toList()))
      .build();

    TableMetadataUnit allFieldsUnit = allFieldsMetadata.toMetadataUnit();

    assertEquals(allFieldsExpectedUnit, allFieldsUnit);
    assertNotNull(BaseTableMetadata.builder().metadataUnit(allFieldsUnit).build());
  }

  @Test
  public void testSegmentMetadata() {
    TableInfo tableInfo = data.basicTableInfo;

    MetadataInfo metadataInfo = MetadataInfo.builder()
      .type(MetadataType.SEGMENT)
      .key("part_int=3")
      .identifier("part_int=3/part_varchar=g")
      .build();

    Path path = new Path("/tmp/nation");
    String unitPath = path.toUri().getPath();

    Set<Path> locations = new HashSet<>();
    locations.add(new Path("part_int=3/part_varchar=g/0_0_0.parquet"));
    locations.add(new Path("part_int=3/part_varchar=g/0_0_1.parquet"));

    List<String> unitLocations = locations.stream()
      .map(location -> location.toUri().getPath())
      .collect(Collectors.toList());

    // check required fields
    SegmentMetadata requiredFieldsMetadata = SegmentMetadata.builder()
      .tableInfo(tableInfo)
      .metadataInfo(metadataInfo)
      .columnsStatistics(data.columnsStatistics)
      .metadataStatistics(data.metadataStatistics)
      .path(path)
      .locations(locations)
      .build();

    TableMetadataUnit requiredFieldsExpectedUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataType(metadataInfo.type().name())
      .metadataKey(metadataInfo.key())
      .metadataIdentifier(metadataInfo.identifier())
      .columnsStatistics(data.unitColumnsStatistics)
      .metadataStatistics(data.unitMetadataStatistics)
      .lastModifiedTime(BaseMetadata.UNDEFINED_TIME)
      .path(path.toUri().getPath())
      .location(unitPath)
      .locations(unitLocations)
      .build();

    TableMetadataUnit requiredFieldsUnit = requiredFieldsMetadata.toMetadataUnit();

    assertEquals(requiredFieldsExpectedUnit, requiredFieldsUnit);
    assertNotNull(SegmentMetadata.builder().metadataUnit(requiredFieldsUnit).build());

    SchemaPath column = SchemaPath.getSimplePath("dir1");

    List<String> partitionValues = Collections.singletonList("part_varchar=g");

    SegmentMetadata allFieldsMetadata = SegmentMetadata.builder()
      .tableInfo(tableInfo)
      .metadataInfo(metadataInfo)
      .schema(data.schema)
      .columnsStatistics(data.columnsStatistics)
      .metadataStatistics(data.metadataStatistics)
      .lastModifiedTime(data.lastModifiedTime)
      .path(path)
      .locations(locations)
      .column(column)
      .partitionValues(partitionValues)
      .build();

    TableMetadataUnit allFieldsExpectedUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataType(metadataInfo.type().name())
      .metadataKey(metadataInfo.key())
      .metadataIdentifier(metadataInfo.identifier())
      .schema(data.unitSchema)
      .columnsStatistics(data.unitColumnsStatistics)
      .metadataStatistics(data.unitMetadataStatistics)
      .lastModifiedTime(data.lastModifiedTime)
      .path(path.toUri().getPath())
      .location(unitPath)
      .locations(unitLocations)
      .column(column.toString())
      .partitionValues(partitionValues)
      .build();

    TableMetadataUnit allFieldsUnit = allFieldsMetadata.toMetadataUnit();

    assertEquals(allFieldsExpectedUnit, allFieldsUnit);
    assertNotNull(SegmentMetadata.builder().metadataUnit(allFieldsUnit).build());
  }

  @Test
  public void testFileMetadata() {
    TableInfo tableInfo = data.basicTableInfo;

    MetadataInfo metadataInfo = MetadataInfo.builder()
      .type(MetadataType.FILE)
      .key("part_int=3")
      .identifier("part_int=3/part_varchar=g/0_0_0.parquet")
      .build();

    Path path = new Path("/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet");

    FileMetadata metadata = FileMetadata.builder()
      .tableInfo(tableInfo)
      .metadataInfo(metadataInfo)
      .schema(data.schema)
      .columnsStatistics(data.columnsStatistics)
      .metadataStatistics(data.metadataStatistics)
      .lastModifiedTime(data.lastModifiedTime)
      .path(path)
      .build();

    TableMetadataUnit expectedUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataType(metadataInfo.type().name())
      .metadataKey(metadataInfo.key())
      .metadataIdentifier(metadataInfo.identifier())
      .schema(data.unitSchema)
      .columnsStatistics(data.unitColumnsStatistics)
      .metadataStatistics(data.unitMetadataStatistics)
      .lastModifiedTime(data.lastModifiedTime)
      .path(path.toUri().getPath())
      .location(path.getParent().toUri().getPath())
      .build();

    TableMetadataUnit actualUnit = metadata.toMetadataUnit();

    assertEquals(expectedUnit, actualUnit);
    assertNotNull(FileMetadata.builder().metadataUnit(actualUnit).build());
  }

  @Test
  public void testRowGroupMetadata() {
    TableInfo tableInfo = data.basicTableInfo;

    MetadataInfo metadataInfo = MetadataInfo.builder()
      .type(MetadataType.ROW_GROUP)
      .key("part_int=3")
      .identifier("part_int=3/part_varchar=g/0_0_0.parquet")
      .build();

    Path path = new Path("/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet");
    int rowGroupIndex = 1;

    Map<String, Float> hostAffinity = new HashMap<>();
    hostAffinity.put("host1", 1F);
    hostAffinity.put("host2", 2F);

    RowGroupMetadata metadata = RowGroupMetadata.builder()
      .tableInfo(tableInfo)
      .metadataInfo(metadataInfo)
      .schema(data.schema)
      .columnsStatistics(data.columnsStatistics)
      .metadataStatistics(data.metadataStatistics)
      .lastModifiedTime(data.lastModifiedTime)
      .path(path)
      .rowGroupIndex(rowGroupIndex)
      .hostAffinity(hostAffinity)
      .build();

    TableMetadataUnit expectedUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataType(metadataInfo.type().name())
      .metadataKey(metadataInfo.key())
      .metadataIdentifier(metadataInfo.identifier())
      .schema(data.unitSchema)
      .columnsStatistics(data.unitColumnsStatistics)
      .metadataStatistics(data.unitMetadataStatistics)
      .lastModifiedTime(data.lastModifiedTime)
      .path(path.toUri().getPath())
      .location(path.getParent().toUri().getPath())
      .rowGroupIndex(rowGroupIndex)
      .hostAffinity(hostAffinity)
      .build();

    TableMetadataUnit actualUnit = metadata.toMetadataUnit();

    assertEquals(expectedUnit, actualUnit);
    assertNotNull(RowGroupMetadata.builder().metadataUnit(actualUnit).build());
  }

  @Test
  public void testPartitionMetadata() {
    TableInfo tableInfo = data.basicTableInfo;
    SchemaPath column = SchemaPath.getSimplePath("part_varchar");
    List<String> partitionValues = Collections.singletonList("g");
    Set<Path> locations = new HashSet<>();
    locations.add(new Path("part_int=3/part_varchar=g/0_0_0.parquet"));
    locations.add(new Path("part_int=3/part_varchar=g/0_0_1.parquet"));

    MetadataInfo metadataInfo = MetadataInfo.builder()
      .type(MetadataType.PARTITION)
      .key("part_int=3")
      .identifier("part_int=3/part_varchar=g")
      .build();

    PartitionMetadata metadata = PartitionMetadata.builder()
      .tableInfo(tableInfo)
      .metadataInfo(metadataInfo)
      .schema(data.schema)
      .columnsStatistics(data.columnsStatistics)
      .metadataStatistics(data.metadataStatistics)
      .lastModifiedTime(data.lastModifiedTime)
      .column(column)
      .partitionValues(partitionValues)
      .locations(locations)
      .build();

    TableMetadataUnit expectedUnit = TableMetadataUnit.builder()
      .storagePlugin(tableInfo.storagePlugin())
      .workspace(tableInfo.workspace())
      .tableName(tableInfo.name())
      .metadataType(metadataInfo.type().name())
      .metadataKey(metadataInfo.key())
      .metadataIdentifier(metadataInfo.identifier())
      .schema(data.unitSchema)
      .columnsStatistics(data.unitColumnsStatistics)
      .metadataStatistics(data.unitMetadataStatistics)
      .lastModifiedTime(data.lastModifiedTime)
      .column(column.toString())
      .partitionValues(partitionValues)
      .locations(locations.stream()
        .map(location -> location.toUri().getPath())
        .collect(Collectors.toList()))
      .build();

    TableMetadataUnit actualUnit = metadata.toMetadataUnit();

    assertEquals(expectedUnit, actualUnit);
    assertNotNull(PartitionMetadata.builder().metadataUnit(actualUnit).build());
  }

  private static class Data {

    private final TableInfo fullTableInfo;
    private final TableInfo basicTableInfo;
    private final Map<SchemaPath, ColumnStatistics<?>> columnsStatistics;
    private final Map<String, String> unitColumnsStatistics;
    private final Collection<StatisticsHolder<?>> metadataStatistics;
    private final List<String> unitMetadataStatistics;
    private final TupleMetadata schema;
    private final String unitSchema;
    private final long lastModifiedTime;

    Data() {
      fullTableInfo = TableInfo.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .name("nation")
        .owner("user")
        .type("parquet")
        .build();

      basicTableInfo = TableInfo.builder()
        .storagePlugin("dfs")
        .workspace("tmp")
        .name("nation")
        .build();

      columnsStatistics = new HashMap<>();
      columnsStatistics.put(SchemaPath.getSimplePath("name"), new ColumnStatistics<>(Arrays.asList(
        new StatisticsHolder<>("aaa", ColumnStatisticsKind.MIN_VALUE),
        new StatisticsHolder<>("zzz", ColumnStatisticsKind.MAX_VALUE)), TypeProtos.MinorType.VARCHAR));

      unitColumnsStatistics = columnsStatistics.entrySet().stream()
        .collect(Collectors.toMap(
          entry -> entry.getKey().toString(),
          entry -> entry.getValue().jsonString(),
          (o, n) -> n));

      metadataStatistics = Collections.singletonList(new StatisticsHolder<>(2.1, ColumnStatisticsKind.NDV));

      unitMetadataStatistics = metadataStatistics.stream()
        .map(StatisticsHolder::jsonString)
        .collect(Collectors.toList());

      schema = new SchemaBuilder()
        .add("id", TypeProtos.MinorType.INT)
        .add("name", TypeProtos.MinorType.VARCHAR)
        .buildSchema();
      schema.setBooleanProperty("drill.strict", true);

      unitSchema = schema.jsonString();

      lastModifiedTime = System.currentTimeMillis();
    }
  }
}
