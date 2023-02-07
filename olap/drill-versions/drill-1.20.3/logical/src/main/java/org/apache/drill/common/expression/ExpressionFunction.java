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

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.List;

import org.apache.drill.common.expression.visitors.ExpressionValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.shaded.guava.com.google.common.reflect.TypeToken;

@SuppressWarnings("unchecked")
public class ExpressionFunction {
  final static Logger logger = LoggerFactory.getLogger(ExpressionFunction.class);

  @SuppressWarnings("serial")
  final static Type EXPR_LIST = (new TypeToken<List<LogicalExpression>>() {
  }).getType();

  private static final Class<?>[] FUNCTIONS = {};

  private static final ImmutableMap<String, Constructor<LogicalExpression>> FUNCTION_MAP;

  static {
    ImmutableMap.Builder<String, Constructor<LogicalExpression>> builder = ImmutableMap.builder();
    for (Class<?> c : FUNCTIONS) {

      // logger.debug("Adding {} and function", c);
      if (!LogicalExpression.class.isAssignableFrom(c)) {
        logger.error(
            "The provided Class [{}] does not derive from LogicalExpression.  Skipping inclusion in registry.", c);
        continue;
      }
      FunctionName fn = c.getAnnotation(FunctionName.class);
      if (fn == null) {
        logger.error(
            "The provided Class [{}] did not have a FunctionName annotation.  Skipping inclusion in registry.", c);
        continue;
      }
      String name = fn.value();
      try {
        Constructor<LogicalExpression> m = (Constructor<LogicalExpression>) c.getConstructor(List.class);
        if (!EXPR_LIST.equals(m.getGenericParameterTypes()[0])) {
          logger
              .error(
                  "The constructor for each function must have a argument list that only contains a List<LogicalExpression>.  The class[{}] has an inccorect List<{}> argument.",
                  c, m.getGenericParameterTypes()[0]);
          continue;
        }

        builder.put(name, m);
      } catch (Exception e) {
        logger
            .error(
                "Failure while attempting to retrieve Logical Expression list constructor on class [{}].  Functions must have one of these.",
                c, e);
      }
    }

    FUNCTION_MAP = builder.build();
  }

  public static LogicalExpression create(String functionName, List<LogicalExpression> expressions)
      throws ExpressionValidationException {
    // logger.debug("Requesting generation of new function with name {}.",
    // functionName);
    if (!FUNCTION_MAP.containsKey(functionName)) {
      throw new ExpressionValidationException(String.format("Unknown function with name '%s'", functionName));
    }
    try {
      return FUNCTION_MAP.get(functionName).newInstance(expressions);
    } catch (Exception e) {
      throw new ExpressionValidationException("Failure while attempting to build type of " + functionName, e);
    }
  }

}
