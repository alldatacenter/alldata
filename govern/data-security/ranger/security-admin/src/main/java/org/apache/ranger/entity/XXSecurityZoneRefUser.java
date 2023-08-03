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

import java.util.Objects;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_security_zone_ref_user")
public class XXSecurityZoneRefUser extends XXDBBase implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
  	@Id
    @SequenceGenerator(name = "x_sec_zone_ref_user_SEQ", sequenceName = "x_sec_zone_ref_user_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "x_sec_zone_ref_user_SEQ")
    @Column(name = "id")
    protected Long id;

  	/**
	 * zoneId of the XXSecurityZoneRefUser
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "zone_id")
	protected Long zoneId;

  	/**
	 * userId of the XXSecurityZoneRefUser
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "user_id")
	protected Long userId;

	/**
	 * userName of the XXSecurityZoneRefUser
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "user_name")
	protected String userName;

	/**
	 * userType of the XXSecurityZoneRefUser , 1 for admin,0 for audit user.
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "user_type")
	protected Integer userType;

	/**
	 * This method sets the value to the member attribute <b> id</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param id
	 *            Value to set member attribute <b> id</b>
	 */

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
	 * This method sets the value to the member attribute <b> zoneId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param zoneId
	 *            Value to set member attribute <b> zoneId</b>
	 */
	public void setZoneId(Long zoneId) {
		this.zoneId = zoneId;
	}

	/**
	 * Returns the value for the member attribute <b>zoneId</b>
	 *
	 * @return Date - value of member attribute <b>zoneId</b> .
	 */
	public Long getZoneId() {
		return this.zoneId;
	}

	/**
	 * This method sets the value to the member attribute <b> userId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param userId
	 *            Value to set member attribute <b> userId</b>
	 */
	public void setUserId(Long userId) {
		this.userId = userId;
	}

	/**
	 * Returns the value for the member attribute <b>userId</b>
	 *
	 * @return Date - value of member attribute <b>userId</b> .
	 */
	public Long getUserId() {
		return userId;
	}

	/**
	 * This method sets the value to the member attribute <b> userName</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param userName
	 *            Value to set member attribute <b> userName</b>
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/**
	 * Returns the value for the member attribute <b>userName</b>
	 *
	 * @return Date - value of member attribute <b>userName</b> .
	 */
	public String getUserName() {
		return userName;
	}

	/**
	 * This method sets the value to the member attribute <b> userType</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param i
	 *            Value to set member attribute <b> userType</b>
	 */
	public void setUserType(int i) {
		this.userType = i;
	}

	/**
	 * Returns the value for the member attribute <b>userType</b>
	 *
	 * @return Date - value of member attribute <b>userType</b> .
	 */
	public Integer getUserType() {
		return userType;
	}
	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id, zoneId, userId, userName, userType);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		XXSecurityZoneRefUser other = (XXSecurityZoneRefUser) obj;

		return super.equals(obj) &&
			   Objects.equals(id, other.id) &&
			   Objects.equals(zoneId, other.zoneId) &&
			   Objects.equals(userId, other.userId) &&
			   Objects.equals(userName, other.userName) &&
			   Objects.equals(userType, other.userType);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XXSecurityZoneRefUser [" + super.toString() + " id=" + id + ", zoneId=" + zoneId + ", userId="
				+ userId + ", userName=" + userName +", userType=" + userType +  "]";
	}
}
