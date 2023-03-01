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

package org.apache.uniffle.common.config;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ConfigUtilsTest {

  private enum Ternary {
    TRUE, FALSE, UNKNOWN
  }

  private static Stream<Arguments> convertValueArgs() {
    return Stream.of(
        Arguments.arguments(1, Integer.class),
        Arguments.arguments(2L, Long.class),
        Arguments.arguments(true, Boolean.class),
        Arguments.arguments(1.0f, Float.class),
        Arguments.arguments(2.0, Double.class),
        Arguments.arguments("foo", String.class),
        Arguments.arguments(Ternary.FALSE, Ternary.class)
    );
  }

  @ParameterizedTest
  @MethodSource("convertValueArgs")
  public void testConvertValue(Object rawValue, Class<?> clazz) {
    assertEquals(rawValue, ConfigUtils.convertValue(rawValue, clazz));
  }

  @Test
  public void testConvertValueWithUnsupportedType() {
    assertThrows(IllegalArgumentException.class, () -> ConfigUtils.convertValue(null, Object.class));
  }

  private static Stream<Arguments> toStringArgs() {
    return Stream.of(
        Arguments.arguments("foo", "foo"),
        Arguments.arguments(123, "123"),
        Arguments.arguments(Ternary.TRUE, "TRUE")
    );
  }

  @ParameterizedTest
  @MethodSource("toStringArgs")
  public void testConvertToString(Object value, String expected) {
    assertEquals(expected, ConfigUtils.convertToString(value));
  }

  private static Stream<Arguments> toIntArgs() {
    final long maxInt = Integer.MAX_VALUE;
    final long minInt = Integer.MIN_VALUE;
    return Stream.of(
        Arguments.arguments(Integer.MAX_VALUE, Integer.MAX_VALUE),
        Arguments.arguments(Integer.MIN_VALUE, Integer.MIN_VALUE),
        Arguments.arguments(maxInt, Integer.MAX_VALUE),
        Arguments.arguments(minInt, Integer.MIN_VALUE),
        Arguments.arguments(maxInt + 1, null),
        Arguments.arguments(minInt - 1, null),
        Arguments.arguments("123", 123),
        Arguments.arguments("2.0", null),
        Arguments.arguments("foo", null)
    );
  }

  @ParameterizedTest
  @MethodSource("toIntArgs")
  public void testConvertToInt(Object value, Integer expected) {
    if (expected == null) {
      assertThrows(IllegalArgumentException.class, () -> ConfigUtils.convertToInt(value));
    } else {
      assertEquals(expected, ConfigUtils.convertToInt(value));
    }
  }

  private static Stream<Arguments> toLongArgs() {
    return Stream.of(
        Arguments.arguments(Integer.MAX_VALUE, (long) Integer.MAX_VALUE),
        Arguments.arguments(Integer.MIN_VALUE, (long) Integer.MIN_VALUE),
        Arguments.arguments(Long.MAX_VALUE, Long.MAX_VALUE),
        Arguments.arguments(Long.MIN_VALUE, Long.MIN_VALUE),
        Arguments.arguments("123", 123L),
        Arguments.arguments("7B", 7L),
        Arguments.arguments("6KB", 6L << 10),
        Arguments.arguments("5MB", 5L << 20),
        Arguments.arguments("4GB", 4L << 30),
        Arguments.arguments("3TB", 3L << 40),
        Arguments.arguments("2PB", 2L << 50),
        Arguments.arguments("2.0", null),
        Arguments.arguments("foo", null)
    );
  }

  @ParameterizedTest
  @MethodSource("toLongArgs")
  public void testConvertToLong(Object value, Long expected) {
    if (expected == null) {
      assertThrows(IllegalArgumentException.class, () -> ConfigUtils.convertToLong(value));
    } else {
      assertEquals(expected, ConfigUtils.convertToLong(value));
    }
  }

  private static Stream<Arguments> toSizeArgs() {
    return Stream.of(
        Arguments.arguments(0, 0),
        Arguments.arguments(12345L, 12345),
        Arguments.arguments("100", 100),
        Arguments.arguments("100b", 100),
        Arguments.arguments("2K", 2L << 10),
        Arguments.arguments("2Kb", 2L << 10),
        Arguments.arguments("3M", 3L << 20),
        Arguments.arguments("3mB", 3L << 20),
        Arguments.arguments("4G", 4L << 30),
        Arguments.arguments("4GB", 4L << 30),
        Arguments.arguments("5T", 5L << 40),
        Arguments.arguments("5tb", 5L << 40),
        Arguments.arguments("6P", 6L << 50),
        Arguments.arguments("6PB", 6L << 50)
    );
  }

  @ParameterizedTest
  @MethodSource("toSizeArgs")
  public void testConvertToSizeInBytes(Object size, long expected) {
    assertEquals(expected, ConfigUtils.convertToSizeInBytes(size));
    if (size instanceof String) {
      assertEquals(expected, ConfigUtils.convertToSizeInBytes(((String) size).toLowerCase()));
      assertEquals(expected, ConfigUtils.convertToSizeInBytes(((String) size).toUpperCase()));
    }
  }

  private static Stream<Arguments> toBooleanArgs() {
    return Stream.of(
        Arguments.arguments(true, true),
        Arguments.arguments("true", true),
        Arguments.arguments("True", true),
        Arguments.arguments("TRUE", true),
        Arguments.arguments("tRuE", true),
        Arguments.arguments(Ternary.TRUE, true),
        Arguments.arguments(false, false),
        Arguments.arguments("false", false),
        Arguments.arguments("False", false),
        Arguments.arguments("FALSE", false),
        Arguments.arguments("fAlsE", false),
        Arguments.arguments(Ternary.FALSE, false),
        Arguments.arguments(Ternary.UNKNOWN, null)
    );
  }

  @ParameterizedTest
  @MethodSource("toBooleanArgs")
  public void testConvertToBoolean(Object value, Boolean expected) {
    if (expected == null) {
      assertThrows(IllegalArgumentException.class, () -> ConfigUtils.convertToBoolean(Ternary.UNKNOWN));
    } else {
      assertEquals(expected, ConfigUtils.convertToBoolean(value));
    }
  }

  private static Stream<Arguments> toFloatArgs() {
    final double maxFloat = Float.MAX_VALUE;
    final double minFloat = Float.MIN_VALUE;
    return Stream.of(
        Arguments.arguments(0.0f, 0.0f),
        Arguments.arguments(Float.MAX_VALUE, Float.MAX_VALUE),
        Arguments.arguments(Float.MIN_VALUE, Float.MIN_VALUE),
        Arguments.arguments(-Float.MAX_VALUE, -Float.MAX_VALUE),
        Arguments.arguments(-Float.MIN_VALUE, -Float.MIN_VALUE),
        Arguments.arguments(0.0d, 0.0f),
        Arguments.arguments(maxFloat, Float.MAX_VALUE),
        Arguments.arguments(minFloat, Float.MIN_VALUE),
        Arguments.arguments(-maxFloat, -Float.MAX_VALUE),
        Arguments.arguments(-minFloat, -Float.MIN_VALUE),
        Arguments.arguments(maxFloat * 1.1, null),
        Arguments.arguments(minFloat * 0.9, null),
        Arguments.arguments(-maxFloat * 1.1, null),
        Arguments.arguments(-minFloat * 0.9, null),
        Arguments.arguments("123", 123.0f),
        Arguments.arguments("123.45", 123.45f),
        Arguments.arguments("foo", null)
    );
  }

  @ParameterizedTest
  @MethodSource("toFloatArgs")
  public void testConvertToFloat(Object value, Float expected) {
    if (expected == null) {
      assertThrows(IllegalArgumentException.class, () -> ConfigUtils.convertToFloat(value));
    } else {
      assertEquals(expected, ConfigUtils.convertToFloat(value));
    }
  }

  private static Stream<Arguments> toDoubleArgs() {
    return Stream.of(
        Arguments.arguments(0.0f, 0.0),
        Arguments.arguments(Float.MAX_VALUE, (double) Float.MAX_VALUE),
        Arguments.arguments(Float.MIN_VALUE, (double) Float.MIN_VALUE),
        Arguments.arguments(-Float.MAX_VALUE, (double) -Float.MAX_VALUE),
        Arguments.arguments(-Float.MIN_VALUE, (double) -Float.MIN_VALUE),
        Arguments.arguments(0.0d, 0.0),
        Arguments.arguments(Double.MAX_VALUE, Double.MAX_VALUE),
        Arguments.arguments(Double.MIN_VALUE, Double.MIN_VALUE),
        Arguments.arguments(-Double.MAX_VALUE, -Double.MAX_VALUE),
        Arguments.arguments(-Double.MIN_VALUE, -Double.MIN_VALUE),
        Arguments.arguments("123", 123.0),
        Arguments.arguments("123.45", 123.45),
        Arguments.arguments("foo", null)
    );
  }

  @ParameterizedTest
  @MethodSource("toDoubleArgs")
  public void testConvertToDouble(Object value, Double expected) {
    if (expected == null) {
      assertThrows(IllegalArgumentException.class, () -> ConfigUtils.convertToDouble(value));
    } else {
      assertEquals(expected, ConfigUtils.convertToDouble(value));
    }
  }

  @Test
  public void testGetAllConfigOptions() {
    assertFalse(ConfigUtils.getAllConfigOptions(RssBaseConf.class).isEmpty());
  }

  @ParameterizedTest
  @ValueSource(longs = {0, 1, 2, -1, Long.MIN_VALUE, Long.MAX_VALUE})
  public void testPositiveLongValidator(long value) {
    assertEquals(value > 0, ConfigUtils.POSITIVE_LONG_VALIDATOR.apply(value));
  }

  @ParameterizedTest
  @ValueSource(longs = {0, 1, 2, -1, Long.MIN_VALUE, Long.MAX_VALUE})
  public void testNonNegativeLongValidator(long value) {
    assertEquals(value >= 0, ConfigUtils.NON_NEGATIVE_LONG_VALIDATOR.apply(value));
  }

  @ParameterizedTest
  @ValueSource(longs = {0, 1, 2, -1, Integer.MIN_VALUE, Integer.MAX_VALUE, Long.MIN_VALUE, Long.MAX_VALUE})
  public void testPositiveIntegerValidator(long value) {
    assertEquals(value > 0 && value <= Integer.MAX_VALUE, ConfigUtils.POSITIVE_INTEGER_VALIDATOR.apply(value));
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 2, -1, Integer.MIN_VALUE, Integer.MAX_VALUE})
  public void testPositiveIntegerValidator2(int value) {
    assertEquals(value > 0, ConfigUtils.POSITIVE_INTEGER_VALIDATOR_2.apply(value));
  }

  @ParameterizedTest
  @ValueSource(doubles = {-1.0, -0.01, 0.0, 1.5, 50.2, 99.9, 100.0, 100.01, 101.0})
  public void testPercentageDoubleValidator(double value) {
    assertEquals(value >= 0.0 && value <= 100.0, ConfigUtils.PERCENTAGE_DOUBLE_VALIDATOR.apply(value));
  }

}
