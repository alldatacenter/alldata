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

package org.apache.inlong.audit;

import org.apache.inlong.audit.protocol.AuditApi;
import org.apache.inlong.audit.send.SenderManager;
import org.apache.inlong.audit.util.AuditConfig;
import org.apache.inlong.audit.util.Config;
import org.apache.inlong.audit.util.StatInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static org.apache.inlong.audit.protocol.AuditApi.BaseCommand.Type.AUDITREQUEST;

public class AuditImp {
    private static final Logger logger = LoggerFactory.getLogger(AuditImp.class);
    private static AuditImp auditImp = new AuditImp();
    private static final String FIELD_SEPARATORS = ":";
    private ConcurrentHashMap<String, StatInfo> countMap = new ConcurrentHashMap<String, StatInfo>();
    private HashMap<String, StatInfo> threadSumMap = new HashMap<String, StatInfo>();
    private ConcurrentHashMap<String, StatInfo> deleteCountMap = new ConcurrentHashMap<String, StatInfo>();
    private List<String> deleteKeyList = new ArrayList<String>();
    private AuditConfig auditConfig = null;
    private Config config = new Config();
    private Long sdkTime;
    private int packageId = 1;
    private int dataId = 0;
    private static final int BATCH_NUM = 100;
    boolean inited = false;
    private SenderManager manager;
    private static ReentrantLock globalLock = new ReentrantLock();
    private static int PERIOD = 1000 * 60;
    private Timer timer = new Timer();
    private TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            try {
                sendReport();
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }
    };

    public static AuditImp getInstance() {
        return auditImp;
    }

    /**
     * init
     */
    private void init() {
        if (inited) {
            return;
        }
        config.init();
        timer.schedule(timerTask, PERIOD, PERIOD);
        if (auditConfig == null) {
            auditConfig = new AuditConfig();
        }
        this.manager = new SenderManager(auditConfig);
    }

    /**
     * setAuditProxy
     *
     * @param ipPortList
     */
    public void setAuditProxy(HashSet<String> ipPortList) {
        try {
            globalLock.lockInterruptibly();
            if (!inited) {
                init();
                inited = true;
            }
            this.manager.setAuditProxy(ipPortList);
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        } finally {
            globalLock.unlock();
        }
    }

    /**
     * set audit config
     *
     * @param config
     */
    public void setAuditConfig(AuditConfig config) {
        auditConfig = config;
        manager.setAuditConfig(config);
    }

    /**
     * api
     *
     * @param auditID
     * @param inlongGroupID
     * @param inlongStreamID
     * @param logTime
     * @param count
     * @param size
     */
    public void add(int auditID, String inlongGroupID, String inlongStreamID, Long logTime, long count, long size) {
        long delayTime = System.currentTimeMillis() - logTime;
        String key = (logTime / PERIOD) + FIELD_SEPARATORS + inlongGroupID + FIELD_SEPARATORS
                + inlongStreamID + FIELD_SEPARATORS + auditID;
        addByKey(key, count, size, delayTime);
    }

    /**
     * add by key
     *
     * @param key
     * @param count
     * @param size
     * @param delayTime
     */
    private void addByKey(String key, long count, long size, long delayTime) {
        try {
            if (countMap.get(key) == null) {
                countMap.put(key, new StatInfo(0L, 0L, 0L));
            }
            countMap.get(key).count.addAndGet(count);
            countMap.get(key).size.addAndGet(size);
            countMap.get(key).delay.addAndGet(delayTime * count);
        } catch (Exception e) {
            return;
        }
    }

    /**
     * Report audit data
     */
    public synchronized void sendReport() {
        manager.clearBuffer();
        resetStat();
        // Retrieve statistics from the list of objects without statistics to be eliminated
        for (Map.Entry<String, StatInfo> entry : this.deleteCountMap.entrySet()) {
            this.sumThreadGroup(entry.getKey(), entry.getValue());
        }
        this.deleteCountMap.clear();
        for (Map.Entry<String, StatInfo> entry : countMap.entrySet()) {
            String key = entry.getKey();
            StatInfo value = entry.getValue();
            // If there is no data, enter the list to be eliminated
            if (value.count.get() == 0) {
                this.deleteKeyList.add(key);
                continue;
            }
            this.sumThreadGroup(key, value);
        }

        // Clean up obsolete statistical data objects
        for (String key : this.deleteKeyList) {
            StatInfo value = this.countMap.remove(key);
            this.deleteCountMap.put(key, value);
        }
        this.deleteKeyList.clear();
        sdkTime = Calendar.getInstance().getTimeInMillis();
        AuditApi.AuditMessageHeader mssageHeader = AuditApi.AuditMessageHeader.newBuilder()
                .setIp(config.getLocalIP()).setDockerId(config.getDockerId())
                .setThreadId(String.valueOf(Thread.currentThread().getId()))
                .setSdkTs(sdkTime).setPacketId(packageId)
                .build();
        AuditApi.AuditRequest.Builder requestBulid = AuditApi.AuditRequest.newBuilder();
        requestBulid.setMsgHeader(mssageHeader).setRequestId(manager.nextRequestId());
        for (Map.Entry<String, StatInfo> entry : threadSumMap.entrySet()) {
            String[] keyArray = entry.getKey().split(FIELD_SEPARATORS);
            long logTime = Long.parseLong(keyArray[0]) * PERIOD;
            String inlongGroupID = keyArray[1];
            String inlongStreamID = keyArray[2];
            String auditID = keyArray[3];
            StatInfo value = entry.getValue();
            AuditApi.AuditMessageBody mssageBody = AuditApi.AuditMessageBody.newBuilder()
                    .setLogTs(logTime).setInlongGroupId(inlongGroupID)
                    .setInlongStreamId(inlongStreamID).setAuditId(auditID)
                    .setCount(value.count.get()).setSize(value.size.get())
                    .setDelay(value.delay.get())
                    .build();
            requestBulid.addMsgBody(mssageBody);
            if (dataId++ >= BATCH_NUM) {
                dataId = 0;
                packageId++;
                sendByBaseCommand(sdkTime, requestBulid.build());
                requestBulid.clearMsgBody();
            }
        }
        if (requestBulid.getMsgBodyCount() > 0) {
            sendByBaseCommand(sdkTime, requestBulid.build());
            requestBulid.clearMsgBody();
        }
        threadSumMap.clear();
        logger.info("finish send report.");
    }

    /**
     * send base command
     *
     * @param sdkTime
     * @param auditRequest
     */
    private void sendByBaseCommand(long sdkTime, AuditApi.AuditRequest auditRequest) {
        AuditApi.BaseCommand.Builder baseCommand = AuditApi.BaseCommand.newBuilder();
        baseCommand.setType(AUDITREQUEST).setAuditRequest(auditRequest).build();
        manager.send(sdkTime, baseCommand.build());
    }

    /**
     * Summary
     *
     * @param key
     * @param statInfo
     */
    private void sumThreadGroup(String key, StatInfo statInfo) {
        long count = statInfo.count.getAndSet(0);
        if (0 == count) {
            return;
        }
        if (threadSumMap.get(key) == null) {
            threadSumMap.put(key, new StatInfo(0, 0, 0));
        }

        long size = statInfo.size.getAndSet(0);
        long delay = statInfo.delay.getAndSet(0);

        threadSumMap.get(key).count.addAndGet(count);
        threadSumMap.get(key).size.addAndGet(size);
        threadSumMap.get(key).delay.addAndGet(delay);
    }

    /**
     * Reset statistics
     */
    private void resetStat() {
        dataId = 0;
        packageId = 1;
    }
}
