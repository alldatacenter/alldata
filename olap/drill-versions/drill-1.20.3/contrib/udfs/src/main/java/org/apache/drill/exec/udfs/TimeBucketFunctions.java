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
import org.apache.drill.exec.expr.holders.BigIntHolder;
import org.apache.drill.exec.expr.holders.Float8Holder;
import org.apache.drill.exec.expr.holders.TimeStampHolder;


public class TimeBucketFunctions {

  /**
   * This function is used for facilitating time series analysis by creating buckets of time intervals.  See
   * https://blog.timescale.com/blog/simplified-time-series-analytics-using-the-time_bucket-function/ for usage. The function takes two arguments:
   * 1. The timestamp in nanoseconds
   * 2. The desired bucket interval IN MILLISECONDS
   *
   * The function returns a BIGINT of the nearest time bucket.
   */
  @FunctionTemplate(name = "time_bucket_ns",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class TimeBucketNSFunction implements DrillSimpleFunc {

    @Param
    BigIntHolder inputDate;

    @Param
    BigIntHolder interval;

    @Output
    BigIntHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      // Get the timestamp in nanoseconds
      long timestamp = inputDate.value;

      // Get the interval in milliseconds and convert to nanoseconds
      long groupByInterval = interval.value * 1000000;

      out.value = timestamp - (timestamp % groupByInterval);
    }
  }

  /**
   * This function is used for facilitating time series analysis by creating buckets of time intervals.  See
   * https://blog.timescale.com/blog/simplified-time-series-analytics-using-the-time_bucket-function/ for usage. The function takes two arguments:
   * 1. The timestamp in milliseconds
   * 2. The desired bucket interval IN milliseconds
   *
   * The function returns a BIGINT of the nearest time bucket.
   */
  @FunctionTemplate(name = "time_bucket",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class TimeBucketFunction implements DrillSimpleFunc {

    @Param
    BigIntHolder inputDate;

    @Param
    BigIntHolder interval;

    @Output
    BigIntHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      // Get the timestamp in milliseconds
      long timestamp = inputDate.value;

      // Get the interval in milliseconds
      long groupByInterval = interval.value;

      out.value = timestamp - (timestamp % groupByInterval);
    }
  }

  /**
   * This function is used for facilitating time series analysis by creating buckets of time intervals.  See
   * https://blog.timescale.com/blog/simplified-time-series-analytics-using-the-time_bucket-function/ for usage. The function takes two arguments:
   * 1. The timestamp (as a Drill timestamp)
   * 2. The desired bucket interval IN milliseconds
   *
   * The function returns a BIGINT of the nearest time bucket.
   */
  @FunctionTemplate(name = "time_bucket",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class TimestampTimeBucketFunction implements DrillSimpleFunc {

    @Param
    TimeStampHolder inputDate;

    @Param
    BigIntHolder interval;

    @Output
    TimeStampHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      // Get the timestamp in milliseconds
      long timestamp = inputDate.value;

      // Get the interval in milliseconds
      long groupByInterval = interval.value;

      out.value = (timestamp - (timestamp % groupByInterval));
    }
  }

  /**
   * This function is used for facilitating time series analysis by creating buckets of time intervals.  See
   * https://blog.timescale.com/blog/simplified-time-series-analytics-using-the-time_bucket-function/ for usage. The function takes two arguments:
   * 1. The timestamp (as a Drill timestamp)
   * 2. The desired bucket interval IN milliseconds
   *
   * The function returns a BIGINT of the nearest time bucket.
   */
  @FunctionTemplate(name = "time_bucket",
    scope = FunctionTemplate.FunctionScope.SIMPLE,
    nulls = FunctionTemplate.NullHandling.NULL_IF_NULL)
  public static class DoubleTimeBucketFunction implements DrillSimpleFunc {

    @Param
    Float8Holder inputDate;

    @Param
    BigIntHolder interval;

    @Output
    BigIntHolder out;

    @Override
    public void setup() {
    }

    @Override
    public void eval() {
      // Get the timestamp in milliseconds
      long timestamp = java.lang.Math.round(inputDate.value);

      // Get the interval in milliseconds
      long groupByInterval = interval.value;

      out.value = timestamp - (timestamp % groupByInterval);
    }
  }
}
