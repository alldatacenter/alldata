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
package org.apache.drill.exec.planner.physical;

import org.apache.drill.exec.planner.physical.DrillDistributionTrait.NamedDistributionField;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rex.RexBuilder;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.exec.planner.physical.DrillDistributionTrait.DistributionField;
import org.apache.drill.exec.planner.sql.DrillSqlOperator;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains utility methods for creating hash expression for either distribution (in PartitionSender) or for HashTable.
 */
public class HashPrelUtil {

  public static final String HASH_EXPR_NAME = "E_X_P_R_H_A_S_H_F_I_E_L_D";

  public static final int DIST_SEED = 1301011; // distribution seed

  /**
   * Interface for creating different forms of hash expression types.
   * @param <T>
   */
  public interface HashExpressionCreatorHelper<T> {
    T createCall(String funcName, List<T> inputFiled);
  }

  /**
   * Implementation of {@link HashExpressionCreatorHelper} for {@link LogicalExpression} type.
   */
  public static HashExpressionCreatorHelper<LogicalExpression> HASH_HELPER_LOGICAL_EXPRESSION =
      (funcName, inputFiled) -> new FunctionCall(funcName, inputFiled, ExpressionPosition.UNKNOWN);

  public static class RexNodeBasedHashExpressionCreatorHelper implements HashExpressionCreatorHelper<RexNode> {
    private final RexBuilder rexBuilder;

    public RexNodeBasedHashExpressionCreatorHelper(RexBuilder rexBuilder) {
      this.rexBuilder = rexBuilder;
    }

    @SuppressWarnings("deprecation")
    @Override
    public RexNode createCall(String funcName, List<RexNode> inputFields) {
      final DrillSqlOperator op =
              new DrillSqlOperator(funcName, inputFields.size(), true, false);
      return rexBuilder.makeCall(op, inputFields);
    }
  }

  // The hash32 functions actually use hash64 underneath.  The reason we want to call hash32 is that
  // the hash based operators make use of 4 bytes of hash value, not 8 bytes (for reduced memory use).
  private static final String HASH32_FUNCTION_NAME = "hash32";
  private static final String HASH32_DOUBLE_FUNCTION_NAME = "hash32AsDouble";
  private static final String HASH64_DOUBLE_FUNCTION_NAME = "hash64AsDouble";

  /**
   * Create hash based partition expression based on the given distribution fields.
   *
   * @param distFields Field list based on which the distribution partition expression is constructed.
   * @param helper Implementation of {@link HashExpressionCreatorHelper}
   *               which is used to create function expressions.
   * @param <T> Input and output expression type.
   *           Currently it could be either {@link RexNode} or {@link LogicalExpression}
   * @return
   */
  public static <T> T createHashBasedPartitionExpression(
      List<T> distFields,
      T seed,
      HashExpressionCreatorHelper<T> helper) {
    return createHashExpression(distFields, seed, helper, true /*for distribution always hash as double*/);
  }

  /**
   * Create hash expression based on the given input fields.
   *
   * @param inputExprs Expression list based on which the hash expression is constructed.
   * @param helper Implementation of {@link HashExpressionCreatorHelper}
   *               which is used to create function expressions.
   * @param hashAsDouble Whether to use the hash as double function or regular hash64 function.
   * @param <T> Input and output expression type.
   *           Currently it could be either {@link RexNode} or {@link LogicalExpression}
   * @return
   */
  public static <T> T createHashExpression(
      List<T> inputExprs,
      T seed,
      HashExpressionCreatorHelper<T> helper,
      boolean hashAsDouble) {

    assert inputExprs.size() > 0;

    final String functionName = hashAsDouble ? HASH32_DOUBLE_FUNCTION_NAME : HASH32_FUNCTION_NAME;

    T func = helper.createCall(functionName,  ImmutableList.of(inputExprs.get(0), seed));
    for (int i = 1; i<inputExprs.size(); i++) {
      func = helper.createCall(functionName, ImmutableList.of(inputExprs.get(i), func));
    }

    return func;
  }

  /**
   * Create hash expression based on the given input fields.
   *
   * @param inputExprs Expression list based on which the hash expression is constructed.
   * @param helper Implementation of {@link HashExpressionCreatorHelper}
   *               which is used to create function expressions.
   * @param hashAsDouble Whether to use the hash as double function or regular hash64 function.
   * @param <T> Input and output expression type.
   *           Currently it could be either {@link RexNode} or {@link LogicalExpression}
   * @return
   */
  public static <T> T createHash64Expression(
    List<T> inputExprs,
    T seed,
    HashExpressionCreatorHelper<T> helper,
    boolean hashAsDouble) {
    Preconditions.checkArgument(inputExprs.size() > 0);
    final String functionName = hashAsDouble ? HASH64_DOUBLE_FUNCTION_NAME : HASH32_FUNCTION_NAME;
    T func = helper.createCall(functionName,  ImmutableList.of(inputExprs.get(0), seed));
    for (int i = 1; i<inputExprs.size(); i++) {
      func = helper.createCall(functionName, ImmutableList.of(inputExprs.get(i), func));
    }
    return func;
  }

  /**
   * Creates hash expression for input field and seed.
   *
   * @param field field expression
   * @param seed seed expression
   * @param hashAsDouble whether to use the hash as double function or regular hash64 function
   * @return hash expression
   */
  public static LogicalExpression getHash64Expression(LogicalExpression field, LogicalExpression seed, boolean hashAsDouble) {
    return createHash64Expression(ImmutableList.of(field), seed, HASH_HELPER_LOGICAL_EXPRESSION, hashAsDouble);
  }

  /**
   * Creates hash expression for input field and seed.
   *
   * @param field field expression
   * @param seed seed expression
   * @param hashAsDouble whether to use the hash as double function or regular hash64 function
   * @return hash expression
   */
  public static LogicalExpression getHashExpression(LogicalExpression field, LogicalExpression seed, boolean hashAsDouble) {
    return createHashExpression(ImmutableList.of(field), seed, HASH_HELPER_LOGICAL_EXPRESSION, hashAsDouble);
  }

  /**
   * Create a distribution hash expression.
   *
   * @param fields Distribution fields
   * @param rowType Row type
   * @return distribution hash expression
   */
  public static LogicalExpression getHashExpression(List<DistributionField> fields, RelDataType rowType) {
    assert fields.size() > 0;

    List<String> childFields = rowType.getFieldNames();

    // If we already included a field with hash - no need to calculate hash further down
    if (childFields.contains(HASH_EXPR_NAME)) {
      return new FieldReference(HASH_EXPR_NAME);
    }

    List<LogicalExpression> expressions = new ArrayList<>();
    for (DistributionField field : fields) {
      if (field instanceof NamedDistributionField) {
        NamedDistributionField namedDistributionField = (NamedDistributionField) field;
        expressions.add(new FieldReference(namedDistributionField.getFieldName(), ExpressionPosition.UNKNOWN));
      } else {
        expressions.add(new FieldReference(childFields.get(field.getFieldId()), ExpressionPosition.UNKNOWN));
      }
    }

    LogicalExpression distSeed = ValueExpressions.getInt(DIST_SEED);
    return createHashBasedPartitionExpression(expressions, distSeed, HASH_HELPER_LOGICAL_EXPRESSION);
  }
}
