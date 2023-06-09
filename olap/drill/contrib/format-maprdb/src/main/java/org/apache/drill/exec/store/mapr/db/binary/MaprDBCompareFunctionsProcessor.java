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
package org.apache.drill.exec.store.mapr.db.binary;

import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.ValueExpressions;
import org.apache.drill.exec.store.hbase.CompareFunctionsProcessor;
import org.apache.hadoop.hbase.util.Order;
import org.apache.hadoop.hbase.util.PositionedByteRange;
import org.apache.hadoop.hbase.util.SimplePositionedMutableByteRange;

class MaprDBCompareFunctionsProcessor extends CompareFunctionsProcessor {

  public MaprDBCompareFunctionsProcessor(String functionName) {
    super(functionName);
  }

  public static MaprDBCompareFunctionsProcessor createFunctionsProcessorInstance(FunctionCall call, boolean nullComparatorSupported) {
    String functionName = call.getName();
    MaprDBCompareFunctionsProcessor evaluator = new MaprDBCompareFunctionsProcessor(functionName);

    return createFunctionsProcessorInstanceInternal(call, nullComparatorSupported, evaluator);
  }

  @Override
  protected ByteBuf getByteBuf(LogicalExpression valueArg, String encodingType) {
    switch (encodingType) {
      case "UTF8_OB":
      case "UTF8_OBD":
        if (valueArg instanceof ValueExpressions.QuotedString) {
          int stringLen = ((ValueExpressions.QuotedString) valueArg).value.getBytes(Charsets.UTF_8).length;
          ByteBuf bb = newByteBuf(stringLen + 2, true);
          PositionedByteRange br = new SimplePositionedMutableByteRange(bb.array(), 0, stringLen + 2);
          if (encodingType.endsWith("_OBD")) {
            org.apache.hadoop.hbase.util.OrderedBytes.encodeString(br, ((ValueExpressions.QuotedString) valueArg).value,
                                                                  Order.DESCENDING);
            setSortOrderAscending(false);
          } else {
            org.apache.hadoop.hbase.util.OrderedBytes.encodeString(br, ((ValueExpressions.QuotedString) valueArg).value,
                                                                  Order.ASCENDING);
          }
          return bb;
        }
    }
    return null;
  }
}
