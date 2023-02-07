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

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.fragment.QueryParallelizer;
import org.apache.drill.exec.planner.fragment.DefaultQueryParallelizer;
import org.apache.drill.exec.util.MemoryAllocationUtilities;
import org.apache.drill.exec.work.QueryWorkUnit;
import org.apache.drill.exec.work.foreman.Foreman;

/**
 * Represents a default resource manager for clusters that do not provide query
 * queues. Without queues to provide a hard limit on the query admission rate,
 * the number of active queries must be estimated and the resulting resource
 * allocations will be rough estimates.
 */

public class DefaultResourceManager implements ResourceManager {

  public static class DefaultResourceAllocator implements QueryResourceAllocator {

    private QueryContext queryContext;

    protected DefaultResourceAllocator(QueryContext queryContext) {
      this.queryContext = queryContext;
    }

    @Override
    public void visitAbstractPlan(PhysicalPlan plan) {
      if (plan == null || plan.getProperties().hasResourcePlan) {
        return;
      }
      MemoryAllocationUtilities.setupBufferedMemoryAllocations(plan, queryContext);
    }

    @Override
    public void visitPhysicalPlan(QueryWorkUnit work) {
    }

    public QueryContext getQueryContext() {
      return queryContext;
    }
  }

  public static class DefaultQueryResourceManager extends DefaultResourceAllocator implements QueryResourceManager {

    @SuppressWarnings("unused")
    private final DefaultResourceManager rm;

    public DefaultQueryResourceManager(final DefaultResourceManager rm, final Foreman foreman) {
      super(foreman.getQueryContext());
      this.rm = rm;
    }

    @Override
    public void setCost(double cost) {
      // Nothing to do by default.
    }

    @Override
    public QueryParallelizer getParallelizer(boolean memoryPlanning){
      return new DefaultQueryParallelizer(memoryPlanning, this.getQueryContext());
    }

    @Override
    public void admit() {
      // No queueing by default
    }

    @Override
    public void exit() {
      // No queueing by default
    }

    @Override
    public boolean hasQueue() { return false; }

    @Override
    public String queueName() { return null; }
  }

  public final long memoryPerNode;
  public final int cpusPerNode;

  public DefaultResourceManager() {
    memoryPerNode = DrillConfig.getMaxDirectMemory();

    // Note: CPUs are not yet used, they will be used in a future
    // enhancement.

    cpusPerNode = Runtime.getRuntime().availableProcessors();
  }

  @Override
  public long memoryPerNode() { return memoryPerNode; }

  @Override
  public int cpusPerNode() { return cpusPerNode; }

  @Override
  public QueryResourceAllocator newResourceAllocator(QueryContext queryContext) {
    return new DefaultResourceAllocator(queryContext);
  }

  @Override
  public QueryResourceManager newQueryRM(final Foreman foreman) {
    return new DefaultQueryResourceManager(this, foreman);
  }

  @Override
  public void close() { }
}
