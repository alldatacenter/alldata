/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.agent.metrics;

import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.inlong.agent.metrics.counter.CounterInt;
import org.apache.inlong.agent.metrics.counter.CounterLong;
import org.apache.inlong.agent.metrics.gauge.GaugeInt;
import org.apache.inlong.agent.metrics.gauge.GaugeLong;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestMetrics.class);

    @Test
    public void testErrorMetric() throws Exception {
        try {
            ErrorMetric errorMetric = ErrorMetric.getMetrics();
            Assert.fail("Error metric should fail");
        } catch (Exception ex) {
            LOGGER.info("error happens" + ex);
        }
    }

    @Test
    public void testMetric() {
        MetricTest metricTest = MetricTest.getMetrics();
        Assert.assertNotNull(metricTest.counterInt);
        Assert.assertNotNull(metricTest.counterLong);
        Assert.assertNotNull(metricTest.gaugeInt);
        Assert.assertNotNull(metricTest.gaugeLong);

        metricTest.counterInt.incr();
        metricTest.counterInt.incr();

        Assert.assertEquals(2, metricTest.counterInt.snapshot().intValue());

        metricTest.gaugeLong.incr();
        metricTest.gaugeLong.incr();
        metricTest.gaugeLong.decr();

        Assert.assertEquals(1L, metricTest.gaugeLong.snapshot().longValue());
    }

    @Metrics
    static class MetricTest {

        private static final AtomicBoolean IS_INITED = new AtomicBoolean(false);
        private static MetricTest metrics;
        @Metric
        CounterLong counterLong;

        @Metric
        CounterInt counterInt;

        @Metric
        GaugeLong gaugeLong;

        @Metric
        GaugeInt gaugeInt;

        private MetricTest() {
            MetricsRegister.register("Test", "TestMetrics", null, this);
        }

        public static MetricTest getMetrics() {
            if (IS_INITED.compareAndSet(false, true)) {
                metrics = new MetricTest();
            }
            return metrics;
        }
    }

    @Metrics
    static class ErrorMetric {

        private static final AtomicBoolean IS_INITED = new AtomicBoolean(false);
        private static ErrorMetric metrics;
        @Metric
        int errorInt;

        private ErrorMetric() {
            MetricsRegister.register("Test", "ErrorMetric", null, this);
        }

        public static ErrorMetric getMetrics() {
            if (IS_INITED.compareAndSet(false, true)) {
                metrics = new ErrorMetric();
            }
            return metrics;
        }
    }

}
