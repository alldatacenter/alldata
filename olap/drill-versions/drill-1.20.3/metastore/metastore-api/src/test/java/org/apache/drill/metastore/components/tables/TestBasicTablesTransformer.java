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
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.metadata.FileMetadata;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.PartitionMetadata;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.metastore.metadata.SegmentMetadata;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(MetastoreTest.class)
public class TestBasicTablesTransformer extends BaseTest {

  @Test
  public void testTables() {
    TableMetadataUnit table = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.TABLE.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .partitionKeys(Collections.emptyMap())
      .build();

    TableMetadataUnit segment = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.SEGMENT.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .build();

    List<BaseTableMetadata> tables = BasicTablesTransformer.tables(Arrays.asList(table, segment));
    assertEquals(1, tables.size());
  }

  @Test
  public void testSegments() {
    TableMetadataUnit table = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.TABLE.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .partitionKeys(Collections.emptyMap())
      .build();

    TableMetadataUnit segment = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.SEGMENT.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation")
      .locations(Collections.emptyList())
      .build();

    List<SegmentMetadata> segments = BasicTablesTransformer.segments(Arrays.asList(table, segment));
    assertEquals(1, segments.size());
  }

  @Test
  public void testFiles() {
    TableMetadataUnit table = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.TABLE.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .partitionKeys(Collections.emptyMap())
      .build();

    TableMetadataUnit file0 = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.FILE.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation/0_0_0.parquet")
      .build();

    TableMetadataUnit file1 = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.FILE.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation/0_0_1.parquet")
      .build();

    List<FileMetadata> files = BasicTablesTransformer.files(Arrays.asList(table, file0, file1));
    assertEquals(2, files.size());
  }

  @Test
  public void testRowGroups() {
    TableMetadataUnit file = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.FILE.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation/0_0_0.parquet")
      .build();

    TableMetadataUnit rowGroup = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.ROW_GROUP.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation/0_0_0.parquet")
      .rowGroupIndex(1)
      .hostAffinity(Collections.emptyMap())
      .build();

    List<RowGroupMetadata> rowGroups = BasicTablesTransformer.rowGroups(Arrays.asList(file, rowGroup));
    assertEquals(1, rowGroups.size());
  }

  @Test
  public void testPartitions() {
    TableMetadataUnit partition1 = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.PARTITION.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation/2018")
      .locations(Collections.emptyList())
      .column("dir0")
      .partitionValues(Collections.emptyList())
      .build();

    TableMetadataUnit partition2 = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.PARTITION.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation/2019")
      .locations(Collections.emptyList())
      .column("dir0")
      .partitionValues(Collections.emptyList())
      .build();

    List<PartitionMetadata> partitions = BasicTablesTransformer.partitions(Arrays.asList(partition1, partition2));
    assertEquals(2, partitions.size());
  }

  @Test
  public void testAll() {
    TableMetadataUnit table = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.TABLE.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .partitionKeys(Collections.emptyMap())
      .build();

    TableMetadataUnit segment = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.SEGMENT.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation")
      .locations(Collections.emptyList())
      .build();

    TableMetadataUnit file0 = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.FILE.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation/0_0_0.parquet")
      .build();

    TableMetadataUnit file1 = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.FILE.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation/0_0_1.parquet")
      .build();

    TableMetadataUnit rowGroup = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.ROW_GROUP.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation/0_0_0.parquet")
      .rowGroupIndex(1)
      .hostAffinity(Collections.emptyMap())
      .build();

    TableMetadataUnit partition1 = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.PARTITION.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation/2018")
      .locations(Collections.emptyList())
      .column("dir0")
      .partitionValues(Collections.emptyList())
      .build();

    TableMetadataUnit partition2 = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.PARTITION.name())
      .metadataKey(MetadataInfo.DEFAULT_SEGMENT_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .path("/tmp/nation/2019")
      .locations(Collections.emptyList())
      .column("dir0")
      .partitionValues(Collections.emptyList())
      .build();

    BasicTablesTransformer.MetadataHolder all = BasicTablesTransformer.all(
      Arrays.asList(table, segment, file0, file1, rowGroup, partition1, partition2));
    assertEquals(1, all.tables().size());
    assertEquals(1, all.segments().size());
    assertEquals(2, all.files().size());
    assertEquals(1, all.rowGroups().size());
    assertEquals(2, all.partitions().size());
  }

  @Test
  public void testUnexpectedType() {
    TableMetadataUnit table = TableMetadataUnit.builder()
      .storagePlugin("dfs")
      .workspace("tmp")
      .tableName("nation")
      .metadataType(MetadataType.TABLE.name())
      .metadataKey(MetadataInfo.GENERAL_INFO_KEY)
      .columnsStatistics(Collections.emptyMap())
      .metadataStatistics(Collections.emptyList())
      .partitionKeys(Collections.emptyMap())
      .build();

    TableMetadataUnit nullMetadataType = TableMetadataUnit.builder().metadataType(null).build();
    TableMetadataUnit noneMetadataType = TableMetadataUnit.builder().metadataType(MetadataType.NONE.name()).build();

    List<TableMetadataUnit> units = Arrays.asList(table, nullMetadataType, noneMetadataType);

    List<BaseTableMetadata> tables = BasicTablesTransformer.tables(units);
    assertEquals(1, tables.size());

    BasicTablesTransformer.MetadataHolder all = BasicTablesTransformer.all(units);
    assertEquals(1, all.tables().size());
    assertTrue(all.segments().isEmpty());
    assertTrue(all.files().isEmpty());
    assertTrue(all.rowGroups().isEmpty());
    assertTrue(all.partitions().isEmpty());
  }

  @Test
  public void testNoResult() {
    List<TableMetadataUnit> units = Collections.singletonList(TableMetadataUnit.builder().metadataType(null).build());

    assertTrue(BasicTablesTransformer.tables(units).isEmpty());
    assertTrue(BasicTablesTransformer.segments(units).isEmpty());
    assertTrue(BasicTablesTransformer.files(units).isEmpty());
    assertTrue(BasicTablesTransformer.rowGroups(units).isEmpty());
    assertTrue(BasicTablesTransformer.partitions(units).isEmpty());
  }
}
