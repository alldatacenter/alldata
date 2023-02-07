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
package org.apache.drill.exec.resourcemgr.config;

import com.typesafe.config.Config;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.resourcemgr.NodeResources;
import org.apache.drill.exec.resourcemgr.config.exception.RMConfigException;
import org.apache.drill.exec.resourcemgr.config.selectors.DefaultSelector;
import org.apache.drill.exec.resourcemgr.config.selectors.ResourcePoolSelector;
import org.apache.drill.exec.resourcemgr.config.selectors.ResourcePoolSelectorFactory;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import java.util.List;
import java.util.Map;

/**
 * Parses and initializes all the provided configuration for a ResourcePool defined in RM configuration. It takes
 * care of creating all the child ResourcePools belonging to this Resource Pool, {@link ResourcePoolSelector} for this
 * pool and a {@link QueryQueueConfig} if it's a leaf pool.
 */
public class ResourcePoolImpl implements ResourcePool {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ResourcePoolImpl.class);

  public static final String POOL_NAME_KEY = "pool_name";

  public static final String POOL_MEMORY_SHARE_KEY = "memory";

  public static final String POOL_CHILDREN_POOLS_KEY = "child_pools";

  public static final String POOL_SELECTOR_KEY = "selector";

  public static final String POOL_QUEUE_KEY = "queue";

  private String poolName;

  private List<ResourcePool> childPools;

  private final double parentResourceShare;

  private final double poolResourceShare;

  private QueryQueueConfig assignedQueue;

  private final ResourcePoolSelector assignedSelector;

  private NodeResources poolResourcePerNode;

  private final ResourcePool parentPool;

  ResourcePoolImpl(Config poolConfig, double poolAbsResourceShare, double parentResourceShare,
                   NodeResources parentNodeResource, ResourcePool parentPool,
                   Map<String, QueryQueueConfig> leafQueueCollector) throws RMConfigException {
    try {
      this.poolName = poolConfig.getString(POOL_NAME_KEY);
      this.parentResourceShare = parentResourceShare;
      this.poolResourceShare = poolAbsResourceShare * this.parentResourceShare;
      this.parentPool = parentPool;
      assignedSelector = ResourcePoolSelectorFactory.createSelector(poolConfig.hasPath(POOL_SELECTOR_KEY)
        ? poolConfig.getConfig(POOL_SELECTOR_KEY) : null);
      parseAndCreateChildPools(poolConfig, parentNodeResource, leafQueueCollector);
    } catch (RMConfigException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RMConfigException(String.format("Failure while parsing configuration for pool: %s. [Details: " +
        "PoolConfig: %s]", poolName, poolConfig), ex);
    }
  }

  @Override
  public String getPoolName() {
    return poolName;
  }

  /**
   * Determines if this ResourcePool is a leaf pool or not which will have a queue associated with it
   * @return <tt>true</tt> If a leaf pool, <tt>false</tt> otherwise
   */
  @Override
  public boolean isLeafPool() {
    return childPools == null && assignedQueue != null;
  }

  /**
   * Determines if this ResourcePool is a default pool or not which will act as a sink for all the queries
   * @return <tt>true</tt> If a Default pool, <tt>false</tt> otherwise
   */
  @Override
  public boolean isDefaultPool() {
    return (assignedSelector instanceof DefaultSelector);
  }

  @Override
  public long getMaxQueryMemoryPerNode() {
    Preconditions.checkState(isLeafPool() && assignedQueue != null, "max_query_memory_per_node is " +
      "only valid for leaf level pools which has a queue assigned to it [Details: PoolName: %s]", poolName);
    return assignedQueue.getMaxQueryMemoryInMBPerNode();
  }

  /**
   * Used to determine if a ResourcePool is selected for a given query or not. It uses the assigned selector of this
   * ResourcePool which takes in query metadata to determine if a query is allowed in this pool.
   * @param assignmentResult Used to keep track of all selected leaf pools and all rejected pools for given query
   * @param queryContext Contains query metadata like user, groups, tags, etc used by ResourcePoolSelector
   */
  @Override
  public void visitAndSelectPool(QueueAssignmentResult assignmentResult, QueryContext queryContext) {
    if (assignedSelector.isQuerySelected(queryContext)) {
      if (isLeafPool()) {
        assignmentResult.addSelectedPool(this);
      } else {
        // Check for each of the child pools
        for (ResourcePool childPool : childPools) {
          childPool.visitAndSelectPool(assignmentResult, queryContext);
        }
      }
    } else {
      assignmentResult.addRejectedPool(this);
    }
  }

  /**
   * Actual percentage share of memory assigned to this ResourcePool
   * @return Pool memory share in percentage
   */
  @Override
  public double getPoolMemoryShare() {
    return poolResourceShare;
  }

  /**
   * Total memory share in MB assigned to this ResourcePool
   * @param numClusterNodes number of available cluster nodes for this pool
   * @return Pool memory share in MB
   */
  @Override
  public long getPoolMemoryInMB(int numClusterNodes) {
    return poolResourcePerNode.getMemoryInMB() * numClusterNodes;
  }

  /**
   * Parses and creates all the child ResourcePools if this is an intermediate pool or QueryQueueConfig is its a leaf
   * resource pool
   * @param poolConfig Config object for this ResourcePool
   * @param parentResource Parent ResourcePool NodeResources
   * @param leafQueueCollector Collector which keeps track of all leaf queues
   * @throws RMConfigException in case of bad configuration
   */
  private void parseAndCreateChildPools(Config poolConfig, NodeResources parentResource,
                                       Map<String, QueryQueueConfig> leafQueueCollector) throws RMConfigException {
    this.poolResourcePerNode = new NodeResources(Math.round(parentResource.getMemoryInBytes() * poolResourceShare),
      parentResource.getNumVirtualCpu());
    if (poolConfig.hasPath(POOL_CHILDREN_POOLS_KEY)) {
      childPools = Lists.newArrayList();
      List<? extends Config> childPoolsConfig = poolConfig.getConfigList(POOL_CHILDREN_POOLS_KEY);
      logger.debug("Creating {} child pools for parent pool {}", childPoolsConfig.size(), poolName);
      for (Config childConfig : childPoolsConfig) {
        try {
          final ResourcePool childPool = new ResourcePoolImpl(childConfig, childConfig.getDouble(POOL_MEMORY_SHARE_KEY),
            poolResourceShare, poolResourcePerNode, this, leafQueueCollector);
          childPools.add(childPool);
        } catch (RMConfigException ex) {
          logger.error("Failure while configuring child ResourcePool. [Details: PoolName: {}, ChildPoolConfig with " +
            "error: {}]", poolName, childConfig);
          throw ex;
        } catch (Exception ex) {
          throw new RMConfigException(String.format("Failure while configuring the child ResourcePool. [Details: " +
            "PoolName: %s, ChildPoolConfig with error: %s]", poolName, childConfig), ex);
        }
      }

      if (childPools.isEmpty()) {
        throw new RMConfigException(String.format("Empty config for child_pools is not allowed. Please configure the " +
          "child_pools property of pool %s correctly or associate a queue with it with no child_pools", poolName));
      }
    } else {
      logger.info("Resource Pool {} is a leaf level pool with queue assigned to it", poolName);

      if (leafQueueCollector.containsKey(poolName)) {
        throw new RMConfigException(String.format("Found non-unique leaf pools with name: %s and config: %s. Leaf " +
            "pool names has to be unique since they represent a queue.", poolName, poolConfig));
      }
      assignedQueue = new QueryQueueConfigImpl(poolConfig.getConfig(POOL_QUEUE_KEY), poolName, poolResourcePerNode);
      leafQueueCollector.put(poolName, assignedQueue);
    }
  }

  /**
   * If this a leaf pool then returns the {@link QueryQueueConfig} for the queue associated with this pool
   * @return {@link QueryQueueConfig} object for this pool
   */
  @Override
  public QueryQueueConfig getQueryQueue() {
    Preconditions.checkState(isLeafPool() && assignedQueue != null, "QueryQueue is only " +
        "valid for leaf level pools.[Details: PoolName: %s]", poolName);
    return assignedQueue;
  }

  @Override
  public ResourcePool getParentPool() {
    return parentPool;
  }

  /**
   * Returns full path in terms of concatenated pool names from root pool to this pool in {@link ResourcePoolTree}
   * @return String with pool names from root to this pool
   */
  @Override
  public String getFullPath() {
    StringBuilder pathBuilder = new StringBuilder(poolName);
    ResourcePool parent = parentPool;
    while(parent != null) {
      pathBuilder.append(parent.getPoolName());
      parent = parent.getParentPool();
    }

    return pathBuilder.toString();
  }

  @Override
  public List<ResourcePool> getChildPools() {
    Preconditions.checkState(!isLeafPool() && assignedQueue == null,
      "There are no child pools for a leaf ResourcePool");
    return childPools;
  }

  @Override
  public ResourcePoolSelector getSelector() {
    return assignedSelector;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{PoolName: ").append(poolName);
    sb.append(", PoolResourceShare: ").append(poolResourceShare);
    sb.append(", Selector: ").append(assignedSelector.getSelectorType());
    if (isLeafPool()) {
      sb.append(", Queue: [").append(assignedQueue.toString()).append("]");
    } else {
      sb.append(", ChildPools: [");

      for (ResourcePool childPool : childPools) {
        sb.append(childPool.toString());
        sb.append(", ");
      }
      sb.append("]");
    }
    sb.append("}");
    return sb.toString();
  }
}
