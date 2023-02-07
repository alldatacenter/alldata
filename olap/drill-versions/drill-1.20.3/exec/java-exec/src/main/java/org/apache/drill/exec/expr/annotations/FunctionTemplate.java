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
package org.apache.drill.exec.expr.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.fn.FunctionAttributes;
import org.apache.drill.exec.expr.fn.output.ConcatReturnTypeInference;
import org.apache.drill.exec.expr.fn.output.DecimalReturnTypeInference;
import org.apache.drill.exec.expr.fn.output.DefaultReturnTypeInference;
import org.apache.drill.exec.expr.fn.output.OutputWidthCalculator;
import org.apache.drill.exec.expr.fn.output.OutputWidthCalculators;
import org.apache.drill.exec.expr.fn.output.PadReturnTypeInference;
import org.apache.drill.exec.expr.fn.output.ReturnTypeInference;
import org.apache.drill.exec.expr.fn.output.SameInOutLengthReturnTypeInference;
import org.apache.drill.exec.expr.fn.output.StringCastReturnTypeInference;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface FunctionTemplate {

  /**
   * Name of function (when only one.)
   * Use this annotation element if there is only one name for the function.
   * Note: If you use this annotation don't use {@link #names()}.
   * <p>
   *   TODO:  Refer to wherever list of possible or at least known names is,
   *   to resolve the current issue of spaces vs. underlines in names (e.g., we
   *   have both "less_than" and "less than".
   * </p>
   * @return function name
   */
  String name() default "";

  /**
   * Names of function (when multiple).
   * Use this annotation element if there are multiple names for the function.
   * Note: If you use this annotation don't use {@link #name()}.
   * <p>
   *   TODO:  Refer to wherever list of possible or at least known names is,
   *   to resolve the current issue of spaces vs. underlines in names (e.g., we
   *   have both "less_than" and "less than".
   * </p>
   * @return list of function names
   */
  String[] names() default {};

  FunctionScope scope();
  ReturnType returnType() default ReturnType.DEFAULT;
  NullHandling nulls() default NullHandling.INTERNAL;
  boolean isBinaryCommutative() default false;
  boolean isRandom()  default false;
  String desc() default "";
  FunctionCostCategory costCategory() default FunctionCostCategory.SIMPLE;

  /**
   * <p>Set Operand type-checking strategy for an operator which takes no operands
   * and need to be invoked without parentheses. E.g.: session_id is a niladic
   * function.</p>
   *
   * <p>Niladic functions override columns that have names same as any niladic
   * function. Such columns cannot be queried without the table qualification.
   * Value of the niladic function is returned when table qualification is not
   * used.</p>
   *
   * <p>For e.g. in the case of session_id:<br/>
   * select session_id from table -> returns the value of niladic function
   * session_id<br/>
   * select t1.session_id from table t1 -> returns session_id column value from
   * table</p>
   */
  boolean isNiladic() default false;
  boolean checkPrecisionRange() default false;

  /**
   * Defines if a function is internal and not intended for public use [e.g. castEmptyStringNullableVarBinaryToNullableVarDecimal(..) ]
   */
  boolean isInternal() default false;

  /**
   * Defines if a function accepts variable arguments count
   */
  boolean isVarArg() default false;

  /**
   * Estimates the average size of the output
   * produced by a function that produces variable length output
   */
  enum OutputWidthCalculatorType {
    DEFAULT(OutputWidthCalculators.DefaultOutputWidthCalculator.INSTANCE),
    CLONE(OutputWidthCalculators.CloneOutputWidthCalculator.INSTANCE),
    CONCAT(OutputWidthCalculators.ConcatOutputWidthCalculator.INSTANCE),
    // Custom calculator is required for functions that don't fall into any predefined
    // calculator categories - like replace and lpad
    // place holder markers on functions until support
    // for CUSTOM calculators is implemented
    // CUSTOM_FIXED_WIDTH_DEFAULT will default to a fixed size - for functions like
    // lpad() where the ouput size does not easily map to the input size
    CUSTOM_FIXED_WIDTH_DEFAULT(OutputWidthCalculators.DefaultOutputWidthCalculator.INSTANCE),
    // CUSTOM CLONE will default to CLONE - for functions like replace() where the output
    // size  does not easily map to the input size but is likely to be at most the size of the input.
    CUSTOM_CLONE_DEFAULT(OutputWidthCalculators.CloneOutputWidthCalculator.INSTANCE);
    OutputWidthCalculator outputWidthCalculator;

    OutputWidthCalculatorType(OutputWidthCalculator outputWidthCalculator) {
      this.outputWidthCalculator = outputWidthCalculator;
    }

    public OutputWidthCalculator getOutputWidthCalculator() { return outputWidthCalculator; }
  }

  OutputWidthCalculatorType outputWidthCalculatorType() default OutputWidthCalculatorType.DEFAULT;

  int OUTPUT_SIZE_ESTIMATE_DEFAULT = -1;
  int outputSizeEstimate() default OUTPUT_SIZE_ESTIMATE_DEFAULT;

  enum NullHandling {
    /**
     * Method handles nulls.
     */
    INTERNAL,

    /**
     * Null output if any null input:
     * Indicates that a method's associated logical operation returns NULL if
     * either input is NULL, and therefore that the method must not be called
     * with null inputs.  (The calling framework must handle NULLs.)
     *
     * <p>Not Supported for aggregate functions and for functions with {@link Output} of type
     * {@link org.apache.drill.exec.vector.complex.writer.BaseWriter.ComplexWriter}.</p>
     */
    NULL_IF_NULL
  }

  /**
   * Function scope is used to indicate function output rows relation:
   * simple / scalar (1 -> 1) or aggregate (n -> 1).
   */
  enum FunctionScope {
    SIMPLE,
    POINT_AGGREGATE,
    HOLISTIC_AGGREGATE,
    RANGE_AGGREGATE
  }

  /**
   * Return type enum is used to indicate which return type calculation logic
   * should be used for functions.
   */
  enum ReturnType {
    DEFAULT(DefaultReturnTypeInference.INSTANCE),

    STRING_CAST(StringCastReturnTypeInference.INSTANCE),
    CONCAT(ConcatReturnTypeInference.INSTANCE),
    PAD(PadReturnTypeInference.INSTANCE),
    SAME_IN_OUT_LENGTH(SameInOutLengthReturnTypeInference.INSTANCE),

    DECIMAL_AGGREGATE(DecimalReturnTypeInference.DecimalAggReturnTypeInference.INSTANCE),
    DECIMAL_SUM_AGGREGATE(DecimalReturnTypeInference.DecimalSumAggReturnTypeInference.INSTANCE),
    DECIMAL_AVG_AGGREGATE(DecimalReturnTypeInference.DecimalAvgAggReturnTypeInference.INSTANCE),
    DECIMAL_MAX_SCALE(DecimalReturnTypeInference.DecimalMaxScaleReturnTypeInference.INSTANCE),
    DECIMAL_SUM_SCALE(DecimalReturnTypeInference.DecimalSumScaleReturnTypeInference.INSTANCE),
    DECIMAL_CAST(DecimalReturnTypeInference.DecimalCastReturnTypeInference.INSTANCE),
    DECIMAL_DIV_SCALE(DecimalReturnTypeInference.DecimalDivScaleReturnTypeInference.INSTANCE),
    DECIMAL_MOD_SCALE(DecimalReturnTypeInference.DecimalModScaleReturnTypeInference.INSTANCE),
    DECIMAL_ADD_SCALE(DecimalReturnTypeInference.DecimalAddReturnTypeInference.INSTANCE),
    DECIMAL_SET_SCALE(DecimalReturnTypeInference.DecimalSetScaleReturnTypeInference.INSTANCE),
    DECIMAL_ZERO_SCALE(DecimalReturnTypeInference.DecimalZeroScaleReturnTypeInference.INSTANCE);

    private final ReturnTypeInference inference;

    ReturnType(ReturnTypeInference inference) {
      this.inference = inference;
    }

    public TypeProtos.MajorType getType(List<LogicalExpression> logicalExpressions, FunctionAttributes attributes) {
      return inference.getType(logicalExpressions, attributes);
    }
  }

  enum FunctionCostCategory {
    SIMPLE(1), MEDIUM(20), COMPLEX(50);

    private final int value;

    FunctionCostCategory(int value) {
      this.value = value;
    }

    public int getValue() {
      return this.value;
    }

    public static FunctionCostCategory getDefault() {
      return SIMPLE;
    }
  }
}
