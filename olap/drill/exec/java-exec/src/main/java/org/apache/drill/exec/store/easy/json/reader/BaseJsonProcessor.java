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

import io.netty.buffer.DrillBuf;

import java.io.IOException;
import java.io.InputStream;

import org.apache.drill.exec.store.easy.json.JsonProcessor;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import org.apache.drill.common.exceptions.UserException;

public abstract class BaseJsonProcessor implements JsonProcessor {

  private static final ObjectMapper DEFAULT_MAPPER = getDefaultMapper();

  private static final ObjectMapper NAN_INF_MAPPER = getDefaultMapper()
    .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true);

  private static final ObjectMapper ESCAPE_ANY_MAPPER = getDefaultMapper()
    .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

  private static final ObjectMapper NAN_INF_AND_ESCAPE_ANY_MAPPER = getDefaultMapper()
    .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true).configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);

  private static final String JACKSON_PARSER_EOF_FILE_MSG = "Unexpected end-of-input:";
  private final boolean enableNanInf;
  private final boolean enableEscapeAnyChar;

  public enum JsonExceptionProcessingState {
    END_OF_STREAM, PROC_SUCCEED
  }

  protected JsonParser parser;
  protected DrillBuf workBuf;
  protected JsonToken lastSeenJsonToken;
  boolean ignoreJSONParseErrors;
  /**
   * The name of the current field being parsed. For Error messages.
   */
  protected String currentFieldName;

  /**
   *
   * @return Default json mapper
   */
  public static ObjectMapper getDefaultMapper() {
    return new ObjectMapper().configure(
        JsonParser.Feature.ALLOW_COMMENTS, true).configure(
        JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
  }

  public boolean ignoreJSONParseError() {
    return ignoreJSONParseErrors;
  }

  public void setIgnoreJSONParseErrors(boolean ignoreJSONParseErrors) {
    this.ignoreJSONParseErrors = ignoreJSONParseErrors;
  }

  public BaseJsonProcessor(DrillBuf workBuf, boolean enableNanInf, boolean enableEscapeAnyChar) {
    this.enableNanInf = enableNanInf;
    this.enableEscapeAnyChar = enableEscapeAnyChar;
    workBuf = Preconditions.checkNotNull(workBuf);
  }

  @Override
  public void setSource(InputStream is) throws IOException {
    if (enableNanInf && enableEscapeAnyChar) {
      parser = NAN_INF_AND_ESCAPE_ANY_MAPPER.getFactory().createParser(is);
    } else if (enableNanInf) {
      parser = NAN_INF_MAPPER.getFactory().createParser(is);
    } else if (enableEscapeAnyChar){
      parser = ESCAPE_ANY_MAPPER.getFactory().createParser(is);
    } else {
      parser = DEFAULT_MAPPER.getFactory().createParser(is);
    }
  }

  @Override
  public void setSource(JsonNode node) {
    this.parser = new TreeTraversingParser(node);
  }

  @Override
  public String toString() {
    JsonLocation location = parser.getCurrentLocation();
    return getClass().getSimpleName() + "[Line=" + location.getLineNr()
        + ", Column=" + (location.getColumnNr() + 1)
        + ", Field=" + getCurrentField()
        + "]";
  }

  @Override
  public UserException.Builder getExceptionWithContext(UserException.Builder builder, String message) {
    builder.message(message);
    JsonLocation location = parser.getCurrentLocation();
    builder.addContext("Line", location.getLineNr())
        .addContext("Column", location.getColumnNr() + 1);
    String fieldName = getCurrentField();
    if (fieldName != null) {
      builder.addContext("Field", fieldName);
    }
    return builder;
  }

  @Override
  public UserException.Builder getExceptionWithContext(Throwable e, String message) {
    UserException.Builder exceptionBuilder = UserException.dataReadError(e);
    return getExceptionWithContext(exceptionBuilder, message);
  }

  /*
   * DRILL - 4653 This method processes JSON tokens until it reaches end of the
   * current line when it processes start of a new JSON line { - return
   * PROC_SUCCEED when it sees EOF the stream - there may not be a closing }
   */

  protected JsonExceptionProcessingState processJSONException()
      throws IOException {
    while (!parser.isClosed()) {
      try {
        JsonToken currentToken = parser.nextToken();
        if(currentToken ==  JsonToken.START_OBJECT && (lastSeenJsonToken == JsonToken.END_OBJECT || lastSeenJsonToken == null))
        {
          lastSeenJsonToken =currentToken;
          break;
        }
        lastSeenJsonToken =currentToken;
        } catch (com.fasterxml.jackson.core.JsonParseException ex1) {
        if (ex1.getOriginalMessage().startsWith(JACKSON_PARSER_EOF_FILE_MSG)) {
          return JsonExceptionProcessingState.END_OF_STREAM;
        }
       continue;
       }
    }
    return JsonExceptionProcessingState.PROC_SUCCEED;
  }

  protected String getCurrentField() {
    return currentFieldName;
  }
}
