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
package org.apache.drill.exec.planner.fragment;

import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.util.MemoryAllocationUtilities;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;

/**
 * Non RM version of the parallelizer. The parallelization logic is fully inherited from SimpleParallelizer.
 * The memory computation of the operators is based on the earlier logic to assign memory for the buffered
 * operators.
 */
public class DefaultQueryParallelizer extends SimpleParallelizer {
  private final boolean planHasMemory;
  private final QueryContext queryContext;

  public DefaultQueryParallelizer(boolean memoryAvailableInPlan, QueryContext queryContext) {
    super(queryContext);
    this.planHasMemory = memoryAvailableInPlan;
    this.queryContext = queryContext;
  }

  public DefaultQueryParallelizer(boolean memoryPlanning, long parallelizationThreshold, int maxWidthPerNode,
                                  int maxGlobalWidth, double affinityFactor) {
    super(parallelizationThreshold, maxWidthPerNode, maxGlobalWidth, affinityFactor);
    this.planHasMemory = memoryPlanning;
    this.queryContext = null;
  }

  @Override
  public void adjustMemory(PlanningSet planningSet, Set<Wrapper> roots,
                           Collection<DrillbitEndpoint> activeEndpoints) {
    if (planHasMemory) {
      return;
    }
    List<PhysicalOperator> bufferedOpers = planningSet.getRootWrapper().getNode().getBufferedOperators(queryContext);
    MemoryAllocationUtilities.setupBufferedOpsMemoryAllocations(planHasMemory, bufferedOpers, queryContext);
  }

  @Override
  protected BiFunction<DrillbitEndpoint, PhysicalOperator, Long> getMemory() {
    return (endpoint, operator) -> operator.getMaxAllocation();
  }
}
