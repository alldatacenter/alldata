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

import org.apache.drill.exec.store.easy.json.parser.MessageParser.MessageContextException;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonToken;

/**
 * The root parsers are special: they must detect EOF. Drill supports
 * top-level objects either enclosed in an array (which forms legal
 * JSON), or as the
 * <a href="http://jsonlines.org/">jsonlines</a> format, restricted
 * to a list of objects (but not scalars or arrays.) Although jsonlines
 * requires newline separators between objects, this parser allows
 * any amount of whitespace, including none.
 */
public abstract class RootParser {
  protected static final Logger logger = LoggerFactory.getLogger(RootParser.class);

  private final JsonStructureParser structParser;
  protected final ObjectParser rootObject;

  public RootParser(JsonStructureParser structParser) {
    this.structParser = structParser;
    this.rootObject = structParser.fieldFactory().rootParser();
  }

  /**
   * Parse one data object. This is the "root" object which may contain
   * nested objects. Overridden to handle different end-of-data indicators
   * for different contexts.
   *
   * @return {@code true} if an object was found, {@code false} if the
   * end of data was reached.
   */
  public abstract boolean parseRoot(TokenIterator tokenizer);

  /**
   * Parse one data object. This is the "root" object which may contain
   * nested objects. Called when the outer parser detects a start
   * object token for a data object.
   *
   * @return {@code true} if an object was found, {@code false} if the
   * end of data was reached.
   */
  protected boolean parseRootObject(JsonToken token, TokenIterator tokenizer) {
    // Position: ^ ?
    switch (token) {
      case NOT_AVAILABLE:
        return false; // Should never occur

      case START_OBJECT:
        // Position: { ^
        rootObject.parse(tokenizer);
        break;

      default:
        // Position ~{ ^
        // Not a valid object.
        // Won't actually get here: the Jackson parser prevents it.
        throw errorFactory().syntaxError(token); // Nothing else is valid
    }
    return true;
  }

  protected ErrorFactory errorFactory() {
    return structParser.errorFactory();
  }

  /**
   * Parser for a <a href="http://jsonlines.org/">jsonlines</a>-style
   * data set which consists of a series of objects. EOF from the parser
   * indicates the end of the data set.
   */
  public static class RootObjectParser extends RootParser {

    public RootObjectParser(JsonStructureParser structParser) {
      super(structParser);
    }

    @Override
    public boolean parseRoot(TokenIterator tokenizer) {
      JsonToken token = tokenizer.next();
      if (token == null) {
        // Position: EOF ^
        return false;
      } else if (token == JsonToken.START_OBJECT) {
        return parseRootObject(token, tokenizer);
      } else {
        throw errorFactory().syntaxError(token); // Nothing else is valid
      }
    }
  }

  /**
   * Parser for a compliant JSON data set which consists of an
   * array at the top level, where each element of the array is a
   * JSON object that represents a data record. A closing array
   * bracket indicates end of data.
   */
  public static class RootArrayParser extends RootParser {

    public RootArrayParser(JsonStructureParser structParser) {
      super(structParser);
    }

    @Override
    public boolean parseRoot(TokenIterator tokenizer) {
      JsonToken token = tokenizer.next();
      if (token == null) {
        // Position: { ... EOF ^
        // Saw EOF, but no closing ]. Warn and ignore.
        // Note that the Jackson parser won't let us get here;
        // it will have already thrown a syntax error.
        logger.warn("Failed to close outer array. {}",
            tokenizer.context());
        return false;
      }
      switch (token) {
        case END_ARRAY:
          return false;
        case START_OBJECT:
          return parseRootObject(token, tokenizer);
        default:
          throw errorFactory().syntaxError(token); // Nothing else is valid
      }
    }
  }

  /**
   * Parser for data embedded within a message structure which is
   * encoded as an array of objects. Example:
   * <pre><code>
   * { status: "ok", results: [ { ... }, { ... } ] }</code></pre>
   * <p>
   * The closing array bracket indicates the end of data; the
   * message parser will parse any content after the closing
   * bracket.
   */
  public static class EmbeddedArrayParser extends RootParser {

    private final MessageParser messageParser;

    public EmbeddedArrayParser(JsonStructureParser structParser, MessageParser messageParser) {
      super(structParser);
      this.messageParser = messageParser;
    }

    @Override
    public boolean parseRoot(TokenIterator tokenizer) {
      JsonToken token = tokenizer.requireNext();
      switch (token) {
        case END_ARRAY:
          break;
        case START_OBJECT:
          return parseRootObject(token, tokenizer);
        default:
          throw errorFactory().syntaxError(token); // Nothing else is valid
      }

      // Parse the trailing message content.
      try {
        messageParser.parseSuffix(tokenizer);
        return false;
      } catch (MessageContextException e) {
        throw errorFactory().messageParseError(e);
      }
    }
  }

  /**
   * Parser for data embedded within a message structure which is encoded
   * as a single JSON object. Example:
   * <pre><code>
   * { status: "ok", results: { ... } }</code></pre>
   * <p>
   * The parser counts over the single object. The
   * message parser will parse any content after the closing
   * bracket.
   */
  public static class EmbeddedObjectParser extends RootParser {

    private final MessageParser messageParser;
    private int objectCount;

    public EmbeddedObjectParser(JsonStructureParser structParser, MessageParser messageParser) {
      super(structParser);
      this.messageParser = messageParser;
    }

    @Override
    public boolean parseRoot(TokenIterator tokenizer) {
      Preconditions.checkState(objectCount <= 1);
      if (objectCount == 0) {
        objectCount++;
        JsonToken token = tokenizer.requireNext();
        if (token == JsonToken.START_OBJECT) {
          return parseRootObject(token, tokenizer);
        } else {
          throw errorFactory().syntaxError(token); // Nothing else is valid
        }
      }
      try {
        messageParser.parseSuffix(tokenizer);
        return false;
      } catch (MessageContextException e) {
        throw errorFactory().messageParseError(e);
      }
    }
  }
}
