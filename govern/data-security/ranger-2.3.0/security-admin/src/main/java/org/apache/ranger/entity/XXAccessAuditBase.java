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
 * Access Audit
 *
 */

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.SequenceGenerator;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.RangerConstants;

@MappedSuperclass
@XmlRootElement
public class XXAccessAuditBase extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name="XA_ACCESS_AUDIT_SEQ",sequenceName="XA_ACCESS_AUDIT_SEQ",allocationSize=1)
	@GeneratedValue(strategy=GenerationType.AUTO,generator="XA_ACCESS_AUDIT_SEQ")
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
	 * Repository Type
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::AssetType
	 * </ul>
	 *
	 */
	@Column(name="AUDIT_TYPE"  , nullable=false )
	protected int auditType = AppConstants.ASSET_UNKNOWN;

	/**
	 * Access Result
	 * <ul>
	 * <li>This attribute is of type enum CommonEnums::AccessResult
	 * </ul>
	 *
	 */
	@Column(name="ACCESS_RESULT"   )
	protected int accessResult = RangerConstants.ACCESS_RESULT_DENIED;

	/**
	 * Access Type
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="ACCESS_TYPE"   , length=255)
	protected String accessType;

	/**
	 * Acl Enforcer
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="ACL_ENFORCER"   , length=255)
	protected String aclEnforcer;

	/**
	 * Agent Id
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="AGENT_ID"   , length=255)
	protected String agentId;

	/**
	 * Client Ip
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="CLIENT_IP"   , length=255)
	protected String clientIP;

	/**
	 * Client Type
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="CLIENT_TYPE"   , length=255)
	protected String clientType;

	/**
	 * Policy Id
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="POLICY_ID"   )
	protected long policyId;

	/**
	 * Repository Name
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="REPO_NAME"   , length=255)
	protected String repoName;

	/**
	 * Repository Type
	 * <ul>
	 * </ul>
	 *
	 */
	@Column(name="REPO_TYPE"   )
	protected int repoType;

	/**
	 * Reason of result
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="RESULT_REASON"   , length=255)
	protected String resultReason;

	/**
	 * Session Id
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="SESSION_ID"   , length=255)
	protected String sessionId;

	/**
	 * Event Time
	 * <ul>
	 * </ul>
	 *
	 */
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="EVENT_TIME"   )
	protected Date eventTime = DateUtil.getUTCDate();

	/**
	 * Requesting User
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="REQUEST_USER"   , length=255)
	protected String requestUser;

	/**
	 * Action
	 * <ul>
	 * <li>The maximum length for this attribute is <b>2000</b>.
	 * </ul>
	 *
	 */
	@Column(name="ACTION"   , length=2000)
	protected String action;

	/**
	 * Requesting Data
	 * <ul>
	 * <li>The maximum length for this attribute is <b>2000</b>.
	 * </ul>
	 *
	 */
	@Column(name="REQUEST_DATA"   , length=2000)
	protected String requestData;

	/**
	 * Resource Path
	 * <ul>
	 * <li>The maximum length for this attribute is <b>2000</b>.
	 * </ul>
	 *
	 */
	@Column(name="RESOURCE_PATH"   , length=2000)
	protected String resourcePath;

	/**
	 * Resource Type
	 * <ul>
	 * <li>The maximum length for this attribute is <b>255</b>.
	 * </ul>
	 *
	 */
	@Column(name="RESOURCE_TYPE"   , length=255)
	protected String resourceType;

	
	
	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXAccessAuditBase ( ) {
		auditType = AppConstants.ASSET_UNKNOWN;
		accessResult = RangerConstants.ACCESS_RESULT_DENIED;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_ACCESS_AUDIT;
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
	 * This method sets the value to the member attribute <b>accessResult</b>.
	 * You cannot set null to the attribute.
	 * @param accessResult Value to set member attribute <b>accessResult</b>
	 */
	public void setAccessResult( int accessResult ) {
		this.accessResult = accessResult;
	}

	/**
	 * Returns the value for the member attribute <b>accessResult</b>
	 * @return int - value of member attribute <b>accessResult</b>.
	 */
	public int getAccessResult( ) {
		return this.accessResult;
	}

	/**
	 * This method sets the value to the member attribute <b>accessType</b>.
	 * You cannot set null to the attribute.
	 * @param accessType Value to set member attribute <b>accessType</b>
	 */
	public void setAccessType( String accessType ) {
		this.accessType = accessType;
	}

	/**
	 * Returns the value for the member attribute <b>accessType</b>
	 * @return String - value of member attribute <b>accessType</b>.
	 */
	public String getAccessType( ) {
		return this.accessType;
	}

	/**
	 * This method sets the value to the member attribute <b>aclEnforcer</b>.
	 * You cannot set null to the attribute.
	 * @param aclEnforcer Value to set member attribute <b>aclEnforcer</b>
	 */
	public void setAclEnforcer( String aclEnforcer ) {
		this.aclEnforcer = aclEnforcer;
	}

	/**
	 * Returns the value for the member attribute <b>aclEnforcer</b>
	 * @return String - value of member attribute <b>aclEnforcer</b>.
	 */
	public String getAclEnforcer( ) {
		return this.aclEnforcer;
	}

	/**
	 * This method sets the value to the member attribute <b>agentId</b>.
	 * You cannot set null to the attribute.
	 * @param agentId Value to set member attribute <b>agentId</b>
	 */
	public void setAgentId( String agentId ) {
		this.agentId = agentId;
	}

	/**
	 * Returns the value for the member attribute <b>agentId</b>
	 * @return String - value of member attribute <b>agentId</b>.
	 */
	public String getAgentId( ) {
		return this.agentId;
	}

	/**
	 * This method sets the value to the member attribute <b>clientIP</b>.
	 * You cannot set null to the attribute.
	 * @param clientIP Value to set member attribute <b>clientIP</b>
	 */
	public void setClientIP( String clientIP ) {
		this.clientIP = clientIP;
	}

	/**
	 * Returns the value for the member attribute <b>clientIP</b>
	 * @return String - value of member attribute <b>clientIP</b>.
	 */
	public String getClientIP( ) {
		return this.clientIP;
	}

	/**
	 * This method sets the value to the member attribute <b>clientType</b>.
	 * You cannot set null to the attribute.
	 * @param clientType Value to set member attribute <b>clientType</b>
	 */
	public void setClientType( String clientType ) {
		this.clientType = clientType;
	}

	/**
	 * Returns the value for the member attribute <b>clientType</b>
	 * @return String - value of member attribute <b>clientType</b>.
	 */
	public String getClientType( ) {
		return this.clientType;
	}

	/**
	 * This method sets the value to the member attribute <b>policyId</b>.
	 * You cannot set null to the attribute.
	 * @param policyId Value to set member attribute <b>policyId</b>
	 */
	public void setPolicyId( long policyId ) {
		this.policyId = policyId;
	}

	/**
	 * Returns the value for the member attribute <b>policyId</b>
	 * @return long - value of member attribute <b>policyId</b>.
	 */
	public long getPolicyId( ) {
		return this.policyId;
	}

	/**
	 * This method sets the value to the member attribute <b>repoName</b>.
	 * You cannot set null to the attribute.
	 * @param repoName Value to set member attribute <b>repoName</b>
	 */
	public void setRepoName( String repoName ) {
		this.repoName = repoName;
	}

	/**
	 * Returns the value for the member attribute <b>repoName</b>
	 * @return String - value of member attribute <b>repoName</b>.
	 */
	public String getRepoName( ) {
		return this.repoName;
	}

	/**
	 * This method sets the value to the member attribute <b>repoType</b>.
	 * You cannot set null to the attribute.
	 * @param repoType Value to set member attribute <b>repoType</b>
	 */
	public void setRepoType( int repoType ) {
		this.repoType = repoType;
	}

	/**
	 * Returns the value for the member attribute <b>repoType</b>
	 * @return int - value of member attribute <b>repoType</b>.
	 */
	public int getRepoType( ) {
		return this.repoType;
	}

	/**
	 * This method sets the value to the member attribute <b>resultReason</b>.
	 * You cannot set null to the attribute.
	 * @param resultReason Value to set member attribute <b>resultReason</b>
	 */
	public void setResultReason( String resultReason ) {
		this.resultReason = resultReason;
	}

	/**
	 * Returns the value for the member attribute <b>resultReason</b>
	 * @return String - value of member attribute <b>resultReason</b>.
	 */
	public String getResultReason( ) {
		return this.resultReason;
	}

	/**
	 * This method sets the value to the member attribute <b>sessionId</b>.
	 * You cannot set null to the attribute.
	 * @param sessionId Value to set member attribute <b>sessionId</b>
	 */
	public void setSessionId( String sessionId ) {
		this.sessionId = sessionId;
	}

	/**
	 * Returns the value for the member attribute <b>sessionId</b>
	 * @return String - value of member attribute <b>sessionId</b>.
	 */
	public String getSessionId( ) {
		return this.sessionId;
	}

	/**
	 * This method sets the value to the member attribute <b>eventTime</b>.
	 * You cannot set null to the attribute.
	 * @param eventTime Value to set member attribute <b>eventTime</b>
	 */
	public void setEventTime( Date eventTime ) {
		this.eventTime = eventTime;
	}

	/**
	 * Returns the value for the member attribute <b>eventTime</b>
	 * @return Date - value of member attribute <b>eventTime</b>.
	 */
	public Date getEventTime( ) {
		return this.eventTime;
	}

	/**
	 * This method sets the value to the member attribute <b>requestUser</b>.
	 * You cannot set null to the attribute.
	 * @param requestUser Value to set member attribute <b>requestUser</b>
	 */
	public void setRequestUser( String requestUser ) {
		this.requestUser = requestUser;
	}

	/**
	 * Returns the value for the member attribute <b>requestUser</b>
	 * @return String - value of member attribute <b>requestUser</b>.
	 */
	public String getRequestUser( ) {
		return this.requestUser;
	}

	/**
	 * This method sets the value to the member attribute <b>action</b>.
	 * You cannot set null to the attribute.
	 * @param action Value to set member attribute <b>action</b>
	 */
	public void setAction( String action ) {
		this.action = action;
	}

	/**
	 * Returns the value for the member attribute <b>action</b>
	 * @return String - value of member attribute <b>action</b>.
	 */
	public String getAction( ) {
		return this.action;
	}

	/**
	 * This method sets the value to the member attribute <b>requestData</b>.
	 * You cannot set null to the attribute.
	 * @param requestData Value to set member attribute <b>requestData</b>
	 */
	public void setRequestData( String requestData ) {
		this.requestData = requestData;
	}

	/**
	 * Returns the value for the member attribute <b>requestData</b>
	 * @return String - value of member attribute <b>requestData</b>.
	 */
	public String getRequestData( ) {
		return this.requestData;
	}

	/**
	 * This method sets the value to the member attribute <b>resourcePath</b>.
	 * You cannot set null to the attribute.
	 * @param resourcePath Value to set member attribute <b>resourcePath</b>
	 */
	public void setResourcePath( String resourcePath ) {
		this.resourcePath = resourcePath;
	}

	/**
	 * Returns the value for the member attribute <b>resourcePath</b>
	 * @return String - value of member attribute <b>resourcePath</b>.
	 */
	public String getResourcePath( ) {
		return this.resourcePath;
	}

	/**
	 * This method sets the value to the member attribute <b>resourceType</b>.
	 * You cannot set null to the attribute.
	 * @param resourceType Value to set member attribute <b>resourceType</b>
	 */
	public void setResourceType( String resourceType ) {
		this.resourceType = resourceType;
	}

	/**
	 * Returns the value for the member attribute <b>resourceType</b>
	 * @return String - value of member attribute <b>resourceType</b>.
	 */
	public String getResourceType( ) {
		return this.resourceType;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXAccessAudit=";
		str += super.toString();
		str += "id={" + id + "} ";
		str += "auditType={" + auditType + "} ";
		str += "accessResult={" + accessResult + "} ";
		str += "accessType={" + accessType + "} ";
		str += "aclEnforcer={" + aclEnforcer + "} ";
		str += "agentId={" + agentId + "} ";
		str += "clientIP={" + clientIP + "} ";
		str += "clientType={" + clientType + "} ";
		str += "policyId={" + policyId + "} ";
		str += "repoName={" + repoName + "} ";
		str += "repoType={" + repoType + "} ";
		str += "resultReason={" + resultReason + "} ";
		str += "eventTime={" + eventTime + "} ";
		str += "requestUser={" + requestUser + "} ";
		str += "action={" + action + "} ";
		str += "requestData={" + requestData + "} ";
		str += "resourcePath={" + resourcePath + "} ";
		str += "resourceType={" + resourceType + "} ";
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
		XXAccessAuditBase other = (XXAccessAuditBase) obj;
		if( this.auditType != other.auditType ) return false;
		if( this.accessResult != other.accessResult ) return false;
    	if ((this.accessType == null && other.accessType != null) || (this.accessType != null && !this.accessType.equals(other.accessType))) {
        		return false;
    	}
    	if ((this.aclEnforcer == null && other.aclEnforcer != null) || (this.aclEnforcer != null && !this.aclEnforcer.equals(other.aclEnforcer))) {
        		return false;
    	}
    	if ((this.agentId == null && other.agentId != null) || (this.agentId != null && !this.agentId.equals(other.agentId))) {
        		return false;
    	}
    	if ((this.clientIP == null && other.clientIP != null) || (this.clientIP != null && !this.clientIP.equals(other.clientIP))) {
        		return false;
    	}
    	if ((this.clientType == null && other.clientType != null) || (this.clientType != null && !this.clientType.equals(other.clientType))) {
        		return false;
    	}
    	if( this.policyId != other.policyId ) return false;
    	if ((this.repoName == null && other.repoName != null) || (this.repoName != null && !this.repoName.equals(other.repoName))) {
        		return false;
    	}
    	if( this.repoType != other.repoType ) return false;
    	if ((this.resultReason == null && other.resultReason != null) || (this.resultReason != null && !this.resultReason.equals(other.resultReason))) {
        		return false;
    	}
    	if ((this.sessionId == null && other.sessionId != null) || (this.sessionId != null && !this.sessionId.equals(other.sessionId))) {
        		return false;
    	}
    	if ((this.eventTime == null && other.eventTime != null) || (this.eventTime != null && !this.eventTime.equals(other.eventTime))) {
        		return false;
    	}
    	if ((this.requestUser == null && other.requestUser != null) || (this.requestUser != null && !this.requestUser.equals(other.requestUser))) {
        		return false;
    	}
    	if ((this.action == null && other.action != null) || (this.action != null && !this.action.equals(other.action))) {
        		return false;
    	}
    	if ((this.requestData == null && other.requestData != null) || (this.requestData != null && !this.requestData.equals(other.requestData))) {
        		return false;
    	}
    	if ((this.resourcePath == null && other.resourcePath != null) || (this.resourcePath != null && !this.resourcePath.equals(other.resourcePath))) {
        		return false;
    	}
    	if ((this.resourceType == null && other.resourceType != null) || (this.resourceType != null && !this.resourceType.equals(other.resourceType))) {
        		return false;
    	}
		return true;
	}
	public static String getEnumName(String fieldName ) {
		if( "auditType".equals(fieldName) ) {
			return "CommonEnums.AssetType";
		}
		if( "accessResult".equals(fieldName) ) {
			return "CommonEnums.AccessResult";
		}
		return null;
	}

}
