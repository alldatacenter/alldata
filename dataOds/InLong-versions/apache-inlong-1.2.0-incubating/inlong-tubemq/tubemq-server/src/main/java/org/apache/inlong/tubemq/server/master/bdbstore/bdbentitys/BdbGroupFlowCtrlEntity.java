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
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.TStoreConstants;

@Entity
public class BdbGroupFlowCtrlEntity implements Serializable {
    private static final long serialVersionUID = 2533735122504168321L;
    @PrimaryKey
    private String groupName;           //group name
    private long serialId = -1L;        //serial id
    private int statusId = -1;         // 0:not active; 1: active
    private String flowCtrlInfo;
    private int ruleCnt = 0;            //flow control rule count
    private long ssdTranslateId = System.currentTimeMillis();
    private boolean needSSDProc = false;    //ssd
    private String attributes;          //extra attributes
    // ** Based on the data compatibility consideration of the original version:
    //     the creation information in this example is the last modified information,
    //     and the modified information is the creation information
    private String createUser;          //create user
    private Date createDate;            //create date

    public BdbGroupFlowCtrlEntity() {

    }

    //Constructor
    public BdbGroupFlowCtrlEntity(final String flowCtrlInfo, final int statusId,
                                  final int ruleCnt, final int qryPriorityId,
                                  final String attributes, final boolean curNeedSSDProc,
                                  final String modifyUser, final Date modifyDate) {
        this.statusId = statusId;
        this.groupName = TServerConstants.TOKEN_DEFAULT_FLOW_CONTROL;
        this.serialId = System.currentTimeMillis();
        this.flowCtrlInfo = flowCtrlInfo;
        this.attributes = attributes;
        this.ruleCnt = ruleCnt;
        this.ssdTranslateId = System.currentTimeMillis();
        this.needSSDProc = curNeedSSDProc;
        this.createUser = modifyUser;
        this.createDate = modifyDate;
        this.setQryPriorityId(qryPriorityId);
    }

    //Constructor
    public BdbGroupFlowCtrlEntity(final String groupName, final String flowCtrlInfo,
                                  final int statusId, final int ruleCnt,
                                  final int qryPriorityId, final String attributes,
                                  final boolean needSSDProc, final String modifyUser,
                                  final Date modifyDate) {
        this.groupName = groupName;
        this.serialId = System.currentTimeMillis();
        this.statusId = statusId;
        this.flowCtrlInfo = flowCtrlInfo;
        this.attributes = attributes;
        this.ruleCnt = ruleCnt;
        this.createUser = modifyUser;
        this.createDate = modifyDate;
        this.needSSDProc = needSSDProc;
        this.ssdTranslateId = TBaseConstants.META_VALUE_UNDEFINED;
        this.setQryPriorityId(qryPriorityId);
    }

    //Constructor
    public BdbGroupFlowCtrlEntity(final String groupName, final String flowCtrlInfo,
                                  final int statusId, final int ruleCnt,
                                  final String attributes, final long ssdTranslateId,
                                  final boolean needSSDProc, final String modifyUser,
                                  final Date modifyDate) {
        this.groupName = groupName;
        this.serialId = System.currentTimeMillis();
        this.statusId = statusId;
        this.flowCtrlInfo = flowCtrlInfo;
        this.attributes = attributes;
        this.ruleCnt = ruleCnt;
        this.createUser = modifyUser;
        this.createDate = modifyDate;
        this.needSSDProc = needSSDProc;
        this.ssdTranslateId = ssdTranslateId;
    }

    //Constructor
    public BdbGroupFlowCtrlEntity(long serialId, String groupName, String flowCtrlInfo,
                                  int statusId, int ruleCnt, int qryPriorityId,
                                  String attributes, String modifyUser,
                                  Date modifyDate) {
        this.groupName = groupName;
        this.serialId = serialId;
        this.statusId = statusId;
        this.flowCtrlInfo = flowCtrlInfo;
        this.attributes = attributes;
        this.ruleCnt = ruleCnt;
        this.createUser = modifyUser;
        this.createDate = modifyDate;
        this.needSSDProc = false;
        this.ssdTranslateId = TBaseConstants.META_VALUE_UNDEFINED;
        this.setQryPriorityId(qryPriorityId);

    }

    public long getSsdTranslateId() {
        return ssdTranslateId;
    }

    public int getRuleCnt() {
        return ruleCnt;
    }

    public void setRuleCnt(int ruleCnt) {
        this.ruleCnt = ruleCnt;
    }

    public long getSerialId() {
        return serialId;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getModifyUser() {
        return createUser;
    }

    public boolean isNeedSSDProc() {
        return needSSDProc;
    }

    public void setNeedSSDProc(boolean needSSDProc) {
        this.needSSDProc = needSSDProc;
    }

    public Date getModifyDate() {
        return createDate;
    }

    public String getFlowCtrlInfo() {
        return flowCtrlInfo;
    }

    public void setFlowCtrlInfo(int ruleCnt, String flowCtrlInfo) {
        this.ruleCnt = ruleCnt;
        this.flowCtrlInfo = flowCtrlInfo;
        this.serialId = System.currentTimeMillis();
    }

    public int getStatusId() {
        return statusId;
    }

    public void setStatusId(int statusId) {
        this.statusId = statusId;
        this.serialId = System.currentTimeMillis();
    }

    public boolean isValidStatus() {
        return (statusId != 0);
    }

    public int getQryPriorityId() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_QRY_PRIORITY_ID);
        if (atrVal != null) {
            return Integer.parseInt(atrVal);
        }
        return 0;
    }

    public void setQryPriorityId(int qryPriorityId) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_QRY_PRIORITY_ID,
                        String.valueOf(qryPriorityId));
    }

    public EnableStatus getResCheckStatus() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_RES_CHECK_STATUS);
        if (atrVal != null) {
            return EnableStatus.valueOf(Integer.parseInt(atrVal));
        }
        return EnableStatus.STATUS_UNDEFINE;
    }

    public void setResCheckStatus(EnableStatus resCheckStatus) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_RES_CHECK_STATUS,
                        String.valueOf(resCheckStatus.getCode()));
    }

    public int getAllowedBrokerClientRate() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_BROKER_CLIENT_RATE);
        if (atrVal != null) {
            return Integer.parseInt(atrVal);
        }
        return TBaseConstants.META_VALUE_UNDEFINED;
    }

    public void setAllowedBrokerClientRate(int allowedBrokerClientRate) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_BROKER_CLIENT_RATE,
                        String.valueOf(allowedBrokerClientRate));
    }

    public void setCreateInfo(String createUser, Date createDate) {
        if (TStringUtils.isNotBlank(createUser)) {
            this.attributes =
                    TStringUtils.setAttrValToAttributes(this.attributes,
                            TStoreConstants.TOKEN_CREATE_USER, createUser);
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

    public String getStrModifyDate() {
        return DateTimeConvertUtils.date2yyyyMMddHHmmss(createDate);
    }

    public String getStrCreateDate() {
        return TStringUtils.getAttrValFrmAttributes(
                this.attributes, TStoreConstants.TOKEN_CREATE_DATE);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("groupName", groupName)
                .append("serialId", serialId)
                .append("statusId", statusId)
                .append("flowCtrlInfo", ".....")
                .append("ruleCnt", ruleCnt)
                .append("ssdTranslateId", ssdTranslateId)
                .append("needSSDProc", needSSDProc)
                .append("attributes", attributes)
                .append("createUser", getCreateUser())
                .append("createDate", getCreateUser())
                .append("modifyUser", createUser)
                .append("modifyDate", getStrModifyDate())
                .toString();
    }

    /**
     * Serialize config field to json format
     *
     * @param sBuilder
     * @return
     */
    public StringBuilder toJsonString(final StringBuilder sBuilder) {
        return sBuilder.append("{\"type\":\"BdbGroupFlowCtrlEntity\",")
                .append("\"groupName\":\"").append(groupName)
                .append("\",\"statusId\":").append(statusId)
                .append(",\"ssdTranslateId\":").append(ssdTranslateId)
                .append(",\"ruleCnt\":").append(ruleCnt)
                .append(",\"needSSDProc\":").append(needSSDProc)
                .append(",\"serialId\":").append(serialId)
                .append(",\"qryPriorityId\":").append(getQryPriorityId())
                .append(",\"flowCtrlInfo\":").append(flowCtrlInfo)
                .append(", \"attributes\":\"").append(attributes)
                .append(",\"createUser\":\"").append(getCreateUser())
                .append("\",\"createDate\":\"").append(getStrCreateDate())
                .append("\",\"modifyUser\":\"").append(createUser)
                .append("\",\"modifyDate\":\"").append(getStrModifyDate())
                .append("\"}");
    }
}
