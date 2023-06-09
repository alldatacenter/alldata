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
package org.apache.drill.exec.store.easy.json.parser;

import com.fasterxml.jackson.core.JsonToken;

/**
 * Parses nulls. On the first non-null token, replaces itself with
 * a "resolved" parser to handle the actual structure.
 */
public abstract class NullValueParser extends AbstractElementParser {

  protected final String key;

  public NullValueParser(JsonStructureParser structParser, String key) {
    super(structParser);
    this.key = key;
  }

  /**
   * Parses nulls. On the first non-null
   * Parses <code>true | false | null | integer | float | string|
   *              embedded-object | { ... } | [ ... ]</code>
   */
  @Override
  public void parse(TokenIterator tokenizer) {
    JsonToken token = tokenizer.requireNext();
    if (token != JsonToken.VALUE_NULL) {
      tokenizer.unget(token);
      resolve(tokenizer).parse(tokenizer);

      // This parser never called again
    }
  }

  /**
   * Replace this parser with a new parser based on the current
   * parse context.
   */
  protected abstract ElementParser resolve(TokenIterator tokenizer);
}
