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

import static org.apache.inlong.agent.constant.AgentConstants.DEFAULT_PROMETHEUS_EXPORTER_PORT;
import static org.apache.inlong.agent.constant.AgentConstants.PROMETHEUS_EXPORTER_PORT;
import static org.apache.inlong.agent.metrics.AgentMetricItem.KEY_COMPONENT_NAME;
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
import static org.apache.inlong.common.metric.MetricItemMBean.DOMAIN_SEPARATOR;
import static org.apache.inlong.common.metric.MetricRegister.JMX_DOMAIN;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.common.metric.MetricItemValue;
import org.apache.inlong.common.metric.MetricListener;
import org.apache.inlong.common.metric.MetricValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * prometheus metric listener
 */
public class AgentPrometheusMetricListener extends Collector implements MetricListener {

    public static final String DEFAULT_DIMENSION_LABEL = "dimension";
    public static final String HYPHEN_SYMBOL = "-";
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentPrometheusMetricListener.class);
    protected HTTPServer httpServer;
    private AgentMetricItem metricItem;
    private Map<String, AtomicLong> metricValueMap = new ConcurrentHashMap<>();
    private Map<String, MetricItemValue> dimensionMetricValueMap = new ConcurrentHashMap<>();
    private List<String> dimensionKeys = new ArrayList<>();

    public AgentPrometheusMetricListener() {
        this.metricItem = new AgentMetricItem();
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        StringBuilder beanName = new StringBuilder();
        beanName.append(JMX_DOMAIN).append(DOMAIN_SEPARATOR).append("type=AgentPrometheus");
        String strBeanName = beanName.toString();
        try {
            ObjectName objName = new ObjectName(strBeanName);
            mbs.registerMBean(metricItem, objName);
        } catch (Exception ex) {
            LOGGER.error("exception while register mbean:{},error:{}", strBeanName, ex);
        }
        // prepare metric value map
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

        int metricsServerPort = AgentConfiguration.getAgentConf()
                .getInt(PROMETHEUS_EXPORTER_PORT, DEFAULT_PROMETHEUS_EXPORTER_PORT);
        try {
            this.httpServer = new HTTPServer(metricsServerPort);
            this.register();
            LOGGER.info("Starting prometheus metrics server on port {}", metricsServerPort);
        } catch (IOException e) {
            LOGGER.error("exception while register agent prometheus http server,error:{}", e.getMessage());
        }
    }

    @Override
    public List<MetricFamilySamples> collect() {
        DefaultExports.initialize();
        // total
        CounterMetricFamily totalCounter = new CounterMetricFamily("agent", "metrics_of_agent_node_total",
                Arrays.asList(DEFAULT_DIMENSION_LABEL));
        totalCounter.addMetric(Arrays.asList(M_JOB_RUNNING_COUNT), metricItem.jobRunningCount.get());
        totalCounter.addMetric(Arrays.asList(M_JOB_FATAL_COUNT), metricItem.jobFatalCount.get());
        totalCounter.addMetric(Arrays.asList(M_TASK_RUNNING_COUNT), metricItem.taskRunningCount.get());
        totalCounter.addMetric(Arrays.asList(M_TASK_RETRYING_COUNT), metricItem.taskRetryingCount.get());
        totalCounter.addMetric(Arrays.asList(M_TASK_FATAL_COUNT), metricItem.taskFatalCount.get());
        totalCounter.addMetric(Arrays.asList(M_SINK_SUCCESS_COUNT), metricItem.sinkSuccessCount.get());
        totalCounter.addMetric(Arrays.asList(M_SINK_FAIL_COUNT), metricItem.sinkFailCount.get());
        totalCounter.addMetric(Arrays.asList(M_SOURCE_SUCCESS_COUNT), metricItem.sourceSuccessCount.get());
        totalCounter.addMetric(Arrays.asList(M_SOURCE_FAIL_COUNT), metricItem.sourceFailCount.get());
        totalCounter.addMetric(Arrays.asList(M_PLUGIN_READ_COUNT), metricItem.pluginReadCount.get());
        totalCounter.addMetric(Arrays.asList(M_PLUGIN_SEND_COUNT), metricItem.pluginSendCount.get());
        totalCounter.addMetric(Arrays.asList(M_PLUGIN_READ_FAIL_COUNT), metricItem.pluginReadFailCount.get());
        totalCounter.addMetric(Arrays.asList(M_PLUGIN_SEND_FAIL_COUNT), metricItem.pluginSendFailCount.get());
        totalCounter.addMetric(Arrays.asList(M_PLUGIN_READ_SUCCESS_COUNT), metricItem.pluginReadSuccessCount.get());
        totalCounter.addMetric(Arrays.asList(M_PLUGIN_SEND_SUCCESS_COUNT), metricItem.pluginSendSuccessCount.get());
        List<MetricFamilySamples> mfs = new ArrayList<>();
        mfs.add(totalCounter);

        // id dimension
        for (Entry<String, MetricItemValue> entry : this.dimensionMetricValueMap.entrySet()) {
            MetricItemValue itemValue = entry.getValue();
            Map<String, String> dimensionMap = itemValue.getDimensions();
            String pluginId = dimensionMap.getOrDefault(KEY_PLUGIN_ID, HYPHEN_SYMBOL);
            String componentName = dimensionMap.getOrDefault(KEY_COMPONENT_NAME, HYPHEN_SYMBOL);
            String counterName = pluginId.equals(HYPHEN_SYMBOL) ? componentName : pluginId;
            List<String> dimensionIdKeys = new ArrayList<>();
            dimensionIdKeys.add(DEFAULT_DIMENSION_LABEL);
            dimensionIdKeys.addAll(dimensionMap.keySet());
            CounterMetricFamily idCounter = new CounterMetricFamily(counterName,
                    "metrics_of_agent_dimensions_" + counterName, dimensionIdKeys);

            addCounterMetricFamily(M_JOB_RUNNING_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_JOB_FATAL_COUNT, itemValue, idCounter);

            addCounterMetricFamily(M_TASK_RUNNING_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_TASK_RETRYING_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_TASK_FATAL_COUNT, itemValue, idCounter);

            addCounterMetricFamily(M_SINK_SUCCESS_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_SINK_FAIL_COUNT, itemValue, idCounter);

            addCounterMetricFamily(M_SOURCE_SUCCESS_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_SOURCE_FAIL_COUNT, itemValue, idCounter);

            addCounterMetricFamily(M_PLUGIN_READ_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_PLUGIN_SEND_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_PLUGIN_READ_FAIL_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_PLUGIN_SEND_FAIL_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_PLUGIN_READ_SUCCESS_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_PLUGIN_SEND_SUCCESS_COUNT, itemValue, idCounter);
            mfs.add(idCounter);
        }
        return mfs;
    }

    @Override
    public void snapshot(String domain, List<MetricItemValue> itemValues) {
        for (MetricItemValue itemValue : itemValues) {
            // total
            for (Entry<String, MetricValue> entry : itemValue.getMetrics().entrySet()) {
                String fieldName = entry.getValue().name;
                AtomicLong metricValue = this.metricValueMap.get(fieldName);
                if (metricValue != null) {
                    long fieldValue = entry.getValue().value;
                    metricValue.addAndGet(fieldValue);
                }
            }
            // id dimension
            String dimensionKey = itemValue.getKey();
            MetricItemValue dimensionMetricValue = this.dimensionMetricValueMap.get(dimensionKey);
            if (dimensionMetricValue == null) {
                dimensionMetricValue = new MetricItemValue(dimensionKey, new ConcurrentHashMap<>(),
                        new ConcurrentHashMap<>());
                this.dimensionMetricValueMap.putIfAbsent(dimensionKey, dimensionMetricValue);
                dimensionMetricValue = this.dimensionMetricValueMap.get(dimensionKey);
                dimensionMetricValue.getDimensions().putAll(itemValue.getDimensions());
                // add prometheus label name
                for (Entry<String, String> entry : itemValue.getDimensions().entrySet()) {
                    if (!this.dimensionKeys.contains(entry.getKey())) {
                        this.dimensionKeys.add(entry.getKey());
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

    private void addCounterMetricFamily(String defaultDimension, MetricItemValue itemValue,
            CounterMetricFamily idCounter) {
        Map<String, String> dimensionMap = itemValue.getDimensions();
        List<String> labelValues = new ArrayList<>(dimensionMap.size() + 1);
        labelValues.add(defaultDimension);
        for (String key : dimensionMap.keySet()) {
            String labelValue = dimensionMap.getOrDefault(key, HYPHEN_SYMBOL);
            labelValues.add(labelValue);
        }
        long value = 0L;
        Map<String, MetricValue> metricValueMap = itemValue.getMetrics();
        MetricValue metricValue = metricValueMap.get(defaultDimension);
        if (metricValue != null) {
            value = metricValue.value;
        }
        idCounter.addMetric(labelValues, value);
    }
}
