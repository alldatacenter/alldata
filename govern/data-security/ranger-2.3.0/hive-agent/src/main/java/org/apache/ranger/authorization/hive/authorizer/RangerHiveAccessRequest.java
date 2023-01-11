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

package org.apache.ranger.authorization.hive.authorizer;

import java.util.Date;
import java.util.Set;

import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzSessionContext;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveOperationType;
import org.apache.hadoop.hive.ql.security.authorization.plugin.HiveAuthzContext;
import org.apache.ranger.plugin.policyengine.RangerAccessRequestImpl;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;

public class RangerHiveAccessRequest extends RangerAccessRequestImpl {
	private HiveAccessType accessType = HiveAccessType.NONE;

	public RangerHiveAccessRequest() {
		super();
	}

	public RangerHiveAccessRequest(RangerHiveResource      resource,
								   String                  user,
								   Set<String>             userGroups,
								   Set<String>             userRoles,
								   String                  hiveOpTypeName,
								   HiveAccessType          accessType,
								   HiveAuthzContext        context,
								   HiveAuthzSessionContext sessionContext) {
		this.setResource(resource);
		this.setUser(user);
		this.setUserGroups(userGroups);
		this.setUserRoles(userRoles);
		this.setAccessTime(new Date());
		this.setAction(hiveOpTypeName);
		this.setHiveAccessType(accessType);

		if(context != null) {
			this.setRequestData(context.getCommandString());
			this.setForwardedAddresses(context.getForwardedAddresses());
			this.setRemoteIPAddress(context.getIpAddress());
		}

		if(sessionContext != null) {
			this.setClientType(sessionContext.getClientType() == null ? null : sessionContext.getClientType().toString());
			this.setSessionId(sessionContext.getSessionString());
		}
		
	}

	public RangerHiveAccessRequest(RangerHiveResource      resource,
			   String                  user,
			   Set<String>             userGroups,
			   Set<String>             userRoles,
			   HiveOperationType       hiveOpType,
			   HiveAccessType          accessType,
			   HiveAuthzContext        context,
			   HiveAuthzSessionContext sessionContext) {
		this(resource, user, userGroups, userRoles, hiveOpType.name(), accessType, context, sessionContext);
	}

	public RangerHiveAccessRequest(RangerHiveResource resource, String user, Set<String> groups, Set<String> roles, HiveAuthzContext context, HiveAuthzSessionContext sessionContext) {
		this(resource, user, groups, roles, "METADATA OPERATION", HiveAccessType.USE, context, sessionContext);
	}

	public HiveAccessType getHiveAccessType() {
		return accessType;
	}

	public void setHiveAccessType(HiveAccessType accessType) {
		this.accessType = accessType;

		if(accessType == HiveAccessType.USE) {
			this.setAccessType(RangerPolicyEngine.ANY_ACCESS);
		} else {
			this.setAccessType(accessType.name().toLowerCase());
		}
	}

	public RangerHiveAccessRequest copy() {
		RangerHiveAccessRequest ret = new RangerHiveAccessRequest();

		ret.setResource(getResource());
		ret.setAccessType(getAccessType());
		ret.setUser(getUser());
		ret.setUserGroups(getUserGroups());
		ret.setUserRoles(getUserRoles());
		ret.setAccessTime(getAccessTime());
		ret.setAction(getAction());
		ret.setClientIPAddress(getClientIPAddress());
		ret.setRemoteIPAddress(getRemoteIPAddress());
		ret.setForwardedAddresses(getForwardedAddresses());
		ret.setRequestData(getRequestData());
		ret.setClientType(getClientType());
		ret.setSessionId(getSessionId());
		ret.setContext(RangerAccessRequestUtil.copyContext(getContext()));
		ret.accessType = accessType;
		ret.setClusterName(getClusterName());
		ret.setClusterType(getClusterType());

		return ret;
	}
}
