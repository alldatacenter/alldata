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

import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.server.options.OptionSet;

/**
 * Input to the JSON structure parser which defines guidelines
 * for low-level parsing as well as listeners for higher-level
 * semantics.
 */
public class JsonStructureOptions {

  /**
   * Allow Infinity and NaN for float values.
   */

  public boolean allowNanInf;

  /**
   * Describes whether or not this reader can unwrap a single root array record
   * and treat it like a set of distinct records.
   */
  public boolean skipOuterList = true;

  /**
   * If true, the structure parser will attempt to recover from JSON syntax
   * errors by starting over at the next record boundary. The Jackson
   * parser has limited recovery abilities. At present, recover can consume
   * two or three valid records before it stabilizes.
   */
  public boolean skipMalformedRecords;

  /**
   * This property works only when {@link #skipMalformedRecords} enabled.
   * If true, {@link TokenIterator.RecoverableJsonException} will be populated for the case of
   * malformed empty document, so it will be possible to handle this exception by caller.
   */
  public boolean skipMalformedDocument;

  public boolean enableEscapeAnyChar;

  public JsonStructureOptions() { }

  public JsonStructureOptions(OptionSet options) {
    this.allowNanInf = options.getBoolean(ExecConstants.JSON_READER_NAN_INF_NUMBERS);
    this.skipMalformedRecords = options.getBoolean(ExecConstants.JSON_READER_SKIP_INVALID_RECORDS_FLAG);
    this.enableEscapeAnyChar = options.getBoolean(ExecConstants.JSON_READER_ESCAPE_ANY_CHAR);
  }
}
