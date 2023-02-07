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
package org.apache.drill.exec.planner.sql.parser.impl;

import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.commons.lang3.StringUtils;
import org.apache.drill.exec.planner.sql.parser.DrillParserUtil;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/**
 * Customized {@link SqlParseException} class
 */
public class DrillSqlParseException extends SqlParseException {
  private final String sql;
  private final ParseException parseException;

  public DrillSqlParseException(String sql, SqlParseException sqlParseException) {
    this(sql, sqlParseException.getMessage(), sqlParseException.getPos(), sqlParseException.getExpectedTokenSequences(),
        sqlParseException.getTokenImages(), sqlParseException.getCause());
  }

  @VisibleForTesting
  public DrillSqlParseException(String sql, SqlParserPos pos) {
    this(sql, null, pos, null, null, null);
  }

  private DrillSqlParseException(String sql, String message, SqlParserPos pos,
                                 int[][] expectedTokenSequences,
                                String[] tokenImages, Throwable ex) {
    super(message, pos, expectedTokenSequences, tokenImages, ex);
    this.parseException = (ex instanceof ParseException) ? (ParseException) ex : null;
    this.sql = sql;
  }

  /**
   * Builds error message just like the original {@link SqlParseException}
   * with special handling for {@link ParseException} class.
   * <p>
   * This is customized implementation of the original {@link ParseException#getMessage()}.
   * Any other underlying {@link SqlParseException} exception messages goes through without changes
   * <p>
   * <p>
   * Example:
   * <pre>
   *
   *   Given query: SELECT FROM (VALUES(1));
   *
   *   Generated message for the unsupported FROM token after SELECT would look like:
   *
   *       Encountered "FROM" at line 1, column 8.
   *</pre>
   * @return formatted string representation of {@link DrillSqlParseException}
   */
  @Override
  public String getMessage() {
    // proxy the original message if exception does not belongs
    // to ParseException or no current token passed
    if (parseException == null || parseException.currentToken == null) {
      return super.getMessage();
    }

    int[][] expectedTokenSequences = getExpectedTokenSequences();
    String[] tokenImage = getTokenImages();

    int maxSize = 0;  // holds max possible length of the token sequence
    for (int[] expectedTokenSequence : expectedTokenSequences) {
      if (maxSize < expectedTokenSequence.length) {
        maxSize = expectedTokenSequence.length;
      }
    }

    // parseException.currentToken points to the last successfully parsed token, next one is considered as fail reason
    Token tok = parseException.currentToken.next;
    StringBuilder sb = new StringBuilder("Encountered \"");

    // Adds unknown token sequences to the message
    for (int i = 0; i < maxSize; i++) {
      if (i != 0) {
        sb.append(" ");
      }

      if (tok.kind == DrillParserImplConstants.EOF) {
        sb.append(tokenImage[0]);
        break;
      }
      sb.append(parseException.add_escapes(tok.image));
      tok = tok.next;
    }

    sb
        .append("\" at line ")
        .append(parseException.currentToken.beginLine)
        .append(", column ")
        .append(parseException.currentToken.next.beginColumn)
        .append(".");

    return sb.toString();
  }

  /**
   * Formats sql query which caused the exception by adding
   * error pointer ^ under incorrect expression.
   *
   * @return The sql with a ^ character under the error
   */
  public String getSqlWithErrorPointer() {
    final String sqlErrorMessageHeader = "SQL Query: ";
    final SqlParserPos pos = getPos();
    String formattedSql = sql;
    if (pos != null) {
      int issueLineNumber = pos.getLineNum() - 1;  // recalculates to base 0
      int issueColumnNumber = pos.getColumnNum() - 1;  // recalculates to base 0
      int messageHeaderLength = sqlErrorMessageHeader.length();

      // If the issue happens on the first line, header width should be calculated alongside with the sql query
      int shiftLength = (issueLineNumber == 0) ? issueColumnNumber + messageHeaderLength : issueColumnNumber;

      StringBuilder sb = new StringBuilder();
      String[] lines = sql.split(DrillParserUtil.EOL);

      for (int i = 0; i < lines.length; i++) {
        sb.append(lines[i]);

        if (i == issueLineNumber) {
          sb
              .append(DrillParserUtil.EOL)
              .append(StringUtils.repeat(' ', shiftLength))
              .append("^");
        }
        if (i < lines.length - 1) {
          sb.append(DrillParserUtil.EOL);
        }
      }
      formattedSql = sb.toString();
    }
    return sqlErrorMessageHeader + formattedSql;
  }
}
