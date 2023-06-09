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
package org.apache.drill.common.util;

import io.netty.buffer.ByteBuf;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

public class DrillStringUtils {

  /**
   * Converts the long number into more human readable string.
   */
  public static String readable(long bytes) {
    int unit = 1024;
    long absBytes = Math.abs(bytes);
    if (absBytes < unit) {
      return bytes + " B";
    }
    int exp = (int) (Math.log(absBytes) / Math.log(unit));
    char pre = ("KMGTPE").charAt(exp-1);
    return String.format("%s%.1f %ciB", (bytes == absBytes ? "" : "-"), absBytes / Math.pow(unit, exp), pre);
  }


  /**
   * Unescapes any Java literals found in the {@code String}.
   * For example, it will turn a sequence of {@code '\'} and
   * {@code 'n'} into a newline character, unless the {@code '\'}
   * is preceded by another {@code '\'}.
   *
   * @param input  the {@code String} to unescape, may be null
   * @return a new unescaped {@code String}, {@code null} if null string input
   */
  public static String unescapeJava(String input) {
    return StringEscapeUtils.unescapeJava(input);
  }

  /**
   * Escapes the characters in a {@code String} according to Java string literal
   * rules.
   *
   * Deals correctly with quotes and control-chars (tab, backslash, cr, ff,
   * etc.) so, for example, a tab becomes the characters {@code '\\'} and
   * {@code 't'}.
   *
   * Example:
   * <pre>
   * input string: He didn't say, "Stop!"
   * output string: He didn't say, \"Stop!\"
   * </pre>
   *
   * @param input  String to escape values in, may be null
   * @return String with escaped values, {@code null} if null string input
   */
  public static String escapeJava(String input) {
    return StringEscapeUtils.escapeJava(input);
  }

  public static String escapeNewLines(String input) {
    if (input == null) {
      return null;
    }
    StringBuilder result = new StringBuilder();
    boolean sawNewline = false;
    for (int i = 0; i < input.length(); i++) {
      char curChar = input.charAt(i);
      if (curChar == '\r' || curChar == '\n') {
        if (sawNewline) {
          continue;
        }
        sawNewline = true;
        result.append("\\n");
      } else {
        sawNewline = false;
        result.append(curChar);
      }
    }
    return result.toString();
  }

  /**
   * Copied form commons-lang 2.x code as common-lang 3.x has this API removed.
   * (http://commons.apache.org/proper/commons-lang/article3_0.html#StringEscapeUtils.escapeSql)
   * @param str
   * @return
   */
  public static String escapeSql(String str) {
    return (str == null) ? null : StringUtils.replace(str, "'", "''");
  }

  /**
   * Return a printable representation of a byte buffer, escaping the non-printable
   * bytes as '\\xNN' where NN is the hexadecimal representation of such bytes.
   *
   * This function does not modify  the {@code readerIndex} and {@code writerIndex}
   * of the byte buffer.
   */
  public static String toBinaryString(ByteBuf buf, int strStart, int strEnd) {
    StringBuilder result = new StringBuilder();
    for (int i = strStart; i < strEnd; ++i) {
      appendByte(result, buf.getByte(i));
    }
    return result.toString();
  }

  /**
   * Return a printable representation of a byte array, escaping the non-printable
   * bytes as '\\xNN' where NN is the hexadecimal representation of such bytes.
   */
  public static String toBinaryString(byte[] buf) {
    return toBinaryString(buf, 0, buf.length);
  }

  /**
   * Return a printable representation of a byte array, escaping the non-printable
   * bytes as '\\xNN' where NN is the hexadecimal representation of such bytes.
   */
  public static String toBinaryString(byte[] buf, int strStart, int strEnd) {
    StringBuilder result = new StringBuilder();
    for (int i = strStart; i < strEnd; ++i) {
      appendByte(result, buf[i]);
    }
    return result.toString();
  }

  private static void appendByte(StringBuilder result, byte b) {
    int ch = b & 0xFF;
    if (   (ch >= '0' && ch <= '9')
        || (ch >= 'A' && ch <= 'Z')
        || (ch >= 'a' && ch <= 'z')
        || " `~!@#$%^&*()-_=+[]{}|;:'\",.<>/?".indexOf(ch) >= 0 ) {
        result.append((char)ch);
    } else {
      result.append(String.format("\\x%02X", ch));
    }
  }

  /**
   * parsing a hex encoded binary string and write to an output buffer.
   *
   * This function does not modify  the {@code readerIndex} and {@code writerIndex}
   * of the byte buffer.
   *
   * @return Index in the byte buffer just after the last written byte.
   */
  public static int parseBinaryString(ByteBuf str, int strStart, int strEnd, ByteBuf out) {
    int dstEnd = 0;
    for (int i = strStart; i < strEnd; i++) {
      byte b = str.getByte(i);
      if (b == '\\'
          && strEnd > i+3
          && (str.getByte(i+1) == 'x' || str.getByte(i+1) == 'X')) {
        // ok, take next 2 hex digits.
        byte hd1 = str.getByte(i+2);
        byte hd2 = str.getByte(i+3);
        if (isHexDigit(hd1) && isHexDigit(hd2)) { // [a-fA-F0-9]
          // turn hex ASCII digit -> number
          b = (byte) ((toBinaryFromHex(hd1) << 4) + toBinaryFromHex(hd2));
          i += 3; // skip 3
        }
      }
      out.setByte(dstEnd++, b);
    }
    return dstEnd;
  }

  /**
   * Takes a ASCII digit in the range A-F0-9 and returns
   * the corresponding integer/ordinal value.
   * @param ch  The hex digit.
   * @return The converted hex value as a byte.
   */
  private static byte toBinaryFromHex(byte ch) {
    if ( ch >= 'A' && ch <= 'F' ) {
      return (byte) ((byte)10 + (byte) (ch - 'A'));
    } else if ( ch >= 'a' && ch <= 'f' ) {
      return (byte) ((byte)10 + (byte) (ch - 'a'));
    }
    return (byte) (ch - '0');
  }

  private static boolean isHexDigit(byte c) {
    return (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || (c >= '0' && c <= '9');
  }

  /**
   * Removes extra spaces and empty values in a CSV String
   * @param csv The CSV string to be sanitized
   * @return The sanitized CSV string
   */
  public static String sanitizeCSV(String csv) {
    String[] tokens = csv.split(",");
    return Arrays.stream(tokens)
        .map(String::trim)
        .filter(StringUtils::isNotEmpty)
        .collect(Collectors.joining(","));
  }

  /**
   * Removes all leading slash characters from specified string.
   *
   * @param path string to remove all leading slash characters
   * @return string with removed leading slash characters
   */
  public static String removeLeadingSlash(String path) {
    return StringUtils.stripStart(path, "/");
  }
}
