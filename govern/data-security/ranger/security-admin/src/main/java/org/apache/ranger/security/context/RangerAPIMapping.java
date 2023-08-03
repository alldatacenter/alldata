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
package org.apache.ranger.security.context;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Component;

@Component
public class RangerAPIMapping {

	/**
	 * @NOTE While adding new tab here, please don't forget to update the function:
	 *       org.apache.ranger.security.context.RangerAPIMapping.getAvailableUITabs()
	 */
	public static final String TAB_RESOURCE_BASED_POLICIES = "Resource Based Policies";
	public static final String TAB_AUDIT = "Audit";
	public static final String TAB_USERS_GROUPS = "Users/Groups";
	public static final String TAB_PERMISSIONS = "Permissions";
	public static final String TAB_KEY_MANAGER = "Key Manager";
	public static final String TAB_TAG_BASED_POLICIES = "Tag Based Policies";
	public static final String TAB_REPORTS = "Reports";

	private static HashMap<String, Set<String>> rangerAPIMappingWithUI = null;
	private static Set<String> tabList = new HashSet<String>();
	private static Map<String, Set<String>> mapApiToTabs = null;

	public RangerAPIMapping() {
		init();
	}

	private void init() {
		if (rangerAPIMappingWithUI == null) {
			rangerAPIMappingWithUI = new HashMap<String, Set<String>>();
		}
		if (mapApiToTabs == null) {
			mapApiToTabs = new HashMap<String, Set<String>>();
		}

		mapResourceBasedPoliciesWithAPIs();
		mapAuditWithAPIs();
		mapUGWithAPIs();
		mapPermissionsWithAPIs();
		mapKeyManagerWithAPIs();
		mapTagBasedPoliciesWithAPIs();
		mapReportsWithAPIs();

		if (CollectionUtils.isEmpty(tabList)) {
			populateAvailableUITabs();
		}

	}

	private void populateAvailableUITabs() {
		tabList = new HashSet<String>();
		tabList.add(TAB_RESOURCE_BASED_POLICIES);
		tabList.add(TAB_TAG_BASED_POLICIES);
		tabList.add(TAB_AUDIT);
		tabList.add(TAB_REPORTS);
		tabList.add(TAB_KEY_MANAGER);
		tabList.add(TAB_PERMISSIONS);
		tabList.add(TAB_USERS_GROUPS);
	}

	private void mapReportsWithAPIs() {
		Set<String> apiAssociatedWithReports = new HashSet<String>();

		apiAssociatedWithReports.add(RangerAPIList.COUNT_X_ASSETS);
		apiAssociatedWithReports.add(RangerAPIList.GET_X_ASSET);
		apiAssociatedWithReports.add(RangerAPIList.SEARCH_X_ASSETS);

		apiAssociatedWithReports.add(RangerAPIList.COUNT_SERVICES);
		apiAssociatedWithReports.add(RangerAPIList.GET_POLICY_FOR_VERSION_NO);
		apiAssociatedWithReports.add(RangerAPIList.GET_POLICY_FROM_EVENT_TIME);
		apiAssociatedWithReports.add(RangerAPIList.GET_POLICY_VERSION_LIST);
		apiAssociatedWithReports.add(RangerAPIList.GET_SERVICE);
		apiAssociatedWithReports.add(RangerAPIList.GET_SERVICE_BY_NAME);
		apiAssociatedWithReports.add(RangerAPIList.GET_SERVICE_DEF);
		apiAssociatedWithReports.add(RangerAPIList.GET_SERVICE_DEF_BY_NAME);
		apiAssociatedWithReports.add(RangerAPIList.GET_SERVICE_DEFS);
		apiAssociatedWithReports.add(RangerAPIList.GET_SERVICES);
		apiAssociatedWithReports.add(RangerAPIList.LOOKUP_RESOURCE);

		apiAssociatedWithReports.add(RangerAPIList.GET_USER_PROFILE_FOR_USER);
		apiAssociatedWithReports.add(RangerAPIList.SEARCH_USERS);

		apiAssociatedWithReports.add(RangerAPIList.COUNT_X_AUDIT_MAPS);
		apiAssociatedWithReports.add(RangerAPIList.COUNT_X_GROUP_GROUPS);
		apiAssociatedWithReports.add(RangerAPIList.COUNT_X_GROUPS);
		apiAssociatedWithReports.add(RangerAPIList.COUNT_X_GROUP_USERS);
		apiAssociatedWithReports.add(RangerAPIList.COUNT_X_PERM_MAPS);
		apiAssociatedWithReports.add(RangerAPIList.COUNT_X_USERS);
		apiAssociatedWithReports.add(RangerAPIList.GET_X_AUDIT_MAP);
		apiAssociatedWithReports.add(RangerAPIList.GET_X_GROUP);
		apiAssociatedWithReports.add(RangerAPIList.GET_X_GROUP_BY_GROUP_NAME);
		apiAssociatedWithReports.add(RangerAPIList.GET_X_GROUP_GROUP);
		apiAssociatedWithReports.add(RangerAPIList.GET_X_GROUP_USER);
		apiAssociatedWithReports.add(RangerAPIList.GET_X_GROUP_USERS);
		apiAssociatedWithReports.add(RangerAPIList.GET_X_PERM_MAP);
		apiAssociatedWithReports.add(RangerAPIList.GET_X_USER);
		apiAssociatedWithReports.add(RangerAPIList.GET_X_USER_BY_USER_NAME);
		apiAssociatedWithReports.add(RangerAPIList.GET_X_USER_GROUPS);
		apiAssociatedWithReports.add(RangerAPIList.SEARCH_X_AUDIT_MAPS);
		apiAssociatedWithReports.add(RangerAPIList.SEARCH_X_GROUP_GROUPS);
		apiAssociatedWithReports.add(RangerAPIList.SEARCH_X_GROUPS);
		apiAssociatedWithReports.add(RangerAPIList.SEARCH_X_GROUP_USERS);
		apiAssociatedWithReports.add(RangerAPIList.SEARCH_X_PERM_MAPS);
		apiAssociatedWithReports.add(RangerAPIList.SEARCH_X_USERS);
		apiAssociatedWithReports.add(RangerAPIList.SECURE_GET_X_GROUP);
		apiAssociatedWithReports.add(RangerAPIList.SECURE_GET_X_USER);

		rangerAPIMappingWithUI.put(TAB_REPORTS, apiAssociatedWithReports);

		for (String api : apiAssociatedWithReports) {
			if (mapApiToTabs.get(api) == null) {
				mapApiToTabs.put(api, new HashSet<String>());
			}
			mapApiToTabs.get(api).add(TAB_REPORTS);
		}
	}

	private void mapTagBasedPoliciesWithAPIs() {
		Set<String> apiAssociatedWithTagBasedPolicy = new HashSet<String>();

		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.COUNT_X_ASSETS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.CREATE_X_ASSET);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.DELETE_X_ASSET);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_X_ASSET);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.SEARCH_X_ASSETS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.TEST_CONFIG);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.UPDATE_X_ASSET);

		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.COUNT_SERVICES);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.CREATE_SERVICE);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.CREATE_SERVICE_DEF);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.DELETE_SERVICE);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.DELETE_SERVICE_DEF);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_POLICY_FOR_VERSION_NO);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_POLICY_FROM_EVENT_TIME);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_POLICY_VERSION_LIST);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_SERVICE);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_SERVICE_BY_NAME);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_SERVICE_DEF);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_SERVICE_DEF_BY_NAME);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_SERVICE_DEFS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_SERVICES);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.LOOKUP_RESOURCE);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.UPDATE_SERVICE);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.UPDATE_SERVICE_DEF);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.VALIDATE_CONFIG);

		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_USER_PROFILE_FOR_USER);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.SEARCH_USERS);

		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.COUNT_X_AUDIT_MAPS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.COUNT_X_GROUP_GROUPS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.COUNT_X_GROUPS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.COUNT_X_GROUP_USERS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.COUNT_X_PERM_MAPS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.COUNT_X_USERS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.CREATE_X_AUDIT_MAP);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.CREATE_X_PERM_MAP);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.DELETE_X_AUDIT_MAP);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.DELETE_X_PERM_MAP);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_X_AUDIT_MAP);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_X_GROUP);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_X_GROUP_BY_GROUP_NAME);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_X_GROUP_GROUP);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_X_GROUP_USER);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_X_GROUP_USERS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_X_PERM_MAP);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_X_USER);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_X_USER_BY_USER_NAME);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.GET_X_USER_GROUPS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.MODIFY_GROUPS_VISIBILITY);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.MODIFY_USER_ACTIVE_STATUS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.MODIFY_USER_VISIBILITY);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.SEARCH_X_AUDIT_MAPS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.SEARCH_X_GROUP_GROUPS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.SEARCH_X_GROUPS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.SEARCH_X_GROUP_USERS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.SEARCH_X_PERM_MAPS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.SEARCH_X_USERS);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.SECURE_GET_X_GROUP);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.SECURE_GET_X_USER);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.UPDATE_X_AUDIT_MAP);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.UPDATE_X_PERM_MAP);

		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.CREATE);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.CREATE_DEFAULT_ACCOUNT_USER);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.UPDATE);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.SET_USER_ROLES);
		apiAssociatedWithTagBasedPolicy.add(RangerAPIList.DEACTIVATE_USER);

		rangerAPIMappingWithUI.put(TAB_TAG_BASED_POLICIES, apiAssociatedWithTagBasedPolicy);

		for (String api : apiAssociatedWithTagBasedPolicy) {
			if (mapApiToTabs.get(api) == null) {
				mapApiToTabs.put(api, new HashSet<String>());
			}
			mapApiToTabs.get(api).add(TAB_TAG_BASED_POLICIES);
		}
	}

	private void mapKeyManagerWithAPIs() {

		Set<String> apiAssociatedWithKeyManager = new HashSet<String>();

		apiAssociatedWithKeyManager.add(RangerAPIList.COUNT_X_ASSETS);
		apiAssociatedWithKeyManager.add(RangerAPIList.CREATE_X_ASSET);
		apiAssociatedWithKeyManager.add(RangerAPIList.DELETE_X_ASSET);
		apiAssociatedWithKeyManager.add(RangerAPIList.GET_X_ASSET);
		apiAssociatedWithKeyManager.add(RangerAPIList.SEARCH_X_ASSETS);
		apiAssociatedWithKeyManager.add(RangerAPIList.TEST_CONFIG);
		apiAssociatedWithKeyManager.add(RangerAPIList.UPDATE_X_ASSET);

		apiAssociatedWithKeyManager.add(RangerAPIList.COUNT_SERVICES);
		apiAssociatedWithKeyManager.add(RangerAPIList.CREATE_SERVICE);
		apiAssociatedWithKeyManager.add(RangerAPIList.CREATE_SERVICE_DEF);
		apiAssociatedWithKeyManager.add(RangerAPIList.DELETE_SERVICE);
		apiAssociatedWithKeyManager.add(RangerAPIList.DELETE_SERVICE_DEF);
		apiAssociatedWithKeyManager.add(RangerAPIList.GET_POLICY_FOR_VERSION_NO);
		apiAssociatedWithKeyManager.add(RangerAPIList.GET_POLICY_FROM_EVENT_TIME);
		apiAssociatedWithKeyManager.add(RangerAPIList.GET_POLICY_VERSION_LIST);
		apiAssociatedWithKeyManager.add(RangerAPIList.GET_SERVICE);
		apiAssociatedWithKeyManager.add(RangerAPIList.GET_SERVICE_BY_NAME);
		apiAssociatedWithKeyManager.add(RangerAPIList.GET_SERVICE_DEF);
		apiAssociatedWithKeyManager.add(RangerAPIList.GET_SERVICE_DEF_BY_NAME);
		apiAssociatedWithKeyManager.add(RangerAPIList.GET_SERVICE_DEFS);
		apiAssociatedWithKeyManager.add(RangerAPIList.GET_SERVICES);
		apiAssociatedWithKeyManager.add(RangerAPIList.LOOKUP_RESOURCE);
		apiAssociatedWithKeyManager.add(RangerAPIList.UPDATE_SERVICE);
		apiAssociatedWithKeyManager.add(RangerAPIList.UPDATE_SERVICE_DEF);
		apiAssociatedWithKeyManager.add(RangerAPIList.VALIDATE_CONFIG);

		apiAssociatedWithKeyManager.add(RangerAPIList.CREATE_KEY);
		apiAssociatedWithKeyManager.add(RangerAPIList.DELETE_KEY);
		apiAssociatedWithKeyManager.add(RangerAPIList.GET_KEY);
		apiAssociatedWithKeyManager.add(RangerAPIList.ROLLOVER_KEYS);
		apiAssociatedWithKeyManager.add(RangerAPIList.SEARCH_KEYS);

		rangerAPIMappingWithUI.put(TAB_KEY_MANAGER, apiAssociatedWithKeyManager);

		for (String api : apiAssociatedWithKeyManager) {
			if (mapApiToTabs.get(api) == null) {
				mapApiToTabs.put(api, new HashSet<String>());
			}
			mapApiToTabs.get(api).add(TAB_KEY_MANAGER);
		}
	}

	private void mapPermissionsWithAPIs() {

		Set<String> apiAssociatedWithPermissions = new HashSet<String>();

		apiAssociatedWithPermissions.add(RangerAPIList.COUNT_X_GROUP_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.COUNT_X_MODULE_DEF);
		apiAssociatedWithPermissions.add(RangerAPIList.COUNT_X_USER_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.CREATE_X_GROUP_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.CREATE_X_MODULE_DEF_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.CREATE_X_USER_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.DELETE_X_GROUP_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.DELETE_X_MODULE_DEF_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.DELETE_X_USER_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.GET_X_GROUP_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.GET_X_MODULE_DEF_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.GET_X_USER_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.SEARCH_X_GROUP_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.SEARCH_X_MODULE_DEF);
		apiAssociatedWithPermissions.add(RangerAPIList.SEARCH_X_USER_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.UPDATE_X_GROUP_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.UPDATE_X_MODULE_DEF_PERMISSION);
		apiAssociatedWithPermissions.add(RangerAPIList.UPDATE_X_USER_PERMISSION);

		rangerAPIMappingWithUI.put(TAB_PERMISSIONS, apiAssociatedWithPermissions);

		for (String api : apiAssociatedWithPermissions) {
			if (mapApiToTabs.get(api) == null) {
				mapApiToTabs.put(api, new HashSet<String>());
			}
			mapApiToTabs.get(api).add(TAB_PERMISSIONS);
		}
	}

	private void mapUGWithAPIs() {
		Set<String> apiAssociatedWithUserAndGroups = new HashSet<String>();

		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_USER_PROFILE_FOR_USER);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SEARCH_USERS);

		apiAssociatedWithUserAndGroups.add(RangerAPIList.COUNT_X_AUDIT_MAPS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.COUNT_X_GROUP_GROUPS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.COUNT_X_GROUPS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.COUNT_X_GROUP_USERS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.COUNT_X_PERM_MAPS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.COUNT_X_USERS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.CREATE_X_AUDIT_MAP);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.CREATE_X_PERM_MAP);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.DELETE_X_AUDIT_MAP);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.DELETE_X_PERM_MAP);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_X_AUDIT_MAP);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_X_GROUP);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_X_GROUP_BY_GROUP_NAME);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_X_GROUP_GROUP);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_X_GROUP_USER);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_X_GROUP_USERS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_X_PERM_MAP);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_X_USER);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_X_USER_BY_USER_NAME);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_X_USER_GROUPS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.MODIFY_GROUPS_VISIBILITY);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.MODIFY_USER_ACTIVE_STATUS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.MODIFY_USER_VISIBILITY);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SEARCH_X_AUDIT_MAPS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SEARCH_X_GROUP_GROUPS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SEARCH_X_GROUPS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SEARCH_X_GROUP_USERS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SEARCH_X_PERM_MAPS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SEARCH_X_USERS);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SECURE_GET_X_GROUP);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SECURE_GET_X_USER);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.UPDATE_X_AUDIT_MAP);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.UPDATE_X_PERM_MAP);

		apiAssociatedWithUserAndGroups.add(RangerAPIList.CREATE);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.CREATE_DEFAULT_ACCOUNT_USER);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.UPDATE);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SET_USER_ROLES);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.DEACTIVATE_USER);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SET_USER_ROLES_BY_ID);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.SET_USER_ROLES_BY_NAME);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_USER_ROLES_BY_ID);
		apiAssociatedWithUserAndGroups.add(RangerAPIList.GET_USER_ROLES_BY_NAME);

		rangerAPIMappingWithUI.put(TAB_USERS_GROUPS, apiAssociatedWithUserAndGroups);

		for (String api : apiAssociatedWithUserAndGroups) {
			if (mapApiToTabs.get(api) == null) {
				mapApiToTabs.put(api, new HashSet<String>());
			}
			mapApiToTabs.get(api).add(TAB_USERS_GROUPS);
		}
	}

	private void mapAuditWithAPIs() {

		Set<String> apiAssociatedWithAudit = new HashSet<String>();

		apiAssociatedWithAudit.add(RangerAPIList.COUNT_X_ASSETS);
		apiAssociatedWithAudit.add(RangerAPIList.GET_X_ASSET);
		apiAssociatedWithAudit.add(RangerAPIList.SEARCH_X_ASSETS);

		apiAssociatedWithAudit.add(RangerAPIList.COUNT_SERVICES);
		apiAssociatedWithAudit.add(RangerAPIList.GET_POLICY_FOR_VERSION_NO);
		apiAssociatedWithAudit.add(RangerAPIList.GET_POLICY_FROM_EVENT_TIME);
		apiAssociatedWithAudit.add(RangerAPIList.GET_POLICY_VERSION_LIST);
		apiAssociatedWithAudit.add(RangerAPIList.GET_PLUGINS_INFO);
		apiAssociatedWithAudit.add(RangerAPIList.GET_SERVICE);
		apiAssociatedWithAudit.add(RangerAPIList.GET_SERVICE_BY_NAME);
		apiAssociatedWithAudit.add(RangerAPIList.GET_SERVICE_DEF);
		apiAssociatedWithAudit.add(RangerAPIList.GET_SERVICE_DEF_BY_NAME);
		apiAssociatedWithAudit.add(RangerAPIList.GET_SERVICE_DEFS);
		apiAssociatedWithAudit.add(RangerAPIList.GET_SERVICES);
		apiAssociatedWithAudit.add(RangerAPIList.LOOKUP_RESOURCE);

		apiAssociatedWithAudit.add(RangerAPIList.GET_USER_PROFILE_FOR_USER);
		apiAssociatedWithAudit.add(RangerAPIList.SEARCH_USERS);

		apiAssociatedWithAudit.add(RangerAPIList.COUNT_X_AUDIT_MAPS);
		apiAssociatedWithAudit.add(RangerAPIList.COUNT_X_GROUP_GROUPS);
		apiAssociatedWithAudit.add(RangerAPIList.COUNT_X_GROUPS);
		apiAssociatedWithAudit.add(RangerAPIList.COUNT_X_GROUP_USERS);
		apiAssociatedWithAudit.add(RangerAPIList.COUNT_X_PERM_MAPS);
		apiAssociatedWithAudit.add(RangerAPIList.COUNT_X_USERS);
		apiAssociatedWithAudit.add(RangerAPIList.GET_X_AUDIT_MAP);
		apiAssociatedWithAudit.add(RangerAPIList.GET_X_GROUP);
		apiAssociatedWithAudit.add(RangerAPIList.GET_X_GROUP_BY_GROUP_NAME);
		apiAssociatedWithAudit.add(RangerAPIList.GET_X_GROUP_GROUP);
		apiAssociatedWithAudit.add(RangerAPIList.GET_X_GROUP_USER);
		apiAssociatedWithAudit.add(RangerAPIList.GET_X_GROUP_USERS);
		apiAssociatedWithAudit.add(RangerAPIList.GET_X_PERM_MAP);
		apiAssociatedWithAudit.add(RangerAPIList.GET_X_USER);
		apiAssociatedWithAudit.add(RangerAPIList.GET_X_USER_BY_USER_NAME);
		apiAssociatedWithAudit.add(RangerAPIList.GET_X_USER_GROUPS);
		apiAssociatedWithAudit.add(RangerAPIList.SEARCH_X_AUDIT_MAPS);
		apiAssociatedWithAudit.add(RangerAPIList.SEARCH_X_GROUP_GROUPS);
		apiAssociatedWithAudit.add(RangerAPIList.SEARCH_X_GROUPS);
		apiAssociatedWithAudit.add(RangerAPIList.SEARCH_X_GROUP_USERS);
		apiAssociatedWithAudit.add(RangerAPIList.SEARCH_X_PERM_MAPS);
		apiAssociatedWithAudit.add(RangerAPIList.SEARCH_X_USERS);
		apiAssociatedWithAudit.add(RangerAPIList.SECURE_GET_X_GROUP);
		apiAssociatedWithAudit.add(RangerAPIList.SECURE_GET_X_USER);

		apiAssociatedWithAudit.add(RangerAPIList.GET_X_TRX_LOG);
		apiAssociatedWithAudit.add(RangerAPIList.CREATE_X_TRX_LOG);
		apiAssociatedWithAudit.add(RangerAPIList.UPDATE_X_TRX_LOG);
		apiAssociatedWithAudit.add(RangerAPIList.DELETE_X_TRX_LOG);
		apiAssociatedWithAudit.add(RangerAPIList.SEARCH_X_TRX_LOG);
		apiAssociatedWithAudit.add(RangerAPIList.COUNT_X_TRX_LOGS);
		apiAssociatedWithAudit.add(RangerAPIList.SEARCH_X_ACCESS_AUDITS);
		apiAssociatedWithAudit.add(RangerAPIList.COUNT_X_ACCESS_AUDITS);
		apiAssociatedWithAudit.add(RangerAPIList.SEARCH_X_POLICY_EXPORT_AUDITS);
		apiAssociatedWithAudit.add(RangerAPIList.GET_REPORT_LOGS);
		apiAssociatedWithAudit.add(RangerAPIList.GET_TRANSACTION_REPORT);
		apiAssociatedWithAudit.add(RangerAPIList.GET_ACCESS_LOGS);
		apiAssociatedWithAudit.add(RangerAPIList.GET_AUTH_SESSION);
		apiAssociatedWithAudit.add(RangerAPIList.GET_AUTH_SESSIONS);

		rangerAPIMappingWithUI.put(TAB_AUDIT, apiAssociatedWithAudit);

		for (String api : apiAssociatedWithAudit) {
			if (mapApiToTabs.get(api) == null) {
				mapApiToTabs.put(api, new HashSet<String>());
			}
			mapApiToTabs.get(api).add(TAB_AUDIT);
		}
	}

	private void mapResourceBasedPoliciesWithAPIs() {
		Set<String> apiAssociatedWithRBPolicies = new HashSet<String>();

		apiAssociatedWithRBPolicies.add(RangerAPIList.COUNT_X_ASSETS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.CREATE_X_ASSET);
		apiAssociatedWithRBPolicies.add(RangerAPIList.DELETE_X_ASSET);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_X_ASSET);
		apiAssociatedWithRBPolicies.add(RangerAPIList.SEARCH_X_ASSETS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.TEST_CONFIG);
		apiAssociatedWithRBPolicies.add(RangerAPIList.UPDATE_X_ASSET);

		apiAssociatedWithRBPolicies.add(RangerAPIList.COUNT_SERVICES);
		apiAssociatedWithRBPolicies.add(RangerAPIList.CREATE_SERVICE);
		apiAssociatedWithRBPolicies.add(RangerAPIList.CREATE_SERVICE_DEF);
		apiAssociatedWithRBPolicies.add(RangerAPIList.DELETE_SERVICE);
		apiAssociatedWithRBPolicies.add(RangerAPIList.DELETE_SERVICE_DEF);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_POLICY_FOR_VERSION_NO);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_POLICY_FROM_EVENT_TIME);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_POLICY_VERSION_LIST);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_SERVICE);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_SERVICE_BY_NAME);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_SERVICE_DEF);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_SERVICE_DEF_BY_NAME);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_SERVICE_DEFS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_SERVICES);
		apiAssociatedWithRBPolicies.add(RangerAPIList.LOOKUP_RESOURCE);
		apiAssociatedWithRBPolicies.add(RangerAPIList.UPDATE_SERVICE);
		apiAssociatedWithRBPolicies.add(RangerAPIList.UPDATE_SERVICE_DEF);
		apiAssociatedWithRBPolicies.add(RangerAPIList.VALIDATE_CONFIG);

		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_USER_PROFILE_FOR_USER);
		apiAssociatedWithRBPolicies.add(RangerAPIList.SEARCH_USERS);

		apiAssociatedWithRBPolicies.add(RangerAPIList.COUNT_X_AUDIT_MAPS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.COUNT_X_GROUP_GROUPS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.COUNT_X_GROUPS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.COUNT_X_GROUP_USERS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.COUNT_X_PERM_MAPS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.COUNT_X_USERS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.CREATE_X_AUDIT_MAP);
		apiAssociatedWithRBPolicies.add(RangerAPIList.CREATE_X_PERM_MAP);
		apiAssociatedWithRBPolicies.add(RangerAPIList.DELETE_X_AUDIT_MAP);
		apiAssociatedWithRBPolicies.add(RangerAPIList.DELETE_X_PERM_MAP);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_X_AUDIT_MAP);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_X_GROUP);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_X_GROUP_BY_GROUP_NAME);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_X_GROUP_GROUP);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_X_GROUP_USER);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_X_GROUP_USERS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_X_PERM_MAP);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_X_USER);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_X_USER_BY_USER_NAME);
		apiAssociatedWithRBPolicies.add(RangerAPIList.GET_X_USER_GROUPS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.MODIFY_GROUPS_VISIBILITY);
		apiAssociatedWithRBPolicies.add(RangerAPIList.MODIFY_USER_ACTIVE_STATUS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.MODIFY_USER_VISIBILITY);
		apiAssociatedWithRBPolicies.add(RangerAPIList.SEARCH_X_AUDIT_MAPS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.SEARCH_X_GROUP_GROUPS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.SEARCH_X_GROUPS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.SEARCH_X_GROUP_USERS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.SEARCH_X_PERM_MAPS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.SEARCH_X_USERS);
		apiAssociatedWithRBPolicies.add(RangerAPIList.SECURE_GET_X_GROUP);
		apiAssociatedWithRBPolicies.add(RangerAPIList.SECURE_GET_X_USER);
		apiAssociatedWithRBPolicies.add(RangerAPIList.UPDATE_X_AUDIT_MAP);
		apiAssociatedWithRBPolicies.add(RangerAPIList.UPDATE_X_PERM_MAP);

		apiAssociatedWithRBPolicies.add(RangerAPIList.CREATE);
		apiAssociatedWithRBPolicies.add(RangerAPIList.CREATE_DEFAULT_ACCOUNT_USER);
		apiAssociatedWithRBPolicies.add(RangerAPIList.UPDATE);
		apiAssociatedWithRBPolicies.add(RangerAPIList.SET_USER_ROLES);
		apiAssociatedWithRBPolicies.add(RangerAPIList.DEACTIVATE_USER);

		rangerAPIMappingWithUI.put(TAB_RESOURCE_BASED_POLICIES, apiAssociatedWithRBPolicies);

		for (String api : apiAssociatedWithRBPolicies) {
			if (mapApiToTabs.get(api) == null) {
				mapApiToTabs.put(api, new HashSet<String>());
			}
			mapApiToTabs.get(api).add(TAB_RESOURCE_BASED_POLICIES);
		}
	}

	// * Utility methods starts from here, to retrieve API-UItab mapping information *

	public Set<String> getAvailableUITabs() {
		if (CollectionUtils.isEmpty(tabList)) {
			populateAvailableUITabs();
		}
		return tabList;
	}

	/**
	 * @param apiName
	 * @return
	 *
	 * @Note: apiName being passed to this function should strictly follow this format: {ClassName}.{apiMethodName} and also API should be listed into
	 *        RangerAPIList and should be mapped properly with UI tabs in the current class.
	 */
	public Set<String> getAssociatedTabsWithAPI(String apiName) {
		Set<String> associatedTabs = mapApiToTabs.get(apiName);
		return associatedTabs;
	}
}
