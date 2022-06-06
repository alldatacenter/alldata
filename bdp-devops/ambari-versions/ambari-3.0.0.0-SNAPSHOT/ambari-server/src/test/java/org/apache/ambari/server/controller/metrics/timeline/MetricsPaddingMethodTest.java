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
package org.apache.ambari.server.controller.metrics.timeline;

import java.util.TreeMap;

import org.apache.ambari.server.controller.metrics.MetricsPaddingMethod;
import org.apache.ambari.server.controller.spi.TemporalInfo;
import org.apache.hadoop.metrics2.sink.timeline.TimelineMetric;
import org.junit.Test;

import junit.framework.Assert;

public class MetricsPaddingMethodTest {

  @Test
  public void testPaddingWithNulls() throws Exception {
    MetricsPaddingMethod paddingMethod =
      new MetricsPaddingMethod(MetricsPaddingMethod.PADDING_STRATEGY.NULLS);

    long now = System.currentTimeMillis();

    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("m1");
    timelineMetric.setHostName("h1");
    timelineMetric.setAppId("a1");
    timelineMetric.setStartTime(now);
    TreeMap<Long, Double> inputValues = new TreeMap<>();
    inputValues.put(now - 1000, 1.0d);
    inputValues.put(now - 2000, 2.0d);
    inputValues.put(now - 3000, 3.0d);
    timelineMetric.setMetricValues(inputValues);

    TemporalInfo temporalInfo = getTemporalInfo(now - 10000, now, 1l);
    paddingMethod.applyPaddingStrategy(timelineMetric, temporalInfo);
    TreeMap<Long, Double> values = (TreeMap<Long, Double>) timelineMetric.getMetricValues();

    Assert.assertEquals(11, values.size());
    Assert.assertEquals(new Long(now - 10000), values.keySet().iterator().next());
    Assert.assertEquals(new Long(now), values.descendingKeySet().iterator().next());
    Assert.assertEquals(null, values.values().iterator().next());
  }

  @Test
  public void testPaddingWithZeros() throws Exception {
    MetricsPaddingMethod paddingMethod =
      new MetricsPaddingMethod(MetricsPaddingMethod.PADDING_STRATEGY.ZEROS);

    long now = System.currentTimeMillis();

    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("m1");
    timelineMetric.setHostName("h1");
    timelineMetric.setAppId("a1");
    timelineMetric.setStartTime(now);
    TreeMap<Long, Double> inputValues = new TreeMap<>();
    inputValues.put(now - 1000, 1.0d);
    inputValues.put(now - 2000, 2.0d);
    inputValues.put(now - 3000, 3.0d);
    timelineMetric.setMetricValues(inputValues);

    TemporalInfo temporalInfo = getTemporalInfo(now - 10000, now, 1l);
    paddingMethod.applyPaddingStrategy(timelineMetric, temporalInfo);
    TreeMap<Long, Double> values = (TreeMap<Long, Double>) timelineMetric.getMetricValues();

    Assert.assertEquals(11, values.size());
    Assert.assertEquals(new Long(now - 10000), values.keySet().iterator().next());
    Assert.assertEquals(new Long(now), values.descendingKeySet().iterator().next());
    Assert.assertEquals(0.0, values.values().iterator().next());
  }

  @Test
  public void testPaddingWithNoPaddingNeeded() throws Exception {
    MetricsPaddingMethod paddingMethod =
      new MetricsPaddingMethod(MetricsPaddingMethod.PADDING_STRATEGY.ZEROS);

    long now = System.currentTimeMillis();

    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("m1");
    timelineMetric.setHostName("h1");
    timelineMetric.setAppId("a1");
    timelineMetric.setStartTime(now);
    TreeMap<Long, Double> inputValues = new TreeMap<>();
    inputValues.put(now, 0.0d);
    inputValues.put(now - 1000, 1.0d);
    inputValues.put(now - 2000, 2.0d);
    inputValues.put(now - 3000, 3.0d);
    timelineMetric.setMetricValues(inputValues);

    TemporalInfo temporalInfo = getTemporalInfo(now - 3000, now, 1l);
    paddingMethod.applyPaddingStrategy(timelineMetric, temporalInfo);
    TreeMap<Long, Double> values = (TreeMap<Long, Double>) timelineMetric.getMetricValues();

    Assert.assertEquals(4, values.size());
    Assert.assertEquals(new Long(now - 3000), values.keySet().iterator().next());
    Assert.assertEquals(new Long(now), values.descendingKeySet().iterator().next());
  }

  @Test
  public void testPaddingWithStepProvided() throws Exception {
    MetricsPaddingMethod paddingMethod =
      new MetricsPaddingMethod(MetricsPaddingMethod.PADDING_STRATEGY.ZEROS);

    long now = System.currentTimeMillis();

    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("m1");
    timelineMetric.setHostName("h1");
    timelineMetric.setAppId("a1");
    timelineMetric.setStartTime(now);
    TreeMap<Long, Double> inputValues = new TreeMap<>();
    inputValues.put(now - 1000, 1.0d);
    timelineMetric.setMetricValues(inputValues);

    TemporalInfo temporalInfo = getTemporalInfo(now - 10000, now, 1000l);
    paddingMethod.applyPaddingStrategy(timelineMetric, temporalInfo);
    TreeMap<Long, Double> values = (TreeMap<Long, Double>) timelineMetric.getMetricValues();

    Assert.assertEquals(11, values.size());
    Assert.assertEquals(new Long(now - 10000), values.keySet().iterator().next());
    Assert.assertEquals(new Long(now), values.descendingKeySet().iterator().next());
    Assert.assertEquals(0.0, values.values().iterator().next());
  }

  @Test
  public void testPaddingWithOneValue() throws Exception {
    MetricsPaddingMethod paddingMethod =
      new MetricsPaddingMethod(MetricsPaddingMethod.PADDING_STRATEGY.ZEROS);

    long now = System.currentTimeMillis();

    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("m1");
    timelineMetric.setHostName("h1");
    timelineMetric.setAppId("a1");
    timelineMetric.setStartTime(now);
    TreeMap<Long, Double> inputValues = new TreeMap<>();
    inputValues.put(now - 1000, 1.0d);
    timelineMetric.setMetricValues(inputValues);

    TemporalInfo temporalInfo = getTemporalInfo(now - 10000, now, null);
    paddingMethod.applyPaddingStrategy(timelineMetric, temporalInfo);
    TreeMap<Long, Double> values = (TreeMap<Long, Double>) timelineMetric.getMetricValues();

    Assert.assertEquals(1, values.size());
    Assert.assertEquals(new Long(now - 1000), values.keySet().iterator().next());
    Assert.assertEquals(1.0, values.values().iterator().next());
  }

  @Test
  public void testPaddingWithWithVariousPrecisionData() throws Exception {
    MetricsPaddingMethod paddingMethod =
      new MetricsPaddingMethod(MetricsPaddingMethod.PADDING_STRATEGY.ZEROS);

    long now = System.currentTimeMillis();
    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("m1");
    timelineMetric.setHostName("h1");
    timelineMetric.setAppId("a1");
    timelineMetric.setStartTime(now);
    TreeMap<Long, Double> inputValues = new TreeMap<>();

    long seconds = 1000;
    long minute = 60*seconds;
    long hour = 60*minute;
    long day = 24*hour;

    //MINUTES
    inputValues.clear();
    for(int i=5;i>=1;i--) {
      inputValues.put(now - i*minute, i+ 0.0);
    }
    timelineMetric.setMetricValues(inputValues);

    TemporalInfo temporalInfo = getTemporalInfo(now - 2*hour - 1*minute, now, null);
    paddingMethod.applyPaddingStrategy(timelineMetric, temporalInfo);
    TreeMap<Long, Double> values = (TreeMap<Long, Double>) timelineMetric.getMetricValues();

    Assert.assertEquals(122, values.size());
    Assert.assertEquals(new Long(now - 2*hour - 1*minute), values.keySet().iterator().next());

    //HOURS
    inputValues.clear();
    for(int i=5;i>=1;i--) {
      inputValues.put(now - i*hour, i+ 0.0);
    }
    timelineMetric.setMetricValues(inputValues);

    temporalInfo = getTemporalInfo(now - 1*day - 1*hour, now, null);
    paddingMethod.applyPaddingStrategy(timelineMetric, temporalInfo);
    values = (TreeMap<Long, Double>) timelineMetric.getMetricValues();

    Assert.assertEquals(26, values.size());
    Assert.assertEquals(new Long(now - 1*day - 1*hour), values.keySet().iterator().next());

    //DAYS
    inputValues.clear();
    inputValues.put(now - day, 1.0);
    timelineMetric.setMetricValues(inputValues);

    temporalInfo = getTemporalInfo(now - 40*day, now, null);
    paddingMethod.applyPaddingStrategy(timelineMetric, temporalInfo);
    values = (TreeMap<Long, Double>) timelineMetric.getMetricValues();

    Assert.assertEquals(41, values.size());
    Assert.assertEquals(new Long(now - 40*day), values.keySet().iterator().next());
  }


  @Test
  public void testNoPaddingRequested() throws Exception {
    MetricsPaddingMethod paddingMethod =
      new MetricsPaddingMethod(MetricsPaddingMethod.PADDING_STRATEGY.NONE);

    long now = System.currentTimeMillis();

    TimelineMetric timelineMetric = new TimelineMetric();
    timelineMetric.setMetricName("m1");
    timelineMetric.setHostName("h1");
    timelineMetric.setAppId("a1");
    timelineMetric.setStartTime(now);
    TreeMap<Long, Double> inputValues = new TreeMap<>();
    inputValues.put(now - 100, 1.0d);
    inputValues.put(now - 200, 2.0d);
    inputValues.put(now - 300, 3.0d);
    timelineMetric.setMetricValues(inputValues);

    TemporalInfo temporalInfo = getTemporalInfo(now - 1000, now, 10l);
    paddingMethod.applyPaddingStrategy(timelineMetric, temporalInfo);
    TreeMap<Long, Double> values = (TreeMap<Long, Double>) timelineMetric.getMetricValues();

    Assert.assertEquals(3, values.size());
  }

  private TemporalInfo getTemporalInfo(final Long startTime, final Long endTime, final Long step) {
    return new TemporalInfo() {
      @Override
      public Long getStartTime() {
        return startTime;
      }

      @Override
      public Long getEndTime() {
        return endTime;
      }

      @Override
      public Long getStep() {
        return step;
      }

      @Override
      public Long getStartTimeMillis() {
        return startTime;
      }

      @Override
      public Long getEndTimeMillis() {
        return endTime;
      }
    };
  }
}
