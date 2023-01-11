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

package org.apache.ranger.audit.model;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.google.gson.annotations.SerializedName;

public class AuthzAuditEvent extends AuditEventBase {
	protected static String FIELD_SEPARATOR = ";";

	protected static final int MAX_ACTION_FIELD_SIZE = 1800;
	protected static final int MAX_REQUEST_DATA_FIELD_SIZE = 1800;

	@SerializedName("repoType")
	protected int repositoryType = 0;

	@SerializedName("repo")
	protected String repositoryName = null;

	@SerializedName("reqUser")
	protected String user = null;

	@SerializedName("evtTime")
	protected Date eventTime = new Date();

	@SerializedName("access")
	protected String accessType = null;

	@SerializedName("resource")
	protected String resourcePath = null;

	@SerializedName("resType")
	protected String resourceType = null;

	@SerializedName("action")
	protected String action = null;

	@SerializedName("result")
	protected short accessResult = 0; // 0 - DENIED; 1 - ALLOWED; HTTP return
										// code

	@SerializedName("agent")
	protected String agentId = null;

	@SerializedName("policy")
	protected long policyId = 0;

	@SerializedName("reason")
	protected String resultReason = null;

	@SerializedName("enforcer")
	protected String aclEnforcer = null;

	@SerializedName("sess")
	protected String sessionId = null;

	@SerializedName("cliType")
	protected String clientType = null;

	@SerializedName("cliIP")
	protected String clientIP = null;

	@SerializedName("reqData")
	protected String requestData = null;

	@SerializedName("agentHost")
	protected String agentHostname = null;

	@SerializedName("logType")
	protected String logType = null;

	@SerializedName("id")
	protected String eventId = null;

	/**
	 * This to ensure order within a session. Order not guaranteed across
	 * processes and hosts
	 */
	@SerializedName("seq_num")
	protected long seqNum = 0;

	@SerializedName("event_count")
	protected long eventCount = 1;

	@SerializedName("event_dur_ms")
	protected long eventDurationMS = 0;

	@SerializedName("tags")
	protected Set<String> tags = new HashSet<>();

	@SerializedName("additional_info")
	protected String additionalInfo;
	
	@SerializedName("cluster_name")
	protected String clusterName;

	@SerializedName("zone_name")
	protected String zoneName;

	@SerializedName("policy_version")
	protected Long policyVersion;

	public AuthzAuditEvent() {
		super();

		this.repositoryType = 0;
	}

	public AuthzAuditEvent(int repositoryType, String repositoryName,
						   String user, Date eventTime, String accessType,
						   String resourcePath, String resourceType, String action,
						   short accessResult, String agentId, long policyId,
						   String resultReason, String aclEnforcer, String sessionId,
						   String clientType, String clientIP, String requestData, String clusterName) {
		this(repositoryType, repositoryName, user, eventTime, accessType, resourcePath, resourceType, action, accessResult, agentId,
				policyId, resultReason, aclEnforcer, sessionId, clientType, clientIP, requestData, clusterName, null);
	}

	public AuthzAuditEvent(int repositoryType, String repositoryName,
			String user, Date eventTime, String accessType,
			String resourcePath, String resourceType, String action,
			short accessResult, String agentId, long policyId,
			String resultReason, String aclEnforcer, String sessionId,
			String clientType, String clientIP, String requestData, String clusterName, String zoneName) {
		this(repositoryType, repositoryName, user, eventTime, accessType, resourcePath, resourceType, action, accessResult, agentId,
				policyId, resultReason, aclEnforcer, sessionId, clientType, clientIP, requestData, clusterName, zoneName, null);

	}

	public AuthzAuditEvent(int repositoryType, String repositoryName,
						   String user, Date eventTime, String accessType,
						   String resourcePath, String resourceType, String action,
						   short accessResult, String agentId, long policyId,
						   String resultReason, String aclEnforcer, String sessionId,
						   String clientType, String clientIP, String requestData, String clusterName, String zoneName, Long policyVersion) {
		this.repositoryType = repositoryType;
		this.repositoryName = repositoryName;
		this.user = user;
		this.eventTime = eventTime;
		this.accessType = accessType;
		this.resourcePath = resourcePath;
		this.resourceType = resourceType;
		this.action = action;
		this.accessResult = accessResult;
		this.agentId = agentId;
		this.policyId = policyId;
		this.resultReason = resultReason;
		this.aclEnforcer = aclEnforcer;
		this.sessionId = sessionId;
		this.clientType = clientType;
		this.clientIP = clientIP;
		this.requestData = requestData;
		this.clusterName = clusterName;
		this.zoneName = zoneName;
		this.policyVersion = policyVersion;
	}

	/**
	 * @return the repositoryType
	 */
	public int getRepositoryType() {
		return repositoryType;
	}

	/**
	 * @param repositoryType
	 *            the repositoryType to set
	 */
	public void setRepositoryType(int repositoryType) {
		this.repositoryType = repositoryType;
	}

	/**
	 * @return the repositoryName
	 */
	public String getRepositoryName() {
		return repositoryName;
	}

	/**
	 * @param repositoryName
	 *            the repositoryName to set
	 */
	public void setRepositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user
	 *            the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return the timeStamp
	 */
	public Date getEventTime() {
		return eventTime;
	}

	/**
	 * @param eventTime
	 *            the eventTime to set
	 */
	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
	}

	/**
	 * @return the accessType
	 */
	public String getAccessType() {
		return accessType;
	}

	/**
	 * @param accessType
	 *            the accessType to set
	 */
	public void setAccessType(String accessType) {
		this.accessType = accessType;
	}

	/**
	 * @return the resourcePath
	 */
	public String getResourcePath() {
		return resourcePath;
	}

	/**
	 * @param resourcePath
	 *            the resourcePath to set
	 */
	public void setResourcePath(String resourcePath) {
		this.resourcePath = resourcePath;
	}

	/**
	 * @return the resourceType
	 */
	public String getResourceType() {
		return resourceType;
	}

	/**
	 * @param resourceType
	 *            the resourceType to set
	 */
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}

	/**
	 * @return the action
	 */
	public String getAction() { return action; }

	/**
	 * @param action
	 *            the action to set
	 */
	public void setAction(String action) {
		this.action = action;
	}

	/**
	 * @return the accessResult
	 */
	public short getAccessResult() {
		return accessResult;
	}

	/**
	 * @param accessResult
	 *            the accessResult to set
	 */
	public void setAccessResult(short accessResult) {
		this.accessResult = accessResult;
	}

	/**
	 * @return the agentId
	 */
	public String getAgentId() {
		return agentId;
	}

	/**
	 * @param agentId
	 *            the agentId to set
	 */
	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	/**
	 * @return the policyId
	 */
	public long getPolicyId() {
		return policyId;
	}

	/**
	 * @param policyId
	 *            the policyId to set
	 */
	public void setPolicyId(long policyId) {
		this.policyId = policyId;
	}

	/**
	 * @return the resultReason
	 */
	public String getResultReason() {
		return resultReason;
	}

	/**
	 * @param resultReason
	 *            the resultReason to set
	 */
	public void setResultReason(String resultReason) {
		this.resultReason = resultReason;
	}

	/**
	 * @return the aclEnforcer
	 */
	public String getAclEnforcer() {
		return aclEnforcer;
	}

	/**
	 * @param aclEnforcer
	 *            the aclEnforcer to set
	 */
	public void setAclEnforcer(String aclEnforcer) {
		this.aclEnforcer = aclEnforcer;
	}

	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * @param sessionId
	 *            the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * @return the clientType
	 */
	public String getClientType() {
		return clientType;
	}

	/**
	 * @param clientType
	 *            the clientType to set
	 */
	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	/**
	 * @return the clientIP
	 */
	public String getClientIP() {
		return clientIP;
	}

	/**
	 * @param clientIP
	 *            the clientIP to set
	 */
	public void setClientIP(String clientIP) {
		this.clientIP = clientIP;
	}

	/**
	 * @return the requestData
	 */
	public String getRequestData() { return requestData; }

	/**
	 * @param requestData
	 *            the requestData to set
	 */
	public void setRequestData(String requestData) {
		this.requestData = requestData;
	}

	public String getAgentHostname() {
		return agentHostname;
	}

	public void setAgentHostname(String agentHostname) {
		this.agentHostname = agentHostname;
	}

	public String getLogType() {
		return logType;
	}

	public void setLogType(String logType) {
		this.logType = logType;
	}

	public String getEventId() {
		return eventId;
	}

	public void setEventId(String eventId) {
		this.eventId = eventId;
	}

	public long getSeqNum() {
		return seqNum;
	}

	public void setSeqNum(long seqNum) {
		this.seqNum = seqNum;
	}

	public long getEventCount() {
		return eventCount;
	}

	public void setEventCount(long frequencyCount) {
		this.eventCount = frequencyCount;
	}

	public long getEventDurationMS() {
		return eventDurationMS;
	}

	public Set<String> getTags() {
		return tags;
	}

	public void setEventDurationMS(long frequencyDurationMS) {
		this.eventDurationMS = frequencyDurationMS;
	}

	public void setTags(Set<String> tags) {
		this.tags = tags;
	}

	public String getClusterName() {
		return clusterName;
	}

	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}

	public String getZoneName() {
		return zoneName;
	}

	public void setPolicyVersion(Long policyVersion) {
		this.policyVersion = policyVersion;
	}

	public Long getPolicyVersion() {
		return policyVersion;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getAdditionalInfo() { return this.additionalInfo; }

	public void setAdditionalInfo(String additionalInfo) { this.additionalInfo = additionalInfo; }

	@Override
	public String getEventKey() {
		String key = user + "^" + accessType + "^" + resourcePath + "^"
				+ resourceType + "^" + action + "^" + accessResult + "^"
				+ sessionId + "^" + clientIP;
		return key;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		sb.append("AuthzAuditEvent{");
		toString(sb);
		sb.append("}");

		return sb.toString();
	}

	protected StringBuilder toString(StringBuilder sb) {
		sb.append("repositoryType=").append(repositoryType)
				.append(FIELD_SEPARATOR).append("repositoryName=")
				.append(repositoryName).append(FIELD_SEPARATOR).append("user=")
				.append(user).append(FIELD_SEPARATOR).append("eventTime=")
				.append(eventTime).append(FIELD_SEPARATOR)
				.append("accessType=").append(accessType)
				.append(FIELD_SEPARATOR).append("resourcePath=")
				.append(resourcePath).append(FIELD_SEPARATOR)
				.append("resourceType=").append(resourceType)
				.append(FIELD_SEPARATOR).append("action=").append(action)
				.append(FIELD_SEPARATOR).append("accessResult=")
				.append(accessResult).append(FIELD_SEPARATOR)
				.append("agentId=").append(agentId).append(FIELD_SEPARATOR)
				.append("policyId=").append(policyId).append(FIELD_SEPARATOR)
				.append("resultReason=").append(resultReason)
				.append(FIELD_SEPARATOR).append("aclEnforcer=")
				.append(aclEnforcer).append(FIELD_SEPARATOR)
				.append("sessionId=").append(sessionId).append(FIELD_SEPARATOR)
				.append("clientType=").append(clientType)
				.append(FIELD_SEPARATOR).append("clientIP=").append(clientIP)
				.append(FIELD_SEPARATOR).append("requestData=")
				.append(requestData).append(FIELD_SEPARATOR)
				.append("agentHostname=").append(agentHostname)
				.append(FIELD_SEPARATOR).append("logType=").append(logType)
				.append(FIELD_SEPARATOR).append("eventId=").append(eventId)
				.append(FIELD_SEPARATOR).append("seq_num=").append(seqNum)
				.append(FIELD_SEPARATOR).append("event_count=")
				.append(eventCount).append(FIELD_SEPARATOR)
				.append("event_dur_ms=").append(eventDurationMS)
				.append(FIELD_SEPARATOR)
				.append("tags=").append("[")
				.append(StringUtils.join(tags, ", "))
				.append("]")
				.append(FIELD_SEPARATOR).append("clusterName=").append(clusterName)
				.append(FIELD_SEPARATOR).append("zoneName=").append(zoneName)
				.append(FIELD_SEPARATOR).append("policyVersion=").append(policyVersion)
				.append(FIELD_SEPARATOR).append("additionalInfo=").append(additionalInfo);

		return sb;
	}
}
