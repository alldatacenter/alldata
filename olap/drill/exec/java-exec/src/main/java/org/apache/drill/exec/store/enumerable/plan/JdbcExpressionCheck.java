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
package org.apache.drill.exec.store.enumerable.plan;

import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexCorrelVariable;
import org.apache.calcite.rex.RexDynamicParam;
import org.apache.calcite.rex.RexFieldAccess;
import org.apache.calcite.rex.RexFieldCollation;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexLocalRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexOver;
import org.apache.calcite.rex.RexPatternFieldRef;
import org.apache.calcite.rex.RexRangeRef;
import org.apache.calcite.rex.RexSubQuery;
import org.apache.calcite.rex.RexTableInputRef;
import org.apache.calcite.rex.RexVisitor;
import org.apache.calcite.rex.RexWindow;
import org.apache.drill.exec.planner.sql.DrillSqlOperator;

/**
 * Visitor class that determines whether or not a particular RexNode expression tree contains only standard expressions.
 * If RexNode tree contains Drill specific expressions, the tree will respond with false.
 */
class JdbcExpressionCheck implements RexVisitor<Boolean> {

  private static final JdbcExpressionCheck INSTANCE = new JdbcExpressionCheck();

  static boolean isOnlyStandardExpressions(RexNode rex) {
    return rex.accept(INSTANCE);
  }

  @Override
  public Boolean visitInputRef(RexInputRef paramRexInputRef) {
    return true;
  }

  @Override
  public Boolean visitLocalRef(RexLocalRef paramRexLocalRef) {
    return true;
  }

  @Override
  public Boolean visitLiteral(RexLiteral paramRexLiteral) {
    return true;
  }

  @Override
  public Boolean visitCall(RexCall paramRexCall) {
    if (paramRexCall.getOperator() instanceof DrillSqlOperator) {
      return false;
    } else {
      for (RexNode operand : paramRexCall.operands) {
        if (!operand.accept(this)) {
          return false;
        }
      }
    }
    return true;
  }

  public Boolean visitOver(RexOver over) {
    if (!visitCall(over)) {
      return false;
    }

    RexWindow window = over.getWindow();
    for (RexFieldCollation orderKey : window.orderKeys) {
      if (!orderKey.left.accept(this)) {
        return false;
      }
    }

    for (RexNode partitionKey : window.partitionKeys) {
      if (!partitionKey.accept(this)) {
        return false;
      }
    }

    return true;

  }

  @Override
  public Boolean visitCorrelVariable(RexCorrelVariable paramRexCorrelVariable) {
    return true;
  }

  @Override
  public Boolean visitDynamicParam(RexDynamicParam paramRexDynamicParam) {
    return true;
  }

  @Override
  public Boolean visitRangeRef(RexRangeRef paramRexRangeRef) {
    return true;
  }

  @Override
  public Boolean visitFieldAccess(RexFieldAccess paramRexFieldAccess) {
    return paramRexFieldAccess.getReferenceExpr().accept(this);
  }

  @Override
  public Boolean visitSubQuery(RexSubQuery subQuery) {
    return null;
  }

  @Override
  public Boolean visitTableInputRef(RexTableInputRef fieldRef) {
    return false;
  }

  @Override
  public Boolean visitPatternFieldRef(RexPatternFieldRef fieldRef) {
    return false;
  }
}
