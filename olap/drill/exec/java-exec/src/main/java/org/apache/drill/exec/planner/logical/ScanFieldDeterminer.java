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
package org.apache.drill.exec.planner.logical;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.LogicalPlan;
import org.apache.drill.common.logical.data.Values;
import org.apache.drill.common.logical.data.Filter;
import org.apache.drill.common.logical.data.GroupingAggregate;
import org.apache.drill.common.logical.data.Join;
import org.apache.drill.common.logical.data.JoinCondition;
import org.apache.drill.common.logical.data.Limit;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.common.logical.data.Order;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.common.logical.data.Project;
import org.apache.drill.common.logical.data.Scan;
import org.apache.drill.common.logical.data.SinkOperator;
import org.apache.drill.common.logical.data.Store;
import org.apache.drill.common.logical.data.Union;
import org.apache.drill.common.logical.data.visitors.AbstractLogicalVisitor;

import org.apache.drill.exec.store.parquet.FilterEvaluatorUtils.FieldReferenceFinder;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

/**
 * This visitor will walk a logical plan and record in a map the list of field references associated to each scan. These
 * can then be used to update scan object to appear to be explicitly fielded for optimization purposes.
 */
public class ScanFieldDeterminer extends AbstractLogicalVisitor<Void, ScanFieldDeterminer.FieldList, RuntimeException> {

  private Map<Scan, FieldList> scanFields = Maps.newHashMap();

  public static Map<Scan, FieldList> getFieldLists(LogicalPlan plan){
    Collection<SinkOperator> ops = plan.getGraph().getRoots();
    Preconditions.checkArgument(ops.size() == 1, "Scan Field determiner currently only works with plans that have a single root.");
    ScanFieldDeterminer sfd = new ScanFieldDeterminer();
    ops.iterator().next().accept(sfd, new FieldList());
    return sfd.scanFields;
  }

  private ScanFieldDeterminer(){
  }

  public static class FieldList {
    private Set<SchemaPath> projected = Sets.newHashSet();
    private Set<SchemaPath> referenced = Sets.newHashSet();

    public void addProjected(SchemaPath path) {
      projected.add(path);
    }

    public void addReferenced(SchemaPath path) {
      referenced.add(path);
    }

    public void addReferenced(Collection<SchemaPath> paths) {
      referenced.addAll(paths);
    }

    public void addProjected(Collection<SchemaPath> paths) {
      projected.addAll(paths);
    }

    @Override
    public FieldList clone() {
      FieldList newList = new FieldList();
      for (SchemaPath p : projected) {
        newList.addProjected(p);
      }
      for (SchemaPath p : referenced) {
        newList.addReferenced(p);
      }
      return newList;
    }
  }

  @Override
  public Void visitScan(Scan scan, FieldList value) {
    if (value == null) {
      scanFields.put(scan, new FieldList());
    } else {
      scanFields.put(scan, value);
    }
    return null;
  }

  @Override
  public Void visitStore(Store store, FieldList value) {
    store.getInput().accept(this, value);
    return null;
  }

  @Override
  public Void visitGroupingAggregate(GroupingAggregate groupBy, FieldList value) {
    FieldList list = new FieldList();
    for (NamedExpression e : groupBy.getExprs()) {
      list.addProjected(e.getExpr().accept(FieldReferenceFinder.INSTANCE, null));
    }
    for (NamedExpression e : groupBy.getKeys()) {
      list.addProjected(e.getExpr().accept(FieldReferenceFinder.INSTANCE, null));
    }
    groupBy.getInput().accept(this, list);
    return null;
  }

  @Override
  public Void visitFilter(Filter filter, FieldList value) {
    value.addReferenced(filter.getExpr().accept(FieldReferenceFinder.INSTANCE, null));
    return null;
  }

  @Override
  public Void visitProject(Project project, FieldList value) {
    FieldList fl = new FieldList();
    for (NamedExpression e : project.getSelections()) {
      fl.addProjected(e.getExpr().accept(FieldReferenceFinder.INSTANCE, null));
    }
    return null;
  }

  @Override
  public Void visitValues(Values constant, FieldList value) {
    return null;
  }

  @Override
  public Void visitOrder(Order order, FieldList fl) {
    for (Ordering o : order.getOrderings()) {
      fl.addReferenced(o.getExpr().accept(FieldReferenceFinder.INSTANCE, null));
    }
    return null;
  }

  @Override
  public Void visitJoin(Join join, FieldList fl) {
    {
      FieldList leftList = fl.clone();
      for (JoinCondition c : join.getConditions()) {
        leftList.addReferenced(c.getLeft().accept(FieldReferenceFinder.INSTANCE, null));
      }
      join.getLeft().accept(this, leftList);
    }

    {
      FieldList rightList = fl.clone();
      for (JoinCondition c : join.getConditions()) {
        rightList.addReferenced(c.getRight().accept(FieldReferenceFinder.INSTANCE, null));
      }
      join.getLeft().accept(this, rightList);
    }
    return null;
  }

  @Override
  public Void visitLimit(Limit limit, FieldList value) {
    limit.getInput().accept(this, value);
    return null;
  }

  @Override
  public Void visitUnion(Union union, FieldList value) {
    for (LogicalOperator o : union.getInputs()) {
      o.accept(this, value.clone());
    }
    return null;
  }
}
