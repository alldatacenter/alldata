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

package org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.io.Serializable;
import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.master.metamanage.metastore.TStoreConstants;

@Entity
public class BdbTopicAuthControlEntity implements Serializable {

    private static final long serialVersionUID = 7356175918639562340L;
    @PrimaryKey
    private String topicName;
    private int enableAuthControl = -1; // -1 : undefine; 0: disable, 1: enable
    // ** Based on the data compatibility consideration of the original version:
    // the creation information in this example is the last modified information,
    // and the modified information is the creation information
    private String createUser;
    private Date createDate;
    private String attributes;

    public BdbTopicAuthControlEntity() {

    }

    public BdbTopicAuthControlEntity(String topicName, boolean enableAuthControl,
            String modifyUser, Date modifyDate) {
        this.topicName = topicName;
        if (enableAuthControl) {
            this.enableAuthControl = 1;
        } else {
            this.enableAuthControl = 0;
        }
        this.createUser = modifyUser;
        this.createDate = modifyDate;
    }

    public BdbTopicAuthControlEntity(String topicName, boolean enableAuthControl,
            String attributes, String modifyUser, Date modifyDate) {
        this.topicName = topicName;
        if (enableAuthControl) {
            this.enableAuthControl = 1;
        } else {
            this.enableAuthControl = 0;
        }
        this.attributes = attributes;
        this.createUser = modifyUser;
        this.createDate = modifyDate;
    }

    public String getAttributes() {
        return attributes;
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public boolean isEnableAuthControl() {
        return enableAuthControl == 1;
    }

    public int getEnableAuthControl() {
        return this.enableAuthControl;
    }

    public void setEnableAuthControl(boolean enableAuthControl) {
        if (enableAuthControl) {
            this.enableAuthControl = 1;
        } else {
            this.enableAuthControl = 0;
        }
    }

    public String getModifyUser() {
        return createUser;
    }

    public Date getModifyDate() {
        return createDate;
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
        String tmpCreateUser = TStringUtils.getAttrValFrmAttributes(
                this.attributes, TStoreConstants.TOKEN_CREATE_USER);
        if (TStringUtils.isEmpty(tmpCreateUser)) {
            tmpCreateUser = this.createUser;
        }
        return tmpCreateUser;
    }

    public Date getCreateDate() {
        String dateStr = TStringUtils.getAttrValFrmAttributes(
                this.attributes, TStoreConstants.TOKEN_CREATE_DATE);
        if (TStringUtils.isNotEmpty(dateStr)) {
            return DateTimeConvertUtils.yyyyMMddHHmmss2date(dateStr);
        }
        return this.createDate;
    }

    public String getStrModifyDate() {
        return DateTimeConvertUtils.date2yyyyMMddHHmmss(createDate);
    }

    public String getStrCreateDate() {
        String dateStr = TStringUtils.getAttrValFrmAttributes(
                this.attributes, TStoreConstants.TOKEN_CREATE_DATE);
        if (TStringUtils.isEmpty(dateStr)) {
            dateStr = DateTimeConvertUtils.date2yyyyMMddHHmmss(createDate);
        }
        return dateStr;
    }

    public StringBuilder toJsonString(final StringBuilder sBuilder) {
        return sBuilder.append("{\"type\":\"BdbConsumerGroupEntity\",")
                .append("\"topicName\":\"").append(topicName)
                .append("\",\"enableAuthControl\":\"").append(enableAuthControl)
                .append("\",\"createUser\":\"").append(getCreateUser())
                .append("\",\"createDate\":\"").append(getStrCreateDate())
                .append("\",\"modifyUser\":\"").append(getModifyUser())
                .append("\",\"modifyDate\":\"").append(getStrModifyDate())
                .append("\",\"attributes\":\"").append(attributes).append("\"}");
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("topicName", topicName)
                .append("enableAuthControl", enableAuthControl)
                .append("createUser", getCreateUser())
                .append("createDate", getStrCreateDate())
                .append("modifyUser", getModifyUser())
                .append("modifyDate", getStrModifyDate())
                .append("attributes", attributes)
                .toString();
    }
}
