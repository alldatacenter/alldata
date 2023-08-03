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

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class RangerAccessRequestReadOnly implements RangerAccessRequest {
	private final RangerAccessRequest source;

	// Cached here for reducing access overhead
	private final RangerAccessResource resource;
	private final Set<String> userGroups;
	private final Set<String> userRoles;
	private final List<String> forwardedAddresses;
	private final Map<String, Object> context;

	RangerAccessRequestReadOnly(final RangerAccessRequest source) {
		this.source = source;
		this.resource = source.getResource().getReadOnlyCopy();
		this.userGroups = Collections.unmodifiableSet(source.getUserGroups());
		this.userRoles = Collections.unmodifiableSet(source.getUserRoles());
		this.context = Collections.unmodifiableMap(source.getContext());
		this.forwardedAddresses = Collections.unmodifiableList(source.getForwardedAddresses());
	}

	@Override
	public RangerAccessResource getResource() { return resource; }

	@Override
	public String getAccessType() { return source.getAccessType(); }

	@Override
	public boolean isAccessTypeAny() { return source.isAccessTypeAny(); }

	@Override
	public boolean isAccessTypeDelegatedAdmin() { return source.isAccessTypeDelegatedAdmin(); }

	@Override
	public String getUser() { return source.getUser(); }

	@Override
	public Set<String> getUserGroups() { return userGroups; }

	@Override
	public Set<String> getUserRoles() { return userRoles; }

	@Override
	public Date getAccessTime() { return source.getAccessTime(); }

	@Override
	public String getClientIPAddress() { return source.getClientIPAddress(); }

	@Override
	public String getRemoteIPAddress() { return source.getRemoteIPAddress(); }

	@Override
	public List<String> getForwardedAddresses() { return forwardedAddresses; }

	@Override
	public String getClientType() { return source.getClientType(); }

	@Override
	public String getAction() { return source.getAction(); }

	@Override
	public String getRequestData() { return source.getRequestData(); }

	@Override
	public String getSessionId() { return source.getSessionId(); }

	@Override
	public Map<String, Object> getContext() { return context; }

	@Override
	public RangerAccessRequest getReadOnlyCopy() { return this; }

	@Override
	public ResourceMatchingScope getResourceMatchingScope() { return source.getResourceMatchingScope(); }

	@Override
	public String getClusterName() { return source.getClusterName();	}

	@Override
	public String getClusterType() {  return source.getClusterType();	}

}
