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

package com.netease.arctic.ams.server.optimize;


import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.util.DataFileInfoUtils;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.AdaptHiveGenericAppenderFactory;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.encryption.EncryptedOutputFile;
import org.apache.iceberg.io.DataWriter;
import org.apache.iceberg.io.OutputFileFactory;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.util.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TestMajorOptimizePlan extends TestBaseOptimizeBase {
  @Test
  public void testKeyedTableMajorOptimize() throws IOException {
    Pair<Snapshot, List<DataFile>> insertBaseResult = insertTableBaseDataFiles(testKeyedTable);
    List<DataFile> baseDataFiles = insertBaseResult.second();
    baseDataFilesInfo.addAll(baseDataFiles.stream()
        .map(dataFile ->
            DataFileInfoUtils.convertToDatafileInfo(dataFile, insertBaseResult.first(), testKeyedTable, false))
        .collect(Collectors.toList()));

    Set<DataTreeNode> targetNodes = baseDataFilesInfo.stream()
        .map(dataFileInfo -> DataTreeNode.of(dataFileInfo.getMask(), dataFileInfo.getIndex())).collect(Collectors.toSet());
    Pair<Snapshot, List<DeleteFile>> deleteResult =
        insertBasePosDeleteFiles(testKeyedTable, baseDataFiles, targetNodes);
    List<DeleteFile> deleteFiles = deleteResult.second();
    posDeleteFilesInfo.addAll(deleteFiles.stream()
        .map(deleteFile -> DataFileInfoUtils.convertToDatafileInfo(deleteFile, deleteResult.first(),
            testKeyedTable.asKeyedTable()))
        .collect(Collectors.toList()));

    List<FileScanTask> baseFiles = planBaseFiles(testKeyedTable);
    MajorOptimizePlan majorOptimizePlan = new MajorOptimizePlan(testKeyedTable,
        new TableOptimizeRuntime(testKeyedTable.id()), baseFiles,
        1, System.currentTimeMillis(), TableOptimizeRuntime.INVALID_SNAPSHOT_ID);
    List<BasicOptimizeTask> tasks = majorOptimizePlan.plan().getOptimizeTasks();

    Assert.assertEquals(OptimizeType.Major, tasks.get(0).getTaskId().getType());
    Assert.assertEquals(4, tasks.size());
    Assert.assertEquals(10, tasks.get(0).getBaseFiles().size());
    Assert.assertEquals(1, tasks.get(0).getPosDeleteFiles().size());
    Assert.assertEquals(0, tasks.get(0).getInsertFileCnt());
    Assert.assertEquals(0, tasks.get(0).getDeleteFileCnt());
  }

  @Test
  public void testKeyedTableFullOptimize() throws IOException {
    Pair<Snapshot, List<DataFile>> insertBaseResult = insertTableBaseDataFiles(testKeyedTable);
    List<DataFile> baseDataFiles = insertBaseResult.second();
    baseDataFilesInfo.addAll(baseDataFiles.stream()
        .map(dataFile ->
            DataFileInfoUtils.convertToDatafileInfo(dataFile, insertBaseResult.first(), testKeyedTable, false))
        .collect(Collectors.toList()));

    Set<DataTreeNode> targetNodes = baseDataFilesInfo.stream()
        .map(dataFileInfo -> DataTreeNode.of(dataFileInfo.getMask(), dataFileInfo.getIndex())).collect(Collectors.toSet());
    Pair<Snapshot, List<DeleteFile>> deleteResult =
        insertBasePosDeleteFiles(testKeyedTable, baseDataFiles, targetNodes);
    List<DeleteFile> deleteFiles = deleteResult.second();
    posDeleteFilesInfo.addAll(deleteFiles.stream()
        .map(deleteFile -> DataFileInfoUtils.convertToDatafileInfo(deleteFile, deleteResult.first(),
            testKeyedTable.asKeyedTable()))
        .collect(Collectors.toList()));

    testKeyedTable.updateProperties()
        .set(TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO, "0")
        .commit();

    List<FileScanTask> baseFiles = planBaseFiles(testKeyedTable);
    FullOptimizePlan fullOptimizePlan = new FullOptimizePlan(testKeyedTable,
        new TableOptimizeRuntime(testKeyedTable.id()), baseFiles,
        1, System.currentTimeMillis(), TableOptimizeRuntime.INVALID_SNAPSHOT_ID);
    List<BasicOptimizeTask> tasks = fullOptimizePlan.plan().getOptimizeTasks();

    Assert.assertEquals(OptimizeType.FullMajor, tasks.get(0).getTaskId().getType());
    Assert.assertEquals(4, tasks.size());
    Assert.assertEquals(10, tasks.get(0).getBaseFiles().size());
    Assert.assertEquals(1, tasks.get(0).getPosDeleteFiles().size());
    Assert.assertEquals(0, tasks.get(0).getInsertFileCnt());
    Assert.assertEquals(0, tasks.get(0).getDeleteFileCnt());
  }

  @Test
  public void testUnKeyedTableMajorOptimize() {
    insertUnKeyedTableDataFiles(testTable);

    List<FileScanTask> baseFiles = planBaseFiles(testTable);
    MajorOptimizePlan majorOptimizePlan = new MajorOptimizePlan(testTable,
        new TableOptimizeRuntime(testTable.id()), baseFiles,
        1, System.currentTimeMillis(), TableOptimizeRuntime.INVALID_SNAPSHOT_ID);
    List<BasicOptimizeTask> tasks = majorOptimizePlan.plan().getOptimizeTasks();

    Assert.assertEquals(OptimizeType.Major, tasks.get(0).getTaskId().getType());
    Assert.assertEquals(2, tasks.size());
    Assert.assertEquals(5, tasks.get(0).getBaseFileCnt());
    Assert.assertEquals(0, tasks.get(0).getPosDeleteFiles().size());
    Assert.assertEquals(0, tasks.get(0).getInsertFileCnt());
    Assert.assertEquals(0, tasks.get(0).getDeleteFileCnt());
  }

  @Test
  public void testUnKeyedTableFullOptimize() {
    testTable.updateProperties()
        .set(TableProperties.SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL, "86400000")
        .commit();
    insertUnKeyedTableDataFiles(testTable);

    List<FileScanTask> baseFiles = planBaseFiles(testTable);
    FullOptimizePlan fullOptimizePlan = new FullOptimizePlan(testTable,
        new TableOptimizeRuntime(testTable.id()), baseFiles,
        1, System.currentTimeMillis(), TableOptimizeRuntime.INVALID_SNAPSHOT_ID);
    List<BasicOptimizeTask> tasks = fullOptimizePlan.plan().getOptimizeTasks();

    Assert.assertEquals(OptimizeType.FullMajor, tasks.get(0).getTaskId().getType());
    Assert.assertEquals(2, tasks.size());
    Assert.assertEquals(5, tasks.get(0).getBaseFileCnt());
    Assert.assertEquals(0, tasks.get(0).getPosDeleteFiles().size());
    Assert.assertEquals(0, tasks.get(0).getInsertFileCnt());
    Assert.assertEquals(0, tasks.get(0).getDeleteFileCnt());
  }

  @Test
  public void testUnKeyedTableMajorOptimizeWithPosDelete() throws Exception {
    insertUnKeyedTablePosDeleteFiles(testTable);

    List<FileScanTask> baseFiles = planBaseFiles(testTable);
    MajorOptimizePlan majorOptimizePlan = new MajorOptimizePlan(testTable,
        new TableOptimizeRuntime(testTable.id()), baseFiles,
        1, System.currentTimeMillis(), TableOptimizeRuntime.INVALID_SNAPSHOT_ID);
    List<BasicOptimizeTask> tasks = majorOptimizePlan.plan().getOptimizeTasks();

    Assert.assertEquals(OptimizeType.Major, tasks.get(0).getTaskId().getType());
    Assert.assertEquals(2, tasks.size());
    Assert.assertEquals(5, tasks.get(0).getBaseFileCnt());
    Assert.assertEquals(1, tasks.get(0).getPosDeleteFiles().size());
    Assert.assertEquals(0, tasks.get(0).getInsertFileCnt());
    Assert.assertEquals(0, tasks.get(0).getDeleteFileCnt());
  }

  @Test
  public void testUnKeyedTableFullOptimizeWithPosDelete() throws Exception {
    testTable.updateProperties()
        .set(TableProperties.SELF_OPTIMIZING_FULL_TRIGGER_INTERVAL, "86400000")
        .commit();
    insertUnKeyedTablePosDeleteFiles(testTable);

    List<FileScanTask> baseFiles = planBaseFiles(testTable);
    FullOptimizePlan fullOptimizePlan = new FullOptimizePlan(testTable,
        new TableOptimizeRuntime(testTable.id()), baseFiles,
        1, System.currentTimeMillis(), TableOptimizeRuntime.INVALID_SNAPSHOT_ID);
    List<BasicOptimizeTask> tasks = fullOptimizePlan.plan().getOptimizeTasks();

    Assert.assertEquals(OptimizeType.FullMajor, tasks.get(0).getTaskId().getType());
    Assert.assertEquals(2, tasks.size());
    Assert.assertEquals(5, tasks.get(0).getBaseFileCnt());
    Assert.assertEquals(1, tasks.get(0).getPosDeleteFiles().size());
    Assert.assertEquals(0, tasks.get(0).getInsertFileCnt());
    Assert.assertEquals(0, tasks.get(0).getDeleteFileCnt());
  }

  @Test
  public void testNoPartitionTableMajorOptimize() throws IOException {
    Pair<Snapshot, List<DataFile>> insertBaseResult = insertTableBaseDataFiles(testNoPartitionTable);
    List<DataFile> baseDataFiles = insertBaseResult.second();
    baseDataFilesInfo.addAll(baseDataFiles.stream()
        .map(dataFile ->
            DataFileInfoUtils.convertToDatafileInfo(dataFile, insertBaseResult.first(), testNoPartitionTable, false))
        .collect(Collectors.toList()));

    Set<DataTreeNode> targetNodes = baseDataFilesInfo.stream()
        .map(dataFileInfo -> DataTreeNode.of(dataFileInfo.getMask(), dataFileInfo.getIndex())).collect(Collectors.toSet());
    Pair<Snapshot, List<DeleteFile>> deleteResult =
        insertBasePosDeleteFiles(testNoPartitionTable, baseDataFiles, targetNodes);
    List<DeleteFile> deleteFiles = deleteResult.second();
    posDeleteFilesInfo.addAll(deleteFiles.stream()
        .map(deleteFile ->
            DataFileInfoUtils.convertToDatafileInfo(deleteFile, deleteResult.first(), testNoPartitionTable.asKeyedTable()))
        .collect(Collectors.toList()));

    List<FileScanTask> baseFiles = planBaseFiles(testNoPartitionTable);
    MajorOptimizePlan majorOptimizePlan = new MajorOptimizePlan(testNoPartitionTable,
        new TableOptimizeRuntime(testNoPartitionTable.id()), baseFiles,
        1, System.currentTimeMillis(), TableOptimizeRuntime.INVALID_SNAPSHOT_ID);
    List<BasicOptimizeTask> tasks = majorOptimizePlan.plan().getOptimizeTasks();

    Assert.assertEquals(OptimizeType.Major, tasks.get(0).getTaskId().getType());
    Assert.assertEquals(4, tasks.size());
    Assert.assertEquals(10, tasks.get(0).getBaseFiles().size());
    Assert.assertEquals(1, tasks.get(0).getPosDeleteFiles().size());
    Assert.assertEquals(0, tasks.get(0).getInsertFileCnt());
    Assert.assertEquals(0, tasks.get(0).getDeleteFileCnt());
  }

  @Test
  public void testNoPartitionTableFullOptimize() throws IOException {
    Pair<Snapshot, List<DataFile>> insertBaseResult = insertTableBaseDataFiles(testNoPartitionTable);
    List<DataFile> baseDataFiles = insertBaseResult.second();
    baseDataFilesInfo.addAll(baseDataFiles.stream()
        .map(dataFile ->
            DataFileInfoUtils.convertToDatafileInfo(dataFile, insertBaseResult.first(), testNoPartitionTable, false))
        .collect(Collectors.toList()));

    Set<DataTreeNode> targetNodes = baseDataFilesInfo.stream()
        .map(dataFileInfo -> DataTreeNode.of(dataFileInfo.getMask(), dataFileInfo.getIndex())).collect(Collectors.toSet());
    Pair<Snapshot, List<DeleteFile>> deleteResult =
        insertBasePosDeleteFiles(testNoPartitionTable, baseDataFiles, targetNodes);
    List<DeleteFile> deleteFiles = deleteResult.second();
    posDeleteFilesInfo.addAll(deleteFiles.stream()
        .map(deleteFile ->
            DataFileInfoUtils.convertToDatafileInfo(deleteFile, deleteResult.first(), testNoPartitionTable.asKeyedTable()))
        .collect(Collectors.toList()));

    testNoPartitionTable.updateProperties()
        .set(TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO, "0")
        .commit();

    List<FileScanTask> baseFiles = planBaseFiles(testNoPartitionTable);
    FullOptimizePlan fullOptimizePlan = new FullOptimizePlan(testNoPartitionTable,
        new TableOptimizeRuntime(testNoPartitionTable.id()), baseFiles,
        1, System.currentTimeMillis(), TableOptimizeRuntime.INVALID_SNAPSHOT_ID);
    List<BasicOptimizeTask> tasks = fullOptimizePlan.plan().getOptimizeTasks();

    Assert.assertEquals(OptimizeType.FullMajor, tasks.get(0).getTaskId().getType());
    Assert.assertEquals(4, tasks.size());
    Assert.assertEquals(10, tasks.get(0).getBaseFiles().size());
    Assert.assertEquals(1, tasks.get(0).getPosDeleteFiles().size());
    Assert.assertEquals(0, tasks.get(0).getInsertFileCnt());
    Assert.assertEquals(0, tasks.get(0).getDeleteFileCnt());
  }

  @Test
  public void testPartitionWeight() {
    List<AbstractOptimizePlan.PartitionWeightWrapper> partitionWeights = new ArrayList<>();
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p1",
        new MajorOptimizePlan.MajorPartitionWeight(true,0)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p2",
        new MajorOptimizePlan.MajorPartitionWeight(true, 1)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p3",
        new MajorOptimizePlan.MajorPartitionWeight(false, 200)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p4",
        new MajorOptimizePlan.MajorPartitionWeight(false, 100)));

    List<String> sortedPartitions = partitionWeights.stream()
        .sorted()
        .map(AbstractOptimizePlan.PartitionWeightWrapper::getPartition)
        .collect(Collectors.toList());
    Assert.assertEquals("p2", sortedPartitions.get(0));
    Assert.assertEquals("p1", sortedPartitions.get(1));
    Assert.assertEquals("p3", sortedPartitions.get(2));
    Assert.assertEquals("p4", sortedPartitions.get(3));
  }

  @Test
  public void testFullPartitionWeight() {
    List<AbstractOptimizePlan.PartitionWeightWrapper> partitionWeights = new ArrayList<>();
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p1",
        new FullOptimizePlan.FullPartitionWeight(true,0)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p2",
        new FullOptimizePlan.FullPartitionWeight(true, 1)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p3",
        new FullOptimizePlan.FullPartitionWeight(false, 200)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p4",
        new FullOptimizePlan.FullPartitionWeight(false, 100)));

    List<String> sortedPartitions = partitionWeights.stream()
        .sorted()
        .map(AbstractOptimizePlan.PartitionWeightWrapper::getPartition)
        .collect(Collectors.toList());
    Assert.assertEquals("p2", sortedPartitions.get(0));
    Assert.assertEquals("p1", sortedPartitions.get(1));
    Assert.assertEquals("p3", sortedPartitions.get(2));
    Assert.assertEquals("p4", sortedPartitions.get(3));
  }

  private List<DataFile> insertUnKeyedTableDataFiles(ArcticTable arcticTable) {
    List<DataFile> dataFiles = insertUnKeyedTableDataFile(FILE_A.partition(), LocalDateTime.of(2022, 1, 1, 12, 0, 0), 5);
    dataFiles.addAll(insertUnKeyedTableDataFile(FILE_B.partition(), LocalDateTime.of(2022, 1, 2, 12, 0, 0), 5));

    AppendFiles appendFiles = arcticTable.asUnkeyedTable().newAppend();
    dataFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
    arcticTable.asUnkeyedTable().refresh();
    Snapshot snapshot = arcticTable.asUnkeyedTable().currentSnapshot();

    baseDataFilesInfo = dataFiles.stream()
        .map(dataFile -> DataFileInfoUtils.convertToDatafileInfo(dataFile, snapshot, arcticTable, false))
        .collect(Collectors.toList());

    return dataFiles;
  }

  private void insertUnKeyedTablePosDeleteFiles(ArcticTable arcticTable) throws Exception {
    List<DataFile> dataFiles = insertUnKeyedTableDataFiles(arcticTable);

    Map<StructLike, List<DataFile>> dataFilesPartitionMap =
        new HashMap<>(dataFiles.stream().collect(Collectors.groupingBy(ContentFile::partition)));
    List<DeleteFile> deleteFiles = new ArrayList<>();
    for (Map.Entry<StructLike, List<DataFile>> dataFilePartitionMap : dataFilesPartitionMap.entrySet()) {
      StructLike partition = dataFilePartitionMap.getKey();
      List<DataFile> partitionFiles = dataFilePartitionMap.getValue();
      SortedPosDeleteWriter<Record> posDeleteWriter = AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
          .buildBasePosDeleteWriter(0, 0, partition);
      for (DataFile partitionFile : partitionFiles) {
        // write pos delete
        posDeleteWriter.delete(partitionFile.path(), 0);
      }
      deleteFiles.addAll(posDeleteWriter.complete());
    }

    UnkeyedTable baseTable = arcticTable.isKeyedTable() ?
        arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();
    RowDelta rowDelta = baseTable.newRowDelta();
    deleteFiles.forEach(rowDelta::addDeletes);
    rowDelta.commit();

    baseTable.refresh();
    Snapshot snapshot = baseTable.currentSnapshot();

    posDeleteFilesInfo.addAll(deleteFiles.stream()
        .map(deleteFile -> DataFileInfoUtils.convertToDatafileInfo(deleteFile, snapshot, arcticTable))
        .collect(Collectors.toList()));
  }

  private List<DataFile> insertUnKeyedTableDataFile(StructLike partitionData, LocalDateTime opTime, int count) {
    List<DataFile> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      AdaptHiveGenericAppenderFactory
          appenderFactory = new AdaptHiveGenericAppenderFactory(testTable.schema(), testTable.spec());
      FileFormat fileFormat = FileFormat.valueOf((testTable.properties().getOrDefault(TableProperties.BASE_FILE_FORMAT,
          TableProperties.BASE_FILE_FORMAT_DEFAULT).toUpperCase(Locale.ENGLISH)));
      OutputFileFactory outputFileFactory = OutputFileFactory
          .builderFor(testTable, 0, 0).format(fileFormat).build();
      EncryptedOutputFile outputFile = outputFileFactory.newOutputFile(partitionData);
      DataFile targetFile = testTable.io().doAs(() -> {
        DataWriter<Record> writer = appenderFactory
            .newDataWriter(outputFile, FileFormat.PARQUET, partitionData);
        for (Record record : baseRecords(1, 100, opTime)) {
          writer.add(record);
        }
        writer.close();
        return writer.toDataFile();
      });

      result.add(targetFile);
    }

    return result;
  }

  private List<Record> baseRecords(int start, int length, LocalDateTime opTime) {
    GenericRecord record = GenericRecord.create(TABLE_SCHEMA);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (int i = start; i < start + length; i++) {
      builder.add(record.copy(ImmutableMap.of("id", i, "name", "name" + i, "op_time", opTime)));
    }

    return builder.build();
  }
}
