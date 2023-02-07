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

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.physical.base.Exchange;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.HasAffinity;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.Store;
import org.apache.drill.exec.planner.AbstractOpWrapperVisitor;
import org.apache.drill.exec.planner.fragment.Fragment.ExchangeFragmentPair;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import java.util.Collections;
import java.util.List;

/**
 * Visitor to collect stats such as cost and parallelization info of operators within a fragment.
 *
 * All operators have cost associated with them, but only few type of operators such as scan,
 * store and exchanges (both sending and receiving) have parallelization info associated with them.
 */
public class StatsCollector extends AbstractOpWrapperVisitor<Void, RuntimeException> {
  private final PlanningSet planningSet;

  public StatsCollector(final PlanningSet planningSet) {
    this.planningSet = planningSet;
  }

  @Override
  public Void visitSendingExchange(Exchange exchange, Wrapper wrapper) throws RuntimeException {
    // Handle the sending side exchange
    Wrapper receivingFragment = planningSet.get(wrapper.getNode().getSendingExchangePair().getNode());

    // List to contain the endpoints where the fragment that receive data to this fragment are running.
    List<DrillbitEndpoint> receiverEndpoints;
    if (receivingFragment.isEndpointsAssignmentDone()) {
      receiverEndpoints = receivingFragment.getAssignedEndpoints();
    } else {
      receiverEndpoints = Collections.emptyList();
    }

    wrapper.getStats().addParallelizationInfo(exchange.getSenderParallelizationInfo(receiverEndpoints));
    return visitOp(exchange, wrapper);
  }

  @Override
  public Void visitReceivingExchange(Exchange exchange, Wrapper wrapper) throws RuntimeException {
    // Handle the receiving side Exchange

    final List<ExchangeFragmentPair> receivingExchangePairs = wrapper.getNode().getReceivingExchangePairs();

    // List to contain the endpoints where the fragment that send dat to this fragment are running.
    final List<DrillbitEndpoint> sendingEndpoints = Lists.newArrayList();

    for(ExchangeFragmentPair pair : receivingExchangePairs) {
      if (pair.getExchange() == exchange) {
        //This is the child fragment which is sending data to this fragment.
        Wrapper sendingFragment = planningSet.get(pair.getNode());
        if (sendingFragment.isEndpointsAssignmentDone()) {
          sendingEndpoints.addAll(sendingFragment.getAssignedEndpoints());
        }
      }
    }

    wrapper.getStats().addParallelizationInfo(exchange.getReceiverParallelizationInfo(sendingEndpoints));
    // no traversal since it would cross current fragment boundary.
    return null;
  }

  @Override
  public Void visitGroupScan(GroupScan groupScan, Wrapper wrapper) {
    final Stats stats = wrapper.getStats();
    stats.addMaxWidth(groupScan.getMaxParallelizationWidth());
    stats.addMinWidth(groupScan.getMinParallelizationWidth());
    return super.visitGroupScan(groupScan, wrapper);
  }

  @Override
  public Void visitStore(Store store, Wrapper wrapper) {
    wrapper.getStats().addMaxWidth(store.getMaxWidth());
    return super.visitStore(store, wrapper);
  }

  @Override
  public Void visitOp(PhysicalOperator op, Wrapper wrapper) {
    final Stats stats = wrapper.getStats();
    if (op instanceof HasAffinity) {
      final HasAffinity hasAffinity = (HasAffinity)op;
      stats.addEndpointAffinities(hasAffinity.getOperatorAffinity());
      stats.setDistributionAffinity(hasAffinity.getDistributionAffinity());
    }
    stats.addCost(op.getCost().getOutputRowCount());
    for (PhysicalOperator child : op) {
      child.accept(this, wrapper);
    }
    return null;
  }
}
