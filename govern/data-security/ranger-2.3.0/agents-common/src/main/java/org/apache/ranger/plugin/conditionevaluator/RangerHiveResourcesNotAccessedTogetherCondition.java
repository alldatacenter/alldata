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

package org.apache.ranger.plugin.conditionevaluator;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.policyengine.RangerAccessRequest;
import org.apache.ranger.plugin.policyresourcematcher.RangerDefaultPolicyResourceMatcher;
import org.apache.ranger.plugin.policyresourcematcher.RangerPolicyResourceMatcher;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.RangerAccessRequestUtil;
import org.apache.ranger.plugin.util.RangerRequestedResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RangerHiveResourcesNotAccessedTogetherCondition extends RangerAbstractConditionEvaluator {
	private static final Logger LOG = LoggerFactory.getLogger(RangerHiveResourcesNotAccessedTogetherCondition.class);

	private List<RangerPolicyResourceMatcher> matchers = new ArrayList<>();
	private boolean isInitialized;

	@Override
	public void init() {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerHiveResourcesNotAccessedTogetherCondition.init(" + condition + ")");
		}

		super.init();

		if (serviceDef != null) {
			doInitialize();
		} else {
			LOG.error("RangerHiveResourcesNotAccessedTogetherCondition.init() - ServiceDef not set ... ERROR ..");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerHiveResourcesNotAccessedTogetherCondition.init(" + condition + ")");
		}
	}

	@Override
	public boolean isMatched(final RangerAccessRequest request) {
		boolean ret = true;

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerHiveResourcesNotAccessedTogetherCondition.isMatched(" + request + ")");
		}

		if (isInitialized && CollectionUtils.isNotEmpty(matchers)) {
			RangerRequestedResources resources = RangerAccessRequestUtil.getRequestedResourcesFromContext(request.getContext());

			ret = resources == null || resources.isMutuallyExcluded(matchers, request.getContext());
		} else {
			LOG.error("RangerHiveResourcesNotAccessedTogetherCondition.isMatched() - Enforcer is not initialized correctly, Mutual Exclusion will NOT be enforced");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerHiveResourcesNotAccessedTogetherCondition.isMatched(" + request + ")" + ", result=" + ret);
		}

		return ret;
	}

	private void doInitialize() {
		List<String> mutuallyExclusiveResources = condition.getValues();

		if (CollectionUtils.isNotEmpty(mutuallyExclusiveResources)) {
			initializeMatchers(mutuallyExclusiveResources);

			if (CollectionUtils.isEmpty(matchers)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("RangerHiveResourcesNotAccessedTogetherCondition.doInitialize() - Cannot create matchers from values in MutualExclustionEnforcer");
				}
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("RangerHiveResourcesNotAccessedTogetherCondition.doInitialize() - Created " + matchers.size() + " matchers from values in MutualExclustionEnforcer");
				}
			}
		} else {
			if (LOG.isDebugEnabled()) {
				LOG.debug("RangerHiveResourcesNotAccessedTogetherCondition.doInitialize() - No values in MutualExclustionEnforcer");
			}
		}

		isInitialized = true;
	}

	private void initializeMatchers(List<String> mutuallyExclusiveResources) {

		for (String s : mutuallyExclusiveResources) {

			String policyResourceSpec = s.trim();

			RangerPolicyResourceMatcher matcher = buildMatcher(policyResourceSpec);

			if (matcher != null) {
				matchers.add(matcher);
			}
		}
	}

	private RangerPolicyResourceMatcher buildMatcher(String policyResourceSpec) {

		RangerPolicyResourceMatcher matcher = null;

		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerHiveResourcesNotAccessedTogetherCondition.buildMatcher(" + policyResourceSpec + ")");
		}

		// Works only for Hive serviceDef for now
		if (serviceDef != null && EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_HIVE_NAME.equals(serviceDef.getName())) {
			//Parse policyResourceSpec
			char separator = '.';
			String any = "*";

			Map<String, RangerPolicy.RangerPolicyResource> policyResources = new HashMap<>();

			String[] elements = StringUtils.split(policyResourceSpec, separator);

			RangerPolicy.RangerPolicyResource policyResource;

			if (elements.length > 0 && elements.length < 4) {
				if (elements.length == 3) {
					policyResource = new RangerPolicy.RangerPolicyResource(elements[2]);
				} else {
					policyResource = new RangerPolicy.RangerPolicyResource(any);
				}
				policyResources.put("column", policyResource);

				if (elements.length >= 2) {
					policyResource = new RangerPolicy.RangerPolicyResource(elements[1]);
				} else {
					policyResource = new RangerPolicy.RangerPolicyResource(any);
				}
				policyResources.put("table", policyResource);

				policyResource = new RangerPolicy.RangerPolicyResource(elements[0]);
				policyResources.put("database", policyResource);

				matcher = new RangerDefaultPolicyResourceMatcher();
				matcher.setPolicyResources(policyResources);
				matcher.setServiceDef(serviceDef);
				matcher.init();

			} else {
				LOG.error("RangerHiveResourcesNotAccessedTogetherCondition.buildMatcher() - Incorrect elements in the hierarchy specified ("
						+ elements.length + ")");
			}
		} else {
			LOG.error("RangerHiveResourcesNotAccessedTogetherCondition.buildMatcher() - ServiceDef not set or ServiceDef is not for Hive");
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerHiveResourcesNotAccessedTogetherCondition.buildMatcher(" + policyResourceSpec + ")" + ", matcher=" + matcher);
		}

		return matcher;
	}
}
