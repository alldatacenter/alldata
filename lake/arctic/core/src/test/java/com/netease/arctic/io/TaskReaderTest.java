/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.io;

import com.google.common.collect.Sets;
import com.netease.arctic.io.reader.BaseIcebergPosDeleteReader;
import com.netease.arctic.io.reader.GenericArcticDataReader;
import com.netease.arctic.io.reader.GenericIcebergDataReader;
import com.netease.arctic.scan.ArcticFileScanTask;
import com.netease.arctic.scan.BasicArcticFileScanTask;
import com.netease.arctic.scan.CombinedScanTask;
import com.netease.arctic.scan.KeyedTableScanTask;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.IdentityPartitionConverters;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.io.DeleteSchemaUtil;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.types.Types;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskReaderTest extends TableDataTestBase {

  public TaskReaderTest() {
    super();
  }

  @Test
  public void testRead() {
    CloseableIterable<CombinedScanTask> combinedScanTasks = getArcticTable().asKeyedTable().newScan().planTasks();
    Schema schema = getArcticTable().asKeyedTable().schema();
    GenericArcticDataReader genericArcticDataReader = new GenericArcticDataReader(
        getArcticTable().asKeyedTable().io(),
        schema,
        schema,
        getArcticTable().asKeyedTable().primaryKeySpec(),
        null,
        true,
        IdentityPartitionConverters::convertConstant
    );
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (CombinedScanTask combinedScanTask : combinedScanTasks) {
      for (KeyedTableScanTask keyedTableScanTask : combinedScanTask.tasks()) {
        builder.addAll(genericArcticDataReader.readData(keyedTableScanTask));
      }
    }
    List<Record> records = builder.build();
    Set<Object> resultIds = records.stream().map(s -> s.get(0)).collect(Collectors.toSet());

    Set<Integer> rightIds = Sets.newHashSet(1, 2, 3, 6);
    Assert.assertEquals(rightIds, resultIds);
  }

  @Test
  public void testReadWithSpillMap() throws Exception {
    CloseableIterable<CombinedScanTask> combinedScanTasks = getArcticTable().asKeyedTable().newScan().planTasks();
    Schema schema = getArcticTable().asKeyedTable().schema();
    GenericArcticDataReader genericArcticDataReader = new GenericArcticDataReader(
        getArcticTable().asKeyedTable().io(),
        schema,
        schema,
        getArcticTable().asKeyedTable().primaryKeySpec(),
        null,
        true,
        IdentityPartitionConverters::convertConstant,
        null, false, new StructLikeCollections(true, 0L)
    );
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (CombinedScanTask combinedScanTask : combinedScanTasks) {
      for (KeyedTableScanTask keyedTableScanTask : combinedScanTask.tasks()) {
        try (CloseableIterator<Record> record = genericArcticDataReader.readData(keyedTableScanTask)) {
          if (record.hasNext()) {
            Record record1 = record.next();
            builder.add(record1);
          }
        }
      }
    }
    List<Record> records = builder.build();
    Set<Object> resultIds = records.stream().map(s -> s.get(0)).collect(Collectors.toSet());

    Set<Integer> rightIds = Sets.newHashSet(1, 2, 3, 6);
    Assert.assertEquals(rightIds, resultIds);
  }

  @Test
  public void testReadChange() {
    Table changeTable = getArcticTable().asKeyedTable().changeTable();
    CloseableIterable<FileScanTask> fileScanTasks = changeTable.newScan().planFiles();
    CloseableIterable<ArcticFileScanTask> arcticFileScanTasks = CloseableIterable.transform(
        fileScanTasks, BasicArcticFileScanTask::new
    );
    Schema schema = changeTable.schema();
    List<Types.NestedField> columns = new ArrayList<>(schema.columns());
    columns.add(com.netease.arctic.table.MetadataColumns.TRANSACTION_ID_FILED);
    columns.add(com.netease.arctic.table.MetadataColumns.FILE_OFFSET_FILED);
    columns.add(com.netease.arctic.table.MetadataColumns.CHANGE_ACTION_FIELD);
    Schema externalSchema = new Schema(columns);

    GenericIcebergDataReader genericIcebergDataReader = new GenericIcebergDataReader(
        getArcticTable().asKeyedTable().io(),
        externalSchema,
        externalSchema,
        null,
        false,
        IdentityPartitionConverters::convertConstant,
        false
    );

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (ArcticFileScanTask arcticFileScanTask : arcticFileScanTasks) {
      builder.addAll(genericIcebergDataReader.readData(arcticFileScanTask));
    }
    List<Record> records = builder.build();
    for (Record record : records) {
      Assert.assertEquals(7, record.size());
    }
  }

  @Test
  public void testReadChangeWithSpillMap() throws Exception {
    Table changeTable = getArcticTable().asKeyedTable().changeTable();
    CloseableIterable<FileScanTask> fileScanTasks = changeTable.newScan().planFiles();
    CloseableIterable<ArcticFileScanTask> arcticFileScanTasks = CloseableIterable.transform(
        fileScanTasks, BasicArcticFileScanTask::new
    );
    Schema schema = changeTable.schema();
    List<Types.NestedField> columns = new ArrayList<>(schema.columns());
    columns.add(com.netease.arctic.table.MetadataColumns.TRANSACTION_ID_FILED);
    columns.add(com.netease.arctic.table.MetadataColumns.FILE_OFFSET_FILED);
    columns.add(com.netease.arctic.table.MetadataColumns.CHANGE_ACTION_FIELD);
    Schema externalSchema = new Schema(columns);

    GenericIcebergDataReader genericIcebergDataReader = new GenericIcebergDataReader(
        getArcticTable().asKeyedTable().io(),
        externalSchema,
        externalSchema,
        null,
        false,
        IdentityPartitionConverters::convertConstant,
        false,
        new StructLikeCollections(true, 0L)
    );

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (ArcticFileScanTask arcticFileScanTask : arcticFileScanTasks) {
      try (CloseableIterator<Record> record = genericIcebergDataReader.readData(arcticFileScanTask).iterator()) {
        if (record.hasNext()) {
          Record record1 = record.next();
          builder.add(record1);
        }
      }
    }
    List<Record> records = builder.build();
    for (Record record : records) {
      Assert.assertEquals(7, record.size());
    }
  }

  @Test
  public void testReadPosDelete() {
    BaseIcebergPosDeleteReader baseIcebergPosDeleteReader =
        new BaseIcebergPosDeleteReader(
            getArcticTable().asKeyedTable().io(),
            Collections.singletonList(deleteFileOfPositionDelete));
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    baseIcebergPosDeleteReader.readDeletes().forEach(record -> builder.add(record.copy()));

    List<Record> resultRecords = builder.build();

    GenericRecord r = GenericRecord.create(DeleteSchemaUtil.pathPosSchema());
    r.set(0, dataFileForPositionDelete.path().toString());
    r.set(1, 0L);
    List<Record> sourceRecords = Collections.singletonList(r);
    Assert.assertEquals(resultRecords.size(), sourceRecords.size());
    Set<String> resultPaths =
        resultRecords.stream().map(baseIcebergPosDeleteReader::readPath).collect(Collectors.toSet());
    Set<String> resourcePaths = sourceRecords.stream()
        .map(record -> (String) record.getField(MetadataColumns.DELETE_FILE_PATH.name())).collect(Collectors.toSet());
    Assert.assertEquals(resultPaths, resourcePaths);
    Set<Long> resultPos = resultRecords.stream().map(baseIcebergPosDeleteReader::readPos).collect(Collectors.toSet());
    Set<Long> resourcePos = sourceRecords.stream()
        .map(record -> (long) record.getField(MetadataColumns.DELETE_FILE_POS.name())).collect(Collectors.toSet());
    Assert.assertEquals(resultPos, resourcePos);
  }

  @Test
  public void testReadNegate() {
    CloseableIterable<CombinedScanTask> combinedScanTasks = getArcticTable().asKeyedTable().newScan().planTasks();
    Schema schema = getArcticTable().asKeyedTable().schema();
    GenericArcticDataReader genericArcticDataReader = new GenericArcticDataReader(
        getArcticTable().asKeyedTable().io(),
        schema,
        schema,
        getArcticTable().asKeyedTable().primaryKeySpec(),
        null,
        true,
        IdentityPartitionConverters::convertConstant
    );
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (CombinedScanTask combinedScanTask : combinedScanTasks) {
      for (KeyedTableScanTask keyedTableScanTask : combinedScanTask.tasks()) {
        builder.addAll(genericArcticDataReader.readDeletedData(keyedTableScanTask));
      }
    }
    List<Record> records = builder.build();
    Set<Integer> resultIds = records.stream().map(s -> (Integer) s.get(0)).collect(Collectors.toSet());

    Set<Integer> rightIds = Sets.newHashSet(5);
    Assert.assertEquals(rightIds, resultIds);
  }

  @Test
  public void testReadNegateWithSpillMap() throws Exception {
    CloseableIterable<CombinedScanTask> combinedScanTasks = getArcticTable().asKeyedTable().newScan().planTasks();
    Schema schema = getArcticTable().asKeyedTable().schema();
    GenericArcticDataReader genericArcticDataReader = new GenericArcticDataReader(
        getArcticTable().asKeyedTable().io(),
        schema,
        schema,
        getArcticTable().asKeyedTable().primaryKeySpec(),
        null,
        true,
        IdentityPartitionConverters::convertConstant,
        null, false, new StructLikeCollections(true, 0L)
    );
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (CombinedScanTask combinedScanTask : combinedScanTasks) {
      for (KeyedTableScanTask keyedTableScanTask : combinedScanTask.tasks()) {
        try (CloseableIterator<Record> record = genericArcticDataReader.readDeletedData(keyedTableScanTask)) {
          if (record.hasNext()) {
            Record record1 = record.next();
            builder.add(record1);
          }
        }
      }
    }
    List<Record> records = builder.build();
    Set<Integer> resultIds = records.stream().map(s -> (Integer) s.get(0)).collect(Collectors.toSet());

    Set<Integer> rightIds = Sets.newHashSet(5);
    Assert.assertEquals(rightIds, resultIds);
  }
}
