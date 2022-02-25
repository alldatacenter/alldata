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
package org.apache.ambari.server.controller.metrics.timeline.cache;

import java.util.Date;

import org.apache.hadoop.metrics2.sink.timeline.Precision;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;

/**
 * Wrapper object for metrics returned from AMS that includes the query time
 * window.
 */
public class TimelineMetricsCacheValue {
  private Long startTime;
  private Long endTime;
  // Same metrics could be requested across hosts
  private TimelineMetrics timelineMetrics = new TimelineMetrics();
  private Precision precision;

  public TimelineMetricsCacheValue(Long startTime, Long endTime, TimelineMetrics timelineMetrics, Precision precision) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.timelineMetrics = timelineMetrics;
    this.precision = precision;
  }

  public TimelineMetrics getTimelineMetrics() {
    return timelineMetrics;
  }

  /**
   * Map of metricName to metric values. Works on the assumption that metric
   * name is unique
   */
  public void setTimelineMetrics(TimelineMetrics timelineMetrics) {
    this.timelineMetrics = timelineMetrics;
  }

  public Long getStartTime() {
    return startTime;
  }

  public void setStartTime(Long startTime) {
    this.startTime = startTime;
  }

  public Long getEndTime() {
    return endTime;
  }

  public void setEndTime(Long endTime) {
    this.endTime = endTime;
  }

  private long getMillisecondsTime(long time) {
    if (time < 9999999999l) {
      return time * 1000;
    } else {
      return time;
    }
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("TimelineMetricsCacheValue {" +
      ", startTime = " + new Date(getMillisecondsTime(startTime)) +
      ", endTime = " + new Date(getMillisecondsTime(endTime)) +
      ", precision = " + precision +
      ", timelineMetrics =");

    for (TimelineMetric metric : timelineMetrics.getMetrics()) {
      sb.append(" { ");
      sb.append(metric.getMetricName());
      sb.append(", ");
      sb.append(metric.getHostName());
      sb.append(" # ");
      sb.append(metric.getMetricValues().size());
      sb.append(" }");
    }

    sb.append("}");
    return sb.toString();
  }

  public Precision getPrecision() {
    return precision;
  }

  public void setPrecision(Precision precision) {
    this.precision = precision;
  }
}
