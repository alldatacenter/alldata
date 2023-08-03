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

package org.apache.ranger.patch;

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.policyengine.RangerPolicyEngine;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class PatchForOzoneDefaultPoliciesUpdate_J10044 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchForOzoneDefaultPoliciesUpdate_J10044.class);
	public static final String ACCESS_TYPE_ALL  = "all";

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	ServiceDBStore svcDBStore;

	public static void main(String[] args) {
		logger.info("main()");
		try {
			PatchForOzoneDefaultPoliciesUpdate_J10044 loader = (PatchForOzoneDefaultPoliciesUpdate_J10044) CLIUtil.getBean(PatchForOzoneDefaultPoliciesUpdate_J10044.class);
			loader.init();
			while (loader.isMoreToProcess()) {
				loader.load();
			}
			logger.info("Load complete. Exiting.");
			System.exit(0);
		} catch (Exception e) {
			logger.error("Error loading", e);
			System.exit(1);
		}
	}

	@Override
	public void printStats() {
		logger.info("PatchForOzoneDefaultPoliciesUpdate data ");
	}

	@Override
	public void execLoad() {
		logger.info("==> PatchForOzoneDefaultPoliciesUpdate.execLoad()");
		try {
			if (!updateOzoneDefaultPolicies()) {
				logger.error("Failed to apply the patch.");
				System.exit(1);
			}
		} catch (Exception e) {
			logger.error("Error while updateOzoneDefaultPolicies()data.", e);
			System.exit(1);
		}
		logger.info("<== PatchForOzoneDefaultPoliciesUpdate.execLoad()");
	}

	@Override
	public void init() throws Exception {
		// Do Nothing
	}

	private boolean updateOzoneDefaultPolicies() throws Exception {
		RangerServiceDef embeddedOzoneServiceDef;

		embeddedOzoneServiceDef = EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_OZONE_NAME);

		if (embeddedOzoneServiceDef != null) {
			List<XXService> dbServices = daoMgr.getXXService().findByServiceDefId(embeddedOzoneServiceDef.getId());
			if (CollectionUtils.isNotEmpty(dbServices)) {
				for(XXService dbService : dbServices) {
					SearchFilter filter = new SearchFilter();
					filter.setParam(SearchFilter.SERVICE_NAME, dbService.getName());
					updateDefaultOzonePolicies(svcDBStore.getServicePolicies(dbService.getId(), filter));
				}
			}
		} else {
			logger.error("The embedded Ozone service-definition does not exist.");
			return false;
		}
		return true;
	}

	private void updateDefaultOzonePolicies(List<RangerPolicy> policies) throws Exception{
		if (CollectionUtils.isNotEmpty(policies)) {
			for (RangerPolicy policy : policies) {
				if (policy.getName().startsWith("all")) {
					RangerPolicy.RangerPolicyItem policyItemOwner = new RangerPolicy.RangerPolicyItem();
					policyItemOwner.setUsers(Collections.singletonList(RangerPolicyEngine.RESOURCE_OWNER));
					policyItemOwner.setAccesses(Collections.singletonList(new RangerPolicy.RangerPolicyItemAccess(ACCESS_TYPE_ALL)));
					policyItemOwner.setDelegateAdmin(true);
					policy.getPolicyItems().add(policyItemOwner);
				}
				svcDBStore.updatePolicy(policy);
			}
		}
	}
}
