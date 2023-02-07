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
 * Parser for a JSON element. Parsers are structured in a hierarchy:
 * <ul>
 * <li>Root - handles top-level objects and arrays, as well as EOF
 * detection.</li>
 * <li>Object - Parses {@code field: value} pairs.</li>
 * <li>Value - Parses a value, which may be an array or an object.</li>
 * <li>Array - Nested within a Value; parses one level of an array.
 * Its children are Values (which may contain more array levels.</li>
 * <li>
 * JSON is completely generic; the element parsers handle JSON's full
 * flexibility. Listeners attached to each parser determine if the
 * actual value in any position makes sense for the structure being
 * parsed.
 */
public interface ElementParser {
  void parse(TokenIterator tokenizer);
}
