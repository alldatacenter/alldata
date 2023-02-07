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
package org.apache.drill.exec.planner.physical.visitor;

import java.util.ArrayList;
import java.util.List;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rel.type.RelRecordType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.tools.RelConversionException;
import org.apache.drill.exec.planner.physical.FlattenPrel;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.sql.DrillOperatorTable;
import org.apache.drill.exec.planner.types.RelDataTypeDrillImpl;
import org.apache.drill.exec.planner.types.RelDataTypeHolder;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class RewriteProjectToFlatten extends BasePrelVisitor<Prel, Object, RelConversionException> {

  RelDataTypeFactory factory;
  DrillOperatorTable table;

  public RewriteProjectToFlatten(RelDataTypeFactory factory, DrillOperatorTable table) {
    super();
    this.factory = factory;
    this.table = table;
  }

  @Override
  public Prel visitPrel(Prel prel, Object value) throws RelConversionException {
    List<RelNode> children = Lists.newArrayList();
    for(Prel child : prel){
      child = child.accept(this, null);
      children.add(child);
    }
    if (children.equals(prel.getInputs())) {
      return prel;
    }
    return (Prel) prel.copy(prel.getTraitSet(), children);
  }


  @Override
  public Prel visitProject(ProjectPrel project, Object unused) throws RelConversionException {
    List<RexNode> exprList = new ArrayList<>();
    boolean rewrite = false;

    List<RelDataTypeField> relDataTypes = new ArrayList<>();
    int i = 0;
    RexNode flatttenExpr = null;
    for (RexNode rex : project.getChildExps()) {
      RexNode newExpr = rex;
      if (rex instanceof RexCall) {
        RexCall function = (RexCall) rex;
        String functionName = function.getOperator().getName();

        if (functionName.equalsIgnoreCase("flatten") ) {
          rewrite = true;
          if (function.getOperands().size() != 1) {
            throw new RelConversionException("Flatten expression expects a single input.");
          }
          newExpr = function.getOperands().get(0);
          RexBuilder builder = new RexBuilder(factory);
          flatttenExpr = builder.makeInputRef( new RelDataTypeDrillImpl(new RelDataTypeHolder(), factory), i);
        }
      }
      relDataTypes.add(project.getRowType().getFieldList().get(i));
      i++;
      exprList.add(newExpr);
    }
    if (rewrite) {
      // TODO - figure out what is the right setting for the traits
      Prel newChild = ((Prel)project.getInput(0)).accept(this, null);
      ProjectPrel newProject = new ProjectPrel(project.getCluster(), project.getTraitSet(), newChild, exprList, new RelRecordType(relDataTypes));
      FlattenPrel flatten = new FlattenPrel(project.getCluster(), project.getTraitSet(), newProject, flatttenExpr);
      return flatten;
    }

    Prel child = ((Prel)project.getInput()).accept(this, null);
    if (child == project.getInput() && exprList.equals(project.getChildExps())) {
      return project;
    }
    return (Prel) project.copy(project.getTraitSet(), child, exprList, new RelRecordType(relDataTypes));
  }

}
