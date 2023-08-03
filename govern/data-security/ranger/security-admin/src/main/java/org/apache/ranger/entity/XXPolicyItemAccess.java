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
@Table(name = "x_policy_item_access")
public class XXPolicyItemAccess extends XXDBBase implements
		java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyItemAccess
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_item_access_SEQ", sequenceName = "x_policy_item_access_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_item_access_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * Global Id for the object
	 * <ul>
	 * <li>The maximum length for this attribute is <b>512</b>.
	 * </ul>
	 *
	 */
	@Column(name = "guid", unique = true, nullable = false, length = 512)
	protected String GUID;
	
	/**
	 * policyItemId of the XXPolicyItemAccess
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "policy_item_id")
	protected Long policyItemId;

	/**
	 * type of the XXPolicyItemAccess
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "type")
	protected Long type;

	/**
	 * isAllowed of the XXPolicyItemAccess
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "is_allowed")
	protected Boolean isAllowed;

	/**
	 * order of the XXPolicyItemAccess
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "sort_order")
	protected Integer order;

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
	 * @return the gUID
	 */
	public String getGUID() {
		return GUID;
	}

	/**
	 * @param gUID
	 *            the gUID to set
	 */
	public void setGUID(String gUID) {
		GUID = gUID;
	}

	/**
	 * This method sets the value to the member attribute <b> policyItemId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param policyItemId
	 *            Value to set member attribute <b> policyItemId</b>
	 */
	public void setPolicyitemid(Long policyItemId) {
		this.policyItemId = policyItemId;
	}

	/**
	 * Returns the value for the member attribute <b>policyItemId</b>
	 *
	 * @return Date - value of member attribute <b>policyItemId</b> .
	 */
	public Long getPolicyitemid() {
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
	 * This method sets the value to the member attribute <b> isAllowed</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param isAllowed
	 *            Value to set member attribute <b> isAllowed</b>
	 */
	public void setIsAllowed(Boolean isAllowed) {
		this.isAllowed = isAllowed;
	}

	/**
	 * Returns the value for the member attribute <b>isAllowed</b>
	 *
	 * @return Date - value of member attribute <b>isAllowed</b> .
	 */
	public Boolean getIsallowed() {
		return this.isAllowed;
	}

	/**
	 * This method sets the value to the member attribute <b> order</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param order
	 *            Value to set member attribute <b> order</b>
	 */
	public void setOrder(Integer order) {
		this.order = order;
	}

	/**
	 * Returns the value for the member attribute <b>order</b>
	 *
	 * @return Date - value of member attribute <b>order</b> .
	 */
	public Integer getOrder() {
		return this.order;
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
		XXPolicyItemAccess other = (XXPolicyItemAccess) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (isAllowed == null) {
			if (other.isAllowed != null) {
				return false;
			}
		} else if (!isAllowed.equals(other.isAllowed)) {
			return false;
		}
		if (order == null) {
			if (other.order != null) {
				return false;
			}
		} else if (!order.equals(other.order)) {
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
		if (GUID == null) {
			if (other.GUID != null) {
				return false;
			}
		} else if (!GUID.equals(other.GUID)) {
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
		return "XXPolicyItemAccess [" + super.toString() + " id=" + id
				+ ", guid=" + GUID + ", policyItemId="
				+ policyItemId + ", type=" + type + ", isAllowed=" + isAllowed
				+ ", order=" + order + "]";
	}

}