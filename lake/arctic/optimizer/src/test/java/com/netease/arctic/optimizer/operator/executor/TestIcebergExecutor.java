package com.netease.arctic.optimizer.operator.executor;

import com.google.common.collect.Iterables;
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.data.file.DataFileWithSequence;
import com.netease.arctic.data.file.DeleteFileWithSequence;
import com.netease.arctic.optimizer.OptimizerConfig;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;

public class TestIcebergExecutor extends TestIcebergExecutorBase {

  @Test
  public void testCompactSmallDataFiles() throws Exception {

    // sequence 0
    DataFileWithSequence smallDataFile1 = new DataFileWithSequence(insertDataFiles(10, 0), 0);
    DataFileWithSequence smallDataFile2 = new DataFileWithSequence(insertDataFiles(8, 10), 1);

    // 2 small data files
    NodeTask nodeTask = constructNodeTask(
        Lists.newArrayList(),
        Lists.newArrayList(smallDataFile1, smallDataFile2),
        Lists.newArrayList(),
        Lists.newArrayList(),
        OptimizeType.Minor);

    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    IcebergExecutor executor = new IcebergExecutor(nodeTask, icebergTable,
        System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = executor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 1);
    ContentFile<?> resultFile = result.getTargetFiles().iterator().next();
    Assert.assertEquals(FileContent.DATA, resultFile.content());
    Assert.assertEquals(18, resultFile.recordCount());
  }

  @Test
  public void testCompactSmallDataFilesWithDeleteFiles() throws Exception {
    // sequence 0
    DeleteFileWithSequence equDeleteFile = new DeleteFileWithSequence(insertEqDeleteFiles(2, 6), 0);
    DataFileWithSequence smallDataFile1 = new DataFileWithSequence(insertDataFiles(10, 0), 0);

    // sequence 1
    DeleteFileWithSequence
        posDeleteFile = new DeleteFileWithSequence(insertPosDeleteFiles(smallDataFile1, 1, 5), 1);
    DataFileWithSequence smallDataFile2 = new DataFileWithSequence(insertDataFiles(8, 10), 1);

    // 2 small data files, 1 positional delete file, 1 equality delete file
    NodeTask nodeTask = constructNodeTask(
        Lists.newArrayList(),
        Lists.newArrayList(smallDataFile1, smallDataFile2),
        Lists.newArrayList(posDeleteFile),
        Lists.newArrayList(equDeleteFile),
        OptimizeType.Minor);

    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    IcebergExecutor executor = new IcebergExecutor(nodeTask, icebergTable,
        System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = executor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 1);
    ContentFile<?> resultFile = result.getTargetFiles().iterator().next();
    Assert.assertEquals(FileContent.DATA, resultFile.content());
    Assert.assertEquals(16, resultFile.recordCount());
  }

  @Test
  public void testCompactDeleteFiles() throws Exception {

    // sequence 0
    DeleteFileWithSequence equDeleteFile1 = new DeleteFileWithSequence(insertEqDeleteFiles(2, 6), 0);
    DataFileWithSequence dataFile1 = new DataFileWithSequence(insertDataFiles(10, 0), 0);
    DataFileWithSequence dataFile2 = new DataFileWithSequence(insertDataFiles(8, 10), 0);

    // sequence 1
    DeleteFileWithSequence
        posDeleteFile = new DeleteFileWithSequence(insertPosDeleteFiles(dataFile1, 1, 5), 1);
    DeleteFileWithSequence
        equDeleteFile2 = new DeleteFileWithSequence(insertEqDeleteFiles(10, 13, 20, 23), 1);

    // 2 data file, 2 quality delete file, 1 positional delete file
    NodeTask nodeTask = constructNodeTask(
        Lists.newArrayList(dataFile1, dataFile2),
        Lists.newArrayList(),
        Lists.newArrayList(equDeleteFile1, equDeleteFile2),
        Lists.newArrayList(posDeleteFile),
        OptimizeType.Minor);

    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    IcebergExecutor executor = new IcebergExecutor(nodeTask, icebergTable,
        System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = executor.execute();
    Assert.assertEquals(Iterables.size(result.getTargetFiles()), 2);
    result.getTargetFiles().forEach(file -> {
      Assert.assertEquals(FileContent.POSITION_DELETES, file.content());
      Assert.assertEquals(2, file.recordCount());
    });
  }

  @Test
  public void testCompactDataFiles() throws Exception {
    // sequence 0
    DataFileWithSequence dataFile1 = new DataFileWithSequence(insertDataFiles(10, 0), 0);

    // sequence 1
    DeleteFileWithSequence
        posDeleteFile = new DeleteFileWithSequence(insertPosDeleteFiles(dataFile1, 1, 5), 1);
    DeleteFileWithSequence equDeleteFile = new DeleteFileWithSequence(insertEqDeleteFiles(2, 6, 20, 23), 1);
    DataFileWithSequence dataFile2 = new DataFileWithSequence(insertDataFiles(8, 10), 1);

    // 2 small data files, 1 positional delete file, 1 equality delete file
    NodeTask nodeTask = constructNodeTask(
        Lists.newArrayList(dataFile1, dataFile2),
        Lists.newArrayList(),
        Lists.newArrayList(posDeleteFile),
        Lists.newArrayList(equDeleteFile),
        OptimizeType.FullMajor);

    String[] arg = new String[0];
    OptimizerConfig optimizerConfig = new OptimizerConfig(arg);
    optimizerConfig.setOptimizerId("UnitTest");
    IcebergExecutor icebergExecutor = new IcebergExecutor(nodeTask, icebergTable,
        System.currentTimeMillis(), optimizerConfig);
    OptimizeTaskResult result = icebergExecutor.execute();
    Assert.assertEquals(1, Iterables.size(result.getTargetFiles()));
    ContentFile<?> resultFile = result.getTargetFiles().iterator().next();
    Assert.assertEquals(FileContent.DATA, resultFile.content());
    Assert.assertEquals(14, resultFile.recordCount());
  }
}
