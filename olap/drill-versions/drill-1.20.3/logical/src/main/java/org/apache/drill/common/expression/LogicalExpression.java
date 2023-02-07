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

import java.io.IOException;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.expression.visitors.ExprVisitor;
import org.apache.drill.common.parser.LogicalExpressionParser;
import org.apache.drill.common.types.TypeProtos.MajorType;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@JsonSerialize(using = LogicalExpression.Se.class)
public interface LogicalExpression extends Iterable<LogicalExpression> {

  MajorType getMajorType();

  <T, V, E extends Exception> T accept(ExprVisitor<T, V, E> visitor, V value) throws E;

  ExpressionPosition getPosition();

  int getSelfCost();
  int getCumulativeCost();

  @SuppressWarnings("serial")
  class De extends StdDeserializer<LogicalExpression> {
    DrillConfig config;

    public De(DrillConfig config) {
      super(LogicalExpression.class);
      this.config = config;
    }

    @Override
    public LogicalExpression deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
      String expr = jp.getText();

      if (expr == null || expr.isEmpty()) {
        return null;
      }

      return LogicalExpressionParser.parse(expr);
    }
  }

  @SuppressWarnings("serial")
  class Se extends StdSerializer<LogicalExpression> {

    protected Se() {
      super(LogicalExpression.class);
    }

    @Override
    public void serialize(LogicalExpression value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      StringBuilder sb = new StringBuilder();
      ExpressionStringBuilder esb = new ExpressionStringBuilder();
      value.accept(esb, sb);
      jgen.writeString(sb.toString());
    }
  }
}
