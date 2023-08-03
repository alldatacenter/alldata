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

public class LdapSyncSourceInfo {
	private String ldapUrl;
	private String incrementalSycn;
	private String groupSearchFirstEnabled;
	private String groupSearchEnabled;
	private String userSearchEnabled;
	private String userSearchFilter;
	private String groupSearchFilter;
	private String groupHierarchyLevel;

	private long totalUsersSynced;
	private long totalGroupsSynced;
	private long totalUsersDeleted;
	private long totalGroupsDeleted;

	public String getLdapUrl() {
		return ldapUrl;
	}

	public void setLdapUrl(String ldapUrl) {
		this.ldapUrl = ldapUrl;
	}

	public String isIncrementalSycn() {
		return incrementalSycn;
	}

	public void setIncrementalSycn(String incrementalSycn) {
		this.incrementalSycn = incrementalSycn;
	}

	public String getUserSearchFilter() {
		return userSearchFilter;
	}

	public void setUserSearchFilter(String userSearchFilter) {
		this.userSearchFilter = userSearchFilter;
	}

	public String getGroupSearchFilter() {
		return groupSearchFilter;
	}

	public void setGroupSearchFilter(String groupSearchFilter) {
		this.groupSearchFilter = groupSearchFilter;
	}

	public String getGroupHierarchyLevel() {
		return groupHierarchyLevel;
	}

	public void setGroupHierarchyLevel(String groupHierarchyLevel) {
		this.groupHierarchyLevel = groupHierarchyLevel;
	}

	public long getTotalUsersSynced() {
		return totalUsersSynced;
	}

	public void setTotalUsersSynced(long totalUsersSynced) {
		this.totalUsersSynced = totalUsersSynced;
	}

	public long getTotalGroupsSynced() {
		return totalGroupsSynced;
	}

	public void setTotalGroupsSynced(long totalGroupsSynced) {
		this.totalGroupsSynced = totalGroupsSynced;
	}

	public String getGroupSearchFirstEnabled() {
		return groupSearchFirstEnabled;
	}

	public void setGroupSearchFirstEnabled(String groupSearchFirstEnabled) {
		this.groupSearchFirstEnabled = groupSearchFirstEnabled;
	}

	public String getGroupSearchEnabled() {
		return groupSearchEnabled;
	}

	public void setGroupSearchEnabled(String groupSearchEnabled) {
		this.groupSearchEnabled = groupSearchEnabled;
	}

	public String getUserSearchEnabled() {
		return userSearchEnabled;
	}

	public void setUserSearchEnabled(String userSearchEnabled) {
		this.userSearchEnabled = userSearchEnabled;
	}

	public long getTotalUsersDeleted() {
		return totalUsersDeleted;
	}

	public void setTotalUsersDeleted(long totalUsersDeleted) {
		this.totalUsersDeleted = totalUsersDeleted;
	}

	public long getTotalGroupsDeleted() {
		return totalGroupsDeleted;
	}

	public void setTotalGroupsDeleted(long totalGroupsDeleted) {
		this.totalGroupsDeleted = totalGroupsDeleted;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString(sb);
		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("LdapSycnSourceInfo [ldapUrl= ").append(ldapUrl);
		sb.append(", isIncrementalSync= ").append(incrementalSycn);
		sb.append(", userSearchEnabled= ").append(userSearchEnabled);
		sb.append(", groupSearchEnabled= ").append(groupSearchEnabled);
		sb.append(", groupSearchFirstEnabled= ").append(groupSearchFirstEnabled);
		sb.append(", userSearchFilter= ").append(userSearchFilter);
		sb.append(", groupSearchFilter= ").append(groupSearchFilter);
		sb.append(", groupHierarchyLevel= ").append(groupHierarchyLevel);
		sb.append(", totalUsersSynced= ").append(totalUsersSynced);
		sb.append(", totalGroupsSynced= ").append(totalGroupsSynced);
		sb.append(", totalUsersDeleted= ").append(totalUsersDeleted);
		sb.append(", totalGroupsDeleted= ").append(totalGroupsDeleted);
		sb.append("]");
		return sb;
	}
}
