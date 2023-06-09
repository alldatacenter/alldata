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

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.logical.LogicalFilter;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.rex.RexUtil;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.exception.UnsupportedOperatorCollector;
import org.apache.drill.exec.planner.sql.DrillOperatorTable;
import org.apache.drill.exec.planner.sql.parser.DrillCalciteWrapperUtility;
import org.apache.drill.exec.util.ApproximateStringMatcher;
import org.apache.drill.exec.work.foreman.SqlUnsupportedException;
import org.apache.calcite.rel.logical.LogicalProject;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.rel.logical.LogicalUnion;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.util.NlsString;

/**
 * This class rewrites all the project expression that contain convert_to/ convert_from
 * to actual implementations.
 * Eg: convert_from(EXPR, 'JSON') is rewritten as convert_fromjson(EXPR)
 *
 * With the actual method name we can find out if the function has a complex
 * output type and we will fire/ ignore certain rules (merge project rule) based on this fact.
 */
public class PreProcessLogicalRel extends RelShuttleImpl {

  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PreProcessLogicalRel.class);

  private RelDataTypeFactory factory;
  private DrillOperatorTable table;
  private UnsupportedOperatorCollector unsupportedOperatorCollector;
  private final UnwrappingExpressionVisitor unwrappingExpressionVisitor;

  public static PreProcessLogicalRel createVisitor(RelDataTypeFactory factory, DrillOperatorTable table, RexBuilder rexBuilder) {
    return new PreProcessLogicalRel(factory, table, rexBuilder);
  }

  private PreProcessLogicalRel(RelDataTypeFactory factory, DrillOperatorTable table, RexBuilder rexBuilder) {
    super();
    this.factory = factory;
    this.table = table;
    this.unsupportedOperatorCollector = new UnsupportedOperatorCollector();
    this.unwrappingExpressionVisitor = new UnwrappingExpressionVisitor(rexBuilder);
  }

  @Override
  public RelNode visit(LogicalProject project) {
    final List<RexNode> projExpr = Lists.newArrayList();
    for(RexNode rexNode : project.getChildExps()) {
      projExpr.add(rexNode.accept(unwrappingExpressionVisitor));
    }

    project =  project.copy(project.getTraitSet(),
        project.getInput(),
        projExpr,
        project.getRowType());

    List<RexNode> exprList = new ArrayList<>();
    boolean rewrite = false;

    for (RexNode rex : project.getChildExps()) {
      RexNode newExpr = rex;
      if (rex instanceof RexCall) {
        RexCall function = (RexCall) rex;
        String functionName = function.getOperator().getName();
        int nArgs = function.getOperands().size();

        // check if its a convert_from or convert_to function
        if (functionName.equalsIgnoreCase("convert_from") || functionName.equalsIgnoreCase("convert_to")) {
          String literal;
          if (nArgs == 2) {
            if (function.getOperands().get(1) instanceof RexLiteral) {
              try {
                literal = ((NlsString) (((RexLiteral) function.getOperands().get(1)).getValue())).getValue();
              } catch (final ClassCastException e) {
                // Caused by user entering a value with a non-string literal
                throw getConvertFunctionInvalidTypeException(function);
              }
            } else {
              // caused by user entering a non-literal
              throw getConvertFunctionInvalidTypeException(function);
            }
          } else {
            // Second operand is missing
            throw UserException.parseError()
                    .message("'%s' expects a string literal as a second argument.", functionName)
                    .build(logger);
          }

          RexBuilder builder = new RexBuilder(factory);

          // construct the new function name based on the input argument
          String newFunctionName = functionName + literal;

          // Look up the new function name in the drill operator table
          List<SqlOperator> operatorList = table.getSqlOperator(newFunctionName);
          if (operatorList.size() == 0) {
            // User typed in an invalid type name
            throw getConvertFunctionException(functionName, literal);
          }
          SqlFunction newFunction = null;

          // Find the SqlFunction with the correct args
          for (SqlOperator op : operatorList) {
            if (op.getOperandTypeChecker().getOperandCountRange().isValidCount(nArgs - 1)) {
              newFunction = (SqlFunction) op;
              break;
            }
          }
          if (newFunction == null) {
            // we are here because we found some dummy convert function. (See DummyConvertFrom and DummyConvertTo)
            throw getConvertFunctionException(functionName, literal);
          }

          // create the new expression to be used in the rewritten project
          newExpr = builder.makeCall(newFunction, function.getOperands().subList(0, 1));
          rewrite = true;
        }
      }
      exprList.add(newExpr);
    }

    if (rewrite) {
      LogicalProject newProject = project.copy(project.getTraitSet(), project.getInput(0), exprList, project.getRowType());
      return visitChild(newProject, 0, project.getInput());
    }

    return visitChild(project, 0, project.getInput());
  }

  @Override
  public RelNode visit(LogicalFilter filter) {
    final RexNode condition = filter.getCondition().accept(unwrappingExpressionVisitor);
    filter = filter.copy(
        filter.getTraitSet(),
        filter.getInput(),
        condition);
    return visitChild(filter, 0, filter.getInput());
  }

  @Override
  public RelNode visit(LogicalJoin join) {
    final RexNode conditionExpr = join.getCondition().accept(unwrappingExpressionVisitor);
    join = join.copy(join.getTraitSet(),
        conditionExpr,
        join.getLeft(),
        join.getRight(),
        join.getJoinType(),
        join.isSemiJoinDone());

    return visitChildren(join);
  }

  @Override
  public RelNode visit(LogicalUnion union) {
    for (RelNode child : union.getInputs()) {
      for (RelDataTypeField dataField : child.getRowType().getFieldList()) {
        if (dataField.getName().contains(SchemaPath.DYNAMIC_STAR)) {
          unsupportedOperatorCollector.setException(SqlUnsupportedException.ExceptionType.RELATIONAL,
              "Union-All over schema-less tables must specify the columns explicitly\n" +
              "See Apache Drill JIRA: DRILL-2414");
          throw new UnsupportedOperationException();
        }
      }
    }

    return visitChildren(union);
  }

  private UserException getConvertFunctionInvalidTypeException(final RexCall function) {
    // Caused by user entering a value with a numeric type
    final String functionName = function.getOperator().getName();
    final String typeName = function.getOperands().get(1).getType().getFullTypeString();
    return UserException.parseError()
            .message("Invalid type %s passed as second argument to function '%s'. " +
                    "The function expects a literal argument.",
                    typeName,
                    functionName)
            .build(logger);
  }

  private UserException getConvertFunctionException(final String functionName, final String typeName) {
    final String newFunctionName = functionName + typeName;
    final String typeNameToPrint = typeName.length()==0 ? "<empty_string>" : typeName;
    final UserException.Builder exceptionBuilder = UserException.unsupportedError()
            .message("%s does not support conversion %s type '%s'.", functionName, functionName.substring(8).toLowerCase(), typeNameToPrint);
    // Build a nice error message
    if (typeName.length()>0) {
      List<String> ops = new ArrayList<>();
      for (SqlOperator op : table.getOperatorList()) {
        ops.add(op.getName());
      }
      final String bestMatch = ApproximateStringMatcher.getBestMatch(ops, newFunctionName);
      if (bestMatch != null && bestMatch.length() > functionName.length() && bestMatch.toLowerCase().startsWith("convert")) {
        final StringBuilder s = new StringBuilder("Did you mean ")
                .append(bestMatch.substring(functionName.length()))
                .append("?");
        exceptionBuilder.addContext(s.toString());
      }
    }
    return exceptionBuilder.build(logger);
  }

  public void convertException() throws SqlUnsupportedException {
    unsupportedOperatorCollector.convertException();
  }

  private static class UnwrappingExpressionVisitor extends RexShuttle {
    private final RexBuilder rexBuilder;

    private UnwrappingExpressionVisitor(RexBuilder rexBuilder) {
      this.rexBuilder = rexBuilder;
    }

    @Override
    public RexNode visitCall(final RexCall call) {
      final List<RexNode> clonedOperands = visitList(call.operands, new boolean[]{true});
      final SqlOperator sqlOperator = DrillCalciteWrapperUtility.extractSqlOperatorFromWrapper(call.getOperator());
      return RexUtil.flatten(rexBuilder,
          rexBuilder.makeCall(
              call.getType(),
              sqlOperator,
              clonedOperands));
    }
  }
}
