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
import org.apache.drill.common.util.function.CheckedConsumer;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalOperatorSetupException;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.cost.NodeResource;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import java.util.Map;
import java.util.HashMap;
import java.util.Collection;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Paralellizer specialized for managing resources for a query based on Queues. This parallelizer
 * does not deal with increase/decrease of the parallelization of a query plan based on the current
 * cluster state. However, the memory assignment for each operator, minor fragment and major
 * fragment is based on the cluster state and provided queue configuration.
 */
public class QueueQueryParallelizer extends SimpleParallelizer {
  private final boolean planHasMemory;
  private final QueryContext queryContext;
  private final Map<DrillbitEndpoint, Map<PhysicalOperator, Long>> operators;

  public QueueQueryParallelizer(boolean memoryPlanning, QueryContext queryContext) {
    super(queryContext);
    this.planHasMemory = memoryPlanning;
    this.queryContext = queryContext;
    this.operators = new HashMap<>();
  }

  // return the memory computed for a physical operator on a drillbitendpoint.
  public BiFunction<DrillbitEndpoint, PhysicalOperator, Long> getMemory() {
    return (endpoint, operator) -> {
      if (!planHasMemory) {
        return operators.get(endpoint).get(operator);
      }
      else {
        return operator.getMaxAllocation();
      }
    };
  }

  /**
   * Function called by the SimpleParallelizer to adjust the memory post parallelization.
   * The overall logic is to traverse the fragment tree and call the MemoryCalculator on
   * each major fragment. Once the memory is computed, resource requirement are accumulated
   * per drillbit.
   *
   * The total resource requirements are used to select a queue. If the selected queue's
   * resource limit is more/less than the query's requirement than the memory will be re-adjusted.
   *
   * @param planningSet context of the fragments.
   * @param roots root fragments.
   * @param activeEndpoints currently active endpoints.
   * @throws PhysicalOperatorSetupException
   */
  public void adjustMemory(PlanningSet planningSet, Set<Wrapper> roots,
                           Collection<DrillbitEndpoint> activeEndpoints) throws PhysicalOperatorSetupException {

    if (planHasMemory) {
      return;
    }
    // total node resources for the query plan maintained per drillbit.
    final Map<DrillbitEndpoint, NodeResource> totalNodeResources =
            activeEndpoints.stream().collect(Collectors.toMap(x ->x,
                                                              x -> NodeResource.create()));

    // list of the physical operators and their memory requirements per drillbit.
    final Map<DrillbitEndpoint, List<Pair<PhysicalOperator, Long>>> operators =
            activeEndpoints.stream().collect(Collectors.toMap(x -> x,
                                                              x -> new ArrayList<>()));

    for (Wrapper wrapper : roots) {
      traverse(wrapper, CheckedConsumer.throwingConsumerWrapper((Wrapper fragment) -> {
        MemoryCalculator calculator = new MemoryCalculator(planningSet, queryContext);
        fragment.getNode().getRoot().accept(calculator, fragment);
        NodeResource.merge(totalNodeResources, fragment.getResourceMap());
        operators.entrySet()
                  .stream()
                  .forEach((entry) -> entry.getValue()
                                           .addAll(calculator.getBufferedOperators(entry.getKey())));
      }));
    }
    //queryrm.selectQueue( pass the max node Resource) returns queue configuration.
    Map<DrillbitEndpoint, List<Pair<PhysicalOperator, Long>>> memoryAdjustedOperators = ensureOperatorMemoryWithinLimits(operators, totalNodeResources, 10);
    memoryAdjustedOperators.entrySet().stream().forEach((x) -> {
      Map<PhysicalOperator, Long> memoryPerOperator = x.getValue().stream()
                                                                  .collect(Collectors.toMap(operatorLongPair -> operatorLongPair.getLeft(),
                                                                                            operatorLongPair -> operatorLongPair.getRight(),
                                                                                            (mem_1, mem_2) -> (mem_1 + mem_2)));
      this.operators.put(x.getKey(), memoryPerOperator);
    });
  }


  /**
   * Helper method to adjust the memory for the buffered operators.
   * @param memoryPerOperator list of physical operators per drillbit
   * @param nodeResourceMap resources per drillbit.
   * @param nodeLimit permissible node limit.
   * @return list of operators which contain adjusted memory limits.
   */
  private Map<DrillbitEndpoint, List<Pair<PhysicalOperator, Long>>>
          ensureOperatorMemoryWithinLimits(Map<DrillbitEndpoint, List<Pair<PhysicalOperator, Long>>> memoryPerOperator,
                                           Map<DrillbitEndpoint, NodeResource> nodeResourceMap, int nodeLimit) {
    // Get the physical operators which are above the node memory limit.
    Map<DrillbitEndpoint, List<Pair<PhysicalOperator, Long>>> onlyMemoryAboveLimitOperators = new HashMap<>();
    memoryPerOperator.entrySet().stream().forEach((entry) -> {
      onlyMemoryAboveLimitOperators.putIfAbsent(entry.getKey(), new ArrayList<>());
      if (nodeResourceMap.get(entry.getKey()).getMemory() > nodeLimit) {
        onlyMemoryAboveLimitOperators.get(entry.getKey()).addAll(entry.getValue());
      }
    });


    // Compute the total memory required by the physical operators on the drillbits which are above node limit.
    // Then use the total memory to adjust the memory requirement based on the permissible node limit.
    Map<DrillbitEndpoint, List<Pair<PhysicalOperator, Long>>> memoryAdjustedDrillbits = new HashMap<>();
    onlyMemoryAboveLimitOperators.entrySet().stream().forEach(
      entry -> {
        Long totalMemory = entry.getValue().stream().mapToLong(Pair::getValue).sum();
        List<Pair<PhysicalOperator, Long>> adjustedMemory = entry.getValue().stream().map(operatorMemory -> {
          // formula to adjust the memory is (optimalMemory / totalMemory(this is for all the operators)) * permissible_node_limit.
          return Pair.of(operatorMemory.getKey(), (long) Math.ceil(operatorMemory.getValue()/totalMemory * nodeLimit));
        }).collect(Collectors.toList());
        memoryAdjustedDrillbits.put(entry.getKey(), adjustedMemory);
      }
    );

    // Get all the operations on drillbits which were adjusted for memory and merge them with operators which are not
    // adjusted for memory.
    Map<DrillbitEndpoint, List<Pair<PhysicalOperator, Long>>> allDrillbits = new HashMap<>();
    memoryPerOperator.entrySet().stream().filter((entry) -> !memoryAdjustedDrillbits.containsKey(entry.getKey())).forEach(
      operatorMemory -> {
        allDrillbits.put(operatorMemory.getKey(), operatorMemory.getValue());
      }
    );

    memoryAdjustedDrillbits.entrySet().stream().forEach(
      operatorMemory -> {
        allDrillbits.put(operatorMemory.getKey(), operatorMemory.getValue());
      }
    );

    // At this point allDrillbits contains the operators on all drillbits. The memory also is adjusted based on the nodeLimit and
    // the ratio of their requirements.
    return allDrillbits;
  }
}