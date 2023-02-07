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
package org.apache.drill.common.expression;

import java.util.Collections;
import java.util.Iterator;

import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.Types;

public class ConvertExpression extends LogicalExpressionBase implements Iterable<LogicalExpression>{

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ConvertExpression.class);

  public static final String CONVERT_FROM = "convert_from";
  public static final String CONVERT_TO = "convert_to";

  private final LogicalExpression input;
  private final MajorType type;
  private final String convertFunction;
  private final String encodingType;

  /**
   * @param encodingType
   * @param convertFunction
   * @param input
   * @param pos
   */
  public ConvertExpression(String convertFunction, String encodingType, LogicalExpression input, ExpressionPosition pos) {
    super(pos);
    this.input = input;
    this.convertFunction = CONVERT_FROM.equals(convertFunction.toLowerCase()) ? CONVERT_FROM : CONVERT_TO;
    this.encodingType = encodingType.toUpperCase();
    this.type = Types.getMajorTypeFromName(encodingType.split("_", 2)[0].toLowerCase());
  }

  @Override
  public <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E {
    return visitor.visitConvertExpression(this, value);
  }

  @Override
  public Iterator<LogicalExpression> iterator() {
    return Collections.singleton(input).iterator();
  }

  public String getConvertFunction() {
    return convertFunction;
  }

  public LogicalExpression getInput() {
    return input;
  }

  @Override
  public MajorType getMajorType() {
    return type;
  }

  public String getEncodingType() {
    return encodingType;
  }

  @Override
  public String toString() {
    return "ConvertExpression [input=" + input + ", type=" + Types.toString(type) + ", convertFunction="
        + convertFunction + ", conversionType=" + encodingType + "]";
  }
}
