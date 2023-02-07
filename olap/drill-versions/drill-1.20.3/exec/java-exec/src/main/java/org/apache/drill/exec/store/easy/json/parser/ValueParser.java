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

/**
 * Parses a JSON value. JSON allows any value type to appear anywhere a
 * value is allowed; this parser reflects that rule. The associated listener
 * is responsible for semantics: whether a given value should be allowed.
 * <p>
 * Scalar value processing occurs in one of two ways:
 * <ul>
 * <li><b>Typed</b>: The type of the JSON value determines which of the
 * listener "on" method is called. This ensures that the JSON text
 * is parsed into typed values using JSON's parsing rules.</li>
 * <li><b>Text</b>: The text value is passed to the listener's
 * {@code onString()} method regardless of the JSON type. (That is,
 * according to Drill's "all-text mode."</li>
 * </ul>
 * Listeners can enforce one type only, or can be more flexible and
 * allow multiple types.
 */
public abstract class ValueParser extends AbstractElementParser {

  protected ValueListener listener;

  public ValueParser(JsonStructureParser structParser, ValueListener listener) {
    super(structParser);
    this.listener = listener;
  }
}
