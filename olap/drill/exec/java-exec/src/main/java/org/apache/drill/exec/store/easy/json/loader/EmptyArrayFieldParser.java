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
package org.apache.drill.exec.store.easy.json.loader;

import org.apache.drill.exec.store.easy.json.loader.JsonLoaderImpl.NullTypeMarker;
import org.apache.drill.exec.store.easy.json.parser.ElementParser;
import org.apache.drill.exec.store.easy.json.parser.EmptyArrayParser;
import org.apache.drill.exec.store.easy.json.parser.TokenIterator;

import com.fasterxml.jackson.core.JsonToken;

/**
 * Represents a run of empty arrays for which we have no type information.
 * Resolves to an actual type when a non-empty array appears. Must resolve
 * the array even if it contains nulls so we can count the null values.
 */
public class EmptyArrayFieldParser extends EmptyArrayParser implements NullTypeMarker {

  private final TupleParser tupleParser;

  public EmptyArrayFieldParser(TupleParser tupleParser, String key) {
    super(tupleParser.structParser(), key);
    this.tupleParser = tupleParser;
    tupleParser.loader().addNullMarker(this);
  }

  @Override
  public void forceResolution() {
    tupleParser.loader().removeNullMarker(this);
    tupleParser.forceEmptyArrayResolution(key);
  }

  /**
   * The column type is now known from context. Create a new array
   * column, writer and parser to replace this parser.
   */
  @Override
  protected ElementParser resolve(TokenIterator tokenizer) {
    tupleParser.loader().removeNullMarker(this);
    if (tokenizer.peek() == JsonToken.START_ARRAY) {

      // The value is [], [ foo, so resolve directly
      return tupleParser.resolveField(key, tokenizer);
    } else {
      // The value is [], foo, so must artificially create the
      // surrounding array to get a repeated type, which will
      // then parse the item as one-item array.
     return tupleParser.resolveArray(key, tokenizer);
    }
  }
}
