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
import java.io.Reader;
import java.util.Arrays;
import java.util.function.Function;

import org.apache.commons.collections4.IterableUtils;
import org.apache.drill.exec.store.easy.json.parser.MessageParser.MessageContextException;
import org.apache.drill.exec.store.easy.json.parser.RootParser.EmbeddedArrayParser;
import org.apache.drill.exec.store.easy.json.parser.RootParser.EmbeddedObjectParser;
import org.apache.drill.exec.store.easy.json.parser.RootParser.RootArrayParser;
import org.apache.drill.exec.store.easy.json.parser.RootParser.RootObjectParser;
import org.apache.drill.exec.store.easy.json.parser.TokenIterator.RecoverableJsonException;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parser for a subset of the <a href="http://jsonlines.org/">jsonlines</a>
 * format. In particular, supports line-delimited JSON objects, or a single
 * array which holds a list of JSON objects. Although jsonlines requires
 * a newline separator between objects, this parser is more relaxed: it
 * allows any whitespace, or no whitespace at all. It simply looks for the
 * pattern <code>{ ... } { ... }</code> with reading top-level objects.
 * <p>
 * Alternatively, a message parser can provide a path to an array of JSON
 * objects within a messages such as a REST response.
 * <p>
 * Implemented as a parser which converts a stream of tokens from the Jackson
 * JSON parser into a set of events on listeners structured to follow the data
 * structure of the incoming data. JSON can assume many forms. This class
 * assumes that the data is in a tree structure that corresponds to the Drill
 * row structure: a series of object with (mostly) the same schema. Members of
 * the top-level object can be Drill types: scalars, arrays, nested objects
 * (Drill "MAP"s), and so on.
 * <p>
 * The structure parser follows the structure of the incoming data, whatever it
 * might be. This class imposes no semantic rules on that data, it just "calls
 * 'em as it sees 'em" as they say. The listeners are responsible for deciding
 * if the data data makes sense, and if so, how it should be handled.
 * <p>
 * The root listener will receive an event to fields in the top-level object as
 * those fields first appear. Each field is a value object and can correspond to
 * a scalar, array, another object, etc. The type of the value is declared when
 * known, but sometimes it is not known, such as if the value is {@code null}.
 * And, of course, according to JSON, the value is free to change from one row
 * to the next. The listener decides it if wants to handle such "schema change",
 * and if so, how.
 */
public class JsonStructureParser {
  protected static final Logger logger = LoggerFactory.getLogger(JsonStructureParser.class);

  public static class JsonStructureParserBuilder {
    private Iterable<InputStream> streams;
    private Reader reader;
    private JsonStructureOptions options;
    private Function<JsonStructureParser, ObjectParser> parserFactory;
    private ErrorFactory errorFactory;
    private String dataPath;
    private MessageParser messageParser;

    public JsonStructureParserBuilder options(JsonStructureOptions options) {
      this.options = options;
      return this;
    }

    public JsonStructureParserBuilder parserFactory(
        Function<JsonStructureParser, ObjectParser> parserFactory) {
      this.parserFactory = parserFactory;
      return this;
    }

    public JsonStructureParserBuilder errorFactory(ErrorFactory errorFactory) {
      this.errorFactory = errorFactory;
      return this;
    }

    public JsonStructureParserBuilder fromStream(InputStream... stream) {
      this.streams = Arrays.asList(stream);
      return this;
    }

    public JsonStructureParserBuilder fromStream(Iterable<InputStream> streams) {
      this.streams = streams;
      return this;
    }

    public JsonStructureParserBuilder fromReader(Reader reader) {
      this.reader = reader;
      return this;
    }

    public JsonStructureParserBuilder messageParser(MessageParser messageParser) {
      this.messageParser = messageParser;
      return this;
    }

    public JsonStructureParserBuilder dataPath(String dataPath) {
      this.dataPath = dataPath;
      return this;
    }

    public JsonStructureParser build() {
      if (dataPath != null) {
        dataPath = dataPath.trim();
        dataPath = dataPath.isEmpty() ? null : dataPath;
      }
      if (dataPath != null && messageParser == null) {
        messageParser = new SimpleMessageParser(dataPath);
      }
      return new JsonStructureParser(this);
    }
  }

  private final JsonStructureOptions options;
  private final ErrorFactory errorFactory;
  private final TokenIterator tokenizer;
  private final RootParser rootState;
  private final FieldParserFactory fieldFactory;
  private int errorRecoveryCount;

  /**
   * Constructor for the structure parser.
   *
   * @param builder builder
   */
  private JsonStructureParser(JsonStructureParserBuilder builder) {
    this.options = Preconditions.checkNotNull(builder.options);
    this.errorFactory = Preconditions.checkNotNull(builder.errorFactory);
    ObjectMapper mapper = new ObjectMapper()
        .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
        .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
        .configure(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature(),
            options.allowNanInf)
        .configure(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER.mappedFeature(),
            options.enableEscapeAnyChar);

    boolean isStream = !IterableUtils.isEmpty(builder.streams);
    Function<InputStream, JsonParser> parserFunction = stream -> getJsonParser(builder, mapper, stream, isStream);

    tokenizer = new TokenIterator(builder.streams, parserFunction, options, errorFactory());
    fieldFactory = new FieldParserFactory(this,
        Preconditions.checkNotNull(builder.parserFactory));

    // Parse to the start of the data object(s), and create a root
    // state to parse objects and watch for the end of data.
    // The root state parses one object on each next() call.
    if (builder.messageParser == null) {
      rootState = makeRootState();
    } else {
      rootState = makeCustomRoot(builder.messageParser);
    }
  }

  private JsonParser getJsonParser(JsonStructureParserBuilder builder, ObjectMapper mapper, InputStream stream, boolean isStream) {
    try {
      if (isStream) {
        return mapper.getFactory().createParser(stream);
      } else {
        return mapper.getFactory().createParser(Preconditions.checkNotNull(builder.reader));
      }
    } catch (JsonParseException e) {
      throw errorFactory().parseError("Failed to create the JSON parser", e);
    } catch (IOException e) {
      throw errorFactory().ioException(e);
    }
  }

  public JsonStructureOptions options() { return options; }
  public ErrorFactory errorFactory() { return errorFactory; }
  public FieldParserFactory fieldFactory() { return fieldFactory; }

  private RootParser makeRootState() {
    JsonToken token = tokenizer.next();
    if (token == null) {
      return null;
    }
    switch (token) {

      // File contains an array of records.
      case START_ARRAY:
        if (options.skipOuterList) {
          return new RootArrayParser(this);
        } else {
          throw errorFactory().structureError(
              "JSON includes an outer array, but outer array support is not enabled");
        }

      // File contains a sequence of one or more records,
      // presumably sequentially.
      case START_OBJECT:
        tokenizer.unget(token);
        return new RootObjectParser(this);

      // Not a valid JSON file for Drill.
      // Won't get here because the Jackson parser catches errors.
      default:
        throw errorFactory().syntaxError(token);
    }
  }

  private RootParser makeCustomRoot(MessageParser messageParser) {
    try {
      if (!messageParser.parsePrefix(tokenizer)) {
        return null;
      }
    } catch (MessageContextException e) {
      throw errorFactory.messageParseError(e);
    }
    JsonToken token = tokenizer.requireNext();
    switch (token) {
      case VALUE_NULL:
        // If the value is null, just treat it as no data.
        return null;
      case START_ARRAY:
        return new EmbeddedArrayParser(this, messageParser);
      case START_OBJECT:
        tokenizer.unget(token);
        return new EmbeddedObjectParser(this, messageParser);
      default:
        throw new IllegalStateException("Message parser misbehaved: " + token.name());
    }
  }

  public boolean next() {
    if (rootState == null) {
      // Only occurs for an empty document
      return false;
    }
    while (true) {
      try {
        return rootState.parseRoot(tokenizer);
      } catch (RecoverableJsonException e) {
        if (! recover()) {
          throw errorFactory().structureError("Unrecoverable error - " + e.getMessage());
        }
      }
    }
  }

  /**
   * Attempt recovery from a JSON syntax error by skipping to the next
   * record. The Jackson parser is quite limited in its recovery abilities.
   *
   * @return {@code true}  if another record can be read, {@code false}
   * if EOF.
   * @throws org.apache.drill.common.exceptions.UserException if the error is unrecoverable
   * @see <a href="https://issues.apache.org/jira/browse/DRILL-4653">DRILL-4653</a>
   * @see <a href="https://issues.apache.org/jira/browse/DRILL-5953">DRILL-5953</a>
   */
  private boolean recover() {
    logger.warn("Attempting recovery from JSON syntax error. " + tokenizer.context());
    boolean firstAttempt = true;
    while (true) {
      while (true) {
        try {
          if (tokenizer.getParser().isClosed()) {
            throw errorFactory().unrecoverableError();
          }
          JsonToken token = tokenizer.next();
          if (token == null) {
            if (firstAttempt && !options().skipMalformedDocument) {
              throw errorFactory().unrecoverableError();
            }
            return false;
          }
          if (token == JsonToken.NOT_AVAILABLE) {
            return false;
          }
          if (token == JsonToken.END_OBJECT) {
            break;
          }
          firstAttempt = false;
        } catch (RecoverableJsonException e) {
          // Ignore, keep trying
        }
      }
      try {
        JsonToken token = tokenizer.next();
        if (token == null || token == JsonToken.NOT_AVAILABLE) {
          return false;
        }
        if (token == JsonToken.START_OBJECT) {
          logger.warn("Attempting to resume JSON parse. " + tokenizer.context());
          tokenizer.unget(token);
          errorRecoveryCount++;
          return true;
        }
      } catch (RecoverableJsonException e) {
        // Ignore, keep trying
      }
    }
  }

  public int recoverableErrorCount() { return errorRecoveryCount; }

  public int lineNumber() {
    return tokenizer.lineNumber();
  }

  public int columnNumber() {
    return tokenizer.columnNumber();
  }

  public String token() {
    return tokenizer.token();
  }

  public void close() {
    if (errorRecoveryCount > 0) {
      logger.warn("Read JSON input with {} recoverable error(s).",
          errorRecoveryCount);
    }
    try {
      tokenizer.close();
    } catch (IOException e) {
      logger.warn("Ignored failure when closing JSON source", e);
    }
  }
}
