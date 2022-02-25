/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.metrics.timeline.cache;

import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.pool.Size;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.impl.DefaultSizeOfEngine;
import net.sf.ehcache.pool.sizeof.ReflectionSizeOf;
import net.sf.ehcache.pool.sizeof.SizeOf;

/**
 * Cache sizing engine that reduces reflective calls over the Object graph to
 * find total Heap usage.
 */
public class TimelineMetricsCacheSizeOfEngine implements SizeOfEngine {

  private final static Logger LOG = LoggerFactory.getLogger(TimelineMetricsCacheSizeOfEngine.class);
  public static final int DEFAULT_MAX_DEPTH = 1000;
  public static final boolean DEFAULT_ABORT_WHEN_MAX_DEPTH_EXCEEDED = false;

  private SizeOfEngine underlying = null;
  SizeOf reflectionSizeOf = new ReflectionSizeOf();

  // Optimizations
  private volatile long timelineMetricPrimitivesApproximation = 0;

  private long sizeOfMapEntry;
  private long sizeOfMapEntryOverhead;

  private TimelineMetricsCacheSizeOfEngine(SizeOfEngine underlying) {
    this.underlying = underlying;
  }

  public TimelineMetricsCacheSizeOfEngine() {
    this(new DefaultSizeOfEngine(DEFAULT_MAX_DEPTH, DEFAULT_ABORT_WHEN_MAX_DEPTH_EXCEEDED));

    this.sizeOfMapEntry = reflectionSizeOf.sizeOf(new Long(1)) +
      reflectionSizeOf.sizeOf(new Double(2.0));

    //SizeOfMapEntryOverhead = SizeOfMapWithOneEntry - (SizeOfEmptyMap + SizeOfOneEntry)
    TreeMap<Long, Double> map = new TreeMap<>();
    long emptyMapSize = reflectionSizeOf.sizeOf(map);
    map.put(new Long(1), new Double(2.0));
    long sizeOfMapOneEntry = reflectionSizeOf.deepSizeOf(DEFAULT_MAX_DEPTH, DEFAULT_ABORT_WHEN_MAX_DEPTH_EXCEEDED, map).getCalculated();
    this.sizeOfMapEntryOverhead =  sizeOfMapOneEntry - (emptyMapSize + this.sizeOfMapEntry);

    LOG.info("Creating custom sizeof engine for TimelineMetrics.");
  }

  @Override
  public Size sizeOf(Object key, Object value, Object container) {
    try {
      LOG.debug("BEGIN - Sizeof, key: {}, value: {}", key, value);

      long size = 0;

      if (key instanceof TimelineAppMetricCacheKey) {
        size += getTimelineMetricCacheKeySize((TimelineAppMetricCacheKey) key);
      }

      if (value instanceof TimelineMetricsCacheValue) {
        size += getTimelineMetricCacheValueSize((TimelineMetricsCacheValue) value);
      }
      // Mark size as not being exact
      return new Size(size, false);
    } finally {
      LOG.debug("END - Sizeof, key: {}", key);
    }
  }

  private long getTimelineMetricCacheKeySize(TimelineAppMetricCacheKey key) {
    long size = reflectionSizeOf.sizeOf(key.getAppId());
    size += key.getMetricNames() != null && !key.getMetricNames().isEmpty() ?
      reflectionSizeOf.deepSizeOf(1000, false, key.getMetricNames()).getCalculated() : 0;
    size += key.getSpec() != null ?
      reflectionSizeOf.deepSizeOf(1000, false, key.getSpec()).getCalculated() : 0;
    size += key.getHostNames() != null ?
      reflectionSizeOf.deepSizeOf(1000, false, key.getHostNames()).getCalculated() : 0;
    // 4 fixed longs of @TemporalInfo + reference
    size += 40;
    size += 8; // Object overhead

    return size;
  }

  private long getTimelineMetricCacheValueSize(TimelineMetricsCacheValue value) {
    long size = 16; // startTime + endTime
    TimelineMetrics metrics = value.getTimelineMetrics();
    size += 8; // Object reference

    if (metrics != null) {
      for (TimelineMetric metric : metrics.getMetrics()) {

        if (timelineMetricPrimitivesApproximation == 0) {
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getMetricName());
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getAppId());
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getHostName());
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getInstanceId());
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getStartTime());
          timelineMetricPrimitivesApproximation += reflectionSizeOf.sizeOf(metric.getType());
          timelineMetricPrimitivesApproximation += 8; // Object overhead

          LOG.debug("timelineMetricPrimitivesApproximation bytes = {}", timelineMetricPrimitivesApproximation);
        }
        size += timelineMetricPrimitivesApproximation;

        Map<Long, Double> metricValues = metric.getMetricValues();
        if (metricValues != null && !metricValues.isEmpty()) {
          // Numeric wrapper: 12 bytes + 8 bytes Data type + 4 bytes alignment = 48 (Long, Double)
          // Tree Map: 12 bytes for header + 20 bytes for 5 object fields : pointers + 1 byte for flag = 40
          LOG.debug("Size of metric value: {}", (sizeOfMapEntry + sizeOfMapEntryOverhead) * metricValues.size());
          size += (sizeOfMapEntry + sizeOfMapEntryOverhead) * metricValues.size(); // Treemap size is O(1)
        }
      }
      LOG.debug("Total Size of metric values in cache: {}", size);
    }

    return size;
  }

  @Override
  public SizeOfEngine copyWith(int maxDepth, boolean abortWhenMaxDepthExceeded) {
    LOG.debug("Copying tracing sizeof engine, maxdepth: {}, abort: {}",
      maxDepth, abortWhenMaxDepthExceeded);

    return new TimelineMetricsCacheSizeOfEngine(
      underlying.copyWith(maxDepth, abortWhenMaxDepthExceeded));
  }
}