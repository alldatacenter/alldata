package com.netease.arctic.ams.server.optimize;

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

public class TestIcebergMinorOptimizePlan extends TestIcebergBase {
  @Test
  public void testUnPartitionMinorOptimize() throws Exception {
    UnkeyedTable table = icebergNoPartitionTable.asUnkeyedTable();
    // 10 small files
    List<DataFile> smallDataFiles = insertDataFiles(table, "", 12, 1, 200);
    table.updateProperties()
        .set(TableProperties.SELF_OPTIMIZING_FRAGMENT_RATIO,
            TableProperties.SELF_OPTIMIZING_TARGET_SIZE_DEFAULT / (smallDataFiles.get(0).fileSizeInBytes() + 100) + "")
        .commit();
    // 10 big files
    List<DataFile> dataFiles = insertDataFiles(table, "", 12, 10, 1);

    // 1 equ-delete for 1 small file
    insertEqDeleteFiles(table, 200);
    // 1 equ-delete for 1 big file
    insertEqDeleteFiles(table, 1);
    // 1 pos-delete for each big file
    insertPosDeleteFiles(table, dataFiles);

    List<FileScanTask> fileScanTasks;
    try (CloseableIterable<FileScanTask> filesIterable = table.newScan()
        .planFiles()) {
      fileScanTasks = Lists.newArrayList(filesIterable);
    }
    long currentSnapshotId = table.currentSnapshot().snapshotId();
    IcebergMinorOptimizePlan optimizePlan = new IcebergMinorOptimizePlan(table,
        new TableOptimizeRuntime(table.id()),
        fileScanTasks, 1, System.currentTimeMillis(),
        currentSnapshotId);
    OptimizePlanResult planResult = optimizePlan.plan();
    assertPlanResult(planResult, 1, 2, currentSnapshotId);
    assertTask(planResult.getOptimizeTasks().get(0), "", 0, 12, 1, 0);
    assertTask(planResult.getOptimizeTasks().get(1), "", 12, 0, 1, 1);
  }

  @Test
  public void testPartitionMinorOptimize() throws Exception {
    UnkeyedTable table = icebergPartitionTable.asUnkeyedTable();
    // partition1:
    // 10 small files
    List<DataFile> smallDataFiles = insertDataFiles(table, "name1", 12, 1, 200);
    table.updateProperties()
        .set(TableProperties.SELF_OPTIMIZING_FRAGMENT_RATIO,
            TableProperties.SELF_OPTIMIZING_TARGET_SIZE_DEFAULT / (smallDataFiles.get(0).fileSizeInBytes() + 100) + "")
        .commit();
    // 10 big files
    List<DataFile> dataFiles = insertDataFiles(table, "name1", 12, 10, 1);

    // 1 equ-delete for 1 small file
    insertEqDeleteFiles(table, "name1", 200);
    // 1 equ-delete for 1 small file
    insertEqDeleteFiles(table, "name1", 1);
    // 1 pos-delete for each big file
    insertPosDeleteFiles(table, dataFiles);

    // partition2:
    // 10 small files
    insertDataFiles(table, "name2", 12, 1, 300);

    List<FileScanTask> fileScanTasks;
    try (CloseableIterable<FileScanTask> filesIterable = table.newScan()
        .planFiles()) {
      fileScanTasks = Lists.newArrayList(filesIterable);
    }
    long currentSnapshotId = table.currentSnapshot().snapshotId();
    IcebergMinorOptimizePlan optimizePlan = new IcebergMinorOptimizePlan(table,
        new TableOptimizeRuntime(table.id()),
        fileScanTasks, 1, System.currentTimeMillis(),
        currentSnapshotId);
    OptimizePlanResult planResult = optimizePlan.plan();
    assertPlanResult(planResult, 2, 3, currentSnapshotId);
    assertTask(planResult.getOptimizeTasks().get(0), "name=name1", 0, 12, 1, 0);
    assertTask(planResult.getOptimizeTasks().get(1), "name=name1", 12, 0, 1, 1);
    assertTask(planResult.getOptimizeTasks().get(2), "name=name2", 0, 12, 0, 0);
  }

  @Test
  public void testPartitionPartialMinorOptimize() throws Exception {
    UnkeyedTable table = icebergPartitionTable.asUnkeyedTable();
    table.updateProperties()
        .set(TableProperties.SELF_OPTIMIZING_MAX_FILE_CNT, "18")
        .set(TableProperties.SELF_OPTIMIZING_MINOR_TRIGGER_FILE_CNT, "4")
        .commit();
    insertDataFiles(table, "name1", 5, 1, 1);
    insertDataFiles(table, "name2", 10, 1, 100);
    insertDataFiles(table, "name3", 8, 1, 200);
    insertDataFiles(table, "name4", 2, 1, 300);

    List<FileScanTask> fileScanTasks;
    try (CloseableIterable<FileScanTask> filesIterable = table.newScan().planFiles()) {
      fileScanTasks = Lists.newArrayList(filesIterable);
    }
    long currentSnapshotId = table.currentSnapshot().snapshotId();
    IcebergMinorOptimizePlan optimizePlan = new IcebergMinorOptimizePlan(table,
        new TableOptimizeRuntime(table.id()),
        fileScanTasks, 1, System.currentTimeMillis(),
        currentSnapshotId);
    OptimizePlanResult planResult = optimizePlan.plan();
    assertPlanResult(planResult, 2, 2, currentSnapshotId);
    assertTask(planResult.getOptimizeTasks().get(0), "name=name2", 0, 10, 0, 0);
    assertTask(planResult.getOptimizeTasks().get(1), "name=name3", 0, 8, 0, 0);
  }

  @Test
  public void testPartitionWeight() {
    List<AbstractOptimizePlan.PartitionWeightWrapper> partitionWeights = new ArrayList<>();
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p1",
        new IcebergMinorOptimizePlan.IcebergMinorPartitionWeight(0)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p2",
        new IcebergMinorOptimizePlan.IcebergMinorPartitionWeight(1)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p3",
        new IcebergMinorOptimizePlan.IcebergMinorPartitionWeight(200)));
    partitionWeights.add(new AbstractOptimizePlan.PartitionWeightWrapper("p4",
        new IcebergMinorOptimizePlan.IcebergMinorPartitionWeight(100)));

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
