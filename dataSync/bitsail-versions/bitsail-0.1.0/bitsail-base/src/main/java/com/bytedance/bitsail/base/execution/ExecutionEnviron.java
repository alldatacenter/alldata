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

package com.bytedance.bitsail.base.execution;

import com.bytedance.bitsail.base.connector.reader.DataReaderDAGBuilder;
import com.bytedance.bitsail.base.connector.transformer.DataTransformDAGBuilder;
import com.bytedance.bitsail.base.connector.writer.DataWriterDAGBuilder;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.configuration.ConfigParser;

import lombok.Getter;

import java.io.Serializable;
import java.net.URI;
import java.util.List;

/**
 * Created 2022/4/21
 */
@Getter
public abstract class ExecutionEnviron implements Serializable {

  protected BitSailConfiguration globalConfiguration;
  protected BitSailConfiguration commonConfiguration;
  protected List<BitSailConfiguration> readerConfigurations;
  protected List<BitSailConfiguration> writerConfigurations;
  protected Mode mode;

  /**
   * Constructor for execution environment.
   *
   * @param globalConfiguration User defined configurations.
   * @param mode                Indicate the job type.
   */
  public ExecutionEnviron(BitSailConfiguration globalConfiguration, Mode mode) {
    this.globalConfiguration = globalConfiguration;
    this.commonConfiguration = ConfigParser.getSysCommonConf(globalConfiguration);
    this.readerConfigurations = ConfigParser.getInputConfList(globalConfiguration);
    this.writerConfigurations = ConfigParser.getOutputConfList(globalConfiguration);
    this.mode = mode;
  }

  /**
   * Re-initialize reader and writer configurations from user defined configurations.
   */
  public void refreshConfiguration() {
    this.commonConfiguration = ConfigParser.getCommonConf(commonConfiguration);
    this.readerConfigurations = ConfigParser.getInputConfList(globalConfiguration);
    this.writerConfigurations = ConfigParser.getOutputConfList(globalConfiguration);
  }

  /**
   * Register execution jars in current execution environment.
   *
   * @param libraries A url list for jar to register.
   */
  public abstract void registerLibraries(List<URI> libraries);

  /**
   * Configure current execution environment, readers, transformer, and writers.
   *
   * @param readerBuilders      Initialized but not configured readers.
   * @param transformDAGBuilder An initialized but not configured transformer.
   * @param writerBuilders      Initialized but not configured writers.
   */
  public abstract void configure(List<DataReaderDAGBuilder> readerBuilders,
                                 DataTransformDAGBuilder transformDAGBuilder,
                                 List<DataWriterDAGBuilder> writerBuilders) throws Exception;

  /**
   * Run job in the current execution environment.
   */
  public abstract void run(List<DataReaderDAGBuilder> readerBuilders,
                           DataTransformDAGBuilder transformDAGBuilder,
                           List<DataWriterDAGBuilder> writerBuilders) throws Exception;

  /**
   * Invoke when job terminal by TERM signal
   */
  public void terminal(List<DataReaderDAGBuilder> readerBuilders,
                       DataTransformDAGBuilder transformDAGBuilder,
                       List<DataWriterDAGBuilder> writerBuilders) {

  }
}
