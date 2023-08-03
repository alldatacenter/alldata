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
@Table(name = "x_datamask_type_def")
public class XXDataMaskTypeDef extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXDataMaskTypeDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_datamask_type_def_SEQ", sequenceName = "x_datamask_type_def_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_datamask_type_def_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * defId of the XXDataMaskTypeDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "def_id")
	protected Long defId;

	/**
	 * itemId of the XXDataMaskTypeDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "item_id")
	protected Long itemId;

	/**
	 * name of the XXDataMaskTypeDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "name")
	protected String name;

	/**
	 * label of the XXDataMaskTypeDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "label")
	protected String label;

	/**
	 * description of the XXDataMaskTypeDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "description")
	protected String description;

	/**
	 * transformer of the XXDataMaskTypeDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "transformer")
	protected String transformer;

	/**
	 * dataMaskOptions of the XXDataMaskTypeDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "datamask_options")
	protected String dataMaskOptions;

	/**
	 * rbKeyLabel of the XXDataMaskTypeDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "rb_key_label")
	protected String rbKeyLabel;

	/**
	 * rbKeyDescription of the XXDataMaskTypeDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "rb_key_description")
	protected String rbKeyDescription;

	/**
	 * order of the XXDataMaskTypeDef
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
	 * This method sets the value to the member attribute <b> defId</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param defId
	 *            Value to set member attribute <b> defId</b>
	 */
	public void setDefid(Long defId) {
		this.defId = defId;
	}

	/**
	 * Returns the value for the member attribute <b>defId</b>
	 *
	 * @return Date - value of member attribute <b>defId</b> .
	 */
	public Long getDefid() {
		return this.defId;
	}

	/**
	 * This method sets the value to the member attribute <b> itemId</b> . You
	 * cannot set null to the attribute.
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
	 * This method sets the value to the member attribute <b> description</b> .
	 *
	 * @param description
	 *            Value to set member attribute <b> description</b>
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Returns the value for the member attribute <b>description</b>
	 *
	 * @return String - value of member attribute <b>description</b> .
	 */
	public String getDescription() {
		return this.description;
	}

	/**
	 * This method sets the value to the member attribute <b> transformer</b> .
	 *
	 * @param transformer
	 *            Value to set member attribute <b> transformer</b>
	 */
	public void setTransformer(String transformer) {
		this.transformer = transformer;
	}

	/**
	 * Returns the value for the member attribute <b>transformer</b>
	 *
	 * @return String - value of member attribute <b>transformer</b> .
	 */
	public String getTransformer() {
		return this.transformer;
	}

	/**
	 * This method sets the value to the member attribute <b> dataMaskOptions</b> .
	 *
	 * @param dataMaskOptions
	 *            Value to set member attribute <b> dataMaskOptions</b>
	 */
	public void setDataMaskOptions(String dataMaskOptions) {
		this.dataMaskOptions = dataMaskOptions;
	}

	/**
	 * Returns the value for the member attribute <b>dataMaskOptions</b>
	 *
	 * @return String - value of member attribute <b>dataMaskOptions</b> .
	 */
	public String getDataMaskOptions() {
		return this.dataMaskOptions;
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
	 * This method sets the value to the member attribute <b> rbKeyDescription</b> .
	 *
	 * @param rbKeyDescription
	 *            Value to set member attribute <b> rbKeyDescription</b>
	 */
	public void setRbKeyDescription(String rbKeyDescription) {
		this.rbKeyDescription = rbKeyDescription;
	}

	/**
	 * Returns the value for the member attribute <b>rbKeyDescription</b>
	 *
	 * @return String - value of member attribute <b>rbKeyDescription</b> .
	 */
	public String getRbKeyDescription() {
		return this.rbKeyDescription;
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
		XXDataMaskTypeDef other = (XXDataMaskTypeDef) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (defId == null) {
			if (other.defId != null) {
				return false;
			}
		} else if (!defId.equals(other.defId)) {
			return false;
		}
		if (itemId == null) {
			if (other.itemId != null) {
				return false;
			}
		} else if (!itemId.equals(other.itemId)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (label == null) {
			if (other.label != null) {
				return false;
			}
		} else if (!label.equals(other.label)) {
			return false;
		}
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (transformer == null) {
			if (other.transformer != null) {
				return false;
			}
		} else if (!transformer.equals(other.transformer)) {
			return false;
		}
		if (dataMaskOptions == null) {
			if (other.dataMaskOptions != null) {
				return false;
			}
		} else if (!dataMaskOptions.equals(other.dataMaskOptions)) {
			return false;
		}
		if (rbKeyLabel == null) {
			if (other.rbKeyLabel != null) {
				return false;
			}
		} else if (!rbKeyLabel.equals(other.rbKeyLabel)) {
			return false;
		}
		if (rbKeyDescription == null) {
			if (other.rbKeyDescription != null) {
				return false;
			}
		} else if (!rbKeyDescription.equals(other.rbKeyDescription)) {
			return false;
		}
		if (order == null) {
			if (other.order != null) {
				return false;
			}
		} else if (!order.equals(other.order)) {
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
		return "XXDataMaskTypeDef [" + super.toString() + " id=" + id
				+ ", defId=" + defId + ", itemId=" + itemId + ", name=" + name + ", label=" + label
				+ ", description=" + description + ", transformer=" + transformer + ", dataMaskOptions=" + dataMaskOptions
				+ ", rbKeyLabel=" + rbKeyLabel + ", rbKeyDescription=" + rbKeyDescription + ", order=" + order + "]";
	}

}
