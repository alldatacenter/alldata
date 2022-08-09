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
package org.apache.ambari.server.controller.metrics;

import static org.apache.ambari.server.controller.utilities.PropertyHelper.AGGREGATE_FUNCTION_IDENTIFIERS;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;

public class MetricsDataTransferMethodFactory {
  private static final Set<String> PERCENTAGE_METRIC;

  static {
    Set<String> percentMetrics = new HashSet<>();
    percentMetrics.add("cpu_wio");
    percentMetrics.add("cpu_idle");
    percentMetrics.add("cpu_nice");
    percentMetrics.add("cpu_aidle");
    percentMetrics.add("cpu_system");
    percentMetrics.add("cpu_user");

    Set<String> metricsWithAggregateFunctionIds = new HashSet<>();
    for (String metric : percentMetrics) {
      for (String aggregateFunctionId : AGGREGATE_FUNCTION_IDENTIFIERS) {
        if (!"._sum".equals(aggregateFunctionId)) {
          metricsWithAggregateFunctionIds.add(metric + aggregateFunctionId);
        }
      }
    }

    percentMetrics.addAll(metricsWithAggregateFunctionIds);

    PERCENTAGE_METRIC = Collections.unmodifiableSet(percentMetrics);
  }

  private static final MetricsDataTransferMethod percentageAdjustment = new PercentageAdjustmentTransferMethod();
  private static final MetricsDataTransferMethod passThrough = new PassThroughTransferMethod();

  public static MetricsDataTransferMethod detectDataTransferMethod(TimelineMetric metricDecl) {
    if (PERCENTAGE_METRIC.contains(metricDecl.getMetricName())) {
      return percentageAdjustment;
    } else {
      return passThrough;
    }
  }
}

class PercentageAdjustmentTransferMethod extends MetricsDataTransferMethod {

  @Override
  public Double getData(Double data) {
    return data < 100 ? data : data / 100 ;
  }
}

class PassThroughTransferMethod extends MetricsDataTransferMethod {

  @Override
  public Double getData(Double data) {
    return data;
  }
}
