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
package org.apache.drill.exec.store.mongo;

import org.apache.drill.common.FunctionNames;
import org.apache.drill.common.expression.BooleanOperator;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.exec.store.mongo.common.MongoOp;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MongoFilterBuilder extends
    AbstractExprVisitor<Document, Void, RuntimeException> implements
    DrillMongoConstants {

  private static final Logger logger = LoggerFactory.getLogger(MongoFilterBuilder.class);

  private final LogicalExpression le;
  private boolean allExpressionsConverted = true;

  public MongoFilterBuilder(LogicalExpression conditionExp) {
    this.le = conditionExp;
  }

  public Document parseTree() {
    return le.accept(this, null);
  }

  private Document mergeFilters(String functionName,
      Document left, Document right) {
    Document newFilter = new Document();

    switch (functionName) {
    case FunctionNames.AND:
      if (left != null && right != null) {
        newFilter = MongoUtils.andFilterAtIndex(left, right);
      } else if (left != null) {
        newFilter = left;
      } else {
        newFilter = right;
      }
      break;
    case FunctionNames.OR:
      newFilter = MongoUtils.orFilterAtIndex(left, right);
    }
    return newFilter;
  }

  public boolean isAllExpressionsConverted() {
    return allExpressionsConverted;
  }

  @Override
  public Document visitUnknown(LogicalExpression e, Void value)
      throws RuntimeException {
    allExpressionsConverted = false;
    return null;
  }

  @Override
  public Document visitBooleanOperator(BooleanOperator op, Void value) {
    List<LogicalExpression> args = op.args();
    Document condition = null;
    String functionName = op.getName();
    for (LogicalExpression arg : args) {
      switch (functionName) {
        case FunctionNames.AND:
        case FunctionNames.OR:
          if (condition == null) {
            condition = arg.accept(this, null);
          } else {
            Document scanSpec = arg.accept(this, null);
            if (scanSpec != null) {
              condition = mergeFilters(functionName, condition, scanSpec);
            } else {
              allExpressionsConverted = false;
            }
          }
          break;
      }
    }
    return condition;
  }

  @Override
  public Document visitFunctionCall(FunctionCall call, Void value)
      throws RuntimeException {
    Document functionCall = null;
    String functionName = call.getName();
    List<LogicalExpression> args = call.args();

    if (MongoCompareFunctionProcessor.isCompareFunction(functionName)) {
      MongoCompareFunctionProcessor processor = MongoCompareFunctionProcessor
          .process(call);
      if (processor.isSuccess()) {
        try {
          functionCall = createFunctionCall(processor.getFunctionName(),
              processor.getPath(), processor.getValue());
        } catch (Exception e) {
          logger.error(" Failed to creare Filter ", e);
          // throw new RuntimeException(e.getMessage(), e);
        }
      }
    } else {
      switch (functionName) {
      case FunctionNames.AND:
      case FunctionNames.OR:
        Document left = args.get(0).accept(this, null);
        Document right = args.get(1).accept(this, null);
        if (left != null && right != null) {
          functionCall = mergeFilters(functionName, left, right);
        } else {
          allExpressionsConverted = false;
          if (FunctionNames.AND.equals(functionName)) {
            functionCall = left == null ? right : left;
          }
        }
        break;
      }
    }

    if (functionCall == null) {
      allExpressionsConverted = false;
    }

    return functionCall;
  }

  private Document createFunctionCall(String functionName,
      SchemaPath field, Object fieldValue) {
    // extract the field name
    String fieldName = field.getRootSegmentPath();
    MongoOp compareOp = null;
    switch (functionName) {
    case FunctionNames.EQ:
      compareOp = MongoOp.EQUAL;
      break;
    case FunctionNames.NE:
      compareOp = MongoOp.NOT_EQUAL;
      break;
    case FunctionNames.GE:
      compareOp = MongoOp.GREATER_OR_EQUAL;
      break;
    case FunctionNames.GT:
      compareOp = MongoOp.GREATER;
      break;
    case FunctionNames.LE:
      compareOp = MongoOp.LESS_OR_EQUAL;
      break;
    case FunctionNames.LT:
      compareOp = MongoOp.LESS;
      break;
    case FunctionNames.IS_NULL:
    case "isNull":
    case "is null":
      compareOp = MongoOp.IFNULL;
      break;
    case FunctionNames.IS_NOT_NULL:
    case "isNotNull":
    case "is not null":
      compareOp = MongoOp.IFNOTNULL;
      break;
    }

    if (compareOp != null) {
      Document queryFilter = new Document();
      if (compareOp == MongoOp.IFNULL) {
        queryFilter.put(fieldName,
            new Document(MongoOp.EQUAL.getCompareOp(), null));
      } else if (compareOp == MongoOp.IFNOTNULL) {
        queryFilter.put(fieldName,
            new Document(MongoOp.NOT_EQUAL.getCompareOp(), null));
      } else {
        queryFilter.put(fieldName, new Document(compareOp.getCompareOp(),
            fieldValue));
      }
      return queryFilter;
    }
    return null;
  }
}
