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

import com.google.common.annotations.VisibleForTesting;
import lombok.Builder;
import lombok.Getter;

import java.math.BigInteger;

/**
 * A time-based amount of time. This class models a quantity or amount of time by parsing time units including years,
 * months, days, hours, minutes, seconds, and microseconds.
 */
@Builder
public class Duration {
  @VisibleForTesting
  static final double DAYS_PER_MONTH_AVG = 365.25 / 12.0d;

  @VisibleForTesting
  static final int MONTHS_PER_YEAR = 12;

  @VisibleForTesting
  static final BigInteger HOURS_PER_DAY = BigInteger.valueOf(24);

  @VisibleForTesting
  static final BigInteger MINUTES_PER_HOUR = BigInteger.valueOf(60);

  @VisibleForTesting
  static final BigInteger SECONDS_PER_MINUTE = BigInteger.valueOf(60);

  @VisibleForTesting
  static final BigInteger MICROSECONDS_PER_SECOND = BigInteger.valueOf(1_000_000L);

  private final int years;
  private final int months;
  private final int days;
  private final int hours;
  private final int minutes;
  private final int seconds;
  private final int microseconds;

  @Builder.Default
  @Getter
  private double daysPerMonthAvg = DAYS_PER_MONTH_AVG;

  /**
   * Converts this duration to the total length in microseconds.
   *
   * @return the total length of the duration in microseconds
   */
  public BigInteger toMicros() {
    long numberOfMonths = years * MONTHS_PER_YEAR + months;
    BigInteger numberOfDays = BigInteger.valueOf((long) (numberOfMonths * daysPerMonthAvg) + days);
    BigInteger numberOfHours = numberOfDays.multiply(HOURS_PER_DAY).add(BigInteger.valueOf(hours));
    BigInteger numberOfMinutes = numberOfHours.multiply(MINUTES_PER_HOUR).add(BigInteger.valueOf(minutes));
    BigInteger numberOfSeconds = numberOfMinutes.multiply(SECONDS_PER_MINUTE).add(BigInteger.valueOf(seconds));
    return numberOfSeconds.multiply(MICROSECONDS_PER_SECOND).add(BigInteger.valueOf(microseconds));
  }
}
