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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.RangerCommonEnums;

@Entity
@Table(name="x_group_module_perm")
@XmlRootElement

public class XXGroupPermission extends XXDBBase implements java.io.Serializable {

	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="X_GROUP_MODULE_PERM_SEQ",sequenceName="X_GROUP_MODULE_PERM_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_GROUP_MODULE_PERM_SEQ")
	@Column(name="ID")
	protected Long id;

	@Column(name="GROUP_ID" , nullable=false)
	protected Long groupId;

	@Column(name="MODULE_ID" , nullable=false)
	protected Long moduleId;

	@Column(name="IS_ALLOWED" , nullable=false)
	protected Integer isAllowed;

	public XXGroupPermission() {
		isAllowed = RangerCommonEnums.STATUS_ENABLED;
	}

	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * @return the groupId
	 */
	public Long getGroupId() {
		return groupId;
	}
	/**
	 * @param groupId the groupId to set
	 */
	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	/**
	 * @return the moduleId
	 */
	public Long getModuleId() {
		return moduleId;
	}
	/**
	 * @param moduleId the moduleId to set
	 */
	public void setModuleId(Long moduleId) {
		this.moduleId = moduleId;
	}

	/**
	 * @return the isAllowed
	 */
	public Integer getIsAllowed() {
		return isAllowed;
	}
	/**
	 * @param isAllowed the isAllowed to set
	 */
	public void setIsAllowed(Integer isAllowed) {
		this.isAllowed = isAllowed;
	}

	@Override
	public int getMyClassType() {
		return AppConstants.CLASS_TYPE_RANGER_GROUP_PERMISSION;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		XXGroupPermission other = (XXGroupPermission) obj;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (isAllowed == null) {
			if (other.isAllowed != null)
				return false;
		} else if (!isAllowed.equals(other.isAllowed))
			return false;
		if (moduleId == null) {
			if (other.moduleId != null)
				return false;
		} else if (!moduleId.equals(other.moduleId))
			return false;
		return true;
	}

	@Override
	public String toString() {

		String str = "XXGroupPermission={";
		str += super.toString();
		str += "id={" + id + "} ";
		str += "groupId={" + groupId + "} ";
		str += "moduleId={" + moduleId + "} ";
		str += "isAllowed={" + isAllowed + "} ";
		str += "}";

		return str;
	}
}