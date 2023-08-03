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
@Table(name = "x_policy_item_datamask")
public class XXPolicyItemDataMaskInfo extends XXDBBase implements
		java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyItemDataMaskInfo
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_item_datamask_SEQ", sequenceName = "x_policy_item_datamask_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_item_datamask_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * policyItemId of the XXPolicyItemDataMaskInfo
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "policy_item_id")
	protected Long policyItemId;

	/**
	 * type of the XXPolicyItemDataMaskInfo
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "type")
	protected Long type;

	/**
	 * isAllowed of the XXPolicyItemDataMaskInfo
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "condition_expr")
	protected String conditionExpr;

	/**
	 * order of the XXPolicyItemDataMaskInfo
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "value_expr")
	protected String valueExpr;

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
	 * This method sets the value to the member attribute <b> policyItemId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param policyItemId
	 *            Value to set member attribute <b> policyItemId</b>
	 */
	public void setPolicyItemId(Long policyItemId) {
		this.policyItemId = policyItemId;
	}

	/**
	 * Returns the value for the member attribute <b>policyItemId</b>
	 *
	 * @return Date - value of member attribute <b>policyItemId</b> .
	 */
	public Long getPolicyItemId() {
		return this.policyItemId;
	}

	/**
	 * This method sets the value to the member attribute <b> type</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param type
	 *            Value to set member attribute <b> type</b>
	 */
	public void setType(Long type) {
		this.type = type;
	}

	/**
	 * Returns the value for the member attribute <b>type</b>
	 *
	 * @return Date - value of member attribute <b>type</b> .
	 */
	public Long getType() {
		return this.type;
	}

	/**
	 * This method sets the value to the member attribute <b> conditionExpr</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param conditionExpr
	 *            Value to set member attribute <b> conditionExpr</b>
	 */
	public void setConditionExpr(String conditionExpr) {
		this.conditionExpr = conditionExpr;
	}

	/**
	 * Returns the value for the member attribute <b>valueExpr</b>
	 *
	 * @return String - value of member attribute <b>valueExpr</b> .
	 */
	public String getConditionExpr() {
		return this.valueExpr;
	}

	/**
	 * This method sets the value to the member attribute <b> valueExpr</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param valueExpr
	 *            Value to set member attribute <b> valueExpr</b>
	 */
	public void setValueExpr(String valueExpr) {
		this.valueExpr = valueExpr;
	}

	/**
	 * Returns the value for the member attribute <b>valueExpr</b>
	 *
	 * @return String - value of member attribute <b>valueExpr</b> .
	 */
	public String getValueExpr() {
		return this.valueExpr;
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
		XXPolicyItemDataMaskInfo other = (XXPolicyItemDataMaskInfo) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (conditionExpr == null) {
			if (other.conditionExpr != null) {
				return false;
			}
		} else if (!conditionExpr.equals(other.conditionExpr)) {
			return false;
		}
		if (valueExpr == null) {
			if (other.valueExpr != null) {
				return false;
			}
		} else if (!valueExpr.equals(other.valueExpr)) {
			return false;
		}
		if (policyItemId == null) {
			if (other.policyItemId != null) {
				return false;
			}
		} else if (!policyItemId.equals(other.policyItemId)) {
			return false;
		}
		if (type == null) {
			if (other.type != null) {
				return false;
			}
		} else if (!type.equals(other.type)) {
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
		return "XXPolicyItemDataMaskInfo [" + super.toString() + " id=" + id
				+ ", policyItemId=" + policyItemId + ", type=" + type
				+ ", conditionExpr=" + conditionExpr + ", valueExpr=" + valueExpr + "]";
	}

}
