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
 * Access Audit
 *
 */

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.DateUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.json.JsonDateSerializer;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXAccessAudit extends VXDataObject implements java.io.Serializable {
	private static final long serialVersionUID = 1L;


	/**
	 * Repository Type
	 */
	protected int auditType;
	/**
	 * Access Result
	 * This attribute is of type enum CommonEnums::AccessResult
	 */
	protected int accessResult = RangerConstants.ACCESS_RESULT_DENIED;
	/**
	 * Access Type
	 */
	protected String accessType;
	/**
	 * Acl Enforcer
	 */
	protected String aclEnforcer;
	/**
	 * Agent Id
	 */
	protected String agentId;
	/**
	 * Client Ip
	 */
	protected String clientIP;
	/**
	 * Client Type
	 */
	protected String clientType;
	/**
	 * Policy Id
	 */
	protected long policyId;
	/**
	 * Repository Name
	 */
	protected String repoName;
	/**
	 * Repository Display Name
	 */
	protected String repoDisplayName;
	/**
	 * Repository Type
	 */
	protected int repoType;
	/**
	 * Service Type ~~ repoType
	 */
	protected String serviceType;
	/**
	 * Service Type Display Name
	 */
	protected String serviceTypeDisplayName;
	/**
	 * Reason of result
	 */
	protected String resultReason;
	/**
	 * Session Id
	 */
	protected String sessionId;
	/**
	 * Event Time
	 */
	@JsonSerialize(using=JsonDateSerializer.class)
	protected Date eventTime = DateUtil.getUTCDate();
	/**
	 * Requesting User
	 */
	protected String requestUser;
	/**
	 * Action
	 */
	protected String action;
	/**
	 * Requesting Data
	 */
	protected String requestData;
	/**
	 * Resource Path
	 */
	protected String resourcePath;
	/**
	 * Resource Type
	 */
	protected String resourceType;

	protected long sequenceNumber;

	protected long eventCount;
	
	//event duration in ms
	protected long eventDuration;
	
	protected String tags;
	
	protected String clusterName;

	// Security Zone
	protected String zoneName;
	// Host Name
	protected String agentHost;

	// Policy Version

	protected Long policyVersion;

	// Event ID
	protected String eventId;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public VXAccessAudit ( ) {
		accessResult = RangerConstants.ACCESS_RESULT_DENIED;
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
	 * This method sets the value to the member attribute <b>repoDisplayName</b>.
	 * You cannot set null to the attribute.
	 * @param repoDisplayName Value to set member attribute <b>repoDisplayName</b>
	 */
	public void setRepoDisplayName(String repoDisplayName) {
		this.repoDisplayName = repoDisplayName;
	}

	/**
	 * Returns the value for the member attribute <b>repoDisplayName</b>
	 * @return String - value of member attribute <b>repoDisplayName</b>.
	 */
	public String getRepoDisplayName() {
		return repoDisplayName;
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
	 * @return the serviceType
	 */
	public String getServiceType() {
		return serviceType;
	}

	/**
	 * @param serviceType the serviceType to set
	 */
	public void setServiceType(String serviceType) {
		this.serviceType = serviceType;
	}

	/**
	 * @return the serviceTypeDisplayName
	 */
	public String getServiceTypeDisplayName() {
		return serviceTypeDisplayName;
	}

	/**
	 * @param serviceTypeDisplayName the serviceTypeDisplayName to set
	 */
	public void setServiceTypeDisplayName(String serviceTypeDisplayName) {
		this.serviceTypeDisplayName = serviceTypeDisplayName;
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

	public long getSequenceNumber() {
		return sequenceNumber;
	}

	public void setSequenceNumber(long sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public long getEventCount() {
		return eventCount;
	}

	public void setEventCount(long eventCount) {
		this.eventCount = eventCount;
	}

	public long getEventDuration() {
		return eventDuration;
	}

	public void setEventDuration(long eventDuration) {
		this.eventDuration = eventDuration;
	}

	/**
	 * @return the tags
	 */
	public String getTags() {
		return tags;
	}

	/**
	 * @param tags
	 *            the tags to set
	 */
	public void setTags(String tags) {
		this.tags = tags;
	}
	
	/**
	 * @return the clusterName
	 */
	public String getClusterName() {
		return clusterName;
	}
	/**
	 * @param clusterName
	 *            the clusterName to set
	 */
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	@Override
	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_XA_ACCESS_AUDIT;
	}

	/**
	 * @return the zoneName
	 */
	public String getZoneName() {
		return zoneName;
	}
	/**
	 * @param zoneName
	 *            the zoneName to set
	 */
	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}

	public String getAgentHost() {
		return agentHost;
	}

	public void setAgentHost(String agentHost) {
		this.agentHost = agentHost;
	}

	/**
	 * @return the policyVersion
	 */
	public Long getPolicyVersion() {
		return policyVersion;
	}
	/**
	 * @param policyVersion
	 *            the policyVersion to set
	 */
	public void setPolicyVersion(Long policyVersion) {
		this.policyVersion = policyVersion;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	public String toString( ) {
		String str = "VXAccessAudit={";
		str += super.toString();
		str += "auditType={" + auditType + "} ";
		str += "accessResult={" + accessResult + "} ";
		str += "accessType={" + accessType + "} ";
		str += "aclEnforcer={" + aclEnforcer + "} ";
		str += "agentId={" + agentId + "} ";
		str += "clientIP={" + clientIP + "} ";
		str += "clientType={" + clientType + "} ";
		str += "policyId={" + policyId + "} ";
		str += "policyVersion={" + policyVersion + "} ";
		str += "repoName={" + repoName + "} ";
		str += "repoDisplayName={" + repoDisplayName + "} ";
		str += "repoType={" + repoType + "} ";
		str += "serviceType={" + serviceType + "} ";
		str += "serviceTypeDisplayName={" + serviceTypeDisplayName + "} ";
		str += "resultReason={" + resultReason + "} ";
		str += "sessionId={" + sessionId + "} ";
		str += "eventTime={" + eventTime + "} ";
		str += "requestUser={" + requestUser + "} ";
		str += "action={" + action + "} ";
		str += "requestData={" + requestData + "} ";
		str += "resourcePath={" + resourcePath + "} ";
		str += "resourceType={" + resourceType + "} ";
		str += "sequenceNumber={" + sequenceNumber + "}";
		str += "eventCount={" + eventCount + "}";
		str += "eventDuration={" + eventDuration + "}";
		str += "tags={" + tags + "}";
		str += "clusterName={" + clusterName + "}";
		str += "zoneName={" + zoneName + "}";
		str += "agentHost={" + agentHost + "}";
		str += "eventId={" + eventId + "}";
		str += "}";
		return str;
	}
}
