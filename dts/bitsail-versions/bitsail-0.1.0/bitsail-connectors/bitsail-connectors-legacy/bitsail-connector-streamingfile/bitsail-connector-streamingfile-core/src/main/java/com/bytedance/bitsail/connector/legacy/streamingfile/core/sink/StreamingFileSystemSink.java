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

import com.bytedance.bitsail.base.decoder.MessageDecodeCompositor;
import com.bytedance.bitsail.base.dirty.AbstractDirtyCollector;
import com.bytedance.bitsail.base.messenger.common.MessageType;
import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.manager.CallTracer;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.LogUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.checkpoint.StreamingFileSinkSnapshotWrapper;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionComputer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.decoder.SkipMessageDecoder;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filestate.FileStateCollector;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filestate.FileStateFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.OutputFormatFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionComputer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionPathUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionTempFileManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionWriter;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionWriterFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.DefaultRollingPolicy;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.RollingPolicy;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMeta;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.syncer.AbstractMetaSyncer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.MetricsFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.filestate.FileStateHelper;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.schema.FileSystemSchemaFactory;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.utils.CloseableUtils;
import org.apache.flink.api.common.io.OutputFormat;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.Path;
import org.apache.flink.runtime.state.CheckpointListener;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.operators.BoundedOneInput;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeCallback;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.bytedance.bitsail.base.constants.BaseMetricsNames.RECORD_INVOKE_LATENCY;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.CHECKPOINT_NOTIFY_LATENCY;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.CHECKPOINT_SNAPSHOT_LATENCY;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.HDFS_FILE_CLOSE_LATENCY;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.SUB_TASK_ID;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.ROLLING_POLICY_INTERVAL_DEFAULT_RATIO;
import static com.bytedance.bitsail.flink.core.serialization.AbstractDeserializationSchema.DUMP_ROW_OFFSET_INDEX;
import static com.bytedance.bitsail.flink.core.serialization.AbstractDeserializationSchema.DUMP_ROW_PARTITION_INDEX;
import static com.bytedance.bitsail.flink.core.serialization.AbstractDeserializationSchema.DUMP_ROW_VALUE_INDEX;

/**
 * Factory of {@link StreamingFileSystemSink}
 */
public class StreamingFileSystemSink<T extends Row>
    extends RichSinkFunction<T>
    implements CheckpointedFunction, CheckpointListener, ProcessingTimeCallback, BoundedOneInput {

  public static final ListStateDescriptor<Tuple2<Long, Integer>> PENDING_COMMIT_CHECKPOINT_TASK_STATE_DESC =
      new ListStateDescriptor<>("pending-commit-checkpoint-task", TypeInformation.of(new TypeHint<Tuple2<Long, Integer>>() {
      }));
  static final ListStateDescriptor<Long> CP_ID_STATE_DESC =
      new ListStateDescriptor<>("checkpoint-id", LongSerializer.INSTANCE);
  private static final Logger LOG = LoggerFactory.getLogger(StreamingFileSystemSink.class);
  private static final long FIRST_CHECKPOINT_ID = 1L;
  private final FileStateCollector fileStateCollector;
  private final OutputFormatFactory<T> outputFormatFactory;
  private final PartitionWriterFactory<T> partitionWriterFactory;
  private final AbstractPartitionComputer<T> partitionComputer;
  private final AbstractPartitionCommitter partitionCommitter;
  private final FileSystemMetaManager fileSystemMetaManager;
  private final FileSystemCommitter committer;
  private final boolean eventTimeSafeModeEnabled;
  private final boolean checkMessageQueueOffset;

  private final String formatType;
  private final long jobCheckInterval;
  private final Long dirtyRecordSampleThreshold;
  private final BitSailConfiguration jobConf;
  private final int remainCheckpointNum;
  private final WatermarkEmitter watermarkEmitter;

  private transient PartitionTempFileManager fileManager;
  private transient PartitionWriter<T> writer;
  private transient StreamingFileSinkSnapshotWrapper snapshotWrapper;
  private transient AbstractDirtyCollector dirtyCollector;

  private transient boolean regionCheckpointEnable;
  private transient int taskId;
  private transient MetricManager metrics;
  private transient Long errorCount;
  private transient ListState<Long> cpIdState;

  /**
   * TODO: DELETE this state when file name state has online
   */
  private transient ListState<Tuple2<Long, Integer>> pendingCommitCheckpointTaskState;
  private transient Boolean startTransaction;
  private transient Map<String, Long> partitionOffset;


  private transient ProcessingTimeService processingTimeService;
  private transient long nextCheckpointId;
  private transient long preCheckpointId;
  private transient long firstCheckpointId;
  private transient List<Tuple2<Long, Integer>> pendingCommitCheckpoints;
  private transient MessageDecodeCompositor decoders;
  private transient AbstractMetaSyncer syncer;
  private transient FileStateHelper fileStateHelper;
  private transient Exception invokeException = null;

  public StreamingFileSystemSink(
      OutputFormatFactory<T> outputFormatFactory,
      PartitionComputer<T> partitionComputer,
      PartitionWriterFactory<T> partitionWriterFactory,
      FileSystemCommitter committer,
      final BitSailConfiguration jobConf) {

    this.outputFormatFactory = outputFormatFactory;
    this.partitionComputer = (AbstractPartitionComputer) partitionComputer;
    this.partitionWriterFactory = partitionWriterFactory;
    this.committer = committer;
    this.jobConf = jobConf;
    this.taskId = -1;
    this.jobCheckInterval = jobConf.get(FileSystemCommonOptions.CHECK_INTERVAL);

    this.fileStateCollector = FileStateFactory.create();
    this.partitionCommitter = committer.createPartitionCommitter(fileStateCollector);
    this.fileSystemMetaManager = FileSystemSchemaFactory.createSchemaManager(jobConf);
    this.watermarkEmitter = WatermarkEmitter.createDefault(jobConf, partitionCommitter);

    this.eventTimeSafeModeEnabled = jobConf.get(FileSystemCommonOptions.ArchiveOptions.EVENT_TIME_SAFE_MODE_ENABLED);
    this.checkMessageQueueOffset = jobConf.get(FileSystemCommonOptions.MQ_OFFSET_CHECK);

    this.remainCheckpointNum = jobConf.get(FileSystemCommonOptions.FileStateOptions.DUMP_REMAIN_DIRECTORY_NUM);
    this.formatType = jobConf.get(FileSystemCommonOptions.DUMP_FORMAT_TYPE);
    this.dirtyRecordSampleThreshold = jobConf.get(CommonOptions.DirtyRecordOptions.DIRTY_RECORD_SAMPLE_THRESHOLD);
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    checkTimezone();
    super.open(parameters);
    parameters.setString(SUB_TASK_ID, String.valueOf(taskId));
    fileSystemMetaManager.open();

    this.startTransaction = true;
    this.partitionOffset = Maps.newHashMap();

    processingTimeService = ((StreamingRuntimeContext) getRuntimeContext()).getProcessingTimeService();
    processingTimeService.registerTimer(processingTimeService.getCurrentProcessingTime() + jobCheckInterval, this);

    watermarkEmitter.open(getRuntimeContext(), partitionComputer, fileSystemMetaManager);

    fileManager = committer.createTempFileManager(taskId, nextCheckpointId);
    dirtyCollector = committer.createDirtyCollector(taskId, parameters);
    PartitionWriter.Context<T> context = new PartitionWriter.Context<>(parameters,
        outputFormatFactory,
        jobConf,
        fileSystemMetaManager
    );

    partitionComputer.open(getRuntimeContext(), fileSystemMetaManager);
    writer = partitionWriterFactory.create(context,
        fileManager,
        partitionComputer,
        initRollingPolicy(),
        fileSystemMetaManager);

    snapshotWrapper = new StreamingFileSinkSnapshotWrapper(taskId, jobConf);
    decoders = new MessageDecodeCompositor();
    decoders.addDecoder(new SkipMessageDecoder(jobConf));
    syncFileSystemMetaBySingleTask();
  }

  private ZoneId getTimezoneFromConf() {
    String defaultTimezone = jobConf.get(FileSystemCommonOptions.DUMP_DEFAULT_TIMEZONE_REGION);
    if (StringUtils.isEmpty(defaultTimezone)) {
      return ZoneId.systemDefault();
    } else {
      return ZoneId.of(defaultTimezone);
    }
  }

  private void checkTimezone() {
    ZoneId timezoneFromConf = getTimezoneFromConf();
    if (!timezoneFromConf.getRules().equals(ZoneId.systemDefault().getRules())) {
      throw new RuntimeException(
          String.format("Subtask %d system timezone : %s is not consistent with the expected timezone: %s", taskId, ZoneId.systemDefault(), timezoneFromConf));
    }
  }

  @Override
  public void initializeState(FunctionInitializationContext context) throws Exception {
    fileStateCollector.open();

    this.regionCheckpointEnable = false;

    this.pendingCommitCheckpoints = Lists.newArrayList();
    this.errorCount = 0L;

    taskId = getRuntimeContext().getIndexOfThisSubtask();
    fileStateHelper = new FileStateHelper(taskId, committer, jobConf);
    committer.setSubTaskId(taskId);
    watermarkEmitter.initializeState(context, getRuntimeContext());
    metrics = MetricsFactory.getInstanceMetricsManager(jobConf, taskId);

    cpIdState = context.getOperatorStateStore().getUnionListState(CP_ID_STATE_DESC);

    pendingCommitCheckpointTaskState = context.getOperatorStateStore().getUnionListState(PENDING_COMMIT_CHECKPOINT_TASK_STATE_DESC);
    boolean usingFileNameState = fileStateHelper.initializePendingFileStateFromFlinkState(context);
    if (context.isRestored()) {
      initializeRegionCheckpointState();
      Optional<List<Tuple2<Long, Integer>>> option = ((DefaultWatermarkEmitter) watermarkEmitter)
          .getPendingCommitCheckpoints(regionCheckpointEnable, pendingCommitCheckpointTaskState);
      option.ifPresent(pendingChks -> this.pendingCommitCheckpoints = pendingChks);

      long restoredCpId = ((DefaultWatermarkEmitter) watermarkEmitter)
          .getRestoredCpId(regionCheckpointEnable, pendingCommitCheckpointTaskState, cpIdState);
      LOG.info("Subtask {} initializing it's state (checkpoint id={}).", taskId, restoredCpId);

      if (usingFileNameState) {
        fileStateHelper.restorePendingTaskCheckpoints();
      } else {
        restorePendingTaskCheckpoints(restoredCpId);
      }
      nextCheckpointId = restoredCpId + 1;
    } else {
      nextCheckpointId = FIRST_CHECKPOINT_ID;
    }

    firstCheckpointId = nextCheckpointId;
    preCheckpointId = nextCheckpointId - 1;

    cleanCheckpointDir(nextCheckpointId);
  }

  private void restorePendingTaskCheckpoints(long restoreCpId) throws Exception {
    if (regionCheckpointEnable) {
      LOG.info("Subtask {} start restore from committed checkpoints.", taskId);
      commitPendingTaskCheckpoints();
      LOG.info("Subtask {} finish restore from committed checkpoints.", taskId);
      return;
    }
    if (taskId == 0) {
      LOG.info("Subtask {} start restore from restore checkpoint id {}.", taskId, restoreCpId);
      committer.commitUpToCheckpoint(restoreCpId);
    }
  }

  private void commitPendingTaskCheckpoints() throws Exception {
    for (Tuple2<Long, Integer> committingCheckpoint : pendingCommitCheckpoints) {
      LOG.info("Subtask {} start commit from checkpoint id {} with assigned task id {}.", taskId,
          committingCheckpoint.f0, committingCheckpoint.f1);
      committer.commitTaskCheckpoint(committingCheckpoint.f0, committingCheckpoint.f1);
    }
    pendingCommitCheckpoints.clear();
  }

  private RollingPolicy<T> initRollingPolicy() {
    DefaultRollingPolicy.PolicyBuilder policyBuilder = DefaultRollingPolicy.create();
    long checkpointInterval = jobConf.get(CommonOptions.CheckPointOptions.CHECKPOINT_INTERVAL);
    long rollingPolicyInterval = (long) Math.floor(checkpointInterval * ROLLING_POLICY_INTERVAL_DEFAULT_RATIO * 1d);

    if (jobConf.fieldExists(FileSystemCommonOptions.CommitOptions.ROLLING_MAX_PART_SIZE)) {
      policyBuilder.withMaxPartSize(jobConf.get(FileSystemCommonOptions.CommitOptions.ROLLING_MAX_PART_SIZE));
    }

    policyBuilder.withRolloverInterval(rollingPolicyInterval);

    if (jobConf.fieldExists(FileSystemCommonOptions.CommitOptions.ROLLING_INACTIVITY_INTERVAL)) {
      policyBuilder.withInactivityInterval(jobConf.get(FileSystemCommonOptions.CommitOptions.ROLLING_INACTIVITY_INTERVAL));
    }
    return policyBuilder.build();
  }

  private void initializeRegionCheckpointState() throws Exception {
    if (Objects.nonNull(pendingCommitCheckpointTaskState) && CollectionUtils
        .isNotEmpty(Lists.newArrayList(pendingCommitCheckpointTaskState.get()))) {
      LOG.info("Subtask {} will restore from region checkpoint state.", taskId);
      regionCheckpointEnable = true;
    }
  }

  private void checkMessageQueueOffset(T value) {
    if (!checkMessageQueueOffset) {
      return;
    }

    String partition = (String) value.getField(DUMP_ROW_PARTITION_INDEX);
    Long currentOffset = (Long) value.getField(DUMP_ROW_OFFSET_INDEX);
    if (currentOffset < 0) {
      return;
    }
    if (!partitionOffset.containsKey(partition)) {
      LOG.info("Subtask {} partition {} offset init, mq offset {}.", taskId, partition, currentOffset);
      partitionOffset.put(partition, currentOffset);
      return;
    }

    Long oldOffset = partitionOffset.get(partition);
    long delta = currentOffset - oldOffset;
    if (delta > 0) {
      partitionOffset.put(partition, currentOffset);
      if (delta > 1) {
        metrics.recordCounter(StreamingFileSystemMetricsNames.MQ_OFFSET_INTERRUPT, delta);
        LOG.warn("Subtask {} partition {} offset interrupt, before mq offset {}, after mq offset {}, may lose {} message.",
            taskId, partition, oldOffset, currentOffset, delta);
      }
    } else if (delta == 0) {
      LOG.warn("Subtask {} partition {} offset duplicate, before mq offset {}, after mq offset {}.",
          taskId, partition, oldOffset, currentOffset);
      metrics.recordCounter(StreamingFileSystemMetricsNames.MQ_OFFSET_DUPLICATE);
    } else {
      LOG.warn("Subtask {} partition {} offset out of order, before mq offset {}, after mq offset {}.",
          taskId, partition, oldOffset, currentOffset);
      metrics.recordCounter(StreamingFileSystemMetricsNames.MQ_OFFSET_OUT_OF_ORDER);
    }
  }

  @Override
  public void invoke(T value, Context context) throws Exception {
    if (startTransaction) {
      startTransaction();
      watermarkEmitter.emit(value,
          eventTimeSafeModeEnabled && Objects.equals(firstCheckpointId, nextCheckpointId));
    }

    checkMessageQueueOffset(value);
    if (value.getField(DUMP_ROW_VALUE_INDEX) == null) {
      LOG.info("Subtask {} skip null record.", taskId);
      return;
    }

    try (CallTracer ignored = metrics.recordTimer(RECORD_INVOKE_LATENCY).get()) {
      byte[] decodeValue = decoders.decode((byte[]) value.getField(DUMP_ROW_VALUE_INDEX));
      value.setField(DUMP_ROW_VALUE_INDEX, decodeValue);

      writer.write(value);
      watermarkEmitter.stimulate();
      metrics.reportRecord(decodeValue.length, MessageType.SUCCESS);
    } catch (BitSailException e) {
      metrics.reportRecord(0, MessageType.FAILED);
      dirtyCollector.collectDirty(value, e, processingTimeService.getCurrentProcessingTime());
      processDirtyRecord("Write one record failed. - " + value, e);
    } catch (Exception e) {
      processDirtyRecord("Couldn't write data - " + value, e);
      invokeException = e;
      throw new IOException(String
          .format("Couldn't write data - %s", LogUtils.logCut(value.toString(),
              StreamingFileSystemValidator.DIRTY_RECORD_LOG_LENGTH)), e);
    }
  }

  private void startTransaction() throws Exception {
    fileManager.generateTaskTmpPath(nextCheckpointId);
    startTransaction = false;
  }

  private void processDirtyRecord(String errorMessage, Exception e) {
    if (errorCount++ % dirtyRecordSampleThreshold == 0) {
      LOG.warn(errorMessage, e);
    }
  }

  private void preSnapshotStateCheck(FunctionSnapshotContext context) {
    long currentCpId = context.getCheckpointId();
    if (invokeException != null) {
      throw new RuntimeException(String.format("Subtask %s trigger " +
              "checkpoint id %s with some exception, skip current snapshot",
          taskId, currentCpId),
          invokeException);
    }
    if (currentCpId < nextCheckpointId) {
      throw new IllegalArgumentException(String.format("Subtask %s trigger " +
              "checkpoint id %s less than next checkpoint id %s, skip current snapshot",
          taskId, currentCpId, nextCheckpointId));
    }
  }

  @Override
  public void snapshotState(FunctionSnapshotContext context) throws Exception {
    Stopwatch snapshotStopWatch = Stopwatch.createStarted();
    preSnapshotStateCheck(context);
    long cpId = context.getCheckpointId();

    snapshotWrapper.snapshotState();
    try (CallTracer ignored = metrics.recordTimer(CHECKPOINT_SNAPSHOT_LATENCY).get()) {
      cpIdState.clear();
      pendingCommitCheckpointTaskState.clear();

      /**
       * cpId is got from context which may be not the same as nextCheckpointId.
       * In file name state, we need nextCheckpointId to restore tmp file path.
       */
      closeWriter(cpId);
      watermarkEmitter.snapshotState(context, nextCheckpointId, CollectionUtils.size(writer.getPartFileInfoCreations()));
      pendingCommitCheckpoints.add(Tuple2.of(nextCheckpointId, taskId));
      preCheckpointId = nextCheckpointId;
      nextCheckpointId = cpId + 1;
      startTransaction = true;

      cpIdState.add(cpId);

      fileStateHelper.snapshotState();
      pendingCommitCheckpointTaskState.addAll(pendingCommitCheckpoints);

      snapshotWrapper.asyncSnapshotState();
    } finally {
      LOG.info("Subtask {} checkpointing for checkpoint with id={}, taken: {} ms.", taskId, cpId, snapshotStopWatch.elapsed(TimeUnit.MILLISECONDS));
    }
  }

  private void updatePartitionFileState() {
    Map<String, OutputFormat<T>> outputFormats = writer.getOutputFormats();
    if (MapUtils.isNotEmpty(outputFormats)) {
      for (Map.Entry<String, OutputFormat<T>> format : outputFormats.entrySet()) {
        Path partitionPath = new Path(format.getKey());
        fileStateCollector.updateFileState(taskId,
            PartitionPathUtils.extractPartitionSpecFromPath(partitionPath));
      }
    }
  }

  private void closeWriter(long contextCpId) throws Exception {
    updatePartitionFileState();

    Stopwatch closeWriterStopWatch = Stopwatch.createStarted();
    try (CallTracer ignored = metrics.recordTimer(HDFS_FILE_CLOSE_LATENCY).get()) {
      LOG.info("Subtask {} checkpointing for checkpoint with id={}, start close hdfs file.", taskId, contextCpId);
      writer.close(Long.MIN_VALUE, contextCpId, false);
      List<PartFileInfo> partitionInfos = writer.getPartFileInfoCreations();
      fileStateHelper.addPendingFileState(this.nextCheckpointId, partitionInfos);
      ((DefaultWatermarkEmitter) watermarkEmitter).updatePartitionStatus(partitionInfos);
      writer.clearPartFileInfo();
    } finally {
      long elapsed = closeWriterStopWatch.elapsed(TimeUnit.MILLISECONDS);
      LOG.info("Subtask {} checkpointing for checkpoint with id={}, finish close hdfs file, taken: {} ms.", taskId, contextCpId, elapsed);
    }
  }

  /**
   * Start Task-0 Special jobs.
   * 1. FileSystemMeta sync in task-0 now.
   */
  private void syncFileSystemMetaBySingleTask() {
    if (taskId == 0) {
      if (fileSystemMetaManager.shouldScheduleUpdate()) {
        LOG.info("Subtask {} start file system meta syncer.", taskId);
        syncer = fileSystemMetaManager.createMetaSyncer();
        syncer.start();
      }
    }
  }

  @Override
  public void notifyCheckpointComplete(long checkpointId) throws Exception {
    Stopwatch notifyStopWatch = Stopwatch.createStarted();
    snapshotWrapper.notifyCheckpointComplete();

    try (CallTracer ignored = metrics.recordTimer(CHECKPOINT_NOTIFY_LATENCY).get()) {
      LOG.info("Subtask {} received completion for checkpoint with id={}.", taskId, checkpointId);

      fileStateHelper.notifyCheckpointComplete(checkpointId, metrics);
      pendingCommitCheckpoints.clear();
      cleanCheckpointDir(checkpointId);

      watermarkEmitter.notifyCheckpointComplete(checkpointId, nextCheckpointId);
    } finally {
      LOG.info("Subtask {} received completion for checkpoint with id={}, taken: {} ms.", taskId, checkpointId,
          notifyStopWatch.elapsed(TimeUnit.MILLISECONDS));
    }
  }

  private void cleanCheckpointDir(long cpId) throws Exception {
    /**
     * delete path of cp < currentCpId - remainCheckpointNum, that means we will keep at most remainCheckpointNum cp directory.
     */
    long toDeleteCpId = cpId - remainCheckpointNum;
    /**
     * When we reset offset, the first checkpoint id will always 1.
     * The next checkpoint id will get from zookeeper which may larger than first checkpoint id plus remainCheckpointNum.
     * This will make us clear directory with files to be rename and lose some data in first checkpoint.
     * So we should clear checkpoint directory only when toDeleteCpId smaller than preCheckpointId.
     */
    if (taskId == 0 && toDeleteCpId > 0 && toDeleteCpId < preCheckpointId) {
      committer.cleanCheckpointDir(toDeleteCpId);
    }
  }

  @Override
  public void close() throws Exception {
    Stopwatch closeStopWatch = Stopwatch.createStarted();

    try {
      LOG.info("Subtask {} exits, start close hdfs file.", taskId);
      if (writer != null) {
        writer.close(Long.MIN_VALUE, Long.MIN_VALUE);
      }
      fileStateCollector.close();
      committer.close();
      if (dirtyCollector != null) {
        dirtyCollector.close();
      }
      CloseableUtils.closeQuietly(syncer);
      MetricsFactory.removeMetricManager(taskId);
      LOG.info("Subtask {} exits, finish close hdfs file, taken: {} ms.", taskId, closeStopWatch.elapsed(TimeUnit.MILLISECONDS));
    } catch (Exception e) {
      throw new IOException("Exception in close hdfs file", e);
    }
  }

  /**
   * Tag service upload invalid timestamp in which avoid create empty partition.
   *
   * @param timestamp current processing time
   */
  @Override
  public void onProcessingTime(long timestamp) throws Exception {
    long currentTime = System.currentTimeMillis();
    watermarkEmitter.onProcessingTime(timestamp);
    //check dirty file on process time
    dirtyCollector.onProcessingTime(timestamp);
    // check rolling file on process time
    writer.onProcessingTime(timestamp);
    processingTimeService.registerTimer(currentTime + jobCheckInterval, this);
  }

  private FileSystemMeta getFileSystemMetaInfo() {
    return Objects.nonNull(syncer) ? syncer.current()
        : null;
  }

  @Override
  public void endInput() throws Exception {
    LOG.info("Run in endInput, start to commit pending file!");
    closeWriter(nextCheckpointId);
    fileStateHelper.endInput();
  }
}
