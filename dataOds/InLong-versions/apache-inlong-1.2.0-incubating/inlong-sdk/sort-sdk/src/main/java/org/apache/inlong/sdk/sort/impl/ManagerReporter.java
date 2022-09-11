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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.inlong.sdk.sort.api.ClientContext;
import org.apache.inlong.sdk.sort.api.InLongTopicFetcher;
import org.apache.inlong.sdk.sort.api.InLongTopicManager;
import org.apache.inlong.sdk.sort.api.ManagerReportHandler;
import org.apache.inlong.sdk.sort.api.ReportApi;
import org.apache.inlong.sdk.sort.entity.ConsumeState;
import org.apache.inlong.sdk.sort.entity.ConsumeStatusParams;
import org.apache.inlong.sdk.sort.entity.ConsumeStatusResult;
import org.apache.inlong.sdk.sort.entity.HeartBeatParams;
import org.apache.inlong.sdk.sort.entity.HeartBeatResult;
import org.apache.inlong.sdk.sort.util.PeriodicTask;

public class ManagerReporter extends PeriodicTask {

    private final ConcurrentHashMap<Integer, Long> reportApiRunTimeMs = new ConcurrentHashMap<>();
    private final ClientContext context;
    private final InLongTopicManager inLongTopicManager;
    private final ManagerReportHandler reportHandler;
    private Map<Integer, Long> reportApiInterval = new HashMap<>();

    /**
     * ManagerReporter Constructor
     *
     * @param context ClientContext
     * @param reportHandler ManagerReportHandler
     * @param inLongTopicManager InLongTopicManager
     * @param runInterval long
     * @param timeUnit TimeUnit
     */
    public ManagerReporter(ClientContext context, ManagerReportHandler reportHandler,
            InLongTopicManager inLongTopicManager,
            long runInterval, TimeUnit timeUnit) {
        super(runInterval, timeUnit, context.getConfig());
        this.context = context;
        this.reportHandler = reportHandler;
        this.inLongTopicManager = inLongTopicManager;
    }

    @Override
    protected void doWork() {
        reportManager();
    }

    private void reportManager() {
        //1.report heartbeat to manager
        heartBeat();
        //2.report consume status to manager
        updateConsumeStatus();
    }

    private long getReportInterval(int methodId) {
        Long reportIntervalMs = reportApiInterval.get(methodId);
        if (reportIntervalMs == null) {
            //default report interval 5s
            reportIntervalMs = 5000L;
        }
        return reportIntervalMs;
    }

    private boolean canReport(int methodId) {
        long reportIntervalMs = getReportInterval(methodId);
        Long lastRunTimeMs = reportApiRunTimeMs.get(methodId);
        return lastRunTimeMs == null || ((System.currentTimeMillis() - lastRunTimeMs) >= reportIntervalMs);
    }

    private void setReportTimeMs(int methodId, long timeMs) {
        reportApiRunTimeMs.put(methodId, timeMs);
    }

    private boolean checkCanNotReportFetchStatus() {
        int methodId = ReportApi.UPDATE_FETCH_STATUS.value();
        boolean canReport = canReport(methodId);
        if (!canReport) {
            return true;
        }
        setReportTimeMs(methodId, System.currentTimeMillis());
        return false;
    }

    private void handleHeartBeatResult(HeartBeatResult heartbeatResult) {
        if (heartbeatResult != null) {
            if (heartbeatResult.getReportInterval() != null
                    && heartbeatResult.getReportInterval().size() > 0) {
                reportApiInterval = heartbeatResult.getReportInterval();
            }
        }
    }

    private boolean checkCanNotReport(int methodId) {
        if (!canReport(methodId)) {
            return true;
        }
        setReportTimeMs(methodId, System.currentTimeMillis());
        return false;
    }

    private void heartBeat() {
        if (null == reportHandler) {
            logger.error("heartBeat reportHandler is null!!");
            return;
        }

        int methodId = ReportApi.HEARTBEAT.value();
        if (checkCanNotReport(methodId)) {
            return;
        }

        try {
            HeartBeatParams heartBeatParams = new HeartBeatParams();
            heartBeatParams.setSortTaskId(context.getConfig().getSortTaskId());
            heartBeatParams.setIp(context.getConfig().getLocalIp());
            HeartBeatResult heartBeatResult = reportHandler.heartbeat(heartBeatParams);
            handleHeartBeatResult(heartBeatResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleConsumeStatusResult(ConsumeStatusResult consumeStatusResult) {
    }

    private void updateConsumeStatus() {
        if (null == reportHandler) {
            logger.error("updateFetchStatus reportHandler is null!!");
            return;
        }
        if (checkCanNotReportFetchStatus()) {
            return;
        }
        try {
            ConsumeStatusParams consumeStatusParams = new ConsumeStatusParams();
            consumeStatusParams.setSubscribedId(context.getConfig().getSortTaskId());
            consumeStatusParams.setIp(context.getConfig().getLocalIp());
            List<ConsumeState> consumeStates = new ArrayList<>();
            Collection<InLongTopicFetcher> allFetchers =
                    inLongTopicManager.getAllFetchers();
            for (InLongTopicFetcher fetcher : allFetchers) {
                ConsumeState consumeState = new ConsumeState();
                consumeState.setTopic(fetcher.getInLongTopic().getTopic());
                consumeState.setTopicType(fetcher.getInLongTopic().getTopicType());
                consumeState.setClusterId(fetcher.getInLongTopic().getInLongCluster().getClusterId());
                consumeState.setConsumedDataSize(fetcher.getConsumedDataSize());
                consumeState.setAckOffset(fetcher.getAckedOffset());
                consumeState.setPartition(fetcher.getInLongTopic().getPartitionId());
                consumeStates.add(consumeState);
            }
            consumeStatusParams.setConsumeStates(consumeStates);

            ConsumeStatusResult consumeStatusResult = reportHandler.updateConsumeStatus(consumeStatusParams);
            handleConsumeStatusResult(consumeStatusResult);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }
}
