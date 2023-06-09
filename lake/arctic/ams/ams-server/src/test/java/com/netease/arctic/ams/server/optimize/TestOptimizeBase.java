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
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.data.file.ContentFileWithSequence;
import com.netease.arctic.data.file.FileNameGenerator;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.hive.utils.HiveTableUtil;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import com.netease.arctic.scan.ChangeTableIncrementalScan;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.BaseLocationKind;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.table.WriteOperationKind;
import com.netease.arctic.utils.IdGenerator;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.util.Pair;
import org.apache.iceberg.util.StructLikeMap;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface TestOptimizeBase {
  List<Record> baseRecords(int start, int length, Schema tableSchema);

  default Pair<Snapshot, List<DataFile>> insertTableBaseDataFiles(UnkeyedTable unkeyedTable) throws IOException {
    AtomicInteger taskId = new AtomicInteger();
    String hiveSubDir = HiveTableUtil.newHiveSubdirectory(IdGenerator.randomId());
    Supplier<TaskWriter<Record>> taskWriterSupplier = () ->
        AdaptHiveGenericTaskWriterBuilder.builderFor(unkeyedTable)
            .withTaskId(taskId.incrementAndGet())
            .withCustomHiveSubdirectory(hiveSubDir)
            .buildWriter(BaseLocationKind.INSTANT);
    List<DataFile> baseDataFiles = insertBaseDataFiles(taskWriterSupplier, unkeyedTable.schema());
    AppendFiles baseAppend = unkeyedTable.newAppend();
    baseDataFiles.forEach(baseAppend::appendFile);
    baseAppend.commit();

    unkeyedTable.refresh();
    Snapshot snapshot = unkeyedTable.currentSnapshot();

    return Pair.of(snapshot, baseDataFiles);
  }

  default Pair<Snapshot, List<DataFile>> insertTableBaseDataFiles(KeyedTable keyedTable)
      throws IOException {
    AtomicInteger taskId = new AtomicInteger();
    long transactionId = keyedTable.beginTransaction(null);
    String hiveSubDir = HiveTableUtil.newHiveSubdirectory(transactionId);
    Supplier<TaskWriter<Record>> taskWriterSupplier = () ->
        AdaptHiveGenericTaskWriterBuilder.builderFor(keyedTable)
            .withTransactionId(transactionId)
            .withTaskId(taskId.incrementAndGet())
            .withCustomHiveSubdirectory(hiveSubDir)
            .buildWriter(BaseLocationKind.INSTANT);
    List<DataFile> baseDataFiles = insertBaseDataFiles(taskWriterSupplier, keyedTable.schema());
    UnkeyedTable baseTable = keyedTable.asKeyedTable().baseTable();
    AppendFiles baseAppend = baseTable.newAppend();
    baseDataFiles.forEach(baseAppend::appendFile);
    baseAppend.commit();

    baseTable.refresh();
    Snapshot snapshot = baseTable.currentSnapshot();

    return Pair.of(snapshot, baseDataFiles);
  }

  default List<DataFile> insertOptimizeTargetDataFiles(ArcticTable arcticTable,
                                                       OptimizeType optimizeType,
                                                       long transactionId) throws IOException {
    final WriteOperationKind writeOperationKind;
    switch (optimizeType) {
      case FullMajor:
        writeOperationKind = WriteOperationKind.FULL_OPTIMIZE;
        break;
      case Major:
        writeOperationKind = WriteOperationKind.MAJOR_OPTIMIZE;
        break;
      case Minor:
        writeOperationKind = WriteOperationKind.MINOR_OPTIMIZE;
        break;
      default:
        throw new IllegalArgumentException("unknown optimize type " + optimizeType);
    }

    String hiveSubDir = HiveTableUtil.newHiveSubdirectory(IdGenerator.randomId());
    AtomicInteger taskId = new AtomicInteger();
    Supplier<TaskWriter<Record>> writerSupplier = () -> arcticTable.isKeyedTable() ?
        AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
            .withTransactionId(transactionId)
            .withCustomHiveSubdirectory(hiveSubDir)
            .withTaskId(taskId.incrementAndGet())
            .buildWriter(writeOperationKind) :
        AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
            .withCustomHiveSubdirectory(hiveSubDir)
            .withTaskId(taskId.incrementAndGet())
            .buildWriter(writeOperationKind);

    return insertBaseDataFiles(writerSupplier, arcticTable.schema());
  }

  default Pair<Snapshot, List<DeleteFile>> insertBasePosDeleteFiles(ArcticTable arcticTable,
                                                    List<DataFile> dataFiles,
                                                    Set<DataTreeNode> targetNodes) throws IOException {
    Long transactionId = null;
    if (arcticTable.isKeyedTable()) {
      transactionId = arcticTable.asKeyedTable().beginTransaction(null);
    }
    Map<StructLike, List<DataFile>> dataFilesPartitionMap =
        new HashMap<>(dataFiles.stream().collect(Collectors.groupingBy(ContentFile::partition)));
    List<DeleteFile> deleteFiles = new ArrayList<>();
    for (Map.Entry<StructLike, List<DataFile>> dataFilePartitionMap : dataFilesPartitionMap.entrySet()) {
      StructLike partition = dataFilePartitionMap.getKey();
      List<DataFile> partitionFiles = dataFilePartitionMap.getValue();
      Map<DataTreeNode, List<DataFile>> nodeFilesPartitionMap = new HashMap<>(partitionFiles.stream()
          .collect(Collectors.groupingBy(dataFile ->
              FileNameGenerator.parseFileNodeFromFileName(dataFile.path().toString()))));
      for (Map.Entry<DataTreeNode, List<DataFile>> nodeFilePartitionMap : nodeFilesPartitionMap.entrySet()) {
        DataTreeNode key = nodeFilePartitionMap.getKey();
        List<DataFile> nodeFiles = nodeFilePartitionMap.getValue();
        if (!targetNodes.contains(key)) {
          continue;
        }

        // write pos delete
        SortedPosDeleteWriter<Record> posDeleteWriter = AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
            .withTransactionId(transactionId)
            .buildBasePosDeleteWriter(key.mask(), key.index(), partition);
        for (DataFile nodeFile : nodeFiles) {
          posDeleteWriter.delete(nodeFile.path(), 0);
        }
        deleteFiles.addAll(posDeleteWriter.complete());
      }
    }

    UnkeyedTable baseTable = arcticTable.isKeyedTable() ?
        arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();
    RowDelta rowDelta = baseTable.newRowDelta();
    deleteFiles.forEach(rowDelta::addDeletes);
    rowDelta.commit();

    baseTable.refresh();
    Snapshot snapshot = baseTable.currentSnapshot();

    return Pair.of(snapshot, deleteFiles);
  }

  default List<DeleteFile> insertOptimizeTargetDeleteFiles(ArcticTable arcticTable,
                                                           List<PrimaryKeyedFile> dataFiles,
                                                           long transactionId) throws IOException {
    Map<StructLike, List<DataFile>> dataFilesPartitionMap =
        new HashMap<>(dataFiles.stream().collect(Collectors.groupingBy(ContentFile::partition)));
    List<DeleteFile> deleteFiles = new ArrayList<>();
    for (Map.Entry<StructLike, List<DataFile>> dataFilePartitionMap : dataFilesPartitionMap.entrySet()) {
      StructLike partition = dataFilePartitionMap.getKey();
      List<DataFile> partitionFiles = dataFilePartitionMap.getValue();
      Map<DataTreeNode, List<DataFile>> nodeFilesPartitionMap = new HashMap<>(partitionFiles.stream()
          .collect(Collectors.groupingBy(dataFile ->
              FileNameGenerator.parseFileNodeFromFileName(dataFile.path().toString()))));
      for (Map.Entry<DataTreeNode, List<DataFile>> nodeFilePartitionMap : nodeFilesPartitionMap.entrySet()) {
        DataTreeNode key = nodeFilePartitionMap.getKey();
        List<DataFile> nodeFiles = nodeFilePartitionMap.getValue();

        // write pos delete
        SortedPosDeleteWriter<Record> posDeleteWriter = AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
            .withTransactionId(transactionId)
            .buildBasePosDeleteWriter(key.mask(), key.index(), partition);
        for (DataFile nodeFile : nodeFiles) {
          posDeleteWriter.delete(nodeFile.path(), 0);
        }
        deleteFiles.addAll(posDeleteWriter.complete());
      }
    }

    return deleteFiles;
  }

  default List<DataFile> insertBaseDataFiles(Supplier<TaskWriter<Record>> writerSupplier, Schema schema) throws IOException {
    List<DataFile> baseDataFiles = new ArrayList<>();
    // write 1000 records to 1 partitions(name="name)
    int length = 100;
    for (int i = 1; i < length * 10; i = i + length) {
      TaskWriter<Record> writer = writerSupplier.get();
      for (Record record : baseRecords(i, length, schema)) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      baseDataFiles.addAll(Arrays.asList(result.dataFiles()));
    }

    return baseDataFiles;
  }

  default List<FileScanTask> planBaseFiles(ArcticTable arcticTable) {
    UnkeyedTable baseTable;
    if (arcticTable.isKeyedTable()) {
      baseTable = arcticTable.asKeyedTable().baseTable();
    } else {
      baseTable = arcticTable.asUnkeyedTable();
    }
    List<FileScanTask> baseFiles = new ArrayList<>();
    try (CloseableIterable<FileScanTask> fileScanTasks = baseTable.newScan()
        .planFiles()) {
      fileScanTasks.forEach(baseFiles::add);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close table scan of " + baseTable.name(), e);
    }
    return baseFiles;
  }

  default List<ContentFileWithSequence<?>> planChangeFiles(KeyedTable keyedTable,
                                                           StructLikeMap<Long> partitionOptimizedSequence,
                                                           StructLikeMap<Long> legacyPartitionMaxTransactionId) {
    ChangeTableIncrementalScan changeTableIncrementalScan =
        keyedTable.changeTable().newChangeScan()
            .fromSequence(partitionOptimizedSequence)
            .fromLegacyTransaction(legacyPartitionMaxTransactionId);
    List<ContentFileWithSequence<?>> changeFiles;
    try (CloseableIterable<ContentFileWithSequence<?>> files = changeTableIncrementalScan.planFilesWithSequence()) {
      changeFiles = Lists.newArrayList(files);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close table scan of " + keyedTable.name(), e);
    }
    return changeFiles;
  }

  default KeyedTableScanResult planKeyedTableFiles(KeyedTable keyedTable) {
    List<FileScanTask> baseFiles = planBaseFiles(keyedTable.baseTable());
    StructLikeMap<Long> partitionOptimizedSequence = TablePropertyUtil.getPartitionOptimizedSequence(keyedTable);
    StructLikeMap<Long> legacyPartitionMaxTransactionId =
        TablePropertyUtil.getLegacyPartitionMaxTransactionId(keyedTable);
    List<ContentFileWithSequence<?>> changeFiles =
        planChangeFiles(keyedTable, partitionOptimizedSequence, legacyPartitionMaxTransactionId);
    return new KeyedTableScanResult(baseFiles, changeFiles);
  }

  class KeyedTableScanResult {
    private final List<FileScanTask> baseFiles;
    private final List<ContentFileWithSequence<?>> changeFiles;

    public KeyedTableScanResult(List<FileScanTask> baseFiles,
                                List<ContentFileWithSequence<?>> changeFiles) {
      this.baseFiles = baseFiles;
      this.changeFiles = changeFiles;
    }

    public List<FileScanTask> getBaseFiles() {
      return baseFiles;
    }

    public List<ContentFileWithSequence<?>> getChangeFiles() {
      return changeFiles;
    }
  }
}
