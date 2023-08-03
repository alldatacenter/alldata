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

 package org.apache.ranger.db;

/**
 *
 */

import javax.persistence.EntityManager;

public abstract class RangerDaoManagerBase {

	abstract public EntityManager getEntityManager();

	public RangerDaoManagerBase() {
	}

	public XXDBBaseDao getXXDBBase() {
		return new XXDBBaseDao(this);
	}

	public XXAuthSessionDao getXXAuthSession() {
		return new XXAuthSessionDao(this);
	}

	public XXPortalUserDao getXXPortalUser() {
		return new XXPortalUserDao(this);
	}

	public XXPortalUserRoleDao getXXPortalUserRole() {
		return new XXPortalUserRoleDao(this);
	}

	public XXAssetDao getXXAsset() {
		return new XXAssetDao(this);
	}

	public XXResourceDao getXXResource() {
		return new XXResourceDao(this);
	}

	public XXCredentialStoreDao getXXCredentialStore() {
		return new XXCredentialStoreDao(this);
	}

	public XXGroupDao getXXGroup() {
		return new XXGroupDao(this);
	}

	public XXUserDao getXXUser() {
		return new XXUserDao(this);
	}

	public XXGroupUserDao getXXGroupUser() {
		return new XXGroupUserDao(this);
	}

	public XXGroupGroupDao getXXGroupGroup() {
		return new XXGroupGroupDao(this);
	}

	public XXPermMapDao getXXPermMap() {
		return new XXPermMapDao(this);
	}

	public XXAuditMapDao getXXAuditMap() {
		return new XXAuditMapDao(this);
	}

	public XXPolicyExportAuditDao getXXPolicyExportAudit() {
		return new XXPolicyExportAuditDao(this);
	}

	public XXTrxLogDao getXXTrxLog() {
		return new XXTrxLogDao(this);
	}

	public XXAccessAuditDao getXXAccessAudit() {
		//Load appropriate class based on audit store
		//TODO: Need to fix this, currently hard coding Solr
		
		return new XXAccessAuditDao(this);
	}

	public XXPolicyDao getXXPolicy() {
		return new XXPolicyDao(this);
	}

	public XXServiceDao getXXService() {
		return new XXServiceDao(this);
	}

	public XXPolicyItemDao getXXPolicyItem() {
		return new XXPolicyItemDao(this);
	}

	public XXServiceDefDao getXXServiceDef() {
		return new XXServiceDefDao(this);
	}

	public XXServiceConfigDefDao getXXServiceConfigDef() {
		return new XXServiceConfigDefDao(this);
	}

	public XXResourceDefDao getXXResourceDef() {
		return new XXResourceDefDao(this);
	}

        public XXPolicyLabelDao getXXPolicyLabels() {
                return new XXPolicyLabelDao(this);
        }

        public XXPolicyLabelMapDao getXXPolicyLabelMap() {
                return new XXPolicyLabelMapDao(this);
        }

	public XXAccessTypeDefDao getXXAccessTypeDef() {
		return new XXAccessTypeDefDao(this);
	}

	public XXAccessTypeDefGrantsDao getXXAccessTypeDefGrants() {
		return new XXAccessTypeDefGrantsDao(this);
	}

	public XXPolicyConditionDefDao getXXPolicyConditionDef() {
		return new XXPolicyConditionDefDao(this);
	}

	public XXContextEnricherDefDao getXXContextEnricherDef() {
		return new XXContextEnricherDefDao(this);
	}

	public XXEnumDefDao getXXEnumDef() {
		return new XXEnumDefDao(this);
	}

	public XXEnumElementDefDao getXXEnumElementDef() {
		return new XXEnumElementDefDao(this);
	}
	
	public XXServiceConfigMapDao getXXServiceConfigMap() {
		return new XXServiceConfigMapDao(this);
	}
	
	public XXPolicyResourceDao getXXPolicyResource() {
		return new XXPolicyResourceDao(this);
	}
	
	public XXPolicyResourceMapDao getXXPolicyResourceMap() {
		return new XXPolicyResourceMapDao(this);
	}
	
	public XXPolicyItemAccessDao getXXPolicyItemAccess() {
		return new XXPolicyItemAccessDao(this);
	}

	public XXPolicyItemConditionDao getXXPolicyItemCondition() {
		return new XXPolicyItemConditionDao(this);
	}
	
	public XXPolicyItemUserPermDao getXXPolicyItemUserPerm() {
		return new XXPolicyItemUserPermDao(this);
	}
	
	public XXPolicyItemGroupPermDao getXXPolicyItemGroupPerm() {
		return new XXPolicyItemGroupPermDao(this);
	}

	public XXDataHistDao getXXDataHist() {
		return new XXDataHistDao(this);
	}
	
	public XXPolicyWithAssignedIdDao getXXPolicyWithAssignedId() {
		return new XXPolicyWithAssignedIdDao(this);
	}
	
	public XXServiceWithAssignedIdDao getXXServiceWithAssignedId() {
		return new XXServiceWithAssignedIdDao(this);
	}

	public XXModuleDefDao getXXModuleDef(){
		return new XXModuleDefDao(this);
	}

	public XXUserPermissionDao getXXUserPermission(){
		return new XXUserPermissionDao(this);
	}

	public XXGroupPermissionDao getXXGroupPermission(){
		return new XXGroupPermissionDao(this);
	}
	
	public XXServiceDefWithAssignedIdDao getXXServiceDefWithAssignedId() {
		return new XXServiceDefWithAssignedIdDao(this);
	}

	public XXTagDefDao getXXTagDef() {
		return new XXTagDefDao(this);
	}

	public XXTagAttributeDefDao getXXTagAttributeDef() {
		return new XXTagAttributeDefDao(this);
	}

	public XXServiceResourceDao getXXServiceResource() {
		return new XXServiceResourceDao(this);
	}

	public XXServiceResourceElementDao getXXServiceResourceElement() {
		return new XXServiceResourceElementDao(this);
	}

	public XXServiceResourceElementValueDao getXXServiceResourceElementValue() {
		return new XXServiceResourceElementValueDao(this);
	}

	public XXTagDao getXXTag() {
		return new XXTagDao(this);
	}

	public XXTagAttributeDao getXXTagAttribute() {
		return new XXTagAttributeDao(this);
	}

	public XXTagResourceMapDao getXXTagResourceMap() {
		return new XXTagResourceMapDao(this);
	}

	public XXDataMaskTypeDefDao getXXDataMaskTypeDef() { return new XXDataMaskTypeDefDao(this); }

	public XXPolicyItemDataMaskInfoDao getXXPolicyItemDataMaskInfo() {
		return new XXPolicyItemDataMaskInfoDao(this);
	}

	public XXPolicyItemRowFilterInfoDao getXXPolicyItemRowFilterInfo() {
		return new XXPolicyItemRowFilterInfoDao(this);
	}

	public XXServiceVersionInfoDao getXXServiceVersionInfo() {
		return new XXServiceVersionInfoDao(this);
	}

	public XXPluginInfoDao getXXPluginInfo() {
		return new XXPluginInfoDao(this);
	}

	public XXUgsyncAuditInfoDao getXXUgsyncAuditInfo() {
		return new XXUgsyncAuditInfoDao(this);
	}

	public XXPolicyRefConditionDao getXXPolicyRefCondition() {
		return new XXPolicyRefConditionDao(this);
	}

	public XXPolicyRefGroupDao getXXPolicyRefGroup() {
		return new XXPolicyRefGroupDao(this);
	}

	public XXPolicyRefDataMaskTypeDao getXXPolicyRefDataMaskType() {
		return new XXPolicyRefDataMaskTypeDao(this);
	}

	public XXPolicyRefResourceDao getXXPolicyRefResource() {
		return new XXPolicyRefResourceDao(this);
	}

	public XXPolicyRefUserDao getXXPolicyRefUser() {
		return new XXPolicyRefUserDao(this);
	}

	public XXPolicyRefAccessTypeDao getXXPolicyRefAccessType() {
		return new XXPolicyRefAccessTypeDao(this);
	}

	public XXSecurityZoneDao getXXSecurityZoneDao() { return new XXSecurityZoneDao(this); }

	public XXSecurityZoneRefServiceDao getXXSecurityZoneRefService() { return new XXSecurityZoneRefServiceDao(this); }

	public XXSecurityZoneRefTagServiceDao getXXSecurityZoneRefTagService() { return new XXSecurityZoneRefTagServiceDao(this); }

	public XXSecurityZoneRefResourceDao getXXSecurityZoneRefResource() { return new XXSecurityZoneRefResourceDao(this); }

	public XXSecurityZoneRefUserDao getXXSecurityZoneRefUser() { return new XXSecurityZoneRefUserDao(this); }

	public XXSecurityZoneRefGroupDao getXXSecurityZoneRefGroup() { return new XXSecurityZoneRefGroupDao(this); }

	public XXGlobalStateDao getXXGlobalState() { return new XXGlobalStateDao(this); }

	public XXPolicyChangeLogDao getXXPolicyChangeLog() { return new XXPolicyChangeLogDao(this); }

	public XXRoleDao getXXRole() { return new XXRoleDao(this); }

	public XXPolicyRefRoleDao getXXPolicyRefRole() { return new XXPolicyRefRoleDao(this); }

	public XXRoleRefUserDao getXXRoleRefUser() { return new XXRoleRefUserDao(this); }

	public XXRoleRefGroupDao getXXRoleRefGroup() { return new XXRoleRefGroupDao(this); }

	public XXRoleRefRoleDao getXXRoleRefRole() { return new XXRoleRefRoleDao(this); }

	public XXTagChangeLogDao getXXTagChangeLog() { return new XXTagChangeLogDao(this); }

	public XXRMSMappingProviderDao getXXRMSMappingProvider() { return new XXRMSMappingProviderDao(this); }
	public XXRMSNotificationDao getXXRMSNotification() { return new XXRMSNotificationDao(this); }
	public XXRMSServiceResourceDao getXXRMSServiceResource() { return new XXRMSServiceResourceDao(this); }
	public XXRMSResourceMappingDao getXXRMSResourceMapping() { return new XXRMSResourceMappingDao(this); }


}

