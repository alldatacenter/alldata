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

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import com.fasterxml.jackson.core.JsonToken;

/**
 * A message parser which accepts a path to the data encoded as a
 * slash-separated string. Given the following JSON message:
 *
 * <pre><code>
 * { status: {
 *     succeeded: true,
 *     runTimeMs: 123,
 *   }
 *   response: {
 *     rowCount: 10,
 *     rows: [
 *       { ... },
 *       { ... } ]
 *     },
 *   footer: "something interesting"
 *  }
 * </code></pre>
 *
 * The path to the actual data would be {@code "response/rows"}.
 * <p>
 * The message parser will "free-wheel" over all objects not on the
 * data path. Thus, this class will skip over the nested structure
 * within the {@code status} member.
 * <p>
 * If the data path is not found then this class reports EOF of
 * the whole data stream. It may have skipped over the actual payload
 * if the path is mis-configured.
 * <p>
 * The payload can also be a single JSON object:
 * <pre><code>
 *   response: {
 *     field1: "value1",
 *     field2: "value2",
 *     ...
 *     },
 * </code></pre>
 * <p>
 * This parser "ungets" the value token (start object or start
 * array) so that the structure parser can determine which case
 * to handle.
 */
public class SimpleMessageParser implements MessageParser {

  private final String[] path;

  public SimpleMessageParser(String dataPath) {
    path = dataPath.split("/");
    Preconditions.checkArgument(path.length > 0,
        "Data path should not be empty.");
  }

  @Override
  public boolean parsePrefix(TokenIterator tokenizer) throws MessageContextException {
    JsonToken token = tokenizer.next();
    if (token == null) {
      return false;
    }
    if (token != JsonToken.START_OBJECT) {
      throw new MessageContextException(token,
          path[0], "Unexpected top-level array");
    }
    return parseToElement(tokenizer, 0);
  }

  private boolean parseToElement(TokenIterator tokenizer, int level) throws MessageContextException {
    while (true) {
      JsonToken token = tokenizer.requireNext();
      switch (token) {
        case FIELD_NAME:
          break;
        case END_OBJECT:
          return false;
        default:
          throw new MessageContextException(token,
              path[0], "Unexpected token");
      }

      String fieldName = tokenizer.textValue();
      if (fieldName.equals(path[level])) {
        return parseInnerLevel(tokenizer, level);
      } else {
        skipElement(tokenizer);
      }
    }
  }

  private boolean parseInnerLevel(TokenIterator tokenizer, int level) throws MessageContextException {
    JsonToken token = tokenizer.requireNext();
    if (level == path.length - 1) {
      switch (token) {
        case VALUE_NULL:
        case START_ARRAY:
        case START_OBJECT:
          tokenizer.unget(token);
          return true;
        default:
          throw new MessageContextException(token,
              path[level], "Expected JSON array for final path element");
      }
    }
    if (token != JsonToken.START_OBJECT) {
      throw new MessageParser.MessageContextException(token,
          path[level], "Expected JSON object");
    }
    return parseToElement(tokenizer, level + 1);
  }

  private void skipElement(TokenIterator tokenizer) {
    DummyValueParser.INSTANCE.parse(tokenizer);
  }

  @Override
  public void parseSuffix(TokenIterator tokenizer) {
    // No need to parse the unwanted tail elements.
  }
}
