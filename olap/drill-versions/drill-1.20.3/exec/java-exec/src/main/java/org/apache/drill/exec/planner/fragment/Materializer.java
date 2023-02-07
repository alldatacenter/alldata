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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.BiFunction;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.exception.FragmentSetupException;
import org.apache.drill.exec.physical.PhysicalOperatorSetupException;
import org.apache.drill.exec.physical.base.AbstractPhysicalVisitor;
import org.apache.drill.exec.physical.base.Exchange;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.Store;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.exec.physical.config.LateralJoinPOP;
import org.apache.drill.exec.physical.config.UnnestPOP;
import org.apache.drill.exec.physical.config.RowKeyJoinPOP;

public class Materializer extends AbstractPhysicalVisitor<PhysicalOperator, Materializer.IndexedFragmentNode, ExecutionSetupException>{

  public static final Materializer INSTANCE = new Materializer();

  private Materializer() { }

  @Override
  public PhysicalOperator visitExchange(Exchange exchange, IndexedFragmentNode iNode) throws ExecutionSetupException {
    iNode.addAllocation(exchange);
    if (exchange == iNode.getNode().getSendingExchange()) {

      // this is a sending exchange.
      PhysicalOperator child = exchange.getChild().accept(this, iNode);
      PhysicalOperator materializedSender = exchange.getSender(iNode.getMinorFragmentId(), child);
      materializedSender.setOperatorId(0);
      materializedSender.setCost(exchange.getCost());
      return materializedSender;

    } else {
      // receiving exchange.
      PhysicalOperator materializedReceiver = exchange.getReceiver(iNode.getMinorFragmentId());
      materializedReceiver.setOperatorId(Short.MAX_VALUE & exchange.getOperatorId());
      materializedReceiver.setCost(exchange.getCost());
      return materializedReceiver;
    }
  }

  @Override
  public PhysicalOperator visitGroupScan(GroupScan groupScan, IndexedFragmentNode iNode) throws ExecutionSetupException {
    SubScan child = groupScan.getSpecificScan(iNode.getMinorFragmentId());
    child.setOperatorId(Short.MAX_VALUE & groupScan.getOperatorId());
    // remember the subscan for future use
    iNode.addSubScan(child);
    return child;
  }

  @Override
  public PhysicalOperator visitSubScan(SubScan subScan, IndexedFragmentNode value) throws ExecutionSetupException {
    value.addAllocation(subScan);
    // remember the subscan for future use
    value.addSubScan(subScan);
    // TODO - implement this
    return super.visitOp(subScan, value);
  }

  @Override
  public PhysicalOperator visitStore(Store store, IndexedFragmentNode iNode) throws ExecutionSetupException {
    PhysicalOperator child = store.getChild().accept(this, iNode);

    iNode.addAllocation(store);

    try {
      PhysicalOperator o = store.getSpecificStore(child, iNode.getMinorFragmentId());
      o.setOperatorId(Short.MAX_VALUE & store.getOperatorId());
      return o;
    } catch (PhysicalOperatorSetupException e) {
      throw new FragmentSetupException("Failure while generating a specific Store materialization.");
    }
  }

  @Override
  public PhysicalOperator visitOp(PhysicalOperator op, IndexedFragmentNode iNode) throws ExecutionSetupException {
    iNode.addAllocation(op);
    List<PhysicalOperator> children = Lists.newArrayList();
    for (PhysicalOperator child : op) {
      children.add(child.accept(this, iNode));
    }
    PhysicalOperator newOp = op.getNewWithChildren(children);
    newOp.setCost(op.getCost());
    newOp.setOperatorId(Short.MAX_VALUE & op.getOperatorId());
    return newOp;
  }

  @Override
  public PhysicalOperator visitLateralJoin(LateralJoinPOP op, IndexedFragmentNode iNode) throws ExecutionSetupException {
    iNode.addAllocation(op);
    List<PhysicalOperator> children = Lists.newArrayList();

    children.add(op.getLeft().accept(this, iNode));
    children.add(op.getRight().accept(this, iNode));
    UnnestPOP unnestForThisLateral = iNode.getUnnest();

    PhysicalOperator newOp = op.getNewWithChildren(children);
    newOp.setCost(op.getCost());
    newOp.setOperatorId(Short.MAX_VALUE & op.getOperatorId());

    ((LateralJoinPOP) newOp).setUnnestForLateralJoin(unnestForThisLateral);
    return newOp;
  }

  @Override
  public PhysicalOperator visitUnnest(UnnestPOP unnest, IndexedFragmentNode value) throws ExecutionSetupException {
    PhysicalOperator newOp = visitOp(unnest, value);
    value.addUnnest((UnnestPOP) newOp);
    return newOp;
  }

  @Override
  public PhysicalOperator visitRowKeyJoin(RowKeyJoinPOP op, IndexedFragmentNode iNode) throws ExecutionSetupException {
    iNode.addAllocation(op);
    List<PhysicalOperator> children = Lists.newArrayList();

    children.add(op.getLeft().accept(this, iNode));

    // keep track of the subscan in left input before visiting the right input such that subsequently we can
    // use it for the rowkey join
    SubScan subScanInLeftInput = iNode.getSubScan();

    children.add(op.getRight().accept(this, iNode));

    PhysicalOperator newOp = op.getNewWithChildren(children);
    newOp.setCost(op.getCost());
    newOp.setOperatorId(Short.MAX_VALUE & op.getOperatorId());

    ((RowKeyJoinPOP)newOp).setSubScanForRowKeyJoin(subScanInLeftInput);

    return newOp;
  }

  public static class IndexedFragmentNode{
    private final Wrapper info;
    private final BiFunction<Wrapper, Integer, DrillbitEndpoint> endpoint;
    private final int minorFragmentId;
    private final BiFunction<DrillbitEndpoint, PhysicalOperator, Long> memoryPerOperPerDrillbit;
    SubScan subScan;

    private final Deque<UnnestPOP> unnest = new ArrayDeque<>();

    public IndexedFragmentNode(int minorFragmentId, Wrapper info,
                               BiFunction<Wrapper, Integer, DrillbitEndpoint> wrapperToEndpoint,
                               BiFunction<DrillbitEndpoint, PhysicalOperator, Long> memoryReqs) {
      super();
      this.info = info;
      this.endpoint = wrapperToEndpoint;
      this.minorFragmentId = minorFragmentId;
      this.memoryPerOperPerDrillbit = memoryReqs;
    }

    public Fragment getNode() {
      return info.getNode();
    }

    public int getMinorFragmentId() {
      return minorFragmentId;
    }

    public Wrapper getInfo() {
      return info;
    }

    public void addAllocation(PhysicalOperator pop) {
      info.addInitialAllocation(pop.getInitialAllocation());
      long maxAllocation = memoryPerOperPerDrillbit.apply(this.endpoint.apply(info, minorFragmentId), pop);
      info.addMaxAllocation(maxAllocation);
      pop.setMaxAllocation(maxAllocation);
    }

    public void addUnnest(UnnestPOP unnest) {
      this.unnest.addFirst(unnest);
    }

    public UnnestPOP getUnnest() {
      return this.unnest.removeFirst();
    }

    public void addSubScan(SubScan subScan) {
      this.subScan = subScan;
    }

    public SubScan getSubScan() {
      return this.subScan;
    }
  }
}
