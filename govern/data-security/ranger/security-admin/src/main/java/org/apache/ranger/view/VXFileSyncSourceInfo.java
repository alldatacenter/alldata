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

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlRootElement;

@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
public class VXFileSyncSourceInfo implements java.io.Serializable  {

	private static final long serialVersionUID = 1L;

	private String fileName;
	private String syncTime;
	private String lastModified;
	private long totalUsersSynced;
	private long totalGroupsSynced;
	private long totalUsersDeleted;
	private long totalGroupsDeleted;

	public VXFileSyncSourceInfo() {
	}

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
		sb.append("{\"fileName\":\"").append(fileName);
		sb.append("\", \"syncTime\":\"").append(syncTime);
		sb.append("\", \"lastModified\":\"").append(lastModified);
		sb.append("\", \"totalUsersSynced\":\"").append(totalUsersSynced);
		sb.append("\", \"totalGroupsSynced\":\"").append(totalGroupsSynced);
		sb.append("\", \"totalUsersDeleted\":\"").append(totalUsersDeleted);
		sb.append("\", \"totalGroupsDeleted\":\"").append(totalGroupsDeleted);
		sb.append("\"}");
		return sb;
	}
}