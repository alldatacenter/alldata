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
package org.apache.drill.exec.store.easy.json.reader;

import com.fasterxml.jackson.core.JsonToken;
import io.netty.buffer.DrillBuf;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.vector.complex.writer.BaseWriter.ComplexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Basic reader implementation for json documents.
 */
public abstract class BaseJsonReader extends BaseJsonProcessor {

  private static final Logger logger = LoggerFactory.getLogger(BaseJsonReader.class);

  /**
   * Describes whether or not this reader can unwrap a single root array record
   * and treat it like a set of distinct records.
   */
  private final boolean skipOuterList;

  /**
   * Whether the reader is currently in a situation where we are unwrapping an
   * outer list.
   */
  private boolean inOuterList;

  public BaseJsonReader(DrillBuf workBuf, boolean enableNanInf, boolean enableEscapeAnyChar, boolean skipOuterList) {
    super(workBuf, enableNanInf, enableEscapeAnyChar);
    this.skipOuterList = skipOuterList;
  }

  @Override
  public ReadState write(ComplexWriter writer) throws IOException {

    try {
      JsonToken t = lastSeenJsonToken;
      if (t == null || t == JsonToken.END_OBJECT) {
        t = parser.nextToken();
      }
      while (!parser.hasCurrentToken() && !parser.isClosed()) {
        t = parser.nextToken();
      }
      lastSeenJsonToken = null;

      if (parser.isClosed()) {
        return ReadState.END_OF_STREAM;
      }

      ReadState readState = writeToVector(writer, t);

      switch (readState) {
        case END_OF_STREAM:
        case WRITE_SUCCEED:
          return readState;
        default:
          throw getExceptionWithContext(UserException.dataReadError(), null).message(
            "Failure while reading JSON. (Got an invalid read state %s )", readState.toString())
            .build(logger);
      }
    } catch (com.fasterxml.jackson.core.JsonParseException ex) {
      if (ignoreJSONParseError()) {
        if (processJSONException() == JsonExceptionProcessingState.END_OF_STREAM) {
          return ReadState.JSON_RECORD_PARSE_EOF_ERROR;
        } else {
          return ReadState.JSON_RECORD_PARSE_ERROR;
        }
      } else {
        throw ex;
      }
    }
  }

  private ReadState writeToVector(ComplexWriter writer, JsonToken t)
    throws IOException {

    switch (t) {
      case START_OBJECT:
        writeDocument(writer, t);
        break;
      case START_ARRAY:
        if (inOuterList) {
          throw createDocumentTopLevelException();
        }

        if (skipOuterList) {
          t = parser.nextToken();
          if (t == JsonToken.START_OBJECT) {
            inOuterList = true;
            writeDocument(writer, t);
          } else {
            throw createDocumentTopLevelException();
          }

        } else {
          writeDocument(writer, t);
        }
        break;
      case END_ARRAY:

        if (inOuterList) {
          confirmLast();
          return ReadState.END_OF_STREAM;
        } else {
          throw getExceptionWithContext(UserException.dataReadError(), null).message(
            "Failure while parsing JSON.  Ran across unexpected %s.", JsonToken.END_ARRAY).build(logger);
        }

      case NOT_AVAILABLE:
        return ReadState.END_OF_STREAM;
      default:
        throw getExceptionWithContext(UserException.dataReadError(), null)
          .message(
            "Failure while parsing JSON.  Found token of [%s].  Drill currently only supports parsing "
              + "json strings that contain either lists or maps.  The root object cannot be a scalar.",
            t).build(logger);
    }

    return ReadState.WRITE_SUCCEED;
  }

  /**
   * Writes the contents of the json node starting with the specified token into a complex vector.
   * Token can take the following values:
   * - START_ARRAY - the top level of json document is an array and skipping of the outer list is disabled
   * - START_OBJECT - the top level of json document is a set of white space delimited maps
   *                  or skipping of the outer list is enabled
   */
  protected abstract void writeDocument(ComplexWriter writer, JsonToken t) throws IOException;

  protected UserException createDocumentTopLevelException() {
    String message = "The top level of your document must either be a single array of maps or a set "
      + "of white space delimited maps.";
    return getExceptionWithContext(UserException.dataReadError(), message).build(logger);
  }

  private void confirmLast() throws IOException {
    parser.nextToken();
    if (!parser.isClosed()) {
      String message = "Drill attempted to unwrap a toplevel list in your document. "
        + "However, it appears that there is trailing content after this top level list.  Drill only "
        + "supports querying a set of distinct maps or a single json array with multiple inner maps.";
      throw getExceptionWithContext(UserException.dataReadError(), message).build(logger);
    }
  }
}
