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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXServiceConfigMapDao;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceConfigDef;
import org.apache.ranger.entity.XXServiceConfigMap;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.service.RangerAuditFields;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatchForDefaultAuidtFilters_J10050 extends BaseLoader {

	private static final Logger logger = LoggerFactory.getLogger(PatchForDefaultAuidtFilters_J10050.class);

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	ServiceDBStore svcStore;

	@Autowired
	RangerAuditFields<?> rangerAuditFields;

	public static void main(String[] args) {

		logger.info("main()");
		try {
			PatchForDefaultAuidtFilters_J10050 loader = (PatchForDefaultAuidtFilters_J10050) CLIUtil
					.getBean(PatchForDefaultAuidtFilters_J10050.class);
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
	public void printStats() {
		logger.info("adding default audit-filters to all services");

	}

	@Override
	public void execLoad() {
		logger.info("==> PatchForDefaultAuidtFilters.execLoad()");

		try {
			addDefaultAuditFilters();
		} catch (Exception e) {
			logger.error("Error while PatchForDefaultAuidtFilters", e);
			System.exit(1);
		}
		logger.info("<== PatchForDefaultAuidtFilters.execLoad()");
	}

	private void addDefaultAuditFilters() throws Exception {
		logger.debug("==> PatchForDefaultAuidtFilters_J10050.addDefaultAuditFilters()");

		Map<String, String> defaultAuditFiltersMap = null;

		List<XXService> xxServiceList = daoMgr.getXXService().getAll();

		if (CollectionUtils.isNotEmpty(xxServiceList)) {
			logger.info("Found " + xxServiceList.size() + " services");
			defaultAuditFiltersMap = new HashMap<String, String>();

			for (XXService xservice : xxServiceList) {
				RangerService rangerService = svcStore.getServiceByName(xservice.getName());
				if (rangerService != null && !rangerService.getConfigs().containsKey(ServiceDBStore.RANGER_PLUGIN_AUDIT_FILTERS)) {

					if (!defaultAuditFiltersMap.containsKey(rangerService.getType())) {
						List<XXServiceConfigDef> svcConfDefList = daoMgr.getXXServiceConfigDef()
								.findByServiceDefName(rangerService.getType());
						for(XXServiceConfigDef svcConfDef : svcConfDefList) {
							if(StringUtils.equals(svcConfDef.getName(),ServiceDBStore.RANGER_PLUGIN_AUDIT_FILTERS)) {
								defaultAuditFiltersMap.put(rangerService.getType(), svcConfDef.getDefaultvalue());
								continue;
							}
						}
					}

					if (defaultAuditFiltersMap.get(rangerService.getType()) != null) {
						Map<String, String> configs = rangerService.getConfigs();
						if (!configs.containsKey(ServiceDBStore.RANGER_PLUGIN_AUDIT_FILTERS)) {
							logger.info("adding default audit-filter to service " + rangerService.getName());
							addDefaultAuditFilterConfig(xservice, defaultAuditFiltersMap.get(rangerService.getType()));
						}
					}else {
						logger.info("No default audit-filter available for service " + rangerService.getName() + ". Skipped");
					}
				}
			}
		}

		logger.info("<== PatchForDefaultAuidtFilters_J10050.addDefaultAuditFilters()");
	}

	private void addDefaultAuditFilterConfig(XXService xservice, String defaultValue) {
		if (logger.isDebugEnabled()) {
			logger.debug("==> PatchForDefaultAuidtFilters_J10050.addDefaultAuditFilterConfig() for service (id="
					+ xservice.getId() + ")");
		}
		try {
			XXServiceConfigMapDao xConfMapDao = daoMgr.getXXServiceConfigMap();
			XXServiceConfigMap xConfMap = new XXServiceConfigMap();
			xConfMap = (XXServiceConfigMap) rangerAuditFields.populateAuditFields(xConfMap, xservice);
			xConfMap.setServiceId(xservice.getId());
			xConfMap.setConfigkey(ServiceDBStore.RANGER_PLUGIN_AUDIT_FILTERS);
			xConfMap.setConfigvalue(defaultValue);
			xConfMapDao.create(xConfMap);
		} catch (Exception e) {
			logger.error("default audit filters addition for service (id=" + xservice.getId() + ") failed!!");
			throw e;
		}
		if (logger.isDebugEnabled()) {
			logger.debug("<== PatchForDefaultAuidtFilters_J10050.addDefaultAuditFilterConfig()");
		}
	}

}
