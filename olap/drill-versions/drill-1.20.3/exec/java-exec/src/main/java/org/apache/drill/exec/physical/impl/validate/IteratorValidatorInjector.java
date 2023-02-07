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
package org.apache.drill.exec.physical.impl.validate;

import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.base.AbstractPhysicalVisitor;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.IteratorValidator;
import org.apache.drill.exec.physical.config.RowKeyJoinPOP;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class IteratorValidatorInjector extends
    AbstractPhysicalVisitor<PhysicalOperator, FragmentContext, ExecutionSetupException> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(IteratorValidatorInjector.class);

  /* This flag when set creates all the validators as repeatable validators */
  private final boolean isRepeatablePipeline;

  public IteratorValidatorInjector() {
    this(false);
  }

  public IteratorValidatorInjector(boolean repeatablePipeline) {
    this.isRepeatablePipeline = repeatablePipeline;
  }

  public static FragmentRoot rewritePlanWithIteratorValidator(FragmentContext context, FragmentRoot root) throws ExecutionSetupException {
    IteratorValidatorInjector inject = new IteratorValidatorInjector();
    PhysicalOperator newOp = root.accept(inject, context);

    if ( !(newOp instanceof FragmentRoot) ) {
      throw new IllegalStateException("This shouldn't happen.");
    }

    return (FragmentRoot) newOp;
  }

  /**
   * Traverse the physical plan and inject the IteratorValidator operator after every operator.
   *
   * @param op
   *          Physical operator under which the IteratorValidator operator will be injected
   * @param context
   *          Fragment context
   * @return same physical operator as passed in, but its child will be a IteratorValidator operator whose child will be the
   *         original child of this operator
   * @throws ExecutionSetupException
   */
  @Override
  public PhysicalOperator visitOp(PhysicalOperator op, FragmentContext context) throws ExecutionSetupException {

    List<PhysicalOperator> newChildren = Lists.newArrayList();
    PhysicalOperator newOp = op;

    if (op instanceof RowKeyJoinPOP) {
      /* create a RepeatablePipeline for the left side of RowKeyJoin */
      PhysicalOperator left = new IteratorValidator(((RowKeyJoinPOP) op).getLeft()
                                   .accept(new IteratorValidatorInjector(true), context), true);
      left.setOperatorId(op.getOperatorId() + 1000);
      newChildren.add(left);
      /* right pipeline is not repeatable pipeline */
      PhysicalOperator right = new IteratorValidator(((RowKeyJoinPOP) op).getRight()
              .accept(this, context));
      right.setOperatorId(op.getOperatorId() + 1000);
      newChildren.add(right);
    } else {
    /* Get the list of child operators */
      for (PhysicalOperator child : op) {
        PhysicalOperator validator = new IteratorValidator(child.accept(this, context), this.isRepeatablePipeline);
        validator.setOperatorId(op.getOperatorId() + 1000);
        newChildren.add(validator);
      }
    }

    /* Inject trace operator */
    if (newChildren.size() > 0) {
      newOp = op.getNewWithChildren(newChildren);
      newOp.setOperatorId(op.getOperatorId());
    }

    return newOp;
  }

}
