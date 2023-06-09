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
package org.apache.drill.exec.physical.base;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.drill.common.graph.GraphVisitor;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.planner.cost.PrelCostEstimates;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public abstract class AbstractBase implements PhysicalOperator {

  public static long INIT_ALLOCATION = 1_000_000L;
  public static long MAX_ALLOCATION = 10_000_000_000L;

  protected long initialAllocation = INIT_ALLOCATION;
  protected long maxAllocation = MAX_ALLOCATION;

  protected final String userName;
  private int id;
  private PrelCostEstimates cost = PrelCostEstimates.ZERO_COST;

  public AbstractBase() {
    userName = null;
  }

  public AbstractBase(String userName) {
    this.userName = userName;
  }

  public AbstractBase(AbstractBase that) {
    Preconditions.checkNotNull(that, "Unable to clone: source is null.");
    this.userName = that.userName;
  }

  @Override
  public void accept(GraphVisitor<PhysicalOperator> visitor) {
    visitor.enter(this);
    if (this.iterator() == null) {
      throw new IllegalArgumentException("Null iterator for pop." + this);
    }
    for (PhysicalOperator o : this) {
      Preconditions.checkNotNull(o, String.format("Null in iterator for pop %s.", this));
      o.accept(visitor);
    }
    visitor.leave(this);
  }

  @Override
  public boolean isExecutable() {
    return true;
  }

  @Override
  public final void setOperatorId(int id) {
    this.id = id;
  }

  @Override
  public int getOperatorId() {
    return id;
  }

  @Override
  public SelectionVectorMode getSVMode() {
    return SelectionVectorMode.NONE;
  }

  @Override
  public long getInitialAllocation() {
    return initialAllocation;
  }

  @Override
  public PrelCostEstimates getCost() {
    return cost;
  }

  @Override
  public void setCost(PrelCostEstimates cost) {
    this.cost = cost;
  }

  @Override
  public long getMaxAllocation() {
    return maxAllocation;
  }

  /**
   * Any operator that supports spilling should override this method
   * @param maxAllocation The max memory allocation to be set
   */
  @Override
  public void setMaxAllocation(long maxAllocation) {
    this.maxAllocation = maxAllocation;
  }

  /**
   * Any operator that supports spilling should override this method (and return true)
   * @return false
   * @param queryContext
   */
  @Override @JsonIgnore
  public boolean isBufferedOperator(QueryContext queryContext) { return false; }

  @Override
  public String getUserName() {
    return userName;
  }
}
