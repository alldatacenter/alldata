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
 * Parses a JSON array, which consists of a list of <i>elements</i>,
 * represented by a {@code ValueListener}. There is a single listener
 * for all the elements, which are presumed to be of the same type.
 * <p>
 * The element is created when first encountered, either as part of field
 * creation (<code>{a: [10]}</code>) or when later encountered in parsing
 * (<code{a: []} {a: [10]}</code>).
 * <p>
 * This parser <i>does not</i> attempt to parse an array as a poor-man's
 * tuple: {@code [ 101, "fred", 23.45 ]}. The listener could handle this
 * case. But, if we need to handle such a case, it would be better to
 * create a new parser for that case, with an element listener per
 * element as is done for objects.
 */
public class ArrayParser extends AbstractElementParser {

  private final ArrayListener arrayListener;
  private final ElementParser elementParser;

  public ArrayParser(JsonStructureParser structParser, ArrayListener arrayListener, ElementParser elementParser) {
    super(structParser);
    this.arrayListener = arrayListener;
    this.elementParser = elementParser;
  }

  public ElementParser elementParser() { return elementParser; }

  @SuppressWarnings("unchecked")
  public <T extends ArrayListener> T listener() { return (T) arrayListener; }

  /**
   * Parses <code>[ ^ ((value)(, (value)* )? ]</code>
   */
  @Override
  public void parse(TokenIterator tokenizer) {
    arrayListener.onStart();
    top: while (true) {
      // Position: [ (value, )* ^ ?
      JsonToken token = tokenizer.requireNext();
      switch (token) {
        case END_ARRAY:
          break top;
        default:
          tokenizer.unget(token);
          parseElement(tokenizer);
      }
    }
    arrayListener.onEnd();
  }

  private void parseElement(TokenIterator tokenizer) {
    arrayListener.onElementStart();
    elementParser.parse(tokenizer);
    arrayListener.onElementEnd();
  }
}
