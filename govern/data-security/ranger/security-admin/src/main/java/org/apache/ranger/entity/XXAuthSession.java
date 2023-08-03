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
 * Authentication session
 *
 */

import java.util.Date;

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


@Entity
@Table(name="x_auth_sess")
@XmlRootElement
public class XXAuthSession extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="X_AUTH_SESS_SEQ",sequenceName="X_AUTH_SESS_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="X_AUTH_SESS_SEQ")
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
	 * Enum values for AuthStatus
	 */
	/**
	 * AUTH_STATUS_UNKNOWN is an element of enum AuthStatus. Its value is "AUTH_STATUS_UNKNOWN".
	 */
	public static final int AUTH_STATUS_UNKNOWN = 0;
	/**
	 * AUTH_STATUS_SUCCESS is an element of enum AuthStatus. Its value is "AUTH_STATUS_SUCCESS".
	 */
	public static final int AUTH_STATUS_SUCCESS = 1;
	/**
	 * AUTH_STATUS_WRONG_PASSWORD is an element of enum AuthStatus. Its value is "AUTH_STATUS_WRONG_PASSWORD".
	 */
	public static final int AUTH_STATUS_WRONG_PASSWORD = 2;
	/**
	 * AUTH_STATUS_DISABLED is an element of enum AuthStatus. Its value is "AUTH_STATUS_DISABLED".
	 */
	public static final int AUTH_STATUS_DISABLED = 3;
	/**
	 * AUTH_STATUS_LOCKED is an element of enum AuthStatus. Its value is "AUTH_STATUS_LOCKED".
	 */
	public static final int AUTH_STATUS_LOCKED = 4;
	/**
	 * AUTH_STATUS_PASSWORD_EXPIRED is an element of enum AuthStatus. Its value is "AUTH_STATUS_PASSWORD_EXPIRED".
	 */
	public static final int AUTH_STATUS_PASSWORD_EXPIRED = 5;
	/**
	 * AUTH_STATUS_USER_NOT_FOUND is an element of enum AuthStatus. Its value is "AUTH_STATUS_USER_NOT_FOUND".
	 */
	public static final int AUTH_STATUS_USER_NOT_FOUND = 6;

	/**
	 * Max value for enum AuthStatus_MAX
	 */
	public static final int AuthStatus_MAX = 6;

	/**
	 * Enum values for AuthType
	 */
	/**
	 * AUTH_TYPE_UNKNOWN is an element of enum AuthType. Its value is "AUTH_TYPE_UNKNOWN".
	 */
	public static final int AUTH_TYPE_UNKNOWN = 0;
	/**
	 * AUTH_TYPE_PASSWORD is an element of enum AuthType. Its value is "AUTH_TYPE_PASSWORD".
	 */
	public static final int AUTH_TYPE_PASSWORD = 1;

	/**
	 * AUTH_TYPE_KERBEROS is an element of enum AuthType. Its value is "AUTH_TYPE_KERBEROS".
	 */
	public static final int AUTH_TYPE_KERBEROS = 2;

	/**
	 * AUTH_TYPE_SSO is an element of enum AuthType. Its value is "AUTH_TYPE_SSO".
	 */
	public static final int AUTH_TYPE_SSO = 3;

	/**
	 * AUTH_TYPE_TRUSTED_PROXY is an element of enum AuthType. Its value is "AUTH_TYPE_TRUSTED_PROXY".
	 */
	public static final int AUTH_TYPE_TRUSTED_PROXY = 4;

	/**
	 * Max value for enum AuthType_MAX
	 */
	public static final int AuthType_MAX = 4;



	/**
	 * Login ID of the user
	 * <ul>
	 * <li>The maximum length for this attribute is <b>767</b>.
	 * </ul>
	 *
	 */
	@Column(name="LOGIN_ID"  , nullable=false , length=767)
	protected String loginId;

	/**
	 * Id of the user
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="USER_ID"   )
	protected Long userId;


	/**
	 * External session id. Currently spring/http session
	 * <ul>
	 * <li>The maximum length for this attribute is <b>512</b>.
	 * </ul>
	 *
	 */
	@Column(name="EXT_SESS_ID"   , length=512)
	protected String extSessionId;

	/**
	 * Date and time of authentication
	 * <ul>
	 * </ul>
	 *
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="AUTH_TIME"  , nullable=false )
	protected Date authTime = DateUtil.getUTCDate();

	/**
	 * Authentication status
	 * <ul>
	 * <li>This attribute is of type enum XXAuthSession::AuthStatus
	 * </ul>
	 *
	 */
	@Column(name="AUTH_STATUS"  , nullable=false )
	protected int authStatus = AUTH_STATUS_UNKNOWN;

	/**
	 * Authentication type
	 * <ul>
	 * <li>This attribute is of type enum XXAuthSession::AuthType
	 * </ul>
	 *
	 */
	@Column(name="AUTH_TYPE"  , nullable=false )
	protected int authType = AUTH_TYPE_UNKNOWN;

	/**
	 * Authentication provider
	 * <ul>
	 * <li>This attribute is of type enum XXAuthSession::AuthType
	 * </ul>
	 *
	 */
	@Column(name="AUTH_PROVIDER"  , nullable=false )
	protected int authProvider = AUTH_TYPE_UNKNOWN;

	/**
	 * Type of the device
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::DeviceType
	 * </ul>
	 *
	 */
	@Column(name="DEVICE_TYPE"  , nullable=false )
	protected int deviceType = RangerConstants.DEVICE_UNKNOWN;

	/**
	 * IP where the request came from
	 * <ul>
	 * <li>The maximum length for this attribute is <b>48</b>.
	 * </ul>
	 *
	 */
	@Column(name="REQ_IP"  , nullable=false , length=48)
	protected String requestIP;

	/**
	 * UserAgent of the requesting device
	 * <ul>
	 * <li>The maximum length for this attribute is <b>1024</b>.
	 * </ul>
	 *
	 */
	@Column(name="REQ_UA"   , length=1024)
	protected String requestUserAgent;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXAuthSession ( ) {
		authStatus = AUTH_STATUS_UNKNOWN;
		authType = AUTH_TYPE_UNKNOWN;
		authProvider = AUTH_TYPE_UNKNOWN;
		deviceType = RangerConstants.DEVICE_UNKNOWN;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_AUTH_SESS;
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
	 * This method sets the value to the member attribute <b>extSessionId</b>.
	 * You cannot set null to the attribute.
	 * @param extSessionId Value to set member attribute <b>extSessionId</b>
	 */
	public void setExtSessionId( String extSessionId ) {
		this.extSessionId = extSessionId;
	}

	/**
	 * Returns the value for the member attribute <b>extSessionId</b>
	 * @return String - value of member attribute <b>extSessionId</b>.
	 */
	public String getExtSessionId( ) {
		return this.extSessionId;
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

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXAuthSession={";
		str += super.toString();
		str += "loginId={" + loginId + "} ";
		str += "userId={" + userId + "} ";
		str += "authTime={" + authTime + "} ";
		str += "authStatus={" + authStatus + "} ";
		str += "authType={" + authType + "} ";
		str += "authProvider={" + authProvider + "} ";
		str += "deviceType={" + deviceType + "} ";
		str += "requestIP={" + requestIP + "} ";
		str += "requestUserAgent={" + requestUserAgent + "} ";
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
		XXAuthSession other = (XXAuthSession) obj;
        	if ((this.loginId == null && other.loginId != null) || (this.loginId != null && !this.loginId.equals(other.loginId))) {
            		return false;
        	}
        	if ((this.userId == null && other.userId != null) || (this.userId != null && !this.userId.equals(other.userId))) {
            		return false;
        	}
        	if ((this.extSessionId == null && other.extSessionId != null) || (this.extSessionId != null && !this.extSessionId.equals(other.extSessionId))) {
            		return false;
        	}
        	if ((this.authTime == null && other.authTime != null) || (this.authTime != null && !this.authTime.equals(other.authTime))) {
            		return false;
        	}
		if( this.authStatus != other.authStatus ) return false;
		if( this.authType != other.authType ) return false;
		if( this.authProvider != other.authProvider ) return false;
		if( this.deviceType != other.deviceType ) return false;
        	if ((this.requestIP == null && other.requestIP != null) || (this.requestIP != null && !this.requestIP.equals(other.requestIP))) {
            		return false;
        	}
        	if ((this.requestUserAgent == null && other.requestUserAgent != null) || (this.requestUserAgent != null && !this.requestUserAgent.equals(other.requestUserAgent))) {
            		return false;
        	}
		return true;
	}
	public static String getEnumName(String fieldName ) {
		if( "authStatus".equals(fieldName) ) {
			return "CommonEnums.AuthStatus";
		}
		if( "authType".equals(fieldName) ) {
			return "CommonEnums.AuthType";
		}
		if( "authProvider".equals(fieldName) ) {
			return "CommonEnums.AuthType";
		}
		if( "deviceType".equals(fieldName) ) {
			return "CommonEnums.DeviceType";
		}
		//Later TODO
		//return super.getEnumName(fieldName);
		return null;
	}

}
