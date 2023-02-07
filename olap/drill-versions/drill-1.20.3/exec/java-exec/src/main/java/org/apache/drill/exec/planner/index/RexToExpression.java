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
package org.apache.drill.exec.planner.index;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexCorrelVariable;
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexFieldAccess;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLocalRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexOver;
import org.apache.calcite.rex.RexRangeRef;
import org.apache.drill.exec.planner.common.DrillProjectRelBase;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.planner.logical.DrillOptiq;
import org.apache.drill.exec.planner.logical.DrillParseContext;

import java.util.Map;
import java.util.Set;

/**
 * This class is an enhanced version for DrillOptiq,
 * 1, it can convert expressions in one more layer(project that have expression) above project-scan,
 * while DrillOptiq can only convert expressions directly reference scan's row type,
 * 2, it can record the generated LogicalExpression of each rexNode in the rexNode tree for future reference
 * this result can serve future rewrite that need to locate particular LogicalExpressions
 */
public class RexToExpression {

  public static LogicalExpression toDrill(DrillParseContext context, DrillProjectRelBase project, RelNode input, RexNode expr) {
    final RexToExpression.RexToDrillExt visitor = new RexToExpression.RexToDrillExt(context, project, input);
    return expr.accept(visitor);
  }

  public static class RexToDrillExt extends DrillOptiq.RexToDrill {

    /**
     * projectToInput, this instance of RexToDrill can do convert for expressions that are directly using inputRef of 'input',
     * e.g. rex nodes in 'project'.
     *
     */
    private final DrillOptiq.RexToDrill projectToInput;

    /**
     * The project in between
     */
    private final DrillProjectRelBase project;

    /**
     * multiple rexNodes could convert to the same logical expressions. map the result expression to the equivalent rexNodes.
     */
    private final Map<LogicalExpression, Set<RexNode>> exprToRexs;


    public RexToDrillExt(DrillParseContext context, DrillProjectRelBase project, RelNode input) {
      super(context, input);
      projectToInput = new DrillOptiq.RexToDrill(context, input);
      this.project = project;
      this.exprToRexs = Maps.newHashMap();
    }

    public Map<LogicalExpression, Set<RexNode>> getMapExprToRex() {
      return exprToRexs;
    }

    private LogicalExpression addExprToRexs(LogicalExpression expr, RexNode rex) {
      if (!exprToRexs.containsKey(expr)) {
        exprToRexs.put(expr, Sets.newHashSet(rex));
      }
      else {
        exprToRexs.get(expr).add(rex);
      }
      return expr;
    }

    protected RelDataType getRowType() {
      if(project != null) {
        return project.getRowType();
      }
      return super.getRowType();
    }

    protected RexBuilder getRexBuilder() {
      if(project != null) {
        return project.getCluster().getRexBuilder();
      }
      return super.getRexBuilder();
    }

    @Override
    public LogicalExpression visitInputRef(RexInputRef inputRef) {
      final int index = inputRef.getIndex();

      //no extra project in between of input and this layer
      if(project == null) {
        final RelDataTypeField field = getRowType().getFieldList().get(index);
        return addExprToRexs(FieldReference.getWithQuotedRef(field.getName()), inputRef);
      }

      //get the actual expression of this projected rex
      RexNode rex = IndexPlanUtils.getProjects(project).get(index);
      return addExprToRexs(rex.accept(projectToInput), inputRef);
    }

    @Override
    public LogicalExpression visitCall(RexCall call) {

      return addExprToRexs(super.visitCall(call), call);
    }

    @Override
    public LogicalExpression visitLocalRef(RexLocalRef localRef) {
      return addExprToRexs(super.visitLocalRef(localRef), localRef);
    }

    @Override
    public LogicalExpression visitOver(RexOver over) {
      return addExprToRexs(super.visitOver(over), over);
    }

    @Override
    public LogicalExpression visitCorrelVariable(RexCorrelVariable correlVariable) {
      return addExprToRexs(super.visitCorrelVariable(correlVariable), correlVariable);
    }

    @Override
    public LogicalExpression visitDynamicParam(RexDynamicParam dynamicParam) {
      return addExprToRexs(super.visitDynamicParam(dynamicParam), dynamicParam);
    }

    @Override
    public LogicalExpression visitRangeRef(RexRangeRef rangeRef) {
      return addExprToRexs(super.visitRangeRef(rangeRef), rangeRef);
    }

    @Override
    public LogicalExpression visitFieldAccess(RexFieldAccess fieldAccess) {
      return addExprToRexs(super.visitFieldAccess(fieldAccess), fieldAccess);
    }
  }

}
