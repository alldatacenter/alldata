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

public class ObjectValueParser extends AbstractElementParser {

  private final ObjectParser objectParser;

  public ObjectValueParser(ObjectParser objectParser) {
    super(objectParser.structParser());
    this.objectParser = objectParser;
  }

  /**
   * Parses <code>{ ... }</code>
   */
  @Override
  public void parse(TokenIterator tokenizer) {
    JsonToken token = tokenizer.requireNext();
    switch (token) {
      case START_OBJECT:
        objectParser.parse(tokenizer);
        break;
      case VALUE_NULL:
        // Silently ignore, treat as a missing field
        break;
      default:
        throw errorFactory().structureError("JSON object expected");
    }
  }
}
