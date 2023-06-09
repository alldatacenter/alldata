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

package org.apache.drill.exec.physical.impl.project;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.IfExpression;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.TypedNullConstant;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.expr.AbstractExecExprVisitor;
import org.apache.drill.exec.expr.DrillFuncHolderExpr;
import org.apache.drill.exec.expr.ValueVectorReadExpression;
import org.apache.drill.exec.expr.ValueVectorWriteExpression;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;
import org.apache.drill.exec.expr.fn.output.OutputWidthCalculator;
import org.apache.drill.exec.physical.impl.project.OutputWidthExpression.FixedLenExpr;
import org.apache.drill.exec.physical.impl.project.OutputWidthExpression.FunctionCallExpr;
import org.apache.drill.exec.physical.impl.project.OutputWidthExpression.VarLenReadExpr;
import org.apache.drill.exec.physical.impl.project.OutputWidthExpression.IfElseWidthExpr;
import org.apache.drill.common.expression.ValueExpressions.VarDecimalExpression;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.record.TypedFieldId;

import java.util.ArrayList;

public class OutputWidthVisitor extends
      AbstractExecExprVisitor<OutputWidthExpression,
      OutputWidthVisitorState, RuntimeException> {

  @Override
  public OutputWidthExpression visitVarDecimalConstant(VarDecimalExpression varDecimalExpression,
                                                       OutputWidthVisitorState state) throws RuntimeException {
    Preconditions.checkArgument(varDecimalExpression.getMajorType().hasPrecision());
    return new FixedLenExpr(varDecimalExpression.getMajorType().getPrecision());
  }

  /**
   * Records the {@link IfExpression} as a {@link IfElseWidthExpr}.
   * IfElseWidthExpr will be reduced to a {@link FixedLenExpr} by taking the max
   * of the if-expr-width and the else-expr-width.
   */
  @Override
  public OutputWidthExpression visitIfExpression(IfExpression ifExpression, OutputWidthVisitorState state)
                                                                  throws RuntimeException {
    IfExpression.IfCondition condition = ifExpression.ifCondition;
    LogicalExpression ifExpr = condition.expression;
    LogicalExpression elseExpr = ifExpression.elseExpression;

    OutputWidthExpression ifWidthExpr = ifExpr.accept(this, state);
    OutputWidthExpression elseWidthExpr = null;
    if (elseExpr != null) {
      elseWidthExpr = elseExpr.accept(this, state);
    }
    return new IfElseWidthExpr(ifWidthExpr, elseWidthExpr);
  }

  /**
   * Handles a {@link FunctionHolderExpression}. Functions that produce
   * fixed-width output are trivially converted to a {@link FixedLenExpr}. For
   * functions that produce variable width output, the output width calculator
   * annotation is looked-up and recorded in a {@link FunctionCallExpr}. This
   * calculator will later be used to convert the FunctionCallExpr to a
   * {@link FixedLenExpr} expression
   */
  @Override
  public OutputWidthExpression visitFunctionHolderExpression(FunctionHolderExpression holderExpr,
                                                             OutputWidthVisitorState state) throws RuntimeException {
    OutputWidthExpression fixedWidth = getFixedLenExpr(holderExpr.getMajorType());
    if (fixedWidth != null) { return fixedWidth; }
    // Only Drill functions can be handled. Non-drill Functions, like HiveFunctions
    // will default to a fixed value
    if (!(holderExpr instanceof DrillFuncHolderExpr)) {
      // We currently only know how to handle DrillFuncs.
      // Use a default if this is not a DrillFunc
      return new FixedLenExpr(OutputSizeEstimateConstants.NON_DRILL_FUNCTION_OUTPUT_SIZE_ESTIMATE);
    }

    final DrillFuncHolder holder = ((DrillFuncHolderExpr) holderExpr).getHolder();

    // If the user has provided a size estimate, use it
    int estimate = holder.variableOutputSizeEstimate();
    if (estimate != FunctionTemplate.OUTPUT_SIZE_ESTIMATE_DEFAULT) {
      return new FixedLenExpr(estimate);
    }
    // Use the calculator provided by the user or use the default
    OutputWidthCalculator widthCalculator = holder.getOutputWidthCalculator();
    final int argSize = holderExpr.args.size();
    ArrayList<OutputWidthExpression> arguments = null;
    if (argSize != 0) {
      arguments = new ArrayList<>(argSize);
      for (LogicalExpression expr : holderExpr.args) {
        arguments.add(expr.accept(this, state));
      }
    }
    return new FunctionCallExpr(holderExpr, widthCalculator, arguments);
  }

  /**
   * Records a variable width write expression. This will be converted to a
   * {@link FixedLenExpr} expression by walking the tree of expression attached
   * to the write expression.
   */
  @Override
  public OutputWidthExpression visitValueVectorWriteExpression(ValueVectorWriteExpression writeExpr,
                                                               OutputWidthVisitorState state) throws RuntimeException {
    TypedFieldId fieldId = writeExpr.getFieldId();
    ProjectMemoryManager manager = state.getManager();
    OutputWidthExpression outputExpr;
    if (manager.isFixedWidth(fieldId)) {
      outputExpr = getFixedLenExpr(fieldId.getFinalType());
    } else {
      LogicalExpression writeArg = writeExpr.getChild();
      outputExpr = writeArg.accept(this, state);
    }
    return outputExpr;
  }

  /**
   * Records a variable width read expression as a {@link VarLenReadExpr}. This
   * will be converted to a {@link FixedLenExpr} expression by getting the size
   * for the corresponding column from the {@link RecordBatchSizer}.
   */
  @Override
  public OutputWidthExpression visitValueVectorReadExpression(ValueVectorReadExpression readExpr,
                                                              OutputWidthVisitorState state) throws RuntimeException {
    return new VarLenReadExpr(readExpr);
  }

  @Override
  public OutputWidthExpression visitQuotedStringConstant(ValueExpressions.QuotedString quotedString,
                                                         OutputWidthVisitorState state) throws RuntimeException {
    return new FixedLenExpr(quotedString.getString().length());
  }

  @Override
  public OutputWidthExpression visitUnknown(LogicalExpression logicalExpression, OutputWidthVisitorState state) {
    OutputWidthExpression fixedLenExpr = getFixedLenExpr(logicalExpression.getMajorType());
    if (fixedLenExpr != null) {
      return fixedLenExpr;
    }
    throw new IllegalStateException("Unknown variable width expression: " + logicalExpression);
  }

  @Override
  public OutputWidthExpression visitNullConstant(TypedNullConstant nullConstant, OutputWidthVisitorState state)
          throws RuntimeException {
    int width;
    if (nullConstant.getMajorType().hasPrecision()) {
      width = nullConstant.getMajorType().getPrecision();
    } else {
      width = 0;
    }
    return new FixedLenExpr(width);
  }


  @Override
  public OutputWidthExpression visitFixedLenExpr(FixedLenExpr fixedLenExpr, OutputWidthVisitorState state)
          throws RuntimeException {
    return fixedLenExpr;
  }

  /**
   * Converts the {@link VarLenReadExpr} to a {@link FixedLenExpr} by getting
   * the size for the corresponding column from the RecordBatchSizer.
   */
  @Override
  public OutputWidthExpression visitVarLenReadExpr(VarLenReadExpr varLenReadExpr, OutputWidthVisitorState state)
                                                      throws RuntimeException {
    String columnName = varLenReadExpr.getInputColumnName();
    if (columnName == null) {
      TypedFieldId fieldId = varLenReadExpr.getReadExpression().getTypedFieldId();
      columnName =  TypedFieldId.getPath(fieldId, state.manager.incomingBatch());
    }
    final RecordBatchSizer.ColumnSize columnSize = state.manager.getColumnSize(columnName);

    int columnWidth = columnSize.getDataSizePerEntry();
    return new FixedLenExpr(columnWidth);
  }

  /**
   * Converts a {@link FunctionCallExpr} to a {@link FixedLenExpr} by passing
   * the the args of the function to the width calculator for this function.
   */
  @Override
  public OutputWidthExpression visitFunctionCallExpr(FunctionCallExpr functionCallExpr, OutputWidthVisitorState state)
                                                      throws RuntimeException {
    ArrayList<OutputWidthExpression> args = functionCallExpr.getArgs();
    ArrayList<FixedLenExpr> estimatedArgs = null;

    if (args != null && args.size() != 0) {
      estimatedArgs = new ArrayList<>(args.size());
      for (OutputWidthExpression expr : args) {
        // Once the args are visited, they will all become FixedWidthExpr
        FixedLenExpr fixedLenExpr = (FixedLenExpr) expr.accept(this, state);
        estimatedArgs.add(fixedLenExpr);
      }
    }
    OutputWidthCalculator estimator = functionCallExpr.getCalculator();
    int estimatedSize = estimator.getOutputWidth(estimatedArgs);
    return new FixedLenExpr(estimatedSize);
  }

  /**
   * Converts the {@link IfElseWidthExpr} to a {@link FixedLenExpr} by taking
   * the max of the if-expr-width and the else-expr-width.
   */
  @Override
  public OutputWidthExpression visitIfElseWidthExpr(IfElseWidthExpr ifElseWidthExpr, OutputWidthVisitorState state)
                                                      throws RuntimeException {
    OutputWidthExpression ifReducedExpr = ifElseWidthExpr.expressions[0].accept(this, state);
    assert ifReducedExpr instanceof FixedLenExpr;
    int ifWidth = ((FixedLenExpr)ifReducedExpr).getDataWidth();
    int elseWidth = -1;
    if (ifElseWidthExpr.expressions[1] != null) {
      OutputWidthExpression elseReducedExpr = ifElseWidthExpr.expressions[1].accept(this, state);
      assert elseReducedExpr instanceof FixedLenExpr;
      elseWidth = ((FixedLenExpr)elseReducedExpr).getDataWidth();
    }
    int outputWidth = Math.max(ifWidth, elseWidth);
    return new FixedLenExpr(outputWidth);
  }

  private OutputWidthExpression getFixedLenExpr(MajorType majorType) {
    MajorType type = majorType;
    if (Types.isFixedWidthType(type)) {
      // Use only the width of the data. Metadata width will be accounted for at the end
      // This is to avoid using metadata size in intermediate calculations
      int fixedDataWidth = ProjectMemoryManager.getFixedWidth(type);
      return new OutputWidthExpression.FixedLenExpr(fixedDataWidth);
    }
    return null;
  }
}
