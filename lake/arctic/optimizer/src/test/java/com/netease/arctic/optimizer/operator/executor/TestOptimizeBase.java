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

package com.netease.arctic.optimizer.operator.executor;

import com.netease.arctic.ams.api.Constants;
import com.netease.arctic.ams.api.DataFileInfo;
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.file.FileNameGenerator;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import com.netease.arctic.optimizer.util.DataFileInfoUtils;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.BaseLocationKind;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.table.WriteOperationKind;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public interface TestOptimizeBase {
  List<Record> baseRecords(int start, int length, Schema tableSchema);

  default List<DataFile> insertTableBaseDataFiles(ArcticTable arcticTable, Long transactionId,
                                                  List<DataFileInfo> baseDataFilesInfo) throws IOException {
    Supplier<TaskWriter<Record>> writerSupplier = () ->
        arcticTable.isKeyedTable() ?
        AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
            .withTransactionId(transactionId)
            .buildWriter(BaseLocationKind.INSTANT) :
        AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
            .buildWriter(BaseLocationKind.INSTANT);

    List<DataFile> baseDataFiles = insertBaseDataFiles(writerSupplier, arcticTable.schema());

    UnkeyedTable baseTable = arcticTable.isKeyedTable() ?
        arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();
    AppendFiles baseAppend = baseTable.newAppend();
    baseDataFiles.forEach(baseAppend::appendFile);
    baseAppend.commit();
    Snapshot snapshot = baseTable.currentSnapshot();

    baseDataFilesInfo.addAll(baseDataFiles.stream()
        .map(dataFile -> DataFileInfoUtils.convertToDatafileInfo(dataFile, snapshot, arcticTable,
            Constants.INNER_TABLE_BASE))
        .collect(Collectors.toList()));
    return baseDataFiles;
  }

  default List<DataFile> insertOptimizeTargetDataFiles(ArcticTable arcticTable,
                                                       OptimizeType optimizeType,
                                                       Long transactionId) throws IOException {
    WriteOperationKind writeOperationKind = getWriteOperationKind(optimizeType);

    Supplier<TaskWriter<Record>> writerSupplier = () -> arcticTable.isKeyedTable() ?
        AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
            .withTransactionId(transactionId)
            .buildWriter(writeOperationKind) :
        AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
            .buildWriter(writeOperationKind);

    return insertBaseDataFiles(writerSupplier , arcticTable.schema());
  }

  default WriteOperationKind getWriteOperationKind(OptimizeType optimizeType) {
    switch (optimizeType) {
      case Major:
        return WriteOperationKind.MAJOR_OPTIMIZE;
      case Minor:
        return WriteOperationKind.MINOR_OPTIMIZE;
      case FullMajor:
        return WriteOperationKind.FULL_OPTIMIZE;
    }
    throw new IllegalStateException("unknown kind optimize");
  }

  default List<DeleteFile> insertBasePosDeleteFiles(ArcticTable arcticTable,
                                                    Long transactionId,
                                                    List<DataFileInfo> baseDataFilesInfo,
                                                    List<DataFileInfo> posDeleteFilesInfo) throws IOException {
    List<DataFile> dataFiles = insertTableBaseDataFiles(arcticTable, transactionId - 1, baseDataFilesInfo);
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

    UnkeyedTable baseTable = arcticTable.isKeyedTable() ?
        arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();
    RowDelta rowDelta = baseTable.newRowDelta();
    deleteFiles.forEach(rowDelta::addDeletes);
    rowDelta.commit();
    Snapshot snapshot = baseTable.currentSnapshot();

    posDeleteFilesInfo.addAll(deleteFiles.stream()
        .map(deleteFile -> DataFileInfoUtils.convertToDatafileInfo(deleteFile, snapshot, arcticTable.asKeyedTable()))
        .collect(Collectors.toList()));

    return deleteFiles;
  }

  default List<DeleteFile> insertOptimizeTargetDeleteFiles(ArcticTable arcticTable,
                                                           List<DataFile> dataFiles,
                                                           Long transactionId) throws IOException {
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
    // write 1000 records to 1 partitions(2022-1-1)
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
}
