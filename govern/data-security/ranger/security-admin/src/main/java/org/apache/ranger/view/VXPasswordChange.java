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
 * Change password structure
 *
 */

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.view.ViewBaseBean;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXPasswordChange extends ViewBaseBean implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Id of the user
	 */
	protected Long id;
	/**
	 * Login ID of the user
	 */
	protected String loginId;
	/**
	 * Email address of the user
	 */
	protected String emailAddress;
	/**
	 * Reset Code
	 */
	protected String resetCode;
	/**
	 * Old Password
	 */
	protected String oldPassword;
	/**
	 * Updated Password
	 */
	protected String updPassword;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXPasswordChange ( ) {
	}

	/**
	 * This method sets the value to the member attribute <b>id</b>.
	 * You cannot set null to the attribute.
	 * @param id Value to set member attribute <b>id</b>
	 */
	public void setId( Long id ) {
		this.id = id;
	}

	/**
	 * Returns the value for the member attribute <b>id</b>
	 * @return Long - value of member attribute <b>id</b>.
	 */
	public Long getId( ) {
		return this.id;
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
	 * This method sets the value to the member attribute <b>resetCode</b>.
	 * You cannot set null to the attribute.
	 * @param resetCode Value to set member attribute <b>resetCode</b>
	 */
	public void setResetCode( String resetCode ) {
		this.resetCode = resetCode;
	}

	/**
	 * Returns the value for the member attribute <b>resetCode</b>
	 * @return String - value of member attribute <b>resetCode</b>.
	 */
	public String getResetCode( ) {
		return this.resetCode;
	}

	/**
	 * This method sets the value to the member attribute <b>oldPassword</b>.
	 * You cannot set null to the attribute.
	 * @param oldPassword Value to set member attribute <b>oldPassword</b>
	 */
	public void setOldPassword( String oldPassword ) {
		this.oldPassword = oldPassword;
	}

	/**
	 * Returns the value for the member attribute <b>oldPassword</b>
	 * @return String - value of member attribute <b>oldPassword</b>.
	 */
	public String getOldPassword( ) {
		return this.oldPassword;
	}

	/**
	 * This method sets the value to the member attribute <b>updPassword</b>.
	 * You cannot set null to the attribute.
	 * @param updPassword Value to set member attribute <b>updPassword</b>
	 */
	public void setUpdPassword( String updPassword ) {
		this.updPassword = updPassword;
	}

	/**
	 * Returns the value for the member attribute <b>updPassword</b>
	 * @return String - value of member attribute <b>updPassword</b>.
	 */
	public String getUpdPassword( ) {
		return this.updPassword;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_PASSWORD_CHANGE;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXPasswordChange={";
		str += super.toString();
		str += "id={" + id + "} ";
		str += "loginId={" + loginId + "} ";
		str += "emailAddress={" + emailAddress + "} ";
		str += "resetCode={" + resetCode + "} ";
		str += "oldPassword={***length=" + (oldPassword == null? 0 : oldPassword.length()) + "***} ";
		str += "updPassword={***length=" + (updPassword == null? 0 : updPassword.length()) + "***} ";
		str += "}";
		return str;
	}
}
