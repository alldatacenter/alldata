package com.netease.arctic.server.optimizing;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.OptimizerRegisterInfo;
import com.netease.arctic.ams.api.OptimizingService;
import com.netease.arctic.ams.api.OptimizingTask;
import com.netease.arctic.ams.api.OptimizingTaskId;
import com.netease.arctic.ams.api.OptimizingTaskResult;
import com.netease.arctic.ams.api.resource.ResourceGroup;
import com.netease.arctic.optimizing.RewriteFilesInput;
import com.netease.arctic.server.ArcticServiceConstants;
import com.netease.arctic.server.exception.OptimizingClosedException;
import com.netease.arctic.server.exception.PluginRetryAuthException;
import com.netease.arctic.server.exception.TaskNotFoundException;
import com.netease.arctic.server.optimizing.plan.OptimizingPlanner;
import com.netease.arctic.server.optimizing.plan.TaskDescriptor;
import com.netease.arctic.server.persistence.PersistentBase;
import com.netease.arctic.server.persistence.TaskFilesPersistence;
import com.netease.arctic.server.persistence.mapper.OptimizerMapper;
import com.netease.arctic.server.persistence.mapper.OptimizingMapper;
import com.netease.arctic.server.resource.OptimizerInstance;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.TableManager;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.server.table.TableRuntimeMeta;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.utils.ArcticDataFiles;
import com.netease.arctic.utils.ExceptionUtil;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.util.StructLikeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class OptimizingQueue extends PersistentBase implements OptimizingService.Iface {

  private static final Logger LOG = LoggerFactory.getLogger(OptimizingQueue.class);
  private final long optimizerTouchTimeout;
  private final long taskAckTimeout;
  private final Lock planLock = new ReentrantLock();
  private ResourceGroup optimizerGroup;
  private final Queue<TaskRuntime> taskQueue = new LinkedTransferQueue<>();
  private final Queue<TaskRuntime> retryQueue = new LinkedTransferQueue<>();
  private final SchedulingPolicy schedulingPolicy;
  // keeps the SCHEDULED and ACKED tasks
  private final Map<OptimizingTaskId, TaskRuntime> executingTaskMap = new ConcurrentHashMap<>();
  private final Map<String, OptimizerInstance> authOptimizers = new ConcurrentHashMap<>();

  private final TableManager tableManager;

  public OptimizingQueue(
      TableManager tableManager,
      ResourceGroup optimizerGroup,
      List<TableRuntimeMeta> tableRuntimeMetaList,
      List<OptimizerInstance> authOptimizers,
      long optimizerTouchTimeout,
      long taskAckTimeout) {
    Preconditions.checkNotNull(optimizerGroup, "optimizerGroup can not be null");
    this.optimizerTouchTimeout = optimizerTouchTimeout;
    this.taskAckTimeout = taskAckTimeout;
    this.optimizerGroup = optimizerGroup;
    this.schedulingPolicy = new SchedulingPolicy(optimizerGroup);
    this.tableManager = tableManager;
    this.authOptimizers.putAll(authOptimizers.stream().collect(Collectors.toMap(
        OptimizerInstance::getToken, optimizer -> optimizer)));
    tableRuntimeMetaList.forEach(this::initTableRuntime);
  }

  private void initTableRuntime(TableRuntimeMeta tableRuntimeMeta) {
    TableRuntime tableRuntime = tableRuntimeMeta.getTableRuntime();
    if (tableRuntime.getOptimizingStatus().isProcessing() &&
        tableRuntimeMeta.getOptimizingProcessId() != 0) {
      tableRuntime.recover(new TableOptimizingProcess(tableRuntimeMeta));
    }

    if (tableRuntime.isOptimizingEnabled()) {
      //TODO: load task quotas
      tableRuntime.resetTaskQuotas(System.currentTimeMillis() - ArcticServiceConstants.QUOTA_LOOK_BACK_TIME);
      if (tableRuntime.getOptimizingStatus() == OptimizingStatus.IDLE ||
          tableRuntime.getOptimizingStatus() == OptimizingStatus.PENDING) {
        schedulingPolicy.addTable(tableRuntime);
      } else if (tableRuntime.getOptimizingStatus() != OptimizingStatus.COMMITTING) {
        TableOptimizingProcess process = new TableOptimizingProcess(tableRuntimeMeta);
        process.getTaskMap().entrySet().stream().filter(
                entry -> entry.getValue().getStatus() == TaskRuntime.Status.SCHEDULED ||
                    entry.getValue().getStatus() == TaskRuntime.Status.ACKED)
            .forEach(entry -> executingTaskMap.put(entry.getKey(), entry.getValue()));
        process.getTaskMap().values().stream()
            .filter(task -> task.getStatus() == TaskRuntime.Status.PLANNED)
            .forEach(taskQueue::offer);
      }
    } else {
      OptimizingProcess process = tableRuntime.getOptimizingProcess();
      if (process != null) {
        process.close();
      }
    }
  }

  public void refreshTable(TableRuntime tableRuntime) {
    if (tableRuntime.isOptimizingEnabled() && !tableRuntime.getOptimizingStatus().isProcessing()) {
      LOG.info("Bind queue {} success with table {}", optimizerGroup.getName(), tableRuntime.getTableIdentifier());
      tableRuntime.resetTaskQuotas(System.currentTimeMillis() - ArcticServiceConstants.QUOTA_LOOK_BACK_TIME);
      schedulingPolicy.addTable(tableRuntime);
    }
  }

  public void releaseTable(TableRuntime tableRuntime) {
    schedulingPolicy.removeTable(tableRuntime);
    LOG.info("Release queue {} with table {}", optimizerGroup.getName(), tableRuntime.getTableIdentifier());
  }

  public boolean containsTable(ServerTableIdentifier identifier) {
    return this.schedulingPolicy.containsTable(identifier);
  }

  public List<OptimizerInstance> getOptimizers() {
    return ImmutableList.copyOf(authOptimizers.values());
  }

  public void removeOptimizer(String resourceId) {
    authOptimizers.entrySet().removeIf(op -> op.getValue().getResourceId().equals(resourceId));
  }

  private void clearTasks(TableOptimizingProcess optimizingProcess) {
    retryQueue.removeIf(taskRuntime -> taskRuntime.getProcessId() == optimizingProcess.getProcessId());
    taskQueue.removeIf(taskRuntime -> taskRuntime.getProcessId() == optimizingProcess.getProcessId());
  }

  @Override
  public void ping() {
  }

  @Override
  public void touch(String authToken) {
    OptimizerInstance optimizer = getAuthenticatedOptimizer(authToken).touch();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Optimizer {} touch time: {}", optimizer.getToken(), optimizer.getTouchTime());
    }
    doAs(OptimizerMapper.class, mapper -> mapper.updateTouchTime(optimizer.getToken()));
  }

  private OptimizerInstance getAuthenticatedOptimizer(String authToken) {
    Preconditions.checkArgument(authToken != null, "authToken can not be null");
    return Optional.ofNullable(authOptimizers.get(authToken))
        .orElseThrow(() -> new PluginRetryAuthException("Optimizer has not been authenticated"));
  }

  @Override
  public OptimizingTask pollTask(String authToken, int threadId) {
    TaskRuntime task = Optional.ofNullable(retryQueue.poll())
        .orElseGet(this::pollOrPlan);

    if (task != null) {
      safelySchedule(task, new OptimizingThread(authToken, threadId));
      executingTaskMap.putIfAbsent(task.getTaskId(), task);
    }
    return task != null ? task.getOptimizingTask() : null;
  }

  private void safelySchedule(TaskRuntime task, OptimizingThread thread) {
    try {
      task.schedule(thread);
    } catch (Throwable throwable) {
      retryTask(task, false);
      throw throwable;
    }
  }

  private void retryTask(TaskRuntime taskRuntime, boolean incRetryCount) {
    taskRuntime.reset(incRetryCount);
    retryQueue.offer(taskRuntime);
  }

  @Override
  public void ackTask(String authToken, int threadId, OptimizingTaskId taskId) {
    Optional.ofNullable(executingTaskMap.get(taskId))
        .orElseThrow(() -> new TaskNotFoundException(taskId))
        .ack(new OptimizingThread(authToken, threadId));
  }

  @Override
  public void completeTask(String authToken, OptimizingTaskResult taskResult) {
    OptimizingThread thread = new OptimizingThread(authToken, taskResult.getThreadId());
    Optional.ofNullable(executingTaskMap.get(taskResult.getTaskId()))
        .orElseThrow(() -> new TaskNotFoundException(taskResult.getTaskId()))
        .complete(thread, taskResult);
    executingTaskMap.remove(taskResult.getTaskId());
  }

  @Override
  public String authenticate(OptimizerRegisterInfo registerInfo) {
    OptimizerInstance optimizer = new OptimizerInstance(registerInfo, optimizerGroup.getContainer());
    if (LOG.isDebugEnabled()) {
      LOG.debug("Register optimizer: " + optimizer);
    }
    doAs(OptimizerMapper.class, mapper -> mapper.insertOptimizer(optimizer));
    authOptimizers.put(optimizer.getToken(), optimizer);
    return optimizer.getToken();
  }

  public List<String> checkSuspending() {
    long currentTime = System.currentTimeMillis();
    List<String> expiredOptimizers = authOptimizers.values().stream()
        .filter(optimizer -> currentTime - optimizer.getTouchTime() > optimizerTouchTimeout)
        .map(OptimizerInstance::getToken)
        .collect(Collectors.toList());

    expiredOptimizers.forEach(authOptimizers.keySet()::remove);

    List<TaskRuntime> suspendingTasks = executingTaskMap.values().stream()
        .filter(task -> task.isSuspending(currentTime, taskAckTimeout) ||
            expiredOptimizers.contains(task.getOptimizingThread().getToken()) ||
            !authOptimizers.containsKey(task.getOptimizingThread().getToken()))
        .collect(Collectors.toList());
    suspendingTasks.forEach(task -> {
      executingTaskMap.remove(task.getTaskId());
      //optimizing task of suspending optimizer would not be counted for retrying
      retryTask(task, false);
    });
    return expiredOptimizers;
  }

  public void updateOptimizerGroup(ResourceGroup optimizerGroup) {
    Preconditions.checkArgument(
        this.optimizerGroup.getName().equals(optimizerGroup.getName()),
        "optimizer group name mismatch");
    this.optimizerGroup = optimizerGroup;
  }

  @VisibleForTesting
  Map<OptimizingTaskId, TaskRuntime> getExecutingTaskMap() {
    return executingTaskMap;
  }

  private TaskRuntime pollOrPlan() {
    planLock.lock();
    try {
      if (taskQueue.isEmpty()) {
        planTasks();
      }
      return taskQueue.poll();
    } finally {
      planLock.unlock();
    }
  }

  private void planTasks() {
    List<TableRuntime> scheduledTables = schedulingPolicy.scheduleTables();
    if (LOG.isDebugEnabled()) {
      LOG.debug("Calculating and sorting tables by quota:" + scheduledTables);
    }

    for (TableRuntime tableRuntime : scheduledTables) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Planning table " + tableRuntime.getTableIdentifier());
      }
      try {
        ArcticTable table = tableManager.loadTable(tableRuntime.getTableIdentifier());
        OptimizingPlanner planner = new OptimizingPlanner(tableRuntime.refresh(table), table,
            getAvailableCore(tableRuntime));
        if (tableRuntime.isBlocked(BlockableOperation.OPTIMIZE)) {
          LOG.info("{} optimize is blocked, continue", tableRuntime.getTableIdentifier());
          continue;
        }
        if (planner.isNecessary()) {
          TableOptimizingProcess optimizingProcess = new TableOptimizingProcess(planner);
          LOG.info("{} after plan get {} tasks", tableRuntime.getTableIdentifier(),
              optimizingProcess.getTaskMap().size());
          optimizingProcess.taskMap.values().forEach(taskQueue::offer);
          break;
        } else {
          tableRuntime.cleanPendingInput();
        }
      } catch (Throwable e) {
        LOG.error(tableRuntime.getTableIdentifier() + " plan failed, continue", e);
      }
    }
  }

  private double getAvailableCore(TableRuntime tableRuntime) {
    return tableRuntime.getOptimizingConfig().getTargetQuota();
  }

  @VisibleForTesting
  SchedulingPolicy getSchedulingPolicy() {
    return schedulingPolicy;
  }

  private class TableOptimizingProcess implements OptimizingProcess, TaskRuntime.TaskOwner {
    private final long processId;
    private final OptimizingType optimizingType;
    private final TableRuntime tableRuntime;
    private final long planTime;
    private final long targetSnapshotId;
    private final long targetChangeSnapshotId;
    private final Map<OptimizingTaskId, TaskRuntime> taskMap = Maps.newHashMap();
    private final Lock lock = new ReentrantLock();
    private volatile Status status = OptimizingProcess.Status.RUNNING;
    private volatile String failedReason;
    private long endTime = ArcticServiceConstants.INVALID_TIME;

    private Map<String, Long> fromSequence = Maps.newHashMap();
    private Map<String, Long> toSequence = Maps.newHashMap();

    private boolean hasCommitted = false;

    public TableOptimizingProcess(OptimizingPlanner planner) {
      processId = planner.getProcessId();
      tableRuntime = planner.getTableRuntime();
      optimizingType = planner.getOptimizingType();
      planTime = planner.getPlanTime();
      targetSnapshotId = planner.getTargetSnapshotId();
      targetChangeSnapshotId = planner.getTargetChangeSnapshotId();
      loadTaskRuntimes(planner.planTasks());
      fromSequence = planner.getFromSequence();
      toSequence = planner.getToSequence();
      beginAndPersistProcess();
    }

    public TableOptimizingProcess(TableRuntimeMeta tableRuntimeMeta) {
      processId = tableRuntimeMeta.getOptimizingProcessId();
      tableRuntime = tableRuntimeMeta.getTableRuntime();
      optimizingType = tableRuntimeMeta.getOptimizingType();
      targetSnapshotId = tableRuntimeMeta.getTargetSnapshotId();
      targetChangeSnapshotId = tableRuntimeMeta.getTargetSnapshotId();
      planTime = tableRuntimeMeta.getPlanTime();
      if (tableRuntimeMeta.getFromSequence() != null) {
        fromSequence = tableRuntimeMeta.getFromSequence();
      }
      if (tableRuntimeMeta.getToSequence() != null) {
        toSequence = tableRuntimeMeta.getToSequence();
      }
      loadTaskRuntimes();
      tableRuntimeMeta.getTableRuntime().recover(this);
    }

    @Override
    public long getProcessId() {
      return processId;
    }

    @Override
    public OptimizingType getOptimizingType() {
      return optimizingType;
    }

    @Override
    public Status getStatus() {
      return status;
    }

    @Override
    public void close() {
      lock.lock();
      try {
        clearTasks(this);
        this.status = OptimizingProcess.Status.CLOSED;
        this.endTime = System.currentTimeMillis();
        persistProcessCompleted(false);
      } finally {
        lock.unlock();
      }
    }

    @Override
    public void acceptResult(TaskRuntime taskRuntime) {
      lock.lock();
      try {
        try {
          tableRuntime.addTaskQuota(taskRuntime.getCurrentQuota());
        } catch (Throwable t) {
          LOG.warn("{} failed to add task quota {}, ignore it", tableRuntime.getTableIdentifier(),
              taskRuntime.getTaskId(), t);
        }
        if (isClosed()) {
          throw new OptimizingClosedException(processId);
        }
        if (taskRuntime.getStatus() == TaskRuntime.Status.SUCCESS) {
          // the lock of TableOptimizingProcess makes it thread-safe
          if (allTasksPrepared() && tableRuntime.getOptimizingStatus().isProcessing() &&
              tableRuntime.getOptimizingStatus() != OptimizingStatus.COMMITTING) {
            tableRuntime.beginCommitting();
          }
        } else if (taskRuntime.getStatus() == TaskRuntime.Status.FAILED) {
          if (taskRuntime.getRetry() <= tableRuntime.getMaxExecuteRetryCount()) {
            retryTask(taskRuntime, true);
          } else {
            clearTasks(this);
            this.failedReason = taskRuntime.getFailReason();
            this.status = OptimizingProcess.Status.FAILED;
            this.endTime = taskRuntime.getEndTime();
            persistProcessCompleted(false);
          }
        }
      } catch (Exception e) {
        LOG.error("accept result error:", e);
      } finally {
        lock.unlock();
      }
    }

    @Override
    public boolean isClosed() {
      return status == OptimizingProcess.Status.CLOSED;
    }

    @Override
    public long getPlanTime() {
      return planTime;
    }

    @Override
    public long getDuration() {
      long dur = endTime == ArcticServiceConstants.INVALID_TIME ?
          System.currentTimeMillis() - planTime :
          endTime - planTime;
      return Math.max(0, dur);
    }

    @Override
    public long getTargetSnapshotId() {
      return targetSnapshotId;
    }

    @Override
    public long getTargetChangeSnapshotId() {
      return targetChangeSnapshotId;
    }

    public String getFailedReason() {
      return failedReason;
    }

    private Map<OptimizingTaskId, TaskRuntime> getTaskMap() {
      return taskMap;
    }

    /**
     * if all tasks are Prepared
     *
     * @return true if tasks is not empty and all Prepared
     */
    private boolean allTasksPrepared() {
      if (!taskMap.isEmpty()) {
        return taskMap.values().stream().allMatch(t -> t.getStatus() == TaskRuntime.Status.SUCCESS);
      }
      return false;
    }

    /**
     * Get optimizeRuntime.
     *
     * @return -
     */
    @Override
    public long getRunningQuotaTime(long calculatingStartTime, long calculatingEndTime) {
      return taskMap.values()
          .stream()
          .filter(t -> !t.finished())
          .mapToLong(task -> task.getQuotaTime(calculatingStartTime, calculatingEndTime))
          .sum();
    }

    @Override
    public void commit() {
      if (LOG.isDebugEnabled()) {
        LOG.debug("{} get {} tasks of {} partitions to commit", tableRuntime.getTableIdentifier(),
            taskMap.size(), taskMap.values());
      }

      lock.lock();
      try {
        if (hasCommitted) {
          LOG.warn("{} has already committed, give up", tableRuntime.getTableIdentifier());
          throw new IllegalStateException("repeat commit, and last error " + failedReason);
        }
        hasCommitted = true;
        buildCommit().commit();
        status = Status.SUCCESS;
        endTime = System.currentTimeMillis();
        persistProcessCompleted(true);
      } catch (Exception e) {
        LOG.warn("{} Commit optimizing failed ", tableRuntime.getTableIdentifier(), e);
        status = Status.FAILED;
        failedReason = ExceptionUtil.getErrorMessage(e, 4000);
        endTime = System.currentTimeMillis();
        persistProcessCompleted(false);
      } finally {
        lock.unlock();
      }
    }

    @Override
    public MetricsSummary getSummary() {
      return new MetricsSummary(taskMap.values());
    }

    private UnKeyedTableCommit buildCommit() {
      ArcticTable table = tableManager.loadTable(tableRuntime.getTableIdentifier());
      if (table.isUnkeyedTable()) {
        return new UnKeyedTableCommit(targetSnapshotId, table, taskMap.values());
      } else {
        return new KeyedTableCommit(table, taskMap.values(), targetSnapshotId,
            convertPartitionSequence(table, fromSequence), convertPartitionSequence(table, toSequence));
      }
    }

    private StructLikeMap<Long> convertPartitionSequence(ArcticTable table, Map<String, Long> partitionSequence) {
      PartitionSpec spec = table.spec();
      StructLikeMap<Long> results = StructLikeMap.create(spec.partitionType());
      partitionSequence.forEach((partition, sequence) -> {
        if (spec.isUnpartitioned()) {
          results.put(TablePropertyUtil.EMPTY_STRUCT, sequence);
        } else {
          StructLike partitionData = ArcticDataFiles.data(spec, partition);
          results.put(partitionData, sequence);
        }
      });
      return results;
    }

    private void beginAndPersistProcess() {
      doAsTransaction(
          () -> doAs(OptimizingMapper.class, mapper ->
              mapper.insertOptimizingProcess(tableRuntime.getTableIdentifier(),
                  processId, targetSnapshotId, targetChangeSnapshotId, status, optimizingType, planTime, getSummary(),
                  fromSequence, toSequence)),
          () -> doAs(OptimizingMapper.class, mapper ->
              mapper.insertTaskRuntimes(Lists.newArrayList(taskMap.values()))),
          () -> TaskFilesPersistence.persistTaskInputs(processId, taskMap.values()),
          () -> tableRuntime.beginProcess(this)
      );
    }

    private void persistProcessCompleted(boolean success) {
      if (!success) {
        doAsTransaction(
            () -> taskMap.values().forEach(TaskRuntime::tryCanceling),
            () -> doAs(OptimizingMapper.class, mapper ->
                mapper.updateOptimizingProcess(tableRuntime.getTableIdentifier().getId(), processId, status, endTime,
                    getSummary(), getFailedReason())),
            () -> tableRuntime.completeProcess(false)
        );
      } else {
        doAsTransaction(
            () -> doAs(OptimizingMapper.class, mapper ->
                mapper.updateOptimizingProcess(tableRuntime.getTableIdentifier().getId(), processId, status, endTime,
                    getSummary(), getFailedReason())),
            () -> tableRuntime.completeProcess(true)
        );
      }
    }

    private void loadTaskRuntimes() {
      List<TaskRuntime> taskRuntimes = getAs(
          OptimizingMapper.class,
          mapper -> mapper.selectTaskRuntimes(tableRuntime.getTableIdentifier().getId(), processId));
      Map<Integer, RewriteFilesInput> inputs = TaskFilesPersistence.loadTaskInputs(processId);
      taskRuntimes.forEach(taskRuntime -> {
        taskRuntime.claimOwnership(this);
        taskRuntime.setInput(inputs.get(taskRuntime.getTaskId().getTaskId()));
        taskMap.put(taskRuntime.getTaskId(), taskRuntime);
      });
    }

    private void loadTaskRuntimes(List<TaskDescriptor> taskDescriptors) {
      int taskId = 1;
      for (TaskDescriptor taskDescriptor : taskDescriptors) {
        TaskRuntime taskRuntime = new TaskRuntime(new OptimizingTaskId(processId, taskId++),
            taskDescriptor, taskDescriptor.properties());
        LOG.info("{} plan new task {}, summary {}", tableRuntime.getTableIdentifier(), taskRuntime.getTaskId(),
            taskRuntime.getSummary());
        taskMap.put(taskRuntime.getTaskId(), taskRuntime.claimOwnership(this));
      }
    }
  }

  public static class OptimizingThread {

    private String token;
    private int threadId;

    public OptimizingThread(String token, int threadId) {
      this.token = token;
      this.threadId = threadId;
    }

    public OptimizingThread() {
    }

    public String getToken() {
      return token;
    }

    public int getThreadId() {
      return threadId;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      OptimizingThread that = (OptimizingThread) o;
      return threadId == that.threadId && Objects.equal(token, that.token);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(token, threadId);
    }

    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this)
          .add("token", token)
          .add("threadId", threadId)
          .toString();
    }
  }
}
