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

import org.apache.drill.exec.expr.DrillSimpleFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.holders.TimeStampHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;

public class NearestDateFunctions {

  /**
   * This function takes two arguments, an input date object, and an interval and returns
   * the previous date that is the first date in that period.  This function is intended to be used in time series analysis to
   * aggregate by various units of time.
   * Usage is:
   * <p>
   * SELECT <date_field>, COUNT(*) AS event_count
   * FROM ...
   * GROUP BY nearestDate(`date_field`, 'QUARTER')
   * <p>
   * Currently supports the following time units:
   * <p>
   * YEAR
   * QUARTER
   * MONTH
   * WEEK_SUNDAY
   * WEEK_MONDAY
   * DAY
   * HOUR
   * HALF_HOUR
   * QUARTER_HOUR
   * MINUTE
   * 30SECOND
   * 15SECOND
   * SECOND
   */
  @FunctionTemplate(name = "nearestDate",
          scope = FunctionTemplate.FunctionScope.SIMPLE,
          nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class NearestDateFunction implements DrillSimpleFunc {

    @Param
    TimeStampHolder inputDate;

    @Param
    VarCharHolder interval;

    @Output
    TimeStampHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {

      String input = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(interval.start, interval.end, interval.buffer);
      java.time.LocalDateTime ld = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(inputDate.value), java.time.ZoneId.of("UTC"));

      java.time.LocalDateTime td = org.apache.drill.exec.udfs.NearestDateUtils.getDate(ld, input);
      out.value = td.atZone(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli();
    }
  }

  /**
   * This function takes three arguments, an input date string, an input date format string, and an interval and returns
   * the previous date that is the first date in that period.  This function is intended to be used in time series analysis to
   * aggregate by various units of time.
   * Usage is:
   * <p>
   * SELECT <date_field>, COUNT(*) AS event_count
   * FROM ...
   * GROUP BY nearestDate(`date_field`, 'yyyy-mm-dd', 'QUARTER')
   * <p>
   * Currently supports the following time units:
   * <p>
   * YEAR
   * QUARTER
   * MONTH
   * WEEK_SUNDAY
   * WEEK_MONDAY
   * DAY
   * HOUR
   * HALF_HOUR
   * QUARTER_HOUR
   * MINUTE
   * 30SECOND
   * 15SECOND
   * SECOND
   */
  @FunctionTemplate(name = "nearestDate",
          scope = FunctionTemplate.FunctionScope.SIMPLE,
          nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class NearestDateFunctionWithString implements DrillSimpleFunc {

    @Param
    VarCharHolder input;

    @Param
    VarCharHolder formatString;

    @Param
    VarCharHolder interval;

    @Output
    TimeStampHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      String inputDate = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(input.start, input.end, input.buffer);

      String format = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(formatString.start, formatString.end, formatString.buffer);

      String intervalString = org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(interval.start, interval.end, interval.buffer);

      java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern(format);
      java.time.LocalDateTime dateTime = java.time.LocalDateTime.parse(inputDate, formatter);

      java.time.LocalDateTime td = org.apache.drill.exec.udfs.NearestDateUtils.getDate(dateTime, intervalString);
      out.value = td.atZone(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli();
    }
  }
}
