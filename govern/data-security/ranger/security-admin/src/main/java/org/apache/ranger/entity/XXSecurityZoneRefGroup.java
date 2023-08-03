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

import java.util.Objects;
import javax.persistence.Cacheable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_security_zone_ref_group")
public class XXSecurityZoneRefGroup extends XXDBBase implements java.io.Serializable{
	private static final long serialVersionUID = 1L;
  	@Id
    @SequenceGenerator(name = "x_sec_zone_ref_group_SEQ", sequenceName = "x_sec_zone_ref_group_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "x_sec_zone_ref_group_SEQ")
    @Column(name = "id")
    protected Long id;

  	/**
	 * zoneId of the XXSecurityZoneRefGroup
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "zone_id")
	protected Long zoneId;

  	/**
	 * groupId of the XXSecurityZoneRefGroup
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "group_id")
	protected Long groupId;

	/**
	 * groupName of the XXSecurityZoneRefGroup
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "group_name")
	protected String groupName;

	/**
	 * groupType of the XXSecurityZoneRefGroup , 1 for admin,0 for audit user.
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name = "group_type")
	protected Integer groupType;

	/**
	 * This method sets the value to the member attribute <b> id</b> . You
	 * cannot set null to the attribute.
	 *
	 * @param id
	 *            Value to set member attribute <b> id</b>
	 */

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public Long getId() {
        return id;
    }

    /**
	 * This method sets the value to the member attribute <b> zoneId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param zoneId
	 *            Value to set member attribute <b> zoneId</b>
	 */
	public void setZoneId(Long zoneId) {
		this.zoneId = zoneId;
	}

	/**
	 * Returns the value for the member attribute <b>zoneId</b>
	 *
	 * @return Date - value of member attribute <b>zoneId</b> .
	 */
	public Long getZoneId() {
		return this.zoneId;
	}

	/**
	 * This method sets the value to the member attribute <b> groupId</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param groupId
	 *            Value to set member attribute <b> groupId</b>
	 */
	public void setGroupId(Long groupId) {
		this.groupId = groupId;
	}

	/**
	 * Returns the value for the member attribute <b>groupId</b>
	 *
	 * @return Date - value of member attribute <b>groupId</b> .
	 */
	public Long getGroupId() {
		return groupId;
	}

	/**
	 * This method sets the value to the member attribute <b> groupName</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param groupName
	 *            Value to set member attribute <b> groupName</b>
	 */
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	/**
	 * Returns the value for the member attribute <b>groupName</b>
	 *
	 * @return Date - value of member attribute <b>groupName</b> .
	 */
	public String getGroupName() {
		return groupName;
	}

	/**
	 * This method sets the value to the member attribute <b> groupType</b> .
	 * You cannot set null to the attribute.
	 *
	 * @param groupType
	 *            Value to set member attribute <b> groupType</b>
	 */
	public void setGroupType(Integer groupType) {
		this.groupType = groupType;
	}

	/**
	 * Returns the value for the member attribute <b>groupType</b>
	 *
	 * @return Date - value of member attribute <b>groupType</b> .
	 */
	public Integer getUserType() {
		return groupType;
	}
	@Override
	public int hashCode() {
		return Objects.hash(super.hashCode(), id, zoneId, groupId, groupName, groupType);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		XXSecurityZoneRefGroup other = (XXSecurityZoneRefGroup) obj;

		return super.equals(obj) &&
			   Objects.equals(id, other.id) &&
			   Objects.equals(zoneId, other.zoneId) &&
			   Objects.equals(groupId, other.groupId) &&
			   Objects.equals(groupName, other.groupName) &&
			   Objects.equals(groupType, other.groupType);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "XXSecurityZoneRefGroup [" + super.toString() + " id=" + id + ", zoneId=" + zoneId + ", groupId="
				+ groupId + ", groupName=" + groupName +", groupType=" + groupType +  "]";
	}
}