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
package org.apache.drill.exec.store.pcap.decoder;

/**
 * Simple port of MurmurHash with some state management.
 *
 * Drill's internal hashing isn't useful here because it only deals with off-heap memory.
 */
public class Murmur128 {
  private long h1;
  private long h2;
  private byte[] buf = new byte[8];

  public Murmur128(long h1, long h2) {
    this.h1 = h1;
    this.h2 = h2;
  }

  private long getLongLittleEndian(byte[] buffer, int offset) {
    return ((long) buffer[offset + 7] << 56)
        | ((buffer[offset + 6] & 0xffL) << 48)
        | ((buffer[offset + 5] & 0xffL) << 40)
        | ((buffer[offset + 4] & 0xffL) << 32)
        | ((buffer[offset + 3] & 0xffL) << 24)
        | ((buffer[offset + 2] & 0xffL) << 16)
        | ((buffer[offset + 1] & 0xffL) << 8)
        | ((buffer[offset] & 0xffL));
  }

  private static long fmix64(long k) {
    k ^= k >>> 33;
    k *= 0xff51afd7ed558ccdL;
    k ^= k >>> 33;
    k *= 0xc4ceb9fe1a85ec53L;
    k ^= k >>> 33;
    return k;
  }

  public void hash(int x) {
    buf[0] = (byte) (x & 0xffL);
    buf[1] = (byte) ((x >> 8) & 0xffL);
    buf[2] = (byte) ((x >> 16) & 0xffL);
    buf[3] = (byte) ((x >> 24) & 0xffL);
    hash(buf, 0, 4);
  }

  public void hash(long x) {
    buf[0] = (byte) (x & 0xffL);
    buf[1] = (byte) ((x >> 8) & 0xffL);
    buf[2] = (byte) ((x >> 16) & 0xffL);
    buf[3] = (byte) ((x >> 24) & 0xffL);
    buf[4] = (byte) ((x >> 32) & 0xffL);
    buf[5] = (byte) ((x >> 40) & 0xffL);
    buf[6] = (byte) ((x >> 48) & 0xffL);
    buf[7] = (byte) ((x >> 56) & 0xffL);
    hash(buf, 0, 8);
  }

  public void hash(byte[] buffer, int start, int end) {
    final long c1 = 0x87c37b91114253d5L;
    final long c2 = 0x4cf5ad432745937fL;
    int length = end - start;
    int roundedEnd = start + (length & 0xFFFFFFF0);  // round down to 16 byte block
    for (int i = start; i < roundedEnd; i += 16) {
      long k1 = getLongLittleEndian(buffer, i);
      long k2 = getLongLittleEndian(buffer, i + 8);
      k1 *= c1;
      k1 = Long.rotateLeft(k1, 31);
      k1 *= c2;
      h1 ^= k1;
      h1 = Long.rotateLeft(h1, 27);
      h1 += h2;
      h1 = h1 * 5 + 0x52dce729;
      k2 *= c2;
      k2 = Long.rotateLeft(k2, 33);
      k2 *= c1;
      h2 ^= k2;
      h2 = Long.rotateLeft(h2, 31);
      h2 += h1;
      h2 = h2 * 5 + 0x38495ab5;
    }

    long k1 = 0;
    long k2 = 0;

    // tail
    switch (length & 15) {
      case 15:
        k2 = (buffer[roundedEnd + 14] & 0xffL) << 48;
      case 14:
        k2 ^= (buffer[roundedEnd + 13] & 0xffL) << 40;
      case 13:
        k2 ^= (buffer[roundedEnd + 12] & 0xffL) << 32;
      case 12:
        k2 ^= (buffer[roundedEnd + 11] & 0xffL) << 24;
      case 11:
        k2 ^= (buffer[roundedEnd + 10] & 0xffL) << 16;
      case 10:
        k2 ^= (buffer[roundedEnd + 9] & 0xffL) << 8;
      case 9:
        k2 ^= (buffer[roundedEnd + 8] & 0xffL);
        k2 *= c2;
        k2 = Long.rotateLeft(k2, 33);
        k2 *= c1;
        h2 ^= k2;
      case 8:
        k1 = (long) buffer[roundedEnd + 7] << 56;
      case 7:
        k1 ^= (buffer[roundedEnd + 6] & 0xffL) << 48;
      case 6:
        k1 ^= (buffer[roundedEnd + 5] & 0xffL) << 40;
      case 5:
        k1 ^= (buffer[roundedEnd + 4] & 0xffL) << 32;
      case 4:
        k1 ^= (buffer[roundedEnd + 3] & 0xffL) << 24;
      case 3:
        k1 ^= (buffer[roundedEnd + 2] & 0xffL) << 16;
      case 2:
        k1 ^= (buffer[roundedEnd + 1] & 0xffL) << 8;
      case 1:
        k1 ^= (buffer[roundedEnd] & 0xffL);
        k1 *= c1;
        k1 = Long.rotateLeft(k1, 31);
        k1 *= c2;
        h1 ^= k1;
    }

    h1 ^= length;
    h2 ^= length;

    h1 += h2;
    h2 += h1;

    h1 = fmix64(h1);
    h2 = fmix64(h2);

    h1 += h2;
    h2 += h1;
  }

  @SuppressWarnings("WeakerAccess")
  public long digest64() {
    return h1 ^ h2;
  }
}
