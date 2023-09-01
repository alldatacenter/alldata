/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common.util;

import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UnitConverterTest {

  private static final long PB = (long)ByteUnit.PiB.toBytes(1L);
  private static final long TB = (long)ByteUnit.TiB.toBytes(1L);
  private static final long GB = (long)ByteUnit.GiB.toBytes(1L);
  private static final long MB = (long)ByteUnit.MiB.toBytes(1L);
  private static final long KB = (long)ByteUnit.KiB.toBytes(1L);

  private static Stream<Arguments> byteStringArgs() {
    return Stream.of(
        Arguments.arguments(10 * PB, "10PB", ByteUnit.BYTE),
        Arguments.arguments(10 * PB, "10pb", ByteUnit.BYTE),
        Arguments.arguments(10 * PB, "10pB", ByteUnit.BYTE),
        Arguments.arguments(10 * PB, "10p", ByteUnit.BYTE),
        Arguments.arguments(10 * PB, "10P", ByteUnit.BYTE),

        Arguments.arguments(10 * TB, "10TB", ByteUnit.BYTE),
        Arguments.arguments(10 * TB, "10tb", ByteUnit.BYTE),
        Arguments.arguments(10 * TB, "10tB", ByteUnit.BYTE),
        Arguments.arguments(10 * TB, "10T", ByteUnit.BYTE),
        Arguments.arguments(10 * TB, "10t", ByteUnit.BYTE),

        Arguments.arguments(10 * GB, "10GB", ByteUnit.BYTE),
        Arguments.arguments(10 * GB, "10gb", ByteUnit.BYTE),
        Arguments.arguments(10 * GB, "10gB", ByteUnit.BYTE),

        Arguments.arguments(10 * MB, "10MB", ByteUnit.BYTE),
        Arguments.arguments(10 * MB, "10mb", ByteUnit.BYTE),
        Arguments.arguments(10 * MB, "10mB", ByteUnit.BYTE),
        Arguments.arguments(10 * MB, "10M", ByteUnit.BYTE),
        Arguments.arguments(10 * MB, "10m", ByteUnit.BYTE),

        Arguments.arguments(10 * KB, "10KB", ByteUnit.BYTE),
        Arguments.arguments(10 * KB, "10kb", ByteUnit.BYTE),
        Arguments.arguments(10 * KB, "10Kb", ByteUnit.BYTE),
        Arguments.arguments(10 * KB, "10K", ByteUnit.BYTE),
        Arguments.arguments(10 * KB, "10k", ByteUnit.BYTE),

        Arguments.arguments(1111L, "1111", ByteUnit.BYTE),

        Arguments.arguments(null, "1/2", ByteUnit.BYTE),
        Arguments.arguments(null, "10f", ByteUnit.BYTE),
        Arguments.arguments(null, "f91", ByteUnit.BYTE),
        Arguments.arguments(null, "1.0", ByteUnit.BYTE)
    );
  }

  @ParameterizedTest
  @MethodSource("byteStringArgs")
  public void testByteString(Long expected, String value, ByteUnit unit) {
    if (expected == null) {
      assertThrows(NumberFormatException.class, () -> UnitConverter.byteStringAs(value, unit));
    } else {
      assertEquals(expected, UnitConverter.byteStringAs(value, unit));
    }
  }

  private static final long US = TimeUnit.MICROSECONDS.toMicros(1L);
  private static final long MS = TimeUnit.MILLISECONDS.toMicros(1L);
  private static final long SEC = TimeUnit.SECONDS.toMicros(1L);
  private static final long MIN = TimeUnit.MINUTES.toMicros(1L);
  private static final long HOUR = TimeUnit.HOURS.toMicros(1L);
  private static final long DAY = TimeUnit.DAYS.toMicros(1L);

  private static Stream<Arguments> timeStringArgs() {
    return Stream.of(
        Arguments.arguments(3 * US, "3us", TimeUnit.MICROSECONDS),
        Arguments.arguments(3 * MS, "3ms", TimeUnit.MICROSECONDS),
        Arguments.arguments(3 * SEC, "3s", TimeUnit.MICROSECONDS),
        Arguments.arguments(3 * MIN, "3m", TimeUnit.MICROSECONDS),
        Arguments.arguments(3 * MIN, "3min", TimeUnit.MICROSECONDS),
        Arguments.arguments(3 * HOUR, "3h", TimeUnit.MICROSECONDS),
        Arguments.arguments(3 * DAY, "3d", TimeUnit.MICROSECONDS),

        Arguments.arguments(5L, "5", TimeUnit.MILLISECONDS),
        Arguments.arguments(5L, "5", TimeUnit.SECONDS),
        Arguments.arguments(5L, "5", TimeUnit.MINUTES),

        Arguments.arguments(null, "3ns", TimeUnit.MICROSECONDS),
        Arguments.arguments(null, "3sec", TimeUnit.MICROSECONDS),
        Arguments.arguments(null, "1.5h", TimeUnit.MICROSECONDS),
        Arguments.arguments(null, "3hours", TimeUnit.MICROSECONDS),
        Arguments.arguments(null, "3days", TimeUnit.MICROSECONDS),
        Arguments.arguments(null, "3w", TimeUnit.MICROSECONDS),
        Arguments.arguments(null, "3weeks", TimeUnit.MICROSECONDS),
        Arguments.arguments(null, "foo", TimeUnit.MICROSECONDS)
    );
  }

  @ParameterizedTest
  @MethodSource("timeStringArgs")
  public void testTimeString(Long expected, String value, TimeUnit unit) {
    if (expected == null) {
      assertThrows(NumberFormatException.class, () -> UnitConverter.timeStringAs(value, unit));
    } else {
      assertEquals(expected, UnitConverter.timeStringAs(value, unit));
    }
  }
}
