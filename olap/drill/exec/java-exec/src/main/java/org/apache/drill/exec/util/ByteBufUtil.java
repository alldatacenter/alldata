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
package org.apache.drill.exec.util;

import io.netty.buffer.DrillBuf;

import org.apache.drill.common.util.DrillStringUtils;

public class ByteBufUtil {

  /**
   * Verifies that the the space provided in the buffer is of specified size.
   * @throws IllegalArgumentException if the specified boundaries do not describe the expected size.
   */
  public static void checkBufferLength(DrillBuf buffer, int start, int end, int requiredLen) {
    int actualLen = (end - start);
    if (actualLen != requiredLen) {
      throw new IllegalArgumentException(String.format("Wrong length %d(%d-%d) in the buffer '%s', expected %d.",
          actualLen, end, start, DrillStringUtils.toBinaryString(buffer, start, end), requiredLen));
    }
  }

  /**
   * Modeled after {@code org.apache.hadoop.io.WritableUtils}.
   * We copy the code to avoid wrapping {@link DrillBuf} to/from {@link DataInput}.
   */
  public static class HadoopWritables {
    /**
     * Serializes an integer to a binary stream with zero-compressed encoding.
     * For -120 <= i <= 127, only one byte is used with the actual value.
     * For other values of i, the first byte value indicates whether the
     * integer is positive or negative, and the number of bytes that follow.
     * If the first byte value v is between -121 and -124, the following integer
     * is positive, with number of bytes that follow are -(v+120).
     * If the first byte value v is between -125 and -128, the following integer
     * is negative, with number of bytes that follow are -(v+124). Bytes are
     * stored in the high-non-zero-byte-first order.
     *
     * @param buffer DrillBuf to read from
     * @param i Integer to be serialized
     */
    public static void writeVInt(DrillBuf buffer,  int start, int end, int i) {
      writeVLong(buffer, start, end, i);
    }

    /**
     * Serializes a long to a binary stream with zero-compressed encoding.
     * For -112 <= i <= 127, only one byte is used with the actual value.
     * For other values of i, the first byte value indicates whether the
     * long is positive or negative, and the number of bytes that follow.
     * If the first byte value v is between -113 and -120, the following long
     * is positive, with number of bytes that follow are -(v+112).
     * If the first byte value v is between -121 and -128, the following long
     * is negative, with number of bytes that follow are -(v+120). Bytes are
     * stored in the high-non-zero-byte-first order.
     *
     * @param buffer DrillBuf to write to
     * @param i Long to be serialized
     */
    public static void writeVLong(DrillBuf buffer, int start, int end, long i) {
      int availableBytes = (end-start);
      if (availableBytes < getVIntSize(i)) {
        throw new NumberFormatException("Expected " + getVIntSize(i) + " bytes but the buffer '"
            + DrillStringUtils.toBinaryString(buffer, start, end) + "' has only "
            + availableBytes + " bytes.");
      }
      buffer.writerIndex(start);

      if (i >= -112 && i <= 127) {
        buffer.writeByte((byte)i);
        return;
      }

      int len = -112;
      if (i < 0) {
        i ^= -1L; // take one's complement'
        len = -120;
      }

      long tmp = i;
      while (tmp != 0) {
        tmp = tmp >> 8;
        len--;
      }

      buffer.writeByte((byte)len);

      len = (len < -120) ? -(len + 120) : -(len + 112);

      for (int idx = len; idx != 0; idx--) {
        int shiftbits = (idx - 1) * 8;
        long mask = 0xFFL << shiftbits;
        buffer.writeByte((byte)((i & mask) >> shiftbits));
      }
    }

    /**
     * Reads a zero-compressed encoded integer from input stream and returns it.
     * @param buffer DrillBuf to read from
     * @return deserialized integer from stream.
     */
    public static int readVInt(DrillBuf buffer, int start, int end) {
      long n = readVLong(buffer, start, end);
      if ((n > Integer.MAX_VALUE) || (n < Integer.MIN_VALUE)) {
        throw new NumberFormatException("Value " + n + " too long to fit in integer");
      }
      return (int)n;
    }

    /**
     * Reads a zero-compressed encoded long from input stream and returns it.
     * @param buffer DrillBuf to read from
     * @return deserialized long from stream.
     */
    public static long readVLong(DrillBuf buffer, int start, int end) {
      buffer.readerIndex(start);
      byte firstByte = buffer.readByte();
      int len = decodeVIntSize(firstByte);
      int availableBytes = (end-start);
      if (len == 1) {
        return firstByte;
      } else if (availableBytes < len) {
        throw new NumberFormatException("Expected " + len + " bytes but the buffer '"
            + DrillStringUtils.toBinaryString(buffer, start, end) + "' has  "
            + availableBytes + " bytes.");
      }

      long longValue = 0;
      for (int idx = 0; idx < len-1; idx++) {
        byte byteValue = buffer.readByte();
        longValue = longValue << 8;
        longValue = longValue | (byteValue & 0xFF);
      }
      return (isNegativeVInt(firstByte) ? (longValue ^ -1L) : longValue);
    }

    /**
     * Parse the first byte of a vint/vlong to determine the number of bytes
     * @param value the first byte of the vint/vlong
     * @return the total number of bytes (1 to 9)
     */
    public static int decodeVIntSize(byte value) {
      if (value >= -112) {
        return 1;
      } else if (value < -120) {
        return -119 - value;
      }
      return -111 - value;
    }

    /**
     * Get the encoded length if an integer is stored in a variable-length format
     * @return the encoded length
     */
    public static int getVIntSize(long i) {
      if (i >= -112 && i <= 127) {
        return 1;
      }

      if (i < 0) {
        i ^= -1L; // take one's complement'
      }
      // find the number of bytes with non-leading zeros
      int dataBits = Long.SIZE - Long.numberOfLeadingZeros(i);
      // find the number of data bytes + length byte
      return (dataBits + 7) / 8 + 1;
    }

    /**
     * Given the first byte of a vint/vlong, determine the sign
     * @param value the first byte
     * @return is the value negative
     */
    public static boolean isNegativeVInt(byte value) {
      return value < -120 || (value >= -112 && value < 0);
    }

  }

}
