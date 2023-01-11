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

import java.io.Serializable;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Version;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;

@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_tag_def")
public class XXTagDef extends XXDBBase implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "X_TAG_DEF_SEQ", sequenceName = "X_TAG_DEF_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "X_TAG_DEF_SEQ")
	@Column(name = "id")
	protected Long id;

	@Column(name = "guid", unique = true, nullable = false, length = 512)
	protected String guid;

	@Version
	@Column(name = "version")
	protected Long version;

	@Column(name = "is_enabled")
	protected Boolean isEnabled;

	@Column(name = "name")
	protected String name;

	@Column(name = "source")
	protected String source;

	@Column(name = "tag_attrs_def_text")
	protected String tagAttrDefs;

	/**
	 * @return the guid
	 */
	public String getGuid() {
		return guid;
	}

	/**
	 * @param guid
	 *            the guid to set
	 */
	public void setGuid(String guid) {
		this.guid = guid;
	}

	/**
	 * @return the version
	 */
	public Long getVersion() {
		return version;
	}

	/**
	 * @param version
	 *            the version to set
	 */
	public void setVersion(Long version) {
		this.version = version;
	}

	/**
	 * @return the isEnabled
	 */
	public Boolean getIsEnabled() {
		return isEnabled;
	}

	/**
	 * @param isEnabled
	 *            the isEnabled to set
	 */
	public void setIsEnabled(Boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source
	 *            the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	public String getTagAttrDefs() { return tagAttrDefs; }

	public void setTagAttrDefs(String tagAttrDefs) { this.tagAttrDefs = tagAttrDefs; }

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public int getMyClassType() {
		return AppConstants.CLASS_TYPE_XA_TAG_DEF;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((guid == null) ? 0 : guid.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((isEnabled == null) ? 0 : isEnabled.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		result = prime * result + ((tagAttrDefs == null) ? 0 : tagAttrDefs.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		XXTagDef other = (XXTagDef) obj;
		if (guid == null) {
			if (other.guid != null)
				return false;
		} else if (!guid.equals(other.guid))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isEnabled == null) {
			if (other.isEnabled != null)
				return false;
		} else if (!isEnabled.equals(other.isEnabled))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		if (tagAttrDefs == null) {
			if (other.tagAttrDefs != null)
				return false;
		} else if (!tagAttrDefs.equals(other.tagAttrDefs))
			return false;
		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb);
		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("{ ");
		sb.append(super.toString() + "} ");
		sb.append("id={").append(id).append("} ");
		sb.append("guid={").append(guid).append("} ");
		sb.append("version={").append(version).append("} ");
		sb.append("isEnabled={").append(isEnabled).append("} ");
		sb.append("source={").append(source).append("} ");
		sb.append("name={").append(name).append("} ");
		sb.append("tagAttrDefs={").append(tagAttrDefs).append("} ");
		sb.append(" }");

		return sb;
	}

}
