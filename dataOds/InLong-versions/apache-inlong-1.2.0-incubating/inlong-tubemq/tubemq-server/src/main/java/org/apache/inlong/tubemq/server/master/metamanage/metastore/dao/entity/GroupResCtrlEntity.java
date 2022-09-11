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
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbGroupFlowCtrlEntity;

/*
 * store the group resource control setting
 *
 */
public class GroupResCtrlEntity extends BaseEntity implements Cloneable {
    // group name
    private String groupName = "";
    // resource check control
    private EnableStatus resCheckStatus = EnableStatus.STATUS_UNDEFINE;
    private int allowedBrokerClientRate = TBaseConstants.META_VALUE_UNDEFINED;
    // consume priority id
    private int qryPriorityId = TBaseConstants.META_VALUE_UNDEFINED;
    // consume group flow control info
    private EnableStatus flowCtrlStatus = EnableStatus.STATUS_UNDEFINE;
    private int ruleCnt = 0;           // flow control rule count
    private String flowCtrlInfo = "";  // flow control info

    // only for query
    public GroupResCtrlEntity() {
        super();
    }

    public GroupResCtrlEntity(BaseEntity opEntity, String groupName) {
        super(opEntity);
        this.groupName = groupName;
    }

    /**
     * Constructor by BdbGroupFlowCtrlEntity
     *
     * @param bdbEntity  the BdbGroupFlowCtrlEntity initial object
     */
    public GroupResCtrlEntity(BdbGroupFlowCtrlEntity bdbEntity) {
        super(bdbEntity.getSerialId(),
                bdbEntity.getModifyUser(), bdbEntity.getModifyDate());
        setCreateInfo(bdbEntity.getCreateUser(), bdbEntity.getCreateDate());
        this.groupName = bdbEntity.getGroupName();
        this.qryPriorityId = bdbEntity.getQryPriorityId();
        this.ruleCnt = bdbEntity.getRuleCnt();
        this.flowCtrlInfo = bdbEntity.getFlowCtrlInfo();
        if (bdbEntity.getStatusId() != 0) {
            this.flowCtrlStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.flowCtrlStatus = EnableStatus.STATUS_DISABLE;
        }
        this.resCheckStatus = bdbEntity.getResCheckStatus();
        this.allowedBrokerClientRate = bdbEntity.getAllowedBrokerClientRate();
        setAttributes(bdbEntity.getAttributes());
    }

    /**
     * build bdb object from current info
     *
     * @return the BdbGroupFlowCtrlEntity object
     */
    public BdbGroupFlowCtrlEntity buildBdbGroupFlowCtrlEntity() {
        //Constructor
        int statusId = (this.flowCtrlStatus == EnableStatus.STATUS_ENABLE) ? 1 : 0;
        BdbGroupFlowCtrlEntity bdbEntity =
                new BdbGroupFlowCtrlEntity(getDataVerId(), this.groupName,
                        this.flowCtrlInfo, statusId, this.ruleCnt, this.qryPriorityId,
                        getAttributes(), getModifyUser(), getModifyDate());
        bdbEntity.setCreateInfo(getCreateUser(), getCreateDate());
        bdbEntity.setResCheckStatus(resCheckStatus);
        bdbEntity.setAllowedBrokerClientRate(allowedBrokerClientRate);
        return bdbEntity;
    }

    /**
     * fill fields with default value
     *
     * @return object
     */
    public GroupResCtrlEntity fillDefaultValue() {
        this.resCheckStatus = EnableStatus.STATUS_DISABLE;
        this.allowedBrokerClientRate = 0;
        this.qryPriorityId = TServerConstants.QRY_PRIORITY_DEF_VALUE;
        this.flowCtrlStatus = EnableStatus.STATUS_DISABLE;
        this.ruleCnt = 0;
        this.flowCtrlInfo = TServerConstants.BLANK_FLOWCTRL_RULES;
        return this;
    }

    public GroupResCtrlEntity setGroupName(String groupName) {
        this.groupName = groupName;
        return this;
    }

    public String getGroupName() {
        return groupName;
    }

    public int getRuleCnt() {
        return ruleCnt;
    }

    public String getFlowCtrlInfo() {
        return flowCtrlInfo;
    }

    public int getQryPriorityId() {
        return qryPriorityId;
    }

    public void setQryPriorityId(int qryPriorityId) {
        this.qryPriorityId = qryPriorityId;
    }

    public boolean isFlowCtrlEnable() {
        return (this.flowCtrlStatus == EnableStatus.STATUS_ENABLE);
    }

    public boolean isEnableResCheck() {
        return resCheckStatus == EnableStatus.STATUS_ENABLE;
    }

    public int getAllowedBrokerClientRate() {
        return allowedBrokerClientRate;
    }

    public void setAllowedBrokerClientRate(int allowedBrokerClientRate) {
        this.allowedBrokerClientRate = allowedBrokerClientRate;
    }

    public EnableStatus getResCheckStatus() {
        return resCheckStatus;
    }

    public EnableStatus getFlowCtrlStatus() {
        return flowCtrlStatus;
    }

    public void setResCheckStatus(EnableStatus resCheckStatus) {
        this.resCheckStatus = resCheckStatus;
    }

    private void setResCheckStatus(boolean enableResChk) {
        if (enableResChk) {
            this.resCheckStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.resCheckStatus = EnableStatus.STATUS_DISABLE;
        }
    }

    public void setFlowCtrlStatus(EnableStatus flowCtrlStatus) {
        this.flowCtrlStatus = flowCtrlStatus;
    }

    private void setFlowCtrlStatus(boolean enableFlowCtrl) {
        if (enableFlowCtrl) {
            this.flowCtrlStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.flowCtrlStatus = EnableStatus.STATUS_DISABLE;
        }
    }

    private void setFlowCtrlRule(int ruleCnt, String flowCtrlInfo) {
        this.ruleCnt = ruleCnt;
        this.flowCtrlInfo = flowCtrlInfo;
    }

    /**
     * update subclass field values
     *
     * @param dataVerId          new data version id
     * @param resChkEnable       new resource check status
     * @param allowedB2CRate     new B2C rate value
     * @param qryPriorityId      new qryPriorityId value
     * @param flowCtrlEnable     new flow control enable status
     * @param flowRuleCnt        new flow control rule count
     * @param flowCtrlRuleInfo   new flow control rule information
     *
     * @return  whether changed
     */
    public boolean updModifyInfo(long dataVerId,
                                 Boolean resChkEnable, int allowedB2CRate,
                                 int qryPriorityId, Boolean flowCtrlEnable,
                                 int flowRuleCnt, String flowCtrlRuleInfo) {
        boolean changed = false;
        // check and set dataVerId info
        if (dataVerId != TBaseConstants.META_VALUE_UNDEFINED
                && this.getDataVerId() != dataVerId) {
            changed = true;
            this.setDataVersionId(dataVerId);
        }
        // check and set resCheckStatus info
        if (resChkEnable != null
                && (this.resCheckStatus == EnableStatus.STATUS_UNDEFINE
                || this.resCheckStatus.isEnable() != resChkEnable)) {
            changed = true;
            setResCheckStatus(resChkEnable);
        }
        // check and set allowed broker client rate
        if (allowedB2CRate != TBaseConstants.META_VALUE_UNDEFINED
                && this.allowedBrokerClientRate != allowedB2CRate) {
            changed = true;
            this.allowedBrokerClientRate = allowedB2CRate;
        }
        // check and set qry priority id
        if (qryPriorityId != TBaseConstants.META_VALUE_UNDEFINED
                && this.qryPriorityId != qryPriorityId) {
            changed = true;
            this.qryPriorityId = qryPriorityId;
        }
        // check and set flowCtrl info
        if (flowCtrlEnable != null
                && (this.flowCtrlStatus == EnableStatus.STATUS_UNDEFINE
                || this.flowCtrlStatus.isEnable() != flowCtrlEnable)) {
            changed = true;
            setFlowCtrlStatus(flowCtrlEnable);
        }
        // check and set flowCtrlInfo info
        if (TStringUtils.isNotBlank(flowCtrlRuleInfo)
                && !flowCtrlRuleInfo.equals(flowCtrlInfo)) {
            changed = true;
            setFlowCtrlRule(flowRuleCnt, flowCtrlRuleInfo);
        }
        if (changed) {
            updSerialId();
        }
        return changed;
    }

    /**
     * Check whether the specified query item value matches
     * Allowed query items:
     *   groupName, qryPriorityId, resCheckStatus,
     *   flowCtrlStatus, allowedBrokerClientRate
     * @return true: matched, false: not match
     */
    public boolean isMatched(GroupResCtrlEntity target) {
        if (target == null) {
            return true;
        }
        if (!super.isMatched(target)) {
            return false;
        }
        return (target.getQryPriorityId() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getQryPriorityId() == this.qryPriorityId)
                && (TStringUtils.isBlank(target.getGroupName())
                || target.getGroupName().equals(this.groupName))
                && (target.getResCheckStatus() == EnableStatus.STATUS_UNDEFINE
                || target.getResCheckStatus() == this.resCheckStatus)
                && (target.getFlowCtrlStatus() == EnableStatus.STATUS_UNDEFINE
                || target.getFlowCtrlStatus() == this.flowCtrlStatus)
                && (target.getAllowedBrokerClientRate() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getAllowedBrokerClientRate() == this.allowedBrokerClientRate);
    }

    /**
     * Serialize field to json format
     *
     * @param sBuffer   build container
     * @param isLongName if return field key is long name
     * @param fullFormat if return full format json
     * @return    process result
     */
    public StringBuilder toWebJsonStr(StringBuilder sBuffer,
                                      boolean isLongName,
                                      boolean fullFormat) {
        if (isLongName) {
            sBuffer.append("{\"groupName\":\"").append(groupName).append("\"")
                    .append(",\"resCheckEnable\":").append(resCheckStatus.isEnable())
                    .append(",\"alwdBrokerClientRate\":").append(allowedBrokerClientRate)
                    .append(",\"qryPriorityId\":").append(qryPriorityId)
                    .append(",\"flowCtrlEnable\":").append(flowCtrlStatus.isEnable())
                    .append(",\"flowCtrlRuleCount\":").append(ruleCnt)
                    .append(",\"flowCtrlInfo\":").append(flowCtrlInfo);
        } else {
            sBuffer.append("{\"group\":\"").append(groupName).append("\"")
                    .append(",\"resChkEn\":").append(resCheckStatus.isEnable())
                    .append(",\"abcr\":").append(allowedBrokerClientRate)
                    .append(",\"qryPriId\":").append(qryPriorityId)
                    .append(",\"fCtrlEn\":").append(flowCtrlStatus.isEnable())
                    .append(",\"fCtrlCnt\":").append(ruleCnt)
                    .append(",\"fCtrlInfo\":").append(flowCtrlInfo);
        }
        super.toWebJsonStr(sBuffer, isLongName);
        if (fullFormat) {
            sBuffer.append("}");
        }
        return sBuffer;
    }

    /**
     * Serialize field to old version json format
     *
     * @param sBuffer   build container
     * @param isLongName if return field key is long name
     * @return   process result
     */
    public StringBuilder toOldVerFlowCtrlWebJsonStr(StringBuilder sBuffer,
                                                    boolean isLongName) {
        int statusId = flowCtrlStatus.isEnable() ? 1 : 0;
        if (isLongName) {
            sBuffer.append("{\"groupName\":\"").append(groupName)
                    .append("\",\"statusId\":").append(statusId)
                    .append(",\"qryPriorityId\":").append(qryPriorityId)
                    .append(",\"ruleCnt\":").append(ruleCnt)
                    .append(",\"flowCtrlInfo\":").append(flowCtrlInfo);
        } else {
            sBuffer.append("{\"group\":\"").append(groupName)
                    .append("\",\"statusId\":").append(statusId)
                    .append(",\"qryPriId\":").append(qryPriorityId)
                    .append(",\"fCtrlCnt\":").append(ruleCnt)
                    .append(",\"fCtrlInfo\":").append(flowCtrlInfo);
        }
        super.toWebJsonStr(sBuffer, isLongName);
        sBuffer.append("}");
        return sBuffer;
    }

    /**
     * check if subclass fields is equals
     *
     * @param other  check object
     * @return if equals
     */
    public boolean isDataEquals(GroupResCtrlEntity other) {
        return super.isDataEquals(other)
                && allowedBrokerClientRate == other.allowedBrokerClientRate
                && qryPriorityId == other.qryPriorityId
                && ruleCnt == other.ruleCnt
                && groupName.equals(other.groupName)
                && resCheckStatus == other.resCheckStatus
                && flowCtrlStatus == other.flowCtrlStatus
                && Objects.equals(flowCtrlInfo, other.flowCtrlInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupResCtrlEntity)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        GroupResCtrlEntity that = (GroupResCtrlEntity) o;
        return isDataEquals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), groupName,
                resCheckStatus, allowedBrokerClientRate,
                qryPriorityId, flowCtrlStatus, ruleCnt, flowCtrlInfo);
    }

    @Override
    public GroupResCtrlEntity clone() {
        GroupResCtrlEntity copy = (GroupResCtrlEntity) super.clone();
        copy.setFlowCtrlStatus(getFlowCtrlStatus());
        copy.setResCheckStatus(getResCheckStatus());
        return copy;
    }

}
