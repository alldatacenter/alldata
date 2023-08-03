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

import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@JsonAutoDetect(getterVisibility = Visibility.NONE, setterVisibility = Visibility.NONE, fieldVisibility = Visibility.ANY)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
public class UsersGroupRoleAssignments {

	List<String> users;
	
	Map<String, String> groupRoleAssignments;

	Map<String, String> userRoleAssignments;

	Map<String, String> whiteListGroupRoleAssignments;

	Map<String, String> whiteListUserRoleAssignments;

	boolean isReset = false;

	public List<String> getUsers() {
		return users;
	}

	public void setUsers(List<String> users) {
		this.users = users;
	}

	public Map<String, String> getGroupRoleAssignments() {
		return groupRoleAssignments;
	}

	public void setGroupRoleAssignments(Map<String, String> groupRoleAssignments) {
		this.groupRoleAssignments = groupRoleAssignments;
	}

	public Map<String, String> getUserRoleAssignments() {
		return userRoleAssignments;
	}

	public void setUserRoleAssignments(Map<String, String> userRoleAssignments) {
		this.userRoleAssignments = userRoleAssignments;
	}

	public Map<String, String> getWhiteListGroupRoleAssignments() {
		return whiteListGroupRoleAssignments;
	}

	public void setWhiteListGroupRoleAssignments(Map<String, String> whiteListGroupRoleAssignments) {
		this.whiteListGroupRoleAssignments = whiteListGroupRoleAssignments;
	}

	public Map<String, String> getWhiteListUserRoleAssignments() {
		return whiteListUserRoleAssignments;
	}

	public void setWhiteListUserRoleAssignments(Map<String, String> whiteListUserRoleAssignments) {
		this.whiteListUserRoleAssignments = whiteListUserRoleAssignments;
	}

	public boolean isReset() {
		return isReset;
	}

	public void setReset(boolean reset) {
		isReset = reset;
	}
}