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

public class FileSyncSourceInfo {
	private String fileName;
	private String syncTime;
	private String lastModified;
	private long totalUsersSynced;
	private long totalGroupsSynced;
	private long totalUsersDeleted;
	private long totalGroupsDeleted;

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getSyncTime() {
		return syncTime;
	}

	public void setSyncTime(String syncTime) {
		this.syncTime = syncTime;
	}

	public String getLastModified() {
		return lastModified;
	}

	public void setLastModified(String lastModified) {
		this.lastModified = lastModified;
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
		sb.append("FileSycnSourceInfo [fileName= ").append(fileName);
		sb.append(", syncTime= ").append(syncTime);
		sb.append(", lastModified= ").append(lastModified);
		sb.append(", totalUsersSynced= ").append(totalUsersSynced);
		sb.append(", totalGroupsSynced= ").append(totalGroupsSynced);
		sb.append(", totalUsersDeleted= ").append(totalUsersDeleted);
		sb.append(", totalGroupsDeleted= ").append(totalGroupsDeleted);
		sb.append("]");
		return sb;
	}
}
