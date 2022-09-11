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
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.TStoreConstants;

/*
 * store the cluster default setting
 *
 */
@Entity
public class BdbClusterSettingEntity implements Serializable {

    private static final long serialVersionUID = 3259439355290322115L;

    @PrimaryKey
    private String recordKey = "";
    private long configId = TBaseConstants.META_VALUE_UNDEFINED;
    //broker tcp port
    private int brokerPort = TBaseConstants.META_VALUE_UNDEFINED;
    //broker tls port
    private int brokerTLSPort = TBaseConstants.META_VALUE_UNDEFINED;
    //broker web port
    private int brokerWebPort = TBaseConstants.META_VALUE_UNDEFINED;
    //store num
    private int numTopicStores = TBaseConstants.META_VALUE_UNDEFINED;
    //partition num
    private int numPartitions = TBaseConstants.META_VALUE_UNDEFINED;
    //flush disk threshold
    private int unflushThreshold = TBaseConstants.META_VALUE_UNDEFINED;
    //flush disk interval
    private int unflushInterval = TBaseConstants.META_VALUE_UNDEFINED;
    //flush disk data count
    private int unflushDataHold = TBaseConstants.META_VALUE_UNDEFINED;
    //flush memory cache count
    private int memCacheMsgCntInK = TBaseConstants.META_VALUE_UNDEFINED;
    //flush memory cache interval
    private int memCacheFlushIntvl = TBaseConstants.META_VALUE_UNDEFINED;
    //flush memory cache size
    private int memCacheMsgSizeInMB = TBaseConstants.META_VALUE_UNDEFINED;
    private boolean acceptPublish = true;   //enable publish
    private boolean acceptSubscribe = true; //enable subscribe
    private String deletePolicy = "";              //delete policy execute time
    private int qryPriorityId = TBaseConstants.META_VALUE_UNDEFINED;
    private int maxMsgSizeInB = TBaseConstants.META_VALUE_UNDEFINED;
    private String attributes = "";             //extra attribute
    private String modifyUser;               //modify user
    private Date modifyDate;                 //modify date

    public BdbClusterSettingEntity() {
    }

    /**
     * Build cluster setting entity
     *
     * @param recordKey              the record key
     * @param configId               the configure id
     * @param brokerPort             the broker port
     * @param brokerTLSPort          the broker TLS port
     * @param brokerWebPort          the broker web port
     * @param numTopicStores         the number of topic store
     * @param numPartitions          the number of partition
     * @param unflushThreshold       the un-flushed message count
     * @param unflushInterval        the un-flushed time delta
     * @param unflushDataHold        the un-flushed data size
     * @param memCacheMsgCntInK      the memory cached message count
     * @param memCacheFlushIntvl     the memory cached time delta
     * @param memCacheMsgSizeInMB    the memory cached message size
     * @param acceptPublish          whether accept publish
     * @param acceptSubscribe        whether accept subscribe
     * @param deletePolicy           the delete policy
     * @param qryPriorityId          the query priority id
     * @param maxMsgSizeInB          the default message max size
     * @param attributes          the attribute information
     * @param modifyUser          the modifier
     * @param modifyDate          the modify date
     */
    public BdbClusterSettingEntity(String recordKey, long configId, int brokerPort,
                                   int brokerTLSPort, int brokerWebPort,
                                   int numTopicStores, int numPartitions,
                                   int unflushThreshold, int unflushInterval,
                                   int unflushDataHold, int memCacheMsgCntInK,
                                   int memCacheFlushIntvl, int memCacheMsgSizeInMB,
                                   boolean acceptPublish, boolean acceptSubscribe,
                                   String deletePolicy, int qryPriorityId,
                                   int maxMsgSizeInB, String attributes,
                                   String modifyUser, Date modifyDate) {
        this.recordKey = recordKey;
        this.configId = configId;
        this.brokerPort = brokerPort;
        this.brokerTLSPort = brokerTLSPort;
        this.brokerWebPort = brokerWebPort;
        this.numTopicStores = numTopicStores;
        this.numPartitions = numPartitions;
        this.unflushThreshold = unflushThreshold;
        this.unflushInterval = unflushInterval;
        this.unflushDataHold = unflushDataHold;
        this.memCacheMsgCntInK = memCacheMsgCntInK;
        this.memCacheFlushIntvl = memCacheFlushIntvl;
        this.memCacheMsgSizeInMB = memCacheMsgSizeInMB;
        this.acceptPublish = acceptPublish;
        this.acceptSubscribe = acceptSubscribe;
        this.deletePolicy = deletePolicy;
        this.qryPriorityId = qryPriorityId;
        this.maxMsgSizeInB = maxMsgSizeInB;
        this.attributes = attributes;
        this.modifyUser = modifyUser;
        this.modifyDate = modifyDate;
    }

    public void setRecordKey(String recordKey) {
        this.recordKey = recordKey;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public long getConfigId() {
        return configId;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public void setBrokerPort(int brokerPort) {
        this.brokerPort = brokerPort;
    }

    public int getBrokerTLSPort() {
        return brokerTLSPort;
    }

    public void setBrokerTLSPort(int brokerTLSPort) {
        this.brokerTLSPort = brokerTLSPort;
    }

    public int getBrokerWebPort() {
        return brokerWebPort;
    }

    public void setBrokerWebPort(int brokerWebPort) {
        this.brokerWebPort = brokerWebPort;
    }

    public int getNumTopicStores() {
        return numTopicStores;
    }

    public void setNumTopicStores(int numTopicStores) {
        this.numTopicStores = numTopicStores;
    }

    public int getNumPartitions() {
        return numPartitions;
    }

    public void setNumPartitions(int numPartitions) {
        this.numPartitions = numPartitions;
    }

    public int getUnflushThreshold() {
        return unflushThreshold;
    }

    public void setUnflushThreshold(int unflushThreshold) {
        this.unflushThreshold = unflushThreshold;
    }

    public int getUnflushInterval() {
        return unflushInterval;
    }

    public void setUnflushInterval(int unflushInterval) {
        this.unflushInterval = unflushInterval;
    }

    public int getUnflushDataHold() {
        return unflushDataHold;
    }

    public void setUnflushDataHold(int unflushDataHold) {
        this.unflushDataHold = unflushDataHold;
    }

    public int getMemCacheMsgCntInK() {
        return memCacheMsgCntInK;
    }

    public void setMemCacheMsgCntInK(int memCacheMsgCntInK) {
        this.memCacheMsgCntInK = memCacheMsgCntInK;
    }

    public int getMemCacheFlushIntvl() {
        return memCacheFlushIntvl;
    }

    public void setMemCacheFlushIntvl(int memCacheFlushIntvl) {
        this.memCacheFlushIntvl = memCacheFlushIntvl;
    }

    public int getMemCacheMsgSizeInMB() {
        return memCacheMsgSizeInMB;
    }

    public void setMemCacheMsgSizeInMB(int memCacheMsgSizeInMB) {
        this.memCacheMsgSizeInMB = memCacheMsgSizeInMB;
    }

    public boolean isAcceptPublish() {
        return acceptPublish;
    }

    public void setAcceptPublish(boolean acceptPublish) {
        this.acceptPublish = acceptPublish;
    }

    public boolean isAcceptSubscribe() {
        return acceptSubscribe;
    }

    public void setAcceptSubscribe(boolean acceptSubscribe) {
        this.acceptSubscribe = acceptSubscribe;
    }

    public String getDeletePolicy() {
        return deletePolicy;
    }

    public void setDeletePolicy(String deletePolicy) {
        this.deletePolicy = deletePolicy;
    }

    public int getQryPriorityId() {
        return qryPriorityId;
    }

    public void setQryPriorityId(int qryPriorityId) {
        this.qryPriorityId = qryPriorityId;
    }

    public int getMaxMsgSizeInB() {
        return maxMsgSizeInB;
    }

    public void setMaxMsgSizeInB(int maxMsgSizeInB) {
        this.maxMsgSizeInB = maxMsgSizeInB;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public void setModifyInfo(String modifyUser, Date modifyDate) {
        this.configId = System.currentTimeMillis();
        this.modifyUser = modifyUser;
        this.modifyDate = modifyDate;
    }

    public String getModifyUser() {
        return modifyUser;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public void setDefDataPath(String dataPath) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_DATA_PATH, dataPath);
    }

    public String getDefDataPath() {
        return TStringUtils.getAttrValFrmAttributes(
                this.attributes, TStoreConstants.TOKEN_DATA_PATH);
    }

    public void setDefDataType(int dataType) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_DATA_TYPE, String.valueOf(dataType));
    }

    public int getDefDataType() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_DATA_TYPE);
        if (atrVal != null) {
            return Integer.parseInt(atrVal);
        }
        return TBaseConstants.META_VALUE_UNDEFINED;
    }

    public void setEnableGloFlowCtrl(Boolean enableGloFlowCtrl) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_ENABLE_FLOW_CTRL,
                        String.valueOf(enableGloFlowCtrl));
    }

    public Boolean getEnableGloFlowCtrl() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_ENABLE_FLOW_CTRL);
        if (atrVal != null) {
            return Boolean.parseBoolean(atrVal);
        }
        return null;
    }

    public void setGloFlowCtrlCnt(int flowCtrlCnt) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_FLOW_CTRL_CNT, String.valueOf(flowCtrlCnt));
    }

    public int getGloFlowCtrlCnt() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_FLOW_CTRL_CNT);
        if (atrVal != null) {
            return Integer.parseInt(atrVal);
        }
        return TBaseConstants.META_VALUE_UNDEFINED;
    }

    public void setGloFlowCtrlInfo(String flowCtrlInfo) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_FLOW_CTRL_INFO, flowCtrlInfo);
    }

    public String getGloFlowCtrlInfo() {
        return TStringUtils.getAttrValFrmAttributes(
                this.attributes, TStoreConstants.TOKEN_FLOW_CTRL_INFO);
    }

    public void setCreateInfo(String creater, Date createDate) {
        if (TStringUtils.isNotBlank(creater)) {
            this.attributes =
                    TStringUtils.setAttrValToAttributes(this.attributes,
                            TStoreConstants.TOKEN_CREATE_USER, creater);
        }
        if (createDate != null) {
            String dataStr = DateTimeConvertUtils.date2yyyyMMddHHmmss(createDate);
            this.attributes =
                    TStringUtils.setAttrValToAttributes(this.attributes,
                            TStoreConstants.TOKEN_CREATE_DATE, dataStr);
        }
    }

    public String getCreateUser() {
        return TStringUtils.getAttrValFrmAttributes(
                this.attributes, TStoreConstants.TOKEN_CREATE_USER);
    }

    public Date getCreateDate() {
        String dateStr = TStringUtils.getAttrValFrmAttributes(
                this.attributes, TStoreConstants.TOKEN_CREATE_DATE);
        return DateTimeConvertUtils.yyyyMMddHHmmss2date(dateStr);
    }

    public String getStrCreateDate() {
        return TStringUtils.getAttrValFrmAttributes(
                this.attributes, TStoreConstants.TOKEN_CREATE_DATE);
    }

    public String getStrModifyDate() {
        return DateTimeConvertUtils.date2yyyyMMddHHmmss(modifyDate);
    }

    /**
     * Serialize field to json format
     *
     * @param sBuilder
     * @return
     */
    public StringBuilder toJsonString(final StringBuilder sBuilder) {
        sBuilder.append("{\"type\":\"BdbClusterSettingEntity\",")
                .append("\"recordKey\":\"").append(recordKey).append("\"")
                .append(",\"configId\":").append(configId)
                .append(",\"brokerPort\":").append(brokerPort)
                .append(",\"brokerTLSPort\":").append(brokerTLSPort)
                .append(",\"brokerWebPort\":").append(brokerWebPort)
                .append(",\"numTopicStores\":").append(numTopicStores)
                .append(",\"numPartitions\":").append(numPartitions)
                .append(",\"unflushThreshold\":").append(unflushThreshold)
                .append(",\"unflushInterval\":").append(unflushInterval)
                .append(",\"unflushDataHold\":").append(unflushDataHold)
                .append(",\"memCacheMsgCntInK\":").append(memCacheMsgCntInK)
                .append(",\"memCacheFlushIntvl\":").append(memCacheFlushIntvl)
                .append(",\"memCacheMsgSizeInMB\":").append(memCacheMsgSizeInMB)
                .append(",\"acceptPublish\":").append(acceptPublish)
                .append(",\"acceptSubscribe\":").append(acceptSubscribe)
                .append(",\"deletePolicy\":\"").append(deletePolicy).append("\"")
                .append(",\"maxMsgSizeInMB\":");
        if (maxMsgSizeInB == TBaseConstants.META_VALUE_UNDEFINED) {
            sBuilder.append(maxMsgSizeInB);
        } else {
            sBuilder.append(maxMsgSizeInB / TBaseConstants.META_MB_UNIT_SIZE);
        }
        return sBuilder.append(",\"qryPriorityId\":").append(qryPriorityId)
                .append(",\"attributes\":\"").append(attributes).append("\"")
                .append(",\"createUser\":\"").append(getCreateUser()).append("\"")
                .append(",\"createDate\":\"").append(getStrCreateDate()).append("\"")
                .append(",\"modifyUser\":\"").append(modifyUser).append("\"")
                .append(",\"modifyDate\":\"")
                .append(getStrModifyDate())
                .append("\"}");
    }

    @Override
    public String toString() {
        ToStringBuilder sBuilder = new ToStringBuilder(this)
                .append("recordKey", recordKey)
                .append("configId", configId)
                .append("brokerPort", brokerPort)
                .append("brokerTLSPort", brokerTLSPort)
                .append("brokerWebPort", brokerWebPort)
                .append("numTopicStores", numTopicStores)
                .append("numPartitions", numPartitions)
                .append("unflushThreshold", unflushThreshold)
                .append("unflushInterval", unflushInterval)
                .append("unflushDataHold", unflushDataHold)
                .append("memCacheMsgCntInK", memCacheMsgCntInK)
                .append("memCacheFlushIntvl", memCacheFlushIntvl)
                .append("memCacheMsgSizeInMB", memCacheMsgSizeInMB)
                .append("acceptPublish", acceptPublish)
                .append("acceptSubscribe", acceptSubscribe)
                .append("deletePolicy", deletePolicy);
        if (maxMsgSizeInB == TBaseConstants.META_VALUE_UNDEFINED) {
            sBuilder.append("maxMsgSizeInMB", maxMsgSizeInB);
        } else {
            sBuilder.append("maxMsgSizeInMB",
                    maxMsgSizeInB / TBaseConstants.META_MB_UNIT_SIZE);
        }
        return sBuilder.append("qryPriorityId", qryPriorityId)
                .append("attributes", attributes)
                .append("createUser", getCreateUser())
                .append("createDate", getStrCreateDate())
                .append("modifyUser", modifyUser)
                .append("modifyDate", getStrModifyDate())
                .toString();
    }
}
