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
 * The persistent class for the x_role_ref_user database table.
 *
 */
@Entity
@Cacheable
@XmlRootElement
@Table(name="x_role_ref_user")
public class XXRoleRefUser extends XXDBBase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id of the XXRoleRefUser
     * <ul>
     * </ul>
     *
     */
    @Id
    @SequenceGenerator(name = "x_role_ref_user_SEQ", sequenceName = "x_role_ref_user_SEQ", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "x_role_ref_user_SEQ")
    @Column(name = "id")
    protected Long id;

    /**
     * roleId of the XXRoleRefUser
     * <ul>
     * </ul>
     *
     */
    @Column(name = "role_id")
    protected Long roleId;

    /**
     * userId of the XXRoleRefUser
     * <ul>
     * </ul>
     *
     */
    @Column(name = "user_id")
    protected Long userId;

    /**
     * userName of the XXRoleRefUser
     * <ul>
     * </ul>
     *
     */
    @Column(name = "user_name")
    protected String userName;

    /**
     * userType of the XXRoleRefGroup
     * <ul>
     * </ul>
     *
     */
    @Column(name = "priv_type")
    protected Integer userType;

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
     * This method sets the value to the member attribute <b> userId</b> .
     * You cannot set null to the attribute.
     *
     * @param userId
     *            Value to set member attribute <b> userId</b>
     */
    public void setUserId(Long userId) {
        this.userId = userId;
    }

    /**
     * Returns the value for the member attribute <b>userId</b>
     *
     * @return Long - value of member attribute <b>userId</b> .
     */
    public Long getUserId() {
        return this.userId;
    }

    /**
     * This method sets the value to the member attribute <b> userName</b> .
     * You cannot set null to the attribute.
     *
     * @param userName
     *            Value to set member attribute <b> userName</b>
     */
    public void setUserName(String userName) {
        this.userName = userName;
    }

    /**
     * Returns the value for the member attribute <b>userName</b>
     *
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Returns the value for the member attribute <b>userType</b>
     *
     * @return userType - value of member attribute <b>userType</b> .
     */
    public Integer getUserType() {
		return userType;
	}

    /**
     * This method sets the value to the member attribute <b> userType</b> . You
     * cannot set null to the attribute.
     *
     * @param userType
     *            Value to set member attribute <b> userType</b>
     */
	public void setUserType(Integer userType) {
		this.userType = userType;
	}

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, roleId, userId, userName, userType);
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

        XXRoleRefUser other = (XXRoleRefUser) obj;

        return super.equals(obj) &&
                Objects.equals(id, other.id) &&
                Objects.equals(roleId, other.roleId) &&
                Objects.equals(userId, other.userId) &&
                Objects.equals(userName, other.userName) &&
                Objects.equals(userType, other.userType);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "XXRoleRefUser [" + super.toString() + " id=" + id + ", roleId=" + roleId +", userId=" + userId 
                + ", userName=" + userName  + ", userType=" + userType + "]";
    }
}