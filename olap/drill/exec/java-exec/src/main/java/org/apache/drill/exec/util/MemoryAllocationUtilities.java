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
package org.apache.drill.exec.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionSet;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

public class MemoryAllocationUtilities {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MemoryAllocationUtilities.class);


  public static void setupBufferedMemoryAllocations(PhysicalPlan plan, final QueryContext queryContext) {
    setupBufferedOpsMemoryAllocations(plan.getProperties().hasResourcePlan,
                                      getBufferedOperators(plan.getSortedOperators(), queryContext), queryContext);
  }

  public static List<PhysicalOperator> getBufferedOperators(List<PhysicalOperator> operators, QueryContext queryContext) {
    final List<PhysicalOperator> bufferedOpList = new ArrayList<>();
    for (final PhysicalOperator op : operators) {
      if (op.isBufferedOperator(queryContext)) {
        bufferedOpList.add(op);
      }
    }
    return bufferedOpList;
  }

  /**
   * Helper method to setup Memory Allocations
   * <p>
   * Plan the memory for buffered operators (the only ones that can spill in this release)
   * based on assumptions. These assumptions are the amount of memory per node to give
   * to each query and the number of sort operators per node.
   * <p>
   * The reason the total
   * memory is an assumption is that we have know knowledge of the number of queries
   * that can run, so we need the user to tell use that information by configuring the
   * amount of memory to be assumed available to each query.
   * <p>
   * The number of sorts per node could be calculated, but we instead simply take
   * the worst case: the maximum per-query, per-node parallization and assume that
   * all sorts appear in all fragments &mdash; a gross oversimplification, but one
   * that Drill has long made.
   * <p>
   * since this method can be used in multiple places adding it in this class
   * rather than keeping it in Foreman
   * @param planHasMemory defines the memory planning needs to be done or not.
   *                             generally skipped when the plan contains memory allocation.
   * @param bufferedOperators list of buffered operators in the plan.
   * @param queryContext context of the query.
   */
  public static void setupBufferedOpsMemoryAllocations(boolean planHasMemory,
    List<PhysicalOperator> bufferedOperators, final QueryContext queryContext) {

    // Test plans may already have a pre-defined memory plan.
    // Otherwise, determine memory allocation.

    if (planHasMemory || bufferedOperators.isEmpty()) {
      return;
    }

    // Setup options, etc.

    final OptionManager optionManager = queryContext.getOptions();
    final long directMemory = DrillConfig.getMaxDirectMemory();

    // Compute per-node, per-query memory.

    final long maxAllocPerNode = computeQueryMemory(queryContext.getConfig(), optionManager, directMemory);
    logger.debug("Memory per query per node: {}", maxAllocPerNode);

    // Now divide up the memory by slices and operators.

    final long opMinMem = computeOperatorMemory(optionManager, maxAllocPerNode, bufferedOperators.size());

    for(final PhysicalOperator op : bufferedOperators) {
      final long alloc = Math.max(opMinMem, op.getInitialAllocation());
      op.setMaxAllocation(alloc);
    }
  }

  /**
   * Compute per-operator memory based on the computed per-node memory, the
   * number of operators, and the computed number of fragments (which house
   * the operators.) Enforces a floor on the amount of memory per operator.
   *
   * @param optionManager system option manager
   * @param maxAllocPerNode computed query memory per node
   * @param opCount number of buffering operators in this query
   * @return the per-operator memory
   */

  public static long computeOperatorMemory(OptionSet optionManager, long maxAllocPerNode, int opCount) {
    final long maxWidth = optionManager.getOption(ExecConstants.MAX_WIDTH_PER_NODE);
    final double cpuLoadAverage = optionManager.getOption(ExecConstants.CPU_LOAD_AVERAGE);
    final long maxWidthPerNode = ExecConstants.MAX_WIDTH_PER_NODE.computeMaxWidth(cpuLoadAverage, maxWidth);
    final long maxOperatorAlloc = maxAllocPerNode / (opCount * maxWidthPerNode);
    logger.debug("Max buffered operator alloc: {}", maxOperatorAlloc);

    // User configurable option to allow forcing minimum memory.
    // Ensure that the buffered ops receive the minimum memory needed to make progress.
    // Without this, the math might work out to allocate too little memory.

    return Math.max(maxOperatorAlloc,
        optionManager.getOption(ExecConstants.MIN_MEMORY_PER_BUFFERED_OP));
  }

  /**
   * Per-node memory calculations based on a number of constraints.
   * <p>
   * Factored out into a separate method to allow unit testing.
   * @param config Drill config
   * @param optionManager system options
   * @param directMemory amount of direct memory
   * @return memory per query per node
   */

  @VisibleForTesting
  public static long computeQueryMemory(DrillConfig config, OptionSet optionManager, long directMemory) {

    // Memory computed as a percent of total memory.

    long perQueryMemory = Math.round(directMemory *
        optionManager.getOption(ExecConstants.PERCENT_MEMORY_PER_QUERY));

    // But, must allow at least the amount given explicitly for
    // backward compatibility.

    perQueryMemory = Math.max(perQueryMemory,
        optionManager.getOption(ExecConstants.MAX_QUERY_MEMORY_PER_NODE));

    // Compute again as either the total direct memory, or the
    // configured maximum top-level allocation (10 GB).

    long maxAllocPerNode = Math.min(directMemory,
        config.getLong(RootAllocatorFactory.TOP_LEVEL_MAX_ALLOC));

    // Final amount per node per query is the minimum of these two.

    maxAllocPerNode = Math.min(maxAllocPerNode, perQueryMemory);
    return maxAllocPerNode;
  }
}
