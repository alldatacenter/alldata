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
@Table(name="x_tag_resource_map")
@XmlRootElement
public class XXTagResourceMap extends XXDBBase implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "X_TAG_RESOURCE_MAP_SEQ", sequenceName = "X_TAG_RESOURCE_MAP_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "X_TAG_RESOURCE_MAP_SEQ")
	@Column(name = "id")
	protected Long id;

	@Column(name = "guid", unique = true, nullable = false, length = 512)
	protected String guid;

	@Column(name = "tag_id")
	protected Long tagId;

	@Column(name = "res_id")
	protected Long resourceId;

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public Long getId() {
		return id;
	}

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
	 * @return the tagId
	 */
	public Long getTagId() {
		return tagId;
	}

	/**
	 * @param tagId
	 *            the tagId to set
	 */
	public void setTagId(Long tagId) {
		this.tagId = tagId;
	}

	/**
	 * @return the resourceId
	 */
	public Long getResourceId() {
		return resourceId;
	}

	/**
	 * @param resourceId
	 *            the resourceId to set
	 */
	public void setResourceId(Long resourceId) {
		this.resourceId = resourceId;
	}

	@Override
	public int getMyClassType() {
		return AppConstants.CLASS_TYPE_XA_TAG_RESOURCE_MAP;
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
		result = prime * result + ((tagId == null) ? 0 : tagId.hashCode());
		result = prime * result + ((resourceId == null) ? 0 : resourceId.hashCode());
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
		XXTagResourceMap other = (XXTagResourceMap) obj;
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
		if (tagId == null) {
			if (other.tagId != null)
				return false;
		} else if (!tagId.equals(other.tagId))
			return false;
		if (resourceId == null) {
			if (other.resourceId != null)
				return false;
		} else if (!resourceId.equals(other.resourceId))
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
		sb.append("tagId={").append(tagId).append("} ");
		sb.append("resourceId={").append(resourceId).append("} ");
		sb.append(" }");

		return sb;
	}

}
