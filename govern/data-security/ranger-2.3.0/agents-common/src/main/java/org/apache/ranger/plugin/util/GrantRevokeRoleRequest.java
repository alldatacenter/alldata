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

package org.apache.ranger.plugin.util;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;


@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GrantRevokeRoleRequest implements Serializable {
	private static final long serialVersionUID = 1L;

	private String              grantor;
	private Set<String>         grantorGroups;
	private Set<String>         targetRoles;
	private Set<String>         users;
	private Set<String>         groups;
	private Set<String>         roles;
	private Boolean             grantOption              = Boolean.FALSE;
	private String              clientIPAddress;
	private String              clientType;
	private String              requestData;
	private String              sessionId;
	private String              clusterName;

	public GrantRevokeRoleRequest() {
		this(null, null, null, null, null, null, null, null, null, null, null);
	}

	public GrantRevokeRoleRequest(String grantor, Set<String> grantorGroups, Set<String> targetRoles, Set<String> users,
								  Set<String> groups, Set<String> roles, Boolean grantOption,
								  String clientIPAddress, String clientType,
								  String requestData, String sessionId) {
		setGrantor(grantor);
		setGrantorGroups(grantorGroups);
		setTargetRoles(targetRoles);
		setUsers(users);
		setGroups(groups);
		setRoles(roles);
		setGrantOption(grantOption);
		setClientIPAddress(clientIPAddress);
		setClientType(clientType);
		setRequestData(requestData);
		setSessionId(sessionId);
		setClusterName(clusterName);
	}

	/**
	 * @return the grantor
	 */
	public String getGrantor() {
		return grantor;
	}

	/**
	 * @param grantor the grantor to set
	 */
	public void setGrantor(String grantor) {
		this.grantor = grantor;
	}

	/**
	 * @return the grantorRoles
	 */
	public Set<String> getGrantorGroups() {
		return grantorGroups;
	}

	/**
	 * @param grantorGroups the grantorRoles to set
	 */

	public void setGrantorGroups(Set<String> grantorGroups) {
		this.grantorGroups = grantorGroups;
	}

	/**
	 * @return the targetRoles
	 */
	public Set<String> getTargetRoles() {
		return targetRoles;
	}

	/**
	 * @param targetRoles the targetRoles to set
	 */
	public void setTargetRoles(Set<String> targetRoles) {
		this.targetRoles = targetRoles == null ? new HashSet<String>() : targetRoles;
	}

	/**
	 * @return the users
	 */
	public Set<String> getUsers() {
		return users;
	}

	/**
	 * @param users the users to set
	 */
	public void setUsers(Set<String> users) {
		this.users = users == null ? new HashSet<String>() : users;
	}

	/**
	 * @return the groups
	 */
	public Set<String> getGroups() {
		return groups;
	}

	/**
	 * @param groups the groups to set
	 */
	public void setGroups(Set<String> groups) {
		this.groups = groups;
	}

	/**
	 * @return the roles
	 */
	public Set<String> getRoles() {
		return roles;
	}

	/**
	 * @param roles the roles to set
	 */
	public void setRoles(Set<String> roles) {
		this.roles = roles == null ? new HashSet<String>() : roles;
	}

	/**
	 * @return the grantOption
	 */
	public Boolean getGrantOption() {
		return grantOption;
	}

	/**
	 * @param grantOption the grantOption to set
	 */
	public void setGrantOption(Boolean grantOption) {
		this.grantOption = grantOption == null ? Boolean.FALSE : grantOption;
	}

	/**
	 * @return the clientIPAddress
	 */
	public String getClientIPAddress() {
		return clientIPAddress;
	}

	/**
	 * @param clientIPAddress the clientIPAddress to set
	 */
	public void setClientIPAddress(String clientIPAddress) {
		this.clientIPAddress = clientIPAddress;
	}

	/**
	 * @return the clientType
	 */
	public String getClientType() {
		return clientType;
	}

	/**
	 * @param clientType the clientType to set
	 */
	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	/**
	 * @return the requestData
	 */
	public String getRequestData() {
		return requestData;
	}

	/**
	 * @param requestData the requestData to set
	 */
	public void setRequestData(String requestData) {
		this.requestData = requestData;
	}

	/**
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * @param sessionId the sessionId to set
	 */
	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}

	/**
	 * @return the clusterName
	 */
	public String getClusterName() {
		return clusterName;
	}

	/**
	 * @param clusterName the clusterName to set
	 */
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	@Override
	public String toString( ) {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("GrantRevokeRoleRequest={");

		sb.append("grantor={").append(grantor).append("} ");

		sb.append("targetRoles={");
		if(targetRoles != null) {
			for(String targetRole : targetRoles) {
				sb.append(targetRole).append(" ");
			}
		}
		sb.append("} ");

		sb.append("users={");
		if(users != null) {
			for(String user : users) {
				sb.append(user).append(" ");
			}
		}
		sb.append("} ");

		sb.append("groups={");
		if(groups != null) {
			for(String group : groups) {
				sb.append(group).append(" ");
			}
		}
		sb.append("} ");

		sb.append("roles={");
		if(roles != null) {
			for(String role : roles) {
				sb.append(role).append(" ");
			}
		}
		sb.append("} ");

		sb.append("grantOption={").append(grantOption).append("} ");
		sb.append("clientIPAddress={").append(clientIPAddress).append("} ");
		sb.append("clientType={").append(clientType).append("} ");
		sb.append("requestData={").append(requestData).append("} ");
		sb.append("sessionId={").append(sessionId).append("} ");
		sb.append("clusterName={").append(clusterName).append("} ");

		sb.append("}");

		return sb;
	}
}
