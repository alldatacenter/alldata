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
import org.apache.drill.common.logical.data.Order;
import org.apache.drill.common.logical.data.Project;
import org.apache.drill.common.logical.data.RunningAggregate;
import org.apache.drill.common.logical.data.Scan;
import org.apache.drill.common.logical.data.Store;
import org.apache.drill.common.logical.data.Transform;
import org.apache.drill.common.logical.data.Union;
import org.apache.drill.common.logical.data.Window;
import org.apache.drill.common.logical.data.Writer;

/**
 * Visitor class designed to traversal of a operator tree.  Basis for a number of operator manipulations including fragmentation and materialization.
 * @param <RETURN> The class associated with the return of each visit method.
 * @param <EXTRA> The class object associated with additional data required for a particular operator modification.
 * @param <EXCEP> An optional exception class that can be thrown when a portion of a modification or traversal fails.  Must extend Throwable.  In the case where the visitor does not throw any caught exception, this can be set as RuntimeException.
 */
public interface LogicalVisitor<RETURN, EXTRA, EXCEP extends Throwable> {
    static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(LogicalVisitor.class);


    public RETURN visitScan(Scan scan, EXTRA value) throws EXCEP;
    public RETURN visitStore(Store store, EXTRA value) throws EXCEP;
    public RETURN visitGroupingAggregate(GroupingAggregate groupBy, EXTRA value) throws EXCEP;
    public RETURN visitFilter(Filter filter, EXTRA value) throws EXCEP;
    public RETURN visitFlatten(Flatten flatten, EXTRA value) throws EXCEP;
    public RETURN visitAnalyze(Analyze analyze, EXTRA value) throws EXCEP;

    public RETURN visitProject(Project project, EXTRA value) throws EXCEP;
    public RETURN visitValues(Values constant, EXTRA value) throws EXCEP;
    public RETURN visitOrder(Order order, EXTRA value) throws EXCEP;
    public RETURN visitJoin(Join join, EXTRA value) throws EXCEP;
    public RETURN visitLimit(Limit limit, EXTRA value) throws EXCEP;
    public RETURN visitRunningAggregate(RunningAggregate runningAggregate, EXTRA value) throws EXCEP;
    public RETURN visitTransform(Transform transform, EXTRA value) throws EXCEP;
    public RETURN visitUnion(Union union, EXTRA value) throws EXCEP;
    public RETURN visitWindow(Window window, EXTRA value) throws EXCEP;
    public RETURN visitWriter(Writer writer, EXTRA value) throws EXCEP;

    public RETURN visitUnnest(Unnest unnest, EXTRA value) throws EXCEP;
    public RETURN visitLateralJoin(LateralJoin lateralJoin, EXTRA value) throws EXCEP;
}
