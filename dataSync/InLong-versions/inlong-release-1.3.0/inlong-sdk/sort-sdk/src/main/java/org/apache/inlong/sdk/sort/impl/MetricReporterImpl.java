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

package org.apache.inlong.sdk.sort.impl;

import java.util.HashMap;
import java.util.Map;
import org.apache.inlong.sdk.sort.api.MetricReporter;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.metrics.SortSdkMetricItem;
import org.apache.inlong.sdk.sort.metrics.SortSdkPrometheusMetricListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricReporterImpl implements MetricReporter {

    private static final Logger LOG = LoggerFactory.getLogger(MetricReporterImpl.class);
    private SortSdkPrometheusMetricListener sortSdkPrometheusMetricListener;
    private SortClientConfig sortClientConfig;

    public MetricReporterImpl(SortClientConfig sortClientConfig) {
        this.sortClientConfig = sortClientConfig;
        if (sortClientConfig.isPrometheusEnabled()) {
            initPrometheusMetricListener();
        }
    }

    /**
     * report metric
     *
     * @param monitorName String
     * @param keys metric key
     * @param values metric val
     * @return true/false
     */
    @Override
    public boolean report(String monitorName, String[] keys, long[] values) {

        if (sortClientConfig.isPrometheusEnabled()) {
            if (sortSdkPrometheusMetricListener != null) {
                Map<String, Long> prometheusMetricVals = getPrometheusMetricVals(values);
                if (prometheusMetricVals != null) {
                    sortSdkPrometheusMetricListener.snapshot(monitorName, prometheusMetricVals);
                } else {
                    LOG.warn("sortSdkPrometheusMetricListener is null! now init it");
                    initPrometheusMetricListener();
                }
            }
        }
        return true;
    }

    /**
     * close
     */
    @Override
    public void close() {

    }

    private void initPrometheusMetricListener() {
        sortSdkPrometheusMetricListener = new SortSdkPrometheusMetricListener(sortClientConfig.getSortTaskId());
    }

    private Map<String, Long> getPrometheusMetricVals(long[] values) {
        if (values.length > 15) {
            Map<String, Long> metricValueMap = new HashMap<>();
            // consume
            metricValueMap.put(SortSdkMetricItem.M_CONSUME_SIZE, values[0]);
            metricValueMap.put(SortSdkMetricItem.M_CONSUME_MSG_COUNT, values[1]);
            // callback
            metricValueMap.put(SortSdkMetricItem.M_CALL_BACK_COUNT, values[2]);
            metricValueMap.put(SortSdkMetricItem.M_CALL_BACK_DONE_COUNT, values[3]);
            metricValueMap.put(SortSdkMetricItem.M_CALL_BACK_TIME_COST, values[4]);
            metricValueMap.put(SortSdkMetricItem.M_CALL_BACK_FAIL_COUNT, values[5]);
            // topic
            metricValueMap.put(SortSdkMetricItem.M_TOPIC_ONLINE_COUNT, values[6]);
            metricValueMap.put(SortSdkMetricItem.M_TOPIC_OFFLINE_COUNT, values[7]);
            // ack
            metricValueMap.put(SortSdkMetricItem.M_ACK_FAIL_COUNT, values[8]);
            metricValueMap.put(SortSdkMetricItem.M_ACK_SUCC_COUNT, values[9]);
            // request manager
            metricValueMap.put(SortSdkMetricItem.M_REQUEST_MANAGER_COUNT, values[10]);
            metricValueMap.put(SortSdkMetricItem.M_REQUEST_MANAGER_TIME_COST, values[11]);
            metricValueMap.put(SortSdkMetricItem.M_REQUEST_MANAGER_FAIL_COUNT, values[12]);
            metricValueMap.put(SortSdkMetricItem.M_REQUEST_MANAGER_CONF_CHANAGED_COUNT, values[13]);
            metricValueMap.put(SortSdkMetricItem.M_RQUEST_MANAGER_COMMON_ERROR_COUNT, values[14]);
            metricValueMap.put(SortSdkMetricItem.M_RQUEST_MANAGER_PARAM_ERROR_COUNT, values[15]);
            return metricValueMap;
        }
        return null;
    }

}
