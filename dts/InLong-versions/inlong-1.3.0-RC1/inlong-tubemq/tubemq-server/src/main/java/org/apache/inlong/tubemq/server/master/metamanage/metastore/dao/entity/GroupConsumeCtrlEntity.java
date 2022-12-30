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
import org.apache.inlong.tubemq.server.common.statusdef.EnableStatus;
import org.apache.inlong.tubemq.server.master.bdbstore.bdbentitys.BdbGroupFilterCondEntity;

/*
 * store the group consume control setting
 *
 */
public class GroupConsumeCtrlEntity extends BaseEntity implements Cloneable {
    private String recordKey = "";
    private String topicName = "";
    private String groupName = "";
    // enable consume control
    private EnableStatus consumeEnable = EnableStatus.STATUS_UNDEFINE;
    private String disableReason = "";
    // filter consume setting
    private EnableStatus filterEnable = EnableStatus.STATUS_UNDEFINE;
    private String filterCondStr = "";

    public GroupConsumeCtrlEntity() {
        super();
    }

    public GroupConsumeCtrlEntity(BaseEntity opInfoEntity,
                                  String groupName, String topicName) {
        super(opInfoEntity);
        setGroupAndTopic(groupName, topicName);
    }

    /**
     * Constructor by GroupConsumeCtrlEntity
     *
     * @param bdbEntity  the GroupConsumeCtrlEntity initial object
     */
    public GroupConsumeCtrlEntity(BdbGroupFilterCondEntity bdbEntity) {
        super(bdbEntity.getDataVerId(),
                bdbEntity.getModifyUser(), bdbEntity.getModifyDate());
        setCreateInfo(bdbEntity.getCreateUser(), bdbEntity.getCreateDate());
        this.setGroupAndTopic(bdbEntity.getConsumerGroupName(), bdbEntity.getTopicName());
        if (bdbEntity.getControlStatus() == 2) {
            this.filterEnable = EnableStatus.STATUS_ENABLE;
        } else if (bdbEntity.getControlStatus() == -2) {
            this.filterEnable = EnableStatus.STATUS_UNDEFINE;
        } else {
            this.filterEnable = EnableStatus.STATUS_DISABLE;
        }
        this.consumeEnable = bdbEntity.getConsumeEnable();
        this.disableReason = bdbEntity.getDisableConsumeReason();
        this.filterCondStr = bdbEntity.getFilterCondStr();
        this.setAttributes(bdbEntity.getAttributes());
    }

    /**
     * build bdb object from current info
     *
     * @return the BdbGroupFilterCondEntity object
     */
    public BdbGroupFilterCondEntity buildBdbGroupFilterCondEntity() {
        BdbGroupFilterCondEntity bdbEntity =
                new BdbGroupFilterCondEntity(topicName, groupName,
                        filterEnable.getCode(), filterCondStr,
                        getAttributes(), getModifyUser(), getModifyDate());
        bdbEntity.setCreateInfo(getCreateUser(), getCreateDate());
        bdbEntity.setDataVerId(getDataVerId());
        bdbEntity.setConsumeEnable(consumeEnable);
        bdbEntity.setDisableConsumeReason(disableReason);
        return bdbEntity;
    }

    public void setGroupAndTopic(String groupName, String topicName) {
        this.groupName = groupName;
        this.topicName = topicName;
        this.recordKey = KeyBuilderUtils.buildGroupTopicRecKey(groupName, topicName);
    }

    public String getRecordKey() {
        return recordKey;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getGroupName() {
        return groupName;
    }

    public EnableStatus getConsumeEnable() {
        return consumeEnable;
    }

    public boolean isEnableConsume() {
        return consumeEnable.isEnable();
    }

    public void setConsumeEnable(boolean enableConsume) {
        if (enableConsume) {
            this.consumeEnable = EnableStatus.STATUS_ENABLE;
        } else {
            this.consumeEnable = EnableStatus.STATUS_DISABLE;
        }
    }

    public String getDisableReason() {
        return disableReason;
    }

    public void setDisableReason(String disableReason) {
        this.disableReason = disableReason;
    }

    public boolean isEnableFilterConsume() {
        return filterEnable == EnableStatus.STATUS_ENABLE;
    }

    private void setFilterEnable(boolean enableFilter) {
        if (enableFilter) {
            this.filterEnable = EnableStatus.STATUS_ENABLE;
        } else {
            this.filterEnable = EnableStatus.STATUS_DISABLE;
        }
    }

    public String getFilterCondStr() {
        return filterCondStr;
    }

    public void setFilterCondStr(String filterCondStr) {
        this.filterCondStr = filterCondStr;
    }

    public EnableStatus getFilterEnable() {
        return filterEnable;
    }

    /**
     * update subclass field values
     *
     * @param dataVerId         new data version id
     * @param consumeEnable     new consume enable status
     * @param disableRsn        new disable reason
     * @param filterEnable      new filter enable status
     * @param filterCondStr     new filter condition configure
     *
     * @return    whether data is changed
     */
    public boolean updModifyInfo(long dataVerId, Boolean consumeEnable,
                                 String disableRsn, Boolean filterEnable,
                                 String filterCondStr) {
        boolean changed = false;
        // check and set brokerPort info
        if (dataVerId != TBaseConstants.META_VALUE_UNDEFINED
                && this.getDataVerId() != dataVerId) {
            changed = true;
            this.setDataVersionId(dataVerId);
        }
        // check and set consumeEnable info
        if (consumeEnable != null
                && (this.consumeEnable == EnableStatus.STATUS_UNDEFINE
                || this.consumeEnable.isEnable() != consumeEnable)) {
            changed = true;
            setConsumeEnable(consumeEnable);
        }
        // check and set disableReason info
        if (disableRsn != null
                && !disableRsn.equals(disableReason)) {
            changed = true;
            disableReason = disableRsn;
        }
        // check and set consumeEnable info
        if (filterEnable != null
                && (this.filterEnable == EnableStatus.STATUS_UNDEFINE
                || this.filterEnable.isEnable() != filterEnable)) {
            changed = true;
            setFilterEnable(filterEnable);
        }
        // check and set filterCondStr info
        if (TStringUtils.isNotBlank(filterCondStr)
                && !filterCondStr.equals(this.filterCondStr)) {
            changed = true;
            this.filterCondStr = filterCondStr;
        }
        if (changed) {
            updSerialId();
        }
        return changed;
    }

    /**
     * Check whether the specified query item value matches
     * Allowed query items:
     *   topicName, groupName, filterConsumeStatus
     * @return true: matched, false: not match
     */
    public boolean isMatched(GroupConsumeCtrlEntity target) {
        if (target == null) {
            return true;
        }
        if (!super.isMatched(target)) {
            return false;
        }
        return (TStringUtils.isBlank(target.getTopicName())
                || target.getTopicName().equals(this.topicName))
                && (TStringUtils.isBlank(target.getGroupName())
                || target.getGroupName().equals(this.groupName))
                && (target.getConsumeEnable() == EnableStatus.STATUS_UNDEFINE
                || target.getConsumeEnable() == this.consumeEnable)
                && (target.getFilterEnable() == EnableStatus.STATUS_UNDEFINE
                || target.getFilterEnable() == this.filterEnable);
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
        String tmpFilterConds = filterCondStr;
        if (tmpFilterConds.length() <= 2) {
            tmpFilterConds = "";
        }
        if (isLongName) {
            sBuilder.append("{\"topicName\":\"").append(topicName).append("\"")
                    .append(",\"groupName\":\"").append(groupName).append("\"")
                    .append(",\"consumeEnable\":").append(consumeEnable.isEnable())
                    .append(",\"disableCsmRsn\":\"").append(disableReason).append("\"")
                    .append(",\"filterEnable\":").append(filterEnable.isEnable())
                    .append(",\"filterConds\":\"").append(tmpFilterConds).append("\"");
        } else {
            sBuilder.append("{\"topic\":\"").append(topicName).append("\"")
                    .append(",\"group\":\"").append(groupName).append("\"")
                    .append(",\"csmEn\":").append(consumeEnable.isEnable())
                    .append(",\"dsCsmRsn\":\"").append(disableReason).append("\"")
                    .append(",\"fltEn\":").append(filterEnable.isEnable())
                    .append(",\"fltRls\":\"").append(tmpFilterConds).append("\"");
        }
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
    public boolean isDataEquals(GroupConsumeCtrlEntity other) {
        return super.isDataEquals(other)
                && recordKey.equals(other.recordKey)
                && Objects.equals(topicName, other.topicName)
                && Objects.equals(groupName, other.groupName)
                && consumeEnable == other.consumeEnable
                && Objects.equals(disableReason, other.disableReason)
                && filterEnable == other.filterEnable
                && Objects.equals(filterCondStr, other.filterCondStr);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GroupConsumeCtrlEntity)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        GroupConsumeCtrlEntity that = (GroupConsumeCtrlEntity) o;
        return isDataEquals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), recordKey, topicName,
                groupName, consumeEnable, disableReason, filterEnable, filterCondStr);
    }

    @Override
    public GroupConsumeCtrlEntity clone() {
        GroupConsumeCtrlEntity copy = (GroupConsumeCtrlEntity) super.clone();
        copy.setConsumeEnable(getConsumeEnable().isEnable());
        copy.setFilterEnable(getFilterEnable().isEnable());
        return copy;
    }
}
