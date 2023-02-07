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
import org.apache.drill.exec.store.easy.json.parser.ValueParser;
import org.apache.drill.exec.store.easy.json.values.ScalarListener;

import com.fasterxml.jackson.core.JsonToken;

public abstract class BaseExtendedValueParser extends ValueParser {

  protected static final String SCALAR_HINT = "{\"%s\": scalar}";

  public BaseExtendedValueParser(JsonStructureParser structParser, ScalarListener listener) {
    super(structParser, listener);
  }

  protected abstract String typeName();

  /**
   * Parse a value in extended form:<pre><code>
   * {"$type": value}</code</pre>.
   * <p>
   * Uses the given type name. Can parse an entire field,
   * or a subfield, as in the V2 date format.
   */
  protected void parseExtended(TokenIterator tokenizer, String typeName) {

    JsonToken token = tokenizer.requireNext();

    // Null: assume the value is null
    // (Extension to extended types)
    if (token == JsonToken.VALUE_NULL) {
      listener.onValue(token, tokenizer);
      return;
    }

    // Value is a scalar, assume "Relaxed format"
    // (Extension to extended types: allow strings.)
    if (token.isScalarValue()) {
      listener.onValue(token, tokenizer);
      return;
    }

    // Must be an object
    requireToken(token, JsonToken.START_OBJECT);

    // Field name must be correct
    requireField(tokenizer, typeName);

    // Value must be a scalar
    listener.onValue(requireScalar(tokenizer), tokenizer);

    // Must be no other fields
    requireToken(tokenizer, JsonToken.END_OBJECT);
  }

  protected void requireToken(TokenIterator tokenizer, JsonToken expected) {
    requireToken(tokenizer.requireNext(), expected);
  }

  protected void requireToken(JsonToken token, JsonToken expected) {
    if (token != expected) {
      throw syntaxError();
    }
  }

  protected JsonToken requireScalar(TokenIterator tokenizer) {
    JsonToken token = tokenizer.requireNext();
    if (!token.isScalarValue()) {
      throw syntaxError();
    }
    return token;
  }

  protected void requireField(TokenIterator tokenizer, String fieldName) {
    requireToken(tokenizer, JsonToken.FIELD_NAME);
    if (!tokenizer.textValue().equals(fieldName)) {
      throw syntaxError();
    }
  }

  protected RuntimeException syntaxError() {
    return errorFactory().structureError(
        String.format("Expected <%s> for extended type %s.",
            formatHint(), typeName()));
  }

  protected abstract String formatHint();
}
