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
import org.apache.drill.exec.resourcemgr.config.exception.QueueSelectionException;
import org.apache.drill.exec.resourcemgr.config.exception.RMConfigException;
import org.apache.drill.exec.resourcemgr.config.selectionpolicy.QueueSelectionPolicy;
import org.apache.drill.exec.resourcemgr.config.selectionpolicy.QueueSelectionPolicy.SelectionPolicy;
import org.apache.drill.exec.resourcemgr.config.selectionpolicy.QueueSelectionPolicyFactory;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

import java.util.List;
import java.util.Map;

import static org.apache.drill.exec.resourcemgr.config.RMCommonDefaults.ROOT_POOL_DEFAULT_MEMORY_PERCENT;
import static org.apache.drill.exec.resourcemgr.config.RMCommonDefaults.ROOT_POOL_DEFAULT_QUEUE_SELECTION_POLICY;

/**
 * Parses and initializes configuration for ResourceManagement in Drill. It takes care of creating all the ResourcePools
 * recursively maintaining the n-ary tree hierarchy defined in the configuration.
 */
public class ResourcePoolTreeImpl implements ResourcePoolTree {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ResourcePoolTreeImpl.class);

  public static final String ROOT_POOL_QUEUE_SELECTION_POLICY_KEY = "queue_selection_policy";

  public static final String ROOT_POOL_CONFIG_KEY = "drill.exec.rm";

  private static final String ROOT_POOL_MEMORY_SHARE_KEY = "memory";

  private final ResourcePool rootPool;

  private final Config rmConfig;

  private final NodeResources totalNodeResources;

  private final double resourceShare;

  private final Map<String, QueryQueueConfig> leafQueues = Maps.newHashMap();

  private final QueueSelectionPolicy selectionPolicy;

  public ResourcePoolTreeImpl(Config rmConfig, long totalNodeMemory,
                              int totalNodePhysicalCpu, int vFactor) throws RMConfigException {
    this(rmConfig, new NodeResources(totalNodeMemory, totalNodePhysicalCpu, vFactor));
  }

  private ResourcePoolTreeImpl(Config rmConfig, NodeResources totalNodeResources) throws RMConfigException {
    try {
      this.rmConfig = rmConfig;
      final Config rootConfig = this.rmConfig.getConfig(ROOT_POOL_CONFIG_KEY);
      this.totalNodeResources = totalNodeResources;
      this.resourceShare = rootConfig.hasPath(ROOT_POOL_MEMORY_SHARE_KEY) ?
        rootConfig.getDouble(ROOT_POOL_MEMORY_SHARE_KEY) : ROOT_POOL_DEFAULT_MEMORY_PERCENT;
      this.selectionPolicy = QueueSelectionPolicyFactory.createSelectionPolicy(
        rootConfig.hasPath(ROOT_POOL_QUEUE_SELECTION_POLICY_KEY) ?
          SelectionPolicy.valueOf(rootConfig.getString(ROOT_POOL_QUEUE_SELECTION_POLICY_KEY).trim().toUpperCase()) :
          ROOT_POOL_DEFAULT_QUEUE_SELECTION_POLICY);
      rootPool = new ResourcePoolImpl(rootConfig, resourceShare, 1.0,
        totalNodeResources, null, leafQueues);
      logger.debug("Dumping RM configuration {}", toString());
    } catch (RMConfigException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RMConfigException(String.format("Failure while parsing root pool configuration. " +
        "[Details: Config: %s]", rmConfig), ex);
    }
  }

  /**
   * @return root {@link ResourcePool}
   */
  @Override
  public ResourcePool getRootPool() {
    return rootPool;
  }

  /**
   * @return Map containing all the configured leaf queues
   */
  @Override
  public Map<String, QueryQueueConfig> getAllLeafQueues() {
    return leafQueues;
  }

  @Override
  public double getResourceShare() {
    return resourceShare;
  }

  /**
   * Creates {@link QueueAssignmentResult} which contains list of all selected leaf ResourcePools and all the rejected
   * ResourcePools for the provided query. Performs DFS of the ResourcePoolTree to traverse and find
   * selected/rejected ResourcePools.
   * @param queryContext {@link QueryContext} which contains metadata required for the given query
   * @return {@link QueueAssignmentResult} populated with selected/rejected ResourcePools
   */
  @Override
  public QueueAssignmentResult selectAllQueues(QueryContext queryContext) {
    QueueAssignmentResult assignmentResult = new QueueAssignmentResult();
    rootPool.visitAndSelectPool(assignmentResult, queryContext);
    return assignmentResult;
  }

  /**
   * Selects a leaf queue out of all the possibles leaf queues returned by
   * {@link ResourcePoolTree#selectAllQueues(QueryContext)} for a given query based on the configured
   * {@link QueueSelectionPolicy}. If none of the queue qualifies then it throws {@link QueueSelectionException}
   * @param queryContext {@link QueryContext} which contains metadata for given query
   * @param queryMaxNodeResource Max resources on a node required for given query
   * @return {@link QueryQueueConfig} for the selected leaf queue
   * @throws QueueSelectionException If no leaf queue is selected
   */
  @Override
  public QueryQueueConfig selectOneQueue(QueryContext queryContext, NodeResources queryMaxNodeResource)
    throws QueueSelectionException {
    final QueueAssignmentResult assignmentResult = selectAllQueues(queryContext);
    final List<ResourcePool> selectedPools = assignmentResult.getSelectedLeafPools();
    if (selectedPools.size() == 0) {
      throw new QueueSelectionException(String.format("No resource pools to choose from for the query: %s",
        queryContext.getQueryId()));
    } else if (selectedPools.size() == 1) {
      return selectedPools.get(0).getQueryQueue();
    }

    return selectionPolicy.selectQueue(selectedPools, queryContext, queryMaxNodeResource).getQueryQueue();
  }

  /**
   * @return {@link QueueSelectionPolicy} which is used to chose one queue out of all the available options for a query
   */
  @Override
  public QueueSelectionPolicy getSelectionPolicyInUse() {
    return selectionPolicy;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("{ NodeResources: ").append(totalNodeResources.toString());
    sb.append(", ResourcePercent: ").append(resourceShare);
    sb.append(", SelectionPolicy: ").append(selectionPolicy.getSelectionPolicy());
    sb.append(", RootPool: ").append(rootPool.toString());
    sb.append("}");
    return sb.toString();
  }
}
