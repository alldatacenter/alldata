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
import org.apache.drill.exec.resourcemgr.config.ResourcePool;
import org.apache.drill.exec.resourcemgr.config.exception.QueueSelectionException;

import java.util.Collections;
import java.util.List;

/**
 * Randomly selects a queue from the list of all the provided queues. If no pools are provided then it throws
 * {@link QueueSelectionException}
 */
public class RandomQueueSelection extends AbstractQueueSelectionPolicy {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(RandomQueueSelection.class);

  public RandomQueueSelection() {
    super(SelectionPolicy.RANDOM);
  }

  @Override
  public ResourcePool selectQueue(List<ResourcePool> allPools, QueryContext queryContext,
                                  NodeResources maxResourcePerNode) throws QueueSelectionException {
    if (allPools.size() == 0) {
      throw new QueueSelectionException(String.format("Input pool list is empty to apply %s selection policy",
        getSelectionPolicy().toString()));
    }
    Collections.shuffle(allPools);
    ResourcePool selectedPool = allPools.get(0);
    logger.debug("Selected random pool: {} for query: {}", selectedPool.getPoolName(), queryContext.getQueryId());
    return selectedPool;
  }
}
