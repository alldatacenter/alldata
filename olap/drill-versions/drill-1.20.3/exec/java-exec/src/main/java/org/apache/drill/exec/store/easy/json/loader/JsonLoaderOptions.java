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

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionSet;
import org.apache.drill.exec.store.easy.json.parser.JsonStructureOptions;

/**
 * Extends the {@link JsonStructureOptions} class, which provides
 * JSON syntactic options, with a number of semantic options enforced
 * at the JSON loader level.
 */
public class JsonLoaderOptions extends JsonStructureOptions {

  /**
   * JSON returns values as typed tokens. If {@code allTextMode} is
   * set, the structure parser converts all scalars (except {@code null})
   * to text and forwards the values to the listener as text.
   * Implements Drill's "all-text mode" for JSON.
   */
  public boolean allTextMode;

  /**
   * Forces all numbers to be double, even if the first number that
   * appears is an integer.
   */
  public boolean readNumbersAsDouble;
  public boolean unionEnabled;

  /**
   * Drill prior to version 1.18 would read a null string
   * array element as the string "null". Drill 1.18 and later
   * reads the same token as a blank string. This flag forces
   * the pre-1.18 behavior.
   * <p>
   * For <code>{a: [null]}</code>
   * <ul>
   * <li>If true: --> "null"</li>
   * <li>if false: --> ""</li>
   * </ul>
   */
  public boolean classicArrayNulls;

  /**
   * If a field contains all nulls, all empty arrays, or the first
   * non-empty array contains a null ({@code [ null, ... ]}, then the
   * default action is to read the column as JSON: literally convert
   * the values (of any JSON complexity) to JSON string of that structure.
   * If {@code false}, then uses the default type.
   */
  public boolean unknownsAsJson = true;

  /**
   * Type to use for a field which contains all nulls, or all empty
   * arrays. Also the default type for an array that starts with the
   * {@code null} value, if {@link #unknownsAsJson} is {@code false}.
   */
  public MinorType nullType = MinorType.VARCHAR;

  /**
   * Enable support for Mongo-style extended types:<br>
   * <code> { field: { "$type": value }, ... }<?code>
   */
  public boolean enableExtendedTypes;

  public JsonLoaderOptions() { }

  public JsonLoaderOptions(OptionSet options) {
    super(options);
    this.allTextMode = options.getBoolean(ExecConstants.JSON_ALL_TEXT_MODE);
    this.readNumbersAsDouble = options.getBoolean(ExecConstants.JSON_READ_NUMBERS_AS_DOUBLE);
    this.unionEnabled = options.getBoolean(ExecConstants.ENABLE_UNION_TYPE_KEY);
    this.enableExtendedTypes = options.getBoolean(ExecConstants.JSON_EXTENDED_TYPES_KEY);
  }
}
