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
 * Authentication sessions
 *
 */

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.json.JsonDateSerializer;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXAuthSession extends VXDataObject implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Login ID of the user
	 */
	protected String loginId;
	/**
	 * Id of the user
	 */
	protected Long userId;
	/**
	 * Email address of the user
	 */
	protected String emailAddress;
	/**
	 * Is this user a test user?
	 */
	protected boolean isTestUser = false;
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
	 * Family name of the user
	 */
	protected String familyScreenName;
	/**
	 * Date and time of authentication
	 */
	@JsonSerialize(using=JsonDateSerializer.class)
	protected Date authTime;
	/**
	 * Authentication status
	 * This attribute is of type enum XXAuthSession::AuthStatus
	 */
	protected int authStatus;
	/**
	 * Authentication type
	 * This attribute is of type enum XXAuthSession::AuthType
	 */
	protected int authType;
	/**
	 * Authentication provider
	 * This attribute is of type enum XXAuthSession::AuthType
	 */
	protected int authProvider;
	/**
	 * Type of the device
	 * This attribute is of type enum CommonEnums::DeviceType
	 */
	protected int deviceType;
	/**
	 * IP where the request came from
	 */
	protected String requestIP;
	/**
	 * City name
	 */
	protected String cityName;
	/**
	 * State name
	 */
	protected String stateName;
	/**
	 * Country name
	 */
	protected String countryName;
	/**
	 * UserAgent of the requesting device
	 */
	protected String requestUserAgent;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXAuthSession ( ) {
		authStatus = 0;
		authType = 0;
		authProvider = 0;
		deviceType = 0;
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
	 * This method sets the value to the member attribute <b>isTestUser</b>.
	 * You cannot set null to the attribute.
	 * @param isTestUser Value to set member attribute <b>isTestUser</b>
	 */
	public void setIsTestUser( boolean isTestUser ) {
		this.isTestUser = isTestUser;
	}

	/**
	 * Returns the value for the member attribute <b>isTestUser</b>
	 * @return boolean - value of member attribute <b>isTestUser</b>.
	 */
	public boolean isIsTestUser( ) {
		return this.isTestUser;
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
	 * This method sets the value to the member attribute <b>familyScreenName</b>.
	 * You cannot set null to the attribute.
	 * @param familyScreenName Value to set member attribute <b>familyScreenName</b>
	 */
	public void setFamilyScreenName( String familyScreenName ) {
		this.familyScreenName = familyScreenName;
	}

	/**
	 * Returns the value for the member attribute <b>familyScreenName</b>
	 * @return String - value of member attribute <b>familyScreenName</b>.
	 */
	public String getFamilyScreenName( ) {
		return this.familyScreenName;
	}

	/**
	 * This method sets the value to the member attribute <b>authTime</b>.
	 * You cannot set null to the attribute.
	 * @param authTime Value to set member attribute <b>authTime</b>
	 */
	public void setAuthTime( Date authTime ) {
		this.authTime = authTime;
	}

	/**
	 * Returns the value for the member attribute <b>authTime</b>
	 * @return Date - value of member attribute <b>authTime</b>.
	 */
	public Date getAuthTime( ) {
		return this.authTime;
	}

	/**
	 * This method sets the value to the member attribute <b>authStatus</b>.
	 * You cannot set null to the attribute.
	 * @param authStatus Value to set member attribute <b>authStatus</b>
	 */
	public void setAuthStatus( int authStatus ) {
		this.authStatus = authStatus;
	}

	/**
	 * Returns the value for the member attribute <b>authStatus</b>
	 * @return int - value of member attribute <b>authStatus</b>.
	 */
	public int getAuthStatus( ) {
		return this.authStatus;
	}

	/**
	 * This method sets the value to the member attribute <b>authType</b>.
	 * You cannot set null to the attribute.
	 * @param authType Value to set member attribute <b>authType</b>
	 */
	public void setAuthType( int authType ) {
		this.authType = authType;
	}

	/**
	 * Returns the value for the member attribute <b>authType</b>
	 * @return int - value of member attribute <b>authType</b>.
	 */
	public int getAuthType( ) {
		return this.authType;
	}

	/**
	 * This method sets the value to the member attribute <b>authProvider</b>.
	 * You cannot set null to the attribute.
	 * @param authProvider Value to set member attribute <b>authProvider</b>
	 */
	public void setAuthProvider( int authProvider ) {
		this.authProvider = authProvider;
	}

	/**
	 * Returns the value for the member attribute <b>authProvider</b>
	 * @return int - value of member attribute <b>authProvider</b>.
	 */
	public int getAuthProvider( ) {
		return this.authProvider;
	}

	/**
	 * This method sets the value to the member attribute <b>deviceType</b>.
	 * You cannot set null to the attribute.
	 * @param deviceType Value to set member attribute <b>deviceType</b>
	 */
	public void setDeviceType( int deviceType ) {
		this.deviceType = deviceType;
	}

	/**
	 * Returns the value for the member attribute <b>deviceType</b>
	 * @return int - value of member attribute <b>deviceType</b>.
	 */
	public int getDeviceType( ) {
		return this.deviceType;
	}

	/**
	 * This method sets the value to the member attribute <b>requestIP</b>.
	 * You cannot set null to the attribute.
	 * @param requestIP Value to set member attribute <b>requestIP</b>
	 */
	public void setRequestIP( String requestIP ) {
		this.requestIP = requestIP;
	}

	/**
	 * Returns the value for the member attribute <b>requestIP</b>
	 * @return String - value of member attribute <b>requestIP</b>.
	 */
	public String getRequestIP( ) {
		return this.requestIP;
	}

	/**
	 * This method sets the value to the member attribute <b>cityName</b>.
	 * You cannot set null to the attribute.
	 * @param cityName Value to set member attribute <b>cityName</b>
	 */
	public void setCityName( String cityName ) {
		this.cityName = cityName;
	}

	/**
	 * Returns the value for the member attribute <b>cityName</b>
	 * @return String - value of member attribute <b>cityName</b>.
	 */
	public String getCityName( ) {
		return this.cityName;
	}

	/**
	 * This method sets the value to the member attribute <b>stateName</b>.
	 * You cannot set null to the attribute.
	 * @param stateName Value to set member attribute <b>stateName</b>
	 */
	public void setStateName( String stateName ) {
		this.stateName = stateName;
	}

	/**
	 * Returns the value for the member attribute <b>stateName</b>
	 * @return String - value of member attribute <b>stateName</b>.
	 */
	public String getStateName( ) {
		return this.stateName;
	}

	/**
	 * This method sets the value to the member attribute <b>countryName</b>.
	 * You cannot set null to the attribute.
	 * @param countryName Value to set member attribute <b>countryName</b>
	 */
	public void setCountryName( String countryName ) {
		this.countryName = countryName;
	}

	/**
	 * Returns the value for the member attribute <b>countryName</b>
	 * @return String - value of member attribute <b>countryName</b>.
	 */
	public String getCountryName( ) {
		return this.countryName;
	}

	/**
	 * This method sets the value to the member attribute <b>requestUserAgent</b>.
	 * You cannot set null to the attribute.
	 * @param requestUserAgent Value to set member attribute <b>requestUserAgent</b>
	 */
	public void setRequestUserAgent( String requestUserAgent ) {
		this.requestUserAgent = requestUserAgent;
	}

	/**
	 * Returns the value for the member attribute <b>requestUserAgent</b>
	 * @return String - value of member attribute <b>requestUserAgent</b>.
	 */
	public String getRequestUserAgent( ) {
		return this.requestUserAgent;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_AUTH_SESS;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXAuthSession={";
		str += super.toString();
		str += "loginId={" + loginId + "} ";
		str += "userId={" + userId + "} ";
		str += "emailAddress={" + emailAddress + "} ";
		str += "isTestUser={" + isTestUser + "} ";
		str += "firstName={" + firstName + "} ";
		str += "lastName={" + lastName + "} ";
		str += "publicScreenName={" + publicScreenName + "} ";
		str += "familyScreenName={" + familyScreenName + "} ";
		str += "authTime={" + authTime + "} ";
		str += "authStatus={" + authStatus + "} ";
		str += "authType={" + authType + "} ";
		str += "authProvider={" + authProvider + "} ";
		str += "deviceType={" + deviceType + "} ";
		str += "requestIP={" + requestIP + "} ";
		str += "cityName={" + cityName + "} ";
		str += "stateName={" + stateName + "} ";
		str += "countryName={" + countryName + "} ";
		str += "requestUserAgent={" + requestUserAgent + "} ";
		str += "}";
		return str;
	}
}
