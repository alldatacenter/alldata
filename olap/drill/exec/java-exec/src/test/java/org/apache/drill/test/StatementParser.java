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
package org.apache.drill.test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Very simple parser for semi-colon separated lists of SQL statements which
 * handles quoted semicolons. Drill can execute only one statement at a time
 * (without a trailing semi-colon.) This parser breaks up a statement list
 * into single statements. Input:<code><pre>
 * USE a.b;
 * ALTER SESSION SET `foo` = ";";
 * SELECT * FROM bar WHERE x = "\";";
 * </pre><code>Output:
 * <ul>
 * <li><tt>USE a.b</tt></li>
 * <li><tt>ALTER SESSION SET `foo` = ";"</tt></li>
 * <li><tt>SELECT * FROM bar WHERE x = "\";"</tt></li>
 */
public class StatementParser {
  private final Reader in;

  public StatementParser(Reader in) {
    this.in = in;
  }

  public StatementParser(String text) {
    this(new StringReader(text));
  }

  public String parseNext() throws IOException {
    boolean eof = false;
    StringBuilder buf = new StringBuilder();
    while (true) {
      int c = in.read();
      if (c == -1) {
        eof = true;
        break;
      }
      if (c == ';') {
        break;
      }
      buf.append((char) c);
      if (c == '"' || c == '\'' || c == '`') {
        int quote = c;
        boolean escape = false;
        while (true) {
          c = in.read();
          if (c == -1) {
            throw new IllegalArgumentException("Mismatched quote: " + (char) c);
          }
          buf.append((char) c);
          if (! escape && c == quote) {
            break;
          }
          escape = c == '\\';
        }
      }
    }
    String stmt = buf.toString().trim();
    if (stmt.isEmpty() && eof) {
      return null;
    }
    return stmt;
  }
}
