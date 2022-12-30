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
 *
 * Original Files: apache/flink(https://github.com/apache/flink)
 * Copyright: Copyright 2014-2022 The Apache Software Foundation
 * SPDX-License-Identifier: Apache License 2.0
 *
 * This file may have been modified by ByteDance Ltd. and/or its affiliates.
 */

package com.bytedance.bitsail.base.metrics.manager;

import com.bytedance.bitsail.base.constants.BaseMetricsNames;
import com.bytedance.bitsail.base.messenger.common.MessageType;
import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.MetricReporter;
import com.bytedance.bitsail.base.metrics.Scheduled;
import com.bytedance.bitsail.base.metrics.ScheduledMetricReporterWrap;
import com.bytedance.bitsail.base.metrics.reporter.MetricReporterFactory;
import com.bytedance.bitsail.common.BitSailException;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.util.Pair;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Abstract {@link MetricManager} that contains key functionality for adding metrics and groups.
 *
 * <p><b>IMPORTANT IMPLEMENTATION NOTE</b>
 *
 * <p>This class uses locks for adding and removing metrics objects. This is done to
 * prevent resource leaks in the presence of concurrently closing a group and adding
 * metrics
 *
 * <p>An AbstractMetricGroup can be {@link #close() closed}. Upon closing, the group de-register all metrics
 * from any metrics reporter and any internal maps. Note that even closed metrics groups
 * return Counters, Gauges, etc to the code, to prevent exceptions in the monitored code.
 * These metrics simply do not get reported any more, when created on a closed group.
 */
@Slf4j
public class BitSailMetricManager implements MetricManager {
  private static final int DEFAULT_MEASUREMENT_WINDOW_SIZE = 65536;
  protected final MetricReporter metricReporter;
  private final ConcurrentMap<String, Metric> metrics = new ConcurrentHashMap<>();
  private final List<Pair<String, String>> dimensions;
  private final String groupName;
  /**
   * Flag indicating whether this group has been closed.
   */
  private volatile boolean closed;

  public BitSailMetricManager(BitSailConfiguration configuration, String groupName) {
    this(configuration, groupName, false);
  }

  public BitSailMetricManager(BitSailConfiguration configuration,
                              String groupName,
                              boolean daemon) {
    this(configuration, groupName, daemon, Collections.EMPTY_LIST);
  }

  public BitSailMetricManager(BitSailConfiguration metricConfiguration,
                              String metricGroup,
                              boolean daemon,
                              List<Pair<String, String>> dimensions) {
    MetricReporter metricReporter = MetricReporterFactory.getMetricReporter(metricConfiguration);
    if (metricReporter instanceof Scheduled) {
      this.metricReporter = new ScheduledMetricReporterWrap(metricReporter, daemon);
    } else {
      this.metricReporter = metricReporter;
    }
    this.metricReporter.open(metricConfiguration);
    this.dimensions = getDefaultDimensions(metricConfiguration);
    this.dimensions.addAll(dimensions);
    this.groupName = metricGroup;
  }

  @Override
  public void start() {
    this.metricReporter.report();
  }

  @Override
  public List<Pair<String, String>> getAllMetricDimensions() {
    return dimensions;
  }

  @Override
  public void close() {
    synchronized (this) {
      if (!closed) {
        metricReporter.close();
        closed = true;
        for (Map.Entry<String, Metric> metricEntry : metrics.entrySet()) {
          metricReporter.notifyOfRemovedMetric(metricEntry.getValue(), metricEntry.getKey(), this);
        }
        metrics.clear();
      }
    }
  }

  public final boolean isClosed() {
    return closed;
  }

  @Override
  public Counter recordCounter(String name) {
    return recordCounter(name, 1);
  }

  @Override
  public Counter recordCounter(String name, long count) {
    Counter counter = getOrAddCounter(name);
    counter.inc(count);
    return counter;
  }

  private Counter getOrAddCounter(String name) {
    Counter counter;
    if (metrics.containsKey(name)) {
      Metric metric = metrics.get(name);
      if (metric instanceof Counter) {
        counter = (Counter) metric;
        return counter;
      } else {
        log.warn("Metric {} has registry as {}, we will registry it with {} again.",
            name, metric.getClass().getName(), Counter.class.getName());
        removeMetric(name);
      }
    }
    counter = new Counter();
    addMetric(name, counter);
    return counter;
  }

  @Override
  public <T> T recordTimer(String name, Supplier<T> event) {
    Timer timer = getOrAddTimer(name);
    return timer.timeSupplier(event);
  }

  @Override
  public Supplier<CallTracer> recordTimer(String name) {
    Counter throughput = getOrAddCounter(name + ".throughput");
    Timer latency = getOrAddTimer(name + ".latency");
    return () -> new CallTracer(throughput, latency);
  }

  /**
   * record timer latency
   *
   * @param name    timer name
   * @param latency latency
   */
  @Override
  public Timer recordTimer(String name, long latency) {
    Timer timer = getOrAddTimer(name);
    timer.update(latency, TimeUnit.MILLISECONDS);
    return timer;
  }

  private Timer getOrAddTimer(String name) {
    if (metrics.containsKey(name)) {
      Metric metric = metrics.get(name);
      if (metric instanceof Timer) {
        return (Timer) metric;
      } else {
        log.warn("Metric {} has registry as {}, we will registry it with {} again.",
            name, metric.getClass().getName(), Meter.class.getName());
        removeMetric(name);
      }
    }
    Timer timer = new Timer(new LockFreeSlidingWindowReservoir(DEFAULT_MEASUREMENT_WINDOW_SIZE));
    addMetric(name, timer);
    return timer;
  }

  @Override
  public <T, G extends Gauge<T>> G recordGauge(String name, G gauge) {
    if (metrics.containsKey(name)) {
      Metric metric = metrics.get(name);
      if (metric.equals(gauge)) {
        return gauge;
      }
      removeMetric(name);
    }
    addMetric(name, gauge);
    return gauge;
  }

  @Override
  public void reportRecord(long size, MessageType recordType) {
    switch (recordType) {
      case SUCCESS:
        recordCounter(BaseMetricsNames.RECORD_SUCCESS_COUNT);
        recordCounter(BaseMetricsNames.RECORD_SUCCESS_BYTES, size);
        break;
      case FAILED:
        recordCounter(BaseMetricsNames.RECORD_FAILED_COUNT);
        break;
      default:
        throw BitSailException.asBitSailException(CommonErrorCode.INTERNAL_ERROR, "Found unsupported enum: " + recordType.name());
    }
  }

  @Override
  public void removeMetric(String name) {
    synchronized (this) {
      Metric metric = metrics.remove(name);
      if (metric != null) {
        metricReporter.notifyOfRemovedMetric(metric, name, this);
      }
    }
  }

  /**
   * Adds the given metric to the group and registers it at the registry, if the group
   * is not yet closed, and if no metric with the same name has been registered before.
   *
   * @param name   the name to register the metric under
   * @param metric the metric to register
   */
  protected void addMetric(String name, Metric metric) {
    if (metric == null) {
      log.warn("Ignoring attempted registration of a metric due to being null for name {}.", name);
      return;
    }
    // add the metric only if the group is still open
    synchronized (this) {
      if (!closed) {
        // immediately put without a 'contains' check to optimize the common case (no collision)
        // collisions are resolved later
        Metric prior = metrics.put(name, metric);

        // check for collisions with other metric names
        if (prior == null) {
          // no other metric with this name yet
          if (metricReporter != null) {
            metricReporter.notifyOfAddedMetric(metric, name, this);
          } else {
            log.info("Metric reporter is null, we will skip add metric to metric reporter!");
          }
        } else {
          // we had a collision. put back the original value
          metrics.put(name, prior);

          // we warn here, rather than failing, because metrics are tools that should not fail the
          // program when used incorrectly
          log.warn("Name collision: Group already contains a Metric with the name '"
              + name + "'. Metric will not be reported.");
        }
      }
    }
  }

  @Override
  public void removeAllMetric() {
    for (String name : metrics.keySet()) {
      removeMetric(name);
    }
    this.metrics.clear();
  }

  @Override
  public String getMetricIdentifier(String metricName) {
    String metricNameWithGroup;
    if (StringUtils.isEmpty(groupName) || groupName.endsWith(".")) {
      metricNameWithGroup = groupName + metricName;
    } else {
      metricNameWithGroup = groupName + "." + metricName;
    }
    return metricNameWithGroup + getAllMetricDimensions().stream()
        .map(metricTag -> metricTag.getFirst() + "=" + metricTag.getSecond())
        .collect(Collectors.joining(",", "{", "}"));
  }
}
