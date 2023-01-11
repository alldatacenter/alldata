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
@Table(name = "x_policy_change_log")
public class XXPolicyChangeLog implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @SequenceGenerator(name = "X_POLICY_CHANGE_LOG_SEQ", sequenceName = "X_POLICY_CHANGE_LOG_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "X_POLICY_CHANGE_LOG_SEQ")
    @Column(name = "id")
    protected Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name="create_time"   )
    protected Date createTime = DateUtil.getUTCDate();

    @Column(name = "service_id")
    protected Long serviceId;

    @Column(name = "change_type")
    protected Integer changeType;

    @Column(name = "policy_version")
    protected Long policyVersion;

	@Column(name = "service_type")
	protected String serviceType;

	@Column(name = "policy_type")
	protected Integer policyType;

	@Column(name = "zone_name")
    protected String zoneName;

	@Column(name = "policy_id")
	protected Long policyId;

	@Column(name = "policy_guid")
	protected String policyGuid;

    /**
     * Default constructor. This will set all the attributes to default value.
     */
    public XXPolicyChangeLog( ) {
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

    public void setPolicyVersion(Long policyVersion) {
        this.policyVersion = policyVersion;
    }

    public Long getPolicyVersion() {
        return this.policyVersion;
    }

    public void setChangeType(Integer changeType) {
        this.changeType = changeType;
    }

    public Integer getChangeType() {
        return this.changeType;
    }

	public String getServiceType() { return this.serviceType; }

	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	public Integer getPolicyType() { return this.policyType; }

	public void setPolicyType(Integer policyType) {
		this.policyType = policyType;
	}

    public String getZoneName() { return this.zoneName; }

    public void setZoneName(String zoneName) {
        this.zoneName = zoneName;
    }

	public Long getPolicyId() { return this.policyId; }

	public void setPolicyId(Long policyId) {
		this.policyId = policyId;
	}

	public String getPolicyGuid() {
		return this.policyGuid;
	}

	public void setPolicyGuid(String policyGuid) {
		this.policyGuid = policyGuid;
	}

    /**
     * This return the bean content in string format
     * @return formatedStr
     */
    @Override
    public String toString( ) {
        String str = "XXPolicyChangeLog={";
        str += "id={" + id + "} ";
        str += "createTime={" + createTime + "} ";
        str += "serviceId={" + serviceId + "} ";
        str += "changeType={" + changeType + "} ";
        str += "policyVersion={" + policyVersion + "} ";
        str += "serviceType={" + serviceType + "} ";
        str += "policyType={" + policyType + "} ";
        str += "zoneName={" + zoneName + "} ";
        str += "policyId={" + policyId + "} ";
        str += "policyGuid={" + policyGuid + "} ";
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

        XXPolicyChangeLog other = (XXPolicyChangeLog) obj;

        return Objects.equals(this.id, other.id) && Objects.equals(this.serviceId, other.serviceId) && Objects.equals(this.policyVersion, other.policyVersion)
                && Objects.equals(this.createTime, other.createTime) && Objects.equals(this.changeType, other.changeType) && Objects.equals(this.serviceType, other.serviceType)
                && Objects.equals(this.policyType, other.policyType) && Objects.equals(this.zoneName, other.zoneName) && Objects.equals(this.policyId, other.policyId)
                && Objects.equals(this.policyGuid, other.policyGuid);
    }

}

