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
@Table(name = "x_policy_item")
public class XXPolicyItem extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyItem
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_item_SEQ", sequenceName = "x_policy_item_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_item_SEQ")
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
	 * policyId of the XXPolicyItem
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "policy_id")
	protected Long policyId;

	/**
	 * delegateAdmin of the XXPolicyItem
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "delegate_admin")
	protected Boolean delegateAdmin;

	/**
	 * item_type of the XXPolicyItem
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "item_type")
	protected Integer itemType;

	/**
	 * isEnabled of the XXPolicyItem
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "is_enabled")
	protected Boolean isEnabled;

	/**
	 * comments of the XXPolicyItem
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "comments")
	protected String comments;

	/**
	 * order of the XXPolicyItem
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
	 * This method sets the value to the member attribute <b> delegateAdmin</b>
	 * . You cannot set null to the attribute.
	 *
	 * @param delegateAdmin
	 *            Value to set member attribute <b> delegateAdmin</b>
	 */
	public void setDelegateAdmin(Boolean delegateAdmin) {
		this.delegateAdmin = delegateAdmin;
	}

	/**
	 * Returns the value for the member attribute <b>delegateAdmin</b>
	 *
	 * @return Date - value of member attribute <b>delegateAdmin</b> .
	 */
	public Boolean getDelegateAdmin() {
		return this.delegateAdmin;
	}

	/**
	 * This method sets the value to the member attribute <b> itemType</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param itemType
	 *            Value to set member attribute <b> itemType</b>
	 */
	public void setItemType(Integer itemType) {
		this.itemType = itemType;
	}

	/**
	 * Returns the value for the member attribute <b>itemType</b>
	 *
	 * @return Integer - value of member attribute <b>itemType</b> .
	 */
	public Integer getItemType() {
		return this.itemType;
	}

	/**
	 * This method sets the value to the member attribute <b> isEnabled</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param isEnabled
	 *            Value to set member attribute <b> isEnabled</b>
	 */
	public void setIsEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	/**
	 * Returns the value for the member attribute <b>isEnabled</b>
	 *
	 * @return Boolean - value of member attribute <b>isEnabled</b> .
	 */
	public Boolean getIsEnabled() {
		return this.isEnabled;
	}

	/**
	 * This method sets the value to the member attribute <b> comments</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param comments
	 *            Value to set member attribute <b> comments</b>
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/**
	 * Returns the value for the member attribute <b>comments</b>
	 *
	 * @return Boolean - value of member attribute <b>comments</b> .
	 */
	public String getComments() {
		return this.comments;
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
		XXPolicyItem other = (XXPolicyItem) obj;
		if (delegateAdmin != other.delegateAdmin) {
			return false;
		}
		if (GUID == null) {
			if (other.GUID != null) {
				return false;
			}
		} else if (!GUID.equals(other.GUID)) {
			return false;
		}
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
		if (policyId == null) {
			if (other.policyId != null) {
				return false;
			}
		} else if (!policyId.equals(other.policyId)) {
			return false;
		}
		if (itemType == null) {
			if (other.itemType != null) {
				return false;
			}
		} else if (!itemType.equals(other.itemType)) {
			return false;
		}
		if (isEnabled == null) {
			if (other.isEnabled != null) {
				return false;
			}
		} else if (!isEnabled.equals(other.isEnabled)) {
			return false;
		}
		if (comments == null) {
			if (other.comments != null) {
				return false;
			}
		} else if (!comments.equals(other.comments)) {
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
		return "XXPolicyItem [" + super.toString() + " id=" + id + ", guid="
				+ GUID + ", policyId=" + policyId
				+ ", delegateAdmin=" + delegateAdmin + ", itemType=" + itemType + ", order=" + order + "]";
	}

}