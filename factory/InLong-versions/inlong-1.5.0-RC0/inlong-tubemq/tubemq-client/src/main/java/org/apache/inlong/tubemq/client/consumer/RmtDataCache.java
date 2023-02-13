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

package org.apache.inlong.tubemq.client.consumer;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.client.config.ConsumerConfig;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.cluster.SubscribeInfo;
import org.apache.inlong.tubemq.corebase.policies.FlowCtrlRuleHandler;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientBroker;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.DataConverterUtil;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.corebase.utils.ThreadUtils;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Remote data cache.
 */
public class RmtDataCache implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(RmtDataCache.class);
    private static final AtomicLong refCont = new AtomicLong(0);
    private static Timer timer;
    private final ConsumerConfig consumerConfig;
    // store flow control rules
    private final AtomicLong lstRegMasterTime = new AtomicLong(0);
    private final AtomicBoolean isCurGroupCtrl = new AtomicBoolean(false);
    private final AtomicLong lastCheckTime = new AtomicLong(0);
    private final FlowCtrlRuleHandler groupFlowCtrlRuleHandler =
            new FlowCtrlRuleHandler(false);
    private final FlowCtrlRuleHandler defFlowCtrlRuleHandler =
            new FlowCtrlRuleHandler(true);
    // store broker configure info
    private long lastEmptyBrokerPrintTime = 0;
    private long lastEmptyTopicPrintTime = 0;
    private long lastBrokerUpdatedTime = System.currentTimeMillis();
    private final AtomicLong lstBrokerConfigId =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private Map<Integer, BrokerInfo> brokersMap =
            new ConcurrentHashMap<>();
    // require Auth info
    private final AtomicBoolean nextWithAuthInfo2M = new AtomicBoolean(false);
    private final ConcurrentHashMap<Integer, AtomicBoolean> nextWithAuthInfo2BMap =
            new ConcurrentHashMap<Integer, AtomicBoolean>();
    // consume control info
    private final AtomicLong reqMaxOffsetCsmId =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private final AtomicBoolean csmFromMaxOffset =
            new AtomicBoolean(false);
    // meta query result
    private final AtomicLong topicMetaInfoId =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private final Set<String> metaInfoSet = new TreeSet<>();
    private ConcurrentHashMap<String, Tuple2<Partition, Integer>> configuredPartInfoMap =
            new ConcurrentHashMap<>();
    private final AtomicLong topicMetaUpdatedTime =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private boolean isFirstReport = true;
    private long reportIntCount = 0;
    private final long maxReportTimes;
    // partition cache
    private final AtomicInteger waitCont = new AtomicInteger(0);
    private final ConcurrentHashMap<String, Timeout> timeouts =
            new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<String> indexPartition =
            new ConcurrentLinkedQueue<String>();
    private volatile long lstReportTime = 0;
    private final AtomicLong partMapChgTime = new AtomicLong(0);
    private final ConcurrentHashMap<String /* index */, PartitionExt> partitionMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String /* index */, Long> partitionUsedMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String /* index */, ConsumeOffsetInfo> partitionOffsetMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String /* index */, Long> partitionFrozenMap =
            new ConcurrentHashMap<String, Long>();
    private final ConcurrentHashMap<String /* topic */, ConcurrentLinkedQueue<Partition>> topicPartitionConMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<BrokerInfo/* broker */, ConcurrentLinkedQueue<Partition>> brokerPartitionConMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String/* partitionKey */, Integer> partRegisterBookMap =
            new ConcurrentHashMap<>();
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    private CountDownLatch dataProcessSync = new CountDownLatch(0);

    /**
     * Construct a remote data cache object.
     *
     * @param consumerConfig    consumer configure
     * @param partitionList     partition list
     */
    public RmtDataCache(ConsumerConfig consumerConfig, List<Partition> partitionList) {
        this.consumerConfig = consumerConfig;
        if (refCont.incrementAndGet() == 1) {
            timer = new HashedWheelTimer();
        }
        this.maxReportTimes = consumerConfig.getMaxSubInfoReportIntvlTimes() * 10L;
        Map<Partition, ConsumeOffsetInfo> tmpPartOffsetMap = new HashMap<>();
        if (partitionList != null) {
            for (Partition partition : partitionList) {
                tmpPartOffsetMap.put(partition,
                        new ConsumeOffsetInfo(partition.getPartitionKey(),
                                TBaseConstants.META_VALUE_UNDEFINED,
                                TBaseConstants.META_VALUE_UNDEFINED));
            }
        }
        addPartitionsInfo(tmpPartOffsetMap);
    }

    public void bookBrokerRequireAuthInfo(int brokerId,
            ClientBroker.HeartBeatResponseB2C heartBeatResponseV2) {
        if (!heartBeatResponseV2.hasRequireAuth()) {
            return;
        }
        AtomicBoolean authStatus = nextWithAuthInfo2BMap.get(brokerId);
        if (authStatus == null) {
            AtomicBoolean tmpAuthStatus = new AtomicBoolean(false);
            authStatus =
                    nextWithAuthInfo2BMap.putIfAbsent(brokerId, tmpAuthStatus);
            if (authStatus == null) {
                authStatus = tmpAuthStatus;
            }
        }
        authStatus.set(heartBeatResponseV2.getRequireAuth());
    }

    /**
     * update ops task in cache
     *
     * @param opsTaskInfo ops task info
     * @param strBuff   the string buffer
     *
     */
    public void updOpsTaskInfo(ClientMaster.OpsTaskInfo opsTaskInfo, StringBuilder strBuff) {
        if (opsTaskInfo == null) {
            return;
        }
        // update group flowctrl info
        if (opsTaskInfo.hasGroupFlowCheckId()
                && opsTaskInfo.getGroupFlowCheckId() >= 0
                && opsTaskInfo.getGroupFlowCheckId() != groupFlowCtrlRuleHandler.getFlowCtrlId()) {
            try {
                groupFlowCtrlRuleHandler.updateFlowCtrlInfo(
                        opsTaskInfo.getQryPriorityId(),
                        opsTaskInfo.getGroupFlowCheckId(),
                        opsTaskInfo.getGroupFlowControlInfo(), strBuff);
            } catch (Exception e1) {
                logger.warn("[Remote Data Cache] found parse group flowCtrl rules failure", e1);
            }
        }
        // update default flowctrl info
        if (opsTaskInfo.hasDefFlowCheckId()
                && opsTaskInfo.getDefFlowCheckId() >= 0
                && opsTaskInfo.getDefFlowCheckId() != defFlowCtrlRuleHandler.getFlowCtrlId()) {
            try {
                defFlowCtrlRuleHandler.updateFlowCtrlInfo(
                        TBaseConstants.META_VALUE_UNDEFINED,
                        opsTaskInfo.getDefFlowCheckId(),
                        opsTaskInfo.getDefFlowControlInfo(), strBuff);
            } catch (Exception e1) {
                logger.warn("[Remote Data Cache] found parse default flowCtrl rules failure", e1);
            }
        }
        // update consume control info
        if (opsTaskInfo.hasCsmFrmMaxOffsetCtrlId()
                && opsTaskInfo.getCsmFrmMaxOffsetCtrlId() >= 0) {
            if (reqMaxOffsetCsmId.get() != opsTaskInfo.getCsmFrmMaxOffsetCtrlId()) {
                reqMaxOffsetCsmId.set(opsTaskInfo.getCsmFrmMaxOffsetCtrlId());
                if (opsTaskInfo.getCsmFrmMaxOffsetCtrlId() > lstRegMasterTime.get()) {
                    csmFromMaxOffset.set(true);
                }
            }
        }
        // update master require auth
        if (opsTaskInfo.hasRequireAuth()) {
            storeMasterAuthRequire(opsTaskInfo.getRequireAuth());
        }
    }

    /**
     * update ops task in cache
     *
     * @param response   master register response
     * @param strBuff  the string buffer
     *
     */
    public void updFlowCtrlInfoInfo(ClientMaster.RegisterResponseM2C response,
            StringBuilder strBuff) {
        if (response == null) {
            return;
        }
        // update group flowctrl info
        if (response.hasGroupFlowCheckId()
                && response.getGroupFlowCheckId() >= 0
                && response.getGroupFlowCheckId() != groupFlowCtrlRuleHandler.getFlowCtrlId()) {
            try {
                groupFlowCtrlRuleHandler.updateFlowCtrlInfo(
                        response.getQryPriorityId(),
                        response.getGroupFlowCheckId(),
                        response.getGroupFlowControlInfo(), strBuff);
            } catch (Exception e1) {
                logger.warn("[Remote Data Cache] found parse group flowCtrl rules failure", e1);
            }
        }
        // update default flowctrl info
        if (response.hasDefFlowCheckId()
                && response.getDefFlowCheckId() >= 0
                && response.getDefFlowCheckId() != defFlowCtrlRuleHandler.getFlowCtrlId()) {
            try {
                defFlowCtrlRuleHandler.updateFlowCtrlInfo(
                        TBaseConstants.META_VALUE_UNDEFINED,
                        response.getDefFlowCheckId(),
                        response.getDefFlowControlInfo(), strBuff);
            } catch (Exception e1) {
                logger.warn("[Remote Data Cache] found parse default flowCtrl rules failure", e1);
            }
        }
    }

    /**
     * update ops task in cache
     *
     * @param response   master register response
     * @param strBuff  the string buffer
     *
     */
    public void updFlowCtrlInfoInfo(ClientMaster.HeartResponseM2C response,
            StringBuilder strBuff) {
        if (response == null) {
            return;
        }
        // update group flowctrl info
        if (response.hasGroupFlowCheckId()
                && response.getGroupFlowCheckId() >= 0
                && response.getGroupFlowCheckId() != groupFlowCtrlRuleHandler.getFlowCtrlId()) {
            try {
                groupFlowCtrlRuleHandler.updateFlowCtrlInfo(
                        response.getQryPriorityId(),
                        response.getGroupFlowCheckId(),
                        response.getGroupFlowControlInfo(), strBuff);
            } catch (Exception e1) {
                logger.warn("[Remote Data Cache] found parse group flowCtrl rules failure", e1);
            }
        }
        // update default flowctrl info
        if (response.hasDefFlowCheckId()
                && response.getDefFlowCheckId() >= 0
                && response.getDefFlowCheckId() != defFlowCtrlRuleHandler.getFlowCtrlId()) {
            try {
                defFlowCtrlRuleHandler.updateFlowCtrlInfo(
                        TBaseConstants.META_VALUE_UNDEFINED,
                        response.getDefFlowCheckId(),
                        response.getDefFlowControlInfo(), strBuff);
            } catch (Exception e1) {
                logger.warn("[Remote Data Cache] found parse default flowCtrl rules failure", e1);
            }
        }
    }

    public boolean isCsmFromMaxOffset() {
        if (csmFromMaxOffset.get()) {
            return csmFromMaxOffset.compareAndSet(true, false);
        }
        return false;
    }

    public int getQryPriorityId() {
        return this.groupFlowCtrlRuleHandler.getQryPriorityId();
    }

    public long getDefFlowCtrlId() {
        return this.defFlowCtrlRuleHandler.getFlowCtrlId();
    }

    public long getGroupFlowCtrlId() {
        return this.groupFlowCtrlRuleHandler.getFlowCtrlId();
    }

    /**
     * store topic meta information
     *
     * @param curTopicMetaInfoId   the topic meta information id
     * @param curMetaInfoSet       the topic meta information
     */
    public void storeTopicMetaInfo(long curTopicMetaInfoId, List<String> curMetaInfoSet) {
        if (curTopicMetaInfoId < 0
                || curTopicMetaInfoId == this.topicMetaInfoId.get()) {
            return;
        }
        if (curMetaInfoSet == null || curMetaInfoSet.isEmpty()) {
            return;
        }
        ConcurrentHashMap<String, Tuple2<Partition, Integer>> curConfMetaInfoMap =
                new ConcurrentHashMap<>();
        for (String metaInfo : curMetaInfoSet) {
            if (TStringUtils.isBlank(metaInfo)) {
                continue;
            }
            String[] strInfo = metaInfo.split(TokenConstants.SEGMENT_SEP);
            String[] strPartInfoSet = strInfo[1].split(TokenConstants.ARRAY_SEP);
            for (String partStr : strPartInfoSet) {
                String[] strPartInfo = partStr.split(TokenConstants.ATTR_SEP);
                BrokerInfo brokerInfo = brokersMap.get(Integer.parseInt(strPartInfo[0]));
                if (brokerInfo == null) {
                    continue;
                }
                int storeId = Integer.parseInt(strPartInfo[1]);
                int partCnt = Integer.parseInt(strPartInfo[2]);
                int statusId = Integer.parseInt(strPartInfo[3]);
                for (int j = 0; j < storeId; j++) {
                    int baseValue = j * TBaseConstants.META_STORE_INS_BASE;
                    for (int i = 0; i < partCnt; i++) {
                        Partition partition =
                                new Partition(brokerInfo, strInfo[0], baseValue + i);
                        curConfMetaInfoMap.put(partition.getPartitionKey(),
                                new Tuple2<>(partition, statusId));
                    }
                }
            }
        }
        if (curConfMetaInfoMap.isEmpty()) {
            return;
        }
        this.metaInfoSet.clear();
        this.metaInfoSet.addAll(curMetaInfoSet);
        this.configuredPartInfoMap = curConfMetaInfoMap;
        this.topicMetaUpdatedTime.set(System.currentTimeMillis());
        this.topicMetaInfoId.set(curTopicMetaInfoId);
    }

    public Map<String, Boolean> getConfPartMetaInfo() {
        Map<String, Boolean> configMap = new HashMap<>();
        for (Map.Entry<String, Tuple2<Partition, Integer>> entry : configuredPartInfoMap.entrySet()) {
            if (entry == null || entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            configMap.put(entry.getKey(), (entry.getValue().getF1() == 1));
        }
        return configMap;
    }

    public boolean isPartSubscribable(String partitionKey) {
        Tuple2<Partition, Integer> partConfig =
                configuredPartInfoMap.get(partitionKey);
        if (partConfig == null
                || partConfig.getF0() == null
                || partConfig.getF1() == null) {
            return false;
        }
        return (partConfig.getF1() == 1);
    }

    /**
     * Get subscribable partition information
     *
     * @param partitionKey   the partition key to query
     * @param result         the topic meta information
     * @param sBuffer        the string buffer
     * @return               whether query success
     */
    public boolean getSubscribablePartition(String partitionKey,
            ProcessResult result,
            StringBuilder sBuffer) {
        Tuple2<Partition, Integer> partStatusInfo =
                configuredPartInfoMap.get(partitionKey);
        if (partStatusInfo == null) {
            result.setFailResult(TErrCodeConstants.NOT_FOUND,
                    sBuffer.append("PartitionKey ").append(partitionKey)
                            .append(" not found in partition-meta Information set!")
                            .toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        if (partStatusInfo.getF1() != 1) {
            result.setFailResult(TErrCodeConstants.PARTITION_UNSUBSCRIBABLE,
                    sBuffer.append("PartitionKey ").append(partitionKey)
                            .append(" not available for subscription now!")
                            .toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        result.setSuccResult(partStatusInfo.getF0());
        return true;
    }

    public void updateReg2MasterTime() {
        this.lstRegMasterTime.set(System.currentTimeMillis());
    }

    public long getRegMasterTime() {
        return this.lstRegMasterTime.get();
    }

    /**
     * Update broker configure info
     *
     * @param pkgCheckSum     checkSum Id of packaged information
     * @param pkgBrokerInfos  packaged broker info string list
     * @param sBuilder        string process buffer
     */
    public void updateBrokerInfoList(long pkgCheckSum,
            List<String> pkgBrokerInfos,
            StringBuilder sBuilder) {
        if (pkgCheckSum != lstBrokerConfigId.get()) {
            if (pkgBrokerInfos != null) {
                brokersMap = DataConverterUtil.convertBrokerInfo(
                        pkgBrokerInfos, consumerConfig.isTlsEnable());
                lstBrokerConfigId.set(pkgCheckSum);
                lastBrokerUpdatedTime = System.currentTimeMillis();
                if (pkgBrokerInfos.isEmpty()) {
                    if (System.currentTimeMillis() - lastEmptyBrokerPrintTime > 60000) {
                        logger.warn(sBuilder
                                .append("[Meta Info] Found empty brokerList, changed checksum is ")
                                .append(lstBrokerConfigId).toString());
                        sBuilder.delete(0, sBuilder.length());
                        lastEmptyBrokerPrintTime = System.currentTimeMillis();
                    }
                } else {
                    logger.info(sBuilder
                            .append("[Meta Info] Changed brokerList checksum is ")
                            .append(lstBrokerConfigId).toString());
                    sBuilder.delete(0, sBuilder.length());
                }
            }
        }
    }

    public void storeMasterAuthRequire(boolean requireAuth) {
        nextWithAuthInfo2M.set(requireAuth);
    }

    /**
     * Book and get Master's authentication status
     *
     * @param isForce    whether force authentication
     * @return           whether to require authentication
     */
    public boolean markAndGetAuthStatus(boolean isForce) {
        boolean needAuth = false;
        if (isForce) {
            nextWithAuthInfo2M.set(false);
        } else if (nextWithAuthInfo2M.get()) {
            if (nextWithAuthInfo2M.compareAndSet(true, false)) {
                needAuth = true;
            }
        }
        return needAuth;
    }

    /**
     * Book and get Broker's authentication status
     *
     * @param brokerId     the broker id to query
     * @param isForce    whether force authentication
     * @return           whether to require authentication
     */
    public boolean markAndGetBrokerAuthStatus(int brokerId, boolean isForce) {
        boolean needAuth = false;
        AtomicBoolean authStatus = nextWithAuthInfo2BMap.get(brokerId);
        if (authStatus == null) {
            AtomicBoolean tmpAuthStatus = new AtomicBoolean(false);
            authStatus =
                    nextWithAuthInfo2BMap.putIfAbsent(brokerId, tmpAuthStatus);
            if (authStatus == null) {
                authStatus = tmpAuthStatus;
            }
        }
        if (isForce) {
            needAuth = true;
            authStatus.set(false);
        } else if (authStatus.get()) {
            if (authStatus.compareAndSet(true, false)) {
                needAuth = true;
            }
        }
        return needAuth;
    }

    /**
     * get client report ops task info id set
     *
     * @return ops task info id set
     */
    public ClientMaster.OpsTaskInfo buildOpsTaskInfo() {
        boolean hasData = false;
        ClientMaster.OpsTaskInfo.Builder builder =
                ClientMaster.OpsTaskInfo.newBuilder();
        if (defFlowCtrlRuleHandler.getFlowCtrlId() >= 0) {
            builder.setDefFlowCheckId(defFlowCtrlRuleHandler.getFlowCtrlId());
            hasData = true;
        }
        if (groupFlowCtrlRuleHandler.getFlowCtrlId() >= 0) {
            builder.setGroupFlowCheckId(groupFlowCtrlRuleHandler.getFlowCtrlId());
            hasData = true;
        }
        if (groupFlowCtrlRuleHandler.getQryPriorityId() >= 0) {
            builder.setQryPriorityId(groupFlowCtrlRuleHandler.getQryPriorityId());
            hasData = true;
        }
        if (reqMaxOffsetCsmId.get() >= 0) {
            builder.setCsmFrmMaxOffsetCtrlId(reqMaxOffsetCsmId.get());
            hasData = true;
        }
        if (hasData) {
            return builder.build();
        } else {
            return null;
        }
    }

    /**
     * get client subscribe report information
     *
     * @return the client subscribe information
     */
    public ClientMaster.ClientSubRepInfo buildClientSubRepInfo() {
        ClientMaster.ClientSubRepInfo.Builder builder =
                ClientMaster.ClientSubRepInfo.newBuilder();
        builder.setBrokerConfigId(this.lstBrokerConfigId.get());
        builder.setTopicMetaInfoId(this.topicMetaInfoId.get());
        if (this.topicMetaUpdatedTime.get() >= 0) {
            builder.setLstAssignedTime(this.topicMetaUpdatedTime.get());
        }
        builder.setReportSubInfo(false);
        if (isFirstReport) {
            if (!this.partitionMap.isEmpty()) {
                isFirstReport = false;
                builder.setReportSubInfo(true);
                lstReportTime = partMapChgTime.get();
                builder.addAllPartSubInfo(getSubscribedPartitionInfo());
            }
        } else {
            if (lstReportTime != partMapChgTime.get()
                    || ((++this.reportIntCount) % this.maxReportTimes == 0)) {
                builder.setReportSubInfo(true);
                lstReportTime = partMapChgTime.get();
                builder.addAllPartSubInfo(getSubscribedPartitionInfo());
            }
        }
        return builder.build();
    }

    public long getLastBrokerConfigId() {
        return this.lstBrokerConfigId.get();
    }

    public long getlastTopicMetaInfoId() {
        return this.topicMetaInfoId.get();
    }

    /**
     * Judge whether the consumer group is in flow control management
     *
     * @return true in control, false not
     */
    public boolean isCurGroupInFlowCtrl() {
        long curCheckTime = this.lastCheckTime.get();
        if (System.currentTimeMillis() - curCheckTime >= 10000) {
            if (this.lastCheckTime.compareAndSet(curCheckTime, System.currentTimeMillis())) {
                this.isCurGroupCtrl.set(
                        groupFlowCtrlRuleHandler.getCurDataLimit(Long.MAX_VALUE) != null);
            }
        }
        return this.isCurGroupCtrl.get();
    }

    /**
     * Set partition context information.
     *
     * @param partitionKey  partition key
     * @param currOffset    current offset
     * @param reqProcType   type information
     * @param errCode       error code
     * @param isEscLimit    if the limitDlt should be ignored
     * @param msgSize       message size
     * @param limitDlt      max offset of the data fetch
     * @param curDataDlt    the offset of current data fetch
     * @param isRequireSlow if the server requires slow down
     * @param maxOffset     partition current max offset
     */
    public void setPartitionContextInfo(String partitionKey, long currOffset,
            int reqProcType, int errCode,
            boolean isEscLimit, int msgSize,
            long limitDlt, long curDataDlt,
            boolean isRequireSlow, long maxOffset) {
        PartitionExt partitionExt = partitionMap.get(partitionKey);
        if (partitionExt != null) {
            updateOffsetCache(partitionKey, currOffset, maxOffset);
            partitionExt.setPullTempData(reqProcType, errCode,
                    isEscLimit, msgSize, limitDlt, curDataDlt, isRequireSlow);
        }
    }

    /**
     * Check if the partitions are ready.
     *
     * @param maxWaitTime max wait time in milliseconds
     * @return partition status
     */
    public boolean isPartitionsReady(long maxWaitTime) {
        long currTime = System.currentTimeMillis();
        do {
            if (this.isClosed.get()) {
                break;
            }
            if (!partitionMap.isEmpty()) {
                return true;
            }
            ThreadUtils.sleep(250);
        } while (System.currentTimeMillis() - currTime > maxWaitTime);
        return (!partitionMap.isEmpty());
    }

    /**
     * Get current partition's consume status.
     * @return current status
     */
    public PartitionSelectResult getCurrPartsStatus() {
        if (isClosed.get()) {
            return new PartitionSelectResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    "Client instance has been shutdown!");
        }
        if (partitionMap.isEmpty()) {
            return new PartitionSelectResult(false,
                    TErrCodeConstants.NO_PARTITION_ASSIGNED,
                    "No partition info in local, please wait and try later");
        }
        if (indexPartition.isEmpty()) {
            if (!timeouts.isEmpty()) {
                return new PartitionSelectResult(false,
                        TErrCodeConstants.ALL_PARTITION_WAITING,
                        "All partition in waiting, retry later!");
            } else if (!partitionUsedMap.isEmpty()) {
                return new PartitionSelectResult(false,
                        TErrCodeConstants.ALL_PARTITION_INUSE,
                        "No idle partition to consume, please wait and try later");
            } else {
                return new PartitionSelectResult(false,
                        TErrCodeConstants.ALL_PARTITION_FROZEN,
                        "All partition are frozen to consume, please unfreeze partition(s) or wait");
            }
        }
        return new PartitionSelectResult(true,
                TErrCodeConstants.SUCCESS, "OK");
    }

    /**
     * Pull the selected partitions.
     *
     * @return pull result
     */
    public PartitionSelectResult pullSelect() {
        PartitionSelectResult result = getCurrPartsStatus();
        if (!result.isSuccess()) {
            return result;
        }
        waitCont.incrementAndGet();
        try {
            rebProcessWait();
            if (this.isClosed.get()) {
                return new PartitionSelectResult(false,
                        TErrCodeConstants.BAD_REQUEST,
                        "Client instance has been shutdown!");
            }
            if (partitionMap.isEmpty()) {
                return new PartitionSelectResult(false,
                        TErrCodeConstants.NO_PARTITION_ASSIGNED,
                        "No partition info in local, please wait and try later");
            }
            String key = indexPartition.poll();
            if (key == null) {
                if (hasPartitionWait()) {
                    return new PartitionSelectResult(false,
                            TErrCodeConstants.ALL_PARTITION_WAITING,
                            "All partition in waiting, retry later!");
                } else if (!partitionUsedMap.isEmpty()) {
                    return new PartitionSelectResult(false,
                            TErrCodeConstants.ALL_PARTITION_INUSE,
                            "No idle partition to consume, please wait and try later");
                } else {
                    return new PartitionSelectResult(false,
                            TErrCodeConstants.ALL_PARTITION_FROZEN,
                            "All partition are frozen to consume, please unfreeze partition(s) or wait");
                }
            }
            PartitionExt partitionExt = partitionMap.get(key);
            if (partitionExt == null) {
                return new PartitionSelectResult(false,
                        TErrCodeConstants.BAD_REQUEST,
                        "No valid partition to consume, retry later 1");
            }
            long curTime = System.currentTimeMillis();
            Long newTime = partitionUsedMap.putIfAbsent(key, curTime);
            if (newTime != null) {
                return new PartitionSelectResult(false,
                        TErrCodeConstants.BAD_REQUEST,
                        "No valid partition to consume, retry later 2");
            }
            return new PartitionSelectResult(true, TErrCodeConstants.SUCCESS, "Ok!",
                    partitionExt, curTime, partitionExt.getAndResetLastPackConsumed());
        } catch (Throwable e1) {
            return new PartitionSelectResult(false,
                    TErrCodeConstants.BAD_REQUEST,
                    new StringBuilder(256)
                            .append("Wait partition to consume abnormal : ")
                            .append(e1.getMessage()).toString());
        } finally {
            waitCont.decrementAndGet();
        }
    }

    /**
     * Push the selected partition.
     *
     * @return push result
     */
    public PartitionSelectResult pushSelect() {
        do {
            if (this.isClosed.get()) {
                break;
            }
            if (!partitionMap.isEmpty()) {
                break;
            }
            ThreadUtils.sleep(300);
        } while (true);
        if (this.isClosed.get()) {
            return null;
        }
        waitCont.incrementAndGet();
        try {
            rebProcessWait();
            if (this.isClosed.get()) {
                return null;
            }
            int cycleCnt = 0;
            String key = null;
            do {
                if (!indexPartition.isEmpty()) {
                    // If there are idle partitions, poll
                    key = indexPartition.poll();
                    if (key != null) {
                        break;
                    }
                }
                if (this.isClosed.get()) {
                    break;
                }
                ThreadUtils.sleep(300);
                // if no idle partitions to get, wait and cycle 500 times
            } while (cycleCnt++ < 500);
            if (key == null) {
                return null;
            }
            PartitionExt partitionExt = partitionMap.get(key);
            if (partitionExt == null) {
                return null;
            }
            long curTime = System.currentTimeMillis();
            Long newTime = partitionUsedMap.putIfAbsent(key, curTime);
            if (newTime != null) {
                return null;
            }
            return new PartitionSelectResult(partitionExt,
                    curTime, partitionExt.getAndResetLastPackConsumed());
        } catch (Throwable e1) {
            return null;
        } finally {
            waitCont.decrementAndGet();
        }
    }

    protected boolean isPartitionInUse(String partitionKey, long usedToken) {
        PartitionExt partitionExt = partitionMap.get(partitionKey);
        if (partitionExt != null) {
            Long curToken = partitionUsedMap.get(partitionKey);
            return curToken != null && curToken == usedToken;
        }
        return false;
    }

    public boolean isPartitionInUse(String partitionKey) {
        return (partitionMap.get(partitionKey) != null);
    }

    public Partition getPartitionByKey(String partitionKey) {
        return partitionMap.get(partitionKey);
    }

    /**
     * Add a partition.
     *
     * @param partition  partition to be added
     * @param currOffset current offset of the partition
     * @param maxOffset current max offset of the partition
     */
    public void addPartition(Partition partition, long currOffset, long maxOffset) {
        if (partition == null) {
            return;
        }
        Map<Partition, ConsumeOffsetInfo> tmpPartOffsetMap = new HashMap<>();
        tmpPartOffsetMap.put(partition,
                new ConsumeOffsetInfo(partition.getPartitionKey(), currOffset, maxOffset));
        addPartitionsInfo(tmpPartOffsetMap);
    }

    /**
     * book a partition for register event.
     *
     * @param partitionKey  partition key
     *
     *  @return Whether to register for the first time
     */
    public boolean bookPartition(String partitionKey) {
        Integer isReged = partRegisterBookMap.get(partitionKey);
        if (isReged == null) {
            isReged = partRegisterBookMap.putIfAbsent(partitionKey, 1);
            return isReged == null;
        }
        return false;
    }

    protected void errReqRelease(String partitionKey, long usedToken, boolean isLastPackConsumed) {
        PartitionExt partitionExt = partitionMap.get(partitionKey);
        if (partitionExt != null) {
            if (!indexPartition.contains(partitionKey) && !isTimeWait(partitionKey)) {
                Long oldUsedToken = partitionUsedMap.get(partitionKey);
                if (oldUsedToken != null && oldUsedToken == usedToken) {
                    oldUsedToken = partitionUsedMap.remove(partitionKey);
                    if (oldUsedToken != null) {
                        partitionExt.setLastPackConsumed(isLastPackConsumed);
                        releaseIdlePartition(partitionKey);
                    }
                }
            }
        }
    }

    protected void succRspRelease(String partitionKey, String topicName,
            long usedToken, boolean isLastPackConsumed,
            boolean isFilterConsume, long currOffset,
            long maxOffset) {
        PartitionExt partitionExt = this.partitionMap.get(partitionKey);
        if (partitionExt != null) {
            if (!indexPartition.contains(partitionKey) && !isTimeWait(partitionKey)) {
                Long oldUsedToken = partitionUsedMap.get(partitionKey);
                if (oldUsedToken != null && oldUsedToken == usedToken) {
                    updateOffsetCache(partitionKey, currOffset, maxOffset);
                    oldUsedToken = partitionUsedMap.remove(partitionKey);
                    if (oldUsedToken != null) {
                        partitionExt.setLastPackConsumed(isLastPackConsumed);
                        long waitDlt =
                                partitionExt.procConsumeResult(isFilterConsume);
                        releaseIdlePartition(waitDlt, partitionKey);
                    }
                }
            }
        }
    }

    /**
     * Release response's partition.
     *
     * @param partitionKey  the partition key to relased
     * @param topicName     the topic name
     * @param usedToken     the used token
     * @param isLastPackConsumed  whether consumed
     * @param currOffset    current offset of the partition
     * @param reqProcType   the request process type
     * @param errCode       the error code
     * @param isEscLimit    Whether to escape the limit
     * @param msgSize       the message size
     * @param limitDlt      the limit delta
     * @param isFilterConsume   whether filter consume
     * @param curDataDlt    the current data delta
     * @param maxOffset     current max offset of the partition
     */
    public void errRspRelease(String partitionKey, String topicName,
            long usedToken, boolean isLastPackConsumed,
            long currOffset, int reqProcType, int errCode,
            boolean isEscLimit, int msgSize, long limitDlt,
            boolean isFilterConsume, long curDataDlt, long maxOffset) {
        PartitionExt partitionExt = this.partitionMap.get(partitionKey);
        if (partitionExt != null) {
            if (!indexPartition.contains(partitionKey) && !isTimeWait(partitionKey)) {
                Long oldUsedToken = partitionUsedMap.get(partitionKey);
                if (oldUsedToken != null && oldUsedToken == usedToken) {
                    updateOffsetCache(partitionKey, currOffset, maxOffset);
                    oldUsedToken = partitionUsedMap.remove(partitionKey);
                    if (oldUsedToken != null) {
                        partitionExt.setLastPackConsumed(isLastPackConsumed);
                        long waitDlt =
                                partitionExt.procConsumeResult(isFilterConsume, reqProcType,
                                        errCode, msgSize, isEscLimit, limitDlt, curDataDlt, false);
                        releaseIdlePartition(waitDlt, partitionKey);
                    }
                }
            }
        }
    }

    public void updPartOffsetInfo(String partitionKey, long currOffset, long maxOffset) {
        PartitionExt partitionExt = this.partitionMap.get(partitionKey);
        if (partitionExt != null) {
            updateOffsetCache(partitionKey, currOffset, maxOffset);
        }
    }

    private void releaseIdlePartition(long waitDlt, String partitionKey) {
        Long frozenTime = partitionFrozenMap.get(partitionKey);
        if (frozenTime == null) {
            if (waitDlt > 10) {
                TimeoutTask timeoutTask = new TimeoutTask(partitionKey);
                timeouts.put(partitionKey, timer.newTimeout(
                        timeoutTask, waitDlt, TimeUnit.MILLISECONDS));
            } else {
                releaseIdlePartition(partitionKey);
            }
        }
    }

    private void releaseIdlePartition(String partitionKey) {
        Long frozenTime = partitionFrozenMap.get(partitionKey);
        PartitionExt partitionExt = partitionMap.get(partitionKey);
        Timeout timeout = timeouts.get(partitionKey);
        Long usedTime = partitionUsedMap.get(partitionKey);
        if (partitionExt == null
                || frozenTime != null
                || timeout != null
                || usedTime != null) {
            return;
        }
        if (!indexPartition.contains(partitionKey)) {
            try {
                indexPartition.offer(partitionKey);
            } catch (Throwable e) {
                //
            }
        }
    }

    /**
     * Close the remote data cache
     */
    @Override
    public void close() {
        if (this.isClosed.get()) {
            return;
        }
        if (this.isClosed.compareAndSet(false, true)) {
            if (refCont.decrementAndGet() == 0) {
                timer.stop();
                timer = null;
            }
            int cnt = 5;
            while (this.waitCont.get() > 0) {
                ThreadUtils.sleep(200);
                if (--cnt <= 0) {
                    break;
                }
            }
        }
    }

    /**
     * Get the subscribe information of the consumer.
     *
     * @param consumerId   consumer id
     * @param consumeGroup consumer group
     * @return subscribe information list
     */
    public List<SubscribeInfo> getSubscribeInfoList(String consumerId, String consumeGroup) {
        List<SubscribeInfo> subscribeInfoList = new ArrayList<>();
        for (Partition partition : partitionMap.values()) {
            if (partition != null) {
                subscribeInfoList.add(new SubscribeInfo(consumerId, consumeGroup, partition));
            }
        }
        return subscribeInfoList;
    }

    /**
     * Get the subscribe partitionKey set.
     *
     * @return subscribe information list
     */
    private List<String> getSubscribedPartitionInfo() {
        List<String> strSubInfoList = new ArrayList<>();
        Map<String, StringBuilder> tmpSubInfoMap = new HashMap<>();
        for (Partition partition : partitionMap.values()) {
            if (partition == null) {
                continue;
            }
            StringBuilder sBuffer = tmpSubInfoMap.get(partition.getTopic());
            if (sBuffer == null) {
                sBuffer = new StringBuilder(512);
                tmpSubInfoMap.put(partition.getTopic(), sBuffer);
                sBuffer.append(partition.getTopic()).append(TokenConstants.SEGMENT_SEP);
            } else {
                sBuffer.append(TokenConstants.ARRAY_SEP);
            }
            sBuffer.append(partition.getBrokerId())
                    .append(TokenConstants.ATTR_SEP).append(partition.getPartitionId());
        }
        for (Map.Entry<String, StringBuilder> entry : tmpSubInfoMap.entrySet()) {
            strSubInfoList.add(entry.getValue().toString());
        }
        return strSubInfoList;
    }

    /**
     * Remove and get required partitions
     * @param unRegisterInfoMap  the unregistered partitions
     * @param partitionKeys      the partition keys
     * @param inUseWaitPeriodMs  the wait period in ms
     * @param isWaitTimeoutRollBack  whether wait timout
     *
     * @return the removed partitions
     */
    public Map<BrokerInfo, List<PartitionSelectResult>> removeAndGetPartition(
            Map<BrokerInfo, List<Partition>> unRegisterInfoMap,
            List<String> partitionKeys, long inUseWaitPeriodMs,
            boolean isWaitTimeoutRollBack) {
        StringBuilder sBuilder = new StringBuilder(512);
        HashMap<BrokerInfo, List<PartitionSelectResult>> unNewRegisterInfoMap =
                new HashMap<>();
        pauseProcess();
        try {
            waitPartitions(partitionKeys, inUseWaitPeriodMs);
            boolean lastPackConsumed = false;
            for (Map.Entry<BrokerInfo, List<Partition>> entry : unRegisterInfoMap.entrySet()) {
                for (Partition partition : entry.getValue()) {
                    PartitionExt partitionExt =
                            rmvPartitionFromMap(partition.getPartitionKey());
                    if (partitionExt != null) {
                        lastPackConsumed = partitionExt.isLastPackConsumed();
                        if (!cancelTimeTask(partition.getPartitionKey())
                                && !indexPartition.remove(partition.getPartitionKey())) {
                            logger.info(sBuilder.append("[Process Interrupt] Partition : ")
                                    .append(partition.toString())
                                    .append(", data in processing, canceled").toString());
                            sBuilder.delete(0, sBuilder.length());
                            if (lastPackConsumed) {
                                if (isWaitTimeoutRollBack) {
                                    lastPackConsumed = false;
                                }
                            }
                        }
                        ConcurrentLinkedQueue<Partition> oldPartitionList =
                                topicPartitionConMap.get(partition.getTopic());
                        if (oldPartitionList != null) {
                            oldPartitionList.remove(partition);
                            if (oldPartitionList.isEmpty()) {
                                topicPartitionConMap.remove(partition.getTopic());
                            }
                        }
                        ConcurrentLinkedQueue<Partition> regMapPartitionList =
                                brokerPartitionConMap.get(entry.getKey());
                        if (regMapPartitionList != null) {
                            regMapPartitionList.remove(partition);
                            if (regMapPartitionList.isEmpty()) {
                                brokerPartitionConMap.remove(entry.getKey());
                            }
                        }
                        partitionOffsetMap.remove(partition.getPartitionKey());
                        partitionUsedMap.remove(partition.getPartitionKey());
                        PartitionSelectResult partitionRet =
                                new PartitionSelectResult(true, TErrCodeConstants.SUCCESS,
                                        "Ok!", partition, 0, lastPackConsumed);
                        List<PartitionSelectResult> targetPartitionList =
                                unNewRegisterInfoMap.computeIfAbsent(
                                        entry.getKey(), k -> new ArrayList<>());
                        targetPartitionList.add(partitionRet);
                    }
                }
            }
        } finally {
            resumeProcess();
        }
        return unNewRegisterInfoMap;
    }

    /**
     * Remove and get required partitions
     * @param partitionKey      the partition key
     * @param inUseWaitPeriodMs  the wait period in ms
     * @param isWaitTimeoutRollBack  whether wait timout
     * @param result  the process result
     * @param sBuffer  the string buffer
     *
     * @return removed or not
     */
    public boolean removeAndGetPartition(String partitionKey, long inUseWaitPeriodMs,
            boolean isWaitTimeoutRollBack, ProcessResult result,
            StringBuilder sBuffer) {
        boolean lastPackConsumed = false;
        List<String> partitionKeys = new ArrayList<>();
        partitionKeys.add(partitionKey);
        pauseProcess();
        try {
            waitPartitions(partitionKeys, inUseWaitPeriodMs);
            PartitionExt partitionExt =
                    rmvPartitionFromMap(partitionKey);
            if (partitionExt == null) {
                result.setSuccResult(null);
                return result.isSuccess();
            }
            lastPackConsumed = partitionExt.isLastPackConsumed();
            if (!cancelTimeTask(partitionKey)
                    && !indexPartition.remove(partitionKey)) {
                logger.info(sBuffer.append("[Process Interrupt] Partition : ")
                        .append(partitionExt.toString())
                        .append(", data in processing, canceled").toString());
                sBuffer.delete(0, sBuffer.length());
                if (lastPackConsumed) {
                    if (isWaitTimeoutRollBack) {
                        lastPackConsumed = false;
                    }
                }
            }
            ConcurrentLinkedQueue<Partition> oldPartitionList =
                    topicPartitionConMap.get(partitionExt.getTopic());
            if (oldPartitionList != null) {
                oldPartitionList.remove(partitionExt);
                if (oldPartitionList.isEmpty()) {
                    topicPartitionConMap.remove(partitionExt.getTopic());
                }
            }
            ConcurrentLinkedQueue<Partition> regMapPartitionList =
                    brokerPartitionConMap.get(partitionExt.getBroker());
            if (regMapPartitionList != null) {
                regMapPartitionList.remove(partitionExt);
                if (regMapPartitionList.isEmpty()) {
                    brokerPartitionConMap.remove(partitionExt.getBroker());
                }
            }
            partitionOffsetMap.remove(partitionKey);
            partitionUsedMap.remove(partitionKey);
            partitionExt.setLastPackConsumed(lastPackConsumed);
            result.setSuccResult(partitionExt);
            return result.isSuccess();
        } finally {
            resumeProcess();
        }
    }

    /**
     * Remove a partition.
     *
     * @param partition partition to be removed
     */
    public void removePartition(Partition partition) {
        rmvPartitionFromMap(partition.getPartitionKey());
        cancelTimeTask(partition.getPartitionKey());
        indexPartition.remove(partition.getPartitionKey());
        partitionUsedMap.remove(partition.getPartitionKey());
        partitionOffsetMap.remove(partition.getPartitionKey());
        ConcurrentLinkedQueue<Partition> oldPartitionList =
                topicPartitionConMap.get(partition.getTopic());
        if (oldPartitionList != null) {
            oldPartitionList.remove(partition);
            if (oldPartitionList.isEmpty()) {
                topicPartitionConMap.remove(partition.getTopic());
            }
        }
        ConcurrentLinkedQueue<Partition> regMapPartitionList =
                brokerPartitionConMap.get(partition.getBroker());
        if (regMapPartitionList != null) {
            regMapPartitionList.remove(partition);
            if (regMapPartitionList.isEmpty()) {
                brokerPartitionConMap.remove(partition.getBroker());
            }
        }
    }

    public Set<String> getCurRegisteredPartSet() {
        Set<String> partKeySet = new TreeSet<>();
        for (String partKey : partitionMap.keySet()) {
            if (partKey == null) {
                continue;
            }
            partKeySet.add(partKey);
        }
        return partKeySet;
    }

    /**
     * Get current partition information.
     *
     * @return consumer offset information map
     */
    public Map<String, ConsumeOffsetInfo> getCurPartitionInfoMap() {
        Map<String, ConsumeOffsetInfo> tmpPartitionMap =
                new ConcurrentHashMap<>();
        for (Map.Entry<String, PartitionExt> entry : partitionMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            ConsumeOffsetInfo offsetInfo = partitionOffsetMap.get(entry.getKey());
            tmpPartitionMap.put(entry.getKey(),
                    new ConsumeOffsetInfo(entry.getKey(), offsetInfo.getCurrOffset(),
                            offsetInfo.getMaxOffset(), offsetInfo.getUpdateTime()));
        }
        return tmpPartitionMap;
    }

    public long getMaxOffsetOfPartition(String partitionKey) {
        ConsumeOffsetInfo offsetInfo = partitionOffsetMap.get(partitionKey);
        if (offsetInfo == null) {
            return -1L;
        }
        return offsetInfo.getMaxOffset();
    }

    public Map<BrokerInfo, List<PartitionSelectResult>> getAllPartitionListWithStatus() {
        Map<BrokerInfo, List<PartitionSelectResult>> registeredInfoMap =
                new HashMap<>();
        for (PartitionExt partitionExt : partitionMap.values()) {
            List<PartitionSelectResult> registerPartitionList =
                    registeredInfoMap.computeIfAbsent(
                            partitionExt.getBroker(), k -> new ArrayList<>());
            registerPartitionList.add(new PartitionSelectResult(true,
                    TErrCodeConstants.SUCCESS, "Ok!",
                    partitionExt, 0, partitionExt.isLastPackConsumed()));
        }
        return registeredInfoMap;
    }

    /**
     * Get registered brokers.
     *
     * @return broker information list
     */
    public Set<BrokerInfo> getAllRegisterBrokers() {
        return this.brokerPartitionConMap.keySet();
    }

    /**
     * Get partition list of a broker.
     *
     * @param brokerInfo broker information
     * @return partition list
     */
    public List<Partition> getBrokerPartitionList(BrokerInfo brokerInfo) {
        List<Partition> retPartition = new ArrayList<>();
        ConcurrentLinkedQueue<Partition> partitionList =
                brokerPartitionConMap.get(brokerInfo);
        if (partitionList != null) {
            retPartition.addAll(partitionList);
        }
        return retPartition;
    }

    /**
     * Filter cached partition information
     *
     * @param registerInfoMap     the partitions to register
     * @param unRegPartitionList  the unregistered partition list
     */
    public void filterCachedPartitionInfo(Map<BrokerInfo, List<Partition>> registerInfoMap,
            List<Partition> unRegPartitionList) {
        List<BrokerInfo> brokerInfoList = new ArrayList<>();
        for (Map.Entry<BrokerInfo, List<Partition>> entry : registerInfoMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            ConcurrentLinkedQueue<Partition> partitionList =
                    brokerPartitionConMap.get(entry.getKey());
            if (partitionList == null || partitionList.isEmpty()) {
                unRegPartitionList.addAll(entry.getValue());
            } else {
                boolean isNewBroker = true;
                for (Partition regPartition : entry.getValue()) {
                    if (!partitionList.contains(regPartition)) {
                        unRegPartitionList.add(regPartition);
                        isNewBroker = false;
                    }
                }
                if (isNewBroker) {
                    brokerInfoList.add(entry.getKey());
                }
            }
        }
        for (BrokerInfo brokerInfo : brokerInfoList) {
            registerInfoMap.remove(brokerInfo);
        }
    }

    public ConcurrentLinkedQueue<Partition> getPartitionByBroker(BrokerInfo brokerInfo) {
        return this.brokerPartitionConMap.get(brokerInfo);
    }

    /**
     * Resume consume timeout partitions
     *
     * @param isPullConsume       whether is pull consume
     * @param allowedPeriodTimes  allowed hold duration
     */
    public void resumeTimeoutConsumePartitions(boolean isPullConsume, long allowedPeriodTimes) {
        if (isPullConsume) {
            // For pull consume, do timeout check on partitions pulled without confirm
            if (!partitionUsedMap.isEmpty()) {
                List<String> partKeys = new ArrayList<>(partitionUsedMap.keySet());
                for (String keyId : partKeys) {
                    Long oldTime = partitionUsedMap.get(keyId);
                    if (oldTime != null && System.currentTimeMillis() - oldTime > allowedPeriodTimes) {
                        oldTime = partitionUsedMap.remove(keyId);
                        if (oldTime != null) {
                            PartitionExt partitionExt = partitionMap.get(keyId);
                            if (partitionExt != null) {
                                partitionExt.setLastPackConsumed(false);
                                releaseIdlePartition(keyId);
                            }
                        }
                    }
                }
            }
        }
        // add timeout expired check
        if (!timeouts.isEmpty()) {
            Timeout timeout1 = null;
            List<String> partKeys = new ArrayList<>(timeouts.keySet());
            for (String keyId : partKeys) {
                timeout1 = timeouts.get(keyId);
                if (timeout1 != null && timeout1.isExpired()) {
                    timeout1 = timeouts.remove(keyId);
                    if (timeout1 != null) {
                        releaseIdlePartition(keyId);
                    }
                }
            }
        }
    }

    /**
     * Freeze or unfreeze partitons
     *
     * @param partitionKeys    the operation targets
     * @param isFreeze         Freeze or unfreeze operation
     */
    public void freezeOrUnFreezeParts(List<String> partitionKeys, boolean isFreeze) {
        if (partitionKeys == null || partitionKeys.isEmpty()) {
            return;
        }
        for (String partitionKey : partitionKeys) {
            if (partitionKey == null) {
                continue;
            }
            if (isFreeze) {
                partitionFrozenMap.put(partitionKey, System.currentTimeMillis());
                logger.info(new StringBuilder(512)
                        .append("[Freeze Partition] Partition : ")
                        .append(partitionKey).append(" is frozen by caller!").toString());
            } else {
                Long frozenTime = partitionFrozenMap.remove(partitionKey);
                if (frozenTime != null) {
                    releaseIdlePartition(partitionKey);
                    logger.info(new StringBuilder(512)
                            .append("[UnFreeze Partition] Partition : ")
                            .append(partitionKey).append(" is unFreeze by caller!").toString());
                }
            }
        }
    }

    public void relAllFrozenPartitions() {
        Long frozenTime = null;
        List<String> partKeys = new ArrayList<>(partitionFrozenMap.keySet());
        for (String partKey : partKeys) {
            frozenTime = partitionFrozenMap.remove(partKey);
            if (frozenTime != null) {
                releaseIdlePartition(partKey);
                logger.info(new StringBuilder(512)
                        .append("[UnFreeze Partition] Partition : ")
                        .append(partKey).append(" is unFreeze by caller-2!").toString());
            }
        }
    }

    public Map<String, Long> getFrozenPartInfo() {
        Map<String, Long> tmpPartKeyMap = new HashMap<String, Long>();
        for (Map.Entry<String, Long> entry : partitionFrozenMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            tmpPartKeyMap.put(entry.getKey(), entry.getValue());
        }
        return tmpPartKeyMap;
    }

    private void waitPartitions(List<String> partitionKeys, long inUseWaitPeriodMs) {
        boolean needWait = false;
        long startWaitTime = System.currentTimeMillis();
        do {
            needWait = false;
            for (String partitionKey : partitionKeys) {
                if (partitionUsedMap.get(partitionKey) != null) {
                    needWait = true;
                    break;
                }
            }
            if (needWait) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e1) {
                    break;
                }
            }
        } while ((needWait)
                && (!this.isClosed.get())
                && ((System.currentTimeMillis() - startWaitTime) < inUseWaitPeriodMs));

    }

    private void updateOffsetCache(String partitionKey, long currOffset, long maxOffset) {
        if (currOffset >= 0) {
            ConsumeOffsetInfo currOffsetInfo = partitionOffsetMap.get(partitionKey);
            if (currOffsetInfo == null) {
                currOffsetInfo =
                        new ConsumeOffsetInfo(partitionKey, currOffset, maxOffset);
                ConsumeOffsetInfo tmpOffsetInfo =
                        partitionOffsetMap.putIfAbsent(partitionKey, currOffsetInfo);
                if (tmpOffsetInfo != null) {
                    currOffsetInfo = tmpOffsetInfo;
                }
            }
            currOffsetInfo.updateOffsetInfo(currOffset, maxOffset);
        }
    }

    private void addPartitionsInfo(Map<Partition, ConsumeOffsetInfo> partOffsetMap) {
        if (partOffsetMap == null || partOffsetMap.isEmpty()) {
            return;
        }
        for (Map.Entry<Partition, ConsumeOffsetInfo> entry : partOffsetMap.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            Partition partition = entry.getKey();
            if (partitionMap.containsKey(partition.getPartitionKey())) {
                continue;
            }
            ConcurrentLinkedQueue<Partition> topicPartitionQue =
                    topicPartitionConMap.get(partition.getTopic());
            if (topicPartitionQue == null) {
                topicPartitionQue = new ConcurrentLinkedQueue<>();
                ConcurrentLinkedQueue<Partition> tmpTopicPartitionQue =
                        topicPartitionConMap.putIfAbsent(partition.getTopic(), topicPartitionQue);
                if (tmpTopicPartitionQue != null) {
                    topicPartitionQue = tmpTopicPartitionQue;
                }
            }
            if (!topicPartitionQue.contains(partition)) {
                topicPartitionQue.add(partition);
            }
            ConcurrentLinkedQueue<Partition> brokerPartitionQue =
                    brokerPartitionConMap.get(partition.getBroker());
            if (brokerPartitionQue == null) {
                brokerPartitionQue = new ConcurrentLinkedQueue<>();
                ConcurrentLinkedQueue<Partition> tmpBrokerPartQues =
                        brokerPartitionConMap.putIfAbsent(partition.getBroker(), brokerPartitionQue);
                if (tmpBrokerPartQues != null) {
                    brokerPartitionQue = tmpBrokerPartQues;
                }
            }
            if (!brokerPartitionQue.contains(partition)) {
                brokerPartitionQue.add(partition);
            }
            updateOffsetCache(partition.getPartitionKey(),
                    entry.getValue().getCurrOffset(), entry.getValue().getMaxOffset());
            addPartitionToMap(partition.getPartitionKey(),
                    new PartitionExt(this.groupFlowCtrlRuleHandler,
                            this.defFlowCtrlRuleHandler, partition.getBroker(),
                            partition.getTopic(), partition.getPartitionId()));
            partitionUsedMap.remove(partition.getPartitionKey());
            releaseIdlePartition(partition.getPartitionKey());
        }
    }

    public void rebProcessWait() {
        if (this.dataProcessSync != null
                && this.dataProcessSync.getCount() != 0) {
            try {
                this.dataProcessSync.await();
            } catch (InterruptedException ee) {
                //
            }
        }
    }

    public boolean isRebProcessing() {
        return (this.dataProcessSync != null
                && this.dataProcessSync.getCount() != 0);
    }

    private void addPartitionToMap(String partKey, PartitionExt partitionExt) {
        partitionMap.put(partKey, partitionExt);
        partMapChgTime.set(System.currentTimeMillis());
    }

    private PartitionExt rmvPartitionFromMap(String partKey) {
        PartitionExt tmpPartExt = partitionMap.remove(partKey);
        if (tmpPartExt != null) {
            partMapChgTime.set(System.currentTimeMillis());
        }
        return tmpPartExt;
    }

    private void pauseProcess() {
        this.dataProcessSync = new CountDownLatch(1);
    }

    private void resumeProcess() {
        this.dataProcessSync.countDown();
    }

    private boolean cancelTimeTask(String indexId) {
        Timeout timeout = timeouts.remove(indexId);
        if (timeout != null) {
            timeout.cancel();
            return true;
        }
        return false;
    }

    private boolean isTimeWait(String indexId) {
        return (timeouts.get(indexId) != null);
    }

    private boolean hasPartitionWait() {
        return !this.timeouts.isEmpty();
    }

    public class TimeoutTask implements TimerTask {

        private String indexId;
        private long createTime = 0L;

        public TimeoutTask(final String indexId) {
            this.indexId = indexId;
            this.createTime = System.currentTimeMillis();
        }

        public long getCreateTime() {
            return this.createTime;
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            Timeout timeout1 = timeouts.remove(indexId);
            if (timeout1 != null) {
                releaseIdlePartition(indexId);
            }
        }
    }
}
