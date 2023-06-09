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
package org.apache.drill.exec.store;

//TODO - make sure we figure out the license on these
public class ByteArrayUtil {

  public static byte[] toByta(Object data) throws Exception {
    if (data instanceof Integer) {
      return toByta((int) data);
    } else if (data instanceof Double) {
      return toByta((double) data);
    } else if (data instanceof Float) {
      return toByta((float) data);
    } else if (data instanceof Boolean) {
      return toByta((boolean) data);
    } else if (data instanceof Long) {
      return toByta((long) data);
    } else {
      throw new Exception("Cannot convert that type to a byte array.");
    }
  }

  // found at http://www.daniweb.com/software-development/java/code/216874/primitive-types-as-byte-arrays
  // I have modified them to switch the endianess of integers and longs
  /* ========================= */
  /* "primitive type --> byte[] data" Methods */
  /* ========================= */
  public static byte[] toByta(byte data) {
    return new byte[]{data};
  }

  public static byte[] toByta(byte[] data) {
    return data;
  }

  /* ========================= */
  public static byte[] toByta(short data) {
    return new byte[]{
        (byte) ((data >> 8) & 0xff),
        (byte) ((data >> 0) & 0xff),
    };
  }

  public static byte[] toByta(short[] data) {
    if (data == null) {
      return null;
    }
    // ----------
    byte[] byts = new byte[data.length * 2];
    for (int i = 0; i < data.length; i++) {
      System.arraycopy(toByta(data[i]), 0, byts, i * 2, 2);
    }
    return byts;
  }

  /* ========================= */
  public static byte[] toByta(char data) {
    return new byte[]{
        (byte) ((data >> 8) & 0xff),
        (byte) ((data >> 0) & 0xff),
    };
  }

  public static byte[] toByta(char[] data) {
    if (data == null) {
      return null;
    }
    // ----------
    byte[] byts = new byte[data.length * 2];
    for (int i = 0; i < data.length; i++) {
      System.arraycopy(toByta(data[i]), 0, byts, i * 2, 2);
    }
    return byts;
  }

  /* ========================= */
  public static byte[] toByta(int data) {
    return new byte[]{
        (byte) ((data >> 0) & 0xff),
        (byte) ((data >> 8) & 0xff),
        (byte) ((data >> 16) & 0xff),
        (byte) ((data >> 24) & 0xff),
    };
  }

  public static byte[] toByta(int[] data) {
    if (data == null) {
      return null;
    }
    // ----------
    byte[] byts = new byte[data.length * 4];
    for (int i = 0; i < data.length; i++) {
      System.arraycopy(toByta(data[i]), 0, byts, i * 4, 4);
    }
    return byts;
  }

  /* ========================= */
  public static byte[] toByta(long data) {
    return new byte[]{
        (byte) ((data >> 0) & 0xff),
        (byte) ((data >> 8) & 0xff),
        (byte) ((data >> 16) & 0xff),
        (byte) ((data >> 24) & 0xff),
        (byte) ((data >> 32) & 0xff),
        (byte) ((data >> 40) & 0xff),
        (byte) ((data >> 48) & 0xff),
        (byte) ((data >> 56) & 0xff),
    };
  }

  public static byte[] toByta(long[] data) {
    if (data == null) {
      return null;
    }
    // ----------
    byte[] byts = new byte[data.length * 8];
    for (int i = 0; i < data.length; i++) {
      System.arraycopy(toByta(data[i]), 0, byts, i * 8, 8);
    }
    return byts;
  }

  /* ========================= */
  public static byte[] toByta(float data) {
    return toByta(Float.floatToRawIntBits(data));
  }

  public static byte[] toByta(float[] data) {
    if (data == null) {
      return null;
    }
    // ----------
    byte[] byts = new byte[data.length * 4];
    for (int i = 0; i < data.length; i++) {
      System.arraycopy(toByta(data[i]), 0, byts, i * 4, 4);
    }
    return byts;
  }

  /* ========================= */
  public static byte[] toByta(double data) {
    return toByta(Double.doubleToRawLongBits(data));
  }

  public static byte[] toByta(double[] data) {
    if (data == null) {
      return null;
    }
    // ----------
    byte[] byts = new byte[data.length * 8];
    for (int i = 0; i < data.length; i++) {
      System.arraycopy(toByta(data[i]), 0, byts, i * 8, 8);
    }
    return byts;
  }

  /* ========================= */
  public static byte[] toByta(boolean data) {
    return new byte[]{(byte) (data ? 0x01 : 0x00)}; // bool -> {1 byte}
  }

  public static byte[] toByta(boolean[] data) {
    // Advanced Technique: The byte array containts information
    // about how many boolean values are involved, so the exact
    // array is returned when later decoded.
    // ----------
    if (data == null) {
      return null;
    }
    // ----------
    int len = data.length;
    byte[] lena = toByta(len); // int conversion; length array = lena
    byte[] byts = new byte[lena.length + (len / 8) + (len % 8 != 0 ? 1 : 0)];
    // (Above) length-array-length + sets-of-8-booleans +? byte-for-remainder
    System.arraycopy(lena, 0, byts, 0, lena.length);
    // ----------
    // (Below) algorithm by Matthew Cudmore: boolean[] -> bits -> byte[]
    for (int i = 0, j = lena.length, k = 7; i < data.length; i++) {
      byts[j] |= (data[i] ? 1 : 0) << k--;
      if (k < 0) {
        j++;
        k = 7;
      }
    }
    // ----------
    return byts;
  }

  // above utility methods found here:
  // http://www.daniweb.com/software-development/java/code/216874/primitive-types-as-byte-arrays
}
