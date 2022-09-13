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

import java.util.Map;
import java.util.Objects;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.utils.SettingValidUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbClusterSettingEntity;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.TStoreConstants;

/*
 * store the cluster default setting
 *
 */
public class ClusterSettingEntity extends BaseEntity implements Cloneable {

    private final String recordKey =
            TStoreConstants.TOKEN_DEFAULT_CLUSTER_SETTING;
    // broker tcp port
    private int brokerPort = TBaseConstants.META_VALUE_UNDEFINED;
    // broker tls port
    private int brokerTLSPort = TBaseConstants.META_VALUE_UNDEFINED;
    // broker web port
    private int brokerWebPort = TBaseConstants.META_VALUE_UNDEFINED;
    private TopicPropGroup clsDefTopicProps = new TopicPropGroup();
    private int maxMsgSizeInB = TBaseConstants.META_VALUE_UNDEFINED;
    private int maxMsgSizeInMB = TBaseConstants.META_VALUE_UNDEFINED;
    private int qryPriorityId = TBaseConstants.META_VALUE_UNDEFINED;
    private EnableStatus gloFlowCtrlStatus = EnableStatus.STATUS_UNDEFINE;
    // flow control rule count
    private int gloFlowCtrlRuleCnt = TBaseConstants.META_VALUE_UNDEFINED;
    // flow control info
    private String gloFlowCtrlRuleInfo = "";

    public ClusterSettingEntity() {
        super();
    }

    public ClusterSettingEntity(BaseEntity opEntity) {
        super(opEntity);
    }

    /**
     * Constructor by BdbClusterSettingEntity
     *
     * @param bdbEntity  the BdbClusterSettingEntity initial object
     */
    public ClusterSettingEntity(BdbClusterSettingEntity bdbEntity) {
        super(bdbEntity.getConfigId(),
                bdbEntity.getCreateUser(), bdbEntity.getCreateDate(),
                bdbEntity.getModifyUser(), bdbEntity.getModifyDate());
        fillDefaultValue();
        TopicPropGroup defTopicProps =
                new TopicPropGroup(bdbEntity.getNumTopicStores(),
                        bdbEntity.getNumPartitions(), bdbEntity.getUnflushThreshold(),
                        bdbEntity.getUnflushInterval(), bdbEntity.getUnflushDataHold(),
                        bdbEntity.getMemCacheMsgSizeInMB(), bdbEntity.getMemCacheMsgCntInK(),
                        bdbEntity.getMemCacheFlushIntvl(), bdbEntity.isAcceptPublish(),
                        bdbEntity.isAcceptSubscribe(), bdbEntity.getDeletePolicy(),
                        bdbEntity.getDefDataType(), bdbEntity.getDefDataPath());
        updModifyInfo(bdbEntity.getConfigId(), bdbEntity.getBrokerPort(),
                bdbEntity.getBrokerTLSPort(), bdbEntity.getBrokerWebPort(),
                (bdbEntity.getMaxMsgSizeInB() / TBaseConstants.META_MB_UNIT_SIZE),
                bdbEntity.getQryPriorityId(), bdbEntity.getEnableGloFlowCtrl(),
                bdbEntity.getGloFlowCtrlCnt(), bdbEntity.getGloFlowCtrlInfo(),
                defTopicProps);
        setAttributes(bdbEntity.getAttributes());
    }

    /**
     * build bdb object from current info
     *
     * @return the BdbClusterSettingEntity object
     */
    public BdbClusterSettingEntity buildBdbClsDefSettingEntity() {
        BdbClusterSettingEntity bdbEntity =
                new BdbClusterSettingEntity(recordKey, getDataVerId(), brokerPort,
                        brokerTLSPort, brokerWebPort, clsDefTopicProps.getNumTopicStores(),
                        clsDefTopicProps.getNumPartitions(), clsDefTopicProps.getUnflushThreshold(),
                        clsDefTopicProps.getUnflushInterval(), clsDefTopicProps.getUnflushDataHold(),
                        clsDefTopicProps.getMemCacheMsgCntInK(), clsDefTopicProps.getMemCacheFlushIntvl(),
                        clsDefTopicProps.getMemCacheMsgSizeInMB(), clsDefTopicProps.isAcceptPublish(),
                        clsDefTopicProps.isAcceptSubscribe(), clsDefTopicProps.getDeletePolicy(),
                        this.qryPriorityId, this.maxMsgSizeInB, getAttributes(),
                        getModifyUser(), getModifyDate());
        if (TStringUtils.isNotBlank(clsDefTopicProps.getDataPath())) {
            bdbEntity.setDefDataPath(clsDefTopicProps.getDataPath());
        }
        bdbEntity.setCreateInfo(getCreateUser(), getCreateDate());
        bdbEntity.setDefDataType(clsDefTopicProps.getDataStoreType());
        bdbEntity.setEnableGloFlowCtrl(enableFlowCtrl());
        bdbEntity.setGloFlowCtrlCnt(gloFlowCtrlRuleCnt);
        if (TStringUtils.isNotBlank(gloFlowCtrlRuleInfo)) {
            bdbEntity.setGloFlowCtrlInfo(gloFlowCtrlRuleInfo);
        }
        return bdbEntity;
    }

    /**
     * fill fields with default value
     *
     * @return object
     */
    public ClusterSettingEntity fillDefaultValue() {
        this.brokerPort = TBaseConstants.META_DEFAULT_BROKER_PORT;
        this.brokerTLSPort = TBaseConstants.META_DEFAULT_BROKER_TLS_PORT;
        this.brokerWebPort = TBaseConstants.META_DEFAULT_BROKER_WEB_PORT;
        this.maxMsgSizeInMB = TBaseConstants.META_MIN_ALLOWED_MESSAGE_SIZE_MB;
        this.maxMsgSizeInB =
                SettingValidUtils.validAndXfeMaxMsgSizeFromMBtoB(this.maxMsgSizeInMB);
        this.qryPriorityId = TServerConstants.QRY_PRIORITY_DEF_VALUE;
        this.gloFlowCtrlStatus = EnableStatus.STATUS_DISABLE;
        this.gloFlowCtrlRuleCnt = 0;
        this.gloFlowCtrlRuleInfo = TServerConstants.BLANK_FLOWCTRL_RULES;
        this.clsDefTopicProps.fillDefaultValue();
        return this;
    }

    /**
     * update subclass field values
     *
     * @return if changed
     */
    public boolean updModifyInfo(long dataVerId, int brokerPort, int brokerTLSPort,
                                 int brokerWebPort, int maxMsgSizeMB,
                                 int qryPriorityId, Boolean flowCtrlEnable,
                                 int flowRuleCnt, String flowCtrlRuleInfo,
                                 TopicPropGroup defTopicProps) {
        boolean changed = false;
        // check and set dataVerId info
        if (dataVerId != TBaseConstants.META_VALUE_UNDEFINED
                && this.getDataVerId() != dataVerId) {
            changed = true;
            this.setDataVersionId(dataVerId);
        }
        if (brokerPort != TBaseConstants.META_VALUE_UNDEFINED
                && this.brokerPort != brokerPort) {
            changed = true;
            this.brokerPort = brokerPort;
        }
        if (brokerTLSPort != TBaseConstants.META_VALUE_UNDEFINED
                && this.brokerTLSPort != brokerTLSPort) {
            changed = true;
            this.brokerTLSPort = brokerTLSPort;
        }
        if (brokerWebPort != TBaseConstants.META_VALUE_UNDEFINED
                && this.brokerWebPort != brokerWebPort) {
            changed = true;
            this.brokerWebPort = brokerWebPort;
        }
        // check and set modified field
        if (maxMsgSizeMB != TBaseConstants.META_VALUE_UNDEFINED) {
            int tmpMaxMsgSizeInMB =
                    SettingValidUtils.validAndGetMsgSizeInMB(maxMsgSizeMB);
            if (this.maxMsgSizeInMB != tmpMaxMsgSizeInMB) {
                changed = true;
                this.maxMsgSizeInMB = tmpMaxMsgSizeInMB;
                this.maxMsgSizeInB =
                        SettingValidUtils.validAndXfeMaxMsgSizeFromMBtoB(tmpMaxMsgSizeInMB);
            }
        }
        // check and set qry priority id
        if (qryPriorityId != TBaseConstants.META_VALUE_UNDEFINED
                && this.qryPriorityId != qryPriorityId) {
            changed = true;
            this.qryPriorityId = qryPriorityId;
        }
        // check and set flowCtrl info
        if (flowCtrlEnable != null
                && (this.gloFlowCtrlStatus == EnableStatus.STATUS_UNDEFINE
                || this.gloFlowCtrlStatus.isEnable() != flowCtrlEnable)) {
            changed = true;
            setEnableFlowCtrl(flowCtrlEnable);
        }
        if (TStringUtils.isNotBlank(flowCtrlRuleInfo)
                && !flowCtrlRuleInfo.equals(gloFlowCtrlRuleInfo)) {
            changed = true;
            setGloFlowCtrlInfo(flowRuleCnt, flowCtrlRuleInfo);
        }
        // check and set clsDefTopicProps info
        if (defTopicProps != null
                && !defTopicProps.isDataEquals(clsDefTopicProps)) {
            if (clsDefTopicProps.updModifyInfo(defTopicProps)) {
                changed = true;
            }
        }
        if (changed) {
            updSerialId();
        }
        return changed;
    }

    public String getRecordKey() {
        return recordKey;
    }

    public int getBrokerPort() {
        return brokerPort;
    }

    public int getBrokerTLSPort() {
        return brokerTLSPort;
    }

    public int getBrokerWebPort() {
        return brokerWebPort;
    }

    public int getMaxMsgSizeInB() {
        return maxMsgSizeInB;
    }

    public int getMaxMsgSizeInMB() {
        return maxMsgSizeInMB;
    }

    public int getQryPriorityId() {
        return qryPriorityId;
    }

    public EnableStatus getGloFlowCtrlStatus() {
        return gloFlowCtrlStatus;
    }

    public int getGloFlowCtrlRuleCnt() {
        return gloFlowCtrlRuleCnt;
    }

    public String getGloFlowCtrlRuleInfo() {
        return gloFlowCtrlRuleInfo;
    }

    public boolean enableFlowCtrl() {
        return gloFlowCtrlStatus == EnableStatus.STATUS_ENABLE;
    }

    public TopicPropGroup getClsDefTopicProps() {
        return clsDefTopicProps;
    }

    /**
     * check if subclass fields is equals
     *
     * @param other  check object
     * @return if equals
     */
    public boolean isDataEquals(ClusterSettingEntity other) {
        return super.isDataEquals(other)
                && brokerPort == other.brokerPort
                && brokerTLSPort == other.brokerTLSPort
                && brokerWebPort == other.brokerWebPort
                && maxMsgSizeInB == other.maxMsgSizeInB
                && qryPriorityId == other.qryPriorityId
                && gloFlowCtrlRuleCnt == other.gloFlowCtrlRuleCnt
                && Objects.equals(clsDefTopicProps, other.clsDefTopicProps)
                && gloFlowCtrlStatus == other.gloFlowCtrlStatus
                && Objects.equals(gloFlowCtrlRuleInfo, other.gloFlowCtrlRuleInfo);
    }

    /**
     * Serialize field to json format
     *
     * @param sBuilder   build container
     * @param isLongName if return field key is long name
     * @param fullFormat if return full format json
     * @return  the serialized result
     */
    public StringBuilder toWebJsonStr(StringBuilder sBuilder,
                                      boolean isLongName,
                                      boolean fullFormat) {
        if (isLongName) {
            sBuilder.append("{\"brokerPort\":").append(brokerPort)
                    .append(",\"brokerTLSPort\":").append(brokerTLSPort)
                    .append(",\"brokerWebPort\":").append(brokerWebPort)
                    .append(",\"maxMsgSizeInMB\":").append(maxMsgSizeInMB)
                    .append(",\"qryPriorityId\":").append(qryPriorityId)
                    .append(",\"flowCtrlEnable\":").append(gloFlowCtrlStatus.isEnable())
                    .append(",\"flowCtrlRuleCount\":").append(gloFlowCtrlRuleCnt)
                    .append(",\"flowCtrlInfo\":").append(gloFlowCtrlRuleInfo);
        } else {
            sBuilder.append("{\"bPort\":").append(brokerPort)
                    .append(",\"bTlsPort\":").append(brokerTLSPort)
                    .append(",\"bWebPort\":").append(brokerWebPort)
                    .append(",\"mxMsgInMB\":").append(maxMsgSizeInMB)
                    .append(",\"qryPriId\":").append(qryPriorityId)
                    .append(",\"fCtrlEn\":").append(gloFlowCtrlStatus.isEnable())
                    .append(",\"fCtrlCnt\":").append(gloFlowCtrlRuleCnt)
                    .append(",\"fCtrlInfo\":").append(gloFlowCtrlRuleInfo);
        }
        clsDefTopicProps.toWebJsonStr(sBuilder, isLongName);
        super.toWebJsonStr(sBuilder, isLongName);
        if (fullFormat) {
            sBuilder.append("}");
        }
        return sBuilder;
    }

    /**
     * Get field value to key and value format.
     *
     * @param paramMap   output map
     * @param isLongName if return field key is long name
     */
    public void getConfigureInfo(Map<String, String> paramMap,
                                 boolean isLongName) {
        if (brokerPort != TBaseConstants.META_VALUE_UNDEFINED) {
            paramMap.put((isLongName ? "brokerPort" : "bPort"),
                    String.valueOf(brokerPort));
        }
        if (brokerTLSPort != TBaseConstants.META_VALUE_UNDEFINED) {
            paramMap.put((isLongName ? "brokerTLSPort" : "bTlsPort"),
                    String.valueOf(brokerTLSPort));
        }
        if (brokerWebPort != TBaseConstants.META_VALUE_UNDEFINED) {
            paramMap.put((isLongName ? "brokerWebPort" : "bWebPort"),
                    String.valueOf(brokerWebPort));
        }
        if (maxMsgSizeInMB != TBaseConstants.META_VALUE_UNDEFINED) {
            paramMap.put((isLongName ? "maxMsgSizeInMB" : "mxMsgInMB"),
                    String.valueOf(maxMsgSizeInMB));
        }
        if (qryPriorityId != TBaseConstants.META_VALUE_UNDEFINED) {
            paramMap.put((isLongName ? "qryPriorityId" : "qryPriId"),
                    String.valueOf(qryPriorityId));
        }
        if (gloFlowCtrlStatus != EnableStatus.STATUS_UNDEFINE) {
            paramMap.put((isLongName ? "flowCtrlEnable" : "fCtrlEn"),
                    String.valueOf(gloFlowCtrlStatus.isEnable()));
        }
        if (gloFlowCtrlRuleCnt != TBaseConstants.META_VALUE_UNDEFINED) {
            paramMap.put((isLongName ? "flowCtrlRuleCount" : "fCtrlCnt"),
                    String.valueOf(gloFlowCtrlRuleCnt));
        }
        if (TStringUtils.isNotBlank(gloFlowCtrlRuleInfo)) {
            paramMap.put((isLongName ? "flowCtrlInfo" : "fCtrlInfo"), gloFlowCtrlRuleInfo);
        }
        clsDefTopicProps.getConfigureInfo(paramMap, isLongName);
        super.getConfigureInfo(paramMap, isLongName);
    }

    /**
     * Serialize field to old version json format
     *
     * @param sBuilder   build container
     * @param isLongName if return field key is long name
     * @return           the build result
     */
    public StringBuilder toOldVerFlowCtrlWebJsonStr(StringBuilder sBuilder,
                                                    boolean isLongName) {
        int statusId = gloFlowCtrlStatus.isEnable() ? 1 : 0;
        if (isLongName) {
            sBuilder.append("{\"statusId\":").append(statusId)
                    .append(",\"maxMsgSizeInMB\":").append(maxMsgSizeInMB)
                    .append(",\"qryPriorityId\":").append(qryPriorityId)
                    .append(",\"ruleCnt\":").append(gloFlowCtrlRuleCnt)
                    .append(",\"flowCtrlInfo\":").append(gloFlowCtrlRuleInfo);
        } else {
            sBuilder.append("{\"statusId\":").append(statusId)
                    .append(",\"mxMsgInMB\":").append(maxMsgSizeInMB)
                    .append(",\"qryPriId\":").append(qryPriorityId)
                    .append(",\"fCtrlCnt\":").append(gloFlowCtrlRuleCnt)
                    .append(",\"fCtrlInfo\":").append(gloFlowCtrlRuleInfo);
        }
        super.toWebJsonStr(sBuilder, isLongName);
        sBuilder.append("}");
        return sBuilder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ClusterSettingEntity)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ClusterSettingEntity entity = (ClusterSettingEntity) o;
        return isDataEquals(entity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), recordKey, brokerPort, brokerTLSPort,
                brokerWebPort, clsDefTopicProps, maxMsgSizeInB, qryPriorityId,
                gloFlowCtrlStatus, gloFlowCtrlRuleCnt, gloFlowCtrlRuleInfo);
    }

    @Override
    public ClusterSettingEntity clone() {
        ClusterSettingEntity copy = (ClusterSettingEntity) super.clone();
        copy.setGloFlowCtrlStatus(getGloFlowCtrlStatus());
        copy.setClsDefTopicProps(getClsDefTopicProps().clone());
        return copy;
    }

    private void setGloFlowCtrlStatus(EnableStatus gloFlowCtrlStatus) {
        this.gloFlowCtrlStatus = gloFlowCtrlStatus;
    }

    private void setGloFlowCtrlInfo(int flowCtrlCnt, String flowCtrlInfo) {
        this.gloFlowCtrlRuleCnt = flowCtrlCnt;
        this.gloFlowCtrlRuleInfo = flowCtrlInfo;
    }

    private void setEnableFlowCtrl(boolean enableFlowCtrl) {
        if (enableFlowCtrl) {
            this.gloFlowCtrlStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.gloFlowCtrlStatus = EnableStatus.STATUS_DISABLE;
        }
    }

    private void setClsDefTopicProps(TopicPropGroup clsDefTopicProps) {
        this.clsDefTopicProps = clsDefTopicProps;
    }
}
