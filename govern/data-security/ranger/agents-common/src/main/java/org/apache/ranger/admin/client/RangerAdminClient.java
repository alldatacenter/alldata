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

 package org.apache.ranger.admin.client;


import org.apache.hadoop.conf.Configuration;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.plugin.util.GrantRevokeRequest;
import org.apache.ranger.plugin.util.GrantRevokeRoleRequest;
import org.apache.ranger.plugin.util.RangerRoles;
import org.apache.ranger.plugin.util.ServicePolicies;
import org.apache.ranger.plugin.util.ServiceTags;
import org.apache.ranger.plugin.util.RangerUserStore;

import java.util.List;


public interface RangerAdminClient {

	void init(String serviceName, String appId, String configPropertyPrefix, Configuration config);

	ServicePolicies getServicePoliciesIfUpdated(long lastKnownVersion, long lastActivationTimeInMillis) throws Exception;

	RangerRoles getRolesIfUpdated(long lastKnownRoleVersion, long lastActivationTimeInMills) throws Exception;

	RangerRole createRole(RangerRole request) throws Exception;

	void dropRole(String execUser, String roleName) throws Exception;

	List<String> getAllRoles(String execUser) throws Exception;

	List<String> getUserRoles(String execUser) throws Exception;

	RangerRole getRole(String execUser, String roleName) throws Exception;

	void grantRole(GrantRevokeRoleRequest request) throws Exception;

	void revokeRole(GrantRevokeRoleRequest request) throws Exception;

	void grantAccess(GrantRevokeRequest request) throws Exception;

	void revokeAccess(GrantRevokeRequest request) throws Exception;

	ServiceTags getServiceTagsIfUpdated(long lastKnownVersion, long lastActivationTimeInMillis) throws Exception;

	List<String> getTagTypes(String tagTypePattern) throws Exception;

	RangerUserStore getUserStoreIfUpdated(long lastKnownUserStoreVersion, long lastActivationTimeInMillis) throws Exception;

}
