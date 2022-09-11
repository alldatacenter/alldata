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

package org.apache.inlong.common.reporpter;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.time.Instant;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.apache.inlong.common.reporpter.dto.StreamConfigLogInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StreamConfigLogMetric implements Runnable {

    public static final Logger LOGGER = LoggerFactory.getLogger(StreamConfigLogMetric.class);

    /*
     * config name for log report
     */
    public static final String CONFIG_LOG_REPORT_ENABLE = "report.config.log.enable";
    public static final String CONFIG_LOG_REPORT_SERVER_URL = "report.config.log.server.url";
    public static final String CONFIG_LOG_REPORT_INTERVAL = "report.config.log.interval";
    public static final String CONFIG_LOG_REPORT_CLIENT_VERSION = "report.config.log.client.version";
    public static final String CONFIG_LOG_PULSAR_PRODUCER = "pulsar-producer";
    public static final String CONFIG_LOG_PULSAR_CLIENT = "pulsar-client";

    private StreamConfigLogReporter streamConfigLogReporter;

    private String moduleName;

    private String clientVersion;

    private String localIp;

    private long reportInterval;

    public ConcurrentHashMap<String, StreamConfigLogInfo> dataCacheMap = new ConcurrentHashMap<>();

    private static ScheduledExecutorService statExecutor =
            Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
                    .setNameFormat("StreamConfigLogMetric-Report")
                    .setUncaughtExceptionHandler((t, e) ->
                            LOGGER.error(t.getName() + " has an uncaught exception: ", e))
                    .build());

    public StreamConfigLogMetric(String moduleName, String serverUrl, long reportInterval,
            String localIp, String clientVersion) {
        this.streamConfigLogReporter = new StreamConfigLogReporter(serverUrl);
        this.reportInterval = reportInterval;
        this.moduleName = moduleName;
        this.localIp = localIp;
        this.clientVersion = clientVersion;
        statExecutor.scheduleWithFixedDelay(this,
                reportInterval, reportInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * updateConfigLog
     * @param inlongGroupId  inlongGroupId
     * @param inlongStreamId inlongStreamId
     * @param configName configName
     * @param configLogTypeEnum configLogTypeEnum
     * @param log log
     */
    public void updateConfigLog(String inlongGroupId, String inlongStreamId, String configName,
            ConfigLogTypeEnum configLogTypeEnum, String log) {
        String key = moduleName + "-" +  inlongGroupId + "-" + inlongStreamId + "-" + configName;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("updateConfigLog key = {}", key);
        }
        dataCacheMap.compute(key, (k, v) -> {
            if (v == null) {
                v = new StreamConfigLogInfo();
            }
            updateDataValue(v, inlongGroupId,
                    inlongStreamId, configName, configLogTypeEnum, log);
            return v;
        });
    }

    /**
     * Update value by config.
     */
    private void updateDataValue(StreamConfigLogInfo streamConfigLogInfo,
            String inlongGroupId, String inlongStreamId, String configName,
            ConfigLogTypeEnum configLogTypeEnum, String log) {
        streamConfigLogInfo.setComponentName(moduleName);
        streamConfigLogInfo.setConfigName(configName);
        streamConfigLogInfo.setInlongGroupId(inlongGroupId);
        streamConfigLogInfo.setInlongStreamId(inlongStreamId);
        streamConfigLogInfo.setIp(localIp);
        streamConfigLogInfo.setVersion(clientVersion);
        streamConfigLogInfo.setLogInfo(log);
        streamConfigLogInfo.setReportTime(Instant.now().toEpochMilli());
        streamConfigLogInfo.setLogType(configLogTypeEnum.getType());
    }

    /**
     * Logic entrance of Metric.
     */
    public void run() {
        try {
            Set<Entry<String, StreamConfigLogInfo>> set = dataCacheMap.entrySet();
            long currentTimeMills = Instant.now().toEpochMilli();
            for (Entry<String, StreamConfigLogInfo> entry : set) {
                StreamConfigLogInfo streamConfigLogInfo = entry.getValue();
                if ((currentTimeMills - streamConfigLogInfo.getReportTime()) < reportInterval) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Report metric data config key = {}!", streamConfigLogInfo.getConfigName());
                    }
                    streamConfigLogReporter.asyncReportData(streamConfigLogInfo);
                } else {
                    dataCacheMap.remove(entry.getKey());
                    LOGGER.info("Remove expired config key {}", entry.getKey());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Report streamConfigLogMetric has exception = {}", e);
        }
    }
}
