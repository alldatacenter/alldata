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
import org.apache.drill.exec.store.easy.json.parser.NullValueParser;
import org.apache.drill.exec.store.easy.json.parser.TokenIterator;

/**
 * Parser for a field that contains only nulls. Waits, consuming nulls, until
 * a non-null value appears, after which this parser is replaced by the
 * "resolved" parser. The loader will force resolution at the end of the
 * batch if no actual values are seen before then.
 */
public class NullFieldParser extends NullValueParser implements NullTypeMarker {

  private final TupleParser tupleParser;

  public NullFieldParser(TupleParser tupleParser, String key) {
    super(tupleParser.structParser(), key);
    this.tupleParser = tupleParser;
    tupleParser.loader().addNullMarker(this);
  }

  @Override
  public void forceResolution() {
    tupleParser.loader().removeNullMarker(this);
    tupleParser.forceNullResolution(key);
  }

  /**
   * The column type is now known from context. Create a new scalar
   * column, writer and parser to replace this parser.
   */
  @Override
  protected ElementParser resolve(TokenIterator tokenizer) {
    tupleParser.loader().removeNullMarker(this);
    return tupleParser.resolveField(key, tokenizer);
  }
}
