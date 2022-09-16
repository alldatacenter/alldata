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

package org.apache.inlong.dataproxy.metrics.prometheus;

import static org.apache.inlong.common.metric.MetricItemMBean.DOMAIN_SEPARATOR;
import static org.apache.inlong.common.metric.MetricRegister.JMX_DOMAIN;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_NODE_DURATION;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_READ_FAIL_COUNT;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_READ_FAIL_SIZE;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_READ_SUCCESS_COUNT;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_READ_SUCCESS_SIZE;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_SEND_COUNT;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_SEND_FAIL_COUNT;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_SEND_FAIL_SIZE;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_SEND_SIZE;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_SEND_SUCCESS_COUNT;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_SEND_SUCCESS_SIZE;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_SINK_DURATION;
import static org.apache.inlong.dataproxy.metrics.DataProxyMetricItem.M_WHOLE_DURATION;

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

import org.apache.inlong.common.metric.MetricValue;
import org.apache.inlong.dataproxy.config.RemoteConfigManager;
import org.apache.inlong.dataproxy.config.holder.CommonPropertiesHolder;
import org.apache.inlong.dataproxy.metrics.DataProxyMetricItem;
import org.apache.inlong.common.metric.MetricItemValue;
import org.apache.inlong.common.metric.MetricListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.exporter.HTTPServer;

/**
 *
 * PrometheusMetricListener
 */
public class PrometheusMetricListener extends Collector implements MetricListener {

    private static final Logger LOG = LoggerFactory.getLogger(PrometheusMetricListener.class);
    public static final String KEY_PROMETHEUS_HTTP_PORT = "prometheusHttpPort";
    public static final int DEFAULT_PROMETHEUS_HTTP_PORT = 8080;
    public static final String DEFAULT_DIMENSION_LABEL = "dimension";

    private String metricName;
    private DataProxyMetricItem metricItem;
    private Map<String, AtomicLong> metricValueMap = new ConcurrentHashMap<>();
    protected HTTPServer httpServer;
    private Map<String, MetricItemValue> dimensionMetricValueMap = new ConcurrentHashMap<>();
    private List<String> dimensionKeys = new ArrayList<>();

    /**
     * Constructor
     */
    public PrometheusMetricListener() {
        this.metricName = CommonPropertiesHolder.getString(RemoteConfigManager.KEY_PROXY_CLUSTER_NAME);
        this.metricItem = new DataProxyMetricItem();
        this.metricItem.clusterId = metricName;
        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        StringBuilder beanName = new StringBuilder();
        beanName.append(JMX_DOMAIN).append(DOMAIN_SEPARATOR).append("type=DataProxyPrometheus");
        String strBeanName = beanName.toString();
        try {
            ObjectName objName = new ObjectName(strBeanName);
            mbs.registerMBean(metricItem, objName);
        } catch (Exception ex) {
            LOG.error("exception while register mbean:{},error:{}", strBeanName, ex);
        }
        // prepare metric value map
        metricValueMap.put(M_READ_SUCCESS_COUNT, metricItem.readSuccessCount);
        metricValueMap.put(M_READ_SUCCESS_SIZE, metricItem.readSuccessSize);
        metricValueMap.put(M_READ_FAIL_COUNT, metricItem.readFailCount);
        metricValueMap.put(M_READ_FAIL_SIZE, metricItem.readFailSize);
        //
        metricValueMap.put(M_SEND_COUNT, metricItem.sendCount);
        metricValueMap.put(M_SEND_SIZE, metricItem.sendSize);
        //
        metricValueMap.put(M_SEND_SUCCESS_COUNT, metricItem.sendSuccessCount);
        metricValueMap.put(M_SEND_SUCCESS_SIZE, metricItem.sendSuccessSize);
        metricValueMap.put(M_SEND_FAIL_COUNT, metricItem.sendFailCount);
        metricValueMap.put(M_SEND_FAIL_SIZE, metricItem.sendFailSize);
        //
        metricValueMap.put(M_SINK_DURATION, metricItem.sinkDuration);
        metricValueMap.put(M_NODE_DURATION, metricItem.nodeDuration);
        metricValueMap.put(M_WHOLE_DURATION, metricItem.wholeDuration);

        int httpPort = CommonPropertiesHolder.getInteger(KEY_PROMETHEUS_HTTP_PORT, DEFAULT_PROMETHEUS_HTTP_PORT);
        try {
            this.httpServer = new HTTPServer(httpPort);
            this.register();
        } catch (IOException e) {
            LOG.error("exception while register prometheus http server:{},error:{}", metricName, e.getMessage());
        }
        this.dimensionKeys.add(DEFAULT_DIMENSION_LABEL);
    }

    /**
     * snapshot
     *
     * @param domain
     * @param itemValues
     */
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
                dimensionMetricValue = new MetricItemValue(dimensionKey, new ConcurrentHashMap<String, String>(),
                        new ConcurrentHashMap<String, MetricValue>());
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

    /**
     * collect
     *
     * @return
     */
    @Override
    public List<MetricFamilySamples> collect() {

        // total
        CounterMetricFamily totalCounter = new CounterMetricFamily(metricName + "&group=total",
                "The metrics of dataproxy node.",
                Arrays.asList("dimension"));
        totalCounter.addMetric(Arrays.asList(M_READ_SUCCESS_COUNT), metricItem.readSuccessCount.get());
        totalCounter.addMetric(Arrays.asList(M_READ_SUCCESS_SIZE), metricItem.readSuccessSize.get());
        totalCounter.addMetric(Arrays.asList(M_READ_FAIL_COUNT), metricItem.readFailCount.get());
        totalCounter.addMetric(Arrays.asList(M_READ_FAIL_SIZE), metricItem.readFailSize.get());
        //
        totalCounter.addMetric(Arrays.asList(M_SEND_COUNT), metricItem.sendCount.get());
        totalCounter.addMetric(Arrays.asList(M_SEND_SIZE), metricItem.sendSize.get());
        //
        totalCounter.addMetric(Arrays.asList(M_SEND_SUCCESS_COUNT), metricItem.sendSuccessCount.get());
        totalCounter.addMetric(Arrays.asList(M_SEND_SUCCESS_SIZE), metricItem.sendSuccessSize.get());
        totalCounter.addMetric(Arrays.asList(M_SEND_FAIL_COUNT), metricItem.sendFailCount.get());
        totalCounter.addMetric(Arrays.asList(M_SEND_FAIL_SIZE), metricItem.sendFailSize.get());
        //
        totalCounter.addMetric(Arrays.asList(M_SINK_DURATION), metricItem.sinkDuration.get());
        totalCounter.addMetric(Arrays.asList(M_NODE_DURATION), metricItem.nodeDuration.get());
        totalCounter.addMetric(Arrays.asList(M_WHOLE_DURATION), metricItem.wholeDuration.get());
        List<MetricFamilySamples> mfs = new ArrayList<>();
        mfs.add(totalCounter);

        // id dimension
        CounterMetricFamily idCounter = new CounterMetricFamily(metricName + "&group=id",
                "The metrics of inlong datastream.", this.dimensionKeys);
        for (Entry<String, MetricItemValue> entry : this.dimensionMetricValueMap.entrySet()) {
            MetricItemValue itemValue = entry.getValue();
            // read
            addCounterMetricFamily(M_READ_SUCCESS_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_READ_SUCCESS_SIZE, itemValue, idCounter);
            addCounterMetricFamily(M_READ_FAIL_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_READ_FAIL_SIZE, itemValue, idCounter);
            // send
            addCounterMetricFamily(M_SEND_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_SEND_SIZE, itemValue, idCounter);
            // send success
            addCounterMetricFamily(M_SEND_SUCCESS_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_SEND_SUCCESS_SIZE, itemValue, idCounter);
            addCounterMetricFamily(M_SEND_FAIL_COUNT, itemValue, idCounter);
            addCounterMetricFamily(M_SEND_FAIL_SIZE, itemValue, idCounter);
            // duration
            addCounterMetricFamily(M_SINK_DURATION, itemValue, idCounter);
            addCounterMetricFamily(M_NODE_DURATION, itemValue, idCounter);
            addCounterMetricFamily(M_WHOLE_DURATION, itemValue, idCounter);
        }
        mfs.add(idCounter);
        return mfs;
    }

    /**
     * addCounterMetricFamily
     *
     * @param defaultDemension
     * @param itemValue
     * @param idCounter
     */
    private void addCounterMetricFamily(String defaultDemension, MetricItemValue itemValue,
            CounterMetricFamily idCounter) {
        List<String> labelValues = new ArrayList<>(this.dimensionKeys.size());
        labelValues.add(defaultDemension);
        Map<String, String> dimensions = itemValue.getDimensions();
        for (String key : this.dimensionKeys) {
            String labelValue = dimensions.getOrDefault(key, "-");
            labelValues.add(labelValue);
        }
        long value = 0L;
        Map<String, MetricValue> metricValueMap = itemValue.getMetrics();
        MetricValue metricValue = metricValueMap.get(defaultDemension);
        if (metricValue != null) {
            value = metricValue.value;
        }
        idCounter.addMetric(labelValues, value);
    }
}