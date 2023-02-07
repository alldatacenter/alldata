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
package org.apache.drill.exec.physical.config;

import java.util.List;

import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.base.PhysicalOperator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("external-sort")
public class ExternalSort extends Sort {

  public static final long DEFAULT_SORT_ALLOCATION = 20_000_000;

  public static final String OPERATOR_TYPE = "EXTERNAL_SORT";

  @JsonCreator
  public ExternalSort(@JsonProperty("child") PhysicalOperator child, @JsonProperty("orderings") List<Ordering> orderings, @JsonProperty("reverse") boolean reverse) {
    super(child, orderings, reverse);
    initialAllocation = DEFAULT_SORT_ALLOCATION;
  }

  @Override
  protected PhysicalOperator getNewWithChild(PhysicalOperator child) {
    ExternalSort newSort = new ExternalSort(child, orderings, reverse);
    newSort.setMaxAllocation(getMaxAllocation());
    return newSort;
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  /**
   *
   * @param maxAllocation The max memory allocation to be set
   */
  @Override
  public void setMaxAllocation(long maxAllocation) {
    this.maxAllocation = maxAllocation;
  }

  /**
   * The External Sort operator supports spilling
   * @return true
   * @param queryContext
   */
  @Override
  public boolean isBufferedOperator(QueryContext queryContext) { return true; }
}
