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

import static org.apache.drill.exec.memory.BoundsChecking.rangeCheck;

public final class XXHash extends DrillHash{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(XXHash.class);

  //UnsignedLongs.decode won't give right output(keep the value in 8 bytes unchanged).
  static final long PRIME64_1 = 0x9e3779b185ebca87L;//UnsignedLongs.decode("11400714785074694791");
  static final long PRIME64_2 = 0xc2b2ae3d27d4eb4fL;//UnsignedLongs.decode("14029467366897019727");
  static final long PRIME64_3 = 0x165667b19e3779f9L;//UnsignedLongs.decode("1609587929392839161");
  static final long PRIME64_4 = 0x85ebca77c2b2ae63L;//UnsignedLongs.decode("9650029242287828579");
  static final long PRIME64_5 = 0x27d4eb2f165667c5L;//UnsignedLongs.decode("2870177450012600261");

  private static long hash64bytes(long start, long bEnd, long seed) {
    long len = bEnd - start;
    long h64;
    long p = start;

    // for long strings (greater than 32 bytes)
    if (len >= 32) {
      final long limit = bEnd - 32;
      long v1 = seed + PRIME64_1 + PRIME64_2;
      long v2 = seed + PRIME64_2;
      long v3 = seed + 0;
      long v4 = seed - PRIME64_1;

      do {
        v1 += PlatformDependent.getLong(p) * PRIME64_2;
        p = p + 8;
        v1 = Long.rotateLeft(v1, 31);
        v1 *= PRIME64_1;

        v2 += PlatformDependent.getLong(p) * PRIME64_2;
        p = p + 8;
        v2 = Long.rotateLeft(v2, 31);
        v2 *= PRIME64_1;

        v3 += PlatformDependent.getLong(p) * PRIME64_2;
        p = p + 8;
        v3 = Long.rotateLeft(v3, 31);
        v3 *= PRIME64_1;

        v4 += PlatformDependent.getLong(p) * PRIME64_2;
        p = p + 8;
        v4 = Long.rotateLeft(v4, 31);
        v4 *= PRIME64_1;
      } while (p <= limit);

      h64 = Long.rotateLeft(v1, 1) + Long.rotateLeft(v2, 7) + Long.rotateLeft(v3, 12) + Long.rotateLeft(v4, 18);

      v1 *= PRIME64_2;
      v1 = Long.rotateLeft(v1, 31);
      v1 *= PRIME64_1;
      h64 ^= v1;

      h64 = h64 * PRIME64_1 + PRIME64_4;

      v2 *= PRIME64_2;
      v2 = Long.rotateLeft(v2, 31);
      v2 *= PRIME64_1;
      h64 ^= v2;

      h64 = h64 * PRIME64_1 + PRIME64_4;

      v3 *= PRIME64_2;
      v3 = Long.rotateLeft(v3, 31);
      v3 *= PRIME64_1;
      h64 ^= v3;

      h64 = h64 * PRIME64_1 + PRIME64_4;

      v4 *= PRIME64_2;
      v4 = Long.rotateLeft(v4, 31);
      v4 *= PRIME64_1;
      h64 ^= v4;

      h64 = h64 * PRIME64_1 + PRIME64_4;
    } else {
      h64 = seed + PRIME64_5;
    }

    h64 += len;

    while (p + 8 <= bEnd) {
      long k1 = PlatformDependent.getLong(p);
      k1 *= PRIME64_2;
      k1 = Long.rotateLeft(k1, 31);
      k1 *= PRIME64_1;
      h64 ^= k1;
      h64 = Long.rotateLeft(h64, 27) * PRIME64_1 + PRIME64_4;
      p += 8;
    }

    if (p + 4 <= bEnd) {
      //IMPORTANT: we are expecting a long from these 4 bytes. Which means it is always positive
      long finalInt = getIntLittleEndian(p);
      h64 ^= finalInt * PRIME64_1;
      h64 = Long.rotateLeft(h64, 23) * PRIME64_2 + PRIME64_3;
      p += 4;
    }
    while (p < bEnd) {
      h64 ^= ((long)(PlatformDependent.getByte(p) & 0x00ff)) * PRIME64_5;
      h64 = Long.rotateLeft(h64, 11) * PRIME64_1;
      p++;
    }

    return applyFinalHashComputation(h64);
  }

  private static long applyFinalHashComputation(long h64) {
    //IMPORTANT: using logical right shift instead of arithmetic right shift
    h64 ^= h64 >>> 33;
    h64 *= PRIME64_2;
    h64 ^= h64 >>> 29;
    h64 *= PRIME64_3;
    h64 ^= h64 >>> 32;
    return h64;
  }


  public static long hash64Internal(long val, long seed){
    long h64 = seed + PRIME64_5;
    h64 += 8; // add length (8 bytes) to hash value
    long k1 = val* PRIME64_2;
    k1 = Long.rotateLeft(k1, 31);
    k1 *= PRIME64_1;
    h64 ^= k1;
    h64 = Long.rotateLeft(h64, 27) * PRIME64_1 + PRIME64_4;
    return applyFinalHashComputation(h64);
  }

  /**
   * @param val the input 64 bit hash value
   * @return converted 32 bit hash value
   */
  private static int convert64To32(long val) {
    return (int) (val & 0x00FFFFFFFF);
  }


  public static long hash64(double val, long seed){
    return hash64Internal(Double.doubleToLongBits(val), seed);
  }

  public static long hash64(long start, long end, DrillBuf buffer, long seed){
    rangeCheck(buffer, (int)start, (int)end);

    long s = buffer.memoryAddress() + start;
    long e = buffer.memoryAddress() + end;

    return hash64bytes(s, e, seed);
  }

  public static int hash32(double val, long seed){
    return convert64To32(hash64(val, seed));
  }

  public static int hash32(int start, int end, DrillBuf buffer, int seed){
    return convert64To32(hash64(start, end, buffer, seed));
  }

}
