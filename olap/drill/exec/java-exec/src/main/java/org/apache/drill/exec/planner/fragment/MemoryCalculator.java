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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.base.Exchange;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.AbstractMuxExchange;
import org.apache.drill.exec.planner.AbstractOpWrapperVisitor;
import org.apache.drill.exec.planner.cost.NodeResource;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A visitor to compute memory requirements for each operator in a minor fragment.
 * This visitor will be called for each major fragment. It traverses the physical operators
 * in that major fragment and computes the memory for each operator per each minor fragment.
 * The minor fragment memory resources are further grouped into per Drillbit resource
 * requirements.
 */
public class MemoryCalculator extends AbstractOpWrapperVisitor<Void, RuntimeException> {

  private final PlanningSet planningSet;
  // List of all the buffered operators and their memory requirement per drillbit.
  private final Map<DrillbitEndpoint, List<Pair<PhysicalOperator, Long>>> bufferedOperators;
  private final QueryContext queryContext;

  public MemoryCalculator(PlanningSet planningSet, QueryContext context) {
    this.planningSet = planningSet;
    this.bufferedOperators = new HashMap<>();
    this.queryContext = context;
  }

  // Helper method to compute the minor fragment count per drillbit. This method returns
  // a map with key as DrillbitEndpoint and value as the width (i.e #minorFragments)
  // per Drillbit.
  private Map<DrillbitEndpoint, Integer> getMinorFragCountPerDrillbit(Wrapper currFragment) {
      return currFragment.getAssignedEndpoints().stream()
                                                .collect(Collectors.groupingBy(Function.identity(),
                                                                               Collectors.summingInt(x -> 1)));
  }

  // Helper method to merge the memory computations for each operator given memory per operator
  // and the number of minor fragments per Drillbit.
  private void merge(Wrapper currFrag,
                     Map<DrillbitEndpoint, Integer> minorFragsPerDrillBit,
                     Function<Entry<DrillbitEndpoint, Integer>, Long> getMemory) {

    NodeResource.merge(currFrag.getResourceMap(),
                       minorFragsPerDrillBit.entrySet()
                                            .stream()
                                            .collect(Collectors.toMap((x) -> x.getKey(),
                                                                      (x) -> NodeResource.create(0,
                                                                                                  getMemory.apply(x)))));
  }

  @Override
  public Void visitSendingExchange(Exchange exchange, Wrapper fragment) throws RuntimeException {
    Wrapper receivingFragment = planningSet.get(fragment.getNode().getSendingExchangePair().getNode());
    merge(fragment,
          getMinorFragCountPerDrillbit(fragment),
          // get the memory requirements for the sender operator.
          (x) -> exchange.getSenderMemory(receivingFragment.getWidth(), x.getValue()));
    return visitOp(exchange, fragment);
  }

  @Override
  public Void visitReceivingExchange(Exchange exchange, Wrapper fragment) throws RuntimeException {

    final List<Fragment.ExchangeFragmentPair> receivingExchangePairs = fragment.getNode().getReceivingExchangePairs();
    final Map<DrillbitEndpoint, Integer> sendingFragsPerDrillBit = new HashMap<>();

    for(Fragment.ExchangeFragmentPair pair : receivingExchangePairs) {
      if (pair.getExchange() == exchange) {
        Wrapper sendingFragment = planningSet.get(pair.getNode());
        Preconditions.checkArgument(sendingFragment.isEndpointsAssignmentDone());
        for (DrillbitEndpoint endpoint : sendingFragment.getAssignedEndpoints()) {
          sendingFragsPerDrillBit.putIfAbsent(endpoint, 0);
          sendingFragsPerDrillBit.put(endpoint, sendingFragsPerDrillBit.get(endpoint)+1);
        }
      }
    }
    final int totalSendingFrags = sendingFragsPerDrillBit.entrySet().stream()
                                                         .mapToInt((x) -> x.getValue()).reduce(0, (x, y) -> x+y);
    merge(fragment,
          getMinorFragCountPerDrillbit(fragment),
          (x) -> exchange.getReceiverMemory(fragment.getWidth(),
            // If the exchange is a MuxExchange then the sending fragments are from that particular drillbit otherwise
            // sending fragments are from across the cluster.
            exchange instanceof AbstractMuxExchange ? sendingFragsPerDrillBit.get(x.getKey()) : totalSendingFrags));
    return null;
  }

  public List<Pair<PhysicalOperator, Long>> getBufferedOperators(DrillbitEndpoint endpoint) {
    return this.bufferedOperators.getOrDefault(endpoint, new ArrayList<>());
  }

  @Override
  public Void visitOp(PhysicalOperator op, Wrapper fragment) {
    long memoryCost = (int) Math.ceil(op.getCost().getMemoryCost());
    if (op.isBufferedOperator(queryContext)) {
      // If the operator is a buffered operator then get the memory estimates of the optimizer.
      // The memory estimates of the optimizer are for the whole operator spread across all the
      // minor fragments. Divide this memory estimation by fragment width to get the memory
      // requirement per minor fragment.
      long memoryCostPerMinorFrag = (int) Math.ceil(memoryCost/fragment.getAssignedEndpoints().size());
      Map<DrillbitEndpoint, Integer> drillbitEndpointMinorFragMap = getMinorFragCountPerDrillbit(fragment);

      Map<DrillbitEndpoint,
          Pair<PhysicalOperator, Long>> bufferedOperatorsPerDrillbit =
                              drillbitEndpointMinorFragMap.entrySet().stream()
                                                          .collect(Collectors.toMap((x) -> x.getKey(),
                                                                              (x) -> Pair.of(op,
                                                                                memoryCostPerMinorFrag * x.getValue())));
      bufferedOperatorsPerDrillbit.entrySet().forEach((x) -> {
        bufferedOperators.putIfAbsent(x.getKey(), new ArrayList<>());
        bufferedOperators.get(x.getKey()).add(x.getValue());
      });

      merge(fragment,
            drillbitEndpointMinorFragMap,
            (x) -> memoryCostPerMinorFrag * x.getValue());

    } else {
      // Memory requirement for the non-buffered operators is just the batch size.
      merge(fragment,
            getMinorFragCountPerDrillbit(fragment),
            (x) -> memoryCost * x.getValue());
    }
    for (PhysicalOperator child : op) {
      child.accept(this, fragment);
    }
    return null;
  }
}

