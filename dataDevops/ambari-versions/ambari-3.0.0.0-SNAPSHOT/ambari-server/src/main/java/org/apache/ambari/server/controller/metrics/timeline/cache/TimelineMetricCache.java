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

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.constructs.blocking.LockTimeoutException;
import net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory;
import net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache;
import net.sf.ehcache.statistics.StatisticsGateway;

public class TimelineMetricCache extends UpdatingSelfPopulatingCache {

  private final static Logger LOG = LoggerFactory.getLogger(TimelineMetricCache.class);
  private static AtomicInteger printCacheStatsCounter = new AtomicInteger(0);

  /**
   * Creates a SelfPopulatingCache.
   *
   * @param cache @Cache
   * @param factory @CacheEntryFactory
   */
  public TimelineMetricCache(Ehcache cache, UpdatingCacheEntryFactory factory) throws CacheException {
    super(cache, factory);
  }

  /**
   * Get metrics for an app grouped by the requested @TemporalInfo which is a
   * part of the @TimelineAppMetricCacheKey
   * @param key @TimelineAppMetricCacheKey
   * @return @org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics
   */
  public TimelineMetrics getAppTimelineMetricsFromCache(TimelineAppMetricCacheKey key) throws IllegalArgumentException, IOException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Fetching metrics with key: {}", key);
    }

    // Make sure key is valid
    validateKey(key);

    Element element = null;
    try {
      element = get(key);
    } catch (LockTimeoutException le) {
      // Ehcache masks the Socket Timeout to look as a LockTimeout
      Throwable t = le.getCause();
      if (t instanceof CacheException) {
        t = t.getCause();
        if (t instanceof SocketTimeoutException) {
          throw new SocketTimeoutException(t.getMessage());
        }
        if (t instanceof ConnectException) {
          throw new ConnectException(t.getMessage());
        }
      }
    }

    TimelineMetrics timelineMetrics = new TimelineMetrics();
    if (element != null && element.getObjectValue() != null) {
      TimelineMetricsCacheValue value = (TimelineMetricsCacheValue) element.getObjectValue();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Returning value from cache: {}", value);
      }
      timelineMetrics = value.getTimelineMetrics();
    }

    if (LOG.isDebugEnabled()) {
      // Print stats every 100 calls - Note: Supported in debug mode only
      if (printCacheStatsCounter.getAndIncrement() == 0) {
        StatisticsGateway statistics = this.getStatistics();
        LOG.debug("Metrics cache stats => \n, Evictions = {}, Expired = {}, Hits = {}, Misses = {}, Hit ratio = {}, Puts = {}, Size in MB = {}",
          statistics.cacheEvictedCount(), statistics.cacheExpiredCount(), statistics.cacheHitCount(), statistics.cacheMissCount(), statistics.cacheHitRatio(),
          statistics.cachePutCount(), statistics.getLocalHeapSizeInBytes() / 1048576);
      } else {
        printCacheStatsCounter.compareAndSet(100, 0);
      }
    }

    return timelineMetrics;
  }

  /**
   * Set new time bounds on the cache key so that update can use the new
   * query window. We do this quietly which means regular get/update logic is
   * not invoked.
   */
  @Override
  public Element get(Object key) throws LockTimeoutException {
    Element element = this.getQuiet(key);
    if (element != null) {
      if (LOG.isTraceEnabled()) {
        LOG.trace("key : {}", element.getObjectKey());
        LOG.trace("value : {}", element.getObjectValue());
      }

      // Set new time boundaries on the key
      TimelineAppMetricCacheKey existingKey = (TimelineAppMetricCacheKey) element.getObjectKey();

      LOG.debug("Existing temporal info: {} for : {}", existingKey.getTemporalInfo(), existingKey.getMetricNames());

      TimelineAppMetricCacheKey newKey = (TimelineAppMetricCacheKey) key;
      existingKey.setTemporalInfo(newKey.getTemporalInfo());

      LOG.debug("New temporal info: {} for : {}", newKey.getTemporalInfo(), existingKey.getMetricNames());

      if (existingKey.getSpec() == null || !existingKey.getSpec().equals(newKey.getSpec())) {
        existingKey.setSpec(newKey.getSpec());
        LOG.debug("New spec: {} for : {}", newKey.getSpec(), existingKey.getMetricNames());
      }
    }

    return super.get(key);
  }

  private void validateKey(TimelineAppMetricCacheKey key) throws IllegalArgumentException {
    StringBuilder msg = new StringBuilder("Invalid metric key requested.");
    boolean throwException = false;

    if (key.getTemporalInfo() == null) {
      msg.append(" No temporal info provided.");
      throwException = true;
    }

    if (key.getSpec() == null) {
      msg.append(" Missing call spec for metric request.");
    }

    if (throwException) {
      throw new IllegalArgumentException(msg.toString());
    }
  }
}
