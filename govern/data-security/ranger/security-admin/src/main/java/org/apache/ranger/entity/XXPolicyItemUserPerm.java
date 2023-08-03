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
@Table(name = "x_policy_item_user_perm")
public class XXPolicyItemUserPerm extends XXDBBase implements
		java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyItemUserPerm
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_item_user_perm_SEQ", sequenceName = "x_policy_item_user_perm_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_item_user_perm_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * policyItemId of the XXPolicyItemUserPerm
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "policy_item_id")
	protected Long policyItemId;

	/**
	 * userId of the XXPolicyItemUserPerm
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "user_id")
	protected Long userId;

	/**
	 * order of the XXPolicyItemUserPerm
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
	public Long getPolicyitemid() {
		return this.policyItemId;
	}

	/**
	 * This method sets the value to the member attribute <b> userId</b> . You
	 * cannot set null to the attribute.
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
	public Long getUserid() {
		return this.userId;
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
		XXPolicyItemUserPerm other = (XXPolicyItemUserPerm) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
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
		if (userId == null) {
			if (other.userId != null) {
				return false;
			}
		} else if (!userId.equals(other.userId)) {
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
		String str = "XXPolicyItemUserPerm={";
		str += super.toString();
		str +=  " [id=" + id + ", policyItemId="
				+ policyItemId + ", userId=" + userId + ", order=" + order
				+ "]";
		str += "}";
		return str;
	}

}