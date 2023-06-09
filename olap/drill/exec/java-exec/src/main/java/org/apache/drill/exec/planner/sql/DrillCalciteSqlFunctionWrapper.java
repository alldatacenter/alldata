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

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlCallBinding;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlOperatorBinding;
import org.apache.calcite.sql.SqlSyntax;
import org.apache.calcite.sql.SqlWriter;
import org.apache.calcite.sql.validate.SqlMonotonicity;
import org.apache.calcite.sql.validate.SqlValidator;
import org.apache.calcite.sql.validate.SqlValidatorScope;

import org.apache.calcite.util.Litmus;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;

import java.util.List;

/**
 * This class serves as a wrapper class for SqlFunction. The motivation is to plug-in the return type inference and operand
 * type check algorithms of Drill into Calcite's sql validation procedure.
 *
 * Except for the methods which are relevant to the return type inference and operand type check algorithms, the wrapper
 * simply forwards the method calls to the wrapped SqlFunction.
 */
public class DrillCalciteSqlFunctionWrapper extends SqlFunction implements DrillCalciteSqlWrapper  {
  private final SqlFunction operator;

  public DrillCalciteSqlFunctionWrapper(
      final SqlFunction wrappedFunction,
    final List<DrillFuncHolder> functions) {
    super(wrappedFunction.getName(),
        wrappedFunction.getSqlIdentifier(),
        wrappedFunction.getKind(),
        TypeInferenceUtils.getDrillSqlReturnTypeInference(
            wrappedFunction.getName(),
            functions),
        wrappedFunction.getOperandTypeInference(),
        Checker.ANY_CHECKER,
        wrappedFunction.getParamTypes(),
        wrappedFunction.getFunctionType());
    this.operator = wrappedFunction;
  }

  @Override
  public SqlNode rewriteCall(SqlValidator validator, SqlCall call) {
    return operator.rewriteCall(validator, call);
  }

  @Override
  public SqlOperator getOperator() {
    return operator;
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

  @Override
  public String toString() {
    return operator.toString();
  }

  @Override
  public void unparse(
      SqlWriter writer,
      SqlCall call,
      int leftPrec,
      int rightPrec) {
    operator.unparse(writer, call, leftPrec, rightPrec);
  }
}
