/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.biz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.common.db.RangerTransactionSynchronizationAdapter;
import org.apache.ranger.db.RangerDaoManager;
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
import org.apache.ranger.entity.XXPolicyRefRole;
import org.apache.ranger.entity.XXPolicyRefUser;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.entity.XXRole;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerDataMaskPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemDataMaskInfo;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.service.RangerAuditFields;
import org.apache.ranger.service.XGroupService;
import org.apache.ranger.view.VXGroup;
import org.apache.ranger.view.VXResponse;
import org.apache.ranger.view.VXUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;


@Component
public class PolicyRefUpdater {

	private static final Logger LOG = LoggerFactory.getLogger(PolicyRefUpdater.class);

	public enum PRINCIPAL_TYPE { USER, GROUP, ROLE }

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	RangerAuditFields<?> rangerAuditFields;

	@Autowired
	XUserMgr xUserMgr;

	@Autowired
	RoleDBStore roleStore;

	@Autowired
	RangerBizUtil rangerBizUtil;

	@Autowired
	XGroupService xGroupService;

	@Autowired
	RangerTransactionSynchronizationAdapter rangerTransactionSynchronizationAdapter;

	@Autowired
	RESTErrorUtil restErrorUtil;

	public void createNewPolMappingForRefTable(RangerPolicy policy, XXPolicy xPolicy, XXServiceDef xServiceDef) throws Exception {
		if(policy == null) {
			return;
		}

		cleanupRefTables(policy);

		final Set<String> resourceNames   = policy.getResources().keySet();
		final Set<String> roleNames       = new HashSet<>();
		final Set<String> groupNames      = new HashSet<>();
		final Set<String> userNames       = new HashSet<>();
		final Set<String> accessTypes     = new HashSet<>();
		final Set<String> conditionTypes  = new HashSet<>();
		final Set<String> dataMaskTypes   = new HashSet<>();
		boolean oldBulkMode = RangerBizUtil.isBulkMode();

		List<RangerPolicy.RangerPolicyItemCondition> rangerPolicyConditions = policy.getConditions();
		if (CollectionUtils.isNotEmpty(rangerPolicyConditions)) {
			for (RangerPolicy.RangerPolicyItemCondition condition : rangerPolicyConditions) {
				conditionTypes.add(condition.getType());
			}
		}

		for (List<? extends RangerPolicyItem> policyItems :  getAllPolicyItems(policy)) {
			if (CollectionUtils.isEmpty(policyItems)) {
				continue;
			}

			for (RangerPolicyItem policyItem : policyItems) {
				roleNames.addAll(policyItem.getRoles());
				groupNames.addAll(policyItem.getGroups());
				userNames.addAll(policyItem.getUsers());

				if (CollectionUtils.isNotEmpty(policyItem.getAccesses())) {
					for (RangerPolicyItemAccess access : policyItem.getAccesses()) {
						accessTypes.add(access.getType());
					}
				}

				if (CollectionUtils.isNotEmpty(policyItem.getConditions())) {
					for (RangerPolicyItemCondition condition : policyItem.getConditions()) {
						conditionTypes.add(condition.getType());
					}
				}

				if (policyItem instanceof RangerDataMaskPolicyItem) {
					RangerPolicyItemDataMaskInfo dataMaskInfo = ((RangerDataMaskPolicyItem) policyItem).getDataMaskInfo();

					dataMaskTypes.add(dataMaskInfo.getDataMaskType());
				}
			}
		}

		List<XXPolicyRefResource> xPolResources = new ArrayList<>();
		for (String resource : resourceNames) {
			XXResourceDef xResDef = daoMgr.getXXResourceDef().findByNameAndPolicyId(resource, policy.getId());

			if (xResDef == null) {
				throw new Exception(resource + ": is not a valid resource-type. policy='"+  policy.getName() + "' service='"+ policy.getService() + "'");
			}

			XXPolicyRefResource xPolRes = rangerAuditFields.populateAuditFields(new XXPolicyRefResource(), xPolicy);

			xPolRes.setPolicyId(policy.getId());
			xPolRes.setResourceDefId(xResDef.getId());
			xPolRes.setResourceName(resource);

			xPolResources.add(xPolRes);
		}
		daoMgr.getXXPolicyRefResource().batchCreate(xPolResources);

		final boolean isAdmin = rangerBizUtil.checkAdminAccess();

		List<XXPolicyRefRole> xPolRoles = new ArrayList<>();
		for (String role : roleNames) {
			if (StringUtils.isBlank(role)) {
				continue;
			}
			PolicyPrincipalAssociator associator = new PolicyPrincipalAssociator(PRINCIPAL_TYPE.ROLE, role, xPolicy);
			if (!associator.doAssociate(false)) {
				if (isAdmin) {
					rangerTransactionSynchronizationAdapter.executeOnTransactionCommit(associator);
				} else {
					VXResponse gjResponse = new VXResponse();
					gjResponse.setStatusCode(HttpServletResponse.SC_BAD_REQUEST);
					gjResponse.setMsgDesc("Operation denied. Role name: " + role + " specified in policy does not exist in ranger admin.");
					throw restErrorUtil.generateRESTException(gjResponse);
				}
			}
		}
		RangerBizUtil.setBulkMode(oldBulkMode);
		daoMgr.getXXPolicyRefRole().batchCreate(xPolRoles);

		for (String group : groupNames) {
			if (StringUtils.isBlank(group)) {
				continue;
			}

			PolicyPrincipalAssociator associator = new PolicyPrincipalAssociator(PRINCIPAL_TYPE.GROUP, group, xPolicy);
			if (!associator.doAssociate(false)) {
				if (isAdmin) {
					rangerTransactionSynchronizationAdapter.executeOnTransactionCommit(associator);
				} else {
					VXResponse gjResponse = new VXResponse();
					gjResponse.setStatusCode(HttpServletResponse.SC_BAD_REQUEST);
					gjResponse.setMsgDesc("Operation denied. Group name: " + group + " specified in policy does not exist in ranger admin.");
					throw restErrorUtil.generateRESTException(gjResponse);
				}
			}
		}

		for (String user : userNames) {
			if (StringUtils.isBlank(user)) {
				continue;
			}
			PolicyPrincipalAssociator associator = new PolicyPrincipalAssociator(PRINCIPAL_TYPE.USER, user, xPolicy);
			if (!associator.doAssociate(false)) {
				if (isAdmin) {
					rangerTransactionSynchronizationAdapter.executeOnTransactionCommit(associator);
				} else {
					VXResponse gjResponse = new VXResponse();
					gjResponse.setStatusCode(HttpServletResponse.SC_BAD_REQUEST);
					gjResponse.setMsgDesc("Operation denied. User name: " + user + " specified in policy does not exist in ranger admin.");
					throw restErrorUtil.generateRESTException(gjResponse);
				}
			}
		}

		List<XXPolicyRefAccessType> xPolAccesses = new ArrayList<>();
		for (String accessType : accessTypes) {
			XXAccessTypeDef xAccTypeDef = daoMgr.getXXAccessTypeDef().findByNameAndServiceId(accessType, xPolicy.getService());

			if (xAccTypeDef == null) {
				throw new Exception(accessType + ": is not a valid access-type. policy='" + policy.getName() + "' service='" + policy.getService() + "'");
			}

			XXPolicyRefAccessType xPolAccess = rangerAuditFields.populateAuditFields(new XXPolicyRefAccessType(), xPolicy);

			xPolAccess.setPolicyId(policy.getId());
			xPolAccess.setAccessDefId(xAccTypeDef.getId());
			xPolAccess.setAccessTypeName(accessType);

			xPolAccesses.add(xPolAccess);
		}
		daoMgr.getXXPolicyRefAccessType().batchCreate(xPolAccesses);

		List<XXPolicyRefCondition> xPolConds = new ArrayList<>();
		for (String condition : conditionTypes) {
			XXPolicyConditionDef xPolCondDef = daoMgr.getXXPolicyConditionDef().findByServiceDefIdAndName(xServiceDef.getId(), condition);

			if (xPolCondDef == null) {
				throw new Exception(condition + ": is not a valid condition-type. policy='"+  xPolicy.getName() + "' service='"+ xPolicy.getService() + "'");
			}

			XXPolicyRefCondition xPolCond = rangerAuditFields.populateAuditFields(new XXPolicyRefCondition(), xPolicy);

			xPolCond.setPolicyId(policy.getId());
			xPolCond.setConditionDefId(xPolCondDef.getId());
			xPolCond.setConditionName(condition);

			xPolConds.add(xPolCond);
		}
		daoMgr.getXXPolicyRefCondition().batchCreate(xPolConds);

		List<XXPolicyRefDataMaskType> xxDataMaskInfos = new ArrayList<>();
		for (String dataMaskType : dataMaskTypes ) {
			XXDataMaskTypeDef dataMaskDef = daoMgr.getXXDataMaskTypeDef().findByNameAndServiceId(dataMaskType, xPolicy.getService());

			if (dataMaskDef == null) {
				throw new Exception(dataMaskType + ": is not a valid datamask-type. policy='" + policy.getName() + "' service='" + policy.getService() + "'");
			}

			XXPolicyRefDataMaskType xxDataMaskInfo = new XXPolicyRefDataMaskType();

			xxDataMaskInfo.setPolicyId(policy.getId());
			xxDataMaskInfo.setDataMaskDefId(dataMaskDef.getId());
			xxDataMaskInfo.setDataMaskTypeName(dataMaskType);

			xxDataMaskInfos.add(xxDataMaskInfo);
		}
		daoMgr.getXXPolicyRefDataMaskType().batchCreate(xxDataMaskInfos);
	}

	private class PolicyPrincipalAssociator implements Runnable {
		final PRINCIPAL_TYPE type;
		final String    name;
		final XXPolicy  xPolicy;

		public PolicyPrincipalAssociator(PRINCIPAL_TYPE type, String name, XXPolicy xPolicy) {
			this.type    = type;
			this.name    = name;
			this.xPolicy = xPolicy;
		}

		@Override
		public void run() {
			if (doAssociate(true)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Associated " + type.name() + ":" + name + " with policy id:[" + xPolicy.getId() + "]");
				}
			} else {
				throw new RuntimeException("Failed to associate " + type.name() + ":" + name + " with policy id:[" + xPolicy.getId() + "]");
			}
		}

		boolean doAssociate(boolean isAdmin) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("===> PolicyPrincipalAssociator.doAssociate(" + isAdmin + ")");
			}
			final boolean ret;

			Long id = createOrGetPrincipal(isAdmin);
			if (id != null) {
				// associate with policy
				createPolicyAssociation(id, name);
				ret = true;
			} else {
				ret = false;
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("<=== PolicyPrincipalAssociator.doAssociate(" + isAdmin + ") : " + ret);
			}
			return ret;
		}

		private Long createOrGetPrincipal(final boolean createIfAbsent) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("===> PolicyPrincipalAssociator.createOrGetPrincipal(" + createIfAbsent + ")");
			}

			Long ret = null;

			switch (type) {
				case USER: {
					XXUser xUser = daoMgr.getXXUser().findByUserName(name);
					if (xUser != null) {
						ret = xUser.getId();
					} else {
						if (createIfAbsent) {
							ret = createPrincipal(name);
						}
					}
				}
				break;
				case GROUP: {
					XXGroup xGroup = daoMgr.getXXGroup().findByGroupName(name);

					if (xGroup != null) {
						ret = xGroup.getId();
					} else {
						if (createIfAbsent) {
							ret = createPrincipal(name);
						}
					}
				}
				break;
				case ROLE: {
					XXRole xRole = daoMgr.getXXRole().findByRoleName(name);
					if (xRole != null) {
						ret = xRole.getId();
					} else {
						if (createIfAbsent) {
							RangerBizUtil.setBulkMode(false);
							ret = createPrincipal(name);
						}
					}
				}
				break;
				default:
					break;
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("<=== PolicyPrincipalAssociator.createOrGetPrincipal(" + createIfAbsent + ") : " + ret);
			}
			return ret;
		}

		private Long createPrincipal(String user) {
			LOG.warn("User specified in policy does not exist in ranger admin, creating new user, Type: " + type.name() + ", name = " + user);

			if (LOG.isDebugEnabled()) {
				LOG.debug("===> PolicyPrincipalAssociator.createPrincipal(type=" + type.name() +", name=" + name + ")");
			}

			Long ret = null;

			switch (type) {
				case USER: {
					// Create External user
					VXUser vXUser = xUserMgr.createServiceConfigUser(name);
					if (vXUser != null) {
						XXUser xUser = daoMgr.getXXUser().findByUserName(name);

						if (xUser == null) {
							LOG.error("No User created!! Irrecoverable error! [" + name + "]");
						} else {
							ret = xUser.getId();
						}
					} else {
						LOG.error("serviceConfigUser:[" + name + "] creation failed");
					}
				}
				break;
				case GROUP: {
					// Create group
					VXGroup vxGroup = new VXGroup();
					vxGroup.setName(name);
					vxGroup.setDescription(name);
					vxGroup.setGroupSource(RangerCommonEnums.GROUP_EXTERNAL);
					VXGroup vXGroup = xGroupService.createXGroupWithOutLogin(vxGroup);
					if (vXGroup != null) {
						List<XXTrxLog> trxLogList = xGroupService.getTransactionLog(vXGroup, "create");
						for (XXTrxLog xTrxLog : trxLogList) {
							xTrxLog.setAddedByUserId(xPolicy.getAddedByUserId());
							xTrxLog.setUpdatedByUserId(xPolicy.getAddedByUserId());
						}
						rangerBizUtil.createTrxLog(trxLogList);
						ret = vXGroup.getId();
					}
				}
				break;
				case ROLE: {
					try {
						RangerRole rRole = new RangerRole(name, null, null, null, null);
						RangerRole createdRole = roleStore.createRole(rRole, false);
						ret = createdRole.getId();
					} catch (Exception e) {
						// Ignore
					}
				}
				break;
				default:
					break;
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("<=== PolicyPrincipalAssociator.createPrincipal(type=" + type.name() + ", name=" + name + ") : " + ret);
			}
			return ret;
		}

		private void createPolicyAssociation(Long id, String name) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("===> PolicyPrincipalAssociator.createPolicyAssociation(policyId=" + xPolicy.getId() + ", type=" + type.name() + ", name=" + name + ", id=" + id + ")");
			}
			switch (type) {
				case USER: {
					XXPolicyRefUser xPolUser = rangerAuditFields.populateAuditFields(new XXPolicyRefUser(), xPolicy);

					xPolUser.setPolicyId(xPolicy.getId());
					xPolUser.setUserId(id);
					xPolUser.setUserName(name);
					daoMgr.getXXPolicyRefUser().create(xPolUser);
				}
				break;
				case GROUP: {
					XXPolicyRefGroup xPolGroup = rangerAuditFields.populateAuditFields(new XXPolicyRefGroup(), xPolicy);

					xPolGroup.setPolicyId(xPolicy.getId());
					xPolGroup.setGroupId(id);
					xPolGroup.setGroupName(name);
					daoMgr.getXXPolicyRefGroup().create(xPolGroup);
				}
				break;
				case ROLE: {
					XXPolicyRefRole xPolRole = rangerAuditFields.populateAuditFields(new XXPolicyRefRole(), xPolicy);

					xPolRole.setPolicyId(xPolicy.getId());
					xPolRole.setRoleId(id);
					xPolRole.setRoleName(name);
					daoMgr.getXXPolicyRefRole().create(xPolRole);
				}
				break;
				default:
					break;
			}
			if(LOG.isDebugEnabled()) {
				LOG.debug("<=== PolicyPrincipalAssociator.createPolicyAssociation(policyId=" + xPolicy.getId() + ", type=" + type.name() + ", name=" + name + ", id=" + id + ")");
			}
		}
	}

	public Boolean cleanupRefTables(RangerPolicy policy) {
		final Long policyId = policy == null ? null : policy.getId();

		if (policyId == null) {
			return false;
		}

		daoMgr.getXXPolicyRefResource().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyRefRole().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyRefGroup().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyRefUser().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyRefAccessType().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyRefCondition().deleteByPolicyId(policyId);
		daoMgr.getXXPolicyRefDataMaskType().deleteByPolicyId(policyId);

		return true;
	}

	public static List<List<? extends RangerPolicyItem>> getAllPolicyItems(RangerPolicy policy) {
		List<List<? extends RangerPolicyItem>> ret = new ArrayList<>();

		if (CollectionUtils.isNotEmpty(policy.getPolicyItems())) {
			ret.add(policy.getPolicyItems());
		}

		if (CollectionUtils.isNotEmpty(policy.getDenyPolicyItems())) {
			ret.add(policy.getDenyPolicyItems());
		}

		if (CollectionUtils.isNotEmpty(policy.getAllowExceptions())) {
			ret.add(policy.getAllowExceptions());
		}

		if (CollectionUtils.isNotEmpty(policy.getDenyExceptions())) {
			ret.add(policy.getDenyExceptions());
		}

		if (CollectionUtils.isNotEmpty(policy.getDataMaskPolicyItems())) {
			ret.add(policy.getDataMaskPolicyItems());
		}

		if (CollectionUtils.isNotEmpty(policy.getRowFilterPolicyItems())) {
			ret.add(policy.getRowFilterPolicyItems());
		}

		return ret;
	}

}
