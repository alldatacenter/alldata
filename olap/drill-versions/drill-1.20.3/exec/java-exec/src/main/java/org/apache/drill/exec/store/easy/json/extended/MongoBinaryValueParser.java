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
package org.apache.drill.exec.store.easy.json.extended;

import org.apache.drill.exec.store.easy.json.parser.JsonStructureParser;
import org.apache.drill.exec.store.easy.json.parser.TokenIterator;
import org.apache.drill.exec.store.easy.json.values.ScalarListener;

import com.fasterxml.jackson.core.JsonToken;

/**
 * Parsers a binary. Ignores the subtype field.</pre>
 */
public class MongoBinaryValueParser extends BaseExtendedValueParser {

  protected static final String BINARY_HINT =
      "{\"$binary\": {base64: (\"<payload>\", subType: \"<t>\" }) | " +
        "(\"<payload>\", \"$type\": \"<t>\") }";

  public MongoBinaryValueParser(JsonStructureParser structParser, ScalarListener listener) {
    super(structParser, listener);
  }

  @Override
  protected String typeName() { return ExtendedTypeNames.BINARY; }

  @Override
  public void parse(TokenIterator tokenizer) {

    // Null: assume the value is null
    // (Extension to extended types)
    JsonToken token = tokenizer.requireNext();
    if (token == JsonToken.VALUE_NULL) {
      listener.onValue(token, tokenizer);
      return;
    }

    // Value is a scalar, assume binary value as a string.
    // This is a harmless extension to the standard.
     if (token.isScalarValue()) {
      listener.onValue(token, tokenizer);
      return;
    }

    // Must be an object
    requireToken(token, JsonToken.START_OBJECT);

    // { ^($binary | $type)
    requireToken(tokenizer, JsonToken.FIELD_NAME);
    String fieldName = tokenizer.textValue();
    if (fieldName.equals(ExtendedTypeNames.BINARY_TYPE)) {
      // { $type ^
      parseV1Format(fieldName, tokenizer);
    } else if (!fieldName.equals(ExtendedTypeNames.BINARY)) {
      throw syntaxError();
    } else if (tokenizer.peek() == JsonToken.START_OBJECT) {
      // { $binary: ^{
      parseV2Format(tokenizer);
    } else {
      // { $binary: ^value
      parseV1Format(fieldName, tokenizer);
    }
  }

  // Parse field: { ($binary | $type) ^ ...
  private void parseV1Format(String fieldName, TokenIterator tokenizer) {
    boolean sawData = false;
    for (;;) {
      // key: ^value
      JsonToken token = requireScalar(tokenizer);
      if (fieldName.equals(ExtendedTypeNames.BINARY)) {
        if (sawData) {
          throw syntaxError();
        }
        sawData = true;
        listener.onValue(token, tokenizer);
      }

      // key: value ^(} | key ...)
      token = tokenizer.requireNext();
      if (token == JsonToken.END_OBJECT) {
        break;
      } if (token != JsonToken.FIELD_NAME) {
        throw syntaxError();
      }
      fieldName = tokenizer.textValue();
      switch (fieldName) {
      case ExtendedTypeNames.BINARY:
      case ExtendedTypeNames.BINARY_TYPE:
        break;
      default:
        throw syntaxError();
      }
    }
    if (!sawData) {
      throw syntaxError();
    }
  }

  // Parse field: { $binary: ^{ "base64": "<payload>", "subType": "<t>" } }
  // With fields in either order
  private void parseV2Format(TokenIterator tokenizer) {
    boolean sawData = false;
    requireToken(tokenizer, JsonToken.START_OBJECT);
    for (;;) {
      JsonToken token = tokenizer.requireNext();
      if (token == JsonToken.END_OBJECT) {
        break;
      } else if (token != JsonToken.FIELD_NAME) {
        throw syntaxError();
      }
      switch (tokenizer.textValue()) {
        case "base64":
          if (sawData) {
            throw syntaxError();
          }
          sawData = true;
          listener.onValue(requireScalar(tokenizer), tokenizer);
          break;
        case "subType":
          requireScalar(tokenizer);
          break;
        default:
          throw syntaxError();
      }
    }
    // { $binary: { ... } ^}
    requireToken(tokenizer, JsonToken.END_OBJECT);
  }

  @Override
  protected String formatHint() {
    return BINARY_HINT;
  }
}
