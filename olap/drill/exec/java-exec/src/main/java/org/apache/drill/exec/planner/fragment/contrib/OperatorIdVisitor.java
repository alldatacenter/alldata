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

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.physical.base.AbstractPhysicalVisitor;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.SubScan;

/**
 * Visitor to renumber operators - needed after materialization is done as some operators may be removed
 * using @ExtendedMaterializerVisitor
 *
 */
public class OperatorIdVisitor extends AbstractPhysicalVisitor<PhysicalOperator, Integer, ExecutionSetupException> {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OperatorIdVisitor.class);

  public static final OperatorIdVisitor INSTANCE = new OperatorIdVisitor();

  private OperatorIdVisitor() {

  }

  @Override
  public PhysicalOperator visitSubScan(SubScan subScan, Integer parentOpId) throws ExecutionSetupException {
    subScan.setOperatorId(Short.MAX_VALUE & parentOpId+1);
    return subScan;
  }

  @Override
  public PhysicalOperator visitOp(PhysicalOperator op, Integer parentOpId) throws ExecutionSetupException {
    for(PhysicalOperator child : op){
      child.accept(this, parentOpId+1);
    }
    op.setOperatorId(Short.MAX_VALUE & parentOpId+1);
    return op;
  }

}
