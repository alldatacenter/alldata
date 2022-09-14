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
import org.apache.inlong.tubemq.corebase.utils.KeyBuilderUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.statusdef.TopicStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbTopicConfEntity;

/*
 * store the topic configure setting
 *
 */
public class TopicDeployEntity extends BaseEntity implements Cloneable {

    private String recordKey = "";
    private String topicName = "";
    private int brokerId = TBaseConstants.META_VALUE_UNDEFINED;
    private TopicStatus deployStatus = TopicStatus.STATUS_TOPIC_UNDEFINED;  // topic status
    private TopicPropGroup topicProps = new TopicPropGroup();
    private String brokerIp =  "";
    private int brokerPort = TBaseConstants.META_VALUE_UNDEFINED;
    private String brokerAddress = "";
    private int topicNameId = TBaseConstants.META_VALUE_UNDEFINED;

    public TopicDeployEntity() {
        super();
    }

    public TopicDeployEntity(BaseEntity opInfoEntity, int brokerId, String topicName) {
        super(opInfoEntity);
        this.brokerId = brokerId;
        this.topicName = topicName;
        this.recordKey = KeyBuilderUtils.buildTopicConfRecKey(brokerId, topicName);
    }

    public TopicDeployEntity(BaseEntity opInfoEntity, int brokerId,
                             String topicName, TopicPropGroup topicProps) {
        super(opInfoEntity);
        this.brokerId = brokerId;
        this.topicName = topicName;
        this.recordKey = KeyBuilderUtils.buildTopicConfRecKey(brokerId, topicName);
        this.topicProps.updModifyInfo(topicProps);
    }

    /**
     * Constructor by BdbTopicConfEntity
     *
     * @param bdbEntity  the BdbTopicConfEntity initial object
     */
    public TopicDeployEntity(BdbTopicConfEntity bdbEntity) {
        super(bdbEntity.getDataVerId(),
                bdbEntity.getCreateUser(), bdbEntity.getCreateDate(),
                bdbEntity.getModifyUser(), bdbEntity.getModifyDate());
        setTopicDeployInfo(bdbEntity.getBrokerId(),
                bdbEntity.getBrokerIp(), bdbEntity.getBrokerPort(),
                bdbEntity.getTopicName());
        this.topicNameId = bdbEntity.getTopicId();
        this.deployStatus = TopicStatus.valueOf(bdbEntity.getTopicStatusId());
        this.topicProps =
                new TopicPropGroup(bdbEntity.getNumTopicStores(), bdbEntity.getNumPartitions(),
                        bdbEntity.getUnflushThreshold(), bdbEntity.getUnflushInterval(),
                        bdbEntity.getUnflushDataHold(), bdbEntity.getMemCacheMsgSizeInMB(),
                        bdbEntity.getMemCacheMsgCntInK(), bdbEntity.getMemCacheFlushIntvl(),
                        bdbEntity.getAcceptPublish(), bdbEntity.getAcceptSubscribe(),
                        bdbEntity.getDeletePolicy(), bdbEntity.getDataStoreType(),
                        bdbEntity.getDataPath());
        this.setAttributes(bdbEntity.getAttributes());
    }

    /**
     * build bdb object from current info
     *
     * @return the BdbTopicConfEntity object
     */
    public BdbTopicConfEntity buildBdbTopicConfEntity() {
        BdbTopicConfEntity bdbEntity =
                new BdbTopicConfEntity(brokerId, brokerIp, brokerPort, topicName,
                        topicProps.getNumPartitions(), topicProps.getUnflushThreshold(),
                        topicProps.getUnflushInterval(), "",
                        topicProps.getDeletePolicy(), topicProps.isAcceptPublish(),
                        topicProps.isAcceptSubscribe(), topicProps.getNumTopicStores(),
                        getAttributes(), getCreateUser(), getCreateDate(),
                        getModifyUser(), getModifyDate());
        bdbEntity.setDataVerId(getDataVerId());
        bdbEntity.setTopicId(topicNameId);
        bdbEntity.setTopicStatusId(deployStatus.getCode());
        bdbEntity.setDataStore(topicProps.getDataStoreType(), topicProps.getDataPath());
        bdbEntity.setNumTopicStores(topicProps.getNumTopicStores());
        bdbEntity.setMemCacheMsgSizeInMB(topicProps.getMemCacheMsgSizeInMB());
        bdbEntity.setMemCacheMsgCntInK(topicProps.getMemCacheMsgCntInK());
        bdbEntity.setMemCacheFlushIntvl(topicProps.getMemCacheFlushIntvl());
        bdbEntity.setUnflushDataHold(topicProps.getUnflushDataHold());
        return bdbEntity;
    }

    private void setTopicDeployInfo(int brokerId, String brokerIp,
                                    int brokerPort, String topicName) {
        this.brokerId = brokerId;
        this.brokerIp = brokerIp;
        this.brokerPort = brokerPort;
        this.topicName = topicName;
        this.recordKey = KeyBuilderUtils.buildTopicConfRecKey(brokerId, topicName);
        this.brokerAddress = KeyBuilderUtils.buildAddressInfo(brokerIp, brokerPort);
    }

    public String getRecordKey() {
        return recordKey;
    }

    public int getBrokerId() {
        return brokerId;
    }

    public String getBrokerIp() {
        return brokerIp;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public String getBrokerAddress() {
        return brokerAddress;
    }

    public TopicPropGroup getTopicProps() {
        return topicProps;
    }

    public void setTopicProps(TopicPropGroup topicProps) {
        this.topicProps = topicProps;
    }

    public int getNumTopicStores() {
        return this.topicProps.getNumTopicStores();
    }

    public int getNumPartitions() {
        return this.topicProps.getNumPartitions();
    }

    public boolean isAcceptPublish() {
        return this.topicProps.isAcceptPublish();
    }

    public boolean isAcceptSubscribe() {
        return this.topicProps.isAcceptSubscribe();
    }

    public int getTopicId() {
        return topicNameId;
    }

    public void setTopicId(int topicId) {
        this.topicNameId = topicId;
    }

    public String getTopicName() {
        return topicName;
    }

    public TopicStatus getTopicStatus() {
        return deployStatus;
    }

    public void setTopicStatusId(int topicStatusId) {
        this.deployStatus = TopicStatus.valueOf(topicStatusId);
    }

    public boolean isInRemoving() {
        return (this.deployStatus == TopicStatus.STATUS_TOPIC_SOFT_REMOVE
                || this.deployStatus == TopicStatus.STATUS_TOPIC_HARD_REMOVE);
    }

    public int getTopicStatusId() {
        return deployStatus.getCode();
    }

    public boolean isValidTopicStatus() {
        return this.deployStatus == TopicStatus.STATUS_TOPIC_OK;
    }

    public TopicStatus getDeployStatus() {
        return deployStatus;
    }

    public void setDeployStatus(TopicStatus deployStatus) {
        this.deployStatus = deployStatus;
    }

    /**
     * update subclass field values
     *
     * @return if changed
     */
    public boolean updModifyInfo(long dataVerId, int topicNameId, int brokerPort,
                                 String brokerIp, TopicStatus deployStatus,
                                 TopicPropGroup topicProps) {
        boolean changed = false;
        // check and set dataVerId info
        if (dataVerId != TBaseConstants.META_VALUE_UNDEFINED
                && this.getDataVerId() != dataVerId) {
            changed = true;
            this.setDataVersionId(dataVerId);
        }
        // check and set topicNameId info
        if (topicNameId != TBaseConstants.META_VALUE_UNDEFINED
                && this.topicNameId != topicNameId) {
            changed = true;
            this.topicNameId = topicNameId;
        }
        // check and set brokerPort info
        if (brokerPort != TBaseConstants.META_VALUE_UNDEFINED
                && this.brokerPort != brokerPort) {
            changed = true;
            this.brokerPort = brokerPort;
        }
        // check and set filterCondStr info
        if (TStringUtils.isNotBlank(brokerIp)
                && !this.brokerIp.equals(brokerIp)) {
            changed = true;
            this.brokerIp = brokerIp;
        }
        // check and set deployStatus info
        if (deployStatus != null
                && deployStatus != TopicStatus.STATUS_TOPIC_UNDEFINED
                && this.deployStatus != deployStatus) {
            changed = true;
            this.deployStatus = deployStatus;
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
            this.brokerAddress =
                    KeyBuilderUtils.buildAddressInfo(this.brokerIp, this.brokerPort);
        }
        return changed;
    }

    /**
     * Check whether the specified query item value matches
     * Allowed query items:
     *   brokerId, topicId, topicName, topicStatus
     *
     * @param target  the matched object
     * @param fullMatch  whether match parent parameters
     * @return true: matched, false: not match
     */
    public boolean isMatched(TopicDeployEntity target, boolean fullMatch) {
        if (target == null) {
            return true;
        }
        if (fullMatch && !super.isMatched(target)) {
            return false;
        }
        return (target.getBrokerId() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getBrokerId() == this.brokerId)
                && (target.getBrokerPort() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getBrokerPort() == this.brokerPort)
                && (target.getTopicId() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getTopicId() == this.topicNameId)
                && (TStringUtils.isBlank(target.getTopicName())
                || target.getTopicName().equals(this.topicName))
                && (TStringUtils.isBlank(target.getBrokerIp())
                || target.getBrokerIp().equals(this.brokerIp))
                && topicProps.isMatched(target.topicProps)
                && (target.getTopicStatus() == TopicStatus.STATUS_TOPIC_UNDEFINED
                || target.getTopicStatus() == this.deployStatus);
    }

    /**
     * Serialize field to json format
     *
     * @param sBuilder   build container
     * @param isLongName if return field key is long name
     * @param fullFormat if return full format json
     * @return  the serialized content
     */
    public StringBuilder toWebJsonStr(StringBuilder sBuilder,
                                      boolean isLongName,
                                      boolean fullFormat) {
        if (isLongName) {
            sBuilder.append("{\"topicName\":\"").append(topicName).append("\"")
                    .append(",\"brokerId\":").append(brokerId)
                    .append(",\"topicNameId\":").append(topicNameId)
                    .append(",\"brokerIp\":\"").append(brokerIp).append("\"")
                    .append(",\"brokerPort\":").append(brokerPort)
                    .append(",\"topicStatusId\":").append(deployStatus.getCode());
        } else {
            sBuilder.append("{\"topic\":\"").append(topicName).append("\"")
                    .append(",\"brkId\":").append(brokerId)
                    .append(",\"topicId\":").append(topicNameId)
                    .append(",\"bIp\":\"").append(brokerIp).append("\"")
                    .append(",\"bPort\":").append(brokerPort)
                    .append(",\"tStsId\":").append(deployStatus.getCode());
        }
        topicProps.toWebJsonStr(sBuilder, isLongName);
        super.toWebJsonStr(sBuilder, isLongName);
        if (fullFormat) {
            sBuilder.append("}");
        }
        return sBuilder;
    }

    /**
     * check if subclass fields is equals
     *
     * @param other  check object
     * @return if equals
     */
    public boolean isDataEquals(TopicDeployEntity other) {
        return super.isDataEquals(other)
                && brokerId == other.brokerId
                && brokerPort == other.brokerPort
                && topicNameId == other.topicNameId
                && recordKey.equals(other.recordKey)
                && topicName.equals(other.topicName)
                && Objects.equals(brokerIp, other.brokerIp)
                && Objects.equals(brokerAddress, other.brokerAddress)
                && deployStatus == other.deployStatus
                && Objects.equals(topicProps, other.topicProps);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TopicDeployEntity)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        TopicDeployEntity that = (TopicDeployEntity) o;
        return isDataEquals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), recordKey, topicName, brokerId,
                brokerIp, brokerPort, brokerAddress, topicNameId, deployStatus, topicProps);
    }

    @Override
    public TopicDeployEntity clone() {
        TopicDeployEntity copy = (TopicDeployEntity) super.clone();
        if (copy.topicProps != null) {
            copy.topicProps = getTopicProps().clone();
        }
        copy.setDeployStatus(getDeployStatus());
        return copy;
    }
}
