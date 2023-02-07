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
 * Represents an empty array: the case where the parser has seen only
 * {@code []}, but no array elements which would indicate the type.
 * Resolves to a specific type upon
 * presentation of the first element. If that element is
 * {@code null}, we must still choose a type to record nulls.
 * <p>
 * This array listener holds no element since none has been
 * created yet; we use this only while we see empty arrays.
 */
public abstract class EmptyArrayParser extends AbstractElementParser {

  protected final String key;

  public EmptyArrayParser(JsonStructureParser structParser, String key) {
    super(structParser);
    this.key = key;
  }

  @Override
  public void parse(TokenIterator tokenizer) {
    JsonToken token1 = tokenizer.requireNext();

    // Ignore null: treat as an empty array.
    if (token1 == JsonToken.VALUE_NULL) {
      return;
    }

    // Assume an scalar is a one-item array.
    if (token1 != JsonToken.START_ARRAY) {
      tokenizer.unget(token1);
      resolve(tokenizer).parse(tokenizer);

      // This parser never called again
      return;
    }

    // Ignore an empty array, resolve a non-empty array.
    // Must be done even if the array value is null so elements
    // can be counted.
    JsonToken token2 = tokenizer.requireNext();
    if (token2 != JsonToken.END_ARRAY) {

      // Saw the first actual element. Swap out this parser for a
      // real field parser, then let that parser parse from here.
      // The real parser is for the entire field, so we unget the
      // opening bracket as well as the element.
      tokenizer.unget(token2);
      tokenizer.unget(token1);
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
