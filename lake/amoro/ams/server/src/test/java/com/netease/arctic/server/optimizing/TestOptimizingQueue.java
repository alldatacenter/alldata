package com.netease.arctic.server.optimizing;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.OptimizerRegisterInfo;
import com.netease.arctic.ams.api.OptimizingTask;
import com.netease.arctic.ams.api.OptimizingTaskId;
import com.netease.arctic.ams.api.OptimizingTaskResult;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.ams.api.resource.ResourceGroup;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.optimizing.RewriteFilesOutput;
import com.netease.arctic.optimizing.TableOptimizing;
import com.netease.arctic.server.ArcticServiceConstants;
import com.netease.arctic.server.persistence.PersistentBase;
import com.netease.arctic.server.persistence.mapper.TableMetaMapper;
import com.netease.arctic.server.resource.OptimizerInstance;
import com.netease.arctic.server.table.AMSTableTestBase;
import com.netease.arctic.server.table.TableConfiguration;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.server.table.TableRuntimeMeta;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.SerializationUtil;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RunWith(Parameterized.class)
public class TestOptimizingQueue extends AMSTableTestBase {

  private final Persistency persistency = new Persistency();

  public TestOptimizingQueue(CatalogTestHelper catalogTestHelper,
                             TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper, true);
  }

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][]{
        {new BasicCatalogTestHelper(TableFormat.ICEBERG),
            new BasicTableTestHelper(false, true)}};
  }

  @Test
  public void testPollNoTask() {
    TableRuntimeMeta tableRuntimeMeta = buildTableRuntimeMeta(OptimizingStatus.PENDING, defaultResourceGroup());
    OptimizingQueue queue = new OptimizingQueue(tableService(), defaultResourceGroup(),
        Collections.singletonList(tableRuntimeMeta), Collections.emptyList(), 60000, 3000);
    OptimizingTask optimizingTask = queue.pollTask("", 1);
    Assert.assertNull(optimizingTask);
  }

  @Test
  public void testRefreshTable() {
    OptimizingQueue queue = new OptimizingQueue(tableService(), defaultResourceGroup(),
        Collections.emptyList(), Collections.emptyList(), 60000, 3000);
    Assert.assertEquals(0, queue.getSchedulingPolicy().getTableRuntimeMap().size());
    TableRuntimeMeta tableRuntimeMeta = buildTableRuntimeMeta(OptimizingStatus.IDLE, defaultResourceGroup());
    queue.refreshTable(tableRuntimeMeta.getTableRuntime());
    Assert.assertEquals(1, queue.getSchedulingPolicy().getTableRuntimeMap().size());
    Assert.assertTrue(queue.getSchedulingPolicy().getTableRuntimeMap().containsKey(serverTableIdentifier()));

    queue.releaseTable(tableRuntimeMeta.getTableRuntime());
    Assert.assertEquals(0, queue.getSchedulingPolicy().getTableRuntimeMap().size());
  }

  @Test
  public void testHandleTask() {
    TableRuntimeMeta tableRuntimeMeta = initTableWithFiles();

    OptimizingQueue queue = new OptimizingQueue(tableService(), defaultResourceGroup(),
        Collections.singletonList(tableRuntimeMeta), Collections.emptyList(), 60000, 3000);

    String authToken = queue.authenticate(buildRegisterInfo());
    OptimizingQueue.OptimizingThread thread = new OptimizingQueue.OptimizingThread(authToken, 1);
    // 1.poll task
    OptimizingTask task = pollTaskAndCheck(authToken, thread, queue);

    // 2.ack task
    ackTaskAndCheck(authToken, thread, queue, task);

    // 3.fail task
    failTaskAndCheck(authToken, thread, queue, task);

    // 4.retry poll task
    thread = new OptimizingQueue.OptimizingThread(authToken, 2);
    task = pollTaskAndCheck(authToken, thread, queue);

    // 5.ackTask
    ackTaskAndCheck(authToken, thread, queue, task);

    // 6.succeed task
    succeedTaskAndCheck(authToken, thread, queue, task);

    // 7.commit
    OptimizingProcess optimizingProcess = tableRuntimeMeta.getTableRuntime().getOptimizingProcess();
    Assert.assertEquals(OptimizingProcess.Status.RUNNING, optimizingProcess.getStatus());
    optimizingProcess.commit();
    Assert.assertEquals(OptimizingProcess.Status.SUCCESS, optimizingProcess.getStatus());
    Assert.assertNull(tableRuntimeMeta.getTableRuntime().getOptimizingProcess());

    // 8.commit again
    optimizingProcess.commit();
    Assert.assertEquals(OptimizingProcess.Status.FAILED, optimizingProcess.getStatus());

    // 9.close
    optimizingProcess.close();
    Assert.assertEquals(OptimizingProcess.Status.CLOSED, optimizingProcess.getStatus());

  }

  @Test
  public void testPollTwice() {
    TableRuntimeMeta tableRuntimeMeta = initTableWithFiles();

    OptimizingQueue queue = new OptimizingQueue(tableService(), defaultResourceGroup(),
        Collections.singletonList(tableRuntimeMeta), Collections.emptyList(), 60000, 3000);

    String authToken = queue.authenticate(buildRegisterInfo());
    OptimizingQueue.OptimizingThread thread = new OptimizingQueue.OptimizingThread(authToken, 1);
    // 1.poll task
    pollTaskAndCheck(authToken, thread, queue);

    // 2.poll twice
    Assert.assertNull(queue.pollTask(thread.getToken(), thread.getThreadId()));
  }

  @Test
  public void testCheckSuspendTask() {
    TableRuntimeMeta tableRuntimeMeta = initTableWithFiles();

    OptimizingQueue queue = new OptimizingQueue(tableService(), defaultResourceGroup(),
        Collections.singletonList(tableRuntimeMeta), Collections.emptyList(), 60000, 3000);

    String authToken = queue.authenticate(buildRegisterInfo());
    OptimizingQueue.OptimizingThread thread = new OptimizingQueue.OptimizingThread(authToken, 1);
    // 1.poll task
    OptimizingTask task1 = pollTaskAndCheck(authToken, thread, queue);
    queue.getOptimizers().forEach(optimizerInstance -> optimizerInstance.setTouchTime(
        System.currentTimeMillis() - 60000 - 1));

    // 2.check suspending and retry task
    queue.checkSuspending();

    // 3.poll again
    Assert.assertEquals(0, queue.getExecutingTaskMap().size());
    OptimizingTask task2 = pollTaskAndCheck(authToken, thread, queue);
    Assert.assertEquals(task1, task2);

  }

  @Test
  public void testReloadScheduledTask() {
    TableRuntimeMeta tableRuntimeMeta = initTableWithFiles();

    OptimizingQueue queue = new OptimizingQueue(tableService(), defaultResourceGroup(),
        Collections.singletonList(tableRuntimeMeta), Collections.emptyList(), 60000, 3000);

    String authToken = queue.authenticate(buildRegisterInfo());
    OptimizingQueue.OptimizingThread thread = new OptimizingQueue.OptimizingThread(authToken, 1);
    OptimizingTask task;
    // 1.poll task
    task = pollTaskAndCheck(authToken, thread, queue);

    // 2.reload from sysdb
    List<TableRuntimeMeta> tableRuntimeMetas = persistency.selectTableRuntimeMetas();
    Assert.assertEquals(1, tableRuntimeMetas.size());
    tableRuntimeMetas.get(0).constructTableRuntime(tableService());
    queue = new OptimizingQueue(tableService(), defaultResourceGroup(), tableRuntimeMetas, Collections.emptyList(),
        60000, 3000);

    Assert.assertEquals(1, queue.getExecutingTaskMap().size());
    TaskRuntime taskRuntime = queue.getExecutingTaskMap().get(task.getTaskId());
    assertTaskRuntime(taskRuntime, TaskRuntime.Status.SCHEDULED, thread);

    // 3.ack task
    ackTaskAndCheck(authToken, thread, queue, task);

    // 4.succeed task
    succeedTaskAndCheck(authToken, thread, queue, task);
  }

  @Test
  public void testReloadAckTask() {
    TableRuntimeMeta tableRuntimeMeta = initTableWithFiles();

    OptimizingQueue queue = new OptimizingQueue(tableService(), defaultResourceGroup(),
        Collections.singletonList(tableRuntimeMeta), Collections.emptyList(), 60000, 3000);

    String authToken = queue.authenticate(buildRegisterInfo());
    OptimizingQueue.OptimizingThread thread = new OptimizingQueue.OptimizingThread(authToken, 1);
    OptimizingTask task;
    // 1.poll task
    task = pollTaskAndCheck(authToken, thread, queue);

    // 2.ack task
    ackTaskAndCheck(authToken, thread, queue, task);

    // 3.reload from sysdb
    List<TableRuntimeMeta> tableRuntimeMetas = persistency.selectTableRuntimeMetas();
    Assert.assertEquals(1, tableRuntimeMetas.size());
    tableRuntimeMetas.get(0).constructTableRuntime(tableService());
    queue = new OptimizingQueue(tableService(), defaultResourceGroup(), tableRuntimeMetas, Collections.emptyList(),
        60000, 3000);

    Assert.assertEquals(1, queue.getExecutingTaskMap().size());
    TaskRuntime taskRuntime = queue.getExecutingTaskMap().get(task.getTaskId());
    assertTaskRuntime(taskRuntime, TaskRuntime.Status.ACKED, thread);

    // 4.succeed task
    succeedTaskAndCheck(authToken, thread, queue, task);
  }

  @Test
  public void testReloadCompleteTask() {
    TableRuntimeMeta tableRuntimeMeta = initTableWithFiles();

    OptimizingQueue queue = new OptimizingQueue(tableService(), defaultResourceGroup(),
        Collections.singletonList(tableRuntimeMeta), Collections.emptyList(), 60000, 3000);

    String authToken = queue.authenticate(buildRegisterInfo());
    OptimizingQueue.OptimizingThread thread = new OptimizingQueue.OptimizingThread(authToken, 1);
    OptimizingTask task;
    // 1.poll task
    task = pollTaskAndCheck(authToken, thread, queue);

    // 2.ack task
    ackTaskAndCheck(authToken, thread, queue, task);

    // 3.succeed task
    succeedTaskAndCheck(authToken, thread, queue, task);

    // 4.reload from sysdb
    List<TableRuntimeMeta> tableRuntimeMetas = persistency.selectTableRuntimeMetas();
    Assert.assertEquals(1, tableRuntimeMetas.size());
    tableRuntimeMetas.get(0).constructTableRuntime(tableService());
    queue = new OptimizingQueue(tableService(), defaultResourceGroup(), tableRuntimeMetas, Collections.emptyList(),
        60000, 3000);

    Assert.assertEquals(0, queue.getExecutingTaskMap().size());
  }

  @Test
  public void testReloadFailTask() {
    TableRuntimeMeta tableRuntimeMeta = initTableWithFiles();

    OptimizingQueue queue = new OptimizingQueue(tableService(), defaultResourceGroup(),
        Collections.singletonList(tableRuntimeMeta), Collections.emptyList(), 60000, 3000);

    String authToken = queue.authenticate(buildRegisterInfo());
    OptimizingQueue.OptimizingThread thread = new OptimizingQueue.OptimizingThread(authToken, 1);
    OptimizingTask task;
    // 1.poll task
    task = pollTaskAndCheck(authToken, thread, queue);

    // 2.ack task
    ackTaskAndCheck(authToken, thread, queue, task);

    // 3.fail task
    failTaskAndCheck(authToken, thread, queue, task);

    // 4.reload from sysdb
    List<TableRuntimeMeta> tableRuntimeMetas = persistency.selectTableRuntimeMetas();
    Assert.assertEquals(1, tableRuntimeMetas.size());
    tableRuntimeMetas.get(0).constructTableRuntime(tableService());
    queue = new OptimizingQueue(tableService(), defaultResourceGroup(), tableRuntimeMetas, Collections.emptyList(),
        60000, 3000);

    Assert.assertEquals(0, queue.getExecutingTaskMap().size());

    // 5.retry poll task
    thread = new OptimizingQueue.OptimizingThread(authToken, 2);
    task = pollTaskAndCheck(authToken, thread, queue);

    // 6.ackTask
    ackTaskAndCheck(authToken, thread, queue, task);

    // 7.succeed task
    succeedTaskAndCheck(authToken, thread, queue, task);
  }

  private TableRuntimeMeta initTableWithFiles() {
    ArcticTable arcticTable = tableService().loadTable(serverTableIdentifier());
    appendData(arcticTable.asUnkeyedTable(), 1);
    appendData(arcticTable.asUnkeyedTable(), 2);
    TableRuntimeMeta tableRuntimeMeta = buildTableRuntimeMeta(OptimizingStatus.PENDING, defaultResourceGroup());
    TableRuntime runtime = tableRuntimeMeta.getTableRuntime();

    runtime.refresh(tableService().loadTable(serverTableIdentifier()));
    return tableRuntimeMeta;
  }

  private void succeedTaskAndCheck(String authToken, OptimizingQueue.OptimizingThread thread, OptimizingQueue queue,
                                   OptimizingTask task) {
    TaskRuntime taskRuntime = queue.getExecutingTaskMap().get(task.getTaskId());
    queue.completeTask(authToken, buildOptimizingTaskResult(task.getTaskId(), thread.getThreadId()));
    Assert.assertEquals(0, queue.getExecutingTaskMap().size());
    assertTaskRuntime(taskRuntime, TaskRuntime.Status.SUCCESS, null);
  }

  private void failTaskAndCheck(String authToken, OptimizingQueue.OptimizingThread thread, OptimizingQueue queue,
                                OptimizingTask task) {
    TaskRuntime taskRuntime = queue.getExecutingTaskMap().get(task.getTaskId());
    String errorMessage = "unknown error";
    queue.completeTask(authToken, buildOptimizingTaskFailResult(task.getTaskId(), thread.getThreadId(), errorMessage));
    Assert.assertEquals(0, queue.getExecutingTaskMap().size());
    assertTaskRuntime(taskRuntime, TaskRuntime.Status.PLANNED, null); // retry and change to PLANNED
    Assert.assertEquals(1, taskRuntime.getRetry());
    Assert.assertEquals(errorMessage, taskRuntime.getFailReason());
  }

  private void ackTaskAndCheck(String authToken,
                               OptimizingQueue.OptimizingThread thread,
                               OptimizingQueue queue,
                               OptimizingTask task) {
    queue.ackTask(authToken, thread.getThreadId(), task.getTaskId());
    Assert.assertEquals(1, queue.getExecutingTaskMap().size());
    TaskRuntime taskRuntime = queue.getExecutingTaskMap().get(task.getTaskId());
    assertTaskRuntime(taskRuntime, TaskRuntime.Status.ACKED, thread);
  }

  private OptimizingTask pollTaskAndCheck(String authToken, OptimizingQueue.OptimizingThread thread,
                                          OptimizingQueue queue) {
    OptimizingTask task = queue.pollTask(authToken, thread.getThreadId());
    Assert.assertNotNull(task);
    Assert.assertEquals(1, queue.getExecutingTaskMap().size());
    TaskRuntime taskRuntime = queue.getExecutingTaskMap().get(task.getTaskId());
    assertTaskRuntime(taskRuntime, TaskRuntime.Status.SCHEDULED, thread);
    return task;
  }

  @Test
  public void testOptimizer() throws InterruptedException {
    OptimizingQueue queue = new OptimizingQueue(tableService(), defaultResourceGroup(),
        Collections.emptyList(), Collections.emptyList(), 60000, 3000);
    OptimizerRegisterInfo registerInfo = buildRegisterInfo();

    // authenticate
    String authToken = queue.authenticate(registerInfo);
    List<OptimizerInstance> optimizers = queue.getOptimizers();
    Assert.assertEquals(1, optimizers.size());
    OptimizerInstance optimizerInstance = optimizers.get(0);
    Assert.assertEquals(authToken, optimizerInstance.getToken());

    // touch
    long oldTouchTime = optimizerInstance.getTouchTime();
    Thread.sleep(1);
    queue.touch(authToken);
    Assert.assertTrue(optimizerInstance.getTouchTime() > oldTouchTime);

    // remove
    queue.removeOptimizer(registerInfo.getResourceId());
    Assert.assertEquals(0, queue.getOptimizers().size());
  }

  private OptimizerRegisterInfo buildRegisterInfo() {
    OptimizerRegisterInfo registerInfo = new OptimizerRegisterInfo();
    registerInfo.setThreadCount(1);
    registerInfo.setMemoryMb(1024);
    registerInfo.setGroupName(defaultResourceGroup().getName());
    registerInfo.setResourceId("1");
    registerInfo.setStartTime(System.currentTimeMillis());
    return registerInfo;
  }

  private void assertTaskRuntime(TaskRuntime taskRuntime, TaskRuntime.Status status,
                                 OptimizingQueue.OptimizingThread thread) {
    Assert.assertEquals(status, taskRuntime.getStatus());
    Assert.assertEquals(thread, taskRuntime.getOptimizingThread());
  }

  private ResourceGroup defaultResourceGroup() {
    return new ResourceGroup.Builder("test", "local").build();
  }

  private TableRuntimeMeta buildTableRuntimeMeta(OptimizingStatus status, ResourceGroup resourceGroup) {
    ArcticTable arcticTable = tableService().loadTable(serverTableIdentifier());
    TableRuntimeMeta tableRuntimeMeta = new TableRuntimeMeta();
    tableRuntimeMeta.setCatalogName(serverTableIdentifier().getCatalog());
    tableRuntimeMeta.setDbName(serverTableIdentifier().getDatabase());
    tableRuntimeMeta.setTableName(serverTableIdentifier().getTableName());
    tableRuntimeMeta.setTableId(serverTableIdentifier().getId());
    tableRuntimeMeta.setTableStatus(status);
    tableRuntimeMeta.setTableConfig(TableConfiguration.parseConfig(arcticTable.properties()));
    tableRuntimeMeta.setCurrentChangeSnapshotId(ArcticServiceConstants.INVALID_SNAPSHOT_ID);
    tableRuntimeMeta.setCurrentSnapshotId(ArcticServiceConstants.INVALID_SNAPSHOT_ID);
    tableRuntimeMeta.setLastOptimizedChangeSnapshotId(ArcticServiceConstants.INVALID_SNAPSHOT_ID);
    tableRuntimeMeta.setLastOptimizedSnapshotId(ArcticServiceConstants.INVALID_SNAPSHOT_ID);
    tableRuntimeMeta.setOptimizerGroup(resourceGroup.getName());
    tableRuntimeMeta.constructTableRuntime(tableService());
    return tableRuntimeMeta;
  }

  private List<DataFile> appendData(UnkeyedTable table, int id) {
    ArrayList<Record> newRecords = Lists.newArrayList(
        DataTestHelpers.createRecord(table.schema(), id, "111", 0L, "2022-01-01T12:00:00"));
    List<DataFile> dataFiles = DataTestHelpers.writeBaseStore(table, 0L, newRecords, false);
    AppendFiles appendFiles = table.newAppend();
    dataFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
    return dataFiles;
  }

  private OptimizingTaskResult buildOptimizingTaskResult(OptimizingTaskId taskId, int threadId) {
    TableOptimizing.OptimizingOutput output = new RewriteFilesOutput(null, null, null);
    OptimizingTaskResult optimizingTaskResult = new OptimizingTaskResult(taskId, threadId);
    optimizingTaskResult.setTaskOutput(SerializationUtil.simpleSerialize(output));
    return optimizingTaskResult;
  }

  private OptimizingTaskResult buildOptimizingTaskFailResult(OptimizingTaskId taskId, int threadId,
                                                             String errorMessage) {
    TableOptimizing.OptimizingOutput output = new RewriteFilesOutput(null, null, null);
    OptimizingTaskResult optimizingTaskResult = new OptimizingTaskResult(taskId, threadId);
    optimizingTaskResult.setTaskOutput(SerializationUtil.simpleSerialize(output));
    optimizingTaskResult.setErrorMessage(errorMessage);
    return optimizingTaskResult;
  }

  private static class Persistency extends PersistentBase {
    public List<TableRuntimeMeta> selectTableRuntimeMetas() {
      return getAs(TableMetaMapper.class, TableMetaMapper::selectTableRuntimeMetas);
    }
  }
}
