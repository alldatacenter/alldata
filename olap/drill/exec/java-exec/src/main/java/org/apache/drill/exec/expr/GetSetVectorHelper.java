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
package org.apache.drill.exec.expr;

import io.netty.buffer.DrillBuf;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.ClassGenerator.HoldingContainer;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JVar;

public class GetSetVectorHelper {

  /**
   * Generates the code to read a vector into a holder. Example: <pre><code>
   *   NullableBigIntHolder out3 = new NullableBigIntHolder();
   *   {
   *    out3 .isSet = vv0 [((leftIndex)>>> 16)].getAccessor().isSet(((leftIndex)& 65535));
   *    if (out3 .isSet == 1) {
   *      out3 .value = vv0 [((leftIndex)>>> 16)].getAccessor().get(((leftIndex)& 65535));
   *    }
   *  }</code></pre>
   */
  public static void read(MajorType type, JExpression vector, JBlock eval, HoldingContainer out, JCodeModel model,
      JExpression indexVariable) {

    JInvocation getValueAccessor = vector.invoke("getAccessor");

    switch(type.getMode()){
      case OPTIONAL:
        eval.assign(out.getIsSet(), getValueAccessor.invoke("isSet").arg(indexVariable));
        eval = eval._if(out.getIsSet().eq(JExpr.lit(1)))._then();
        // fall through

      case REQUIRED:
        switch (type.getMinorType()) {
          case BIGINT:
          case FLOAT4:
          case FLOAT8:
          case INT:
          case MONEY:
          case SMALLINT:
          case TINYINT:
          case UINT1:
          case UINT2:
          case UINT4:
          case UINT8:
          case INTERVALYEAR:
          case DATE:
          case TIME:
          case TIMESTAMP:
          case BIT:
            eval.assign(out.getValue(), getValueAccessor.invoke("get").arg(indexVariable));
            return;
          case DECIMAL9:
          case DECIMAL18:
            eval.assign(out.getHolder().ref("scale"), vector.invoke("getField").invoke("getScale"));
            eval.assign(out.getHolder().ref("precision"), vector.invoke("getField").invoke("getPrecision"));
            eval.assign(out.getValue(), getValueAccessor.invoke("get").arg(indexVariable));
            return;
          case DECIMAL28DENSE:
          case DECIMAL28SPARSE:
          case DECIMAL38DENSE:
          case DECIMAL38SPARSE:
            eval.assign(out.getHolder().ref("scale"), vector.invoke("getField").invoke("getScale"));
            eval.assign(out.getHolder().ref("precision"), vector.invoke("getField").invoke("getPrecision"));
            eval.assign(out.getHolder().ref("start"), JExpr.lit(TypeHelper.getSize(type)).mul(indexVariable));
            eval.assign(out.getHolder().ref("buffer"), vector.invoke("getBuffer"));
            return;
          case VARDECIMAL: {
            eval.assign(out.getHolder().ref("buffer"), vector.invoke("getBuffer"));
            JVar se = eval.decl(model.LONG, "startEnd", getValueAccessor.invoke("getStartEnd").arg(indexVariable));
            eval.assign(out.getHolder().ref("start"), JExpr.cast(model._ref(int.class), se));
            eval.assign(out.getHolder().ref("end"), JExpr.cast(model._ref(int.class), se.shr(JExpr.lit(32))));
            eval.assign(out.getHolder().ref("scale"), vector.invoke("getField").invoke("getScale"));
            eval.assign(out.getHolder().ref("precision"), vector.invoke("getField").invoke("getPrecision"));
            return;
          }
          case INTERVAL: {
            JVar start = eval.decl(model.INT, "start", JExpr.lit(TypeHelper.getSize(type)).mul(indexVariable));
            JVar data = eval.decl(model.ref(DrillBuf.class), "data", vector.invoke("getBuffer"));
            eval.assign(out.getHolder().ref("months"), data.invoke("getInt").arg(start));
            eval.assign(out.getHolder().ref("days"), data.invoke("getInt").arg(start.plus(JExpr.lit(4))));
            eval.assign(out.getHolder().ref("milliseconds"), data.invoke("getInt").arg(start.plus(JExpr.lit(8))));
            return;
          }
          case INTERVALDAY: {
            JVar start = eval.decl(model.INT, "start", JExpr.lit(TypeHelper.getSize(type)).mul(indexVariable));
            eval.assign(out.getHolder().ref("days"), vector.invoke("getBuffer").invoke("getInt").arg(start));
            eval.assign(out.getHolder().ref("milliseconds"), vector.invoke("getBuffer").invoke("getInt").arg(start.plus(JExpr.lit(4))));
            return;
          }
          case VAR16CHAR:
          case VARBINARY:
          case VARCHAR: {
             eval.assign(out.getHolder().ref("buffer"), vector.invoke("getBuffer"));
             JVar se = eval.decl(model.LONG, "startEnd", getValueAccessor.invoke("getStartEnd").arg(indexVariable));
             eval.assign(out.getHolder().ref("start"), JExpr.cast(model._ref(int.class), se));
             eval.assign(out.getHolder().ref("end"), JExpr.cast(model._ref(int.class), se.shr(JExpr.lit(32))));
            return;
          }
          default:
        }
      default:
    }

    // fallback.
    eval.add(getValueAccessor.invoke("get").arg(indexVariable).arg(out.getHolder()));
  }

  public static JInvocation write(MajorType type, JVar vector, HoldingContainer in, JExpression indexVariable, String setMethodName) {

    JInvocation setMethod = vector.invoke("getMutator").invoke(setMethodName).arg(indexVariable);

    if (type.getMinorType() == MinorType.UNION) {
      return setMethod.arg(in.getHolder());
    }
    switch(type.getMode()){
      case OPTIONAL:
        setMethod = setMethod.arg(in.f("isSet"));
        // Fall through

      case REQUIRED:
        switch (type.getMinorType()) {
          case BIGINT:
          case FLOAT4:
          case FLOAT8:
          case INT:
          case MONEY:
          case SMALLINT:
          case TINYINT:
          case UINT1:
          case UINT2:
          case UINT4:
          case UINT8:
          case INTERVALYEAR:
          case DATE:
          case TIME:
          case TIMESTAMP:
          case BIT:
          case DECIMAL9:
          case DECIMAL18:
            return setMethod
                .arg(in.getValue());
          case DECIMAL28DENSE:
          case DECIMAL28SPARSE:
          case DECIMAL38DENSE:
          case DECIMAL38SPARSE:
            return setMethod
                .arg(in.f("start"))
                .arg(in.f("buffer"));
          case INTERVAL:{
            return setMethod
                .arg(in.f("months"))
                .arg(in.f("days"))
                .arg(in.f("milliseconds"));
          }
          case INTERVALDAY: {
            return setMethod
                .arg(in.f("days"))
                .arg(in.f("milliseconds"));
          }
          case VAR16CHAR:
          case VARBINARY:
          case VARCHAR:
          case VARDECIMAL:
            return setMethod
                .arg(in.f("start"))
                .arg(in.f("end"))
                .arg(in.f("buffer"));
          default:
        }
      default:
    }

    return setMethod.arg(in.getHolder());
  }
}
