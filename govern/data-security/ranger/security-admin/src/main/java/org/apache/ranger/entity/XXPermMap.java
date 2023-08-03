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
 * Permission map
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
import org.apache.ranger.common.RangerConstants;


@Entity
@Table(name="x_perm_map")
@XmlRootElement
public class XXPermMap extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	@Id
	@SequenceGenerator(name="X_PERM_MAP_SEQ",sequenceName="X_PERM_MAP_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_PERM_MAP_SEQ")
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
	 * Group to which the permission belongs to
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1024</b>.
	 * </ul>
	 *
	 */
	@Column(name="PERM_GROUP"   , length=1024)
	protected String permGroup;

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
	 * Permission for user or group
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::XAPermForType
	 * </ul>
	 *
	 */
	@Column(name="PERM_FOR"  , nullable=false )
	protected int permFor = AppConstants.XA_PERM_FOR_UNKNOWN;

	/**
	 * Type of permission
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::XAPermType
	 * </ul>
	 *
	 */
	@Column(name="PERM_TYPE"  , nullable=false )
	protected int permType = AppConstants.XA_PERM_TYPE_UNKNOWN;

	/**
	 * Is recursive
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::BooleanValue
	 * </ul>
	 *
	 */
	@Column(name="IS_RECURSIVE"  , nullable=false )
	protected int isRecursive = RangerConstants.BOOL_NONE;

	/**
	 * Is wild card
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="IS_WILD_CARD"  , nullable=false )
	protected boolean isWildCard = true;

	/**
	 * Grant is true and revoke is false
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="GRANT_REVOKE"  , nullable=false )
	protected boolean grantOrRevoke = true;
	/**
	 * IP address to which the group belongs to
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1024</b>.
	 * </ul>
	 *
	 */
	@Column(name="IP_ADDRESS"   , length=1024)
	protected String ipAddress;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXPermMap ( ) {
		permFor = AppConstants.XA_PERM_FOR_UNKNOWN;
		permType = AppConstants.XA_PERM_TYPE_UNKNOWN;
		isRecursive = RangerConstants.BOOL_NONE;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_PERM_MAP;
	}

	/**
	 * This method sets the value to the member attribute <b>permGroup</b>.
	 * You cannot set null to the attribute.
	 * @param permGroup Value to set member attribute <b>permGroup</b>
	 */
	public void setPermGroup( String permGroup ) {
		this.permGroup = permGroup;
	}

	/**
	 * Returns the value for the member attribute <b>permGroup</b>
	 * @return String - value of member attribute <b>permGroup</b>.
	 */
	public String getPermGroup( ) {
		return this.permGroup;
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
	 * This method sets the value to the member attribute <b>permFor</b>.
	 * You cannot set null to the attribute.
	 * @param permFor Value to set member attribute <b>permFor</b>
	 */
	public void setPermFor( int permFor ) {
		this.permFor = permFor;
	}

	/**
	 * Returns the value for the member attribute <b>permFor</b>
	 * @return int - value of member attribute <b>permFor</b>.
	 */
	public int getPermFor( ) {
		return this.permFor;
	}

	/**
	 * This method sets the value to the member attribute <b>permType</b>.
	 * You cannot set null to the attribute.
	 * @param permType Value to set member attribute <b>permType</b>
	 */
	public void setPermType( int permType ) {
		this.permType = permType;
	}

	/**
	 * Returns the value for the member attribute <b>permType</b>
	 * @return int - value of member attribute <b>permType</b>.
	 */
	public int getPermType( ) {
		return this.permType;
	}

	/**
	 * This method sets the value to the member attribute <b>isRecursive</b>.
	 * You cannot set null to the attribute.
	 * @param isRecursive Value to set member attribute <b>isRecursive</b>
	 */
	public void setIsRecursive( int isRecursive ) {
		this.isRecursive = isRecursive;
	}

	/**
	 * Returns the value for the member attribute <b>isRecursive</b>
	 * @return int - value of member attribute <b>isRecursive</b>.
	 */
	public int getIsRecursive( ) {
		return this.isRecursive;
	}

	/**
	 * This method sets the value to the member attribute <b>isWildCard</b>.
	 * You cannot set null to the attribute.
	 * @param isWildCard Value to set member attribute <b>isWildCard</b>
	 */
	public void setIsWildCard( boolean isWildCard ) {
		this.isWildCard = isWildCard;
	}

	/**
	 * Returns the value for the member attribute <b>isWildCard</b>
	 * @return boolean - value of member attribute <b>isWildCard</b>.
	 */
	public boolean isIsWildCard( ) {
		return this.isWildCard;
	}

	/**
	 * This method sets the value to the member attribute <b>grantOrRevoke</b>.
	 * You cannot set null to the attribute.
	 * @param grantOrRevoke Value to set member attribute <b>grantOrRevoke</b>
	 */
	public void setGrantOrRevoke( boolean grantOrRevoke ) {
		this.grantOrRevoke = grantOrRevoke;
	}

	/**
	 * Returns the value for the member attribute <b>grantOrRevoke</b>
	 * @return boolean - value of member attribute <b>grantOrRevoke</b>.
	 */
	public boolean isGrantOrRevoke( ) {
		return this.grantOrRevoke;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXPermMap={";
		str += super.toString();
		str += "permGroup={" + permGroup + "} ";
		str += "resourceId={" + resourceId + "} ";
		str += "groupId={" + groupId + "} ";
		str += "userId={" + userId + "} ";
		str += "permFor={" + permFor + "} ";
		str += "permType={" + permType + "} ";
		str += "isRecursive={" + isRecursive + "} ";
		str += "isWildCard={" + isWildCard + "} ";
		str += "grantOrRevoke={" + grantOrRevoke + "} ";
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
		XXPermMap other = (XXPermMap) obj;
        	if ((this.permGroup == null && other.permGroup != null) || (this.permGroup != null && !this.permGroup.equals(other.permGroup))) {
            		return false;
        	}
        	if ((this.resourceId == null && other.resourceId != null) || (this.resourceId != null && !this.resourceId.equals(other.resourceId))) {
            		return false;
        	}
        	if ((this.groupId == null && other.groupId != null) || (this.groupId != null && !this.groupId.equals(other.groupId))) {
            		return false;
        	}
        	if ((this.userId == null && other.userId != null) || (this.userId != null && !this.userId.equals(other.userId))) {
            		return false;
        	}
		if( this.permFor != other.permFor ) return false;
		if( this.permType != other.permType ) return false;
		if( this.isRecursive != other.isRecursive ) return false;
		if( this.isWildCard != other.isWildCard ) return false;
		if( this.grantOrRevoke != other.grantOrRevoke ) return false;
		return true;
	}
	public static String getEnumName(String fieldName ) {
		if( "permFor".equals(fieldName) ) {
			return "CommonEnums.XAPermForType";
		}
		if( "permType".equals(fieldName) ) {
			return "CommonEnums.XAPermType";
		}
		if( "isRecursive".equals(fieldName) ) {
			return "CommonEnums.BooleanValue";
		}
		//Later TODO
		//return super.getEnumName(fieldName);
		return null;
	}

}
