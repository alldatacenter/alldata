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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.corebase.utils.DateTimeConvertUtils;
import org.apache.inlong.tubemq.corebase.utils.TStringUtils;
import org.apache.inlong.tubemq.server.common.TServerConstants;
import org.apache.inlong.tubemq.server.common.utils.SerialIdUtils;

// AbstractEntity: entity's abstract class
public class BaseEntity implements Serializable, Cloneable {

    private long dataVersionId =
            TBaseConstants.META_VALUE_UNDEFINED;    // -2: undefined, other: version
    private AtomicLong serialId =
            new AtomicLong(TBaseConstants.META_VALUE_UNDEFINED);
    private String createUser = "";        // create user
    private Date createDate = null;        // create date
    private String modifyUser = "";       // modify user
    private Date modifyDate = null;        // modify date
    private String attributes = "";        // attribute info
    private String createDateStr = "";     // create data string
    private String modifyDateStr = "";     // create data string

    public BaseEntity() {

    }

    public BaseEntity(String createUser, Date createDate) {
        this(TServerConstants.DEFAULT_DATA_VERSION,
                createUser, createDate, createUser, createDate);
    }

    /**
     * Constructor by BaseEntity
     *
     * @param other  the BaseEntity initial object
     */
    public BaseEntity(BaseEntity other) {
        this.dataVersionId = other.dataVersionId;
        setCreateInfo(other.createUser, other.createDate);
        setModifyInfo(other.modifyUser, other.modifyDate);
        this.serialId.set(other.serialId.get());
        this.attributes = other.attributes;
    }

    public BaseEntity(long dataVersionId, String createUser, Date createDate) {
        this(dataVersionId, createUser, createDate, createUser, createDate);
    }

    public BaseEntity(String createUser, Date createDate,
                      String modifyUser, Date modifyDate) {
        this(TServerConstants.DEFAULT_DATA_VERSION,
                createUser, createDate, modifyUser, modifyDate);
    }

    public BaseEntity(long dataVersionId,
                      String createUser, Date createDate,
                      String modifyUser, Date modifyDate) {
        this.dataVersionId = dataVersionId;
        setCreateInfo(createUser, createDate);
        setModifyInfo(modifyUser, modifyDate);
        updSerialId();
    }

    /**
     * Update modify key fields information
     *
     * @param opInfoEntity   need updated BaseEntity information
     * @return  whether changed current value
     */
    public boolean updBaseModifyInfo(BaseEntity opInfoEntity) {
        boolean changed = false;
        if (TStringUtils.isNotBlank(opInfoEntity.getModifyUser())
                && !Objects.equals(modifyUser, opInfoEntity.getModifyUser())) {
            changed = true;
            this.modifyUser = opInfoEntity.getModifyUser();
        }
        if (opInfoEntity.getModifyDate() != null
                && !Objects.equals(modifyDate, opInfoEntity.getModifyDate())) {
            changed = true;
            this.setModifyDate(opInfoEntity.getModifyDate());
        }
        if (TStringUtils.isNotBlank(opInfoEntity.getAttributes())
                && !Objects.equals(attributes, opInfoEntity.getAttributes())) {
            changed = true;
            this.attributes = opInfoEntity.getAttributes();
        }
        return changed;
    }

    /**
     * Update query key fields information
     *
     * @param newDataVerId   need updated data version id
     * @param newCreateUser  need updated creator
     * @param newModifyUser  need updated modify user
     * @return  whether changed current value
     */
    public boolean updQueryKeyInfo(long newDataVerId,
                                   String newCreateUser,
                                   String newModifyUser) {
        boolean changed = false;
        // check and set dataVersionId field
        if (newDataVerId != TBaseConstants.META_VALUE_UNDEFINED
                && this.dataVersionId != newDataVerId) {
            changed = true;
            this.dataVersionId = newDataVerId;
        }
        if (TStringUtils.isNotBlank(newCreateUser)
                && !Objects.equals(createUser, newCreateUser)) {
            changed = true;
            this.createUser = newCreateUser;
        }
        if (TStringUtils.isNotBlank(newModifyUser)
                && !Objects.equals(modifyUser, newModifyUser)) {
            changed = true;
            this.modifyUser = newModifyUser;
        }
        return changed;
    }

    public void setDataVersionId(long dataVersionId) {
        this.dataVersionId = dataVersionId;
    }

    public void setKeyAndVal(String key, String value) {
        this.attributes =
                TStringUtils.setAttrValToAttributes(this.attributes, key, value);
    }

    public String getValueByKey(String key) {
        return TStringUtils.getAttrValFrmAttributes(this.attributes, key);
    }

    public String getAttributes() {
        return attributes;
    }

    public void setCreateInfo(String creator, Date createDate) {
        if (TStringUtils.isNotBlank(creator)) {
            this.createUser = creator;
        }
        setCreateDate(createDate);
    }

    public void setModifyInfo(String modifyUser, Date modifyDate) {
        if (TStringUtils.isNotBlank(modifyUser)) {
            this.modifyUser = modifyUser;
        }
        setModifyDate(modifyDate);
    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getCreateUser() {
        return createUser;
    }

    public Date getCreateDate() {
        return createDate;
    }

    public long getDataVerId() {
        return dataVersionId;
    }

    public long getSerialId() {
        return serialId.get();
    }

    protected void updSerialId() {
        SerialIdUtils.updTimeStampSerialIdValue(this.serialId);
    }

    public String getModifyUser() {
        return modifyUser;
    }

    public Date getModifyDate() {
        return modifyDate;
    }

    public String getCreateDateStr() {
        return createDateStr;
    }

    public String getModifyDateStr() {
        return modifyDateStr;
    }

    public String toJsonString(Gson gson) {
        return gson.toJson(this);
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().create();
        return toJsonString(gson);
    }

    /**
     * Check whether the specified query item value matches
     * Allowed query items:
     *   dataVersionId, createUser, modifyUser
     * @return true: matched, false: not match
     */
    public boolean isMatched(BaseEntity target) {
        if (target == null) {
            return true;
        }
        return (target.getDataVerId() == TBaseConstants.META_VALUE_UNDEFINED
                || this.getDataVerId() == target.getDataVerId())
                && (TStringUtils.isBlank(target.getCreateUser())
                || target.getCreateUser().equals(createUser))
                && (TStringUtils.isBlank(target.getModifyUser())
                || target.getModifyUser().equals(modifyUser));
    }

    /**
     * Serialize field to json format
     *
     * @param sBuilder   build container
     * @param isLongName if return field key is long name
     * @return   process result
     */
    public StringBuilder toWebJsonStr(StringBuilder sBuilder, boolean isLongName) {
        if (isLongName) {
            sBuilder.append(",\"dataVersionId\":").append(dataVersionId)
                    .append(",\"serialId\":").append(serialId.get())
                    .append(",\"createUser\":\"").append(createUser).append("\"")
                    .append(",\"createDate\":\"").append(createDateStr).append("\"")
                    .append(",\"modifyUser\":\"").append(modifyUser).append("\"")
                    .append(",\"modifyDate\":\"").append(modifyDateStr).append("\"");
                    //.append(",\"attributes\":\"").append(attributes).append("\"");
        } else {
            sBuilder.append(",\"dVerId\":").append(dataVersionId)
                    .append(",\"serialId\":").append(serialId.get())
                    .append(",\"cur\":\"").append(createUser).append("\"")
                    .append(",\"cDate\":\"").append(createDateStr).append("\"")
                    .append(",\"mur\":\"").append(modifyUser).append("\"")
                    .append(",\"mDate\":\"").append(modifyDateStr).append("\"");
                    //.append(",\"attrs\":\"").append(attributes).append("\"");
        }
        return sBuilder;
    }

    /**
     * Get field value to key and value format.
     *
     * @param paramMap   build container
     * @param isLongName if return field key is long name
     */
    public void getConfigureInfo(Map<String, String> paramMap,
                                 boolean isLongName) {
        if (dataVersionId != TBaseConstants.META_VALUE_UNDEFINED) {
            paramMap.put((isLongName ? "dataVersionId" : "dVerId"),
                    String.valueOf(dataVersionId));
        }
        if (serialId.get() != TBaseConstants.META_VALUE_UNDEFINED) {
            paramMap.put((isLongName ? "serialId" : "serialId"),
                    String.valueOf(serialId.get()));
        }
        if (TStringUtils.isNotBlank(createUser)) {
            paramMap.put((isLongName ? "createUser" : "cur"), createUser);
        }
        if (TStringUtils.isNotBlank(createDateStr)) {
            paramMap.put((isLongName ? "createDate" : "cDate"), createDateStr);
        }
        if (TStringUtils.isNotBlank(modifyUser)) {
            paramMap.put((isLongName ? "modifyUser" : "mur"), modifyUser);
        }
        if (TStringUtils.isNotBlank(modifyDateStr)) {
            paramMap.put((isLongName ? "modifyDate" : "mDate"), modifyDateStr);
        }
    }

    private void setModifyDate(Date date) {
        if (date == null) {
            return;
        }
        this.modifyDate = date;
        this.modifyDateStr = DateTimeConvertUtils.date2yyyyMMddHHmmss(date);
    }

    private void setCreateDate(Date date) {
        if (date == null) {
            return;
        }
        this.createDate = date;
        this.createDateStr = DateTimeConvertUtils.date2yyyyMMddHHmmss(date);
    }

    /**
     * Check if data field values are equal
     *
     * @param other  check object
     * @return if equals
     */
    public boolean isDataEquals(BaseEntity other) {
        return dataVersionId == other.dataVersionId
                && Objects.equals(createUser, other.createUser)
                && Objects.equals(createDateStr, other.createDateStr)
                && Objects.equals(modifyUser, other.modifyUser)
                && Objects.equals(modifyDateStr, other.modifyDateStr);
                // && Objects.equals(attributes, other.attributes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BaseEntity)) {
            return false;
        }
        BaseEntity that = (BaseEntity) o;
        return (serialId.get() == that.serialId.get())
                && isDataEquals(that);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dataVersionId, serialId.get(), createUser,
                createDate, modifyUser, modifyDate, attributes);
    }

    @Override
    public BaseEntity clone() {
        try {
            BaseEntity copy = (BaseEntity) super.clone();
            if (copy != null) {
                copy.serialId = new AtomicLong(this.serialId.get());
            }
            return copy;
        } catch (Throwable e) {
            return null;
        }
    }
}
