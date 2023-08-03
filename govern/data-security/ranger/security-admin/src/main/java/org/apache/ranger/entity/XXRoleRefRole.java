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
 * The persistent class for the x_role_ref_role database table.
 *
 */
@Entity
@Cacheable
@XmlRootElement
@Table(name="x_role_ref_role")
public class XXRoleRefRole extends XXDBBase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id of the XXRoleRefRole
     * <ul>
     * </ul>
     *
     */
    @Id
    @SequenceGenerator(name = "x_role_ref_role_SEQ", sequenceName = "x_role_ref_role_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "x_role_ref_role_SEQ")
    @Column(name = "id")
    protected Long id;

    /**
     * roleId of the XXRoleRefRole
     * <ul>
     * </ul>
     *
     */
    @Column(name = "role_id")
    protected Long roleId;

    /**
     * subRoleId of the XXRoleRefRole
     * <ul>
     * </ul>
     *
     */
    @Column(name = "role_ref_id")
    protected Long subRoleId;

    /**
     * subRoleName of the XXRoleRefRole
     * <ul>
     * </ul>
     *
     */
    @Column(name = "role_name")
    protected String subRoleName;

    /**
     * subRoleType of the XXRoleRefRole
     * <ul>
     * </ul>
     *
     */
    @Column(name = "priv_type")
    protected Integer subRoleType;

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
     * This method sets the value to the member attribute <b> subRoleId</b> .
     * You cannot set null to the attribute.
     *
     * @param subRoleId
     *            Value to set member attribute <b> subRoleId</b>
     */
    public void setSubRoleId(Long subRoleId) {
        this.subRoleId = subRoleId;
    }

    /**
     * Returns the value for the member attribute <b>subRoleId</b>
     *
     * @return Long - value of member attribute <b>subRoleId</b> .
     */
    public Long getSubRoleId() {
        return this.subRoleId;
    }

    /**
     * This method sets the value to the member attribute <b> subRoleName</b> .
     * You cannot set null to the attribute.
     *
     * @param subRoleName
     *            Value to set member attribute <b> subRoleName</b>
     */
    public void setSubRoleName(String subRoleName) {
        this.subRoleName = subRoleName;
    }

    /**
     * Returns the value for the member attribute <b>subRoleName</b>
     *
     */
    public String getSubRoleName() {
        return subRoleName;
    }

    /**
     * Returns the value for the member attribute <b>subRoleType</b>
     *
     * @return subRoleType - value of member attribute <b>subRoleType</b> .
     */
    public Integer getSubRoleType() {
		return subRoleType;
	}

    /**
     * This method sets the value to the member attribute <b> subRoleType</b> . You
     * cannot set null to the attribute.
     *
     * @param subRoleType
     *            Value to set member attribute <b> subRoleType</b>
     */
	public void setSubRoleType(Integer subRoleType) {
		this.subRoleType = subRoleType;
	}

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, roleId, subRoleId, subRoleName, subRoleType);
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

        XXRoleRefRole other = (XXRoleRefRole) obj;

        return super.equals(obj) &&
                Objects.equals(id, other.id) &&
                Objects.equals(roleId, other.roleId) &&
                Objects.equals(subRoleId, other.subRoleId) &&
                Objects.equals(subRoleName, other.subRoleName) &&
        		Objects.equals(subRoleType, other.subRoleType);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "XXRoleRefRole [" + super.toString() + " id=" + id + ", roleId=" + roleId +", subRoleId=" + subRoleId 
                + ", subRoleName=" + subRoleName + ", subRoleType=" + subRoleType + "]";
    }
}