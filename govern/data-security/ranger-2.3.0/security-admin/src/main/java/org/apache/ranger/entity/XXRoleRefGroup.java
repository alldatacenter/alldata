/* Licensed to the Apache Software Foundation (ASF) under one
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


/**
 * The persistent class for the x_role_ref_group database table.
 *
 */
@Entity
@Cacheable
@XmlRootElement
@Table(name="x_role_ref_group")
public class XXRoleRefGroup extends XXDBBase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id of the XXRoleRefGroup
     * <ul>
     * </ul>
     *
     */
    @Id
    @SequenceGenerator(name = "x_role_ref_group_SEQ", sequenceName = "x_role_ref_group_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "x_role_ref_group_SEQ")
    @Column(name = "id")
    protected Long id;

    /**
     * roleId of the XXRoleRefGroup
     * <ul>
     * </ul>
     *
     */
    @Column(name = "role_id")
    protected Long roleId;

    /**
     * groupId of the XXRoleRefGroup
     * <ul>
     * </ul>
     *
     */
    @Column(name = "group_id")
    protected Long groupId;

    /**
     * groupName of the XXRoleRefGroup
     * <ul>
     * </ul>
     *
     */
    @Column(name = "group_name")
    protected String groupName;

    /**
     * groupType of the XXRoleRefGroup
     * <ul>
     * </ul>
     *
     */
    @Column(name = "priv_Type")
    protected Integer groupType;

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
     * This method sets the value to the member attribute <b> roleId</b> .
     * You cannot set null to the attribute.
     *
     * @param roleId
     *            Value to set member attribute <b> roleId</b>
     */
    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    /**
     * Returns the value for the member attribute <b>roleId</b>
     *
     * @return Long - value of member attribute <b>roleId</b> .
     */
    public Long getRoleId() {
        return roleId;
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
     * @return Long - value of member attribute <b>groupId</b> .
     */
    public Long getGroupId() {
        return this.groupId;
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
     */
    public String getGroupName() {
        return groupName;
    }

    /**
     * Returns the value for the member attribute <b>groupType</b>
     *
     * @return groupType - value of member attribute <b>groupType</b> .
     */
    public Integer getGroupType() {
		return groupType;
	}

    /**
     * This method sets the value to the member attribute <b> groupType</b> . You
     * cannot set null to the attribute.
     *
     * @param groupType
     *            Value to set member attribute <b> groupType</b>
     */
	public void setGroupType(Integer groupType) {
		this.groupType = groupType;
	}

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, roleId, groupId, groupName, groupType);
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

        XXRoleRefGroup other = (XXRoleRefGroup) obj;

        return super.equals(obj) &&
                Objects.equals(id, other.id) &&
                Objects.equals(roleId, other.roleId) &&
                Objects.equals(groupId, other.groupId) &&
                Objects.equals(groupName, other.groupName) &&
                Objects.equals(groupType, other.groupType);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "XXRoleRefGroup [" + super.toString() + " id=" + id + ", roleId=" + roleId +", groupId=" + groupId 
                + ", groupName=" + groupName + ", groupType=" + groupType + "]";
    }
}