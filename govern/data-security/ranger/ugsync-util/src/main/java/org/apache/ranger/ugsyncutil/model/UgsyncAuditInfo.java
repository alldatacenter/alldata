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

 package org.apache.ranger.ugsyncutil.model;

public class UgsyncAuditInfo {

	private String userName;
	private Long noOfNewUsers;
	private Long noOfNewGroups;
	private Long noOfModifiedUsers;
	private Long noOfModifiedGroups;
	private String 	syncSource;
	private String sessionId;
	private LdapSyncSourceInfo ldapSyncSourceInfo;
	private UnixSyncSourceInfo unixSyncSourceInfo;
	private FileSyncSourceInfo fileSyncSourceInfo;

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

	public LdapSyncSourceInfo getLdapSyncSourceInfo() {
		return ldapSyncSourceInfo;
	}

	public void setLdapSyncSourceInfo(LdapSyncSourceInfo ldapSyncSourceInfo) {
		this.ldapSyncSourceInfo = ldapSyncSourceInfo;
	}

	public UnixSyncSourceInfo getUnixSyncSourceInfo() {
		return unixSyncSourceInfo;
	}

	public void setUnixSyncSourceInfo(UnixSyncSourceInfo unixSyncSourceInfo) {
		this.unixSyncSourceInfo = unixSyncSourceInfo;
	}

	public FileSyncSourceInfo getFileSyncSourceInfo() {
		return fileSyncSourceInfo;
	}

	public void setFileSyncSourceInfo(FileSyncSourceInfo fileSyncSourceInfo) {
		this.fileSyncSourceInfo = fileSyncSourceInfo;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb);
		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("UgsyncAuditInfo [No. of New users= ").append(noOfNewUsers);
		sb.append(", No. of New groups= ").append(noOfNewGroups);
		sb.append(", No. of Modified users= ").append(noOfModifiedUsers);
		sb.append(", No. of Modified groups= ").append(noOfModifiedGroups);
		sb.append(", syncSource= ").append(syncSource);
		sb.append(", ldapSyncSourceInfo= ").append(ldapSyncSourceInfo);
		sb.append(", unixSyncSourceInfo= ").append(unixSyncSourceInfo);
		sb.append(", fileSyncSourceInfo= ").append(fileSyncSourceInfo);
		sb.append("]");
		return sb;
	}
}
