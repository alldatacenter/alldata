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

public abstract class ScalarValueParser extends ValueParser {

  public ScalarValueParser(JsonStructureParser structParser, ValueListener listener) {
    super(structParser, listener);
  }

  /**
   * Parses <code>true | false | null | integer | float | string|
   *              embedded-object</code>
   */
  @Override
  public void parse(TokenIterator tokenizer) {
    JsonToken token = tokenizer.requireNext();
    if (token.isScalarValue()) {
      parseValue(tokenizer, token);
    } else {
      throw errorFactory().structureError("Structure value found where scalar expected");
    }
  }

  protected abstract void parseValue(TokenIterator tokenizer, JsonToken token);

  /**
   * Parses <code>true | false | null | integer | float | string |<br>
   *              embedded-object</code><br>
   * and simply passes the value token on to the listener.
   */
  public static class SimpleValueParser extends ScalarValueParser {

    public SimpleValueParser(JsonStructureParser structParser, ValueListener listener) {
      super(structParser, listener);
    }

    @Override
    public void parseValue(TokenIterator tokenizer, JsonToken token) {
      listener.onValue(token, tokenizer);
    }
  }

  /**
   * Parses <code>true | false | null | integer | float | string |<br>
   *              embedded-object</code>
   * <p>
   * Forwards the result as a string.
   */
  public static class TextValueParser extends ScalarValueParser {

    public TextValueParser(JsonStructureParser structParser, ValueListener listener) {
      super(structParser, listener);
    }

    @Override
    public void parseValue(TokenIterator tokenizer, JsonToken token) {
      if (token == JsonToken.VALUE_NULL) {
        listener.onValue(token, tokenizer);
      } else {
        listener.onText(
            token == JsonToken.VALUE_NULL ? null : tokenizer.textValue());
      }
    }
  }
}
