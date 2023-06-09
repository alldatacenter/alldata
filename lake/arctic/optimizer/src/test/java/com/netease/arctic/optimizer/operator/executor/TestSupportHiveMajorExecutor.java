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
import com.netease.arctic.ams.api.OptimizeTaskId;
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.file.ContentFileWithSequence;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.util.ContentFileUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class TestSupportHiveMajorExecutor extends TestSupportHiveMajorOptimizeBase {
  @Test
  public void testMajorExecutor() throws Exception {
    insertBasePosDeleteFiles(testKeyedHiveTable, 2L, baseDataFilesInfo, posDeleteFilesInfo);
    NodeTask nodeTask = constructNodeTask(testKeyedHiveTable, OptimizeType.Major);
    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    MajorExecutor majorExecutor = new MajorExecutor(nodeTask, testKeyedHiveTable, System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = majorExecutor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 4);
    result.getTargetFiles().forEach(dataFile -> {
      Assert.assertEquals(240, dataFile.recordCount());
      Assert.assertTrue(dataFile.path().toString().contains(testKeyedHiveTable.baseLocation()));
    });
  }

  @Test
  public void testNoPosDeleteMajorExecutor() throws Exception {
    insertTableBaseDataFiles(testKeyedHiveTable, 2L, baseDataFilesInfo);
    NodeTask nodeTask = constructNodeTask(testKeyedHiveTable, OptimizeType.Major);
    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    MajorExecutor majorExecutor = new MajorExecutor(nodeTask, testKeyedHiveTable, System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = majorExecutor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 4);
    result.getTargetFiles().forEach(dataFile -> {
      Assert.assertEquals(250, dataFile.recordCount());
      Assert.assertTrue(dataFile.path().toString().contains(testKeyedHiveTable.baseLocation()));
    });
  }

  @Test
  public void testFullMajorExecutor() throws Exception {
    insertBasePosDeleteFiles(testKeyedHiveTable, 2L, baseDataFilesInfo, posDeleteFilesInfo);
    NodeTask nodeTask = constructNodeTask(testKeyedHiveTable, OptimizeType.FullMajor);
    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    MajorExecutor majorExecutor = new MajorExecutor(nodeTask, testKeyedHiveTable, System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = majorExecutor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 4);
    result.getTargetFiles().forEach(dataFile -> {
      Assert.assertEquals(240, dataFile.recordCount());
      Assert.assertTrue(dataFile.path().toString().contains(testKeyedHiveTable.hiveLocation()));
    });
  }

  @Test
  public void testUnKeyedTableMajorExecutor() throws Exception {
    insertTableBaseDataFiles(testHiveTable, null, baseDataFilesInfo);
    NodeTask nodeTask = constructNodeTask(testHiveTable, OptimizeType.Major);
    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    MajorExecutor majorExecutor = new MajorExecutor(nodeTask, testHiveTable, System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = majorExecutor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 1);
    result.getTargetFiles().forEach(dataFile -> {
      Assert.assertEquals(1000, dataFile.recordCount());
      Assert.assertTrue(dataFile.path().toString().contains(testHiveTable.location()));
    });
  }

  @Test
  public void testUnKeyedTableFullMajorExecutor() throws Exception {
    insertTableBaseDataFiles(testHiveTable, null, baseDataFilesInfo);
    NodeTask nodeTask = constructNodeTask(testHiveTable, OptimizeType.FullMajor);
    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    MajorExecutor majorExecutor = new MajorExecutor(nodeTask, testHiveTable, System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = majorExecutor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 1);
    result.getTargetFiles().forEach(dataFile -> {
      Assert.assertEquals(1000, dataFile.recordCount());
      Assert.assertTrue(dataFile.path().toString().contains(testHiveTable.hiveLocation()));
    });
  }

  @Test
  public void testNoPartitionTableMajorExecutor() throws Exception {
    insertBasePosDeleteFiles(testUnPartitionKeyedHiveTable, 2L, baseDataFilesInfo, posDeleteFilesInfo);
    NodeTask nodeTask = constructNodeTask(testUnPartitionKeyedHiveTable, OptimizeType.Major);
    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    MajorExecutor majorExecutor = new MajorExecutor(nodeTask, testUnPartitionKeyedHiveTable, System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = majorExecutor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 4);
    result.getTargetFiles().forEach(dataFile -> {
      Assert.assertEquals(240, dataFile.recordCount());
      Assert.assertTrue(dataFile.path().toString().contains(testUnPartitionKeyedHiveTable.baseLocation()));
    });
  }

  @Test
  public void testNoPartitionTableFullMajorExecutor() throws Exception {
    insertBasePosDeleteFiles(testUnPartitionKeyedHiveTable, 2L, baseDataFilesInfo, posDeleteFilesInfo);
    NodeTask nodeTask = constructNodeTask(testUnPartitionKeyedHiveTable, OptimizeType.FullMajor);
    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    MajorExecutor majorExecutor = new MajorExecutor(nodeTask, testUnPartitionKeyedHiveTable, System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = majorExecutor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 4);
    result.getTargetFiles().forEach(dataFile -> {
      Assert.assertEquals(240, dataFile.recordCount());
      Assert.assertTrue(dataFile.path().toString().contains(testUnPartitionKeyedHiveTable.hiveLocation()));
    });
  }

  private NodeTask constructNodeTask(ArcticTable arcticTable, OptimizeType optimizeType) {

    UnkeyedTable baseTable = arcticTable.isKeyedTable() ?
        arcticTable.asKeyedTable().baseTable() : arcticTable.asUnkeyedTable();

    String fileFormat = arcticTable.properties().getOrDefault(TableProperties.DEFAULT_FILE_FORMAT,
        TableProperties.DEFAULT_FILE_FORMAT_DEFAULT);

    List<ContentFileWithSequence<?>> base =
        baseDataFilesInfo.stream().map(s -> ContentFileUtil.buildContentFile(s, baseTable.spec(),
            fileFormat)).collect(Collectors.toList());
    List<ContentFileWithSequence<?>> pos =
        posDeleteFilesInfo.stream().map(s -> ContentFileUtil.buildContentFile(s, baseTable.spec(),
            fileFormat)).collect(Collectors.toList());

    NodeTask nodeTask = new NodeTask(base, null, null, pos, true);
    nodeTask.setSourceNodes(baseDataFilesInfo.stream()
        .map(dataFileInfo -> DataTreeNode.of(dataFileInfo.getMask(), dataFileInfo.getIndex()))
        .collect(Collectors.toSet()));
    nodeTask.setTableIdentifier(arcticTable.id());
    nodeTask.setTaskId(new OptimizeTaskId(optimizeType, UUID.randomUUID().toString()));
    nodeTask.setAttemptId(Math.abs(ThreadLocalRandom.current().nextInt()));
    nodeTask.setPartition(FILE_A.partition());

    return nodeTask;
  }
}
