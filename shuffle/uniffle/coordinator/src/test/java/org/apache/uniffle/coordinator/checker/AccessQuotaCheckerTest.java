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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

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
import org.apache.uniffle.coordinator.access.checker.AccessQuotaChecker;
import org.apache.uniffle.coordinator.metric.CoordinatorMetrics;

import static org.apache.uniffle.common.util.Constants.ACCESS_INFO_REQUIRED_SHUFFLE_NODES_NUM;
import static org.apache.uniffle.coordinator.CoordinatorConf.COORDINATOR_ACCESS_CHECKERS;
import static org.apache.uniffle.coordinator.CoordinatorConf.COORDINATOR_QUOTA_DEFAULT_APP_NUM;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccessQuotaCheckerTest {
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
    conf.set(COORDINATOR_ACCESS_CHECKERS,
        Collections.singletonList(AccessQuotaChecker.class.getName()));
    conf.set(COORDINATOR_QUOTA_DEFAULT_APP_NUM, 3);
    ApplicationManager applicationManager = new ApplicationManager(conf);
    AccessManager accessManager = new AccessManager(conf, clusterManager,
        applicationManager.getQuotaManager(), new Configuration());

    AccessQuotaChecker accessQuotaChecker =
        (AccessQuotaChecker) accessManager.getAccessCheckers().get(0);

    /**
     * case1:
     * when user set default app num is 5, and commit 6 app which current app num is greater than default app num,
     * it will reject 1 app and return false.
     */
    Map<String, String> properties = new HashMap<>();
    AccessInfo accessInfo = new AccessInfo("test", new HashSet<>(), properties, "user");
    assertTrue(accessQuotaChecker.check(accessInfo).isSuccess());
    assertTrue(accessQuotaChecker.check(accessInfo).isSuccess());
    assertTrue(accessQuotaChecker.check(accessInfo).isSuccess());
    assertFalse(accessQuotaChecker.check(accessInfo).isSuccess());

    /**
     * case2:
     * when setting the valid required shuffle nodes number of job and available servers greater than
     * the COORDINATOR_SHUFFLE_NODES_MAX, it should return true
     */
    conf.set(COORDINATOR_QUOTA_DEFAULT_APP_NUM, 0);
    applicationManager = new ApplicationManager(conf);
    accessManager = new AccessManager(conf, clusterManager, applicationManager.getQuotaManager(), new Configuration());
    accessQuotaChecker = (AccessQuotaChecker) accessManager.getAccessCheckers().get(0);
    accessInfo = new AccessInfo("test", new HashSet<>(), properties, "user");
    assertFalse(accessQuotaChecker.check(accessInfo).isSuccess());

    /**
     * case3:
     * when setting two checkers and the valid required shuffle nodes number of job and available servers less than
     * the COORDINATOR_SHUFFLE_NODES_MAX, it should return false
     */
    conf.set(COORDINATOR_QUOTA_DEFAULT_APP_NUM, 10);
    conf.set(COORDINATOR_ACCESS_CHECKERS,
        Arrays.asList("org.apache.uniffle.coordinator.access.checker.AccessQuotaChecker",
            "org.apache.uniffle.coordinator.access.checker.AccessClusterLoadChecker"));
    applicationManager = new ApplicationManager(conf);
    accessManager = new AccessManager(conf, clusterManager, applicationManager.getQuotaManager(), new Configuration());
    accessQuotaChecker = (AccessQuotaChecker) accessManager.getAccessCheckers().get(0);
    final AccessClusterLoadChecker accessClusterLoadChecker =
        (AccessClusterLoadChecker) accessManager.getAccessCheckers().get(1);
    properties.put(ACCESS_INFO_REQUIRED_SHUFFLE_NODES_NUM, "100");
    accessInfo = new AccessInfo("test", new HashSet<>(), properties, "user");
    assertTrue(accessQuotaChecker.check(accessInfo).isSuccess());
    assertFalse(accessClusterLoadChecker.check(accessInfo).isSuccess());
  }
}
