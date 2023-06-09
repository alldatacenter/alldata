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
package org.apache.drill.exec.store.pcap;

import org.apache.drill.shaded.guava.com.google.common.primitives.Ints;
import org.apache.drill.shaded.guava.com.google.common.primitives.Shorts;

public class PcapFormatUtils {

  private static final String EMPTY_PACKET_PLACEHOLDER = "[]";
  /**
   *
   * @param byteOrder true for forward file order, false fore revers file order
   * @param buf byte buffer
   * @param offset buffer offset
   * @return integer value of specific bytes from buffer
   */
  public static int getIntFileOrder(boolean byteOrder, final byte[] buf, final int offset) {
    if (byteOrder) {
      return Ints.fromBytes(buf[offset], buf[offset + 1], buf[offset + 2], buf[offset + 3]);
    } else {
      return Ints.fromBytes(buf[offset + 3], buf[offset + 2], buf[offset + 1], buf[offset]);
    }
  }

  /**
   *
   * @param byteOrder true for forward file order, false for reverse file order
   * @param buf byte buffer
   * @param offset buffer offset
   * @return short value as int of specific bytes from buffer
   */
  public static int getShortFileOrder(boolean byteOrder, final byte[] buf, final int offset) {
    if (byteOrder) {
      return Shorts.fromBytes(buf[offset], buf[offset + 1]);
    } else {
      return Shorts.fromBytes(buf[offset + 1], buf[offset]);
    }
  }

  public static int getInt(final byte[] buf, final int offset) {
    return Ints.fromBytes(buf[offset], buf[offset + 1], buf[offset + 2], buf[offset + 3]);
  }

  public static int getShort(final byte[] buf, final int offset) {
    return 0xffff & Shorts.fromBytes(buf[offset], buf[offset + 1]);
  }

  public static int getByte(final byte[] buf, final int offset) {
    return 0xff & buf[offset];
  }

  public static int convertShort(final byte[] data, int offset) {
    return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
  }

  public static int convertInt(final byte[] data, int offset) {
    return ((data[offset] & 0xFF) << 24) | ((data[offset + 1] & 0xFF) << 16) |
        ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);
  }

  public static String parseBytesToASCII(byte[] data) {
    if (data == null) {
      return EMPTY_PACKET_PLACEHOLDER;
    }

    return new String(data).trim()
        .replaceAll("\\P{Print}", ".");
  }
}
