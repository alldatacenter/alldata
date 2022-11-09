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

package com.bytedance.bitsail.flink.core.reader;

import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.extension.GlobalCommittable;
import com.bytedance.bitsail.base.extension.ParallelismComputable;
import com.bytedance.bitsail.base.extension.SchemaAlignmentable;
import com.bytedance.bitsail.base.parallelism.ParallelismAdvice;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.ExternalEngineConnector;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.ReaderOptions;
import com.bytedance.bitsail.flink.core.constants.TypeSystem;
import com.bytedance.bitsail.flink.core.execution.FlinkExecutionEnviron;
import com.bytedance.bitsail.flink.core.legacy.connector.InputFormatPlugin;
import com.bytedance.bitsail.flink.core.legacy.connector.InputFormatSourceFunction;
import com.bytedance.bitsail.flink.core.parallelism.batch.FlinkBatchReaderParallelismComputer;
import com.bytedance.bitsail.flink.core.plugins.InputAdapter;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import lombok.Getter;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.types.Row;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Created 2022/4/21
 */
public class PluginableInputFormatDAGBuilder<T extends Row, Split extends InputSplit> extends FlinkDataReaderDAGBuilder<T>
    implements GlobalCommittable, SchemaAlignmentable {
  private static final Logger LOG = LoggerFactory.getLogger(PluginableInputFormatDAGBuilder.class);

  @Getter
  private InputFormatPlugin<T, Split> inputFormatPlugin;

  @VisibleForTesting
  public PluginableInputFormatDAGBuilder() {

  }

  @SuppressWarnings("unchecked")
  public PluginableInputFormatDAGBuilder(InputFormatPlugin<T, Split> inputFormatPlugin) {
    this.inputFormatPlugin = inputFormatPlugin;
  }

  @Override
  public void configure(ExecutionEnviron execution, BitSailConfiguration readerConfiguration) throws Exception {
    inputFormatPlugin.initFromConf(execution.getCommonConfiguration(), readerConfiguration);
  }

  @Override
  @SuppressWarnings("unchecked")
  public DataStream<T> addSource(FlinkExecutionEnviron executionEnviron,
                                 int readerParallelism) throws Exception {

    TypeInformation<T> sourceRowTypeInfo = TypeExtractor.<T>getInputFormatTypes(inputFormatPlugin);
    InputFormatSourceFunction<T> formatSourceFunction = new InputFormatSourceFunction<>(inputFormatPlugin,
        sourceRowTypeInfo);

    DataStream<T> source = executionEnviron.getExecutionEnvironment()
        .addSource(formatSourceFunction, sourceRowTypeInfo)
        .name(getReaderName())
        .setParallelism(readerParallelism);

    if (TypeSystem.FLINK.equals(inputFormatPlugin.getTypeSystem())) {
      LOG.info("Input format plugin use flink type system");
      if (!(inputFormatPlugin instanceof ResultTypeQueryable)) {
        throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
            "Plugin must be implement ResultTypeQueryable");
      }
      BitSailConfiguration inputPluginAdapterConfiguration = inputFormatPlugin.getAdapterConf();
      //todo result type
      RowTypeInfo rowTypeInfo = (RowTypeInfo) ((ResultTypeQueryable) inputFormatPlugin).getProducedType();

      //todo input adapter
      InputAdapter inputAdapter = new InputAdapter();
      inputAdapter.initFromConf(executionEnviron.getCommonConfiguration(), inputPluginAdapterConfiguration, rowTypeInfo);
      source = source
          .flatMap((FlatMapFunction) inputAdapter)
          .name(inputAdapter.getType())
          .setParallelism(source.getParallelism());
    }
    return source;
  }

  @Override
  public BaseStatistics getStatistics(BaseStatistics baseStatistics) throws IOException {
    return inputFormatPlugin.getStatistics(baseStatistics);
  }

  @Override
  public String getReaderName() {
    return inputFormatPlugin.getType();
  }

  @Override
  public void commit(ProcessResult processResult) throws Exception {
    inputFormatPlugin.onSuccessComplete(processResult);
  }

  @Override
  public void abort() throws Exception {
    inputFormatPlugin.onFailureComplete();
  }

  @Override
  public ExternalEngineConnector createExternalEngineConnector(ExecutionEnviron executionEnviron,
                                                               BitSailConfiguration readerConfiguration) {
    BitSailConfiguration commonConfiguration = executionEnviron.getCommonConfiguration();
    ExternalEngineConnector sourceEngineConnector = null;
    try {
      sourceEngineConnector = inputFormatPlugin.initSourceSchemaManager(commonConfiguration, readerConfiguration);
    } catch (Exception e) {
      LOG.error("failed to init source engine connector for {}", this.getReaderName());
    }
    return sourceEngineConnector;
  }

  @Override
  public boolean isSchemaComparable() {
    return inputFormatPlugin.supportSchemaCheck();
  }

  @Override
  public ParallelismAdvice getParallelismAdvice(BitSailConfiguration commonConf,
                                                BitSailConfiguration readerConf,
                                                ParallelismAdvice upstreamAdvice) throws Exception {
    Integer userConfigReaderParallelism = readerConf.get(ReaderOptions.BaseReaderOptions.READER_PARALLELISM_NUM);

    Stopwatch stopwatch = Stopwatch.createStarted();
    try {
      if (inputFormatPlugin instanceof ParallelismComputable) {
        return ((ParallelismComputable) inputFormatPlugin).getParallelismAdvice(commonConf, readerConf, upstreamAdvice);
      }

      FlinkBatchReaderParallelismComputer parallelismComputer = new FlinkBatchReaderParallelismComputer(commonConf);
      int adviceReaderMaxParallelism = inputFormatPlugin.createInputSplits(
          Objects.isNull(userConfigReaderParallelism) ? 1 : userConfigReaderParallelism
      ).length;
      int adviceReaderParallelism = parallelismComputer.adviceReaderParallelism(
          this.getStatistics(null), 0, adviceReaderMaxParallelism);

      if (Objects.nonNull(userConfigReaderParallelism)) {
        adviceReaderParallelism = userConfigReaderParallelism;
      }
      return ParallelismAdvice.builder()
          .adviceParallelism(adviceReaderParallelism)
          .enforceDownStreamChain(false)
          .build();
    } finally {
      LOG.info("Reader parallelism advice taken {}(s).", stopwatch.elapsed(TimeUnit.SECONDS));
    }
  }

  @Override
  public void onDestroy() throws Exception {
    if (Objects.nonNull(inputFormatPlugin)) {
      inputFormatPlugin.onDestroy();
    }
  }

  @VisibleForTesting
  public void setInputFormatPlugin(InputFormatPlugin inputFormatPlugin) {
    this.inputFormatPlugin = inputFormatPlugin;
  }
}
