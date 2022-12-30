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

package com.bytedance.bitsail.connector.legacy.hudi.format;

import com.bytedance.bitsail.base.dirty.impl.NoOpDirtyCollector;
import com.bytedance.bitsail.base.messenger.common.MessageType;
import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.manager.BitSailMetricManager;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;

import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("checkstyle:MagicNumber")
public class RowDataDeserializationFunction<I extends Row, O extends RowData> extends RichFlatMapFunction<I, O>
    implements ResultTypeQueryable {
  public static final RowData FILTERED_ROW = new GenericRowData(0);
  private static final Logger LOG = LoggerFactory.getLogger(RowDataDeserializationFunction.class);
  protected BitSailConfiguration jobConf;
  protected List<DeserializationSchema<O>> deserializationSchema;
  private transient NoOpDirtyCollector dirtyCollector;
  private transient MetricManager metrics;

  public RowDataDeserializationFunction(BitSailConfiguration jobConf, RowType outputSchema) {
    this.jobConf = jobConf;
    boolean multiSourceEnabled = jobConf.get(CommonOptions.MULTI_SOURCE_ENABLED);
    if (multiSourceEnabled) {
      throw new UnsupportedOperationException("Multi source not supported yet");
    } else {
      this.deserializationSchema = new ArrayList<>(1);
      this.deserializationSchema.add(0, ParserFormatFactory.getDeserializationSchema(jobConf, outputSchema));
    }
  }

  @Override
  public void open(Configuration parameters) throws Exception {
    super.open(parameters);
    int taskId = getRuntimeContext().getIndexOfThisSubtask();
    // TODO: move metric constructor to a common place with dump
    this.metrics = new BitSailMetricManager(jobConf, "dump");
    // TODO: configurable dirty collector
    this.dirtyCollector = new NoOpDirtyCollector(jobConf, taskId);
    this.deserializationSchema.forEach(d -> {
      try {
        d.open(() -> getRuntimeContext().getMetricGroup());
      } catch (Exception e) {
        throw new RuntimeException("Failed to open des", e);
      }
    });
  }

  @Override
  public void flatMap(I inputRow, Collector<O> output) throws IOException {
    try {
      byte[] record = (byte[]) inputRow.getField(1);
      O outputRow;
      int sourceIndex = (int) inputRow.getField(4);
      outputRow = deserializationSchema.get(sourceIndex).deserialize(record);
      if (Objects.isNull(outputRow)) {
        metrics.reportRecord(0, MessageType.FAILED);
        dirtyCollector.collectDirty(inputRow, new RuntimeException("Empty record found."), System.currentTimeMillis());
      } else if (outputRow != FILTERED_ROW) {
        emitWatermark(inputRow);
        output.collect(outputRow);
      }
    } catch (Exception e) {
      metrics.reportRecord(0, MessageType.FAILED);
      dirtyCollector.collectDirty(inputRow, e, System.currentTimeMillis());
    }
  }

  @Override
  public void close() throws Exception {
    super.close();
  }

  @Override
  public TypeInformation<O> getProducedType() {
    return deserializationSchema.get(0).getProducedType();
  }

  protected void emitWatermark(I inputRow) throws Exception {
    //do nothing
  }
}
