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
@Table(name = "x_policy_ref_condition")
public class XXPolicyRefCondition extends XXDBBase implements
		java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyRefCondition
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_ref_condition_SEQ", sequenceName = "x_policy_ref_condition_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_ref_condition_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * policyId of the XXPolicyRefCondition
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "policy_id")
	protected Long policyId;

	/**
	 * conditionDefId of the XXPolicyRefCondition
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "condition_def_id")
	protected Long conditionDefId;

	/**
	 * conditionName of the XXPolicyRefCondition
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "condition_name")
	protected String conditionName;

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
	 * This method sets the value to the member attribute <b> conditionDefId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param conditionDefId
	 *            Value to set member attribute <b> conditionDefId</b>
	 */
	public void setConditionDefId(Long conditionDefId) {
		this.conditionDefId = conditionDefId;
	}

	/**
	 * Returns the value for the member attribute <b>conditionDefId</b>
	 *
	 * @return Date - value of member attribute <b>conditionDefId</b> .
	 */
	public Long getConditionDefId() {
		return conditionDefId;
	}

	/**
	 * This method sets the value to the member attribute <b> conditionName</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param conditionName
	 *            Value to set member attribute <b> conditionName</b>
	 */
	public void setConditionName(String conditionName) {
		this.conditionName = conditionName;
	}

	/**
	 * Returns the value for the member attribute <b>conditionName</b>
	 *
	 * @return Date - value of member attribute <b>conditionName</b> .
	 */
	public String getConditionName() {
		return conditionName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id, policyId, conditionDefId, conditionName);
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

		XXPolicyRefCondition other = (XXPolicyRefCondition) obj;

		return super.equals(obj) &&
			   Objects.equals(id, other.id) &&
			   Objects.equals(policyId, other.policyId) &&
			   Objects.equals(conditionDefId, other.conditionDefId) &&
			   Objects.equals(conditionName, other.conditionName);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XXPolicyRefCondition [" + super.toString() + " id=" + id + ", policyId=" + policyId + ", conditionDefId="
				+ conditionDefId + ", conditionName=" + conditionName + "]";
	}



}