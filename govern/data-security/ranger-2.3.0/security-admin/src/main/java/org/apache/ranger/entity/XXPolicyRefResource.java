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
@Table(name = "x_policy_ref_resource")
public class XXPolicyRefResource extends XXDBBase implements
		java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyRefResource
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_ref_resource_SEQ", sequenceName = "x_policy_ref_resource_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_ref_resource_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * policyId of the XXPolicyRefResource
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "policy_id")
	protected Long policyId;

	/**
	 * resourceDefId of the XXPolicyRefResource
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "resource_def_id")
	protected Long resourceDefId;

	/**
	 * resource_name of the XXPolicyRefResource
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "resource_name")
	protected String resourceName;

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
	 * This method sets the value to the member attribute <b> resourceDefId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param resourceDefId
	 *            Value to set member attribute <b> resourceDefId</b>
	 */
	public void setResourceDefId(Long resourceDefId) {
		this.resourceDefId = resourceDefId;
	}

	/**
	 * Returns the value for the member attribute <b>resourceDefId</b>
	 *
	 * @return Date - value of member attribute <b>resourceDefId</b> .
	 */
	public Long getResourceDefId() {
		return resourceDefId;
	}

	/**
	 * This method sets the value to the member attribute <b> resource_name</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param resourceName
	 *            Value to set member attribute <b> resource_name</b>
	 */
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}

	/**
	 * Returns the value for the member attribute <b>resourceName</b>
	 *
	 * @return Date - value of member attribute <b>resourceName</b> .
	 */
	public String getResourceName() {
		return resourceName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id, policyId, resourceDefId, resourceName);
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

		XXPolicyRefResource other = (XXPolicyRefResource) obj;

		return super.equals(obj) &&
			   Objects.equals(id, other.id) &&
			   Objects.equals(policyId, other.policyId) &&
			   Objects.equals(resourceDefId, other.resourceDefId) &&
			   Objects.equals(resourceName, other.resourceName);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XXPolicyRefResource [" + super.toString() + " id=" + id + ", policyId=" + policyId + ", resourceDefId="
				+ resourceDefId + ", resource_name=" + resourceName + "]";
	}



}