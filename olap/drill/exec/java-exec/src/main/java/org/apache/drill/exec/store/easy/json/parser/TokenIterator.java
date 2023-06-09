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
import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Function;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.vector.accessor.UnsupportedConversionError;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

public class TokenIterator {
  public static final int MAX_LOOKAHEAD = 30;

  /**
   * Internal exception to unwind the stack when a syntax
   * error is detected within a record. Allows for recovery.
   */
  @SuppressWarnings("serial")
  public static class RecoverableJsonException extends RuntimeException {
  }

  private final ParserManager parserManager;
  private final JsonStructureOptions options;
  private final ErrorFactory errorFactory;
  private final JsonToken[] lookahead = new JsonToken[MAX_LOOKAHEAD];
  private int count;

  public TokenIterator(Iterable<InputStream> streams, Function<InputStream, JsonParser> parserFunction, JsonStructureOptions options, ErrorFactory errorFactory) {
    this.options = options;
    this.errorFactory = errorFactory;
    this.parserManager = new ParserManager(streams, parserFunction);
  }

  public JsonParser getParser() {
    JsonParser parser = parserManager.getParser();
    if (parser == null) {
      parserManager.nextParser();
    }
    return parserManager.getParser();
  }

  public ErrorFactory errorFactory() { return errorFactory; }

  public JsonToken next() {
    if (count > 0) {
      return lookahead[--count];
    }
    try {
      return getNextToken();
    } catch (JsonParseException e) {
      if (options.skipMalformedRecords) {
        throw new RecoverableJsonException();
      } else {
        throw errorFactory.syntaxError(e);
      }
    } catch (IOException e) {
      throw errorFactory.ioException(e);
    }
  }

  private JsonToken getNextToken() throws IOException {
    JsonToken jsonToken = getParser().nextToken();
    if (jsonToken == null) {
      parserManager.nextParser();
      JsonParser parser = getParser();
      if (parser != null) {
        jsonToken = parser.nextToken();
      }
    }
    return jsonToken;
  }

  public String context() {
    JsonLocation location = getParser().getCurrentLocation();
    String token;
    try {
      token = getParser().getText();
    } catch (IOException e) {
      token = "<unknown>";
    }
    return new StringBuilder()
        .append("line ")
        .append(location.getLineNr())
        .append(", column ")
        .append(location.getColumnNr())
        .append(", near token \"")
        .append(token)
        .append("\"")
        .toString();
  }

  public int lineNumber() {
    JsonParser parser = getParser();
    return parser != null ? parser.getCurrentLocation().getLineNr() : 0;
  }

  public int columnNumber() {
    JsonParser parser = getParser();
    return parser != null ? parser.getCurrentLocation().getColumnNr() : 0;
  }

  public String token() {
    try {
      JsonParser parser = getParser();
      return parser != null ? getParser().getText() : null;
    } catch (IOException e) {
      return null;
    }
  }

  public JsonToken requireNext() {
    JsonToken token = next();
    if (token == null) {
      throw errorFactory.structureError("Premature EOF of JSON file");
    }
    return token;
  }

  public JsonToken peek() {
    JsonToken token = requireNext();
    unget(token);
    return token;
  }

  public void unget(JsonToken token) {
    if (count == lookahead.length) {
      throw errorFactory.structureError(
          String.format("Excessive JSON array nesting. Max allowed: %d", lookahead.length));
    }
    lookahead[count++] = token;
  }

  public String textValue() {
    try {
      return getParser().getText();
    } catch (JsonParseException e) {
      throw errorFactory.syntaxError(e);
    } catch (IOException e) {
      throw errorFactory.ioException(e);
    }
  }

  public long longValue() {
    try {
      return getParser().getLongValue();
    } catch (JsonParseException e) {
      throw errorFactory.syntaxError(e);
    } catch (IOException e) {
      throw errorFactory.ioException(e);
    } catch (UnsupportedConversionError e) {
      throw errorFactory.typeError(e);
    }
  }

  public String stringValue() {
    try {
      return getParser().getValueAsString();
    } catch (JsonParseException e) {
      throw errorFactory.syntaxError(e);
    } catch (IOException e) {
      throw errorFactory.ioException(e);
    } catch (UnsupportedConversionError e) {
      throw errorFactory.typeError(e);
    }
  }

  public double doubleValue() {
    try {
      return getParser().getValueAsDouble();
    } catch (JsonParseException e) {
      throw errorFactory.syntaxError(e);
    } catch (IOException e) {
      throw errorFactory.ioException(e);
    } catch (UnsupportedConversionError e) {
      throw errorFactory.typeError(e);
    }
  }

  public byte[] binaryValue() {
    try {
      return getParser().getBinaryValue();
    } catch (JsonParseException e) {
      throw errorFactory.syntaxError(e);
    } catch (IOException e) {
      throw errorFactory.ioException(e);
    } catch (UnsupportedConversionError e) {
      throw errorFactory.typeError(e);
    }
  }

  public RuntimeException invalidValue(JsonToken token) {
    return errorFactory.structureError("Unexpected JSON value: " + token.name());
  }

  public static class ParserManager {
    private final Function<InputStream, JsonParser> parserFunction;
    private final Iterator<InputStream> parsersIterator;
    private JsonParser currentParser;

    public ParserManager(Iterable<InputStream> parsers, Function<InputStream, JsonParser> parserFunction) {
      this.parsersIterator = parsers.iterator();
      this.parserFunction = parserFunction;
      this.nextParser();
    }

    public JsonParser getParser() {
      return currentParser;
    }

    public ParserManager nextParser() {
      if (parsersIterator.hasNext()) {
        try {
          if (currentParser != null) {
            currentParser.close();
          }
        } catch (IOException e) {
          throw new DrillRuntimeException(e);
        }
        currentParser = parserFunction.apply(parsersIterator.next());
      } else {
        currentParser = null;
      }
      return this;
    }

    public void close() throws IOException {
      if (currentParser != null) {
        currentParser.close();
      }
    }
  }

  public void close() throws IOException {
    parserManager.close();
  }
}
