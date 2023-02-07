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
package org.apache.drill.exec.expr.fn.output;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.planner.types.decimal.DecimalScalePrecisionAddFunction;
import org.apache.drill.exec.planner.types.decimal.DecimalScalePrecisionDivideFunction;
import org.apache.drill.exec.planner.types.decimal.DecimalScalePrecisionModFunction;
import org.apache.drill.exec.planner.types.decimal.DecimalScalePrecisionMulFunction;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.fn.FunctionAttributes;
import org.apache.drill.exec.expr.fn.FunctionUtils;
import org.apache.drill.exec.util.DecimalUtility;

import java.util.List;

import static org.apache.drill.exec.planner.types.DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM;

public class DecimalReturnTypeInference {

  /**
   * Return type calculation implementation for functions with return type set as
   * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DECIMAL_ADD_SCALE}.
   */
  public static class DecimalAddReturnTypeInference implements ReturnTypeInference {

    public static final DecimalAddReturnTypeInference INSTANCE = new DecimalAddReturnTypeInference();

    /**
     * This return type is used by add and subtract functions for decimal data type.
     * DecimalScalePrecisionAddFunction is used to compute the output types' scale and precision.
     *
     * @param logicalExpressions logical expressions
     * @param attributes function attributes
     * @return return type
     */
    @Override
    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
      TypeProtos.DataMode mode = FunctionUtils.getReturnTypeDataMode(logicalExpressions, attributes);

      assert logicalExpressions.size() == 2;

      TypeProtos.MajorType leftMajorType = logicalExpressions.get(0).getMajorType();
      TypeProtos.MajorType rightMajorType = logicalExpressions.get(1).getMajorType();

      DecimalScalePrecisionAddFunction outputScalePrec =
          new DecimalScalePrecisionAddFunction(
              DecimalUtility.getDefaultPrecision(leftMajorType.getMinorType(), leftMajorType.getPrecision()),
              leftMajorType.getScale(),
              DecimalUtility.getDefaultPrecision(rightMajorType.getMinorType(), rightMajorType.getPrecision()),
              rightMajorType.getScale());
      return TypeProtos.MajorType.newBuilder()
          .setMinorType(TypeProtos.MinorType.VARDECIMAL)
          .setScale(outputScalePrec.getOutputScale())
          .setPrecision(outputScalePrec.getOutputPrecision())
          .setMode(mode)
          .build();
    }
  }

  /**
   * Return type calculation implementation for functions with return type set as
   * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DECIMAL_AGGREGATE}.
   */
  public static class DecimalAggReturnTypeInference implements ReturnTypeInference {

    public static final DecimalAggReturnTypeInference INSTANCE = new DecimalAggReturnTypeInference();

    @Override
    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
      int scale = 0;
      int precision = 0;
      TypeProtos.DataMode mode = FunctionUtils.getReturnTypeDataMode(logicalExpressions, attributes);

      // Get the max scale and precision from the inputs
      for (LogicalExpression e : logicalExpressions) {
        scale = Math.max(scale, e.getMajorType().getScale());
        precision = Math.max(precision, e.getMajorType().getPrecision());
      }

      return TypeProtos.MajorType.newBuilder()
          .setMinorType(attributes.getReturnValue().getType().getMinorType())
          .setScale(scale)
          .setPrecision(precision)
          .setMode(mode)
          .build();
    }
  }

  /**
   * Return type calculation implementation for functions with return type set as
   * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DECIMAL_CAST}.
   */
  public static class DecimalCastReturnTypeInference implements ReturnTypeInference {

    public static final DecimalCastReturnTypeInference INSTANCE = new DecimalCastReturnTypeInference();

    @Override
    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
      TypeProtos.DataMode mode = FunctionUtils.getReturnTypeDataMode(logicalExpressions, attributes);

      if (logicalExpressions.size() != 3) {
        StringBuilder err = new StringBuilder();
        for (int i = 0; i < logicalExpressions.size(); i++) {
          err.append("arg").append(i).append(": ").append(logicalExpressions.get(i).getMajorType().getMinorType());
        }
        throw new DrillRuntimeException("Decimal cast function invoked with incorrect arguments" + err);
      }

      int scale = ((ValueExpressions.IntExpression) logicalExpressions.get(logicalExpressions.size() - 1)).getInt();
      int precision = ((ValueExpressions.IntExpression) logicalExpressions.get(logicalExpressions.size() - 2)).getInt();
      return TypeProtos.MajorType.newBuilder()
          .setMinorType(attributes.getReturnValue().getType().getMinorType())
          .setScale(scale)
          .setPrecision(precision)
          .setMode(mode)
          .build();
    }
  }

  /**
   * Return type calculation implementation for functions with return type set as
   * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DECIMAL_DIV_SCALE}.
   */
  public static class DecimalDivScaleReturnTypeInference implements ReturnTypeInference {

    public static final DecimalDivScaleReturnTypeInference INSTANCE = new DecimalDivScaleReturnTypeInference();

    /**
     * Return type is used by divide functions for decimal data type.
     * DecimalScalePrecisionDivideFunction is used to compute the output types' scale and precision.
     *
     * @param logicalExpressions logical expressions
     * @param attributes function attributes
     * @return return type
     */
    @Override
    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
      TypeProtos.DataMode mode = FunctionUtils.getReturnTypeDataMode(logicalExpressions, attributes);

      assert logicalExpressions.size() == 2;

      TypeProtos.MajorType leftMajorType = logicalExpressions.get(0).getMajorType();
      TypeProtos.MajorType rightMajorType = logicalExpressions.get(1).getMajorType();

      DecimalScalePrecisionDivideFunction outputScalePrec =
          new DecimalScalePrecisionDivideFunction(
              DecimalUtility.getDefaultPrecision(leftMajorType.getMinorType(), leftMajorType.getPrecision()),
              leftMajorType.getScale(),
              DecimalUtility.getDefaultPrecision(rightMajorType.getMinorType(), rightMajorType.getPrecision()),
              rightMajorType.getScale());
      return TypeProtos.MajorType.newBuilder()
          .setMinorType(TypeProtos.MinorType.VARDECIMAL)
          .setScale(outputScalePrec.getOutputScale())
          .setPrecision(outputScalePrec.getOutputPrecision())
          .setMode(mode)
          .build();
    }
  }

  /**
   * Return type calculation implementation for functions with return type set as
   * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DECIMAL_MAX_SCALE}.
   */
  public static class DecimalMaxScaleReturnTypeInference implements ReturnTypeInference {

    public static final DecimalMaxScaleReturnTypeInference INSTANCE = new DecimalMaxScaleReturnTypeInference();

    @Override
    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {

      TypeProtos.DataMode mode = FunctionUtils.getReturnTypeDataMode(logicalExpressions, attributes);
      int scale = 0;
      int precision = 0;

      for (LogicalExpression e : logicalExpressions) {
        scale = Math.max(scale, e.getMajorType().getScale());
        precision = Math.max(precision, e.getMajorType().getPrecision());
      }

      return TypeProtos.MajorType.newBuilder()
          .setMinorType(attributes.getReturnValue().getType().getMinorType())
          .setScale(scale)
          .setPrecision(precision)
          .setMode(mode)
          .build();
    }
  }

  /**
   * Return type calculation implementation for functions with return type set as
   * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DECIMAL_MOD_SCALE}.
   */
  public static class DecimalModScaleReturnTypeInference implements ReturnTypeInference {

    public static final DecimalModScaleReturnTypeInference INSTANCE = new DecimalModScaleReturnTypeInference();

    /**
     * Return type is used by divide functions for decimal data type.
     * DecimalScalePrecisionDivideFunction is used to compute the output types' scale and precision.
     *
     * @param logicalExpressions logical expressions
     * @param attributes function attributes
     * @return return type
     */
    @Override
    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
      TypeProtos.DataMode mode = FunctionUtils.getReturnTypeDataMode(logicalExpressions, attributes);

      assert logicalExpressions.size() == 2;

      TypeProtos.MajorType leftMajorType = logicalExpressions.get(0).getMajorType();
      TypeProtos.MajorType rightMajorType = logicalExpressions.get(1).getMajorType();

      DecimalScalePrecisionModFunction outputScalePrec =
          new DecimalScalePrecisionModFunction(
              DecimalUtility.getDefaultPrecision(leftMajorType.getMinorType(), leftMajorType.getPrecision()),
              leftMajorType.getScale(),
              DecimalUtility.getDefaultPrecision(rightMajorType.getMinorType(), rightMajorType.getPrecision()),
              rightMajorType.getScale());
      return TypeProtos.MajorType.newBuilder()
          .setMinorType(TypeProtos.MinorType.VARDECIMAL)
          .setScale(outputScalePrec.getOutputScale())
          .setPrecision(outputScalePrec.getOutputPrecision())
          .setMode(mode)
          .build();
    }
  }

  /**
   * Return type calculation implementation for functions with return type set as
   * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DECIMAL_SET_SCALE}.
   */
  public static class DecimalSetScaleReturnTypeInference implements ReturnTypeInference {

    public static final DecimalSetScaleReturnTypeInference INSTANCE = new DecimalSetScaleReturnTypeInference();

    @Override
    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
      TypeProtos.DataMode mode = attributes.getReturnValue().getType().getMode();
      int scale = 0;
      int precision = 0;

      if (attributes.getNullHandling() == FunctionTemplate.NullHandling.NULL_IF_NULL) {
        // if any one of the input types is nullable, then return nullable return type
        for (LogicalExpression e : logicalExpressions) {

          precision = Math.max(precision, e.getMajorType().getPrecision());
          if (e.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) {
            mode = TypeProtos.DataMode.OPTIONAL;
          }
        }

        // Used by functions like round, truncate which specify the scale for the output as the second argument
        assert (logicalExpressions.size() == 2) && (logicalExpressions.get(1) instanceof ValueExpressions.IntExpression);

        // Get the scale from the second argument which should be a constant
        scale = ((ValueExpressions.IntExpression) logicalExpressions.get(1)).getInt();
      }

      return TypeProtos.MajorType.newBuilder()
          .setMinorType(attributes.getReturnValue().getType().getMinorType())
          .setScale(Math.max(scale, 0))
          .setPrecision(precision)
          .setMode(mode)
          .build();
    }
  }

  /**
   * Return type calculation implementation for functions with return type set as
   * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DECIMAL_SUM_AGGREGATE}.
   */
  public static class DecimalSumAggReturnTypeInference implements ReturnTypeInference {

    public static final DecimalSumAggReturnTypeInference INSTANCE = new DecimalSumAggReturnTypeInference();

    @Override
    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
      int scale = 0;
      TypeProtos.DataMode mode = FunctionUtils.getReturnTypeDataMode(logicalExpressions, attributes);

      // Get the max scale and precision from the inputs
      for (LogicalExpression e : logicalExpressions) {
        scale = Math.max(scale, e.getMajorType().getScale());
      }

      return TypeProtos.MajorType.newBuilder()
          .setMinorType(TypeProtos.MinorType.VARDECIMAL)
          .setScale(scale)
          .setPrecision(DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision())
          .setMode(mode)
          .build();
    }
  }

  /**
   * Return type calculation implementation for functions with return type set as
   * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DECIMAL_AVG_AGGREGATE}.
   * Resulting scale is calculated as the max of 6 and the scale of input.
   * Resulting precision is max allowed numeric precision.
   */
  public static class DecimalAvgAggReturnTypeInference implements ReturnTypeInference {

    public static final DecimalAvgAggReturnTypeInference INSTANCE = new DecimalAvgAggReturnTypeInference();

    @Override
    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
      int scale = 0;
      TypeProtos.DataMode mode = FunctionUtils.getReturnTypeDataMode(logicalExpressions, attributes);

      // Get the max scale and precision from the inputs
      for (LogicalExpression e : logicalExpressions) {
        scale = Math.max(scale, e.getMajorType().getScale());
      }

      return TypeProtos.MajorType.newBuilder()
          .setMinorType(TypeProtos.MinorType.VARDECIMAL)
          .setScale(Math.min(Math.max(6, scale),
              DRILL_REL_DATATYPE_SYSTEM.getMaxNumericScale()))
          .setPrecision(DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision())
          .setMode(mode)
          .build();
    }
  }

  /**
   * Return type calculation implementation for functions with return type set as
   * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DECIMAL_SUM_SCALE}.
   */
  public static class DecimalSumScaleReturnTypeInference implements ReturnTypeInference {

    public static final DecimalSumScaleReturnTypeInference INSTANCE = new DecimalSumScaleReturnTypeInference();

    @Override
    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {

      TypeProtos.DataMode mode = FunctionUtils.getReturnTypeDataMode(logicalExpressions, attributes);

      assert logicalExpressions.size() == 2;

      TypeProtos.MajorType leftMajorType = logicalExpressions.get(0).getMajorType();
      TypeProtos.MajorType rightMajorType = logicalExpressions.get(1).getMajorType();

      DecimalScalePrecisionMulFunction outputScalePrec =
          new DecimalScalePrecisionMulFunction(
              DecimalUtility.getDefaultPrecision(leftMajorType.getMinorType(), leftMajorType.getPrecision()),
              leftMajorType.getScale(),
              DecimalUtility.getDefaultPrecision(rightMajorType.getMinorType(), rightMajorType.getPrecision()),
              rightMajorType.getScale());
      return TypeProtos.MajorType.newBuilder()
          .setMinorType(TypeProtos.MinorType.VARDECIMAL)
          .setScale(outputScalePrec.getOutputScale())
          .setPrecision(outputScalePrec.getOutputPrecision())
          .setMode(mode)
          .build();
    }
  }

  /**
   * Return type calculation implementation for functions with return type set as
   * {@link org.apache.drill.exec.expr.annotations.FunctionTemplate.ReturnType#DECIMAL_ZERO_SCALE}.
   */
  public static class DecimalZeroScaleReturnTypeInference implements ReturnTypeInference {

    public static final DecimalZeroScaleReturnTypeInference INSTANCE = new DecimalZeroScaleReturnTypeInference();

    /**
     * Return type is used for functions where we need to remove the scale part.
     * For example, truncate and round functions.
     *
     * @param logicalExpressions logical expressions
     * @param attributes function attributes
     * @return return type
     */
    @Override
    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {

      int precision = 0;
      TypeProtos.DataMode mode = attributes.getReturnValue().getType().getMode();

      if (attributes.getNullHandling() == FunctionTemplate.NullHandling.NULL_IF_NULL) {
        // if any one of the input types is nullable, then return nullable return type
        for (LogicalExpression e : logicalExpressions) {
          if (e.getMajorType().getMode() == TypeProtos.DataMode.OPTIONAL) {
            mode = TypeProtos.DataMode.OPTIONAL;
          }
          precision = Math.max(precision, e.getMajorType().getPrecision());
        }
      }

      return TypeProtos.MajorType.newBuilder()
          .setMinorType(attributes.getReturnValue().getType().getMinorType())
          .setScale(0)
          .setPrecision(precision)
          .setMode(mode)
          .build();
    }
  }
}
