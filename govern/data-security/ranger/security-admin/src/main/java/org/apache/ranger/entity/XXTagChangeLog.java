/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.ranger.entity;

import java.util.Date;
import java.util.Objects;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Column;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.DateUtil;

@EntityListeners( org.apache.ranger.common.db.JPABeanCallbacks.class)
@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_tag_change_log")
public class XXTagChangeLog implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "X_TAG_CHANGE_LOG_SEQ", sequenceName = "X_TAG_CHANGE_LOG_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "X_TAG_CHANGE_LOG_SEQ")
    @Column(name = "id")
    protected Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="create_time"   )
    protected Date createTime = DateUtil.getUTCDate();

    @Column(name = "service_id")
    protected Long serviceId;

    @Column(name = "change_type")
    protected Integer changeType;

    @Column(name = "service_tags_version")
    protected Long serviceTagsVersion;

    @Column(name = "service_resource_id")
    protected Long serviceResourceId;

    @Column(name = "tag_id")
    protected Long tagId;

    /**
     * Default constructor. This will set all the attributes to default value.
     */
    public XXTagChangeLog( ) {
        this(null, null, null, null, null, null, null);
    }

    public XXTagChangeLog(Long id, Integer changeType, Long serviceTagsVersion, Long serviceResourceId,  Long tagId) {
        this(id, null, null, changeType, serviceTagsVersion, serviceResourceId, tagId);
    }

    public XXTagChangeLog(Long id, Date createTime, Long serviceId, Integer changeType, Long serviceTagsVersion, Long serviceResourceId, Long tagId) {
        setId(id);
        setCreateTime(createTime);
        setServiceId(serviceId);
        setChangeType(changeType);
        setServiceTagsVersion(serviceTagsVersion);
        setServiceResourceId(serviceResourceId);
        setTagId(tagId);
    }

    public int getMyClassType( ) {
        return AppConstants.CLASS_TYPE_NONE;
    }

    public String getMyDisplayValue() {
        return null;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return this.id;
    }

    public void setCreateTime( Date createTime ) {
        this.createTime = createTime;
    }

    public Date getCreateTime( ) {
        return this.createTime;
    }

    public void setServiceId(Long serviceId) {
        this.serviceId = serviceId;
    }

    public Long getServiceId() {
        return this.serviceId;
    }

    public void setChangeType(Integer changeType) { this.changeType = changeType; }

    public Integer getChangeType() { return this.changeType; }

    public void setServiceTagsVersion(Long serviceTagsVersion) {
        this.serviceTagsVersion = serviceTagsVersion;
    }

    public Long getServiceTagsVersion() {
        return this.serviceTagsVersion;
    }

    public Long getServiceResourceId() { return this.serviceResourceId; }

    public void setServiceResourceId(Long serviceResourceId) {
        this.serviceResourceId = serviceResourceId;
    }

    public Long getTagId() { return this.tagId; }

    public void setTagId(Long tagId) {
        this.tagId = tagId;
    }

    /**
     * This return the bean content in string format
     * @return formatedStr
     */
    @Override
    public String toString( ) {
        String str = "XXTagChangeLog={";
        str += "id={" + id + "} ";
        str += "createTime={" + createTime + "} ";
        str += "serviceId={" + serviceId + "} ";
        str += "changeType={" + changeType + "} ";
        str += "serviceTagsVersion={" + serviceTagsVersion + "} ";
        str += "serviceResourceId={" + serviceResourceId + "} ";
        str += "tagId={" + tagId + "} ";
        str += "}";
        return str;
    }

    /**
     * Checks for all attributes except referenced db objects
     * @return true if all attributes match
     */
    @Override
    public boolean equals( Object obj) {
        if (obj == null)
            return false;
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;

        XXTagChangeLog other = (XXTagChangeLog) obj;

        return Objects.equals(this.id, other.id) && Objects.equals(this.createTime, other.createTime) && Objects.equals(this.serviceId, other.serviceId)
                && Objects.equals(this.changeType, other.changeType) && Objects.equals(this.serviceTagsVersion, other.serviceTagsVersion)
                && Objects.equals(this.serviceResourceId, other.serviceResourceId) && Objects.equals(this.tagId, other.tagId);
    }

}


