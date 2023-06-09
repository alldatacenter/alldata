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
package org.apache.drill.exec.work.foreman.rm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.AbstractPhysicalVisitor;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.fragment.QueryParallelizer;
import org.apache.drill.exec.planner.fragment.QueueQueryParallelizer;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.work.QueryWorkUnit;
import org.apache.drill.exec.work.QueryWorkUnit.MinorFragmentDefn;
import org.apache.drill.exec.work.foreman.Foreman;
import org.apache.drill.exec.work.foreman.rm.QueryQueue.QueryQueueException;
import org.apache.drill.exec.work.foreman.rm.QueryQueue.QueueLease;
import org.apache.drill.exec.work.foreman.rm.QueryQueue.QueueTimeoutException;

import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Multimap;

/**
 * Global resource manager that provides basic admission control (AC) via a
 * configured queue: either the Zookeeper-based distributed queue or the
 * in-process embedded Drillbit queue. The queue places an upper limit on the
 * number of running queries. This limit then "slices" memory and CPU between
 * queries: each gets the same share of resources.
 * <p>
 * This is a "basic" implementation. Clearly, a more advanced implementation
 * could look at query cost to determine whether to give a given query more or
 * less than the "standard" share. That is left as a future exercise; in this
 * version we just want to get the basics working.
 * <p>
 * This is the resource manager level. This resource manager is paired with a
 * queue implementation to produce a complete solution. This composition-based
 * approach allows sharing of functionality across queue implementations.
 */

public class ThrottledResourceManager extends AbstractResourceManager {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory
      .getLogger(ThrottledResourceManager.class);

  public static class QueuedResourceAllocator
      implements QueryResourceAllocator {

    protected final ThrottledResourceManager rm;
    protected QueryContext queryContext;
    protected PhysicalPlan plan;
    protected QueryWorkUnit work;
    protected double queryCost;

    protected QueuedResourceAllocator(final ThrottledResourceManager rm,
        QueryContext queryContext) {
      this.rm = rm;
      this.queryContext = queryContext;
    }

    @Override
    public void visitAbstractPlan(PhysicalPlan plan) {
      this.plan = plan;
      queryCost = plan.totalCost();
    }

    @Override
    public void visitPhysicalPlan(final QueryWorkUnit work) {
      this.work = work;
      planMemory();
    }

    private void planMemory() {
      if (plan.getProperties().hasResourcePlan) {
        logger.debug("Memory already planned.");
        return;
      }

      // Group fragments by node.

      Map<String, Collection<PhysicalOperator>> nodeMap = buildBufferedOpMap();

      // Memory must be symmetric to avoid bottlenecks in which one node has
      // sorts (say) with less memory than another, causing skew in data arrival
      // rates for downstream operators.

      int width = countBufferingOperators(nodeMap);

      // Then, share memory evenly across the
      // all sort operators on that node. This handles asymmetric distribution
      // such as occurs if a sort appears in the root fragment (the one with
      // screen),
      // which is never parallelized.

      for (Entry<String, Collection<PhysicalOperator>> entry : nodeMap.entrySet()) {
        planNodeMemory(entry.getKey(), entry.getValue(), width);
      }
    }

    public QueryContext getQueryContext() {
      return queryContext;
    }

    private int countBufferingOperators(
        Map<String, Collection<PhysicalOperator>> nodeMap) {
      int width = 0;
      for (Collection<PhysicalOperator> fragSorts : nodeMap.values()) {
        width = Math.max(width, fragSorts.size());
      }
      return width;
    }

    /**
     * Given the set of buffered operators (from any number of fragments) on a
     * single node, shared the per-query memory equally across all the
     * operators.
     *
     * @param nodeAddr
     * @param bufferedOps
     * @param width
     */

    private void planNodeMemory(String nodeAddr,
        Collection<PhysicalOperator> bufferedOps, int width) {

      // If no buffering operators, nothing to plan.

      if (bufferedOps.isEmpty()) {
        return;
      }

      // Divide node memory evenly among the set of operators, in any minor
      // fragment, on the node. This is not very sophisticated: it does not
      // deal with, say, three stacked sorts in which, if sort A runs, then
      // B may be using memory, but C cannot be active. That kind of analysis
      // is left as a later exercise.

      long nodeMemory = queryMemoryPerNode();

      // Set a floor on the amount of memory per operator based on the
      // configured minimum. This is likely unhelpful because we are trying
      // to work around constrained memory by assuming more than we actually
      // have. This may lead to an OOM at run time.

      long preferredOpMemory = nodeMemory / width;
      long perOpMemory = Math.max(preferredOpMemory, rm.minimumOperatorMemory());
      if (preferredOpMemory < perOpMemory) {
        logger.warn("Preferred per-operator memory: {}, actual amount: {}",
            preferredOpMemory, perOpMemory);
      }
      logger.debug(
          "Query: {}, Node: {}, allocating {} bytes each for {} buffered operator(s).",
          QueryIdHelper.getQueryId(queryContext.getQueryId()), nodeAddr,
          perOpMemory, width);

      for (PhysicalOperator op : bufferedOps) {

        // Limit the memory to the maximum in the plan. Doing so is
        // likely unnecessary, and perhaps harmful, because the pre-planned
        // allocation is the default maximum hard-coded to 10 GB. This means
        // that even if 20 GB is available to the sort, it won't use more
        // than 10GB. This is probably more of a bug than a feature.

        long alloc = Math.min(perOpMemory, op.getMaxAllocation());

        // Place a floor on the memory that is the initial allocation,
        // since we don't want the operator to run out of memory when it
        // first starts.

        alloc = Math.max(alloc, op.getInitialAllocation());

        if (alloc > preferredOpMemory && alloc != perOpMemory) {
          logger.warn("Allocated memory of {} for {} exceeds available memory of {} " +
                      "due to operator minimum",
              alloc, op.getClass().getSimpleName(), preferredOpMemory);
        }
        else if (alloc < preferredOpMemory) {
          logger.warn("Allocated memory of {} for {} is less than available memory " +
              "of {} due to operator limit",
              alloc, op.getClass().getSimpleName(), preferredOpMemory);
        }
        op.setMaxAllocation(alloc);
      }
    }

    protected long queryMemoryPerNode() {
      return rm.defaultQueryMemoryPerNode(plan.totalCost());
    }

    /**
     * Build a list of external sorts grouped by node. We start with a list of
     * minor fragments, each with an endpoint (node). Multiple minor fragments
     * may appear on each node, and each minor fragment may have 0, 1 or more
     * sorts.
     *
     * @return
     */

    private Map<String, Collection<PhysicalOperator>> buildBufferedOpMap() {
      Multimap<String, PhysicalOperator> map = ArrayListMultimap.create();
      getBufferedOps(map, work.getRootFragmentDefn());
      for (MinorFragmentDefn defn : work.getMinorFragmentDefns()) {
        getBufferedOps(map, defn);
      }
      return map.asMap();
    }

    /**
     * Searches a fragment operator tree to find buffered within that fragment.
     */

    protected static class BufferedOpFinder extends
        AbstractPhysicalVisitor<Void, List<PhysicalOperator>, RuntimeException> {
      @Override
      public Void visitOp(PhysicalOperator op, List<PhysicalOperator> value)
          throws RuntimeException {
        if (op.isBufferedOperator(null)) {
          value.add(op);
        }
        visitChildren(op, value);
        return null;
      }
    }

    private void getBufferedOps(Multimap<String, PhysicalOperator> map,
        MinorFragmentDefn defn) {
      List<PhysicalOperator> bufferedOps = getBufferedOps(defn.root());
      if (!bufferedOps.isEmpty()) {
        map.putAll(defn.fragment().getAssignment().getAddress(), bufferedOps);
      }
    }

    /**
     * Search an individual fragment tree to find any buffered operators it may
     * contain.
     *
     * @param root
     * @return
     */

    private List<PhysicalOperator> getBufferedOps(FragmentRoot root) {
      List<PhysicalOperator> bufferedOps = new ArrayList<>();
      BufferedOpFinder finder = new BufferedOpFinder();
      root.accept(finder, bufferedOps);
      return bufferedOps;
    }
  }

  /**
   * Per-query resource manager. Handles resources and optional queue lease for
   * a single query. As such, this is a non-shared resource: it is associated
   * with a Foreman: a single thread at plan time, and a single event (in some
   * thread) at query completion time. Because of these semantics, no
   * synchronization is needed within this class.
   */

  public static class QueuedQueryResourceManager extends QueuedResourceAllocator
      implements QueryResourceManager {

    private final Foreman foreman;
    private QueueLease lease;

    public QueuedQueryResourceManager(final ThrottledResourceManager rm,
        final Foreman foreman) {
      super(rm, foreman.getQueryContext());
      this.foreman = foreman;
    }

    @Override
    public void setCost(double cost) {
      this.queryCost = cost;
    }

    @Override
    public QueryParallelizer getParallelizer(boolean planHasMemory) {
      // currently memory planning is disabled. Enable it once the RM functionality is fully implemented.
      return new QueueQueryParallelizer(true || planHasMemory, this.getQueryContext());
    }

    @Override
    public void admit() throws QueueTimeoutException, QueryQueueException {
      lease = rm.queue().enqueue(foreman.getQueryId(), queryCost);
    }

    @Override
    protected long queryMemoryPerNode() {

      // No lease: use static estimate.

      if (lease == null) {
        return super.queryMemoryPerNode();
      }

      // Use actual memory assigned to this query.

      return lease.queryMemoryPerNode();
    }

    @Override
    public void exit() {
      if (lease != null) {
        lease.release();
      }
      lease = null;
    }

    @Override
    public boolean hasQueue() { return true; }

    @Override
    public String queueName() {
      return lease == null ? null : lease.queueName();
    }
  }

  private final QueryQueue queue;

  public ThrottledResourceManager(final DrillbitContext drillbitContext,
      final QueryQueue queue) {
    super(drillbitContext);
    this.queue = queue;
    queue.setMemoryPerNode(memoryPerNode());
  }

  public long minimumOperatorMemory() {
    return queue.minimumOperatorMemory();
  }

  public long defaultQueryMemoryPerNode(double cost) {
    return queue.defaultQueryMemoryPerNode(cost);
  }

  public QueryQueue queue() { return queue; }

  @Override
  public QueryResourceAllocator newResourceAllocator(
      QueryContext queryContext) {
    return new QueuedResourceAllocator(this, queryContext);
  }

  @Override
  public QueryResourceManager newQueryRM(Foreman foreman) {
    return new QueuedQueryResourceManager(this, foreman);
  }

  @Override
  public void close() {
    queue.close();
  }
}
