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

import org.apache.calcite.sql.fun.SqlBetweenOperator;
import org.apache.calcite.sql.SqlCallBinding;
import org.apache.calcite.sql.SqlOperator;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.resolver.TypeCastRules;

import java.util.ArrayList;
import java.util.List;

/**
 * This class serves as a wrapper class for SqlBetweenOperator. The motivation is to plug-in the return type inference and operand
 * type check algorithms of Drill into Calcite's sql validation procedure.
 *
 * Except for the methods which are relevant to the return type inference and operand type check algorithms, the wrapper
 * simply forwards the method calls to the wrapped SqlOperator.
 *
 * Note that SqlBetweenOperator cannot be wrapped in {@link DrillCalciteSqlOperatorWrapper}. The reason is when RexNode
 * conversion is happening, StandardConvertletTable.convertBetween expects the SqlOperator to be a subclass of SqlBetweenOperator.
 */
public class DrillCalciteSqlBetweenOperatorWrapper extends SqlBetweenOperator implements DrillCalciteSqlWrapper  {
  private final SqlBetweenOperator operator;

  public DrillCalciteSqlBetweenOperatorWrapper(SqlBetweenOperator sqlBetweenOperator) {
    super(sqlBetweenOperator.flag, sqlBetweenOperator.isNegated());
    operator = sqlBetweenOperator;
  }

  @Override
  public SqlOperator getOperator() {
    return operator;
  }

  /**
   * Since Calcite has its rule for type compatibility
   * (see {@link org.apache.calcite.sql.type.SqlTypeUtil#isComparable(org.apache.calcite.rel.type.RelDataType,
   * org.apache.calcite.rel.type.RelDataType)}), which is usually different from Drill's, this method is overridden here to avoid
   * Calcite early terminating the queries.
   */
  @Override
  public boolean checkOperandTypes(SqlCallBinding callBinding, boolean throwOnFailure) {
    final List<TypeProtos.MinorType> types = new ArrayList<>();
    for (int i = 0; i < callBinding.getOperandCount(); i++) {
      final TypeProtos.MinorType inMinorType = TypeInferenceUtils.getDrillTypeFromCalciteType(callBinding.getOperandType(i));
      if (inMinorType == TypeProtos.MinorType.LATE) {
        return true;
      }
      types.add(inMinorType);
    }

    final boolean isCompatible = TypeCastRules.getLeastRestrictiveType(types) != null;
    if (!isCompatible && throwOnFailure) {
      throw callBinding.newValidationSignatureError();
    }
    return isCompatible;
  }
}
