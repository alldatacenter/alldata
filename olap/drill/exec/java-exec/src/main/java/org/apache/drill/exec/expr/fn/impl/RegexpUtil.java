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

/**
 * Code copied from optiq (https://github.com/julianhyde/optiq).
 * Thanks goes to Julian Hyde and other contributors of optiq.
 */

/**
 * Utilities for converting SQL {@code LIKE} and {@code SIMILAR} operators
 * to regular expressions.
 */

public class RegexpUtil {
  private static final String JAVA_REGEX_SPECIALS = "[]()|^-+*?{}$\\.";
  private static final String SQL_SIMILAR_SPECIALS = "[]()|^-+*_%?{}";
  private static final String [] REG_CHAR_CLASSES = {
      "[:ALPHA:]", "\\p{Alpha}",
      "[:alpha:]", "\\p{Alpha}",
      "[:UPPER:]", "\\p{Upper}",
      "[:upper:]", "\\p{Upper}",
      "[:LOWER:]", "\\p{Lower}",
      "[:lower:]", "\\p{Lower}",
      "[:DIGIT:]", "\\d",
      "[:digit:]", "\\d",
      "[:SPACE:]", " ",
      "[:space:]", " ",
      "[:WHITESPACE:]", "\\s",
      "[:whitespace:]", "\\s",
      "[:ALNUM:]", "\\p{Alnum}",
      "[:alnum:]", "\\p{Alnum}"
  };

  // type of pattern string.
  public enum SqlPatternType {
    STARTS_WITH, // Starts with a constant string followed by any string values (ABC%)
    ENDS_WITH, // Ends with a constant string, starts with any string values (%ABC)
    CONTAINS, // Contains a constant string, starts and ends with any string values (%ABC%)
    CONSTANT, // Is a constant string (ABC)
    COMPLEX // Not a simple pattern. Needs regex evaluation.
  };

  public static class SqlPatternInfo {
    private final SqlPatternType patternType; // type of pattern

    // simple pattern with like meta characters(% and _) and escape characters removed.
    // Used for simple pattern matching.
    private final String simplePatternString;

    // javaPatternString used for regex pattern match.
    private final String javaPatternString;

    public SqlPatternInfo(final SqlPatternType patternType, final String javaPatternString, final String simplePatternString) {
      this.patternType = patternType;
      this.simplePatternString = simplePatternString;
      this.javaPatternString = javaPatternString;
    }

    public SqlPatternType getPatternType() {
      return patternType;
    }

    public String getSimplePatternString() {
      return simplePatternString;
    }

    public String getJavaPatternString() {
      return javaPatternString;
    }

  }

  /**
   * Translates a SQL LIKE pattern to Java regex pattern. No escape char.
   */
  public static SqlPatternInfo sqlToRegexLike(String sqlPattern) {
    return sqlToRegexLike(sqlPattern, (char)0);
  }

  /**
   * Translates a SQL LIKE pattern to Java regex pattern, with optional
   * escape string.
   */
  public static SqlPatternInfo sqlToRegexLike(
      String sqlPattern,
      CharSequence escapeStr) {
    final char escapeChar;
    if (escapeStr != null) {
      if (escapeStr.length() != 1) {
        throw invalidEscapeCharacter(escapeStr.toString());
      }
      escapeChar = escapeStr.charAt(0);
    } else {
      escapeChar = 0;
    }
    return sqlToRegexLike(sqlPattern, escapeChar);
  }

  /**
   * Translates a SQL LIKE pattern to Java regex pattern.
   */
  public static SqlPatternInfo sqlToRegexLike(
      String sqlPattern,
      char escapeChar) {
    int i;
    final int len = sqlPattern.length();
    final StringBuilder javaPattern = new StringBuilder(len + len);
    final StringBuilder simplePattern = new StringBuilder(len);

    // Figure out the pattern type and build simplePatternString
    // as we are going through the sql pattern string
    // to build java regex pattern string. This is better instead of using
    // regex later for determining if a pattern is simple or not.
    // Saves CPU cycles.
    // Start with patternType as CONSTANT
    SqlPatternType patternType = SqlPatternType.CONSTANT;

    for (i = 0; i < len; i++) {
      char c = sqlPattern.charAt(i);
      if (JAVA_REGEX_SPECIALS.indexOf(c) >= 0) {
        javaPattern.append('\\');
      }
      if (c == escapeChar) {
        if (i == (sqlPattern.length() - 1)) {
          throw invalidEscapeSequence(sqlPattern, i);
        }
        char nextChar = sqlPattern.charAt(i + 1);
        if ((nextChar == '_')
            || (nextChar == '%')
            || (nextChar == escapeChar)) {
          javaPattern.append(nextChar);
          simplePattern.append(nextChar);
          i++;
        } else {
          throw invalidEscapeSequence(sqlPattern, i);
        }
      } else if (c == '_') {
        // if we find _, it is not simple pattern, we are looking for only %
        patternType = SqlPatternType.COMPLEX;
        javaPattern.append('.');
      } else if (c == '%') {
        if (i == 0) {
          // % at the start could potentially be one of the simple cases i.e. ENDS_WITH.
          patternType = SqlPatternType.ENDS_WITH;
        } else if (i == (len-1)) {
          if (patternType == SqlPatternType.ENDS_WITH) {
            // Starts and Ends with %. This is contains.
            patternType = SqlPatternType.CONTAINS;
          } else if (patternType == SqlPatternType.CONSTANT) {
            // % at the end with constant string in the beginning i.e. STARTS_WITH
            patternType = SqlPatternType.STARTS_WITH;
          }
        } else {
          // If we find % anywhere other than start or end, it is not a simple case.
          patternType = SqlPatternType.COMPLEX;
        }
        javaPattern.append(".");
        javaPattern.append('*');
      } else {
        javaPattern.append(c);
        simplePattern.append(c);
      }
    }

    return new SqlPatternInfo(patternType, javaPattern.toString(), simplePattern.toString());
  }

  private static RuntimeException invalidEscapeCharacter(String s) {
    return new RuntimeException(
        "Invalid escape character '" + s + "'");
  }

  private static RuntimeException invalidEscapeSequence(String s, int i) {
    return new RuntimeException(
        "Invalid escape sequence '" + s + "', " + i);
  }

  private static void similarEscapeRuleChecking(
      String sqlPattern,
      char escapeChar) {
    if (escapeChar == 0) {
      return;
    }
    if (SQL_SIMILAR_SPECIALS.indexOf(escapeChar) >= 0) {
      // The the escape character is a special character
      // SQL 2003 Part 2 Section 8.6 General Rule 3.b
      for (int i = 0; i < sqlPattern.length(); i++) {
        if (sqlPattern.charAt(i) == escapeChar) {
          if (i == (sqlPattern.length() - 1)) {
            throw invalidEscapeSequence(sqlPattern, i);
          }
          char c = sqlPattern.charAt(i + 1);
          if ((SQL_SIMILAR_SPECIALS.indexOf(c) < 0)
              && (c != escapeChar)) {
            throw invalidEscapeSequence(sqlPattern, i);
          }
        }
      }
    }

    // SQL 2003 Part 2 Section 8.6 General Rule 3.c
    if (escapeChar == ':') {
      int position;
      position = sqlPattern.indexOf("[:");
      if (position >= 0) {
        position = sqlPattern.indexOf(":]");
      }
      if (position < 0) {
        throw invalidEscapeSequence(sqlPattern, position);
      }
    }
  }

  private static RuntimeException invalidRegularExpression(
      String pattern, int i) {
    return new RuntimeException(
        "Invalid regular expression '" + pattern + "'");
  }

  private static int sqlSimilarRewriteCharEnumeration(
      String sqlPattern,
      StringBuilder javaPattern,
      int pos,
      char escapeChar) {
    int i;
    for (i = pos + 1; i < sqlPattern.length(); i++) {
      char c = sqlPattern.charAt(i);
      if (c == ']') {
        return i - 1;
      } else if (c == escapeChar) {
        i++;
        char nextChar = sqlPattern.charAt(i);
        if (SQL_SIMILAR_SPECIALS.indexOf(nextChar) >= 0) {
          if (JAVA_REGEX_SPECIALS.indexOf(nextChar) >= 0) {
            javaPattern.append('\\');
          }
          javaPattern.append(nextChar);
        } else if (escapeChar == nextChar) {
          javaPattern.append(nextChar);
        } else {
          throw invalidRegularExpression(sqlPattern, i);
        }
      } else if (c == '-') {
        javaPattern.append('-');
      } else if (c == '^') {
        javaPattern.append('^');
      } else if (sqlPattern.startsWith("[:", i)) {
        int numOfRegCharSets = REG_CHAR_CLASSES.length / 2;
        boolean found = false;
        for (int j = 0; j < numOfRegCharSets; j++) {
          if (sqlPattern.startsWith(REG_CHAR_CLASSES[j + j], i)) {
            javaPattern.append(REG_CHAR_CLASSES[j + j + 1]);

            i += REG_CHAR_CLASSES[j + j].length() - 1;
            found = true;
            break;
          }
        }
        if (!found) {
          throw invalidRegularExpression(sqlPattern, i);
        }
      } else if (SQL_SIMILAR_SPECIALS.indexOf(c) >= 0) {
        throw invalidRegularExpression(sqlPattern, i);
      } else {
        javaPattern.append(c);
      }
    }
    return i - 1;
  }

  /**
   * Translates a SQL SIMILAR pattern to Java regex pattern. No escape char.
   */
  public static String sqlToRegexSimilar(String sqlPattern) {
    return sqlToRegexSimilar(sqlPattern, (char) 0);
  }

  /**
   * Translates a SQL SIMILAR pattern to Java regex pattern, with optional
   * escape string.
   */
  public static String sqlToRegexSimilar(
      String sqlPattern,
      CharSequence escapeStr) {
    final char escapeChar;
    if (escapeStr != null) {
      if (escapeStr.length() != 1) {
        throw invalidEscapeCharacter(escapeStr.toString());
      }
      escapeChar = escapeStr.charAt(0);
    } else {
      escapeChar = 0;
    }
    return sqlToRegexSimilar(sqlPattern, escapeChar);
  }

  /**
   * Translates SQL SIMILAR pattern to Java regex pattern.
   */
  public static String sqlToRegexSimilar(
      String sqlPattern,
      char escapeChar) {
    similarEscapeRuleChecking(sqlPattern, escapeChar);

    boolean insideCharacterEnumeration = false;
    final StringBuilder javaPattern =
        new StringBuilder(sqlPattern.length() * 2);
    final int len = sqlPattern.length();
    for (int i = 0; i < len; i++) {
      char c = sqlPattern.charAt(i);
      if (c == escapeChar) {
        if (i == (len - 1)) {
          // It should never reach here after the escape rule
          // checking.
          throw invalidEscapeSequence(sqlPattern, i);
        }
        char nextChar = sqlPattern.charAt(i + 1);
        if (SQL_SIMILAR_SPECIALS.indexOf(nextChar) >= 0) {
          // special character, use \ to replace the escape char.
          if (JAVA_REGEX_SPECIALS.indexOf(nextChar) >= 0) {
            javaPattern.append('\\');
          }
          javaPattern.append(nextChar);
        } else if (nextChar == escapeChar) {
          javaPattern.append(nextChar);
        } else {
          // It should never reach here after the escape rule
          // checking.
          throw invalidEscapeSequence(sqlPattern, i);
        }
        i++; // we already process the next char.
      } else {
        switch (c) {
        case '_':
          javaPattern.append('.');
          break;
        case '%':
          javaPattern.append('.');
          javaPattern.append('*');
          break;
        case '[':
          javaPattern.append('[');
          insideCharacterEnumeration = true;
          i = sqlSimilarRewriteCharEnumeration(
              sqlPattern,
              javaPattern,
              i,
              escapeChar);
          break;
        case ']':
          if (!insideCharacterEnumeration) {
            throw invalidRegularExpression(sqlPattern, i);
          }
          insideCharacterEnumeration = false;
          javaPattern.append(']');
          break;
        case '\\':
          javaPattern.append("\\\\");
          break;
        case '$':

          // $ is special character in java regex, but regular in
          // SQL regex.
          javaPattern.append("\\$");
          break;
        default:
          javaPattern.append(c);
        }
      }
    }
    if (insideCharacterEnumeration) {
      throw invalidRegularExpression(sqlPattern, len);
    }

    return javaPattern.toString();
  }
}
