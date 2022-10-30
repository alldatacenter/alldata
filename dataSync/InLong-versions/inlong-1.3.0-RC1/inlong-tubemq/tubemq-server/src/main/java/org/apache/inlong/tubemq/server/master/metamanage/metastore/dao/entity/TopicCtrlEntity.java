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
import org.apache.inlong.tubemq.corebase.utils.SettingValidUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbTopicAuthControlEntity;

/*
 * store the topic authenticate control setting
 *
 */
public class TopicCtrlEntity extends BaseEntity implements Cloneable {

    private String topicName = "";
    // topic id, require globally unique
    private int topicNameId = TBaseConstants.META_VALUE_UNDEFINED;
    private EnableStatus authCtrlStatus = EnableStatus.STATUS_UNDEFINE;
    private int maxMsgSizeInB = TBaseConstants.META_VALUE_UNDEFINED;
    private int maxMsgSizeInMB = TBaseConstants.META_VALUE_UNDEFINED;

    public TopicCtrlEntity() {
        super();
    }

    public TopicCtrlEntity(BaseEntity opEntity, String topicName) {
        super(opEntity);
        this.topicName = topicName;
    }

    public TopicCtrlEntity(BaseEntity opEntity, String topicName,
                           int topicNameId, int maxMsgSizeInMB) {
        super(opEntity.getDataVerId(),
                opEntity.getModifyUser(),
                opEntity.getModifyDate());
        this.topicName = topicName;
        this.topicNameId = topicNameId;
        this.authCtrlStatus = EnableStatus.STATUS_DISABLE;
        this.maxMsgSizeInB =
                SettingValidUtils.validAndXfeMaxMsgSizeFromMBtoB(maxMsgSizeInMB);
        this.maxMsgSizeInMB = maxMsgSizeInMB;
    }

    /**
     * Constructor by BdbTopicAuthControlEntity
     *
     * @param bdbEntity  the BdbTopicAuthControlEntity initial object
     */
    public TopicCtrlEntity(BdbTopicAuthControlEntity bdbEntity) {
        super(bdbEntity.getDataVerId(),
                bdbEntity.getModifyUser(), bdbEntity.getModifyDate());
        setCreateInfo(bdbEntity.getCreateUser(), bdbEntity.getCreateDate());
        this.topicName = bdbEntity.getTopicName();
        this.topicNameId = bdbEntity.getTopicId();
        this.fillMaxMsgSizeInB(bdbEntity.getMaxMsgSize());
        if (bdbEntity.isEnableAuthControl()) {
            this.authCtrlStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.authCtrlStatus = EnableStatus.STATUS_DISABLE;
        }
        this.setAttributes(bdbEntity.getAttributes());
    }

    /**
     * build bdb object from current info
     *
     * @return the BdbTopicAuthControlEntity object
     */
    public BdbTopicAuthControlEntity buildBdbTopicAuthControlEntity() {
        BdbTopicAuthControlEntity bdbEntity =
                new BdbTopicAuthControlEntity(topicName, isAuthCtrlEnable(),
                        getAttributes(), getModifyUser(), getModifyDate());
        bdbEntity.setCreateInfo(getCreateUser(), getCreateDate());
        bdbEntity.setTopicId(topicNameId);
        bdbEntity.setDataVerId(getDataVerId());
        bdbEntity.setMaxMsgSize(maxMsgSizeInB);
        return bdbEntity;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public boolean isAuthCtrlEnable() {
        return authCtrlStatus == EnableStatus.STATUS_ENABLE;
    }

    public void setEnableAuthCtrl(boolean enableAuth) {
        if (enableAuth) {
            this.authCtrlStatus = EnableStatus.STATUS_ENABLE;
        } else {
            this.authCtrlStatus = EnableStatus.STATUS_DISABLE;
        }
    }

    public EnableStatus getAuthCtrlStatus() {
        return authCtrlStatus;
    }

    public void setAuthCtrlStatus(EnableStatus authCtrlStatus) {
        this.authCtrlStatus = authCtrlStatus;
    }

    public int getMaxMsgSizeInB() {
        return maxMsgSizeInB;
    }

    public int getMaxMsgSizeInMB() {
        return maxMsgSizeInMB;
    }

    public int getTopicId() {
        return topicNameId;
    }

    public void setTopicId(int topicNameId) {
        this.topicNameId = topicNameId;
    }

    /**
     * update subclass field values
     *
     * @param dataVerId         new data version id
     * @param topicNameId       new topicName Id
     * @param newMaxMsgSizeMB   new max message size
     * @param enableTopicAuth   new topicAuth enable status
     * @return  whether changed
     */
    public boolean updModifyInfo(long dataVerId, int topicNameId,
                                 int newMaxMsgSizeMB, Boolean enableTopicAuth) {
        boolean changed = false;
        // check and set brokerPort info
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
        // check and set modified field
        if (newMaxMsgSizeMB != TBaseConstants.META_VALUE_UNDEFINED) {
            int tmpMaxMsgSizeInMB =
                    SettingValidUtils.validAndGetMsgSizeInMB(newMaxMsgSizeMB);
            if (this.maxMsgSizeInMB != tmpMaxMsgSizeInMB) {
                changed = true;
                this.maxMsgSizeInMB = tmpMaxMsgSizeInMB;
                this.maxMsgSizeInB =
                        SettingValidUtils.validAndXfeMaxMsgSizeFromMBtoB(tmpMaxMsgSizeInMB);
            }
        }
        // check and set authCtrlStatus info
        if (enableTopicAuth != null
                && (this.authCtrlStatus == EnableStatus.STATUS_UNDEFINE
                || this.authCtrlStatus.isEnable() != enableTopicAuth)) {
            setEnableAuthCtrl(enableTopicAuth);
            changed = true;
        }
        if (changed) {
            updSerialId();
        }
        return changed;
    }

    /**
     * Check whether the specified query item value matches
     * Allowed query items:
     *   topicName, maxMsgSizeInB, authCtrlStatus
     *
     * @param target  the matched object
     * @param fullMatch  whether match parent parameters
     * @return true: matched, false: not match
     */
    public boolean isMatched(TopicCtrlEntity target, boolean fullMatch) {
        if (target == null) {
            return true;
        }
        if (fullMatch && !super.isMatched(target)) {
            return false;
        }
        return (target.getMaxMsgSizeInB() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getMaxMsgSizeInB() == this.maxMsgSizeInB)
                && (TStringUtils.isBlank(target.getTopicName())
                || target.getTopicName().equals(this.topicName))
                && (target.getAuthCtrlStatus() == EnableStatus.STATUS_UNDEFINE
                || target.getAuthCtrlStatus() == this.authCtrlStatus)
                && (target.getTopicId() == TBaseConstants.META_VALUE_UNDEFINED
                || target.getTopicId() == this.topicNameId);
    }

    /**
     * Serialize field to json format
     *
     * @param sBuilder   build container
     * @param isLongName if return field key is long name
     * @param fullFormat if return full format json
     * @return   process result
     */
    public StringBuilder toWebJsonStr(StringBuilder sBuilder,
                                      boolean isLongName,
                                      boolean fullFormat) {
        if (isLongName) {
            sBuilder.append("{\"topicName\":\"").append(topicName).append("\"")
                    .append(",\"topicNameId\":").append(topicNameId)
                    .append(",\"enableAuthControl\":").append(authCtrlStatus.isEnable())
                    .append(",\"maxMsgSizeInMB\":").append(maxMsgSizeInMB);
        } else {
            sBuilder.append("{\"topic\":\"").append(topicName).append("\"")
                    .append(",\"topicId\":").append(topicNameId)
                    .append(",\"acEn\":").append(authCtrlStatus.isEnable())
                    .append(",\"mxMsgInMB\":").append(maxMsgSizeInMB);
        }
        super.toWebJsonStr(sBuilder, isLongName);
        if (fullFormat) {
            sBuilder.append("}");
        }
        return sBuilder;
    }

    private void fillMaxMsgSizeInB(int maxMsgSizeInB) {
        int tmpMaxMsgSizeInMB = TBaseConstants.META_MIN_ALLOWED_MESSAGE_SIZE_MB;
        if (maxMsgSizeInB > TBaseConstants.META_MB_UNIT_SIZE) {
            tmpMaxMsgSizeInMB = SettingValidUtils.validAndGetMsgSizeBtoMB(maxMsgSizeInB);
        }
        this.maxMsgSizeInMB = tmpMaxMsgSizeInMB;
        this.maxMsgSizeInB =
                SettingValidUtils.validAndXfeMaxMsgSizeFromMBtoB(this.maxMsgSizeInMB);
    }

    /**
     * check if subclass fields is equals
     *
     * @param other  check object
     * @return if equals
     */
    public boolean isDataEquals(TopicCtrlEntity other) {
        return super.isDataEquals(other)
                && topicNameId == other.topicNameId
                && maxMsgSizeInB == other.maxMsgSizeInB
                && topicName.equals(other.topicName)
                && authCtrlStatus == other.authCtrlStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TopicCtrlEntity)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        TopicCtrlEntity that = (TopicCtrlEntity) o;
        return isDataEquals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), topicName,
                topicNameId, authCtrlStatus, maxMsgSizeInB);
    }

    @Override
    public TopicCtrlEntity clone() {
        TopicCtrlEntity copy = (TopicCtrlEntity) super.clone();
        copy.setAuthCtrlStatus(getAuthCtrlStatus());
        return copy;
    }
}
