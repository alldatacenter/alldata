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

package com.bytedance.bitsail.base.metrics.reporter;

import com.bytedance.bitsail.base.metrics.manager.BitSailMetricManager;
import com.bytedance.bitsail.base.metrics.reporter.impl.LogMetricReporter;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.SlidingWindowReservoir;
import com.codahale.metrics.Timer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class MetricReporterTest {

  private Gauge mockedGauge = Mockito.spy(new TestGauge());
  private Counter mockedCounter = Mockito.spy(new Counter());
  private Histogram mockedHistogram = Mockito.spy(new Histogram(new SlidingWindowReservoir(10)));
  private Meter mockedMeter = Mockito.spy(new Meter());
  private Timer mockedTimer = Mockito.spy(new Timer());

  private LogMetricReporter reporter;
  private BitSailMetricManager metricManager;

  @Before
  public void init() {
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    jobConf.set(CommonOptions.JOB_ID, -1L);
    jobConf.set(CommonOptions.METRICS_REPORTER_TYPE, "nop");
    metricManager = new BitSailMetricManager(jobConf, "group");

    reporter = new LogMetricReporter();

    reporter.notifyOfAddedMetric(mockedGauge, "gauge", metricManager);
    reporter.notifyOfAddedMetric(mockedCounter, "counter", metricManager);
    reporter.notifyOfAddedMetric(mockedHistogram, "histogram", metricManager);
    reporter.notifyOfAddedMetric(mockedMeter, "meter", metricManager);
    reporter.notifyOfAddedMetric(mockedTimer, "timer", metricManager);
  }

  @Test
  public void testNotifyOfAddedMetric() {
    reporter.report();
    Mockito.verify(mockedGauge, Mockito.times(1)).getValue();
    Mockito.verify(mockedCounter, Mockito.times(1)).getCount();
    Mockito.verify(mockedHistogram, Mockito.times(1)).getSnapshot();
    Mockito.verify(mockedMeter, Mockito.times(1)).getCount();
    Mockito.verify(mockedTimer, Mockito.times(1)).getSnapshot();
  }

  @Test
  public void testNotifyOfRemovedMetric() {
    reporter.notifyOfRemovedMetric(mockedGauge, "gauge", metricManager);
    reporter.notifyOfRemovedMetric(mockedHistogram, "histogram", metricManager);
    reporter.report();
    Mockito.verify(mockedGauge, Mockito.times(0)).getValue();
    Mockito.verify(mockedCounter, Mockito.times(1)).getCount();
    Mockito.verify(mockedHistogram, Mockito.times(0)).getSnapshot();
    Mockito.verify(mockedMeter, Mockito.times(1)).getCount();
    Mockito.verify(mockedTimer, Mockito.times(1)).getSnapshot();
  }

  class TestGauge implements Gauge<Integer> {
    @Override
    public Integer getValue() {
      return 1;
    }
  }
}
