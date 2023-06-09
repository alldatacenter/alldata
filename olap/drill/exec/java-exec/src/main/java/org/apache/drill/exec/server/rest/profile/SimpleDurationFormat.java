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
package org.apache.drill.exec.server.rest.profile;

import java.util.concurrent.TimeUnit;

/**
 * Representation of a millisecond duration in a human-readable format
 */
public class SimpleDurationFormat {
  private long days;
  private long hours;
  private long minutes;
  private long seconds;
  private long milliSeconds;
  private long durationInMillis;

  //Block creation of any default objects
  @SuppressWarnings("unused")
  private SimpleDurationFormat() {}

  /**
   * If end time is less than the start time, current epoch time is assumed as the end time.
   * @param startTimeMillis
   * @param endTimeMillis
   */
  public SimpleDurationFormat(long startTimeMillis, long endTimeMillis) {
    durationInMillis = (startTimeMillis > endTimeMillis ? System.currentTimeMillis() : endTimeMillis) - startTimeMillis;
    days = TimeUnit.MILLISECONDS.toDays(durationInMillis);
    hours = TimeUnit.MILLISECONDS.toHours(durationInMillis) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(durationInMillis));
    minutes = TimeUnit.MILLISECONDS.toMinutes(durationInMillis) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(durationInMillis));
    seconds = TimeUnit.MILLISECONDS.toSeconds(durationInMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationInMillis));
    milliSeconds = durationInMillis - TimeUnit.SECONDS.toMillis(TimeUnit.MILLISECONDS.toSeconds(durationInMillis));
  }

  /**
   * Return a compact representation of elapsed time with only the most significant time units and no spaces
   * @return duration
   */
  public String compact() {
    if (days >= 1) {
      return days + "d" + hours + "h" + minutes + "m";
    } else if (hours >= 1) {
      return hours + "h" + minutes + "m";
    } else if (minutes >= 1) {
      return minutes + "m" + seconds + "s";
    } else {
      return String.format("%.3fs", seconds + milliSeconds/1000.0);
    }
  }

  /**
   * Return a verbose representation of elapsed time down to millisecond granularity
   * @return duration
   */
  public String verbose() {
    return (days > 0 ? days + " day " : "") +
        ((hours + days) > 0 ? hours + " hr " : "") +
        ((minutes + hours + days) > 0 ? String.format("%02d min ", minutes) : "") +
        seconds + "." + String.format("%03d sec", milliSeconds);
  }
}
