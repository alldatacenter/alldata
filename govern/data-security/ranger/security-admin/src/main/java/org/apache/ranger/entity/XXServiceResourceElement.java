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
@Table(name="x_service_resource_element")
@XmlRootElement
public class XXServiceResourceElement extends XXDBBase implements Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "X_SERVICE_RESOURCE_ELEMENT_SEQ", sequenceName = "X_SERVICE_RESOURCE_ELEMENT_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "X_SERVICE_RESOURCE_ELEMENT_SEQ")
	@Column(name = "id")
	protected Long id;

	@Column(name = "res_def_id")
	protected Long resDefId;

	@Column(name = "res_id")
	protected Long resourceId;

	@Column(name = "is_excludes")
	protected Boolean isExcludes;

	@Column(name = "is_recursive")
	protected Boolean isRecursive;

	@Override
	public void setId(Long id) {
		this.id = id;
	}

	@Override
	public Long getId() {
		return id;
	}

	/**
	 * @return the resDefId
	 */
	public Long getResDefId() {
		return resDefId;
	}

	/**
	 * @param resDefId
	 *            the resDefId to set
	 */
	public void setResDefId(Long resDefId) {
		this.resDefId = resDefId;
	}

	/**
	 * @return the isExcludes
	 */
	public Boolean getIsExcludes() {
		return isExcludes;
	}

	/**
	 * @param isExcludes
	 *            the isExcludes to set
	 */
	public void setIsExcludes(Boolean isExcludes) {
		this.isExcludes = isExcludes;
	}

	/**
	 * @return the isRecursive
	 */
	public Boolean getIsRecursive() {
		return isRecursive;
	}

	/**
	 * @param isRecursive
	 *            the isRecursive to set
	 */
	public void setIsRecursive(Boolean isRecursive) {
		this.isRecursive = isRecursive;
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
		return AppConstants.CLASS_TYPE_XA_SERVICE_RESOURCE_ELEMENT;
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
		result = prime * result + ((isExcludes == null) ? 0 : isExcludes.hashCode());
		result = prime * result + ((isRecursive == null) ? 0 : isRecursive.hashCode());
		result = prime * result + ((resDefId == null) ? 0 : resDefId.hashCode());
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
		XXServiceResourceElement other = (XXServiceResourceElement) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isExcludes == null) {
			if (other.isExcludes != null)
				return false;
		} else if (!isExcludes.equals(other.isExcludes))
			return false;
		if (isRecursive == null) {
			if (other.isRecursive != null)
				return false;
		} else if (!isRecursive.equals(other.isRecursive))
			return false;
		if (resDefId == null) {
			if (other.resDefId != null)
				return false;
		} else if (!resDefId.equals(other.resDefId))
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
		sb.append("resDefId={").append(resDefId).append("} ");
		sb.append("resourceId={").append(resourceId).append("} ");
		sb.append("isExcludes={").append(isExcludes).append("} ");
		sb.append("isRecursive={").append(isRecursive).append("} ");
		sb.append(" }");

		return sb;
	}

}
