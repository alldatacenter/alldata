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

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_policy_ref_user")
public class XXPolicyRefUser extends XXDBBase implements
		java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyRefUser
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_ref_user_SEQ", sequenceName = "x_policy_ref_user_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_ref_user_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * policyId of the XXPolicyRefUser
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "policy_id")
	protected Long policyId;

	/**
	 * userId of the XXPolicyRefUser
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "user_id")
	protected Long userId;

	/**
	 * userName of the XXPolicyRefUser
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "user_name")
	protected String userName;

	/**
	 * This method sets the value to the member attribute <b> id</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param id
	 *            Value to set member attribute <b> id</b>
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Returns the value for the member attribute <b>id</b>
	 *
	 * @return Date - value of member attribute <b>id</b> .
	 */
	public Long getId() {
		return this.id;
	}

	/**
	 * This method sets the value to the member attribute <b> policyId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param policyId
	 *            Value to set member attribute <b> policyId</b>
	 */
	public void setPolicyId(Long policyId) {
		this.policyId = policyId;
	}

	/**
	 * Returns the value for the member attribute <b>policyId</b>
	 *
	 * @return Date - value of member attribute <b>policyId</b> .
	 */
	public Long getPolicyId() {
		return this.policyId;
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

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id, policyId, userId, userName);
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

		XXPolicyRefUser other = (XXPolicyRefUser) obj;

		return super.equals(obj) &&
			   Objects.equals(id, other.id) &&
			   Objects.equals(policyId, other.policyId) &&
			   Objects.equals(userId, other.userId) &&
			   Objects.equals(userName, other.userName);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XXPolicyRefUser [" + super.toString() + " id=" + id + ", policyId=" + policyId + ", userId="
				+ userId + ", userName=" + userName +  "]";
	}



}