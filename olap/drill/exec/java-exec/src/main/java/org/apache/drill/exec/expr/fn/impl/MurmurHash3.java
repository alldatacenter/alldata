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

import io.netty.buffer.DrillBuf;
import io.netty.util.internal.PlatformDependent;


/**
 *
 * MurmurHash3 was written by Austin Appleby, and is placed in the public
 * domain.
 * See http://smhasher.googlecode.com/svn/trunk/MurmurHash3.cpp
 * MurmurHash3_x64_128
 * MurmurHash3_x86_32
 */
public final class MurmurHash3 extends DrillHash{

   public static final long fmix64(long k) {
    k ^= k >>> 33;
    k *= 0xff51afd7ed558ccdL;
    k ^= k >>> 33;
    k *= 0xc4ceb9fe1a85ec53L;
    k ^= k >>> 33;
    return k;
  }

  /*
  Take 64 bit of murmur3_128's output
   */
  public static long murmur3_64(long bStart, long bEnd, DrillBuf buffer, int seed) {

    long h1 = seed & 0x00000000FFFFFFFFL;
    long h2 = seed & 0x00000000FFFFFFFFL;

    final long c1 = 0x87c37b91114253d5L;
    final long c2 = 0x4cf5ad432745937fL;
    long start = buffer.memoryAddress() + bStart;
    long end = buffer.memoryAddress() + bEnd;
    long length = bEnd - bStart;
    long roundedEnd = start + ( length & 0xFFFFFFF0);  // round down to 16 byte block
    for (long i=start; i<roundedEnd; i+=16) {
      long k1 = getLongLittleEndian(i);
      long k2 = getLongLittleEndian(i+8);
      k1 *= c1;
      k1  = Long.rotateLeft(k1,31);
      k1 *= c2;
      h1 ^= k1;
      h1 = Long.rotateLeft(h1,27);
      h1 += h2;
      h1 = h1*5+0x52dce729;
      k2 *= c2;
      k2  = Long.rotateLeft(k2,33);
      k2 *= c1;
      h2 ^= k2;
      h2 = Long.rotateLeft(h2,31);
      h2 += h1;
      h2 = h2*5+0x38495ab5;
    }

    long k1 = 0;
    long k2 = 0;

    // tail
    switch ((int)length & 15) {
      case 15: k2  = (PlatformDependent.getByte(roundedEnd+14) & 0xffL) << 48;
      case 14: k2 ^= (PlatformDependent.getByte(roundedEnd+13) & 0xffL) << 40;
      case 13: k2 ^= (PlatformDependent.getByte(roundedEnd+12) & 0xffL) << 32;
      case 12: k2 ^= (PlatformDependent.getByte(roundedEnd+11) & 0xffL) << 24;
      case 11: k2 ^= (PlatformDependent.getByte(roundedEnd+10) & 0xffL) << 16;
      case 10: k2 ^= (PlatformDependent.getByte(roundedEnd+ 9) & 0xffL) << 8;
      case  9: k2 ^= (PlatformDependent.getByte(roundedEnd+ 8) & 0xffL);
        k2 *= c2;
        k2  = Long.rotateLeft(k2, 33);
        k2 *= c1;
        h2 ^= k2;
      case  8: k1  = (long)PlatformDependent.getByte(roundedEnd+7) << 56;
      case  7: k1 ^= (PlatformDependent.getByte(roundedEnd+6) & 0xffL) << 48;
      case  6: k1 ^= (PlatformDependent.getByte(roundedEnd+5) & 0xffL) << 40;
      case  5: k1 ^= (PlatformDependent.getByte(roundedEnd+4) & 0xffL) << 32;
      case  4: k1 ^= (PlatformDependent.getByte(roundedEnd+3) & 0xffL) << 24;
      case  3: k1 ^= (PlatformDependent.getByte(roundedEnd+2) & 0xffL) << 16;
      case  2: k1 ^= (PlatformDependent.getByte(roundedEnd+1) & 0xffL) << 8;
      case  1: k1 ^= (PlatformDependent.getByte(roundedEnd ) & 0xffL);
        k1 *= c1;
        k1  = Long.rotateLeft(k1,31);
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
    // murmur3_128 should return 128 bit (h1,h2), now we return only 64bits,
    return h1;
  }

  public static long murmur3_64(long val, int seed) {

    long h1 = seed & 0x00000000FFFFFFFFL;
    long h2 = seed & 0x00000000FFFFFFFFL;

    final long c1 = 0x87c37b91114253d5L;
    final long c2 = 0x4cf5ad432745937fL;

    int length = 8;
    long k1 = 0;

    k1 = val;
    k1 *= c1;
    k1  = Long.rotateLeft(k1,31);
    k1 *= c2;
    h1 ^= k1;

    h1 ^= length;
    h2 ^= length;

    h1 += h2;
    h2 += h1;

    h1 = fmix64(h1);
    h2 = fmix64(h2);

    h1 += h2;

    //h2 += h1;
    // murmur3_128 should return 128 bit (h1,h2), now we return only 64bits,
    return h1;

  }

  public static int murmur3_32(int bStart, int bEnd, DrillBuf buffer, int seed) {

    final long c1 = 0xcc9e2d51L;
    final long c2 = 0x1b873593L;
    long start = buffer.memoryAddress() + bStart;
    long length = bEnd - bStart;
    long UINT_MASK=0xffffffffL;
    long lh1 = seed;
    long roundedEnd = start + (length & 0xfffffffc);  // round down to 4 byte block

    for (long i=start; i<roundedEnd; i+=4) {
      // little endian load order
      long lk1 = (PlatformDependent.getByte(i) & 0xff) | ((PlatformDependent.getByte(i+1) & 0xff) << 8) |
              ((PlatformDependent.getByte(i+2) & 0xff) << 16) | (PlatformDependent.getByte(i+3) << 24);

      //k1 *= c1;
      lk1 *= c1;
      lk1 &= UINT_MASK;

      lk1 = ((lk1 << 15) & UINT_MASK) | (lk1 >>> 17);

      lk1 *= c2;
      lk1 = lk1 & UINT_MASK;
      lh1 ^= lk1;
      lh1 = ((lh1 << 13) & UINT_MASK) | (lh1 >>> 19);

      lh1 = lh1*5+0xe6546b64L;
      lh1 = UINT_MASK & lh1;
    }

    // tail
    long lk1 = 0;

    switch((byte)length & 0x03) {
      case 3:
        lk1 = (PlatformDependent.getByte(roundedEnd + 2) & 0xff) << 16;
      case 2:
        lk1 |= (PlatformDependent.getByte(roundedEnd + 1) & 0xff) << 8;
      case 1:
        lk1 |= (PlatformDependent.getByte(roundedEnd) & 0xff);
        lk1 *= c1;
        lk1 = UINT_MASK & lk1;
        lk1 = ((lk1 << 15) & UINT_MASK) | (lk1 >>> 17);

        lk1 *= c2;
        lk1 = lk1 & UINT_MASK;

        lh1 ^= lk1;
    }

    // finalization
    lh1 ^= length;

    lh1 ^= lh1 >>> 16;
    lh1 *= 0x85ebca6b;
    lh1 = UINT_MASK & lh1;
    lh1 ^= lh1 >>> 13;

    lh1 *= 0xc2b2ae35;
    lh1 = UINT_MASK & lh1;
    lh1 ^= lh1 >>> 16;

    return (int)(lh1 & UINT_MASK);
  }

  public static int murmur3_32(long val, int seed) {
    final long c1 = 0xcc9e2d51L;
    final long c2 = 0x1b873593;
    long length = 8;
    long UINT_MASK=0xffffffffL;
    long lh1 = seed & UINT_MASK;
    for (int i=0; i<2; i++) {
      //int ik1 = (int)((val >> i*32) & UINT_MASK);
      long lk1 = ((val >> i*32) & UINT_MASK);

      //k1 *= c1;
      lk1 *= c1;
      lk1 &= UINT_MASK;

      lk1 = ((lk1 << 15) & UINT_MASK) | (lk1 >>> 17);

      lk1 *= c2;
      lk1 &= UINT_MASK;

      lh1 ^= lk1;
      lh1 = ((lh1 << 13) & UINT_MASK) | (lh1 >>> 19);

      lh1 = lh1*5+0xe6546b64L;
      lh1 = UINT_MASK & lh1;
    }
    // finalization
    lh1 ^= length;

    lh1 ^= lh1 >>> 16;
    lh1 *= 0x85ebca6bL;
    lh1 = UINT_MASK & lh1;
    lh1 ^= lh1 >>> 13;
    lh1 *= 0xc2b2ae35L;
    lh1 = UINT_MASK & lh1;
    lh1 ^= lh1 >>> 16;

    return (int)lh1;
  }

  public static long hash64(double val, long seed) {
    return murmur3_64(Double.doubleToLongBits(val), (int) seed);
  }

  public static long hash64(long start, long end, DrillBuf buffer, long seed) {
    return murmur3_64(start, end, buffer, (int) seed);
  }

  public static int hash32(double val, long seed) {
    return murmur3_32(Double.doubleToLongBits(val), (int) seed);
  }

  public static int hash32(int start, int end, DrillBuf buffer, int seed) {
    return murmur3_32(start, end, buffer, seed);
  }

}

