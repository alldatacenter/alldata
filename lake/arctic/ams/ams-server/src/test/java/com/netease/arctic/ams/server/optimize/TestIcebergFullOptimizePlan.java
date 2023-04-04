package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TestIcebergFullOptimizePlan extends TestIcebergBase {
  @Test
  public void testNoPartitionFullOptimize() throws Exception {
    UnkeyedTable table = icebergNoPartitionTable.asUnkeyedTable();
    table.updateProperties()
        .set(com.netease.arctic.table.TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO, "0")
        .commit();
    List<DataFile> dataFiles = insertDataFiles(table, 10, 10);
    insertEqDeleteFiles(table, 1);
    insertPosDeleteFiles(table, dataFiles);
    List<FileScanTask> fileScanTasks;
    try (CloseableIterable<FileScanTask> fileIterable = table.newScan()
        .planFiles()) {
      fileScanTasks = Lists.newArrayList(fileIterable);
    }
    long currentSnapshotId = table.currentSnapshot().snapshotId();
    IcebergFullOptimizePlan optimizePlan = new IcebergFullOptimizePlan(table,
        new TableOptimizeRuntime(table.id()),
        fileScanTasks, 1, System.currentTimeMillis(),
        currentSnapshotId);
    OptimizePlanResult planResult = optimizePlan.plan();
    assertPlanResult(planResult, 1, 1, currentSnapshotId);
    assertTask(planResult.getOptimizeTasks().get(0), "", 10, 0, 1, 1);
  }

  @Test
  public void testPartitionFullOptimize() throws Exception {
    UnkeyedTable table = icebergPartitionTable.asUnkeyedTable();
    table.updateProperties()
        .set(com.netease.arctic.table.TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO, "0")
        .commit();
    // partition1
    List<DataFile> dataFiles = insertDataFiles(table, "name1", 10, 10, 1);
    insertEqDeleteFiles(table, "name1", 1);
    insertPosDeleteFiles(table, dataFiles);
    // partition2
    insertDataFiles(table, "name2", 10, 10, 1);
    insertEqDeleteFiles(table, "name2", 1);
    List<FileScanTask> fileScanTasks;
    try (CloseableIterable<FileScanTask> fileIterable = table.newScan()
        .planFiles()) {
      fileScanTasks = Lists.newArrayList(fileIterable);
    }
    long currentSnapshotId = table.currentSnapshot().snapshotId();
    IcebergFullOptimizePlan optimizePlan = new IcebergFullOptimizePlan(table,
        new TableOptimizeRuntime(table.id()),
        fileScanTasks, 1, System.currentTimeMillis(),
        currentSnapshotId);
    OptimizePlanResult planResult = optimizePlan.plan();
    assertPlanResult(planResult, 2, 2, currentSnapshotId);
    assertTask(planResult.getOptimizeTasks().get(0), "name=name1", 10, 0, 1, 1);
    assertTask(planResult.getOptimizeTasks().get(1), "name=name2", 10, 0, 1, 0);
  }

  @Test
  public void testPartitionPartialFullOptimize() throws Exception {
    UnkeyedTable table = icebergPartitionTable.asUnkeyedTable();
    table.updateProperties()
        .set(TableProperties.SELF_OPTIMIZING_MAX_FILE_CNT, "18")
        .set(com.netease.arctic.table.TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO, "0")
        .commit();
    insertDataFiles(table, "name1", 5, 1, 1);
    insertDataFiles(table, "name2", 10, 1, 100);
    insertDataFiles(table, "name3", 8, 1, 200);
    insertDataFiles(table, "name4", 2, 1, 300);
    insertEqDeleteFiles(table, "name1", 1);
    insertEqDeleteFiles(table, "name1", 2);
    insertEqDeleteFiles(table, "name1", 3);
    insertEqDeleteFiles(table, "name2", 100);
    insertEqDeleteFiles(table, "name3", 200);
    insertEqDeleteFiles(table, "name3", 201);
    List<FileScanTask> fileScanTasks;
    try (CloseableIterable<FileScanTask> filesIterable = table.newScan().planFiles()) {
      fileScanTasks = Lists.newArrayList(filesIterable);
    }
    long currentSnapshotId = table.currentSnapshot().snapshotId();
    IcebergFullOptimizePlan optimizePlan = new IcebergFullOptimizePlan(table,
        new TableOptimizeRuntime(table.id()),
        fileScanTasks, 1, System.currentTimeMillis(),
        currentSnapshotId);
    OptimizePlanResult planResult = optimizePlan.plan();
    assertPlanResult(planResult, 2, 2, currentSnapshotId);
    assertTask(planResult.getOptimizeTasks().get(0), "name=name1", 5, 0, 3, 0);
    assertTask(planResult.getOptimizeTasks().get(1), "name=name3", 8, 0, 2, 0);
  }

  @Test
  public void testBinPackPlan() throws Exception {
    UnkeyedTable table = icebergNoPartitionTable.asUnkeyedTable();
    int packFileCnt = 3;
    List<DataFile> dataFiles = insertDataFiles(table, 10, 10);
    long targetSize = dataFiles.get(0).fileSizeInBytes() * packFileCnt + 100;
    table.updateProperties()
        .set(TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO, "0")
        .set(TableProperties.SELF_OPTIMIZING_TARGET_SIZE, targetSize + "")
        .commit();
    insertEqDeleteFiles(table, 1);
    insertPosDeleteFiles(table, dataFiles);
    List<FileScanTask> fileScanTasks;
    try (CloseableIterable<FileScanTask> fileIterable = table.newScan()
        .planFiles()) {
      fileScanTasks = Lists.newArrayList(fileIterable);
    }
    IcebergFullOptimizePlan optimizePlan = new IcebergFullOptimizePlan(table,
        new TableOptimizeRuntime(table.id()),
        fileScanTasks, 1, System.currentTimeMillis(),
        table.currentSnapshot().snapshotId());
    List<BasicOptimizeTask> tasks = optimizePlan.plan().getOptimizeTasks();
    Assert.assertEquals((int) Math.ceil(1.0 * dataFiles.size() / packFileCnt), tasks.size());
  }

  @Test
  public void testPartitionWeight() {
    List<AbstractOptimizePlan.PartitionWeightWrapper> partitionWeights = new ArrayList<>();
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p1",
        new IcebergFullOptimizePlan.IcebergFullPartitionWeight(0)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p2",
        new IcebergFullOptimizePlan.IcebergFullPartitionWeight(1)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p3",
        new IcebergFullOptimizePlan.IcebergFullPartitionWeight(200)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p4",
        new IcebergFullOptimizePlan.IcebergFullPartitionWeight(100)));

    List<String> sortedPartitions = partitionWeights.stream()
        .sorted()
        .map(AbstractOptimizePlan.PartitionWeightWrapper::getPartition)
        .collect(Collectors.toList());
    Assert.assertEquals("p3", sortedPartitions.get(0));
    Assert.assertEquals("p4", sortedPartitions.get(1));
    Assert.assertEquals("p2", sortedPartitions.get(2));
    Assert.assertEquals("p1", sortedPartitions.get(3));
  }
}
