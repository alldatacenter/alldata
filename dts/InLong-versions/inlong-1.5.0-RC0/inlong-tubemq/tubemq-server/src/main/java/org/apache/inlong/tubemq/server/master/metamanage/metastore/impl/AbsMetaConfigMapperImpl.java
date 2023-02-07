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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.rv.ProcessResult;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.fielddef.WebFieldDef;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStatus;
import org.apache.inlong.tubemq.server.common.utils.RowLock;
import org.apache.inlong.tubemq.server.master.MasterConfig;
import org.apache.inlong.tubemq.server.master.metamanage.DataOpErrCode;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.ConfigObserver;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.MetaConfigMapper;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BaseEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.BrokerConfEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.ClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupConsumeCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.GroupResCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicCtrlEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicDeployEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity.TopicPropGroup;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.BrokerConfigMapper;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.ClusterConfigMapper;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.ConsumeCtrlMapper;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.GroupResCtrlMapper;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.TopicCtrlMapper;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.mapper.TopicDeployMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbsMetaConfigMapperImpl implements MetaConfigMapper {

    protected static final Logger logger =
            LoggerFactory.getLogger(AbsMetaConfigMapperImpl.class);
    // master configure
    protected final MasterConfig masterConfig;
    // 0 stopped, 1 starting, 2 started, 3 stopping
    protected final AtomicInteger srvStatus = new AtomicInteger(0);
    // master role flag
    protected volatile boolean isMaster = false;
    // time since node become active
    protected final AtomicLong masterSinceTime = new AtomicLong(Long.MAX_VALUE);
    // row lock.
    private final RowLock metaRowLock;
    // default cluster setting
    private static final ClusterSettingEntity defClusterSetting =
            new ClusterSettingEntity().fillDefaultValue();
    // cluster default setting
    protected ClusterConfigMapper clusterConfigMapper;
    // broker configure
    protected BrokerConfigMapper brokerConfigMapper;
    // topic deployment configure
    protected TopicDeployMapper topicDeployMapper;
    // topic control configure
    protected TopicCtrlMapper topicCtrlMapper;
    // group resource control configure
    protected GroupResCtrlMapper groupResCtrlMapper;
    // group consume control configure
    protected ConsumeCtrlMapper consumeCtrlMapper;
    // the observers focusing on active-standby switching
    private final List<ConfigObserver> eventObservers = new ArrayList<>();

    public AbsMetaConfigMapperImpl(MasterConfig masterConfig) {
        this.masterConfig = masterConfig;
        this.metaRowLock =
                new RowLock("MetaData-RowLock", masterConfig.getRowLockWaitDurMs());
    }

    @Override
    public void regMetaConfigObserver(ConfigObserver eventObserver) {
        if (eventObserver != null) {
            eventObservers.add(eventObserver);
        }
    }

    @Override
    public boolean checkStoreStatus(boolean checkIsMaster, ProcessResult result) {
        if (!isServiceStarted()) {
            result.setFailResult(DataOpErrCode.DERR_STORE_STOPPED.getCode(),
                    "Meta store service stopped!");
            return result.isSuccess();
        }
        if (checkIsMaster && !isMasterNow()) {
            result.setFailResult(DataOpErrCode.DERR_STORE_NOT_MASTER.getCode(),
                    "Current node not active, please send your request to the active Node!");
            return result.isSuccess();
        }
        result.setSuccResult(null);
        return true;
    }

    @Override
    public boolean addOrUpdClusterDefSetting(BaseEntity opEntity,
            int brokerPort, int brokerTlsPort,
            int brokerWebPort, int maxMsgSizeMB,
            int qryPriorityId, EnableStatus flowCtrlEnable,
            int flowRuleCnt, String flowCtrlInfo,
            TopicPropGroup topicProps,
            StringBuilder strBuff, ProcessResult result) {
        Integer lid = null;
        boolean isAddOp = false;
        String printPrefix = "[updClusterConfig], ";
        ClusterSettingEntity curEntity;
        ClusterSettingEntity newEntity;
        try {
            // lock clusterConfig meta-lock
            lid = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8("clusterConfig#"), true);
            // build add or update data
            curEntity = clusterConfigMapper.getClusterConfig();
            if (curEntity == null) {
                isAddOp = true;
                printPrefix = "[addClusterConfig], ";
                newEntity = new ClusterSettingEntity(opEntity);
                newEntity.fillDefaultValue();
                newEntity.updModifyInfo(opEntity.getDataVerId(), brokerPort,
                        brokerTlsPort, brokerWebPort, maxMsgSizeMB, qryPriorityId,
                        flowCtrlEnable, flowRuleCnt, flowCtrlInfo, topicProps);
            } else {
                newEntity = curEntity.clone();
                newEntity.updBaseModifyInfo(opEntity);
                if (!newEntity.updModifyInfo(opEntity.getDataVerId(), brokerPort,
                        brokerTlsPort, brokerWebPort, maxMsgSizeMB, qryPriorityId,
                        flowCtrlEnable, flowRuleCnt, flowCtrlInfo, topicProps)) {
                    result.setFailResult(DataOpErrCode.DERR_UNCHANGED.getCode(),
                            "Cluster configure not changed!");
                    return result.isSuccess();
                }
            }
            // add or update data to storage
            clusterConfigMapper.addUpdClusterConfig(newEntity, strBuff, result);
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (lid != null) {
                metaRowLock.releaseRowLock(lid);
            }
        }
        if (result.isSuccess()) {
            // print operation result
            if (isAddOp) {
                strBuff.append(printPrefix).append(newEntity.getCreateUser())
                        .append(" added cluster configure: ").append(newEntity);
            } else {
                strBuff.append(printPrefix).append(newEntity.getModifyUser())
                        .append(" updated cluster configure: from ").append(curEntity)
                        .append(" to ").append(newEntity);
            }
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public ClusterSettingEntity getClusterDefSetting(boolean isMustConf) {
        ClusterSettingEntity curClsSetting =
                clusterConfigMapper.getClusterConfig();
        if (!isMustConf && curClsSetting == null) {
            curClsSetting = defClusterSetting;
        }
        return curClsSetting;
    }

    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean addOrUpdBrokerConfig(boolean isAddOp, BrokerConfEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        Integer lid = null;
        BrokerConfEntity curEntity = null;
        BrokerConfEntity newEntity;
        String printPrefix = "[addBrokerConf], ";
        // execute add or update operation
        try {
            // lock brokerId meta-lock
            lid = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(String.valueOf(entity.getBrokerId())), true);
            if (isAddOp) {
                newEntity = new BrokerConfEntity(entity, entity.getBrokerId(),
                        entity.getBrokerIp(), getClusterDefSetting(false));
                newEntity.updModifyInfo(entity.getDataVerId(), entity.getBrokerPort(),
                        entity.getBrokerTLSPort(), entity.getBrokerWebPort(),
                        entity.getRegionId(), entity.getGroupId(), entity.getManageStatus(),
                        entity.getTopicProps());
                brokerConfigMapper.addBrokerConf(newEntity, strBuff, result);
            } else {
                printPrefix = "[updBrokerConf], ";
                curEntity = brokerConfigMapper.getBrokerConfByBrokerId(entity.getBrokerId());
                brokerConfigMapper.updBrokerConf(entity, strBuff, result);
            }
            newEntity = brokerConfigMapper.getBrokerConfByBrokerId(entity.getBrokerId());
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (lid != null) {
                metaRowLock.releaseRowLock(lid);
            }
        }
        // print log to file
        if (result.isSuccess()) {
            if (isAddOp) {
                strBuff.append(printPrefix).append(entity.getCreateUser())
                        .append(" added broker configure: ").append(newEntity);
            } else {
                strBuff.append(printPrefix).append(entity.getModifyUser())
                        .append(" updated broker configure from ").append(curEntity)
                        .append(" to ").append(newEntity);
            }
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public boolean changeBrokerConfStatus(BaseEntity opEntity,
            int brokerId, ManageStatus newMngStatus,
            StringBuilder strBuff, ProcessResult result) {
        Integer lid = null;
        BrokerConfEntity curEntity;
        BrokerConfEntity newEntity;
        String printPrefix = "[updBrokerConf], ";
        // execute update operation
        try {
            // lock brokerId meta-lock
            lid = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(String.valueOf(brokerId)), true);
            curEntity = brokerConfigMapper.getBrokerConfByBrokerId(brokerId);
            brokerConfigMapper.updBrokerMngStatus(opEntity,
                    brokerId, newMngStatus, strBuff, result);
            newEntity = brokerConfigMapper.getBrokerConfByBrokerId(brokerId);
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (lid != null) {
                metaRowLock.releaseRowLock(lid);
            }
        }
        // print log to file
        if (result.isSuccess()) {
            strBuff.append(printPrefix).append(opEntity.getModifyUser())
                    .append(" updated broker configure from ").append(curEntity)
                    .append(" to ").append(newEntity);
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public boolean delBrokerConfInfo(String operator, int brokerId, boolean rsvData,
            StringBuilder strBuff, ProcessResult result) {
        Integer lid = null;
        BrokerConfEntity curEntity;
        String printPrefix = "[delBrokerConf], ";
        // execute delete operation
        try {
            // lock brokerId meta-lock
            lid = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(String.valueOf(brokerId)), true);
            // get current broker configure
            curEntity = brokerConfigMapper.getBrokerConfByBrokerId(brokerId);
            if (curEntity == null) {
                result.setSuccResult(null);
                return result.isSuccess();
            }
            // check broker's manage status
            if (curEntity.getManageStatus().isOnlineStatus()) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        strBuff.append("Illegal manage status, please offline the broker(")
                                .append(WebFieldDef.BROKERID.name).append("=")
                                .append(curEntity.getBrokerId()).append(") first!").toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
            // check topic deploy status
            if (!topicDeployMapper.getConfiguredTopicInfo(curEntity.getBrokerId()).isEmpty()) {
                if (rsvData) {
                    if (!topicDeployMapper.delTopicConfByBrokerId(brokerId, strBuff, result)) {
                        return result.isSuccess();
                    }
                    strBuff.append("[delTopicDeployByBrokerId], ").append(operator)
                            .append(" deleted topic deploy configure: ").append(brokerId);
                    logger.info(strBuff.toString());
                    strBuff.delete(0, strBuff.length());
                } else {
                    result.setFailResult(DataOpErrCode.DERR_UNCLEANED.getCode(),
                            strBuff.append("Illegal operate conditions, the broker(")
                                    .append(curEntity.getBrokerId())
                                    .append(")'s topic deploy configure uncleaned, please delete them first!")
                                    .toString());
                    strBuff.delete(0, strBuff.length());
                    return result.isSuccess();
                }
            }
            // execute delete operation
            brokerConfigMapper.delBrokerConf(brokerId, strBuff, result);
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (lid != null) {
                metaRowLock.releaseRowLock(lid);
            }
        }
        // print log to file
        if (result.isSuccess()) {
            strBuff.append(printPrefix).append(operator)
                    .append(" deleted broker configure: ").append(curEntity);
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public Map<Integer, BrokerConfEntity> getBrokerConfInfo(BrokerConfEntity qryEntity) {
        return brokerConfigMapper.getBrokerConfInfo(qryEntity);
    }

    @Override
    public Map<Integer, BrokerConfEntity> getBrokerConfInfo(Set<Integer> brokerIdSet,
            Set<String> brokerIpSet,
            BrokerConfEntity qryEntity) {
        return brokerConfigMapper.getBrokerConfInfo(brokerIdSet, brokerIpSet, qryEntity);
    }

    @Override
    public BrokerConfEntity getBrokerConfByBrokerId(int brokerId) {
        return brokerConfigMapper.getBrokerConfByBrokerId(brokerId);
    }

    @Override
    public BrokerConfEntity getBrokerConfByBrokerIp(String brokerIp) {
        return brokerConfigMapper.getBrokerConfByBrokerIp(brokerIp);
    }

    // ///////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean addOrUpdTopicCtrlConf(boolean isAddOp, TopicCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        return innAddOrUpdTopicCtrlConf(true, isAddOp, entity, strBuff, result);
    }

    @Override
    public boolean insertTopicCtrlConf(BaseEntity opEntity,
            String topicName, EnableStatus enableTopicAuth,
            StringBuilder strBuff, ProcessResult result) {
        TopicCtrlEntity entity =
                new TopicCtrlEntity(opEntity, topicName);
        entity.updModifyInfo(opEntity.getDataVerId(),
                TBaseConstants.META_VALUE_UNDEFINED,
                TBaseConstants.META_VALUE_UNDEFINED, enableTopicAuth);
        return innAddOrUpdTopicCtrlConf(false, false, entity, strBuff, result);
    }

    @Override
    public boolean insertTopicCtrlConf(TopicCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        return innAddOrUpdTopicCtrlConf(false, false, entity, strBuff, result);
    }

    @Override
    public boolean delTopicCtrlConf(String operator, String topicName,
            StringBuilder strBuff, ProcessResult result) {
        Integer lid = null;
        TopicCtrlEntity curEntity;
        String printPrefix = "[delTopicCtrlConf], ";
        // execute delete operation
        try {
            // lock brokerId meta-lock
            lid = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(topicName), true);
            curEntity = topicCtrlMapper.getTopicCtrlConf(topicName);
            if (curEntity == null) {
                result.setSuccResult(null);
                return result.isSuccess();
            }
            // check topic use status
            if (topicDeployMapper.isTopicDeployed(topicName)) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        strBuff.append("TopicName ").append(topicName)
                                .append(" is in use, please delete deploy configure first!")
                                .toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
            if (consumeCtrlMapper.isTopicNameInUse(topicName)) {
                result.setFailResult(DataOpErrCode.DERR_ILLEGAL_STATUS.getCode(),
                        strBuff.append("TopicName ").append(topicName)
                                .append(" is in use, please delete the consume control first!")
                                .toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
            topicCtrlMapper.delTopicCtrlConf(topicName, strBuff, result);
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (lid != null) {
                metaRowLock.releaseRowLock(lid);
            }
        }
        // print operation log
        if (result.isSuccess()) {
            strBuff.append(printPrefix).append(operator)
                    .append(" deleted topic control configure: ")
                    .append(curEntity);
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public TopicCtrlEntity getTopicCtrlByTopicName(String topicName) {
        return this.topicCtrlMapper.getTopicCtrlConf(topicName);
    }

    @Override
    public int getTopicMaxMsgSizeInMB(String topicName) {
        // get maxMsgSizeInMB info
        ClusterSettingEntity clusterSettingEntity = getClusterDefSetting(false);
        int maxMsgSizeInMB = clusterSettingEntity.getMaxMsgSizeInMB();
        TopicCtrlEntity topicCtrlEntity = topicCtrlMapper.getTopicCtrlConf(topicName);
        if (topicCtrlEntity != null
                && topicCtrlEntity.getMaxMsgSizeInMB() != TBaseConstants.META_VALUE_UNDEFINED) {
            maxMsgSizeInMB = topicCtrlEntity.getMaxMsgSizeInMB();
        }
        return maxMsgSizeInMB;
    }

    @Override
    public List<TopicCtrlEntity> queryTopicCtrlConf(TopicCtrlEntity qryEntity) {
        return topicCtrlMapper.getTopicCtrlConf(qryEntity);
    }

    @Override
    public Map<String, TopicCtrlEntity> getTopicCtrlConf(Set<String> topicNameSet,
            TopicCtrlEntity qryEntity) {
        return topicCtrlMapper.getTopicCtrlConf(topicNameSet, qryEntity);
    }

    @Override
    public Map<String, Integer> getMaxMsgSizeInBByTopics(int defMaxMsgSizeInB,
            Set<String> topicNameSet) {
        return topicCtrlMapper.getMaxMsgSizeInBByTopics(defMaxMsgSizeInB, topicNameSet);
    }

    /**
     * Add if absent topic control configure info
     *
     * @param opEntity  the operation info
     * @param topicName the topic name will be added
     * @param strBuff   the print info string buffer
     * @param result    the process result return
     * @return true if success otherwise false
     */
    private boolean addTopicCtrlConfIfAbsent(BaseEntity opEntity, String topicName,
            StringBuilder strBuff, ProcessResult result) {
        TopicCtrlEntity entity = new TopicCtrlEntity(opEntity, topicName,
                TBaseConstants.META_VALUE_UNDEFINED, TBaseConstants.META_VALUE_UNDEFINED);
        return innAddOrUpdTopicCtrlConf(false, true, entity, strBuff, result);
    }

    /**
     * Add or Update topic control configure info
     *
     * @param chkConsistent     whether order operation condition
     * @param isAddOpOrOnlyAdd  the operation type,
     * @param entity            the entity need to operation
     * @param strBuff           the string buffer
     * @param result            the process result return
     * @return true if success otherwise false
     */
    private boolean innAddOrUpdTopicCtrlConf(boolean chkConsistent, boolean isAddOpOrOnlyAdd,
            TopicCtrlEntity entity, StringBuilder strBuff,
            ProcessResult result) {
        Integer lid = null;
        TopicCtrlEntity curEntity;
        TopicCtrlEntity newEntity;
        boolean addRecord = true;
        String printPrefix = "[addTopicCtrlConf], ";
        // execute add or update operation
        try {
            // lock brokerId meta-lock
            lid = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(entity.getTopicName()), true);
            // get current record and judge execute condition
            curEntity = topicCtrlMapper.getTopicCtrlConf(entity.getTopicName());
            if (curEntity == null) {
                if (chkConsistent && !isAddOpOrOnlyAdd) {
                    result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                            strBuff.append("Not found topic control configure for topicName(")
                                    .append(entity.getTopicName()).append(")!").toString());
                    strBuff.delete(0, strBuff.length());
                    return result.isSuccess();
                }
                entity.fillEmptyValues(getClusterDefSetting(false));
                topicCtrlMapper.addTopicCtrlConf(entity, strBuff, result);
            } else {
                if (isAddOpOrOnlyAdd) {
                    if (chkConsistent) {
                        result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                                strBuff.append("Existed record found for topicName(")
                                        .append(entity.getTopicName()).append(")!").toString());
                        strBuff.delete(0, strBuff.length());
                    } else {
                        result.setSuccResult(null);
                    }
                    return result.isSuccess();
                }
                addRecord = false;
                printPrefix = "[udpTopicCtrlConf], ";
                topicCtrlMapper.updTopicCtrlConf(entity, strBuff, result);
            }
            newEntity = topicCtrlMapper.getTopicCtrlConf(entity.getTopicName());
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (lid != null) {
                metaRowLock.releaseRowLock(lid);
            }
        }
        // print log to file
        if (result.isSuccess()) {
            if (addRecord) {
                strBuff.append(printPrefix).append(entity.getCreateUser())
                        .append(" added topic control configure: ").append(newEntity);
            } else {
                strBuff.append(printPrefix).append(entity.getModifyUser())
                        .append(" updated topic control configure from ")
                        .append(curEntity).append(" to ").append(newEntity);
            }
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    // ///////////////////////////////////////////////////////////////////////////////

    @Override
    public void addSystemTopicDeploy(int brokerId, int brokerPort,
            String brokerIp, StringBuilder strBuff) {
        BaseEntity opEntity = new BaseEntity("systemSelf", new Date());
        TopicPropGroup topicPropInfo = new TopicPropGroup();
        topicPropInfo.setNumTopicStores(TServerConstants.OFFSET_HISTORY_NUMSTORES);
        topicPropInfo.setNumPartitions(TServerConstants.OFFSET_HISTORY_NUMPARTS);
        TopicDeployEntity entity =
                new TopicDeployEntity(opEntity, brokerId, TServerConstants.OFFSET_HISTORY_NAME);
        entity.updModifyInfo(opEntity.getDataVerId(), TBaseConstants.META_VALUE_UNDEFINED,
                brokerPort, brokerIp, TopicStatus.STATUS_TOPIC_OK, topicPropInfo);
        addOrUpdTopicDeployInfo(true, entity, strBuff, new ProcessResult());
    }

    @Override
    public boolean addOrUpdTopicDeployInfo(boolean isAddOp, TopicDeployEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        TopicDeployEntity curEntity;
        TopicDeployEntity newEntity;
        String printPrefix = "[addTopicDeployConf], ";
        Integer topicLockId = null;
        Integer brokerLockId = null;
        // add topic control configure
        BaseEntity opEntity = new BaseEntity("systemSelf", new Date());
        addTopicCtrlConfIfAbsent(opEntity, entity.getTopicName(), strBuff, result);
        // execute add or update operation
        try {
            // lock topicName meta-lock
            topicLockId = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(entity.getTopicName()), true);
            try {
                // lock brokerId meta-lock
                brokerLockId = metaRowLock.getLock(null,
                        StringUtils.getBytesUtf8(String.valueOf(entity.getBrokerId())), true);
                // check broker configure exist
                BrokerConfEntity brokerEntity =
                        brokerConfigMapper.getBrokerConfByBrokerId(entity.getBrokerId());
                if (brokerEntity == null) {
                    result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                            strBuff.append("Not found broker configure by brokerId=")
                                    .append(entity.getBrokerId())
                                    .append(", please create the broker's configure first!").toString());
                    strBuff.delete(0, strBuff.length());
                    return result.isSuccess();
                }
                // check topic deploy configure
                curEntity = topicDeployMapper.getTopicConfByeRecKey(entity.getRecordKey());
                if (isAddOp) {
                    if (curEntity != null) {
                        if (curEntity.isValidTopicStatus()) {
                            result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                                    strBuff.append("Existed record found for brokerId-topicName(")
                                            .append(curEntity.getRecordKey()).append(")!").toString());
                        } else {
                            result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                                    strBuff.append("Softly deleted record found for brokerId-topicName(")
                                            .append(curEntity.getRecordKey())
                                            .append("), please resume or remove it first!").toString());
                        }
                        strBuff.delete(0, strBuff.length());
                        return result.isSuccess();
                    }
                    // add record
                    TopicPropGroup newProps =
                            getClusterDefSetting(false).getClsDefTopicProps().clone();
                    newProps.updModifyInfo(brokerEntity.getTopicProps());
                    newEntity = new TopicDeployEntity(entity,
                            entity.getBrokerId(), entity.getTopicName(), newProps);
                    newEntity.updModifyInfo(entity.getDataVerId(), entity.getTopicId(),
                            brokerEntity.getBrokerPort(), brokerEntity.getBrokerIp(),
                            entity.getTopicStatus(), entity.getTopicProps());
                    topicDeployMapper.addTopicDeployConf(newEntity, strBuff, result);
                } else {
                    printPrefix = "[updTopicDeployConf], ";
                    if (curEntity == null) {
                        result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                                strBuff.append("Not found topic deploy configure for brokerId-topicName(")
                                        .append(entity.getRecordKey()).append(")!").toString());
                        strBuff.delete(0, strBuff.length());
                        return result.isSuccess();
                    }
                    // update record
                    topicDeployMapper.updTopicDeployConf(entity, strBuff, result);
                }
                newEntity = topicDeployMapper.getTopicConfByeRecKey(entity.getRecordKey());
            } finally {
                if (brokerLockId != null) {
                    metaRowLock.releaseRowLock(brokerLockId);
                }
            }
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (topicLockId != null) {
                metaRowLock.releaseRowLock(topicLockId);
            }
        }
        // print log to file
        if (result.isSuccess()) {
            if (isAddOp) {
                strBuff.append(printPrefix).append(entity.getCreateUser())
                        .append(" added topic deploy configure: ").append(newEntity);
            } else {
                strBuff.append(printPrefix).append(entity.getModifyUser())
                        .append(" updated topic deploy configure from ")
                        .append(curEntity).append(" to ").append(newEntity);
            }
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public boolean updTopicDeployStatusInfo(BaseEntity opEntity, int brokerId,
            String topicName, TopicStatus topicStatus,
            StringBuilder strBuff, ProcessResult result) {
        TopicDeployEntity curEntity;
        TopicDeployEntity newEntity;
        String printPrefix = "[updTopicDeployConf], ";
        Integer topicLockId = null;
        Integer brokerLockId = null;
        // execute add or update operation
        try {
            // lock topicName meta-lock
            topicLockId = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(topicName), true);
            try {
                // lock brokerId meta-lock
                brokerLockId = metaRowLock.getLock(null,
                        StringUtils.getBytesUtf8(String.valueOf(brokerId)), true);
                // check broker configure exist
                BrokerConfEntity brokerEntity =
                        brokerConfigMapper.getBrokerConfByBrokerId(brokerId);
                if (brokerEntity == null) {
                    result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                            strBuff.append("Not found broker configure by brokerId=")
                                    .append(brokerId)
                                    .append(", please create the broker's configure first!").toString());
                    strBuff.delete(0, strBuff.length());
                    return result.isSuccess();
                }
                // check topic deploy configure
                curEntity = topicDeployMapper.getTopicConf(brokerId, topicName);
                if (curEntity == null) {
                    result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                            strBuff.append("Not found topic deploy configure for brokerId-topicName(")
                                    .append(brokerId).append("-").append(topicName)
                                    .append(")!").toString());
                    strBuff.delete(0, strBuff.length());
                    return result.isSuccess();
                }
                if (curEntity.getTopicStatus() == topicStatus) {
                    result.setSuccResult(null);
                    return result.isSuccess();
                }
                // update record
                topicDeployMapper.updTopicDeployStatus(opEntity,
                        brokerId, topicName, topicStatus, strBuff, result);
                newEntity = topicDeployMapper.getTopicConfByeRecKey(curEntity.getRecordKey());
            } finally {
                if (brokerLockId != null) {
                    metaRowLock.releaseRowLock(brokerLockId);
                }
            }
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (topicLockId != null) {
                metaRowLock.releaseRowLock(topicLockId);
            }
        }
        // print log to file
        if (result.isSuccess()) {
            strBuff.append(printPrefix).append(opEntity.getModifyUser())
                    .append(" updated topic deploy configure from ")
                    .append(curEntity).append(" to ").append(newEntity);
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public boolean delTopicDeployInfo(String operator, int brokerId, String topicName,
            StringBuilder strBuff, ProcessResult result) {
        TopicDeployEntity curEntity;
        String printPrefix = "[delTopicDeployConf], ";
        Integer topicLockId = null;
        Integer brokerLockId = null;
        // execute add or update operation
        try {
            // lock topicName meta-lock
            topicLockId = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(topicName), true);
            try {
                // lock brokerId meta-lock
                brokerLockId = metaRowLock.getLock(null,
                        StringUtils.getBytesUtf8(String.valueOf(brokerId)), true);
                // check broker configure exist
                BrokerConfEntity brokerEntity = brokerConfigMapper.getBrokerConfByBrokerId(brokerId);
                if (brokerEntity == null) {
                    result.setSuccResult(null);
                    return result.isSuccess();
                }
                // check topic deploy configure
                curEntity = topicDeployMapper.getTopicConf(brokerId, topicName);
                if (curEntity == null) {
                    result.setSuccResult(null);
                    return result.isSuccess();
                }
                // delete record
                topicDeployMapper.delTopicDeployConf(curEntity.getRecordKey(), strBuff, result);
            } finally {
                if (brokerLockId != null) {
                    metaRowLock.releaseRowLock(brokerLockId);
                }
            }
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (topicLockId != null) {
                metaRowLock.releaseRowLock(topicLockId);
            }
        }
        // print log to file
        if (result.isSuccess()) {
            logger.info(strBuff.append(printPrefix).append(operator)
                    .append(" deleted topic deploy configure: ")
                    .append(curEntity).toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public Map<String, List<TopicDeployEntity>> getTopicDeployInfoMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet,
            TopicDeployEntity qryEntity) {
        return topicDeployMapper.getTopicConfMap(topicNameSet, brokerIdSet, qryEntity);
    }

    @Override
    public Map<Integer, List<TopicDeployEntity>> getTopicDeployInfoMap(Set<String> topicNameSet,
            Set<Integer> brokerIdSet) {
        Map<Integer, BrokerConfEntity> qryBrokerInfoMap =
                brokerConfigMapper.getBrokerConfInfo(brokerIdSet, null, null);
        if (qryBrokerInfoMap.isEmpty()) {
            return Collections.emptyMap();
        }
        return topicDeployMapper.getTopicDeployInfoMap(topicNameSet, qryBrokerInfoMap.keySet());
    }

    @Override
    public Map<String, List<TopicDeployEntity>> getTopicConfInfoByTopicAndBrokerIds(
            Set<String> topicNameSet, Set<Integer> brokerIdSet) {
        return topicDeployMapper.getTopicConfMapByTopicAndBrokerIds(topicNameSet, brokerIdSet);
    }

    @Override
    public Map<String, TopicDeployEntity> getConfiguredTopicInfo(int brokerId) {
        return topicDeployMapper.getConfiguredTopicInfo(brokerId);
    }

    @Override
    public TopicDeployEntity getConfiguredTopicInfo(int brokerId, String topicName) {
        return topicDeployMapper.getTopicConf(brokerId, topicName);
    }

    @Override
    public Map<Integer/* brokerId */, Set<String>> getConfiguredTopicInfo(Set<Integer> brokerIdSet) {
        return topicDeployMapper.getConfiguredTopicInfo(brokerIdSet);
    }

    @Override
    public Map<String, Map<Integer, String>> getTopicBrokerInfo(Set<String> topicNameSet) {
        return topicDeployMapper.getTopicBrokerInfo(topicNameSet);
    }

    @Override
    public Set<Integer> getDeployedBrokerIdByTopic(Set<String> topicNameSet) {
        return topicDeployMapper.getDeployedBrokerIdByTopic(topicNameSet);
    }

    @Override
    public Set<String> getDeployedTopicSet() {
        return topicDeployMapper.getDeployedTopicSet();
    }

    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean addOrUpdGroupResCtrlConf(boolean isAddOp, GroupResCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        return addOrUpdGroupCtrlConf(true,
                isAddOp, entity, strBuff, result);
    }

    @Override
    public boolean insertGroupCtrlConf(BaseEntity opEntity, String groupName,
            int qryPriorityId, EnableStatus flowCtrlEnable,
            int flowRuleCnt, String flowCtrlRuleInfo,
            StringBuilder strBuff, ProcessResult result) {
        GroupResCtrlEntity newEntity = new GroupResCtrlEntity(opEntity, groupName);
        newEntity.updModifyInfo(opEntity.getDataVerId(), null,
                TBaseConstants.META_VALUE_UNDEFINED, qryPriorityId,
                flowCtrlEnable, flowRuleCnt, flowCtrlRuleInfo);
        return addOrUpdGroupCtrlConf(false,
                false, newEntity, strBuff, result);
    }

    @Override
    public boolean insertGroupCtrlConf(BaseEntity opEntity, String groupName,
            EnableStatus resChkEnable, int allowedB2CRate,
            StringBuilder strBuff, ProcessResult result) {
        GroupResCtrlEntity newEntity = new GroupResCtrlEntity(opEntity, groupName);
        newEntity.updModifyInfo(opEntity.getDataVerId(), resChkEnable, allowedB2CRate,
                TBaseConstants.META_VALUE_UNDEFINED, null,
                TBaseConstants.META_VALUE_UNDEFINED, null);
        return addOrUpdGroupCtrlConf(false,
                false, newEntity, strBuff, result);
    }

    @Override
    public boolean insertGroupCtrlConf(GroupResCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        return addOrUpdGroupCtrlConf(false, false, entity, strBuff, result);
    }

    /**
     * Add if absent group control configure info
     *
     * @param opEntity  the operation info
     * @param groupName the group name will be add
     * @param strBuff   the print info string buffer
     * @param result    the process result return
     * @return true if success otherwise false
     */
    private boolean addGroupCtrlConfIfAbsent(BaseEntity opEntity, String groupName,
            StringBuilder strBuff, ProcessResult result) {
        GroupResCtrlEntity resCtrlEntity =
                groupResCtrlMapper.getGroupResCtrlConf(groupName);
        if (resCtrlEntity != null) {
            result.setSuccResult(null);
            return true;
        }
        resCtrlEntity = new GroupResCtrlEntity(opEntity, groupName);
        resCtrlEntity.fillDefaultValue();
        return addOrUpdGroupCtrlConf(false, true, resCtrlEntity, strBuff, result);
    }

    /**
     * Add or Update group control configure info
     *
     * @param chkConsistent     whether order operation condition
     * @param isAddOpOrOnlyAdd  the operation type,
     * @param entity            the entity need to operation
     * @param strBuff           the string buffer
     * @param result            the process result return
     * @return true if success otherwise false
     */
    private boolean addOrUpdGroupCtrlConf(boolean chkConsistent, boolean isAddOpOrOnlyAdd,
            GroupResCtrlEntity entity, StringBuilder strBuff,
            ProcessResult result) {
        Integer lid = null;
        boolean addRecord = true;
        GroupResCtrlEntity curEntity;
        GroupResCtrlEntity newEntity;
        String printPrefix = "[addGroupCtrlConf], ";
        // execute add or update operation
        try {
            // lock group name meta-lock
            lid = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(entity.getGroupName()), true);
            // get current record and judge execute condition
            curEntity = groupResCtrlMapper.getGroupResCtrlConf(entity.getGroupName());
            if (curEntity == null) {
                if (chkConsistent && !isAddOpOrOnlyAdd) {
                    result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                            strBuff.append("Not found group control configure for groupName(")
                                    .append(entity.getGroupName()).append(")!").toString());
                    strBuff.delete(0, strBuff.length());
                    return result.isSuccess();
                }
                entity.fillEmptyValues();
                groupResCtrlMapper.addGroupResCtrlConf(entity, strBuff, result);
            } else {
                if (isAddOpOrOnlyAdd) {
                    if (chkConsistent) {
                        result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                                strBuff.append("Existed record found for groupName(")
                                        .append(entity.getGroupName()).append(")!").toString());
                        strBuff.delete(0, strBuff.length());
                    } else {
                        result.setSuccResult(null);
                    }
                    return result.isSuccess();
                }
                addRecord = false;
                printPrefix = "[updGroupCtrlConf], ";
                groupResCtrlMapper.updGroupResCtrlConf(entity, strBuff, result);
            }
            newEntity = groupResCtrlMapper.getGroupResCtrlConf(entity.getGroupName());
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (lid != null) {
                metaRowLock.releaseRowLock(lid);
            }
        }
        // print log to file
        if (result.isSuccess()) {
            if (addRecord) {
                strBuff.append(printPrefix).append(entity.getCreateUser())
                        .append(" added group control configure: ").append(newEntity);
            } else {
                strBuff.append(printPrefix).append(entity.getModifyUser())
                        .append(" updated group control configure from ")
                        .append(curEntity).append(" to ").append(newEntity);
            }
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public boolean delGroupCtrlConf(String operator, String groupName,
            StringBuilder strBuff, ProcessResult result) {
        Integer lid = null;
        GroupResCtrlEntity curEntity;
        String printPrefix = "[delGroupCtrlConf], ";
        // execute add or update operation
        try {
            // lock group name meta-lock
            lid = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(groupName), true);
            // get current record and judge execute condition
            curEntity = groupResCtrlMapper.getGroupResCtrlConf(groupName);
            if (curEntity == null) {
                result.setFailResult(null);
                return result.isSuccess();
            }
            if (consumeCtrlMapper.isGroupNameInUse(groupName)) {
                result.setFailResult(DataOpErrCode.DERR_CONDITION_LACK.getCode(),
                        strBuff.append("Group ").append(groupName)
                                .append(" has consume control configures,")
                                .append(", please delete consume control configures first!")
                                .toString());
                strBuff.delete(0, strBuff.length());
                return result.isSuccess();
            }
            groupResCtrlMapper.delGroupResCtrlConf(groupName, strBuff, result);
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (lid != null) {
                metaRowLock.releaseRowLock(lid);
            }
        }
        // print log to file
        if (result.isSuccess()) {
            strBuff.append(printPrefix).append(operator)
                    .append(" deleted group control configure: ").append(curEntity);
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public Map<String, GroupResCtrlEntity> getGroupCtrlConf(Set<String> groupSet,
            GroupResCtrlEntity qryEntity) {
        return groupResCtrlMapper.getGroupResCtrlConf(groupSet, qryEntity);
    }

    @Override
    public GroupResCtrlEntity getGroupCtrlConf(String groupName) {
        return groupResCtrlMapper.getGroupResCtrlConf(groupName);
    }

    // //////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean addOrUpdConsumeCtrlInfo(boolean isAddOp, GroupConsumeCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        return addOrUpdConsumeCtrlConf(true, isAddOp, entity, strBuff, result);
    }

    @Override
    public boolean insertConsumeCtrlInfo(BaseEntity opEntity, String groupName,
            String topicName, EnableStatus enableCsm,
            String disReason, EnableStatus enableFlt,
            String fltCondStr, StringBuilder strBuff,
            ProcessResult result) {
        GroupConsumeCtrlEntity entity =
                new GroupConsumeCtrlEntity(opEntity, groupName, topicName);
        entity.updModifyInfo(opEntity.getDataVerId(),
                enableCsm, disReason, enableFlt, fltCondStr);
        return addOrUpdConsumeCtrlConf(false, false, entity, strBuff, result);
    }

    @Override
    public boolean insertConsumeCtrlInfo(GroupConsumeCtrlEntity entity,
            StringBuilder strBuff, ProcessResult result) {
        return addOrUpdConsumeCtrlConf(false, false, entity, strBuff, result);
    }

    /**
     * Add or Update consume control configure info
     *
     * @param chkConsistent     whether order operation condition
     * @param isAddOpOrOnlyAdd  the operation type,
     * @param entity            the entity need to operation
     * @param strBuff           the string buffer
     * @param result            the process result return
     * @return true if success otherwise false
     */
    private boolean addOrUpdConsumeCtrlConf(boolean chkConsistent, boolean isAddOpOrOnlyAdd,
            GroupConsumeCtrlEntity entity, StringBuilder strBuff,
            ProcessResult result) {
        boolean addRecord = true;
        Integer topicLockId = null;
        Integer groupLockId = null;
        GroupConsumeCtrlEntity curEntity;
        GroupConsumeCtrlEntity newEntity;
        String printPrefix = "[addConsumeCtrlConf], ";
        // append topic control configure
        BaseEntity opEntity = new BaseEntity("systemSelf", new Date());
        addTopicCtrlConfIfAbsent(opEntity, entity.getTopicName(), strBuff, result);
        // append group control configure
        addGroupCtrlConfIfAbsent(opEntity, entity.getGroupName(), strBuff, result);
        // execute add or update operation
        try {
            // lock topicName meta-lock
            topicLockId = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(entity.getTopicName()), true);
            try {
                // lock groupName meta-lock
                groupLockId = metaRowLock.getLock(null,
                        StringUtils.getBytesUtf8(entity.getGroupName()), true);
                // check consume control configure exist
                curEntity = consumeCtrlMapper.getGroupConsumeCtrlConfByRecKey(entity.getRecordKey());
                if (curEntity == null) {
                    if (chkConsistent && !isAddOpOrOnlyAdd) {
                        result.setFailResult(DataOpErrCode.DERR_NOT_EXIST.getCode(),
                                strBuff.append("Not found consume control for groupName-topicName(")
                                        .append(entity.getRecordKey()).append(")!").toString());
                        strBuff.delete(0, strBuff.length());
                        return result.isSuccess();
                    }
                    entity.fillEmptyValues();
                    consumeCtrlMapper.addGroupConsumeCtrlConf(entity, strBuff, result);
                } else {
                    if (isAddOpOrOnlyAdd) {
                        if (chkConsistent) {
                            result.setFailResult(DataOpErrCode.DERR_EXISTED.getCode(),
                                    strBuff.append("Existed record found for groupName-topicName(")
                                            .append(entity.getRecordKey()).append(")!").toString());
                            strBuff.delete(0, strBuff.length());
                        } else {
                            result.setSuccResult(null);
                        }
                        return result.isSuccess();
                    }
                    addRecord = false;
                    printPrefix = "[updConsumeCtrlConf], ";
                    consumeCtrlMapper.updGroupConsumeCtrlConf(entity, strBuff, result);
                }
                newEntity = consumeCtrlMapper.getGroupConsumeCtrlConfByRecKey(entity.getRecordKey());
            } finally {
                if (groupLockId != null) {
                    metaRowLock.releaseRowLock(groupLockId);
                }
            }
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (topicLockId != null) {
                metaRowLock.releaseRowLock(topicLockId);
            }
        }
        // print log to file
        if (result.isSuccess()) {
            if (addRecord) {
                strBuff.append(printPrefix).append(entity.getCreateUser())
                        .append(" added consume control configure: ").append(newEntity);
            } else {
                strBuff.append(printPrefix).append(entity.getModifyUser())
                        .append(" updated consume control configure from ")
                        .append(curEntity).append(" to ").append(newEntity);
            }
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public boolean delConsumeCtrlConf(String operator,
            String groupName, String topicName,
            StringBuilder strBuff, ProcessResult result) {
        Integer topicLockId = null;
        Integer groupLockId = null;
        GroupConsumeCtrlEntity curEntity;
        String printPrefix = "[delConsumeCtrlConf], ";
        // execute delete operation
        try {
            // lock topicName meta-lock
            topicLockId = metaRowLock.getLock(null,
                    StringUtils.getBytesUtf8(topicName), true);
            try {
                // lock groupName meta-lock
                groupLockId = metaRowLock.getLock(null,
                        StringUtils.getBytesUtf8(groupName), true);
                // check consume control configure exist
                curEntity = consumeCtrlMapper.getConsumeCtrlByGroupAndTopic(groupName, topicName);
                if (curEntity == null) {
                    result.setSuccResult(null);
                    return result.isSuccess();
                }
                consumeCtrlMapper.delGroupConsumeCtrlConf(groupName, topicName, strBuff, result);
            } finally {
                if (groupLockId != null) {
                    metaRowLock.releaseRowLock(groupLockId);
                }
            }
        } catch (Throwable e) {
            return logExceptionInfo(e, printPrefix, strBuff, result);
        } finally {
            if (topicLockId != null) {
                metaRowLock.releaseRowLock(topicLockId);
            }
        }
        // print log to file
        if (result.isSuccess()) {
            strBuff.append(printPrefix).append(operator)
                    .append(" deleted consume control configure: ").append(curEntity);
            logger.info(strBuff.toString());
            strBuff.delete(0, strBuff.length());
        }
        return result.isSuccess();
    }

    @Override
    public List<GroupConsumeCtrlEntity> getConsumeCtrlByTopic(String topicName) {
        return consumeCtrlMapper.getConsumeCtrlByTopicName(topicName);
    }

    @Override
    public Map<String, List<GroupConsumeCtrlEntity>> getConsumeCtrlByTopic(Set<String> topicSet) {
        return consumeCtrlMapper.getConsumeCtrlByTopicName(topicSet);
    }

    @Override
    public GroupConsumeCtrlEntity getConsumeCtrlByGroupAndTopic(String groupName, String topicName) {
        return consumeCtrlMapper.getConsumeCtrlByGroupAndTopic(groupName, topicName);
    }

    @Override
    public Set<String> getDisableTopicByGroupName(String groupName) {
        Set<String> disTopicSet = new HashSet<>();
        List<GroupConsumeCtrlEntity> qryResult =
                consumeCtrlMapper.getConsumeCtrlByGroupName(groupName);
        if (qryResult.isEmpty()) {
            return disTopicSet;
        }
        for (GroupConsumeCtrlEntity ctrlEntity : qryResult) {
            if (ctrlEntity != null
                    && ctrlEntity.getConsumeEnable() == EnableStatus.STATUS_DISABLE) {
                disTopicSet.add(ctrlEntity.getTopicName());
            }
        }
        return disTopicSet;
    }

    @Override
    public List<GroupConsumeCtrlEntity> getConsumeCtrlByGroupName(String groupName) {
        return consumeCtrlMapper.getConsumeCtrlByGroupName(groupName);
    }

    @Override
    public Map<String, List<GroupConsumeCtrlEntity>> getConsumeCtrlByGroupName(
            Set<String> groupSet) {
        return consumeCtrlMapper.getConsumeCtrlByGroupName(groupSet);
    }

    @Override
    public Map<String, List<GroupConsumeCtrlEntity>> getGroupConsumeCtrlConf(
            Set<String> groupSet, Set<String> topicSet, GroupConsumeCtrlEntity qryEntry) {
        return consumeCtrlMapper.getConsumeCtrlInfoMap(groupSet, topicSet, qryEntry);
    }

    @Override
    public boolean isGroupInUse(String groupName) {
        return consumeCtrlMapper.isGroupNameInUse(groupName);
    }

    @Override
    public boolean isTopicInUse(String topicName) {
        return consumeCtrlMapper.isTopicNameInUse(topicName);
    }

    /**
     * Whether service started
     *
     * @return true for started, false for other cases
     */
    protected boolean isServiceStarted() {
        return (this.srvStatus.get() == 2);
    }

    /**
     * Initial meta-data stores.
     *
     * @param strBuff  the string buffer
     */
    protected abstract void initMetaStore(StringBuilder strBuff);

    /**
     * Reload meta-data stores.
     *
     * @param strBuff  the string buffer
     */
    protected void reloadMetaStore(StringBuilder strBuff) {
        // Clear observers' cache data.
        for (ConfigObserver observer : eventObservers) {
            observer.clearCacheData();
        }
        // Load the latest meta-data from persistent
        clusterConfigMapper.loadConfig(strBuff);
        brokerConfigMapper.loadConfig(strBuff);
        topicDeployMapper.loadConfig(strBuff);
        topicCtrlMapper.loadConfig(strBuff);
        groupResCtrlMapper.loadConfig(strBuff);
        consumeCtrlMapper.loadConfig(strBuff);
        // load the latest meta-data to observers
        for (ConfigObserver observer : eventObservers) {
            observer.reloadCacheData();
        }
    }

    /**
     * Close meta-data stores.
     *
     */
    protected void closeMetaStore() {
        brokerConfigMapper.close();
        topicDeployMapper.close();
        groupResCtrlMapper.close();
        topicCtrlMapper.close();
        consumeCtrlMapper.close();
        clusterConfigMapper.close();
    }

    private boolean logExceptionInfo(Throwable e, String printPrefix,
            StringBuilder strBuff, ProcessResult result) {
        strBuff.delete(0, strBuff.length());
        strBuff.append(printPrefix).append("failed to lock meta-lock.");
        logger.warn(strBuff.toString(), e);
        result.setFailResult(DataOpErrCode.DERR_STORE_LOCK_FAILURE.getCode(),
                strBuff.toString());
        strBuff.delete(0, strBuff.length());
        return result.isSuccess();
    }

}
