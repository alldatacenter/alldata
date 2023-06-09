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
package org.apache.drill.exec.store.easy.text.reader;

import com.univocity.parsers.common.ParsingContext;
import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.common.record.RecordMetaData;

import java.util.Collections;
import java.util.Map;

class TextParsingContext implements ParsingContext {

  private final TextInput input;
  private final TextOutput output;

  private boolean stopped;

  TextParsingContext(TextInput input, TextOutput output) {
    this.input = input;
    this.output = output;
  }

  public boolean isFull() {
    return output.isFull();
  }

  public void stop(boolean stopped) {
    this.stopped = stopped;
  }

  @Override
  public void stop() {
    stopped = true;
  }

  @Override
  public boolean isStopped() {
    return stopped;
  }

  @Override
  public int errorContentLength() {
    return -1;
  }

  @Override
  public Record toRecord(String[] row) {
    return null;
  }

  @Override
  public RecordMetaData recordMetaData() {
    return null;
  }

  @Override
  public long currentLine() {
    return input.lineCount();
  }

  @Override
  public long currentChar() {
    return input.charCount();
  }

  @Override
  public void skipLines(long lines) {
  }

  @Override
  public String[] parsedHeaders() {
    return new String[0];
  }

  @Override
  public int currentColumn() {
    return -1;
  }

  @Override
  public String[] headers() {
    return new String[0];
  }

  @Override
  public String[] selectedHeaders() {
    return new String[0];
  }

  @Override
  public int[] extractedFieldIndexes() {
    return new int[0];
  }

  @Override
  public long currentRecord() {
    return output.getRecordCount();
  }

  @Override
  public String currentParsedContent() {
    return input.getStringSinceMarkForError();
  }

  @Override
  public int currentParsedContentLength() {
    return input.getStringSinceMarkForError().toCharArray().length;
  }

  @Override
  public String fieldContentOnError() {
    return null;
  }

  @Override
  public Map<Long, String> comments() {
    return Collections.emptyMap();
  }

  @Override
  public String lastComment() {
    return null;
  }

  @Override
  public char[] lineSeparator() {
    return new char[0];
  }

  @Override
  public boolean columnsReordered() {
    return false;
  }

  @Override
  public int indexOf(String header) {
    return -1;
  }

  @Override
  public int indexOf(Enum<?> header) {
    return -1;
  }
}

