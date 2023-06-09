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
package org.apache.drill.exec.store.mapr.db.json;

import org.apache.commons.codec.binary.Base64;

import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.ValueExpressions.IntExpression;
import org.apache.drill.common.expression.ValueExpressions.LongExpression;
import org.apache.drill.common.expression.ValueExpressions.QuotedString;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.ojai.Value;
import org.ojai.store.QueryCondition;

import com.mapr.db.impl.ConditionImpl;
import com.mapr.db.impl.MapRDBImpl;

import java.nio.ByteBuffer;

class OjaiFunctionsProcessor extends AbstractExprVisitor<Void, Void, RuntimeException> {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(OjaiFunctionsProcessor.class);
  private QueryCondition queryCond;

  private OjaiFunctionsProcessor() {
  }

  private static String getStackTrace() {
    final Throwable throwable = new Throwable();
    final StackTraceElement[] ste = throwable.getStackTrace();
    final StringBuilder sb = new StringBuilder();
    for (int i = 1; i < ste.length; ++i) {
      sb.append(ste[i].toString());
      sb.append('\n');
    }

    return sb.toString();
  }

  @Override
  public Void visitUnknown(LogicalExpression e, Void valueArg) throws RuntimeException {
    logger.debug("visitUnknown() e class " + e.getClass());
    logger.debug(getStackTrace());
    return null;
  }

  private static class Ref<T> {
    T value;
  }

  private static SchemaPath getSchemaPathArg(LogicalExpression expr) {
    final Ref<SchemaPath> ref = new Ref<>();
    expr.accept(new OjaiFunctionsProcessor() {
        @Override
        public Void visitSchemaPath(SchemaPath e, Void v) {
          ref.value = e;
          return null;
        }
     }, null);

      return ref.value;
  }

  private static String getStringArg(LogicalExpression expr) {
    final Ref<QuotedString> ref = new Ref<>();
    expr.accept(new OjaiFunctionsProcessor() {
        @Override
        public Void visitQuotedStringConstant(QuotedString e, Void v) {
          ref.value = e;
          return null;
        }
     }, null);

      return ref.value != null ? ref.value.getString() : null;
  }

  private static int getIntArg(LogicalExpression expr) {
    final Ref<Integer> ref = new Ref<>();
    expr.accept(new OjaiFunctionsProcessor() {
        @Override
        public Void visitIntConstant(IntExpression e, Void v) {
          ref.value = new Integer(e.getInt());
          return null;
        }
     }, null);

      return ref.value != null ? ref.value.intValue() : 0;
  }

  private static long getLongArg(LogicalExpression expr) {
    final Ref<Long> ref = new Ref<>();
    expr.accept(new OjaiFunctionsProcessor() {
        @Override
        public Void visitIntConstant(IntExpression e, Void v) {
          ref.value = new Long(e.getInt());
          return null;
        }

        @Override
        public Void visitLongConstant(LongExpression e, Void v) {
          ref.value = e.getLong();
          return null;
        }
     }, null);

      return ref.value != null ? ref.value.longValue() : 0;
  }

  private final static ImmutableMap<String, QueryCondition.Op> STRING_TO_RELOP;
  static {
    ImmutableMap.Builder<String, QueryCondition.Op> builder = ImmutableMap.builder();
    STRING_TO_RELOP = builder
        .put("=", QueryCondition.Op.EQUAL)
        .put("<>", QueryCondition.Op.NOT_EQUAL)
        .put("<", QueryCondition.Op.LESS)
        .put("<=", QueryCondition.Op.LESS_OR_EQUAL)
        .put(">", QueryCondition.Op.GREATER)
        .put(">=", QueryCondition.Op.GREATER_OR_EQUAL)
        .build();
  }

  @Override
  public Void visitFunctionCall(FunctionCall call, Void v) throws RuntimeException {
    final String functionName = call.getName();
    final String fieldName = FieldPathHelper.schemaPath2FieldPath(getSchemaPathArg(call.arg(0))).asPathString();
    switch(functionName) {
    case "ojai_sizeof": {
      // ojai_sizeof(field, "<rel-op>", <int-value>)
      final String relOp = getStringArg(call.arg(1));
      final long size = getLongArg(call.arg(2));
      queryCond = MapRDBImpl.newCondition()
          .sizeOf(fieldName, STRING_TO_RELOP.get(relOp), size)
          .build();
      break;
    }

    case "ojai_typeof":
    case "ojai_nottypeof": {
      // ojai_[not]typeof(field, <type-code>);
      final int typeCode = getIntArg(call.arg(1));
      final Value.Type typeValue = Value.Type.valueOf(typeCode);
      queryCond = MapRDBImpl.newCondition();
      if (functionName.equals("ojai_typeof")) {
        queryCond.typeOf(fieldName, typeValue);
      } else {
        queryCond.notTypeOf(fieldName, typeValue);
      }
      queryCond.build();
      break;
    }

    case "ojai_matches":
    case "ojai_notmatches": {
      // ojai_[not]matches(field, <regex>);
      final String regex = getStringArg(call.arg(1));
      if (functionName.equals("ojai_matches")) {
        queryCond = MapRDBImpl.newCondition()
            .matches(fieldName, regex);
      } else {
        queryCond = MapRDBImpl.newCondition()
            .notMatches(fieldName, regex);
      }
      queryCond.build();
      break;
    }

    case "ojai_condition": {
      // ojai_condition(field, <serialized-condition>);
       final String condString = getStringArg(call.arg(1));
      final byte[] condBytes = Base64.decodeBase64(condString);
      final ByteBuffer condBuffer = ByteBuffer.wrap(condBytes);
      queryCond = ConditionImpl.parseFrom(condBuffer);
      break;
    }

    default:
      throw new IllegalArgumentException("unrecognized functionName " + functionName);
    } // switch(functionName)

    return null;
  }

  public static OjaiFunctionsProcessor process(FunctionCall call) {
    final OjaiFunctionsProcessor processor = new OjaiFunctionsProcessor();

    call.accept(processor, null);
    return processor;
  }

  public QueryCondition getCondition() {
    return queryCond;
  }

}
