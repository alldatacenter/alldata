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

package org.apache.inlong.tubemq.server.master.metamanage.metastore.dao.entity;

import java.util.Objects;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.statusdef.ManageStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbBrokerConfEntity;

/*
 * store the broker default setting
 *
 */

public class BrokerConfEntity extends BaseEntity implements Cloneable {
    // Primary Key
    private int brokerId = TBaseConstants.META_VALUE_UNDEFINED;
    private String brokerIp = "";
    private int brokerPort = TBaseConstants.META_VALUE_UNDEFINED;
    // broker tls port
    private int brokerTLSPort = TBaseConstants.META_VALUE_UNDEFINED;
    // broker web port
    private int brokerWebPort = TBaseConstants.META_VALUE_UNDEFINED;
    private ManageStatus manageStatus = ManageStatus.STATUS_MANAGE_UNDEFINED;
    private int regionId = TBaseConstants.META_VALUE_UNDEFINED;
    private int groupId = TBaseConstants.META_VALUE_UNDEFINED;
    private TopicPropGroup topicProps = new TopicPropGroup();
    // Redundant fields begin
    private String brokerAddress = "";       // broker ip:port
    private String brokerFullInfo = "";      // broker brokerId:ip:port
    private String brokerSimpleInfo = "";    // broker brokerId:ip:
    private String brokerTLSSimpleInfo = ""; //tls simple info
    private String brokerTLSFullInfo = "";   //tls full info
    // Redundant fields end

    public BrokerConfEntity() {
        super();
    }

    public BrokerConfEntity(BaseEntity opEntity, int brokerId, String brokerIp) {
        super(opEntity);
        this.brokerId = brokerId;
        this.brokerIp = brokerIp;
    }

    public BrokerConfEntity(BaseEntity opEntity, int brokerId,
                            String brokerIp, ClusterSettingEntity defSetting) {
        super(opEntity);
        this.brokerWebPort = defSetting.getBrokerWebPort();
        this.topicProps.updModifyInfo(defSetting.getClsDefTopicProps());
        setBrokerIpAndAllPort(brokerId, brokerIp,
                defSetting.getBrokerPort(), defSetting.getBrokerTLSPort());
    }

    /**
     * Initial Broker Configure entity by BdbBrokerConfEntity
     *
     * @param bdbEntity   need initialed BdbBrokerConfEntity information
     */
    public BrokerConfEntity(BdbBrokerConfEntity bdbEntity) {
        super(bdbEntity.getDataVerId(), bdbEntity.getRecordCreateUser(),
                bdbEntity.getRecordCreateDate(), bdbEntity.getRecordModifyUser(),
                bdbEntity.getRecordModifyDate());
        setBrokerIpAndAllPort(bdbEntity.getBrokerId(), bdbEntity.getBrokerIp(),
                bdbEntity.getBrokerPort(), bdbEntity.getBrokerTLSPort());
        this.regionId = bdbEntity.getRegionId();
        this.groupId = bdbEntity.getBrokerGroupId();
        this.brokerWebPort = bdbEntity.getBrokerWebPort();
        this.topicProps =
                new TopicPropGroup(bdbEntity.getNumTopicStores(), bdbEntity.getDftNumPartitions(),
                        bdbEntity.getDftUnflushThreshold(), bdbEntity.getDftUnflushInterval(),
                        bdbEntity.getDftUnFlushDataHold(), bdbEntity.getDftMemCacheMsgSizeInMB(),
                        bdbEntity.getDftMemCacheMsgCntInK(), bdbEntity.getDftMemCacheFlushIntvl(),
                        bdbEntity.isAcceptPublish(), bdbEntity.isAcceptSubscribe(),
                        bdbEntity.getDftDeletePolicy(), bdbEntity.getDataStoreType(),
                        bdbEntity.getDataPath());
        this.manageStatus = ManageStatus.valueOf(bdbEntity.getManageStatus());
        setAttributes(bdbEntity.getAttributes());
    }

    /**
     * build bdb object from current info
     *
     * @return the BdbBrokerConfEntity object
     */
    public BdbBrokerConfEntity buildBdbBrokerConfEntity() {
        BdbBrokerConfEntity bdbEntity = new BdbBrokerConfEntity(brokerId, brokerIp, brokerPort,
                topicProps.getNumPartitions(), topicProps.getUnflushThreshold(),
                topicProps.getUnflushInterval(), "", topicProps.getDeletePolicy(),
                manageStatus.getCode(), topicProps.isAcceptPublish(),
                topicProps.isAcceptSubscribe(), getAttributes(), true,
                false, getCreateUser(), getCreateDate(),
                getModifyUser(), getModifyDate());
        bdbEntity.setDataVerId(getDataVerId());
        bdbEntity.setRegionId(regionId);
        bdbEntity.setBrokerGroupId(groupId);
        bdbEntity.setBrokerTLSPort(brokerTLSPort);
        bdbEntity.setBrokerWebPort(brokerWebPort);
        bdbEntity.setNumTopicStores(topicProps.getNumTopicStores());
        bdbEntity.setDftMemCacheMsgSizeInMB(topicProps.getMemCacheMsgSizeInMB());
        bdbEntity.setDftMemCacheMsgCntInK(topicProps.getMemCacheMsgCntInK());
        bdbEntity.setDftMemCacheFlushIntvl(topicProps.getMemCacheFlushIntvl());
        bdbEntity.setDftUnFlushDataHold(topicProps.getUnflushDataHold());
        bdbEntity.setDataStore(topicProps.getDataStoreType(), topicProps.getDataPath());
        return bdbEntity;
    }

    public int getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(int brokerId) {
        this.brokerId = brokerId;
    }

    public ManageStatus getManageStatus() {
        return manageStatus;
    }

    public String getManageStatusStr() {
        return manageStatus.getDescription();
    }

    public void setManageStatus(ManageStatus manageStatus) {
        this.manageStatus = manageStatus;
    }

    public String getBrokerAddress() {
        return brokerAddress;
    }

    public String getBrokerIp() {
        return brokerIp;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public void setBrokerIpAndAllPort(int brokerId, String brokerIp,
                                      int brokerPort, int brokerTLSPort) {
        this.brokerId = brokerId;
        this.brokerIp = brokerIp;
        this.brokerPort = brokerPort;
        this.brokerTLSPort = brokerTLSPort;
        this.buildStrInfo();
    }

    public int getBrokerWebPort() {
        return brokerWebPort;
    }

    public void setBrokerWebPort(int brokerWebPort) {
        this.brokerWebPort = brokerWebPort;
    }

    public int getBrokerTLSPort() {
        return brokerTLSPort;
    }

    public String getBrokerIdAndAddress() {
        return brokerFullInfo;
    }

    public String getSimpleBrokerInfo() {
        if (this.brokerPort == TBaseConstants.META_DEFAULT_BROKER_PORT) {
            return this.brokerSimpleInfo;
        } else {
            return this.brokerFullInfo;
        }
    }

    public String getSimpleTLSBrokerInfo() {
        if (getBrokerTLSPort() == TBaseConstants.META_DEFAULT_BROKER_PORT) {
            return this.brokerTLSSimpleInfo;
        } else {
            return this.brokerTLSFullInfo;
        }
    }

    public String getBrokerTLSFullInfo() {
        return brokerTLSFullInfo;
    }

    public int getRegionId() {
        return regionId;
    }

    public void setRegionId(int regionId) {
        this.regionId = regionId;
    }

    public int getGroupId() {
        return groupId;
    }

    public void setGroupId(int groupId) {
        this.groupId = groupId;
    }

    public TopicPropGroup getTopicProps() {
        return topicProps;
    }

    public void setTopicProps(TopicPropGroup topicProps) {
        this.topicProps = topicProps;
    }

    private void buildStrInfo() {
        StringBuilder sBuilder = new StringBuilder(512);
        this.brokerAddress = sBuilder.append(this.brokerIp)
                .append(TokenConstants.ATTR_SEP)
                .append(this.brokerPort).toString();
        sBuilder.delete(0, sBuilder.length());
        this.brokerSimpleInfo = sBuilder.append(this.brokerId)
                .append(TokenConstants.ATTR_SEP).append(this.brokerIp)
                .append(TokenConstants.ATTR_SEP).append(" ").toString();
        sBuilder.delete(0, sBuilder.length());
        this.brokerFullInfo = sBuilder.append(this.brokerId)
                .append(TokenConstants.ATTR_SEP).append(this.brokerIp)
                .append(TokenConstants.ATTR_SEP).append(this.brokerPort).toString();
        sBuilder.delete(0, sBuilder.length());
        this.brokerTLSSimpleInfo = sBuilder.append(this.brokerId)
                .append(TokenConstants.ATTR_SEP).append(this.brokerIp)
                .append(TokenConstants.ATTR_SEP).append(" ").toString();
        sBuilder.delete(0, sBuilder.length());
        this.brokerTLSFullInfo = sBuilder.append(this.brokerId)
                .append(TokenConstants.ATTR_SEP).append(this.brokerIp)
                .append(TokenConstants.ATTR_SEP).append(brokerTLSPort).toString();
    }

    /**
     * update subclass field values
     *
     * @return if changed
     */
    public boolean updModifyInfo(long dataVerId, int brokerPort, int brokerTlsPort,
                                 int brokerWebPort, int regionId, int groupId,
                                 ManageStatus manageStatus, TopicPropGroup topicProps) {
        boolean changed = false;
        // check and set dataVerId info
        if (dataVerId != TBaseConstants.META_VALUE_UNDEFINED
                && this.getDataVerId() != dataVerId) {
            changed = true;
            this.setDataVersionId(dataVerId);
        }
        // check and set brokerPort info
        if (brokerPort != TBaseConstants.META_VALUE_UNDEFINED
                && this.brokerPort != brokerPort) {
            changed = true;
            this.brokerPort = brokerPort;
        }
        // check and set brokerTLSPort info
        if (brokerTlsPort != TBaseConstants.META_VALUE_UNDEFINED
                && this.brokerTLSPort != brokerTlsPort) {
            changed = true;
            this.brokerTLSPort = brokerTlsPort;
        }
        // check and set brokerWebPort info
        if (brokerWebPort != TBaseConstants.META_VALUE_UNDEFINED
                && this.brokerWebPort != brokerWebPort) {
            changed = true;
            this.brokerWebPort = brokerWebPort;
        }
        // check and set regionId info
        if (regionId != TBaseConstants.META_VALUE_UNDEFINED
                && this.regionId != regionId) {
            changed = true;
            this.regionId = regionId;
        }
        // check and set regionId info
        if (groupId != TBaseConstants.META_VALUE_UNDEFINED
                && this.groupId != groupId) {
            changed = true;
            this.groupId = groupId;
        }
        // check and set resCheckStatus info
        if (manageStatus != null
                && manageStatus != ManageStatus.STATUS_MANAGE_UNDEFINED
                && this.manageStatus != manageStatus) {
            changed = true;
            this.manageStatus = manageStatus;
        }
        // check and set topicProps info
        if (topicProps != null
                && !topicProps.isDataEquals(this.topicProps)) {
            if (this.topicProps.updModifyInfo(topicProps)) {
                changed = true;
            }
        }
        if (changed) {
            updSerialId();
            buildStrInfo();
        }
        return changed;
    }

    /**
     * Check whether the specified query item value matches
     * Allowed query items:
     *   brokerId, brokerIp, brokerPort, brokerTLSPort, regionId, groupId
     *   manageStatus, brokerWebPort
     * @return true: matched, false: not match
     */
    public boolean isMatched(BrokerConfEntity target) {
        if (target == null) {
            return true;
        }
        if (!super.isMatched(target)) {
            return false;
        }
        return (target.getBrokerId() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getBrokerId() == this.brokerId)
                && (TStringUtils.isBlank(target.getBrokerIp())
                || target.getBrokerIp().equals(this.brokerIp))
                && (target.getBrokerPort() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getBrokerPort() == this.brokerPort)
                && (target.getBrokerTLSPort() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getBrokerTLSPort() == this.brokerTLSPort)
                && (target.getRegionId() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getRegionId() == this.regionId)
                && (target.getGroupId() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getGroupId() == this.groupId)
                && (target.getManageStatus() == ManageStatus.STATUS_MANAGE_UNDEFINED
                || target.getManageStatus() == this.manageStatus)
                && (target.getBrokerWebPort() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getBrokerWebPort() == this.brokerWebPort)
                && this.topicProps.isMatched(target.getTopicProps());
    }

    /**
     * Serialize field to json format
     *
     * @param sBuffer   build container
     * @param isConfLoaded  whether configure updated
     * @param isConfUpdated  whether configure loaded
     * @param isLongName if return field key is long name
     * @param fullFormat if return full format json
     * @return process result
     */
    public StringBuilder toWebJsonStr(StringBuilder sBuffer,
                                      boolean isConfUpdated, boolean isConfLoaded,
                                      boolean isLongName, boolean fullFormat) {
        if (isLongName) {
            sBuffer.append("{\"brokerId\":").append(brokerId)
                    .append(",\"brokerIp\":\"").append(brokerIp).append("\"")
                    .append(",\"brokerPort\":").append(brokerPort)
                    .append(",\"brokerTLSPort\":").append(brokerTLSPort)
                    .append(",\"brokerWebPort\":").append(brokerWebPort)
                    .append(",\"manageStatus\":\"").append(manageStatus.getDescription()).append("\"")
                    .append(",\"isConfChanged\":").append(isConfUpdated)
                    .append(",\"isConfLoaded\":").append(isConfLoaded)
                    .append(",\"regionId\":").append(regionId)
                    .append(",\"groupId\":").append(groupId);
        } else {
            sBuffer.append("{\"brkId\":").append(brokerId)
                    .append(",\"bIp\":\"").append(brokerIp).append("\"")
                    .append(",\"bPort\":").append(brokerPort)
                    .append(",\"bTlsPort\":").append(brokerTLSPort)
                    .append(",\"bWebPort\":").append(brokerWebPort)
                    .append(",\"mSts\":\"").append(manageStatus.getDescription()).append("\"")
                    .append(",\"isConfChg\":").append(isConfUpdated)
                    .append(",\"isConfLd\":").append(isConfLoaded)
                    .append(",\"rId\":").append(regionId)
                    .append(",\"gId\":").append(groupId);
        }
        topicProps.toWebJsonStr(sBuffer, isLongName);
        super.toWebJsonStr(sBuffer, isLongName);
        if (fullFormat) {
            sBuffer.append("}");
        }
        return sBuffer;
    }

    /**
     * Get broker config string
     *
     * @param defSetting  the default setting
     * @param strBuff     the string buffer
     */
    public void getBrokerDefaultConfInfo(ClusterSettingEntity defSetting,
                                         StringBuilder strBuff) {
        TopicPropGroup defTopicProps = defSetting.getClsDefTopicProps();
        strBuff.append((topicProps.getNumPartitions() == TBaseConstants.META_VALUE_UNDEFINED)
                ? defTopicProps.getNumPartitions() : topicProps.getNumPartitions())
                .append(TokenConstants.ATTR_SEP)
                .append((topicProps.getAcceptPublish() == null)
                        ? defTopicProps.isAcceptPublish() : topicProps.isAcceptPublish())
                .append(TokenConstants.ATTR_SEP)
                .append((topicProps.getAcceptSubscribe() == null)
                        ? defTopicProps.isAcceptSubscribe() : topicProps.isAcceptSubscribe())
                .append(TokenConstants.ATTR_SEP)
                .append((topicProps.getUnflushThreshold() == TBaseConstants.META_VALUE_UNDEFINED)
                        ? defTopicProps.getUnflushThreshold() : topicProps.getUnflushThreshold())
                .append(TokenConstants.ATTR_SEP)
                .append((topicProps.getUnflushInterval() == TBaseConstants.META_VALUE_UNDEFINED)
                        ? defTopicProps.getUnflushInterval() : topicProps.getUnflushInterval())
                .append(TokenConstants.ATTR_SEP).append(" ").append(TokenConstants.ATTR_SEP)
                .append((TStringUtils.isEmpty(topicProps.getDeletePolicy()))
                        ? defTopicProps.getDeletePolicy() : topicProps.getDeletePolicy())
                .append(TokenConstants.ATTR_SEP)
                .append((topicProps.getNumTopicStores() == TBaseConstants.META_VALUE_UNDEFINED)
                        ? defTopicProps.getNumTopicStores() : topicProps.getNumTopicStores())
                .append(TokenConstants.ATTR_SEP)
                .append((topicProps.getUnflushDataHold() == TBaseConstants.META_VALUE_UNDEFINED)
                        ? defTopicProps.getUnflushDataHold() : topicProps.getUnflushDataHold())
                .append(TokenConstants.ATTR_SEP)
                .append((topicProps.getMemCacheMsgSizeInMB() == TBaseConstants.META_VALUE_UNDEFINED)
                        ? defTopicProps.getMemCacheMsgSizeInMB() : topicProps.getMemCacheMsgSizeInMB())
                .append(TokenConstants.ATTR_SEP)
                .append((topicProps.getMemCacheMsgCntInK() == TBaseConstants.META_VALUE_UNDEFINED)
                        ? defTopicProps.getMemCacheMsgCntInK() : topicProps.getMemCacheMsgCntInK())
                .append(TokenConstants.ATTR_SEP)
                .append((topicProps.getMemCacheFlushIntvl() == TBaseConstants.META_VALUE_UNDEFINED)
                        ? defTopicProps.getMemCacheFlushIntvl() : topicProps.getMemCacheFlushIntvl());
    }

    /**
     * check if subclass fields is equals
     *
     * @param other  check object
     * @return if equals
     */
    public boolean isDataEquals(BrokerConfEntity other) {
        return super.isDataEquals(other)
                && brokerId == other.brokerId
                && brokerPort == other.brokerPort
                && brokerTLSPort == other.brokerTLSPort
                && brokerWebPort == other.brokerWebPort
                && regionId == other.regionId
                && groupId == other.groupId
                && Objects.equals(brokerIp, other.brokerIp)
                && manageStatus == other.manageStatus
                && Objects.equals(topicProps, other.topicProps)
                && Objects.equals(brokerAddress, other.brokerAddress)
                && Objects.equals(brokerFullInfo, other.brokerFullInfo)
                && Objects.equals(brokerSimpleInfo, other.brokerSimpleInfo)
                && Objects.equals(brokerTLSSimpleInfo, other.brokerTLSSimpleInfo)
                && Objects.equals(brokerTLSFullInfo, other.brokerTLSFullInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BrokerConfEntity)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        BrokerConfEntity entity = (BrokerConfEntity) o;
        return isDataEquals(entity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), brokerId, brokerIp, brokerPort, brokerTLSPort,
                brokerWebPort, manageStatus, regionId, groupId, topicProps, brokerAddress,
                brokerFullInfo, brokerSimpleInfo, brokerTLSSimpleInfo, brokerTLSFullInfo);
    }

    @Override
    public BrokerConfEntity clone() {
        BrokerConfEntity copy = (BrokerConfEntity) super.clone();
        copy.setManageStatus(getManageStatus());
        if (copy.getTopicProps() != null) {
            copy.setTopicProps(getTopicProps().clone());
        }
        return copy;
    }

}
