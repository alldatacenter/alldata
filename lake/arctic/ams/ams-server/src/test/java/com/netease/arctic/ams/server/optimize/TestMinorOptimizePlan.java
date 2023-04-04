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

import com.netease.arctic.ams.api.DataFileInfo;
import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.util.DataFileInfoUtils;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.ChangeLocationKind;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.util.Pair;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TestMinorOptimizePlan extends TestBaseOptimizeBase {
  protected List<DataFileInfo> changeInsertFilesInfo = new ArrayList<>();
  protected List<DataFileInfo> changeDeleteFilesInfo = new ArrayList<>();

  @Test
  public void testMinorOptimize() throws IOException {
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
        .map(deleteFile ->
            DataFileInfoUtils.convertToDatafileInfo(deleteFile, deleteResult.first(), testKeyedTable.asKeyedTable()))
        .collect(Collectors.toList()));
    insertChangeDeleteFiles(testKeyedTable);
    insertChangeDataFiles(testKeyedTable);

    KeyedTableScanResult keyedTableScanResult = planKeyedTableFiles(testKeyedTable.asKeyedTable());
    MinorOptimizePlan minorOptimizePlan = new MinorOptimizePlan(testKeyedTable,
        new TableOptimizeRuntime(testKeyedTable.id()), keyedTableScanResult.getBaseFiles(),
        keyedTableScanResult.getChangeFiles(),
        1, System.currentTimeMillis(),
        testKeyedTable.changeTable().currentSnapshot().snapshotId(), TableOptimizeRuntime.INVALID_SNAPSHOT_ID);
    List<BasicOptimizeTask> tasks = minorOptimizePlan.plan().getOptimizeTasks();
    Assert.assertEquals(4, tasks.size());
    Assert.assertEquals(10, tasks.get(0).getBaseFiles().size());
    Assert.assertEquals(1, tasks.get(0).getPosDeleteFiles().size());
    Assert.assertEquals(10, tasks.get(0).getInsertFileCnt());
    Assert.assertEquals(10, tasks.get(0).getDeleteFileCnt());
  }

  @Test
  public void testPartitionWeight() {
    List<AbstractOptimizePlan.PartitionWeightWrapper> partitionWeights = new ArrayList<>();
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p1",
        new MinorOptimizePlan.MinorPartitionWeight(true,0)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p2",
        new MinorOptimizePlan.MinorPartitionWeight(true, 1)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p3",
        new MinorOptimizePlan.MinorPartitionWeight(false, 200)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p4",
        new MinorOptimizePlan.MinorPartitionWeight(false, 100)));

    List<String> sortedPartitions = partitionWeights.stream()
        .sorted()
        .map(AbstractOptimizePlan.PartitionWeightWrapper::getPartition)
        .collect(Collectors.toList());
    Assert.assertEquals("p2", sortedPartitions.get(0));
    Assert.assertEquals("p1", sortedPartitions.get(1));
    Assert.assertEquals("p3", sortedPartitions.get(2));
    Assert.assertEquals("p4", sortedPartitions.get(3));
  }

  protected List<DataFile> insertChangeDeleteFiles(ArcticTable arcticTable) throws IOException {
    AtomicInteger taskId = new AtomicInteger();
    List<DataFile> changeDeleteFiles = new ArrayList<>();
    // delete 1000 records in 1 partitions(2022-1-1)
    int length = 100;
    for (int i = 1; i < length * 10; i = i + length) {
      TaskWriter<Record> writer = AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
          .withChangeAction(ChangeAction.DELETE)
          .withTaskId(taskId.incrementAndGet())
          .buildWriter(ChangeLocationKind.INSTANT);
      for (Record record : baseRecords(i, length, arcticTable.schema())) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      changeDeleteFiles.addAll(Arrays.asList(result.dataFiles()));
    }
    AppendFiles baseAppend = arcticTable.asKeyedTable().changeTable().newAppend();
    changeDeleteFiles.forEach(baseAppend::appendFile);
    baseAppend.commit();
    arcticTable.asKeyedTable().changeTable().refresh();
    Snapshot snapshot = arcticTable.asKeyedTable().changeTable().currentSnapshot();

    changeDeleteFilesInfo = changeDeleteFiles.stream()
        .map(deleteFile -> DataFileInfoUtils.convertToDatafileInfo(deleteFile, snapshot, arcticTable, false))
        .collect(Collectors.toList());
    return changeDeleteFiles;
  }

  protected List<PrimaryKeyedFile> insertChangeDataFiles(ArcticTable arcticTable) throws IOException {
    AtomicInteger taskId = new AtomicInteger();
    List<DataFile> changeInsertFiles = new ArrayList<>();
    // write 1000 records to 1 partitions(2022-1-1)
    int length = 100;
    for (int i = 1; i < length * 10; i = i + length) {
      TaskWriter<Record> writer = AdaptHiveGenericTaskWriterBuilder.builderFor(arcticTable)
          .withChangeAction(ChangeAction.INSERT)
          .withTaskId(taskId.incrementAndGet())
          .buildWriter(ChangeLocationKind.INSTANT);
      for (Record record : baseRecords(i, length, arcticTable.schema())) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      changeInsertFiles.addAll(Arrays.asList(result.dataFiles()));
    }
    AppendFiles baseAppend = arcticTable.asKeyedTable().changeTable().newAppend();
    changeInsertFiles.forEach(baseAppend::appendFile);
    baseAppend.commit();
    arcticTable.asKeyedTable().changeTable().refresh();
    Snapshot snapshot = arcticTable.asKeyedTable().changeTable().currentSnapshot();

    changeInsertFilesInfo = changeInsertFiles.stream()
        .map(dataFile -> DataFileInfoUtils.convertToDatafileInfo(dataFile, snapshot, arcticTable, true))
        .collect(Collectors.toList());

    return changeInsertFiles.stream()
        .map(dataFile -> DefaultKeyedFile.parseChange(dataFile, snapshot.sequenceNumber()))
        .collect(Collectors.toList());
  }
}
