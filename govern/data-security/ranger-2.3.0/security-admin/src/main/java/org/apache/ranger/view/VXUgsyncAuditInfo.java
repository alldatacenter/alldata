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
 * UserGroupInfo
 *
 */

import org.apache.ranger.json.JsonDateSerializer;
import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXUgsyncAuditInfo extends VXDataObject implements java.io.Serializable  {

	private static final long serialVersionUID = 1L;

	@JsonSerialize(using=JsonDateSerializer.class)
	private Date eventTime;

	private String userName;
	private Long noOfNewUsers;
	private Long noOfNewGroups;
	private Long noOfModifiedUsers;
	private Long noOfModifiedGroups;
	private String syncSource;
	private String sessionId;
	private Map<String, String> syncSourceInfo;
	private VXLdapSyncSourceInfo ldapSyncSourceInfo;
	private VXFileSyncSourceInfo fileSyncSourceInfo;
	private VXUnixSyncSourceInfo unixSyncSourceInfo;

	public VXUgsyncAuditInfo() {
	}

	public Date getEventTime() {
		return eventTime;
	}

	public void setEventTime(Date eventTime) {
		this.eventTime = eventTime;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Long getNoOfNewUsers() {
		return noOfNewUsers;
	}

	public void setNoOfNewUsers(Long noOfUsers) {
		this.noOfNewUsers = noOfUsers;
	}

	public Long getNoOfModifiedUsers() {
		return noOfModifiedUsers;
	}

	public void setNoOfModifiedUsers(Long noOfModifiedUsers) {
		this.noOfModifiedUsers = noOfModifiedUsers;
	}

	public Long getNoOfNewGroups() {
		return noOfNewGroups;
	}

	public void setNoOfNewGroups(Long noOfNewGroups) {
		this.noOfNewGroups = noOfNewGroups;
	}

	public Long getNoOfModifiedGroups() {
		return noOfModifiedGroups;
	}

	public void setNoOfModifiedGroups(Long noOfModifiedGroups) {
		this.noOfModifiedGroups = noOfModifiedGroups;
	}

	public String getSyncSource() {
		return syncSource;
	}

	public void setSyncSource(String syncSource) {
		this.syncSource = syncSource;
	}

	public VXLdapSyncSourceInfo getLdapSyncSourceInfo() {
		return ldapSyncSourceInfo;
	}

	public void setLdapSyncSourceInfo(VXLdapSyncSourceInfo ldapSyncSourceInfo) {
		this.ldapSyncSourceInfo = ldapSyncSourceInfo;
	}

	public VXFileSyncSourceInfo getFileSyncSourceInfo() {
		return fileSyncSourceInfo;
	}

	public void setFileSyncSourceInfo(VXFileSyncSourceInfo fileSyncSourceInfo) {
		this.fileSyncSourceInfo = fileSyncSourceInfo;
	}

	public VXUnixSyncSourceInfo getUnixSyncSourceInfo() {
		return unixSyncSourceInfo;
	}

	public void setUnixSyncSourceInfo(VXUnixSyncSourceInfo unixSyncSourceInfo) {
		this.unixSyncSourceInfo = unixSyncSourceInfo;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	public Map<String, String> getSyncSourceInfo() {
		return syncSourceInfo;
	}

	public void setSyncSourceInfo(Map<String, String> syncSourceInfo) {
		this.syncSourceInfo = syncSourceInfo == null ? new HashMap<String, String>() :syncSourceInfo;
	}
}