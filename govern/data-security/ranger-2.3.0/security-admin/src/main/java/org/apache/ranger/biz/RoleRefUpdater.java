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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.common.db.RangerTransactionSynchronizationAdapter;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXRoleRefGroupDao;
import org.apache.ranger.db.XXRoleRefRoleDao;
import org.apache.ranger.db.XXRoleRefUserDao;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXRole;
import org.apache.ranger.entity.XXRoleRefGroup;
import org.apache.ranger.entity.XXRoleRefRole;
import org.apache.ranger.entity.XXRoleRefUser;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.RangerRole;
import org.apache.ranger.service.RangerAuditFields;
import org.apache.ranger.service.XGroupService;
import org.apache.ranger.view.VXGroup;
import org.apache.ranger.view.VXUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoleRefUpdater {
	private static final Logger LOG = LoggerFactory.getLogger(RoleRefUpdater.class);

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	RangerAuditFields<?> rangerAuditFields;

	@Autowired
	RESTErrorUtil restErrorUtil;

	@Autowired
    XUserMgr xUserMgr;

    @Autowired
    XGroupService xGroupService;

    @Autowired
    RangerTransactionSynchronizationAdapter rangerTransactionSynchronizationAdapter;

	@Autowired
	RangerBizUtil xaBizUtil;

	public void createNewRoleMappingForRefTable(RangerRole rangerRole, Boolean createNonExistUserGroup) {
		if (rangerRole == null) {
			return;
		}

		cleanupRefTables(rangerRole);
		final Long roleId = rangerRole.getId();

		final Set<String> roleUsers = new HashSet<>();
		final Set<String> roleGroups = new HashSet<>();
		final Set<String> roleRoles = new HashSet<>();

		for (RangerRole.RoleMember user : rangerRole.getUsers()) {
			roleUsers.add(user.getName());
		}
		for (RangerRole.RoleMember group : rangerRole.getGroups()) {
			roleGroups.add(group.getName());
		}
		for (RangerRole.RoleMember role : rangerRole.getRoles()) {
			roleRoles.add(role.getName());
		}

		final boolean isCreateNonExistentUGs = createNonExistUserGroup && xaBizUtil.checkAdminAccess();

		if (CollectionUtils.isNotEmpty(roleUsers)) {
			for (String roleUser : roleUsers) {

				if (StringUtils.isBlank(roleUser)) {
					continue;
				}
				RolePrincipalAssociator associator = new RolePrincipalAssociator(PolicyRefUpdater.PRINCIPAL_TYPE.USER, roleUser, roleId);

				if (!associator.doAssociate(false)) {
					if (isCreateNonExistentUGs) {
						rangerTransactionSynchronizationAdapter.executeOnTransactionCommit(associator);
					} else {
						throw restErrorUtil.createRESTException("user with name: " + roleUser + " does not exist ", MessageEnums.INVALID_INPUT_DATA);
					}
				}
			}
		}

		if (CollectionUtils.isNotEmpty(roleGroups)) {
			for (String roleGroup : roleGroups) {

				if (StringUtils.isBlank(roleGroup)) {
					continue;
				}
				RolePrincipalAssociator associator = new RolePrincipalAssociator(PolicyRefUpdater.PRINCIPAL_TYPE.GROUP, roleGroup, roleId);

				if (!associator.doAssociate(false)) {
					if (isCreateNonExistentUGs) {
						rangerTransactionSynchronizationAdapter.executeOnTransactionCommit(associator);
					} else {
						throw restErrorUtil.createRESTException("Group with name: " + roleGroup + " does not exist ", MessageEnums.INVALID_INPUT_DATA);
					}
				}
			}
		}

		if (CollectionUtils.isNotEmpty(roleRoles)) {
			for (String roleRole : roleRoles) {

				if (StringUtils.isBlank(roleRole)) {
					continue;
				}

				RolePrincipalAssociator associator = new RolePrincipalAssociator(PolicyRefUpdater.PRINCIPAL_TYPE.ROLE, roleRole, roleId);

				if (!associator.doAssociate(false)) {
					throw restErrorUtil.createRESTException("Role with name: " + roleRole + " does not exist ", MessageEnums.INVALID_INPUT_DATA);
				}
			}
		}

	}

	public Boolean cleanupRefTables(RangerRole rangerRole) {
		final Long roleId = rangerRole.getId();

		if (roleId == null) {
			return false;
		}

		XXRoleRefUserDao xRoleUserDao = daoMgr.getXXRoleRefUser();
		XXRoleRefGroupDao xRoleGroupDao = daoMgr.getXXRoleRefGroup();
		XXRoleRefRoleDao xRoleRoleDao = daoMgr.getXXRoleRefRole();

		for (XXRoleRefUser xxRoleRefUser : xRoleUserDao.findByRoleId(roleId)) {
			xRoleUserDao.remove(xxRoleRefUser);
		}

		for (XXRoleRefGroup xxRoleRefGroup : xRoleGroupDao.findByRoleId(roleId)) {
			xRoleGroupDao.remove(xxRoleRefGroup);
		}

		for (XXRoleRefRole xxRoleRefRole : xRoleRoleDao.findByRoleId(roleId)) {
			xRoleRoleDao.remove(xxRoleRefRole);
		}
		return true;
	}

	private class RolePrincipalAssociator implements Runnable {
		final PolicyRefUpdater.PRINCIPAL_TYPE type;
		final String                     name;
		final Long                       roleId;

		public RolePrincipalAssociator(PolicyRefUpdater.PRINCIPAL_TYPE type, String name, Long roleId) {
			this.type    = type;
			this.name    = name;
			this.roleId  = roleId;
		}

		@Override
		public void run() {
			if (doAssociate(true)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Associated " + type.name() + ":" + name + " with role id:[" + roleId + "]");
				}
			} else {
				throw new RuntimeException("Failed to associate " + type.name() + ":" + name + " with role id:[" + roleId + "]");
			}
		}

		boolean doAssociate(boolean isAdmin) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("===> RolePrincipalAssociator.doAssociate(" + isAdmin + ")");
			}
			final boolean ret;

			Long id = createOrGetPrincipal(isAdmin);
			if (id != null) {
				// associate with role
				createRoleAssociation(id, name);
				ret = true;
			} else {
				ret = false;
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("<=== RolePrincipalAssociator.doAssociate(" + isAdmin + ") : " + ret);
			}
			return ret;
		}

		private Long createOrGetPrincipal(final boolean createIfAbsent) {
			if (LOG.isDebugEnabled()) {
				LOG.debug("===> RolePrincipalAssociator.createOrGetPrincipal(" + createIfAbsent + ")");
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
				LOG.debug("<=== RolePrincipalAssociator.createOrGetPrincipal(" + createIfAbsent + ") : " + ret);
			}
			return ret;
		}

		private Long createPrincipal(String user) {
			LOG.warn("User specified in role does not exist in ranger admin, creating new user, Type: " + type.name() + ", name = " + user);

			if (LOG.isDebugEnabled()) {
				LOG.debug("===> RolePrincipalAssociator.createPrincipal(type=" + type.name() +", name=" + name + ")");
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
						xaBizUtil.createTrxLog(trxLogList);
						ret = vXGroup.getId();
					}
				}
				break;
				default:
					break;
			}
			if (LOG.isDebugEnabled()) {
				LOG.debug("<=== RolePrincipalAssociator.createPrincipal(type=" + type.name() + ", name=" + name + ") : " + ret);
			}
			return ret;
		}

		private void createRoleAssociation(Long id, String name) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("===> RolePrincipalAssociator.createRoleAssociation(roleId=" + roleId + ", type=" + type.name() + ", name=" + name + ", id=" + id + ")");
			}
			switch (type) {
				case USER: {
					XXRoleRefUser xRoleRefUser = rangerAuditFields.populateAuditFieldsForCreate(new XXRoleRefUser());

					xRoleRefUser.setRoleId(roleId);
					xRoleRefUser.setUserId(id);
					xRoleRefUser.setUserName(name);
					xRoleRefUser.setUserType(0);
					daoMgr.getXXRoleRefUser().create(xRoleRefUser);
				}
				break;
				case GROUP: {
					XXRoleRefGroup xRoleRefGroup = rangerAuditFields.populateAuditFieldsForCreate(new XXRoleRefGroup());

					xRoleRefGroup.setRoleId(roleId);
					xRoleRefGroup.setGroupId(id);
					xRoleRefGroup.setGroupName(name);
					xRoleRefGroup.setGroupType(0);
					daoMgr.getXXRoleRefGroup().create(xRoleRefGroup);
				}
				break;
				case ROLE: {
					XXRoleRefRole xRoleRefRole = rangerAuditFields.populateAuditFieldsForCreate(new XXRoleRefRole());

					xRoleRefRole.setRoleId(roleId);
					xRoleRefRole.setSubRoleId(id);
					xRoleRefRole.setSubRoleName(name);
					xRoleRefRole.setSubRoleType(0);
					daoMgr.getXXRoleRefRole().create(xRoleRefRole);
				}
				break;
				default:
					break;
			}
			if(LOG.isDebugEnabled()) {
				LOG.debug("<=== RolePrincipalAssociator.createRoleAssociation(roleId=" + roleId + ", type=" + type.name() + ", name=" + name + ", id=" + id + ")");
			}
		}
	}

}
