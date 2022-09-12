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

package org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.io.Serializable;
import java.util.Date;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.TokenConstants;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.TStoreConstants;

@Entity
public class BdbTopicConfEntity implements Serializable {
    private static final long serialVersionUID = -3266492818900652275L;

    @PrimaryKey
    private String recordKey;
    private int topicStatusId = 0; // topic status, 0: valid, 1: soft deleted
    private int brokerId = -2;      //broker id
    private String brokerIp;        //broker ip
    private int brokerPort;         //broker port
    private String brokerAddress;   //broker address
    private String topicName;       //topic name
    private int numPartitions = -2;     //partition num
    private int unflushThreshold = -2;  //flush threshold
    private int unflushInterval = -2;   //flush interval
    private boolean acceptPublish = true;   //enable publish
    private boolean acceptSubscribe = true; //enable subscribe
    private int numTopicStores = 1;     //store num
    private String deleteWhen;          //delete policy execute time
    private String deletePolicy;        //delete policy
    private int dataStoreType = -2;     //type
    private String dataPath;            //data path
    private String attributes;          //extra attribute
    private String createUser;          //create user
    private Date createDate;            //create date
    private String modifyUser;          //modify user
    private Date modifyDate;            //modify date

    public BdbTopicConfEntity() {
    }

    /**
     * Build topic configure entity
     *
     * @param brokerId            the broker id
     * @param brokerIp            the broker ip
     * @param brokerPort          the broker port
     * @param topicName           the topic name
     * @param numPartitions       the number of partition
     * @param unflushThreshold    the un-flushed message count
     * @param unflushInterval     the un-flushed time delta
     * @param deleteWhen          the delete time
     * @param deletePolicy        the delete policy
     * @param acceptPublish       whether accept publish
     * @param acceptSubscribe     whether accept subscribe
     * @param numTopicStores      the number of topic store
     * @param attributes          the attribute information
     * @param createUser          the creator
     * @param createDate          the create date
     * @param modifyUser          the modifier
     * @param modifyDate          the modify date
     */
    public BdbTopicConfEntity(final int brokerId, final String brokerIp,
                              final int brokerPort, final String topicName,
                              final int numPartitions, final int unflushThreshold,
                              final int unflushInterval, final String deleteWhen,
                              final String deletePolicy, final boolean acceptPublish,
                              final boolean acceptSubscribe, final int numTopicStores,
                              final String attributes, final String createUser,
                              final Date createDate, final String modifyUser,
                              final Date modifyDate) {
        StringBuilder sBuilder = new StringBuilder(512);
        this.recordKey = sBuilder.append(brokerId)
                .append(TokenConstants.ATTR_SEP).append(topicName).toString();
        sBuilder.delete(0, sBuilder.length());
        this.brokerId = brokerId;
        this.brokerIp = brokerIp;
        this.brokerPort = brokerPort;
        this.brokerAddress = sBuilder.append(this.brokerIp)
                .append(TokenConstants.ATTR_SEP).append(this.brokerPort).toString();
        this.topicName = topicName;
        this.numPartitions = numPartitions;
        this.unflushThreshold = unflushThreshold;
        this.unflushInterval = unflushInterval;
        this.deleteWhen = deleteWhen;
        this.deletePolicy = deletePolicy;
        this.acceptPublish = acceptPublish;
        this.acceptSubscribe = acceptSubscribe;
        this.numTopicStores = numTopicStores;
        this.createUser = createUser;
        this.createDate = createDate;
        this.modifyUser = modifyUser;
        this.modifyDate = modifyDate;
        this.attributes = attributes;
    }

    public void setBrokerAndTopicInfo(int brokerId, String brokerIp,
                                      int brokerPort, String topicName) {
        StringBuilder sBuilder = new StringBuilder(512);
        this.recordKey = sBuilder.append(brokerId)
                .append(TokenConstants.ATTR_SEP).append(topicName).toString();
        this.brokerId = brokerId;
        this.brokerIp = brokerIp;
        this.brokerPort = brokerPort;
        if (this.brokerPort != TBaseConstants.META_VALUE_UNDEFINED) {
            sBuilder.delete(0, sBuilder.length());
            this.brokerAddress = sBuilder.append(brokerIp)
                    .append(TokenConstants.ATTR_SEP).append(brokerPort).toString();
        }
        this.topicName = topicName;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(final String attributes) {
        this.attributes = attributes;
    }

    public int getNumTopicStores() {
        if (this.numTopicStores == 0) {
            return 1;
        }
        return this.numTopicStores;
    }

    public void setNumTopicStores(final int numTopicStores) {
        this.numTopicStores = numTopicStores;
    }

    public void setUnflushDataHold(final int unFlushDataHold) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_DATA_UNFLUSHHOLD,
                        String.valueOf(unFlushDataHold));
    }

    public void setDataStore(int dataStoreType, String dataPath) {
        this.dataPath = dataPath;
        this.dataStoreType = dataStoreType;
    }

    public int getDataStoreType() {
        return dataStoreType;
    }

    public String getDataPath() {
        return dataPath;
    }

    public int getUnflushThreshold() {
        return this.unflushThreshold;
    }

    public void setUnflushThreshold(int unflushThreshold) {
        this.unflushThreshold = unflushThreshold;
    }

    public int getUnflushInterval() {
        return this.unflushInterval;
    }

    public void setUnflushInterval(int unflushInterval) {
        this.unflushInterval = unflushInterval;
    }

    public String getDeleteWhen() {
        return this.deleteWhen;
    }

    public void setDeleteWhen(String deleteWhen) {
        this.deleteWhen = deleteWhen;
    }

    public String getDeletePolicy() {
        return this.deletePolicy;
    }

    public void setDeletePolicy(String deletePolicy) {
        this.deletePolicy = deletePolicy;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public int getTopicStatusId() {
        return topicStatusId;
    }

    public void setTopicStatusId(int topicStatusId) {
        this.topicStatusId = topicStatusId;
    }

    public boolean isValidTopicStatus() {
        return this.topicStatusId == 0;
    }

    public boolean getAcceptPublish() {
        return acceptPublish;
    }

    public void setAcceptPublish(boolean acceptPublish) {
        this.acceptPublish = acceptPublish;
    }

    public boolean getAcceptSubscribe() {
        return acceptSubscribe;
    }

    public void setAcceptSubscribe(boolean acceptSubscribe) {
        this.acceptSubscribe = acceptSubscribe;
    }

    public int getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(int brokerId) {
        this.brokerId = brokerId;
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

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public int getNumPartitions() {
        return numPartitions;
    }

    public void setNumPartitions(int numPartitions) {
        this.numPartitions = numPartitions;
    }

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Date createDate) {
        this.createDate = createDate;
    }

    public String getModifyUser() {
        return modifyUser;
    }

    public void setModifyUser(String modifyUser) {
        this.modifyUser = modifyUser;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setModifyDate(Date modifyDate) {
        this.modifyDate = modifyDate;
    }

    public int getUnflushDataHold() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_DATA_UNFLUSHHOLD);
        if (atrVal != null) {
            return Integer.parseInt(atrVal);
        }
        return TServerConstants.CFG_DEFAULT_DATA_UNFLUSH_HOLD;
    }

    public int getMemCacheMsgCntInK() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_MCACHE_MSG_CNT);
        if (atrVal != null) {
            return Integer.parseInt(atrVal);
        }
        return 10;
    }

    public void setMemCacheMsgCntInK(final int memCacheMsgCntInK) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_MCACHE_MSG_CNT,
                        String.valueOf(memCacheMsgCntInK));
    }

    public int getMemCacheMsgSizeInMB() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_MCACHE_MSG_SIZE);
        if (atrVal != null) {
            return Integer.parseInt(atrVal);
        }
        return 2;
    }

    public void setMemCacheMsgSizeInMB(final int memCacheMsgSizeInMB) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_MCACHE_MSG_SIZE,
                        String.valueOf(memCacheMsgSizeInMB));
    }

    public int getMemCacheFlushIntvl() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_MCACHE_FLUSH_INTVL);
        if (atrVal != null) {
            return Integer.parseInt(atrVal);
        }
        return 20000;
    }

    public void setMemCacheFlushIntvl(final int memCacheFlushIntvl) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_MCACHE_FLUSH_INTVL,
                        String.valueOf(memCacheFlushIntvl));
    }

    public int getMaxMsgSize() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_MAX_MSG_SIZE);
        if (atrVal != null) {
            return Integer.parseInt(atrVal);
        }
        return TBaseConstants.META_VALUE_UNDEFINED;
    }

    public void setMaxMsgSize(int maxMsgSize) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_MAX_MSG_SIZE,
                        String.valueOf(maxMsgSize));
    }

    public long getDataVerId() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_DATA_VERSION_ID);
        if (atrVal != null) {
            return Long.parseLong(atrVal);
        }
        return TBaseConstants.META_VALUE_UNDEFINED;
    }

    public void setDataVerId(long dataVerId) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_DATA_VERSION_ID,
                        String.valueOf(dataVerId));
    }

    public int getTopicId() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_TOPICNAME_ID);
        if (atrVal != null) {
            return Integer.parseInt(atrVal);
        }
        return TBaseConstants.META_VALUE_UNDEFINED;
    }

    public void setTopicId(int topicId) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_TOPICNAME_ID,
                        String.valueOf(topicId));
    }

    public void appendAttributes(String attrKey, String attrVal) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes, attrKey, attrVal);
    }

    /**
     * Serialize field to json format
     *
     * @param sBuilder
     * @return
     */
    public StringBuilder toJsonString(final StringBuilder sBuilder) {
        return sBuilder.append("{\"type\":\"BdbTopicConfEntity\",")
                .append("\"recordKey\":\"").append(recordKey)
                .append("\",\"topicStatusId\":\"").append(topicStatusId)
                .append("\",\"brokerId\":\"").append(brokerId)
                .append("\",\"brokerAddress\":\"").append(brokerAddress)
                .append("\",\"topicName\":\"").append(topicName)
                .append("\",\"numPartitions\":").append(numPartitions)
                .append(",\"unflushThreshold\":").append(unflushThreshold)
                .append(",\"unflushDataHold\":").append(getUnflushDataHold())
                .append(",\"unflushInterval\":").append(unflushInterval)
                .append(",\"deleteWhen\":\"").append(deleteWhen)
                .append("\",\"deletePolicy\":\"").append(deletePolicy)
                .append(",\"acceptPublish\":").append(acceptPublish)
                .append(",\"acceptSubscribe\":").append(acceptSubscribe)
                .append(",\"numTopicStores\":").append(numTopicStores)
                .append(",\"memCacheMsgCntInK\":").append(getMemCacheMsgCntInK())
                .append(",\"memCacheMsgSizeInMB\":").append(getMemCacheMsgSizeInMB())
                .append(",\"memCacheFlushIntvl\":").append(getMemCacheFlushIntvl())
                .append(",\"maxMsgSize\":").append(getMaxMsgSize())
                .append(",\"dataPath\":\"").append(dataPath)
                .append("\",\"createUser\":\"").append(createUser)
                .append("\",\"createDate\":\"")
                .append(DateTimeConvertUtils.date2yyyyMMddHHmmss(createDate))
                .append("\",\"modifyUser\":\"").append(modifyUser)
                .append("\",\"modifyDate\":\"")
                .append(DateTimeConvertUtils.date2yyyyMMddHHmmss(modifyDate))
                .append("\"}");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("recordKey", recordKey)
                .append("topicStatusId", topicStatusId)
                .append("brokerId", brokerId)
                .append("brokerIp", brokerIp)
                .append("brokerPort", brokerPort)
                .append("brokerAddress", brokerAddress)
                .append("topicName", topicName)
                .append("numPartitions", numPartitions)
                .append("unflushThreshold", unflushThreshold)
                .append("unflushInterval", unflushInterval)
                .append("acceptPublish", acceptPublish)
                .append("acceptSubscribe", acceptSubscribe)
                .append("numTopicStores", numTopicStores)
                .append("deleteWhen", deleteWhen)
                .append("deletePolicy", deletePolicy)
                .append("dataStoreType", dataStoreType)
                .append("dataPath", dataPath)
                .append("attributes", attributes)
                .append("createUser", createUser)
                .append("createDate", createDate)
                .append("modifyUser", modifyUser)
                .append("modifyDate", modifyDate)
                .toString();
    }
}
