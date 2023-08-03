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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.ServiceUtil;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXAsset;
import org.apache.ranger.entity.XXAuditMap;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXPolicyConditionDef;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXResource;
import org.apache.ranger.entity.XXServiceConfigDef;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.service.XPermMapService;
import org.apache.ranger.service.XPolicyService;
import org.apache.ranger.util.CLIUtil;
import org.apache.ranger.view.VXPermMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatchMigration_J10002 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchMigration_J10002.class);

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

	private static int policyCounter = 0;
	private static int serviceCounter = 0;

	static Set<String> unsupportedLegacyPermTypes = new HashSet<String>();

	static {
		unsupportedLegacyPermTypes.add("Unknown");
		unsupportedLegacyPermTypes.add("Reset");
		unsupportedLegacyPermTypes.add("Obfuscate");
		unsupportedLegacyPermTypes.add("Mask");
	}

	public static void main(String[] args) {
		logger.info("main()");
		try {
			PatchMigration_J10002 loader = (PatchMigration_J10002) CLIUtil.getBean(PatchMigration_J10002.class);
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
		logger.info("==> MigrationPatch.execLoad()");
		try {
			migrateServicesToNewSchema();
			migratePoliciesToNewSchema();
			updateSequences();
		} catch (Exception e) {
			logger.error("Error whille migrating data.", e);
		}
		logger.info("<== MigrationPatch.execLoad()");
	}

	@Override
	public void printStats() {
		logger.info("Total Number of migrated repositories/services: " + serviceCounter);
		logger.info("Total Number of migrated resources/policies: " + policyCounter);
	}

	public void migrateServicesToNewSchema() throws Exception {
		logger.info("==> MigrationPatch.migrateServicesToNewSchema()");

		try {
			List<XXAsset> repoList = daoMgr.getXXAsset().getAll();

			if (repoList.isEmpty()) {
				return;
			}
			if (!repoList.isEmpty()) {
				EmbeddedServiceDefsUtil.instance().init(svcDBStore);
			}

			svcDBStore.setPopulateExistingBaseFields(true);
			for (XXAsset xAsset : repoList) {

				if (xAsset.getActiveStatus() == AppConstants.STATUS_DELETED) {
					continue;
				}

				RangerService existing = svcDBStore.getServiceByName(xAsset.getName());
				if (existing != null) {
					logger.info("Repository/Service already exists. Ignoring migration of repo: " + xAsset.getName());
					continue;
				}

				RangerService service = new RangerService();
				service = mapXAssetToService(service, xAsset);

				service = svcDBStore.createService(service);

				serviceCounter++;
				logger.info("New Service created. ServiceName: " + service.getName());
			}
			svcDBStore.setPopulateExistingBaseFields(false);
		} catch (Exception e) {
			throw new Exception("Error while migrating data to new Plugin Schema.", e);
		}
		logger.info("<== MigrationPatch.migrateServicesToNewSchema()");
	}

	public void migratePoliciesToNewSchema() throws Exception {
		logger.info("==> MigrationPatch.migratePoliciesToNewSchema()");

		try {
			List<XXResource> resList = daoMgr.getXXResource().getAll();
			if (resList.isEmpty()) {
				return;
			}

			svcDBStore.setPopulateExistingBaseFields(true);
			for (XXResource xRes : resList) {

				if (xRes.getResourceStatus() == AppConstants.STATUS_DELETED) {
					continue;
				}

				XXAsset xAsset = daoMgr.getXXAsset().getById(xRes.getAssetId());
				if (xAsset == null) {
					logger.error("No Repository found for policyName: " + xRes.getPolicyName());
					continue;
				}

				RangerService service = svcDBStore.getServiceByName(xAsset.getName());
				
				if (service == null) {
					logger.error("No Service found for policy. Ignoring migration of such policy, policyName: "
							+ xRes.getPolicyName());
					continue;
				}

				XXPolicy existing = daoMgr.getXXPolicy().findByNameAndServiceId(xRes.getPolicyName(), service.getId());
				if (existing != null) {
					logger.info("Policy already exists. Ignoring migration of policy: " + existing.getName());
					continue;
				}

				RangerPolicy policy = new RangerPolicy();
				policy = mapXResourceToPolicy(policy, xRes, service);

				if(policy != null) {
					policy = svcDBStore.createPolicy(policy);

					policyCounter++;
					logger.info("New policy created. policyName: " + policy.getName());
				}
			}
			svcDBStore.setPopulateExistingBaseFields(false);
		} catch (Exception e) {
			throw new Exception("Error while migrating data to new Plugin Schema.", e);
		}
		logger.info("<== MigrationPatch.migratePoliciesToNewSchema()");
	}

	private RangerService mapXAssetToService(RangerService service, XXAsset xAsset) throws Exception {

		String type = "";
		String name = xAsset.getName();
		String description = xAsset.getDescription();
		Map<String, String> configs = null;

		int typeInt = xAsset.getAssetType();
		XXServiceDef serviceDef = daoMgr.getXXServiceDef().findByName(AppConstants.getLabelFor_AssetType(typeInt).toLowerCase());

		if (serviceDef == null) {
			throw new Exception("No ServiceDefinition found for repository: " + name);
		}
		type = serviceDef.getName();
		configs = jsonUtil.jsonToMap(xAsset.getConfig());

		List<XXServiceConfigDef> mandatoryConfigs = daoMgr.getXXServiceConfigDef().findByServiceDefName(type);
		for (XXServiceConfigDef serviceConf : mandatoryConfigs) {
			if (serviceConf.getIsMandatory()) {
				if (!stringUtil.isEmpty(configs.get(serviceConf.getName()))) {
					continue;
				}
				String dataType = serviceConf.getType();
				String defaultValue = serviceConf.getDefaultvalue();

				if (stringUtil.isEmpty(defaultValue)) {
					defaultValue = getDefaultValueForDataType(dataType);
				}
				configs.put(serviceConf.getName(), defaultValue);
			}
		}

		service.setType(type);
		service.setName(name);
		service.setDescription(description);
		service.setConfigs(configs);

		service.setCreateTime(xAsset.getCreateTime());
		service.setUpdateTime(xAsset.getUpdateTime());

		XXPortalUser createdByUser = daoMgr.getXXPortalUser().getById(xAsset.getAddedByUserId());
		XXPortalUser updByUser = daoMgr.getXXPortalUser().getById(xAsset.getUpdatedByUserId());

		if (createdByUser != null) {
			service.setCreatedBy(createdByUser.getLoginId());
		}
		if (updByUser != null) {
			service.setUpdatedBy(updByUser.getLoginId());
		}
		service.setId(xAsset.getId());

		return service;
	}

	private String getDefaultValueForDataType(String dataType) {

		String defaultValue = "";
		switch (dataType) {
		case "int":
			defaultValue = "0";
			break;
		case "string":
			defaultValue = "unknown";
			break;
		case "bool":
			defaultValue = "false";
			break;
		case "enum":
			defaultValue = "0";
			break;
		case "password":
			defaultValue = "password";
			break;
		default:
			break;
		}
		return defaultValue;
	}

	private RangerPolicy mapXResourceToPolicy(RangerPolicy policy, XXResource xRes, RangerService service) {
		String serviceName = service.getName();
		String serviceType = service.getType();
		String name = xRes.getPolicyName();
		String description = xRes.getDescription();
		Boolean isAuditEnabled = true;
		Boolean isEnabled = true;
		Map<String, RangerPolicyResource> resources = new HashMap<String, RangerPolicyResource>();
		List<RangerPolicyItem> policyItems = new ArrayList<RangerPolicyItem>();

		XXServiceDef svcDef = daoMgr.getXXServiceDef().findByName(serviceType);
		
		if(svcDef == null) {
			logger.error(serviceType + ": service-def not found. Skipping policy '" + name + "'");

			return null;
		}

		List<XXAuditMap> auditMapList = daoMgr.getXXAuditMap().findByResourceId(xRes.getId());
		if (stringUtil.isEmpty(auditMapList)) {
			isAuditEnabled = false;
		}
		if (xRes.getResourceStatus() == AppConstants.STATUS_DISABLED) {
			isEnabled = false;
		}

		Boolean isPathRecursive  = xRes.getIsRecursive() == RangerCommonEnums.BOOL_TRUE;
		Boolean isTableExcludes  = xRes.getTableType() == RangerCommonEnums.POLICY_EXCLUSION;
		Boolean isColumnExcludes = xRes.getColumnType() == RangerCommonEnums.POLICY_EXCLUSION;

		if (StringUtils.equalsIgnoreCase(serviceType, "hdfs")) {
			toRangerResourceList(xRes.getName(), "path", Boolean.FALSE, isPathRecursive, resources);
		} else if (StringUtils.equalsIgnoreCase(serviceType, "hbase")) {
			toRangerResourceList(xRes.getTables(), "table", isTableExcludes, Boolean.FALSE, resources);
			toRangerResourceList(xRes.getColumnFamilies(), "column-family", Boolean.FALSE, Boolean.FALSE, resources);
			toRangerResourceList(xRes.getColumns(), "column", isColumnExcludes, Boolean.FALSE, resources);
		} else if (StringUtils.equalsIgnoreCase(serviceType, "hive")) {
			toRangerResourceList(xRes.getDatabases(), "database", Boolean.FALSE, Boolean.FALSE, resources);
			toRangerResourceList(xRes.getTables(), "table", isTableExcludes, Boolean.FALSE, resources);
			toRangerResourceList(xRes.getColumns(), "column", isColumnExcludes, Boolean.FALSE, resources);
			toRangerResourceList(xRes.getUdfs(), "udf", Boolean.FALSE, Boolean.FALSE, resources);
		} else if (StringUtils.equalsIgnoreCase(serviceType, "knox")) {
			toRangerResourceList(xRes.getTopologies(), "topology", Boolean.FALSE, Boolean.FALSE, resources);
			toRangerResourceList(xRes.getServices(), "service", Boolean.FALSE, Boolean.FALSE, resources);
		} else if (StringUtils.equalsIgnoreCase(serviceType, "storm")) {
			toRangerResourceList(xRes.getTopologies(), "topology", Boolean.FALSE, Boolean.FALSE, resources);
		}

		policyItems = getPolicyItemListForRes(xRes, svcDef);

		policy.setService(serviceName);
		policy.setName(name);
		policy.setDescription(description);
		policy.setIsAuditEnabled(isAuditEnabled);
		policy.setIsEnabled(isEnabled);
		policy.setResources(resources);
		policy.setPolicyItems(policyItems);

		policy.setCreateTime(xRes.getCreateTime());
		policy.setUpdateTime(xRes.getUpdateTime());

		XXPortalUser createdByUser = daoMgr.getXXPortalUser().getById(xRes.getAddedByUserId());
		XXPortalUser updByUser = daoMgr.getXXPortalUser().getById(xRes.getUpdatedByUserId());

		if (createdByUser != null) {
			policy.setCreatedBy(createdByUser.getLoginId());
		}
		if (updByUser != null) {
			policy.setUpdatedBy(updByUser.getLoginId());
		}

		policy.setId(xRes.getId());

		return policy;
	}

	private Map<String, RangerPolicy.RangerPolicyResource> toRangerResourceList(String resourceString, String resourceType, Boolean isExcludes, Boolean isRecursive, Map<String, RangerPolicy.RangerPolicyResource> resources) {
		Map<String, RangerPolicy.RangerPolicyResource> ret = resources == null ? new HashMap<String, RangerPolicy.RangerPolicyResource>() : resources;

		if(StringUtils.isNotBlank(resourceString)) {
			RangerPolicy.RangerPolicyResource resource = ret.get(resourceType);

			if(resource == null) {
				resource = new RangerPolicy.RangerPolicyResource();
				resource.setIsExcludes(isExcludes);
				resource.setIsRecursive(isRecursive);

				ret.put(resourceType, resource);
			}

			Collections.addAll(resource.getValues(), resourceString.split(","));
		}

		return ret;
	}

	private List<RangerPolicyItem> getPolicyItemListForRes(XXResource xRes, XXServiceDef svcDef) {
		List<RangerPolicyItem> policyItems = new ArrayList<RangerPolicyItem>();

		SearchCriteria sc = new SearchCriteria();

		sc.addParam("resourceId", xRes.getId());
		List<VXPermMap> permMapList = xPermMapService.searchXPermMaps(sc).getVXPermMaps();

		HashMap<String, List<VXPermMap>> sortedPermMap = new HashMap<String, List<VXPermMap>>();

		// re-group the list with permGroup as the key
		if (permMapList != null) {
			for(VXPermMap permMap : permMapList) {
				String          permGrp    = permMap.getPermGroup();
				List<VXPermMap> sortedList = sortedPermMap.get(permGrp);

				if(sortedList == null) {
					sortedList = new ArrayList<VXPermMap>();
					sortedPermMap.put(permGrp, sortedList);
				}

				sortedList.add(permMap);
			}
		}

		for (Entry<String, List<VXPermMap>> entry : sortedPermMap.entrySet()) {
			List<String>                 userList   = new ArrayList<String>();
			List<String>                 groupList  = new ArrayList<String>();
			List<RangerPolicyItemAccess> accessList = new ArrayList<RangerPolicyItemAccess>();
			String                       ipAddress  = null;

			RangerPolicy.RangerPolicyItem policyItem = new RangerPolicy.RangerPolicyItem();

			for(VXPermMap permMap : entry.getValue()) {
				if(permMap.getPermFor() == AppConstants.XA_PERM_FOR_USER) {
					String userName = getUserName(permMap);

					if (! userList.contains(userName)) {
						userList.add(userName);
					}
				} else if(permMap.getPermFor() == AppConstants.XA_PERM_FOR_GROUP) {
					String groupName = getGroupName(permMap);

					if (! groupList.contains(groupName)) {
						groupList.add(groupName);
					}
				}

				String accessType = ServiceUtil.toAccessType(permMap.getPermType());
				if(StringUtils.isBlank(accessType) || unsupportedLegacyPermTypes.contains(accessType)) {
					logger.info(accessType + ": is not a valid access-type, ignoring accesstype for policy: " + xRes.getPolicyName());
					continue;
				}

				if(StringUtils.equalsIgnoreCase(accessType, "Admin")) {
					policyItem.setDelegateAdmin(Boolean.TRUE);
					if ( svcDef.getId() == EmbeddedServiceDefsUtil.instance().getHBaseServiceDefId()) {
						addAccessType(accessType, accessList);
					}
				} else {
					addAccessType(accessType, accessList);
				}

				ipAddress = permMap.getIpAddress();
			}

			if(CollectionUtils.isEmpty(accessList)) {
				logger.info("no access specified. ignoring policyItem for policy: " + xRes.getPolicyName());
				continue;
			}

			if(CollectionUtils.isEmpty(userList) && CollectionUtils.isEmpty(groupList)) {
				logger.info("no user or group specified. ignoring policyItem for policy: " + xRes.getPolicyName());
				continue;
			}

			policyItem.setUsers(userList);
			policyItem.setGroups(groupList);
			policyItem.setAccesses(accessList);

			if(ipAddress != null && !ipAddress.isEmpty()) {
				XXPolicyConditionDef policyCond = daoMgr.getXXPolicyConditionDef().findByServiceDefIdAndName(svcDef.getId(), "ip-range");

				if(policyCond != null) {
					RangerPolicy.RangerPolicyItemCondition ipCondition = new RangerPolicy.RangerPolicyItemCondition("ip-range", Collections.singletonList(ipAddress));

					policyItem.getConditions().add(ipCondition);
				}
			}

			policyItems.add(policyItem);
		}

		return policyItems;
	}

	private void addAccessType(String accessType, List<RangerPolicyItemAccess> accessList) {
		boolean alreadyExists = false;

		for(RangerPolicyItemAccess access : accessList) {
			if(StringUtils.equalsIgnoreCase(accessType, access.getType())) {
				alreadyExists = true;

				break;
			}
		}

		if(!alreadyExists) {
			accessList.add(new RangerPolicyItemAccess(accessType));
		}
	}

	private void updateSequences() {
		daoMgr.getXXServiceDef().updateSequence();
		daoMgr.getXXService().updateSequence();
		daoMgr.getXXPolicy().updateSequence();
	}

	private String getUserName(VXPermMap permMap) {
		String userName = permMap.getUserName();

		if(userName == null || userName.isEmpty()) {
			Long userId = permMap.getUserId();

			if(userId != null) {
				XXUser xxUser = daoMgr.getXXUser().getById(userId);

				if(xxUser != null) {
					userName = xxUser.getName();
				}
			}
		}

		return userName;
	}

	private String getGroupName(VXPermMap permMap) {
		String groupName = permMap.getGroupName();

		if(groupName == null || groupName.isEmpty()) {
			Long groupId = permMap.getGroupId();

			if(groupId != null) {
				XXGroup xxGroup = daoMgr.getXXGroup().getById(groupId);

				if(xxGroup != null) {
					groupName = xxGroup.getName();
				}
			}
		}

		return groupName;
	}
}
