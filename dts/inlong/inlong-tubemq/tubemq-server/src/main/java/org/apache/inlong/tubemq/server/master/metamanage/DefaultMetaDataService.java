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

package org.apache.inlong.tubemq.server.master.metamanage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TErrCodeConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.fileconfig.BdbMetaConfig;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStatus;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStsChgType;
import org.apache.inlong.tubemq.server.master.MasterConfig;
import org.apache.inlong.tubemq.server.master.TMaster;
import org.apache.inlong.tubemq.server.master.bdbstore.MasterGroupStatus;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.ConfigObserver;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicPropGroup;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.MetaConfigMapper;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.bdbimpl.BdbMetaConfigMapperImpl;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.impl.zkimpl.ZKMetaConfigMapperImpl;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunManager;
import org.apache.inlong.tubemq.server.master.nodemanage.nodebroker.BrokerRunStatusInfo;
import org.apache.inlong.tubemq.server.master.web.handler.BrokerProcessResult;
import org.apache.inlong.tubemq.server.master.web.handler.GroupProcessResult;
import org.apache.inlong.tubemq.server.master.web.handler.TopicProcessResult;
import org.apache.inlong.tubemq.server.master.web.model.ClusterGroupVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMetaDataService implements MetaDataService {

    private static final Logger logger =
            LoggerFactory.getLogger(DefaultMetaDataService.class);
    private final TMaster tMaster;
    private final MetaConfigMapper metaConfigMapper;
    private final ScheduledExecutorService scheduledExecutorService;
    private final MasterGroupStatus masterGroupStatus = new MasterGroupStatus();

    private volatile boolean isStarted = false;
    private volatile boolean isStopped = false;
    private long serviceStartTime = System.currentTimeMillis();

    public DefaultMetaDataService(TMaster tMaster) {
        this.tMaster = tMaster;
        MasterConfig masterConfig = tMaster.getMasterConfig();
        if (masterConfig.isUseBdbStoreMetaData()) {
            this.metaConfigMapper = new BdbMetaConfigMapperImpl(masterConfig);
            this.scheduledExecutorService =
                    Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {

                        @Override
                        public Thread newThread(Runnable r) {
                            return new Thread(r, "Master Status Check");
                        }
                    });
        } else {
            this.metaConfigMapper = new ZKMetaConfigMapperImpl(masterConfig);
            this.scheduledExecutorService = null;
        }
    }

    @Override
    public void start() throws Exception {
        if (isStarted) {
            return;
        }
        // start meta store service
        this.metaConfigMapper.start();
        if (this.tMaster.getMasterConfig().isUseBdbStoreMetaData()) {
            final BdbMetaConfig bdbMetaConfig = this.tMaster.getMasterConfig().getBdbMetaConfig();
            this.scheduledExecutorService.scheduleAtFixedRate(new Runnable() {

                @Override
                public void run() {
                    try {
                        MasterGroupStatus tmpGroupStatus =
                                metaConfigMapper.getMasterGroupStatus(true);
                        if (tmpGroupStatus == null) {
                            masterGroupStatus.setMasterGroupStatus(false, false, false);
                        } else {
                            masterGroupStatus.setMasterGroupStatus(metaConfigMapper.isMasterNow(),
                                    tmpGroupStatus.isWritable(), tmpGroupStatus.isReadable());
                        }
                    } catch (Throwable e) {
                        logger.error(new StringBuilder(512)
                                .append("BDBGroupStatus Check exception, wait ")
                                .append(bdbMetaConfig.getRepStatusCheckTimeoutMs())
                                .append(" ms to try again.").append(e.getMessage()).toString());
                    }
                }
            }, 0, bdbMetaConfig.getRepStatusCheckTimeoutMs(), TimeUnit.MILLISECONDS);
        }
        // initial running data
        BrokerRunManager brokerRunManager = this.tMaster.getBrokerRunManager();
        brokerRunManager.updBrokerStaticInfo(this.metaConfigMapper.getBrokerConfInfo(null));
        serviceStartTime = System.currentTimeMillis();
        isStarted = true;
        logger.info("DefaultMetaDataService Started");
    }

    @Override
    public void stop() throws Exception {
        if (isStopped) {
            return;
        }
        if (tMaster.getMasterConfig().isUseBdbStoreMetaData()) {
            this.scheduledExecutorService.shutdownNow();
        }
        isStopped = true;
        logger.info("DefaultMetaDataService stopped");
    }

    @Override
    public void regMetaConfigObserver(ConfigObserver eventObserver) {
        metaConfigMapper.regMetaConfigObserver(eventObserver);
    }

    @Override
    public boolean isSelfMaster() {
        return metaConfigMapper.isMasterNow();
    }

    @Override
    public boolean isPrimaryNodeActive() {
        return metaConfigMapper.isPrimaryNodeActive();
    }

    @Override
    public void transferMaster() throws Exception {
        if (metaConfigMapper.isMasterNow()
                && !metaConfigMapper.isPrimaryNodeActive()) {
            metaConfigMapper.transferMaster();
        }
    }

    @Override
    public String getMasterAddress() {
        return metaConfigMapper.getMasterAddress();
    }

    @Override
    public ClusterGroupVO getGroupAddressStrInfo() {
        return metaConfigMapper.getGroupAddressStrInfo();
    }

    // /////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean isConsumeTargetAuthorized(String consumerId, String groupName,
            Set<String> reqTopicSet,
            Map<String, TreeSet<String>> reqTopicCondMap,
            StringBuilder strBuff, ProcessResult result) {
        // check topic set
        if ((reqTopicSet == null) || (reqTopicSet.isEmpty())) {
            result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                    "Request miss necessary subscribed topic data");
            return result.isSuccess();
        }
        if ((reqTopicCondMap != null) && (!reqTopicCondMap.isEmpty())) {
            // check if request topic set is all in the filter topic set
            Set<String> condTopics = reqTopicCondMap.keySet();
            List<String> unSetTopic = new ArrayList<>();
            for (String topic : condTopics) {
                if (!reqTopicSet.contains(topic)) {
                    unSetTopic.add(topic);
                }
            }
            if (!unSetTopic.isEmpty()) {
                result.setFailResult(TErrCodeConstants.BAD_REQUEST,
                        strBuff.append("Filter's Topic not subscribed :")
                                .append(unSetTopic).toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
        }
        // check if group enable consume
        Set<String> disableCsmTopicSet = new HashSet<>();
        Set<String> enableFltCsmTopicSet = new HashSet<>();
        for (String topicItem : reqTopicSet) {
            if (TStringUtils.isBlank(topicItem)) {
                continue;
            }
            TopicCtrlEntity topicEntity = metaConfigMapper.getTopicCtrlByTopicName(topicItem);
            if (topicEntity == null) {
                continue;
            }
            if (topicEntity.isAuthCtrlEnable()) {
                // check if consume group is allowed to consume
                GroupConsumeCtrlEntity ctrlEntity =
                        metaConfigMapper.getConsumeCtrlByGroupAndTopic(groupName, topicItem);
                if (ctrlEntity == null || !ctrlEntity.isEnableConsume()) {
                    disableCsmTopicSet.add(topicItem);
                }
                // check if consume group is required filter consume
                if (ctrlEntity != null && ctrlEntity.isEnableFilterConsume()) {
                    enableFltCsmTopicSet.add(topicItem);
                }
            }
        }
        if (!disableCsmTopicSet.isEmpty()) {
            result.setFailResult(TErrCodeConstants.CONSUME_GROUP_FORBIDDEN,
                    strBuff.append("[unAuthorized Group] ").append(consumerId)
                            .append("'s consumerGroup not authorized by administrator, unAuthorizedTopics: ")
                            .append(disableCsmTopicSet).toString());
            strBuff.delete(0, strBuff.length());
            return result.isSuccess();
        }
        // check if group enable filter consume
        return checkFilterRstrTopics(groupName, consumerId,
                enableFltCsmTopicSet, reqTopicCondMap, strBuff, result);
    }

    /**
     * Check if consume target is authorization or not
     *
     * @param groupName    checked group name
     * @param consumerId   checked consume id
     * @param enableFltCsmTopicSet    target consume topic set
     * @param reqTopicCondMap   consumer request filter items
     * @param strBuff        the print information string buffer
     * @param result         the process result return
     * @return true is authorized, false not
     */
    private boolean checkFilterRstrTopics(String groupName, String consumerId,
            Set<String> enableFltCsmTopicSet,
            Map<String, TreeSet<String>> reqTopicCondMap,
            StringBuilder strBuff, ProcessResult result) {
        if (enableFltCsmTopicSet == null || enableFltCsmTopicSet.isEmpty()) {
            result.setSuccResult("Ok!");
            return result.isSuccess();
        }
        GroupConsumeCtrlEntity ctrlEntity;
        for (String topicName : enableFltCsmTopicSet) {
            ctrlEntity =
                    metaConfigMapper.getConsumeCtrlByGroupAndTopic(groupName, topicName);
            if (ctrlEntity == null || !ctrlEntity.isEnableFilterConsume()) {
                continue;
            }
            String allowedCondStr = ctrlEntity.getFilterCondStr();
            if (allowedCondStr.equals(TServerConstants.BLANK_FILTER_ITEM_STR)) {
                result.setFailResult(TErrCodeConstants.CONSUME_CONTENT_FORBIDDEN,
                        strBuff.append("[Restricted Group] ").append(consumerId)
                                .append(" : ").append(groupName)
                                .append(" not allowed to consume any data of topic ")
                                .append(topicName).toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
            TreeSet<String> condItemSet = reqTopicCondMap.get(topicName);
            if (condItemSet == null || condItemSet.isEmpty()) {
                result.setFailResult(TErrCodeConstants.CONSUME_CONTENT_FORBIDDEN,
                        strBuff.append("[Restricted Group] ").append(consumerId)
                                .append(" : ").append(groupName)
                                .append(" must set the filter conditions of topic ")
                                .append(topicName).toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
            Map<String, List<String>> unAuthorizedCondMap = new HashMap<>();
            for (String item : condItemSet) {
                if (!allowedCondStr.contains(strBuff.append(TokenConstants.ARRAY_SEP)
                        .append(item).append(TokenConstants.ARRAY_SEP).toString())) {
                    List<String> unAuthConds =
                            unAuthorizedCondMap.computeIfAbsent(
                                    topicName, k -> new ArrayList<>());
                    unAuthConds.add(item);
                }
                strBuff.delete(0, strBuff.length());
            }
            if (!unAuthorizedCondMap.isEmpty()) {
                result.setFailResult(TErrCodeConstants.CONSUME_CONTENT_FORBIDDEN,
                        strBuff.append("[Restricted Group] ").append(consumerId)
                                .append(" : unAuthorized filter conditions ")
                                .append(unAuthorizedCondMap).toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
        }
        result.setSuccResult("Ok!");
        return result.isSuccess();
    }

    // ////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean addOrUpdClusterDefSetting(BaseEntity opEntity, int brokerPort,
            int brokerTlsPort, int brokerWebPort,
            int maxMsgSizeMB, int qryPriorityId,
            EnableStatus flowCtrlEnable, int flowRuleCnt,
            String flowCtrlInfo, TopicPropGroup topicProps,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        return metaConfigMapper.addOrUpdClusterDefSetting(opEntity, brokerPort, brokerTlsPort,
                brokerWebPort, maxMsgSizeMB, qryPriorityId, flowCtrlEnable, flowRuleCnt,
                flowCtrlInfo, topicProps, strBuff, result);
    }

    @Override
    public ClusterSettingEntity getClusterDefSetting(boolean isMustConf) {
        return metaConfigMapper.getClusterDefSetting(isMustConf);
    }

    // ////////////////////////////////////////////////////////////////////////////////////

    @Override
    public BrokerProcessResult addOrUpdBrokerConfig(boolean isAddOp, BaseEntity opEntity,
            int brokerId, String brokerIp, int brokerPort,
            int brokerTlsPort, int brokerWebPort,
            int regionId, int groupId, ManageStatus mngStatus,
            TopicPropGroup topicProps, StringBuilder strBuff,
            ProcessResult result) {
        BrokerConfEntity entity =
                new BrokerConfEntity(opEntity, brokerId, brokerIp);
        entity.updModifyInfo(opEntity.getDataVerId(), brokerPort,
                brokerTlsPort, brokerWebPort, regionId, groupId, mngStatus, topicProps);
        return addOrUpdBrokerConfig(isAddOp, entity, strBuff, result);
    }

    @Override
    public BrokerProcessResult addOrUpdBrokerConfig(boolean isAddOp, BrokerConfEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new BrokerProcessResult(entity.getBrokerId(), entity.getBrokerIp(), result);
        }
        // add or update broker meta-data
        if (!metaConfigMapper.addOrUpdBrokerConfig(isAddOp, entity, strBuff, result)) {
            return new BrokerProcessResult(entity.getBrokerId(), entity.getBrokerIp(), result);
        }
        BrokerConfEntity curEntity =
                metaConfigMapper.getBrokerConfByBrokerId(entity.getBrokerId());
        // update broker static information
        this.tMaster.getBrokerRunManager().updBrokerStaticInfo(curEntity);
        if (isAddOp) {
            // add system topic if absent
            metaConfigMapper.addSystemTopicDeploy(curEntity.getBrokerId(),
                    curEntity.getBrokerPort(), curEntity.getBrokerIp(), strBuff);
        }
        // auto trigger configure sync
        triggerBrokerConfDataSync(curEntity.getBrokerId(), strBuff, result);
        // return result
        return new BrokerProcessResult(curEntity.getBrokerId(), curEntity.getBrokerIp(), result);
    }

    @Override
    public BrokerProcessResult changeBrokerConfStatus(BaseEntity opEntity,
            int brokerId, ManageStatus newMngStatus,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new BrokerProcessResult(brokerId, "", result);
        }
        // change broker manage status
        if (metaConfigMapper.changeBrokerConfStatus(opEntity,
                brokerId, newMngStatus, strBuff, result)) {
            // auto trigger configure sync
            triggerBrokerConfDataSync(brokerId, strBuff, result);
        }
        return new BrokerProcessResult(brokerId, "", result);
    }

    @Override
    public BrokerProcessResult reloadBrokerConfInfo(BaseEntity opEntity, int brokerId,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new BrokerProcessResult(brokerId, "", result);
        }
        // query the operated object
        BrokerConfEntity curEntry = metaConfigMapper.getBrokerConfByBrokerId(brokerId);
        if (curEntry == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    "The broker configure not exist!");
            return new BrokerProcessResult(brokerId, "", result);
        }
        // query the target manage status
        if (!curEntry.getManageStatus().isOnlineStatus()) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    strBuff.append("The broker manage status by brokerId=").append(brokerId)
                            .append(" is not in online status, can't reload this configure! ")
                            .toString());
            strBuff.delete(0, strBuff.length());
            return new BrokerProcessResult(brokerId, "", result);
        }
        // trigger broker sync
        triggerBrokerConfDataSync(curEntry.getBrokerId(), strBuff, result);
        return new BrokerProcessResult(brokerId, curEntry.getBrokerIp(), result);
    }

    @Override
    public BrokerProcessResult delBrokerConfInfo(String operator, boolean rsvData,
            int brokerId, StringBuilder strBuff,
            ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new BrokerProcessResult(brokerId, "", result);
        }
        // query the operated object
        BrokerConfEntity curEntry = metaConfigMapper.getBrokerConfByBrokerId(brokerId);
        if (curEntry == null) {
            result.setFullInfo(true,
                    DataOpErrCode.DERR_SUCCESS.getCode(),
                    DataOpErrCode.DERR_SUCCESS.getDescription());
            return new BrokerProcessResult(brokerId, "", result);
        }
        // check broker's manage status
        if (curEntry.getManageStatus().isOnlineStatus()) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    "Broker manage status is online, please offline first!");
            return new BrokerProcessResult(brokerId, curEntry.getBrokerIp(), result);
        }
        // check broker node running status
        BrokerRunManager brokerRunManager = tMaster.getBrokerRunManager();
        BrokerRunStatusInfo runStatusInfo =
                brokerRunManager.getBrokerRunStatusInfo(brokerId);
        if (runStatusInfo != null
                && curEntry.getManageStatus() == ManageStatus.STATUS_MANAGE_OFFLINE
                && runStatusInfo.inProcessingStatus()) {
            result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                    strBuff.append("Illegal value: the broker is processing offline event by brokerId=")
                            .append(brokerId).append(", please offline first and try later!").toString());
            strBuff.delete(0, strBuff.length());
            return new BrokerProcessResult(brokerId, curEntry.getBrokerIp(), result);
        }
        // delete broker configure
        if (metaConfigMapper.delBrokerConfInfo(operator, brokerId, rsvData, strBuff, result)) {
            // delete broker static information
            brokerRunManager.delBrokerStaticInfo(brokerId);
            if (runStatusInfo != null) {
                // release broker run information
                brokerRunManager.releaseBrokerRunInfo(brokerId, runStatusInfo.getCreateId(), false);
            }
        }
        return new BrokerProcessResult(brokerId, curEntry.getBrokerIp(), result);
    }

    @Override
    public Map<Integer, BrokerConfEntity> getBrokerConfInfo(BrokerConfEntity qryEntity) {
        return metaConfigMapper.getBrokerConfInfo(qryEntity);
    }

    @Override
    public Map<Integer, BrokerConfEntity> getBrokerConfInfo(Set<Integer> brokerIdSet,
            Set<String> brokerIpSet,
            BrokerConfEntity qryEntity) {
        return metaConfigMapper.getBrokerConfInfo(brokerIdSet, brokerIpSet, qryEntity);
    }

    /**
     * Reload broker config info
     *
     * @param brokerId   the broker id
     * @param strBuff    the string buffer
     * @param result     the process return result
     * @return true if success otherwise false
     */
    private boolean triggerBrokerConfDataSync(int brokerId,
            StringBuilder strBuff,
            ProcessResult result) {
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        BrokerRunManager brokerRunManager = this.tMaster.getBrokerRunManager();
        BrokerRunStatusInfo runStatusInfo =
                brokerRunManager.getBrokerRunStatusInfo(brokerId);
        if (runStatusInfo == null) {
            result.setSuccResult(null);
            return result.isSuccess();
        }
        runStatusInfo.notifyDataChanged();
        logger.info(strBuff.append("[Meta data] trigger broker syncStatus info, brokerId=")
                .append(brokerId).toString());
        strBuff.delete(0, strBuff.length());
        result.setSuccResult(null);
        return result.isSuccess();
    }

    /**
     * Reload topic's deploy config info
     *
     * @param topicNameSet  the topic name set
     * @param strBuff       the string buffer
     * @param result        the process return result
     * @return true if success otherwise false
     */
    private boolean triggerBrokerConfDataSync(Set<String> topicNameSet,
            StringBuilder strBuff,
            ProcessResult result) {
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        Set<Integer> brokerIdSet =
                metaConfigMapper.getDeployedBrokerIdByTopic(topicNameSet);
        if (brokerIdSet.isEmpty()) {
            result.setSuccResult();
            return result.isSuccess();
        }
        BrokerRunStatusInfo runStatusInfo;
        BrokerRunManager brokerRunManager = this.tMaster.getBrokerRunManager();
        for (Integer brokerId : brokerIdSet) {
            if (brokerId == null) {
                continue;
            }
            runStatusInfo = brokerRunManager.getBrokerRunStatusInfo(brokerId);
            if (runStatusInfo == null) {
                continue;
            }
            runStatusInfo.notifyDataChanged();
        }
        logger.info(strBuff.append("[Meta data] trigger broker syncStatus info for")
                .append(" maxMsgSize modify, brokerId set is ").append(brokerIdSet)
                .toString());
        strBuff.delete(0, strBuff.length());
        result.setSuccResult(null);
        return result.isSuccess();
    }

    // ///////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean delCleanedTopicDeployInfo(int brokerId, List<String> removedTopics,
            StringBuilder strBuff, ProcessResult result) {
        result.setSuccResult(null);
        if (removedTopics == null || removedTopics.isEmpty()) {
            return result.isSuccess();
        }
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        // delete deploy configure topic by topic
        Map<String, TopicDeployEntity> topicDeployMap = metaConfigMapper.getConfiguredTopicInfo(brokerId);
        if (topicDeployMap == null || topicDeployMap.isEmpty()) {
            return result.isSuccess();
        }
        boolean needSync2Broker = false;
        for (String topicName : removedTopics) {
            TopicDeployEntity topicEntity = topicDeployMap.get(topicName);
            if (topicEntity != null
                    && topicEntity.getTopicStatus() == TopicStatus.STATUS_TOPIC_SOFT_REMOVE) {
                if (metaConfigMapper.delTopicDeployInfo("system-self",
                        brokerId, topicName, strBuff, result)) {
                    needSync2Broker = true;
                }
            }
        }
        // sync change to broker node
        if (needSync2Broker) {
            return triggerBrokerConfDataSync(brokerId, strBuff, result);
        }
        result.setSuccResult(null);
        return result.isSuccess();
    }

    @Override
    public Map<Integer, Set<String>> getBrokerTopicConfigInfo(Set<Integer> brokerIdSet) {
        return metaConfigMapper.getConfiguredTopicInfo(brokerIdSet);
    }

    @Override
    public Map<String, Map<Integer, String>> getTopicBrokerConfigInfo(Set<String> topicNameSet) {
        return metaConfigMapper.getTopicBrokerInfo(topicNameSet);
    }

    @Override
    public Set<String> getDeployedTopicSet() {
        return metaConfigMapper.getDeployedTopicSet();
    }

    @Override
    public BrokerConfEntity getBrokerConfByBrokerId(int brokerId) {
        return metaConfigMapper.getBrokerConfByBrokerId(brokerId);
    }

    @Override
    public BrokerConfEntity getBrokerConfByBrokerIp(String brokerIp) {
        return metaConfigMapper.getBrokerConfByBrokerIp(brokerIp);
    }

    @Override
    public Map<String, TopicDeployEntity> getBrokerTopicConfEntitySet(int brokerId) {
        return metaConfigMapper.getConfiguredTopicInfo(brokerId);
    }

    // ///////////////////////////////////////////////////////////////////////////////

    @Override
    public TopicProcessResult addOrUpdTopicDeployInfo(boolean isAddOp, BaseEntity opEntity,
            int brokerId, String topicName,
            TopicStatus deployStatus,
            TopicPropGroup topicPropInfo,
            StringBuilder strBuff, ProcessResult result) {
        TopicDeployEntity entity =
                new TopicDeployEntity(opEntity, brokerId, topicName);
        entity.updModifyInfo(opEntity.getDataVerId(),
                TBaseConstants.META_VALUE_UNDEFINED, TBaseConstants.META_VALUE_UNDEFINED,
                null, deployStatus, topicPropInfo);
        return addOrUpdTopicDeployInfo(isAddOp, entity, strBuff, result);
    }

    @Override
    public TopicProcessResult addOrUpdTopicDeployInfo(boolean isAddOp, TopicDeployEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new TopicProcessResult(entity.getBrokerId(), entity.getTopicName(), result);
        }
        // check broker configure exist
        BrokerConfEntity brokerConf =
                getBrokerConfByBrokerId(entity.getBrokerId());
        if (brokerConf == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    strBuff.append("Not found broker configure record by brokerId=")
                            .append(entity.getBrokerId())
                            .append(", please create the broker's configure first!").toString());
            strBuff.delete(0, strBuff.length());
            return new TopicProcessResult(entity.getBrokerId(), entity.getTopicName(), result);
        }
        // add or update topic deploy information
        if (metaConfigMapper.addOrUpdTopicDeployInfo(isAddOp, entity, strBuff, result)) {
            // auto trigger configure sync
            triggerBrokerConfDataSync(entity.getBrokerId(), strBuff, result);
        }
        // return result
        return new TopicProcessResult(entity.getBrokerId(), entity.getTopicName(), result);
    }

    @Override
    public TopicProcessResult updTopicDeployStatusInfo(BaseEntity opEntity, int brokerId,
            String topicName, TopicStsChgType chgType,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new TopicProcessResult(brokerId, topicName, result);
        }
        // check broker configure exist
        BrokerConfEntity brokerConf = getBrokerConfByBrokerId(brokerId);
        if (brokerConf == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    strBuff.append("Not found broker configure record by brokerId=")
                            .append(brokerId)
                            .append(", please create the broker's configure first!").toString());
            strBuff.delete(0, strBuff.length());
            return new TopicProcessResult(brokerId, topicName, result);
        }
        // get topic deploy configure record
        TopicDeployEntity curEntity = metaConfigMapper.getConfiguredTopicInfo(brokerId, topicName);
        if (curEntity == null) {
            result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                    strBuff.append("Not found the topic ").append(topicName)
                            .append("'s deploy configure in broker=").append(brokerId)
                            .append(", please confirm the configure first!").toString());
            strBuff.delete(0, strBuff.length());
            return new TopicProcessResult(brokerId, topicName, result);
        }
        // map topic status
        TopicStatus topicStatus;
        if (chgType == TopicStsChgType.STATUS_CHANGE_SOFT_DELETE) {
            topicStatus = TopicStatus.STATUS_TOPIC_SOFT_DELETE;
        } else if (chgType == TopicStsChgType.STATUS_CHANGE_REMOVE) {
            topicStatus = TopicStatus.STATUS_TOPIC_SOFT_REMOVE;
        } else {
            topicStatus = TopicStatus.STATUS_TOPIC_OK;
        }
        // add or update topic deploy information
        if (metaConfigMapper.updTopicDeployStatusInfo(opEntity,
                brokerId, topicName, topicStatus, strBuff, result)) {
            // trigger data sync
            triggerBrokerConfDataSync(brokerId, strBuff, result);
        }
        return new TopicProcessResult(brokerId, topicName, result);
    }

    @Override
    public Map<String, List<TopicDeployEntity>> getTopicDeployInfoMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet,
            TopicDeployEntity qryEntity) {
        return metaConfigMapper.getTopicDeployInfoMap(topicNameSet, brokerIdSet, qryEntity);
    }

    @Override
    public Map<Integer, List<TopicDeployEntity>> getTopicDeployInfoMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet) {
        Map<Integer, BrokerConfEntity> qryBrokerInfoMap =
                metaConfigMapper.getBrokerConfInfo(brokerIdSet, null, null);
        if (qryBrokerInfoMap.isEmpty()) {
            return Collections.emptyMap();
        }
        return metaConfigMapper.getTopicDeployInfoMap(topicNameSet, qryBrokerInfoMap.keySet());
    }

    @Override
    public Map<String, List<TopicDeployEntity>> getTopicConfMapByTopicAndBrokerIds(
            Set<String> topicNameSet, Set<Integer> brokerIdSet) {
        return metaConfigMapper.getTopicConfInfoByTopicAndBrokerIds(topicNameSet, brokerIdSet);
    }

    @Override
    public Map<String, String> getBrokerTopicStrConfigInfo(
            BrokerConfEntity brokerConfEntity, StringBuilder strBuff) {
        return inGetTopicConfStrInfo(brokerConfEntity, false, strBuff);
    }

    @Override
    public Map<String, String> getBrokerRemovedTopicStrConfigInfo(
            BrokerConfEntity brokerConfEntity, StringBuilder strBuff) {
        return inGetTopicConfStrInfo(brokerConfEntity, true, strBuff);
    }

    private Map<String, String> inGetTopicConfStrInfo(BrokerConfEntity brokerEntity,
            boolean isRemoved, StringBuilder strBuff) {
        Map<String, String> topicConfStrMap = new HashMap<>();
        Map<String, TopicDeployEntity> topicEntityMap =
                metaConfigMapper.getConfiguredTopicInfo(brokerEntity.getBrokerId());
        if (topicEntityMap.isEmpty()) {
            return topicConfStrMap;
        }
        TopicPropGroup defTopicProps = brokerEntity.getTopicProps();
        ClusterSettingEntity clusterDefConf = metaConfigMapper.getClusterDefSetting(false);
        int defMsgSizeInB = clusterDefConf.getMaxMsgSizeInB();
        for (TopicDeployEntity topicEntity : topicEntityMap.values()) {
            /*
             * topic:partNum:acceptPublish:acceptSubscribe:unflushThreshold:unflushInterval:deleteWhen:
             * deletePolicy:filterStatusId:statusId
             */
            if ((isRemoved && !topicEntity.isInRemoving())
                    || (!isRemoved && topicEntity.isInRemoving())) {
                continue;
            }
            strBuff.append(topicEntity.getTopicName());
            TopicPropGroup topicProps = topicEntity.getTopicProps();
            if (topicProps.getNumPartitions() == TBaseConstants.META_VALUE_UNDEFINED
                    || topicProps.getNumPartitions() == defTopicProps.getNumPartitions()) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(topicProps.getNumPartitions());
            }
            if (topicProps.getAcceptPublish() == null
                    || topicProps.isAcceptPublish() == defTopicProps.isAcceptPublish()) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(topicProps.isAcceptPublish());
            }
            if (topicProps.getAcceptSubscribe() == null
                    || topicProps.isAcceptSubscribe() == defTopicProps.isAcceptSubscribe()) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(topicProps.isAcceptSubscribe());
            }
            if (topicProps.getUnflushThreshold() == TBaseConstants.META_VALUE_UNDEFINED
                    || topicProps.getUnflushThreshold() == defTopicProps.getUnflushThreshold()) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(topicProps.getUnflushThreshold());
            }
            if (topicProps.getUnflushInterval() == TBaseConstants.META_VALUE_UNDEFINED
                    || topicProps.getUnflushInterval() == defTopicProps.getUnflushInterval()) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(topicProps.getUnflushInterval());
            }
            strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            if (TStringUtils.isEmpty(topicProps.getDeletePolicy())
                    || topicProps.getDeletePolicy().equals(defTopicProps.getDeletePolicy())) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(topicProps.getDeletePolicy());
            }
            if (topicProps.getNumTopicStores() == TBaseConstants.META_VALUE_UNDEFINED
                    || topicProps.getNumTopicStores() == defTopicProps.getNumTopicStores()) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(topicProps.getNumTopicStores());
            }
            strBuff.append(TokenConstants.ATTR_SEP).append(topicEntity.getTopicStatusId());
            if (topicProps.getUnflushDataHold() == TBaseConstants.META_VALUE_UNDEFINED
                    || topicProps.getUnflushDataHold() == defTopicProps.getUnflushDataHold()) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(topicProps.getUnflushDataHold());
            }
            if (topicProps.getMemCacheMsgSizeInMB() == TBaseConstants.META_VALUE_UNDEFINED
                    || topicProps.getMemCacheMsgSizeInMB() == defTopicProps.getMemCacheMsgSizeInMB()) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(topicProps.getMemCacheMsgSizeInMB());
            }
            if (topicProps.getMemCacheMsgCntInK() == TBaseConstants.META_VALUE_UNDEFINED
                    || topicProps.getMemCacheMsgCntInK() == defTopicProps.getMemCacheMsgCntInK()) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(topicProps.getMemCacheMsgCntInK());
            }
            if (topicProps.getMemCacheFlushIntvl() == TBaseConstants.META_VALUE_UNDEFINED
                    || topicProps.getMemCacheFlushIntvl() == defTopicProps.getMemCacheFlushIntvl()) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(topicProps.getMemCacheFlushIntvl());
            }
            int maxMsgSize = defMsgSizeInB;
            TopicCtrlEntity topicCtrlEntity =
                    metaConfigMapper.getTopicCtrlByTopicName(topicEntity.getTopicName());
            if (topicCtrlEntity != null
                    && topicCtrlEntity.getMaxMsgSizeInB() != TBaseConstants.META_VALUE_UNDEFINED) {
                maxMsgSize = topicCtrlEntity.getMaxMsgSizeInB();
            }
            if (maxMsgSize == defMsgSizeInB) {
                strBuff.append(TokenConstants.ATTR_SEP).append(" ");
            } else {
                strBuff.append(TokenConstants.ATTR_SEP).append(maxMsgSize);
            }
            topicConfStrMap.put(topicEntity.getTopicName(), strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return topicConfStrMap;
    }

    @Override
    public List<TopicProcessResult> addOrUpdTopicCtrlConf(boolean isAddOp, BaseEntity opEntity,
            Set<String> topicNameSet, int topicNameId,
            EnableStatus enableTopicAuth, int maxMsgSizeInMB,
            StringBuilder strBuff, ProcessResult result) {
        TopicCtrlEntity entity;
        Map<String, TopicCtrlEntity> topicCtrlEntityMap = new HashMap<>();
        for (String topicName : topicNameSet) {
            entity = new TopicCtrlEntity(opEntity, topicName);
            entity.updModifyInfo(opEntity.getDataVerId(),
                    topicNameId, maxMsgSizeInMB, enableTopicAuth);
            topicCtrlEntityMap.put(topicName, entity);
        }
        return addOrUpdTopicCtrlConf(isAddOp, topicCtrlEntityMap, strBuff, result);
    }

    @Override
    public List<TopicProcessResult> addOrUpdTopicCtrlConf(boolean isAddOp,
            Map<String, TopicCtrlEntity> entityMap,
            StringBuilder strBuff, ProcessResult result) {
        List<TopicProcessResult> retInfo = new ArrayList<>();
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            for (String topicName : entityMap.keySet()) {
                retInfo.add(new TopicProcessResult(0, topicName, result));
            }
            return retInfo;
        }
        if (isAddOp) {
            for (TopicCtrlEntity entity : entityMap.values()) {
                metaConfigMapper.addOrUpdTopicCtrlConf(isAddOp, entity, strBuff, result);
                retInfo.add(new TopicProcessResult(0, entity.getTopicName(), result));
            }
        } else {
            TopicCtrlEntity curEntity;
            Set<String> changedTopicSet = new HashSet<>();
            for (TopicCtrlEntity entity : entityMap.values()) {
                curEntity = metaConfigMapper.getTopicCtrlByTopicName(entity.getTopicName());
                if (curEntity != null) {
                    if (curEntity.getMaxMsgSizeInB() != entity.getMaxMsgSizeInB()) {
                        changedTopicSet.add(entity.getTopicName());
                    }
                }
                metaConfigMapper.addOrUpdTopicCtrlConf(isAddOp, entity, strBuff, result);
                retInfo.add(new TopicProcessResult(0, entity.getTopicName(), result));
            }
            if (!changedTopicSet.isEmpty()) {
                triggerBrokerConfDataSync(changedTopicSet, strBuff, result);
            }
        }
        return retInfo;
    }

    @Override
    public TopicProcessResult insertTopicCtrlConf(BaseEntity opEntity, String topicName,
            EnableStatus enableTopicAuth, StringBuilder strBuff,
            ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new TopicProcessResult(0, topicName, result);
        }
        metaConfigMapper.insertTopicCtrlConf(opEntity, topicName, enableTopicAuth, strBuff, result);
        return new TopicProcessResult(0, topicName, result);
    }

    @Override
    public TopicProcessResult insertTopicCtrlConf(TopicCtrlEntity entity,
            StringBuilder strBuff,
            ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new TopicProcessResult(0, entity.getTopicName(), result);
        }
        metaConfigMapper.insertTopicCtrlConf(entity, strBuff, result);
        return new TopicProcessResult(0, entity.getTopicName(), result);
    }

    @Override
    public boolean delTopicCtrlConf(String operator, String topicName,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        return metaConfigMapper.delTopicCtrlConf(operator, topicName, strBuff, result);
    }

    @Override
    public TopicCtrlEntity getTopicCtrlByTopicName(String topicName) {
        return this.metaConfigMapper.getTopicCtrlByTopicName(topicName);
    }

    @Override
    public int getTopicMaxMsgSizeInMB(String topicName) {
        // get maxMsgSizeInMB info
        ClusterSettingEntity clusterSettingEntity = metaConfigMapper.getClusterDefSetting(false);
        int maxMsgSizeInMB = clusterSettingEntity.getMaxMsgSizeInMB();
        TopicCtrlEntity topicCtrlEntity = metaConfigMapper.getTopicCtrlByTopicName(topicName);
        if (topicCtrlEntity != null
                && topicCtrlEntity.getMaxMsgSizeInMB() != TBaseConstants.META_VALUE_UNDEFINED) {
            maxMsgSizeInMB = topicCtrlEntity.getMaxMsgSizeInMB();
        }
        return maxMsgSizeInMB;
    }

    @Override
    public Map<String, TopicCtrlEntity> getTopicCtrlConf(Set<String> topicNameSet,
            TopicCtrlEntity qryEntity) {
        return metaConfigMapper.getTopicCtrlConf(topicNameSet, qryEntity);
    }

    @Override
    public Map<String, Integer> getMaxMsgSizeInBByTopics(int defMaxMsgSizeInB,
            Set<String> topicNameSet) {
        return metaConfigMapper.getMaxMsgSizeInBByTopics(defMaxMsgSizeInB, topicNameSet);
    }
    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public GroupProcessResult addOrUpdGroupCtrlConf(boolean isAddOp, BaseEntity opEntity,
            String groupName, EnableStatus resCheckEnable,
            int allowedBClientRate, int qryPriorityId,
            EnableStatus flowCtrlEnable, int flowRuleCnt,
            String flowCtrlInfo, StringBuilder strBuff,
            ProcessResult result) {
        GroupResCtrlEntity entity =
                new GroupResCtrlEntity(opEntity, groupName);
        entity.updModifyInfo(opEntity.getDataVerId(), resCheckEnable, allowedBClientRate,
                qryPriorityId, flowCtrlEnable, flowRuleCnt, flowCtrlInfo);
        return addOrUpdGroupCtrlConf(isAddOp, entity, strBuff, result);
    }

    @Override
    public GroupProcessResult addOrUpdGroupCtrlConf(boolean isAddOp,
            GroupResCtrlEntity entity,
            StringBuilder strBuff,
            ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new GroupProcessResult(entity.getGroupName(), null, result);
        }
        metaConfigMapper.addOrUpdGroupResCtrlConf(isAddOp, entity, strBuff, result);
        return new GroupProcessResult(entity.getGroupName(), null, result);
    }

    @Override
    public GroupProcessResult insertGroupCtrlConf(BaseEntity opEntity, String groupName,
            int qryPriorityId, EnableStatus flowCtrlEnable,
            int flowRuleCnt, String flowCtrlRuleInfo,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new GroupProcessResult(groupName, null, result);
        }
        metaConfigMapper.insertGroupCtrlConf(opEntity, groupName,
                qryPriorityId, flowCtrlEnable, flowRuleCnt, flowCtrlRuleInfo, strBuff, result);
        return new GroupProcessResult(groupName, null, result);
    }

    @Override
    public GroupProcessResult insertGroupCtrlConf(BaseEntity opEntity, String groupName,
            EnableStatus resChkEnable, int allowedB2CRate,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new GroupProcessResult(groupName, null, result);
        }
        metaConfigMapper.insertGroupCtrlConf(opEntity, groupName,
                resChkEnable, allowedB2CRate, strBuff, result);
        return new GroupProcessResult(groupName, null, result);
    }

    @Override
    public GroupProcessResult insertGroupCtrlConf(GroupResCtrlEntity entity,
            StringBuilder strBuff,
            ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new GroupProcessResult(entity.getGroupName(), null, result);
        }
        metaConfigMapper.insertGroupCtrlConf(entity, strBuff, result);
        return new GroupProcessResult(entity.getGroupName(), null, result);
    }

    @Override
    public GroupProcessResult delGroupResCtrlConf(String operator, String groupName,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new GroupProcessResult(groupName, null, result);
        }
        // judge whether exists the record
        GroupResCtrlEntity curEntity = metaConfigMapper.getGroupCtrlConf(groupName);
        if (curEntity == null) {
            result.setSuccResult();
            return new GroupProcessResult(groupName, null, result);
        }
        // judge whether the group in use
        if (metaConfigMapper.isGroupInUse(groupName)) {
            result.setFailResult(DataOpErrCode.DERR_CONDITION_LACK.getCode(),
                    strBuff.append("Group ").append(groupName)
                            .append(" has consume control configures,")
                            .append(", please delete consume control configures first!")
                            .toString());
            strBuff.delete(0, strBuff.length());
            return new GroupProcessResult(groupName, null, result);
        }
        // delete record
        metaConfigMapper.delGroupCtrlConf(operator, groupName, strBuff, result);
        return new GroupProcessResult(groupName, null, result);
    }

    @Override
    public Map<String, GroupResCtrlEntity> getGroupCtrlConf(Set<String> groupSet,
            GroupResCtrlEntity qryEntity) {
        return metaConfigMapper.getGroupCtrlConf(groupSet, qryEntity);
    }

    @Override
    public GroupResCtrlEntity getGroupCtrlConf(String groupName) {
        return metaConfigMapper.getGroupCtrlConf(groupName);
    }

    @Override
    public GroupProcessResult addOrUpdConsumeCtrlInfo(boolean isAddOp, BaseEntity opEntity,
            String groupName, String topicName,
            EnableStatus enableCsm, String disableRsn,
            EnableStatus enableFlt, String fltCondStr,
            StringBuilder strBuff, ProcessResult result) {
        GroupConsumeCtrlEntity entity =
                new GroupConsumeCtrlEntity(opEntity, groupName, topicName);
        entity.updModifyInfo(opEntity.getDataVerId(),
                enableCsm, disableRsn, enableFlt, fltCondStr);
        return addOrUpdConsumeCtrlInfo(isAddOp, entity, strBuff, result);
    }

    @Override
    public GroupProcessResult addOrUpdConsumeCtrlInfo(boolean isAddOp, GroupConsumeCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new GroupProcessResult(entity.getGroupName(), entity.getTopicName(), result);
        }
        metaConfigMapper.addOrUpdConsumeCtrlInfo(isAddOp, entity, strBuff, result);
        return new GroupProcessResult(entity.getGroupName(), entity.getTopicName(), result);
    }

    @Override
    public GroupProcessResult insertConsumeCtrlInfo(BaseEntity opEntity, String groupName,
            String topicName, EnableStatus enableCsm,
            String disReason, EnableStatus enableFlt,
            String fltCondStr, StringBuilder strBuff,
            ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new GroupProcessResult(groupName, topicName, result);
        }
        metaConfigMapper.insertConsumeCtrlInfo(opEntity, groupName,
                topicName, enableCsm, disReason, enableFlt, fltCondStr, strBuff, result);
        return new GroupProcessResult(groupName, topicName, result);
    }

    @Override
    public GroupProcessResult insertConsumeCtrlInfo(GroupConsumeCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return new GroupProcessResult(entity.getGroupName(), entity.getTopicName(), result);
        }
        metaConfigMapper.insertConsumeCtrlInfo(entity, strBuff, result);
        return new GroupProcessResult(entity.getGroupName(), entity.getTopicName(), result);
    }

    @Override
    public boolean delConsumeCtrlConf(String operator,
            String groupName, String topicName,
            StringBuilder strBuff, ProcessResult result) {
        // check current status
        if (!metaConfigMapper.checkStoreStatus(true, result)) {
            return result.isSuccess();
        }
        GroupConsumeCtrlEntity curEntity =
                metaConfigMapper.getConsumeCtrlByGroupAndTopic(groupName, topicName);
        if (curEntity == null) {
            result.setSuccResult(null);
            return result.isSuccess();
        }
        metaConfigMapper.delConsumeCtrlConf(operator, groupName, topicName, strBuff, result);
        return result.isSuccess();
    }

    @Override
    public GroupConsumeCtrlEntity getConsumeCtrlByGroupAndTopic(String groupName,
            String topicName) {
        return metaConfigMapper.getConsumeCtrlByGroupAndTopic(groupName, topicName);
    }

    @Override
    public List<GroupConsumeCtrlEntity> getConsumeCtrlByTopic(String topicName) {
        return metaConfigMapper.getConsumeCtrlByTopic(topicName);
    }

    @Override
    public Map<String, List<GroupConsumeCtrlEntity>> getConsumeCtrlByTopic(Set<String> topicSet) {
        return metaConfigMapper.getConsumeCtrlByTopic(topicSet);
    }

    @Override
    public Set<String> getDisableTopicByGroupName(String groupName) {
        return metaConfigMapper.getDisableTopicByGroupName(groupName);
    }

    @Override
    public List<GroupConsumeCtrlEntity> getConsumeCtrlByGroupName(String groupName) {
        return metaConfigMapper.getConsumeCtrlByGroupName(groupName);
    }

    @Override
    public Map<String, List<GroupConsumeCtrlEntity>> getConsumeCtrlByGroupName(
            Set<String> groupSet) {
        return metaConfigMapper.getConsumeCtrlByGroupName(groupSet);
    }

    @Override
    public Map<String, List<GroupConsumeCtrlEntity>> getGroupConsumeCtrlConf(
            Set<String> groupSet, Set<String> topicSet, GroupConsumeCtrlEntity qryEntry) {
        return metaConfigMapper.getGroupConsumeCtrlConf(groupSet, topicSet, qryEntry);
    }
}
