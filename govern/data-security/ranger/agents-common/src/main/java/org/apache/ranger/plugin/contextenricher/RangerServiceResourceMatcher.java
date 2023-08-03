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

package org.apache.ranger.plugin.contextenricher;

import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceResource;
import org.apache.ranger.plugin.policyengine.RangerAccessResource;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.policyresourcematcher.RangerResourceEvaluator;
import org.apache.ranger.plugin.resourcematcher.RangerResourceMatcher;
import org.apache.ranger.plugin.util.ServiceDefUtil;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;

public class RangerServiceResourceMatcher implements RangerResourceEvaluator {
	public static final Comparator<RangerServiceResourceMatcher> ID_COMPARATOR = new IdComparator();

	private final RangerServiceResource       serviceResource;
	private final RangerPolicyResourceMatcher policyResourceMatcher;
	private RangerServiceDef.RangerResourceDef leafResourceDef;

	public RangerServiceResourceMatcher(final RangerServiceResource serviceResource, RangerPolicyResourceMatcher policyResourceMatcher) {
		this.serviceResource       = serviceResource;
		this.policyResourceMatcher = policyResourceMatcher;
		this.leafResourceDef   = ServiceDefUtil.getLeafResourceDef(policyResourceMatcher.getServiceDef(), getPolicyResource());
	}

	public RangerServiceResource getServiceResource() { return serviceResource; }

	@Override
	public long getId() {
		return serviceResource != null ? serviceResource.getId() :-1;
	}

	@Override
	public RangerPolicyResourceMatcher getPolicyResourceMatcher() { return policyResourceMatcher; }

	@Override
	public Map<String, RangerPolicy.RangerPolicyResource> getPolicyResource() {
		return serviceResource != null ? serviceResource.getResourceElements() : null;
	}

	@Override
	public RangerResourceMatcher getResourceMatcher(String resourceName) {
		return policyResourceMatcher != null ? policyResourceMatcher.getResourceMatcher(resourceName) : null;
	}

	@Override
	public boolean isAncestorOf(RangerServiceDef.RangerResourceDef resourceDef) {
		return ServiceDefUtil.isAncestorOf(policyResourceMatcher.getServiceDef(), leafResourceDef, resourceDef);
	}

	public RangerPolicyResourceMatcher.MatchType getMatchType(RangerAccessResource requestedResource, Map<String, Object> evalContext) {
		return policyResourceMatcher != null ?  policyResourceMatcher.getMatchType(requestedResource, evalContext) : RangerPolicyResourceMatcher.MatchType.NONE;
	}

	static class IdComparator implements Comparator<RangerServiceResourceMatcher>, Serializable {
		@Override
		public int compare(RangerServiceResourceMatcher me, RangerServiceResourceMatcher other) {
			return Long.compare(me.getId(), other.getId());
		}
	}

	@Override
	public String toString() {
		return String.valueOf(getId());
	}
}
