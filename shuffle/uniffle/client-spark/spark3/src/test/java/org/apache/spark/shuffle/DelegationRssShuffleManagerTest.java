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

package org.apache.spark.shuffle;

import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.collect.Lists;
import org.apache.spark.SparkConf;
import org.apache.spark.shuffle.sort.SortShuffleManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.apache.uniffle.client.api.CoordinatorClient;
import org.apache.uniffle.client.response.RssAccessClusterResponse;
import org.apache.uniffle.storage.util.StorageType;

import static org.apache.uniffle.common.rpc.StatusCode.ACCESS_DENIED;
import static org.apache.uniffle.common.rpc.StatusCode.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

public class DelegationRssShuffleManagerTest {
  private static MockedStatic<RssSparkShuffleUtils> mockedStaticRssShuffleUtils;

  @BeforeAll
  public static void setUp() {
    mockedStaticRssShuffleUtils = mockStatic(RssSparkShuffleUtils.class, Mockito.CALLS_REAL_METHODS);
  }

  @AfterAll
  public static void tearDown() {
    mockedStaticRssShuffleUtils.close();
  }

  @Test
  public void testCreateInDriverDenied() throws Exception {
    CoordinatorClient mockCoordinatorClient = mock(CoordinatorClient.class);
    when(mockCoordinatorClient.accessCluster(any())).thenReturn(new RssAccessClusterResponse(ACCESS_DENIED, ""));
    List<CoordinatorClient> coordinatorClients = Lists.newArrayList();
    coordinatorClients.add(mockCoordinatorClient);
    mockedStaticRssShuffleUtils.when(() ->
      RssSparkShuffleUtils.createCoordinatorClients(any())).thenReturn(coordinatorClients);
    SparkConf conf = new SparkConf();
    conf.set(RssSparkConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED.key(), "false");
    assertCreateSortShuffleManager(conf);
  }

  @Test
  public void testCreateInDriver() throws Exception {
    CoordinatorClient mockCoordinatorClient = mock(CoordinatorClient.class);
    when(mockCoordinatorClient.accessCluster(any())).thenReturn(
        new RssAccessClusterResponse(SUCCESS, ""));
    List<CoordinatorClient> coordinatorClients = Lists.newArrayList();
    coordinatorClients.add(mockCoordinatorClient);
    mockedStaticRssShuffleUtils.when(() ->
      RssSparkShuffleUtils.createCoordinatorClients(any())).thenReturn(coordinatorClients);

    SparkConf conf = new SparkConf();
    assertCreateSortShuffleManager(conf);
    conf.set(RssSparkConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED.key(), "false");
    conf.set(RssSparkConfig.RSS_ACCESS_ID.key(), "mockId");
    assertCreateSortShuffleManager(conf);
    conf.set(RssSparkConfig.RSS_COORDINATOR_QUORUM.key(), "m1:8001,m2:8002");
    conf.set("spark.rss.storage.type", StorageType.LOCALFILE.name());
    conf.set(RssSparkConfig.RSS_TEST_MODE_ENABLE, true);
    assertCreateRssShuffleManager(conf);

    conf = new SparkConf();
    conf.set(RssSparkConfig.RSS_COORDINATOR_QUORUM.key(), "m1:8001,m2:8002");
    when(mockCoordinatorClient.accessCluster(any())).thenReturn(
        new RssAccessClusterResponse(SUCCESS, ""));
    assertCreateSortShuffleManager(conf);
  }

  @Test
  public void testCreateInExecutor() throws Exception {
    DelegationRssShuffleManager delegationRssShuffleManager;
    SparkConf conf = new SparkConf();
    conf.set(RssSparkConfig.RSS_COORDINATOR_QUORUM.key(), "m1:8001,m2:8002");
    delegationRssShuffleManager = new DelegationRssShuffleManager(conf, false);
    assertFalse(delegationRssShuffleManager.getDelegate() instanceof RssShuffleManager);
    assertTrue(delegationRssShuffleManager.getDelegate() instanceof SortShuffleManager);
  }

  @Test
  public void testCreateFallback() throws Exception {
    CoordinatorClient mockCoordinatorClient = mock(CoordinatorClient.class);
    when(mockCoordinatorClient.accessCluster(any())).thenReturn(new RssAccessClusterResponse(SUCCESS, ""));
    List<CoordinatorClient> coordinatorClients = Lists.newArrayList();
    coordinatorClients.add(mockCoordinatorClient);
    mockedStaticRssShuffleUtils.when(() ->
      RssSparkShuffleUtils.createCoordinatorClients(any())).thenReturn(coordinatorClients);

    SparkConf conf = new SparkConf();
    conf.set(RssSparkConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED.key(), "false");
    conf.set(RssSparkConfig.RSS_ACCESS_ID.key(), "mockId");
    conf.set(RssSparkConfig.RSS_ENABLED.key(), "true");

    // fall back to SortShuffleManager in driver
    assertCreateSortShuffleManager(conf);

    // No fall back in executor
    conf.set(RssSparkConfig.RSS_ENABLED.key(), "true");
    boolean hasException = false;
    try {
      new DelegationRssShuffleManager(conf, false);
    } catch (NoSuchElementException e) {
      assertTrue(e.getMessage().startsWith("spark.rss.coordinator.quorum"));
      hasException = true;
    }
    assertTrue(hasException);
  }

  @Test
  public void testTryAccessCluster() throws Exception {
    CoordinatorClient mockDeniedCoordinatorClient = mock(CoordinatorClient.class);
    when(mockDeniedCoordinatorClient.accessCluster(any()))
        .thenReturn(new RssAccessClusterResponse(ACCESS_DENIED, ""))
        .thenReturn(new RssAccessClusterResponse(ACCESS_DENIED, ""))
        .thenReturn(new RssAccessClusterResponse(SUCCESS, ""));
    List<CoordinatorClient> coordinatorClients = Lists.newArrayList();
    coordinatorClients.add(mockDeniedCoordinatorClient);
    mockedStaticRssShuffleUtils.when(() ->
        RssSparkShuffleUtils.createCoordinatorClients(any())).thenReturn(coordinatorClients);
    SparkConf conf = new SparkConf();
    conf.set(RssSparkConfig.RSS_CLIENT_ACCESS_RETRY_INTERVAL_MS, 3000L);
    conf.set(RssSparkConfig.RSS_CLIENT_ACCESS_RETRY_TIMES, 3);
    conf.set(RssSparkConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED.key(), "false");
    conf.set(RssSparkConfig.RSS_ACCESS_ID.key(), "mockId");
    conf.set(RssSparkConfig.RSS_COORDINATOR_QUORUM.key(), "m1:8001,m2:8002");
    conf.set("spark.rss.storage.type", StorageType.LOCALFILE.name());
    conf.set(RssSparkConfig.RSS_TEST_MODE_ENABLE, true);
    assertCreateRssShuffleManager(conf);

    CoordinatorClient mockCoordinatorClient = mock(CoordinatorClient.class);
    when(mockCoordinatorClient.accessCluster(any()))
        .thenReturn(new RssAccessClusterResponse(ACCESS_DENIED, ""))
        .thenReturn(new RssAccessClusterResponse(ACCESS_DENIED, ""))
        .thenReturn(new RssAccessClusterResponse(ACCESS_DENIED, ""));
    List<CoordinatorClient> secondCoordinatorClients = Lists.newArrayList();
    secondCoordinatorClients.add(mockCoordinatorClient);
    mockedStaticRssShuffleUtils.when(() ->
        RssSparkShuffleUtils.createCoordinatorClients(any())).thenReturn(secondCoordinatorClients);
    SparkConf secondConf = new SparkConf();
    secondConf.set(RssSparkConfig.RSS_CLIENT_ACCESS_RETRY_INTERVAL_MS, 3000L);
    secondConf.set(RssSparkConfig.RSS_CLIENT_ACCESS_RETRY_TIMES, 3);
    secondConf.set(RssSparkConfig.RSS_DYNAMIC_CLIENT_CONF_ENABLED.key(), "false");
    secondConf.set(RssSparkConfig.RSS_ACCESS_ID.key(), "mockId");
    secondConf.set(RssSparkConfig.RSS_COORDINATOR_QUORUM.key(), "m1:8001,m2:8002");
    secondConf.set("spark.rss.storage.type", StorageType.LOCALFILE.name());
    assertCreateSortShuffleManager(secondConf);
  }

  private void assertCreateSortShuffleManager(SparkConf conf) throws Exception {
    DelegationRssShuffleManager delegationRssShuffleManager = new DelegationRssShuffleManager(conf, true);
    assertTrue(delegationRssShuffleManager.getDelegate() instanceof SortShuffleManager);
    assertFalse(delegationRssShuffleManager.getDelegate() instanceof RssShuffleManager);
    assertFalse(conf.getBoolean(RssSparkConfig.RSS_ENABLED.key(), false));
    assertEquals("sort", conf.get("spark.shuffle.manager"));
  }

  private void assertCreateRssShuffleManager(SparkConf conf) throws Exception {
    DelegationRssShuffleManager delegationRssShuffleManager = new DelegationRssShuffleManager(conf, true);
    assertFalse(delegationRssShuffleManager.getDelegate() instanceof SortShuffleManager);
    assertTrue(delegationRssShuffleManager.getDelegate() instanceof RssShuffleManager);
    assertTrue(Boolean.parseBoolean(conf.get(RssSparkConfig.RSS_ENABLED.key())));
    assertEquals(RssShuffleManager.class.getCanonicalName(), conf.get("spark.shuffle.manager"));
  }
}
