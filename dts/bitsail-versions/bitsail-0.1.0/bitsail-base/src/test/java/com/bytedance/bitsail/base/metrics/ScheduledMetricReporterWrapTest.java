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

package com.bytedance.bitsail.base.metrics;

import com.bytedance.bitsail.base.metrics.manager.BitSailMetricManager;
import com.bytedance.bitsail.base.metrics.reporter.impl.LogMetricReporter;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.option.CommonOptions;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Timer;
import org.junit.Assert;
import org.junit.Test;

public class ScheduledMetricReporterWrapTest {

  @Test
  public void testWrappedReporter() {
    BitSailConfiguration jobConf = BitSailConfiguration.newDefault();
    jobConf.set(CommonOptions.JOB_ID, -1L);
    jobConf.set(CommonOptions.METRICS_REPORTER_TYPE, "nop");
    BitSailMetricManager metricManager = new BitSailMetricManager(jobConf, "group");

    try (LogMetricReporter metricReporter = new LogMetricReporter()) {
      ScheduledMetricReporterWrap wrappedReporter = new TestScheduledMetricReporterWrap(metricReporter);
      wrappedReporter.open(jobConf);

      wrappedReporter.notifyOfAddedMetric(new Counter(), "counter", metricManager);
      wrappedReporter.notifyOfAddedMetric(new Timer(), "timer", metricManager);

      wrappedReporter.report();
      wrappedReporter.close();

      Assert.assertEquals(2, ((TestScheduledMetricReporterWrap) wrappedReporter).addCount);
      Assert.assertEquals(0, ((TestScheduledMetricReporterWrap) wrappedReporter).removeCount);
    }
  }

  class TestScheduledMetricReporterWrap extends ScheduledMetricReporterWrap {

    public int addCount = 0;
    public int removeCount = 0;

    TestScheduledMetricReporterWrap(MetricReporter metricReporter) {
      super(metricReporter, false);
    }

    @Override
    public void notifyOfAddedMetric(Metric metric, String metricName, MetricManager group) {
      super.notifyOfAddedMetric(metric, metricName, group);
      addCount += 1;
    }

    @Override
    public void notifyOfRemovedMetric(Metric metric, String metricName, MetricManager group) {
      super.notifyOfRemovedMetric(metric, metricName, group);
      removeCount += 1;
    }
  }
}
