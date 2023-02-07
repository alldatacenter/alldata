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
package org.apache.drill.exec.resourcemgr.config.selectionpolicy;

import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.resourcemgr.NodeResources;
import org.apache.drill.exec.resourcemgr.config.QueryQueueConfig;
import org.apache.drill.exec.resourcemgr.config.ResourcePool;
import org.apache.drill.exec.resourcemgr.config.QueryQueueConfigImpl;
import org.apache.drill.exec.resourcemgr.config.exception.QueueSelectionException;

import java.util.Comparator;
import java.util.List;

/**
 * Helps to select a queue whose {@link QueryQueueConfig#getMaxQueryMemoryInMBPerNode()} is nearest to the max memory
 * on a node required by the given query. Nearest is found by following rule:
 * <ul>
 *   <li>
 *     Queue whose {@link QueryQueueConfigImpl#MAX_QUERY_MEMORY_PER_NODE_KEY} is
 *     equal to max memory per node of given query
 *   </li>
 *   <li>
 *     Queue whose {@link QueryQueueConfigImpl#MAX_QUERY_MEMORY_PER_NODE_KEY} is
 *     just greater than max memory per node of given query. From all queues whose max_query_memory_per_node is
 *     greater than what is needed by the query, the queue with minimum value is chosen.
 *   </li>
 *   <li>
 *     Queue whose {@link QueryQueueConfigImpl#MAX_QUERY_MEMORY_PER_NODE_KEY} is
 *     just less than max memory per node of given query. From all queues whose max_query_memory_per_node is
 *     less than what is needed by the query, the queue with maximum value is chosen.
 *   </li>
 * </ul>
 */
public class BestFitQueueSelection extends AbstractQueueSelectionPolicy {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BestFitQueueSelection.class);

  public BestFitQueueSelection() {
    super(SelectionPolicy.BESTFIT);
  }

  /**
   * Comparator used to sort the leaf ResourcePool lists based on
   * {@link QueryQueueConfigImpl#MAX_QUERY_MEMORY_PER_NODE_KEY}
   */
  private static class BestFitComparator implements Comparator<ResourcePool> {
    @Override
    public int compare(ResourcePool o1, ResourcePool o2) {
      long pool1Value = o1.getQueryQueue().getMaxQueryMemoryInMBPerNode();
      long pool2Value = o2.getQueryQueue().getMaxQueryMemoryInMBPerNode();
      return Long.compare(pool1Value, pool2Value);
    }
  }

  @Override
  public ResourcePool selectQueue(List<ResourcePool> allPools, QueryContext queryContext,
                                  NodeResources maxResourcePerNode) throws QueueSelectionException {
    if (allPools.isEmpty()) {
      throw new QueueSelectionException(String.format("There are no pools to apply %s selection policy pool for the " +
          "query: %s", getSelectionPolicy().toString(), queryContext.getQueryId()));
    }

    allPools.sort(new BestFitComparator());
    final long queryMaxNodeMemory = maxResourcePerNode.getMemoryInMB();
    ResourcePool selectedPool = allPools.get(0);
    for (ResourcePool pool : allPools) {
      selectedPool = pool;
      long poolMaxNodeMem = pool.getQueryQueue().getMaxQueryMemoryInMBPerNode();
      if (poolMaxNodeMem >= queryMaxNodeMemory) {
        break;
      }
    }
    logger.debug("Selected pool {} based on {} policy for query {}", selectedPool.getPoolName(),
      getSelectionPolicy().toString(),
      queryContext.getQueryId());
    return selectedPool;
  }
}
