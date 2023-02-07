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

package org.apache.inlong.tubemq.server.master.stats.prometheus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import io.prometheus.client.Collector;
import io.prometheus.client.CounterMetricFamily;
import io.prometheus.client.exporter.HTTPServer;
import org.apache.inlong.tubemq.server.common.fileconfig.PrometheusConfig;
import org.apache.inlong.tubemq.server.common.webbase.WebCallStatsHolder;
import org.apache.inlong.tubemq.server.master.stats.MasterSrvStatsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MasterPromMetricService extends Collector {

    private static final Logger logger =
            LoggerFactory.getLogger(MasterPromMetricService.class);
    private PrometheusConfig promConfig = new PrometheusConfig();
    private HTTPServer httpServer;
    private volatile boolean started = false;

    public MasterPromMetricService(PrometheusConfig prometheusConfig) {
        if (prometheusConfig == null || !prometheusConfig.isPromEnable()) {
            return;
        }
        this.promConfig = prometheusConfig;
    }

    public void start() {
        try {
            this.httpServer = new HTTPServer(promConfig.getPromHttpPort());
            this.register();
            this.started = true;
        } catch (IOException e) {
            logger.error("exception while register Master prometheus http server, error:{}",
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
                        "The service status metrics of TubeMQ-Master node.",
                        Arrays.asList("serviceStatus"));
        strBuff.delete(0, strBuff.length());
        MasterSrvStatsHolder.snapShort(statsMap);
        for (Map.Entry<String, Long> entry : statsMap.entrySet()) {
            srvStatusCounter.addMetric(Arrays.asList(entry.getKey()), entry.getValue());
        }
        mfs.add(srvStatusCounter);
        // web api call status metric data
        CounterMetricFamily webAPICounter =
                new CounterMetricFamily(strBuff.append(promConfig.getPromClusterName())
                        .append("&group=webAPI").toString(),
                        "The web api call metrics of TubeMQ-Master node.",
                        Arrays.asList("webAPI"));
        strBuff.delete(0, strBuff.length());
        statsMap.clear();
        WebCallStatsHolder.snapShort(statsMap);
        for (Map.Entry<String, Long> entry : statsMap.entrySet()) {
            webAPICounter.addMetric(Arrays.asList(entry.getKey()), entry.getValue());
        }
        mfs.add(webAPICounter);
        return mfs;
    }
}
