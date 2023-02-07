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
 * Represents a JSON scalar value, either a direct object field, or level
 * within an array. That is:
 * <ul>
 * <li>{@code foo: <value>} - Field value</li>
 * <li>{@code foo: [ <value> ]} - 1D array value</li>
 * <li>{@code foo: [ [<value> ] ]} - 2D array value</li>
 * <li><code>foo: { ... }</code> - object</li>
 * <li><code>foo: [+ { ... } ]</code> - object array</li>
 * </ul>
 */
public interface ValueListener {

  /**
   * Called for a JSON scalar token.
   *
   * @param token the scalar token
   * @param tokenizer provides access to the value of the token
   */
  void onValue(JsonToken token, TokenIterator tokenizer);

  /**
   * Called when a parser converts a JSON structure to text rather
   * than delivering the token directly.
   *
   * @param value the string value of the parsed token or structure
   */
  void onText(String value);
}
