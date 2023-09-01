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

package org.apache.uniffle.coordinator.checker;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.Lists;
import org.apache.hadoop.conf.Configuration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.coordinator.AccessManager;
import org.apache.uniffle.coordinator.ApplicationManager;
import org.apache.uniffle.coordinator.ClusterManager;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.ServerNode;
import org.apache.uniffle.coordinator.SimpleClusterManager;
import org.apache.uniffle.coordinator.access.AccessInfo;
import org.apache.uniffle.coordinator.access.checker.AccessClusterLoadChecker;
import org.apache.uniffle.coordinator.metric.CoordinatorMetrics;

import static org.apache.uniffle.common.util.Constants.ACCESS_INFO_REQUIRED_SHUFFLE_NODES_NUM;
import static org.apache.uniffle.coordinator.CoordinatorConf.COORDINATOR_ACCESS_CHECKERS;
import static org.apache.uniffle.coordinator.CoordinatorConf.COORDINATOR_ACCESS_LOADCHECKER_MEMORY_PERCENTAGE;
import static org.apache.uniffle.coordinator.CoordinatorConf.COORDINATOR_SHUFFLE_NODES_MAX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccessClusterLoadCheckerTest {
  private static final String clusterLoaderCheckerName = AccessClusterLoadChecker.class.getName();

  @BeforeEach
  public void setUp() {
    CoordinatorMetrics.register();
  }

  @AfterEach
  public void clear() {
    CoordinatorMetrics.clear();
  }

  @Test
  public void testAccessInfoRequiredShuffleServers() throws Exception {
    List<ServerNode> nodes = Lists.newArrayList();
    ServerNode node1 = new ServerNode(
        "1",
        "1",
        0,
        50,
        20,
        1000,
        0,
        null,
        true);
    ServerNode node2 = new ServerNode(
        "1",
        "1",
        0,
        50,
        20,
        1000,
        0,
        null,
        true);
    nodes.add(node1);
    nodes.add(node2);

    ClusterManager clusterManager = mock(SimpleClusterManager.class);
    when(clusterManager.getServerList(any())).thenReturn(nodes);

    CoordinatorConf conf = new CoordinatorConf();
    conf.set(COORDINATOR_ACCESS_CHECKERS, Collections.singletonList(clusterLoaderCheckerName));
    conf.set(COORDINATOR_SHUFFLE_NODES_MAX, 3);
    conf.set(COORDINATOR_ACCESS_LOADCHECKER_MEMORY_PERCENTAGE, 20.0);
    ApplicationManager applicationManager = new ApplicationManager(conf);
    AccessManager accessManager = new AccessManager(conf, clusterManager,
        applicationManager.getQuotaManager(), new Configuration());

    AccessClusterLoadChecker accessClusterLoadChecker =
        (AccessClusterLoadChecker) accessManager.getAccessCheckers().get(0);

    /**
     * case1:
     * when setting the invalid required shuffle nodes number of job and available servers less than
     * the COORDINATOR_SHUFFLE_NODES_MAX, it should return false
     */
    Map<String, String> properties = new HashMap<>();
    properties.put(ACCESS_INFO_REQUIRED_SHUFFLE_NODES_NUM, "-1");
    AccessInfo accessInfo = new AccessInfo("test", new HashSet<>(), properties, "user");
    assertFalse(accessClusterLoadChecker.check(accessInfo).isSuccess());

    /**
     * case2:
     * when setting the valid required shuffle nodes number of job and available servers greater than
     * the COORDINATOR_SHUFFLE_NODES_MAX, it should return true
     */
    properties.put(ACCESS_INFO_REQUIRED_SHUFFLE_NODES_NUM, "1");
    accessInfo = new AccessInfo("test", new HashSet<>(), properties, "user");
    assertTrue(accessClusterLoadChecker.check(accessInfo).isSuccess());

    /**
     * case3:
     * when setting the valid required shuffle nodes number of job and available servers less than
     * the COORDINATOR_SHUFFLE_NODES_MAX, it should return false
     */
    properties.put(ACCESS_INFO_REQUIRED_SHUFFLE_NODES_NUM, "100");
    accessInfo = new AccessInfo("test", new HashSet<>(), properties, "user");
    assertFalse(accessClusterLoadChecker.check(accessInfo).isSuccess());

    /**
     * case4:
     * when the required shuffle nodes number is not specified in access info, it should use the
     * default shuffle nodes max from coordinator conf.
     */
    properties = new HashMap<>();
    accessInfo = new AccessInfo("test", new HashSet<>(), properties, "user");
    assertFalse(accessClusterLoadChecker.check(accessInfo).isSuccess());
  }

  @Test
  public void testWhenAvailableServerThresholdSpecified() throws Exception {
    ClusterManager clusterManager = mock(SimpleClusterManager.class);
    List<ServerNode> serverNodeList = Lists.newArrayList();
    ServerNode node1 = new ServerNode(
        "1",
        "1",
        0,
        50,
        20,
        30,
        0,
        null,
        false);
    serverNodeList.add(node1);
    final String filePath = Objects.requireNonNull(
        getClass().getClassLoader().getResource("coordinator.conf")).getFile();
    CoordinatorConf conf = new CoordinatorConf(filePath);
    conf.setString(COORDINATOR_ACCESS_CHECKERS.key(), clusterLoaderCheckerName);
    ApplicationManager applicationManager = new ApplicationManager(conf);
    AccessManager accessManager = new AccessManager(conf, clusterManager,
        applicationManager.getQuotaManager(), new Configuration());
    AccessClusterLoadChecker accessClusterLoadChecker =
        (AccessClusterLoadChecker) accessManager.getAccessCheckers().get(0);
    when(clusterManager.getServerList(any())).thenReturn(serverNodeList);
    assertFalse(accessClusterLoadChecker.check(new AccessInfo("test")).isSuccess());
    assertEquals(2, accessClusterLoadChecker.getAvailableServerNumThreshold());
    assertEquals(0, Double.compare(accessClusterLoadChecker.getMemoryPercentThreshold(), 20.0));
    ServerNode node2 = new ServerNode(
        "1",
        "1",
        0,
        90,
        40,
        10,
        0,
        null,
        true);
    serverNodeList.add(node2);
    ServerNode node3 = new ServerNode(
        "1",
        "1",
        0,
        80,
        25,
        20,
        0,
        null,
        true);
    serverNodeList.add(node3);
    assertFalse(accessClusterLoadChecker.check(new AccessInfo("test")).isSuccess());
    ServerNode node4 = new ServerNode(
        "1",
        "1",
        0,
        75,
        25,
        25,
        0,
        null,
        true);
    serverNodeList.add(node4);
    assertTrue(accessClusterLoadChecker.check(new AccessInfo("test")).isSuccess());
  }
}
