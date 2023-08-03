
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXAccessTypeDef;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceConfigMap;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
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
public class PatchForAtlasToAddEntityLabelAndBusinessMetadata_J10034 extends BaseLoader {
    private static final Logger logger = LoggerFactory.getLogger(PatchForAtlasToAddEntityLabelAndBusinessMetadata_J10034.class);

    private static final String RESOURCE_DEF_ENTITY_LABEL = "all - entity-type, entity-classification, entity, entity-label";
    private static final String RESOURCE_DEF_ENTITY_BUSINESS_METADATA = "all - entity-type, entity-classification, entity, entity-business-metadata";

    private static final List<String> ATLAS_RESOURCES = new ArrayList<>(
            Arrays.asList("entity-label", "entity-business-metadata"));
    private static final List<String> ATLAS_ACCESS_TYPES = new ArrayList<>(
            Arrays.asList("admin-purge", "entity-add-label", "entity-remove-label", "entity-update-business-metadata"));

    private static final List<String> ATLAS_RESOURCE_LABEL = new ArrayList<>(
            Arrays.asList("entity-type", "entity-classification", "entity", "entity-label"));
    private static final List<String> ATLAS_RESOURCE_BUSINESS_METADATA = new ArrayList<>(
            Arrays.asList("entity-type", "entity-classification", "entity", "entity-business-metadata"));
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
            PatchForAtlasToAddEntityLabelAndBusinessMetadata_J10034 loader = (PatchForAtlasToAddEntityLabelAndBusinessMetadata_J10034) CLIUtil
                    .getBean(PatchForAtlasToAddEntityLabelAndBusinessMetadata_J10034.class);
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
        logger.info("==> PatchForAtlasToAddEntityLabelAndBusinessMetadata.execLoad()");
        try {
            addResourceEntityLabelAndEntityBusinessMetadataInServiceDef();
            createDefaultPolicyForEntityLabelAndEntityBusinessMetadata();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error while updating " + EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME + " service-def");
        }
        logger.info("<== PatchForAtlasToAddEntityLabelAndBusinessMetadata.execLoad()");
    }

    @Override
    public void printStats() {
        logger.info("PatchForAtlasToAddEntityLabelAndBusinessMetadata Logs");
    }

    private void addResourceEntityLabelAndEntityBusinessMetadataInServiceDef() throws Exception {
        RangerServiceDef ret = null;
        RangerServiceDef embeddedAtlasServiceDef = null;
        XXServiceDef xXServiceDefObj = null;
        RangerServiceDef dbAtlasServiceDef = null;
        List<RangerServiceDef.RangerResourceDef> embeddedAtlasResourceDefs = null;
        List<RangerServiceDef.RangerAccessTypeDef> embeddedAtlasAccessTypes = null;

        embeddedAtlasServiceDef = EmbeddedServiceDefsUtil.instance()
                .getEmbeddedServiceDef(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
        if (embeddedAtlasServiceDef != null) {
            xXServiceDefObj = daoMgr.getXXServiceDef()
                    .findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
            if (xXServiceDefObj == null) {
                logger.info(xXServiceDefObj + ": service-def not found. No patching is needed");
                return;
            }

            dbAtlasServiceDef = svcDBStore.getServiceDefByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);

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
            }
        }
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

    private void createDefaultPolicyForEntityLabelAndEntityBusinessMetadata() throws Exception {
        logger.info("==> createDefaultPolicyForEntityLabelAndEntityBusinessMetadata ");
        XXServiceDef xXServiceDefObj = daoMgr.getXXServiceDef()
                .findByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
        if (xXServiceDefObj == null) {
            logger.debug("ServiceDef not found with name :" + EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME);
            return;
        }
        Long xServiceDefId = xXServiceDefObj.getId();
        List<XXService> xxServices = daoMgr.getXXService().findByServiceDefId(xServiceDefId);
        for (XXService xxService : xxServices) {
            List<XXPolicy> xxPolicies = daoMgr.getXXPolicy().findByServiceId(xxService.getId());
            Boolean isEntityLabelPolicyPresent = false;
            Boolean isEntityBusinessMetadataPolicyPresent = false;
            for (XXPolicy xxPolicy : xxPolicies) {
                if (xxPolicy.getName().equalsIgnoreCase(RESOURCE_DEF_ENTITY_LABEL)) {
                    isEntityLabelPolicyPresent = true;
                }
                if (xxPolicy.getName().equalsIgnoreCase(RESOURCE_DEF_ENTITY_BUSINESS_METADATA)) {
                    isEntityBusinessMetadataPolicyPresent = true;
                }
                if (isEntityLabelPolicyPresent && isEntityBusinessMetadataPolicyPresent) {
                    break;
                }
            }

            if (!isEntityLabelPolicyPresent) {
                List<String> accessTypesLabel = Arrays.asList("entity-add-label", "entity-remove-label");
                List<String> accessTypesReadEntity = Arrays.asList("entity-read");
                createDefaultRangerPolicy(xServiceDefId, xxService, RESOURCE_DEF_ENTITY_LABEL, accessTypesLabel,
                        accessTypesReadEntity, ATLAS_RESOURCE_LABEL);
            }

            if (!isEntityBusinessMetadataPolicyPresent) {
                List<String> accessTypesBusinessMetadata = Arrays.asList("entity-update-business-metadata");
                List<String> accessTypesReadEntity = Arrays.asList("entity-read");
                createDefaultRangerPolicy(xServiceDefId, xxService, RESOURCE_DEF_ENTITY_BUSINESS_METADATA, accessTypesBusinessMetadata,
                        accessTypesReadEntity, ATLAS_RESOURCE_BUSINESS_METADATA);
            }

        }
        logger.info("<== createDefaultPolicyForEntityLabelAndEntityBusinessMetadata ");
    }

    private RangerPolicy createDefaultRangerPolicy(Long xServiceDefId, XXService xxService, String policyName,
            List<String> accessTypesLableOrBusinessMetadata, List<String> accessTypesReadEntity, List<String> resources)
            throws Exception {
        RangerPolicy rangerPolicy = getRangerPolicyObject(xxService.getName(), policyName);

        RangerPolicyItem rangerPolicyItemLabelOrBusinessMetadata = new RangerPolicyItem();
        List<RangerPolicyItemAccess> accessesLabelOrBusinessMetadata = getRangerPolicyItemAccessList(
                accessTypesLableOrBusinessMetadata, xxService, rangerPolicy.getName());
        rangerPolicyItemLabelOrBusinessMetadata.setDelegateAdmin(Boolean.TRUE);
        rangerPolicyItemLabelOrBusinessMetadata.setAccesses(accessesLabelOrBusinessMetadata);
        List<String> usersOfPolicyItem1 = getDefaultPolicyUsers(xxService);
        rangerPolicyItemLabelOrBusinessMetadata.setUsers(usersOfPolicyItem1);

        RangerPolicyItem rangerPolicyItemReadEntity = new RangerPolicyItem();
        List<RangerPolicyItemAccess> accessesReadEntity = getRangerPolicyItemAccessList(accessTypesReadEntity,
                xxService, rangerPolicy.getName());
        rangerPolicyItemReadEntity.setDelegateAdmin(Boolean.FALSE);
        rangerPolicyItemReadEntity.setAccesses(accessesReadEntity);
        List<String> usersOfPolicyItem2 = new ArrayList<String>();
        usersOfPolicyItem2.add("rangertagsync");
        List<String> groups = Arrays.asList(GROUP_PUBLIC);
        rangerPolicyItemReadEntity.setGroups(groups);
        rangerPolicyItemReadEntity.setUsers(usersOfPolicyItem2);

        List<RangerPolicyItem> rangerPolicyItems = new ArrayList<RangerPolicyItem>();
        rangerPolicyItems.add(rangerPolicyItemLabelOrBusinessMetadata);
        rangerPolicyItems.add(rangerPolicyItemReadEntity);
        rangerPolicy.setPolicyItems(rangerPolicyItems);
        Map<String, RangerPolicyResource> xPolResMap = getRangerPolicyResourceMap(resources, xServiceDefId,
                xxService.getName(), rangerPolicy.getName());
        rangerPolicy.setResources(xPolResMap);
        logger.info("Creating policy for service id : " + xxService.getId());
        RangerPolicy createdRangerPolicy = svcDBStore.createPolicy(rangerPolicy);
        if (createdRangerPolicy != null) {
            logger.info("Policy created : " + createdRangerPolicy.getName());
        }
        return createdRangerPolicy;
    }

    private RangerPolicy getRangerPolicyObject(String serviceName, String policyName) {
        RangerPolicy rangerPolicy = new RangerPolicy();
        RangerPolicyResourceSignature resourceSignature = new RangerPolicyResourceSignature(rangerPolicy);

        rangerPolicy.setName(policyName);
        rangerPolicy.setDescription("Policy for " + policyName);
        rangerPolicy.setService(serviceName);
        rangerPolicy.setPolicyPriority(RangerPolicy.POLICY_PRIORITY_NORMAL);
        rangerPolicy.setIsAuditEnabled(Boolean.TRUE);
        rangerPolicy.setIsEnabled(Boolean.TRUE);
        rangerPolicy.setPolicyType(RangerPolicy.POLICY_TYPE_ACCESS);
        rangerPolicy.setGuid(guidUtil.genGUID());
        rangerPolicy.setResourceSignature(resourceSignature.getSignature());
        rangerPolicy.setZoneName("");
        rangerPolicy.setUpdatedBy(LOGIN_ID_ADMIN);
        return rangerPolicy;
    }

    private List<RangerPolicyItemAccess> getRangerPolicyItemAccessList(List<String> accessTypesLabel,
            XXService xxService, String policyName) {
        List<RangerPolicyItemAccess> accessesLabel = new ArrayList<RangerPolicyItemAccess>();
        for (int i = 0; i < accessTypesLabel.size(); i++) {
            XXAccessTypeDef xAccTypeDef = daoMgr.getXXAccessTypeDef().findByNameAndServiceId(accessTypesLabel.get(i),
                    xxService.getId());
            if (xAccTypeDef == null) {
                throw new RuntimeException(accessTypesLabel.get(i) + ": is not a valid access-type. policy='"
                        + policyName + "' service='" + xxService.getName() + "'");
            }
            RangerPolicyItemAccess xPolItemAcc = new RangerPolicyItemAccess();
            xPolItemAcc.setIsAllowed(Boolean.TRUE);
            xPolItemAcc.setType(xAccTypeDef.getName());
            accessesLabel.add(xPolItemAcc);
        }
        return accessesLabel;
    }

    private List<String> getDefaultPolicyUsers(XXService xxService) {
        XXPortalUser xxServiceCreator = daoMgr.getXXPortalUser().getById(xxService.getAddedByUserId());
        XXServiceConfigMap cfgMap = daoMgr.getXXServiceConfigMap().findByServiceNameAndConfigKey(xxService.getName(),
                "username");
        XXPortalUser xxServiceCfgUser = daoMgr.getXXPortalUser().findByLoginId(cfgMap.getConfigvalue());
        List<String> users = new ArrayList<String>();
        if (xxServiceCreator != null) {
            users.add(xxServiceCreator.getLoginId());
        }
        if (xxServiceCfgUser != null) {
            users.add(xxServiceCfgUser.getLoginId());
        }
        return users;
    }

    private Map<String, RangerPolicyResource> getRangerPolicyResourceMap(List<String> resources, Long serviceDefId,
            String serviceName, String policyName) {
        Map<String, RangerPolicyResource> xPolResMap = new HashMap<String, RangerPolicyResource>();
        for (int i = 0; i < resources.size(); i++) {
            XXResourceDef xResDef = daoMgr.getXXResourceDef().findByNameAndServiceDefId(resources.get(i), serviceDefId);
            if (xResDef == null) {
                throw new RuntimeException(resources.get(i) + ": is not a valid resource-type. policy='" + policyName
                        + "' service='" + serviceName + "'");
            }
            RangerPolicyResource xPolRes = new RangerPolicyResource();

            xPolRes.setIsExcludes(Boolean.FALSE);
            xPolRes.setIsRecursive(Boolean.FALSE);
            xPolRes.setValue("*");
            xPolResMap.put(xResDef.getName(), xPolRes);
        }
        return xPolResMap;
    }
}
