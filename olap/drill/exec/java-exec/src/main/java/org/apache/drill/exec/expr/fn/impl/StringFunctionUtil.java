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

import io.netty.buffer.ByteBuf;

import org.apache.drill.common.exceptions.DrillRuntimeException;

public class StringFunctionUtil {

  /* Decode the input bytebuf using UTF-8, and return the number of characters
   */
  public static int getUTF8CharLength(ByteBuf buffer, int start, int end) {
    int charCount = 0;

    for (int idx = start, charLen = 0; idx < end; idx += charLen) {
      charLen = utf8CharLen(buffer, idx);
      ++charCount;  //Advance the counter, since we find one char.
    }
    return charCount;
  }

  /* Decode the input bytebuf using UTF-8. Search in the range of [start, end], find
   * the position of the first byte of next char after we see "charLength" chars.
   *
   */
  public static int getUTF8CharPosition(ByteBuf buffer, int start, int end, int charLength) {
    int charCount = 0;

    if (start >= end) {
      return -1;  //wrong input here.
    }

    for (int idx = start, charLen = 0; idx < end; idx += charLen) {
      charLen = utf8CharLen(buffer, idx);
      ++charCount;  //Advance the counter, since we find one char.
      if (charCount == charLength + 1) {
        return idx;
      }
    }
    return end;
  }

  public static int stringLeftMatchUTF8(ByteBuf str, int strStart, int strEnd,
                                    ByteBuf substr, int subStart, int subEnd) {
    for (int i = strStart; i <= strEnd - (subEnd - subStart); i++) {
      int j = subStart;
      for (; j< subEnd; j++) {
        if (str.getByte(i + j - subStart) != substr.getByte(j)) {
          break;
        }
      }

      if (j == subEnd  && j!= subStart) {  // found a matched substr (non-empty) in str.
        return i;   // found a match.
      }
    }

    return -1;
  }

  public static int utf8CharLen(ByteBuf buffer, int idx) {
    byte firstByte = buffer.getByte(idx);
    if (firstByte >= 0) { // 1-byte char. First byte is 0xxxxxxx.
      return 1;
    } else if ((firstByte & 0xE0) == 0xC0) { // 2-byte char. First byte is 110xxxxx
      return 2;
    } else if ((firstByte & 0xF0) == 0xE0) { // 3-byte char. First byte is 1110xxxx
      return 3;
    } else if ((firstByte & 0xF8) == 0xF0) { //4-byte char. First byte is 11110xxx
      return 4;
    }
    throw new DrillRuntimeException("Unexpected byte 0x" + Integer.toString((int)firstByte & 0xff, 16)
        + " at position " + idx + " encountered while decoding UTF8 string.");
  }

  public static int utf8CharLen(byte currentByte) {
    if (currentByte >= 0) {                 // 1-byte char. First byte is 0xxxxxxx.
      return 1;
    }
    else if ((currentByte & 0xE0) == 0xC0 ) {   // 2-byte char. First byte is 110xxxxx
      return 2;
    }
    else if ((currentByte & 0xF0) == 0xE0 ) {   // 3-byte char. First byte is 1110xxxx
      return 3;
    }
    else if ((currentByte & 0xF8) == 0xF0) {    //4-byte char. First byte is 11110xxx
      return 4;
    }
    throw new DrillRuntimeException("Unexpected byte 0x" + Integer.toString((int)currentByte & 0xff, 16) + " encountered while decoding UTF8 string.");
  }

}
