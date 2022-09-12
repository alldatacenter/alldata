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

package org.apache.inlong.tubemq.client.common;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.metric.TrafficStatsUnit;
import org.apache.inlong.tubemq.corebase.metric.impl.ESTHistogram;
import org.apache.inlong.tubemq.corebase.metric.impl.LongStatsCounter;
import org.apache.inlong.tubemq.corebase.metric.impl.SinceTime;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientStatsInfo {
    private static final Logger logger =
            LoggerFactory.getLogger(ClientStatsInfo.class);
    private final boolean isProducer;
    private final String logPrefix;
    // statistic configure
    private final StatsConfig statsConfig = new StatsConfig();
    // switchable statistic items
    private final ClientStatsItemSet[] switchableSets = new ClientStatsItemSet[2];
    // current writable index
    private final AtomicInteger writableIndex = new AtomicInteger(0);
    // last self-print time
    private final AtomicLong lstSelfPrintTime = new AtomicLong(0);
    // last snapshot time
    private final AtomicLong lstSnapshotTime = new AtomicLong(0);

    public ClientStatsInfo(boolean isProducer, String clientId, StatsConfig statsConfig) {
        this.isProducer = isProducer;
        this.statsConfig.updateStatsConfig(statsConfig);
        StringBuilder strBuff = new StringBuilder(512);
        if (isProducer) {
            strBuff.append("[Producer");
        } else {
            strBuff.append("[Consumer");
        }
        this.logPrefix = strBuff.append(" Stats]: ")
                .append("Client=").append(clientId).toString();
        this.switchableSets[0] = new ClientStatsItemSet();
        this.switchableSets[1] = new ClientStatsItemSet();
    }

    public void updStatsControlInfo(StatsLevel statsLevel, boolean enableSelfPrint) {
        this.statsConfig.updateStatsControl(statsLevel, enableSelfPrint);
    }

    public void bookReg2Master(boolean isFailure) {
        if (this.statsConfig.getStatsLevel() == StatsLevel.ZERO) {
            return;
        }
        switchableSets[getIndex()].regMasterCnt.incValue();
        if (isFailure) {
            switchableSets[getIndex()].regMasterFailCnt.incValue();
        }
    }

    public void bookHB2MasterTimeout() {
        if (this.statsConfig.getStatsLevel() == StatsLevel.ZERO) {
            return;
        }
        switchableSets[getIndex()].regMasterTimoutCnt.incValue();
    }

    public void bookHB2MasterException() {
        if (this.statsConfig.getStatsLevel() == StatsLevel.ZERO) {
            return;
        }
        switchableSets[getIndex()].hbMasterExcCnt.incValue();
    }

    public void bookReg2Broker(boolean isFailure) {
        if (this.statsConfig.getStatsLevel() == StatsLevel.ZERO) {
            return;
        }
        switchableSets[getIndex()].regBrokerCnt.incValue();
        if (isFailure) {
            switchableSets[getIndex()].regBrokerFailCnt.incValue();
        }
    }

    public void bookHB2BrokerTimeout() {
        if (this.statsConfig.getStatsLevel() == StatsLevel.ZERO) {
            return;
        }
        switchableSets[getIndex()].regBrokerTimoutCnt.incValue();
    }

    public void bookHB2BrokerException() {
        if (this.statsConfig.getStatsLevel() == StatsLevel.ZERO) {
            return;
        }
        switchableSets[getIndex()].hbBrokerExcCnt.incValue();
    }

    public void bookReturnDuration(String partitionKey, long dltTime) {
        if (this.statsConfig.getStatsLevel() == StatsLevel.ZERO) {
            return;
        }
        ClientStatsItemSet curItemSet = switchableSets[getIndex()];
        curItemSet.csmLatencyStats.update(dltTime);
        if (this.statsConfig.getStatsLevel() == StatsLevel.FULL) {
            curItemSet.addCsmLatencyByPartKey(partitionKey, dltTime);
        }
    }

    public void bookConfirmDuration(String partitionKey, long dltTime) {
        if (this.statsConfig.getStatsLevel() == StatsLevel.ZERO) {
            return;
        }
        ClientStatsItemSet curItemSet = switchableSets[getIndex()];
        curItemSet.confirmDltStats.update(dltTime);
        if (this.statsConfig.getStatsLevel() == StatsLevel.FULL) {
            curItemSet.addConfirmDltByPartKey(partitionKey, dltTime);
        }
    }

    public void bookFailRpcCall(int errCode) {
        if (this.statsConfig.getStatsLevel() == StatsLevel.ZERO) {
            return;
        }
        switchableSets[getIndex()].bookFailRpcCall(errCode);
    }

    public void bookSuccSendMsg(long dltTime, String topicName,
                                String partitionKey, int msgSize) {
        if (this.statsConfig.getStatsLevel() == StatsLevel.ZERO) {
            return;
        }
        ClientStatsItemSet curItemSet = switchableSets[getIndex()];
        curItemSet.sendOrRecvMsg(topicName, dltTime, 1, msgSize);
        curItemSet.bookFailRpcCall(TErrCodeConstants.SUCCESS);
        if (this.statsConfig.getStatsLevel() == StatsLevel.FULL) {
            curItemSet.addTrafficInfoByPartKey(partitionKey, dltTime, 1, msgSize);
        }
    }

    public void bookSuccGetMsg(long dltTime, String topicName,
                               String partitionKey, int msgCnt, int msgSize) {
        if (this.statsConfig.getStatsLevel() == StatsLevel.ZERO) {
            return;
        }
        ClientStatsItemSet curItemSet = switchableSets[getIndex()];
        curItemSet.sendOrRecvMsg(topicName, dltTime, msgCnt, msgSize);
        curItemSet.bookFailRpcCall(TErrCodeConstants.SUCCESS);
        if (this.statsConfig.getStatsLevel() == StatsLevel.FULL) {
            curItemSet.addTrafficInfoByPartKey(partitionKey, dltTime, msgCnt, msgSize);
        }
    }

    /**
     * Self print statistics information to log file
     *
     * @param forcePrint    whether force print statistics information
     * @param needReset     whether reset statistics set
     * @param strBuff       string buffer
     */
    public void selfPrintStatsInfo(boolean forcePrint,
                                   boolean needReset,
                                   StringBuilder strBuff) {
        if ((this.statsConfig.getStatsLevel() == StatsLevel.ZERO)
                || !this.statsConfig.isEnableSelfPrint()) {
            return;
        }
        long lstPrintTime = lstSelfPrintTime.get();
        long curChkTime = Clock.systemDefaultZone().millis();
        if (forcePrint || (curChkTime - lstPrintTime > this.statsConfig.getSelfPrintPeriodMs())) {
            if (lstSelfPrintTime.compareAndSet(lstPrintTime, curChkTime)) {
                if (switchWritingStatsUnit(needReset)) {
                    strBuff.append(this.logPrefix).append(", reset value= ");
                    getStatsInfo(switchableSets[getIndex(writableIndex.get() - 1)],
                            true, strBuff);
                } else {
                    strBuff.append(this.logPrefix).append(", value= ");
                    getStatsInfo(switchableSets[getIndex()], false, strBuff);
                }
                logger.info(strBuff.toString());
                strBuff.delete(0, strBuff.length());
            }
        }
    }

    // private functions
    private boolean switchWritingStatsUnit(boolean needReset) {
        long lstResetTime = lstSnapshotTime.get();
        long checkDltTime = System.currentTimeMillis() - lstResetTime;
        if (((needReset && (checkDltTime > TBaseConstants.CFG_STATS_MIN_SNAPSHOT_PERIOD_MS))
                || (checkDltTime > this.statsConfig.getForcedResetPeriodMs()))
                && lstSnapshotTime.compareAndSet(lstResetTime, System.currentTimeMillis())) {
            switchableSets[getIndex(writableIndex.incrementAndGet())].resetStartTime();
            return true;
        }
        return false;
    }

    /**
     * Get current data encapsulated by Json format
     *
     * @param statsSet     the statistics to be printed
     * @param resetValue   whether to reset the current data
     * @param strBuff      string buffer
     */
    private void getStatsInfo(ClientStatsItemSet statsSet,
                              boolean resetValue, StringBuilder strBuff) {
        strBuff.append("{\"").append(statsSet.resetTime.getFullName())
                .append("\":\"").append(statsSet.resetTime.getStrSinceTime())
                .append("\",\"probe_time\":\"")
                .append(DateTimeConvertUtils.ms2yyyyMMddHHmmss(System.currentTimeMillis()))
                .append("\",");
        statsSet.totalTrafficStats.getValue(strBuff, resetValue);
        strBuff.append(",");
        statsSet.getTopicDetailInfo(strBuff, resetValue);
        if (this.statsConfig.getStatsLevel() != StatsLevel.SIMPLEST) {
            strBuff.append(",");
            if (resetValue) {
                statsSet.msgCallDltStats.snapShort(strBuff, false);
                if (!isProducer) {
                    strBuff.append(",");
                    statsSet.csmLatencyStats.snapShort(strBuff, false);
                    strBuff.append(",");
                    statsSet.confirmDltStats.getValue(strBuff, false);
                }
            } else {
                statsSet.msgCallDltStats.getValue(strBuff, false);
                if (!isProducer) {
                    strBuff.append(",");
                    statsSet.csmLatencyStats.getValue(strBuff, false);
                    strBuff.append(",");
                    statsSet.confirmDltStats.getValue(strBuff, false);
                }
            }
            strBuff.append(",");
            statsSet.getErrorRspInfo(strBuff, resetValue);
            strBuff.append(",");
            statsSet.getStatusInfo(strBuff, resetValue);
            if (this.statsConfig.getStatsLevel() == StatsLevel.FULL) {
                strBuff.append(",");
                statsSet.getPartDetailsInfo(strBuff, isProducer, resetValue);
            }
        }
        strBuff.append("}");
    }

    /**
     * Get current writable block index.
     *
     * @return the writable block index
     */
    private int getIndex() {
        return getIndex(writableIndex.get());
    }

    /**
     * Gets the metric block index based on the specified value.
     *
     * @param origIndex    the specified value
     * @return the metric block index
     */
    private int getIndex(int origIndex) {
        return Math.abs(origIndex % 2);
    }

    /**
     * ClientStatsItemSet, Client related statistics set
     *
     */
    private static class ClientStatsItemSet {
        // The reset time of statistics set
        protected final SinceTime resetTime =
                new SinceTime("reset_time", null);
        // received or sent message traffic statistic
        protected final TrafficStatsUnit totalTrafficStats =
                new TrafficStatsUnit("msg_cnt", "msg_size", "total_traffic");
        // topic-based traffic statistics
        protected final ConcurrentHashMap<String, TrafficStatsUnit> topicTrafficMap =
                new ConcurrentHashMap<>();
        // partition-based traffic statistics
        protected final ConcurrentHashMap<String, PartitionStatsItemSet> partDltStatsMap =
                new ConcurrentHashMap<>();
        // time consumption statistics for sending or receiving messages
        protected final ESTHistogram msgCallDltStats =
                new ESTHistogram("msg_call_dlt", "");
        // statistics on consumption transaction time
        protected final ESTHistogram csmLatencyStats =
                new ESTHistogram("csm_latency_dlt", "");
        // time consumption statistics for confirm request
        protected final ESTHistogram confirmDltStats =
                new ESTHistogram("msg_confirm_dlt", "");
        // error response distribution statistics
        protected final ConcurrentHashMap<Integer, LongStatsCounter> errRspStatsMap =
                new ConcurrentHashMap<>();
        // client 2 Master status statistics
        protected final LongStatsCounter regMasterCnt =
                new LongStatsCounter("reg_master_cnt", null);
        protected final LongStatsCounter regMasterFailCnt =
                new LongStatsCounter("reg_master_fail", null);
        protected final LongStatsCounter regMasterTimoutCnt =
                new LongStatsCounter("reg_master_timeout", null);
        protected final LongStatsCounter hbMasterExcCnt =
                new LongStatsCounter("hb_master_exception", null);
        // client 2 Broker status statistics
        protected final LongStatsCounter regBrokerCnt =
                new LongStatsCounter("reg_broker_cnt", null);
        protected final LongStatsCounter regBrokerFailCnt =
                new LongStatsCounter("reg_broker_fail", null);
        protected final LongStatsCounter regBrokerTimoutCnt =
                new LongStatsCounter("reg_broker_timeout", null);
        protected final LongStatsCounter hbBrokerExcCnt =
                new LongStatsCounter("hb_broker_exception", null);

        public ClientStatsItemSet() {
            resetStartTime();
        }

        public void resetStartTime() {
            this.resetTime.reset();
        }

        /**
         * Accumulate sent or received message information
         *
         * @param topic     the topic name
         * @param dltTime   the latency
         * @param msgCnt    the message count
         * @param msgSize   the message size
         */
        public void sendOrRecvMsg(String topic, long dltTime,
                                  int msgCnt, int msgSize) {
            msgCallDltStats.update(dltTime);
            totalTrafficStats.addMsgCntAndSize(msgCnt, msgSize);
            // accumulate traffic information by topic
            TrafficStatsUnit curStatsUnit = topicTrafficMap.get(topic);
            if (curStatsUnit == null) {
                TrafficStatsUnit tmpUnit =
                        new TrafficStatsUnit("msg_cnt", "msg_size", topic);
                curStatsUnit = topicTrafficMap.putIfAbsent(topic, tmpUnit);
                if (curStatsUnit == null) {
                    curStatsUnit = tmpUnit;
                }
            }
            curStatsUnit.addMsgCntAndSize(msgCnt, msgSize);
        }

        /**
         * Accumulate msg count by errcode
         *
         * @param errCode  the error code
         */
        public void bookFailRpcCall(int errCode) {
            LongStatsCounter curItemCounter = errRspStatsMap.get(errCode);
            if (curItemCounter == null) {
                LongStatsCounter tmpCounter = new LongStatsCounter(
                        "err_" + errCode, "");
                curItemCounter = errRspStatsMap.putIfAbsent(errCode, tmpCounter);
                if (curItemCounter == null) {
                    curItemCounter = tmpCounter;
                }
            }
            curItemCounter.incValue();
        }

        /**
         * Accumulate traffic information by partition
         *
         * @param dltTime   the latency
         * @param msgCnt    the message count
         * @param msgSize   the message size
         */
        public void addTrafficInfoByPartKey(String partitionKey,
                                            long dltTime, int msgCnt, int msgSize) {
            PartitionStatsItemSet partStatsUnit =
                    partDltStatsMap.get(partitionKey);
            if (partStatsUnit == null) {
                PartitionStatsItemSet tmpUnit = new PartitionStatsItemSet(partitionKey);
                partStatsUnit = partDltStatsMap.putIfAbsent(partitionKey, tmpUnit);
                if (partStatsUnit == null) {
                    partStatsUnit = tmpUnit;
                }
            }
            partStatsUnit.trafficStatsUnit.addMsgCntAndSize(msgCnt, msgSize);
            partStatsUnit.msgCallDltStats.update(dltTime);
        }

        /**
         * Accumulate consume latency information by partition
         *
         * @param dltTime   the latency
         */
        public void addCsmLatencyByPartKey(String partitionKey, long dltTime) {
            PartitionStatsItemSet partStatsUnit = partDltStatsMap.get(partitionKey);
            if (partStatsUnit == null) {
                PartitionStatsItemSet tmpUnit = new PartitionStatsItemSet(partitionKey);
                partStatsUnit = partDltStatsMap.putIfAbsent(partitionKey, tmpUnit);
                if (partStatsUnit == null) {
                    partStatsUnit = tmpUnit;
                }
            }
            partStatsUnit.csmLatencyStats.update(dltTime);
        }

        /**
         * Accumulate confirm delta time information by partition
         *
         * @param dltTime   the latency
         */
        public void addConfirmDltByPartKey(String partitionKey, long dltTime) {
            PartitionStatsItemSet partStatsUnit = partDltStatsMap.get(partitionKey);
            if (partStatsUnit == null) {
                PartitionStatsItemSet tmpUnit = new PartitionStatsItemSet(partitionKey);
                partStatsUnit = partDltStatsMap.putIfAbsent(partitionKey, tmpUnit);
                if (partStatsUnit == null) {
                    partStatsUnit = tmpUnit;
                }
            }
            partStatsUnit.confirmDltStats.update(dltTime);
        }

        /**
         * Get traffic statistics details by topic
         *
         * @param strBuff      string buffer
         * @param resetValue   whether to reset the current data
         */
        public void getTopicDetailInfo(StringBuilder strBuff, boolean resetValue) {
            int totalCnt = 0;
            strBuff.append("\"topic_details\":{");
            for (Map.Entry<String, TrafficStatsUnit> entry : topicTrafficMap.entrySet()) {
                if (entry == null) {
                    continue;
                }
                if (totalCnt++ > 0) {
                    strBuff.append(",");
                }
                entry.getValue().getValue(strBuff, true);
            }
            strBuff.append("}");
            if (resetValue) {
                topicTrafficMap.clear();
            }
        }

        /**
         * Get error response statistics
         *
         * @param strBuff      string buffer
         * @param resetValue   whether to reset the current data
         */
        public void getErrorRspInfo(StringBuilder strBuff, boolean resetValue) {
            int totalCnt = 0;
            strBuff.append("\"rsp_details\":{");
            for (LongStatsCounter statsCounter : errRspStatsMap.values()) {
                if (statsCounter == null) {
                    continue;
                }
                if (totalCnt++ > 0) {
                    strBuff.append(",");
                }
                strBuff.append("\"").append(statsCounter.getFullName()).append("\":")
                        .append(statsCounter.getValue());
            }
            strBuff.append("}");
            if (resetValue) {
                errRspStatsMap.clear();
            }
        }

        /**
         * Get running status statistics
         *
         * @param strBuff      string buffer
         * @param resetValue   whether to reset the current data
         */
        public void getStatusInfo(StringBuilder strBuff, boolean resetValue) {
            strBuff.append("\"status_details\":{\"");
            if (resetValue) {
                strBuff.append(regMasterCnt.getFullName()).append("\":")
                        .append(regMasterCnt.getAndResetValue())
                        .append(",\"").append(regMasterFailCnt.getFullName()).append("\":")
                        .append(regMasterFailCnt.getAndResetValue())
                        .append(",\"").append(regMasterTimoutCnt.getFullName()).append("\":")
                        .append(regMasterTimoutCnt.getAndResetValue())
                        .append(",\"").append(hbMasterExcCnt.getFullName()).append("\":")
                        .append(hbMasterExcCnt.getAndResetValue())
                        .append(",\"").append(regBrokerCnt.getFullName()).append("\":")
                        .append(regBrokerCnt.getAndResetValue())
                        .append(",\"").append(regBrokerFailCnt.getFullName()).append("\":")
                        .append(regBrokerFailCnt.getAndResetValue())
                        .append(",\"").append(regBrokerTimoutCnt.getFullName()).append("\":")
                        .append(regBrokerTimoutCnt.getAndResetValue())
                        .append(",\"").append(hbBrokerExcCnt.getFullName()).append("\":")
                        .append(hbBrokerExcCnt.getAndResetValue())
                        .append("}");
            } else {
                strBuff.append(regMasterCnt.getFullName()).append("\":")
                        .append(regMasterCnt.getValue())
                        .append(",\"").append(regMasterFailCnt.getFullName()).append("\":")
                        .append(regMasterFailCnt.getValue())
                        .append(",\"").append(regMasterTimoutCnt.getFullName()).append("\":")
                        .append(regMasterTimoutCnt.getValue())
                        .append(",\"").append(hbMasterExcCnt.getFullName()).append("\":")
                        .append(hbMasterExcCnt.getValue())
                        .append(",\"").append(regBrokerCnt.getFullName()).append("\":")
                        .append(regBrokerCnt.getValue())
                        .append(",\"").append(regBrokerFailCnt.getFullName()).append("\":")
                        .append(regBrokerFailCnt.getValue())
                        .append(",\"").append(regBrokerTimoutCnt.getFullName()).append("\":")
                        .append(regBrokerTimoutCnt.getValue())
                        .append(",\"").append(hbBrokerExcCnt.getFullName()).append("\":")
                        .append(hbBrokerExcCnt.getValue())
                        .append("}");
            }
        }

        /**
         * Get traffic statistics by partition key
         *
         * @param strBuff      string buffer
         * @param isProducer   whether producer role
         * @param resetValue   whether to reset the current data
         */
        public void getPartDetailsInfo(StringBuilder strBuff,
                                       boolean isProducer,
                                       boolean resetValue) {
            int totalCnt = 0;
            strBuff.append("\"part_details\":{");
            for (PartitionStatsItemSet partStatsSet : partDltStatsMap.values()) {
                if (partStatsSet == null) {
                    continue;
                }
                if (totalCnt++ > 0) {
                    strBuff.append(",");
                }
                partStatsSet.getValue(strBuff, isProducer, false);
            }
            strBuff.append("}");
            if (resetValue) {
                partDltStatsMap.clear();
            }
        }
    }

    /**
     * PartitionStatsItemSet, partition related statistics set
     *
     */
    private static class PartitionStatsItemSet {
        private final String partKey;
        protected  final TrafficStatsUnit trafficStatsUnit =
                new TrafficStatsUnit("msg_cnt", "msg_size", "traffic");
        // time consumption statistics for sending or receiving messages
        protected final ESTHistogram msgCallDltStats =
                new ESTHistogram("msg_call_dlt", "");
        // statistics on consumption transaction time
        protected final ESTHistogram csmLatencyStats =
                new ESTHistogram("csm_latency_dlt", "");
        // time consumption statistics for confirm request
        protected final ESTHistogram confirmDltStats =
                new ESTHistogram("msg_confirm_dlt", "");

        public PartitionStatsItemSet(String partKey) {
            this.partKey = partKey;
        }

        /**
         * Get partition's traffic statistics
         *
         * @param strBuff      string buffer
         * @param isProducer   whether producer role
         * @param resetValue   whether to reset the current data
         */
        public void getValue(StringBuilder strBuff, boolean isProducer, boolean resetValue) {
            strBuff.append("\"").append(partKey).append("\":{");
            trafficStatsUnit.getValue(strBuff, resetValue);
            strBuff.append(",");
            if (resetValue) {
                msgCallDltStats.snapShort(strBuff, false);
                if (!isProducer) {
                    strBuff.append(",");
                    csmLatencyStats.snapShort(strBuff, false);
                    strBuff.append(",");
                    confirmDltStats.snapShort(strBuff, false);
                }
            } else {
                msgCallDltStats.getValue(strBuff, false);
                if (!isProducer) {
                    strBuff.append(",");
                    csmLatencyStats.getValue(strBuff, false);
                    strBuff.append(",");
                    confirmDltStats.getValue(strBuff, false);
                }
            }
            strBuff.append("}");
        }
    }
}

