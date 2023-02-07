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

public class ArrayValueParser extends AbstractElementParser {

  protected final ArrayParser arrayParser;

  public ArrayValueParser(ArrayParser arrayParser) {
    super(arrayParser.structParser());
    this.arrayParser = arrayParser;
  }

  /**
   * Parses <code>true | false | null | integer | float | string|
   *              embedded-object | [ ... ]</code>
   */
  @Override
  public void parse(TokenIterator tokenizer) {
    JsonToken token = tokenizer.requireNext();
    if (token == JsonToken.START_ARRAY) {
      // Position: [ ^
      arrayParser.parse(tokenizer);
    } else if (token == JsonToken.VALUE_NULL) {
      // Treat as if the field was not present: ignore
    } else if (token.isScalarValue()) {
      tokenizer.unget(token);
      parseValue(tokenizer);
    } else {
      throw errorFactory().structureError("JSON array expected");
    }
  }

  protected void parseValue(TokenIterator tokenizer) {
    throw errorFactory().structureError("JSON array expected");
  }

  public static class LenientArrayValueParser extends ArrayValueParser {

    public LenientArrayValueParser(ArrayParser arrayParser) {
      super(arrayParser);
    }

    @Override
    protected void parseValue(TokenIterator tokenizer) {
      arrayParser.elementParser().parse(tokenizer);
    }
  }
}
