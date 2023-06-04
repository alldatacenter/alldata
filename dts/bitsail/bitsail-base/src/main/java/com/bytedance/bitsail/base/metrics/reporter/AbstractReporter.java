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

package com.bytedance.bitsail.base.metrics.reporter;

import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.base.metrics.MetricReporter;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public abstract class AbstractReporter implements MetricReporter {

  protected final Map<Gauge<?>, String> gauges = new ConcurrentHashMap<>();
  protected final Map<Counter, String> counters = new ConcurrentHashMap<>();
  protected final Map<Histogram, String> histograms = new ConcurrentHashMap<>();
  protected final Map<Meter, String> meters = new ConcurrentHashMap<>();
  protected final Map<Timer, String> timers = new ConcurrentHashMap<>();

  @Override
  public void notifyOfAddedMetric(Metric metric, String metricName, MetricManager group) {
    final String name = group.getMetricIdentifier(metricName);

    synchronized (this) {
      if (metric instanceof Counter) {
        counters.put((Counter) metric, name);
      } else if (metric instanceof Gauge) {
        gauges.put((Gauge<?>) metric, name);
      } else if (metric instanceof Histogram) {
        histograms.put((Histogram) metric, name);
      } else if (metric instanceof Meter) {
        meters.put((Meter) metric, name);
      } else if (metric instanceof Timer) {
        timers.put((Timer) metric, name);
      } else {
        log.warn("Cannot add unknown metric type {}. This indicates that the reporter "
            + "does not support this metric type.", metric.getClass().getName());
      }
    }
  }

  @Override
  public void notifyOfRemovedMetric(Metric metric, String metricName, MetricManager group) {
    synchronized (this) {
      if (metric instanceof Counter) {
        counters.remove(metric);
      } else if (metric instanceof Gauge) {
        gauges.remove(metric);
      } else if (metric instanceof Histogram) {
        histograms.remove(metric);
      } else if (metric instanceof Meter) {
        meters.remove(metric);
      } else if (metric instanceof Timer) {
        timers.remove(metric);
      } else {
        log.warn("Cannot remove unknown metric type {}. This indicates that the reporter "
            + "does not support this metric type.", metric.getClass().getName());
      }
    }
  }
}
