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
import org.apache.drill.metastore.TestData;
import org.apache.drill.metastore.config.MetastoreConfigConstants;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.metastore.metadata.TableInfo;
import org.apache.drill.test.BaseTest;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@Category(MetastoreTest.class)
public abstract class AbstractBasicTablesRequestsTest extends BaseTest {

  @ClassRule
  public static TemporaryFolder defaultFolder = new TemporaryFolder();

  protected static Tables tables;
  protected static BasicTablesRequests basicRequests;
  protected static TableMetadataUnit nationTable;
  protected static TableInfo nationTableInfo;

  protected static void innerInit(Config config, Class<?> implementationClass) {
    DrillConfig drillConfig = new DrillConfig(config
      .withValue(MetastoreConfigConstants.USE_PROVIDED_CONFIG,
        ConfigValueFactory.fromAnyRef(true))
      .withValue(MetastoreConfigConstants.IMPLEMENTATION_CLASS,
        ConfigValueFactory.fromAnyRef(implementationClass.getName())));
    tables = new MetastoreRegistry(drillConfig).get().tables();
    basicRequests = tables.basicRequests();
    prepareData(tables);
  }

  @Test
  public void testMetastoreTableInfoAbsentTable() {
    TableInfo tableInfo = TableInfo.builder().storagePlugin("dfs").workspace("tmp").name("absent").build();
    MetastoreTableInfo metastoreTableInfo = basicRequests.metastoreTableInfo(tableInfo);
    assertFalse(metastoreTableInfo.isExists());
    assertEquals(tableInfo, metastoreTableInfo.tableInfo());
    assertNull(metastoreTableInfo.lastModifiedTime());
  }

  @Test
  public void testHasMetastoreTableInfoChangedFalse() {
    MetastoreTableInfo metastoreTableInfo = basicRequests.metastoreTableInfo(nationTableInfo);
    assertFalse(basicRequests.hasMetastoreTableInfoChanged(metastoreTableInfo));
  }

  @Test
  public void testHasMetastoreTableInfoChangedTrue() {
    TableMetadataUnit unit = nationTable.toBuilder()
      .tableName("changingTable")
      .lastModifiedTime(1L)
      .build();
    tables.modify()
      .overwrite(unit)
      .execute();
    TableInfo tableInfo = TableInfo.builder().metadataUnit(unit).build();
    MetastoreTableInfo metastoreTableInfo = basicRequests.metastoreTableInfo(tableInfo);

    TableMetadataUnit updatedUnit = unit.toBuilder()
      .lastModifiedTime(2L)
      .build();
    tables.modify()
      .overwrite(updatedUnit)
      .execute();

    assertTrue(basicRequests.hasMetastoreTableInfoChanged(metastoreTableInfo));
  }

  @Test
  public void testTablesMetadataAbsent() {
    List<BaseTableMetadata> tablesMetadata = basicRequests.tablesMetadata(
      FilterExpression.and(
        FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs"),
        FilterExpression.equal(MetastoreColumn.WORKSPACE, "absent")));
    assertTrue(tablesMetadata.isEmpty());
  }

  @Test
  public void testTablesMetadataExisting() {
    List<BaseTableMetadata> baseTableMetadata = basicRequests.tablesMetadata(
      FilterExpression.and(
        FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, "dfs"),
        FilterExpression.equal(MetastoreColumn.WORKSPACE, "tmp")));
    assertTrue(baseTableMetadata.size() > 1);
  }

  @Test
  public void testTableMetadataAbsent() {
    TableInfo tableInfo = TableInfo.builder().storagePlugin("dfs").workspace("tmp").name("absent").build();
    BaseTableMetadata tableMetadata = basicRequests.tableMetadata(tableInfo);
    assertNull(tableMetadata);
  }

  @Test
  public void testTableMetadataExisting() {
    BaseTableMetadata tableMetadata = basicRequests.tableMetadata(nationTableInfo);
    assertNotNull(tableMetadata);
  }

  @Test
  public void testSegmentsMetadataByMetadataKeyAbsent() {
    List<SegmentMetadata> segmentMetadata = basicRequests.segmentsMetadataByMetadataKey(
      nationTableInfo,
      Collections.singletonList("/tmp/nation/part_int=3/d6"),
      "part_int=3");
    assertTrue(segmentMetadata.isEmpty());
  }

  @Test
  public void testSegmentsMetadataByMetadataKeyExisting() {
    List<SegmentMetadata> segmentMetadata = basicRequests.segmentsMetadataByMetadataKey(
      nationTableInfo,
      Arrays.asList("/tmp/nation/part_int=3/d3", "/tmp/nation/part_int=3/d4"),
      "part_int=3");
    assertEquals(2, segmentMetadata.size());
  }

  @Test
  public void testSegmentsMetadataByColumnAbsent() {
    List<SegmentMetadata> segmentMetadata = basicRequests.segmentsMetadataByColumn(
      nationTableInfo,
      Arrays.asList("/tmp/nation/part_int=3/d4", "/tmp/nation/part_int=3/d5"),
      "n_region");
    assertTrue(segmentMetadata.isEmpty());
  }

  @Test
  public void testSegmentsMetadataByColumnExisting() {
    List<SegmentMetadata> segmentMetadata = basicRequests.segmentsMetadataByColumn(
      nationTableInfo,
      Arrays.asList("/tmp/nation/part_int=3/d3", "/tmp/nation/part_int=3/d4"),
      "n_nation");
    assertEquals(2, segmentMetadata.size());
  }

  @Test
  public void testSegmentMetadataByMetadataInfosAbsent() {
    List<SegmentMetadata> segmentMetadata = basicRequests.segmentsMetadata(
      nationTableInfo,
      Collections.singletonList(MetadataInfo.builder()
        .type(MetadataType.SEGMENT)
        .key("part_int=4")
        .identifier("part_int=4")
        .build()));
    assertTrue(segmentMetadata.isEmpty());
  }

  @Test
  public void testSegmentMetadataByMetadataInfosExisting() {
    List<SegmentMetadata> segmentMetadata = basicRequests.segmentsMetadata(
      nationTableInfo,
      Arrays.asList(
        MetadataInfo.builder()
          .type(MetadataType.SEGMENT)
          .key("part_int=3")
          .identifier("part_int=3/d3")
          .build(),
        MetadataInfo.builder()
          .type(MetadataType.SEGMENT)
          .key("part_int=3")
          .identifier("part_int=3/d4")
          .build())
    );
    assertEquals(2, segmentMetadata.size());
  }

  @Test
  public void testMetadataUnitsByMetadataInfosAbsent() {
    List<TableMetadataUnit> segmentMetadata = basicRequests.metadata(
      nationTableInfo,
      Collections.singletonList(MetadataInfo.builder()
        .type(MetadataType.ROW_GROUP)
        .key("part_int=4")
        .identifier("part_int=4")
        .build()));
    assertTrue(segmentMetadata.isEmpty());
  }

  @Test
  public void testMetadataUnitsByMetadataInfosExisting() {
    List<TableMetadataUnit> segmentMetadata = basicRequests.metadata(
      nationTableInfo,
      Arrays.asList(
        MetadataInfo.builder()
          .type(MetadataType.SEGMENT)
          .key("part_int=3")
          .identifier("part_int=3/d3")
          .build(),
        MetadataInfo.builder()
          .type(MetadataType.SEGMENT)
          .key("part_int=3")
          .identifier("part_int=3/d4")
          .build(),
        MetadataInfo.builder()
          .type(MetadataType.PARTITION)
          .key("part_int=3")
          .identifier("part_int=4/d5")
          .build())
    );
    assertEquals(2, segmentMetadata.size());
  }

  @Test
  public void testFilesMetadataByMetadataInfosAbsent() {
    List<FileMetadata> segmentMetadata = basicRequests.filesMetadata(
      nationTableInfo,
      Collections.singletonList(MetadataInfo.builder()
        .type(MetadataType.FILE)
        .key("part_int=4")
        .identifier("part_int=4/part_varchar=g/0_0_3.parquet")
        .build()));
    assertTrue(segmentMetadata.isEmpty());
  }

  @Test
  public void testFilesMetadataByMetadataInfosExisting() {
    List<FileMetadata> segmentMetadata = basicRequests.filesMetadata(
      nationTableInfo,
      Arrays.asList(
        MetadataInfo.builder()
          .type(MetadataType.FILE)
          .key("part_int=4")
          .identifier("part_int=4/part_varchar=g/0_0_0.parquet")
          .build(),
        MetadataInfo.builder()
          .type(MetadataType.FILE)
          .key("part_int=3")
          .identifier("part_int=3/part_varchar=g/0_0_1.parquet")
          .build())
    );
    assertEquals(2, segmentMetadata.size());
  }

  @Test
  public void testRowGroupsMetadataByMetadataKeysAndPathsAbsent() {
    List<RowGroupMetadata> segmentMetadata = basicRequests.rowGroupsMetadata(
      nationTableInfo,
      Collections.singletonList("part_int=4"),
      Collections.singletonList("/tmp/nation/part_int=4/part_varchar=g/0_0_3.parquet"));
    assertTrue(segmentMetadata.isEmpty());
  }

  @Test
  public void testRowGroupsByMetadataKeysAndPathsExisting() {
    List<RowGroupMetadata> segmentMetadata = basicRequests.rowGroupsMetadata(
      nationTableInfo,
      Arrays.asList(
        "part_int=4",
        "part_int=3"),
      Arrays.asList(
        "/tmp/nation/part_int=4/part_varchar=g/0_0_0.parquet",
        "/tmp/nation/part_int=3/part_varchar=g/0_0_1.parquet")
    );
    assertEquals(2, segmentMetadata.size());
  }

  @Test
  public void testRowGroupsMetadataByMetadataInfosAbsent() {
    List<RowGroupMetadata> segmentMetadata = basicRequests.rowGroupsMetadata(
      nationTableInfo,
      Collections.singletonList(MetadataInfo.builder()
        .type(MetadataType.ROW_GROUP)
        .key("part_int=4")
        .identifier("part_int=4/part_varchar=g/0_0_3.parquet/1")
        .build()));
    assertTrue(segmentMetadata.isEmpty());
  }

  @Test
  public void testRowGroupsMetadataByMetadataInfosExisting() {
    List<RowGroupMetadata> rowGroupMetadata = basicRequests.rowGroupsMetadata(
      nationTableInfo,
      Arrays.asList(
        MetadataInfo.builder()
          .type(MetadataType.ROW_GROUP)
          .key("part_int=4")
          .identifier("part_int=4/part_varchar=g/0_0_0.parquet/1")
          .build(),
        MetadataInfo.builder()
          .type(MetadataType.ROW_GROUP)
          .key("part_int=3")
          .identifier("part_int=3/part_varchar=g/0_0_0.parquet/1")
          .build())
    );
    assertEquals(2, rowGroupMetadata.size());
  }

  @Test
  public void testPartitionsMetadataAbsent() {
    List<PartitionMetadata> partitionMetadata = basicRequests.partitionsMetadata(
      nationTableInfo,
      Arrays.asList("part_int=3", "part_int=4"),
      "id");
    assertTrue(partitionMetadata.isEmpty());
  }

  @Test
  public void testPartitionsMetadataExisting() {
    List<PartitionMetadata> partitionMetadata = basicRequests.partitionsMetadata(
      nationTableInfo,
      Arrays.asList("part_int=3", "part_int=4"),
      "n_nation");
    assertEquals(2, partitionMetadata.size());
  }

  @Test
  public void testFilesMetadataAbsent() {
    List<FileMetadata> fileMetadata = basicRequests.filesMetadata(
      nationTableInfo,
      "part_int=3",
      Collections.singletonList("/tmp/nation/part_int=3/part_varchar=g/0_0_2.parquet"));
    assertTrue(fileMetadata.isEmpty());
  }

  @Test
  public void testFilesMetadataExisting() {
    List<FileMetadata> fileMetadata = basicRequests.filesMetadata(
      nationTableInfo,
      "part_int=3",
      Arrays.asList("/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet",
        "/tmp/nation/part_int=3/part_varchar=g/0_0_1.parquet"));
    assertEquals(2, fileMetadata.size());
  }

  @Test
  public void testFileMetadataAbsent() {
    FileMetadata fileMetadata = basicRequests.fileMetadata(
      nationTableInfo,
      "part_int=3",
      "/tmp/nation/part_int=3/part_varchar=g/0_0_2.parquet");
    assertNull(fileMetadata);
  }

  @Test
  public void testFileMetadataExisting() {
    FileMetadata fileMetadata = basicRequests.fileMetadata(
      nationTableInfo,
      "part_int=3",
      "/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet");
    assertNotNull(fileMetadata);
  }

  @Test
  public void testRowGroupsMetadataAbsent() {
    List<RowGroupMetadata> rowGroupMetadata = basicRequests.rowGroupsMetadata(
      nationTableInfo,
      "part_int=3",
      "/tmp/nation/part_int=3/part_varchar=g/0_0_2.parquet");
    assertTrue(rowGroupMetadata.isEmpty());
  }

  @Test
  public void testRowGroupsMetadataExisting() {
    List<RowGroupMetadata> rowGroupMetadata = basicRequests.rowGroupsMetadata(
      nationTableInfo,
      "part_int=3",
      "/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet");
    assertEquals(2, rowGroupMetadata.size());
  }

  @Test
  public void testFullSegmentsMetadataWithoutPartitions() {
    BasicTablesTransformer.MetadataHolder metadataHolder = basicRequests.fullSegmentsMetadataWithoutPartitions(
      nationTableInfo,
      Arrays.asList("part_int=4", "part_int=5"),
      Arrays.asList("/tmp/nation/part_int=4/d5", "/tmp/nation/part_int=4/part_varchar=g"));
    assertTrue(metadataHolder.tables().isEmpty());
    assertTrue(metadataHolder.partitions().isEmpty());
    assertEquals(1, metadataHolder.segments().size());
    assertEquals(1, metadataHolder.files().size());
    assertEquals(2, metadataHolder.rowGroups().size());
  }

  @Test
  public void testFilesLastModifiedTime() {
    Map<String, Long> result = basicRequests.filesLastModifiedTime(
      nationTableInfo,
      "part_int=3",
      Collections.singletonList("/tmp/nation/part_int=3/part_varchar=g"));

    Map<String, Long> expected = new HashMap<>();
    expected.put("/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet", 1L);
    expected.put("/tmp/nation/part_int=3/part_varchar=g/0_0_1.parquet", 2L);

    assertEquals(expected, result);
  }

  @Test
  public void testSegmentsLastModifiedTime() {
    Map<String, Long> result = basicRequests.segmentsLastModifiedTime(
      nationTableInfo,
      Arrays.asList("/tmp/nation/part_int=3/d3", "/tmp/nation/part_int=4/d5"));

    Map<String, Long> expected = new HashMap<>();
    expected.put("part_int=3", 1L);
    expected.put("part_int=4", 3L);

    assertEquals(expected, result);
  }

  @Test
  public void testInterestingColumnsAndPartitionKeys() {
    TableMetadataUnit result = basicRequests.interestingColumnsAndPartitionKeys(nationTableInfo);
    assertEquals(nationTable.interestingColumns(), result.interestingColumns());
    assertEquals(nationTable.partitionKeys(), result.partitionKeys());
    assertNull(result.tableName());
    assertNull(result.lastModifiedTime());
  }

  @Test
  public void testCustomRequest() {
    BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
      .column("n_nation")
      .metadataType(MetadataType.PARTITION)
      .build();

    List<TableMetadataUnit> units = basicRequests.request(requestMetadata);
    assertEquals(2, units.size());
  }

  /**
   * Prepares data which will be used in the unit tests.
   * Note: data is filled to check basic request results and might not be exactly true to reality.
   *
   * @param tables Drill Metastore Tables instance
   */
  protected static void prepareData(Tables tables) {
    TableMetadataUnit basicUnit = TestData.basicTableMetadataUnit();

    nationTable = BaseTableMetadata.builder()
      .metadataUnit(basicUnit.toBuilder()
        .tableName("nation")
        .metadataType(MetadataType.TABLE.name())
        .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
        .build())
      .build()
      .toMetadataUnit();

    nationTableInfo = TableInfo.builder().metadataUnit(nationTable).build();

    TableMetadataUnit basicSegment = SegmentMetadata.builder()
      .metadataUnit(basicUnit.toBuilder()
        .tableName(nationTableInfo.name())
        .metadataType(MetadataType.SEGMENT.name())
        .build())
      .build()
      .toMetadataUnit();

    TableMetadataUnit nationSegment1 = basicSegment.toBuilder()
      .metadataKey("part_int=3")
      .metadataIdentifier("part_int=3/d3")
      .location("/tmp/nation/part_int=3/d3")
      .column("n_nation")
      .lastModifiedTime(1L)
      .build();

    TableMetadataUnit nationSegment2 = basicSegment.toBuilder()
      .metadataKey("part_int=3")
      .metadataIdentifier("part_int=3/d4")
      .location("/tmp/nation/part_int=3/d4")
      .column("n_nation")
      .lastModifiedTime(2L)
      .build();

    TableMetadataUnit nationSegment3 = basicSegment.toBuilder()
      .metadataKey("part_int=4")
      .metadataIdentifier("part_int=3/d5")
      .location("/tmp/nation/part_int=4/d5")
      .column("n_nation")
      .lastModifiedTime(3L)
      .build();

    TableMetadataUnit basicPartition = PartitionMetadata.builder()
      .metadataUnit(basicUnit.toBuilder()
        .tableName(nationTableInfo.name())
        .metadataType(MetadataType.PARTITION.name())
        .build())
      .build()
      .toMetadataUnit();

    TableMetadataUnit nationPartition1 = basicPartition.toBuilder()
      .metadataKey("part_int=3")
      .metadataIdentifier("part_int=3/d5")
      .location("/tmp/nation/part_int=3/d5")
      .column("n_nation")
      .build();

    TableMetadataUnit nationPartition2 = basicPartition.toBuilder()
      .metadataKey("part_int=4")
      .metadataIdentifier("part_int=4/d5")
      .location("/tmp/nation/part_int=4/d5")
      .column("n_nation")
      .build();

    TableMetadataUnit nationPartition3 = basicPartition.toBuilder()
      .metadataKey("part_int=4")
      .metadataIdentifier("part_int=4/d6")
      .column("n_region")
      .location("/tmp/nation/part_int=4/d6")
      .build();

    TableMetadataUnit basicFile = FileMetadata.builder()
      .metadataUnit(basicUnit.toBuilder()
        .tableName(nationTableInfo.name())
        .metadataType(MetadataType.FILE.name())
        .build())
      .build()
      .toMetadataUnit();

    TableMetadataUnit nationFile1 = basicFile.toBuilder()
      .metadataKey("part_int=3")
      .metadataIdentifier("part_int=3/part_varchar=g/0_0_0.parquet")
      .location("/tmp/nation/part_int=3/part_varchar=g")
      .path("/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet")
      .lastModifiedTime(1L)
      .build();

    TableMetadataUnit nationFile2 = basicFile.toBuilder()
      .metadataKey("part_int=3")
      .metadataIdentifier("part_int=3/part_varchar=g/0_0_1.parquet")
      .location("/tmp/nation/part_int=3/part_varchar=g")
      .path("/tmp/nation/part_int=3/part_varchar=g/0_0_1.parquet")
      .lastModifiedTime(System.currentTimeMillis())
      .lastModifiedTime(2L)
      .build();

    TableMetadataUnit nationFile3 = basicFile.toBuilder()
      .metadataKey("part_int=4")
      .metadataIdentifier("part_int=4/part_varchar=g/0_0_0.parquet")
      .location("/tmp/nation/part_int=4/part_varchar=g")
      .path("/tmp/nation/part_int=4/part_varchar=g/0_0_0.parquet")
      .lastModifiedTime(3L)
      .build();

    TableMetadataUnit basicRowGroup = RowGroupMetadata.builder()
      .metadataUnit(basicUnit.toBuilder()
        .tableName(nationTableInfo.name())
        .metadataType(MetadataType.ROW_GROUP.name())
        .build())
      .build()
      .toMetadataUnit();

    TableMetadataUnit nationRowGroup1 = basicRowGroup.toBuilder()
      .metadataKey("part_int=3")
      .metadataIdentifier("part_int=3/part_varchar=g/0_0_0.parquet/1")
      .location("/tmp/nation/part_int=3/part_varchar=g")
      .path("/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet")
      .rowGroupIndex(1)
      .build();

    TableMetadataUnit nationRowGroup2 = basicRowGroup.toBuilder()
      .metadataKey("part_int=3")
      .metadataIdentifier("part_int=3/part_varchar=g/0_0_0.parquet/2")
      .location("/tmp/nation/part_int=3/part_varchar=g")
      .path("/tmp/nation/part_int=3/part_varchar=g/0_0_0.parquet")
      .rowGroupIndex(2)
      .build();

    TableMetadataUnit nationRowGroup3 = basicRowGroup.toBuilder()
      .metadataKey("part_int=4")
      .metadataIdentifier("part_int=4/part_varchar=g/0_0_0.parquet/1")
      .location("/tmp/nation/part_int=4/part_varchar=g")
      .path("/tmp/nation/part_int=4/part_varchar=g/0_0_0.parquet")
      .rowGroupIndex(1)
      .build();

    TableMetadataUnit nationRowGroup4 = basicRowGroup.toBuilder()
      .metadataKey("part_int=4")
      .metadataIdentifier("part_int=4/part_varchar=g/0_0_0.parquet/2")
      .location("/tmp/nation/part_int=4/part_varchar=g")
      .path("/tmp/nation/part_int=4/part_varchar=g/0_0_0.parquet")
      .rowGroupIndex(2)
      .build();

    TableMetadataUnit regionTable = BaseTableMetadata.builder()
      .metadataUnit(basicUnit.toBuilder()
        .tableName("region")
        .metadataType(MetadataType.TABLE.name())
        .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
        .build())
      .build()
      .toMetadataUnit();

    tables.modify()
      .overwrite(nationTable,
        nationSegment1, nationSegment2, nationSegment3,
        nationPartition1, nationPartition2, nationPartition3,
        nationFile1, nationFile2, nationFile3,
        nationRowGroup1, nationRowGroup2, nationRowGroup3, nationRowGroup4,
        regionTable)
      .execute();
  }
}
