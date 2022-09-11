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

package org.apache.inlong.sdk.sort.stat;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.inlong.sdk.sort.api.Cleanable;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.MetricReporter;
import org.apache.inlong.sdk.sort.api.SortClientConfig;
import org.apache.inlong.sdk.sort.util.PeriodicTask;
import org.apache.inlong.sdk.sort.util.StringUtil;

public class StatManager implements Cleanable {

    private static final ConcurrentHashMap<String, SortClientStateCounter> READAPISTATE = new ConcurrentHashMap<>();
    private final SortClientConfig config;
    private final MetricReporter reporter;
    private final PeriodicTask processTask;
    private final String defaultCluster = "default";
    private final String defaultTopic = "default";
    private final int defaultPartitionId = 0;

    /**
     * StatManager Constructor
     *
     * @param context {@link ClientContext}
     * @param reporter {@link MetricReporter}
     */
    public StatManager(ClientContext context, MetricReporter reporter) {
        this.config = context.getConfig();
        this.reporter = reporter;
        this.processTask = new ProcessStatThread(config.getReportStatisticIntervalSec(),
                TimeUnit.SECONDS);
        String threadName = "sortsdk_stat_manager_process_data_"
                + StringUtil.formatDate(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
        processTask.start(threadName);
    }

    /**
     * clean
     *
     * @return true/false
     */
    @Override
    public boolean clean() {
        if (this.processTask != null) {
            processTask.stop();
        }

        if (reporter != null) {
            reporter.close();
        }

        return true;
    }

    private String makeKey(String... keys) {
        return String.join("|", keys);
    }

    /**
     * use for sortTaskId
     *
     * @param sortTaskId String
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter getStatistics(String sortTaskId) {
        String key = makeKey(sortTaskId, defaultCluster, defaultTopic, String.valueOf(defaultPartitionId));
        return READAPISTATE
                .computeIfAbsent(key,
                        k -> new SortClientStateCounter(sortTaskId, defaultCluster, defaultTopic, defaultPartitionId));
    }

    /**
     * use for pulsar type
     *
     * @param sortTaskId String
     * @param clusterId String
     * @param topic String
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter getStatistics(String sortTaskId, String clusterId, String topic) {
        String key = makeKey(sortTaskId, clusterId, topic, String.valueOf(defaultPartitionId));
        return READAPISTATE
                .computeIfAbsent(key,
                        k -> new SortClientStateCounter(sortTaskId, clusterId, topic, defaultPartitionId));
    }

    /**
     * use for common SortClientStateCounter
     *
     * @param sortTaskId String
     * @param clusterId String
     * @param topic String
     * @param partitionId int
     * @return {@link SortClientStateCounter}
     */
    public SortClientStateCounter getStatistics(String sortTaskId, String clusterId, String topic, int partitionId) {
        String key = makeKey(sortTaskId, clusterId, topic);
        return READAPISTATE
                .computeIfAbsent(key, k -> new SortClientStateCounter(sortTaskId, clusterId, topic, partitionId));
    }

    private class ProcessStatThread extends PeriodicTask {

        public ProcessStatThread(long runInterval, TimeUnit timeUnit) {
            super(runInterval, timeUnit, config);
        }

        @Override
        protected void doWork() {
            try {
                String monitorName = SortClientConfig.MONITOR_NAME;
                for (SortClientStateCounter offsetCounter : READAPISTATE.values()) {
                    SortClientStateCounter counter = offsetCounter.reset();
                    String[] keys = new String[]{counter.sortTaskId, config.getLocalIp(), counter.cacheClusterId,
                            counter.topic, String.valueOf(counter.partitionId)};
                    if (reporter != null) {
                        logger.debug("report statistics:{} {}", Arrays.toString(keys),
                                Arrays.toString(counter.getStatvalue()));
                        reporter.report(monitorName, keys, counter.getStatvalue());
                    } else {
                        logger.error("reporter is null");
                    }
                }
            } catch (Exception e) {
                logger.error("StatManager doWork error " + e.getMessage(), e);
            }
        }
    }
}
