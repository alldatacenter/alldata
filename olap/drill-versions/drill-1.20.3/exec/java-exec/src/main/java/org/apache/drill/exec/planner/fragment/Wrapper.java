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

import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.drill.exec.planner.cost.NodeResource;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.exec.physical.PhysicalOperatorSetupException;
import org.apache.drill.exec.physical.base.AbstractPhysicalVisitor;
import org.apache.drill.exec.physical.base.Exchange;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.Store;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.planner.fragment.Fragment.ExchangeFragmentPair;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Wrapper class that allows us to add additional information to each fragment
 * node for planning purposes.
 */
public class Wrapper {

  private final Fragment node;
  private final int majorFragmentId;
  private int width = -1;
  private final Stats stats;
  private boolean endpointsAssigned;
  private long initialAllocation;
  private long maxAllocation;
  // Resources (i.e memory and cpu) are stored per drillbit in this map.
  // A Drillbit can have n number of minor fragments then the NodeResource
  // contains cumulative resources required for all the minor fragments
  // for that major fragment on that Drillbit.
  private Map<DrillbitEndpoint, NodeResource> nodeResourceMap;

  // List of fragments this particular fragment depends on for determining its
  // parallelization and endpoint assignments.
  private final List<Wrapper> fragmentDependencies = Lists.newArrayList();

  // List of assigned endpoints. Technically, there could repeated endpoints
  // in this list if we'd like to assign the
  // same fragment multiple times to the same endpoint.
  private final List<DrillbitEndpoint> endpoints = Lists.newLinkedList();

  public Wrapper(Fragment node, int majorFragmentId) {
    this.majorFragmentId = majorFragmentId;
    this.node = node;
    this.stats = new Stats();
  }

  public Stats getStats() {
    return stats;
  }

  public void resetAllocation() {
    initialAllocation = 0;
    maxAllocation = 0;
  }

  public int getMajorFragmentId() {
    return majorFragmentId;
  }

  public int getWidth() {
    return width;
  }

  public void setWidth(int width) {
    Preconditions.checkState(this.width == -1);
    this.width = width;
  }

  public Fragment getNode() {
    return node;
  }

  public long getInitialAllocation() {
    return initialAllocation;
  }

  public long getMaxAllocation() {
    return maxAllocation;
  }

  public void addInitialAllocation(long memory) {
    initialAllocation += memory;
  }

  public void addMaxAllocation(long memory) {
    maxAllocation += memory;
  }

  private class AssignEndpointsToScanAndStore extends
      AbstractPhysicalVisitor<Void, List<DrillbitEndpoint>, PhysicalOperatorSetupException> {

    @Override
    public Void visitExchange(Exchange exchange, List<DrillbitEndpoint> value)
        throws PhysicalOperatorSetupException {
      if (exchange == node.getSendingExchange()) {
        return visitOp(exchange, value);
      }
      // stop on receiver exchange.
      return null;
    }

    @Override
    public Void visitGroupScan(GroupScan groupScan,
        List<DrillbitEndpoint> value) throws PhysicalOperatorSetupException {
      groupScan.applyAssignments(value);
      return super.visitGroupScan(groupScan, value);
    }

    @Override
    public Void visitSubScan(SubScan subScan, List<DrillbitEndpoint> value)
        throws PhysicalOperatorSetupException {
      // TODO - implement this
      return visitOp(subScan, value);
    }

    @Override
    public Void visitStore(Store store, List<DrillbitEndpoint> value)
        throws PhysicalOperatorSetupException {
      store.applyAssignments(value);
      return super.visitStore(store, value);
    }

    @Override
    public Void visitOp(PhysicalOperator op, List<DrillbitEndpoint> value)
        throws PhysicalOperatorSetupException {
      return visitChildren(op, value);
    }

  }

  public void assignEndpoints(List<DrillbitEndpoint> assignedEndpoints)
      throws PhysicalOperatorSetupException {
    Preconditions.checkState(!endpointsAssigned);
    endpointsAssigned = true;

    endpoints.addAll(assignedEndpoints);

    // Set scan and store endpoints.
    AssignEndpointsToScanAndStore visitor = new AssignEndpointsToScanAndStore();
    node.getRoot().accept(visitor, endpoints);

    // Set the endpoints for this (one at most) sending exchange.
    if (node.getSendingExchange() != null) {
      node.getSendingExchange().setupSenders(majorFragmentId, endpoints);
    }

    // Set the endpoints for each incoming exchange within this fragment.
    for (ExchangeFragmentPair e : node.getReceivingExchangePairs()) {
      e.getExchange().setupReceivers(majorFragmentId, endpoints);
    }
  }

  @Override
  public String toString() {
    return "FragmentWrapper [majorFragmentId=" + majorFragmentId + ", width="
        + width + ", stats=" + stats + "]";
  }

  public List<DrillbitEndpoint> getAssignedEndpoints() {
    Preconditions.checkState(endpointsAssigned);
    return ImmutableList.copyOf(endpoints);
  }

  public DrillbitEndpoint getAssignedEndpoint(int minorFragmentId) {
    Preconditions.checkState(endpointsAssigned);
    return endpoints.get(minorFragmentId);
  }

  /**
   * Add a parallelization dependency on given fragment.
   *
   * @param dependsOn
   */
  public void addFragmentDependency(Wrapper dependsOn) {
    fragmentDependencies.add(dependsOn);
  }

  /**
   * Is the endpoints assignment done for this fragment?
   *
   * @return True if the endpoints assignment done for this fragment. False
   *         otherwise.
   */
  public boolean isEndpointsAssignmentDone() {
    return endpointsAssigned;
  }

  /**
   * Get the list of fragements this particular fragment depends on.
   *
   * @return The list of fragements this particular fragment depends on.
   */
  public List<Wrapper> getFragmentDependencies() {
    return ImmutableList.copyOf(fragmentDependencies);
  }

  /**
   * Compute the cpu resources required for all the minor fragments of this
   * major fragment. This information is stored per DrillbitEndpoint. It is
   * assumed that this function is called only once.
   */
  public void computeCpuResources() {
    Preconditions.checkArgument(nodeResourceMap == null);
    BinaryOperator<NodeResource> merge = (first, second) -> {
      NodeResource result = NodeResource.create();
      result.add(first);
      result.add(second);
      return result;
    };

    Function<DrillbitEndpoint, NodeResource> cpuPerEndpoint =
        endpoint -> new NodeResource(1, 0);

    nodeResourceMap = endpoints.stream()
        .collect(Collectors.groupingBy(Function.identity(),
            Collectors.reducing(NodeResource.create(), cpuPerEndpoint, merge)));
  }

  public Map<DrillbitEndpoint, NodeResource> getResourceMap() {
    return nodeResourceMap;
  }
}
