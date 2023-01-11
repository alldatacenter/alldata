/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlRootElement;

@MappedSuperclass
@XmlRootElement
public abstract class XXServiceDefBase extends XXDBBase implements Serializable {
	private static final long serialVersionUID = 1L;

	/**
	 * Global Id for the object
	 * <ul>
	 * <li>The maximum length for this attribute is <b>512</b>.
	 * </ul>
	 *
	 */
	@Column(name = "guid", unique = true, nullable = false, length = 512)
	protected String guid;
	/**
	 * version of the XXServiceDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Version
	@Column(name = "version")
	protected Long version;

	/**
	 * name of the XXServiceDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "name")
	protected String name;

	/**
	 * displayName of the XXServiceDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "display_name")
	protected String displayName;

	/**
	 * implClassName of the XXServiceDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "impl_class_name")
	protected String implClassName;

	/**
	 * label of the XXServiceDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "label")
	protected String label;

	/**
	 * description of the XXServiceDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "description")
	protected String description;

	/**
	 * options of the XXServiceDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "def_options")
	protected String defOptions;

	/**
	 * rbKeyLabel of the XXServiceDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "rb_key_label")
	protected String rbKeyLabel;

	/**
	 * rbKeyDescription of the XXServiceDef
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "rb_key_description")
	protected String rbKeyDescription;
	/**
	 * isEnabled of the XXPolicy
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "is_enabled")
	protected Boolean isEnabled;

	/**
	 * @return the gUID
	 */
	public String getGuid() {
		return this.guid;
	}

	/**
	 * @param guid
	 *            the gUID to set
	 */
	public void setGuid(String guid) {
		this.guid = guid;
	}

	/**
	 * This method sets the value to the member attribute <b> version</b> . You cannot set null to the attribute.
	 *
	 * @param version
	 *            Value to set member attribute <b> version</b>
	 */
	public void setVersion(Long version) {
		this.version = version;
	}

	/**
	 * Returns the value for the member attribute <b>version</b>
	 *
	 * @return Date - value of member attribute <b>version</b> .
	 */
	public Long getVersion() {
		return this.version;
	}

	/**
	 * This method sets the value to the member attribute <b> name</b> . You cannot set null to the attribute.
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
	 * This method sets the value to the member attribute <b> implClassName</b> . You cannot set null to the attribute.
	 *
	 * @param implClassName
	 *            Value to set member attribute <b> implClassName</b>
	 */
	public void setImplclassname(String implClassName) {
		this.implClassName = implClassName;
	}

	/**
	 * Returns the value for the member attribute <b>implClassName</b>
	 *
	 * @return Date - value of member attribute <b>implClassName</b> .
	 */
	public String getImplclassname() {
		return this.implClassName;
	}

	/**
	 * This method sets the value to the member attribute <b> label</b> . You cannot set null to the attribute.
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
	 * This method sets the value to the member attribute <b> description</b> . You cannot set null to the attribute.
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
	 * This method sets the value to the member attribute <b> defOptions</b> .
	 *
	 * @param options
	 *            Value to set member attribute <b> defOptions</b>
	 */
	public void setDefOptions(String options) {
		this.defOptions = options;
	}

	/**
	 * Returns the value for the member attribute <b>defOptions</b>
	 *
	 * @return String - value of member attribute <b>defOptions</b> .
	 */
	public String getDefOptions() {
		return this.defOptions;
	}

	/**
	 * This method sets the value to the member attribute <b> rbKeyLabel</b> . You cannot set null to the attribute.
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
	 * This method sets the value to the member attribute <b> rbKeyDescription</b> . You cannot set null to the
	 * attribute.
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
	 * This method sets the value to the member attribute <b> isEnabled</b> . You cannot set null to the attribute.
	 *
	 * @param isEnabled
	 *            Value to set member attribute <b> isEnabled</b>
	 */
	public void setIsEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	/**
	 * Returns the value for the member attribute <b>isEnabled</b>
	 *
	 * @return Date - value of member attribute <b>isEnabled</b> .
	 */
	public boolean getIsEnabled() {
		return this.isEnabled;
	}

	/**
	 * This method sets the value to the member attribute <b> displayName</b> . You cannot set null to the attribute.
	 *
	 * @param displayName
	 *            Value to set member attribute <b> displayName</b>
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Returns the value for the member attribute <b>displayName</b>
	 *
	 * @return Date - value of member attribute <b>displayName</b> .
	 */
	public String getDisplayName() {
		return displayName;
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
		if (getClass() != obj.getClass()) {
			return false;
		}
		XXServiceDefBase other = (XXServiceDefBase) obj;
		if (description == null) {
			if (other.description != null) {
				return false;
			}
		} else if (!description.equals(other.description)) {
			return false;
		}
		if (guid == null) {
			if (other.guid != null) {
				return false;
			}
		} else if (!guid.equals(other.guid)) {
			return false;
		}
		if (implClassName == null) {
			if (other.implClassName != null) {
				return false;
			}
		} else if (!implClassName.equals(other.implClassName)) {
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
		if (version == null) {
			if (other.version != null) {
				return false;
			}
		} else if (!version.equals(other.version)) {
			return false;
		}
		if (isEnabled == null) {
			if (other.isEnabled != null) {
				return false;
			}
		} else if (!isEnabled.equals(other.isEnabled)) {
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
		return "XXServiceDefBase [" + super.toString() + " guid=" + guid + ", version=" + version + ", name=" + name +", displayName=" + displayName
				+ ", implClassName=" + implClassName + ", label=" + label + ", description=" + description
				+ ", rbKeyLabel=" + rbKeyLabel + ", rbKeyDescription=" + rbKeyDescription + ", isEnabled" + isEnabled
				+ "]";
	}

}
