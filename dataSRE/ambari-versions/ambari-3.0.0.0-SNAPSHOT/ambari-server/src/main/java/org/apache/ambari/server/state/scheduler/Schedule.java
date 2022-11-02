/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.state.scheduler;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class Schedule {
  private String minutes;
  private String hours;
  private String daysOfMonth;
  private String month;
  private String dayOfWeek;
  private String year;
  private String startTime;
  private String endTime;

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("minutes")
  public String getMinutes() {
    return minutes;
  }

  public void setMinutes(String minutes) {
    this.minutes = minutes;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("hours")
  public String getHours() {
    return hours;
  }

  public void setHours(String hours) {
    this.hours = hours;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("days_of_month")
  public String getDaysOfMonth() {
    return daysOfMonth;
  }

  public void setDaysOfMonth(String daysOfMonth) {
    this.daysOfMonth = daysOfMonth;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("month")
  public String getMonth() {
    return month;
  }

  public void setMonth(String month) {
    this.month = month;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("day_of_week")
  public String getDayOfWeek() {
    return dayOfWeek;
  }

  public void setDayOfWeek(String dayOfWeek) {
    this.dayOfWeek = dayOfWeek;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("year")
  public String getYear() {
    return year;
  }

  public void setYear(String year) {
    this.year = year;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("start_time")
  public String getStartTime() {
    return startTime;
  }

  public void setStartTime(String startTime) {
    this.startTime = startTime;
  }

  @JsonSerialize(include = JsonSerialize.Inclusion.NON_EMPTY)
  @JsonProperty("end_time")
  public String getEndTime() {
    return endTime;
  }

  public void setEndTime(String endTime) {
    this.endTime = endTime;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Schedule schedule = (Schedule) o;

    if (dayOfWeek != null ? !dayOfWeek.equals(schedule.dayOfWeek) : schedule.dayOfWeek != null)
      return false;
    if (daysOfMonth != null ? !daysOfMonth.equals(schedule.daysOfMonth) : schedule.daysOfMonth != null)
      return false;
    if (endTime != null ? !endTime.equals(schedule.endTime) : schedule.endTime != null)
      return false;
    if (hours != null ? !hours.equals(schedule.hours) : schedule.hours != null)
      return false;
    if (minutes != null ? !minutes.equals(schedule.minutes) : schedule.minutes != null)
      return false;
    if (month != null ? !month.equals(schedule.month) : schedule.month != null)
      return false;
    if (startTime != null ? !startTime.equals(schedule.startTime) : schedule.startTime != null)
      return false;
    if (year != null ? !year.equals(schedule.year) : schedule.year != null)
      return false;

    return true;
  }

  /**
   * Return empty if schedule has non usable fields.
   * @return
   */
  @JsonIgnore
  public boolean isEmpty() {
    return (minutes == null || minutes.isEmpty())
      && (hours == null || hours.isEmpty())
      && (dayOfWeek == null || dayOfWeek.isEmpty())
      && (daysOfMonth == null || daysOfMonth.isEmpty())
      && (month == null || month.isEmpty())
      && (year == null || year.isEmpty())
      && (startTime == null || startTime.isEmpty())
      && (endTime == null || endTime.isEmpty());
  }

  /**
   * Return a cron expression from the schedule fields.
   * Example: "0 0 12 * * ?"
   * @return
   */
  @JsonIgnore
  public String getScheduleExpression() {
    StringBuilder expression = new StringBuilder();
    expression.append("0"); // seconds
    expression.append(" ");
    expression.append(minutes);
    expression.append(" ");
    expression.append(hours);
    expression.append(" ");
    expression.append(daysOfMonth);
    expression.append(" ");
    expression.append(month);
    expression.append(" ");
    expression.append(dayOfWeek);
    if (year != null && !year.isEmpty()) {
      expression.append(" ");
      expression.append(year);
    }
    return expression.toString();
  }

  @Override
  public int hashCode() {
    int result = minutes != null ? minutes.hashCode() : 0;
    result = 31 * result + (hours != null ? hours.hashCode() : 0);
    result = 31 * result + (daysOfMonth != null ? daysOfMonth.hashCode() : 0);
    result = 31 * result + (month != null ? month.hashCode() : 0);
    result = 31 * result + (dayOfWeek != null ? dayOfWeek.hashCode() : 0);
    result = 31 * result + (year != null ? year.hashCode() : 0);
    result = 31 * result + (startTime != null ? startTime.hashCode() : 0);
    result = 31 * result + (endTime != null ? endTime.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "Schedule {" +
      "minutes='" + minutes + '\'' +
      ", hours='" + hours + '\'' +
      ", days_of_month='" + daysOfMonth + '\'' +
      ", month='" + month + '\'' +
      ", day_of_week='" + dayOfWeek + '\'' +
      ", year='" + year + '\'' +
      ", startTime='" + startTime + '\'' +
      ", endTime='" + endTime + '\'' +
      '}';
  }
}
