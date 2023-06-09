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
import org.apache.calcite.rel.type.RelDataTypeFieldImpl;
import org.apache.calcite.rel.type.RelRecordType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.sql.validate.SqlValidatorUtil;
import org.apache.calcite.tools.RelConversionException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.drill.exec.planner.physical.Prel;
import org.apache.drill.exec.planner.physical.PrelUtil;
import org.apache.drill.exec.planner.physical.ProjectPrel;
import org.apache.drill.exec.planner.types.RelDataTypeDrillImpl;
import org.apache.drill.exec.planner.types.RelDataTypeHolder;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

public class SplitUpComplexExpressions extends BasePrelVisitor<Prel, Object, RelConversionException> {

  private final RelDataTypeFactory factory;
  private final RexBuilder rexBuilder;
  private final FunctionImplementationRegistry funcReg;

  public SplitUpComplexExpressions(RelDataTypeFactory factory, FunctionImplementationRegistry funcReg, RexBuilder rexBuilder) {
    this.factory = factory;
    this.funcReg = funcReg;
    this.rexBuilder = rexBuilder;
  }

  @Override
  public Prel visitPrel(Prel prel, Object unused) throws RelConversionException {
    List<RelNode> children = new ArrayList<>();
    for (Prel child : prel) {
      child = child.accept(this, unused);
      children.add(child);
    }
    if (children.equals(prel.getInputs())) {
      return prel;
    }
    return (Prel) prel.copy(prel.getTraitSet(), children);
  }


  @Override
  public Prel visitProject(final ProjectPrel project, Object unused) throws RelConversionException {
    final Prel oldInput = (Prel) project.getInput(0);
    RelNode newInput = oldInput.accept(this, unused);

    ProjectPrel newProject = (ProjectPrel) project.copy(project.getTraitSet(), Lists.newArrayList(newInput));

    final int lastColumnReferenced = PrelUtil.getLastUsedColumnReference(newProject.getProjects());
    if (lastColumnReferenced == -1) {
      return newProject;
    }

    List<RelDataTypeField> projectFields = newProject.getRowType().getFieldList();
    List<RelDataTypeField> origRelDataTypes = new ArrayList<>();
    List<RexNode> exprList = new ArrayList<>();
    final int lastRexInput = lastColumnReferenced + 1;
    RexVisitorComplexExprSplitter exprSplitter = new RexVisitorComplexExprSplitter(funcReg, rexBuilder, lastRexInput);
    int i = 0;
    for (RexNode rex : newProject.getChildExps()) {
      RelDataTypeField originField = projectFields.get(i++);
      RexNode splitRex = rex.accept(exprSplitter);
      origRelDataTypes.add(originField);
      exprList.add(splitRex);
    }

    final List<RexNode> complexExprs = exprSplitter.getComplexExprs();
    if (complexExprs.size() == 1 && findTopComplexFunc(newProject.getChildExps()).size() == 1) {
      return newProject;
    }

    // if the projection expressions contained complex outputs, split them into their own individual projects
    if (complexExprs.size() > 0 ) {
      List<RexNode> allExprs = new ArrayList<>();
      int exprIndex = 0;
      List<String> fieldNames = newInput.getRowType().getFieldNames();

      List<RelDataTypeField> relDataTypes = new ArrayList<>();
      for (int index = 0; index < lastRexInput; index++) {
        allExprs.add(rexBuilder.makeInputRef( new RelDataTypeDrillImpl(new RelDataTypeHolder(), factory), index));

        if (fieldNames.get(index).contains(SchemaPath.DYNAMIC_STAR)) {
          relDataTypes.add(new RelDataTypeFieldImpl(fieldNames.get(index), allExprs.size(), factory.createSqlType(SqlTypeName.ANY)));
        } else {
          relDataTypes.add(new RelDataTypeFieldImpl(getExprName(exprIndex), allExprs.size(), factory.createSqlType(SqlTypeName.ANY)));
          exprIndex++;
        }
      }
      RexNode currRexNode;
      int index = lastRexInput - 1;
      while (complexExprs.size() > 0) {
        if ( index >= lastRexInput ) {
          RexInputRef newLastRex = rexBuilder.makeInputRef(new RelDataTypeDrillImpl(new RelDataTypeHolder(), factory), index);
          // replace last rex with new one
          allExprs.set(allExprs.size() - 1, newLastRex);
        }
        index++;
        exprIndex++;

        currRexNode = complexExprs.remove(0);
        allExprs.add(currRexNode);
        relDataTypes.add(new RelDataTypeFieldImpl(getExprName(exprIndex), allExprs.size(), factory.createSqlType(SqlTypeName.ANY)));

        RelRecordType childProjectType = new RelRecordType(relDataTypes);
        ProjectPrel childProject  = new ProjectPrel(newProject.getCluster(), newProject.getTraitSet(), newInput, ImmutableList.copyOf(allExprs), childProjectType);
        newInput = childProject;
      }

      allExprs.set(allExprs.size() - 1,
          rexBuilder.makeInputRef(new RelDataTypeDrillImpl(new RelDataTypeHolder(), factory), index));

      relDataTypes.add(new RelDataTypeFieldImpl(getExprName(exprIndex), allExprs.size(), factory.createSqlType(SqlTypeName.ANY)));
    }

    return (Prel) project.copy(project.getTraitSet(), newInput, exprList, new RelRecordType(origRelDataTypes));
  }

  private String getExprName(int exprIndex) {
    return SqlValidatorUtil.EXPR_SUGGESTER.apply(null, exprIndex, 0);
  }

  /**
   *  Find the list of expressions where Complex type function is at top level.
   */
  private List<RexNode> findTopComplexFunc(List<RexNode> exprs) {
    final List<RexNode> topComplexFuncs = new ArrayList<>();

    for (RexNode exp : exprs) {
      if (exp instanceof RexCall) {
        RexCall call = (RexCall) exp;
        String functionName = call.getOperator().getName();

        if (funcReg.isFunctionComplexOutput(functionName) ) {
          topComplexFuncs.add(exp);
        }
      }
    }

    return topComplexFuncs;
  }

}
