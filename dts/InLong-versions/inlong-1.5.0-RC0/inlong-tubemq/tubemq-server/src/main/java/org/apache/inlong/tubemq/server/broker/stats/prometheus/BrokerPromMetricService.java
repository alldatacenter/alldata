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

package org.apache.inlong.tubemq.server.broker.stats.prometheus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.inlong.tubemq.server.broker.TubeBroker;
import org.apache.inlong.tubemq.server.broker.msgstore.MessageStore;
import org.apache.inlong.tubemq.server.broker.stats.BrokerSrvStatsHolder;
import org.apache.inlong.tubemq.server.common.fileconfig.PrometheusConfig;
import org.apache.inlong.tubemq.server.common.webbase.WebCallStatsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BrokerPromMetricService extends Collector {

    private static final Logger logger =
            LoggerFactory.getLogger(BrokerPromMetricService.class);
    private TubeBroker tubeBroker;
    private PrometheusConfig promConfig = new PrometheusConfig();
    private HTTPServer httpServer;
    private volatile boolean started = false;

    public BrokerPromMetricService(TubeBroker tubeBroker,
            PrometheusConfig prometheusConfig) {
        if (prometheusConfig == null || !prometheusConfig.isPromEnable()) {
            return;
        }
        this.tubeBroker = tubeBroker;
        this.promConfig = prometheusConfig;
    }

    public void start() {
        try {
            this.httpServer = new HTTPServer(promConfig.getPromHttpPort());
            this.register();
            this.started = true;
        } catch (IOException e) {
            logger.error("exception while register Broker prometheus http server, error:{}",
                    e.getMessage());
        }
    }

    @Override
    public List<MetricFamilySamples> collect() {
        final List<MetricFamilySamples> mfs = new ArrayList<>();
        if (!started) {
            return mfs;
        }
        // service status metric data
        Map<String, Long> statsMap = new LinkedHashMap<>();
        StringBuilder strBuff = new StringBuilder(512);
        CounterMetricFamily srvStatusCounter =
                new CounterMetricFamily(strBuff.append(promConfig.getPromClusterName())
                        .append("&group=serviceStatus").toString(),
                        "The service status metrics of TubeMQ-Broker node.",
                        Arrays.asList("serviceStatus"));
        strBuff.delete(0, strBuff.length());
        BrokerSrvStatsHolder.snapShort(statsMap);
        for (Map.Entry<String, Long> entry : statsMap.entrySet()) {
            srvStatusCounter.addMetric(Arrays.asList(entry.getKey()), entry.getValue());
        }
        mfs.add(srvStatusCounter);
        // web api call status metric data
        CounterMetricFamily webAPICounter =
                new CounterMetricFamily(strBuff.append(promConfig.getPromClusterName())
                        .append("&group=webAPI").toString(),
                        "The web api call metrics of TubeMQ-Broker node.",
                        Arrays.asList("webAPI"));
        strBuff.delete(0, strBuff.length());
        statsMap.clear();
        WebCallStatsHolder.snapShort(statsMap);
        for (Map.Entry<String, Long> entry : statsMap.entrySet()) {
            webAPICounter.addMetric(Arrays.asList(entry.getKey()), entry.getValue());
        }
        mfs.add(webAPICounter);
        // msg store metric data
        CounterMetricFamily msgStoreCounter =
                new CounterMetricFamily(strBuff.append(promConfig.getPromClusterName())
                        .append("&group=msgStore").toString(),
                        "The message store metrics of TubeMQ-Broker node.",
                        Arrays.asList("msgStore"));
        strBuff.delete(0, strBuff.length());
        // set topic's statistic status
        List<String> labelValues = new ArrayList<>();
        Map<String, ConcurrentHashMap<Integer, MessageStore>> msgTopicStores =
                tubeBroker.getStoreManager().getMessageStores();
        for (ConcurrentHashMap<Integer, MessageStore> storeMap : msgTopicStores.values()) {
            if (storeMap == null) {
                continue;
            }
            for (MessageStore msgStore : storeMap.values()) {
                if (msgStore == null) {
                    continue;
                }
                statsMap.clear();
                msgStore.getMsgStoreStatsHolder().snapShort(statsMap);
                for (Map.Entry<String, Long> entry : statsMap.entrySet()) {
                    labelValues.clear();
                    labelValues.add(entry.getKey());
                    labelValues.add(strBuff.append("topicName=")
                            .append(msgStore.getTopic()).toString());
                    strBuff.delete(0, strBuff.length());
                    labelValues.add(strBuff.append("storeId=")
                            .append(msgStore.getStoreId()).toString());
                    strBuff.delete(0, strBuff.length());
                    msgStoreCounter.addMetric(labelValues, entry.getValue());
                }
            }
        }
        mfs.add(msgStoreCounter);
        return mfs;
    }
}
