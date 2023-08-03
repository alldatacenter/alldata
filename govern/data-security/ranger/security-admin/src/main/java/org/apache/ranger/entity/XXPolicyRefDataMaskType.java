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
@Table(name = "x_policy_ref_datamask_type")
public class XXPolicyRefDataMaskType extends XXDBBase implements
		java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyRefDataMaskType
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_ref_datamask_type_SEQ", sequenceName = "x_policy_ref_datamask_type_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_ref_datamask_type_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * policyId of the XXPolicyRefDataMaskType
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "policy_id")
	protected Long policyId;

	/**
	 * DatamaskDefId of the XXPolicyRefDataMaskType
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "datamask_def_id")
	protected Long dataMaskDefId;

	/**
	 * dataMaskTypeName of the XXPolicyRefDataMaskType
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "datamask_type_name")
	protected String dataMaskTypeName;

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
	 * This method sets the value to the member attribute <b> dataMaskDefId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param dataMaskDefId
	 *            Value to set member attribute <b> dataMaskDefId</b>
	 */
	public void setDataMaskDefId(Long dataMaskDefId) {
		this.dataMaskDefId = dataMaskDefId;
	}

	/**
	 * Returns the value for the member attribute <b>dataMaskDefId</b>
	 *
	 * @return Date - value of member attribute <b>dataMaskDefId</b> .
	 */
	public Long getDataMaskDefId() {
		return dataMaskDefId;
	}

	/**
	 * This method sets the value to the member attribute <b> dataMaskTypeName</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param dataMaskTypeName
	 *            Value to set member attribute <b> dataMaskTypeName</b>
	 */
	public void setDataMaskTypeName(String dataMaskTypeName) {
		this.dataMaskTypeName = dataMaskTypeName;
	}

	/**
	 * Returns the value for the member attribute <b>dataMaskTypeName</b>
	 *
	 * @return Date - value of member attribute <b>dataMaskTypeName</b> .
	 */
	public String getDataMaskTypeName() {
		return dataMaskTypeName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id, policyId, dataMaskDefId, dataMaskTypeName);
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

		XXPolicyRefDataMaskType other = (XXPolicyRefDataMaskType) obj;

		return super.equals(obj) &&
			   Objects.equals(id, other.id) &&
			   Objects.equals(policyId, other.policyId) &&
			   Objects.equals(dataMaskDefId, other.dataMaskDefId) &&
			   Objects.equals(dataMaskTypeName, other.dataMaskTypeName);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XXPolicyRefDataMaskType [" + super.toString() + " id=" + id + ", policyId=" + policyId + ", dataMaskDefId="
				+ dataMaskDefId + ", dataMaskTypeName=" + dataMaskTypeName + "]";
	}



}