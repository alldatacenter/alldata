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

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.extension.GlobalCommittable;
import com.bytedance.bitsail.base.extension.SchemaAlignmentable;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.ExternalEngineConnector;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.flink.core.constants.TypeSystem;
import com.bytedance.bitsail.flink.core.legacy.connector.OutputFormatPlugin;
import com.bytedance.bitsail.flink.core.option.FlinkCommonOptions;
import com.bytedance.bitsail.flink.core.plugins.OutputAdapter;

import com.google.common.annotations.VisibleForTesting;
import lombok.Getter;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Created 2022/4/21
 */
public class PluginableOutputFormatDAGBuilder<OUT extends Row> extends FlinkDataWriterDAGBuilder<OUT> implements GlobalCommittable, SchemaAlignmentable {
  private static final Logger LOG = LoggerFactory.getLogger(PluginableOutputFormatDAGBuilder.class);

  @Getter
  private OutputFormatPlugin<OUT> outputFormatPlugin;

  private BitSailConfiguration commonConfiguration;

  @VisibleForTesting
  public PluginableOutputFormatDAGBuilder() {

  }

  @SuppressWarnings("unchecked")
  public PluginableOutputFormatDAGBuilder(OutputFormatPlugin<OUT> outOutputFormatPlugin) {
    this.outputFormatPlugin = outOutputFormatPlugin;
  }

  @Override
  public void configure(ExecutionEnviron execution, BitSailConfiguration writerConfiguration) throws Exception {
    commonConfiguration = execution.getCommonConfiguration();
    outputFormatPlugin.initFromConf(commonConfiguration, writerConfiguration);
  }

  @Override
  public String getWriterName() {
    return outputFormatPlugin.getType();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void addWriter(DataStream<OUT> source, int writerParallelism) throws Exception {
    if (TypeSystem.FLINK.equals(outputFormatPlugin.getTypeSystem())) {
      BitSailConfiguration outputAdapterConfiguration = outputFormatPlugin.getAdapterConf();
      RowTypeInfo rowTypeInfo = (RowTypeInfo) ((ResultTypeQueryable) outputFormatPlugin).getProducedType();

      OutputAdapter outputAdapter = new OutputAdapter();
      outputAdapter.initFromConf(commonConfiguration, outputAdapterConfiguration, rowTypeInfo);

      source = source.flatMap((FlatMapFunction) outputAdapter)
          .name(outputAdapter.getType())
          .setParallelism(source.getParallelism());
    }
    source = outputFormatPlugin.transform((DataStream) source);

    source.writeUsingOutputFormat(outputFormatPlugin)
        .name(getWriterName())
        .setParallelism(writerParallelism);
  }

  @Override
  public void commit(ProcessResult processResult) throws Exception {
    outputFormatPlugin.onSuccessComplete(processResult);
  }

  @Override
  public void abort() throws Exception {
    outputFormatPlugin.onFailureComplete();
  }

  @Override
  public ExternalEngineConnector createExternalEngineConnector(ExecutionEnviron executionEnviron,
                                                               BitSailConfiguration writerConfiguration) {
    BitSailConfiguration commonConfiguration = executionEnviron.getCommonConfiguration();
    ExternalEngineConnector sinkEngineConnector = null;
    try {
      sinkEngineConnector = outputFormatPlugin.initSinkSchemaManager(commonConfiguration, writerConfiguration);
    } catch (Exception e) {
      LOG.error("failed to init sink engine connector for {}", this.getWriterName());
    }
    return sinkEngineConnector;
  }

  @Override
  public ParallelismAdvice getParallelismAdvice(BitSailConfiguration commonConf,
                                                BitSailConfiguration writerConf,
                                                ParallelismAdvice upstreamAdvice) {
    int adviceReaderParallelism = upstreamAdvice.getAdviceParallelism();
    int adviceWriterParallelism;
    if (upstreamAdvice.isEnforceDownStreamChain()) {
      adviceWriterParallelism = adviceReaderParallelism;
    } else if (writerConf.fieldExists(WriterOptions.BaseWriterOptions.WRITER_PARALLELISM_NUM)) {
      adviceWriterParallelism = writerConf.get(WriterOptions.BaseWriterOptions.WRITER_PARALLELISM_NUM);
    } else {
      int adviceWriterMaxParallelism = outputFormatPlugin.getMaxParallelism();
      int adviceWriterMinParallelism = outputFormatPlugin.getMinParallelism();
      adviceWriterParallelism = Math.min(adviceWriterMaxParallelism, Math.max(adviceWriterMinParallelism, adviceReaderParallelism));

      int userConfigMinParallelism = commonConf.get(FlinkCommonOptions.FLINK_MIN_PARALLELISM);
      int userConfigMaxParallelism = commonConf.get(FlinkCommonOptions.FLINK_MAX_PARALLELISM);
      adviceWriterParallelism = Math.min(userConfigMaxParallelism, Math.max(userConfigMinParallelism, adviceWriterParallelism));
    }

    LOG.info("Writer parallelism advice: {}.", adviceWriterParallelism);
    return ParallelismAdvice.builder()
        .adviceParallelism(adviceWriterParallelism)
        .enforceDownStreamChain(false)
        .build();
  }

  @Override
  public void onDestroy() throws Exception {
    if (Objects.nonNull(outputFormatPlugin)) {
      outputFormatPlugin.onDestroy();
    }
  }

  @VisibleForTesting
  public void setOutputFormatPlugin(OutputFormatPlugin outputFormatPlugin) {
    this.outputFormatPlugin = outputFormatPlugin;
  }
}
