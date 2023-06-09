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
package org.apache.drill.exec.planner.cost;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Cost estimates per physical relation. These cost estimations are computed
 * during physical planning by the optimizer. These are also used post physical
 * planning to compute memory requirements in minor fragment generation phase.
 *
 */
@JsonTypeName("cost-estimates")
public class PrelCostEstimates {
  // memory requirement for an operator.
  private final double memoryCost;
  // number of rows that are output by this operator.
  private final double outputRowCount;

  public static PrelCostEstimates ZERO_COST = new PrelCostEstimates(0,0);

  public PrelCostEstimates(@JsonProperty("memoryCost") double memory,
                           @JsonProperty("outputRowCount") double rowCount) {
    this.memoryCost = memory;
    this.outputRowCount = rowCount;
  }

  public double getOutputRowCount() {
    return outputRowCount;
  }

  public double getMemoryCost() {
    return memoryCost;
  }
}