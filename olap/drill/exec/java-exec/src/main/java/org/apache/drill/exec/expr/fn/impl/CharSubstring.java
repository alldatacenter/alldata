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

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

/**
 * Evaluate a substring expression for a given UTF-8 value; specifying the start
 * position, and optionally the end position.
 *
 *  - If the start position is negative, start from abs(start) characters from
 *    the end of the buffer.
 *
 *  - If no length is specified, continue to the end of the string.
 *
 *  - If the substring expression's length exceeds the value's upward bound, the
 *    value's length will be used.
 *
 *  - If the substring is invalid, return an empty string.
 *
 *  - NOTE: UTF-8 values range from 1 to 4 bytes per character, thus searching for the
 *          start, length, and negative length may result in 3 partial scans of the
 *          UTF-8 string.
 *
 *  - TODO: implement optional length parameter
 */
@FunctionTemplate(names = {"charsubstring", "substring2", "substr2"},
                  scope = FunctionTemplate.FunctionScope.SIMPLE,
                  nulls = FunctionTemplate.NullHandling.NULL_IF_NULL,
                  outputWidthCalculatorType = FunctionTemplate.OutputWidthCalculatorType.CLONE)
public class CharSubstring implements DrillSimpleFunc {

  @Param VarCharHolder string;
  @Param BigIntHolder offset;
  @Param BigIntHolder length;
  @Output VarCharHolder out;

  @Override
  public void setup() { }

  @Override
  public void eval() {
    out.buffer = string.buffer;

    // handle invalid values; e.g. SUBSTRING(value, 0, x) or SUBSTRING(value, x, 0)
    if (offset.value == 0 || length.value <= 0) {
      out.start = 0;
      out.end = 0;
    } else {

      // start iterating over the UTF-8 buffer to find the first character of the substring
      int byteCount = 0;
      int charCount = 0;
      int byteStart = 0;
      int charStart = 0;
      int byteEnd = 0;
      byte currentByte;
      while (byteCount < string.end - string.start) {
        currentByte = string.buffer.getByte(string.start + byteCount);
        // check current position matches the (positive) start position
        if (offset.value > 0 && charCount == (int)offset.value - 1) {
          byteStart = byteCount;
          charStart = charCount;
        }

        // check current position matches the supplied length
        if (offset.value > 0 && charCount - charStart == (int)length.value) {
          byteEnd = byteCount;
          break;
        }

        if (currentByte < 128) {
          ++charCount;
        }
        ++byteCount;
      }

      out.start = string.start + byteStart;
      out.end = string.start + byteEnd;

      // search backwards for negative offsets
      if (offset.value < 0) {
        int endBytePos = --byteCount;
        int endCharPos = --charCount;
        while (byteCount >= 0) {
          currentByte = string.buffer.getByte(byteCount);

          if (endCharPos - charCount == -(int)offset.value) {
            // matched the negative start offset
            out.start = byteCount;
            charCount = 0;

            // search forward until we find <length> characters
            while (byteCount <= endBytePos) {
              currentByte = string.buffer.getByte(byteCount);
              if (currentByte < 128) {
                ++charCount;
              }
              ++byteCount;
              if (charCount == (int)length.value) {
                out.end = byteCount;
                break;
              }
            }
            break;
          }
          if (currentByte < 128) {
            --charCount;
          }
          --byteCount;
        }
      }

      // if length exceeds value, stop at end of value
      if (out.end == 0) {
        out.end = string.end;
      }
    }
  }

}
