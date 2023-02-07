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
package org.apache.drill.exec.planner.torel;

import java.util.List;
import java.util.Map;

import org.apache.calcite.prepare.Prepare;

import org.apache.calcite.rel.RelRoot;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.logical.LogicalPlan;
import org.apache.drill.common.logical.data.Filter;
import org.apache.drill.common.logical.data.GroupingAggregate;
import org.apache.drill.common.logical.data.Join;
import org.apache.drill.common.logical.data.Limit;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.Order;
import org.apache.drill.common.logical.data.Project;
import org.apache.drill.common.logical.data.Scan;
import org.apache.drill.common.logical.data.Union;
import org.apache.drill.common.logical.data.visitors.AbstractLogicalVisitor;
import org.apache.drill.exec.planner.logical.DrillAggregateRel;
import org.apache.drill.exec.planner.logical.DrillJoinRel;
import org.apache.drill.exec.planner.logical.DrillLimitRel;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.logical.DrillSortRel;
import org.apache.drill.exec.planner.logical.DrillUnionRel;
import org.apache.drill.exec.planner.logical.ScanFieldDeterminer;
import org.apache.drill.exec.planner.logical.ScanFieldDeterminer.FieldList;
import org.apache.calcite.rel.InvalidRelException;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptCluster;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelOptTable.ToRelContext;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.schema.SchemaPlus;

public class ConversionContext implements ToRelContext {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConversionContext.class);

  private static final ConverterVisitor VISITOR = new ConverterVisitor();

  private final Map<Scan, FieldList> scanFieldLists;
  private final RelOptCluster cluster;
  private final Prepare prepare;

  public ConversionContext(RelOptCluster cluster, LogicalPlan plan) {
    super();
    scanFieldLists = ScanFieldDeterminer.getFieldLists(plan);
    this.cluster = cluster;
    this.prepare = null;
  }

  @Override
  public RelOptCluster getCluster() {
    return cluster;
  }


  private FieldList getFieldList(Scan scan) {
    assert scanFieldLists.containsKey(scan);
    return scanFieldLists.get(scan);
  }


  public RexBuilder getRexBuilder(){
    return cluster.getRexBuilder();
  }

  public RelTraitSet getLogicalTraits(){
    RelTraitSet set = RelTraitSet.createEmpty();
    set.add(DrillRel.DRILL_LOGICAL);
    return set;
  }

  public RelNode toRel(LogicalOperator operator) throws InvalidRelException{
    return operator.accept(VISITOR, this);
  }

  public RexNode toRex(LogicalExpression e){
    return null;
  }

  public RelDataTypeFactory getTypeFactory(){
    return cluster.getTypeFactory();
  }

  public RelOptTable getTable(Scan scan){
    FieldList list = getFieldList(scan);

    return null;
  }

  @Override
  public RelRoot expandView(RelDataType rowType, String queryString, List<String> schemaPath, List<String> viewPath) {
    throw new UnsupportedOperationException();
  }

  @Override
  public RelRoot expandView(RelDataType rowType, String queryString, SchemaPlus rootSchema, List<String> schemaPath) {
    throw new UnsupportedOperationException();
  }

  private static class ConverterVisitor extends AbstractLogicalVisitor<RelNode, ConversionContext, InvalidRelException>{

    @Override
    public RelNode visitScan(Scan scan, ConversionContext context){
      //return BaseScanRel.convert(scan, context);
      return null;
    }

    @Override
    public RelNode visitFilter(Filter filter, ConversionContext context) throws InvalidRelException{
      //return BaseFilterRel.convert(filter, context);
      return null;
    }

    @Override
    public RelNode visitProject(Project project, ConversionContext context) throws InvalidRelException{
      //return BaseProjectRel.convert(project, context);
      return null;
    }

    @Override
    public RelNode visitOrder(Order order, ConversionContext context) throws InvalidRelException{
      return DrillSortRel.convert(order, context);
    }

    @Override
    public RelNode visitJoin(Join join, ConversionContext context) throws InvalidRelException{
      return DrillJoinRel.convert(join, context);
    }

    @Override
    public RelNode visitLimit(Limit limit, ConversionContext context) throws InvalidRelException{
      return DrillLimitRel.convert(limit, context);
    }

    @Override
    public RelNode visitUnion(Union union, ConversionContext context) throws InvalidRelException{
      return DrillUnionRel.convert(union, context);
    }

    @Override
    public RelNode visitGroupingAggregate(GroupingAggregate groupBy, ConversionContext context)
        throws InvalidRelException {
      return DrillAggregateRel.convert(groupBy, context);
    }

  }



}
