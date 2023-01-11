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
@Table(name = "x_policy_resource_map")
public class XXPolicyResourceMap extends XXDBBase implements
		java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyResourceMap
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_resource_map_SEQ", sequenceName = "x_policy_resource_map_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_resource_map_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * resourceId of the XXPolicyResourceMap
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "resource_id")
	protected Long resourceId;

	/**
	 * value of the XXPolicyResourceMap
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "value")
	protected String value;

	/**
	 * order of the XXPolicyResourceMap
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
	 * This method sets the value to the member attribute <b> resourceId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param resourceId
	 *            Value to set member attribute <b> resourceId</b>
	 */
	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
	}

	/**
	 * Returns the value for the member attribute <b>resourceId</b>
	 *
	 * @return Date - value of member attribute <b>resourceId</b> .
	 */
	public Long getResourceid() {
		return this.resourceId;
	}

	/**
	 * This method sets the value to the member attribute <b> value</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param value
	 *            Value to set member attribute <b> value</b>
	 */
	public void setValue(String value) {
		this.value = value;
	}

	/**
	 * Returns the value for the member attribute <b>value</b>
	 *
	 * @return Date - value of member attribute <b>value</b> .
	 */
	public String getValue() {
		return this.value;
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
		XXPolicyResourceMap other = (XXPolicyResourceMap) obj;
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
		if (resourceId == null) {
			if (other.resourceId != null) {
				return false;
			}
		} else if (!resourceId.equals(other.resourceId)) {
			return false;
		}
		if (value == null) {
			if (other.value != null) {
				return false;
			}
		} else if (!value.equals(other.value)) {
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
		return "XXPolicyResourceMap [" + super.toString() + " id=" + id
				+ ", resourceId=" + resourceId + ", value=" + value
				+ ", order=" + order + "]";
	}

}