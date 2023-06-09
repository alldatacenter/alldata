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
package org.apache.drill.exec.server;

import org.apache.drill.exec.server.rest.profile.SimpleDurationFormat;
import org.apache.drill.test.DrillTest;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test translation of millisecond durations into human readable format
 */
public class TestDurationFormat extends DrillTest {
  enum DurationFormat {
    COMPACT,
    VERBOSE
  }

  private void validateDurationFormat(long durationInMillisec, String expected, DurationFormat format) {
    String formatted = null;
    if (format.equals(DurationFormat.COMPACT)) {
      formatted = new SimpleDurationFormat(0, durationInMillisec).compact();
    }
    else if (format.equals(DurationFormat.VERBOSE)) {
      formatted = new SimpleDurationFormat(0, durationInMillisec).verbose();
    }
    assertEquals(formatted,expected);
  }

  @Test
  public void testCompactTwoDigitMilliSec() {
    validateDurationFormat(45, "0.045s", DurationFormat.COMPACT);
  }

  @Test
  public void testVerboseTwoDigitMilliSec() {
    validateDurationFormat(45, "0.045 sec", DurationFormat.VERBOSE);
  }

  @Test
  public void testCompactSecMillis() {
    validateDurationFormat(4545, "4.545s", DurationFormat.COMPACT);
  }

  @Test
  public void testVerboseSecMillis() {
    validateDurationFormat(4545, "4.545 sec", DurationFormat.VERBOSE);
  }

  @Test
  public void testCompactMinSec() {
    validateDurationFormat(454534, "7m34s", DurationFormat.COMPACT);
  }

  @Test
  public void testVerboseMinSec() {
    validateDurationFormat(454534, "07 min 34.534 sec", DurationFormat.VERBOSE);
  }

  @Test
  public void testCompactHourMin() {
    validateDurationFormat(4545342, "1h15m", DurationFormat.COMPACT);
  }

  @Test
  public void testVerboseHourMin() {
    validateDurationFormat(4545342, "1 hr 15 min 45.342 sec", DurationFormat.VERBOSE);
  }

  @Test
  public void testCompactHalfDayHourMin() {
    validateDurationFormat(45453420, "12h37m", DurationFormat.COMPACT);
  }

  @Test
  public void testVerboseHalfDayHourMin() {
    validateDurationFormat(45453420, "12 hr 37 min 33.420 sec", DurationFormat.VERBOSE);
  }

  @Test
  public void testCompactOneDayHourMin() {
    validateDurationFormat(45453420 + 86400000, "1d12h37m", DurationFormat.COMPACT);
  }

  @Test
  public void testVerboseOneDayHourMin() {
    validateDurationFormat(45453420 + 86400000, "1 day 12 hr 37 min 33.420 sec", DurationFormat.VERBOSE);
  }

  @Test
  public void testCompactManyDayHourMin() {
    validateDurationFormat(45453420 + 20*86400000, "20d12h37m", DurationFormat.COMPACT);
  }

  @Test
  public void testVerboseManyDayHourMin() {
    validateDurationFormat(45453420 + 20*86400000, "20 day 12 hr 37 min 33.420 sec", DurationFormat.VERBOSE);
  }
}
