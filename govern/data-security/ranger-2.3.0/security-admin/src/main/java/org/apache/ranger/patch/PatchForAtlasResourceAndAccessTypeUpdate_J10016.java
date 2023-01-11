
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
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXAccessTypeDef;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXPolicyItem;
import org.apache.ranger.entity.XXPolicyItemAccess;
import org.apache.ranger.entity.XXPolicyItemGroupPerm;
import org.apache.ranger.entity.XXPolicyResource;
import org.apache.ranger.entity.XXPolicyResourceMap;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicyResourceSignature;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefValidator;
import org.apache.ranger.plugin.model.validation.RangerValidator.Action;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatchForAtlasResourceAndAccessTypeUpdate_J10016 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchForAtlasResourceAndAccessTypeUpdate_J10016.class);
	private static final String RESOURCE_DEF_NAME = "all - relationship-type, end-one-entity-type, end-one-entity-classification, end-one-entity, end-two-entity-type, end-two-entity-classification, end-two-entity";
	private static final List<String> ATLAS_RESOURCES = new ArrayList<>(
			Arrays.asList("relationship-type", "end-one-entity-type", "end-one-entity-classification", "end-one-entity",
					"end-two-entity-type", "end-two-entity-classification", "end-two-entity"));
	private static final List<String> ATLAS_ACCESS_TYPES = new ArrayList<>(
			Arrays.asList("add-relationship", "update-relationship", "remove-relationship"));
	private static final String LOGIN_ID_ADMIN = "admin";
	private static final String GROUP_PUBLIC = "public";

	@Autowired
	RangerDaoManager daoMgr;

	@Autowired
	ServiceDBStore svcDBStore;

	@Autowired
	GUIDUtil guidUtil;

	@Autowired
	JSONUtil jsonUtil;

	@Autowired
	StringUtil stringUtil;

	@Autowired
	RangerValidatorFactory validatorFactory;

	@Autowired
	ServiceDBStore svcStore;

	@Autowired
	RangerPolicyService policyService;

	public static void main(String[] args) {
		logger.info("main()");
		try {
			PatchForAtlasResourceAndAccessTypeUpdate_J10016 loader = (PatchForAtlasResourceAndAccessTypeUpdate_J10016) CLIUtil
					.getBean(PatchForAtlasResourceAndAccessTypeUpdate_J10016.class);
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
		logger.info("==> PatchForAtlasResourceAndAccessTypeUpdate.execLoad()");
		try {
			updateAtlasResourceAndAccessType();
		} catch (Exception e) {
			logger.error("Error whille updateAtlasResourceAndAccessType()data.", e);
		}
		logger.info("<== PatchForAtlasResourceAndAccessTypeUpdate.execLoad()");
	}

	@Override
	public void printStats() {
		logger.info("AtlasResourceAndAccessTypeUpdate data ");
	}

	private void updateAtlasResourceAndAccessType() {
		RangerServiceDef ret = null;
		RangerServiceDef embeddedAtlasServiceDef = null;
		XXServiceDef xXServiceDefObj = null;
		RangerServiceDef dbAtlasServiceDef = null;
		List<RangerServiceDef.RangerResourceDef> embeddedAtlasResourceDefs = null;
		List<RangerServiceDef.RangerAccessTypeDef> embeddedAtlasAccessTypes = null;

		try {
			embeddedAtlasServiceDef = EmbeddedServiceDefsUtil.instance()
					.getEmbeddedServiceDef(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
			if (embeddedAtlasServiceDef != null) {
				xXServiceDefObj = daoMgr.getXXServiceDef()
						.findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
				if (xXServiceDefObj == null) {
					logger.info(xXServiceDefObj + ": service-def not found. No patching is needed");
					return;
				}

				dbAtlasServiceDef = svcDBStore
						.getServiceDefByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
				embeddedAtlasResourceDefs = embeddedAtlasServiceDef.getResources();
				embeddedAtlasAccessTypes = embeddedAtlasServiceDef.getAccessTypes();
				if (checkResourcePresent(embeddedAtlasResourceDefs)) {
					dbAtlasServiceDef.setResources(embeddedAtlasResourceDefs);
					if (checkAccessPresent(embeddedAtlasAccessTypes)) {
						dbAtlasServiceDef.setAccessTypes(embeddedAtlasAccessTypes);
					}
				}

				RangerServiceDefValidator validator = validatorFactory.getServiceDefValidator(svcStore);
				validator.validate(dbAtlasServiceDef, Action.UPDATE);
				ret = svcStore.updateServiceDef(dbAtlasServiceDef);
				if (ret == null) {
					logger.error("Error while updating " + EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME
							+ " service-def");
					throw new RuntimeException("Error while updating "
							+ EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME + " service-def");
				} else {
					createDefaultPolicyToExistingService();
					updatePolicyForRelationshipType();
				}
			}
		} catch (Exception e) {
			logger.error("Error while updating " + EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME + " service-def",e);
		}

	}

	private void createDefaultPolicyToExistingService() {
		logger.info("==> createDefaultPolicyToExistingService ");
		XXPortalUser xxPortalUser = daoMgr.getXXPortalUser().findByLoginId(LOGIN_ID_ADMIN);
		Long currentUserId = xxPortalUser.getId();

		XXServiceDef xXServiceDefObj = daoMgr.getXXServiceDef()
				.findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
		if (xXServiceDefObj == null) {
			logger.debug("ServiceDef not fount with name :" + EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
			return;
		}
		Long xServiceDefId = xXServiceDefObj.getId();
		List<XXService> xxServices = daoMgr.getXXService().findByServiceDefId(xServiceDefId);
		for (XXService xxService : xxServices) {
			List<XXPolicy> xxPolicies = daoMgr.getXXPolicy().findByServiceId(xxService.getId());
			Boolean isPolicyPresent = true;
			for (XXPolicy xxPolicy : xxPolicies) {
				if (!xxPolicy.getName().equalsIgnoreCase(RESOURCE_DEF_NAME)) {
					isPolicyPresent = false;
				} else {
					isPolicyPresent = true;
					break;
				}
			}
			if (!isPolicyPresent) {
				XXPolicy xxPolicy = new XXPolicy();
				xxPolicy.setName(RESOURCE_DEF_NAME);
				xxPolicy.setDescription(RESOURCE_DEF_NAME);
				xxPolicy.setService(xxService.getId());
				xxPolicy.setPolicyPriority(RangerPolicy.POLICY_PRIORITY_NORMAL);
				xxPolicy.setIsAuditEnabled(Boolean.TRUE);
				xxPolicy.setIsEnabled(Boolean.TRUE);
				xxPolicy.setPolicyType(RangerPolicy.POLICY_TYPE_ACCESS);
				xxPolicy.setGuid(guidUtil.genGUID());
				xxPolicy.setAddedByUserId(currentUserId);
				xxPolicy.setUpdatedByUserId(currentUserId);
				RangerPolicy rangerPolicy = new RangerPolicy();
				RangerPolicyResourceSignature resourceSignature = new RangerPolicyResourceSignature(rangerPolicy);
				xxPolicy.setResourceSignature(resourceSignature.getSignature());
				xxPolicy.setZoneId(1L);
				XXPolicy createdPolicy = daoMgr.getXXPolicy().create(xxPolicy);

				XXPolicyItem xxPolicyItem = new XXPolicyItem();
				xxPolicyItem.setIsEnabled(Boolean.TRUE);
				xxPolicyItem.setDelegateAdmin(Boolean.TRUE);
				xxPolicyItem.setItemType(0);
				xxPolicyItem.setOrder(0);
				xxPolicyItem.setAddedByUserId(currentUserId);
				xxPolicyItem.setUpdatedByUserId(currentUserId);
				xxPolicyItem.setPolicyId(createdPolicy.getId());
				XXPolicyItem createdXXPolicyItem = daoMgr.getXXPolicyItem().create(xxPolicyItem);

				List<String> accessTypes = Arrays.asList("add-relationship", "update-relationship",
						"remove-relationship");
				for (int i = 0; i < accessTypes.size(); i++) {
					XXAccessTypeDef xAccTypeDef = daoMgr.getXXAccessTypeDef().findByNameAndServiceId(accessTypes.get(i),
							xxPolicy.getService());
					if (xAccTypeDef == null) {
						throw new RuntimeException(accessTypes.get(i) + ": is not a valid access-type. policy='"
								+ xxPolicy.getName() + "' service='" + xxPolicy.getService() + "'");
					}
					XXPolicyItemAccess xPolItemAcc = new XXPolicyItemAccess();
					xPolItemAcc.setIsAllowed(Boolean.TRUE);
					xPolItemAcc.setType(xAccTypeDef.getId());
					xPolItemAcc.setOrder(i);
					xPolItemAcc.setAddedByUserId(currentUserId);
					xPolItemAcc.setUpdatedByUserId(currentUserId);
					xPolItemAcc.setPolicyitemid(createdXXPolicyItem.getId());
					daoMgr.getXXPolicyItemAccess().create(xPolItemAcc);
				}

				List<String> groups = Arrays.asList(GROUP_PUBLIC);
				for (int i = 0; i < groups.size(); i++) {
					String group = groups.get(i);
					if (StringUtils.isBlank(group)) {
						continue;
					}
					XXGroup xGrp = daoMgr.getXXGroup().findByGroupName(group);
					if (xGrp == null) {
						throw new RuntimeException(group + ": group does not exist. policy='" + xxPolicy.getName()
								+ "' service='" + xxPolicy.getService() + "' group='" + group + "'");
					}
					XXPolicyItemGroupPerm xGrpPerm = new XXPolicyItemGroupPerm();
					xGrpPerm.setGroupId(xGrp.getId());
					xGrpPerm.setPolicyItemId(createdXXPolicyItem.getId());
					xGrpPerm.setOrder(i);
					xGrpPerm.setAddedByUserId(currentUserId);
					xGrpPerm.setUpdatedByUserId(currentUserId);
					daoMgr.getXXPolicyItemGroupPerm().create(xGrpPerm);
				}

				for (int i = 0; i < ATLAS_RESOURCES.size(); i++) {
					XXResourceDef xResDef = daoMgr.getXXResourceDef().findByNameAndPolicyId(ATLAS_RESOURCES.get(i),
							createdPolicy.getId());
					if (xResDef == null) {
						throw new RuntimeException(ATLAS_RESOURCES.get(i) + ": is not a valid resource-type. policy='"
								+ createdPolicy.getName() + "' service='" + createdPolicy.getService() + "'");
					}
					XXPolicyResource xPolRes = new XXPolicyResource();

					xPolRes.setAddedByUserId(currentUserId);
					xPolRes.setUpdatedByUserId(currentUserId);
					xPolRes.setIsExcludes(Boolean.FALSE);
					xPolRes.setIsRecursive(Boolean.FALSE);
					xPolRes.setPolicyId(createdPolicy.getId());
					xPolRes.setResDefId(xResDef.getId());
					xPolRes = daoMgr.getXXPolicyResource().create(xPolRes);

					XXPolicyResourceMap xPolResMap = new XXPolicyResourceMap();
					xPolResMap.setResourceId(xPolRes.getId());
					xPolResMap.setValue("*");
					xPolResMap.setOrder(i);
					xPolResMap.setAddedByUserId(currentUserId);
					xPolResMap.setUpdatedByUserId(currentUserId);
					daoMgr.getXXPolicyResourceMap().create(xPolResMap);
				}
				logger.info("Creating policy for service id : " + xxService.getId());
			}
		}
		logger.info("<== createDefaultPolicyToExistingService ");
	}

	private boolean checkResourcePresent(List<RangerServiceDef.RangerResourceDef> resourceDefs) {
		boolean ret = false;
		for (RangerServiceDef.RangerResourceDef resourceDef : resourceDefs) {
			if (ATLAS_RESOURCES.contains(resourceDef.getName())) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	private boolean checkAccessPresent(List<RangerAccessTypeDef> embeddedAtlasAccessTypes) {
		boolean ret = false;
		for (RangerServiceDef.RangerAccessTypeDef accessDef : embeddedAtlasAccessTypes) {
			if (ATLAS_ACCESS_TYPES.contains(accessDef.getName())) {
				ret = true;
				break;
			}
		}
		return ret;
	}

	private void updatePolicyForRelationshipType() {
		logger.info("===> updatePolicyForRelationshipType ");
		XXPortalUser xxPortalUser = daoMgr.getXXPortalUser().findByLoginId(LOGIN_ID_ADMIN);
		Long currentUserId = xxPortalUser.getId();
		XXServiceDef xXServiceDefObj = daoMgr.getXXServiceDef()
				.findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
		if (xXServiceDefObj == null) {
			logger.debug(
					"xXServiceDefObj not found with name : " + EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
			return;
		}
		Long xServiceDefId = xXServiceDefObj.getId();
		XXResourceDef xxResourceDef = daoMgr.getXXResourceDef().findByNameAndServiceDefId(RESOURCE_DEF_NAME,
				xServiceDefId);
		List<XXPolicyResource> policyResources = daoMgr.getXXPolicyResource().findByResDefId(xxResourceDef.getId());
		for (XXPolicyResource xxPolicyResource : policyResources) {
			XXPolicy xxPolicy = daoMgr.getXXPolicy().getById(xxPolicyResource.getPolicyid());
			List<XXPolicyItem> xxPolicyItems = daoMgr.getXXPolicyItem().findByPolicyId(xxPolicy.getId());
			for (XXPolicyItem xxPolicyItem : xxPolicyItems) {
				XXGroup xxGroup = daoMgr.getXXGroup().findByGroupName(GROUP_PUBLIC);
				if (xxGroup == null) {
					logger.error("Group name 'public' not found in database");
					return;
				}
				Long publicGroupId = xxGroup.getId();
				XXPolicyItemGroupPerm xxPolicyItemGroupPerm = new XXPolicyItemGroupPerm();
				xxPolicyItemGroupPerm.setPolicyItemId(xxPolicyItem.getId());
				xxPolicyItemGroupPerm.setGroupId(publicGroupId);
				xxPolicyItemGroupPerm.setOrder(0);
				xxPolicyItemGroupPerm.setAddedByUserId(currentUserId);
				xxPolicyItemGroupPerm.setUpdatedByUserId(currentUserId);
				daoMgr.getXXPolicyItemGroupPerm().create(xxPolicyItemGroupPerm);
			}
		}
		logger.info("<=== updatePolicyForRelationshipType ");
	}
}
