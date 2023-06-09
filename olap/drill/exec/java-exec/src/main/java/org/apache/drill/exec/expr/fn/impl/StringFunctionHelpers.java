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

import static org.apache.drill.exec.memory.BoundsChecking.rangeCheck;

import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.joda.time.chrono.ISOChronology;

import io.netty.buffer.DrillBuf;
import io.netty.util.internal.PlatformDependent;

public class StringFunctionHelpers {

  static final int RADIX = 10;
  static final long MAX_LONG = -Long.MAX_VALUE / RADIX;
  static final int MAX_INT = -Integer.MAX_VALUE / RADIX;

  public static long varTypesToLong(final int start, final int end, DrillBuf buffer) {
    if ((end - start) == 0) {
      //empty, not a valid number
      throw nfeL(start, end, buffer);
    }

    int readIndex = start;

    boolean negative = buffer.getByte(readIndex) == '-';

    if (negative && ++readIndex == end) {
      //only one single '-'
      throw nfeL(start, end, buffer);
    }


    long result = 0;
    int digit;

    while (readIndex < end) {
      digit = Character.digit(buffer.getByte(readIndex++),RADIX);
      //not valid digit.
      if (digit == -1) {
        throw nfeL(start, end, buffer);
      }
      //overflow
      if (MAX_LONG > result) {
        throw nfeL(start, end, buffer);
      }

      long next = result * RADIX - digit;

      //overflow
      if (next > result) {
        throw nfeL(start, end, buffer);
      }
      result = next;
    }
    if (!negative) {
      result = -result;
      //overflow
      if (result < 0) {
        throw nfeL(start, end, buffer);
      }
    }

    return result;
  }

  private static NumberFormatException nfeL(int start, int end, DrillBuf buffer) {
    byte[] buf = new byte[end - start];
    buffer.getBytes(start, buf, 0, end - start);
    return new NumberFormatException(new String(buf, com.google.common.base.Charsets.UTF_8));
  }

  private static NumberFormatException nfeI(int start, int end, DrillBuf buffer) {
    byte[] buf = new byte[end - start];
    buffer.getBytes(start, buf, 0, end - start);
    return new NumberFormatException(new String(buf, com.google.common.base.Charsets.UTF_8));
  }

  public static int varTypesToInt(final int start, final int end, DrillBuf buffer) {
    if ((end - start) == 0) {
      // empty, not a valid number
      throw nfeI(start, end, buffer);
    }

    int readIndex = start;

    boolean negative = buffer.getByte(readIndex) == '-';

    if (negative && ++readIndex == end) {
      // only one single '-'
      throw nfeI(start, end, buffer);
    }

    int result = 0;
    int digit;

    while (readIndex < end) {
      digit = Character.digit(buffer.getByte(readIndex++), RADIX);
      // not valid digit.
      if (digit == -1) {
        throw nfeI(start, end, buffer);
      }
      // overflow
      if (MAX_INT > result) {
        throw nfeI(start, end, buffer);
      }

      int next = result * RADIX - digit;

      // overflow
      if (next > result) {
        throw nfeI(start, end, buffer);
      }
      result = next;
    }
    if (!negative) {
      result = -result;
      //overflow
      if (result < 0) {
        throw nfeI(start, end, buffer);
      }
    }

    return result;
  }

  /**
   * Capitalizes first letter in each word.
   * Any symbol except digits and letters is considered as word delimiter.
   *
   * @param source input characters
   */
  public static String initCap(String source) {
    boolean capitalizeNext = true;
    StringBuilder str = new StringBuilder(source);
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if (Character.isLetterOrDigit(c)) {
        str.setCharAt(i, capitalizeNext ? Character.toUpperCase(c) : Character.toLowerCase(c));
        capitalizeNext = false;
      } else {
        capitalizeNext = true;
      }
    }

    return str.toString();
  }

  /**
   * Convert a VarCharHolder to a String.
   *
   * VarCharHolders are designed specifically for object reuse and mutability,
   * only use this method when absolutely necessary for interacting with
   * interfaces that must take a String.
   *
   * @param varCharHolder
   *          a mutable wrapper object that stores a variable length char array,
   *          always in UTF-8
   * @return String of the bytes interpreted as UTF-8
   */
  public static String getStringFromVarCharHolder(VarCharHolder varCharHolder) {
    return toStringFromUTF8(varCharHolder.start, varCharHolder.end, varCharHolder.buffer);
  }

  /**
   * Convert a NullableVarCharHolder to a String.
   */
  public static String getStringFromVarCharHolder(NullableVarCharHolder varCharHolder) {
    return toStringFromUTF8(varCharHolder.start, varCharHolder.end, varCharHolder.buffer);
  }

  public static String toStringFromUTF8(int start, int end, DrillBuf buffer) {
    byte[] buf = new byte[end - start];
    buffer.getBytes(start, buf, 0, end - start);
    String s = new String(buf, Charsets.UTF_8);
    return s;
  }

  public static String toStringFromUTF16(int start, int end, DrillBuf buffer) {
    byte[] buf = new byte[end - start];
    buffer.getBytes(start, buf, 0, end - start);
    return new String(buf, Charsets.UTF_16);
  }

  private static final ISOChronology CHRONOLOGY = org.joda.time.chrono.ISOChronology.getInstanceUTC();

  public static long getDate(DrillBuf buf, int start, int end) {
    rangeCheck(buf, start, end);
    int[] dateFields = memGetDate(buf.memoryAddress(), start, end);
    return CHRONOLOGY.getDateTimeMillis(dateFields[0], dateFields[1], dateFields[2], 0);
  }

  /**
   * Takes a string value, specified as a buffer with a start and end and
   * returns true if the value can be read as a date.
   *
   * @param buf
   * @param start
   * @param end
   * @return true iff the string value can be read as a date
   */
  public static boolean isReadableAsDate(DrillBuf buf, int start, int end) {
    // Tried looking for a method that would do this check without relying on
    // an exception in the failure case (for better performance). Joda does
    // not appear to provide such a function, so the try/catch block
    // was chosen for compatibility with the getDate() method that actually
    // returns the result of parsing.
    try {
      getDate(buf, start, end);
      // the parsing from the line above succeeded, this was a valid date
      return true;
    } catch(IllegalArgumentException ex) {
      return false;
    }
  }

  private static int[] memGetDate(long memoryAddress, int start, int end) {
    long index = memoryAddress + start;
    final long endIndex = memoryAddress + end;
    int digit = 0;

    // Stores three fields (year, month, day)
    int[] dateFields = new int[3];
    int dateIndex = 0;
    int value = 0;

    while (dateIndex < 3 && index < endIndex) {
      digit = Character.digit(PlatformDependent.getByte(index++), RADIX);

      if (digit == -1) {
        dateFields[dateIndex++] = value;
        value = 0;
      } else {
        value = (value * 10) + digit;
      }
    }

    if (dateIndex < 3) {
      // If we reached the end of input, we would have not encountered a separator, store the last value
      dateFields[dateIndex++] = value;
    }

    /* Handle two digit years
     * Follow convention as done by Oracle, Postgres
     * If range of two digits is between 70 - 99 then year = 1970 - 1999
     * Else if two digits is between 00 - 69 = 2000 - 2069
     */
    if (dateFields[0] < 100) {
      if (dateFields[0] < 70) {
        dateFields[0] += 2000;
      } else {
        dateFields[0] += 1900;
      }
    }
    return dateFields;
  }
}
