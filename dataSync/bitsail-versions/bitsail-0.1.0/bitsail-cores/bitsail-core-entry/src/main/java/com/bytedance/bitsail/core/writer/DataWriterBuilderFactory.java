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

package com.bytedance.bitsail.core.writer;

import com.bytedance.bitsail.base.connector.writer.DataWriterDAGBuilder;
import com.bytedance.bitsail.base.connector.writer.v1.Sink;
import com.bytedance.bitsail.base.execution.Mode;
import com.bytedance.bitsail.base.packages.PackageManager;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.WriterOptions;
import com.bytedance.bitsail.flink.core.legacy.connector.OutputFormatPlugin;
import com.bytedance.bitsail.flink.core.writer.FlinkWriterBuilder;
import com.bytedance.bitsail.flink.core.writer.PluginableOutputFormatDAGBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Created 2022/4/21
 */
@SuppressWarnings("unchecked")
public class DataWriterBuilderFactory {
  private static final Logger LOG = LoggerFactory.getLogger(DataWriterBuilderFactory.class);

  public static <T> List<DataWriterDAGBuilder> getDataWriterDAGBuilderList(Mode mode,
                                                                           List<BitSailConfiguration> writerConfigurations,
                                                                           PackageManager packageManager) {
    return writerConfigurations.stream()
        .map(writerConf -> {
          try {
            return (DataWriterDAGBuilder) getDataWriterDAGBuilder(mode, writerConf, packageManager);
          } catch (Exception e) {
            LOG.error("failed to create writer DAG builder");
            throw new RuntimeException(e);
          }
        })
        .collect(Collectors.toList());
  }

  public static <T> DataWriterDAGBuilder getDataWriterDAGBuilder(Mode mode,
                                                                 BitSailConfiguration globalConfiguration,
                                                                 PackageManager packageManager) throws Exception {

    Class<T> writerClass = DataWriterBuilderFactory.<T>getWriterClass(globalConfiguration, packageManager);
    if (Sink.class.isAssignableFrom(writerClass)) {
      return new FlinkWriterBuilder((Sink<?, ?, ?>) writerClass.getConstructor().newInstance());
    }
    if (DataWriterDAGBuilder.class.isAssignableFrom(writerClass)) {
      return (DataWriterDAGBuilder) writerClass.getConstructor().newInstance();
    }
    if (OutputFormatPlugin.class.isAssignableFrom(writerClass)) {
      return new PluginableOutputFormatDAGBuilder((OutputFormatPlugin<?>) writerClass.getConstructor().newInstance());
    }
    throw BitSailException.asBitSailException(CommonErrorCode.CONFIG_ERROR,
        "Writer " + writerClass.getName() + "class is not supported ");
  }

  @SuppressWarnings("unchecked")
  private static <T> Class<T> getWriterClass(BitSailConfiguration globalConfiguration,
                                             PackageManager packageManager) {
    String writerClassName = globalConfiguration.get(WriterOptions.WRITER_CLASS);
    LOG.info("Writer class name is {}", writerClassName);
    return (Class<T>) packageManager.loadDynamicLibrary(writerClassName, classLoader -> {
      try {
        return classLoader.loadClass(writerClassName);
      } catch (Exception e) {
        throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, e);
      }
    });
  }
}
