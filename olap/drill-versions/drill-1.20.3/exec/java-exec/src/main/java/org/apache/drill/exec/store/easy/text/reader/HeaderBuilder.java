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

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Text output that implements a header reader/parser.
 * The caller parses out the characters of each header;
 * this class assembles UTF-8 bytes into Unicode characters,
 * fixes invalid characters (those not legal for SQL symbols),
 * and maps duplicate names to unique names.
 * <p>
 * That is, this class is as permissive as possible with file
 * headers to avoid spurious query failures for trivial reasons.
 */

// Note: this class uses Java heap strings and the usual Java
// convenience classes. Since we do heavy Unicode string operations,
// and read a single row, there is no good reason to try to use
// value vectors and direct memory for this task.

public class HeaderBuilder implements TextOutput {

  private static final Logger logger = LoggerFactory.getLogger(HeaderBuilder.class);

  /**
   * Maximum Drill symbol length, as enforced for headers.
   * @see <a href="https://drill.apache.org/docs/lexical-structure/#identifier">
   * identifier documentation</a>
   */
  // TODO: Replace with the proper constant, if available
  public static final int MAX_HEADER_LEN = 1024;

  /**
   * Prefix used to replace non-alphabetic characters at the start of
   * a column name. For example, $foo becomes col_foo. Used
   * because SQL does not allow _foo.
   */

  public static final String COLUMN_PREFIX = "col_";

  /**
   * Prefix used to create numbered columns for missing
   * headers. Typical names: column_1, column_2, ...
   */
  public static final String ANONYMOUS_COLUMN_PREFIX = "column_";

  private final Path filePath;
  private final List<String> headers = new ArrayList<>();
  private final ByteBuffer currentField = ByteBuffer.allocate(MAX_HEADER_LEN);

  public HeaderBuilder(Path filePath) {
    this.filePath = filePath;
  }

  @Override
  public void startField(int index) {
    currentField.clear();
  }

  @Override
  public boolean endField() {
    String header = new String(currentField.array(), 0, currentField.position(), Charsets.UTF_8);
    header = validateSymbol(header);
    headers.add(header);
    return true;
  }

  @Override
  public boolean endEmptyField() {

    // Empty header will be rewritten to "column_<n>".
    return endField();
  }

  /**
   * Validate the header name according to the SQL lexical rules.
   * @see <a href="https://drill.apache.org/docs/lexical-structure/#identifier">
   * identifier documentation</a>
   * @param header the header name to validate
   */

  // TODO: Replace with existing code, if any.
  private String validateSymbol(String header) {
    header = header.trim();

    // To avoid unnecessary query failures, just make up a column name
    // if the name is missing or all blanks.

    if (header.isEmpty()) {
      return ANONYMOUS_COLUMN_PREFIX + (headers.size() + 1);
    }
    if (! Character.isAlphabetic(header.charAt(0))) {
      return rewriteHeader(header);
    }
    for (int i = 1; i < header.length(); i++) {
      char ch = header.charAt(i);
      if (! Character.isAlphabetic(ch)  &&
          ! Character.isDigit(ch)  &&  ch != '_') {
        return rewriteHeader(header);
      }
    }
    return header;
  }

  /**
   * Given an invalid header, rewrite it to replace illegal characters
   * with valid ones. The header won't be what the user specified,
   * but it will be a valid SQL identifier. This solution avoids failing
   * queries due to corrupted or invalid header data.
   * <p>
   * Names with invalid first characters are mapped to "col_". Example:
   * $foo maps to col_foo. If the only character is non-alphabetic, treat
   * the column as anonymous and create a generic name: column_4, etc.
   * <p>
   * This mapping could create a column that exceeds the maximum length
   * of 1024. Since that is not really a hard limit, we just live with the
   * extra few characters.
   *
   * @param header the original header
   * @return the rewritten header, valid for SQL
   */
  private String rewriteHeader(String header) {
    final StringBuilder buf = new StringBuilder();

    // If starts with non-alphabetic, can't map the character to
    // underscore, so just tack on a prefix.

    char ch = header.charAt(0);
    if (Character.isAlphabetic(ch)) {
      buf.append(ch);
    } else if (Character.isDigit(ch)) {
      buf.append(COLUMN_PREFIX);
      buf.append(ch);

      // For the strange case of only one character, format
      // the same as an empty header.

    } else if (header.length() == 1) {
      return ANONYMOUS_COLUMN_PREFIX + (headers.size() + 1);
    } else {
      buf.append(COLUMN_PREFIX);
    }

    // Convert all remaining invalid characters to underscores

    for (int i = 1; i < header.length(); i++) {
      ch = header.charAt(i);
      if (Character.isAlphabetic(ch)  ||
          Character.isDigit(ch)  ||  ch == '_') {
        buf.append(ch);
      } else {
        buf.append("_");
      }
    }
    return buf.toString();
  }

  @Override
  public void append(byte data) {

    // Ensure the data fits. Note that, if the name is Unicode, the actual
    // number of characters might be less than the limit even though the
    // byte count exceeds the limit. Fixing this, in general, would require
    // a buffer four times larger, so we leave that as a later improvement
    // if ever needed.

    try {
      currentField.put(data);
    } catch (BufferOverflowException e) {
      throw UserException.dataReadError()
        .message("Column exceeds maximum length of %d", MAX_HEADER_LEN)
        .addContext("File Path", filePath.toString())
        .build(logger);
    }
  }

  @Override
  public void finishRecord() {
    if (headers.isEmpty()) {
      throw UserException.dataReadError()
          .message("The file must define at least one header.")
          .addContext("File Path", filePath.toString())
          .build(logger);
    }

    // Force headers to be unique.

    final Set<String> idents = new HashSet<>();
    for (int i = 0; i < headers.size(); i++) {
      String header = headers.get(i);
      String key = header.toLowerCase();

      // Is the header a duplicate?

      if (idents.contains(key)) {

        // Make header unique by appending a suffix.
        // This loop must end because we have a finite
        // number of headers.
        // The original column is assumed to be "1", so
        // the first duplicate is "2", and so on.
        // Note that this will map columns of the form:
        // "col,col,col_2,col_2_2" to
        // "col", "col_2", "col_2_2", "col_2_2_2".
        // No mapping scheme is perfect...

        for (int l = 2;  ; l++) {
          final String rewritten = header + "_" + l;
          key = rewritten.toLowerCase();
          if (! idents.contains(key)) {
            headers.set(i, rewritten);
            break;
          }
        }
      }
      idents.add(key);
    }
  }

  @Override
  public void startRecord() { }

  public String[] getHeaders() {

    // Just return the headers: any needed checks were done in
    // finishRecord()

    final String[] array = new String[headers.size()];
    return headers.toArray(array);
  }

  // Not used.
  @Override
  public long getRecordCount() { return 0; }

  @Override
  public boolean isFull() { return false; }
}
