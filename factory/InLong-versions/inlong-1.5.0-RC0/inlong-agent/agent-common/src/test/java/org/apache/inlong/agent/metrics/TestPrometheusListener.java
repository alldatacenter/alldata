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

import org.apache.inlong.common.metric.MetricItemValue;
import org.apache.inlong.common.metric.MetricListener;
import org.apache.inlong.common.metric.MetricListenerRunnable;
import org.apache.inlong.common.metric.MetricRegister;
import org.apache.inlong.common.metric.MetricValue;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_INLONG_GROUP_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_INLONG_STREAM_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_PLUGIN_ID;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_JOB_FATAL_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_JOB_RUNNING_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_PLUGIN_READ_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_PLUGIN_READ_FAIL_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_PLUGIN_READ_SUCCESS_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_PLUGIN_SEND_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_PLUGIN_SEND_FAIL_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_PLUGIN_SEND_SUCCESS_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_SINK_FAIL_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_SINK_SUCCESS_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_SOURCE_FAIL_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_SOURCE_SUCCESS_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_TASK_FATAL_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_TASK_RETRYING_COUNT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.M_TASK_RUNNING_COUNT;

/**
 * use to test prometheus listener.
 */
public class TestPrometheusListener {

    protected static final AtomicLong METRIC_INDEX = new AtomicLong(0);
    private static final Logger LOGGER = LoggerFactory.getLogger(TestPrometheusListener.class);
    private static final Map<String, AtomicLong> metricValueMap = new ConcurrentHashMap<>();
    protected static AgentMetricItemSet metricItemSet;
    protected static Map<String, String> dimensions;
    private static AgentMetricItem metricItem = new AgentMetricItem();
    private final Map<String, MetricItemValue> dimensionMetricValueMap = new ConcurrentHashMap<>();
    private final List<String> dimensionKeys = new ArrayList<>();

    @BeforeClass
    public static void setup() {
        dimensions = new HashMap<>();
        dimensions.put(KEY_PLUGIN_ID, TestPrometheusListener.class.getSimpleName());
        String groupId1 = "groupId_test1";
        dimensions.put(KEY_INLONG_GROUP_ID, groupId1);
        String streamId = "streamId";
        dimensions.put(KEY_INLONG_STREAM_ID, streamId);
        String metricName = String.join("-", TestPrometheusListener.class.getSimpleName(),
                String.valueOf(METRIC_INDEX.incrementAndGet()));
        metricItemSet = new AgentMetricItemSet(metricName);
        MetricRegister.register(metricItemSet);
        Assert.assertEquals(metricItemSet.getName(), "TestPrometheusListener-1");
        metricValueMap.put(M_JOB_RUNNING_COUNT, metricItem.jobRunningCount);
        metricValueMap.put(M_JOB_FATAL_COUNT, metricItem.jobFatalCount);

        metricValueMap.put(M_TASK_RUNNING_COUNT, metricItem.taskRunningCount);
        metricValueMap.put(M_TASK_RETRYING_COUNT, metricItem.taskRetryingCount);
        metricValueMap.put(M_TASK_FATAL_COUNT, metricItem.taskFatalCount);

        metricValueMap.put(M_SINK_SUCCESS_COUNT, metricItem.sinkSuccessCount);
        metricValueMap.put(M_SINK_FAIL_COUNT, metricItem.sinkFailCount);

        metricValueMap.put(M_SOURCE_SUCCESS_COUNT, metricItem.sourceSuccessCount);
        metricValueMap.put(M_SOURCE_FAIL_COUNT, metricItem.sourceFailCount);

        metricValueMap.put(M_PLUGIN_READ_COUNT, metricItem.pluginReadCount);
        metricValueMap.put(M_PLUGIN_SEND_COUNT, metricItem.pluginSendCount);
        metricValueMap.put(M_PLUGIN_READ_FAIL_COUNT, metricItem.pluginReadFailCount);
        metricValueMap.put(M_PLUGIN_SEND_FAIL_COUNT, metricItem.pluginSendFailCount);
        metricValueMap.put(M_PLUGIN_READ_SUCCESS_COUNT, metricItem.pluginReadSuccessCount);
        metricValueMap.put(M_PLUGIN_SEND_SUCCESS_COUNT, metricItem.pluginSendSuccessCount);
    }

    @Test
    public void testSnapshot() {
        metricItem = metricItemSet.findMetricItem(dimensions);
        metricItem.pluginReadFailCount.incrementAndGet();
        metricItem.pluginReadSuccessCount.incrementAndGet();
        // report
        MetricListener listener = new MetricListener() {

            @Override
            public void snapshot(String domain, List<MetricItemValue> itemValues) {
                for (MetricItemValue itemValue : itemValues) {
                    String key = itemValue.getKey();
                    LOGGER.info("KEY : " + key);
                    Map<String, MetricValue> metricMap = itemValue.getMetrics();
                    // total
                    for (Entry<String, MetricValue> entry : itemValue.getMetrics().entrySet()) {
                        String fieldName = entry.getValue().name;
                        AtomicLong metricValue = metricValueMap.get(fieldName);
                        if (metricValue != null) {
                            long fieldValue = entry.getValue().value;
                            metricValue.addAndGet(fieldValue);
                            metricValue.addAndGet(100);
                        }
                    }
                    // dimension
                    String dimensionKey = itemValue.getKey();
                    MetricItemValue dimensionMetricValue = dimensionMetricValueMap.get(dimensionKey);
                    if (dimensionMetricValue == null) {
                        dimensionMetricValue = new MetricItemValue(dimensionKey,
                                new ConcurrentHashMap<String, String>(),
                                new ConcurrentHashMap<String, MetricValue>());
                        dimensionMetricValueMap.putIfAbsent(dimensionKey, dimensionMetricValue);
                        dimensionMetricValue = dimensionMetricValueMap.get(dimensionKey);
                        dimensionMetricValue.getDimensions().putAll(itemValue.getDimensions());
                        // add prometheus label name
                        for (Entry<String, String> entry : itemValue.getDimensions().entrySet()) {
                            if (!dimensionKeys.contains(entry.getKey())) {
                                dimensionKeys.add(entry.getKey());
                            }
                        }
                    }
                    // count
                    for (Entry<String, MetricValue> entry : itemValue.getMetrics().entrySet()) {
                        String fieldName = entry.getValue().name;
                        MetricValue metricValue = dimensionMetricValue.getMetrics().get(fieldName);
                        if (metricValue == null) {
                            metricValue = MetricValue.of(fieldName, entry.getValue().value);
                            dimensionMetricValue.getMetrics().put(metricValue.name, metricValue);
                            continue;
                        }
                        metricValue.value += entry.getValue().value;
                    }
                }
            }
        };
        Assert.assertEquals(metricItem.pluginReadSuccessCount.get(), 1);
        Assert.assertEquals(metricItem.pluginReadFailCount.get(), 1);
        List<MetricListener> listeners = new ArrayList<>();
        listeners.add(listener);
        MetricListenerRunnable runnable = new MetricListenerRunnable("Agent", listeners);
        runnable.run();
        Assert.assertEquals(metricValueMap.get("pluginReadFailCount").intValue(), 101);
        Assert.assertTrue(
                dimensionMetricValueMap.toString().contains("{\"name\":\"pluginReadSuccessCount\",\"value\":1}"));
        Assert.assertTrue(dimensionKeys.contains("inlongGroupId"));
        for (Entry<String, MetricItemValue> entry : this.dimensionMetricValueMap.entrySet()) {
            MetricItemValue itemValue = entry.getValue();
            // JOB
            addMetric(M_JOB_RUNNING_COUNT, itemValue);
            addMetric(M_JOB_FATAL_COUNT, itemValue);
            // TASK
            addMetric(M_TASK_RUNNING_COUNT, itemValue);
            addMetric(M_TASK_RETRYING_COUNT, itemValue);
            addMetric(M_TASK_FATAL_COUNT, itemValue);
            // SINK
            addMetric(M_SINK_SUCCESS_COUNT, itemValue);
            addMetric(M_SINK_FAIL_COUNT, itemValue);
            // SOURCE
            addMetric(M_SOURCE_SUCCESS_COUNT, itemValue);
            addMetric(M_SOURCE_FAIL_COUNT, itemValue);
            // PLUGIN
            addMetric(M_PLUGIN_READ_COUNT, itemValue);
            addMetric(M_PLUGIN_SEND_COUNT, itemValue);
            addMetric(M_PLUGIN_READ_FAIL_COUNT, itemValue);
            addMetric(M_PLUGIN_SEND_FAIL_COUNT, itemValue);
            addMetric(M_PLUGIN_READ_SUCCESS_COUNT, itemValue);
            addMetric(M_PLUGIN_SEND_SUCCESS_COUNT, itemValue);
        }
        List<MetricItemValue> metricItemValueList = new ArrayList<>(dimensionMetricValueMap.values());
        AgentPrometheusMetricListener agentPrometheusMetricListener = new AgentPrometheusMetricListener();
        agentPrometheusMetricListener.snapshot("Agent", metricItemValueList);
        LOGGER.debug(agentPrometheusMetricListener.collect().toString());
    }

    private void addMetric(String defaultDimension, MetricItemValue itemValue) {
        List<String> labelValues = new ArrayList<>(this.dimensionKeys.size());
        labelValues.add(defaultDimension);
        Map<String, String> dimensions = itemValue.getDimensions();
        for (String key : this.dimensionKeys) {
            String labelValue = dimensions.getOrDefault(key, "-");
            labelValues.add(labelValue);
        }
        long value = 0L;
        Map<String, MetricValue> metricValueMap = itemValue.getMetrics();
        MetricValue metricValue = metricValueMap.get(defaultDimension);
        if (metricValue != null) {
            value = metricValue.value;
        }
        LOGGER.debug("labelValues is " + labelValues + " and value is " + value);
    }
}
