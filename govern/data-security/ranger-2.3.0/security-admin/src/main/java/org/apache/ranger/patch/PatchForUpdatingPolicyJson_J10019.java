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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.authorization.utils.StringUtil;
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
import org.apache.ranger.entity.XXPolicyItem;
import org.apache.ranger.entity.XXPolicyItemAccess;
import org.apache.ranger.entity.XXPolicyItemCondition;
import org.apache.ranger.entity.XXPolicyItemDataMaskInfo;
import org.apache.ranger.entity.XXPolicyItemGroupPerm;
import org.apache.ranger.entity.XXPolicyItemRowFilterInfo;
import org.apache.ranger.entity.XXPolicyItemUserPerm;
import org.apache.ranger.entity.XXPolicyLabel;
import org.apache.ranger.entity.XXPolicyLabelMap;
import org.apache.ranger.entity.XXPolicyRefAccessType;
import org.apache.ranger.entity.XXPolicyRefCondition;
import org.apache.ranger.entity.XXPolicyRefDataMaskType;
import org.apache.ranger.entity.XXPolicyRefGroup;
import org.apache.ranger.entity.XXPolicyRefResource;
import org.apache.ranger.entity.XXPolicyRefUser;
import org.apache.ranger.entity.XXPolicyResource;
import org.apache.ranger.entity.XXPolicyResourceMap;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerDataMaskPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemDataMaskInfo;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemRowFilterInfo;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerPolicy.RangerRowFilterPolicyItem;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.apache.ranger.plugin.policyevaluator.RangerPolicyItemEvaluator;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.service.RangerPolicyService;
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
public class PatchForUpdatingPolicyJson_J10019 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchForUpdatingPolicyJson_J10019.class);

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
			PatchForUpdatingPolicyJson_J10019 loader = (PatchForUpdatingPolicyJson_J10019) CLIUtil.getBean(PatchForUpdatingPolicyJson_J10019.class);

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
		logger.info("==> PatchForUpdatingPolicyJson.execLoad()");

		try {
			updateRangerPolicyTableWithPolicyJson();
		} catch (Exception e) {
			logger.error("Error while updateRangerPolicyTableWithPolicyJson()", e);
			System.exit(1);
		}

		logger.info("<== PatchForUpdatingPolicyJson.execLoad()");
	}

	@Override
	public void printStats() {
		logger.info("updateRangerPolicyTableWithPolicyJson data ");
	}

	private void updateRangerPolicyTableWithPolicyJson() throws Exception {
		logger.info("==> updateRangerPolicyTableWithPolicyJson() ");

		List<RangerService> allServices = svcStore.getServices(new SearchFilter());

		if (CollectionUtils.isNotEmpty(allServices)) {
			for (RangerService service : allServices) {
				XXService dbService = daoMgr.getXXService().getById(service.getId());

				logger.info("==> Port Policies of service(name=" + dbService.getName() + ")");

				RangerPolicyRetriever policyRetriever = new RangerPolicyRetriever(daoMgr, txManager);

				List<RangerPolicy> policies = policyRetriever.getServicePolicies(dbService);

				if (CollectionUtils.isNotEmpty(policies)) {
					TransactionTemplate txTemplate = new TransactionTemplate(txManager);

					for (RangerPolicy policy : policies) {
						XXPolicy xPolicy = daoMgr.getXXPolicy().getById(policy.getId());
						if (xPolicy != null && StringUtil.isEmpty(xPolicy.getPolicyText())) {

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
				}
			}
		}

		logger.info("<== updateRangerPolicyTableWithPolicyJson() ");
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

	static private class RangerPolicyRetriever {
		static final Logger LOG      = LoggerFactory.getLogger(RangerPolicyRetriever.class);
		static final Logger PERF_LOG = RangerPerfTracer.getPerfLogger("db.RangerPolicyRetriever");

		private final RangerDaoManager daoMgr;
		private final LookupCache      lookupCache = new LookupCache();

		private final PlatformTransactionManager txManager;
		private final TransactionTemplate        txTemplate;

		RangerPolicyRetriever(RangerDaoManager daoMgr, PlatformTransactionManager txManager) {
			this.daoMgr    = daoMgr;
			this.txManager = txManager;

			if (this.txManager != null) {
				this.txTemplate = new TransactionTemplate(this.txManager);

				this.txTemplate.setReadOnly(true);
			} else {
				this.txTemplate = null;
			}
		}

		private class PolicyLoaderThread extends Thread {
			final TransactionTemplate txTemplate;
			final XXService           xService;
			List<RangerPolicy>        policies;

			PolicyLoaderThread(TransactionTemplate txTemplate, final XXService xService) {
				this.txTemplate = txTemplate;
				this.xService   = xService;
			}

			public List<RangerPolicy> getPolicies() {
				return policies;
			}

			@Override
			public void run() {
				txTemplate.setReadOnly(true);
				policies = txTemplate.execute(new TransactionCallback<List<RangerPolicy>>() {
					@Override
					public List<RangerPolicy> doInTransaction(TransactionStatus status) {
						RetrieverContext ctx = new RetrieverContext(xService);
						return ctx.getAllPolicies();
					}
				});
			}
		}

		public List<RangerPolicy> getServicePolicies(final XXService xService) throws InterruptedException {
			String serviceName = xService == null ? null : xService.getName();
			Long   serviceId   = xService == null ? null : xService.getId();

			if (LOG.isDebugEnabled()) {
				LOG.debug("==> RangerPolicyRetriever.getServicePolicies(serviceName=" + serviceName + ", serviceId=" + serviceId + ")");
			}

			List<RangerPolicy> ret  = null;
			RangerPerfTracer   perf = null;

			if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
				perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerPolicyRetriever.getServicePolicies(serviceName=" + serviceName + ",serviceId=" + serviceId + ")");
			}

			if (xService != null) {
				if (txTemplate == null) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Transaction Manager is null; Retrieving policies in the existing transaction");
					}

					RetrieverContext ctx = new RetrieverContext(xService);

					ret = ctx.getAllPolicies();
				} else {
					if (LOG.isDebugEnabled()) {
						LOG.debug("Retrieving policies in a new, read-only transaction");
					}

					PolicyLoaderThread t = new PolicyLoaderThread(txTemplate, xService);
					t.start();
					t.join();
					ret = t.getPolicies();
				}
			} else {
				if (LOG.isDebugEnabled()) {
					LOG.debug("RangerPolicyRetriever.getServicePolicies(xService=" + xService + "): invalid parameter");
				}
			}

			RangerPerfTracer.log(perf);

			if (LOG.isDebugEnabled()) {
				LOG.debug("<== RangerPolicyRetriever.getServicePolicies(serviceName=" + serviceName + ", serviceId=" + serviceId + "): policyCount=" + (ret == null ? 0 : ret.size()));
			}

			return ret;
		}

		class LookupCache {
			final Map<Long, String> userNames       = new HashMap<Long, String>();
			final Map<Long, String> userScreenNames = new HashMap<Long, String>();
			final Map<Long, String> groupNames      = new HashMap<Long, String>();
			final Map<Long, String> accessTypes     = new HashMap<Long, String>();
			final Map<Long, String> conditions      = new HashMap<Long, String>();
			final Map<Long, String> resourceDefs    = new HashMap<Long, String>();
			final Map<Long, String> dataMasks       = new HashMap<Long, String>();
            final Map<Long, String> policyLabels    = new HashMap<Long, String>();

			String getUserName(Long userId) {
				String ret = null;

				if (userId != null) {
					ret = userNames.get(userId);

					if (ret == null) {
						XXUser user = daoMgr.getXXUser().getById(userId);

						if (user != null) {
							ret = user.getName(); // Name is `loginId`

							userNames.put(userId, ret);
						}
					}
				}

				return ret;
			}

            String getPolicyLabelName(Long policyLabelId) {
                String ret = null;

                if (policyLabelId != null) {
                    ret = policyLabels.get(policyLabelId);

                    if (ret == null) {
                        XXPolicyLabel xxPolicyLabel = daoMgr.getXXPolicyLabels().getById(policyLabelId);

                        if (xxPolicyLabel != null) {
                            ret = xxPolicyLabel.getPolicyLabel();

                            policyLabels.put(policyLabelId, ret);
                        }
                    }
                }

                return ret;
            }

			String getUserScreenName(Long userId) {
				String ret = null;

				if (userId != null) {
					ret = userScreenNames.get(userId);

					if (ret == null) {
						XXPortalUser user = daoMgr.getXXPortalUser().getById(userId);

						if (user != null) {
							ret = user.getPublicScreenName();

							if (StringUtil.isEmpty(ret)) {
								ret = user.getFirstName();

								if (StringUtil.isEmpty(ret)) {
									ret = user.getLoginId();
								} else {
									if (!StringUtil.isEmpty(user.getLastName())) {
										ret += (" " + user.getLastName());
									}
								}
							}

							if (ret != null) {
								userScreenNames.put(userId, ret);
							}
						}
					}
				}

				return ret;
			}

			String getGroupName(Long groupId) {
				String ret = null;

				if (groupId != null) {
					ret = groupNames.get(groupId);

					if (ret == null) {
						XXGroup group = daoMgr.getXXGroup().getById(groupId);

						if (group != null) {
							ret = group.getName();

							groupNames.put(groupId, ret);
						}
					}
				}

				return ret;
			}

			String getAccessType(Long accessTypeId) {
				String ret = null;

				if (accessTypeId != null) {
					ret = accessTypes.get(accessTypeId);

					if (ret == null) {
						XXAccessTypeDef xAccessType = daoMgr.getXXAccessTypeDef().getById(accessTypeId);

						if (xAccessType != null) {
							ret = xAccessType.getName();

							accessTypes.put(accessTypeId, ret);
						} else {
							LOG.warn("getAccessType(): Canot find name for accessTypeId " + accessTypeId + ". This will cause Ranger policy migration to fail. Please check if all service-defs are migrated correctly!");
						}
					}
				}

				return ret;
			}

			String getConditionType(Long conditionDefId) {
				String ret = null;

				if (conditionDefId != null) {
					ret = conditions.get(conditionDefId);

					if (ret == null) {
						XXPolicyConditionDef xPolicyConditionDef = daoMgr.getXXPolicyConditionDef()
								.getById(conditionDefId);

						if (xPolicyConditionDef != null) {
							ret = xPolicyConditionDef.getName();

							conditions.put(conditionDefId, ret);
						}
					}
				}

				return ret;
			}

			String getResourceName(Long resourceDefId) {
				String ret = null;

				if (resourceDefId != null) {
					ret = resourceDefs.get(resourceDefId);

					if (ret == null) {
						XXResourceDef xResourceDef = daoMgr.getXXResourceDef().getById(resourceDefId);

						if (xResourceDef != null) {
							ret = xResourceDef.getName();

							resourceDefs.put(resourceDefId, ret);
						}
					}
				}

				return ret;
			}

			String getDataMaskName(Long dataMaskDefId) {
				String ret = null;

				if (dataMaskDefId != null) {
					ret = dataMasks.get(dataMaskDefId);

					if (ret == null) {
						XXDataMaskTypeDef xDataMaskDef = daoMgr.getXXDataMaskTypeDef().getById(dataMaskDefId);

						if (xDataMaskDef != null) {
							ret = xDataMaskDef.getName();

							dataMasks.put(dataMaskDefId, ret);
						}
					}
				}

				return ret;
			}
		}

		static List<XXPolicy> asList(XXPolicy policy) {
			List<XXPolicy> ret = new ArrayList<XXPolicy>();

			if (policy != null) {
				ret.add(policy);
			}

			return ret;
		}

		class RetrieverContext {
			final XXService                               service;
			final ListIterator<XXPolicy>                  iterPolicy;
			final ListIterator<XXPolicyResource>          iterResources;
			final ListIterator<XXPolicyResourceMap>       iterResourceMaps;
			final ListIterator<XXPolicyItem>              iterPolicyItems;
			final ListIterator<XXPolicyItemUserPerm>      iterUserPerms;
			final ListIterator<XXPolicyItemGroupPerm>     iterGroupPerms;
			final ListIterator<XXPolicyItemAccess>        iterAccesses;
			final ListIterator<XXPolicyItemCondition>     iterConditions;
			final ListIterator<XXPolicyItemDataMaskInfo>  iterDataMaskInfos;
			final ListIterator<XXPolicyItemRowFilterInfo> iterRowFilterInfos;
            final ListIterator<XXPolicyLabelMap>          iterPolicyLabels;

			RetrieverContext(XXService xService) {
				Long           serviceId = xService == null ? null : xService.getId();
				List<XXPolicy> xPolicies = daoMgr.getXXPolicy().findByServiceId(serviceId);

				this.service    = xService;
				this.iterPolicy = xPolicies.listIterator();

				List<XXPolicyResource>          xResources      = daoMgr.getXXPolicyResource().findByServiceId(serviceId);
				List<XXPolicyResourceMap>       xResourceMaps   = daoMgr.getXXPolicyResourceMap().findByServiceId(serviceId);
				List<XXPolicyItem>              xPolicyItems    = daoMgr.getXXPolicyItem().findByServiceId(serviceId);
				List<XXPolicyItemUserPerm>      xUserPerms      = daoMgr.getXXPolicyItemUserPerm().findByServiceId(serviceId);
				List<XXPolicyItemGroupPerm>     xGroupPerms     = daoMgr.getXXPolicyItemGroupPerm().findByServiceId(serviceId);
				List<XXPolicyItemAccess>        xAccesses       = daoMgr.getXXPolicyItemAccess().findByServiceId(serviceId);
				List<XXPolicyItemCondition>     xConditions     = daoMgr.getXXPolicyItemCondition().findByServiceId(serviceId);
				List<XXPolicyItemDataMaskInfo>  xDataMaskInfos  = daoMgr.getXXPolicyItemDataMaskInfo().findByServiceId(serviceId);
				List<XXPolicyItemRowFilterInfo> xRowFilterInfos = daoMgr.getXXPolicyItemRowFilterInfo().findByServiceId(serviceId);
                List<XXPolicyLabelMap>          xPolicyLabelMap = daoMgr.getXXPolicyLabelMap().findByServiceId(serviceId);

				this.iterResources      = xResources.listIterator();
				this.iterResourceMaps   = xResourceMaps.listIterator();
				this.iterPolicyItems    = xPolicyItems.listIterator();
				this.iterUserPerms      = xUserPerms.listIterator();
				this.iterGroupPerms     = xGroupPerms.listIterator();
				this.iterAccesses       = xAccesses.listIterator();
				this.iterConditions     = xConditions.listIterator();
				this.iterDataMaskInfos  = xDataMaskInfos.listIterator();
				this.iterRowFilterInfos = xRowFilterInfos.listIterator();
                this.iterPolicyLabels   = xPolicyLabelMap.listIterator();
			}

			RetrieverContext(XXPolicy xPolicy, XXService xService) {
				Long           policyId  = xPolicy == null ? null : xPolicy.getId();
				List<XXPolicy> xPolicies = asList(xPolicy);

				this.service    = xService;
				this.iterPolicy = xPolicies.listIterator();

				List<XXPolicyResource>          xResources      = daoMgr.getXXPolicyResource().findByPolicyId(policyId);
				List<XXPolicyResourceMap>       xResourceMaps   = daoMgr.getXXPolicyResourceMap().findByPolicyId(policyId);
				List<XXPolicyItem>              xPolicyItems    = daoMgr.getXXPolicyItem().findByPolicyId(policyId);
				List<XXPolicyItemUserPerm>      xUserPerms      = daoMgr.getXXPolicyItemUserPerm().findByPolicyId(policyId);
				List<XXPolicyItemGroupPerm>     xGroupPerms     = daoMgr.getXXPolicyItemGroupPerm().findByPolicyId(policyId);
				List<XXPolicyItemAccess>        xAccesses       = daoMgr.getXXPolicyItemAccess().findByPolicyId(policyId);
				List<XXPolicyItemCondition>     xConditions     = daoMgr.getXXPolicyItemCondition().findByPolicyId(policyId);
				List<XXPolicyItemDataMaskInfo>  xDataMaskInfos  = daoMgr.getXXPolicyItemDataMaskInfo().findByPolicyId(policyId);
				List<XXPolicyItemRowFilterInfo> xRowFilterInfos = daoMgr.getXXPolicyItemRowFilterInfo().findByPolicyId(policyId);
                List<XXPolicyLabelMap>          xPolicyLabelMap = daoMgr.getXXPolicyLabelMap().findByPolicyId(policyId);

				this.iterResources      = xResources.listIterator();
				this.iterResourceMaps   = xResourceMaps.listIterator();
				this.iterPolicyItems    = xPolicyItems.listIterator();
				this.iterUserPerms      = xUserPerms.listIterator();
				this.iterGroupPerms     = xGroupPerms.listIterator();
				this.iterAccesses       = xAccesses.listIterator();
				this.iterConditions     = xConditions.listIterator();
				this.iterDataMaskInfos  = xDataMaskInfos.listIterator();
				this.iterRowFilterInfos = xRowFilterInfos.listIterator();
                this.iterPolicyLabels   = xPolicyLabelMap.listIterator();
			}

			RangerPolicy getNextPolicy() {
				RangerPolicy ret = null;

				if (iterPolicy.hasNext()) {
					XXPolicy xPolicy = iterPolicy.next();

					if (xPolicy != null) {
						ret = new RangerPolicy();

						ret.setId(xPolicy.getId());
						ret.setGuid(xPolicy.getGuid());
						ret.setIsEnabled(xPolicy.getIsEnabled());
						ret.setCreatedBy(lookupCache.getUserScreenName(xPolicy.getAddedByUserId()));
						ret.setUpdatedBy(lookupCache.getUserScreenName(xPolicy.getUpdatedByUserId()));
						ret.setCreateTime(xPolicy.getCreateTime());
						ret.setUpdateTime(xPolicy.getUpdateTime());
						ret.setVersion(xPolicy.getVersion());
						ret.setService(service == null ? null : service.getName());
						ret.setName(StringUtils.trim(xPolicy.getName()));
						ret.setPolicyType(xPolicy.getPolicyType() == null ? RangerPolicy.POLICY_TYPE_ACCESS : xPolicy.getPolicyType());
						ret.setDescription(xPolicy.getDescription());
						ret.setResourceSignature(xPolicy.getResourceSignature());
						ret.setIsAuditEnabled(xPolicy.getIsAuditEnabled());
						ret.setPolicyPriority(xPolicy.getPolicyPriority());

						Map<String, String> mapOfOptions = JsonUtils.jsonToMapStringString(xPolicy.getOptions());

						if (MapUtils.isNotEmpty(mapOfOptions)) {
							String validitySchedulesStr = mapOfOptions.get(RangerPolicyService.OPTION_POLICY_VALIDITY_SCHEDULES);

							if (StringUtils.isNotEmpty(validitySchedulesStr)) {
								List<RangerValiditySchedule> validitySchedules = JsonUtils.jsonToRangerValiditySchedule(validitySchedulesStr);

								ret.setValiditySchedules(validitySchedules);
							}
						}

						getPolicyLabels(ret);
						getResource(ret);
						getPolicyItems(ret);
					}
				}

				return ret;
			}

            private void getPolicyLabels(RangerPolicy ret) {
                List<String> xPolicyLabels = new ArrayList<String>();
                while (iterPolicyLabels.hasNext()) {
                    XXPolicyLabelMap xPolicyLabel = iterPolicyLabels.next();
                    if (xPolicyLabel.getPolicyId().equals(ret.getId())) {
                        String policyLabel = lookupCache.getPolicyLabelName(xPolicyLabel.getPolicyLabelId());
                        if (policyLabel != null) {
                            xPolicyLabels.add(policyLabel);
                        }
                        ret.setPolicyLabels(xPolicyLabels);
                    } else {
                        if (iterPolicyLabels.hasPrevious()) {
                            iterPolicyLabels.previous();
                        }
                        break;
                    }
                }
            }

			List<RangerPolicy> getAllPolicies() {
				List<RangerPolicy> ret = new ArrayList<RangerPolicy>();

				while (iterPolicy.hasNext()) {
					RangerPolicy policy = getNextPolicy();

					if (policy != null) {
						ret.add(policy);
					}
				}

				if (!hasProcessedAll()) {
					LOG.warn("getAllPolicies(): perhaps one or more policies got updated during retrieval. Falling back to secondary method");

					ret = getAllPoliciesBySecondary();
				}

				return ret;
			}

			List<RangerPolicy> getAllPoliciesBySecondary() {
				List<RangerPolicy> ret = null;

				if (service != null) {
					List<XXPolicy> xPolicies = daoMgr.getXXPolicy().findByServiceId(service.getId());

					if (CollectionUtils.isNotEmpty(xPolicies)) {
						ret = new ArrayList<RangerPolicy>(xPolicies.size());

						for (XXPolicy xPolicy : xPolicies) {
							RetrieverContext ctx = new RetrieverContext(xPolicy, service);

							RangerPolicy policy = ctx.getNextPolicy();

							if (policy != null) {
								ret.add(policy);
							}
						}
					}
				}

				return ret;
			}

			private boolean hasProcessedAll() {
				boolean moreToProcess = iterPolicy.hasNext() || iterResources.hasNext() || iterResourceMaps.hasNext()
						|| iterPolicyItems.hasNext() || iterUserPerms.hasNext() || iterGroupPerms.hasNext()
						|| iterAccesses.hasNext() || iterConditions.hasNext() || iterDataMaskInfos.hasNext()
						|| iterRowFilterInfos.hasNext() || iterPolicyLabels.hasNext();

				return !moreToProcess;
			}

			private void getResource(RangerPolicy policy) {
				while (iterResources.hasNext()) {
					XXPolicyResource xResource = iterResources.next();

					if (xResource.getPolicyid().equals(policy.getId())) {
						RangerPolicyResource resource = new RangerPolicyResource();

						resource.setIsExcludes(xResource.getIsexcludes());
						resource.setIsRecursive(xResource.getIsrecursive());

						while (iterResourceMaps.hasNext()) {
							XXPolicyResourceMap xResourceMap = iterResourceMaps.next();

							if (xResourceMap.getResourceid().equals(xResource.getId())) {
								resource.getValues().add(xResourceMap.getValue());
							} else {
								if (iterResourceMaps.hasPrevious()) {
									iterResourceMaps.previous();
								}

								break;
							}
						}

						policy.getResources().put(lookupCache.getResourceName(xResource.getResdefid()), resource);
					} else if (xResource.getPolicyid().compareTo(policy.getId()) > 0) {
						if (iterResources.hasPrevious()) {
							iterResources.previous();
						}

						break;
					}
				}
			}

			private void getPolicyItems(RangerPolicy policy) {
				while (iterPolicyItems.hasNext()) {
					XXPolicyItem xPolicyItem = iterPolicyItems.next();

					if (xPolicyItem.getPolicyid().equals(policy.getId())) {
						final RangerPolicyItem          policyItem;
						final RangerDataMaskPolicyItem  dataMaskPolicyItem;
						final RangerRowFilterPolicyItem rowFilterPolicyItem;

						if (xPolicyItem.getItemType() == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DATAMASK) {
							dataMaskPolicyItem  = new RangerDataMaskPolicyItem();
							rowFilterPolicyItem = null;
							policyItem          = dataMaskPolicyItem;
						} else if (xPolicyItem.getItemType() == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ROWFILTER) {
							dataMaskPolicyItem  = null;
							rowFilterPolicyItem = new RangerRowFilterPolicyItem();
							policyItem          = rowFilterPolicyItem;
						} else {
							dataMaskPolicyItem  = null;
							rowFilterPolicyItem = null;
							policyItem          = new RangerPolicyItem();
						}

						while (iterAccesses.hasNext()) {
							XXPolicyItemAccess xAccess = iterAccesses.next();

							if (xAccess.getPolicyitemid().equals(xPolicyItem.getId())) {
								policyItem.getAccesses().add(new RangerPolicyItemAccess(lookupCache.getAccessType(xAccess.getType()), xAccess.getIsallowed()));
							} else {
								if (iterAccesses.hasPrevious()) {
									iterAccesses.previous();
								}

								break;
							}
						}

						while (iterUserPerms.hasNext()) {
							XXPolicyItemUserPerm xUserPerm = iterUserPerms.next();

							if (xUserPerm.getPolicyitemid().equals(xPolicyItem.getId())) {
								String userName = lookupCache.getUserName(xUserPerm.getUserid());

								if (userName != null) {
									policyItem.getUsers().add(userName);
								}
							} else {
								if (iterUserPerms.hasPrevious()) {
									iterUserPerms.previous();
								}

								break;
							}
						}

						while (iterGroupPerms.hasNext()) {
							XXPolicyItemGroupPerm xGroupPerm = iterGroupPerms.next();

							if (xGroupPerm.getPolicyitemid().equals(xPolicyItem.getId())) {
								String groupName = lookupCache.getGroupName(xGroupPerm.getGroupid());

								if (groupName != null) {
									policyItem.getGroups().add(groupName);
								}
							} else {
								if (iterGroupPerms.hasPrevious()) {
									iterGroupPerms.previous();
								}

								break;
							}
						}

						RangerPolicyItemCondition condition         = null;
						Long                      prevConditionType = null;

						while (iterConditions.hasNext()) {
							XXPolicyItemCondition xCondition = iterConditions.next();

							if (xCondition.getPolicyitemid().equals(xPolicyItem.getId())) {
								if (!xCondition.getType().equals(prevConditionType)) {
									condition = new RangerPolicyItemCondition();

									condition.setType(lookupCache.getConditionType(xCondition.getType()));
									condition.getValues().add(xCondition.getValue());

									policyItem.getConditions().add(condition);

									prevConditionType = xCondition.getType();
								} else {
									condition.getValues().add(xCondition.getValue());
								}
							} else {
								if (iterConditions.hasPrevious()) {
									iterConditions.previous();
								}

								break;
							}
						}

						policyItem.setDelegateAdmin(xPolicyItem.getDelegateAdmin());

						if (dataMaskPolicyItem != null) {
							while (iterDataMaskInfos.hasNext()) {
								XXPolicyItemDataMaskInfo xDataMaskInfo = iterDataMaskInfos.next();

								if (xDataMaskInfo.getPolicyItemId().equals(xPolicyItem.getId())) {
									dataMaskPolicyItem.setDataMaskInfo(new RangerPolicyItemDataMaskInfo(lookupCache.getDataMaskName(xDataMaskInfo.getType()), xDataMaskInfo.getConditionExpr(), xDataMaskInfo.getValueExpr()));
								} else {
									if (iterDataMaskInfos.hasPrevious()) {
										iterDataMaskInfos.previous();
									}

									break;
								}
							}
						}

						if (rowFilterPolicyItem != null) {
							while (iterRowFilterInfos.hasNext()) {
								XXPolicyItemRowFilterInfo xRowFilterInfo = iterRowFilterInfos.next();

								if (xRowFilterInfo.getPolicyItemId().equals(xPolicyItem.getId())) {
									rowFilterPolicyItem.setRowFilterInfo(new RangerPolicyItemRowFilterInfo(xRowFilterInfo.getFilterExpr()));
								} else {
									if (iterRowFilterInfos.hasPrevious()) {
										iterRowFilterInfos.previous();
									}

									break;
								}
							}
						}

						int itemType = xPolicyItem.getItemType() == null ? RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW : xPolicyItem.getItemType();

						if (itemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW) {
							policy.getPolicyItems().add(policyItem);
						} else if (itemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY) {
							policy.getDenyPolicyItems().add(policyItem);
						} else if (itemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ALLOW_EXCEPTIONS) {
							policy.getAllowExceptions().add(policyItem);
						} else if (itemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DENY_EXCEPTIONS) {
							policy.getDenyExceptions().add(policyItem);
						} else if (itemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_DATAMASK) {
							policy.getDataMaskPolicyItems().add(dataMaskPolicyItem);
						} else if (itemType == RangerPolicyItemEvaluator.POLICY_ITEM_TYPE_ROWFILTER) {
							policy.getRowFilterPolicyItems().add(rowFilterPolicyItem);
						} else { // unknown itemType
							LOG.warn("RangerPolicyRetriever.getPolicy(policyId=" + policy.getId() + "): ignoring unknown policyItemType " + itemType);
						}
					} else if (xPolicyItem.getPolicyid().compareTo(policy.getId()) > 0) {
						if (iterPolicyItems.hasPrevious()) {
							iterPolicyItems.previous();
						}

						break;
					}
				}
			}
		}
	}
}
