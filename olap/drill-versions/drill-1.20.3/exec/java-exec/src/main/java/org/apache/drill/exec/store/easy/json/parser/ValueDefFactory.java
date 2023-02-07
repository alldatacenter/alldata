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

import org.apache.drill.exec.store.easy.json.parser.ValueDef.JsonType;

import com.fasterxml.jackson.core.JsonToken;

/**
 * Constructs a {@link ValueDef} by looking ahead on the input stream.
 * Looking ahead is safe because this class only looks at syntactic
 * tokens such as <code>{</code>, {@code [} or the first value token.
 * The underlying JSON parser is left with the first value token
 * as its current token. Pushes other tokens back on the token stack
 * so they can be re-consumed by the actual parser.
 */
public class ValueDefFactory {

  private int arrayDims;
  private JsonType jsonType = JsonType.EMPTY;

  public ValueDefFactory(TokenIterator tokenizer) {
    inferValueType(tokenizer);
  }

  public static ValueDef lookAhead(TokenIterator tokenizer) {
    ValueDefFactory factory = new ValueDefFactory(tokenizer);
    return new ValueDef(factory.jsonType, factory.arrayDims);
  }

  public static ValueDef arrayLookAhead(TokenIterator tokenizer) {
    ValueDefFactory factory = new ValueDefFactory(tokenizer);
    // Already in an array (saw [), so add one to dimensions
    return new ValueDef(factory.jsonType, factory.arrayDims + 1);
  }

  private void inferValueType(TokenIterator tokenizer) {
    JsonToken token = tokenizer.requireNext();
    switch (token) {
      case START_ARRAY:
        // Position: key: [ ^
        arrayDims++;
        inferValueType(tokenizer);
        break;

      case END_ARRAY:
        break;

      case START_OBJECT:
        // Position: key: { ^
        jsonType = JsonType.OBJECT;
        break;

      case VALUE_NULL:

        // Position: key: null ^
        jsonType = JsonType.NULL;
        break;

      default:
        jsonType = ValueDef.jsonTypeFor(token);
    }
    tokenizer.unget(token);
  }
}
