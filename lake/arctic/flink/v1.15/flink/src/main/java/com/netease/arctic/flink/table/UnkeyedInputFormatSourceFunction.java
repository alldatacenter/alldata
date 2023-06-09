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

package com.netease.arctic.flink.table;

import com.netease.arctic.flink.interceptor.ProxyFactory;
import org.apache.flink.api.common.io.RichInputFormat;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.metrics.Counter;
import org.apache.flink.runtime.jobgraph.tasks.InputSplitProvider;
import org.apache.flink.runtime.jobgraph.tasks.InputSplitProviderException;
import org.apache.flink.streaming.api.functions.source.InputFormatSourceFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import org.apache.flink.streaming.api.operators.StreamingRuntimeContext;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.flink.source.FlinkInputFormat;
import org.apache.iceberg.flink.source.FlinkInputSplit;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Copy from {@link InputFormatSourceFunction}
 */
public class UnkeyedInputFormatSourceFunction extends RichParallelSourceFunction<RowData> {
  private static final long serialVersionUID = 1L;

  private TypeInformation<RowData> typeInfo;
  private transient TypeSerializer<RowData> serializer;

  private FlinkInputFormat format;
  private ProxyFactory<FlinkInputFormat> formatFactory;

  private transient InputSplitProvider provider;
  private transient Iterator<InputSplit> splitIterator;

  private volatile boolean isRunning = true;

  @SuppressWarnings("unchecked")
  public UnkeyedInputFormatSourceFunction(ProxyFactory<FlinkInputFormat> formatFactory,
                                          TypeInformation<RowData> typeInfo) {
    this.formatFactory = formatFactory;
    this.typeInfo = typeInfo;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void open(Configuration parameters) throws Exception {
    StreamingRuntimeContext context = (StreamingRuntimeContext) getRuntimeContext();

    format = formatFactory.getInstance();
    if (format instanceof RichInputFormat) {
      ((RichInputFormat) format).setRuntimeContext(context);
    }
    format.configure(parameters);

    provider = context.getInputSplitProvider();
    serializer = typeInfo.createSerializer(getRuntimeContext().getExecutionConfig());
    splitIterator = getInputSplits();
    isRunning = splitIterator.hasNext();
  }

  @Override
  public void run(SourceContext<RowData> ctx) throws Exception {
    try {
      Counter completedSplitsCounter =
          getRuntimeContext().getMetricGroup().counter("numSplitsProcessed");
      if (isRunning && format instanceof RichInputFormat) {
        ((RichInputFormat) format).openInputFormat();
      }

      RowData nextElement = serializer.createInstance();
      while (isRunning) {
        format.open((FlinkInputSplit) splitIterator.next());

        // for each element we also check if cancel
        // was called by checking the isRunning flag

        while (isRunning && !format.reachedEnd()) {
          nextElement = format.nextRecord(nextElement);
          if (nextElement != null) {
            ctx.collect(nextElement);
          } else {
            break;
          }
        }
        format.close();
        completedSplitsCounter.inc();

        if (isRunning) {
          isRunning = splitIterator.hasNext();
        }
      }
    } finally {
      format.close();
      if (format instanceof RichInputFormat) {
        ((RichInputFormat) format).closeInputFormat();
      }
      isRunning = false;
    }
  }

  @Override
  public void cancel() {
    isRunning = false;
  }

  @Override
  public void close() throws Exception {
    format.close();
    if (format instanceof RichInputFormat) {
      ((RichInputFormat) format).closeInputFormat();
    }
  }

  /**
   * Returns the {@code InputFormat}. This is only needed because we need to set the input split
   * assigner on the {@code StreamGraph}.
   */
  public FlinkInputFormat getFormat() {
    return format;
  }

  private Iterator<InputSplit> getInputSplits() {

    return new Iterator<InputSplit>() {

      private InputSplit nextSplit;

      private boolean exhausted;

      @Override
      public boolean hasNext() {
        if (exhausted) {
          return false;
        }

        if (nextSplit != null) {
          return true;
        }

        final InputSplit split;
        try {
          split =
              provider.getNextInputSplit(
                  getRuntimeContext().getUserCodeClassLoader());
        } catch (InputSplitProviderException e) {
          throw new RuntimeException("Could not retrieve next input split.", e);
        }

        if (split != null) {
          this.nextSplit = split;
          return true;
        } else {
          exhausted = true;
          return false;
        }
      }

      @Override
      public InputSplit next() {
        if (this.nextSplit == null && !hasNext()) {
          throw new NoSuchElementException();
        }

        final InputSplit tmp = this.nextSplit;
        this.nextSplit = null;
        return tmp;
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }
    };
  }
}
