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
 * User details
 *
 */

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.RangerConstants;

import java.util.Date;


@Entity
@Table(name="x_portal_user")
@XmlRootElement
public class XXPortalUser extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="X_PORTAL_USER_SEQ",sequenceName="X_PORTAL_USER_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_PORTAL_USER_SEQ")
	@Column(name="ID")
	protected Long id;
		@Override
	public void setId(Long id) {
		// TODO Auto-generated method stub
		this.id=id;
	}
	@Override
	public Long getId() {
		// TODO Auto-generated method stub
		return id;
	}
	/**
	 * First name of the user
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1022</b>.
	 * </ul>
	 *
	 */
	@Column(name="FIRST_NAME"   , length=1022)
	protected String firstName;

	/**
	 * Last name of the user
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1022</b>.
	 * </ul>
	 *
	 */
	@Column(name="LAST_NAME"   , length=1022)
	protected String lastName;

	/**
	 * Public screen name for the user
	 * <ul>
	 * <li>The maximum length for this attribute is <b>2048</b>.
	 * </ul>
	 *
	 */
	@Column(name="PUB_SCR_NAME"   , length=2048)
	protected String publicScreenName;

	/**
	 * Login ID of the user
	 * <ul>
	 * <li>The maximum length for this attribute is <b>767</b>.
	 * </ul>
	 *
	 */
	@Column(name="LOGIN_ID" , unique=true  , length=767)
	protected String loginId;

	/**
	 * <ul>
	 * <li>The maximum length for this attribute is <b>512</b>.
	 * </ul>
	 *
	 */
	@Column(name="PASSWORD"  , nullable=false , length=512)
	protected String password;

	/**
	 * Email address of the user
	 * <ul>
	 * <li>The maximum length for this attribute is <b>512</b>.
	 * </ul>
	 *
	 */
	@Column(name="EMAIL" , unique=true  , length=512)
	protected String emailAddress;

	/**
	 * Status of the user
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::ActivationStatus
	 * </ul>
	 *
	 */
	@Column(name="STATUS"  , nullable=false )
	protected int status = RangerConstants.ACT_STATUS_DISABLED;

	/**
	 * Source of the user
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::UserSource
	 * </ul>
	 *
	 */
	@Column(name="USER_SRC"  , nullable=false )
	protected int userSource = RangerConstants.USER_APP;

	/**
	 * Note
	 * <ul>
	 * <li>The maximum length for this attribute is <b>4000</b>.
	 * </ul>
	 *
	 */
	@Column(name="NOTES"   , length=4000)
	protected String notes;

	/**
	 * Additional store attributes.
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="OTHER_ATTRIBUTES")
	protected String otherAttributes;

	/**
	 * Sync Source Attribute.
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="SYNC_SOURCE")
	protected String syncSource;

	@Column(name="OLD_PASSWORDS")
	protected String oldPasswords;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="PASSWORD_UPDATED_TIME")
	protected Date passwordUpdatedTime = DateUtil.getUTCDate();

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXPortalUser ( ) {
		status = RangerConstants.ACT_STATUS_DISABLED;
		userSource = RangerConstants.USER_APP;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_USER_PROFILE;
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
	 * This method sets JSON {@link String} representation of additional store attributes.
	 * This method accepts null values.
	 * @param otherAttributes
	 */
	public void setOtherAttributes(String otherAttributes) {
		this.otherAttributes = otherAttributes;
	}

	/**
	 * @return JSON {@link String} representation of additional store attributes if available,
	 * <code>null</code> otherwise.
	 */
	public String getOtherAttributes() {
		return otherAttributes;
	}

	/**
	 * This method sets JSON {@link String} representation of sync source attribute.
	 * This method accepts null values.
	 * @param syncSource
	 */
	public void setSyncSource(String syncSource) {
		this.syncSource = syncSource;
	}

	/**
	 * @return JSON {@link String} representation of sync source attribute if available,
	 * <code>null</code> otherwise.
	 */
	public String getSyncSource() { return syncSource; }

	public String getOldPasswords() {
		return oldPasswords;
	}

	public void setOldPasswords(String oldPasswords) {
		this.oldPasswords = oldPasswords;
	}

	public Date getPasswordUpdatedTime() {
		return passwordUpdatedTime;
	}

	public void setPasswordUpdatedTime(Date passwordUpdatedTime) {
		this.passwordUpdatedTime = passwordUpdatedTime;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXPortalUser={";
		str += super.toString();
		str += "firstName={" + firstName + "} ";
		str += "lastName={" + lastName + "} ";
		str += "publicScreenName={" + publicScreenName + "} ";
		str += "loginId={" + loginId + "} ";
		str += "emailAddress={" + emailAddress + "} ";
		str += "status={" + status + "} ";
		str += "userSource={" + userSource + "} ";
		str += "notes={" + notes + "} ";
		str += "otherAttributes={" + otherAttributes + "} ";
		str += "syncSource={" + syncSource + "} ";
		str += "passwordUpdatedTime={" + passwordUpdatedTime + "} ";
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
		XXPortalUser other = (XXPortalUser) obj;
        	if ((this.firstName == null && other.firstName != null) || (this.firstName != null && !this.firstName.equals(other.firstName))) {
            		return false;
        	}
        	if ((this.lastName == null && other.lastName != null) || (this.lastName != null && !this.lastName.equals(other.lastName))) {
            		return false;
        	}
        	if ((this.publicScreenName == null && other.publicScreenName != null) || (this.publicScreenName != null && !this.publicScreenName.equals(other.publicScreenName))) {
            		return false;
        	}
        	if ((this.loginId == null && other.loginId != null) || (this.loginId != null && !this.loginId.equals(other.loginId))) {
            		return false;
        	}
        	if ((this.password == null && other.password != null) || (this.password != null && !this.password.equals(other.password))) {
            		return false;
        	}
        	if ((this.emailAddress == null && other.emailAddress != null) || (this.emailAddress != null && !this.emailAddress.equals(other.emailAddress))) {
            		return false;
        	}
		if( this.status != other.status ) return false;
		if( this.userSource != other.userSource ) return false;
        	if ((this.notes == null && other.notes != null) || (this.notes != null && !this.notes.equals(other.notes))) {
            		return false;
        	}
		return true;
	}
	public static String getEnumName(String fieldName ) {
		if( "status".equals(fieldName) ) {
			return "CommonEnums.ActivationStatus";
		}
		if( "userSource".equals(fieldName) ) {
			return "CommonEnums.UserSource";
		}
		//Later TODO
		//return super.getEnumName(fieldName);
		return null;
	}

}
