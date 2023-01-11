/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ranger.patch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.biz.PolicyRefUpdater;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXGroupDao;
import org.apache.ranger.db.XXPolicyDao;
import org.apache.ranger.db.XXPolicyRefAccessTypeDao;
import org.apache.ranger.db.XXPolicyRefConditionDao;
import org.apache.ranger.db.XXPolicyRefDataMaskTypeDao;
import org.apache.ranger.db.XXPolicyRefGroupDao;
import org.apache.ranger.db.XXPolicyRefResourceDao;
import org.apache.ranger.db.XXPolicyRefUserDao;
import org.apache.ranger.db.XXUserDao;
import org.apache.ranger.entity.XXAccessTypeDef;
import org.apache.ranger.entity.XXDataMaskTypeDef;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXPolicyConditionDef;
import org.apache.ranger.entity.XXPolicyRefAccessType;
import org.apache.ranger.entity.XXPolicyRefCondition;
import org.apache.ranger.entity.XXPolicyRefDataMaskType;
import org.apache.ranger.entity.XXPolicyRefGroup;
import org.apache.ranger.entity.XXPolicyRefResource;
import org.apache.ranger.entity.XXPolicyRefUser;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerDataMaskPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Consolidates Ranger policy details into a JSON string and stores it into a
 * column in x_policy table After running this patch Ranger policy can be
 * completely read/saved into x_policy table and some related Ref tables (which
 * maintain ID->String mapping for each policy).
 *
 */
@Component
public class PatchForMigratingOldRegimePolicyJson_J10046 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchForMigratingOldRegimePolicyJson_J10046.class);

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	ServiceDBStore svcStore;

	@Autowired
	@Qualifier(value = "transactionManager")
	PlatformTransactionManager txManager;

	@Autowired
	PolicyRefUpdater policyRefUpdater;

	private final Map<String, Long>              groupIdMap         = new HashMap<>();
	private final Map<String, Long>              userIdMap          = new HashMap<>();
	private final Map<String, Map<String, Long>> resourceNameIdMap  = new HashMap<>();
	private final Map<String, Map<String, Long>> accessTypeIdMap    = new HashMap<>();
	private final Map<String, Map<String, Long>> conditionNameIdMap = new HashMap<>();
	private final Map<String, Map<String, Long>> dataMaskTypeIdMap  = new HashMap<>();

	public static void main(String[] args) {
		logger.info("main()");
		try {
			PatchForMigratingOldRegimePolicyJson_J10046 loader = (PatchForMigratingOldRegimePolicyJson_J10046) CLIUtil.getBean(PatchForMigratingOldRegimePolicyJson_J10046.class);

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
		logger.info("==> PatchForMigratingOldRegimePolicyJson.execLoad()");

		try {
			migrateRangerPolicyTableWithPolicyJson();
		} catch (Exception e) {
			logger.error("Error while PatchForMigratingOldRegimePolicyJson()", e);
			System.exit(1);
		}

		logger.info("<== PatchForMigratingOldRegimePolicyJson.execLoad()");
	}

	@Override
	public void printStats() {
		logger.info("Migrating OldRegimePolicyJson data ");
	}

	private void migrateRangerPolicyTableWithPolicyJson() throws Exception {
		logger.info("==> updateRangerPolicyTableWithPolicyJson() ");
		List<XXPolicy> xxPolicyList = daoMgr.getXXPolicy().getAllByPolicyItem();
		if (CollectionUtils.isNotEmpty(xxPolicyList)) {
			for (XXPolicy xxPolicy : xxPolicyList) {
				logger.info("XXPolicy : " + xxPolicy);
				RangerPolicy policy = svcStore.getPolicy(xxPolicy.getId());
				if (policy != null) {
					TransactionTemplate txTemplate = new TransactionTemplate(txManager);
					RangerService service = svcStore.getServiceByName(policy.getService());
					PolicyUpdaterThread updaterThread = new PolicyUpdaterThread(txTemplate, service, policy);
					updaterThread.setDaemon(true);
					updaterThread.start();
					updaterThread.join();
					String errorMsg = updaterThread.getErrorMsg();
					if (StringUtils.isNotEmpty(errorMsg)) {
						throw new Exception(errorMsg);
					}
				}
			}
		} else {
			logger.info("no old XXPolicyItems found ");
		}
		logger.info("<== updateRangerPolicyTableWithPolicyJson() ");
	}
	public Boolean cleanupOldRefTables(RangerPolicy policy) {
		final Long policyId = policy == null ? null : policy.getId();

		if (policyId == null) {
			return false;
		}
		logger.info("==> cleanupOldRefTables() ");
		daoMgr.getXXPolicyItemGroupPerm().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyItemUserPerm().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyItemAccess().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyItemCondition().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyItemDataMaskInfo().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyItemRowFilterInfo().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyItem().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyResourceMap().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyResource().deleteByPolicyId(policyId);
		logger.info("<== cleanupOldRefTables() ");
		return true;
	}

	private class PolicyUpdaterThread extends Thread {
		final TransactionTemplate txTemplate;
		final RangerService       service;
		final RangerPolicy        policy;
		String                    errorMsg;

		PolicyUpdaterThread(TransactionTemplate txTemplate, final RangerService service, final RangerPolicy policy) {
			this.txTemplate = txTemplate;
			this.service   = service;
			this.policy    = policy;
			this.errorMsg  = null;
		}

		public String getErrorMsg() {
			return errorMsg;
		}

		@Override
		public void run() {
			errorMsg = txTemplate.execute(new TransactionCallback<String>() {
				@Override
				public String doInTransaction(TransactionStatus status) {
					String ret = null;
					try {
						policyRefUpdater.cleanupRefTables(policy);
						portPolicy(service.getType(), policy);
						cleanupOldRefTables(policy);
					} catch (Throwable e) {
						logger.error("PortPolicy failed for policy:[" + policy + "]", e);
						ret = e.toString();
					}
					return ret;
				}
			});
		}
	}

	private void portPolicy(String serviceType, RangerPolicy policy) throws Exception {
		logger.info("==> portPolicy(id=" + policy.getId() + ")");

		String policyText = JsonUtils.objectToJson(policy);

		if (StringUtils.isEmpty(policyText)) {
			throw new Exception("Failed to convert policy to json string. Policy: [id=" +  policy.getId() + "; name=" + policy.getName() + "; serviceType=" + serviceType + "]");
		}

		XXPolicyDao policyDao = daoMgr.getXXPolicy();
		XXPolicy    dbBean    = policyDao.getById(policy.getId());

		dbBean.setPolicyText(policyText);

		policyDao.update(dbBean);

		try {
			Set<String> accesses = new HashSet<>();
			Set<String> users = new HashSet<>();
			Set<String> groups = new HashSet<>();
			Set<String> conditions = new HashSet<>();
			Set<String> dataMasks = new HashSet<>();

			buildLists(policy.getPolicyItems(), accesses, conditions, users, groups);
			buildLists(policy.getDenyPolicyItems(), accesses, conditions, users, groups);
			buildLists(policy.getAllowExceptions(), accesses, conditions, users, groups);
			buildLists(policy.getDenyExceptions(), accesses, conditions, users, groups);
			buildLists(policy.getDataMaskPolicyItems(), accesses, conditions, users, groups);
			buildLists(policy.getRowFilterPolicyItems(), accesses, conditions, users, groups);

			buildList(policy.getDataMaskPolicyItems(), dataMasks);

			addResourceDefRef(serviceType, policy);
			addUserNameRef(policy.getId(), users);
			addGroupNameRef(policy.getId(), groups);
			addAccessDefRef(serviceType, policy.getId(), accesses);
			addPolicyConditionDefRef(serviceType, policy.getId(), conditions);
			addDataMaskDefRef(serviceType, policy.getId(), dataMasks);
		} catch (Exception e) {
		    logger.error("portPoliry(id=" + policy.getId() +") failed!!");
		    logger.error("Offending policy:" + policyText);
		    throw e;
        	}

		logger.info("<== portPolicy(id=" + policy.getId() + ")");
	}

	private void addResourceDefRef(String serviceType, RangerPolicy policy) throws Exception {
		logger.info("==> addResourceDefRef(id=" + policy.getId() + ")");

		Map<String, Long> serviceDefResourceNameIDMap = resourceNameIdMap.get(serviceType);

		if (serviceDefResourceNameIDMap == null) {
			serviceDefResourceNameIDMap = new HashMap<>();

			resourceNameIdMap.put(serviceType, serviceDefResourceNameIDMap);

			XXServiceDef dbServiceDef = daoMgr.getXXServiceDef().findByName(serviceType);

			for (XXResourceDef resourceDef : daoMgr.getXXResourceDef().findByServiceDefId(dbServiceDef.getId())) {
				serviceDefResourceNameIDMap.put(resourceDef.getName(), resourceDef.getId());
			}
		}

		Map<String, RangerPolicyResource> policyResources = policy.getResources();

		if (MapUtils.isNotEmpty(policyResources)) {
			XXPolicyRefResourceDao policyRefResourceDao = daoMgr.getXXPolicyRefResource();
			Set<String>            resourceNames        = policyResources.keySet();

			for (String resourceName : resourceNames) {
				Long resourceDefId = serviceDefResourceNameIDMap.get(resourceName);

				if (resourceDefId == null) {
					throw new Exception(resourceName + ": unknown resource in policy [id=" +  policy.getId() + "; name=" + policy.getName() + "; serviceType=" + serviceType + "]. Known resources: " + serviceDefResourceNameIDMap.keySet());
				}

				// insert policy-id, resourceDefId, resourceName into Ref table
				XXPolicyRefResource policyRefResource = new XXPolicyRefResource();

				policyRefResource.setPolicyId(policy.getId());
				policyRefResource.setResourceDefId(resourceDefId);
				policyRefResource.setResourceName(resourceName);

				policyRefResourceDao.create(policyRefResource);
			}
		}

		logger.info("<== addResourceDefRef(id=" + policy.getId() + ")");
	}

	private void addUserNameRef(Long policyId, Set<String> users) throws Exception {
		logger.info("==> addUserNameRef(id=" + policyId + ")");

		XXPolicyRefUserDao policyRefUserDao = daoMgr.getXXPolicyRefUser();
		XXUserDao          userDao          = daoMgr.getXXUser();

		// insert policy-id, userName into Ref table
		for (String user : users) {
			Long userId = userIdMap.get(user);

			if (userId == null) {
				XXUser userObject = userDao.findByUserName(user);

				if (userObject == null) {
					throw new Exception(user + ": unknown user in policy [id=" + policyId + "]");
				}

				userId = userObject.getId();

				userIdMap.put(user, userId);
			}

			XXPolicyRefUser policyRefUser = new XXPolicyRefUser();

			policyRefUser.setPolicyId(policyId);
			policyRefUser.setUserName(user);
			policyRefUser.setUserId(userId);

			policyRefUserDao.create(policyRefUser);
		}

		logger.info("<== addUserNameRef(id=" + policyId + ")");
	}

	private void addGroupNameRef(Long policyId, Set<String> groups) throws Exception {
		logger.info("==> addGroupNameRef(id=" + policyId + ")");

		// insert policy-id, groupName into Ref table
		XXPolicyRefGroupDao policyRefGroupDao = daoMgr.getXXPolicyRefGroup();
		XXGroupDao          groupDao          = daoMgr.getXXGroup();

		for (String group : groups) {
			Long groupId = groupIdMap.get(group);

			if (groupId == null) {
				XXGroup groupObject = groupDao.findByGroupName(group);

				if (groupObject == null) {
					throw new Exception(group + ": unknown group in policy [id=" + policyId + "]");
				}

				groupId = groupObject.getId();

				groupIdMap.put(group, groupId);
			}

			XXPolicyRefGroup policyRefGroup = new XXPolicyRefGroup();

			policyRefGroup.setPolicyId(policyId);
			policyRefGroup.setGroupName(group);
			policyRefGroup.setGroupId(groupId);

			policyRefGroupDao.create(policyRefGroup);
		}

		logger.info("<== addGroupNameRef(id=" + policyId + ")");

	}

	private void addAccessDefRef(String serviceType, Long policyId, Set<String> accesses) throws Exception {
		logger.info("==> addAccessDefRef(id=" + policyId + ")");
		// insert policy-id, accessName into Ref table

		Map<String, Long> serviceDefAccessTypeIDMap = accessTypeIdMap.get(serviceType);

		if (serviceDefAccessTypeIDMap == null) {
			serviceDefAccessTypeIDMap = new HashMap<>();

			accessTypeIdMap.put(serviceType, serviceDefAccessTypeIDMap);

			XXServiceDef dbServiceDef = daoMgr.getXXServiceDef().findByName(serviceType);

			for (XXAccessTypeDef accessTypeDef : daoMgr.getXXAccessTypeDef().findByServiceDefId(dbServiceDef.getId())) {
				serviceDefAccessTypeIDMap.put(accessTypeDef.getName(), accessTypeDef.getId());
			}
		}

		XXPolicyRefAccessTypeDao policyRefAccessTypeDao = daoMgr.getXXPolicyRefAccessType();

		for (String access : accesses) {
			Long accessTypeDefId = serviceDefAccessTypeIDMap.get(access);

			if (accessTypeDefId == null) {
				throw new Exception(access + ": unknown accessType in policy [id=" +  policyId + "; serviceType=" + serviceType + "]. Known accessTypes: " + serviceDefAccessTypeIDMap.keySet());
			}

			XXPolicyRefAccessType policyRefAccessType = new XXPolicyRefAccessType();

			policyRefAccessType.setPolicyId(policyId);
			policyRefAccessType.setAccessTypeName(access);
			policyRefAccessType.setAccessDefId(accessTypeDefId);

			policyRefAccessTypeDao.create(policyRefAccessType);
		}

		logger.info("<== addAccessDefRef(id=" + policyId + ")");
	}

	private void addPolicyConditionDefRef(String serviceType, Long policyId, Set<String> conditions) throws Exception {
		logger.info("==> addPolicyConditionDefRef(id=" + policyId + ")");
		// insert policy-id, conditionName into Ref table

		Map<String, Long> serviceDefConditionNameIDMap = conditionNameIdMap.get(serviceType);

		if (serviceDefConditionNameIDMap == null) {
			serviceDefConditionNameIDMap = new HashMap<>();

			conditionNameIdMap.put(serviceType, serviceDefConditionNameIDMap);

			XXServiceDef dbServiceDef = daoMgr.getXXServiceDef().findByName(serviceType);

			for (XXPolicyConditionDef conditionDef : daoMgr.getXXPolicyConditionDef().findByServiceDefId(dbServiceDef.getId())) {
				serviceDefConditionNameIDMap.put(conditionDef.getName(), conditionDef.getId());
			}
		}

		XXPolicyRefConditionDao policyRefConditionDao = daoMgr.getXXPolicyRefCondition();

		for (String condition : conditions) {
			Long conditionDefId = serviceDefConditionNameIDMap.get(condition);

			if (conditionDefId == null) {
				throw new Exception(condition + ": unknown condition in policy [id=" +  policyId + "; serviceType=" + serviceType + "]. Known conditions are: " + serviceDefConditionNameIDMap.keySet());
			}

			XXPolicyRefCondition policyRefCondition = new XXPolicyRefCondition();

			policyRefCondition.setPolicyId(policyId);
			policyRefCondition.setConditionName(condition);
			policyRefCondition.setConditionDefId(conditionDefId);

			policyRefConditionDao.create(policyRefCondition);
		}

		logger.info("<== addPolicyConditionDefRef(id=" + policyId + ")");
	}

	private void addDataMaskDefRef(String serviceType, Long policyId, Set<String> datamasks) throws Exception {
		logger.info("==> addDataMaskDefRef(id=" + policyId + ")");

		// insert policy-id, datamaskName into Ref table

		Map<String, Long> serviceDefDataMaskTypeIDMap = dataMaskTypeIdMap.get(serviceType);

		if (serviceDefDataMaskTypeIDMap == null) {
			serviceDefDataMaskTypeIDMap = new HashMap<>();

			dataMaskTypeIdMap.put(serviceType, serviceDefDataMaskTypeIDMap);

			XXServiceDef dbServiceDef = daoMgr.getXXServiceDef().findByName(serviceType);

			for (XXDataMaskTypeDef dataMaskTypeDef : daoMgr.getXXDataMaskTypeDef().findByServiceDefId(dbServiceDef.getId())) {
				serviceDefDataMaskTypeIDMap.put(dataMaskTypeDef.getName(), dataMaskTypeDef.getId());
			}
		}

		XXPolicyRefDataMaskTypeDao policyRefDataMaskTypeDao = daoMgr.getXXPolicyRefDataMaskType();

		for (String datamask : datamasks) {
			Long dataMaskTypeId = serviceDefDataMaskTypeIDMap.get(datamask);

			if (dataMaskTypeId == null) {
				throw new Exception(datamask + ": unknown dataMaskType in policy [id=" +  policyId + "; serviceType=" + serviceType + "]. Known dataMaskTypes " + serviceDefDataMaskTypeIDMap.keySet());
			}

			XXPolicyRefDataMaskType policyRefDataMaskType = new XXPolicyRefDataMaskType();

			policyRefDataMaskType.setPolicyId(policyId);
			policyRefDataMaskType.setDataMaskTypeName(datamask);
			policyRefDataMaskType.setDataMaskDefId(dataMaskTypeId);

			policyRefDataMaskTypeDao.create(policyRefDataMaskType);
		}

		logger.info("<== addDataMaskDefRef(id=" + policyId + ")");

	}

	private void buildLists(List<? extends RangerPolicyItem> policyItems, Set<String> accesses, Set<String> conditions, Set<String> users, Set<String> groups) {
		for (RangerPolicyItem item : policyItems) {
			for (RangerPolicyItemAccess policyAccess : item.getAccesses()) {
				accesses.add(policyAccess.getType());
			}

			for (RangerPolicyItemCondition policyCondition : item.getConditions()) {
				conditions.add(policyCondition.getType());
			}

			users.addAll(item.getUsers());
			groups.addAll(item.getGroups());
		}
	}

	private void buildList(List<RangerDataMaskPolicyItem> dataMaskPolicyItems, Set<String> dataMasks) {
		for (RangerDataMaskPolicyItem datMaskPolicyItem : dataMaskPolicyItems) {
			dataMasks.add(datMaskPolicyItem.getDataMaskInfo().getDataMaskType());
		}
	}

}
