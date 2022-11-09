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

package com.bytedance.bitsail.flink.core.parallelism;

import com.bytedance.bitsail.base.connector.reader.DataReaderDAGBuilder;
import com.bytedance.bitsail.base.connector.writer.DataWriterDAGBuilder;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.flink.core.legacy.connector.InputFormatPlugin;
import com.bytedance.bitsail.flink.core.legacy.connector.OutputFormatPlugin;
import com.bytedance.bitsail.flink.core.reader.PluginableInputFormatDAGBuilder;
import com.bytedance.bitsail.flink.core.writer.PluginableOutputFormatDAGBuilder;

import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.types.Row;
import org.mockito.Mockito;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class BaseParallelismTest<T extends Row> {

  protected final int selfReaderParallelism = 10;
  protected final int mockReaderSplitNum = 400;
  protected final long mockReaderNumberOfRecords = (long) 1e8;
  protected final long mockReaderTotalInputSize = (long) 1e10;

  protected final int mockWriterMaxParallelism = 20;
  protected final int mockWriterMinParallelism = 1;

  protected DataReaderDAGBuilder getBatchReader(boolean isSelfParallelismAdvice) throws Exception {
    ParallelismAdvice selfParallelismAdvice = ParallelismAdvice.builder()
        .adviceParallelism(selfReaderParallelism)
        .enforceDownStreamChain(true)
        .build();
    if (!isSelfParallelismAdvice) {
      selfParallelismAdvice = null;
    }

    InputFormatPlugin inputFormatPlugin = getMockedInputFormat(mockReaderSplitNum, mockReaderNumberOfRecords, mockReaderTotalInputSize, selfParallelismAdvice);
    PluginableInputFormatDAGBuilder reader = new PluginableInputFormatDAGBuilder();
    reader.setInputFormatPlugin(inputFormatPlugin);
    return reader;
  }

  protected DataWriterDAGBuilder getBatchWriter() {
    OutputFormatPlugin outputFormatPlugin = getMockedOutputFormat(mockWriterMaxParallelism, mockWriterMinParallelism);
    PluginableOutputFormatDAGBuilder writer = new PluginableOutputFormatDAGBuilder();
    writer.setOutputFormatPlugin(outputFormatPlugin);
    return writer;
  }

  private OutputFormatPlugin<T> getMockedOutputFormat(int writerMaxParallelism,
                                                      int writerMinParallelism) {
    OutputFormatPlugin mockPlugin = mock(OutputFormatPlugin.class);
    doReturn(writerMinParallelism).when(mockPlugin).getMinParallelism();
    doReturn(writerMaxParallelism).when(mockPlugin).getMaxParallelism();
    return mockPlugin;
  }

  private InputFormatPlugin<T, ?> getMockedInputFormat(int mockSplitNum,
                                                       long mockNumberOfRecords,
                                                       long mockTotalInputSize,
                                                       ParallelismAdvice selfParallelismAdvice) throws Exception {
    InputFormatPlugin mockPlugin = mock(InputFormatPlugin.class);

    InputSplit[] mockSplits = new InputSplit[mockSplitNum];
    doReturn(mockSplits).when(mockPlugin).createInputSplits(Mockito.anyInt());

    BaseStatistics baseStatistics = mock(BaseStatistics.class);
    when(baseStatistics.getNumberOfRecords()).thenReturn(mockNumberOfRecords);
    when(baseStatistics.getTotalInputSize()).thenReturn(mockTotalInputSize);
    when(mockPlugin.getStatistics(null)).thenReturn(baseStatistics);

    return mockPlugin;
  }
}
