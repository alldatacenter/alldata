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
package org.apache.drill.exec.record.metadata.schema.parser;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.misc.Interval;

/**
 * Is used for case-insensitive lexing.
 * Constructs a new stream wrapping forcing all characters to be in upper case.
 * Allows building lexical rules match only upper case, making lexer easier to read.
 */
public class UpperCaseCharStream implements CharStream {

  private final CharStream stream;

  public UpperCaseCharStream(CharStream stream) {
    this.stream = stream;
  }

  @Override
  public String getText(Interval interval) {
    return stream.getText(interval);
  }

  @Override
  public void consume() {
    stream.consume();
  }

  @Override
  public int LA(int i) {
    int c = stream.LA(i);
    if (c <= 0) {
      return c;
    }
    return Character.toUpperCase(c);
  }

  @Override
  public int mark() {
    return stream.mark();
  }

  @Override
  public void release(int marker) {
    stream.release(marker);
  }

  @Override
  public int index() {
    return stream.index();
  }

  @Override
  public void seek(int index) {
    stream.seek(index);
  }

  @Override
  public int size() {
    return stream.size();
  }

  @Override
  public String getSourceName() {
    return stream.getSourceName();
  }
}
