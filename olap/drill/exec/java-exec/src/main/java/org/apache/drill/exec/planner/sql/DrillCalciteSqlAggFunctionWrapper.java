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

import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCallBinding;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.SqlSyntax;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlReturnTypeInference;
import org.apache.calcite.sql.validate.SqlMonotonicity;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorScope;

import org.apache.calcite.util.Litmus;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;

import java.util.List;

/**
 * This class serves as a wrapper class for SqlAggFunction. The motivation is to plug-in the return type inference and operand
 * type check algorithms of Drill into Calcite's sql validation procedure.
 *
 * Except for the methods which are relevant to the return type inference and operand type check algorithms, the wrapper
 * simply forwards the method calls to the wrapped SqlAggFunction.
 */
public class DrillCalciteSqlAggFunctionWrapper extends SqlAggFunction implements DrillCalciteSqlWrapper {

  public final static DrillCalciteSqlAggFunctionWrapper SUM =
      new DrillCalciteSqlAggFunctionWrapper(SqlStdOperatorTable.SUM,
          SqlStdOperatorTable.SUM.getReturnTypeInference());

  private final SqlAggFunction operator;

  @Override
  public SqlOperator getOperator() {
    return operator;
  }

  private DrillCalciteSqlAggFunctionWrapper(
      SqlAggFunction sqlAggFunction,
      SqlReturnTypeInference sqlReturnTypeInference) {
    super(sqlAggFunction.getName(),
        sqlAggFunction.getSqlIdentifier(),
        sqlAggFunction.getKind(),
        sqlReturnTypeInference,
        sqlAggFunction.getOperandTypeInference(),
        Checker.ANY_CHECKER,
        sqlAggFunction.getFunctionType(),
        sqlAggFunction.requiresOrder(),
        sqlAggFunction.requiresOver());
    this.operator = sqlAggFunction;
  }

  public DrillCalciteSqlAggFunctionWrapper(
      SqlAggFunction sqlAggFunction,
      List<DrillFuncHolder> functions) {
    this(sqlAggFunction,
        TypeInferenceUtils.getDrillSqlReturnTypeInference(
            sqlAggFunction.getName(),
            functions));
  }

  public DrillCalciteSqlAggFunctionWrapper(
      final SqlAggFunction sqlAggFunction,
      final RelDataType relDataType) {
    this(sqlAggFunction, new SqlReturnTypeInference() {
      @Override
      public RelDataType inferReturnType(SqlOperatorBinding opBinding) {
        return relDataType;
      }
    });
  }

  @Override
  public boolean validRexOperands(int count, Litmus litmus) {
    return true;
  }

  @Override
  public String getAllowedSignatures(String opNameToUse) {
    return operator.getAllowedSignatures(opNameToUse);
  }

  @Override
  public boolean isAggregator() {
    return operator.isAggregator();
  }

  @Override
  public boolean allowsFraming() {
    return operator.allowsFraming();
  }

  @Override
  public SqlMonotonicity getMonotonicity(SqlOperatorBinding call) {
    return operator.getMonotonicity(call);
  }

  @Override
  public boolean isDeterministic() {
    return operator.isDeterministic();
  }

  @Override
  public boolean isDynamicFunction() {
    return operator.isDynamicFunction();
  }

  @Override
  public boolean requiresDecimalExpansion() {
    return operator.requiresDecimalExpansion();
  }

  @Override
  public boolean argumentMustBeScalar(int ordinal) {
    return operator.argumentMustBeScalar(ordinal);
  }

  @Override
  public boolean checkOperandTypes(
      SqlCallBinding callBinding,
      boolean throwOnFailure) {
    return true;
  }

  @Override
  public SqlSyntax getSyntax() {
    return operator.getSyntax();
  }

  @Override
  public List<String> getParamNames() {
    return operator.getParamNames();
  }

  @Override
  public String getSignatureTemplate(final int operandsCount) {
    return operator.getSignatureTemplate(operandsCount);
  }

  @Override
  public RelDataType deriveType(
      SqlValidator validator,
      SqlValidatorScope scope,
      SqlCall call) {
    return operator.deriveType(validator,
        scope,
        call);
  }
}
