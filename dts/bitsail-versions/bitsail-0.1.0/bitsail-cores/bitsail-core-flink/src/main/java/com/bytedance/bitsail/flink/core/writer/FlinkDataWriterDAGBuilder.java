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

import com.bytedance.bitsail.base.connector.writer.DataWriterDAGBuilder;
import com.bytedance.bitsail.base.extension.ParallelismComputable;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.flink.core.operator.BoundedDataStreamSink;
import com.bytedance.bitsail.flink.core.operator.BoundedStreamSinkOperator;

import org.apache.flink.api.java.typeutils.InputTypeConfigurable;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSink;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.operators.BoundedOneInput;

/**
 * Created 2022/4/21
 */
public abstract class FlinkDataWriterDAGBuilder<T> implements DataWriterDAGBuilder, ParallelismComputable {

  protected SinkFunction<T> sinkFunction;

  public void addWriter(DataStream<T> source,
                        int writerParallelism) throws Exception {
    DataStreamSink<T> dataStreamSink;
    if (sinkFunction instanceof BoundedOneInput) {
      source.getTransformation().getOutputType();
      // configure the type if needed
      if (sinkFunction instanceof InputTypeConfigurable) {
        ((InputTypeConfigurable) sinkFunction).setInputType(source.getType(), source.getExecutionConfig());
      }
      BoundedStreamSinkOperator<T> sinkOperator = new BoundedStreamSinkOperator<>(sinkFunction);
      BoundedDataStreamSink<T> sink = new BoundedDataStreamSink<>(source, sinkOperator);
      source.getExecutionEnvironment().addOperator(sink.getTransformation());
      dataStreamSink = sink;
    } else {
      dataStreamSink = source.addSink(sinkFunction);
    }
    dataStreamSink.uid(getWriterName())
        .name(getWriterName())
        .setParallelism(writerParallelism);
  }

  /**
   * for general case (e.g., in our flink streaming jobs), the reader and writer are chained
   */
  @Override
  public ParallelismAdvice getParallelismAdvice(BitSailConfiguration commonConf,
                                                BitSailConfiguration writerConf,
                                                ParallelismAdvice upstreamAdvice) {
    int adviceWriterParallelism = upstreamAdvice.getAdviceParallelism();
    if (writerConf.fieldExists(WriterOptions.BaseWriterOptions.WRITER_PARALLELISM_NUM) && !upstreamAdvice.isEnforceDownStreamChain()) {
      adviceWriterParallelism = writerConf.get(WriterOptions.BaseWriterOptions.WRITER_PARALLELISM_NUM);
    }
    return ParallelismAdvice.builder()
        .adviceParallelism(adviceWriterParallelism)
        .enforceDownStreamChain(false)
        .build();
  }
}
