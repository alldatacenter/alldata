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
@Table(name = "x_enum_element_def")
public class XXEnumElementDef extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXEnumDefElement
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_enum_element_def_SEQ", sequenceName = "x_enum_element_def_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_enum_element_def_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * enumDefId of the XXEnumDefElement
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "enum_def_id")
	protected Long enumDefId;

	/**
	 * itemId of the XXEnumDefElement
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "item_id")
	protected Long itemId;

	/**
	 * name of the XXEnumDefElement
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "name")
	protected String name;

	/**
	 * label of the XXEnumDefElement
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "label")
	protected String label;

	/**
	 * rbKeyLabel of the XXEnumDefElement
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "rb_key_label")
	protected String rbKeyLabel;

	/**
	 * order of the XXEnumDefElement
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
	 * This method sets the value to the member attribute <b> enumDefId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param enumDefId
	 *            Value to set member attribute <b> enumDefId</b>
	 */
	public void setEnumdefid(Long enumDefId) {
		this.enumDefId = enumDefId;
	}

	/**
	 * Returns the value for the member attribute <b>enumDefId</b>
	 *
	 * @return Date - value of member attribute <b>enumDefId</b> .
	 */
	public Long getEnumdefid() {
		return this.enumDefId;
	}

	/**
	 * This method sets the value to the member attribute <b> itemId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param itemId
	 *            Value to set member attribute <b> itemId</b>
	 */
	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	/**
	 * Returns the value for the member attribute <b>itemId</b>
	 *
	 * @return Long - value of member attribute <b>itemId</b> .
	 */
	public Long getItemId() {
		return this.itemId;
	}

	/**
	 * This method sets the value to the member attribute <b> name</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param name
	 *            Value to set member attribute <b> name</b>
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns the value for the member attribute <b>name</b>
	 *
	 * @return Date - value of member attribute <b>name</b> .
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * This method sets the value to the member attribute <b> label</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param label
	 *            Value to set member attribute <b> label</b>
	 */
	public void setLabel(String label) {
		this.label = label;
	}

	/**
	 * Returns the value for the member attribute <b>label</b>
	 *
	 * @return Date - value of member attribute <b>label</b> .
	 */
	public String getLabel() {
		return this.label;
	}

	/**
	 * This method sets the value to the member attribute <b> rbKeyLabel</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param rbKeyLabel
	 *            Value to set member attribute <b> rbKeyLabel</b>
	 */
	public void setRbkeylabel(String rbKeyLabel) {
		this.rbKeyLabel = rbKeyLabel;
	}

	/**
	 * Returns the value for the member attribute <b>rbKeyLabel</b>
	 *
	 * @return Date - value of member attribute <b>rbKeyLabel</b> .
	 */
	public String getRbkeylabel() {
		return this.rbKeyLabel;
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
		XXEnumElementDef other = (XXEnumElementDef) obj;
		if (enumDefId == null) {
			if (other.enumDefId != null) {
				return false;
			}
		} else if (!enumDefId.equals(other.enumDefId)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (label == null) {
			if (other.label != null) {
				return false;
			}
		} else if (!label.equals(other.label)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (order == null) {
			if (other.order != null) {
				return false;
			}
		} else if (!order.equals(other.order)) {
			return false;
		}
		if (rbKeyLabel == null) {
			if (other.rbKeyLabel != null) {
				return false;
			}
		} else if (!rbKeyLabel.equals(other.rbKeyLabel)) {
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
		return "XXEnumElementDef [" + super.toString() + " id=" + id
				+ ", enumDefId=" + enumDefId + "itemId=" + itemId + ", name=" + name + ", label="
				+ label + ", rbKeyLabel=" + rbKeyLabel + ", order=" + order
				+ "]";
	}

}