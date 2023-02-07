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
package org.apache.drill.exec.store.mongo.plan;

import org.apache.calcite.adapter.enumerable.RexImpTable;
import org.apache.calcite.adapter.enumerable.RexToLixTranslator;
import org.apache.calcite.adapter.java.JavaTypeFactory;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexVisitorImpl;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.exec.store.mongo.common.MongoOp;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonValue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Translator from {@link RexNode} to strings in MongoDB's expression language.
 */
class RexToMongoTranslator extends RexVisitorImpl<BsonValue> {

  private final JavaTypeFactory typeFactory;

  private final List<String> inFields;

  private static final Map<SqlOperator, String> MONGO_OPERATORS = ImmutableMap.<SqlOperator, String>builder()
    .put(SqlStdOperatorTable.DIVIDE, "$divide")
    .put(SqlStdOperatorTable.MULTIPLY, "$multiply")
    .put(SqlStdOperatorTable.ABS, "$abs")
    .put(SqlStdOperatorTable.ACOS, "$acos")
    .put(SqlStdOperatorTable.ASIN, "$asin")
    .put(SqlStdOperatorTable.ATAN, "$atan")
    .put(SqlStdOperatorTable.ATAN2, "$atan2")
    .put(SqlStdOperatorTable.CEIL, "$ceil")
    .put(SqlStdOperatorTable.CONCAT, "$concat")
    .put(SqlStdOperatorTable.COS, "$cos")
    .put(SqlStdOperatorTable.DAYOFMONTH, "$dayOfMonth")
    .put(SqlStdOperatorTable.WEEK, "$isoWeek")
    .put(SqlStdOperatorTable.YEAR, "$isoWeekYear")
    .put(SqlStdOperatorTable.DAYOFWEEK, "$isoDayOfWeek")
    .put(SqlStdOperatorTable.DAYOFYEAR, "$dayOfYear")
    .put(SqlStdOperatorTable.RADIANS, "$degreesToRadians")
    .put(SqlStdOperatorTable.DENSE_RANK, "$denseRank")
    .put(SqlStdOperatorTable.EXP, "$exp")
    .put(SqlStdOperatorTable.FLOOR, "$floor")
    .put(SqlStdOperatorTable.HOUR, "$hour")
    .put(SqlStdOperatorTable.LN, "$ln")
    .put(SqlStdOperatorTable.LOG10, "$log10")
    .put(SqlStdOperatorTable.MINUTE, "$minute")
    .put(SqlStdOperatorTable.MOD, "$mod")
    .put(SqlStdOperatorTable.MONTH, "$month")
    .put(SqlStdOperatorTable.POWER, "$pow")
    .put(SqlStdOperatorTable.DEGREES, "$radiansToDegrees")
    .put(SqlStdOperatorTable.RAND, "$rand")
    .put(SqlStdOperatorTable.REPLACE, "$replaceAll")
    .put(SqlStdOperatorTable.ROUND, "$round")
    .put(SqlStdOperatorTable.SECOND, "$second")
    .put(SqlStdOperatorTable.SIN, "$sin")
    .put(SqlStdOperatorTable.SQRT, "$sqrt")
    .put(SqlStdOperatorTable.SUBSTRING, "$substr")
    .put(SqlStdOperatorTable.PLUS, "$add")
    .put(SqlStdOperatorTable.MINUS, "$subtract")
    .put(SqlStdOperatorTable.TAN, "$tan")
    .put(SqlStdOperatorTable.TRIM, "trim")
    .put(SqlStdOperatorTable.TRUNCATE, "$trunc")
    .put(SqlStdOperatorTable.AND, MongoOp.AND.getCompareOp())
    .put(SqlStdOperatorTable.OR, MongoOp.OR.getCompareOp())
    .put(SqlStdOperatorTable.NOT, MongoOp.NOT.getCompareOp())
    .put(SqlStdOperatorTable.EQUALS, MongoOp.EQUAL.getCompareOp())
    .put(SqlStdOperatorTable.NOT_EQUALS, MongoOp.NOT_EQUAL.getCompareOp())
    .put(SqlStdOperatorTable.GREATER_THAN, MongoOp.GREATER.getCompareOp())
    .put(SqlStdOperatorTable.GREATER_THAN_OR_EQUAL, MongoOp.GREATER_OR_EQUAL.getCompareOp())
    .put(SqlStdOperatorTable.LESS_THAN, MongoOp.LESS.getCompareOp())
    .put(SqlStdOperatorTable.LESS_THAN_OR_EQUAL, MongoOp.LESS_OR_EQUAL.getCompareOp())
    .build();


  protected RexToMongoTranslator(JavaTypeFactory typeFactory,
      List<String> inFields) {
    super(true);
    this.typeFactory = typeFactory;
    this.inFields = inFields;
  }

  @Override
  public BsonValue visitLiteral(RexLiteral literal) {
    if (literal.getValue() == null) {
      return BsonNull.VALUE;
    }
    return BsonDocument.parse(String.format("{$literal: %s}",
        RexToLixTranslator.translateLiteral(literal, literal.getType(),
            typeFactory, RexImpTable.NullAs.NOT_POSSIBLE)));
  }

  @Override
  public BsonValue visitInputRef(RexInputRef inputRef) {
    return new BsonString("$" + inFields.get(inputRef.getIndex()));
  }

  @Override
  public BsonValue visitCall(RexCall call) {
    String name = isItem(call);
    if (name != null) {
      return new BsonString("'$" + name + "'");
    }
    List<BsonValue> strings = call.operands.stream()
        .map(operand -> operand.accept(this))
        .collect(Collectors.toList());

    if (call.getKind() == SqlKind.CAST) {
      return strings.get(0);
    }
    SqlOperator sqlOperator = call.getOperator();
    String stdOperator = MONGO_OPERATORS.get(sqlOperator);
    if (stdOperator != null) {
      return new BsonDocument(stdOperator, new BsonArray(strings));
    }
    if (sqlOperator == SqlStdOperatorTable.ITEM) {
      RexNode op1 = call.operands.get(1);
      if (op1 instanceof RexLiteral) {
        if (op1.getType().getSqlTypeName() == SqlTypeName.INTEGER) {
          return new BsonDocument("$arrayElemAt", new BsonArray(
              Arrays.asList(strings.get(0), new BsonInt32(((RexLiteral) op1).getValueAs(Integer.class)))));
        } else if (op1.getType().getSqlTypeName() == SqlTypeName.CHAR) {
          return new BsonString(strings.get(0).asString().getValue() + "." + ((RexLiteral) op1).getValueAs(String.class));
        }
      }
    }
    if (sqlOperator == SqlStdOperatorTable.CASE) {
      // case(a, b, c)  -> $cond:[a, b, c]
      // case(a, b, c, d) -> $cond:[a, b, $cond:[c, d, null]]
      // case(a, b, c, d, e) -> $cond:[a, b, $cond:[c, d, e]]
      BsonDocument result = new BsonDocument();
      BsonArray args = new BsonArray();
      result.put("$cond", args);
      for (int i = 0; i < strings.size(); i += 2) {
        args.add(strings.get(i));
        args.add(strings.get(i + 1));
        if (i == strings.size() - 3) {
          args.add(strings.get(i + 2));
          break;
        }
        if (i == strings.size() - 2) {
          args.add(BsonNull.VALUE);
          break;
        }
        BsonArray innerArgs = new BsonArray();
        BsonDocument innerDocument = new BsonDocument();
        innerDocument.put("$cond", innerArgs);
        args.add(innerDocument);
        args = innerArgs;
      }
      return result;
    }
    if (sqlOperator == SqlStdOperatorTable.IS_NULL) {
      BsonDocument result = new BsonDocument();
      BsonArray args = new BsonArray();
      args.add(strings.get(0));
      args.add(BsonNull.VALUE);
      // Perf: the $eq operator can make use of indexes in Mongo
      result.put(MongoOp.EQUAL.getCompareOp(), args);
      return result;
    }
    if (sqlOperator == SqlStdOperatorTable.IS_NOT_NULL) {
      BsonDocument result = new BsonDocument();
      BsonArray args = new BsonArray();
      args.add(strings.get(0));
      args.add(BsonNull.VALUE);
      // Perf: the $ne operator can make use of indexes in Mongo
      result.put(MongoOp.NOT_EQUAL.getCompareOp(), args);
      return result;
    }
    throw new IllegalArgumentException("Translation of " + call + " is not supported by MongoProject");
  }


  /**
   * Returns 'string' if it is a call to item['string'], null otherwise.
   */
  public static String isItem(RexCall call) {
    if (call.getOperator() != SqlStdOperatorTable.ITEM) {
      return null;
    }
    RexNode op0 = call.operands.get(0);
    RexNode op1 = call.operands.get(1);
    if (op0 instanceof RexInputRef
        && ((RexInputRef) op0).getIndex() == 0
        && op1 instanceof RexLiteral
        && ((RexLiteral) op1).getValue2() instanceof String) {
      return (String) ((RexLiteral) op1).getValue2();
    }
    return null;
  }

  public static boolean supportsExpression(RexNode expr) {
    return expr.accept(new RexMongoChecker());
  }

  private static class RexMongoChecker extends RexVisitorImpl<Boolean> {

    protected RexMongoChecker() {
      super(true);
    }

    @Override
    public Boolean visitLiteral(RexLiteral literal) {
      return true;
    }

    @Override
    public Boolean visitInputRef(RexInputRef inputRef) {
      return inputRef.getType().getSqlTypeName() != SqlTypeName.DYNAMIC_STAR;
    }

    @Override
    public Boolean visitCall(RexCall call) {
      if (isItem(call) != null
        || call.getKind() == SqlKind.CAST
        || call.getOperator() == SqlStdOperatorTable.CASE
        || MONGO_OPERATORS.get(call.getOperator()) != null) {
        return true;
      }

      if (call.getOperator() == SqlStdOperatorTable.ITEM) {
        RexNode op = call.operands.get(1);
        return op instanceof RexLiteral
          && (op.getType().getSqlTypeName() == SqlTypeName.INTEGER
          || op.getType().getSqlTypeName() == SqlTypeName.CHAR);
      }

      return false;
    }
  }
}
