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

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@XmlRootElement
public class VXAuditRecord {

	/**
	 * Date of audit log
	 */
	protected Date date;
	
	/**
	 * Name of the resource
	 */
	protected String resource;
	
	/**
	 * Action which was audited
	 */
	protected String action;
	
	/**
	 * Result of the policy enforced
	 */
	protected String result;
	
	/**
	 * User name whose action was audited
	 */
	protected String user;
	
	/**
	 * Name of the policy enforcer
	 */
	protected String enforcer;
	
	/**
	 * Type of resource for which the audit was done
	 */
	protected int resourceType = AppConstants.RESOURCE_UNKNOWN;
	
	/**
	 * Type of asset for which the audit was done
	 * This attribute is of type enum AppCommonEnums::AssetType
	 */
	protected int assetType = AppConstants.ASSET_UNKNOWN;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXAuditRecord() {
		resourceType = AppConstants.RESOURCE_UNKNOWN;
		assetType = AppConstants.ASSET_UNKNOWN;
	}

	/**
	 * Returns the value for the member attribute <b>date</b>
	 * @return Date - value of member attribute <b>date</b>.
	 */
	public Date getDate() {
		return date;
	}

	/**
	 * This method sets the value to the member attribute <b>date</b>.
	 * You cannot set null to the attribute.
	 * @param date Value to set member attribute <b>date</b>
	 */
	public void setDate(Date date) {
		this.date = date;
	}

	/**
	 * Returns the value for the member attribute <b>resource</b>
	 * @return String - value of member attribute <b>resource</b>.
	 */
	public String getResource() {
		return resource;
	}

	/**
	 * This method sets the value to the member attribute <b>resource</b>.
	 * You cannot set null to the attribute.
	 * @param resource Value to set member attribute <b>resource</b>
	 */
	public void setResource(String resource) {
		this.resource = resource;
	}

	/**
	 * Returns the value for the member attribute <b>action</b>
	 * @return String - value of member attribute <b>action</b>.
	 */
	public String getAction() {
		return action;
	}

	/**
	 * This method sets the value to the member attribute <b>action</b>.
	 * You cannot set null to the attribute.
	 * @param action Value to set member attribute <b>action</b>
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * Returns the value for the member attribute <b>result</b>
	 * @return String - value of member attribute <b>result</b>.
	 */
	public String getResult() {
		return result;
	}

	/**
	 * This method sets the value to the member attribute <b>result</b>.
	 * You cannot set null to the attribute.
	 * @param result Value to set member attribute <b>result</b>
	 */
	public void setResult(String result) {
		this.result = result;
	}

	/**
	 * Returns the value for the member attribute <b>user</b>
	 * @return String - value of member attribute <b>user</b>.
	 */
	public String getUser() {
		return user;
	}

	/**
	 * This method sets the value to the member attribute <b>user</b>.
	 * You cannot set null to the attribute.
	 * @param user Value to set member attribute <b>user</b>
	 */
	public void setUser(String user) {
		this.user = user;
	}

	public String getEnforcer() {
		return enforcer;
	}

	/**
	 * This method sets the value to the member attribute <b>enforcer</b>.
	 * You cannot set null to the attribute.
	 * @param enforcer Value to set member attribute <b>enforcer</b>
	 */
	public void setEnforcer(String enforcer) {
		this.enforcer = enforcer;
	}
	
	/**
	 * Returns the value for the member attribute <b>resourceType</b>
	 * @return int - value of member attribute <b>resourceType</b>.
	 */
	public int getResourceType( ) {
		return this.resourceType;
	}

	/**
	 * This method sets the value to the member attribute <b>resourceType</b>.
	 * You cannot set null to the attribute.
	 * @param resourceType Value to set member attribute <b>resourceType</b>
	 */
	public void setResourceType( int resourceType ) {
		this.resourceType = resourceType;
	}

	/**
	 * Returns the value for the member attribute <b>assetType</b>
	 * @return int - value of member attribute <b>assetType</b>.
	 */
	public int getAssetType() {
		return assetType;
	}

	/**
	 * This method sets the value to the member attribute <b>assetType</b>.
	 * You cannot set null to the attribute.
	 * @param assetType Value to set member attribute <b>assetType</b>
	 */
	public void setAssetType(int assetType) {
		this.assetType = assetType;
	}

	/**
	 * This return the bean content in string format
	 *
	 * @return formatedStr
	 */

	public String toString() {
		String str = "XVAuditRecord={";
		str += super.toString();
		str += "date={" + date + "} ";
		str += "resource={" + resource + "} ";
		str += "action={" + action + "} ";
		str += "result={" + result + "} ";
		str += "user={" + user + "} ";
		str += "enforcer={" + enforcer + "} ";
		str += "resourceType={" + resourceType + "} ";
		str += "assetType={" + assetType + "} ";
		str += "}";
		return str;
	}

}
