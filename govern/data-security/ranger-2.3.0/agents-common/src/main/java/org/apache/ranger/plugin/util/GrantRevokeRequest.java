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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonAutoDetect;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.annotate.JsonSerialize;


@JsonAutoDetect(getterVisibility=Visibility.NONE, setterVisibility=Visibility.NONE, fieldVisibility=Visibility.ANY)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL )
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class GrantRevokeRequest implements Serializable {
	private static final long serialVersionUID = 1L;

	private String              grantor;
	private Set<String>         grantorGroups;
	private Map<String, String> resource;
	private Set<String>         users;
	private Set<String>         groups;
	private Set<String>         roles;
	private Set<String>         accessTypes;
	private List<String>        forwardedAddresses;
	private String              remoteIPAddress;
	private Boolean             delegateAdmin              = Boolean.FALSE;
	private Boolean             enableAudit                = Boolean.TRUE;
	private Boolean             replaceExistingPermissions = Boolean.FALSE;
	private Boolean             isRecursive                = Boolean.FALSE;
	private String              clientIPAddress;
	private String              clientType;
	private String              requestData;
	private String              sessionId;
	private String              clusterName;
	private String              zoneName;
	private String 				ownerUser;

	public GrantRevokeRequest() {
		this(null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
	}

	public GrantRevokeRequest(String grantor, Set<String> grantorGroups, Map<String, String> resource, Set<String> users,
							  Set<String> groups, Set<String> accessTypes, Boolean delegateAdmin, Boolean enableAudit,
							  Boolean replaceExistingPermissions, Boolean isRecursive, String clientIPAddress,
							  String clientType, String requestData, String sessionId, String clusterName, String zoneName) {
		this(grantor, grantorGroups, resource, users, groups, null, accessTypes, delegateAdmin, enableAudit, replaceExistingPermissions, isRecursive, clientIPAddress, clientType, requestData, sessionId, clusterName, zoneName);
	}

	public GrantRevokeRequest(String grantor, Set<String> grantorGroups, Map<String, String> resource, Set<String> users,
							  Set<String> groups, Set<String> roles, Set<String> accessTypes, Boolean delegateAdmin, Boolean enableAudit,
							  Boolean replaceExistingPermissions, Boolean isRecursive, String clientIPAddress,
							  String clientType, String requestData, String sessionId, String clusterName, String zoneName) {
		this(grantor, grantorGroups, resource, users, groups, roles, accessTypes, delegateAdmin, enableAudit, replaceExistingPermissions, isRecursive, clientIPAddress, clientType, requestData, sessionId, clusterName, zoneName, null);
	}

	public GrantRevokeRequest(String grantor, Set<String> grantorGroups, Map<String, String> resource, Set<String> users,
							  Set<String> groups, Set<String> roles, Set<String> accessTypes, Boolean delegateAdmin, Boolean enableAudit,
							  Boolean replaceExistingPermissions, Boolean isRecursive, String clientIPAddress,
							  String clientType, String requestData, String sessionId, String clusterName, String zoneName, String ownerUser) {
		setGrantor(grantor);
		setGrantorGroups(grantorGroups);
		setResource(resource);
		setUsers(users);
		setGroups(groups);
		setRoles(roles);
		setAccessTypes(accessTypes);
		setDelegateAdmin(delegateAdmin);
		setEnableAudit(enableAudit);
		setReplaceExistingPermissions(replaceExistingPermissions);
		setIsRecursive(isRecursive);
		setClientIPAddress(clientIPAddress);
		setClientType(clientType);
		setRequestData(requestData);
		setSessionId(sessionId);
		setClusterName(clusterName);
		setZoneName(zoneName);
		setOwnerUser(ownerUser);
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
	 * @return the grantorGroups
	 */
	public Set<String> getGrantorGroups() {
		return grantorGroups;
	}

	/**
	 * @param grantorGroups the grantorGroups to set
	 */
	public void setGrantorGroups(Set<String> grantorGroups) {
		this.grantorGroups = grantorGroups == null ? new HashSet<String>() : grantorGroups;
	}
	/**
	 * @return the resource
	 */
	public Map<String, String> getResource() {
		return resource;
	}
	
	public void setForwardedAddresses(List<String> forwardedAddresses) {
		this.forwardedAddresses = (forwardedAddresses == null) ? new ArrayList<String>() : forwardedAddresses;
	}

	public void setRemoteIPAddress(String remoteIPAddress) {
		this.remoteIPAddress = remoteIPAddress;
	}


	/**
	 * @param resource the resource to set
	 */
	public void setResource(Map<String, String> resource) {
		this.resource = resource == null ? new HashMap<String, String>() : resource;
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
		this.groups = groups == null ? new HashSet<String>() : groups;
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
	 * @return the accessTypes
	 */
	public Set<String> getAccessTypes() {
		return accessTypes;
	}

	/**
	 * @param accessTypes the accessTypes to set
	 */
	public void setAccessTypes(Set<String> accessTypes) {
		this.accessTypes = accessTypes == null ? new HashSet<String>() : accessTypes;
	}

	/**
	 * @return the delegateAdmin
	 */
	public Boolean getDelegateAdmin() {
		return delegateAdmin;
	}

	/**
	 * @param delegateAdmin the delegateAdmin to set
	 */
	public void setDelegateAdmin(Boolean delegateAdmin) {
		this.delegateAdmin = delegateAdmin == null ? Boolean.FALSE : delegateAdmin;
	}

	/**
	 * @return the enableAudit
	 */
	public Boolean getEnableAudit() {
		return enableAudit;
	}

	/**
	 * @param enableAudit the enableAudit to set
	 */
	public void setEnableAudit(Boolean enableAudit) {
		this.enableAudit = enableAudit == null ? Boolean.TRUE : enableAudit;
	}

	/**
	 * @return the ownerUser
	 */
	public String getOwnerUser() {
		return ownerUser;
	}

	/**
	 * @param ownerUser the ownerUser to set
	 */
	public void setOwnerUser(String ownerUser) {
		this.ownerUser = ownerUser;
	}

	/**
	 * @return the replaceExistingPermissions
	 */
	public Boolean getReplaceExistingPermissions() {
		return replaceExistingPermissions;
	}

	/**
	 * @param replaceExistingPermissions the replaceExistingPermissions to set
	 */
	public void setReplaceExistingPermissions(Boolean replaceExistingPermissions) {
		this.replaceExistingPermissions = replaceExistingPermissions == null ? Boolean.FALSE : replaceExistingPermissions;
	}

	/**
	 * @return the isRecursive
	 */
	public Boolean getIsRecursive() {
		return isRecursive;
	}

	/**
	 * @param isRecursive the isRecursive to set
	 */
	public void setIsRecursive(Boolean isRecursive) {
		this.isRecursive = isRecursive == null ? Boolean.FALSE : isRecursive;
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
	
	public String getRemoteIPAddress() {
		return remoteIPAddress;
	}

	public List<String> getForwardedAddresses() { 
		return forwardedAddresses; 
	}

	/**
	 * @param clusterName the clusterName to set
	 */
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	/**
	 * @return the clusterName
	 */
	public String getZoneName() {
		return zoneName;
	}

	/**
	 * @param zoneName the clusterName to set
	 */
	public void setZoneName(String zoneName) {
		this.zoneName = zoneName;
	}

	@Override
	public String toString( ) {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("GrantRevokeRequest={");

		sb.append("grantor={").append(grantor).append("} ");

		sb.append("grantorGroups={");
		if(grantorGroups != null) {
			for(String grantorGroup : grantorGroups) {
				sb.append(grantorGroup).append(" ");
			}
		}
		sb.append("} ");

		sb.append("resource={");
		if(resource != null) {
			for(Map.Entry<String, String> e : resource.entrySet()) {
				sb.append(e.getKey()).append("=").append(e.getValue()).append("; ");
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

		sb.append("accessTypes={");
		if(accessTypes != null) {
			for(String accessType : accessTypes) {
				sb.append(accessType).append(" ");
			}
		}
		sb.append("} ");

		sb.append("delegateAdmin={").append(delegateAdmin).append("} ");
		sb.append("enableAudit={").append(enableAudit).append("} ");
		sb.append("replaceExistingPermissions={").append(replaceExistingPermissions).append("} ");
		sb.append("isRecursive={").append(isRecursive).append("} ");
		sb.append("clientIPAddress={").append(clientIPAddress).append("} ");
		sb.append("clientType={").append(clientType).append("} ");
		sb.append("requestData={").append(requestData).append("} ");
		sb.append("sessionId={").append(sessionId).append("} ");
		sb.append("clusterName={").append(clusterName).append("} ");
		sb.append("zoneName={").append(zoneName).append("} ");

		sb.append("}");

		return sb;
	}
}
