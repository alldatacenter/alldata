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

package com.bytedance.bitsail.base.metrics.reporter.impl;

import com.bytedance.bitsail.base.metrics.Scheduled;
import com.bytedance.bitsail.base.metrics.reporter.AbstractReporter;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class LogMetricReporter extends AbstractReporter implements Scheduled {
  private static final Logger LOG = LoggerFactory.getLogger(LogMetricReporter.class);

  public static final String LOG_HEADER_GAUGES = "-- gauges --";
  public static final String LOG_HEADER_COUNTERS = "-- counters --";
  public static final String LOG_HEADER_HISTOGRAMS = "-- histograms --";
  public static final String LOG_HEADER_METERS = "-- counters --";
  public static final String LOG_HEADER_TIMERS = "-- timers --";
  private double rateFactor;
  private double durationFactor;

  @Override
  public void open(BitSailConfiguration configuration) {
    rateFactor = getDelayTimeUnit().toSeconds(1);
    durationFactor = 1.0 / getDelayTimeUnit().toNanos(1);
  }

  @Override
  public void close() {
  }

  @Override
  public void report() {
    if (gauges.size() > 0) {
      LOG.info(LOG_HEADER_GAUGES);
    }
    for (Map.Entry<Gauge<?>, String> entry : gauges.entrySet()) {
      reportGauge(entry.getKey(), entry.getValue());
    }

    if (counters.size() > 0) {
      LOG.info(LOG_HEADER_COUNTERS);
    }
    for (Map.Entry<Counter, String> entry : counters.entrySet()) {
      reportCounter(entry.getKey(), entry.getValue());
    }

    if (histograms.size() > 0) {
      LOG.info(LOG_HEADER_HISTOGRAMS);
    }
    for (Map.Entry<Histogram, String> entry : histograms.entrySet()) {
      reportHistogram(entry.getKey(), entry.getValue());
    }

    if (meters.size() > 0) {
      LOG.info(LOG_HEADER_METERS);
    }
    for (Map.Entry<Meter, String> entry : meters.entrySet()) {
      reportMeter(entry.getKey(), entry.getValue());
    }

    if (timers.size() > 0) {
      LOG.info(LOG_HEADER_TIMERS);
    }
    for (Map.Entry<Timer, String> entry : timers.entrySet()) {
      reportTimer(entry.getKey(), entry.getValue());
    }
  }

  private double convertRate(double rate) {
    return rateFactor * rate;
  }

  private double convertDuration(double duration) {
    return duration * durationFactor;
  }

  private void reportMeter(Meter meter, String name) {
    LOG.info("Meter {}'s count = {}", name, meter.getCount());
  }

  private void reportTimer(Timer timer, String name) {
    Snapshot snapshot = timer.getSnapshot();
    LOG.info("Timer {}'s count = {}", name, timer.getCount());
    LOG.info("Timer {}'s p999 = {}, unit = {}", name, convertDuration(snapshot.get999thPercentile()), getDelayTimeUnit());
  }

  private void reportCounter(Counter counter, String name) {
    LOG.info("Counter {}'s count = {}", name, counter.getCount());
  }

  private void reportGauge(Gauge<?> gauge, String name) {
    LOG.info("gauge {} value = {}", name, gauge.getValue());
  }

  private void reportHistogram(Histogram histogram, String name) {
    Snapshot snapshot = histogram.getSnapshot();
    LOG.info("Histogram {}'s count = {}", name, histogram.getCount());
    LOG.info("Histogram {}'s p999 = {}", name, snapshot.get999thPercentile());
  }
}
