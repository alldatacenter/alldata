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

package com.bytedance.bitsail.flink.core.execution;

import com.bytedance.bitsail.base.connector.reader.DataReaderDAGBuilder;
import com.bytedance.bitsail.base.connector.transformer.DataTransformDAGBuilder;
import com.bytedance.bitsail.base.connector.writer.DataWriterDAGBuilder;
import com.bytedance.bitsail.base.execution.ExecutionEnviron;
import com.bytedance.bitsail.base.execution.Mode;
import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.extension.GlobalCommittable;
import com.bytedance.bitsail.base.runtime.RuntimePlugin;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.flink.core.FlinkJobMode;
import com.bytedance.bitsail.flink.core.execution.configurer.BitSailRuntimePluginConfigurer;
import com.bytedance.bitsail.flink.core.execution.configurer.FlinkDAGBuilderConfigurer;
import com.bytedance.bitsail.flink.core.execution.configurer.StreamExecutionEnvironmentConfigurer;
import com.bytedance.bitsail.flink.core.execution.utils.ExecutionUtils;
import com.bytedance.bitsail.flink.core.parallelism.FlinkParallelismAdvisor;
import com.bytedance.bitsail.flink.core.reader.FlinkDataReaderDAGBuilder;
import com.bytedance.bitsail.flink.core.reader.FlinkSourceDAGBuilder;
import com.bytedance.bitsail.flink.core.writer.FlinkDataWriterDAGBuilder;

import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.flink.api.common.JobExecutionResult;
import org.apache.flink.api.dag.Transformation;
import org.apache.flink.configuration.ConfigUtils;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.TableEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class FlinkExecutionEnviron extends ExecutionEnviron {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkExecutionEnviron.class);

  private final FlinkJobMode flinkJobMode;

  /**
   * runtime plugins including JobProgress and Metrics
   */
  private List<RuntimePlugin> runtimePlugins;

  /**
   * compute parallelism for each reader and writer
   */
  private FlinkParallelismAdvisor parallelismAdvisor;

  private StreamExecutionEnvironment executionEnvironment;
  private TableEnvironment tableEnvironment;

  public FlinkExecutionEnviron(BitSailConfiguration globalConfiguration, Mode mode) {
    super(globalConfiguration, mode);
    switch (mode) {
      case STREAMING:
        this.flinkJobMode = FlinkJobMode.STREAMING;
        break;
      case BATCH:
        this.flinkJobMode = FlinkJobMode.BATCH;
        break;
      default:
        throw new UnsupportedOperationException(String.format("Execution environ %s is not supported.", mode));
    }

    this.executionEnvironment = flinkJobMode.getStreamExecutionEnvironment(commonConfiguration);
    this.tableEnvironment = flinkJobMode.getStreamTableEnvironment(executionEnvironment);
  }

  @Override
  public void registerLibraries(List<URI> libraries) {
    Configuration configuration = getFlinkConfiguration();
    List<URI> classpath = ConfigUtils
        .decodeListFromConfig(configuration, PipelineOptions.JARS, URI::create);

    classpath.addAll(libraries);

    LOG.info("Setting classpath " + JSONObject.toJSONString(classpath));
    ConfigUtils.encodeCollectionToConfig(configuration, PipelineOptions.JARS, classpath, URI::toString);
  }

  public Configuration getFlinkConfiguration() {
    try {
      Method method = StreamExecutionEnvironment.class.getDeclaredMethod("getConfiguration");
      method.setAccessible(true);
      return (Configuration) method.invoke(executionEnvironment);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void configure(List<DataReaderDAGBuilder> readerBuilders,
                        DataTransformDAGBuilder transformDAGBuilder,
                        List<DataWriterDAGBuilder> writerBuilders) throws Exception {

    /* initialize and launch runtime plugins */
    BitSailRuntimePluginConfigurer runtimePluginConfigurer = new BitSailRuntimePluginConfigurer(flinkJobMode);
    runtimePlugins = runtimePluginConfigurer.getRuntimePlugins();
    runtimePlugins.forEach(plugin -> {
      plugin.configure(commonConfiguration, readerBuilders, writerBuilders);
      plugin.start();
    });

    /* try to do schema alignment and configure each DAG builder */
    FlinkDAGBuilderConfigurer dagBuilderConfigurer = new FlinkDAGBuilderConfigurer(this);
    dagBuilderConfigurer.doSchemaAlignment(readerBuilders, writerBuilders);
    dagBuilderConfigurer.configureDAGBuilders(readerBuilders, writerBuilders);

    /* get parallelism advice for each dag builder */
    parallelismAdvisor = new FlinkParallelismAdvisor(commonConfiguration, readerConfigurations, writerConfigurations);
    parallelismAdvisor.advice(readerBuilders, writerBuilders);
    parallelismAdvisor.display();

    /* init and configure flink execution environment (checkpoint, restart strategy, etc.) */
    StreamExecutionEnvironmentConfigurer configurer = new StreamExecutionEnvironmentConfigurer(flinkJobMode, this, commonConfiguration);
    configurer.prepareExecutionEnvironment();
  }

  @Override
  public void run(List<DataReaderDAGBuilder> readerBuilders,
                  DataTransformDAGBuilder transformDAGBuilder,
                  List<DataWriterDAGBuilder> writerBuilders) throws Exception {
    buildDAG(readerBuilders, transformDAGBuilder, writerBuilders);

    try {
      String jobName = commonConfiguration.getUnNecessaryOption(CommonOptions.JOB_NAME,
          flinkJobMode.getJobName());
      JobExecutionResult result = executionEnvironment.execute(jobName);
      LOG.info("Flink job finished, execution result: \n{}.", result);

      long instanceId = commonConfiguration.getUnNecessaryOption(CommonOptions.INSTANCE_ID, -1L);
      ProcessResult<?> processResult = ProcessResult.builder()
          .jobExecutionResult(result)
          .instanceId(String.valueOf(instanceId))
          .build();

      doGlobalCommit(readerBuilders, writerBuilders, processResult);
    } catch (Exception e) {
      LOG.error("Job execution failed.", e);
      doGlobalAbort(readerBuilders, writerBuilders);
      throw e;
    }
  }

  @Override
  public void terminal(List<DataReaderDAGBuilder> readerBuilders,
                       DataTransformDAGBuilder transformDAGBuilder,
                       List<DataWriterDAGBuilder> writerBuilders) {
    LOG.info("Flink job start terminal.");

    try {
      for (DataReaderDAGBuilder reader : readerBuilders) {
        if (reader instanceof GlobalCommittable) {
          ((GlobalCommittable) reader).onDestroy();
        }
      }

      for (DataWriterDAGBuilder writer : writerBuilders) {
        if (writer instanceof GlobalCommittable) {
          ((GlobalCommittable) writer).onDestroy();
        }
      }
    } catch (Exception e) {
      LOG.warn("Flink job terminal failed.", e);
    }

    for (RuntimePlugin runtimePlugin : runtimePlugins) {
      runtimePlugin.close();
    }
    LOG.info("Flink job terminal finished.");
  }

  /**
   * build DAG in flink streaming execution environment
   * 1. using union operator to gather multiple readers
   * 2. when there is only one reader, there is no union operator
   * 3. each writer is linked with the upstream operator separately
   *
   * <pre>example 1: when there are 2 readers and 3 writers, the DAG will be like:
   *   reader_1           writer_1
   *            \       /
   *              union --writer_2
   *            /       \
   *   reader_2           writer_3
   *
   * example 2: when there is only 1 reader, the DAG will be like:
   *           writer_1
   *         /
   *  reader --writer_2
   *         \
   *           writer_3
   * </pre>
   *  todo: we will support more flexible dag in future
   */
  private <T> void buildDAG(List<DataReaderDAGBuilder> readerBuilders,
                            DataTransformDAGBuilder transformDAGBuilder,
                            List<DataWriterDAGBuilder> writerBuilders) throws Exception {
    List<DataStream> sources = new ArrayList<>();
    for (int i = 0; i < readerBuilders.size(); ++i) {
      DataReaderDAGBuilder dataReaderDAGBuilder = readerBuilders.get(i);

      DataStream<T> dataStream;
      if (dataReaderDAGBuilder instanceof FlinkDataReaderDAGBuilder) {

        dataStream = ((FlinkDataReaderDAGBuilder<T>) dataReaderDAGBuilder)
            .addSource(this, parallelismAdvisor.getAdviceReaderParallelism(dataReaderDAGBuilder));

      } else if (dataReaderDAGBuilder instanceof FlinkSourceDAGBuilder) {

        dataStream = ((FlinkSourceDAGBuilder) dataReaderDAGBuilder)
            .fromSource(this, parallelismAdvisor.getAdviceReaderParallelism(dataReaderDAGBuilder));
      } else {
        //todo
        throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, "");
      }

      dataStream = ExecutionUtils.addExecutionPartitioner(dataStream, commonConfiguration);
      sources.add(dataStream);
    }

    DataStream<T> unionStream;
    if (sources.size() == 1) {
      unionStream = sources.get(0);
    } else {
      DataStream<?> firstSource = sources.get(0);
      DataStream[] remainSources = sources
          .subList(1, sources.size()).toArray(new DataStream[0]);
      unionStream = firstSource.union(remainSources);
      setParallelism(unionStream, parallelismAdvisor.getGlobalParallelism());
      unionStream = ExecutionUtils.addExecutionPartitioner(unionStream, commonConfiguration);
    }

    for (int i = 0; i < writerBuilders.size(); ++i) {
      FlinkDataWriterDAGBuilder<T> flinkDataWriterDAGBuilder = (FlinkDataWriterDAGBuilder<T>) writerBuilders.get(i);
      flinkDataWriterDAGBuilder.addWriter(unionStream, parallelismAdvisor.getAdviceWriterParallelism(flinkDataWriterDAGBuilder));
    }
  }

  private void setParallelism(DataStream<?> dataStream, int parallelism) {
    Transformation<?> transformation = dataStream.getTransformation();
    transformation.setParallelism(parallelism);
  }

  private <T> void doGlobalCommit(List<DataReaderDAGBuilder> readerBuilders,
                                  List<DataWriterDAGBuilder> writerBuilders,
                                  ProcessResult processResult) throws Exception {
    for (DataReaderDAGBuilder reader : readerBuilders) {
      if (reader instanceof GlobalCommittable) {
        ((GlobalCommittable) reader).commit(processResult);
      }
    }

    for (DataWriterDAGBuilder writer : writerBuilders) {
      if (writer instanceof GlobalCommittable) {
        ((GlobalCommittable) writer).commit(processResult);
      }
    }

    for (RuntimePlugin runtimePlugin : runtimePlugins) {
      runtimePlugin.onSuccessComplete(processResult);
    }
  }

  private <T> void doGlobalAbort(List<DataReaderDAGBuilder> readerBuilders,
                                 List<DataWriterDAGBuilder> writerBuilders) throws Exception {
    for (DataReaderDAGBuilder reader : readerBuilders) {
      if (reader instanceof GlobalCommittable) {
        ((GlobalCommittable) reader).abort();
      }
    }
    for (DataWriterDAGBuilder writer : writerBuilders) {
      if (writer instanceof GlobalCommittable) {
        ((GlobalCommittable) writer).abort();
      }
    }
  }
}
