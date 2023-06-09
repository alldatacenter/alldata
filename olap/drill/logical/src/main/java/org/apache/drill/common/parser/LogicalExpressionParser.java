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
package org.apache.drill.common.parser;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.parser.ExprLexer;
import org.apache.drill.common.expression.parser.ExprParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper class for parsing logical expression.
 */
public class LogicalExpressionParser {
  private static final Logger logger = LoggerFactory.getLogger(LogicalExpressionParser.class);

  /**
   * Initializes logical expression lexer and parser, add error listener that converts all
   * syntax error into {@link org.apache.drill.common.exceptions.ExpressionParsingException}.
   * Parses given expression into logical expression instance.
   *
   * @param expr expression to be parsed
   * @return logical expression instance
   */
  public static LogicalExpression parse(String expr) {
    ExprLexer lexer = new ExprLexer(CharStreams.fromString(expr));
    lexer.removeErrorListeners(); // need to remove since default listener will output warning
    lexer.addErrorListener(ErrorListener.INSTANCE);
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    ExprParser parser = new ExprParser(tokens);
    parser.removeErrorListeners(); // need to remove since default listener will output warning
    parser.addErrorListener(ErrorListener.INSTANCE);
    ExprParser.ParseContext parseContext = parser.parse();
    logger.trace("Tokens: [{}]. Parsing details: [{}].", tokens.getText(), parseContext.toInfoString(parser));
    return parseContext.e;
  }
}
