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

import com.fasterxml.jackson.core.JsonToken;

/**
 * Optional custom parser for the portion of a JSON message that
 * surrounds the data "payload". Can be used to extract status codes.
 * See {@link SimpleMessageParser} to simply skip all fields but
 * a given path.
 */
public interface MessageParser {

  @SuppressWarnings("serial")
  class MessageContextException extends Exception {
    public final JsonToken token;
    public final String nextElement;

    public MessageContextException(JsonToken token, String nextElement, String descrip) {
      super(descrip);
      this.token = token;
      this.nextElement = nextElement;
    }
  }

  boolean parsePrefix(TokenIterator tokenizer) throws MessageContextException;
  void parseSuffix(TokenIterator tokenizer) throws MessageContextException;
}
