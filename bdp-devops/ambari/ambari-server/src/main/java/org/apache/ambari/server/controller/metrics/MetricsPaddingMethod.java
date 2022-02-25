/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.metrics;

import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;

public class MetricsPaddingMethod {
  private final PADDING_STRATEGY strategy;
  public static final String ZERO_PADDING_PARAM = "params/padding";
  private static final long MINIMUM_STEP_INTERVAL = 999l; // ~ 1 second
  public enum PADDING_STRATEGY {
    ZEROS,
    NULLS,
    NONE
  }

  public MetricsPaddingMethod(PADDING_STRATEGY strategy) {
    this.strategy = strategy;
  }

  /**
   * Adds zero/null values towards the end of metrics sequence as well as the
   * beginning to support backward compatibility with Ganglia.
   * We use step if only a single datapoint is found.
   * It is assumed that @TimelineMetric.metricsValues are sorted in ascending
   * order and thee is no padding between the interval.
   *
   * @param metric AMS @TimelineMetric
   * @param temporalInfo @TemporalInfo Requested interval
   */
  public void applyPaddingStrategy(TimelineMetric metric, TemporalInfo temporalInfo) {
    if (strategy.equals(PADDING_STRATEGY.NONE) || temporalInfo == null) {
      return;
    }

    TreeMap<Long, Double> values = metric.getMetricValues();

    if (values==null || values.isEmpty()) {
      return;
    }

    long intervalStartTime = longToMillis(temporalInfo.getStartTime());
    long intervalEndTime = longToMillis(temporalInfo.getEndTime());
    long dataStartTime = longToMillis(values.firstKey());
    long dataEndTime = longToMillis(values.lastKey());

    long dataInterval = getTimelineMetricInterval(values, intervalStartTime, intervalEndTime);

    if (dataInterval == -1 || dataInterval < MINIMUM_STEP_INTERVAL) {
      dataInterval = temporalInfo.getStep() != null ? temporalInfo.getStep() : -1;
    }
    // Unable to determine what interval to use for padding
    if (dataInterval == -1) {
      return;
    }

    Double paddingValue = 0.0d;

    if (strategy.equals(PADDING_STRATEGY.NULLS)) {
      paddingValue = null;
    }
    // Pad before data interval
    for (long counter = intervalStartTime; counter < dataStartTime; counter += dataInterval) {
      // Until counter approaches or goes past dataStartTime : pad
      values.put(counter, paddingValue);
    }
    // Pad after data interval
    for (long counter = dataEndTime + dataInterval; counter <= intervalEndTime; counter += dataInterval) {
      values.put(counter, paddingValue);
    }
    // Put back new + old values
    metric.setMetricValues(values);
  }

  private long longToMillis(long time) {
    if (time < 9999999999l) {
      return time * 1000;
    }
    return time;
  }

  private long getTimelineMetricInterval(TreeMap<Long, Double> values, long startTime, long endTime) {

    Precision precision = Precision.getPrecision(startTime, endTime);
    long interval;

    if (precision.equals(Precision.DAYS)) {
      interval = TimeUnit.DAYS.toMillis(1);
    } else if (precision.equals(Precision.HOURS)) {
      interval = TimeUnit.HOURS.toMillis(1);
    } else if (precision.equals(Precision.MINUTES)) {
      interval = TimeUnit.MINUTES.toMillis(1);
    } else {
      //Precision = SECONDS.
      //More than 1 point.
      if (values != null && values.size() > 1) {
        Iterator<Long> tsValuesIterator = values.descendingKeySet().iterator();
        long lastValue = tsValuesIterator.next();
        long secondToLastValue = tsValuesIterator.next();
        interval =  Math.abs(lastValue - secondToLastValue);
      } else {
        // Only 1 point
        interval = -1;
      }
    }
    return interval;
  }
}
