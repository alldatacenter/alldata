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

package com.bytedance.bitsail.flink.core.delagate.writer;

import com.bytedance.bitsail.base.connector.writer.v1.WriterCommitter;
import com.bytedance.bitsail.base.connector.writer.v1.comittable.CommittableMessage;
import com.bytedance.bitsail.base.connector.writer.v1.comittable.CommittableState;
import com.bytedance.bitsail.base.serializer.BinarySerializer;
import com.bytedance.bitsail.flink.core.delagate.serializer.CommittableStateSerializer;
import com.bytedance.bitsail.flink.core.delagate.serializer.DelegateSimpleVersionedSerializer;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.api.common.typeutils.base.array.BytePrimitiveArraySerializer;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.runtime.state.StateSnapshotContext;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.BoundedOneInput;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.util.SimpleVersionedListState;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Created 2022/6/16
 */
public class DelegateFlinkCommitter<CommitT> extends AbstractStreamOperator<CommittableMessage<CommitT>>
    implements OneInputStreamOperator<CommittableMessage<CommitT>, CommittableMessage<CommitT>>, BoundedOneInput {

  private static final ListStateDescriptor<byte[]> PENDING_COMMIT_STATES =
      new ListStateDescriptor<>(
          "pending-commit-states", BytePrimitiveArraySerializer.INSTANCE);

  private static final ListStateDescriptor<Long> PENDING_COMMIT_CHECKPOINT_STATES =
      new ListStateDescriptor<Long>(
          "checkpoint-id-states", LongSerializer.INSTANCE);

  private final DelegateSimpleVersionedSerializer<CommitT> commitStateSerializer;
  private final NavigableMap<Long, List<CommitT>> committablesPerCheckpoint;

  private final WriterCommitter<CommitT> writerCommitter;
  private final boolean isBatchMode;
  private final boolean isCheckpointingEnabled;
  private transient ListState<CommittableState<CommitT>> pendingCommitStates;
  private transient ListState<Long> lastCompletelyCheckpointStates;
  private long lastCompletedCheckpointId = -1;

  private DelegateFlinkCommitter(WriterCommitter<CommitT> writerCommitter,
                                 BinarySerializer<CommitT> committerSerializer,
                                 boolean isBatchMode,
                                 boolean isCheckpointEnabled) {
    this.isBatchMode = isBatchMode;
    this.isCheckpointingEnabled = isCheckpointEnabled;
    this.writerCommitter = Preconditions.checkNotNull(writerCommitter);
    this.committablesPerCheckpoint = new TreeMap<>();
    this.commitStateSerializer = DelegateSimpleVersionedSerializer.delegate(committerSerializer);
  }

  public static <CommitT> DelegateFlinkCommitter<CommitT> of(WriterCommitter<CommitT> writerCommitter,
                                                             BinarySerializer<CommitT> committerSerializer,
                                                             boolean isBatchMode,
                                                             boolean isCheckpointingEnabled) {
    return new DelegateFlinkCommitter<>(writerCommitter,
        committerSerializer,
        isBatchMode,
        isCheckpointingEnabled);
  }

  public static long restoreMinCheckpointId(ListState<Long> checkpoints) throws Exception {
    return Lists.newArrayList(checkpoints.get())
        .stream().min(Long::compare)
        .get();
  }

  private static <CommitT> NavigableMap<Long, List<CommitT>> restoreCommittableState(
      ListState<CommittableState<CommitT>> commitStates) throws Exception {

    NavigableMap<Long, List<CommitT>> restoredCommittablesPerCheckpoint = Maps.newTreeMap();
    for (CommittableState<CommitT> committableState : commitStates.get()) {
      restoredCommittablesPerCheckpoint.put(committableState.getCheckpointId(),
          committableState.getCommittables());
    }
    return restoredCommittablesPerCheckpoint;
  }

  @Override
  public void initializeState(StateInitializationContext context) throws Exception {
    super.initializeState(context);
    if (!isCheckpointingEnabled) {
      return;
    }
    ListState<byte[]> pendingCommitStates = context
        .getOperatorStateStore()
        .getListState(PENDING_COMMIT_STATES);

    this.pendingCommitStates = new SimpleVersionedListState<CommittableState<CommitT>>(pendingCommitStates,
        new CommittableStateSerializer<>(commitStateSerializer));

    lastCompletelyCheckpointStates = context
        .getOperatorStateStore()
        .getUnionListState(PENDING_COMMIT_CHECKPOINT_STATES);

    if (context.isRestored()) {
      lastCompletedCheckpointId = restoreMinCheckpointId(lastCompletelyCheckpointStates);
      committablesPerCheckpoint.putAll(restoreCommittableState(this.pendingCommitStates));
      commitAndEmitCheckpoints(lastCompletedCheckpointId);
    }
  }

  @Override
  public void processElement(StreamRecord<CommittableMessage<CommitT>> element) throws Exception {
    if (committablesPerCheckpoint.containsKey(element.getValue().getCheckpointId())) {
      committablesPerCheckpoint
          .get(element.getValue().getCheckpointId())
          .add(element.getValue().getCommittable());
    } else {
      committablesPerCheckpoint
          .put(element.getValue().getCheckpointId(),
              Lists.<CommitT>newArrayList(element.getValue().getCommittable()));
    }
  }

  @Override
  public void snapshotState(StateSnapshotContext context) throws Exception {
    super.snapshotState(context);
    List<CommittableState<CommitT>> committableStates = CommittableState
        .fromNavigableMap(committablesPerCheckpoint);
    lastCompletedCheckpointId = context.getCheckpointId();
    lastCompletelyCheckpointStates.clear();
    lastCompletelyCheckpointStates.add(lastCompletedCheckpointId);

    pendingCommitStates.update(committableStates);
  }

  @Override
  public void endInput() throws Exception {
    if (!isCheckpointingEnabled || isBatchMode) {
      // There will be no final checkpoint, all committables should be committed here
      notifyCheckpointComplete(Long.MAX_VALUE);
    }
  }

  @Override
  public void notifyCheckpointComplete(long checkpointId) throws Exception {
    super.notifyCheckpointComplete(checkpointId);
    commitAndEmitCheckpoints(checkpointId);
  }

  private void commitAndEmitCheckpoints(long checkpointId) throws IOException, InterruptedException {
    final Iterator<Map.Entry<Long, List<CommitT>>> it =
        committablesPerCheckpoint.headMap(checkpointId, true).entrySet().iterator();

    final List<CommitT> readyCommittables = new ArrayList<>();

    while (it.hasNext()) {
      final Map.Entry<Long, List<CommitT>> entry = it.next();
      final List<CommitT> committables = entry.getValue();

      readyCommittables.addAll(committables);
      it.remove();
    }

    LOG.info("Committing the state for checkpoint {}", checkpointId);
    final List<CommitT> neededToRetryCommittables = writerCommitter.commit(readyCommittables);
    if (!neededToRetryCommittables.isEmpty()) {
      throw new UnsupportedOperationException("Currently does not support the re-commit!");
    }
  }
}
