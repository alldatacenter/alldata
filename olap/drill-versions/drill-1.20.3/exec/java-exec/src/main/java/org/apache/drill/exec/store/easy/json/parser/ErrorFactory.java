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

import java.io.IOException;

import org.apache.drill.exec.vector.accessor.UnsupportedConversionError;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonToken;

/**
 * To avoid coupling the JSON structure parser with Drill's error
 * reporting mechanism, the caller passes in an instance of this
 * error factory which will build the required errors, including
 * filling in caller-specific context.
 */
public interface ErrorFactory {

  /**
   * The Jackson JSON parser failed to start on the input file.
   */
  RuntimeException parseError(String string, JsonParseException e);

  /**
   * I/O error reported from the Jackson JSON parser.
   */
  RuntimeException ioException(IOException e);

  /**
   * General structure-level error: something very unusual occurred
   * in the JSON that passed Jackson, but failed in the structure
   * parser.
=   */
  RuntimeException structureError(String string);

  /**
   * The Jackson parser reported a syntax error. Will not
   * occur if recovery is enabled.
   */
  RuntimeException syntaxError(JsonParseException e);

  /**
   * The Jackson parser reported an error when trying to convert
   * a value to a specific type. Should never occur since we only
   * convert to the type that Jackson itself identified.
   */
  RuntimeException typeError(UnsupportedConversionError e);

  /**
   * Received an unexpected token. Should never occur as
   * the Jackson parser itself catches errors.
   */
  RuntimeException syntaxError(JsonToken token);

  /**
   * Error recovery is on, the structure parser tried to recover, but
   * encountered too many other errors and gave up.
   */
  RuntimeException unrecoverableError();

  /**
   * Parser is configured to find a message tag within the JSON
   * and a syntax occurred when following the data path.
   */
  RuntimeException messageParseError(MessageParser.MessageContextException e);
}
