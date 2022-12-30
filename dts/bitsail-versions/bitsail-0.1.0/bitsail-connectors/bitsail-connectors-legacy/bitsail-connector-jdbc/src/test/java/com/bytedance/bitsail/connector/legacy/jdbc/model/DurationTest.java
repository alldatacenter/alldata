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

package com.bytedance.bitsail.connector.legacy.jdbc.model;

import org.junit.Test;

import java.math.BigInteger;
import java.util.Random;

import static com.bytedance.bitsail.connector.legacy.jdbc.model.Duration.DAYS_PER_MONTH_AVG;
import static com.bytedance.bitsail.connector.legacy.jdbc.model.Duration.HOURS_PER_DAY;
import static com.bytedance.bitsail.connector.legacy.jdbc.model.Duration.MICROSECONDS_PER_SECOND;
import static com.bytedance.bitsail.connector.legacy.jdbc.model.Duration.MINUTES_PER_HOUR;
import static com.bytedance.bitsail.connector.legacy.jdbc.model.Duration.MONTHS_PER_YEAR;
import static com.bytedance.bitsail.connector.legacy.jdbc.model.Duration.SECONDS_PER_MINUTE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DurationTest {

  private final double delta = 1e-8;
  private final Random random = new Random();

  @Test
  public void testDefaultDaysPerMonthAvg() {
    assertEquals(DAYS_PER_MONTH_AVG, Duration.builder().build().getDaysPerMonthAvg(), delta);
  }

  @Test
  public void testToMicros() {
    for (int i = 0; i < 100; i++) {
      final int years = random.nextInt(100);
      final int months = random.nextInt(100);
      final double daysPerMonthAvg = 30.5;
      final int days = random.nextInt(100);
      final int hours = random.nextInt(100);
      final int minutes = random.nextInt(100);
      final int seconds = random.nextInt(100);
      final int microseconds = random.nextInt(100);
      final BigInteger expected = BigInteger.valueOf((long) ((years * MONTHS_PER_YEAR + months) * daysPerMonthAvg + days))
              .multiply(HOURS_PER_DAY).add(BigInteger.valueOf(hours))
              .multiply(MINUTES_PER_HOUR).add(BigInteger.valueOf(minutes))
              .multiply(SECONDS_PER_MINUTE).add(BigInteger.valueOf(seconds))
              .multiply(MICROSECONDS_PER_SECOND).add(BigInteger.valueOf(microseconds));
      final Duration duration = Duration.builder()
              .years(years)
              .months(months)
              .daysPerMonthAvg(daysPerMonthAvg)
              .days(days)
              .hours(hours)
              .minutes(minutes)
              .seconds(seconds)
              .microseconds(microseconds)
              .build();
      assertEquals(expected, duration.toMicros());
      assertTrue(expected.compareTo(BigInteger.ZERO) >= 0);
    }
  }

  @Test
  public void testToMicrosWithYearsOnly() {
    for (int i = 0; i < 100; i++) {
      final int years = random.nextInt(100);
      final BigInteger expected = BigInteger.valueOf((long) (years * MONTHS_PER_YEAR * DAYS_PER_MONTH_AVG))
              .multiply(HOURS_PER_DAY).multiply(MINUTES_PER_HOUR).multiply(SECONDS_PER_MINUTE).multiply(MICROSECONDS_PER_SECOND);
      assertEquals(expected, Duration.builder().years(years).build().toMicros());
      assertTrue(expected.compareTo(BigInteger.ZERO) >= 0);
    }
  }

  @Test
  public void testToMicrosWithMonthsOnly() {
    for (int i = 0; i < 100; i++) {
      final int months = random.nextInt(100);
      final BigInteger expected = BigInteger.valueOf((long) (months * DAYS_PER_MONTH_AVG))
              .multiply(HOURS_PER_DAY).multiply(MINUTES_PER_HOUR).multiply(SECONDS_PER_MINUTE).multiply(MICROSECONDS_PER_SECOND);
      assertEquals(expected, Duration.builder().months(months).build().toMicros());
      assertTrue(expected.compareTo(BigInteger.ZERO) >= 0);
    }
  }

  @Test
  public void testToMicrosWithDaysOnly() {
    for (int i = 0; i < 100; i++) {
      final int days = random.nextInt(100);
      final BigInteger expected = BigInteger.valueOf(days)
              .multiply(HOURS_PER_DAY).multiply(MINUTES_PER_HOUR).multiply(SECONDS_PER_MINUTE).multiply(MICROSECONDS_PER_SECOND);
      assertEquals(expected, Duration.builder().days(days).build().toMicros());
      assertTrue(expected.compareTo(BigInteger.ZERO) >= 0);
    }
  }

  @Test
  public void testToMicrosWithHoursOnly() {
    for (int i = 0; i < 100; i++) {
      final int hours = random.nextInt(100);
      final BigInteger expected = BigInteger.valueOf(hours)
              .multiply(MINUTES_PER_HOUR).multiply(SECONDS_PER_MINUTE).multiply(MICROSECONDS_PER_SECOND);
      assertEquals(expected, Duration.builder().hours(hours).build().toMicros());
      assertTrue(expected.compareTo(BigInteger.ZERO) >= 0);
    }
  }

  @Test
  public void testToMicrosWithMinutesOnly() {
    for (int i = 0; i < 100; i++) {
      final int minutes = random.nextInt(100);
      final BigInteger expected = BigInteger.valueOf(minutes)
              .multiply(SECONDS_PER_MINUTE).multiply(MICROSECONDS_PER_SECOND);
      assertEquals(expected, Duration.builder().minutes(minutes).build().toMicros());
      assertTrue(expected.compareTo(BigInteger.ZERO) >= 0);
    }
  }

  @Test
  public void testToMicrosWithSecondsOnly() {
    for (int i = 0; i < 100; i++) {
      final int seconds = random.nextInt(100);
      final BigInteger expected = BigInteger.valueOf(seconds).multiply(MICROSECONDS_PER_SECOND);
      assertEquals(expected, Duration.builder().seconds(seconds).build().toMicros());
      assertTrue(expected.compareTo(BigInteger.ZERO) >= 0);
    }
  }

  @Test
  public void testToMicrosWithMicrosecondsOnly() {
    for (int i = 0; i < 100; i++) {
      final int microseconds = random.nextInt(100);
      final BigInteger expected = BigInteger.valueOf(microseconds);
      assertEquals(expected, Duration.builder().microseconds(microseconds).build().toMicros());
      assertTrue(expected.compareTo(BigInteger.ZERO) >= 0);
    }
  }
}
