package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.ams.api.OptimizeStatus;
import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.OptimizeTaskRuntime;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.utils.JDBCSqlSessionFactoryProvider;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.SerializationUtils;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@PrepareForTest({
    JDBCSqlSessionFactoryProvider.class
})
@PowerMockIgnore({"org.apache.logging.log4j.*", "javax.management.*", "org.apache.http.conn.ssl.*",
    "com.amazonaws.http.conn.ssl.*",
    "javax.net.ssl.*", "org.apache.hadoop.*", "javax.*", "com.sun.org.apache.*", "org.apache.xerces.*"})
public class TestIcebergFullOptimizeCommit extends TestIcebergBase {
  @Test
  public void testNoPartitionTableMajorOptimizeCommit() throws Exception {
    UnkeyedTable table = icebergNoPartitionTable.asUnkeyedTable();
    table.updateProperties()
        .set(TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO, "0")
        .commit();
    List<DataFile> dataFiles = insertDataFiles(table, 10, 10);
    insertEqDeleteFiles(table, 1);
    insertPosDeleteFiles(table, dataFiles);
    Set<String> oldDataFilesPath = new HashSet<>();
    Set<String> oldDeleteFilesPath = new HashSet<>();
    try (CloseableIterable<FileScanTask> filesIterable = table.newScan()
        .planFiles()) {
      filesIterable.forEach(fileScanTask -> {
        oldDataFilesPath.add((String) fileScanTask.file().path());
        fileScanTask.deletes().forEach(deleteFile -> oldDeleteFilesPath.add((String) deleteFile.path()));
      });
    }

    List<FileScanTask> fileScanTasks;
    try (CloseableIterable<FileScanTask> filesIterable = table.newScan()
        .planFiles()) {
      fileScanTasks = Lists.newArrayList(filesIterable);
    }

    long currentSnapshotId = table.currentSnapshot().snapshotId();
    IcebergFullOptimizePlan optimizePlan = new IcebergFullOptimizePlan(table,
        new TableOptimizeRuntime(table.id()),
        fileScanTasks, 1, System.currentTimeMillis(),
        currentSnapshotId);
    List<BasicOptimizeTask> tasks = optimizePlan.plan().getOptimizeTasks();

    List<DataFile> resultFiles = insertDataFiles(table, 10, 1);
    List<OptimizeTaskItem> taskItems = tasks.stream().map(task -> {
      OptimizeTaskRuntime optimizeRuntime = new OptimizeTaskRuntime(task.getTaskId());
      optimizeRuntime.setPreparedTime(System.currentTimeMillis());
      optimizeRuntime.setStatus(OptimizeStatus.Prepared);
      optimizeRuntime.setReportTime(System.currentTimeMillis());
      if (resultFiles != null) {
        optimizeRuntime.setNewFileSize(resultFiles.get(0).fileSizeInBytes());
        optimizeRuntime.setTargetFiles(resultFiles.stream().map(SerializationUtils::toByteBuffer).collect(Collectors.toList()));
      }
      List<ByteBuffer> finalTargetFiles = optimizeRuntime.getTargetFiles();
      optimizeRuntime.setTargetFiles(finalTargetFiles);
      optimizeRuntime.setNewFileCnt(finalTargetFiles.size());
      // 1min
      optimizeRuntime.setCostTime(60 * 1000);
      return new OptimizeTaskItem(task, optimizeRuntime);
    }).collect(Collectors.toList());
    Map<String, List<OptimizeTaskItem>> partitionTasks = taskItems.stream()
        .collect(Collectors.groupingBy(taskItem -> taskItem.getOptimizeTask().getPartition()));

    IcebergOptimizeCommit optimizeCommit = new IcebergOptimizeCommit(table, partitionTasks);
    optimizeCommit.commit(currentSnapshotId);

    Set<String> newDataFilesPath = new HashSet<>();
    Set<String> newDeleteFilesPath = new HashSet<>();
    try (CloseableIterable<FileScanTask> fileIterable = table.newScan()
        .planFiles()) {
      fileIterable.forEach(fileScanTask -> {
        newDataFilesPath.add((String) fileScanTask.file().path());
        fileScanTask.deletes().forEach(deleteFile -> newDeleteFilesPath.add((String) deleteFile.path()));
      });
    }

    Assert.assertNotEquals(oldDataFilesPath, newDataFilesPath);
    Assert.assertNotEquals(oldDeleteFilesPath, newDeleteFilesPath);
  }

  @Test
  public void testPartitionTableMajorOptimizeCommit() throws Exception {
    UnkeyedTable table = icebergPartitionTable.asUnkeyedTable();
    table.updateProperties()
        .set(TableProperties.SELF_OPTIMIZING_MAJOR_TRIGGER_DUPLICATE_RATIO, "0")
        .set(org.apache.iceberg.TableProperties.DEFAULT_WRITE_METRICS_MODE, "full")
        .commit();
    List<DataFile> dataFiles = insertDataFiles(table, 10, 10);
    insertEqDeleteFiles(table, 1);
    insertPosDeleteFiles(table, dataFiles);
    Set<String> oldDataFilesPath = new HashSet<>();
    Set<String> oldDeleteFilesPath = new HashSet<>();
    try (CloseableIterable<FileScanTask> filesIterable = table.newScan().planFiles()) {
      filesIterable.forEach(fileScanTask -> {
        oldDataFilesPath.add((String) fileScanTask.file().path());
        fileScanTask.deletes().forEach(deleteFile -> oldDeleteFilesPath.add((String) deleteFile.path()));
      });
    }

    List<FileScanTask> fileScanTasks;
    try (CloseableIterable<FileScanTask> filesIterable = table.newScan().planFiles()) {
      fileScanTasks = Lists.newArrayList(filesIterable);
    }

    IcebergFullOptimizePlan optimizePlan = new IcebergFullOptimizePlan(table,
        new TableOptimizeRuntime(table.id()),
        fileScanTasks, 1, System.currentTimeMillis(),
        table.currentSnapshot().snapshotId());
    List<BasicOptimizeTask> tasks = optimizePlan.plan().getOptimizeTasks();

    List<DataFile> resultFiles = insertDataFiles(table, 10, 1);
    List<OptimizeTaskItem> taskItems = tasks.stream().map(task -> {
      OptimizeTaskRuntime optimizeRuntime = new OptimizeTaskRuntime(task.getTaskId());
      optimizeRuntime.setPreparedTime(System.currentTimeMillis());
      optimizeRuntime.setStatus(OptimizeStatus.Prepared);
      optimizeRuntime.setReportTime(System.currentTimeMillis());
      if (resultFiles != null) {
        optimizeRuntime.setNewFileSize(resultFiles.get(0).fileSizeInBytes());
        optimizeRuntime.setTargetFiles(resultFiles.stream().map(SerializationUtils::toByteBuffer).collect(Collectors.toList()));
      }
      List<ByteBuffer> finalTargetFiles = optimizeRuntime.getTargetFiles();
      optimizeRuntime.setTargetFiles(finalTargetFiles);
      optimizeRuntime.setNewFileCnt(finalTargetFiles.size());
      // 1min
      optimizeRuntime.setCostTime(60 * 1000);
      return new OptimizeTaskItem(task, optimizeRuntime);
    }).collect(Collectors.toList());
    Map<String, List<OptimizeTaskItem>> partitionTasks = taskItems.stream()
        .collect(Collectors.groupingBy(taskItem -> taskItem.getOptimizeTask().getPartition()));

    IcebergOptimizeCommit optimizeCommit = new IcebergOptimizeCommit(table, partitionTasks);
    optimizeCommit.commit(table.currentSnapshot().snapshotId());

    Set<String> newDataFilesPath = new HashSet<>();
    Set<String> newDeleteFilesPath = new HashSet<>();
    try (CloseableIterable<FileScanTask> fileIterable = table.newScan().planFiles()) {
      fileIterable.forEach(fileScanTask -> {
        newDataFilesPath.add((String) fileScanTask.file().path());
        fileScanTask.deletes().forEach(deleteFile -> newDeleteFilesPath.add((String) deleteFile.path()));
      });
    }

    Assert.assertNotEquals(oldDataFilesPath, newDataFilesPath);
    Assert.assertNotEquals(oldDeleteFilesPath, newDeleteFilesPath);
  }
}
