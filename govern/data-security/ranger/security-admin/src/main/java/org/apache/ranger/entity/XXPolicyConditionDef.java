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
@Table(name = "x_policy_condition_def")
public class XXPolicyConditionDef extends XXDBBase implements
		java.io.Serializable {
	private static final long serialVersionUID = 1L;
	/**
	 * id of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Id
	@SequenceGenerator(name = "x_policy_condition_def_SEQ", sequenceName = "x_policy_condition_def_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "x_policy_condition_def_SEQ")
	@Column(name = "id")
	protected Long id;

	/**
	 * defId of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "def_id")
	protected Long defId;

	/**
	 * itemId of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "item_id")
	protected Long itemId;

	/**
	 * name of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "name")
	protected String name;

	/**
	 * evaluator of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "evaluator")
	protected String evaluator;

	/**
	 * evaluatorOptions of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "evaluator_options")
	protected String evaluatorOptions;

	/**
	 * validationRegEx of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "validation_reg_ex")
	protected String validationRegEx;

	/**
	 * validationMessage of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "validation_message")
	protected String validationMessage;

	/**
	 * uiHint of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "ui_hint")
	protected String uiHint;

	/**
	 * label of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "label")
	protected String label;

	/**
	 * description of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "description")
	protected String description;

	/**
	 * rbKeyLabel of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "rb_key_label")
	protected String rbKeyLabel;

	/**
	 * rbKeyDescription of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "rb_key_description")
	protected String rbKeyDescription;

	/**
	 * rbKeyValidationMessage of the XXPolicyConditionDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "rb_key_validation_message")
	protected String rbKeyValidationMessage;

	/**
	 * order of the XXPolicyConditionDef
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
	 * Returns the value for the member attribute <b>itemId</b>
	 *
	 * @return Long - value of member attribute <b>itemId</b> .
	 */
	public Long getItemId() {
		return this.itemId;
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
	 * Returns the value for the member attribute <b>defId</b>
	 *
	 * @return Date - value of member attribute <b>defId</b> .
	 */
	public Long getDefid() {
		return this.defId;
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
	 * This method sets the value to the member attribute <b> evaluator</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param evaluator
	 *            Value to set member attribute <b> evaluator</b>
	 */
	public void setEvaluator(String evaluator) {
		this.evaluator = evaluator;
	}

	/**
	 * Returns the value for the member attribute <b>evaluator</b>
	 *
	 * @return Date - value of member attribute <b>evaluator</b> .
	 */
	public String getEvaluator() {
		return this.evaluator;
	}

	/**
	 * This method sets the value to the member attribute <b>
	 * evaluatorOptions</b> . You cannot set null to the attribute.
	 *
	 * @param evaluatorOptions
	 *            Value to set member attribute <b> evaluatorOptions</b>
	 */
	public void setEvaluatoroptions(String evaluatorOptions) {
		this.evaluatorOptions = evaluatorOptions;
	}

	/**
	 * Returns the value for the member attribute <b>evaluatorOptions</b>
	 *
	 * @return Date - value of member attribute <b>evaluatorOptions</b> .
	 */
	public String getEvaluatoroptions() {
		return this.evaluatorOptions;
	}

	/**
	 * @return the validationRegEx
	 */
	public String getValidationRegEx() {
		return validationRegEx;
	}

	/**
	 * @param validationRegEx the validationRegEx to set
	 */
	public void setValidationRegEx(String validationRegEx) {
		this.validationRegEx = validationRegEx;
	}

	/**
	 * @return the validationMessage
	 */
	public String getValidationMessage() {
		return validationMessage;
	}

	/**
	 * @param validationMessage the validationMessage to set
	 */
	public void setValidationMessage(String validationMessage) {
		this.validationMessage = validationMessage;
	}

	/**
	 * @return the uiHint
	 */
	public String getUiHint() {
		return uiHint;
	}

	/**
	 * @param uiHint the uiHint to set
	 */
	public void setUiHint(String uiHint) {
		this.uiHint = uiHint;
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
	 * You cannot set null to the attribute.
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
	 * @return Date - value of member attribute <b>description</b> .
	 */
	public String getDescription() {
		return this.description;
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
	 * This method sets the value to the member attribute <b>
	 * rbKeyDescription</b> . You cannot set null to the attribute.
	 *
	 * @param rbKeyDescription
	 *            Value to set member attribute <b> rbKeyDescription</b>
	 */
	public void setRbkeydescription(String rbKeyDescription) {
		this.rbKeyDescription = rbKeyDescription;
	}

	/**
	 * Returns the value for the member attribute <b>rbKeyDescription</b>
	 *
	 * @return Date - value of member attribute <b>rbKeyDescription</b> .
	 */
	public String getRbkeydescription() {
		return this.rbKeyDescription;
	}

	/**
	 * @return the rbKeyValidationMessage
	 */
	public String getRbKeyValidationMessage() {
		return rbKeyValidationMessage;
	}

	/**
	 * @param rbKeyValidationMessage the rbKeyValidationMessage to set
	 */
	public void setRbKeyValidationMessage(String rbKeyValidationMessage) {
		this.rbKeyValidationMessage = rbKeyValidationMessage;
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
		XXPolicyConditionDef other = (XXPolicyConditionDef) obj;
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
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (evaluator == null) {
			if (other.evaluator != null) {
				return false;
			}
		} else if (!evaluator.equals(other.evaluator)) {
			return false;
		}
		if (evaluatorOptions == null) {
			if (other.evaluatorOptions != null) {
				return false;
			}
		} else if (!evaluatorOptions.equals(other.evaluatorOptions)) {
			return false;
		}
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}

		if(!equals(validationRegEx, other.validationRegEx) ||
		   !equals(validationMessage, other.validationMessage) ||
		   !equals(uiHint, other.uiHint) ||
		   !equals(rbKeyValidationMessage, other.rbKeyValidationMessage)) {
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
		if (rbKeyDescription == null) {
			if (other.rbKeyDescription != null) {
				return false;
			}
		} else if (!rbKeyDescription.equals(other.rbKeyDescription)) {
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
		String str = "XXPolicyConditionDef={";
		str += super.toString();
		str += " [id=" + id + ", defId=" + defId + ", itemId=" + itemId
				+ ", name=" + name + ", evaluator=" + evaluator
				+ ", evaluatorOptions=" + evaluatorOptions + ", label=" + label
				+ ", validationRegEx=" + validationRegEx
				+ ", validationMessage=" + validationMessage
				+ ", uiHint=" + uiHint
				+ ", description=" + description + ", rbKeyLabel=" + rbKeyLabel
				+ ", rbKeyValidationMessage=" + rbKeyValidationMessage
				+ ", rbKeyDescription=" + rbKeyDescription + ", order=" + order
				+ "]";
		str += "}";
		return str;
	}

}