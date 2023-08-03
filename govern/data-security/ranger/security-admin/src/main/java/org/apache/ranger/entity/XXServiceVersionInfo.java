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
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.DateUtil;

@EntityListeners( org.apache.ranger.common.db.JPABeanCallbacks.class)
@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_service_version_info")
public class XXServiceVersionInfo implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "X_SERVICE_VERSION_INFO_SEQ", sequenceName = "X_SERVICE_VERSION_INFO_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "X_SERVICE_VERSION_INFO_SEQ")

	@Column(name = "id")
	protected Long id;

	@Column(name = "service_id")
	protected Long serviceId;

	@Column(name = "policy_version")
	protected Long policyVersion;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="policy_update_time"   )
	protected Date policyUpdateTime = DateUtil.getUTCDate();

	@Column(name = "tag_version")
	protected Long tagVersion;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="tag_update_time"   )
	protected Date tagUpdateTime = DateUtil.getUTCDate();

	@Column(name = "role_version")
	protected Long roleVersion;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="role_update_time"   )
	protected Date roleUpdateTime = DateUtil.getUTCDate();

	@Version
	@Column(name = "version")
	protected Long version;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXServiceVersionInfo ( ) {
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

	public void setVersion(Long version) {
		this.version = version;
	}

	public Long getId() {
		return this.id;
	}

	public Long getVersion() {
		return version;
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

	public void setPolicyUpdateTime( Date updateTime ) {
		this.policyUpdateTime = updateTime;
	}

	public Date getPolicyUpdateTime( ) {
		return this.policyUpdateTime;
	}

	public void setTagVersion(Long tagVersion) {
		this.tagVersion = tagVersion;
	}

	public Long getTagVersion() {
		return this.tagVersion;
	}

	public void setTagUpdateTime( Date updateTime ) {
		this.tagUpdateTime = updateTime;
	}

	public Date getTagUpdateTime( ) {
		return this.tagUpdateTime;
	}

	public void setRoleVersion(Long roleVersion) {
		this.roleVersion = roleVersion;
	}

	public Long getRoleVersion() {
		return this.roleVersion;
	}

	public void setRoleUpdateTime( Date updateTime ) {
		this.roleUpdateTime = updateTime;
	}

	public Date getRoleUpdateTime( ) {
		return this.roleUpdateTime;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXServiceVersionInfo={";
		str += "id={" + id + "} ";
		str += "version={" + version + "} ";
		str += "serviceId={" + serviceId + "} ";
		str += "policyVersion={" + policyVersion + "} ";
		str += "policyUpdateTime={" + policyUpdateTime + "} ";
		str += "tagVersion={" + tagVersion + "} ";
		str += "tagUpdateTime={" + tagUpdateTime + "} ";
		str += "setRoleVersion={" + roleVersion + "}" ;
		str += "setRoleUpdateTime={" + roleUpdateTime + "}" ;
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
		XXServiceVersionInfo other = (XXServiceVersionInfo) obj;
		if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
			return false;
		}
		if ((version == null && other.version != null) || (this.version != null && !this.version.equals(other.version))) {
			return false;
		}
		if ((this.serviceId == null && other.serviceId != null) || (this.serviceId != null && !this.serviceId.equals(other.serviceId))) {
			return false;
		}
		if ((this.policyVersion == null && other.policyVersion != null) || (this.policyVersion != null && !this.policyVersion.equals(other.policyVersion))) {
			return false;
		}
		if ((this.policyUpdateTime == null && other.policyUpdateTime != null) || (this.policyUpdateTime != null && !this.policyUpdateTime.equals(other.policyUpdateTime))) {
			return false;
		}
		if ((this.tagVersion == null && other.tagVersion != null) || (this.tagVersion != null && !this.tagVersion.equals(other.tagVersion))) {
			return false;
		}
		if ((this.tagUpdateTime == null && other.tagUpdateTime != null) || (this.tagUpdateTime != null && !this.tagUpdateTime.equals(other.tagUpdateTime))) {
			return false;
		}
		if ((this.roleVersion == null && other.roleVersion != null) || (this.roleVersion != null && !this.roleVersion.equals(other.roleVersion))) {
			return false;
		}
		if ((this.roleUpdateTime == null && other.roleUpdateTime != null) || (this.roleUpdateTime != null && !this.roleUpdateTime.equals(other.roleUpdateTime))) {
			return false;
		}
		return true;
	}

	public static boolean equals(Object object1, Object object2) {
		if (object1 == object2) {
			return true;
		}
		if ((object1 == null) || (object2 == null)) {
			return false;
		}
		return object1.equals(object2);
	}

}
