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

package org.apache.inlong.tubemq.server.master.stats;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.metric.impl.ESTHistogram;
import org.apache.inlong.tubemq.corebase.metric.impl.LongOnlineCounter;
import org.apache.inlong.tubemq.corebase.metric.impl.LongStatsCounter;
import org.apache.inlong.tubemq.corebase.metric.impl.SinceTime;

/**
 * MasterSrvStatsHolder, statistics Master's RPC service metric information
 *
 * This class counts the number of consumer groups, timeouts, the load balancing duration
 * distribution of consumer groups, as well as the total number of registered consumers
 * in the system, the number of timeouts, the number of tasks being processed,
 * the total number of producers, the total number of timeouts,
 * and Broker registration and timeouts, etc.
 */
public class MasterSrvStatsHolder {
    // online consume group count statistic
    private static final LongOnlineCounter csmOnlineGroupCnt =
            new LongOnlineCounter("csm_online_group_cnt", null);
    // online client-balance consume group count statistic
    private static final LongOnlineCounter csmCltBalanceGroupCnt =
            new LongOnlineCounter("csm_client_bal_group_cnt", null);
    // online consumer count statistic
    private static final LongOnlineCounter consumerOnlineCnt =
            new LongOnlineCounter("consumer_online_cnt", null);
    // in balance with connection event consumer count statistic
    private static final LongOnlineCounter consumerInConnectCount =
            new LongOnlineCounter("consumer_con_event_cnt", null);
    // in balance with disconnection event consumer count statistic
    private static final LongOnlineCounter consumerInDisConnectCount =
            new LongOnlineCounter("consumer_discon_event_cnt", null);
    // online producer count statistic
    private static final LongOnlineCounter producerOnlineCnt =
            new LongOnlineCounter("producer_online_cnt", null);
    // configured broker count statistic
    private static final LongOnlineCounter brokerConfiguredCnt =
            new LongOnlineCounter("broker_configured_cnt", null);
    // broker online count statistic
    private static final LongOnlineCounter brokerOnlineCnt =
            new LongOnlineCounter("broker_online_cnt", null);
    // broker abnormal count statistic
    private static final LongOnlineCounter brokerAbnCurCnt =
            new LongOnlineCounter("broker_abnormal_cnt", null);
    // broker forbidden count statistic
    private static final LongOnlineCounter brokerFbdCurCnt =
            new LongOnlineCounter("broker_forbidden_cnt", null);
    // Switchable statistic items
    private static final ServiceStatsSet[] switchableSets = new ServiceStatsSet[2];
    // Current writable index
    private static final AtomicInteger writableIndex = new AtomicInteger(0);
    // Last snapshot time
    private static final AtomicLong lstSnapshotTime = new AtomicLong(0);

    // Initial service statistic set
    static {
        switchableSets[0] = new ServiceStatsSet();
        switchableSets[1] = new ServiceStatsSet();
    }

    // metric set operate APIs begin
    public static void getValue(Map<String, Long> statsMap) {
        getStatsValue(switchableSets[getIndex()], false, statsMap);
    }

    public static void getValue(StringBuilder strBuff) {
        getStatsValue(switchableSets[getIndex()], false, strBuff);
    }

    public static void snapShort(Map<String, Long> statsMap) {
        if (switchWritingStatsUnit()) {
            getStatsValue(switchableSets[getIndex(writableIndex.get() - 1)], true, statsMap);
        } else {
            getStatsValue(switchableSets[getIndex()], false, statsMap);
        }
    }

    public static void snapShort(StringBuilder strBuff) {
        if (switchWritingStatsUnit()) {
            getStatsValue(switchableSets[getIndex(writableIndex.get() - 1)], true, strBuff);
        } else {
            getStatsValue(switchableSets[getIndex()], false, strBuff);
        }
    }
    // metric set operate APIs end

    // metric item operate APIs begin
    public static void incConsumerCnt(boolean isGroupEmpty, boolean isCltBal) {
        consumerOnlineCnt.incValue();
        if (isGroupEmpty) {
            csmOnlineGroupCnt.incValue();
            if (isCltBal) {
                csmCltBalanceGroupCnt.incValue();
            }
        }
    }

    public static void decConsumerCnt(boolean isTimeout,
                                      boolean isGroupEmpty,
                                      boolean isCltBal) {
        consumerOnlineCnt.decValue();
        if (isTimeout) {
            switchableSets[getIndex()].consumerTmoTotCnt.incValue();
        }
        if (isGroupEmpty) {
            decConsumeGroupCnt(isTimeout, isCltBal);
        }
    }

    public static void decConsumeGroupCnt(boolean isTimeout, boolean isCltBal) {
        csmOnlineGroupCnt.decValue();
        if (isTimeout) {
            switchableSets[getIndex()].csmGroupTimeoutCnt.incValue();
        }
        if (isCltBal) {
            csmCltBalanceGroupCnt.decValue();
            if (isTimeout) {
                switchableSets[getIndex()].cltBalGroupTmototCnt.incValue();
            }
        }
    }

    public static void incProducerCnt() {
        producerOnlineCnt.incValue();
    }

    public static void decProducerCnt(boolean isTimeout) {
        producerOnlineCnt.decValue();
        if (isTimeout) {
            switchableSets[getIndex()].producerTmoTotCnt.incValue();
        }
    }

    public static void incSvrBalDisConConsumerCnt() {
        consumerInDisConnectCount.incValue();
    }

    public static void decSvrBalDisConConsumerCnt() {
        consumerInDisConnectCount.decValue();
    }

    public static void incSvrBalConEventConsumerCnt() {
        consumerInConnectCount.incValue();
    }

    public static void decSvrBalConEventConsumerCnt() {
        consumerInConnectCount.decValue();
    }

    public static void incBrokerConfigCnt() {
        brokerConfiguredCnt.incValue();
    }

    public static void decBrokerConfigCnt() {
        brokerConfiguredCnt.decValue();
    }

    public static void incBrokerOnlineCnt() {
        brokerOnlineCnt.incValue();
    }

    public static void decBrokerOnlineCnt(boolean isTimeout) {
        brokerOnlineCnt.decValue();
        if (isTimeout) {
            switchableSets[getIndex()].brokerTmoTotCnt.incValue();
        }
    }

    public static void incBrokerAbnormalCnt() {
        brokerAbnCurCnt.incValue();
    }

    public static void decBrokerAbnormalCnt() {
        brokerAbnCurCnt.decValue();
    }

    public static void incBrokerForbiddenCnt() {
        brokerFbdCurCnt.incValue();
    }

    public static void decBrokerForbiddenCnt() {
        brokerFbdCurCnt.decValue();
    }

    public static void updSvrBalanceDurations(long dltTime) {
        switchableSets[getIndex()].svrNormalBalanceStats.update(dltTime);
    }

    public static void updSvrBalResetDurations(long dltTime) {
        switchableSets[getIndex()].svrResetBalanceStats.update(dltTime);
    }
    // metric set operate APIs end

    // private functions
    private static boolean switchWritingStatsUnit() {
        long curSnapshotTime = lstSnapshotTime.get();
        // Avoid frequent snapshots
        if ((System.currentTimeMillis() - curSnapshotTime)
                >= TBaseConstants.CFG_STATS_MIN_SNAPSHOT_PERIOD_MS) {
            if (lstSnapshotTime.compareAndSet(curSnapshotTime, System.currentTimeMillis())) {
                switchableSets[getIndex(writableIndex.incrementAndGet())].resetSinceTime();
                return true;
            }
        }
        return false;
    }

    private static void getStatsValue(ServiceStatsSet statsSet,
                                      boolean resetValue,
                                      Map<String, Long> statsMap) {
        statsMap.put(statsSet.lstResetTime.getFullName(),
                statsSet.lstResetTime.getSinceTime());
        if (resetValue) {
            // for consume group
            statsMap.put(csmOnlineGroupCnt.getFullName(),
                    csmOnlineGroupCnt.getAndResetValue());
            statsMap.put(statsSet.csmGroupTimeoutCnt.getFullName(),
                    statsSet.csmGroupTimeoutCnt.getAndResetValue());
            statsMap.put(csmCltBalanceGroupCnt.getFullName(),
                    csmCltBalanceGroupCnt.getAndResetValue());
            statsMap.put(statsSet.cltBalGroupTmototCnt.getFullName(),
                    statsSet.cltBalGroupTmototCnt.getAndResetValue());
            statsSet.svrNormalBalanceStats.snapShort(statsMap, false);
            statsSet.svrResetBalanceStats.snapShort(statsMap, false);
            // for consumer
            statsMap.put(consumerOnlineCnt.getFullName(),
                    consumerOnlineCnt.getAndResetValue());
            statsMap.put(statsSet.consumerTmoTotCnt.getFullName(),
                    statsSet.consumerTmoTotCnt.getAndResetValue());
            statsMap.put(consumerInConnectCount.getFullName(),
                    consumerInConnectCount.getAndResetValue());
            statsMap.put(consumerInDisConnectCount.getFullName(),
                    consumerInDisConnectCount.getAndResetValue());
            // for producer
            statsMap.put(producerOnlineCnt.getFullName(),
                    producerOnlineCnt.getAndResetValue());
            statsMap.put(statsSet.producerTmoTotCnt.getFullName(),
                    statsSet.producerTmoTotCnt.getAndResetValue());
            // for broker
            statsMap.put(brokerConfiguredCnt.getFullName(),
                    brokerConfiguredCnt.getAndResetValue());
            statsMap.put(brokerOnlineCnt.getFullName(),
                    brokerOnlineCnt.getAndResetValue());
            statsMap.put(statsSet.brokerTmoTotCnt.getFullName(),
                    statsSet.brokerTmoTotCnt.getAndResetValue());
            statsMap.put(brokerAbnCurCnt.getFullName(),
                    brokerAbnCurCnt.getAndResetValue());
            statsMap.put(brokerFbdCurCnt.getFullName(),
                    brokerFbdCurCnt.getAndResetValue());
        } else {
            // for consume group
            statsMap.put(csmOnlineGroupCnt.getFullName(),
                    csmOnlineGroupCnt.getValue());
            statsMap.put(statsSet.csmGroupTimeoutCnt.getFullName(),
                    statsSet.csmGroupTimeoutCnt.getValue());
            statsMap.put(csmCltBalanceGroupCnt.getFullName(),
                    csmCltBalanceGroupCnt.getValue());
            statsMap.put(statsSet.cltBalGroupTmototCnt.getFullName(),
                    statsSet.cltBalGroupTmototCnt.getValue());
            statsSet.svrNormalBalanceStats.getValue(statsMap, false);
            statsSet.svrResetBalanceStats.getValue(statsMap, false);
            // for consumer
            statsMap.put(consumerOnlineCnt.getFullName(),
                    consumerOnlineCnt.getValue());
            statsMap.put(statsSet.consumerTmoTotCnt.getFullName(),
                    statsSet.consumerTmoTotCnt.getValue());
            statsMap.put(consumerInConnectCount.getFullName(),
                    consumerInConnectCount.getValue());
            statsMap.put(consumerInDisConnectCount.getFullName(),
                    consumerInDisConnectCount.getValue());
            // for producer
            statsMap.put(producerOnlineCnt.getFullName(),
                    producerOnlineCnt.getValue());
            statsMap.put(statsSet.producerTmoTotCnt.getFullName(),
                    statsSet.producerTmoTotCnt.getValue());
            // for broker
            statsMap.put(brokerConfiguredCnt.getFullName(),
                    brokerConfiguredCnt.getValue());
            statsMap.put(brokerOnlineCnt.getFullName(),
                    brokerOnlineCnt.getValue());
            statsMap.put(statsSet.brokerTmoTotCnt.getFullName(),
                    statsSet.brokerTmoTotCnt.getValue());
            statsMap.put(brokerAbnCurCnt.getFullName(),
                    brokerAbnCurCnt.getValue());
            statsMap.put(brokerFbdCurCnt.getFullName(),
                    brokerFbdCurCnt.getValue());
        }
    }

    private static void getStatsValue(ServiceStatsSet statsSet,
                                      boolean resetValue,
                                      StringBuilder strBuff) {
        strBuff.append("{\"").append(statsSet.lstResetTime.getFullName())
                .append("\":\"").append(statsSet.lstResetTime.getStrSinceTime())
                .append("\"");
        if (resetValue) {
            // for consume group
            strBuff.append(",\"").append(csmOnlineGroupCnt.getFullName())
                    .append("\":").append(csmOnlineGroupCnt.getAndResetValue())
                    .append(",\"").append(statsSet.csmGroupTimeoutCnt.getFullName())
                    .append("\":").append(statsSet.csmGroupTimeoutCnt.getAndResetValue())
                    .append(",\"").append(csmCltBalanceGroupCnt.getFullName())
                    .append("\":").append(csmCltBalanceGroupCnt.getAndResetValue())
                    .append(",\"").append(statsSet.cltBalGroupTmototCnt.getFullName())
                    .append("\":").append(statsSet.cltBalGroupTmototCnt.getAndResetValue())
                    .append(",");
            statsSet.svrNormalBalanceStats.snapShort(strBuff, false);
            strBuff.append(",");
            statsSet.svrResetBalanceStats.snapShort(strBuff, false);
            // for consumer
            strBuff.append(",\"").append(consumerOnlineCnt.getFullName())
                    .append("\":").append(consumerOnlineCnt.getAndResetValue())
                    .append(",\"").append(statsSet.consumerTmoTotCnt.getFullName())
                    .append("\":").append(statsSet.consumerTmoTotCnt.getAndResetValue())
                    .append(",\"").append(consumerInConnectCount.getFullName())
                    .append("\":").append(consumerInConnectCount.getAndResetValue())
                    .append(",\"").append(consumerInDisConnectCount.getFullName())
                    .append("\":").append(consumerInDisConnectCount.getAndResetValue())
                    // for producer
                    .append(",\"").append(producerOnlineCnt.getFullName())
                    .append("\":").append(producerOnlineCnt.getAndResetValue())
                    .append(",\"").append(statsSet.producerTmoTotCnt.getFullName())
                    .append("\":").append(statsSet.producerTmoTotCnt.getAndResetValue())
                    // for broker
                    .append(",\"").append(brokerConfiguredCnt.getFullName())
                    .append("\":").append(brokerConfiguredCnt.getAndResetValue())
                    .append(",\"").append(brokerOnlineCnt.getFullName())
                    .append("\":").append(brokerOnlineCnt.getAndResetValue())
                    .append(",\"").append(statsSet.brokerTmoTotCnt.getFullName())
                    .append("\":").append(statsSet.brokerTmoTotCnt.getAndResetValue())
                    .append(",\"").append(brokerAbnCurCnt.getFullName())
                    .append("\":").append(brokerAbnCurCnt.getAndResetValue())
                    .append(",\"").append(brokerFbdCurCnt.getFullName())
                    .append("\":").append(brokerFbdCurCnt.getAndResetValue())
                    .append("}");
        } else {
            // for consume group
            strBuff.append(",\"").append(csmOnlineGroupCnt.getFullName())
                    .append("\":").append(csmOnlineGroupCnt.getValue())
                    .append(",\"").append(statsSet.csmGroupTimeoutCnt.getFullName())
                    .append("\":").append(statsSet.csmGroupTimeoutCnt.getValue())
                    .append(",\"").append(csmCltBalanceGroupCnt.getFullName())
                    .append("\":").append(csmCltBalanceGroupCnt.getValue())
                    .append(",\"").append(statsSet.cltBalGroupTmototCnt.getFullName())
                    .append("\":").append(statsSet.cltBalGroupTmototCnt.getValue())
                    .append(",");
            statsSet.svrNormalBalanceStats.getValue(strBuff, false);
            strBuff.append(",");
            statsSet.svrResetBalanceStats.getValue(strBuff, false);
            // for consumer
            strBuff.append(",\"").append(consumerOnlineCnt.getFullName())
                    .append("\":").append(consumerOnlineCnt.getValue())
                    .append(",\"").append(statsSet.consumerTmoTotCnt.getFullName())
                    .append("\":").append(statsSet.consumerTmoTotCnt.getValue())
                    .append(",\"").append(consumerInConnectCount.getFullName())
                    .append("\":").append(consumerInConnectCount.getValue())
                    .append(",\"").append(consumerInDisConnectCount.getFullName())
                    .append("\":").append(consumerInDisConnectCount.getValue())
                    // for producer
                    .append(",\"").append(producerOnlineCnt.getFullName())
                    .append("\":").append(producerOnlineCnt.getValue())
                    .append(",\"").append(statsSet.producerTmoTotCnt.getFullName())
                    .append("\":").append(statsSet.producerTmoTotCnt.getValue())
                    // for broker
                    .append(",\"").append(brokerConfiguredCnt.getFullName())
                    .append("\":").append(brokerConfiguredCnt.getValue())
                    .append(",\"").append(brokerOnlineCnt.getFullName())
                    .append("\":").append(brokerOnlineCnt.getValue())
                    .append(",\"").append(statsSet.brokerTmoTotCnt.getFullName())
                    .append("\":").append(statsSet.brokerTmoTotCnt.getValue())
                    .append(",\"").append(brokerAbnCurCnt.getFullName())
                    .append("\":").append(brokerAbnCurCnt.getValue())
                    .append(",\"").append(brokerFbdCurCnt.getFullName())
                    .append("\":").append(brokerFbdCurCnt.getValue())
                    .append("}");
        }
    }

    /**
     * Get current writable block index.
     *
     * @return the writable block index
     */
    private static int getIndex() {
        return getIndex(writableIndex.get());
    }

    /**
     * Gets the metric block index based on the specified value.
     *
     * @param origIndex    the specified value
     * @return the metric block index
     */
    private static int getIndex(int origIndex) {
        return Math.abs(origIndex % 2);
    }

    /**
     * ServiceStatsSet, Switchable metric data statistics block
     *
     * In which the object is the metric item that can be counted in stages
     */
    private static class ServiceStatsSet {
        protected final SinceTime lstResetTime =
                new SinceTime("reset_time", null);
        // consume group timeout statistics
        protected final LongStatsCounter csmGroupTimeoutCnt =
                new LongStatsCounter("csm_group_timeout_cnt", null);
        // client-balance consume group timeout statistics
        protected final LongStatsCounter cltBalGroupTmototCnt =
                new LongStatsCounter("client_balance_timeout_cnt", null);
        // consumer timeout statistics
        protected final LongStatsCounter consumerTmoTotCnt =
                new LongStatsCounter("consumer_timeout_cnt", null);
        // producer timeout statistics
        protected final LongStatsCounter producerTmoTotCnt =
                new LongStatsCounter("producer_timeout_cnt", null);
        // broker timeout statistics
        protected final LongStatsCounter brokerTmoTotCnt =
                new LongStatsCounter("broker_timeout_cnt", null);
        // normal server balance delta time statistics
        protected final ESTHistogram svrNormalBalanceStats =
                new ESTHistogram("server_balance_normal", null);
        // reset server balance delta time statistics
        protected final ESTHistogram svrResetBalanceStats =
                new ESTHistogram("server_balance_reset", null);

        public ServiceStatsSet() {
            resetSinceTime();
        }

        public void resetSinceTime() {
            this.lstResetTime.reset();
        }
    }
}

