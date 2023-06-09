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
 * Parses an arbitrary JSON value (which can be a subtree of any
 * complexity) into a JSON string. That is, converts the parsed
 * JSON tokens back into the original JSON text.
 */
public class JsonValueParser extends ValueParser {

  private final StringBuilder json = new StringBuilder();

  protected JsonValueParser(JsonStructureParser structParser,
      ValueListener listener) {
    super(structParser, listener);
  }

  @Override
  public void parse(TokenIterator tokenizer) {
    JsonToken token = tokenizer.requireNext();
    json.setLength(0);
    parseValue(tokenizer, token);
    listener.onText(json.toString());
    json.setLength(0);
  }

  private void parseValue(TokenIterator tokenizer, JsonToken token) {
    String textValue = tokenizer.textValue();
    switch (token) {
      case START_ARRAY:
        json.append(token.asString());
        parseArrayTail(tokenizer);
        break;

      case START_OBJECT:
        json.append(token.asString());
        parseObjectTail(tokenizer);
        break;

      case VALUE_STRING:
        json.append("\"");
        json.append(textValue);
        json.append("\"");
        break;

      default:
        json.append(textValue);
        break;
    }
  }

  public void parseArrayTail(TokenIterator tokenizer) {

    // Accept value* ]
    boolean first = true;
    while (true) {
      JsonToken token = tokenizer.requireNext();
      if (token == JsonToken.END_ARRAY) {
        json.append(token.asString());
        return;
      }
      if (! first) {
        json.append(", ");
      }
      first = false;
      parseValue(tokenizer, token);
    }
  }

  public void parseObjectTail(TokenIterator tokenizer) {

    // Accept (field: value)* }
    boolean first = true;
    while (true) {
      JsonToken token = tokenizer.requireNext();
      if (token == JsonToken.END_OBJECT) {
        json.append(token.asString());
        return;
      }
      if (! first) {
        json.append(", ");
      }
      first = false;
      if (token != JsonToken.FIELD_NAME) {
        throw errorFactory().syntaxError(token);
      }

      json.append("\"");
      json.append(tokenizer.textValue());
      json.append("\": ");
      parseValue(tokenizer, tokenizer.requireNext());
    }
  }
}
