/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink;

import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.manager.CallTracer;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionComputer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.StreamingJobCommitStatus;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.aggregate.GlobalAggregateResult;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.aggregate.PartitionCommitFunction;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.aggregate.TaskSnapshotMeta;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.directory.DefaultTimeManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.AbstractPartitionStateProcessFunction;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.PartitionStrategyFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMeta;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.syncer.AbstractMetaSyncer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.MetricsFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.schema.FileSystemSchemaFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.runtime.taskexecutor.GlobalAggregateManager;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.GLOBAL_AGGREGATE_LATENCY;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionComputer.INVALID_TIMESTAMPS;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.JOB_RUN_MODE_STREAMING;

public class DefaultWatermarkEmitter implements WatermarkEmitter {
  static final ListStateDescriptor<Long> SNAPSHOT_TIME_STATE_DESC =
      new ListStateDescriptor<>("snapshot-time", LongSerializer.INSTANCE);
  static final ListStateDescriptor<Long> COMMIT_TIME_STATE_DESC =
      new ListStateDescriptor<>("commit-time", LongSerializer.INSTANCE);
  static final ListStateDescriptor<Tuple2<Long, Long>> PENDING_COMMIT_PARTITION_TIMES_STATE_DESC =
      new ListStateDescriptor<>("pending-commit-partition-times", TypeInformation.of(new TypeHint<Tuple2<Long, Long>>() {
      }));
  private static final Logger LOG = LoggerFactory.getLogger(DefaultWatermarkEmitter.class);
  private static final String GLOBAL_AGGREGATE_NAME = "TaskMinTimestamp";
  private static final Long HOUR_MILLIS = 3600 * 1000L;
  private static final Long DAY_MILLIS = 24 * HOUR_MILLIS;
  private final BitSailConfiguration jobConf;
  private final boolean initialJobCommitTimeEnabled;
  private final boolean eventTimeConjectureEnabled;
  private final Long initialJobMaxLookupThreshold;

  private final Boolean isEventTime;
  private final List<PartitionInfo> partitionKeys;
  private final DefaultTimeManager.DefaultTimeStrategy defaultTimeStrategy;
  private final AbstractPartitionCommitter partitionCommitter;

  private final String taskTimeType;
  private final String jobTimeType;

  private transient int parallelTasks;

  private transient SimpleDateFormat simpleDateFormat;

  private transient MetricManager metrics;

  private transient ProcessingTimeService processingTimeService;
  private transient FileSystemMetaManager fileSystemMetaManager;
  private transient AbstractPartitionComputer<Row> partitionComputer;
  private transient GlobalAggregateManager aggregateManager;
  private transient AbstractPartitionStateProcessFunction partitionStateProcessFunction;
  private transient PartitionCommitFunction partitionCommitFunction;
  private transient AbstractMetaSyncer syncer;

  private transient ListState<Long> snapshotTimeState;
  private transient ListState<Long> commitTimeState;

  private transient long latestTaskMinTimestamp;
  private transient long latestJobCommitTimestamp;

  private transient int taskId;

  public DefaultWatermarkEmitter(BitSailConfiguration jobConf,
                                 AbstractPartitionCommitter partitionCommitter) {
    this.jobConf = jobConf;
    this.initialJobCommitTimeEnabled = jobConf.get(FileSystemCommonOptions.CommitOptions.INITIAL_JOB_COMMIT_TIME_ENABLED);
    this.initialJobMaxLookupThreshold = jobConf.get(FileSystemCommonOptions.CommitOptions.INITIAL_JOB_COMMIT_TIME_MAX_LOOKBACK_DAY) * DAY_MILLIS;
    this.partitionCommitter = partitionCommitter;
    //TODO optimize repeated initialization
    this.fileSystemMetaManager = FileSystemSchemaFactory.createSchemaManager(jobConf);
    this.partitionKeys = PartitionUtils.getPartitionInfo(jobConf);
    this.isEventTime = jobConf.get(FileSystemCommonOptions.ArchiveOptions.ENABLE_EVENT_TIME);
    this.eventTimeConjectureEnabled = jobConf.get(FileSystemCommonOptions.ArchiveOptions.EVENT_TIME_CONJECTURE_ENABLED);
    this.taskTimeType = "task min process time";
    this.jobTimeType = "job latest commit time";
    this.defaultTimeStrategy = DefaultTimeManager.getDefaultTimeStrategy(jobConf);
    LOG.info("init job default time strategy: {}", defaultTimeStrategy);
  }

  /**
   * Amend incorrect as zero.
   */
  private static long amendTimestamp(long timestamp) {
    return timestamp <= 0 ? 0 : timestamp;
  }

  @Override
  public void open(RuntimeContext runtimeContext,
                   AbstractPartitionComputer partitionComputer,
                   FileSystemMetaManager fileSystemMetaManager) {
    this.partitionComputer = partitionComputer;
    this.fileSystemMetaManager = fileSystemMetaManager;
    this.processingTimeService = ((StreamingRuntimeContext) runtimeContext).getProcessingTimeService();
    this.partitionComputer.open(runtimeContext, fileSystemMetaManager);
    syncFileSystemMetaBySingleTask();
    reportDumpMetrics();
  }

  @Override
  public void initializeState(FunctionInitializationContext context, RuntimeContext runtimeContext) throws Exception {
    this.parallelTasks = runtimeContext.getNumberOfParallelSubtasks();
    this.simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    long initJobCommitTime = jobConf.get(FileSystemCommonOptions.CommitOptions.INITIAL_JOB_COMMIT_TIME);
    long currentTime = System.currentTimeMillis();
    if (initialJobCommitTimeEnabled && initJobCommitTime + initialJobMaxLookupThreshold > currentTime && initJobCommitTime < currentTime) {
      this.latestTaskMinTimestamp = initJobCommitTime;
      this.latestJobCommitTimestamp = initJobCommitTime;
      LOG.info("initial job commit time enabled, latest job commit timestamp is {}", latestJobCommitTimestamp);
    } else {
      this.latestTaskMinTimestamp = Long.MIN_VALUE;
      this.latestJobCommitTimestamp = Long.MIN_VALUE;
    }

    this.taskId = runtimeContext.getIndexOfThisSubtask();
    this.partitionStateProcessFunction = PartitionStrategyFactory.getPartitionStateProcessFunction(jobConf);
    this.aggregateManager = ((StreamingRuntimeContext) runtimeContext).getGlobalAggregateManager();
    this.metrics = MetricsFactory.getInstanceMetricsManager(jobConf, taskId);
    this.snapshotTimeState = context.getOperatorStateStore().getUnionListState(SNAPSHOT_TIME_STATE_DESC);
    this.commitTimeState = context.getOperatorStateStore().getUnionListState(COMMIT_TIME_STATE_DESC);

    this.partitionStateProcessFunction.initializeState(context);
    if (context.isRestored()) {
      initializeSnapshotTime(snapshotTimeState);
      initializeCommitTime(commitTimeState);
    }

    StreamingJobCommitStatus initJobCommitStatus = new StreamingJobCommitStatus(jobConf);
    initJobCommitStatus.setJobCommitTime(latestJobCommitTimestamp);
    partitionStateProcessFunction.updateStreamingJobCommitStatus(initJobCommitStatus);
    partitionCommitFunction = new PartitionCommitFunction(runtimeContext.getNumberOfParallelSubtasks(),
        partitionCommitter, initJobCommitStatus, jobConf);
  }

  @Override
  public void snapshotState(FunctionSnapshotContext context,
                            long nextCheckpointId,
                            int partFileSize) throws Exception {
    long cpId = context.getCheckpointId();
    LOG.info("Subtask {} checkpointing for checkpoint with id={}, start: {}={}.", taskId, cpId,
        taskTimeType, simpleDateFormat.format(latestTaskMinTimestamp));
    this.snapshotTimeState.clear();
    this.commitTimeState.clear();
    computeTaskMinTimestamp(cpId, partFileSize);
    computeJobMinTimestamp(nextCheckpointId);

    this.snapshotTimeState.add(latestTaskMinTimestamp);
    this.commitTimeState.add(latestJobCommitTimestamp);

    this.partitionStateProcessFunction.snapshotState(context);
  }

  @Override
  public void notifyCheckpointComplete(long checkpointId, long nextCheckpointId) throws Exception {
    if (latestTaskMinTimestamp <= 0) {
      LOG.info("Subtask {} received completion for checkpoint with id={}, {} is invalid, value={}.", taskId, checkpointId,
          taskTimeType, simpleDateFormat.format(latestTaskMinTimestamp));
      return;
    }
    commitFileSystemPartition(nextCheckpointId);
  }

  @Override
  public void onProcessingTime(long timestamp) throws IOException {
    // update job commit time to partition computer
    LOG.info("Subtask {} time service was triggered in {}.", taskId, simpleDateFormat.format(timestamp));
    if (isEventTime || fileSystemMetaManager.shouldScheduleUpdate()) {
      GlobalAggregateResult globalAggregateResult = getGlobalAggregateResult(INVALID_TIMESTAMPS, -1, false);

      if (isEventTime) {
        updateLatestJobCommitTimestamp(globalAggregateResult);
      }

      if (fileSystemMetaManager.shouldScheduleUpdate()) {
        fileSystemMetaManager.updateFileSystemMeta(globalAggregateResult.getFileSystemMeta(), taskId);
      }
    }
  }

  @Override
  public void emit(Row row, boolean eventTimeSafeModeEnabled) {
    partitionComputer.updateLatestJobTimestamp(latestJobCommitTimestamp);
    partitionComputer.updateDefaultTimestamp(defaultTimeStrategy.getDefaultTime(latestJobCommitTimestamp));
    partitionComputer.configureSafeMode(eventTimeSafeModeEnabled, System.currentTimeMillis(), latestTaskMinTimestamp);
    partitionComputer.updatePartitionLatestTimestamp(Maps.newHashMap());
    if (eventTimeSafeModeEnabled) {
      LOG.info("Subtask {} in safe mode, min task timestamp: {}.",
          taskId, simpleDateFormat.format(latestTaskMinTimestamp));
    }
    LOG.info("Subtask {} start new transaction", taskId);
  }

  @Override
  public void stimulate() {

  }

  public long getTaskMinTime() {
    return latestTaskMinTimestamp;
  }

  public long getJobCommitTime() {
    return latestJobCommitTimestamp;
  }

  public void updatePartitionStatus(List<PartFileInfo> partFileInfos) {
    partitionStateProcessFunction.updateStatusInSnapshot(partFileInfos);
  }

  private void initializeSnapshotTime(final ListState<Long> snapshotState) throws Exception {
    this.latestTaskMinTimestamp = getStateMinTimestamp(snapshotState);
  }

  private void initializeCommitTime(final ListState<Long> commitState) throws Exception {
    this.latestJobCommitTimestamp = getStateMaxTimestamp(commitState);
  }

  private long getStateMinTimestamp(final ListState<Long> timeStates) throws Exception {
    long minTimestamp = Long.MAX_VALUE;
    for (Long timestamp : timeStates.get()) {
      minTimestamp = Math.min(minTimestamp, timestamp);
    }
    if (minTimestamp == Long.MAX_VALUE) {
      minTimestamp = Long.MIN_VALUE;
    }
    return minTimestamp;
  }

  private long getStateMaxTimestamp(final ListState<Long> timeStates) throws Exception {
    long maxTimestamp = Long.MIN_VALUE;
    for (Long timestamp : timeStates.get()) {
      maxTimestamp = Math.max(maxTimestamp, timestamp);
    }
    return maxTimestamp;
  }

  /**
   * Compute task min timestamp
   */
  private void computeTaskMinTimestamp(long cpId, int partFileSize) {
    if (isEventTime) {
      long taskMinTimestamp = partitionComputer.getPartitionMinTimestamp();
      if (taskMinTimestamp > 0) {
        latestTaskMinTimestamp = taskMinTimestamp;
      } else if (eventTimeConjectureEnabled && partFileSize != 0) {
        /*
         *  If event time fields is invalid, taskMinTimestamp will be always {@link INVALID_TIMESTAMPS}, then
         *  aggregate will be never invoked in notify phrase.
         *  In this situation, if {@link eventTimeConjectureEnabled} is true and
         *  task create data files actually in the checkpoint duration, task will use processing time to
         *  replace event time.
         */
        latestTaskMinTimestamp = processingTimeService.getCurrentProcessingTime();
        LOG.warn("Subtask {} checkpointing for checkpoint with id={}, partition file creations={}, " +
                "replace {} to processing time {}.", taskId, cpId, partFileSize,
            taskTimeType, latestTaskMinTimestamp);
      } else {
        LOG.info("Subtask {} checkpointing for checkpoint with id={}, {} is invalid, value={}.", taskId, cpId,
            taskTimeType, simpleDateFormat.format(taskMinTimestamp));
      }
    } else {
      latestTaskMinTimestamp = processingTimeService.getCurrentProcessingTime();
    }
  }

  private void computeJobMinTimestamp(long checkpointId) throws IOException {
    // only partition data need to commit partition when checkpoint complete
    if (partitionKeys.size() > 0) {
      GlobalAggregateResult globalAggregateResult = getGlobalAggregateResult(latestTaskMinTimestamp, checkpointId, false);
      updateLatestJobCommitTimestamp(globalAggregateResult);
    }
  }

  /**
   * Metric and get global aggregate result.
   */
  public GlobalAggregateResult getGlobalAggregateResult(long latestTaskMinTimestamp, long checkpointId,
                                                        boolean notifyCheckpointPhase) throws IOException {
    try (CallTracer ignored = metrics.recordTimer(GLOBAL_AGGREGATE_LATENCY).get()) {
      TaskSnapshotMeta snapshotMeta = new TaskSnapshotMeta(taskId,
          latestTaskMinTimestamp,
          getFileSystemMetaInfo(),
          checkpointId,
          notifyCheckpointPhase);
      partitionStateProcessFunction.updateSnapshotMeta(snapshotMeta);
      return aggregateManager.updateGlobalAggregate(
          GLOBAL_AGGREGATE_NAME,
          snapshotMeta,
          partitionCommitFunction
      );
    }
  }

  /**
   * Upload task min timestamp to job manager and get all job latest commit timestamp.
   *
   * @param aggregateResult task min timestamp
   */
  private void updateLatestJobCommitTimestamp(GlobalAggregateResult aggregateResult) {
    long jobCommitTimeBefore = latestJobCommitTimestamp;
    long jobCommitTimeAfter = aggregateResult.getMinJobTimestamp();

    if (jobCommitTimeAfter <= 0) {
      LOG.info("Subtask {} update job commit time, {} is invalid, value={}.", taskId,
          jobTimeType, simpleDateFormat.format(jobCommitTimeAfter));
      return;
    }
    partitionStateProcessFunction.updateStatusFromAggregateResult(aggregateResult);
    latestJobCommitTimestamp = jobCommitTimeAfter;
    partitionComputer.updateLatestJobTimestamp(latestJobCommitTimestamp);
    partitionComputer.updateDefaultTimestamp(defaultTimeStrategy.getDefaultTime(latestJobCommitTimestamp));

    LOG.info("Subtask {} update job commit time, before commit time={}, after commit time={}.", taskId,
        simpleDateFormat.format(jobCommitTimeBefore),
        simpleDateFormat.format(latestJobCommitTimestamp));
  }

  private void reportDumpMetrics() {
    metrics.removeMetric(StreamingFileSystemMetricsNames.TASK_MIN_TIMESTAMP);
    metrics.recordGauge(StreamingFileSystemMetricsNames.TASK_MIN_TIMESTAMP, () -> {
      if (isEventTime) {
        return amendTimestamp(Math.max(partitionComputer.getPartitionMinTimestamp(), latestTaskMinTimestamp));
      }
      return amendTimestamp(System.currentTimeMillis());
    });
    metrics.removeMetric(StreamingFileSystemMetricsNames.JOB_MIN_TIMESTAMP);
    metrics.recordGauge(StreamingFileSystemMetricsNames.JOB_MIN_TIMESTAMP, () -> amendTimestamp(latestJobCommitTimestamp));
  }

  private void commitFileSystemPartition(long nextCheckpointId) throws Exception {
    if (CollectionUtils.size(partitionKeys) <= 0) {
      return;
    }
    GlobalAggregateResult globalAggregateResult =
        getGlobalAggregateResult(INVALID_TIMESTAMPS, nextCheckpointId, true);
    updateLatestJobCommitTimestamp(globalAggregateResult);
  }

  /**
   * Combine task id and last success checkpoint id as a tuple.
   * In old state mode, there only have cpIdState state in flink.
   * In new state mode, we can direct use pendingCommitCheckpointTaskState to build as tuple
   */
  public long getRestoredCpId(boolean regionCheckpointEnable,
                              ListState<Tuple2<Long, Integer>> pendingCommitCheckpointTaskState,
                              ListState<Long> cpIdState) throws Exception {
    Long restoreCpId;
    if (regionCheckpointEnable) {
      List<Tuple2<Long, Integer>> pendingCommitCheckpoints = Lists.newArrayList(pendingCommitCheckpointTaskState.get().iterator());
      Long maxCpId = null;
      Long taskMaxCpId = null;
      for (Tuple2<Long, Integer> pendingCommitCheckpoint : pendingCommitCheckpoints) {
        if (pendingCommitCheckpoint.f1 == taskId) {
          if (Objects.isNull(taskMaxCpId) || taskMaxCpId < pendingCommitCheckpoint.f0) {
            taskMaxCpId = pendingCommitCheckpoint.f0;
          }
        }
        if (Objects.isNull(maxCpId) || maxCpId < pendingCommitCheckpoint.f0) {
          maxCpId = pendingCommitCheckpoint.f0;
        }
      }

      if (Objects.nonNull(taskMaxCpId)) {
        restoreCpId = taskMaxCpId;
        LOG.info("Subtask {} restore from region checkpoint id = {}.", taskId, restoreCpId);
      } else {
        restoreCpId = maxCpId;
        LOG.info("Subtask {} restore from max checkpoint id = {}.", taskId, restoreCpId);
      }
      return restoreCpId;
    }
    if (!cpIdState.get().iterator().hasNext()) {
      LOG.info("No checkpoint-id state found, check your last start version.");
      return 0L;
    }
    return cpIdState.get().iterator().next();
  }

  /**
   * In region checkpoint state, each task will be assigned itself task checkpoint state task-0 will be a special task,
   * because it not only contains itself but also contain state which
   * task id bigger than the parallel.
   */
  public Optional<List<Tuple2<Long, Integer>>> getPendingCommitCheckpoints(boolean regionCheckpointEnable,
                                                                           final ListState<Tuple2<Long, Integer>> pendingCommitsCheckpointsState) throws Exception {
    if (regionCheckpointEnable) {
      List<Tuple2<Long, Integer>> tempPendingCommitCheckpoints = Lists.newArrayList(pendingCommitsCheckpointsState.get());
      Collection<Tuple2<Long, Integer>> assignedPendingCommitCheckpoints = Lists.newArrayList();

      for (Tuple2<Long, Integer> pendingCommitCheckpoint : tempPendingCommitCheckpoints) {
        if (pendingCommitCheckpoint.f1 == taskId) {
          assignedPendingCommitCheckpoints.add(pendingCommitCheckpoint);
        }
        if (pendingCommitCheckpoint.f1 >= parallelTasks && taskId == 0) {
          assignedPendingCommitCheckpoints.add(pendingCommitCheckpoint);
        }
      }
      return Optional.ofNullable(Lists.newArrayList(assignedPendingCommitCheckpoints));
    }
    return Optional.empty();
  }

  /**
   * Start Task-0 Special jobs.
   * 1. FileSystemMeta sync in task-0 now.
   */
  private void syncFileSystemMetaBySingleTask() {
    String jobRunMode = jobConf.get(CommonOptions.JOB_TYPE);
    if (JOB_RUN_MODE_STREAMING.equalsIgnoreCase(jobRunMode) && taskId == 0) {
      if (fileSystemMetaManager.shouldScheduleUpdate()) {
        LOG.info("Subtask {} start file system meta syncer.", taskId);
        syncer = fileSystemMetaManager.createMetaSyncer();
        syncer.start();
      }
    }
  }

  /**
   * Get the newest hive meta by round robin in task-0.
   */
  private FileSystemMeta getFileSystemMetaInfo() {
    return Objects.nonNull(syncer) ? syncer.current()
        : null;
  }

}
