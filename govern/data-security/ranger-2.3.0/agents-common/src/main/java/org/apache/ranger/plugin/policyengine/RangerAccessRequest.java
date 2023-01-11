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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RangerAccessRequest {
	String RANGER_ACCESS_REQUEST_SCOPE_STRING = "Scope";

	RangerAccessResource getResource();

	String getAccessType();

	boolean isAccessTypeAny();

	boolean isAccessTypeDelegatedAdmin();

	String getUser();

	Set<String> getUserGroups();

	Set<String> getUserRoles();

	Date getAccessTime();

	String getClientIPAddress();

	String getRemoteIPAddress();

	List<String> getForwardedAddresses();

	String getClientType();

	String getAction();

	String getRequestData();

	String getSessionId();
	
	String getClusterName();

	String getClusterType();

	Map<String, Object> getContext();

	RangerAccessRequest getReadOnlyCopy();

	ResourceMatchingScope getResourceMatchingScope();

	enum ResourceMatchingScope {SELF, SELF_OR_DESCENDANTS, SELF_OR_CHILD}
}
