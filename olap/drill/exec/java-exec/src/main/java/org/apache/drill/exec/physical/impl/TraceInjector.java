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
package org.apache.drill.exec.physical.impl;

import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.physical.base.AbstractPhysicalVisitor;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.Trace;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;


public class TraceInjector extends AbstractPhysicalVisitor<PhysicalOperator, FragmentContext, ExecutionSetupException> {

    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(TraceInjector.class);

    static int traceTagCount = 0;
    RootExec root = null;

    public static PhysicalOperator getExec(FragmentContext context, FragmentRoot root) throws ExecutionSetupException {
        TraceInjector tI = new TraceInjector();
        PhysicalOperator newOp = root.accept(tI, context);

        return newOp;
    }

    /**
     * Traverse the physical plan and inject the trace operator after
     * every operator.
     * @param op Physical operator under which the trace operator will be injected
     * @param context Fragment context
     * @return same physical operator as passed in, but its child will be a trace operator
     *         whose child will be the original child of this operator
     * @throws ExecutionSetupException
     */
    @Override
    public PhysicalOperator visitOp(PhysicalOperator op, FragmentContext context) throws ExecutionSetupException{

        List<PhysicalOperator> newChildren = Lists.newArrayList();
        List<PhysicalOperator> list = null;
        PhysicalOperator newOp = op;

        /* Get the list of child operators */
        for (PhysicalOperator child : op)
        {
            newChildren.add(child.accept(this, context));
        }

        list = Lists.newArrayList();

        /* For every child operator create a trace operator as its parent */
        for (int i = 0; i < newChildren.size(); i++)
        {
            String traceTag = newChildren.get(i).toString() + Integer.toString(traceTagCount++);

            /* Trace operator */
            Trace traceOp = new Trace(newChildren.get(i), traceTag);
            list.add(traceOp);
        }

        /* Inject trace operator */
        if (list.size() > 0) {
          newOp = op.getNewWithChildren(list);
        }
        newOp.setOperatorId(op.getOperatorId());

        return newOp;
    }
}
