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

import java.util.Set;

import org.apache.calcite.rel.RelNode;
import org.apache.drill.common.logical.LogicalPlan;
import org.apache.drill.common.logical.LogicalPlanBuilder;
import org.apache.drill.common.logical.PlanProperties.Generator.ResultMode;
import org.apache.drill.common.logical.PlanProperties.PlanType;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.visitors.AbstractLogicalVisitor;

import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

/**
 * Context for converting a tree of {@link DrillRel} nodes into a Drill logical plan.
 */
public class DrillImplementor {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillImplementor.class);

  private Set<DrillTable> tables = Sets.newHashSet();
  private Set<String> storageEngineNames = Sets.newHashSet();
  private LogicalPlanBuilder planBuilder = new LogicalPlanBuilder();
  private LogicalPlan plan;
  private final DrillParseContext context;


  public DrillImplementor(DrillParseContext context, ResultMode mode) {
    planBuilder.planProperties(PlanType.APACHE_DRILL_LOGICAL, 1, DrillImplementor.class.getName(), "", mode);
    this.context = context;
  }

  public DrillParseContext getContext(){
    return context;
  }

  public void registerSource(DrillTable table){
    if(tables.add(table) && storageEngineNames.add(table.getStorageEngineName())){
      planBuilder.addStorageEngine(table.getStorageEngineName(), table.getStorageEngineConfig());
    }
  }

  public void go(DrillRel root) {
    LogicalOperator rootLOP = root.implement(this);
    rootLOP.accept(new AddOpsVisitor(), null);
  }

  public LogicalPlan getPlan(){
    if(plan == null){
      plan = planBuilder.build();
      planBuilder = null;
    }
    return plan;
  }

  public LogicalOperator visitChild(DrillRel parent, int ordinal, RelNode child) {
    return ((DrillRel) child).implement(this);
  }

  private class AddOpsVisitor extends AbstractLogicalVisitor<Void, Void, RuntimeException> {
    @Override
    public Void visitOp(LogicalOperator op, Void value) throws RuntimeException {
      planBuilder.addLogicalOperator(op);
      for(LogicalOperator o : op){
        o.accept(this, null);
      }
      return null;
    }
  }

}
