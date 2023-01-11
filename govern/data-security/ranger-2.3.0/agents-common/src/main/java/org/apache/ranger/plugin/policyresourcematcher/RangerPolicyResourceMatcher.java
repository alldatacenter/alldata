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

package org.apache.ranger.plugin.policyresourcematcher;

import java.util.Map;

import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefHelper;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.resourcematcher.RangerResourceMatcher;

public interface RangerPolicyResourceMatcher {
	enum MatchScope { SELF, SELF_OR_DESCENDANT, SELF_OR_ANCESTOR, DESCENDANT, ANCESTOR, ANY, SELF_AND_ALL_DESCENDANTS}
	enum MatchType { NONE, SELF, DESCENDANT, ANCESTOR, SELF_AND_ALL_DESCENDANTS}

	void setServiceDef(RangerServiceDef serviceDef);

	void setPolicy(RangerPolicy policy);

	void setPolicyResources(Map<String, RangerPolicyResource> policyResources);

	void setPolicyResources(Map<String, RangerPolicyResource> policyResources, int policyType);

	void setServiceDefHelper(RangerServiceDefHelper serviceDefHelper);

	void init();

	RangerServiceDef getServiceDef();

	RangerResourceMatcher getResourceMatcher(String resourceName);

	boolean isMatch(RangerAccessResource resource, Map<String, Object> evalContext);

	boolean isMatch(Map<String, RangerPolicyResource> resources, Map<String, Object> evalContext);

	boolean isMatch(RangerAccessResource resource, MatchScope scope, Map<String, Object> evalContext);

	boolean isMatch(RangerPolicy policy, MatchScope scope, Map<String, Object> evalContext);

	MatchType getMatchType(RangerAccessResource resource, Map<String, Object> evalContext);

	boolean isCompleteMatch(RangerAccessResource resource, Map<String, Object> evalContext);

	boolean isCompleteMatch(Map<String, RangerPolicyResource> resources, Map<String, Object> evalContext);

	boolean getNeedsDynamicEval();

	StringBuilder toString(StringBuilder sb);
}
