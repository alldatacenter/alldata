/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.api.predicate;

import java.util.Collection;

import org.apache.ambari.server.controller.spi.Predicate;

/**
 * Compiler which takes a query expression as input and produces a predicate instance as output.
 */
public class PredicateCompiler {

  /**
   * Lexer instance used to translate expressions into stream of tokens.
   */
  private QueryLexer lexer = new QueryLexer();

  /**
   * Parser instance used to produce a predicate instance from a stream of tokens.
   */
  private QueryParser parser = new QueryParser();

  /**
   * Generate a predicate from a query expression.
   *
   * @param exp  query expression
   *
   * @return a predicate instance
   * @throws InvalidQueryException if unable to compile the expression
   */
  public Predicate compile(String exp) throws InvalidQueryException {
    return parser.parse(lexer.tokens(exp));
  }

  /**
   * Generate a predicate from a query expression.
   *
   * @param exp               query expression
   * @param ignoredProperties  collection of property names to ignore
   *
   * @return a predicate instance
   * @throws InvalidQueryException if unable to compile the expression
   */
  public Predicate compile(String exp, Collection<String> ignoredProperties) throws InvalidQueryException {
    return parser.parse(lexer.tokens(exp, ignoredProperties));
  }
}
