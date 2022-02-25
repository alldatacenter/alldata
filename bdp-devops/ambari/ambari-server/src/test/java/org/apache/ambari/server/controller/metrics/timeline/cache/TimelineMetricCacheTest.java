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

import static org.apache.ambari.server.controller.metrics.timeline.cache.TimelineMetricCacheProvider.TIMELINE_METRIC_CACHE_INSTANCE_NAME;
import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createMockBuilder;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.getCurrentArguments;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.controller.internal.TemporalInfoImpl;
import org.apache.ambari.server.controller.metrics.timeline.MetricsRequestHelper;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetrics;
import org.apache.http.client.utils.URIBuilder;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Test;

import junit.framework.Assert;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.PersistenceConfiguration;
import net.sf.ehcache.config.SizeOfPolicyConfiguration;
import net.sf.ehcache.constructs.blocking.UpdatingCacheEntryFactory;
import net.sf.ehcache.constructs.blocking.UpdatingSelfPopulatingCache;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

public class TimelineMetricCacheTest {

  @After
  public void removeCacheInstance() {
    // Avoids Object Exists Exception on unit tests by adding a new cache for
    // every provider.
    CacheManager manager = CacheManager.getInstance();
    manager.removeAllCaches();
  }

  // General cache behavior demonstration
  @Test
  public void testSelfPopulatingCacheUpdates() throws Exception {
    UpdatingCacheEntryFactory cacheEntryFactory = createMock(UpdatingCacheEntryFactory.class);

    StringBuilder value = new StringBuilder("b");

    expect(cacheEntryFactory.createEntry("a")).andReturn(value);
    cacheEntryFactory.updateEntryValue("a", value);
    expectLastCall().andAnswer(new IAnswer<Object>() {
      @Override
      public Object answer() throws Throwable {
        String key = (String) getCurrentArguments()[0];
        StringBuilder value = (StringBuilder) getCurrentArguments()[1];
        System.out.println("key = " + key + ", value = " + value);
        value.append("c");
        return null;
      }
    });

    replay(cacheEntryFactory);

    // Need to set this due what seems like a bug in Ehcache 2.10.0, setting
    // it on the second cache instance results in a assertion error.
    // Since this is not out production use case, setting it here as well.
    net.sf.ehcache.config.Configuration managerConfig = new net.sf.ehcache.config.Configuration();
    managerConfig.setMaxBytesLocalHeap("10%");
    CacheManager manager = CacheManager.create(managerConfig);
    Cache cache = new Cache("test", 0, false, false, 10000, 10000);
    UpdatingSelfPopulatingCache testCache = new UpdatingSelfPopulatingCache(cache, cacheEntryFactory);
    manager.addCache(testCache);

    Assert.assertEquals("b", testCache.get("a").getObjectValue().toString());
    Assert.assertEquals("bc", testCache.get("a").getObjectValue().toString());

    verify(cacheEntryFactory);
  }

  private CacheConfiguration createTestCacheConfiguration(Configuration configuration) {

    CacheConfiguration cacheConfiguration = new CacheConfiguration()
      .name(TIMELINE_METRIC_CACHE_INSTANCE_NAME)
      .timeToLiveSeconds(configuration.getMetricCacheTTLSeconds()) // 1 hour
      .timeToIdleSeconds(configuration.getMetricCacheIdleSeconds()) // 5 minutes
      .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU)
      .sizeOfPolicy(new SizeOfPolicyConfiguration() // Set sizeOf policy to continue on max depth reached - avoid OOM
        .maxDepth(10000)
        .maxDepthExceededBehavior(SizeOfPolicyConfiguration.MaxDepthExceededBehavior.CONTINUE))
      .eternal(false)
      .persistence(new PersistenceConfiguration()
        .strategy(PersistenceConfiguration.Strategy.NONE.name()));

    cacheConfiguration.setMaxBytesLocalHeap(20*1024*1024l);
    return cacheConfiguration;
  }
  @Test
  public void testTimelineMetricCacheProviderGets() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getMetricCacheTTLSeconds()).andReturn(3600);
    expect(configuration.getMetricCacheIdleSeconds()).andReturn(100);
    expect(configuration.getMetricsCacheManagerHeapPercent()).andReturn("10%").anyTimes();

    replay(configuration);

    final long now = System.currentTimeMillis();

    TimelineMetrics metrics = new TimelineMetrics();

    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("cpu_user");
    timelineMetric.setAppId("app1");
    TreeMap<Long, Double> metricValues = new TreeMap<>();
    metricValues.put(now + 100, 1.0);
    metricValues.put(now + 200, 2.0);
    metricValues.put(now + 300, 3.0);
    timelineMetric.setMetricValues(metricValues);

    metrics.getMetrics().add(timelineMetric);

    TimelineMetricCacheEntryFactory cacheEntryFactory = createMock(TimelineMetricCacheEntryFactory.class);

    TimelineAppMetricCacheKey queryKey = new TimelineAppMetricCacheKey(
      Collections.singleton("cpu_user"),
      "app1",
      new TemporalInfoImpl(now, now + 1000, 1)
    );
    TimelineMetricsCacheValue value = new TimelineMetricsCacheValue(now, now + 1000, metrics, null);
    TimelineAppMetricCacheKey testKey = new TimelineAppMetricCacheKey(
      Collections.singleton("cpu_user"),
      "app1",
      new TemporalInfoImpl(now, now + 2000, 1)
    );

    expect(cacheEntryFactory.createEntry(anyObject())).andReturn(value);
    cacheEntryFactory.updateEntryValue(testKey, value);
    expectLastCall().once();

    replay(cacheEntryFactory);

    TimelineMetricCacheProvider cacheProvider = createMockBuilder(TimelineMetricCacheProvider.class)
      .addMockedMethod("createCacheConfiguration")
      .withConstructor(configuration, cacheEntryFactory)
      .createNiceMock();

    expect(cacheProvider.createCacheConfiguration()).andReturn(createTestCacheConfiguration(configuration)).anyTimes();
    replay(cacheProvider);

    TimelineMetricCache cache = cacheProvider.getTimelineMetricsCache();

    // call to get
    metrics = cache.getAppTimelineMetricsFromCache(queryKey);
    List<TimelineMetric> metricsList = metrics.getMetrics();
    Assert.assertEquals(1, metricsList.size());
    TimelineMetric metric = metricsList.iterator().next();
    Assert.assertEquals("cpu_user", metric.getMetricName());
    Assert.assertEquals("app1", metric.getAppId());
    Assert.assertSame(metricValues, metric.getMetricValues());

    // call to update with new key
    metrics = cache.getAppTimelineMetricsFromCache(testKey);
    metricsList = metrics.getMetrics();
    Assert.assertEquals(1, metricsList.size());
    Assert.assertEquals("cpu_user", metric.getMetricName());
    Assert.assertEquals("app1", metric.getAppId());
    Assert.assertSame(metricValues, metric.getMetricValues());

    verify(configuration, cacheEntryFactory);
  }

  @Test
  @SuppressWarnings("all")
  public void testCacheUpdateBoundsOnVariousRequestScenarios() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getMetricsRequestConnectTimeoutMillis()).andReturn(10000);
    expect(configuration.getMetricsRequestReadTimeoutMillis()).andReturn(10000);
    expect(configuration.getMetricsRequestIntervalReadTimeoutMillis()).andReturn(10000);
    // Disable buffer fudge factor
    expect(configuration.getMetricRequestBufferTimeCatchupInterval()).andReturn(0l);

    replay(configuration);

    TimelineMetricCacheEntryFactory factory =
      createMockBuilder(TimelineMetricCacheEntryFactory.class)
        .withConstructor(configuration).createMock();

    replay(factory);

    long now = System.currentTimeMillis();
    final long existingSeriesStartTime = now - (3600 * 1000); // now - 1 hour
    final long existingSeriesEndTime = now;

    // Regular timeseries overlap
    long requestedStartTime = existingSeriesStartTime + 60000; // + 1 min
    long requestedEndTime = existingSeriesEndTime + 60000; // + 1 min

    long newStartTime = factory.getRefreshRequestStartTime(existingSeriesStartTime,
      existingSeriesEndTime, requestedStartTime);

    long newEndTime = factory.getRefreshRequestEndTime(existingSeriesStartTime,
      existingSeriesEndTime, requestedEndTime);

    Assert.assertEquals(existingSeriesEndTime, newStartTime);
    Assert.assertEquals(requestedEndTime, newEndTime);

    // Disconnected timeseries graph
    requestedStartTime = existingSeriesEndTime + 60000; // end + 1 min
    requestedEndTime = existingSeriesEndTime + 60000 + 3600000; // + 1 min + 1 hour

    newStartTime = factory.getRefreshRequestStartTime(existingSeriesStartTime,
      existingSeriesEndTime, requestedStartTime);

    newEndTime = factory.getRefreshRequestEndTime(existingSeriesStartTime,
      existingSeriesEndTime, requestedEndTime);

    Assert.assertEquals(requestedStartTime, newStartTime);
    Assert.assertEquals(requestedEndTime, newEndTime);

    // Complete overlap
    requestedStartTime = existingSeriesStartTime - 60000; // - 1 min
    requestedEndTime = existingSeriesEndTime + 60000; // + 1 min

    newStartTime = factory.getRefreshRequestStartTime(existingSeriesStartTime,
      existingSeriesEndTime, requestedStartTime);

    newEndTime = factory.getRefreshRequestEndTime(existingSeriesStartTime,
      existingSeriesEndTime, requestedEndTime);

    Assert.assertEquals(requestedStartTime, newStartTime);
    Assert.assertEquals(requestedEndTime, newEndTime);

    // Timeseries in the past
    requestedStartTime = existingSeriesStartTime - 3600000 - 60000; // - 1 hour - 1 min
    requestedEndTime = existingSeriesStartTime - 60000; // start - 1 min

    newStartTime = factory.getRefreshRequestStartTime(existingSeriesStartTime,
      existingSeriesEndTime, requestedStartTime);

    newEndTime = factory.getRefreshRequestEndTime(existingSeriesStartTime,
      existingSeriesEndTime, requestedEndTime);

    Assert.assertEquals(requestedStartTime, newStartTime);
    Assert.assertEquals(requestedEndTime, newEndTime);

    // Timeseries overlap - no new request needed
    requestedStartTime = existingSeriesStartTime + 60000; // + 1 min
    requestedEndTime = existingSeriesEndTime - 60000; // - 1 min

    newStartTime = factory.getRefreshRequestStartTime(existingSeriesStartTime,
      existingSeriesEndTime, requestedStartTime);

    newEndTime = factory.getRefreshRequestEndTime(existingSeriesStartTime,
      existingSeriesEndTime, requestedEndTime);

    Assert.assertEquals(newStartTime, existingSeriesEndTime);
    Assert.assertEquals(newEndTime, existingSeriesStartTime);

    verify(configuration, factory);

  }

  @Test
  public void testTimelineMetricCacheTimeseriesUpdates() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getMetricsRequestConnectTimeoutMillis()).andReturn(10000);
    expect(configuration.getMetricsRequestReadTimeoutMillis()).andReturn(10000);
    expect(configuration.getMetricsRequestIntervalReadTimeoutMillis()).andReturn(10000);
    // Disable buffer fudge factor
    expect(configuration.getMetricRequestBufferTimeCatchupInterval()).andReturn(0l);

    replay(configuration);

    TimelineMetricCacheEntryFactory factory =
      createMockBuilder(TimelineMetricCacheEntryFactory.class)
        .withConstructor(configuration).createMock();

    replay(factory);

    long now = System.currentTimeMillis();

    // Existing values

    final TimelineMetric timelineMetric1 = new TimelineMetric();
    timelineMetric1.setMetricName("cpu_user");
    timelineMetric1.setAppId("app1");
    TreeMap<Long, Double> metricValues = new TreeMap<>();
    metricValues.put(now - 100, 1.0);
    metricValues.put(now - 200, 2.0);
    metricValues.put(now - 300, 3.0);
    timelineMetric1.setMetricValues(metricValues);
    final TimelineMetric timelineMetric2 = new TimelineMetric();
    timelineMetric2.setMetricName("cpu_nice");
    timelineMetric2.setAppId("app1");
    metricValues = new TreeMap<>();
    metricValues.put(now + 400, 1.0);
    metricValues.put(now + 500, 2.0);
    metricValues.put(now + 600, 3.0);
    timelineMetric2.setMetricValues(metricValues);

    TimelineMetrics existingMetrics = new TimelineMetrics();
    existingMetrics.getMetrics().add(timelineMetric1);
    existingMetrics.getMetrics().add(timelineMetric2);

    TimelineMetricsCacheValue existingMetricValue = new TimelineMetricsCacheValue(
      now - 1000, now + 1000, existingMetrics, null);

    // New values
    TimelineMetrics newMetrics = new TimelineMetrics();
    TimelineMetric timelineMetric3 = new TimelineMetric();
    timelineMetric3.setMetricName("cpu_user");
    timelineMetric3.setAppId("app1");
    metricValues = new TreeMap<>();
    metricValues.put(now + 1400, 1.0);
    metricValues.put(now + 1500, 2.0);
    metricValues.put(now + 1600, 3.0);
    timelineMetric3.setMetricValues(metricValues);
    newMetrics.getMetrics().add(timelineMetric3);

    factory.updateTimelineMetricsInCache(newMetrics, existingMetricValue,
      now, now + 2000, false);

    Assert.assertEquals(2, existingMetricValue.getTimelineMetrics().getMetrics().size());

    TimelineMetric newMetric1 = null;
    TimelineMetric newMetric2 = null;

    for (TimelineMetric metric : existingMetricValue.getTimelineMetrics().getMetrics()) {
      if (metric.getMetricName().equals("cpu_user")) {
        newMetric1 = metric;
      }
      if (metric.getMetricName().equals("cpu_nice")) {
        newMetric2 = metric;
      }
    }

    Assert.assertNotNull(newMetric1);
    Assert.assertNotNull(newMetric2);
    Assert.assertEquals(3, newMetric1.getMetricValues().size());
    Assert.assertEquals(3, newMetric2.getMetricValues().size());
    Map<Long, Double> newMetricsMap = newMetric1.getMetricValues();
    Iterator<Long> metricKeyIterator = newMetricsMap.keySet().iterator();
    Assert.assertEquals(now + 1400, metricKeyIterator.next().longValue());
    Assert.assertEquals(now + 1500, metricKeyIterator.next().longValue());
    Assert.assertEquals(now + 1600, metricKeyIterator.next().longValue());

    verify(configuration, factory);
  }

  @Test
  public void testEqualsOnKeys() {
    long now = System.currentTimeMillis();
    TemporalInfo temporalInfo = new TemporalInfoImpl(now - 1000, now, 1);

    TimelineAppMetricCacheKey key1 = new TimelineAppMetricCacheKey(
      new HashSet<String>() {{ add("cpu_num._avg"); add("proc_run._avg"); }},
      "HOST",
      temporalInfo
    );

    TimelineAppMetricCacheKey key2 = new TimelineAppMetricCacheKey(
      new HashSet<String>() {{ add("cpu_num._avg"); }},
      "HOST",
      temporalInfo
    );

    Assert.assertFalse(key1.equals(key2));
    Assert.assertFalse(key2.equals(key1));

    key2.getMetricNames().add("proc_run._avg");

    Assert.assertTrue(key1.equals(key2));
  }

  @Test
  public void testTimelineMetricCachePrecisionUpdates() throws Exception {
    Configuration configuration = createNiceMock(Configuration.class);
    expect(configuration.getMetricCacheTTLSeconds()).andReturn(3600);
    expect(configuration.getMetricCacheIdleSeconds()).andReturn(100);
    expect(configuration.getMetricsCacheManagerHeapPercent()).andReturn("10%").anyTimes();
    expect(configuration.getMetricRequestBufferTimeCatchupInterval()).andReturn(1000l).anyTimes();
    replay(configuration);

    final long now = System.currentTimeMillis();
    long second = 1000;
    long min = 60 * second;
    long hour = 60 * min;
    long day = 24 * hour;
    long year = 365 * day;

    //Original Values
    Map<String, TimelineMetric> valueMap = new HashMap<>();
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("cpu_user");
    timelineMetric.setAppId("app1");

    TreeMap<Long, Double> metricValues = new TreeMap<>();
    for (long i = 1 * year - 1 * day; i >= 0; i -= 1 * day) {
      metricValues.put(now-i, 1.0);
    }

    timelineMetric.setMetricValues(metricValues);
    valueMap.put("cpu_user", timelineMetric);

    List<TimelineMetric> timelineMetricList = new ArrayList<>();
    timelineMetricList.add(timelineMetric);
    TimelineMetrics metrics = new TimelineMetrics();
    metrics.setMetrics(timelineMetricList);

    TimelineAppMetricCacheKey key = new TimelineAppMetricCacheKey(
        Collections.singleton("cpu_user"),
        "app1",
        new TemporalInfoImpl(now-1*year, now, 1)
    );
    key.setSpec("");

    //Updated values
    Map<String, TimelineMetric> newValueMap = new HashMap<>();
    TimelineMetric newTimelineMetric = new TimelineMetric();
    newTimelineMetric.setMetricName("cpu_user");
    newTimelineMetric.setAppId("app1");

    TreeMap<Long, Double> newMetricValues = new TreeMap<>();
    for(long i=1*hour;i<=2*day;i+=hour) {
      newMetricValues.put(now-1*day+i, 2.0);
    }

    newTimelineMetric.setMetricValues(newMetricValues);
    newValueMap.put("cpu_user", newTimelineMetric);

    List<TimelineMetric> newTimelineMetricList = new ArrayList<>();
    newTimelineMetricList.add(newTimelineMetric);
    TimelineMetrics newMetrics = new TimelineMetrics();
    newMetrics.setMetrics(newTimelineMetricList);

    TimelineAppMetricCacheKey newKey = new TimelineAppMetricCacheKey(
        Collections.singleton("cpu_user"),
        "app1",
        new TemporalInfoImpl(now - 1 * day, now + 2 * day, 1)
    );
    newKey.setSpec("");

    MetricsRequestHelper metricsRequestHelperForGets = createMock(MetricsRequestHelper.class);
    expect(metricsRequestHelperForGets.fetchTimelineMetrics(EasyMock.isA(URIBuilder.class), anyLong(), anyLong()))
      .andReturn(metrics).andReturn(newMetrics);
    replay(metricsRequestHelperForGets);

    TimelineMetricCacheEntryFactory cacheEntryFactory =
      createMockBuilder(TimelineMetricCacheEntryFactory.class)
        .withConstructor(configuration).createMock();

    Field requestHelperField = TimelineMetricCacheEntryFactory.class.getDeclaredField("requestHelperForGets");
    requestHelperField.setAccessible(true);
    requestHelperField.set(cacheEntryFactory, metricsRequestHelperForGets);

    requestHelperField = TimelineMetricCacheEntryFactory.class.getDeclaredField("requestHelperForUpdates");
    requestHelperField.setAccessible(true);
    requestHelperField.set(cacheEntryFactory, metricsRequestHelperForGets);

    replay(cacheEntryFactory);

    TimelineMetricCacheProvider cacheProvider = createMockBuilder(TimelineMetricCacheProvider.class)
      .addMockedMethod("createCacheConfiguration")
      .withConstructor(configuration, cacheEntryFactory)
      .createNiceMock();

    expect(cacheProvider.createCacheConfiguration()).andReturn(createTestCacheConfiguration(configuration)).anyTimes();
    replay(cacheProvider);

    TimelineMetricCache cache = cacheProvider.getTimelineMetricsCache();

    // call to get
    metrics = cache.getAppTimelineMetricsFromCache(key);
    List<TimelineMetric> metricsList = metrics.getMetrics();
    Assert.assertEquals(1, metricsList.size());
    TimelineMetric metric = metricsList.iterator().next();
    Assert.assertEquals("cpu_user", metric.getMetricName());
    Assert.assertEquals("app1", metric.getAppId());
    Assert.assertEquals(metricValues, metric.getMetricValues());

    // call to update with new key
    metrics = cache.getAppTimelineMetricsFromCache(newKey);
    metricsList = metrics.getMetrics();
    Assert.assertEquals(1, metricsList.size());
    Assert.assertEquals("cpu_user", metric.getMetricName());
    Assert.assertEquals("app1", metric.getAppId());
    Assert.assertEquals(newMetricValues, metric.getMetricValues());

    verify(configuration, metricsRequestHelperForGets, cacheEntryFactory);
  }
}
