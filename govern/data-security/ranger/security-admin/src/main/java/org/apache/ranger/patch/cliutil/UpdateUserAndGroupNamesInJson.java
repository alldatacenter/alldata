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

package org.apache.ranger.patch.cliutil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.authorization.utils.StringUtil;
import org.apache.ranger.biz.PolicyRefUpdater;
import org.apache.ranger.biz.SecurityZoneDBStore;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.RangerSearchUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXPolicyDao;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.patch.BaseLoader;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.util.RangerPerfTracer;
import org.apache.ranger.plugin.util.SearchFilter;
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
 * Update Ranger policy JSON string with actual user/group name value after 
 * user/group name case is converted via usersync.
 * This patch shall also update the user/group names in security zone schema.
 */
@Component
public class UpdateUserAndGroupNamesInJson extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(UpdateUserAndGroupNamesInJson.class);

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	ServiceDBStore svcStore;

	@Autowired
	@Qualifier(value = "transactionManager")
	PlatformTransactionManager txManager;

	@Autowired
	PolicyRefUpdater policyRefUpdater;

	@Autowired
	RangerSearchUtil searchUtil;

	@Autowired
	SecurityZoneDBStore securityZoneStore;

	public static void main(String[] args) {
		logger.info("main()");
		try {
			UpdateUserAndGroupNamesInJson loader = (UpdateUserAndGroupNamesInJson) CLIUtil.getBean(UpdateUserAndGroupNamesInJson.class);
			loader.init();
			while (loader.isMoreToProcess()) {
				loader.load();
			}
			logger.info("Load complete. Exiting!!!");
			System.exit(0);
		} catch (Exception e) {
			logger.error("Error loading UpdateUserAndGroupNamesInJson Utility", e);
			System.exit(1);
		}
	}

	@Override
	public void init() throws Exception {

	}

	@Override
	public void execLoad() {
		logger.info("==> UpdateUserAndGroupNamesInJson.execLoad()");
		try {
			RangerPolicyRetriever policyRetriever = new RangerPolicyRetriever(daoMgr, txManager);
			Map<String, String> usersInDB = policyRetriever.getAllUsers();
			Map<String, String> groupsInDB = policyRetriever.getAllGroups();
			updateUserAndGroupNamesInPolicyJson(policyRetriever, usersInDB, groupsInDB);
			updateRangerSecurityZoneJson(usersInDB, groupsInDB);
		} catch (Exception e) {
			logger.error("Error while UpdateUserAndGroupNamesInJson()", e);
			System.exit(1);
		}
		logger.info("<== UpdateUserAndGroupNamesInJson.execLoad()");
	}

	@Override
	public void printStats() {
		logger.info("UpdateUserAndGroupNamesInJson data ");
	}

	//Update user and group name in policy json
	private void updateUserAndGroupNamesInPolicyJson(RangerPolicyRetriever policyRetriever, Map<String, String> usersInDB, Map<String, String> groupsInDB) throws Exception {
		logger.info("==> updateUserAndGroupNamesInPolicyJson() ");
		List<RangerService> allServices = svcStore.getServices(new SearchFilter());
		if (CollectionUtils.isNotEmpty(allServices)) {
			for (RangerService service : allServices) {
				XXService dbService = daoMgr.getXXService().getById(service.getId());
				TransactionTemplate txTemplate = new TransactionTemplate(txManager);
				logger.info("==> Update Policies of service(name=" + dbService.getName() + ")");
				List<XXPolicy> policies = policyRetriever.getServicePolicies(dbService);
				if (CollectionUtils.isNotEmpty(policies)) {
					for (XXPolicy xPolicy : policies) {
						if (xPolicy != null && !StringUtil.isEmpty(xPolicy.getPolicyText())) {
							//logger.info("existingPolicyText:" + xPolicy.getPolicyText());
							RangerPolicy rangerPolicy = JsonUtils.jsonToObject(xPolicy.getPolicyText(), RangerPolicy.class);

							updatePolicyItemUsersAndGroups(rangerPolicy.getPolicyItems(), usersInDB, groupsInDB);
							updatePolicyItemUsersAndGroups(rangerPolicy.getDenyPolicyItems(), usersInDB, groupsInDB);
							updatePolicyItemUsersAndGroups(rangerPolicy.getAllowExceptions(), usersInDB, groupsInDB);
							updatePolicyItemUsersAndGroups(rangerPolicy.getDenyExceptions(), usersInDB, groupsInDB);
							updatePolicyItemUsersAndGroups(rangerPolicy.getDataMaskPolicyItems(), usersInDB, groupsInDB);
							updatePolicyItemUsersAndGroups(rangerPolicy.getRowFilterPolicyItems(), usersInDB, groupsInDB);

							String updatedPolicyText = JsonUtils.objectToJson(rangerPolicy);
							xPolicy.setPolicyText(updatedPolicyText);
							//logger.info("updatedPolicyText:" + updatedPolicyText);
							PolicyUpdaterThread updaterThread = new PolicyUpdaterThread(txTemplate, xPolicy);
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
		logger.info("<== updateUserAndGroupNamesInPolicyJson() ");
	}

	private void updatePolicyItemUsersAndGroups(List<? extends RangerPolicyItem> policyItems, Map<String, String> usersInDB, Map<String, String> groupsInDB) throws Exception {
		for (RangerPolicyItem rangerPolicyItem : policyItems) {
			List<String> policyJsonUsers = rangerPolicyItem.getUsers();
			for (int i = 0; i < policyJsonUsers.size(); i++) {
				if (usersInDB.containsKey(policyJsonUsers.get(i).toLowerCase())) {
					policyJsonUsers.set(i, usersInDB.get(policyJsonUsers.get(i).toLowerCase()));
				}
			}
			List<String> policyJsonGroups = rangerPolicyItem.getGroups();
			for (int i = 0; i < policyJsonGroups.size(); i++) {
				if (groupsInDB.containsKey(policyJsonGroups.get(i).toLowerCase())) {
					policyJsonGroups.set(i, groupsInDB.get(policyJsonGroups.get(i).toLowerCase()));
				}
			}
		}
	}

	//Update user and group name in security json
	private void updateRangerSecurityZoneJson(Map<String, String> usersInDB, Map<String, String> groupsInDB) {
		SearchFilter filter = new SearchFilter();
		try {
			List<RangerSecurityZone> securityZones = securityZoneStore.getSecurityZones(filter);
			TransactionTemplate txTemplate = new TransactionTemplate(txManager);
			for (RangerSecurityZone securityZone : securityZones) {
				updateRangerSecurityZoneUsersAndGroups(securityZone.getAdminUserGroups(), groupsInDB);
				updateRangerSecurityZoneUsersAndGroups(securityZone.getAdminUsers(), usersInDB);
				updateRangerSecurityZoneUsersAndGroups(securityZone.getAuditUserGroups(), groupsInDB);
				updateRangerSecurityZoneUsersAndGroups(securityZone.getAuditUsers(), usersInDB);

				SecurityZoneUpdaterThread updaterThread = new SecurityZoneUpdaterThread(txTemplate, securityZone);
				updaterThread.setDaemon(true);
				updaterThread.start();
				updaterThread.join();

				String errorMsg = updaterThread.getErrorMsg();
				if (StringUtils.isNotEmpty(errorMsg)) {
					throw new Exception(errorMsg);
				}
			}
		} catch (Exception ex) {
			logger.error("Error in updateRangerSecurityZoneJson()", ex);
		}
	}

	private List<String> updateRangerSecurityZoneUsersAndGroups(List<String> userOrGroups, Map<String, String> usersOrGroupsInDB) throws Exception {
		for (int i = 0; i < userOrGroups.size(); i++) {
			if (usersOrGroupsInDB.containsKey(userOrGroups.get(i).toLowerCase())) {
				userOrGroups.set(i, usersOrGroupsInDB.get(userOrGroups.get(i).toLowerCase()));
			}
		}
		return userOrGroups;
	}

	private class PolicyUpdaterThread extends Thread {
		final TransactionTemplate txTemplate;
		final XXPolicy policy;
		String errorMsg;

		PolicyUpdaterThread(TransactionTemplate txTemplate, final XXPolicy policy) {
			this.txTemplate = txTemplate;
			this.policy = policy;
			this.errorMsg = null;
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
						updatePolicyJson(policy);
					} catch (Throwable e) {
						logger.error("updatePolicyJson failed for policy:[" + policy + "]", e);
						ret = e.toString();
					}
					return ret;
				}
			});
		}
	}

	private class SecurityZoneUpdaterThread extends Thread {
		final TransactionTemplate txTemplate;
		RangerSecurityZone rangerSecurityZone;
		String errorMsg;

		SecurityZoneUpdaterThread(TransactionTemplate txTemplate, RangerSecurityZone rangerSecurityZone) {
			this.txTemplate = txTemplate;
			this.errorMsg = null;
			this.rangerSecurityZone = rangerSecurityZone;
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
						updateSecurityZone(rangerSecurityZone);
					} catch (Throwable e) {
						logger.error("updateSecurityZone failed for zone:[" + rangerSecurityZone.getId() + "]", e);
						ret = e.toString();
					}
					return ret;
				}
			});
		}
	}

	private void updatePolicyJson(XXPolicy policy) throws Exception {
		logger.info("==> updatePolicyJson(id=" + policy.getId() + ")");
		XXPolicyDao policyDao = daoMgr.getXXPolicy();
		policyDao.update(policy);
		logger.info("<== updatePolicyJson(id=" + policy.getId() + ")");
	}

	private void updateSecurityZone(RangerSecurityZone rangerSecurityZone) throws Exception {
		logger.info("==> updateSecurityZone(id=" + rangerSecurityZone.getId() + ")");
		securityZoneStore.updateSecurityZoneById(rangerSecurityZone);
		logger.info("<== updateSecurityZone(id=" + rangerSecurityZone.getId() + ")");
	}

	static private class RangerPolicyRetriever {
		static final Logger LOG = LoggerFactory.getLogger(RangerPolicyRetriever.class);
		static final Logger PERF_LOG = RangerPerfTracer.getPerfLogger("db.RangerPolicyRetriever");
		private final RangerDaoManager daoMgr;
		private final PlatformTransactionManager txManager;
		private final TransactionTemplate txTemplate;

		RangerPolicyRetriever(RangerDaoManager daoMgr, PlatformTransactionManager txManager) {
			this.daoMgr = daoMgr;
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
			final XXService xService;
			List<XXPolicy> policies;

			PolicyLoaderThread(TransactionTemplate txTemplate, final XXService xService) {
				this.txTemplate = txTemplate;
				this.xService = xService;
			}

			public List<XXPolicy> getPolicies() {
				return policies;
			}

			@Override
			public void run() {
				txTemplate.setReadOnly(true);
				policies = txTemplate.execute(new TransactionCallback<List<XXPolicy>>() {
					@Override
					public List<XXPolicy> doInTransaction(TransactionStatus status) {
						RetrieverContext ctx = new RetrieverContext(xService);
						return ctx.getAllPolicies();
					}
				});
			}
		}

		public List<XXPolicy> getServicePolicies(final XXService xService) throws InterruptedException {
			String serviceName = xService == null ? null : xService.getName();
			Long serviceId = xService == null ? null : xService.getId();
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> RangerPolicyRetriever.getServicePolicies(serviceName=" + serviceName + ", serviceId=" + serviceId + ")");
			}
			List<XXPolicy> ret = null;
			RangerPerfTracer perf = null;
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

		public Map<String, String> getAllUsers() throws InterruptedException {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> RangerPolicyRetriever.getAllUsers()");
			}
			Map<String, String> ret = null;
			RangerPerfTracer perf = null;
			if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
				perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerPolicyRetriever.getAllUsers()");
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("Transaction Manager is null; Retrieving users in the existing transaction");
			}
			RetrieverContext ctx = new RetrieverContext(null);
			ret = ctx.getAllUsersMap();
			RangerPerfTracer.log(perf);
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== RangerPolicyRetriever.getAllUsers(): userCount=" + (ret == null ? 0 : ret.size()));
			}
			return ret;
		}

		public Map<String, String> getAllGroups() throws InterruptedException {
			if (LOG.isDebugEnabled()) {
				LOG.debug("==> RangerPolicyRetriever.getAllGroups()");
			}
			Map<String, String> ret = null;
			RangerPerfTracer perf = null;
			if (RangerPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
				perf = RangerPerfTracer.getPerfTracer(PERF_LOG, "RangerPolicyRetriever.getAllGroups()");
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("Transaction Manager is null; Retrieving groups in the existing transaction");
			}
			RetrieverContext ctx = new RetrieverContext(null);
			ret = ctx.getAllGroupsMap();
			RangerPerfTracer.log(perf);
			if (LOG.isDebugEnabled()) {
				LOG.debug("<== RangerPolicyRetriever.getAllGroups(): groupCount=" + (ret == null ? 0 : ret.size()));
			}
			return ret;
		}

		class RetrieverContext {
			final XXService service;
			RetrieverContext(XXService xService) {
				this.service = xService;
			}
			List<XXPolicy> getAllPolicies() {
				Long serviceId = service == null ? 0 : service.getId();
				List<XXPolicy> xPolicies = daoMgr.getXXPolicy().findByServiceId(serviceId);
				return xPolicies;
			}
			Map<String, String> getAllUsersMap() {
				List<XXUser> xXusers = daoMgr.getXXUser().getAll();
				Map<String, String> usersMap = new HashMap<String, String>();
				for (XXUser xxUser : xXusers) {
					usersMap.put(xxUser.getName().toLowerCase(), xxUser.getName());
				}
				return usersMap;
			}
			Map<String, String> getAllGroupsMap() {
				List<XXGroup> xXgroups = daoMgr.getXXGroup().getAll();
				Map<String, String> groupsMap = new HashMap<String, String>();
				for (XXGroup xxGroup : xXgroups) {
					groupsMap.put(xxGroup.getName().toLowerCase(), xxGroup.getName());
				}
				return groupsMap;
			}
		}
	}
}
