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

import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.resourcemgr.config.selectors.ResourcePoolSelector;

import java.util.List;

/**
 * Interface which defines an implementation of ResourcePool configuration for {@link ResourcePoolTree}
 */
public interface ResourcePool {
  String getPoolName();

  boolean isLeafPool();

  boolean isDefaultPool();

  // Only valid for leaf pool since it will have a queue assigned to it with this configuration
  long getMaxQueryMemoryPerNode();

  /**
   * Evaluates this pool selector to see if the query can be admitted in this pool. If yes then evaluates all
   * the child pools selectors as well. During traversal it builds the QueueAssignment result which consists of all
   * the selected leaf pools and all rejected intermediate pools.
   * @param assignmentResult
   * @param queryContext
   */
  void visitAndSelectPool(QueueAssignmentResult assignmentResult, QueryContext queryContext);

  /**
   * @return Percentage of memory share assigned to this pool
   */
  double getPoolMemoryShare();

  long getPoolMemoryInMB(int numClusterNodes);

  /**
   * Only valid for leaf pool.
   * @return Returns queue configuration assigned to this leaf pool
   */
  QueryQueueConfig getQueryQueue();

  /**
   * @return Full path of the resource pool from root to this pool
   */
  String getFullPath();

  ResourcePool getParentPool();

  List<ResourcePool> getChildPools();

  ResourcePoolSelector getSelector();
}
