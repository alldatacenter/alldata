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

import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.common.metric.MetricItem;
import org.apache.inlong.common.metric.MetricObserver;
import org.apache.inlong.common.metric.MetricRegister;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_PLUGIN_ID;

public class TestAgentMetrics {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestAgentMetrics.class);
    // metric
    protected static final AtomicLong METRIC_INDEX = new AtomicLong(0);
    protected static AgentMetricItemSet metricItemSet;
    protected static Map<String, String> dimensions;

    private static String groupId1 = "groupId_test1";
    private static String groupId2 = "groupId_test2";
    private static String streamId = "streamId";

    @BeforeClass
    public static void setup() {
        dimensions = new HashMap<>();
        dimensions.put(KEY_PLUGIN_ID, TestAgentMetrics.class.getSimpleName());
        dimensions.put(KEY_INLONG_GROUP_ID, groupId1);
        dimensions.put(KEY_INLONG_STREAM_ID, streamId);
        String metricName = String.join("-", TestAgentMetrics.class.getSimpleName(),
                String.valueOf(METRIC_INDEX.incrementAndGet()));
        metricItemSet = new AgentMetricItemSet(metricName);
        MetricRegister.register(metricItemSet);
        Assert.assertEquals(metricItemSet.getName(), "TestAgentMetrics-1");
    }

    @AfterClass
    public static void testSnapshot() {
        List<MetricItem> items = new LinkedList<>();
        for (MetricItem item : metricItemSet.snapshot()) {
            items.add(item);
            LOGGER.info(item.getDimensionsKey());
            LOGGER.info(item.snapshot().toString());
        }
        Assert.assertEquals(items.size(), 2);
    }

    @Test
    public void testMetricsOp() {
        metricItemSet.findMetricItem(dimensions).pluginReadCount.addAndGet(10);
        metricItemSet.findMetricItem(dimensions).pluginSendFailCount.incrementAndGet();
        Assert.assertEquals(metricItemSet.findMetricItem(dimensions).pluginReadCount.get(), 10);
        Assert.assertEquals(metricItemSet.findMetricItem(dimensions).pluginReadFailCount.get(), 0);
        Assert.assertEquals(metricItemSet.findMetricItem(dimensions).pluginSendFailCount.get(), 1);
    }

    @Test
    public void testAddGroupId() {
        Map<String, String> newDimension = new HashMap<>();
        newDimension.put(KEY_PLUGIN_ID, getClass().getSimpleName());
        newDimension.put(KEY_INLONG_GROUP_ID, groupId2);
        newDimension.put(KEY_INLONG_STREAM_ID, streamId);
        AgentMetricItem newItem = metricItemSet.findMetricItem(newDimension);
        Assert.assertEquals(newDimension.get(KEY_PLUGIN_ID), TestAgentMetrics.class.getSimpleName());
        newItem.taskRunningCount.addAndGet(5);
        newItem.taskRunningCount.decrementAndGet();
        Assert.assertEquals(metricItemSet.findMetricItem(newDimension).taskRunningCount.get(), 4);
    }

    @Test
    public void testMultipleRegister() {
        String metricName = String.join("-", TestAgentMetrics.class.getSimpleName(),
                String.valueOf(METRIC_INDEX.incrementAndGet()));
        AgentMetricItemSet newItems = new AgentMetricItemSet(metricName);
        MetricRegister.register(newItems);
        Assert.assertEquals(newItems.getName(), "TestAgentMetrics-2");
    }

    @Test
    public void testMetricObserverInit() {
        MetricObserver.init(AgentConfiguration.getAgentConf().getConfigProperties());
    }

}
