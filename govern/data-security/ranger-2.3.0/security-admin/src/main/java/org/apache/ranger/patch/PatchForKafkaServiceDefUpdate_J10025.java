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

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXAccessTypeDef;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXPolicyItem;
import org.apache.ranger.entity.XXPolicyItemAccess;
import org.apache.ranger.entity.XXPolicyItemUserPerm;
import org.apache.ranger.entity.XXPolicyResource;
import org.apache.ranger.entity.XXPolicyResourceMap;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicyResourceSignature;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefValidator;
import org.apache.ranger.plugin.model.validation.RangerValidator.Action;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.service.XPermMapService;
import org.apache.ranger.service.XPolicyService;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PatchForKafkaServiceDefUpdate_J10025 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchForKafkaServiceDefUpdate_J10025.class);
	private static final List<String> POLICY_NAMES = new ArrayList<>(Arrays.asList("all - cluster", "all - delegationtoken"));
	private static final String LOGIN_ID_ADMIN = "admin";
	private static final String KAFKA_RESOURCE_CLUSTER = "cluster";
	private static final String KAFKA_RESOURCE_DELEGATIONTOKEN = "delegationtoken";

	private static final List<String> DEFAULT_POLICY_USERS = new ArrayList<>(Arrays.asList("kafka","rangerlookup"));


	public static final String SERVICEDBSTORE_SERVICEDEFBYNAME_KAFKA_NAME  = "kafka";
	public static final String CLUSTER_RESOURCE_NAME ="cluster";


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
	GUIDUtil guidUtil;

	@Autowired
	XPolicyService xPolService;

	@Autowired
	XPermMapService xPermMapService;

	@Autowired
	RangerBizUtil bizUtil;

	@Autowired
	RangerValidatorFactory validatorFactory;

	@Autowired
	ServiceDBStore svcStore;

	public static void main(String[] args) {
		logger.info("main()");
		try {
			PatchForKafkaServiceDefUpdate_J10025 loader = (PatchForKafkaServiceDefUpdate_J10025) CLIUtil.getBean(PatchForKafkaServiceDefUpdate_J10025.class);
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
		logger.info("==> PatchForKafkaServiceDefUpdate_J10025.execLoad()");
		try {
			updateKafkaServiceDef();
		} catch (Exception e) {
			logger.error("Error while applying PatchForKafkaServiceDefUpdate_J10025...", e);
		}
		logger.info("<== PatchForKafkaServiceDefUpdate_J10025.execLoad()");
	}

	@Override
	public void printStats() {
		logger.info("PatchForKafkaServiceDefUpdate_J10025 ");
	}

	private void updateKafkaServiceDef(){
		RangerServiceDef ret                = null;
		RangerServiceDef embeddedKafkaServiceDef = null;
		RangerServiceDef dbKafkaServiceDef         = null;
		List<RangerServiceDef.RangerResourceDef>   embeddedKafkaResourceDefs  = null;
		List<RangerServiceDef.RangerAccessTypeDef>     embeddedKafkaAccessTypes   = null;
		XXServiceDef xXServiceDefObj         = null;
		try{
			embeddedKafkaServiceDef=EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(SERVICEDBSTORE_SERVICEDEFBYNAME_KAFKA_NAME);
			if(embeddedKafkaServiceDef!=null){

				xXServiceDefObj = daoMgr.getXXServiceDef().findByName(SERVICEDBSTORE_SERVICEDEFBYNAME_KAFKA_NAME);
				Map<String, String> serviceDefOptionsPreUpdate=null;
				String jsonStrPreUpdate=null;
				if(xXServiceDefObj!=null) {
					jsonStrPreUpdate=xXServiceDefObj.getDefOptions();
					serviceDefOptionsPreUpdate=jsonStringToMap(jsonStrPreUpdate);
					xXServiceDefObj=null;
				}
				dbKafkaServiceDef=svcDBStore.getServiceDefByName(SERVICEDBSTORE_SERVICEDEFBYNAME_KAFKA_NAME);

				if(dbKafkaServiceDef!=null){
					embeddedKafkaResourceDefs = embeddedKafkaServiceDef.getResources();
					embeddedKafkaAccessTypes  = embeddedKafkaServiceDef.getAccessTypes();

					if (checkNewKafkaresourcePresent(embeddedKafkaResourceDefs)) {
						// This is to check if CLUSTER resource is added to the resource definition, if so update the resource def and accessType def
						if (embeddedKafkaResourceDefs != null) {
							dbKafkaServiceDef.setResources(embeddedKafkaResourceDefs);
						}
						if (embeddedKafkaAccessTypes != null) {
							if(!embeddedKafkaAccessTypes.toString().equalsIgnoreCase(dbKafkaServiceDef.getAccessTypes().toString())) {
								dbKafkaServiceDef.setAccessTypes(embeddedKafkaAccessTypes);
							}
						}
					}

					RangerServiceDefValidator validator = validatorFactory.getServiceDefValidator(svcStore);
					validator.validate(dbKafkaServiceDef, Action.UPDATE);

					ret = svcStore.updateServiceDef(dbKafkaServiceDef);
					if(ret==null){
						logger.error("Error while updating "+SERVICEDBSTORE_SERVICEDEFBYNAME_KAFKA_NAME+"service-def");
						throw new RuntimeException("Error while updating "+SERVICEDBSTORE_SERVICEDEFBYNAME_KAFKA_NAME+"service-def");
					}
					xXServiceDefObj = daoMgr.getXXServiceDef().findByName(SERVICEDBSTORE_SERVICEDEFBYNAME_KAFKA_NAME);
					if(xXServiceDefObj!=null) {
						String jsonStrPostUpdate=xXServiceDefObj.getDefOptions();
						Map<String, String> serviceDefOptionsPostUpdate=jsonStringToMap(jsonStrPostUpdate);
						if (serviceDefOptionsPostUpdate != null && serviceDefOptionsPostUpdate.containsKey(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES)) {
							if(serviceDefOptionsPreUpdate == null || !serviceDefOptionsPreUpdate.containsKey(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES)) {
								String preUpdateValue = serviceDefOptionsPreUpdate == null ? null : serviceDefOptionsPreUpdate.get(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES);
								if (preUpdateValue == null) {
									serviceDefOptionsPostUpdate.remove(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES);
								} else {
									serviceDefOptionsPostUpdate.put(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES, preUpdateValue);
								}
								xXServiceDefObj.setDefOptions(mapToJsonString(serviceDefOptionsPostUpdate));
								daoMgr.getXXServiceDef().update(xXServiceDefObj);
							}
						}
						createDefaultPolicyForNewResources();
					}
				}
			}
		}catch(Exception e)
		{
			logger.error("Error while updating "+SERVICEDBSTORE_SERVICEDEFBYNAME_KAFKA_NAME+"service-def", e);
		}
	}

	private boolean checkNewKafkaresourcePresent(List<RangerServiceDef.RangerResourceDef> resourceDefs) {
		boolean ret = false;
		for(RangerServiceDef.RangerResourceDef resourceDef : resourceDefs) {
			if (CLUSTER_RESOURCE_NAME.equals(resourceDef.getName()) ) {
				ret = true ;
				break;
			}
		}
		return ret;
	}

	private String mapToJsonString(Map<String, String> map) {
		String ret = null;
		if(map != null) {
			try {
				ret = jsonUtil.readMapToString(map);
			} catch(Exception excp) {
				logger.warn("mapToJsonString() failed to convert map: " + map, excp);
			}
		}
		return ret;
	}

	protected Map<String, String> jsonStringToMap(String jsonStr) {
		Map<String, String> ret = null;
		if(!StringUtils.isEmpty(jsonStr)) {
			try {
				ret = jsonUtil.jsonToMap(jsonStr);
			} catch(Exception excp) {
				// fallback to earlier format: "name1=value1;name2=value2"
				for(String optionString : jsonStr.split(";")) {
					if(StringUtils.isEmpty(optionString)) {
						continue;
					}
					String[] nvArr = optionString.split("=");
					String name  = (nvArr != null && nvArr.length > 0) ? nvArr[0].trim() : null;
					String value = (nvArr != null && nvArr.length > 1) ? nvArr[1].trim() : null;
					if(StringUtils.isEmpty(name)) {
						continue;
					}
					if(ret == null) {
						ret = new HashMap<String, String>();
					}
					ret.put(name, value);
				}
			}
		}
		return ret;
	}

	private void createDefaultPolicyForNewResources() {
		logger.info("==> createDefaultPolicyForNewResources ");
		XXPortalUser xxPortalUser = daoMgr.getXXPortalUser().findByLoginId(LOGIN_ID_ADMIN);
		Long currentUserId = xxPortalUser.getId();

		XXServiceDef xXServiceDefObj = daoMgr.getXXServiceDef()
				.findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_KAFKA_NAME);
		if (xXServiceDefObj == null) {
			logger.debug("ServiceDef not fount with name :" + EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_KAFKA_NAME);
			return;
		}

		Long xServiceDefId = xXServiceDefObj.getId();
		List<XXService> xxServices = daoMgr.getXXService().findByServiceDefId(xServiceDefId);

		for (XXService xxService : xxServices) {
			int resourceMapOrder = 0;
			for (String newResource : POLICY_NAMES) {
				XXPolicy xxPolicy = new XXPolicy();
				xxPolicy.setName(newResource);
				xxPolicy.setDescription(newResource);
				xxPolicy.setService(xxService.getId());
				xxPolicy.setPolicyPriority(RangerPolicy.POLICY_PRIORITY_NORMAL);
				xxPolicy.setIsAuditEnabled(Boolean.TRUE);
				xxPolicy.setIsEnabled(Boolean.TRUE);
				xxPolicy.setPolicyType(RangerPolicy.POLICY_TYPE_ACCESS);
				xxPolicy.setGuid(guidUtil.genGUID());
				xxPolicy.setAddedByUserId(currentUserId);
				xxPolicy.setUpdatedByUserId(currentUserId);
				RangerPolicy rangerPolicy = getRangerPolicy(newResource,xxPortalUser,xxService);
				xxPolicy.setPolicyText(JsonUtils.objectToJson(rangerPolicy));
				xxPolicy.setResourceSignature(rangerPolicy.getResourceSignature());
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

				List<String> accessTypes = getAccessTypes();
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

				for (int i = 0; i < DEFAULT_POLICY_USERS.size(); i++) {
					String user = DEFAULT_POLICY_USERS.get(i);
					if (StringUtils.isBlank(user)) {
						continue;
					}
					XXUser xxUser = daoMgr.getXXUser().findByUserName(user);
					if (xxUser == null) {
						throw new RuntimeException(user + ": user does not exist. policy='" + xxPolicy.getName()
								+ "' service='" + xxPolicy.getService() + "' user='" + user + "'");
					}
					XXPolicyItemUserPerm xUserPerm = new XXPolicyItemUserPerm();
					xUserPerm.setUserId(xxUser.getId());
					xUserPerm.setPolicyItemId(createdXXPolicyItem.getId());
					xUserPerm.setOrder(i);
					xUserPerm.setAddedByUserId(currentUserId);
					xUserPerm.setUpdatedByUserId(currentUserId);
					daoMgr.getXXPolicyItemUserPerm().create(xUserPerm);
				}

				String policyResourceName = KAFKA_RESOURCE_CLUSTER;
				if ("all - delegationtoken".equals(newResource)) {
					policyResourceName = KAFKA_RESOURCE_DELEGATIONTOKEN;
				}
				XXResourceDef xResDef = daoMgr.getXXResourceDef().findByNameAndPolicyId(policyResourceName,
						createdPolicy.getId());
				if (xResDef == null) {
					throw new RuntimeException(policyResourceName + ": is not a valid resource-type. policy='"
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
				xPolResMap.setOrder(resourceMapOrder);
				xPolResMap.setAddedByUserId(currentUserId);
				xPolResMap.setUpdatedByUserId(currentUserId);
				daoMgr.getXXPolicyResourceMap().create(xPolResMap);
				resourceMapOrder++;
				logger.info("Creating policy for service id : " + xxService.getId());
			}
		}
		logger.info("<== createDefaultPolicyForNewResources ");
	}


	private RangerPolicy getRangerPolicy(String newResource, XXPortalUser xxPortalUser, XXService xxService) {
		RangerPolicy policy = new RangerPolicy();

		List<RangerPolicy.RangerPolicyItemAccess> accesses = getPolicyItemAccesses();
		List<String> users = new ArrayList<>(DEFAULT_POLICY_USERS);
		List<String> groups = new ArrayList<>();
		List<RangerPolicy.RangerPolicyItemCondition> conditions = new ArrayList<>();
		List<RangerPolicy.RangerPolicyItem> policyItems = new ArrayList<>();
		RangerPolicy.RangerPolicyItem rangerPolicyItem = new RangerPolicy.RangerPolicyItem();
		rangerPolicyItem.setAccesses(accesses);
		rangerPolicyItem.setConditions(conditions);
		rangerPolicyItem.setGroups(groups);
		rangerPolicyItem.setUsers(users);
		rangerPolicyItem.setDelegateAdmin(false);

		policyItems.add(rangerPolicyItem);

		Map<String, RangerPolicy.RangerPolicyResource> policyResource = new HashMap<>();
		RangerPolicy.RangerPolicyResource rangerPolicyResource = new RangerPolicy.RangerPolicyResource();
		rangerPolicyResource.setIsExcludes(false);
		rangerPolicyResource.setIsRecursive(false);
		rangerPolicyResource.setValue("*");
		String policyResourceName = KAFKA_RESOURCE_CLUSTER;
		if ("all - delegationtoken".equals(newResource)) {
			policyResourceName = KAFKA_RESOURCE_DELEGATIONTOKEN;
		}
		policyResource.put(policyResourceName, rangerPolicyResource);
		policy.setCreateTime(new Date());
		policy.setDescription(newResource);
		policy.setIsEnabled(true);
		policy.setName(newResource);
		policy.setCreatedBy(xxPortalUser.getLoginId());
		policy.setUpdatedBy(xxPortalUser.getLoginId());
		policy.setUpdateTime(new Date());
		policy.setService(xxService.getName());
		policy.setIsAuditEnabled(true);
		policy.setPolicyItems(policyItems);
		policy.setResources(policyResource);
		policy.setPolicyType(0);
		policy.setId(0L);
		policy.setGuid("");
		policy.setPolicyLabels(new ArrayList<>());
		policy.setVersion(1L);
		RangerPolicyResourceSignature resourceSignature = new RangerPolicyResourceSignature(policy);
		policy.setResourceSignature(resourceSignature.getSignature());
		return policy;
	}

	private List<String> getAccessTypes() {
		List<String> accessTypes = Arrays.asList("create", "delete", "configure", "alter_configs", "describe", "describe_configs", "consume", "publish", "kafka_admin", "idempotent_write");
		return accessTypes;
	}

	private ArrayList<RangerPolicy.RangerPolicyItemAccess> getPolicyItemAccesses() {
		ArrayList<RangerPolicy.RangerPolicyItemAccess> rangerPolicyItemAccesses = new ArrayList<>();
		for(String type:getAccessTypes()) {
			RangerPolicy.RangerPolicyItemAccess policyItemAccess = new  RangerPolicy.RangerPolicyItemAccess();
			policyItemAccess.setType(type);
			policyItemAccess.setIsAllowed(true);
			rangerPolicyItemAccesses.add(policyItemAccess);
		}
		return rangerPolicyItemAccesses;
	}
}
