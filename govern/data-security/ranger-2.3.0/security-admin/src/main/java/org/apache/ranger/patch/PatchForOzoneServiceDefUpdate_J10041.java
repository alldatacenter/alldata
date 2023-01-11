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
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefValidator;
import org.apache.ranger.plugin.model.validation.RangerValidator;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.service.XPermMapService;
import org.apache.ranger.service.XPolicyService;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

@Component
public class PatchForOzoneServiceDefUpdate_J10041 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchForOzoneServiceDefUpdate_J10041.class);
	private static final List<String> OZONE_CONFIGS = new ArrayList<>(
			Arrays.asList("dfs.datanode.kerberos.principal", "dfs.namenode.kerberos.principal", "dfs.secondary.namenode.kerberos.principal", "commonNameForCertificate"));
	private static final String OZONE_RESOURCE_VOLUME = "volume";
	private static final String OZONE_RESOURCE_KEY = "key";
	private static final String ACCESS_TYPE_READ_ACL = "read_acl";
	private static final String ACCESS_TYPE_WRITE_ACL = "write_acl";

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	ServiceDBStore svcDBStore;

	@Autowired
	JSONUtil jsonUtil;

	@Autowired
	RangerPolicyService policyService;

	@Autowired
	StringUtil stringUtil;

	@Autowired
	XPolicyService xPolService;

	@Autowired
	XPermMapService xPermMapService;

	@Autowired
	RangerBizUtil bizUtil;

	@Autowired
	RangerValidatorFactory validatorFactory;

	public static void main(String[] args) {
		logger.info("main()");
		try {
			PatchForOzoneServiceDefUpdate_J10041 loader = (PatchForOzoneServiceDefUpdate_J10041) CLIUtil.getBean(PatchForOzoneServiceDefUpdate_J10041.class);
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
		logger.info("PatchForOzoneServiceDefUpdate data ");
	}

	@Override
	public void execLoad() {
		logger.info("==> PatchForOzoneServiceDefUpdate.execLoad()");
		try {
			if (!updateOzoneServiceDef()) {
				logger.error("Failed to apply the patch.");
				System.exit(1);
			}
		} catch (Exception e) {
			logger.error("Error while updateOzoneServiceDef()data.", e);
			System.exit(1);
		}
		logger.info("<== PatchForOzoneServiceDefUpdate.execLoad()");
	}

	@Override
	public void init() throws Exception {
		// Do Nothing
	}

	private boolean updateOzoneServiceDef() throws Exception {
		RangerServiceDef ret;
		RangerServiceDef embeddedOzoneServiceDef;
		RangerServiceDef dbOzoneServiceDef;
		List<RangerServiceDef.RangerServiceConfigDef>   embeddedOzoneConfigDefs;
		List<RangerServiceDef.RangerResourceDef>   embeddedOzoneResourceDefs;
		List<RangerServiceDef.RangerAccessTypeDef> embeddedOzoneAccessTypes;
		XXServiceDef xXServiceDefObj;

		embeddedOzoneServiceDef = EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_OZONE_NAME);

		if (embeddedOzoneServiceDef != null) {
			xXServiceDefObj = daoMgr.getXXServiceDef().findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_OZONE_NAME);
			Map<String, String> serviceDefOptionsPreUpdate;
			String jsonPreUpdate;

			if (xXServiceDefObj != null) {
				jsonPreUpdate = xXServiceDefObj.getDefOptions();
				serviceDefOptionsPreUpdate = jsonStringToMap(jsonPreUpdate);
			} else {
				logger.error("Ozone service-definition does not exist in the Ranger DAO.");
				return false;
			}
			dbOzoneServiceDef = svcDBStore.getServiceDefByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_OZONE_NAME);

			if (dbOzoneServiceDef != null) {
				// Remove old Ozone configs
				embeddedOzoneConfigDefs = embeddedOzoneServiceDef.getConfigs();
				if (checkNotConfigPresent(embeddedOzoneConfigDefs)) {
					dbOzoneServiceDef.setConfigs(embeddedOzoneConfigDefs);
				}

				// Update volume resource with recursive flag false and key resource with recursive flag true
				embeddedOzoneResourceDefs = embeddedOzoneServiceDef.getResources();
				if (checkVolKeyResUpdate(embeddedOzoneResourceDefs)) {
					dbOzoneServiceDef.setResources(embeddedOzoneResourceDefs);
				}

				// Add new access types
				embeddedOzoneAccessTypes = embeddedOzoneServiceDef.getAccessTypes();

				if (embeddedOzoneAccessTypes != null) {
					if (checkAccessTypesPresent(embeddedOzoneAccessTypes)) {
						if (!embeddedOzoneAccessTypes.toString().equalsIgnoreCase(dbOzoneServiceDef.getAccessTypes().toString())) {
							dbOzoneServiceDef.setAccessTypes(embeddedOzoneAccessTypes);
						}
					}
				}
			} else {
				logger.error("Ozone service-definition does not exist in the db store.");
				return false;
			}
			RangerServiceDefValidator validator = validatorFactory.getServiceDefValidator(svcDBStore);
			validator.validate(dbOzoneServiceDef, RangerValidator.Action.UPDATE);

			ret = svcDBStore.updateServiceDef(dbOzoneServiceDef);
			if (ret == null) {
				throw new RuntimeException("Error while updating " + EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_OZONE_NAME + " service-def");
			}
			xXServiceDefObj = daoMgr.getXXServiceDef().findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_OZONE_NAME);
			if (xXServiceDefObj != null) {
				String jsonStrPostUpdate = xXServiceDefObj.getDefOptions();
				Map<String, String> serviceDefOptionsPostUpdate = jsonStringToMap(jsonStrPostUpdate);
				if (serviceDefOptionsPostUpdate != null && serviceDefOptionsPostUpdate.containsKey(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES)) {
					if (serviceDefOptionsPreUpdate == null || !serviceDefOptionsPreUpdate.containsKey(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES)) {
						String preUpdateValue = serviceDefOptionsPreUpdate == null ? null : serviceDefOptionsPreUpdate.get(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES);
						if (preUpdateValue == null) {
							serviceDefOptionsPostUpdate.remove(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES);
						} else {
							serviceDefOptionsPostUpdate.put(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES, preUpdateValue);
						}
						xXServiceDefObj.setDefOptions(mapToJsonString(serviceDefOptionsPostUpdate));
						daoMgr.getXXServiceDef().update(xXServiceDefObj);
					}
				}
			} else {
				logger.error("Ozone service-definition does not exist in the Ranger DAO.");
				return false;
			}
			List<XXService> dbServices = daoMgr.getXXService().findByServiceDefId(embeddedOzoneServiceDef.getId());
			if (CollectionUtils.isNotEmpty(dbServices)) {
				for(XXService dbService : dbServices) {
					SearchFilter filter = new SearchFilter();
					filter.setParam(SearchFilter.SERVICE_NAME, dbService.getName());
					updateExisitngOzonePolicies(svcDBStore.getServicePolicies(dbService.getId(), filter));
				}
			}
		} else {
			logger.error("The embedded Ozone service-definition does not exist.");
			return false;
		}
		return true;
	}

	private boolean checkNotConfigPresent(List<RangerServiceDef.RangerServiceConfigDef> configDefs) {
		boolean ret = false;
		List<String> configNames = new ArrayList<>();
		for (RangerServiceDef.RangerServiceConfigDef configDef : configDefs) {
			configNames.add(configDef.getName());
		}
		for (String delConfig : OZONE_CONFIGS) {
			if (!configNames.contains(delConfig)) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	private boolean checkVolKeyResUpdate(List<RangerServiceDef.RangerResourceDef> embeddedOzoneResDefs) {
		boolean ret = false;
		for (RangerServiceDef.RangerResourceDef resDef : embeddedOzoneResDefs) {
			if ((resDef.getName().equals(OZONE_RESOURCE_VOLUME) && (!resDef.getRecursiveSupported()  || resDef.getExcludesSupported())) ||
					(resDef.getName().equals(OZONE_RESOURCE_KEY) && resDef.getRecursiveSupported())) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	private boolean checkAccessTypesPresent(List<RangerServiceDef.RangerAccessTypeDef> embeddedOzoneAccessTypes) {
		boolean ret = false;
		for (RangerServiceDef.RangerAccessTypeDef accessDef : embeddedOzoneAccessTypes) {
			if (ACCESS_TYPE_READ_ACL.equals(accessDef.getName()) || ACCESS_TYPE_WRITE_ACL.equals(accessDef.getName())) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	private void updateExisitngOzonePolicies(List<RangerPolicy> policies) throws Exception{
		if (CollectionUtils.isNotEmpty(policies)) {
			for (RangerPolicy policy : policies) {
				List<RangerPolicy.RangerPolicyItem> policyItems = policy.getPolicyItems();
				if (CollectionUtils.isNotEmpty(policyItems)) {
					for (RangerPolicy.RangerPolicyItem policyItem : policyItems) {
						List<RangerPolicy.RangerPolicyItemAccess> policyItemAccesses = policyItem.getAccesses();
						// Add new access types
						policyItemAccesses.add(new RangerPolicy.RangerPolicyItemAccess("read_acl"));
						policyItemAccesses.add(new RangerPolicy.RangerPolicyItemAccess("write_acl"));
						policyItem.setAccesses(policyItemAccesses);
					}
				}
				Map<String, RangerPolicy.RangerPolicyResource> policyResources = policy.getResources();
				if (MapUtils.isNotEmpty(policyResources)) {
					if (policyResources.containsKey(OZONE_RESOURCE_VOLUME)) {
						// Set recursive flag as false for volume resource
						policyResources.get(OZONE_RESOURCE_VOLUME).setIsRecursive(false);
						// Set exclude support flag as true for volume resource
						policyResources.get(OZONE_RESOURCE_VOLUME).setIsExcludes(false);
					}
					if (policyResources.containsKey(OZONE_RESOURCE_KEY)) {
						// Set is recursive flag as true for volume resource
						policyResources.get(OZONE_RESOURCE_KEY).setIsRecursive(true);
					}
				}
				svcDBStore.updatePolicy(policy);
			}
		}
	}


	private String mapToJsonString(Map<String, String> map) {
		String ret = null;
		if (map != null) {
			try {
				ret = jsonUtil.readMapToString(map);
			} catch (Exception ex) {
				logger.warn("mapToJsonString() failed to convert map: " + map, ex);
			}
		}
		return ret;
	}

	protected Map<String, String> jsonStringToMap(String jsonStr) {
		Map<String, String> ret = null;
		if (!StringUtils.isEmpty(jsonStr)) {
			try {
				ret = jsonUtil.jsonToMap(jsonStr);
			} catch (Exception ex) {
				// fallback to earlier format: "name1=value1;name2=value2"
				for (String optionString : jsonStr.split(";")) {
					if (StringUtils.isEmpty(optionString)) {
						continue;
					}
					String[] nvArr = optionString.split("=");
					String name = (nvArr != null && nvArr.length > 0) ? nvArr[0].trim() : null;
					String value = (nvArr != null && nvArr.length > 1) ? nvArr[1].trim() : null;
					if (StringUtils.isEmpty(name)) {
						continue;
					}
					if (ret == null) {
						ret = new HashMap<String, String>();
					}
					ret.put(name, value);
				}
			}
		}
		return ret;
	}
}
