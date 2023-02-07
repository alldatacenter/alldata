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

import java.util.List;

/**
 * Helps to select the first default queue in the list of all the provided queues. If there is no default queue
 * present it throws {@link QueueSelectionException}. Default queue is a queue associated with {@link ResourcePool}
 * which has {@link org.apache.drill.exec.resourcemgr.config.selectors.DefaultSelector} assigned to it
 */
public class DefaultQueueSelection extends AbstractQueueSelectionPolicy {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DefaultQueueSelection.class);

  public DefaultQueueSelection() {
    super(SelectionPolicy.DEFAULT);
  }

  @Override
  public ResourcePool selectQueue(List<ResourcePool> allPools, QueryContext queryContext,
                                  NodeResources maxResourcePerNode) throws QueueSelectionException {
    for (ResourcePool pool : allPools) {
      if (pool.isDefaultPool()) {
        logger.debug("Selected default pool: {} for the query: {}", pool.getPoolName(), queryContext.getQueryId());
        return pool;
      }
    }

    throw new QueueSelectionException(String.format("There is no default pool to select from list of pools provided " +
      "for the query: %s", queryContext.getQueryId()));
  }
}
