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
package org.apache.drill.common.logical.data.visitors;

import org.apache.drill.common.logical.data.LateralJoin;
import org.apache.drill.common.logical.data.Unnest;
import org.apache.drill.common.logical.data.Analyze;
import org.apache.drill.common.logical.data.Values;
import org.apache.drill.common.logical.data.Filter;
import org.apache.drill.common.logical.data.Flatten;
import org.apache.drill.common.logical.data.GroupingAggregate;
import org.apache.drill.common.logical.data.Join;
import org.apache.drill.common.logical.data.Limit;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.Order;
import org.apache.drill.common.logical.data.Project;
import org.apache.drill.common.logical.data.RunningAggregate;
import org.apache.drill.common.logical.data.Scan;
import org.apache.drill.common.logical.data.Store;
import org.apache.drill.common.logical.data.Transform;
import org.apache.drill.common.logical.data.Union;
import org.apache.drill.common.logical.data.Window;
import org.apache.drill.common.logical.data.Writer;


public abstract class AbstractLogicalVisitor<T, X, E extends Throwable> implements LogicalVisitor<T, X, E> {

    public T visitOp(LogicalOperator op, X value) throws E{
        throw new UnsupportedOperationException(String.format(
                "The LogicalVisitor of type %s does not currently support visiting the PhysicalOperator type %s.", this
                .getClass().getCanonicalName(), op.getClass().getCanonicalName()));
    }

    @Override
    public T visitAnalyze(Analyze analyze, X value) throws E {
      return visitOp(analyze, value);
    }

    @Override
    public T visitScan(Scan scan, X value) throws E {
        return visitOp(scan, value);
    }

    @Override
    public T visitStore(Store store, X value) throws E {
        return visitOp(store, value);
    }

    @Override
    public T visitFilter(Filter filter, X value) throws E {
        return visitOp(filter, value);
    }

    @Override
    public T visitFlatten(Flatten flatten, X value) throws E {
        return visitOp(flatten, value);
    }

    @Override
    public T visitProject(Project project, X value) throws E {
        return visitOp(project, value);
    }

    @Override
    public T visitOrder(Order order, X value) throws E {
        return visitOp(order, value);
    }

    @Override
    public T visitJoin(Join join, X value) throws E {
        return visitOp(join, value);
    }

    @Override
    public T visitLimit(Limit limit, X value) throws E {
        return visitOp(limit, value);
    }

    @Override
    public T visitRunningAggregate(RunningAggregate runningAggregate, X value) throws E {
        return visitOp(runningAggregate, value);
    }

    @Override
    public T visitGroupingAggregate(GroupingAggregate groupBy, X value) throws E {
      return visitOp(groupBy, value);
    }

    @Override
    public T visitTransform(Transform transform, X value) throws E {
        return visitOp(transform, value);
    }

    @Override
    public T visitUnion(Union union, X value) throws E {
        return visitOp(union, value);
    }

    @Override
    public T visitWindow(Window window, X value) throws E {
        return visitOp(window, value);
    }

    @Override
    public T visitValues(Values constant, X value) throws E {
       return visitOp(constant, value);
    }

    @Override
    public T visitWriter(Writer writer, X value) throws E {
      return visitOp(writer, value);
    }

    @Override
    public T visitUnnest(Unnest unnest, X value) throws E {
      return visitOp(unnest, value);
    }

    @Override
    public T visitLateralJoin(LateralJoin lateralJoin, X value) throws E {
        return visitOp(lateralJoin, value);
    }
}
