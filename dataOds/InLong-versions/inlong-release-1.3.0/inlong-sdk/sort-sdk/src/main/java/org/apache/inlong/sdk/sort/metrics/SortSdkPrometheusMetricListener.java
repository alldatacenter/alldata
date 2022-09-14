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

package org.apache.inlong.sdk.sort.metrics;

import static org.apache.inlong.common.metric.MetricItemMBean.DOMAIN_SEPARATOR;
import static org.apache.inlong.common.metric.MetricRegister.JMX_DOMAIN;

import java.lang.management.ManagementFactory;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SortSdkPrometheusMetricListener {

    private static final Logger LOG = LoggerFactory.getLogger(SortSdkPrometheusMetricListener.class);

    private SortSdkMetricItem metricItem;
    private Map<String, AtomicLong> metricValueMap = new ConcurrentHashMap<>();

    public SortSdkPrometheusMetricListener(String sortTaskId) {
        this.metricItem = new SortSdkMetricItem(sortTaskId);

        final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        StringBuilder beanName = new StringBuilder();
        beanName.append(JMX_DOMAIN).append(DOMAIN_SEPARATOR).append("type=SortSdk").append(",name=").append(sortTaskId);
        String strBeanName = beanName.toString();
        try {
            ObjectName objName = new ObjectName(strBeanName);
            mbs.registerMBean(metricItem, objName);
        } catch (Exception ex) {
            LOG.error("exception while register mbean:{},error:{}", strBeanName, ex.getMessage());
            LOG.error(ex.getMessage(), ex);
        }

        // consume
        metricValueMap.put(metricItem.M_CONSUME_SIZE, metricItem.consumeSize);
        metricValueMap.put(metricItem.M_CONSUME_MSG_COUNT, metricItem.consumeMsgCount);
        // callback
        metricValueMap.put(metricItem.M_CALL_BACK_COUNT, metricItem.callbackCount);
        metricValueMap.put(metricItem.M_CALL_BACK_DONE_COUNT, metricItem.callbackDoneCount);
        metricValueMap.put(metricItem.M_CALL_BACK_TIME_COST, metricItem.callbackTimeCost);
        metricValueMap.put(metricItem.M_CALL_BACK_FAIL_COUNT, metricItem.callbackFailCount);
        // topic
        metricValueMap.put(metricItem.M_TOPIC_ONLINE_COUNT, metricItem.topicOnlineCount);
        metricValueMap.put(metricItem.M_TOPIC_OFFLINE_COUNT, metricItem.topicOfflineCount);
        // ack
        metricValueMap.put(metricItem.M_ACK_FAIL_COUNT, metricItem.ackFailCount);
        metricValueMap.put(metricItem.M_ACK_SUCC_COUNT, metricItem.ackSuccCount);
        // request manager
        metricValueMap.put(metricItem.M_REQUEST_MANAGER_COUNT, metricItem.requestManagerCount);
        metricValueMap.put(metricItem.M_REQUEST_MANAGER_TIME_COST, metricItem.requestManagerTimeCost);
        metricValueMap.put(metricItem.M_REQUEST_MANAGER_FAIL_COUNT, metricItem.requestManagerFailCount);
        metricValueMap.put(metricItem.M_REQUEST_MANAGER_CONF_CHANAGED_COUNT, metricItem.requestManagerConfChangedCount);
        metricValueMap.put(metricItem.M_RQUEST_MANAGER_COMMON_ERROR_COUNT, metricItem.requestManagerCommonErrorCount);
        metricValueMap.put(metricItem.M_RQUEST_MANAGER_PARAM_ERROR_COUNT, metricItem.requestManagerParamErrorCount);

    }

    /**
     * snapshot
     *
     * @param domain
     * @param metrics
     */
    public void snapshot(String domain, Map<String, Long> metrics) {
        for (Map.Entry<String, Long> entry : metrics.entrySet()) {
            String fieldName = entry.getKey();
            AtomicLong metricValue = this.metricValueMap.get(fieldName);
            if (metricValue != null) {
                long fieldValue = entry.getValue();
                metricValue.addAndGet(fieldValue);
            }
        }
    }

}
