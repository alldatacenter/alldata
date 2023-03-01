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

package org.apache.uniffle.server;

import java.util.Map;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Summary;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShuffleServerGrpcMetricsTest {
  @Test
  public void testLatencyMetrics() {
    ShuffleServerGrpcMetrics metrics = new ShuffleServerGrpcMetrics();
    metrics.register(new CollectorRegistry(true));
    metrics.recordTransportTime(ShuffleServerGrpcMetrics.SEND_SHUFFLE_DATA_METHOD, 1000);
    metrics.recordTransportTime(ShuffleServerGrpcMetrics.GET_SHUFFLE_DATA_METHOD, 500);
    metrics.recordTransportTime(ShuffleServerGrpcMetrics.GET_MEMORY_SHUFFLE_DATA_METHOD, 200);
    metrics.recordProcessTime(ShuffleServerGrpcMetrics.SEND_SHUFFLE_DATA_METHOD, 1000);
    metrics.recordProcessTime(ShuffleServerGrpcMetrics.GET_SHUFFLE_DATA_METHOD, 500);
    metrics.recordProcessTime(ShuffleServerGrpcMetrics.GET_MEMORY_SHUFFLE_DATA_METHOD, 200);
    Map<String, Summary> sendTimeSummaryTime = metrics.getTransportTimeSummaryMap();
    Map<String, Summary> processTimeSummaryTime = metrics.getProcessTimeSummaryMap();
    assertEquals(3, sendTimeSummaryTime.size());
    assertEquals(3, processTimeSummaryTime.size());

    assertEquals(1D, sendTimeSummaryTime.get(
        ShuffleServerGrpcMetrics.SEND_SHUFFLE_DATA_METHOD).get().sum);
    assertEquals(0.5D, sendTimeSummaryTime.get(
        ShuffleServerGrpcMetrics.GET_SHUFFLE_DATA_METHOD).get().sum);
    assertEquals(0.2D, sendTimeSummaryTime.get(
        ShuffleServerGrpcMetrics.GET_MEMORY_SHUFFLE_DATA_METHOD).get().sum);

    assertEquals(1D, processTimeSummaryTime.get(
        ShuffleServerGrpcMetrics.SEND_SHUFFLE_DATA_METHOD).get().sum);
    assertEquals(0.5D, processTimeSummaryTime.get(
        ShuffleServerGrpcMetrics.GET_SHUFFLE_DATA_METHOD).get().sum);
    assertEquals(0.2D, processTimeSummaryTime.get(
        ShuffleServerGrpcMetrics.GET_MEMORY_SHUFFLE_DATA_METHOD).get().sum);
  }

}
