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

import java.io.Serializable;
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


/**
 * The persistent class for the x_policy_ref_group database table.
 * 
 */
@Entity
@Cacheable
@XmlRootElement
@Table(name="x_policy_ref_group")
public class XXPolicyRefGroup extends XXDBBase implements Serializable {

	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyRefGroup
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_ref_group_SEQ", sequenceName = "x_policy_ref_group_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_ref_group_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * policyId of the XXPolicyRefGroup
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "policy_id")
	protected Long policyId;

	/**
	 * groupId of the XXPolicyRefGroup
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "group_id")
	protected Long groupId;

	/**
	 * groupName of the XXPolicyRefGroup
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "group_name")
	protected String groupName;

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
	 * This method sets the value to the member attribute <b> groupId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param groupId
	 *            Value to set member attribute <b> groupId</b>
	 */
	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	/**
	 * Returns the value for the member attribute <b>groupId</b>
	 *
	 * @return Date - value of member attribute <b>groupId</b> .
	 */
	public Long getGroupId() {
		return groupId;
	}

	/**
	 * This method sets the value to the member attribute <b> groupName</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param groupName
	 *            Value to set member attribute <b> groupName</b>
	 */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/**
	 * Returns the value for the member attribute <b>groupName</b>
	 *
	 * @return Date - value of member attribute <b>groupName</b> .
	 */
	public String getGroupName() {
		return groupName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id, policyId, groupId, groupName);
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

		XXPolicyRefGroup other = (XXPolicyRefGroup) obj;

		return super.equals(obj) &&
			   Objects.equals(id, other.id) &&
			   Objects.equals(policyId, other.policyId) &&
			   Objects.equals(groupId, other.groupId) &&
			   Objects.equals(groupName, other.groupName);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XXPolicyRefGroup [" + super.toString() + " id=" + id + ", policyId=" + policyId + ", groupId=" + groupId
				+ ", groupName=" + groupName + "]";
	}

}