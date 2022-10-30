/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.master.nodemanage.nodebroker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.cluster.BrokerInfo;
import org.apache.inlong.tubemq.corebase.cluster.Partition;
import org.apache.inlong.tubemq.corebase.cluster.TopicInfo;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.HeartResponseM2B;
import org.apache.inlong.tubemq.corebase.protobuf.generated.ClientMaster.RegisterResponseM2B;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.Tuple2;
import org.apache.inlong.tubemq.corebase.utils.Tuple3;
import org.apache.inlong.tubemq.corebase.utils.Tuple4;
import org.apache.inlong.tubemq.server.common.heartbeat.HeartbeatManager;
import org.apache.inlong.tubemq.server.common.heartbeat.TimeoutInfo;
import org.apache.inlong.tubemq.server.common.heartbeat.TimeoutListener;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.common.utils.SerialIdUtils;
import org.apache.inlong.tubemq.server.master.MasterConfig;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.metamanage.MetaDataService;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.ConfigObserver;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.stats.MasterSrvStatsHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Broker run manager
 */
public class DefBrokerRunManager implements BrokerRunManager, ConfigObserver {
    private static final Logger logger =
            LoggerFactory.getLogger(DefBrokerRunManager.class);
    // meta data manager
    private final MetaDataService metaDataService;
    private final HeartbeatManager heartbeatManager;
    // broker string info
    private final AtomicLong brokerInfoCheckSum =
            new AtomicLong(System.currentTimeMillis());
    private long lastBrokerUpdatedTime = System.currentTimeMillis();
    private final ConcurrentHashMap<Integer, String> brokersMap =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, String> brokersTLSMap =
            new ConcurrentHashMap<>();
    // broker sync FSM
    private final AtomicInteger brokerTotalCount =
            new AtomicInteger(0);
    // brokerId -- broker run status info map
    private final ConcurrentHashMap<Integer, BrokerRunStatusInfo> brokerRunSyncManageMap =
            new ConcurrentHashMap<>();
    // broker abnormal holder
    private final BrokerAbnHolder brokerAbnHolder;
    // broker topic configure for consumer and producer
    private final BrokerPSInfoHolder brokerPubSubInfo = new BrokerPSInfoHolder();

    /**
     * Constructor by TMaster
     *
     * @param tMaster  the initial TMaster object
     */
    public DefBrokerRunManager(TMaster tMaster) {
        this.metaDataService = tMaster.getMetaDataService();
        this.heartbeatManager = tMaster.getHeartbeatManager();
        MasterConfig masterConfig = tMaster.getMasterConfig();
        this.brokerAbnHolder =
                new BrokerAbnHolder(masterConfig.getMaxAutoForbiddenCnt(), this.metaDataService);
        heartbeatManager.regBrokerCheckBusiness(masterConfig.getBrokerHeartbeatTimeoutMs(),
                new TimeoutListener() {
                    @Override
                    public void onTimeout(final String nodeId, TimeoutInfo nodeInfo) throws Exception {
                        logger.info(new StringBuilder(512).append("[Broker Timeout] ")
                                .append(nodeId).toString());
                        releaseBrokerRunInfo(Integer.parseInt(nodeId),
                                nodeInfo.getSecondKey(), true);
                    }
                });
        this.metaDataService.regMetaConfigObserver(this);
    }

    @Override
    public void clearCacheData() {
        // cache data not need clear
    }

    @Override
    public void reloadCacheData() {
        updBrokerStaticInfo(metaDataService.getBrokerConfInfo(null));
    }

    @Override
    public Tuple2<Long, Map<Integer, String>> getBrokerStaticInfo(boolean isOverTLS) {
        if (isOverTLS) {
            return new Tuple2<>(brokerInfoCheckSum.get(), brokersTLSMap);
        } else {
            return new Tuple2<>(brokerInfoCheckSum.get(), brokersMap);
        }
    }

    @Override
    public void updBrokerStaticInfo(Map<Integer, BrokerConfEntity> brokerConfMap) {
        if (brokerConfMap == null || brokerConfMap.isEmpty()) {
            return;
        }
        for (BrokerConfEntity entity : brokerConfMap.values()) {
            updBrokerStaticInfo(entity);
        }
    }

    @Override
    public void updBrokerStaticInfo(BrokerConfEntity entity) {
        if (entity == null) {
            return;
        }
        String brokerReg =
                this.brokersMap.putIfAbsent(entity.getBrokerId(),
                        entity.getSimpleBrokerInfo());
        String brokerTLSReg =
                this.brokersTLSMap.putIfAbsent(entity.getBrokerId(),
                        entity.getSimpleTLSBrokerInfo());
        if (brokerReg == null
                || brokerTLSReg == null
                || !brokerReg.equals(entity.getSimpleBrokerInfo())
                || !brokerTLSReg.equals(entity.getSimpleTLSBrokerInfo())) {
            if (brokerReg == null) {
                MasterSrvStatsHolder.incBrokerConfigCnt();
            } else {
                if (!brokerReg.equals(entity.getSimpleBrokerInfo())) {
                    this.brokersMap.put(entity.getBrokerId(), entity.getSimpleBrokerInfo());
                }
            }
            if (brokerTLSReg != null
                    && !brokerTLSReg.equals(entity.getSimpleTLSBrokerInfo())) {
                this.brokersTLSMap.put(entity.getBrokerId(), entity.getSimpleTLSBrokerInfo());
            }
            SerialIdUtils.updTimeStampSerialIdValue(this.brokerInfoCheckSum);
        }
    }

    @Override
    public void delBrokerStaticInfo(int brokerId) {
        if (brokerId == TBaseConstants.META_VALUE_UNDEFINED) {
            return;
        }
        String brokerReg = this.brokersMap.remove(brokerId);
        String brokerTLSReg = this.brokersTLSMap.remove(brokerId);
        if (brokerReg != null || brokerTLSReg != null) {
            SerialIdUtils.updTimeStampSerialIdValue(this.brokerInfoCheckSum);
        }
    }

    @Override
    public Tuple2<Boolean, Boolean> getBrokerPublishStatus(int brokerId) {
        return brokerPubSubInfo.getBrokerPubStatus(brokerId);
    }

    @Override
    public BrokerAbnHolder getBrokerAbnHolder() {
        return this.brokerAbnHolder;
    }

    @Override
    public boolean brokerRegister2M(String clientId, BrokerInfo brokerInfo,
                                    long reportConfigId, int reportCheckSumId,
                                    boolean isTackData, String repBrokerConfInfo,
                                    List<String> repTopicConfInfo, boolean isOnline,
                                    boolean isOverTLS, StringBuilder sBuffer,
                                    ProcessResult result) {
        BrokerConfEntity brokerEntry =
                metaDataService.getBrokerConfByBrokerId(brokerInfo.getBrokerId());
        if (brokerEntry == null) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    sBuffer.append("Not found broker configure info, please create first!")
                            .append(" the connecting client id is:")
                            .append(clientId).toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        if ((!brokerInfo.getHost().equals(brokerEntry.getBrokerIp()))
                || (brokerInfo.getPort() != brokerEntry.getBrokerPort())) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    sBuffer.append("Inconsistent broker configure,please confirm first!")
                            .append(" the connecting client id is:").append(clientId)
                            .append(", the configured broker address by brokerId is:")
                            .append(brokerEntry.getBrokerIdAndAddress()).toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        int confTLSPort = brokerEntry.getBrokerTLSPort();
        if (confTLSPort != brokerInfo.getTlsPort()) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    sBuffer.append("Inconsistent TLS configure, please confirm first!")
                            .append(" the connecting client id is:").append(clientId)
                            .append(", the configured TLS port is:").append(confTLSPort)
                            .append(", the broker reported TLS port is ")
                            .append(brokerInfo.getTlsPort()).toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        if (brokerEntry.getManageStatus() == ManageStatus.STATUS_MANAGE_APPLY) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    sBuffer.append("Broker's configure not online, please online configure first!")
                            .append(" the connecting client id is:").append(clientId).toString());
            sBuffer.delete(0, sBuffer.length());
            return result.isSuccess();
        }
        brokerEntry.getBrokerDefaultConfInfo(
                metaDataService.getClusterDefSetting(false), sBuffer);
        String brokerConfInfo = sBuffer.toString();
        sBuffer.delete(0, sBuffer.length());
        Map<String, String> topicConfInfoMap =
                metaDataService.getBrokerTopicStrConfigInfo(brokerEntry, sBuffer);
        //
        BrokerRunStatusInfo runStatusInfo =
                brokerRunSyncManageMap.get(brokerInfo.getBrokerId());
        if (runStatusInfo == null) {
            BrokerRunStatusInfo tmpRunStatusInfo =
                    new BrokerRunStatusInfo(this, brokerInfo,
                            brokerEntry.getManageStatus(), brokerConfInfo,
                            topicConfInfoMap, isOverTLS);
            runStatusInfo =
                    brokerRunSyncManageMap.putIfAbsent(
                            brokerInfo.getBrokerId(), tmpRunStatusInfo);
            if (runStatusInfo == null) {
                brokerTotalCount.incrementAndGet();
                MasterSrvStatsHolder.incBrokerOnlineCnt();
                runStatusInfo = tmpRunStatusInfo;
            }
        } else {
            runStatusInfo.reInitRunStatusInfo(brokerInfo,
                    brokerEntry.getManageStatus(), brokerConfInfo,
                    topicConfInfoMap, isOverTLS);
        }
        runStatusInfo.bookBrokerReportInfo(true, isOnline, reportConfigId,
                reportCheckSumId, isTackData, repBrokerConfInfo, repTopicConfInfo, sBuffer);
        heartbeatManager.regBrokerNode(String.valueOf(brokerInfo.getBrokerId()),
                runStatusInfo.getCreateId());
        result.setSuccResult(null);
        return result.isSuccess();
    }

    @Override
    public boolean brokerHeartBeat2M(int brokerId, long reportConfigId, int reportCheckSumId,
                                     boolean isTackData, String repBrokerConfInfo,
                                     List<String> repTopicConfInfo,
                                     boolean isTackRmvInfo, List<String> removedTopics,
                                     int rptReadStatus, int rptWriteStatus, boolean isOnline,
                                     StringBuilder sBuffer, ProcessResult result) {
        BrokerRunStatusInfo runStatusInfo =
                brokerRunSyncManageMap.get(brokerId);
        if (runStatusInfo == null) {
            result.setFailResult(TErrCodeConstants.HB_NO_NODE, sBuffer
                    .append("Not found Broker run status info, please register broker first!")
                    .append(" the connecting client id is:").append(brokerId).toString());
            return result.isSuccess();
        }
        // update heartbeat
        if (!heartbeatManager.updBrokerNode(String.valueOf(brokerId),
                runStatusInfo.getCreateId(), sBuffer, result)) {
            return result.isSuccess();
        }
        // update broker status
        runStatusInfo.bookBrokerReportInfo(false, isOnline, reportConfigId,
                reportCheckSumId, isTackData, repBrokerConfInfo, repTopicConfInfo, sBuffer);
        // process removed topic info
        if (isTackRmvInfo) {
            metaDataService.delCleanedTopicDeployInfo(brokerId, removedTopics, sBuffer, result);
            logger.info(sBuffer.append("[Broker Report] receive broker removed topics = ")
                    .append(removedTopics.toString()).append(", removed result is ")
                    .append(result.getErrMsg()).toString());
            sBuffer.delete(0, sBuffer.length());
        }
        brokerAbnHolder.updateBrokerReportStatus(brokerId, rptReadStatus, rptWriteStatus);
        result.setSuccResult(null);
        return result.isSuccess();
    }

    @Override
    public boolean brokerClose2M(int brokerId, StringBuilder sBuffer, ProcessResult result) {
        BrokerRunStatusInfo runStatusInfo =
                brokerRunSyncManageMap.get(brokerId);
        if (runStatusInfo == null) {
            result.setFailResult(TErrCodeConstants.HB_NO_NODE, sBuffer
                    .append("Not found Broker run status info, please register broker first!")
                    .append(" the connecting client id is:").append(brokerId).toString());
            return result.isSuccess();
        }
        if (!heartbeatManager.unRegBrokerNode(String.valueOf(brokerId),
                runStatusInfo.getCreateId())) {
            logger.info(sBuffer.append("[Broker Closed] brokerId=").append(brokerId)
                    .append(" unregister failure, run-info has been replaced by new request!")
                    .toString());
            return result.isSuccess();
        }
        boolean isOverTls = runStatusInfo.isOverTLS();
        releaseBrokerRunInfo(brokerId, runStatusInfo.getCreateId(), false);
        logger.info(sBuffer.append("[Broker Closed]").append(brokerId)
                .append(" unregister success, isOverTLS=").append(isOverTls).toString());
        result.setSuccResult(null);
        return result.isSuccess();
    }

    @Override
    public Tuple3<ManageStatus, String, Map<String, String>> getBrokerMetaConfigInfo(int brokerId) {
        String brokerConfInfo = null;
        ManageStatus manageStatus = ManageStatus.STATUS_MANAGE_UNDEFINED;
        StringBuilder sBuffer = new StringBuilder(512);
        BrokerConfEntity brokerConfEntity =
                metaDataService.getBrokerConfByBrokerId(brokerId);
        if (brokerConfEntity != null) {
            brokerConfEntity.getBrokerDefaultConfInfo(
                    metaDataService.getClusterDefSetting(false), sBuffer);
            brokerConfInfo = sBuffer.toString();
            sBuffer.delete(0, sBuffer.length());
            manageStatus = brokerConfEntity.getManageStatus();
        }
        Map<String, String> brokerTopicSetConfInfo =
                this.metaDataService.getBrokerTopicStrConfigInfo(brokerConfEntity, sBuffer);
        return new Tuple3<>(manageStatus, brokerConfInfo, brokerTopicSetConfInfo);
    }

    @Override
    public void setRegisterDownConfInfo(int brokerId, StringBuilder sBuffer,
                                        RegisterResponseM2B.Builder builder) {
        BrokerRunStatusInfo runStatusInfo =
                brokerRunSyncManageMap.get(brokerId);
        if (runStatusInfo == null) {
            logger.info(sBuffer.append("Get Broker run-info failure, brokerId=")
                    .append(brokerId).append(", please check the implement first!")
                    .toString());
            sBuffer.delete(0, sBuffer.length());
            return;
        }
        Tuple4<Long, Integer, String, List<String>> retTuple =
                runStatusInfo.getNeedSyncData();
        builder.setCurBrokerConfId(retTuple.getF0());
        builder.setConfCheckSumId(retTuple.getF1());
        Tuple2<Boolean, Boolean> autoFbdTuple =
                brokerAbnHolder.getBrokerAutoFbdStatus(brokerId);
        builder.setStopWrite(autoFbdTuple.getF0());
        builder.setStopRead(autoFbdTuple.getF1());
        if (retTuple.getF2() == null) {
            builder.setTakeConfInfo(false);
        } else {
            builder.setTakeConfInfo(true);
            builder.setBrokerDefaultConfInfo(retTuple.getF2());
            builder.addAllBrokerTopicSetConfInfo(retTuple.getF3());
            logger.info(sBuffer.append("[TMaster sync] push broker configure: brokerId = ")
                    .append(brokerId).append(",configureId=").append(retTuple.getF0())
                    .append(",stopWrite=").append(builder.getStopWrite())
                    .append(",stopRead=").append(builder.getStopRead())
                    .append(",checksumId=").append(retTuple.getF1())
                    .append(",default configure is ").append(retTuple.getF2())
                    .append(",topic configure is ").append(retTuple.getF3()).toString());
            sBuffer.delete(0, sBuffer.length());
        }
    }

    @Override
    public void setHeatBeatDownConfInfo(int brokerId, StringBuilder sBuffer,
                                        HeartResponseM2B.Builder builder) {
        BrokerRunStatusInfo runStatusInfo =
                brokerRunSyncManageMap.get(brokerId);
        if (runStatusInfo == null) {
            logger.info(sBuffer.append("Get Broker run-info failure, brokerId=")
                    .append(brokerId).append(", please check the implement first!")
                    .toString());
            sBuffer.delete(0, sBuffer.length());
            return;
        }
        Tuple4<Long, Integer, String, List<String>> retTuple =
                runStatusInfo.getNeedSyncData();
        builder.setCurBrokerConfId(retTuple.getF0());
        builder.setConfCheckSumId(retTuple.getF1());
        Tuple2<Boolean, Boolean> autoFbdTuple =
                brokerAbnHolder.getBrokerAutoFbdStatus(brokerId);
        builder.setStopWrite(autoFbdTuple.getF0());
        builder.setStopRead(autoFbdTuple.getF1());
        if (retTuple.getF2() == null) {
            builder.setNeedReportData(false);
            builder.setTakeConfInfo(false);
        } else {
            builder.setNeedReportData(true);
            builder.setTakeConfInfo(true);
            builder.setBrokerDefaultConfInfo(retTuple.getF2());
            builder.addAllBrokerTopicSetConfInfo(retTuple.getF3());
            logger.info(sBuffer.append("[TMaster sync] heartbeat sync config: brokerId = ")
                    .append(brokerId).append(",configureId=").append(retTuple.getF0())
                    .append(",stopWrite=").append(builder.getStopWrite())
                    .append(",stopRead=").append(builder.getStopRead())
                    .append(",checksumId=").append(retTuple.getF1())
                    .append(",default configure is ").append(retTuple.getF2())
                    .append(",topic configure is ").append(retTuple.getF3()).toString());
            sBuffer.delete(0, sBuffer.length());
        }
    }

    @Override
    public BrokerRunStatusInfo getBrokerRunStatusInfo(int brokerId) {
        return this.brokerRunSyncManageMap.get(brokerId);
    }

    @Override
    public BrokerInfo getBrokerInfo(int brokerId) {
        BrokerRunStatusInfo runStatusInfo =
                brokerRunSyncManageMap.get(brokerId);
        if (runStatusInfo == null) {
            return null;
        }
        return runStatusInfo.getBrokerInfo();
    }

    @Override
    public Map<Integer, BrokerInfo> getBrokerInfoMap(List<Integer> brokerIds) {
        Map<Integer, BrokerInfo> brokerInfoMap = new HashMap<>();
        if (brokerIds == null || brokerIds.isEmpty()) {
            for (BrokerRunStatusInfo runStatusInfo : brokerRunSyncManageMap.values()) {
                if (runStatusInfo == null) {
                    continue;
                }
                BrokerInfo brokerInfo = runStatusInfo.getBrokerInfo();
                brokerInfoMap.put(brokerInfo.getBrokerId(), brokerInfo);
            }
        } else {
            for (Integer brokerId : brokerIds) {
                BrokerRunStatusInfo runStatusInfo =
                        brokerRunSyncManageMap.get(brokerId);
                if (runStatusInfo == null) {
                    continue;
                }
                brokerInfoMap.put(brokerId, runStatusInfo.getBrokerInfo());
            }
        }
        return brokerInfoMap;
    }

    @Override
    public boolean releaseBrokerRunInfo(int brokerId, String blockId, boolean isTimeout) {
        StringBuilder sBuffer = new StringBuilder(512);
        BrokerRunStatusInfo runStatusInfo =
                brokerRunSyncManageMap.get(brokerId);
        if (runStatusInfo == null) {
            logger.info(sBuffer.append("[Broker Release] brokerId=").append(brokerId)
                    .append(", isTimeout=").append(isTimeout)
                    .append(", release failure, run-info has deleted before!").toString());
            return false;
        }
        if (!blockId.equals(runStatusInfo.getCreateId())) {
            logger.info(sBuffer.append("[Broker Release] brokerId=").append(brokerId)
                    .append(", isTimeout=").append(isTimeout)
                    .append(", release failure, run-info has been replaced by new register!")
                    .toString());
            return false;
        }
        runStatusInfo = brokerRunSyncManageMap.remove(brokerId);
        if (runStatusInfo == null) {
            return false;
        }
        MasterSrvStatsHolder.decBrokerOnlineCnt(isTimeout);
        brokerTotalCount.decrementAndGet();
        brokerAbnHolder.removeBroker(brokerId);
        brokerPubSubInfo.rmvBrokerAllPushedInfo(brokerId);
        logger.info(sBuffer.append("[Broker Release] brokerId=").append(brokerId)
                .append(", isTimeout=").append(isTimeout)
                .append(", release success!").toString());
        return true;
    }

    @Override
    public boolean updBrokerCsmConfInfo(int brokerId, ManageStatus mngStatus,
                                        Map<String, TopicInfo> topicInfoMap) {
        brokerPubSubInfo.updBrokerMangeStatus(brokerId, mngStatus);
        return brokerPubSubInfo.updBrokerSubTopicConfInfo(brokerId, topicInfoMap);
    }

    @Override
    public void updBrokerPrdConfInfo(int brokerId, ManageStatus mngStatus,
                                     Map<String, TopicInfo> topicInfoMap) {
        brokerPubSubInfo.updBrokerPubTopicConfInfo(brokerId, topicInfoMap);
    }

    @Override
    public Map<String, String> getPubBrokerAcceptPubPartInfo(Set<String> topicSet) {
        return brokerPubSubInfo.getAcceptPubPartInfo(topicSet);
    }

    @Override
    public int getSubTopicMaxBrokerCount(Set<String> topicSet) {
        return brokerPubSubInfo.getTopicMaxSubBrokerCnt(topicSet);
    }

    @Override
    public Map<String, Partition> getSubBrokerAcceptSubParts(Set<String> topicSet) {
        return brokerPubSubInfo.getAcceptSubParts(topicSet);
    }

    @Override
    public List<Partition> getSubBrokerAcceptSubParts(String topic) {
        return brokerPubSubInfo.getAcceptSubParts(topic);
    }

    @Override
    public TopicInfo getPubBrokerTopicInfo(int brokerId, String topic) {
        return brokerPubSubInfo.getBrokerPubPushedTopicInfo(brokerId, topic);
    }

    @Override
    public List<TopicInfo> getPubBrokerPushedTopicInfo(int brokerId) {
        return brokerPubSubInfo.getPubBrokerPushedTopicInfo(brokerId);
    }

}
