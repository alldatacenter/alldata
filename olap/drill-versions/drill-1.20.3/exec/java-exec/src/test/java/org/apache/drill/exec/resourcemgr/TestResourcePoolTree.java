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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.apache.drill.categories.ResourceManagerTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.resourcemgr.config.QueueAssignmentResult;
import org.apache.drill.exec.resourcemgr.config.ResourcePool;
import org.apache.drill.exec.resourcemgr.config.ResourcePoolImpl;
import org.apache.drill.exec.resourcemgr.config.ResourcePoolTree;
import org.apache.drill.exec.resourcemgr.config.ResourcePoolTreeImpl;
import org.apache.drill.exec.resourcemgr.config.exception.RMConfigException;
import org.apache.drill.exec.server.options.OptionValue;
import org.apache.drill.test.BaseTest;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.drill.exec.resourcemgr.config.RMCommonDefaults.ROOT_POOL_DEFAULT_QUEUE_SELECTION_POLICY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Category(ResourceManagerTest.class)
public final class TestResourcePoolTree extends BaseTest {

  private static final Map<String, Object> poolTreeConfig = new HashMap<>();

  private static final Map<String, Object> pool1 = new HashMap<>();

  private static final Map<String, Object> pool2 = new HashMap<>();

  private static final Map<String, Object> queue1 = new HashMap<>();

  private static final List<Object> childResourcePools = new ArrayList<>();

  private static final Map<String, Object> tagSelectorConfig1 = new HashMap<>();

  private static final Map<String, Object> tagSelectorConfig2 = new HashMap<>();

  private static final QueryContext mockContext = mock(QueryContext.class);

  @BeforeClass
  public static void testSuiteSetup() {
    pool1.put(ResourcePoolImpl.POOL_NAME_KEY, "dev");
    pool1.put(ResourcePoolImpl.POOL_MEMORY_SHARE_KEY, 0.80);

    pool2.put(ResourcePoolImpl.POOL_NAME_KEY, "qa");
    pool2.put(ResourcePoolImpl.POOL_MEMORY_SHARE_KEY, 0.20);

    queue1.put("max_query_memory_per_node", 5534);

    tagSelectorConfig1.put("tag", "small");
    tagSelectorConfig2.put("tag", "large");
  }

  @After
  public void afterTestCleanup() {
    // cleanup resource tree
    poolTreeConfig.clear();

    // cleanup pools
    pool1.remove(ResourcePoolImpl.POOL_QUEUE_KEY);
    pool1.remove(ResourcePoolImpl.POOL_SELECTOR_KEY);

    pool2.remove(ResourcePoolImpl.POOL_QUEUE_KEY);
    pool2.remove(ResourcePoolImpl.POOL_SELECTOR_KEY);

    childResourcePools.clear();
  }

  private ResourcePoolTree getPoolTreeConfig() throws RMConfigException {
    poolTreeConfig.put(ResourcePoolImpl.POOL_NAME_KEY, "drill");
    poolTreeConfig.put(ResourcePoolImpl.POOL_CHILDREN_POOLS_KEY, childResourcePools);

    Config rmConfig = ConfigFactory.empty()
      .withValue("drill.exec.rm", ConfigValueFactory.fromMap(poolTreeConfig));
    return new ResourcePoolTreeImpl(rmConfig, 10000, 10, 2);
  }

  private boolean checkExpectedVsActualPools(List<ResourcePool> actual, List<String> expectedNames) {
    if (actual.size() != expectedNames.size()) {
      return false;
    }

    for (ResourcePool pool : actual) {
      if (!expectedNames.contains(pool.getPoolName())) {
        return false;
      }
    }
    return true;
  }

  @Test
  public void testTreeWith2LeafPool() throws Exception {
    // pool with tag selector
    pool1.put(ResourcePoolImpl.POOL_QUEUE_KEY, queue1);
    pool1.put(ResourcePoolImpl.POOL_SELECTOR_KEY, tagSelectorConfig1);

    // pool with default selector
    pool2.put(ResourcePoolImpl.POOL_QUEUE_KEY, queue1);

    childResourcePools.add(pool1);
    childResourcePools.add(pool2);

    ResourcePoolTree configTree = getPoolTreeConfig();

    // get all leaf queues names
    Set<String> expectedLeafQueue = new HashSet<>();
    expectedLeafQueue.add((String)pool1.get("pool_name"));
    expectedLeafQueue.add((String)pool2.get("pool_name"));

    assertEquals("Root pool is different than expected", "drill", configTree.getRootPool().getPoolName());
    assertEquals("Expected and actual leaf queue names are different", expectedLeafQueue,
      configTree.getAllLeafQueues().keySet());
    assertEquals("Unexpected Selection policy is in use", ROOT_POOL_DEFAULT_QUEUE_SELECTION_POLICY,
      configTree.getSelectionPolicyInUse().getSelectionPolicy());
  }

  @Test(expected = RMConfigException.class)
  public void testDuplicateLeafPool() throws Exception {
    // leaf pool
    pool1.put(ResourcePoolImpl.POOL_QUEUE_KEY, queue1);
    childResourcePools.add(pool1);
    childResourcePools.add(pool1);

    getPoolTreeConfig();
  }

  @Test(expected = RMConfigException.class)
  public void testMissingQueueAtLeafPool() throws Exception {
    // leaf pool with queue
    pool1.put(ResourcePoolImpl.POOL_QUEUE_KEY, queue1);
    pool1.put(ResourcePoolImpl.POOL_SELECTOR_KEY, tagSelectorConfig1);
    childResourcePools.add(pool1);
    childResourcePools.add(pool2);

    getPoolTreeConfig();
  }

  @Test(expected = RMConfigException.class)
  public void testInvalidQueueAtLeafPool() throws Exception {
    // leaf pool with invalid queue
    int initialValue = (Integer)queue1.remove("max_query_memory_per_node");

    try {
      pool1.put(ResourcePoolImpl.POOL_QUEUE_KEY, queue1);
      pool1.put(ResourcePoolImpl.POOL_SELECTOR_KEY, tagSelectorConfig1);
      childResourcePools.add(pool1);

      getPoolTreeConfig();
    } finally {
      queue1.put("max_query_memory_per_node", initialValue);
    }
  }

  @Test
  public void testRootPoolAsLeaf() throws Exception {
    // leaf pool with queue
    poolTreeConfig.put(ResourcePoolImpl.POOL_NAME_KEY, "drill");
    poolTreeConfig.put(ResourcePoolImpl.POOL_QUEUE_KEY, queue1);
    poolTreeConfig.put(ResourcePoolImpl.POOL_SELECTOR_KEY, tagSelectorConfig1);

    Config rmConfig = ConfigFactory.empty()
      .withValue("drill.exec.rm", ConfigValueFactory.fromMap(poolTreeConfig));
    ResourcePoolTree poolTree = new ResourcePoolTreeImpl(rmConfig, 10000, 10, 2);

    assertTrue("Root pool is not a leaf pool", poolTree.getRootPool().isLeafPool());
    assertEquals("Root pool name is not drill", "drill", poolTree.getRootPool().getPoolName());
    assertTrue("Root pool is not the only leaf pool", poolTree.getAllLeafQueues().size() == 1);
    assertTrue("Root pool name is not same as leaf pool name", poolTree.getAllLeafQueues().containsKey("drill"));
    assertFalse("Root pool should not be a default pool", poolTree.getRootPool().isDefaultPool());
  }

  @Test
  public void testSelectionPolicyLowerCase() throws Exception {
    // leaf pool with queue
    poolTreeConfig.put(ResourcePoolImpl.POOL_NAME_KEY, "drill");
    poolTreeConfig.put(ResourcePoolImpl.POOL_QUEUE_KEY, queue1);
    poolTreeConfig.put(ResourcePoolImpl.POOL_SELECTOR_KEY, tagSelectorConfig1);
    poolTreeConfig.put(ResourcePoolTreeImpl.ROOT_POOL_QUEUE_SELECTION_POLICY_KEY, "bestfit");

    Config rmConfig = ConfigFactory.empty()
      .withValue("drill.exec.rm", ConfigValueFactory.fromMap(poolTreeConfig));
    ResourcePoolTree poolTree = new ResourcePoolTreeImpl(rmConfig, 10000, 10, 2);

    assertTrue("Root pool is not a leaf pool", poolTree.getRootPool().isLeafPool());
    assertEquals("Root pool name is not drill", "drill", poolTree.getRootPool().getPoolName());
    assertTrue("Root pool is not the only leaf pool", poolTree.getAllLeafQueues().size() == 1);
    assertTrue("Root pool name is not same as leaf pool name", poolTree.getAllLeafQueues().containsKey("drill"));
    assertFalse("Root pool should not be a default pool", poolTree.getRootPool().isDefaultPool());
    assertTrue("Selection policy is not bestfit",
      poolTree.getSelectionPolicyInUse().getSelectionPolicy().toString().equals("bestfit"));
  }

  @Test
  public void testTreeWithLeafAndIntermediatePool() throws Exception {
    // left leaf pool1 with tag selector
    pool1.put(ResourcePoolImpl.POOL_QUEUE_KEY, queue1);
    pool1.put(ResourcePoolImpl.POOL_SELECTOR_KEY, tagSelectorConfig1);

    // left leaf pool2 with default selector
    pool2.put(ResourcePoolImpl.POOL_QUEUE_KEY, queue1);

    // intermediate left pool1 with 2 leaf pools (pool1, pool2)
    Map<String, Object> interPool1 = new HashMap<>();
    List<Object> childPools1 = new ArrayList<>();
    childPools1.add(pool1);
    childPools1.add(pool2);
    interPool1.put(ResourcePoolImpl.POOL_NAME_KEY, "eng");
    interPool1.put(ResourcePoolImpl.POOL_MEMORY_SHARE_KEY, 0.9);
    interPool1.put(ResourcePoolImpl.POOL_CHILDREN_POOLS_KEY, childPools1);

    // right leaf pool
    Map<String, Object> rightLeafPool = new HashMap<>();
    rightLeafPool.put(ResourcePoolImpl.POOL_NAME_KEY, "marketing");
    rightLeafPool.put(ResourcePoolImpl.POOL_MEMORY_SHARE_KEY, 0.1);
    rightLeafPool.put(ResourcePoolImpl.POOL_QUEUE_KEY, queue1);
    rightLeafPool.put(ResourcePoolImpl.POOL_SELECTOR_KEY, tagSelectorConfig2);

    childResourcePools.add(interPool1);
    childResourcePools.add(rightLeafPool);
    ResourcePoolTree configTree = getPoolTreeConfig();

    // Test successful selection of all leaf pools
    OptionValue testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY, ExecConstants
      .RM_QUERY_TAGS_KEY, "small,large", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    QueueAssignmentResult assignmentResult = configTree.selectAllQueues(mockContext);
    List<ResourcePool> selectedPools = assignmentResult.getSelectedLeafPools();
    List<String> expectedPools = new ArrayList<>();
    expectedPools.add("dev");
    expectedPools.add("qa");
    expectedPools.add("marketing");

    assertTrue("All leaf pools are not selected", selectedPools.size() == 3);
    assertTrue("Selected leaf pools and expected pools are different",
      checkExpectedVsActualPools(selectedPools, expectedPools));

    // Test successful selection of multiple leaf pools
    expectedPools.clear();
    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY, ExecConstants
      .RM_QUERY_TAGS_KEY, "small", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assignmentResult = configTree.selectAllQueues(mockContext);
    selectedPools = assignmentResult.getSelectedLeafPools();
    expectedPools.add("qa");
    expectedPools.add("dev");
    assertTrue("Expected 2 pools to be selected", selectedPools.size() == 2);
    assertTrue("Selected leaf pools and expected pools are different",
      checkExpectedVsActualPools(selectedPools, expectedPools));

    // Test successful selection of only left default pool
    expectedPools.clear();
    testOption = OptionValue.create(OptionValue.AccessibleScopes.SESSION_AND_QUERY, ExecConstants
      .RM_QUERY_TAGS_KEY, "medium", OptionValue.OptionScope.SESSION);
    when(mockContext.getOption(ExecConstants.RM_QUERY_TAGS_KEY)).thenReturn(testOption);
    assignmentResult = configTree.selectAllQueues(mockContext);
    selectedPools = assignmentResult.getSelectedLeafPools();
    expectedPools.add("qa");
    assertTrue("More than one leaf pool is selected", selectedPools.size() == 1);
    assertTrue("Selected leaf pools and expected pools are different",
      checkExpectedVsActualPools(selectedPools, expectedPools));

    // cleanup
    interPool1.clear();
    rightLeafPool.clear();
    expectedPools.clear();
  }
}
