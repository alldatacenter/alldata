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

import org.apache.ranger.common.AppConstants;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@Entity
@Cacheable
@XmlRootElement
@Table(name = "x_ugsync_audit_info")
public class XXUgsyncAuditInfo extends XXDBBase implements java.io.Serializable {
	private static final long serialVersionUID = 1L;

	@Id
	@SequenceGenerator(name = "X_UGSYNC_AUDIT_INFO_SEQ", sequenceName = "X_UGSYNC_AUDIT_INFO_SEQ", allocationSize = 1)
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "X_UGSYNC_AUDIT_INFO_SEQ")
	@Column(name = "id")
	protected Long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="event_time"   )
	protected Date eventTime;

	@Column(name = "user_name")
	protected String userName;

	@Column(name = "sync_source")
	protected String syncSource;

	@Column(name = "no_of_new_users")
	protected Long noOfNewUsers;

	@Column(name = "no_of_new_groups")
	protected Long noOfNewGroups;

	@Column(name = "no_of_modified_users")
	protected Long noOfModifiedUsers;

	@Column(name = "no_of_modified_groups")
	protected Long noOfModifiedGroups;

	@Column(name = "sync_source_info")
	protected String syncSourceInfo;

	@Column(name="session_id")
	protected String sessionId;

	/**
	 * Default constructor. This will set all the attributes to default value.
	 */
	public XXUgsyncAuditInfo() {
	}

	public int getMyClassType( ) {
	    return AppConstants.CLASS_TYPE_UGYNC_AUDIT_INFO;
	}

	public String getMyDisplayValue() {
		return null;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return this.id;
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

	public String getSyncSource() {
		return syncSource;
	}

	public void setSyncSource(String syncSource) {
		this.syncSource = syncSource;
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

	public String getSyncSourceInfo() {
		return syncSourceInfo;
	}

	public void setSyncSourceInfo(String syncSourceInfo) {
		this.syncSourceInfo = syncSourceInfo;
	}

	public String getSessionId() {
		return sessionId;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * This return the bean content in string format
	 * @return formatedStr
	*/
	@Override
	public String toString( ) {
		String str = "XXUgsyncAuditInfo={";
		str += "id={" + id + "} ";
		str += "eventTime={" + eventTime + "} ";
		str += "userName={" + userName + "} ";
		str += "syncSource={" + syncSource + "} ";
		str += "noOfNewUsers={" + noOfNewUsers + "} ";
		str += "noOfNewGroups={" + noOfNewGroups + "} ";
		str += "noOfModifiedUsers={" + noOfModifiedUsers + "} ";
		str += "noOfModifiedGroups={" + noOfModifiedGroups + "} ";
		str += "syncSourceInfo={" + syncSourceInfo + "} ";
		str += "sessionId={" + sessionId + "} ";
		str += "}";
		return str;
	}

	/**
	 * Checks for all attributes except referenced db objects
	 * @return true if all attributes match
	*/
	@Override
	public boolean equals( Object obj) {
		if (obj == null)
			return false;
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		XXUgsyncAuditInfo other = (XXUgsyncAuditInfo) obj;
		if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
			return false;
		}
		if ((this.eventTime == null && other.eventTime != null) || (this.eventTime != null && !this.eventTime.equals(other.eventTime))) {
			return false;
		}
		if ((this.userName == null && other.userName != null) || (this.userName != null && !this.userName.equals(other.userName))) {
			return false;
		}
		if ((this.syncSource == null && other.syncSource != null) || (this.syncSource != null && !this.syncSource.equals(other.syncSource))) {
			return false;
		}
		if ((this.noOfNewUsers == null && other.noOfNewUsers != null) || (this.noOfNewUsers != null && !this.noOfNewUsers.equals(other.noOfNewUsers))) {
			return false;
		}
		if ((this.noOfNewGroups == null && other.noOfNewGroups != null) || (this.noOfNewGroups != null && !this.noOfNewGroups.equals(other.noOfNewGroups))) {
			return false;
		}
		if ((this.noOfModifiedUsers == null && other.noOfModifiedUsers != null) || (this.noOfModifiedUsers != null && !this.noOfModifiedUsers.equals(other.noOfModifiedUsers))) {
			return false;
		}
		if ((this.noOfModifiedGroups == null && other.noOfModifiedGroups != null) || (this.noOfModifiedGroups != null && !this.noOfModifiedGroups.equals(other.noOfModifiedGroups))) {
			return false;
		}
		if ((this.syncSourceInfo == null && other.syncSourceInfo != null) || (this.syncSourceInfo != null && !this.syncSourceInfo.equals(other.syncSourceInfo))) {
			return false;
		}
		if ((this.sessionId == null && other.sessionId != null) || (this.sessionId != null && !this.sessionId.equals(other.sessionId))) {
			return false;
		}
		return true;
	}

	public static boolean equals(Object object1, Object object2) {
		if (object1 == object2) {
			return true;
		}
		if ((object1 == null) || (object2 == null)) {
			return false;
		}
		return object1.equals(object2);
	}

}
