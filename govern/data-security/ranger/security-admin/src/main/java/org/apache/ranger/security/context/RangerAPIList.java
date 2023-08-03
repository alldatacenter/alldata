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

/**
 * This class holds list of APIs available in the system.
 * This Class needs to be updated when writing new API in any of the REST.
 */
public class RangerAPIList {

	/**
	 * List of APIs for AssetREST
	 */
	public static final String GET_X_ASSET = "AssetREST.getXAsset";
	public static final String CREATE_X_ASSET = "AssetREST.createXAsset";
	public static final String UPDATE_X_ASSET = "AssetREST.updateXAsset";
	public static final String DELETE_X_ASSET = "AssetREST.deleteXAsset";
	public static final String TEST_CONFIG = "AssetREST.testConfig";
	public static final String SEARCH_X_ASSETS = "AssetREST.searchXAssets";
	public static final String COUNT_X_ASSETS = "AssetREST.countXAssets";
	public static final String GET_X_RESOURCE = "AssetREST.getXResource";
	public static final String CREATE_X_RESOURCE = "AssetREST.createXResource";
	public static final String UPDATE_X_RESOURCE = "AssetREST.updateXResource";
	public static final String DELETE_X_RESOURCE = "AssetREST.deleteXResource";
	public static final String SEARCH_X_RESOURCES = "AssetREST.searchXResources";
	public static final String COUNT_X_RESOURCES = "AssetREST.countXResources";
	public static final String GET_X_CRED_STORE = "AssetREST.getXCredentialStore";
	public static final String CREATE_X_CRED_STORE = "AssetREST.createXCredentialStore";
	public static final String UPDATE_X_CRED_STORE = "AssetREST.updateXCredentialStore";
	public static final String DELETE_X_CRED_STORE = "AssetREST.deleteXCredentialStore";
	public static final String SEARCH_X_CRED_STORE = "AssetREST.searchXCredentialStores";
	public static final String COUNT_X_CRED_STORE = "AssetREST.countXCredentialStores";
	public static final String GET_X_RESOURCE_FILE = "AssetREST.getXResourceFile";
	public static final String GET_RESOURCE_JSON = "AssetREST.getResourceJSON";
	public static final String SEARCH_X_POLICY_EXPORT_AUDITS = "AssetREST.searchXPolicyExportAudits";
	public static final String GET_REPORT_LOGS = "AssetREST.getReportLogs";
	public static final String GET_TRANSACTION_REPORT = "AssetREST.getTransactionReport";
	public static final String GET_ACCESS_LOGS = "AssetREST.getAccessLogs";
	public static final String GRANT_PERMISSION = "AssetREST.grantPermission";
	public static final String REVOKE_PERMISSION = "AssetREST.revokePermission";
	public static final String GET_UGSYNC_AUDITS = "AssetREST.getUgsyncAudits";
	public static final String GET_UGSYNC_AUDITS_BY_SYNCSOURCE = "AssetREST.getUgsyncAuditsBySyncSource";

	/**
	 * List of APIs for ServiceREST
	 */
	public static final String CREATE_SERVICE_DEF = "ServiceREST.createServiceDef";
	public static final String UPDATE_SERVICE_DEF = "ServiceREST.updateServiceDef";
	public static final String DELETE_SERVICE_DEF = "ServiceREST.deleteServiceDef";
	public static final String GET_SERVICE_DEF = "ServiceREST.getServiceDef";
	public static final String GET_SERVICE_DEF_BY_NAME = "ServiceREST.getServiceDefByName";
	public static final String GET_SERVICE_DEFS = "ServiceREST.getServiceDefs";
	public static final String CREATE_SERVICE = "ServiceREST.createService";
	public static final String UPDATE_SERVICE = "ServiceREST.updateService";
	public static final String DELETE_SERVICE = "ServiceREST.deleteService";
	public static final String GET_SERVICE = "ServiceREST.getService";
	public static final String GET_SERVICE_BY_NAME = "ServiceREST.getServiceByName";
	public static final String GET_SERVICES = "ServiceREST.getServices";
	public static final String COUNT_SERVICES = "ServiceREST.countServices";
	public static final String VALIDATE_CONFIG = "ServiceREST.validateConfig";
	public static final String LOOKUP_RESOURCE = "ServiceREST.lookupResource";
	public static final String GRANT_ACCESS = "ServiceREST.grantAccess";
	public static final String REVOKE_ACCESS = "ServiceREST.revokeAccess";
	public static final String CREATE_POLICY = "ServiceREST.createPolicy";
	public static final String UPDATE_POLICY = "ServiceREST.updatePolicy";
	public static final String DELETE_POLICY = "ServiceREST.deletePolicy";
	public static final String GET_POLICY = "ServiceREST.getPolicy";
	public static final String GET_POLICIES = "ServiceREST.getPolicies";
	public static final String COUNT_POLICIES = "ServiceREST.countPolicies";
	public static final String GET_SERVICE_POLICIES = "ServiceREST.getServicePolicies";
	public static final String GET_SERVICE_POLICIES_BY_NAME = "ServiceREST.getServicePoliciesByName";
	public static final String GET_SERVICE_POLICIES_IF_UPDATED = "ServiceREST.getServicePoliciesIfUpdated";
	public static final String GET_POLICY_FROM_EVENT_TIME = "ServiceREST.getPolicyFromEventTime";
	public static final String GET_POLICY_VERSION_LIST = "ServiceREST.getPolicyVersionList";
	public static final String GET_POLICY_FOR_VERSION_NO = "ServiceREST.getPolicyForVersionNumber";
	public static final String GET_PLUGINS_INFO = "ServiceREST.getPluginsInfo";
	public static final String GET_METRICS_BY_TYPE = "ServiceREST.getMetricByType";
	public static final String DELETE_CLUSTER_SERVICES = "ServiceREST.deleteClusterServices";

	/**
	 * List of APIs for UserREST
	 */
	public static final String SEARCH_USERS = "UserREST.searchUsers";
	public static final String GET_USER_PROFILE_FOR_USER = "UserREST.getUserProfileForUser";
	public static final String CREATE = "UserREST.create";
	public static final String CREATE_DEFAULT_ACCOUNT_USER = "UserREST.createDefaultAccountUser";
	public static final String UPDATE = "UserREST.update";
	public static final String SET_USER_ROLES = "UserREST.setUserRoles";
	public static final String DEACTIVATE_USER = "UserREST.deactivateUser";
	public static final String GET_USER_PROFILE = "UserREST.getUserProfile";
	public static final String CHANGE_PASSWORD = "UserREST.changePassword";
	public static final String CHANGE_EMAIL_ADDRESS = "UserREST.changeEmailAddress";

	/**
	 * List of APIs for XAuditREST
	 */
	public static final String GET_X_TRX_LOG = "XAuditREST.getXTrxLog";
	public static final String CREATE_X_TRX_LOG = "XAuditREST.createXTrxLog";
	public static final String UPDATE_X_TRX_LOG = "XAuditREST.updateXTrxLog";
	public static final String DELETE_X_TRX_LOG = "XAuditREST.deleteXTrxLog";
	public static final String SEARCH_X_TRX_LOG = "XAuditREST.searchXTrxLogs";
	public static final String COUNT_X_TRX_LOGS = "XAuditREST.countXTrxLogs";
	public static final String SEARCH_X_ACCESS_AUDITS = "XAuditREST.searchXAccessAudits";
	public static final String COUNT_X_ACCESS_AUDITS = "XAuditREST.countXAccessAudits";

	/**
	 * List of APIs for XKeyREST
	 */
	public static final String SEARCH_KEYS = "XKeyREST.searchKeys";
	public static final String ROLLOVER_KEYS = "XKeyREST.rolloverKey";
	public static final String DELETE_KEY = "XKeyREST.deleteKey";
	public static final String CREATE_KEY = "XKeyREST.createKey";
	public static final String GET_KEY = "XKeyREST.getKey";

	/**
	 * List of APIs for XUserREST
	 */
	public static final String GET_X_GROUP = "XUserREST.getXGroup";
	public static final String SECURE_GET_X_GROUP = "XUserREST.secureGetXGroup";
	public static final String CREATE_X_GROUP = "XUserREST.createXGroup";
	public static final String SECURE_CREATE_X_GROUP = "XUserREST.secureCreateXGroup";
	public static final String UPDATE_X_GROUP = "XUserREST.updateXGroup";
	public static final String SECURE_UPDATE_X_GROUP = "XUserREST.secureUpdateXGroup";
	public static final String MODIFY_GROUPS_VISIBILITY = "XUserREST.modifyGroupsVisibility";
	public static final String DELETE_X_GROUP = "XUserREST.deleteXGroup";
	public static final String SEARCH_X_GROUPS = "XUserREST.searchXGroups";
	public static final String COUNT_X_GROUPS = "XUserREST.countXGroups";
	public static final String GET_X_USER = "XUserREST.getXUser";
	public static final String SECURE_GET_X_USER = "XUserREST.secureGetXUser";
	public static final String CREATE_X_USER = "XUserREST.createXUser";
	public static final String CREATE_X_USER_GROUP_FROM_MAP = "XUserREST.createXUserGroupFromMap";
	public static final String SECURE_CREATE_X_USER = "XUserREST.secureCreateXUser";
	public static final String UPDATE_X_USER = "XUserREST.updateXUser";
	public static final String SECURE_UPDATE_X_USER = "XUserREST.secureUpdateXUser";
	public static final String MODIFY_USER_VISIBILITY = "XUserREST.modifyUserVisibility";
	public static final String DELETE_X_USER = "XUserREST.deleteXUser";
	public static final String SEARCH_X_USERS = "XUserREST.searchXUsers";
	public static final String GET_USERS_LOOKUP = "XUserREST.getUsersLookup";
	public static final String GET_GROUPS_LOOKUP = "XUserREST.getGroupsLookup";
	public static final String COUNT_X_USERS = "XUserREST.countXUsers";
	public static final String GET_X_GROUP_USER = "XUserREST.getXGroupUser";
	public static final String CREATE_X_GROUP_USER = "XUserREST.createXGroupUser";
	public static final String UPDATE_X_GROUP_USER = "XUserREST.updateXGroupUser";
	public static final String DELETE_X_GROUP_USER = "XUserREST.deleteXGroupUser";
	public static final String SEARCH_X_GROUP_USERS = "XUserREST.searchXGroupUsers";
	public static final String GET_X_GROUP_USERS_BY_GROUP_NAME = "XUserREST.getXGroupUsersByGroupName";
	public static final String COUNT_X_GROUP_USERS = "XUserREST.countXGroupUsers";
	public static final String GET_X_GROUP_GROUP = "XUserREST.getXGroupGroup";
	public static final String CREATE_X_GROUP_GROUP = "XUserREST.createXGroupGroup";
	public static final String UPDATE_X_GROUP_GROUP = "XUserREST.updateXGroupGroup";
	public static final String DELETE_X_GROUP_GROUP = "XUserREST.deleteXGroupGroup";
	public static final String SEARCH_X_GROUP_GROUPS = "XUserREST.searchXGroupGroups";
	public static final String COUNT_X_GROUP_GROUPS = "XUserREST.countXGroupGroups";
	public static final String GET_X_PERM_MAP = "XUserREST.getXPermMap";
	public static final String CREATE_X_PERM_MAP = "XUserREST.createXPermMap";
	public static final String UPDATE_X_PERM_MAP = "XUserREST.updateXPermMap";
	public static final String DELETE_X_PERM_MAP = "XUserREST.deleteXPermMap";
	public static final String SEARCH_X_PERM_MAPS = "XUserREST.searchXPermMaps";
	public static final String COUNT_X_PERM_MAPS = "XUserREST.countXPermMaps";
	public static final String GET_X_AUDIT_MAP = "XUserREST.getXAuditMap";
	public static final String CREATE_X_AUDIT_MAP = "XUserREST.createXAuditMap";
	public static final String UPDATE_X_AUDIT_MAP = "XUserREST.updateXAuditMap";
	public static final String DELETE_X_AUDIT_MAP = "XUserREST.deleteXAuditMap";
	public static final String SEARCH_X_AUDIT_MAPS = "XUserREST.searchXAuditMaps";
	public static final String COUNT_X_AUDIT_MAPS = "XUserREST.countXAuditMaps";
	public static final String GET_X_USER_BY_USER_NAME = "XUserREST.getXUserByUserName";
	public static final String GET_X_GROUP_BY_GROUP_NAME = "XUserREST.getXGroupByGroupName";
	public static final String DELETE_X_USER_BY_USER_NAME = "XUserREST.deleteXUserByUserName";
	public static final String DELETE_X_GROUP_BY_GROUP_NAME = "XUserREST.deleteXGroupByGroupName";
	public static final String DELETE_X_GROUP_AND_X_USER = "XUserREST.deleteXGroupAndXUser";
	public static final String GET_X_USER_GROUPS = "XUserREST.getXUserGroups";
	public static final String GET_X_GROUP_USERS = "XUserREST.getXGroupUsers";
	public static final String GET_AUTH_SESSIONS = "XUserREST.getAuthSessions";
	public static final String GET_AUTH_SESSION = "XUserREST.getAuthSession";
	public static final String CREATE_X_MODULE_DEF_PERMISSION = "XUserREST.createXModuleDefPermission";
	public static final String GET_X_MODULE_DEF_PERMISSION = "XUserREST.getXModuleDefPermission";
	public static final String UPDATE_X_MODULE_DEF_PERMISSION = "XUserREST.updateXModuleDefPermission";
	public static final String DELETE_X_MODULE_DEF_PERMISSION = "XUserREST.deleteXModuleDefPermission";
	public static final String SEARCH_X_MODULE_DEF = "XUserREST.searchXModuleDef";
	public static final String COUNT_X_MODULE_DEF = "XUserREST.countXModuleDef";
	public static final String CREATE_X_USER_PERMISSION = "XUserREST.createXUserPermission";
	public static final String GET_X_USER_PERMISSION = "XUserREST.getXUserPermission";
	public static final String UPDATE_X_USER_PERMISSION = "XUserREST.updateXUserPermission";
	public static final String DELETE_X_USER_PERMISSION = "XUserREST.deleteXUserPermission";
	public static final String SEARCH_X_USER_PERMISSION = "XUserREST.searchXUserPermission";
	public static final String COUNT_X_USER_PERMISSION = "XUserREST.countXUserPermission";
	public static final String CREATE_X_GROUP_PERMISSION = "XUserREST.createXGroupPermission";
	public static final String GET_X_GROUP_PERMISSION = "XUserREST.getXGroupPermission";
	public static final String UPDATE_X_GROUP_PERMISSION = "XUserREST.updateXGroupPermission";
	public static final String DELETE_X_GROUP_PERMISSION = "XUserREST.deleteXGroupPermission";
	public static final String SEARCH_X_GROUP_PERMISSION = "XUserREST.searchXGroupPermission";
	public static final String COUNT_X_GROUP_PERMISSION = "XUserREST.countXGroupPermission";
	public static final String MODIFY_USER_ACTIVE_STATUS = "XUserREST.modifyUserActiveStatus";
	public static final String SET_USER_ROLES_BY_ID="XUserREST.setUserRolesByID";
	public static final String SET_USER_ROLES_BY_NAME="XUserREST.setUserRolesByName";
	public static final String GET_USER_ROLES_BY_ID="XUserREST.getUserRolesByID";
	public static final String GET_USER_ROLES_BY_NAME="XUserREST.getUserRolesByName";
}
