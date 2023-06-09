/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.resourcemgr;

import org.apache.drill.categories.ResourceManagerTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.resourcemgr.config.QueryQueueConfig;
import org.apache.drill.exec.resourcemgr.config.RMCommonDefaults;
import org.apache.drill.exec.resourcemgr.config.ResourcePoolTree;
import org.apache.drill.exec.resourcemgr.config.selectors.AclSelector;
import org.apache.drill.exec.work.foreman.rm.DefaultResourceManager;
import org.apache.drill.exec.work.foreman.rm.DistributedResourceManager;
import org.apache.drill.exec.work.foreman.rm.ResourceManager;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.DrillTest;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore("These tests will be ignored until integration with new DistributedResourceManager is done")
@Category(ResourceManagerTest.class)
public final class TestRMConfigLoad extends DrillTest {

  @Rule
  public final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @Test
  public void testDefaultRMConfig() throws Exception {
    ClusterFixtureBuilder fixtureBuilder = ClusterFixture.builder(dirTestWatcher)
      .configProperty(ExecConstants.RM_ENABLED, true)
      .configProperty(ExecConstants.DRILL_PORT_HUNT, true)
      .withLocalZk();

    try (ClusterFixture cluster = fixtureBuilder.build()) {
      ResourceManager resourceManager = cluster.drillbit().getContext().getResourceManager();
      assertTrue(resourceManager instanceof DistributedResourceManager);

      ResourcePoolTree poolTree = ((DistributedResourceManager) resourceManager).getRmPoolTree();
      assertTrue("In drill-rm-default root pool is not leaf pool", poolTree.getRootPool().isLeafPool());
      assertTrue("selector in drill-rm-default is not acl selector",
        poolTree.getRootPool().getSelector() instanceof AclSelector);
      assertEquals("max_query_memory_per_node in drill-rm-default is not configured with expected default value",
        8 * 1024L, poolTree.getRootPool().getMaxQueryMemoryPerNode());
      assertEquals("queue_selection_policy in drill-rm-default is not configured with expected default value",
        RMCommonDefaults.ROOT_POOL_DEFAULT_QUEUE_SELECTION_POLICY,
        poolTree.getSelectionPolicyInUse().getSelectionPolicy());
      assertEquals("memory share of root pool in drill-rm-default is not configured with expected default value",
        RMCommonDefaults.ROOT_POOL_DEFAULT_MEMORY_PERCENT, poolTree.getResourceShare(), 0);

      final QueryQueueConfig defaultQueue = poolTree.getRootPool().getQueryQueue();
      assertEquals("max_admissible in drill-rm-default is not configured with expected default value",
        RMCommonDefaults.MAX_ADMISSIBLE_QUERY_COUNT, defaultQueue.getMaxAdmissibleQueries());
      assertEquals("max_waiting in drill-rm-default is not configured with expected default value",
        RMCommonDefaults.MAX_WAITING_QUERY_COUNT, defaultQueue.getMaxWaitingQueries());
      assertEquals("max_wait_timeout in drill-rm-default is not configured with expected default value",
        RMCommonDefaults.MAX_WAIT_TIMEOUT_IN_MS, defaultQueue.getWaitTimeoutInMs());
      assertEquals("wait_for_preferred_nodes in drill-rm-default is not configured with expected default value",
        RMCommonDefaults.WAIT_FOR_PREFERRED_NODES, defaultQueue.waitForPreferredNodes());
    }
  }

  @Test
  public void testDefaultRMWithLocalCoordinatorAndRMEnabled() throws Exception {
    ClusterFixtureBuilder fixtureBuilder = ClusterFixture.builder(dirTestWatcher)
      .configProperty(ExecConstants.RM_ENABLED, true);

    try (ClusterFixture cluster = fixtureBuilder.build()) {
      ResourceManager resourceManager = cluster.drillbit().getContext().getResourceManager();
      assertTrue(resourceManager instanceof DefaultResourceManager);
    }
  }

  @Test
  public void testDefaultRMWithLocalCoordinatorAndRMDisabled() throws Exception {
    ClusterFixtureBuilder fixtureBuilder = ClusterFixture.builder(dirTestWatcher)
      .configProperty(ExecConstants.RM_ENABLED, false);

    try (ClusterFixture cluster = fixtureBuilder.build()) {
      ResourceManager resourceManager = cluster.drillbit().getContext().getResourceManager();
      assertTrue(resourceManager instanceof DefaultResourceManager);
    }
  }

  @Test
  public void testDefaultRMOnlyRMDisabled() throws Exception {
    ClusterFixtureBuilder fixtureBuilder = ClusterFixture.builder(dirTestWatcher)
      .configProperty(ExecConstants.RM_ENABLED, false)
      .configProperty(ExecConstants.DRILL_PORT_HUNT, true)
      .withLocalZk();

    try (ClusterFixture cluster = fixtureBuilder.build()) {
      ResourceManager resourceManager = cluster.drillbit().getContext().getResourceManager();
      assertTrue(resourceManager instanceof DefaultResourceManager);
    }
  }
}
