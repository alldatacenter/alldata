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

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXPortalUser extends VXDataObject implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Login Id for the user
	 */
	protected String loginId;
	/**
	 * Password
	 */
	protected String password;
	/**
	 * Status
	 * This attribute is of type enum CommonEnums::ActivationStatus
	 */
	protected int status;
	/**
	 * Email address of the user
	 */
	protected String emailAddress;
	/**
	 * First name of the user
	 */
	protected String firstName;
	/**
	 * Last name of the user
	 */
	protected String lastName;
	/**
	 * Public name of the user
	 */
	protected String publicScreenName;
	/**
	 * Source of the user
	 * This attribute is of type enum CommonEnums::UserSource
	 */
	protected int userSource;
	/**
	 * Notes for the user
	 */
	protected String notes;
	/**
	 * List of roles for this user
	 */
	protected Collection<String> userRoleList;
	protected Collection<Long> groupIdList;
	protected List<VXUserPermission> userPermList;
	protected List<VXGroupPermission> groupPermissions;


	/**
	 * Additional store attributes.
	 *
	 */
	protected String otherAttributes;

	/**
	 * sync Source Attribute.
	 *
	 */
	protected String syncSource;

	/**
	 * Configuration properties.
	 *
	 */
	protected Map<String, String> configProperties;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXPortalUser ( ) {
		status = 0;
		userSource = 0;
	}

	/**
	 * This method sets the value to the member attribute <b>loginId</b>.
	 * You cannot set null to the attribute.
	 * @param loginId Value to set member attribute <b>loginId</b>
	 */
	public void setLoginId( String loginId ) {
		this.loginId = loginId;
	}

	/**
	 * Returns the value for the member attribute <b>loginId</b>
	 * @return String - value of member attribute <b>loginId</b>.
	 */
	public String getLoginId( ) {
		return this.loginId;
	}

	/**
	 * This method sets the value to the member attribute <b>password</b>.
	 * You cannot set null to the attribute.
	 * @param password Value to set member attribute <b>password</b>
	 */
	public void setPassword( String password ) {
		this.password = password;
	}

	/**
	 * Returns the value for the member attribute <b>password</b>
	 * @return String - value of member attribute <b>password</b>.
	 */
	public String getPassword( ) {
		return this.password;
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
	 * This method sets the value to the member attribute <b>emailAddress</b>.
	 * You cannot set null to the attribute.
	 * @param emailAddress Value to set member attribute <b>emailAddress</b>
	 */
	public void setEmailAddress( String emailAddress ) {
		this.emailAddress = emailAddress;
	}

	/**
	 * Returns the value for the member attribute <b>emailAddress</b>
	 * @return String - value of member attribute <b>emailAddress</b>.
	 */
	public String getEmailAddress( ) {
		return this.emailAddress;
	}

	/**
	 * This method sets the value to the member attribute <b>firstName</b>.
	 * You cannot set null to the attribute.
	 * @param firstName Value to set member attribute <b>firstName</b>
	 */
	public void setFirstName( String firstName ) {
		this.firstName = firstName;
	}

	/**
	 * Returns the value for the member attribute <b>firstName</b>
	 * @return String - value of member attribute <b>firstName</b>.
	 */
	public String getFirstName( ) {
		return this.firstName;
	}

	/**
	 * This method sets the value to the member attribute <b>lastName</b>.
	 * You cannot set null to the attribute.
	 * @param lastName Value to set member attribute <b>lastName</b>
	 */
	public void setLastName( String lastName ) {
		this.lastName = lastName;
	}

	/**
	 * Returns the value for the member attribute <b>lastName</b>
	 * @return String - value of member attribute <b>lastName</b>.
	 */
	public String getLastName( ) {
		return this.lastName;
	}

	/**
	 * This method sets the value to the member attribute <b>publicScreenName</b>.
	 * You cannot set null to the attribute.
	 * @param publicScreenName Value to set member attribute <b>publicScreenName</b>
	 */
	public void setPublicScreenName( String publicScreenName ) {
		this.publicScreenName = publicScreenName;
	}

	/**
	 * Returns the value for the member attribute <b>publicScreenName</b>
	 * @return String - value of member attribute <b>publicScreenName</b>.
	 */
	public String getPublicScreenName( ) {
		return this.publicScreenName;
	}

	/**
	 * This method sets the value to the member attribute <b>userSource</b>.
	 * You cannot set null to the attribute.
	 * @param userSource Value to set member attribute <b>userSource</b>
	 */
	public void setUserSource( int userSource ) {
		this.userSource = userSource;
	}

	/**
	 * Returns the value for the member attribute <b>userSource</b>
	 * @return int - value of member attribute <b>userSource</b>.
	 */
	public int getUserSource( ) {
		return this.userSource;
	}

	/**
	 * This method sets the value to the member attribute <b>notes</b>.
	 * You cannot set null to the attribute.
	 * @param notes Value to set member attribute <b>notes</b>
	 */
	public void setNotes( String notes ) {
		this.notes = notes;
	}

	/**
	 * Returns the value for the member attribute <b>notes</b>
	 * @return String - value of member attribute <b>notes</b>.
	 */
	public String getNotes( ) {
		return this.notes;
	}

	/**
	 * This method sets the value to the member attribute <b>userRoleList</b>.
	 * You cannot set null to the attribute.
	 * @param userRoleList Value to set member attribute <b>userRoleList</b>
	 */
	public void setUserRoleList( Collection<String> userRoleList ) {
		this.userRoleList = userRoleList;
	}

	/**
	 * Returns the value for the member attribute <b>userRoleList</b>
	 * @return Collection<String> - value of member attribute <b>userRoleList</b>.
	 */
	public Collection<String> getUserRoleList( ) {
		return this.userRoleList;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_USER_PROFILE;
	}

	public Collection<Long> getGroupIdList() {
		return groupIdList;
	}

	public void setGroupIdList(Collection<Long> groupIdList) {
		this.groupIdList = groupIdList;
	}


	public List<VXUserPermission> getUserPermList() {
		return userPermList;
	}

	public void setUserPermList(List<VXUserPermission> userPermList) {
		this.userPermList = userPermList;
	}

	public List<VXGroupPermission> getGroupPermissions() {
		return groupPermissions;
	}

	public void setGroupPermissions(List<VXGroupPermission> groupPermissions) {
		this.groupPermissions = groupPermissions;
	}


	/**
	 * @return {@link String} - additional attributes.
	 */
	public String getOtherAttributes() {
		return otherAttributes;
	}

	/**
	 * This method sets additional attributes.
	 * @param otherAttributes
	 */
	public void setOtherAttributes(final String otherAttributes) {
		this.otherAttributes = otherAttributes;
	}

	public Map<String, String> getConfigProperties() {
		return configProperties;
	}

	public void setConfigProperties(Map<String, String> configProperties) {
		this.configProperties = configProperties;
	}

	/**
	 * @return {@link String} - sync Source attribute.
	 */
	public String getSyncSource() {
		return syncSource;
	}

	/**
	 * This method sets sync Source attribute.
	 * @param syncSource
	 */
	public void setSyncSource(final String syncSource) {
		this.syncSource = syncSource;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXPortalUser={";
		str += super.toString();
		str += "loginId={" + loginId + "} ";
		str += "status={" + status + "} ";
		str += "emailAddress={" + emailAddress + "} ";
		str += "firstName={" + firstName + "} ";
		str += "lastName={" + lastName + "} ";
		str += "publicScreenName={" + publicScreenName + "} ";
		str += "userSource={" + userSource + "} ";
		str += "notes={" + notes + "} ";
		str += "userRoleList={" + userRoleList + "} ";
		str += "otherAttributes={" + otherAttributes + "} ";
		str += "syncSource={" + syncSource + "} ";
		str += "}";
		return str;
	}
}
