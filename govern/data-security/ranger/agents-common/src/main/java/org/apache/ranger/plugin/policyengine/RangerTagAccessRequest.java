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


import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.contextenricher.RangerTagForEval;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;

import java.util.Map;

public class RangerTagAccessRequest extends RangerAccessRequestImpl {
	private final RangerPolicyResourceMatcher.MatchType matchType;
	public RangerTagAccessRequest(RangerTagForEval resourceTag, RangerServiceDef tagServiceDef, RangerAccessRequest request) {
		String owner = request.getResource() != null ? request.getResource().getOwnerUser() : null;

		matchType = resourceTag.getMatchType();

		super.setResource(new RangerTagResource(resourceTag.getType(), tagServiceDef, owner));
		super.setUser(request.getUser());
		super.setUserGroups(request.getUserGroups());
		super.setUserRoles(request.getUserRoles());
		super.setAction(request.getAction());
		super.setAccessType(request.getAccessType());
		super.setAccessTime(request.getAccessTime());
		super.setRequestData(request.getRequestData());

		Map<String, Object> requestContext = request.getContext();

		RangerAccessRequestUtil.setCurrentTagInContext(request.getContext(), resourceTag);
		RangerAccessRequestUtil.setCurrentResourceInContext(request.getContext(), request.getResource());
		RangerAccessRequestUtil.setCurrentUserInContext(request.getContext(), request.getUser());

		if (StringUtils.isNotEmpty(owner)) {
			RangerAccessRequestUtil.setOwnerInContext(request.getContext(), owner);
		}
		super.setContext(requestContext);

		super.setClientType(request.getClientType());
		super.setClientIPAddress(request.getClientIPAddress());
		super.setRemoteIPAddress(request.getRemoteIPAddress());
		super.setForwardedAddresses(request.getForwardedAddresses());
		super.setSessionId(request.getSessionId());
		super.setResourceMatchingScope(request.getResourceMatchingScope());
	}
	public RangerPolicyResourceMatcher.MatchType getMatchType() {
		return matchType;
	}
}
