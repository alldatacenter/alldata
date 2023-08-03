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

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXServiceConfigDefDao;
import org.apache.ranger.entity.XXServiceConfigDef;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.service.RangerServiceDefService;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049 extends BaseLoader {
	private static final Logger logger = LoggerFactory
			.getLogger(PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049.class);

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	ServiceDBStore svcDBStore;

	@Autowired
	RangerServiceDefService serviceDefService;

	@Autowired
	StringUtil stringUtil;

	public static void main(String[] args) {
		try {
			PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049 loader = (PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049) CLIUtil
					.getBean(PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049.class);
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
		logger.info("==> PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049.execLoad()");
		try {
			updateAllServiceDef();
		} catch (Exception e) {
			logger.error("Error in PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049.execLoad()", e);
		}
		logger.info("<== PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049.execLoad()");
	}

	@Override
	public void printStats() {
		logger.info("adding default audit-filters to all service-defs");
	}

	private void updateAllServiceDef() throws Exception {
		if(logger.isDebugEnabled()) {
			logger.debug("==> PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049.updateAllServiceDef()");
		}
		List<XXServiceDef> allXXServiceDefs;
		allXXServiceDefs = daoMgr.getXXServiceDef().getAll();

		if (CollectionUtils.isNotEmpty(allXXServiceDefs)) {
			logger.info("Found " + allXXServiceDefs.size() + " services-defs");
			for (XXServiceDef xxServiceDef : allXXServiceDefs) {

				String serviceDefName = xxServiceDef.getName();

				try {
					RangerServiceConfigDef defualtAuditFiltersSvcConfDef = getDefaultAuditFiltersByServiceDef(serviceDefName);

					if (defualtAuditFiltersSvcConfDef == null) {
						logger.info("No default audit-filter available for service-def " + serviceDefName + ". Skipped");
						continue;
					}

					RangerServiceDef serviceDef = svcDBStore.getServiceDefByName(serviceDefName);

					if (serviceDef != null) {
						List<RangerServiceConfigDef> svcConfDefList = serviceDef.getConfigs();
						boolean defaultAuditFiltresFound = false;
						for (RangerServiceConfigDef svcConfDef : svcConfDefList) {
							if (StringUtils.equals(svcConfDef.getName(), ServiceDBStore.RANGER_PLUGIN_AUDIT_FILTERS)) {
								defaultAuditFiltresFound = true;
								break;
							}
						}
						if (!defaultAuditFiltresFound) {
							logger.info("adding default audit-filter for service-def:[" + serviceDefName + "]");
							int sortOrder = serviceDef.getConfigs().size() - 1;
							addDefaultAuditFilterConfig(defualtAuditFiltersSvcConfDef, xxServiceDef, sortOrder);
							logger.info("Completed adding default audit-filter for service-def:[" + serviceDefName + "]");
						}else {
							logger.info("default audit-filter already available for service-def " + serviceDefName + ". Skipped");
						}

					}else {
						logger.info("No service-def:[" + serviceDefName + "] found");
					}
					
				} catch (Exception e) {
					logger.error("Error while adding default audit-filter service-def:[" + serviceDefName + "]", e);
				}
			}
		}else {
			logger.info("No service-def found");
		}
		if(logger.isDebugEnabled()) {
			logger.debug("<== PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049.updateAllServiceDef()");
		}
	}

	private RangerServiceConfigDef getDefaultAuditFiltersByServiceDef(String serviceDefName) throws Exception {
		if(logger.isDebugEnabled()) {
			logger.debug("==> PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049.getDefaultAuditFiltersByServiceDef() for serviceDefName:["+serviceDefName+ "]");
		}
		RangerServiceConfigDef ret = null;
		RangerServiceDef embeddedAtlasServiceDef = null;
		embeddedAtlasServiceDef = EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(serviceDefName);

		List<RangerServiceConfigDef> svcConfDefList = embeddedAtlasServiceDef.getConfigs();
		for (RangerServiceConfigDef svcConfDef : svcConfDefList) {
			if (StringUtils.equals(svcConfDef.getName(), ServiceDBStore.RANGER_PLUGIN_AUDIT_FILTERS)) {
				ret = svcConfDef;
				break;
			}
		}

		if(logger.isDebugEnabled()) {
			logger.debug("<== PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049.getDefaultAuditFiltersByServiceDef() for serviceDefName:["+serviceDefName+"] ret : "+ret);
		}
		return ret;
	}

	private void addDefaultAuditFilterConfig(RangerServiceConfigDef config, XXServiceDef createdSvcDef, int sortOrder) {
		if(logger.isDebugEnabled()) {
			logger.debug("==> PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049.addDefaultAuditFilterConfig() for config:["+config+"] sortOrder: "+sortOrder );
		}
		XXServiceConfigDefDao xxServiceConfigDao = daoMgr.getXXServiceConfigDef();
		XXServiceConfigDef xConfig = new XXServiceConfigDef();
		xConfig = serviceDefService.populateRangerServiceConfigDefToXX(config, xConfig, createdSvcDef,
				RangerServiceDefService.OPERATION_CREATE_CONTEXT);
		xConfig.setOrder(sortOrder);
		xConfig = xxServiceConfigDao.create(xConfig);
		if(logger.isDebugEnabled()) {
			logger.debug("<== PatchForAllServiceDefUpdateForDefaultAuditFilters_J10049.addDefaultAuditFilterConfig() for config:["+config+"] sortOrder: "+sortOrder);
		}
	}
}
