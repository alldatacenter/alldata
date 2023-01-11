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

package org.apache.ranger.services.yarn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ranger.authorization.yarn.authorizer.RangerYarnAuthorizer;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.resourcematcher.RangerAbstractResourceMatcher;
import org.apache.ranger.plugin.service.RangerBaseService;
import org.apache.ranger.plugin.service.ResourceLookupContext;
import org.apache.ranger.services.yarn.client.YarnResourceMgr;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RangerServiceYarn extends RangerBaseService {

	private static final Logger LOG = LoggerFactory.getLogger(RangerServiceYarn.class);
	public static final String ACCESS_TYPE_SUBMIT_APP  = "submit-app";
	
	public RangerServiceYarn() {
		super();
	}
	
	@Override
	public void init(RangerServiceDef serviceDef, RangerService service) {
		super.init(serviceDef, service);
	}

	@Override
	public Map<String,Object> validateConfig() throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		String 	serviceName  	    = getServiceName();
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceYarn.validateConfig Service: (" + serviceName + " )");
		}
		if ( configs != null) {
			try  {
				ret = YarnResourceMgr.validateConfig(serviceName, configs);
			} catch (Exception e) {
				LOG.error("<== RangerServiceYarn.validateConfig Error:" + e);
				throw e;
			}
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceYarn.validateConfig Response : (" + ret + " )");
		}
		return ret;
	}

	@Override
	public List<String> lookupResource(ResourceLookupContext context) throws Exception {
		
		List<String> ret 		   = new ArrayList<String>();
		String 	serviceName  	   = getServiceName();
		Map<String,String> configs = getConfigs();
		if(LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceYarn.lookupResource Context: (" + context + ")");
		}
		if (context != null) {
			try {
				ret  = YarnResourceMgr.getYarnResources(serviceName,configs,context);
			} catch (Exception e) {
			  LOG.error( "<==RangerServiceYarn.lookupResource Error : " + e);
			  throw e;
			}
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceYarn.lookupResource Response: (" + ret + ")");
		}
		return ret;
	}

	public List<RangerPolicy> getDefaultRangerPolicies() throws Exception {
		if (LOG.isDebugEnabled()) {
			LOG.debug("==> RangerServiceYarn.getDefaultRangerPolicies() ");
		}

		List<RangerPolicy> ret = super.getDefaultRangerPolicies();

		String queueResourceName = RangerYarnAuthorizer.KEY_RESOURCE_QUEUE;

		for (RangerPolicy defaultPolicy : ret) {
			if(defaultPolicy.getName().contains("all")){
				RangerPolicy.RangerPolicyResource queuePolicyResource = defaultPolicy.getResources().get(queueResourceName);

				if (StringUtils.isNotBlank(lookUpUser)) {
					RangerPolicyItem policyItemForLookupUser = new RangerPolicyItem();
					policyItemForLookupUser.setUsers(Collections.singletonList(lookUpUser));
					policyItemForLookupUser.setAccesses(Collections.singletonList(new RangerPolicyItemAccess(ACCESS_TYPE_SUBMIT_APP)));
					policyItemForLookupUser.setDelegateAdmin(false);
					defaultPolicy.getPolicyItems().add(policyItemForLookupUser);
				}

				if (queuePolicyResource != null) {
					List<RangerServiceDef.RangerResourceDef> resourceDefs = serviceDef.getResources();
					RangerServiceDef.RangerResourceDef queueResourceDef = null;
					for (RangerServiceDef.RangerResourceDef resourceDef : resourceDefs) {
						if (resourceDef.getName().equals(queueResourceName)) {
							queueResourceDef = resourceDef;
							break;
						}
					}
					if (queueResourceDef != null) {
						queuePolicyResource.setValue(RangerAbstractResourceMatcher.WILDCARD_ASTERISK);
					} else {
						LOG.warn("No resourceDef found in YARN service-definition for '" + queueResourceName + "'");
					}
				} else {
					LOG.warn("No '" + queueResourceName + "' found in default policy");
				}
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("<== RangerServiceYarn.getDefaultRangerPolicies() : " + ret);
		}
		return ret;
	}
}

