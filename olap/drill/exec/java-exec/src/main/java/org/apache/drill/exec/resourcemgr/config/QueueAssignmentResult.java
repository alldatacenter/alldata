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
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

/**
 * Used to keep track of selected leaf and all rejected {@link ResourcePool} for the provided query.
 * It is used by {@link ResourcePoolImpl#visitAndSelectPool(QueueAssignmentResult, QueryContext)} to store
 * information about all the matching and non-matching ResourcePools for a query when ResourcePool selector is
 * evaluated against query metadata. Later it is used by
 * {@link ResourcePool#visitAndSelectPool(QueueAssignmentResult, QueryContext)} to apply
 * {@link org.apache.drill.exec.resourcemgr.config.selectionpolicy.QueueSelectionPolicy} to select only one queue out of all
 * the selected queues for a query. It also provides an API to dump all the debug information to know which pools were
 * selected and rejected for a query.
 */
public class QueueAssignmentResult {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(QueueAssignmentResult.class);

  private final List<ResourcePool> selectedLeafPools = new ArrayList<>();

  private final List<ResourcePool> rejectedPools = new ArrayList<>();

  public void addSelectedPool(ResourcePool pool) {
    Preconditions.checkState(pool.isLeafPool(), "Selected pool %s is not a leaf pool",
      pool.getPoolName());
    selectedLeafPools.add(pool);
  }

  public void addRejectedPool(ResourcePool pool) {
    rejectedPools.add(pool);
  }

  public List<ResourcePool> getSelectedLeafPools() {
    return selectedLeafPools;
  }

  public List<ResourcePool> getRejectedPools() {
    return rejectedPools;
  }

  public void logAssignmentResult(String queryId) {
    logger.debug("For query {}. Details[{}]", queryId, toString());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Selected Leaf Pools: {");
    for (ResourcePool pool : selectedLeafPools) {
      sb.append(pool.getPoolName()).append(", ");
    }
    sb.append("} and Rejected pools: {");
    for (ResourcePool pool : rejectedPools) {
      sb.append(pool.getPoolName()).append(", ");
    }
    sb.append("}");
    return sb.toString();
  }
}
