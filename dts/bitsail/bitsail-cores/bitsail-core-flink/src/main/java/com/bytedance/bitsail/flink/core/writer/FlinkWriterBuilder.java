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

package com.bytedance.bitsail.flink.core.writer;

import com.bytedance.bitsail.base.connector.writer.v1.Sink;
import com.bytedance.bitsail.base.connector.writer.v1.WriterCommitter;
import com.bytedance.bitsail.base.connector.writer.v1.comittable.CommittableMessage;
import com.bytedance.bitsail.base.dirty.AbstractDirtyCollector;
import com.bytedance.bitsail.base.dirty.DirtyCollectorFactory;
import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.base.execution.Mode;
import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.extension.GlobalCommittable;
import com.bytedance.bitsail.base.messenger.Messenger;
import com.bytedance.bitsail.base.messenger.checker.DirtyRecordChecker;
import com.bytedance.bitsail.base.messenger.common.MessengerGroup;
import com.bytedance.bitsail.base.messenger.context.MessengerContext;
import com.bytedance.bitsail.base.messenger.context.SimpleMessengerContext;
import com.bytedance.bitsail.base.ratelimit.Channel;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.flink.core.delagate.writer.DelegateFlinkCommitter;
import com.bytedance.bitsail.flink.core.delagate.writer.DelegateFlinkWriter;
import com.bytedance.bitsail.flink.core.execution.FlinkExecutionEnviron;
import com.bytedance.bitsail.flink.core.runtime.messenger.impl.StringBatchMessenger;

import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Optional;

/**
 * Created 2022/6/10
 */
public class FlinkWriterBuilder<InputT, CommitT extends Serializable, WriterStateT extends Serializable>
    extends FlinkDataWriterDAGBuilder<InputT> implements GlobalCommittable {

  private static final Logger LOG = LoggerFactory.getLogger(FlinkWriterBuilder.class);

  private final Sink<InputT, CommitT, WriterStateT> sink;

  private boolean isBatchMode;

  private ExecutionEnviron execution;

  private BitSailConfiguration commonConfiguration;
  private BitSailConfiguration writerConfiguration;

  private MessengerContext messengerContext;
  private Messenger<String> messenger;
  private AbstractDirtyCollector dirtyCollector;
  private DirtyRecordChecker dirtyRecordChecker;

  private Channel channel;

  public FlinkWriterBuilder(Sink<InputT, CommitT, WriterStateT> sink) {
    this.sink = sink;
  }

  @Override
  public void configure(ExecutionEnviron execution, BitSailConfiguration writerConfiguration) throws Exception {
    this.execution = execution;
    this.isBatchMode = Mode.BATCH.equals(execution.getMode());
    this.commonConfiguration = execution.getCommonConfiguration();
    this.writerConfiguration = writerConfiguration;

    sink.configure(execution.getCommonConfiguration(), writerConfiguration);

    this.messengerContext = SimpleMessengerContext.builder()
        .messengerGroup(MessengerGroup.WRITER)
        .instanceId(commonConfiguration.get(CommonOptions.INTERNAL_INSTANCE_ID))
        .build();
    this.messenger = new StringBatchMessenger(messengerContext);
    this.dirtyCollector = DirtyCollectorFactory.initDirtyCollector(commonConfiguration, messengerContext);
    this.dirtyRecordChecker = new DirtyRecordChecker(commonConfiguration);

    long recordSpeed = commonConfiguration.get(CommonOptions.WRITER_TRANSPORT_CHANNEL_SPEED_RECORD);
    long byteSpeed = commonConfiguration.get(CommonOptions.WRITER_TRANSPORT_CHANNEL_SPEED_BYTE);
    LOG.info("Init Output Flow Control: ");
    this.channel = new Channel(recordSpeed, byteSpeed);
  }

  @Override
  public void addWriter(DataStream<InputT> source, int writerParallelism) {
    boolean isCheckpointingEnabled = ((FlinkExecutionEnviron) execution).getExecutionEnvironment()
        .getCheckpointConfig()
        .isCheckpointingEnabled();

    DelegateFlinkWriter<InputT, CommitT, WriterStateT> flinkWriter = new DelegateFlinkWriter<>(
        commonConfiguration,
        writerConfiguration,
        sink,
        isCheckpointingEnabled);
    flinkWriter.setMessenger(messenger);
    flinkWriter.setDirtyCollector(dirtyCollector);
    flinkWriter.setChannel(channel);

    DataStream<CommittableMessage<CommitT>> writeStream = source.transform(getWriterOperatorName(),
            TypeInformation.of(new TypeHint<CommittableMessage<CommitT>>() {
            }), flinkWriter)
        .setParallelism(writerParallelism)
        .name(getWriterOperatorName())
        .uid(getWriterOperatorName());

    Optional<WriterCommitter<CommitT>> committer = sink.createCommitter();
    if (committer.isPresent()) {
      LOG.info("Writer enabled committer.");
      DataStream<CommittableMessage<CommitT>> commitStream = writeStream
          .transform(
              getWriterCommitterOperatorName(),
              TypeInformation.of(new TypeHint<CommittableMessage<CommitT>>() {
              }),
              DelegateFlinkCommitter.of(committer.get(),
                  sink.getCommittableSerializer(), isBatchMode, isCheckpointingEnabled))
          .uid(getWriterCommitterOperatorName())
          .name(getWriterCommitterOperatorName())
          .setParallelism(writerParallelism);

      if (isBatchMode) {
        commitStream.getTransformation().setParallelism(1);
      }
    }
  }

  @Override
  public String getWriterName() {
    return sink.getWriterName();
  }

  private String getWriterOperatorName() {
    return getWriterName() + "_" + "writer";
  }

  private String getWriterCommitterOperatorName() {
    return getWriterName() + "_" + "committer";
  }

  @Override
  public void commit(ProcessResult processResult) throws Exception {
    messenger.restoreMessengerCounter(processResult);
    dirtyCollector.restoreDirtyRecords(processResult);
    LOG.info("Checking dirty records during output...");
    dirtyRecordChecker.check(processResult, MessengerGroup.WRITER);
  }

  @Override
  public void abort() {

  }

  @Override
  public void onDestroy() {

  }
}
