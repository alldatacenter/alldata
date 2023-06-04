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

package com.bytedance.bitsail.connector.legacy.messagequeue.source;

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.base.extension.ParallelismComputable;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.flink.core.execution.FlinkExecutionEnviron;
import com.bytedance.bitsail.flink.core.reader.FlinkDataReaderDAGBuilder;

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.annotation.Internal;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;

import java.util.Properties;

@Internal
@NoArgsConstructor
public abstract class AbstractMessageQueueSourceFunctionDAGBuilder<T> extends FlinkDataReaderDAGBuilder<T> implements ParallelismComputable {

  private static final int DEFAULT_SOURCE_PARALLELISM = 1;

  protected BitSailConfiguration jobConf;

  protected String topic;

  protected Properties properties;

  @Override
  public void configure(ExecutionEnviron execution,
                        BitSailConfiguration readerConfiguration) throws Exception {
    if (execution.getReaderConfigurations().size() > 1) {
      jobConf = BitSailConfiguration.newDefault();
      jobConf.merge(execution.getCommonConfiguration(), true);
      jobConf.merge(readerConfiguration, true);
    } else {
      jobConf = execution.getGlobalConfiguration();
    }
    properties = generateProperties();
  }

  @Override
  public ParallelismAdvice getParallelismAdvice(BitSailConfiguration commonConf,
                                                BitSailConfiguration readerConf,
                                                ParallelismAdvice upstreamAdvice) {
    int readerParallelism = upstreamAdvice.getAdviceParallelism();
    if (readerConf.fieldExists(ReaderOptions.BaseReaderOptions.READER_PARALLELISM_NUM)) {
      readerParallelism = readerConf.get(ReaderOptions.BaseReaderOptions.READER_PARALLELISM_NUM);
    }

    if (readerParallelism <= 0) {
      readerParallelism = calculateParallelism();
    } else {
      readerParallelism = Math.min(readerParallelism, getSourceMaxParallelism());
    }

    return ParallelismAdvice.builder()
        .adviceParallelism(readerParallelism)
        .enforceDownStreamChain(false)
        .build();
  }

  protected String getOperatorName() {
    String operatorDesc = jobConf.get(ReaderOptions.SOURCE_OPERATOR_DESC);
    return StringUtils.isEmpty(operatorDesc) ? getReaderName() : getReaderName() + "_" + operatorDesc;
  }

  @Override
  public DataStream<T> addSource(FlinkExecutionEnviron executionEnviron, int readerParallelism) {
    String operatorName = getOperatorName();
    return getStreamSource(executionEnviron)
        .setParallelism(readerParallelism)
        .uid(operatorName)
        .name(operatorName);
  }

  public abstract  DataStreamSource<T> getStreamSource(FlinkExecutionEnviron executionEnviron);

  protected abstract Properties generateProperties();

  protected abstract int getSourcePartitionNumber();

  protected abstract int getSingleTaskPartitionThreshold();

  public int calculateParallelism() {
    int parallelism = getSourcePartitionNumber() / getSingleTaskPartitionThreshold();

    return parallelism <= 0 ? DEFAULT_SOURCE_PARALLELISM : parallelism;
  }

  public int getSourceMaxParallelism() {
    return getSourcePartitionNumber();
  }
}
