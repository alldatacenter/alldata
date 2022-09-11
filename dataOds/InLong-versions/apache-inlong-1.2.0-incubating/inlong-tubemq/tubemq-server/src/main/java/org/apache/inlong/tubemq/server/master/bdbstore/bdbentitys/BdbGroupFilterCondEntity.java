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
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.TStoreConstants;

@Entity
public class BdbGroupFilterCondEntity implements Serializable {
    private static final long serialVersionUID = 5305233169489425210L;

    @PrimaryKey
    private String recordKey;
    private String topicName;
    private String consumerGroupName;
    private int controlStatus = -2;   // -2: undefine; 0: not started; 1:started, not limited; 2: started, limited
    private String attributes;
    // ** Based on the data compatibility consideration of the original version:
    //     the creation information in this example is the last modified information,
    //     and the modified information is the creation information
    private String createUser;
    private Date createDate;

    public BdbGroupFilterCondEntity() {

    }

    public BdbGroupFilterCondEntity(String topicName, String consumerGroupName,
                                    int controlStatus, String filterCondStr,
                                    String modifyUser, Date modifyDate) {
        this.recordKey =
                new StringBuilder(512)
                        .append(topicName)
                        .append(TokenConstants.ATTR_SEP)
                        .append(consumerGroupName).toString();
        this.topicName = topicName;
        this.consumerGroupName = consumerGroupName;
        this.controlStatus = controlStatus;
        setFilterCondStr(filterCondStr);
        this.createUser = modifyUser;
        this.createDate = modifyDate;
    }

    public BdbGroupFilterCondEntity(String topicName, String consumerGroupName,
                                    int controlStatus, String filterCondStr,
                                    String attributes, String modifyUser, Date modifyDate) {
        this.recordKey =
                new StringBuilder(512)
                        .append(topicName)
                        .append(TokenConstants.ATTR_SEP)
                        .append(consumerGroupName).toString();
        this.topicName = topicName;
        this.consumerGroupName = consumerGroupName;
        this.controlStatus = controlStatus;
        this.createUser = modifyUser;
        this.createDate = modifyDate;
        this.attributes = attributes;
        setFilterCondStr(filterCondStr);
    }

    public String getFilterCondStr() {
        if (TStringUtils.isNotBlank(attributes)
                && attributes.contains(TokenConstants.EQ)) {
            return TStringUtils.getAttrValFrmAttributes(
                    this.attributes, TStoreConstants.TOKEN_FILTER_COND_STR);
        } else {
            return attributes;
        }
    }

    public void setFilterCondStr(String filterCondStr) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_FILTER_COND_STR, filterCondStr);
    }

    public EnableStatus getConsumeEnable() {
        String atrVal =
                TStringUtils.getAttrValFrmAttributes(this.attributes,
                        TStoreConstants.TOKEN_ENABLE_CONSUME);
        if (atrVal != null) {
            return EnableStatus.valueOf(Integer.parseInt(atrVal));
        }
        return EnableStatus.STATUS_ENABLE;
    }

    public void setConsumeEnable(EnableStatus enableConsume) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_ENABLE_CONSUME,
                        String.valueOf(enableConsume.getCode()));
    }

    public String getDisableConsumeReason() {
        if (TStringUtils.isNotBlank(attributes)
                && attributes.contains(TokenConstants.EQ)) {
            return TStringUtils.getAttrValFrmAttributes(
                    this.attributes, TStoreConstants.TOKEN_BLK_REASON);
        } else {
            return "";
        }
    }

    public void setDisableConsumeReason(String disableConsumeReason) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_BLK_REASON, disableConsumeReason);
    }

    public String getRecordKey() {
        return recordKey;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getConsumerGroupName() {
        return consumerGroupName;
    }

    public void setConsumerGroupName(String consumerGroupName) {
        this.consumerGroupName = consumerGroupName;
    }

    public int getControlStatus() {
        return controlStatus;
    }

    public void setControlStatus(int controlStatus) {
        this.controlStatus = controlStatus;
    }

    public String getModifyUser() {
        return createUser;
    }

    public Date getModifyDate() {
        return createDate;
    }

    public String getAttributes() {
        if (TStringUtils.isNotBlank(attributes)
                && !attributes.contains(TokenConstants.EQ)) {
            return attributes;
        } else {
            return "";
        }
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public long getDataVerId() {
        if (TStringUtils.isNotBlank(attributes)
                && attributes.contains(TokenConstants.EQ)) {
            String atrVal =
                    TStringUtils.getAttrValFrmAttributes(this.attributes,
                            TStoreConstants.TOKEN_DATA_VERSION_ID);
            if (atrVal != null) {
                return Long.parseLong(atrVal);
            }
        }
        return TBaseConstants.META_VALUE_UNDEFINED;
    }

    public void setDataVerId(long dataVerId) {
        if (TStringUtils.isNotBlank(attributes)
                && !attributes.contains(TokenConstants.EQ)) {
            setFilterCondStr(attributes);
        }
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes,
                        TStoreConstants.TOKEN_DATA_VERSION_ID,
                        String.valueOf(dataVerId));
    }

    // for
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
                .append("recordKey", recordKey)
                .append("topicName", topicName)
                .append("consumerGroupName", consumerGroupName)
                .append("controlStatus", controlStatus)
                .append("attributes", attributes)
                .append("createUser", getCreateUser())
                .append("createDate", getStrCreateDate())
                .append("modifyUser", createUser)
                .append("modifyDate", getStrModifyDate())
                .toString();
    }

    public StringBuilder toJsonString(final StringBuilder sBuilder) {
        return sBuilder.append("{\"type\":\"BdbGroupFilterCondEntity\",")
                .append("\"recordKey\":\"").append(recordKey)
                .append("\",\"topicName\":\"").append(topicName)
                .append("\",\"consumerGroupName\":\"").append(consumerGroupName)
                .append("\",\"filterConds\":\"").append(attributes)
                .append("\",\"condStatus\":").append(controlStatus)
                .append(",\"createUser\":\"").append(getCreateUser())
                .append("\",\"createDate\":\"").append(getStrCreateDate())
                .append("\",\"modifyUser\":\"").append(createUser)
                .append("\",\"modifyDate\":\"").append(getStrModifyDate())
                .append("\"}");
    }
}
