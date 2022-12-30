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

package com.bytedance.bitsail.base.runtime.progress;

import com.bytedance.bitsail.base.connector.reader.DataReaderDAGBuilder;
import com.bytedance.bitsail.base.connector.writer.DataWriterDAGBuilder;
import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.progress.JobProgress;
import com.bytedance.bitsail.base.progress.NoOpJobProgress;
import com.bytedance.bitsail.base.runtime.RuntimePlugin;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.CommonOptions;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Objects;

@NoArgsConstructor
public class JobProgressPlugin extends RuntimePlugin {

  private static final Logger LOG = LoggerFactory.getLogger(JobProgressPlugin.class);

  private JobProgress progressReporter;

  @Override
  public void configure(BitSailConfiguration commonConfiguration,
                        List<DataReaderDAGBuilder> readerDAGBuilders,
                        List<DataWriterDAGBuilder> writerDAGBuilders) {
    progressReporter = null;
    if (!commonConfiguration.get(CommonOptions.SHOW_JOB_PROGRESS)) {
      return;
    }

    String jobProgressType = commonConfiguration.get(CommonOptions.JOB_PROGRESS_TYPE);
    SupportedJobProgressType type = SupportedJobProgressType.valueOf(jobProgressType.toUpperCase());
    try {
      progressReporter = type.getInstance(commonConfiguration);
    } catch (Exception e) {
      LOG.error("failed to initialize job progress plugin.");
      throw BitSailException.asBitSailException(CommonErrorCode.RUNTIME_ERROR, e);
    }
  }

  @Override
  public void start() {
    if (Objects.nonNull(progressReporter)) {
      progressReporter.start();
    }
  }

  @Override
  public void onSuccessComplete(ProcessResult<?> result) {
    if (Objects.nonNull(progressReporter)) {
      progressReporter.onSuccessComplete(result);
    }
  }

  @Override
  public void close() {
    if (Objects.nonNull(progressReporter)) {
      progressReporter.close();
    }
  }

  @AllArgsConstructor
  public enum SupportedJobProgressType {
    NO_OP("com.bytedance.bitsail.base.progress.NoOpJobProgress") {
      @Override
      public JobProgress getInstance(BitSailConfiguration commonConfiguration) throws Exception {
        return new NoOpJobProgress();
      }
    },

    FLINK_REST("com.bytedance.bitsail.component.progress.FlinkJobProgress") {
      @Override
      public JobProgress getInstance(BitSailConfiguration commonConfiguration) throws Exception {
        return createInstance(commonConfiguration);
      }
    };

    private final String className;

    abstract JobProgress getInstance(BitSailConfiguration commonConfiguration) throws Exception;

    JobProgress createInstance(BitSailConfiguration commonConfiguration) throws Exception {
      LOG.info("Job progress type is {} and class name is {}.", this.name(), className);
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
      Constructor<?> constructor = classLoader.loadClass(className).getConstructor(BitSailConfiguration.class);
      return (JobProgress) constructor.newInstance(commonConfiguration);
    }
  }
}
