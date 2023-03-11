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

import com.google.common.collect.Iterables;
import com.netease.arctic.ams.api.Constants;
import com.netease.arctic.ams.api.DataFileInfo;
import com.netease.arctic.ams.api.OptimizeTaskId;
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.file.ContentFileWithSequence;
import com.netease.arctic.io.writer.GenericChangeTaskWriter;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.util.ContentFileUtil;
import com.netease.arctic.optimizer.util.DataFileInfoUtils;
import com.netease.arctic.table.TableProperties;
import org.apache.hadoop.fs.Path;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.WriteResult;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class TestMinorExecutor extends TestBaseOptimizeBase {
  protected List<DataFileInfo> changeInsertFilesInfo = new ArrayList<>();
  protected List<DataFileInfo> changeDeleteFilesInfo = new ArrayList<>();

  @Test
  public void testMinorExecutor() throws Exception {
    insertBasePosDeleteFiles(testKeyedTable, 2L, baseDataFilesInfo, posDeleteFilesInfo);
    insertChangeDeleteFiles(3);
    insertChangeDataFiles(4);

    NodeTask nodeTask = constructNodeTask();
    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    MinorExecutor minorExecutor = new MinorExecutor(nodeTask, testKeyedTable, System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = minorExecutor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 4);
    result.getTargetFiles().forEach(dataFile -> {
      Assert.assertEquals(250, dataFile.recordCount());
      Assert.assertTrue(dataFile.path().toString().contains(new Path(testKeyedTable.baseLocation()).toString()));
    });
  }

  @Test
  public void testNoPartitionTableMinorExecutor() throws Exception {
    insertBasePosDeleteFiles(testNoPartitionTable, 2L, baseDataFilesInfo, posDeleteFilesInfo);
    insertChangeDeleteFiles(3);
    insertChangeDataFiles(4);

    NodeTask nodeTask = constructNodeTask();
    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    MinorExecutor minorExecutor = new MinorExecutor(nodeTask, testNoPartitionTable, System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = minorExecutor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 4);
    result.getTargetFiles().forEach(dataFile -> {
      Assert.assertEquals(250, dataFile.recordCount());
      Assert.assertTrue(dataFile.path().toString().contains(new Path(testNoPartitionTable.baseLocation()).toString()));
    });
  }

  private NodeTask constructNodeTask() {
    String fileFormat = testKeyedTable.properties().getOrDefault(TableProperties.DEFAULT_FILE_FORMAT,
        TableProperties.DEFAULT_FILE_FORMAT_DEFAULT);
    List<ContentFileWithSequence<?>> base =
        baseDataFilesInfo.stream().map(s -> ContentFileUtil.buildContentFile(s, testKeyedTable.baseTable().spec(),
            fileFormat)).collect(Collectors.toList());
    List<ContentFileWithSequence<?>> pos =
        posDeleteFilesInfo.stream().map(s -> ContentFileUtil.buildContentFile(s, testKeyedTable.baseTable().spec(),
            fileFormat)).collect(Collectors.toList());
    List<ContentFileWithSequence<?>> changeInsert =
        changeInsertFilesInfo.stream().map(s -> ContentFileUtil.buildContentFile(s, testKeyedTable.baseTable().spec(),
            fileFormat)).collect(Collectors.toList());
    List<ContentFileWithSequence<?>> changeDelete =
        changeDeleteFilesInfo.stream().map(s -> ContentFileUtil.buildContentFile(s, testKeyedTable.baseTable().spec(),
            fileFormat)).collect(Collectors.toList());

    NodeTask nodeTask = new NodeTask(base, changeInsert, changeDelete, pos, true);
    nodeTask.setSourceNodes(baseDataFilesInfo.stream()
        .map(dataFileInfo -> DataTreeNode.of(dataFileInfo.getMask(), dataFileInfo.getIndex()))
        .collect(Collectors.toSet()));
    nodeTask.setTableIdentifier(testKeyedTable.id());
    nodeTask.setTaskId(new OptimizeTaskId(OptimizeType.Minor, UUID.randomUUID().toString()));
    nodeTask.setAttemptId(Math.abs(ThreadLocalRandom.current().nextInt()));
    nodeTask.setPartition(FILE_A.partition());

    return nodeTask;
  }

  protected void insertChangeDeleteFiles(long transactionId) throws IOException {


    List<DataFile> changeDeleteFiles = new ArrayList<>();
    // delete 1000 records in 2 partitions(2022-1-1\2022-1-2)
    int length = 100;
    for (int i = 1; i < length * 10; i = i + length) {
      GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(testKeyedTable)
          .withChangeAction(ChangeAction.DELETE)
          .withTransactionId(transactionId).buildChangeWriter();
      for (Record record : baseRecords(i, length, testKeyedTable.changeTable().schema())) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      changeDeleteFiles.addAll(Arrays.asList(result.dataFiles()));
    }
    AppendFiles baseAppend = testKeyedTable.changeTable().newAppend();
    changeDeleteFiles.forEach(baseAppend::appendFile);
    baseAppend.commit();
    Snapshot snapshot = testKeyedTable.changeTable().currentSnapshot();

    changeDeleteFilesInfo = changeDeleteFiles.stream()
        .map(deleteFile -> DataFileInfoUtils.convertToDatafileInfo(deleteFile, snapshot, testKeyedTable,
            Constants.INNER_TABLE_CHANGE))
        .collect(Collectors.toList());
  }

  protected void insertChangeDataFiles(long transactionId) throws IOException {

    List<DataFile> changeInsertFiles = new ArrayList<>();
    // write 1000 records to 2 partitions(2022-1-1\2022-1-2)
    int length = 100;
    for (int i = 1; i < length * 10; i = i + length) {
      GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(testKeyedTable)
          .withChangeAction(ChangeAction.INSERT)
          .withTransactionId(transactionId).buildChangeWriter();

      for (Record record : baseRecords(i, length, testKeyedTable.changeTable().schema())) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      changeInsertFiles.addAll(Arrays.asList(result.dataFiles()));
    }
    AppendFiles baseAppend = testKeyedTable.changeTable().newAppend();
    changeInsertFiles.forEach(baseAppend::appendFile);
    baseAppend.commit();
    Snapshot snapshot = testKeyedTable.changeTable().currentSnapshot();

    changeInsertFilesInfo = changeInsertFiles.stream()
        .map(dataFile -> DataFileInfoUtils.convertToDatafileInfo(dataFile, snapshot, testKeyedTable,
            Constants.INNER_TABLE_CHANGE))
        .collect(Collectors.toList());
  }
}
