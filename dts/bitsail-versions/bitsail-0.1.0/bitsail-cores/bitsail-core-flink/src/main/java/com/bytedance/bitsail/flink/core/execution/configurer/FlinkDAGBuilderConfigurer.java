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

package com.bytedance.bitsail.flink.core.execution.configurer;

import com.bytedance.bitsail.base.connector.reader.DataReaderDAGBuilder;
import com.bytedance.bitsail.base.connector.writer.DataWriterDAGBuilder;
import com.bytedance.bitsail.base.extension.SchemaAlignmentable;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.ddl.DdlSyncManager;
import com.bytedance.bitsail.common.ddl.sink.SinkEngineConnector;
import com.bytedance.bitsail.common.ddl.source.SourceEngineConnector;
import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.flink.core.execution.FlinkExecutionEnviron;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

@AllArgsConstructor
public class FlinkDAGBuilderConfigurer {
  private static final Logger LOG = LoggerFactory.getLogger(FlinkDAGBuilderConfigurer.class);

  private final FlinkExecutionEnviron executionEnviron;

  /**
   * Try schema alignment if there is only one reader and one writer.
   * reader and writer should implement interface {@link SchemaAlignmentable}
   *
   * @param readerBuilders
   * @param writerBuilders
   */
  public void doSchemaAlignment(List<DataReaderDAGBuilder> readerBuilders,
                                List<DataWriterDAGBuilder> writerBuilders) throws Exception {
    if (readerBuilders.size() != 1 || writerBuilders.size() != 1) {
      LOG.warn("Schema alignment is not supported for multi source or sink.");
      return;
    }
    if (!(readerBuilders.get(0) instanceof SchemaAlignmentable)) {
      LOG.warn("reader {} does not support schema alignment.", readerBuilders.get(0).getReaderName());
      return;
    }
    if (!(writerBuilders.get(0) instanceof SchemaAlignmentable)) {
      LOG.warn("writer {} does not support schema alignment.", writerBuilders.get(0).getWriterName());
      return;
    }

    SchemaAlignmentable source = (SchemaAlignmentable) readerBuilders.get(0);
    SchemaAlignmentable sink = (SchemaAlignmentable) writerBuilders.get(0);

    BitSailConfiguration readerConfiguration = executionEnviron.getReaderConfigurations().get(0);
    BitSailConfiguration writerConfiguration = executionEnviron.getWriterConfigurations().get(0);

    SourceEngineConnector sourceEngineConnector = (SourceEngineConnector) source.createExternalEngineConnector(executionEnviron, readerConfiguration);
    SinkEngineConnector sinkEngineConnector = (SinkEngineConnector) sink.createExternalEngineConnector(executionEnviron, writerConfiguration);

    if (Objects.isNull(sourceEngineConnector) || Objects.isNull(sinkEngineConnector)) {
      LOG.warn("Skip schema alignment, source engine connector or sink engine connector not supported.");
      return;
    }

    DdlSyncManager aligner = new DdlSyncManager(
        sourceEngineConnector,
        sinkEngineConnector,
        executionEnviron.getCommonConfiguration(),
        executionEnviron.getGlobalConfiguration(),
        executionEnviron.getGlobalConfiguration());
    aligner.doColumnAlignment(source.isSchemaComparable());

    executionEnviron.refreshConfiguration();
  }

  /**
   * configure each of reader/writer DAG builders
   *
   * @param readerBuilders
   * @param writerBuilders
   */
  public <T> void configureDAGBuilders(List<DataReaderDAGBuilder> readerBuilders,
                                       List<DataWriterDAGBuilder> writerBuilders) throws Exception {
    List<BitSailConfiguration> readerConfigurations = executionEnviron.getReaderConfigurations();
    List<BitSailConfiguration> writerConfigurations = executionEnviron.getWriterConfigurations();
    Preconditions.checkState(readerBuilders.size() == readerConfigurations.size());
    Preconditions.checkState(writerBuilders.size() == writerConfigurations.size());
    for (int i = 0; i < readerBuilders.size(); ++i) {
      readerBuilders.get(i).configure(executionEnviron, readerConfigurations.get(i));
    }
    for (int i = 0; i < writerBuilders.size(); ++i) {
      writerBuilders.get(i).configure(executionEnviron, writerConfigurations.get(i));
    }
  }
}
