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

package com.bytedance.bitsail.base.metrics;

import com.bytedance.bitsail.base.messenger.common.MessageType;
import com.bytedance.bitsail.base.metrics.manager.CallTracer;
import com.bytedance.bitsail.base.version.VersionHolder;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.util.Pair;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A MetricGroup is a named container for {@link Metric Metrics}.
 *
 * <p>Instances of this class can be used to register new metrics and
 *
 * <p>A MetricGroup is uniquely identified by its tags and name.
 */
public interface MetricManager extends Closeable {
  static final String DEFAULT_DIMENSION_JOB_ID = "job";
  static final String DEFAULT_DIMENSION_COMMIT_ID = "git_commit";
  static final String DEFAULT_DIMENSION_VERSION = "version";

  void start();

  /**
   * add 1 to a counter
   * if counter does not exist, we will create it first
   *
   * @param name counter name
   */
  Counter recordCounter(String name);

  /**
   * add count to a counter
   * if counter does not exist, we will create it first
   *
   * @param name counter name
   */
  Counter recordCounter(String name, long count);

  /**
   * record timer
   *
   * @param name timer name
   * @return the supplier to record
   */
  <T> T recordTimer(String name, Supplier<T> event);

  Supplier<CallTracer> recordTimer(String name);

  /**
   * record timer latency
   *
   * @param name    timer name
   * @param latency latency
   */
  Timer recordTimer(String name, long latency);

  /**
   * record gauge
   *
   * @param name  gauge name
   * @param gauge gauge
   * @param <T>   gauge return type
   * @param <G>   gauge type
   */
  <T, G extends Gauge<T>> G recordGauge(String name, G gauge);

  void reportRecord(long size, MessageType recordType);

  /**
   * get all metric tag
   *
   * @return metric tag
   */
  List<Pair<String, String>> getAllMetricDimensions();

  /**
   * Returns the fully qualified metric name with metric tag
   *
   * @param metricName metric name
   * @return fully qualified metric name
   */
  String getMetricIdentifier(String metricName);

  void removeAllMetric();

  void removeMetric(String name);

  default List<Pair<String, String>> getDefaultDimensions(BitSailConfiguration jobConfiguration) {
    String jobId = String.valueOf(jobConfiguration.get(CommonOptions.JOB_ID));
    List<Pair<String, String>> defaultDimensions = new ArrayList<>();
    defaultDimensions
        .add(Pair.newPair(DEFAULT_DIMENSION_JOB_ID, jobId));
    String commitId = VersionHolder.INSTANCE.getGitCommitId();
    String buildVersion = VersionHolder.INSTANCE.getBuildVersion();
    if (VersionHolder.isCommitIdValid(commitId)) {
      defaultDimensions.add(Pair.newPair(DEFAULT_DIMENSION_COMMIT_ID, commitId));
    }
    if (VersionHolder.isBuildVersionValid(buildVersion)) {
      defaultDimensions.add(Pair.newPair(DEFAULT_DIMENSION_VERSION, buildVersion));
    }
    return defaultDimensions;
  }
}
