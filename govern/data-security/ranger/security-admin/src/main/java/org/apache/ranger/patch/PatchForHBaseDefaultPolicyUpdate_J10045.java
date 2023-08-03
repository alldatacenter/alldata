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
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.util.CLIUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
public class PatchForHBaseDefaultPolicyUpdate_J10045 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchForHBaseDefaultPolicyUpdate_J10045.class);
	public static final  String SERVICE_CONFIG_USER_NAME_PARAM = "username";
	public static final  String DEFAULT_HBASE_USER_NAME        = "hbase";
	public static final  String DEFAULT_HBASE_POLICY_NAME      = "all - table, column-family, column";

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	ServiceDBStore svcDBStore;

	public static void main(String[] args) {
		logger.info("main()");
		try {
			PatchForHBaseDefaultPolicyUpdate_J10045 loader = (PatchForHBaseDefaultPolicyUpdate_J10045) CLIUtil.getBean(PatchForHBaseDefaultPolicyUpdate_J10045.class);
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
		logger.info("PatchForHBaseDefaultPolicyUpdate data ");
	}

	@Override
	public void execLoad() {
		logger.info("==> PatchForHBaseDefaultPolicyUpdate.execLoad()");
		try {
			if (!updateHBaseDefaultPolicy()) {
				logger.error("Failed to apply the patch.");
				System.exit(1);
			}
		} catch (Exception e) {
			logger.error("Error while updateHBaseDefaultPolicy()data.", e);
			System.exit(1);
		}
		logger.info("<== PatchForHBaseDefaultPolicyUpdate.execLoad()");
	}

	@Override
	public void init() throws Exception {
		// Do Nothing
	}

	private boolean updateHBaseDefaultPolicy() throws Exception {
		RangerServiceDef embeddedHBaseServiceDef;

		embeddedHBaseServiceDef = EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_HBASE_NAME);

		if (embeddedHBaseServiceDef != null) {
			List<XXService> dbServices = daoMgr.getXXService().findByServiceDefId(embeddedHBaseServiceDef.getId());
			if (CollectionUtils.isNotEmpty(dbServices)) {
				SearchFilter filter = new SearchFilter();

				for(XXService dbService : dbServices) {
					RangerService service = svcDBStore.getServiceByName(dbService.getName());
					if (service != null) {
						String userName = service.getConfigs().get(SERVICE_CONFIG_USER_NAME_PARAM);
						if (StringUtils.isEmpty(userName)) {
							userName = DEFAULT_HBASE_USER_NAME;
						}
						updateDefaultHBasePolicy(svcDBStore.getServicePolicies(dbService.getId(), filter), userName);
					} else {
						logger.error("Cannot get RangerService with name:[" + dbService.getName() + "]");
					}
				}
			}
		} else {
			logger.error("The embedded HBase service-definition does not exist.");
			return false;
		}
		return true;
	}

	private void updateDefaultHBasePolicy(List<RangerPolicy> policies, String userName) throws Exception{
		if (CollectionUtils.isNotEmpty(policies)) {
			for (RangerPolicy policy : policies) {
				if (policy.getName().equals(DEFAULT_HBASE_POLICY_NAME)) {
					RangerPolicy.RangerPolicyItem policyItemForHBase = new RangerPolicy.RangerPolicyItem();
					policyItemForHBase.setUsers(Collections.singletonList(userName));
					List<RangerPolicy.RangerPolicyItemAccess> accesses = new ArrayList<>();
					accesses.add(new RangerPolicy.RangerPolicyItemAccess("read", true));
					accesses.add(new RangerPolicy.RangerPolicyItemAccess("write", true));
					accesses.add(new RangerPolicy.RangerPolicyItemAccess("create", true));
					accesses.add(new RangerPolicy.RangerPolicyItemAccess("admin", true));
					accesses.add(new RangerPolicy.RangerPolicyItemAccess("execute", true));
					policyItemForHBase.setAccesses(accesses);
					policyItemForHBase.setDelegateAdmin(true);
					policy.getPolicyItems().add(policyItemForHBase);
					svcDBStore.updatePolicy(policy);
					break;
				}
			}
		}
	}
}
