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
package org.apache.drill.exec.planner.sql.conversion;

import java.math.BigDecimal;

import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.util.DecimalUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class DrillRexBuilder extends RexBuilder {

  private static final Logger logger = LoggerFactory.getLogger(DrillRexBuilder.class);

  DrillRexBuilder(RelDataTypeFactory typeFactory) {
    super(typeFactory);
  }

  /**
   * Since Drill has different mechanism and rules for implicit casting,
   * ensureType() is overridden to avoid conflicting cast functions being added to the expressions.
   */
  @Override
  public RexNode ensureType(
      RelDataType type,
      RexNode node,
      boolean matchNullability) {
    return node;
  }

  /**
   * Creates a call to the CAST operator, expanding if possible, and optionally
   * also preserving nullability.
   *
   * <p>Tries to expand the cast, and therefore the result may be something
   * other than a {@link org.apache.calcite.rex.RexCall} to the CAST operator, such as a
   * {@link RexLiteral} if {@code matchNullability} is false.
   *
   * @param type             Type to cast to
   * @param exp              Expression being cast
   * @param matchNullability Whether to ensure the result has the same
   *                         nullability as {@code type}
   * @return Call to CAST operator
   */
  @Override
  public RexNode makeCast(RelDataType type, RexNode exp, boolean matchNullability) {
    if (matchNullability) {
      return makeAbstractCast(type, exp);
    }
    // for the case when BigDecimal literal has a scale or precision
    // that differs from the value from specified RelDataType, cast cannot be removed
    // TODO: remove this code when CALCITE-1468 is fixed
    if (type.getSqlTypeName() == SqlTypeName.DECIMAL && exp instanceof RexLiteral) {
      int precision = type.getPrecision();
      int scale = type.getScale();
      validatePrecisionAndScale(precision, scale);
      Comparable<?> value = ((RexLiteral) exp).getValueAs(Comparable.class);
      if (value instanceof BigDecimal) {
        BigDecimal bigDecimal = (BigDecimal) value;
        DecimalUtility.checkValueOverflow(bigDecimal, precision, scale);
        if (bigDecimal.precision() != precision || bigDecimal.scale() != scale) {
          return makeAbstractCast(type, exp);
        }
      }
    }
    return super.makeCast(type, exp, false);
  }

  private void validatePrecisionAndScale(int precision, int scale) {
    if (precision < 1) {
      throw UserException.validationError()
          .message("Expected precision greater than 0, but was %s.", precision)
          .build(logger);
    }
    if (scale > precision) {
      throw UserException.validationError()
          .message("Expected scale less than or equal to precision, " +
              "but was precision %s and scale %s.", precision, scale)
          .build(logger);
    }
  }
}
