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

import java.util.Date;
import java.util.List;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceVersionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.ranger.util.CLIUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatchForServiceVersionInfo_J10004 extends BaseLoader {
	private static final Logger logger = LoggerFactory
			.getLogger(PatchForServiceVersionInfo_J10004.class);

	@Autowired
	RangerDaoManager daoManager;

	public static void main(String[] args) {
		logger.info("main()");
		try {
			PatchForServiceVersionInfo_J10004 loader = (PatchForServiceVersionInfo_J10004) CLIUtil
					.getBean(PatchForServiceVersionInfo_J10004.class);

			loader.init();
			while (loader.isMoreToProcess()) {
				loader.load();
			}
			logger.info("Load complete. Exiting!!!");
			System.exit(0);
		} catch (Exception e) {
			logger.error("Error loading", e);
			System.exit(1);
		}
	}

	@Override
	public void init() throws Exception {
		// Do Nothing
	}

	@Override
	public void execLoad() {
		logger.info("==> ServiceVersionInfoPatch.execLoad()");
		copyVersionsFromServiceToServiceVersionInfo();
		logger.info("<== ServiceVersionInfoPatch.execLoad()");
	}

	public void copyVersionsFromServiceToServiceVersionInfo() {
		List<XXService> allServices = daoManager.getXXService().getAll();
		Date now = new Date();

		for (XXService xService : allServices) {

			boolean needToCreateServiceVersionInfo = false;
			XXServiceVersionInfo serviceVersionInfoDbObj = daoManager.getXXServiceVersionInfo().findByServiceId(xService.getId());

			if (serviceVersionInfoDbObj == null) {
				needToCreateServiceVersionInfo = true;
				serviceVersionInfoDbObj = new XXServiceVersionInfo();
				serviceVersionInfoDbObj.setServiceId(xService.getId());
			}
			serviceVersionInfoDbObj.setPolicyVersion(xService.getPolicyVersion() == null ? 1L : xService.getPolicyVersion());
			serviceVersionInfoDbObj.setTagVersion(xService.getTagVersion());
			serviceVersionInfoDbObj.setPolicyUpdateTime(xService.getPolicyUpdateTime());
			serviceVersionInfoDbObj.setTagUpdateTime(xService.getTagUpdateTime());

			if (needToCreateServiceVersionInfo) {
				daoManager.getXXServiceVersionInfo().create(serviceVersionInfoDbObj);
				logger.info("Created serviceVesionInfo for serviceName [" + xService.getName() + "]");
			} else {
				daoManager.getXXServiceVersionInfo().update(serviceVersionInfoDbObj);
				logger.info("Updated serviceVesionInfo for serviceName [" + xService.getName() + "]");
			}

			// Consider this scenario:
			// 1. ranger-admin is upgraded to use versions from x_service_version_info table;
			// 2. there are updates to policies and/or tags;
			// 3. no plug-ins download service-policies;
			// 4. upgrade is rolled back, for ranger-admin to use versions from x_service table;
			// 5. Now plug-in downloads service-policies.
			// In this scenario, plug-ins will miss the policy/tag updates down in step 2. To ensure that
			// plug-ins get updated policies/tags, we increment versions in x_service table when x_service_version_info
			// table is updated in this patch. This may cause one potentially unnecessary download to plugin in case
			// step 2 above did not take place, but it is safer.

			xService.setPolicyVersion(xService.getPolicyVersion() == null ? 2L : xService.getPolicyVersion() + 1);
			xService.setTagVersion(xService.getTagVersion() + 1);

			xService.setPolicyUpdateTime(now);
			xService.setTagUpdateTime(now);

			daoManager.getXXService().update(xService);
			logger.info("Incremented policy and tag versions for serviceName [" + xService.getName() + "]");
		}
	}

	@Override
	public void printStats() {
	}

}
