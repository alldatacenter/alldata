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
package org.apache.drill.exec.expr.fn.impl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * To get good performance for most commonly used pattern matches
 * CONSTANT('ABC') {@link SqlPatternConstantMatcher}
 * STARTSWITH('%ABC') {@link SqlPatternStartsWithMatcher}
 * ENDSWITH('ABC%') {@link SqlPatternEndsWithMatcher }
 * CONTAINS('%ABC%') {@link SqlPatternContainsMatcher}
 * we have simple pattern matchers.
 * Idea is to have our own implementation for simple pattern matchers so we can
 * avoid heavy weight regex processing, skip UTF-8 decoding and char conversion.
 * Instead, we encode the pattern string and do byte comparison against native memory.
 * Overall, this approach
 * gives us orders of magnitude performance improvement for simple pattern matches.
 * Anything that is not simple is considered
 * complex pattern and we use Java regex for complex pattern matches.
 */

public abstract class AbstractSqlPatternMatcher implements SqlPatternMatcher {
  private static final Logger logger = LoggerFactory.getLogger(AbstractSqlPatternMatcher.class);

  protected final String patternString;
  protected final int patternLength;
  protected final ByteBuffer patternByteBuffer;

  public AbstractSqlPatternMatcher(String patternString) {
    this.patternString = patternString;

    CharsetEncoder charsetEncoder = Charsets.UTF_8.newEncoder();
    CharBuffer patternCharBuffer = CharBuffer.wrap(patternString);

    try {
      patternByteBuffer = charsetEncoder.encode(patternCharBuffer);
    } catch (CharacterCodingException e) {
      throw UserException.validationError(e)
          .message("Failure to encode pattern %s using UTF-8", patternString)
          .addContext("Message: ", e.getMessage())
          .build(logger);
    }
    patternLength = patternByteBuffer.limit();
  }
}
