/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.flink.write;

import com.netease.arctic.flink.shuffle.ShuffleHelper;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.write.hidden.HiddenLogWriter;
import com.netease.arctic.flink.write.hidden.LogMsgFactory;
import com.netease.arctic.log.LogData;
import org.apache.flink.runtime.state.StateInitializationContext;
import org.apache.flink.runtime.state.StateSnapshotContext;
import org.apache.flink.streaming.api.graph.StreamConfig;
import org.apache.flink.streaming.api.operators.Output;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.StreamTask;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.Schema;

import java.time.Duration;
import java.util.Properties;

/**
 * This is an automatic logstore writer util class.
 * It will write logstore when the system current timestamp is greater than the watermark of all subtasks plus the
 * {@link com.netease.arctic.flink.table.descriptors.ArcticValidator#AUTO_EMIT_LOGSTORE_WATERMARK_GAP} value.
 */
public class AutomaticLogWriter extends ArcticLogWriter {
  private final AutomaticDoubleWriteStatus status;
  private final ArcticLogWriter arcticLogWriter;

  public AutomaticLogWriter(
      Schema schema,
      Properties producerConfig,
      String topic,
      LogMsgFactory<RowData> factory,
      LogData.FieldGetterFactory<RowData> fieldGetterFactory,
      byte[] jobId,
      ShuffleHelper helper,
      ArcticTableLoader tableLoader,
      Duration writeLogstoreWatermarkGap) {
    this.arcticLogWriter =
        new HiddenLogWriter(schema, producerConfig, topic, factory, fieldGetterFactory, jobId, helper);
    this.status = new AutomaticDoubleWriteStatus(tableLoader, writeLogstoreWatermarkGap);
  }

  @Override
  public void setup(StreamTask<?, ?> containingTask, StreamConfig config, Output<StreamRecord<RowData>> output) {
    super.setup(containingTask, config, output);
    arcticLogWriter.setup(containingTask, config, output);
    status.setup(getRuntimeContext().getIndexOfThisSubtask());
  }

  @Override
  public void initializeState(StateInitializationContext context) throws Exception {
    super.initializeState(context);
    arcticLogWriter.initializeState(context);
  }

  @Override
  public void open() throws Exception {
    super.open();
    arcticLogWriter.open();
    status.open();
  }

  @Override
  public void processElement(StreamRecord<RowData> element) throws Exception {
    if (status.isDoubleWrite()) {
      arcticLogWriter.processElement(element);
    }
  }

  @Override
  public void processWatermark(Watermark mark) throws Exception {
    status.processWatermark(mark);
    super.processWatermark(mark);
  }

  @Override
  public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {
    if (status.isDoubleWrite()) {
      arcticLogWriter.prepareSnapshotPreBarrier(checkpointId);
    } else {
      status.sync();
    }
  }

  @Override
  public void snapshotState(StateSnapshotContext context) throws Exception {
    if (status.isDoubleWrite()) {
      arcticLogWriter.snapshotState(context);
    }
  }

  @Override
  public void notifyCheckpointComplete(long checkpointId) throws Exception {
    if (status.isDoubleWrite()) {
      arcticLogWriter.notifyCheckpointComplete(checkpointId);
    }
  }

  @Override
  public void notifyCheckpointAborted(long checkpointId) throws Exception {
    if (status.isDoubleWrite()) {
      arcticLogWriter.notifyCheckpointAborted(checkpointId);
    }
  }

  @Override
  public void close() throws Exception {
    if (status.isDoubleWrite()) {
      arcticLogWriter.close();
    }
  }

  @Override
  public void endInput() throws Exception {
    if (status.isDoubleWrite()) {
      arcticLogWriter.endInput();
    }
  }
}
