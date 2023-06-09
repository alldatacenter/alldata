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
package org.apache.drill.exec.planner.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.calcite.sql.type.OperandTypes;
import org.apache.calcite.sql.type.SqlOperandTypeChecker;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlSyntax;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;

public class DrillSqlOperator extends SqlFunction {
  private final boolean isDeterministic;
  private final boolean isNiladic;
  private final List<DrillFuncHolder> functions;

  /**
   * This constructor exists for the legacy reason.
   *
   * It is because Drill cannot access to DrillOperatorTable at the place where this constructor is being called.
   * In principle, if Drill needs a DrillSqlOperator, it is supposed to go to DrillOperatorTable for pickup.
   */
  @Deprecated
  public DrillSqlOperator(final String name, final int argCount, final boolean isDeterministic, final boolean isNiladic) {
    this(name,
        argCount,
        isDeterministic,
        DynamicReturnType.INSTANCE,
        isNiladic);
  }

  /**
   * This constructor exists for the legacy reason.
   *
   * It is because Drill cannot access to DrillOperatorTable at the place where this constructor is being called.
   * In principle, if Drill needs a DrillSqlOperator, it is supposed to go to DrillOperatorTable for pickup.
   */
  @Deprecated
  public DrillSqlOperator(final String name, final int argCount, final boolean isDeterministic,
      final SqlReturnTypeInference sqlReturnTypeInference, final boolean isNiladic) {
    this(name,
        new ArrayList<>(),
        Checker.getChecker(argCount, argCount),
        isDeterministic,
        sqlReturnTypeInference,
        isNiladic);
  }

  /**
   * This constructor exists for the legacy reason.
   *
   * It is because Drill cannot access to DrillOperatorTable at the place where this constructor is being called.
   * In principle, if Drill needs a DrillSqlOperator, it is supposed to go to DrillOperatorTable for pickup.
   */
  @Deprecated
  public DrillSqlOperator(final String name, final int argCount, final boolean isDeterministic, final RelDataType type, final boolean isNiladic) {
    this(name,
        new ArrayList<>(),
        Checker.getChecker(argCount, argCount),
        isDeterministic, opBinding -> type, isNiladic);
  }

  protected DrillSqlOperator(String name, List<DrillFuncHolder> functions,
      SqlOperandTypeChecker operandTypeChecker, boolean isDeterministic,
      SqlReturnTypeInference sqlReturnTypeInference, boolean isNiladic) {
    super(new SqlIdentifier(name, SqlParserPos.ZERO),
        sqlReturnTypeInference,
        null,
        operandTypeChecker,
        null,
        SqlFunctionCategory.USER_DEFINED_FUNCTION);
    this.functions = functions;
    this.isDeterministic = isDeterministic;
    this.isNiladic = isNiladic;
  }

  @Override
  public boolean isDeterministic() {
    return isDeterministic;
  }

  public boolean isNiladic() {
    return isNiladic;
  }

  public List<DrillFuncHolder> getFunctions() {
    return functions;
  }

  @Override
  public SqlSyntax getSyntax() {
    if(isNiladic) {
      return SqlSyntax.FUNCTION_ID;
    }
    return super.getSyntax();
  }

  public static class DrillSqlOperatorBuilder {
    private String name;
    private final List<DrillFuncHolder> functions = Lists.newArrayList();
    private int argCountMin = Integer.MAX_VALUE;
    private int argCountMax = Integer.MIN_VALUE;
    private boolean isDeterministic = true;
    private boolean isNiladic = false;
    private boolean isVarArg = false;

    public DrillSqlOperatorBuilder setName(final String name) {
      this.name = name;
      return this;
    }

    public DrillSqlOperatorBuilder addFunctions(Collection<DrillFuncHolder> functions) {
      this.functions.addAll(functions);
      return this;
    }

    public DrillSqlOperatorBuilder setArgumentCount(final int argCountMin, final int argCountMax) {
      this.argCountMin = Math.min(this.argCountMin, argCountMin);
      this.argCountMax = Math.max(this.argCountMax, argCountMax);
      return this;
    }

    public DrillSqlOperatorBuilder setVarArg(boolean isVarArg) {
      this.isVarArg = isVarArg;
      return this;
    }

    public DrillSqlOperatorBuilder setDeterministic(boolean isDeterministic) {
      /* By the logic here, we will group the entire Collection as a DrillSqlOperator. and claim it is non-deterministic.
       * Add if there is a non-deterministic DrillFuncHolder, then we claim this DrillSqlOperator is non-deterministic.
       *
       * In fact, in this case, separating all DrillFuncHolder into two DrillSqlOperator
       * (one being deterministic and the other being non-deterministic does not help) since in DrillOperatorTable.lookupOperatorOverloads(),
       * parameter list is not passed in. So even if we have two DrillSqlOperator, DrillOperatorTable.lookupOperatorOverloads()
       * does not have enough information to pick the one matching the argument list.
       */
      if(this.isDeterministic) {
        this.isDeterministic = isDeterministic;
      }
      return this;
    }

    public DrillSqlOperatorBuilder setNiladic(boolean isNiladic) {
      /*
       * Set Operand type-checking strategy for an operator which takes no operands and need to be invoked
       * without parentheses. E.g.: session_id
       *
       * Niladic functions override columns that have names same as any niladic function. Such columns cannot be
       * queried without the table qualification. Value of the niladic function is returned when table
       * qualification is not used.
       *
       * For e.g. in the case of session_id:
       *
       * select session_id from <table> -> returns the value of niladic function session_id
       * select t1.session_id from <table> t1 -> returns session_id column value from <table>
       *
       */
      this.isNiladic = isNiladic;
      return this;
    }

    public DrillSqlOperator build() {
      if(name == null || functions.isEmpty()) {
        throw new AssertionError("The fields, name and functions, need to be set before build DrillSqlAggOperator");
      }

      return new DrillSqlOperator(
          name,
          functions,
          isVarArg ? OperandTypes.VARIADIC : Checker.getChecker(argCountMin, argCountMax),
          isDeterministic,
          TypeInferenceUtils.getDrillSqlReturnTypeInference(
              name,
              functions),
          isNiladic);
    }
  }
}