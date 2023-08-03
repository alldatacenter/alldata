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

 package org.apache.ranger.view;

/**
 * Permission map
 *
 */

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.RangerConstants;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXPermMap extends VXDataObject implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Group to which the permission belongs to
	 */
	protected String permGroup;
	/**
	 * Id of the resource
	 */
	protected Long resourceId;
	/**
	 * Id of the group
	 */
	protected Long groupId;
	/**
	 * Id of the user
	 */
	protected Long userId;
	/**
	 * Permission for user or group
	 * This attribute is of type enum CommonEnums::XAPermForType
	 */
	protected int permFor = AppConstants.XA_PERM_FOR_UNKNOWN;
	/**
	 * Type of permission
	 * This attribute is of type enum CommonEnums::XAPermType
	 */
	protected int permType = AppConstants.XA_PERM_TYPE_UNKNOWN;
	/**
	 * Grant is true and revoke is false
	 */
	protected boolean grantOrRevoke = true;
	/**
	 * Name of the group
	 */
	protected String groupName;
	/**
	 * Name of the user
	 */
	protected String userName;
	/**
	 * Is recursive
	 * This attribute is of type enum CommonEnums::BooleanValue
	 */
	protected int isRecursive = RangerConstants.BOOL_NONE;
	/**
	 * Is wild card
	 */
	protected boolean isWildCard = true;
	/**
	 * IP address for groups
	 */
	protected String ipAddress;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXPermMap ( ) {
		permFor = AppConstants.XA_PERM_FOR_UNKNOWN;
		permType = AppConstants.XA_PERM_TYPE_UNKNOWN;
		isRecursive = RangerConstants.BOOL_NONE;
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

	/**
	 * This method sets the value to the member attribute <b>groupName</b>.
	 * You cannot set null to the attribute.
	 * @param groupName Value to set member attribute <b>groupName</b>
	 */
	public void setGroupName( String groupName ) {
		this.groupName = groupName;
	}

	/**
	 * Returns the value for the member attribute <b>groupName</b>
	 * @return String - value of member attribute <b>groupName</b>.
	 */
	public String getGroupName( ) {
		return this.groupName;
	}

	/**
	 * This method sets the value to the member attribute <b>userName</b>.
	 * You cannot set null to the attribute.
	 * @param userName Value to set member attribute <b>userName</b>
	 */
	public void setUserName( String userName ) {
		this.userName = userName;
	}

	/**
	 * Returns the value for the member attribute <b>userName</b>
	 * @return String - value of member attribute <b>userName</b>.
	 */
	public String getUserName( ) {
		return this.userName;
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

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_PERM_MAP;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXPermMap={";
		str += super.toString();
		str += "permGroup={" + permGroup + "} ";
		str += "resourceId={" + resourceId + "} ";
		str += "groupId={" + groupId + "} ";
		str += "userId={" + userId + "} ";
		str += "permFor={" + permFor + "} ";
		str += "permType={" + permType + "} ";
		str += "grantOrRevoke={" + grantOrRevoke + "} ";
		str += "groupName={" + groupName + "} ";
		str += "userName={" + userName + "} ";
		str += "isRecursive={" + isRecursive + "} ";
		str += "isWildCard={" + isWildCard + "} ";
		str += "}";
		return str;
	}
}
