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

import com.bytedance.bitsail.base.connector.writer.v1.Sink;
import com.bytedance.bitsail.base.connector.writer.v1.Writer;
import com.bytedance.bitsail.base.connector.writer.v1.comittable.CommittableMessage;
import com.bytedance.bitsail.base.dirty.AbstractDirtyCollector;
import com.bytedance.bitsail.base.messenger.Messenger;
import com.bytedance.bitsail.base.messenger.common.MessageType;
import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.manager.BitSailMetricManager;
import com.bytedance.bitsail.base.metrics.manager.CallTracer;
import com.bytedance.bitsail.base.ratelimit.Channel;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.column.ColumnCast;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.common.typeinfo.TypeInfo;
import com.bytedance.bitsail.common.typeinfo.TypeInfoUtils;
import com.bytedance.bitsail.common.util.Pair;
import com.bytedance.bitsail.flink.core.delagate.converter.FlinkRowConvertSerializer;
import com.bytedance.bitsail.flink.core.delagate.serializer.DelegateSimpleVersionedSerializer;
import com.bytedance.bitsail.flink.core.runtime.RuntimeContextInjectable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.PrimitiveArrayTypeInfo;
import org.apache.flink.runtime.state.CheckpointListener;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.runtime.state.StateSnapshotContext;
import org.apache.flink.streaming.api.operators.AbstractStreamOperator;
import org.apache.flink.streaming.api.operators.BoundedOneInput;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.streaming.api.operators.util.SimpleVersionedListState;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.types.Row;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.bytedance.bitsail.base.constants.BaseMetricsNames.RECORD_CHANNEL_FLOW_CONTROL;
import static com.bytedance.bitsail.base.constants.BaseMetricsNames.RECORD_INVOKE_LATENCY;

/**
 * Created 2022/6/10
 */
public class DelegateFlinkWriter<InputT, CommitT extends Serializable, WriterStateT extends Serializable> extends AbstractStreamOperator<CommittableMessage<CommitT>>
    implements
    CheckpointListener, BoundedOneInput, OneInputStreamOperator<InputT, CommittableMessage<CommitT>> {

  private static final ListStateDescriptor<byte[]> WRITE_STATES_DESCRIPTOR =
      new ListStateDescriptor<byte[]>("write-states", PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO);
  private final boolean isCheckpointingEnabled;
  private final Sink<InputT, CommitT, WriterStateT> sink;
  private final BitSailConfiguration writerConfiguration;
  private final BitSailConfiguration commonConfiguration;
  private final FlinkRowConvertSerializer flinkRowConvertSerializer;
  private final TypeInfo<?>[] typeInfos;
  private transient Writer<InputT, CommitT, WriterStateT> writer;
  private transient ListState<WriterStateT> writeState;
  private final DelegateSimpleVersionedSerializer<WriterStateT> writeStateSerializer;
  private boolean endOfInput = false;

  @Setter
  private Messenger<String> messenger;

  @Setter
  private AbstractDirtyCollector dirtyCollector;

  @Setter
  private Channel channel;

  private transient MetricManager metricManager;

  public DelegateFlinkWriter(BitSailConfiguration commonConfiguration,
                             BitSailConfiguration writerConfiguration,
                             Sink<InputT, CommitT, WriterStateT> sink,
                             boolean isCheckpointingEnabled) {
    super();
    this.isCheckpointingEnabled = isCheckpointingEnabled;
    this.commonConfiguration = commonConfiguration;
    this.writerConfiguration = writerConfiguration;
    this.sink = sink;

    List<ColumnInfo> columnInfos = writerConfiguration.get(WriterOptions.BaseWriterOptions.COLUMNS);
    this.typeInfos = TypeInfoUtils
        .getTypeInfos(sink.createTypeInfoConverter(),
            columnInfos);

    this.flinkRowConvertSerializer = new FlinkRowConvertSerializer(
        typeInfos,
        columnInfos,
        this.commonConfiguration);

    this.writeStateSerializer = DelegateSimpleVersionedSerializer
        .delegate(sink.getWriteStateSerializer());

  }

  @Override
  public void open() throws Exception {
    if (messenger instanceof RuntimeContextInjectable) {
      ((RuntimeContextInjectable) messenger).setRuntimeContext(getRuntimeContext());
    }
    if (dirtyCollector instanceof RuntimeContextInjectable) {
      ((RuntimeContextInjectable) dirtyCollector).setRuntimeContext(getRuntimeContext());
    }
    messenger.open();
    ColumnCast.initColumnCast(commonConfiguration);

    int taskId = getRuntimeContext().getIndexOfThisSubtask();
    metricManager = new BitSailMetricManager(commonConfiguration,
        "output",
        false,
        ImmutableList.of(
            Pair.newPair("instance", String.valueOf(commonConfiguration.get(CommonOptions.INSTANCE_ID))),
            Pair.newPair("type", sink.getWriterName()),
            Pair.newPair("task", String.valueOf(taskId))
        )
    );
    metricManager.start();
  }

  @Override
  public void initializeState(StateInitializationContext context) throws Exception {
    super.initializeState(context);
    ListState<byte[]> rawWriteState = context
        .getOperatorStateStore()
        .getListState(WRITE_STATES_DESCRIPTOR);

    writeState = new SimpleVersionedListState<>(rawWriteState, writeStateSerializer);
    Writer.Context<WriterStateT> writeSinkContext = new Writer.Context<WriterStateT>() {

      @Override
      public TypeInfo<?>[] getTypeInfos() {
        return typeInfos;
      }

      @Override
      public int getIndexOfSubTaskId() {
        return getRuntimeContext().getIndexOfThisSubtask();
      }

      @Override
      public boolean isRestored() {
        return isCheckpointingEnabled && context.isRestored();
      }

      @SneakyThrows
      @Override
      public List<WriterStateT> getRestoreStates() {
        return isCheckpointingEnabled && context.isRestored() ?
            Lists.newArrayList(writeState.get()) :
            Collections.emptyList();
      }
    };
    writer = sink.createWriter(writeSinkContext);
  }

  @Override
  public void processElement(StreamRecord<InputT> element) throws Exception {
    InputT value = element.getValue();
    try (CallTracer ignore = metricManager.recordTimer(RECORD_INVOKE_LATENCY).get()) {
      try {
        if (value instanceof Row) {
          // convert flink row to BitSail row.
          com.bytedance.bitsail.common.row.Row deserializer = flinkRowConvertSerializer.deserialize((Row) value);
          writer.write((InputT) deserializer);
        } else {
          writer.write(element.getValue());
        }
        messenger.addSuccessRecord(value.toString());
        metricManager.reportRecord(0, MessageType.SUCCESS);
      } catch (BitSailException e) {
        Row dirtyRow = new Row(2);
        dirtyRow.setField(1, value.toString());

        messenger.addFailedRecord(value.toString(), e);
        dirtyCollector.collectDirty(dirtyRow, e, System.currentTimeMillis());
        LOG.debug("Failed to write one record. - {}", value, e);
        metricManager.reportRecord(0, MessageType.FAILED);
      }
    } catch (Exception e) {
      metricManager.reportRecord(0, MessageType.FAILED);
      throw new IOException("Couldn't write date - " + value, e);
    }

    try (CallTracer ignore = metricManager.recordTimer(RECORD_CHANNEL_FLOW_CONTROL).get()) {
      channel.checkFlowControl(
          messenger.getSuccessRecords(),
          messenger.getSuccessRecordBytes(),
          messenger.getFailedRecords()
      );
    }
  }

  @Override
  public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {
    super.prepareSnapshotPreBarrier(checkpointId);
    if (!endOfInput) {
      writer.flush(false);
      emitCommittables(checkpointId);
    }
  }

  @Override
  public void snapshotState(StateSnapshotContext context) throws Exception {
    super.snapshotState(context);
    writeState.update(writer.snapshotState(context.getCheckpointId()));
  }

  @Override
  public void endInput() throws Exception {
    endOfInput = true;
    writer.flush(true);
    emitCommittables(Long.MAX_VALUE);
  }

  @Override
  public void close() throws Exception {
    writer.close();
    messenger.commit();
    dirtyCollector.storeDirtyRecords();

    messenger.close();
    dirtyCollector.close();

    if (Objects.nonNull(metricManager)) {
      metricManager.close();
    }
  }

  private void emitCommittables(long checkpointId) throws IOException {
    List<CommitT> committables = writer.prepareCommit();
    if (CollectionUtils.isEmpty(committables)) {
      return;
    }
    StreamingRuntimeContext runtimeContext = getRuntimeContext();
    final int indexOfThisSubtask = runtimeContext.getIndexOfThisSubtask();

    emit(indexOfThisSubtask, checkpointId, committables);
  }

  private void emit(int indexOfThisSubtask,
                    long checkpointId,
                    Collection<CommitT> committables) {
    for (CommitT committable : committables) {
      output.collect(
          new StreamRecord<CommittableMessage<CommitT>>(
              CommittableMessage.<CommitT>builder()
                  .committable(committable)
                  .checkpointId(checkpointId)
                  .subTaskId(indexOfThisSubtask)
                  .build()));
    }
  }
}
