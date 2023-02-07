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
 * Parses a Mongo date in the
 * <a href="https://docs.mongodb.com/manual/reference/mongodb-extended-json-v1/#date">V1</a> format:<pre><code>
 * { "$date": "&lt;date>" }</code></pre> and in the
 * <a href="https://docs.mongodb.com/manual/reference/mongodb-extended-json/#bson.Date">V2</a> formats:<pre><code>
 * {"$date": {"$numberLong": "&lt;millis>"}
 * {"$date": "&lt;ISO-8601 Date/Time Format>"}</code></pre>
 */
public class MongoDateValueParser extends BaseExtendedValueParser {

  private static final String DATE_HINT = "^{\"$date\": scalar | " +
    String.format(SCALAR_HINT, ExtendedTypeNames.DOUBLE) + "}";

  public MongoDateValueParser(JsonStructureParser structParser, ScalarListener listener) {
    super(structParser, listener);
  }

  @Override
  protected String typeName() { return ExtendedTypeNames.DATE; }

  @Override
  public void parse(TokenIterator tokenizer) {

    // Null: assume the value is null
    // (Extension to extended types)
    JsonToken token = tokenizer.requireNext();
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
    requireField(tokenizer, ExtendedTypeNames.DATE);

    // If value is an object, assume V2 canonical format.
    token = tokenizer.requireNext();
    if (token == JsonToken.START_OBJECT) {
      tokenizer.unget(token);
      parseExtended(tokenizer, ExtendedTypeNames.LONG);
    } else {
      // Otherwise, Value must be a scalar
      tokenizer.unget(token);
      listener.onValue(requireScalar(tokenizer), tokenizer);
    }

    // Must be no other fields
    requireToken(tokenizer, JsonToken.END_OBJECT);
  }

  @Override
  protected String formatHint() {
    return DATE_HINT;
  }
}
