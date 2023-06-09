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
package org.apache.drill.exec.planner.fragment.contrib;

import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.exception.FragmentSetupException;
import org.apache.drill.exec.physical.PhysicalOperatorSetupException;
import org.apache.drill.exec.physical.base.AbstractPhysicalVisitor;
import org.apache.drill.exec.physical.base.Exchange;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.Store;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.planner.fragment.Materializer;
import org.apache.drill.exec.planner.fragment.Materializer.IndexedFragmentNode;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Materializer visitor to remove exchange(s)
 * NOTE: this Visitor does NOT set OperatorId, as after Exchange removal all operators need renumbering
 * Use OperatorIdVisitor on top to set correct OperatorId
 */
public class ExchangeRemoverMaterializer extends AbstractPhysicalVisitor<PhysicalOperator, Materializer.IndexedFragmentNode, ExecutionSetupException> {

  //private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ExchangeRemoverMaterializer.class);

  public static final ExchangeRemoverMaterializer INSTANCE = new ExchangeRemoverMaterializer();

  private ExchangeRemoverMaterializer() {

  }

  @Override
  public PhysicalOperator visitExchange(Exchange exchange, IndexedFragmentNode iNode) throws ExecutionSetupException {
    iNode.addAllocation(exchange);
    PhysicalOperator childEx = exchange.getChild().accept(this, iNode);
    return childEx;
  }

  @Override
  public PhysicalOperator visitGroupScan(GroupScan groupScan, IndexedFragmentNode iNode) throws ExecutionSetupException {
    PhysicalOperator child = groupScan.getSpecificScan(iNode.getMinorFragmentId());
    return child;
  }

  @Override
  public PhysicalOperator visitSubScan(SubScan subScan, IndexedFragmentNode value) throws ExecutionSetupException {
    value.addAllocation(subScan);
    // TODO - implement this
    return super.visitOp(subScan, value);
  }

  @Override
  public PhysicalOperator visitStore(Store store, IndexedFragmentNode iNode) throws ExecutionSetupException {
    PhysicalOperator child = store.getChild().accept(this, iNode);

    iNode.addAllocation(store);

    try {
      PhysicalOperator o = store.getSpecificStore(child, iNode.getMinorFragmentId());
      return o;
    } catch (PhysicalOperatorSetupException e) {
      throw new FragmentSetupException("Failure while generating a specific Store materialization.", e);
    }
  }

  @Override
  public PhysicalOperator visitOp(PhysicalOperator op, IndexedFragmentNode iNode) throws ExecutionSetupException {
    iNode.addAllocation(op);
    List<PhysicalOperator> children = Lists.newArrayList();
    for(PhysicalOperator child : op){
      children.add(child.accept(this, iNode));
    }
    PhysicalOperator newOp = op.getNewWithChildren(children);
    newOp.setCost(op.getCost());
    return newOp;
  }
}
