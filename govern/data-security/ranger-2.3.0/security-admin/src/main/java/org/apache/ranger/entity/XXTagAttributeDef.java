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
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;

@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_tag_attr_def")
public class XXTagAttributeDef extends XXDBBase implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "X_TAG_ATTR_DEF_SEQ", sequenceName = "X_TAG_ATTR_DEF_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "X_TAG_ATTR_DEF_SEQ")
	@Column(name = "id")
	protected Long id;

	@Column(name = "tag_def_id")
	protected Long tagDefId;

	@Column(name = "name")
	protected String name;

	@Column(name = "type")
	protected String type;

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public Long getId() {
		return id;
	}

	/**
	 * @return the tagDefId
	 */
	public Long getTagDefId() {
		return tagDefId;
	}

	/**
	 * @param tagDefId
	 *            the tagDefId to set
	 */
	public void setTagDefId(Long tagDefId) {
		this.tagDefId = tagDefId;
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
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public int getMyClassType() {
		return AppConstants.CLASS_TYPE_XA_TAG_ATTR_DEF;
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
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((tagDefId == null) ? 0 : tagDefId.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
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
		XXTagAttributeDef other = (XXTagAttributeDef) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (tagDefId == null) {
			if (other.tagDefId != null)
				return false;
		} else if (!tagDefId.equals(other.tagDefId))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
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
		sb.append("tagDefId={").append(tagDefId).append("} ");
		sb.append("name={").append(name).append("} ");
		sb.append("type={").append(type).append("} ");
		sb.append(" }");

		return sb;
	}

}
