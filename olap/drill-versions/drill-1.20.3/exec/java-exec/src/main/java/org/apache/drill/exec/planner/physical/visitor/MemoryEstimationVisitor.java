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
package org.apache.drill.exec.planner.physical.visitor;

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.cost.DrillCostBase;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.server.options.OptionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.calcite.rel.metadata.RelMetadataQuery;

public class MemoryEstimationVisitor extends BasePrelVisitor<Double, Void, RuntimeException> {

  private static final Logger logger = LoggerFactory.getLogger(MemoryEstimationVisitor.class);

  public static boolean enoughMemory(Prel prel, OptionManager options, int numDrillbits) {
    long allottedMemory = options.getOption(ExecConstants.MAX_QUERY_MEMORY_PER_NODE_KEY).num_val * numDrillbits;
    long estimatedMemory = (long) Math.ceil(prel.accept(new MemoryEstimationVisitor(), null) / (1024.0 * 1024.0));
    estimatedMemory += options.getOption(ExecConstants.NON_BLOCKING_OPERATORS_MEMORY_KEY).num_val * numDrillbits;

    if (estimatedMemory > allottedMemory) {
      logger.debug("Estimated memory (" + estimatedMemory + ") exceeds maximum allowed (" + allottedMemory + ")");
    } else {
      logger.debug("Estimated memory (" + estimatedMemory + ") within maximum allowed (" + allottedMemory + ")");
    }
    return estimatedMemory <= allottedMemory;
  }

  public static Double estimateMemory(Prel prel) {
    return prel.accept(new MemoryEstimationVisitor(), null);
  }

  public MemoryEstimationVisitor() { }

  @Override
  public Double visitPrel(Prel prel, Void value) throws RuntimeException {
    RelMetadataQuery mq = RelMetadataQuery.instance();
    return ((DrillCostBase) mq.getCumulativeCost(prel)).getMemory();
//    return findCost(prel, mq);
  }

  @SuppressWarnings("unused")
  private double findCost(Prel prel, RelMetadataQuery mq) {
    DrillCostBase cost = (DrillCostBase) mq.getNonCumulativeCost(prel);
    double memory = cost.getMemory();

    for (Prel child : prel) {
      memory += findCost(child, mq);
    }
    return memory;
  }
}
