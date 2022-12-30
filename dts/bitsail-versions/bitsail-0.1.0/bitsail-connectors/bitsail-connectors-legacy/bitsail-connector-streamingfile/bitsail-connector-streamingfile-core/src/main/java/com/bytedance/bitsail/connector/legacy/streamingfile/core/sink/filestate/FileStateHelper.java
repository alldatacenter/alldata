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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.filestate;

import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.manager.CallTracer;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.rollingpolicies.PartFileInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.FileSystemCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.StreamingFileSystemSink;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.io.SimpleVersionedSerialization;
import org.apache.flink.runtime.state.FunctionInitializationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.HDFS_FILE_MOVE_LATENCY;

@Slf4j
public class FileStateHelper {
  private static final ListStateDescriptor<byte[]> PENDING_COMMIT_FILENAME_STATE_DESC =
      new ListStateDescriptor<>("pending-commit-filename", PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO);
  @VisibleForTesting
  final List<AbstractFileState> pendingFileStates;
  private final int taskId;
  private final FileStateSerializer fileStateSerializer;
  private final FileSystemCommitter committer;
  private final FileStateFactory fileStateFactory;
  private transient ListState<byte[]> pendingCommitFileNameState;

  public FileStateHelper(int taskId,
                         FileSystemCommitter committer,
                         BitSailConfiguration jobConf) {
    this.taskId = taskId;
    FileStateFactory.FileStateType fileStateType = FileStateFactory.FileStateType.valueOf(
        jobConf.get(FileSystemCommonOptions.FileStateOptions.FILE_NAME_STATE_TYPE).toUpperCase());
    this.fileStateSerializer = new FileStateSerializer();
    this.pendingFileStates = new ArrayList<>();
    this.committer = committer;
    this.fileStateFactory = new FileStateFactory(this.taskId, fileStateType, committer.getCompressionExtension());
  }

  public void addPendingFileState(long cpId, List<PartFileInfo> partFileInfos) {
    log.info("PartFileInfo size is {} for checkpoint {}.", partFileInfos.size(), cpId);
    AbstractFileState currentFileState = fileStateFactory.buildFileNameState(cpId, partFileInfos);
    this.pendingFileStates.add(currentFileState);
  }

  public boolean initializePendingFileStateFromFlinkState(FunctionInitializationContext context) throws Exception {
    pendingCommitFileNameState = context.getOperatorStateStore().getListState(PENDING_COMMIT_FILENAME_STATE_DESC);
    ListState<Tuple2<Long, Integer>> pendingCommitCheckpointTaskState =
        context.getOperatorStateStore().getUnionListState(StreamingFileSystemSink.PENDING_COMMIT_CHECKPOINT_TASK_STATE_DESC);
    if (context.isRestored()) {
      return initializePendingFileStateFromFlinkState(pendingCommitFileNameState, pendingCommitCheckpointTaskState);
    }
    return false;
  }

  public boolean initializePendingFileStateFromFlinkState(ListState<byte[]> fileNameStates, ListState<Tuple2<Long, Integer>> pendingCommitCheckpointTaskState)
      throws Exception {
    if (Objects.nonNull(fileNameStates) && CollectionUtils
        .isNotEmpty(Lists.newArrayList(fileNameStates.get()))) {
      Map<Integer, List<Long>> taskToPendingCommitCheckpointMap = getTaskToPendingCommitCheckpointMap(pendingCommitCheckpointTaskState);
      for (byte[] state : fileNameStates.get()) {
        List<AbstractFileState> fileNameStateList = SimpleVersionedSerialization.readVersionAndDeSerialize(fileStateSerializer, state);
        if (!validateFileState(fileNameStateList, taskToPendingCommitCheckpointMap)) {
          this.pendingFileStates.clear();
          log.info("Subtask {} will not restore from file name state.", taskId);
          return false;
        }
        this.pendingFileStates.addAll(fileNameStateList);
      }
      log.info("Subtask {} will restore from file name state.", taskId);
      log.info("Restore from checkpoint with pendingFileNameState size {}.", pendingFileStates.size());
      return true;
    } else {
      log.info("Subtask {} will not restore from file name state.", taskId);
      return false;
    }
  }

  private Map<Integer, List<Long>> getTaskToPendingCommitCheckpointMap(ListState<Tuple2<Long, Integer>> pendingCommitCheckpointTaskState) throws Exception {
    List<Tuple2<Long, Integer>> tempPendingCommitCheckpoints = Lists.newArrayList(pendingCommitCheckpointTaskState.get());
    Map<Integer, List<Long>> result = new HashMap<>(tempPendingCommitCheckpoints.size());
    for (Tuple2<Long, Integer> tempPendingCommitCheckpoint : tempPendingCommitCheckpoints) {
      List<Long> pendingCommitCheckpoints = result.getOrDefault(tempPendingCommitCheckpoint.f1, new ArrayList<>());
      pendingCommitCheckpoints.add(tempPendingCommitCheckpoint.f0);
      result.putIfAbsent(tempPendingCommitCheckpoint.f1, pendingCommitCheckpoints);
    }
    return result;
  }

  /**
   * If we roll back to region checkpoint, file name state will exist when we update again.
   * So we need to validate if the file name state is out of date by checking cp id.
   *
   * @param fileNameStates                   file name state
   * @param taskToPendingCommitCheckpointMap pending commit task id to checkpoint id list
   * @return true use file name state, false use region checkpoint
   */
  private boolean validateFileState(List<AbstractFileState> fileNameStates, Map<Integer, List<Long>> taskToPendingCommitCheckpointMap) {
    for (AbstractFileState fileNameState : fileNameStates) {
      int taskId = fileNameState.getTaskId();
      long checkpointId = fileNameState.getCheckpointId();
      List<Long> checkpointIdList = taskToPendingCommitCheckpointMap.getOrDefault(taskId, new ArrayList<>());
      if (!checkpointIdList.contains(checkpointId)) {
        log.info("File name state with task id {} cp id {} is not empty, but state is out of date for cp id {}. "
            + "We will not restore from it!", taskId, checkpointId, checkpointIdList);
        return false;
      }
    }
    return true;
  }

  public void restorePendingTaskCheckpoints() throws Exception {
    log.info("Subtask {} start restore from committed checkpoints.", taskId);
    commitPendingFileStates();
    log.info("Subtask {} finish restore from committed checkpoints.", taskId);
  }

  private void commitPendingFileStates() throws Exception {
    for (AbstractFileState fileNameState : pendingFileStates) {
      commitPendingFile(fileNameState);

      // TODO: delete this when file name state run static online
      // Adding this logic is to make sure that rolling back will have right data
      long cpId = fileNameState.getCheckpointId();
      int taskId = fileNameState.getTaskId();
      committer.cleanTaskCheckpointDir(taskId, cpId);
    }
    log.debug("Pending File name state size is {}", pendingFileStates.size());
    this.pendingFileStates.clear();
  }

  public void commitPendingFile(AbstractFileState fileNameState) throws Exception {
    log.info("Subtask {} start to commit subtask {} checkpoint {}", taskId, fileNameState.getTaskId(), fileNameState.getCheckpointId());
    committer.commitTaskCheckpoint(fileNameState);
  }

  public byte[] serializeFileNameState(List<AbstractFileState> fileNameState) throws IOException {
    return SimpleVersionedSerialization.writeVersionAndSerialize(fileStateSerializer, fileNameState);
  }

  public void snapshotState() throws Exception {
    pendingCommitFileNameState.clear();
    byte[] serializeState = serializeFileNameState(pendingFileStates);
    pendingCommitFileNameState.add(serializeState);
  }

  public void notifyCheckpointComplete(long checkpointId, MetricManager metrics) throws Exception {
    Stopwatch commitCheckpoint = Stopwatch.createStarted();

    try (CallTracer ignored = metrics.recordTimer(HDFS_FILE_MOVE_LATENCY).get()) {
      log.info("Subtask {} received completion for checkpoint with id={}, start commit file.", taskId, checkpointId);
      commitPendingFileStates();
    } finally {
      long elapsed = commitCheckpoint.elapsed(TimeUnit.MILLISECONDS);
      log.info("Subtask {} received completion for checkpoint with id={}, finish commit file, taken: {} ms.", taskId, checkpointId, elapsed);
    }
  }

  public void endInput() throws Exception {
    log.info("Start commit pending file in end of input!");
    commitPendingFileStates();
  }
}
