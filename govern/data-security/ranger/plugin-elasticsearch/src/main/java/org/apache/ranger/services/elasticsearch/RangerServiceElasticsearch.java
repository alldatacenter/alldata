/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.services.elasticsearch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.slf4j.Logger;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.service.RangerBaseService;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.services.elasticsearch.client.ElasticsearchResourceMgr;
import org.slf4j.LoggerFactory;

public class RangerServiceElasticsearch extends RangerBaseService {

	private static final Logger LOG = LoggerFactory.getLogger(RangerServiceElasticsearch.class);
	public static final String ACCESS_TYPE_READ  = "read";

	public RangerServiceElasticsearch() {
		super();
	}

	@Override
	public void init(RangerServiceDef serviceDef, RangerService service) {
		super.init(serviceDef, service);
	}

	@Override
	public List<RangerPolicy> getDefaultRangerPolicies() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceElasticsearch.getDefaultRangerPolicies()");
		}

		List<RangerPolicy> ret = super.getDefaultRangerPolicies();
		for (RangerPolicy defaultPolicy : ret) {
			if (defaultPolicy.getName().contains("all") && StringUtils.isNotBlank(lookUpUser)) {
				List<RangerPolicyItemAccess> accessListForLookupUser = new ArrayList<RangerPolicyItemAccess>();
				accessListForLookupUser.add(new RangerPolicyItemAccess(ACCESS_TYPE_READ));
				RangerPolicyItem policyItemForLookupUser = new RangerPolicyItem();
				policyItemForLookupUser.setUsers(Collections.singletonList(lookUpUser));
				policyItemForLookupUser.setAccesses(accessListForLookupUser);
				policyItemForLookupUser.setDelegateAdmin(false);
				defaultPolicy.getPolicyItems().add(policyItemForLookupUser);
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceElasticsearch.getDefaultRangerPolicies()");
		}
		return ret;
	}

	@Override
	public Map<String, Object> validateConfig() throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		String serviceName = getServiceName();
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceElasticsearch.validateConfig() service: " + serviceName);
		}
		if (configs != null) {
			try {
				ret = ElasticsearchResourceMgr.validateConfig(serviceName, configs);
			} catch (Exception e) {
				LOG.error("<== RangerServiceElasticsearch.validateConfig() error: " + e);
				throw e;
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceElasticsearch.validateConfig() result: " + ret);
		}
		return ret;
	}

	@Override
	public List<String> lookupResource(ResourceLookupContext context) throws Exception {

		List<String> ret = new ArrayList<String>();
		String serviceName = getServiceName();
		Map<String, String> configs = getConfigs();
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceElasticsearch.lookupResource() context: " + context);
		}
		if (context != null) {
			try {
				ret = ElasticsearchResourceMgr.getElasticsearchResources(serviceName, configs, context);
			} catch (Exception e) {
				LOG.error("<==RangerServiceElasticsearch.lookupResource() error: " + e);
				throw e;
			}
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceElasticsearch.lookupResource() result: " + ret);
		}
		return ret;
	}
}
