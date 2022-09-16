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

package org.apache.inlong.tubemq.server.broker.stats;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.apache.inlong.tubemq.corebase.metric.MetricMXBean;
import org.apache.inlong.tubemq.server.common.webbase.WebCallStatsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * BrokerJMXHolder
 *
 * A wrapper class for Broker JMX metric display, which currently includes RPC service status
 * and web API call status metric data output
 */
public class BrokerJMXHolder {
    private static final Logger logger =
            LoggerFactory.getLogger(BrokerJMXHolder.class);
    // Registration status indicator
    private static final AtomicBoolean registered = new AtomicBoolean(false);
    // broker metrics information
    private static final BrokerServiceStatusBean serviceStatusInfo =
            new BrokerServiceStatusBean();
    // broker web api status information
    private static final BrokerWebAPIStatusBean webAPIStatusInfo =
            new BrokerWebAPIStatusBean();

    /**
     * Register MXBean
     *
     */
    public static void registerMXBean() {
        if (!registered.compareAndSet(false, true)) {
            return;
        }
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            // register service status jmx
            ObjectName srvStatusMxBeanName =
                    new ObjectName("org.apache.inlong.tubemq.server.broker:type=serviceStatus");
            mbs.registerMBean(serviceStatusInfo, srvStatusMxBeanName);
            // register web api status jmx
            ObjectName webAPIMxBeanName =
                    new ObjectName("org.apache.inlong.tubemq.server.broker:type=webAPI");
            mbs.registerMBean(webAPIStatusInfo, webAPIMxBeanName);

        } catch (Exception ex) {
            logger.error("Register Broker MXBean error: ", ex);
        }
    }

    /**
     * BrokerServiceStatusBean
     *
     * Broker service status metric wrapper class
     */
    private static class BrokerServiceStatusBean implements MetricMXBean {

        @Override
        public Map<String, Long> getValue() {
            Map<String, Long> metricValues = new LinkedHashMap<>();
            BrokerSrvStatsHolder.getValue(metricValues);
            return metricValues;
        }

        @Override
        public Map<String, Long> snapshot() {
            Map<String, Long> metricValues = new LinkedHashMap<>();
            BrokerSrvStatsHolder.snapShort(metricValues);
            return metricValues;
        }
    }

    /**
     * BrokerWebAPIStatusBean
     *
     * Broker web api status metric wrapper class
     */
    private static class BrokerWebAPIStatusBean implements MetricMXBean {

        @Override
        public Map<String, Long> getValue() {
            Map<String, Long> metricValues = new LinkedHashMap<>();
            WebCallStatsHolder.getValue(metricValues);
            return metricValues;
        }

        @Override
        public Map<String, Long> snapshot() {
            Map<String, Long> metricValues = new LinkedHashMap<>();
            WebCallStatsHolder.snapShort(metricValues);
            return metricValues;
        }
    }
}

