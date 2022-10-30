/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corebase.metric;

import org.apache.inlong.tubemq.corebase.metric.impl.BaseMetric;
import org.apache.inlong.tubemq.corebase.metric.impl.LongMaxGauge;
import org.apache.inlong.tubemq.corebase.metric.impl.LongMinGauge;
import org.apache.inlong.tubemq.corebase.metric.impl.LongOnlineCounter;
import org.apache.inlong.tubemq.corebase.metric.impl.LongStatsCounter;
import org.apache.inlong.tubemq.corebase.metric.impl.SinceTime;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.junit.Assert;
import org.junit.Test;

public class SimpleMetricTest {

    @Test
    public void testBaseMetric() {
        String metricName = "test";
        String prefix1 = "prefix_1";
        String prefix2 = "";
        // test BaseMetric class
        BaseMetric metric1 = new BaseMetric(metricName, prefix1);
        Assert.assertEquals(metricName, metric1.getShortName());
        Assert.assertEquals(prefix1 + "_" + metricName, metric1.getFullName());
        BaseMetric metric2 = new BaseMetric(metricName, prefix2);
        Assert.assertEquals(metricName, metric2.getShortName());
        Assert.assertEquals(metricName, metric2.getFullName());
        // test sub-class
        LongMaxGauge metric3 = new LongMaxGauge(metricName, prefix1);
        Assert.assertEquals(metricName, metric3.getShortName());
        Assert.assertEquals(prefix1 + "_" + metricName, metric3.getFullName());
        LongMaxGauge metric4 = new LongMaxGauge(metricName, prefix2);
        Assert.assertEquals(metricName, metric4.getShortName());
        Assert.assertEquals(metricName, metric4.getFullName());
        // test SinceTime
        SinceTime sinceTime1 = new SinceTime(metricName, prefix1);
        Assert.assertEquals(metricName, sinceTime1.getShortName());
        Assert.assertEquals(prefix1 + "_" + metricName, sinceTime1.getFullName());
        SinceTime sinceTime2 = new SinceTime(metricName, prefix2);
        Assert.assertEquals(metricName, sinceTime2.getShortName());
        Assert.assertEquals(metricName, sinceTime2.getFullName());
        ThreadUtils.sleep(50);
        long since11 = sinceTime1.getAndResetSinceTime();
        Assert.assertNotEquals(since11, sinceTime1.getSinceTime());
    }

    @Test
    public void testLongMetric() {
        // test LongMaxGauge
        LongMaxGauge maxGauge = new LongMaxGauge("max", "long");
        Assert.assertEquals(Long.MIN_VALUE, maxGauge.getValue());
        maxGauge.update(3);
        maxGauge.update(100);
        maxGauge.update(50);
        Assert.assertEquals(100, maxGauge.getValue());
        maxGauge.update(5000);
        Assert.assertEquals(5000, maxGauge.getAndResetValue());
        maxGauge.update(200);
        maxGauge.update(300);
        maxGauge.update(50);
        Assert.assertEquals(300, maxGauge.getValue());
        // test LongMinGauge
        LongMinGauge minGauge = new LongMinGauge("min", "long");
        Assert.assertEquals(Long.MAX_VALUE, minGauge.getValue());
        minGauge.update(3);
        minGauge.update(100);
        minGauge.update(50);
        Assert.assertEquals(3, minGauge.getValue());
        minGauge.update(1);
        Assert.assertEquals(1, minGauge.getAndResetValue());
        minGauge.update(500);
        minGauge.update(600);
        minGauge.update(30);
        Assert.assertEquals(30, minGauge.getValue());
        // test LongOnlineCounter
        LongOnlineCounter onlineCounter = new LongOnlineCounter("online", "long");
        onlineCounter.incValue();
        onlineCounter.incValue();
        onlineCounter.decValue();
        Assert.assertEquals(1, onlineCounter.getValue());
        onlineCounter.incValue();
        Assert.assertEquals(2, onlineCounter.getAndResetValue());
        onlineCounter.decValue();
        Assert.assertEquals(1, onlineCounter.getValue());
        onlineCounter.addValue(5);
        Assert.assertEquals(6, onlineCounter.getValue());
        onlineCounter.addValue(-5);
        Assert.assertEquals(1, onlineCounter.getValue());
        onlineCounter.clear();
        Assert.assertEquals(0, onlineCounter.getValue());
        // test LongStatsCounter
        LongStatsCounter statsCounter = new LongStatsCounter("stats", "long");
        statsCounter.incValue();
        statsCounter.incValue();
        statsCounter.decValue();
        Assert.assertEquals(1, statsCounter.getValue());
        statsCounter.incValue();
        Assert.assertEquals(2, statsCounter.getAndResetValue());
        statsCounter.decValue();
        Assert.assertEquals(-1, statsCounter.getValue());
        statsCounter.addValue(5);
        Assert.assertEquals(4, statsCounter.getValue());
        statsCounter.addValue(-3);
        Assert.assertEquals(1, statsCounter.getValue());
        statsCounter.clear();
        Assert.assertEquals(0, statsCounter.getValue());
    }
}
