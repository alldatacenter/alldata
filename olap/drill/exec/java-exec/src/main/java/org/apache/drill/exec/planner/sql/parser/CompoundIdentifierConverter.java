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
package org.apache.drill.exec.planner.sql.parser;

import java.util.List;
import java.util.Map;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlJoin;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOrderBy;
import org.apache.calcite.sql.SqlSelect;
import org.apache.calcite.sql.util.SqlShuttle;
import org.apache.calcite.sql.util.SqlVisitor;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;

/**
 * Implementation of {@link SqlVisitor} that converts bracketed compound {@link SqlIdentifier}
 * to bracket-less compound {@link SqlIdentifier} (also known as {@link DrillCompoundIdentifier})
 * to provide ease of use while querying complex types.
 * <p/>
 * For example, this visitor converts {@code a['b'][4]['c']} to {@code a.b[4].c}
 */
public class CompoundIdentifierConverter extends SqlShuttle {
  /**
   * This map stores the rules that instruct each SqlCall class which data field needs
   * to be rewritten if that data field is a {@link DrillCompoundIdentifier}.
   * <p/>
   * <ul>
   * <li>Key  : Each rule corresponds to a {@link SqlCall} class;
   * <li>Value: It is an array of {@link RewriteType}, each being associated with a data field
   * in that class.
   * </ul>
   * <p/>
   * For example, there are four data fields (query, orderList, offset, fetch)
   * in {@link SqlOrderBy}. Since only orderList needs to be written,
   * {@link RewriteType[]} should be {@code arrayOf(D, E, D, D)}.
   */
  private static final Map<Class<? extends SqlCall>, RewriteType[]> REWRITE_RULES;

  static {
    final RewriteType E = RewriteType.ENABLE;
    final RewriteType D = RewriteType.DISABLE;

    // Every element of the array corresponds to the item in the list
    // returned by getOperandList() method for concrete SqlCall implementation.
    REWRITE_RULES = ImmutableMap.<Class<? extends SqlCall>, RewriteType[]>builder()
        .put(SqlAnalyzeTable.class, arrayOf(D, D, E, D))
        .put(SqlMetastoreAnalyzeTable.class, arrayOf(D, E, D, D, D))
        .put(SqlDropTableMetadata.class, arrayOf(D, D, D))
        .put(SqlSelect.class, arrayOf(D, E, D, E, E, E, E, E, D, D))
        .put(SqlCreateTable.class, arrayOf(D, D, D, E, D, D))
        .put(SqlCreateView.class, arrayOf(D, E, E, D))
        .put(DrillSqlDescribeTable.class, arrayOf(D, D, E))
        .put(SqlDropView.class, arrayOf(D, D))
        .put(SqlShowFiles.class, arrayOf(D))
        .put(SqlShowSchemas.class, arrayOf(D, D))
        .put(SqlUseSchema.class, arrayOf(D))
        .put(SqlJoin.class, arrayOf(D, D, D, D, D, E))
        .put(SqlOrderBy.class, arrayOf(D, E, D, D))
        .put(SqlDropTable.class, arrayOf(D, D))
        .put(SqlRefreshMetadata.class, arrayOf(D, D, E))
        .put(DrillSqlSetOption.class, arrayOf(D, D, D))
        .put(DrillSqlResetOption.class, arrayOf(D, D))
        .put(SqlCreateFunction.class, arrayOf(D))
        .put(SqlDropFunction.class, arrayOf(D))
        .put(SqlSchema.Create.class, arrayOf(D, D, D, D, D, D))
        .put(SqlSchema.Drop.class, arrayOf(D, D))
        .put(SqlSchema.Describe.class, arrayOf(D, D))
        .put(SqlSchema.Add.class, arrayOf(D, D, D, D, D, D))
        .put(SqlSchema.Remove.class, arrayOf(D, D, D, D, D))
        .build();
  }

  private boolean enableComplex = true;
  private boolean allowNoTableRefCompoundIdentifier = false;

  public CompoundIdentifierConverter(boolean allowNoTableRefCompoundIdentifier) {
    this.allowNoTableRefCompoundIdentifier = allowNoTableRefCompoundIdentifier;
  }

  @Override
  public SqlNode visit(SqlIdentifier id) {
    if (id instanceof DrillCompoundIdentifier) {
      DrillCompoundIdentifier compoundIdentifier = (DrillCompoundIdentifier) id;
      if (enableComplex) {
        return compoundIdentifier.getAsSqlNode(allowNoTableRefCompoundIdentifier);
      } else {
        return compoundIdentifier.getAsCompoundIdentifier();
      }
    } else {
      return id;
    }
  }

  @Override
  public SqlNode visit(final SqlCall call) {
    // Handler creates a new copy of 'call' only if one or more operands
    // change.
    ArgHandler<SqlNode> argHandler = new ComplexExpressionAware(call);
    boolean localEnableComplex = enableComplex;
    // for the case of UNNEST call set enableComplex to true
    // to convert DrillCompoundIdentifier to the item call.
    if (call.getKind() == SqlKind.UNNEST) {
      enableComplex = true;
    }
    call.getOperator().acceptCall(this, call, false, argHandler);
    enableComplex = localEnableComplex;
    return argHandler.result();
  }

  /**
   * Constructs array which contains specified parameters.
   */
  private static RewriteType[] arrayOf(RewriteType... types) {
    return types;
  }

  enum RewriteType {
    UNCHANGED, DISABLE, ENABLE
  }

  /**
   * Argument handler which accepts {@link CompoundIdentifierConverter}
   * for every operand of {@link SqlCall} and constructs new {@link SqlCall}
   * if one or more operands changed.
   */
  private class ComplexExpressionAware implements ArgHandler<SqlNode> {

    private final SqlCall call;
    private final SqlNode[] clonedOperands;
    private final RewriteType[] rewriteTypes;

    private boolean update;

    public ComplexExpressionAware(SqlCall call) {
      this.call = call;
      this.update = false;
      final List<SqlNode> operands = call.getOperandList();
      this.clonedOperands = operands.toArray(new SqlNode[0]);
      rewriteTypes = REWRITE_RULES.get(call.getClass());
    }

    @Override
    public SqlNode result() {
      if (update) {
        return call.getOperator().createCall(
            call.getFunctionQuantifier(),
            call.getParserPosition(),
            clonedOperands);
      } else {
        return call;
      }
    }

    @Override
    public SqlNode visitChild(
        SqlVisitor<SqlNode> visitor,
        SqlNode expr,
        int i,
        SqlNode operand) {
      if (operand == null) {
        return null;
      }

      boolean localEnableComplex = enableComplex;
      if (rewriteTypes != null) {
        switch (rewriteTypes[i]) {
          case DISABLE:
            enableComplex = false;
            break;
          case ENABLE:
            enableComplex = true;
        }
      }
      SqlNode newOperand = operand.accept(CompoundIdentifierConverter.this);
      enableComplex = localEnableComplex;
      if (newOperand != operand) {
        update = true;
      }
      clonedOperands[i] = newOperand;
      return newOperand;
    }
  }
}
