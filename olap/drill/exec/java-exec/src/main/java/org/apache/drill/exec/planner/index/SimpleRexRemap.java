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

import org.apache.drill.exec.util.Utilities;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexCorrelVariable;
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexFieldAccess;

import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexLocalRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexOver;
import org.apache.calcite.rex.RexRangeRef;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.rex.RexVisitorImpl;

import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.calcite.util.NlsString;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.PathSegment;

import java.util.List;
import java.util.Map;

/**
 * Rewrite RexNode with these policies:
 * 1) field renamed. The input field was named differently in index table,
 * 2) field is in different position of underlying rowtype
 *
 * TODO: 3) certain operator needs rewriting. e.g. CAST function
 * This class for now applies to only filter on scan, for filter-on-project-on-scan. A stack of
 * rowType is required.
 */
public class SimpleRexRemap {
  final RelNode origRel;
  final RelDataType origRowType;
  final RelDataType newRowType;

  private RexBuilder builder;
  private Map<LogicalExpression, LogicalExpression> destExprMap;

  public SimpleRexRemap(RelNode origRel,
                        RelDataType newRowType, RexBuilder builder) {
    super();
    this.origRel = origRel;
    this.origRowType = origRel.getRowType();
    this.newRowType = newRowType;
    this.builder = builder;
    this.destExprMap = Maps.newHashMap();
  }

  /**
   * Set the map of src expression to target expression, expressions not in the map do not have assigned destinations
   * @param exprMap
   * @return
   */
  public SimpleRexRemap setExpressionMap(Map<LogicalExpression, LogicalExpression>  exprMap) {
    destExprMap.putAll(exprMap);
    return this;
  }

  public RexNode rewriteEqualOnCharToLike(RexNode expr,
                                          Map<RexNode, LogicalExpression> equalOnCastCharExprs) {
    Map<RexNode, RexNode> srcToReplace = Maps.newIdentityHashMap();
    for (Map.Entry<RexNode, LogicalExpression> entry: equalOnCastCharExprs.entrySet()) {
      RexNode equalOp = entry.getKey();
      LogicalExpression opInput = entry.getValue();

      final List<RexNode> operands = ((RexCall)equalOp).getOperands();
      RexLiteral newLiteral = null;
      RexNode input = null;
      if (operands.size() == 2 ) {
        RexLiteral oplit = null;
        if (operands.get(0) instanceof RexLiteral) {
          oplit = (RexLiteral) operands.get(0);
          if (oplit.getTypeName() == SqlTypeName.CHAR) {
            newLiteral = builder.makeLiteral(((NlsString) oplit.getValue()).getValue() + "%");
            input = operands.get(1);
          }
        }
        else if (operands.get(1) instanceof RexLiteral) {
          oplit = (RexLiteral) operands.get(1);
          if (oplit.getTypeName() == SqlTypeName.CHAR) {
            newLiteral = builder.makeLiteral(((NlsString) oplit.getValue()).getValue() + "%");
            input = operands.get(0);
          }
        }
      }
      if (newLiteral != null) {
        srcToReplace.put(equalOp, builder.makeCall(SqlStdOperatorTable.LIKE, input, newLiteral));
      }
    }
    if (srcToReplace.size() > 0) {
      RexReplace replacer = new RexReplace(srcToReplace);
      RexNode resultRex = expr.accept(replacer);
      return resultRex;
    }
    return expr;
  }

  /**
   *
   * @param srcRex  the source RexNode to be rewritten
   * @param mapRexToExpr a map of rex->logical expression to guide what rex to rewrite
   * @return the RexNode after rewriting
   */
  public RexNode rewriteWithMap(RexNode srcRex, Map<RexNode, LogicalExpression> mapRexToExpr) {
    Map<RexNode, RexNode> destNodeMap = Maps.newHashMap();
    for (Map.Entry<RexNode, LogicalExpression> entry: mapRexToExpr.entrySet()) {
      LogicalExpression entryExpr = entry.getValue();

      LogicalExpression destExpr = destExprMap.get(entryExpr);
      // then build rexNode from the path
      RexNode destRex = buildRexForField(destExpr==null?entryExpr : destExpr, newRowType);
      destNodeMap.put(entry.getKey(), destRex);
    }

    // Visit through the nodes, if destExprMap has an entry to provide substitute to replace a rexNode, replace the rexNode
    RexReplace replacer = new RexReplace(destNodeMap);
    RexNode resultRex = srcRex.accept(replacer);
    return resultRex;
  }

  public RexNode rewrite(RexNode expr) {
    IndexableExprMarker marker = new IndexableExprMarker(origRel);
    expr.accept(marker);
    return rewriteWithMap(expr, marker.getIndexableExpression());
  }

  private RexNode buildRexForField(LogicalExpression expr, RelDataType newRowType) {
    ExprToRex toRex = new ExprToRex(origRel, newRowType, builder);
    return expr.accept(toRex, null);
  }

  public static String getFullPath(PathSegment pathSeg) {
    PathSegment.NameSegment nameSeg = (PathSegment.NameSegment)pathSeg;
    if (nameSeg.isLastPath()) {
      return nameSeg.getPath();
    }
    return String.format("%s.%s",
        nameSeg.getPath(),
        getFullPath(nameSeg.getChild()));
  }

  /**
   * This class go through the RexNode, collect all the fieldNames, mark starting positions(RexNode) of fields
   * so this information can be used later e,.g. replaced with a substitute node
   */
  public static class FieldsMarker extends RexVisitorImpl<PathSegment> {
    final List<String> fieldNames;
    final List<RelDataTypeField> fields;
    final Map<RexNode, String> desiredFields = Maps.newHashMap();

    int stackDepth;

    public FieldsMarker(RelDataType rowType) {
      super(true);
      this.fieldNames = rowType.getFieldNames();
      this.fields = rowType.getFieldList();
      this.stackDepth = 0;
    }

    private PathSegment newPath(PathSegment segment, RexNode node) {
      if (stackDepth == 0) {
        desiredFields.put(node, getFullPath(segment));
      }
      return segment;
    }

    private PathSegment newPath(String path, RexNode node) {
      PathSegment segment = new PathSegment.NameSegment(path);
      if (stackDepth == 0) {
        desiredFields.put(node, getFullPath(segment));
      }
      return segment;
    }

    public Map<RexNode, String> getFieldAndPos() {
      return ImmutableMap.copyOf(desiredFields);
    }

    @Override
    public PathSegment visitInputRef(RexInputRef inputRef) {
      int index = inputRef.getIndex();
      String name = fieldNames.get(index);
      return newPath(name, inputRef);
    }

    @Override
    public PathSegment visitCall(RexCall call) {
      if ("ITEM".equals(call.getOperator().getName())) {
        stackDepth++;
        PathSegment mapOrArray = call.operands.get(0).accept(this);
        stackDepth--;
        if (mapOrArray != null) {
          if (call.operands.get(1) instanceof RexLiteral) {
            PathSegment newFieldPath = newPath(
                mapOrArray.cloneWithNewChild(Utilities.convertLiteral((RexLiteral) call.operands.get(1))),
                call);
            return newFieldPath;
          }
          return mapOrArray;
        }
      } else {
        for (RexNode operand : call.operands) {
          operand.accept(this);
        }
      }
      return null;
    }
  }

  public static class RexReplace extends RexShuttle {

    final Map<RexNode, RexNode> rexMap;

    public RexReplace( Map<RexNode, RexNode> rexMap) {
      this.rexMap = rexMap;
    }
    boolean toReplace(RexNode node) {
      return rexMap.containsKey(node);
    }

    RexNode replace(RexNode node) {
      return rexMap.get(node);
    }

    public RexNode visitOver(RexOver over) {
      return toReplace(over) ? replace(over) : super.visitOver(over);
    }

    public RexNode visitCall(final RexCall call) {
      return toReplace(call) ? replace(call) : super.visitCall(call);
    }

    public RexNode visitCorrelVariable(RexCorrelVariable variable) {
      return variable;
    }

    public RexNode visitFieldAccess(RexFieldAccess fieldAccess) {
      return toReplace(fieldAccess) ? replace(fieldAccess) : super.visitFieldAccess(fieldAccess);
    }

    public RexNode visitInputRef(RexInputRef inputRef) {
      return toReplace(inputRef) ? replace(inputRef) : super.visitInputRef(inputRef);
    }

    public RexNode visitLocalRef(RexLocalRef localRef) {
      return toReplace(localRef) ? replace(localRef) : super.visitLocalRef(localRef);
    }

    public RexNode visitLiteral(RexLiteral literal) {
      return literal;
    }

    public RexNode visitDynamicParam(RexDynamicParam dynamicParam) {
      return dynamicParam;
    }

    public RexNode visitRangeRef(RexRangeRef rangeRef) {
      return toReplace(rangeRef) ? replace(rangeRef) : super.visitRangeRef(rangeRef);
    }
  }

}
