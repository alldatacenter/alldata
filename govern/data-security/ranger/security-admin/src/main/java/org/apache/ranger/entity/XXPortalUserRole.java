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

/**
 * Role of the user
 *
 */

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.RangerConstants;


@Entity
@Table(name="x_portal_user_role")
@XmlRootElement
public class XXPortalUserRole extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	@Id
	@SequenceGenerator(name="X_PORTAL_USER_ROLE_SEQ",sequenceName="X_PORTAL_USER_ROLE_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_PORTAL_USER_ROLE_SEQ")
	@Column(name="ID")
	protected Long id;
	@Override
	public void setId(Long id) {
		this.id=id;
	}
	@Override
	public Long getId() {
		return id;
	}
	/**
	 * Id of the user
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="USER_ID"  , nullable=false )
	protected Long userId;


	/**
	 * Role of the user
	 * <ul>
	 * <li>The maximum length for this attribute is <b>128</b>.
	 * </ul>
	 *
	 */
	@Column(name="USER_ROLE"   , length=128)
	protected String userRole;

	/**
	 * Status
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::ActiveStatus
	 * </ul>
	 *
	 */
	@Column(name="STATUS"  , nullable=false )
	protected int status = RangerConstants.STATUS_DISABLED;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXPortalUserRole ( ) {
		status = RangerConstants.STATUS_DISABLED;
	}

	/**
	 * This method sets the value to the member attribute <b>userId</b>.
	 * You cannot set null to the attribute.
	 * @param userId Value to set member attribute <b>userId</b>
	 */
	public void setUserId( Long userId ) {
		this.userId = userId;
	}

	/**
	 * Returns the value for the member attribute <b>userId</b>
	 * @return Long - value of member attribute <b>userId</b>.
	 */
	public Long getUserId( ) {
		return this.userId;
	}


	/**
	 * This method sets the value to the member attribute <b>userRole</b>.
	 * You cannot set null to the attribute.
	 * @param userRole Value to set member attribute <b>userRole</b>
	 */
	public void setUserRole( String userRole ) {
		this.userRole = userRole;
	}

	/**
	 * Returns the value for the member attribute <b>userRole</b>
	 * @return String - value of member attribute <b>userRole</b>.
	 */
	public String getUserRole( ) {
		return this.userRole;
	}

	/**
	 * This method sets the value to the member attribute <b>status</b>.
	 * You cannot set null to the attribute.
	 * @param status Value to set member attribute <b>status</b>
	 */
	public void setStatus( int status ) {
		this.status = status;
	}

	/**
	 * Returns the value for the member attribute <b>status</b>
	 * @return int - value of member attribute <b>status</b>.
	 */
	public int getStatus( ) {
		return this.status;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXPortalUserRole={";
		str += super.toString();
		str += "userId={" + userId + "} ";
		str += "userRole={" + userRole + "} ";
		str += "status={" + status + "} ";
		str += "}";
		return str;
	}

	/**
	 * Checks for all attributes except referenced db objects
	 * @return true if all attributes match
	*/
	@Override
	public boolean equals( Object obj) {
		if ( !super.equals(obj) ) {
			return false;
		}
		XXPortalUserRole other = (XXPortalUserRole) obj;
        	if ((this.userId == null && other.userId != null) || (this.userId != null && !this.userId.equals(other.userId))) {
            		return false;
        	}
        	if ((this.userRole == null && other.userRole != null) || (this.userRole != null && !this.userRole.equals(other.userRole))) {
            		return false;
        	}
		if( this.status != other.status ) return false;
		return true;
	}
	public static String getEnumName(String fieldName ) {
		if( "status".equals(fieldName) ) {
			return "CommonEnums.ActiveStatus";
		}
		//Later TODO
		//return super.getEnumName(fieldName);
		return null;
	}

}
