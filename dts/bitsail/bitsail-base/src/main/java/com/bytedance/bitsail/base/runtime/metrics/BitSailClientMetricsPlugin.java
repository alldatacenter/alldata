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

package com.bytedance.bitsail.base.runtime.metrics;

import com.bytedance.bitsail.base.connector.reader.DataReaderDAGBuilder;
import com.bytedance.bitsail.base.connector.writer.DataWriterDAGBuilder;
import com.bytedance.bitsail.base.constants.ClientMetricName;
import com.bytedance.bitsail.base.execution.ProcessResult;
import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.manager.BitSailMetricManager;
import com.bytedance.bitsail.base.runtime.RuntimePlugin;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.Pair;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class BitSailClientMetricsPlugin extends RuntimePlugin {

  private MetricManager metricManager;
  private long startTime;
  private boolean jobSuccess = false;

  public static <T> String getWrappedName(List<T> operators, Function<T, String> getName) {
    String wrappedName = operators.stream().map(getName).collect(Collectors.joining(","));
    if (operators.size() == 1) {
      return wrappedName;
    }
    return "[" + wrappedName + "]";
  }

  @Override
  public void configure(BitSailConfiguration commonConfiguration,
                        List<DataReaderDAGBuilder> dataReaderDAGBuilders,
                        List<DataWriterDAGBuilder> dataWriterDAGBuilders) {
    String groupName = "client";

    String inputName = getWrappedName(dataReaderDAGBuilders, DataReaderDAGBuilder::getReaderName);
    String outputName = getWrappedName(dataWriterDAGBuilders, DataWriterDAGBuilder::getWriterName);
    List<Pair<String, String>> dimensions = ImmutableList.of(
        Pair.newPair("source", inputName),
        Pair.newPair("target", outputName)
    );

    this.metricManager = new BitSailMetricManager(
        commonConfiguration,
        groupName,
        Thread.currentThread().isDaemon(),
        dimensions);
  }

  @Override
  public void start() {
    this.metricManager.start();
    this.startTime = System.currentTimeMillis();
    metricManager.recordCounter(ClientMetricName.METRIC_JOB_START.getName());
  }

  @Override
  public void onSuccessComplete(ProcessResult<?> result) {
    jobSuccess = true;
    metricManager.recordCounter(ClientMetricName.METRIC_JOB_SUCCESS_INPUT_RECORD_COUNT.getName(), result.getJobSuccessInputRecordCount());
    metricManager.recordCounter(ClientMetricName.METRIC_JOB_SUCCESS_INPUT_RECORD_BYTES.getName(), result.getJobSuccessInputRecordBytes());
    metricManager.recordCounter(ClientMetricName.METRIC_JOB_FAILED_INPUT_RECORD_COUNT.getName(), result.getJobFailedInputRecordCount());

    metricManager.recordCounter(ClientMetricName.METRIC_JOB_SUCCESS_OUTPUT_RECORD_COUNT.getName(), result.getJobSuccessOutputRecordCount());
    metricManager.recordCounter(ClientMetricName.METRIC_JOB_SUCCESS_OUTPUT_RECORD_BYTES.getName(), result.getJobSuccessOutputRecordBytes());
    metricManager.recordCounter(ClientMetricName.METRIC_JOB_FAILED_OUTPUT_RECORD_COUNT.getName(), result.getJobFailedOutputRecordCount());

    metricManager.recordCounter(ClientMetricName.METRIC_JOB_TASK_RETRY_COUNT.getName(), result.getTaskRetryCount());
  }

  @Override
  public void close() {
    reportFinalStatus();
    long duration = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime);
    metricManager.recordCounter(ClientMetricName.METRIC_JOB_DURATION.getName(), duration);
    try {
      metricManager.close();
    } catch (Exception e) {
      log.warn("Close metric plugin error. ", e);
    }
  }

  private void reportFinalStatus() {
    metricManager.recordCounter(
        jobSuccess ? ClientMetricName.METRIC_JOB_SUCCESS.getName() : ClientMetricName.METRIC_JOB_FAILED.getName(), 1);
  }

  @VisibleForTesting
  public List<Pair<String, String>> getAllMetricTags() {
    return metricManager.getAllMetricDimensions();
  }
}
