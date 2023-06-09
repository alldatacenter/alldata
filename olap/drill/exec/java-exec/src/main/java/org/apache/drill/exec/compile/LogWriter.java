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
package org.apache.drill.exec.compile;

import java.io.IOException;
import java.io.Writer;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import org.apache.log4j.Level;
import org.slf4j.Logger;

/**
 * A simple Writer that will forward whole lines (lines ending with a newline) to
 * a Logger. The newlines themselves are not forwarded.
 */
public class LogWriter extends Writer {
  private final Logger logger; // the underlying logger to write to
  private final int level; // the debug level to write at
  private final StringBuilder stringBuilder; // a buffer for incomplete lines
  private boolean isClosed; // close() has been called

  /**
   * Constructor.
   *
   * @param logger the logger this Writer should write to
   * @param level the debug level to write to the logger with
   */
  public LogWriter(final Logger logger, final Level level) {
    Preconditions.checkNotNull(logger);
    Preconditions.checkArgument((level == Level.DEBUG) || (level == Level.ERROR) ||
        (level == Level.INFO) || (level == Level.TRACE) || (level == Level.WARN),
        "level must be a logging level");

    this.logger = logger;
    this.level = level.toInt();
    stringBuilder = new StringBuilder(80);
    isClosed = false;
  }

  @Override
  public void write(char[] cbuf, int off, int len) throws IOException {
    checkNotClosed();

    stringBuilder.append(cbuf, off, len);

    // log all of the whole lines we have
    do {
      final int newlinePos = stringBuilder.indexOf("\n");
      if (newlinePos < 0) {
        break;
      }

      final String oneLine = stringBuilder.substring(0, newlinePos); // leaves out the newline
      writeToLog(oneLine);
      stringBuilder.delete(0, newlinePos + 1); // removes the newline as well

    } while (stringBuilder.length() > 0);
  }

  @Override
  public void flush() throws IOException {
    checkNotClosed();
    flushToLog();
  }

  @Override
  public void close() throws IOException {
    checkNotClosed();
    isClosed = true;
    flushToLog();
  }

  /*
   * Assumes there are no newlines.
   */
  private void flushToLog() {
    writeToLog(stringBuilder.toString());
    stringBuilder.setLength(0);
  }

  private void writeToLog(final String s) {
    if ((s == null) || s.isEmpty()) {
      return;
    }

    switch(level) {
    case Level.DEBUG_INT:
      logger.debug(s);
      break;

    case Level.ERROR_INT:
      logger.error(s);
      break;

    case Level.INFO_INT:
      logger.info(s);
      break;

    case Level.TRACE_INT:
      logger.trace(s);
      break;

    case Level.WARN_INT:
      logger.warn(s);
      break;

    default:
      throw new IllegalStateException();
    }
  }

  private void checkNotClosed() throws IOException {
    if (isClosed) {
      throw new IOException("LogWriter is already closed()");
    }
  }
}
