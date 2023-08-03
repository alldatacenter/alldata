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

package org.apache.ranger.plugin.policyengine;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerAccessRequestImpl implements RangerAccessRequest {
	private static final Logger LOG = LoggerFactory.getLogger(RangerAccessRequestImpl.class);

	private RangerAccessResource resource;
	private String               accessType;
	private String               user;
	private Set<String>          userGroups;
	private Set<String>          userRoles;
	private Date                 accessTime;
	private String               clientIPAddress;
	private List<String>         forwardedAddresses;
	private String               remoteIPAddress;
	private String               clientType;
	private String               action;
	private String               requestData;
	private String               sessionId;
	private Map<String, Object>  context;
	private String				 clusterName;
	private String				 clusterType;

	private boolean isAccessTypeAny;
	private boolean isAccessTypeDelegatedAdmin;
	private ResourceMatchingScope resourceMatchingScope = ResourceMatchingScope.SELF;

	public RangerAccessRequestImpl() {
		this(null, null, null, null, null);
	}

	public RangerAccessRequestImpl(RangerAccessResource resource, String accessType, String user, Set<String> userGroups, Set<String> userRoles) {
		setResource(resource);
		setAccessType(accessType);
		setUser(user);
		setUserGroups(userGroups);
		setUserRoles(userRoles);
		setForwardedAddresses(null);

		// set remaining fields to default value
		setAccessTime(null);
		setRemoteIPAddress(null);
		setClientType(null);
		setAction(null);
		setRequestData(null);
		setSessionId(null);
		setContext(null);
		setClusterName(null);
	}

	public RangerAccessRequestImpl(RangerAccessRequest request) {
		setResource(request.getResource());
		setAccessType(request.getAccessType());
		setUser(request.getUser());
		setUserGroups(request.getUserGroups());
		setUserRoles(request.getUserRoles());
		setForwardedAddresses(request.getForwardedAddresses());
		setAccessTime(request.getAccessTime());
		setRemoteIPAddress(request.getRemoteIPAddress());
		setClientType(request.getClientType());
		setAction(request.getAction());
		setRequestData(request.getRequestData());
		setSessionId(request.getSessionId());
		setContext(request.getContext());
		setClusterName(request.getClusterName());
		setResourceMatchingScope(request.getResourceMatchingScope());
		setClientIPAddress(request.getClientIPAddress());
		setClusterType(request.getClusterType());
	}

	@Override
	public RangerAccessResource getResource() {
		return resource;
	}

	@Override
	public String getAccessType() {
		return accessType;
	}

	@Override
	public String getUser() {
		return user;
	}

	@Override
	public Set<String> getUserGroups() {
		return userGroups;
	}

	@Override
	public Set<String> getUserRoles() {
		return userRoles;
	}

	@Override
	public Date getAccessTime() {
		return accessTime;
	}

	@Override
	public String getClientIPAddress() { return clientIPAddress;}

	@Override
	public String getRemoteIPAddress() {
		return remoteIPAddress;
	}

	@Override
	public List<String> getForwardedAddresses() { return forwardedAddresses; }

	@Override
	public String getClientType() {
		return clientType;
	}

	@Override
	public String getAction() {
		return action;
	}

	@Override
	public String getRequestData() {
		return requestData;
	}

	@Override
	public String getSessionId() {
		return sessionId;
	}

	@Override
	public Map<String, Object> getContext() {
		return context;
	}

	@Override
	public ResourceMatchingScope getResourceMatchingScope() {
		return resourceMatchingScope;
	}

	@Override
	public boolean isAccessTypeAny() {
		return isAccessTypeAny;
	}

	@Override
	public boolean isAccessTypeDelegatedAdmin() {
		return isAccessTypeDelegatedAdmin;
	}

	public void setResource(RangerAccessResource resource) {
		this.resource = resource;
	}

	public void setAccessType(String accessType) {
		if (StringUtils.isEmpty(accessType)) {
			accessType = RangerPolicyEngine.ANY_ACCESS;
		}

		this.accessType            = accessType;
		isAccessTypeAny            = StringUtils.equals(accessType, RangerPolicyEngine.ANY_ACCESS);
		isAccessTypeDelegatedAdmin = StringUtils.equals(accessType, RangerPolicyEngine.ADMIN_ACCESS);
	}

	public void setUser(String user) {
		this.user = user;
	}

	public void setUserGroups(Set<String> userGroups) {
		this.userGroups = (userGroups == null) ? new HashSet<String>() : userGroups;
	}

	public void setUserRoles(Set<String> userRoles) {
		this.userRoles = (userRoles == null) ? new HashSet<String>() : userRoles;
	}

	public void setAccessTime(Date accessTime) {
		this.accessTime = accessTime;
	}

	public void setClientIPAddress(String ipAddress) {
		this.clientIPAddress = ipAddress;
	}

	public void setForwardedAddresses(List<String> forwardedAddresses) {
		this.forwardedAddresses = (forwardedAddresses == null) ? new ArrayList<String>() : forwardedAddresses;
	}

	public void setRemoteIPAddress(String remoteIPAddress) {
		this.remoteIPAddress = remoteIPAddress;
	}

	public void setClientType(String clientType) {
		this.clientType = clientType;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public void setRequestData(String requestData) {
		this.requestData = requestData;
	}

	public void setSessionId(String sessionId) {
		this.sessionId = sessionId;
	}
	
	public String getClusterName() {
		return clusterName;
	}

	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	public String getClusterType() {
		return clusterType;
	}

	public void setClusterType(String clusterType) {
		this.clusterType = clusterType;
	}

	public void setResourceMatchingScope(ResourceMatchingScope scope) { this.resourceMatchingScope = scope; }

	public void setContext(Map<String, Object> context) {
		if (context == null) {
			this.context = new HashMap<>();
		} else {
			this.context = context;
		}

		RangerAccessRequest current = RangerAccessRequestUtil.getRequestFromContext(this.context);

		if (current == null) {
			RangerAccessRequestUtil.setRequestInContext(this);
		}
	}

	public void extractAndSetClientIPAddress(boolean useForwardedIPAddress, String[]trustedProxyAddresses) {
		String ip = getRemoteIPAddress();
		if (ip == null) {
			ip = getClientIPAddress();
		}

		String newIp = ip;

		if (useForwardedIPAddress) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("Using X-Forward-For...");
			}
			if (CollectionUtils.isNotEmpty(getForwardedAddresses())) {
				if (trustedProxyAddresses != null && trustedProxyAddresses.length > 0) {
					if (StringUtils.isNotEmpty(ip)) {
						for (String trustedProxyAddress : trustedProxyAddresses) {
							if (StringUtils.equals(ip, trustedProxyAddress)) {
								newIp = getForwardedAddresses().get(0);
								break;
							}
						}
					}
				} else {
					newIp = getForwardedAddresses().get(0);
				}
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("No X-Forwarded-For addresses in the access-request");
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Old Remote/Client IP Address=" + ip + ", new IP Address=" + newIp);
		}
		setClientIPAddress(newIp);
	}

	@Override
	public String toString( ) {
		StringBuilder sb = new StringBuilder();

		toString(sb);

		return sb.toString();
	}

	public StringBuilder toString(StringBuilder sb) {
		sb.append("RangerAccessRequestImpl={");

		sb.append("resource={").append(resource).append("} ");
		sb.append("accessType={").append(accessType).append("} ");
		sb.append("user={").append(user).append("} ");

		sb.append("userGroups={");
		if(userGroups != null) {
			for(String userGroup : userGroups) {
				sb.append(userGroup).append(" ");
			}
		}
		sb.append("} ");

		sb.append("userRoles={");
		if(userRoles != null) {
			for(String role : userRoles) {
				sb.append(role).append(" ");
			}
		}
		sb.append("} ");

		sb.append("accessTime={").append(accessTime).append("} ");
		sb.append("clientIPAddress={").append(getClientIPAddress()).append("} ");
		sb.append("forwardedAddresses={").append(StringUtils.join(forwardedAddresses, " ")).append("} ");
		sb.append("remoteIPAddress={").append(remoteIPAddress).append("} ");
		sb.append("clientType={").append(clientType).append("} ");
		sb.append("action={").append(action).append("} ");
		sb.append("requestData={").append(requestData).append("} ");
		sb.append("sessionId={").append(sessionId).append("} ");
		sb.append("resourceMatchingScope={").append(resourceMatchingScope).append("} ");
		sb.append("clusterName={").append(clusterName).append("} ");
		sb.append("clusterType={").append(clusterType).append("} ");

		sb.append("context={");
		if(context != null) {
			for(Map.Entry<String, Object> e : context.entrySet()) {
				Object val = e.getValue();

				if (!(val instanceof RangerAccessRequest)) { // to avoid recursive calls
					sb.append(e.getKey()).append("={").append(val).append("} ");
				}
			}
		}
		sb.append("} ");

		sb.append("}");

		return sb;
	}
	@Override
	public RangerAccessRequest getReadOnlyCopy() {
		return new RangerAccessRequestReadOnly(this);
	}
}
