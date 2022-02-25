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

import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration.Strategy;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration.MaxDepthExceededBehavior;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

/**
 * Cache implementation that provides ability to perform incremental reads
 * from Metrics backend and reduce the amount of calls between Ambari and the
 * Metrics backend.
 */
@Singleton
public class TimelineMetricCacheProvider {
  private TimelineMetricCache timelineMetricsCache;
  private volatile boolean isCacheInitialized = false;
  public static final String TIMELINE_METRIC_CACHE_MANAGER_NAME = "timelineMetricCacheManager";
  public static final String TIMELINE_METRIC_CACHE_INSTANCE_NAME = "timelineMetricCache";

  Configuration configuration;
  TimelineMetricCacheEntryFactory cacheEntryFactory;

  private final static Logger LOG = LoggerFactory.getLogger(TimelineMetricCacheProvider.class);

  @Inject
  public TimelineMetricCacheProvider(Configuration configuration,
                                     TimelineMetricCacheEntryFactory cacheEntryFactory) {
    this.configuration = configuration;
    this.cacheEntryFactory = cacheEntryFactory;
  }

  private synchronized void initializeCache() {
    // Check in case of contention to avoid ObjectExistsException
    if (isCacheInitialized) {
      return;
    }

    System.setProperty("net.sf.ehcache.skipUpdateCheck", "true");
    if (configuration.useMetricsCacheCustomSizingEngine()) {
      // Use custom sizing engine to speed cache sizing calculations
      System.setProperty("net.sf.ehcache.sizeofengine." + TIMELINE_METRIC_CACHE_MANAGER_NAME,
        "org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricsCacheSizeOfEngine");
    }

    net.sf.ehcache.config.Configuration managerConfig =
      new net.sf.ehcache.config.Configuration();
    managerConfig.setName(TIMELINE_METRIC_CACHE_MANAGER_NAME);

    // Set max heap available to the cache manager
    managerConfig.setMaxBytesLocalHeap(configuration.getMetricsCacheManagerHeapPercent());

    //Create a singleton CacheManager using defaults
    CacheManager manager = CacheManager.create(managerConfig);

    LOG.info("Creating Metrics Cache with timeouts => ttl = " +
      configuration.getMetricCacheTTLSeconds() + ", idle = " +
      configuration.getMetricCacheIdleSeconds());

    // Create a Cache specifying its configuration.
    CacheConfiguration cacheConfiguration = createCacheConfiguration();
    Cache cache = new Cache(cacheConfiguration);

    // Decorate with UpdatingSelfPopulatingCache
    timelineMetricsCache = new TimelineMetricCache(cache, cacheEntryFactory);

    LOG.info("Registering metrics cache with provider: name = " +
      cache.getName() + ", guid: " + cache.getGuid());

    manager.addCache(timelineMetricsCache);

    isCacheInitialized = true;
  }

  // Having this as a separate public method for testing/mocking purposes
  public CacheConfiguration createCacheConfiguration() {

    CacheConfiguration cacheConfiguration = new CacheConfiguration()
      .name(TIMELINE_METRIC_CACHE_INSTANCE_NAME)
      .timeToLiveSeconds(configuration.getMetricCacheTTLSeconds()) // 1 hour
      .timeToIdleSeconds(configuration.getMetricCacheIdleSeconds()) // 5 minutes
      .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
      .sizeOfPolicy(new SizeOfPolicyConfiguration() // Set sizeOf policy to continue on max depth reached - avoid OOM
        .maxDepth(10000)
        .maxDepthExceededBehavior(MaxDepthExceededBehavior.CONTINUE))
      .eternal(false)
      .persistence(new PersistenceConfiguration()
        .strategy(Strategy.NONE.name()));

    return cacheConfiguration;
  }

  /**
   * Return an instance of a Ehcache
   * @return @TimelineMetricCache or null if caching is disabled through config.
   */
  public TimelineMetricCache getTimelineMetricsCache() {
    if (configuration.isMetricsCacheDisabled()) {
      return null;
    }

    if (!isCacheInitialized) {
      initializeCache();
    }
    return timelineMetricsCache;
  }

}
