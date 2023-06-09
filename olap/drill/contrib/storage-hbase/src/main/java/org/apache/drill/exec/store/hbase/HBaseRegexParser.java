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
package org.apache.drill.exec.store.hbase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.ValueExpressions.QuotedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HBaseRegexParser {
  private static final Logger logger = LoggerFactory.getLogger(HBaseRegexParser.class);

  /**
   * Regular expression pattern to parse the value operand of the SQL LIKE operator.
   * The tokens could be one of the 3 types.<br/>
   * <ol>
   * <li>Wildcards, i.e. "%" or "_" ==> first regex group ([%_])</li>
   * <li>Character ranges, i.e. "[]" or "[^]" ==> second regex group (\[[^]]*\])</li>
   * <li>Literals ==> third regex group ([^%_\[]+)</li>
   * </ol>
   */
  private static final Pattern SQL_LIKE_REGEX = Pattern.compile("([%_])|(\\[[^]]*\\])|([^%_\\[]+)");

  private static final String SQL_LIKE_ESCAPE_REGEX_STR = "(%s.?)|([%%_])|(\\[[^]]*\\])|([^%%_\\[%s]+)";

  private static final String JAVA_REGEX_SPECIALS = ".()[]{}<>|^-+=*?!$\\";

  private final String likeString_;

  private final String escapeChar_;

  private String regexString_;

  private String prefixString_;

  public HBaseRegexParser(FunctionCall call) {
    this(likeString(call), escapeString(call));
  }

  public HBaseRegexParser(String likeString) {
    this(likeString, null);
  }

  public HBaseRegexParser(String likeString, Character escapeChar) {
    likeString_ = likeString;
    if (escapeChar == null) {
      escapeChar_ = null;
    } else {
      escapeChar_ = JAVA_REGEX_SPECIALS.indexOf(escapeChar) == -1
          ? String.valueOf(escapeChar) : ("\\" + escapeChar);
    }
  }

  /**
   * Convert a SQL LIKE operator Value to a Regular Expression.
   */
  public HBaseRegexParser parse() {
    if (regexString_ != null) {
      return this;
    }

    Matcher matcher = null;
    StringBuilder prefixSB = new StringBuilder();
    StringBuilder regexSB = new StringBuilder("^"); // starts with
    if (escapeChar_ == null) {
      matcher = SQL_LIKE_REGEX.matcher(likeString_);
    } else {
      /*
       * When an escape character is specified, add another capturing group
       * with the escape character in the front for the escape sequence and
       * add the escape character to the exclusion list of literals
       */
      matcher = Pattern.compile(
          String.format(SQL_LIKE_ESCAPE_REGEX_STR, escapeChar_, escapeChar_))
        .matcher(likeString_);
    }
    String fragment = null;
    boolean literalsSoFar = true;
    while (matcher.find()) {
      if (escapeChar_ != null && matcher.group(1) != null) {
        fragment = matcher.group(1);
        if (fragment.length() != 2) {
          throw new IllegalArgumentException("Invalid fragment '"
              + fragment + "' at index " + matcher.start()
              + " in the LIKE operand '" + likeString_ + "'");
        }
        String escapedChar = fragment.substring(1);
        if (literalsSoFar) {
          prefixSB.append(escapedChar);
        }
        regexSB.append(Pattern.quote(escapedChar));
      } else {
        fragment = matcher.group();
        switch (fragment) {
        case "_": // LIKE('_') => REGEX('.')
          literalsSoFar = false;
          regexSB.append(".");
          break;
        case "%": // LIKE('%') => REGEX('.*')
          literalsSoFar = false;
          regexSB.append(".*");
          break;
        default: // ALL other including character ranges
          if (fragment.startsWith("[") && fragment.endsWith("]")) {
            literalsSoFar = false;
            regexSB.append(fragment);
          } else {
            if (literalsSoFar) {
              prefixSB.append(fragment);
            }
            // found literal, just quote it.
            regexSB.append(Pattern.quote(fragment));
          }
          break;
        }
      }
    }
    prefixString_ = prefixSB.toString();
    regexString_ = regexSB.append("$") // ends with
        .toString();

    logger.debug("Converted LIKE string '{}' to REGEX string '{}'.", likeString_, regexString_);
    return this;
  }

  public String getRegexString() {
    return regexString_;
  }

  public String getPrefixString() {
    return prefixString_;
  }

  public String getLikeString() {
    return likeString_;
  }

  private static String likeString(FunctionCall call) {
    return ((QuotedString) call.arg(1)).value;
  }

  private static Character escapeString(FunctionCall call) {
    if (call.argCount() > 2) {
      return ((QuotedString) call.arg(2)).value.charAt(0);
    }
    return null;
  }

}
