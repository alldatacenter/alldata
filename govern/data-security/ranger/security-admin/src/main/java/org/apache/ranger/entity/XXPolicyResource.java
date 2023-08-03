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

@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_policy_resource")
public class XXPolicyResource extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyResource
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_resource_SEQ", sequenceName = "x_policy_resource_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_resource_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * policyId of the XXPolicyResource
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "policy_id")
	protected Long policyId;

	/**
	 * resDefId of the XXPolicyResource
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "res_def_id")
	protected Long resDefId;

	/**
	 * isExcludes of the XXPolicyResource
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "is_excludes")
	protected boolean isExcludes;

	/**
	 * isRecursive of the XXPolicyResource
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "is_recursive")
	protected boolean isRecursive;

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
	 * This method sets the value to the member attribute <b> policyId</b> . You
	 * cannot set null to the attribute.
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
	public Long getPolicyid() {
		return this.policyId;
	}

	/**
	 * This method sets the value to the member attribute <b> resDefId</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param resDefId
	 *            Value to set member attribute <b> resDefId</b>
	 */
	public void setResDefId(Long resDefId) {
		this.resDefId = resDefId;
	}

	/**
	 * Returns the value for the member attribute <b>resDefId</b>
	 *
	 * @return Date - value of member attribute <b>resDefId</b> .
	 */
	public Long getResdefid() {
		return this.resDefId;
	}

	/**
	 * This method sets the value to the member attribute <b> isExcludes</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param isExcludes
	 *            Value to set member attribute <b> isExcludes</b>
	 */
	public void setIsExcludes(boolean isExcludes) {
		this.isExcludes = isExcludes;
	}

	/**
	 * Returns the value for the member attribute <b>isExcludes</b>
	 *
	 * @return Date - value of member attribute <b>isExcludes</b> .
	 */
	public boolean getIsexcludes() {
		return this.isExcludes;
	}

	/**
	 * This method sets the value to the member attribute <b> isRecursive</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param isRecursive
	 *            Value to set member attribute <b> isRecursive</b>
	 */
	public void setIsRecursive(boolean isRecursive) {
		this.isRecursive = isRecursive;
	}

	/**
	 * Returns the value for the member attribute <b>isRecursive</b>
	 *
	 * @return Date - value of member attribute <b>isRecursive</b> .
	 */
	public boolean getIsrecursive() {
		return this.isRecursive;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!super.equals(obj)) {
			return false;
		}
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		XXPolicyResource other = (XXPolicyResource) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (isExcludes != other.isExcludes) {
			return false;
		}
		if (isRecursive != other.isRecursive) {
			return false;
		}
		if (policyId == null) {
			if (other.policyId != null) {
				return false;
			}
		} else if (!policyId.equals(other.policyId)) {
			return false;
		}
		if (resDefId == null) {
			if (other.resDefId != null) {
				return false;
			}
		} else if (!resDefId.equals(other.resDefId)) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XXPolicyResource [" + super.toString() + " id=" + id
				+ ", policyId=" + policyId + ", resDefId=" + resDefId
				+ ", isExcludes=" + isExcludes + ", isRecursive=" + isRecursive
				+ "]";
	}

}