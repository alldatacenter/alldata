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

package org.apache.ranger.services.tag;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.service.RangerBaseService;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.plugin.store.TagStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

import static org.apache.ranger.plugin.policyengine.RangerPolicyEngine.GROUP_PUBLIC;

public class RangerServiceTag extends RangerBaseService {

	private static final Logger LOG = LoggerFactory.getLogger(RangerServiceTag.class);

	public static final String TAG_RESOURCE_NAME = "tag";
	public static final String RANGER_TAG_NAME_EXPIRES_ON = "EXPIRES_ON";
	public static final String RANGER_TAG_EXPIRY_CONDITION_NAME = "accessed-after-expiry";

	private TagStore tagStore;

	public RangerServiceTag() {
		super();
	}

	@Override
	public void init(RangerServiceDef serviceDef, RangerService service) {
		super.init(serviceDef, service);
	}

	public void setTagStore(TagStore tagStore) {
		this.tagStore = tagStore;
	}

	@Override
	public Map<String,Object> validateConfig() throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceTag.validateConfig(" + serviceName + " )");
		}

		Map<String, Object> ret = new HashMap<>();

		ret.put("connectivityStatus", true);

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceTag.validateConfig(" + serviceName + " ): " + ret);
		}

		return ret;
	}

	@Override
	public List<String> lookupResource(ResourceLookupContext context) throws Exception {
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceTag.lookupResource(" + context + ")");
		}

		List<String> ret = new ArrayList<>();

		if (context != null && StringUtils.equals(context.getResourceName(), TAG_RESOURCE_NAME)) {
			try {
				List<String> tags = tagStore != null ? tagStore.getTagTypes() : null;

				if(CollectionUtils.isNotEmpty(tags)) {
					List<String> valuesToExclude = MapUtils.isNotEmpty(context.getResources()) ? context.getResources().get(TAG_RESOURCE_NAME) : null;

					if(CollectionUtils.isNotEmpty(valuesToExclude)) {
						tags.removeAll(valuesToExclude);
					}

					String valueToMatch = context.getUserInput();

					if(StringUtils.isNotEmpty(valueToMatch)) {
						if(! valueToMatch.endsWith("*")) {
							valueToMatch += "*";
						}

						for (String tag : tags) {
							if(FilenameUtils.wildcardMatch(tag, valueToMatch)) {
								ret.add(tag);
							}
						}
					}
				}
			} catch (Exception excp) {
				LOG.error("RangerServiceTag.lookupResource()", excp);
			}
		}

		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceTag.lookupResource(): tag count=" + ret.size());
		}

		return ret;
	}

	@Override
	public List<RangerPolicy> getDefaultRangerPolicies() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceTag.getDefaultRangerPolicies() ");
		}

		List<RangerPolicy> ret = new ArrayList<RangerPolicy>();

		boolean isConditionDefFound = false;

		List<RangerServiceDef.RangerPolicyConditionDef> policyConditionDefs = serviceDef.getPolicyConditions();

		if (CollectionUtils.isNotEmpty(policyConditionDefs)) {
			for (RangerServiceDef.RangerPolicyConditionDef conditionDef : policyConditionDefs) {
				if (conditionDef.getName().equals(RANGER_TAG_EXPIRY_CONDITION_NAME)) {
					isConditionDefFound = true;
					break;
				}
			}
		}

		if (isConditionDefFound) {

			ret = super.getDefaultRangerPolicies();
			String tagResourceName = null;
			if (!serviceDef.getResources().isEmpty()) {
				tagResourceName = serviceDef.getResources().get(0).getName();

				for (RangerPolicy defaultPolicy : ret) {

					RangerPolicy.RangerPolicyResource tagPolicyResource = defaultPolicy.getResources().get(tagResourceName);

					if (tagPolicyResource != null) {

						String value = RANGER_TAG_NAME_EXPIRES_ON;

						tagPolicyResource.setValue(value);
						defaultPolicy.setName(value);
						defaultPolicy.setDescription("Policy for data with " + value + " tag");

						List<RangerPolicy.RangerPolicyItem> defaultPolicyItems = defaultPolicy.getPolicyItems();

						for (RangerPolicy.RangerPolicyItem defaultPolicyItem : defaultPolicyItems) {

							List<String> groups = new ArrayList<String>();
							groups.add(GROUP_PUBLIC);
							defaultPolicyItem.setGroups(groups);

							List<RangerPolicy.RangerPolicyItemCondition> policyItemConditions = new ArrayList<RangerPolicy.RangerPolicyItemCondition>();
							List<String> values = new ArrayList<String>();
							values.add("yes");
							RangerPolicy.RangerPolicyItemCondition policyItemCondition = new RangerPolicy.RangerPolicyItemCondition(RANGER_TAG_EXPIRY_CONDITION_NAME, values);
							policyItemConditions.add(policyItemCondition);

							defaultPolicyItem.setConditions(policyItemConditions);
							defaultPolicyItem.setDelegateAdmin(Boolean.FALSE);
						}

						defaultPolicy.setDenyPolicyItems(defaultPolicyItems);
						defaultPolicy.setPolicyItems(null);
					}
				}
			}
		} else {
			LOG.error("RangerServiceTag.getDefaultRangerPolicies() - Cannot create default TAG policy: Cannot get tagPolicyConditionDef with name=" + RANGER_TAG_EXPIRY_CONDITION_NAME);
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceTag.getDefaultRangerPolicies() : " + ret);
		}
		return ret;
	}
}
