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
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.planner.logical.partition.FindPartitionConditions;

import java.util.BitSet;
import java.util.List;
import java.util.Map;

public class RexSeparator {

  final private List<LogicalExpression> relatedPaths;
  final private RelNode inputRel;
  final private RexBuilder builder;

  public RexSeparator(List<LogicalExpression> relatedPaths, RelNode inputRel, RexBuilder builder) {
    this.relatedPaths = relatedPaths;
    this.inputRel = inputRel;
    this.builder = builder;
  }

  public RexNode getSeparatedCondition(RexNode expr) {
    IndexableExprMarker marker = new IndexableExprMarker(inputRel);
    expr.accept(marker);

    final Map<RexNode, LogicalExpression> markMap = Maps.newHashMap();
    final Map<RexNode, LogicalExpression> relevantRexMap = marker.getIndexableExpression();
    for(Map.Entry<RexNode, LogicalExpression> entry : relevantRexMap.entrySet()) {
      //for the expressions found in expr, only these in relatedPaths is related
      LogicalExpression relevantExpr = entry.getValue();
      int idxFound = relatedPaths.indexOf(relevantExpr);
      if ( idxFound >= 0 ) {
        if (relevantExpr instanceof SchemaPath) {
          //case sensitive comparison
          if (!((SchemaPath) relevantExpr).toExpr().equals(
              ((SchemaPath) relatedPaths.get(idxFound)).toExpr())) {
            continue;
          }
        }
        else if (relevantExpr instanceof CastExpression) {
          final CastExpression castExprInFilter = (CastExpression) relevantExpr;
          if (castExprInFilter.getMajorType().getMinorType() == TypeProtos.MinorType.VARCHAR
              && (castExprInFilter.getMajorType().getPrecision() > relatedPaths.get(idxFound).getMajorType().getPrecision())) {
            continue;
          }
        }
        markMap.put(entry.getKey(), entry.getValue());
      }
    }

    ConditionSeparator separator = new ConditionSeparator(markMap, builder);
    separator.analyze(expr);
    return separator.getFinalCondition();
  }

  private static class ConditionSeparator extends  FindPartitionConditions {

    final private Map<RexNode, LogicalExpression> markMap;
    private boolean inAcceptedPath;

    public ConditionSeparator(Map<RexNode, LogicalExpression> markMap, RexBuilder builder) {
      super(new BitSet(), builder);
      this.markMap = markMap;
      inAcceptedPath = false;
    }

    @Override
    protected boolean inputRefToPush(RexInputRef inputRef) {
      //this class will based on the schemaPath to decide what to push
      if (markMap.containsKey(inputRef) || inAcceptedPath) {
        return true;
      }
      return false;
    }

    @Override
    public Void visitCall(RexCall call) {
      boolean oldValue = inAcceptedPath;
      try {
        if (markMap.containsKey(call)) {
          inAcceptedPath = true;

        }
        return super.visitCall(call);
      } finally {
        inAcceptedPath = oldValue;
      }
    }
  }
}
