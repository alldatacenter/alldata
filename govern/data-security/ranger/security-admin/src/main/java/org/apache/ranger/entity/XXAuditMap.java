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
 * Audi map
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

import org.apache.ranger.common.AppConstants;


@Entity
@Table(name="x_audit_map")
@XmlRootElement
public class XXAuditMap extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	@Id
	@SequenceGenerator(name="X_AUDIT_MAP_SEQ",sequenceName="X_AUDIT_MAP_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_AUDIT_MAP_SEQ")
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
	 * Id of the resource
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="RES_ID"   )
	protected Long resourceId;


	/**
	 * Id of the group
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="GROUP_ID"   )
	protected Long groupId;


	/**
	 * Id of the user
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="USER_ID"   )
	protected Long userId;


	/**
	 * Type of audit
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::XAAuditType
	 * </ul>
	 *
	 */
	@Column(name="AUDIT_TYPE"  , nullable=false )
	protected int auditType = AppConstants.XA_AUDIT_TYPE_UNKNOWN;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXAuditMap ( ) {
		auditType = AppConstants.XA_AUDIT_TYPE_UNKNOWN;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_AUDIT_MAP;
	}

	/**
	 * This method sets the value to the member attribute <b>resourceId</b>.
	 * You cannot set null to the attribute.
	 * @param resourceId Value to set member attribute <b>resourceId</b>
	 */
	public void setResourceId( Long resourceId ) {
		this.resourceId = resourceId;
	}

	/**
	 * Returns the value for the member attribute <b>resourceId</b>
	 * @return Long - value of member attribute <b>resourceId</b>.
	 */
	public Long getResourceId( ) {
		return this.resourceId;
	}


	/**
	 * This method sets the value to the member attribute <b>groupId</b>.
	 * You cannot set null to the attribute.
	 * @param groupId Value to set member attribute <b>groupId</b>
	 */
	public void setGroupId( Long groupId ) {
		this.groupId = groupId;
	}

	/**
	 * Returns the value for the member attribute <b>groupId</b>
	 * @return Long - value of member attribute <b>groupId</b>.
	 */
	public Long getGroupId( ) {
		return this.groupId;
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
	 * This method sets the value to the member attribute <b>auditType</b>.
	 * You cannot set null to the attribute.
	 * @param auditType Value to set member attribute <b>auditType</b>
	 */
	public void setAuditType( int auditType ) {
		this.auditType = auditType;
	}

	/**
	 * Returns the value for the member attribute <b>auditType</b>
	 * @return int - value of member attribute <b>auditType</b>.
	 */
	public int getAuditType( ) {
		return this.auditType;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXAuditMap={";
		str += super.toString();
		str += "resourceId={" + resourceId + "} ";
		str += "groupId={" + groupId + "} ";
		str += "userId={" + userId + "} ";
		str += "auditType={" + auditType + "} ";
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
		XXAuditMap other = (XXAuditMap) obj;
        	if ((this.resourceId == null && other.resourceId != null) || (this.resourceId != null && !this.resourceId.equals(other.resourceId))) {
            		return false;
        	}
        	if ((this.groupId == null && other.groupId != null) || (this.groupId != null && !this.groupId.equals(other.groupId))) {
            		return false;
        	}
        	if ((this.userId == null && other.userId != null) || (this.userId != null && !this.userId.equals(other.userId))) {
            		return false;
        	}
		if( this.auditType != other.auditType ) return false;
		return true;
	}
	public static String getEnumName(String fieldName ) {
		if( "auditType".equals(fieldName) ) {
			return "CommonEnums.XAAuditType";
		}
		//Later TODO
		//return super.getEnumName(fieldName);
		return null;
	}

}
