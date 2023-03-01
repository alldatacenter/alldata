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

package org.apache.uniffle.common.metrics;

import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import org.junit.jupiter.api.Test;

import static io.prometheus.client.Collector.MetricFamilySamples;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetricsManagerTest {

  @Test
  public void testMetricsManager() {
    MetricsManager metricsManager = new MetricsManager();
    assertEquals(CollectorRegistry.defaultRegistry, metricsManager.getCollectorRegistry());

    CollectorRegistry expectedRegistry = new CollectorRegistry();
    metricsManager = new MetricsManager(expectedRegistry);
    assertEquals(expectedRegistry, metricsManager.getCollectorRegistry());

    String expectedName1 = "counter";
    metricsManager.addCounter(expectedName1);

    String expectedName2 = "name2";
    String label = "gaugeLabel";
    Gauge gauge = metricsManager.addGauge(expectedName2, label);
    gauge.labels("lv1").inc();
    gauge.labels("lv2").inc();

    Map<String, MetricFamilySamples> metricsSamples = new HashMap<>();
    Enumeration<MetricFamilySamples> mfs = expectedRegistry.metricFamilySamples();
    while (mfs.hasMoreElements()) {
      MetricFamilySamples cur = mfs.nextElement();
      metricsSamples.put(cur.name, cur);
    }

    String expectedHelp1 = "Counter " + expectedName1;
    assertEquals(expectedHelp1, metricsSamples.get(expectedName1).help);
    assertEquals(1, metricsSamples.get(expectedName1).samples.size());

    String expectedHelp2 = "Gauge " + expectedName2;
    assertEquals(expectedHelp2, metricsSamples.get(expectedName2).help);
    List<MetricFamilySamples.Sample> f = metricsSamples.get(expectedName2).samples;
    assertEquals(2, metricsSamples.get(expectedName2).samples.size());
    String[] actualLabelValues = metricsSamples
        .get(expectedName2).samples
        .stream().map(i -> i.labelValues.get(0))
        .collect(Collectors.toList()).toArray(new String[0]);
    Arrays.sort(actualLabelValues);
    assertArrayEquals(new String[]{"lv1", "lv2"}, actualLabelValues);
  }
}
