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

package org.apache.drill.exec.udfs;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.TemporalAdjusters;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

public class NearestDateUtils {
  /**
   * Specifies the time grouping to be used with the nearest date function
   */
  protected enum TimeInterval {
    YEAR,
    QUARTER,
    MONTH,
    WEEK_SUNDAY,
    WEEK_MONDAY,
    DAY,
    HOUR,
    HALF_HOUR,
    QUARTER_HOUR,
    MINUTE,
    HALF_MINUTE,
    QUARTER_MINUTE,
    SECOND
  }

  private static final Logger logger = LoggerFactory.getLogger(NearestDateUtils.class);

  /**
   * This function takes a Java LocalDateTime object, and an interval string and returns
   * the nearest date closets to that time.  For instance, if you specified the date as 2018-05-04 and YEAR, the function
   * will return 2018-01-01
   *
   * @param d        the original datetime before adjustments
   * @param interval The interval string to deduct from the supplied date
   * @return the modified LocalDateTime
   */
  public final static java.time.LocalDateTime getDate(java.time.LocalDateTime d, String interval) {
    java.time.LocalDateTime newDate = d;
    int year = d.getYear();
    int month = d.getMonth().getValue();
    int day = d.getDayOfMonth();
    int hour = d.getHour();
    int minute = d.getMinute();
    int second = d.getSecond();
    TimeInterval adjustmentAmount;
    try {
      adjustmentAmount = TimeInterval.valueOf(interval.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new DrillRuntimeException(String.format("[%s] is not a valid time statement. Expecting: %s", interval, Arrays.asList(TimeInterval.values())));
    }
    switch (adjustmentAmount) {
      case YEAR:
        newDate = LocalDateTime.of(year, 1, 1, 0, 0, 0);
        break;
      case QUARTER:
        newDate = LocalDateTime.of(year, ((month - 1) / 3) * 3 + 1, 1, 0, 0, 0);
        break;
      case MONTH:
        newDate = LocalDateTime.of(year, month, 1, 0, 0, 0);
        break;
      case WEEK_SUNDAY:
        newDate = newDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
                .truncatedTo(ChronoUnit.DAYS);
        break;
      case WEEK_MONDAY:
        newDate = newDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .truncatedTo(ChronoUnit.DAYS);
        break;
      case DAY:
        newDate = LocalDateTime.of(year, month, day, 0, 0, 0);
        break;
      case HOUR:
        newDate = LocalDateTime.of(year, month, day, hour, 0, 0);
        break;
      case HALF_HOUR:
        if (minute >= 30) {
          minute = 30;
        } else {
          minute = 0;
        }
        newDate = LocalDateTime.of(year, month, day, hour, minute, 0);
        break;
      case QUARTER_HOUR:
        if (minute >= 45) {
          minute = 45;
        } else if (minute >= 30) {
          minute = 30;
        } else if (minute >= 15) {
          minute = 15;
        } else {
          minute = 0;
        }
        newDate = LocalDateTime.of(year, month, day, hour, minute, 0);
        break;
      case MINUTE:
        newDate = LocalDateTime.of(year, month, day, hour, minute, 0);
        break;
      case HALF_MINUTE:
        if (second >= 30) {
          second = 30;
        } else {
          second = 0;
        }
        newDate = LocalDateTime.of(year, month, day, hour, minute, second);
        break;
      case QUARTER_MINUTE:
        if (second >= 45) {
          second = 45;
        } else if (second >= 30) {
          second = 30;
        } else if (second >= 15) {
          second = 15;
        } else {
          second = 0;
        }
        newDate = LocalDateTime.of(year, month, day, hour, minute, second);
        break;
      case SECOND:
        newDate = LocalDateTime.of(year, month, day, hour, minute, second);
        break;
      default:
        throw new DrillRuntimeException(String.format("[%s] is not a valid time statement. Expecting: %s", interval, Arrays.asList(TimeInterval.values())));
    }
    return newDate;
  }
}
