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

package org.apache.ranger.patch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;
import java.util.List;
import java.util.Map;
import java.util.Date;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefValidator;
import org.apache.ranger.plugin.model.validation.RangerValidator;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.plugin.util.SearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import static org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME;

@Component
public class PatchAtlasForClassificationResource_J10047 extends BaseLoader {


    private static final Logger logger = LoggerFactory.getLogger(PatchAtlasForClassificationResource_J10047.class);


    private static final List<String> ATLAS_RESOURCES = new ArrayList<>(
            Arrays.asList( "classification"));

    private static final List<String> ATLAS_ACCESS_TYPES = new ArrayList<>(
            Arrays.asList("entity-remove-classification", "entity-add-classification", "entity-update-classification"));

    private static final List<String> ATLAS_RESOURCE_ENTITY = new ArrayList<>(
            Arrays.asList("entity-type", "entity-classification", "entity"));

    private static final String ENTITY_CLASSIFICATION = "entity-classification";
 
    private static final String CLASSIFICATION = "classification";

    private static final String ENTITY = "entity";

    private static final List<String> TYPES = new ArrayList<>(
            Arrays.asList("type", "entity-type", "entity-classification", "relationship-type", "end-one-entity-type",
                    "end-one-entity-classification","end-two-entity-type","end-two-entity-classification","entity-business-metadata"));

    @Autowired
    RangerDaoManager daoMgr;

    @Autowired
    ServiceDBStore svcDBStore;

    @Autowired
    RangerValidatorFactory validatorFactory;

    @Autowired
    ServiceDBStore svcStore;


    public static void main(String[] args) {
        logger.info("main()");
        try {
            PatchAtlasForClassificationResource_J10047 loader = (PatchAtlasForClassificationResource_J10047) org.apache.ranger.util.CLIUtil
                    .getBean(PatchAtlasForClassificationResource_J10047.class);
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
        logger.info("==> PatchAtlasForClassificationResource_J10047.execLoad()");
        try {
            addResourceClassificationsInServiceDef();
            createAdditionalPolicyWithClassificationForExistingEntityClassificationPolicy();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while updating " + EMBEDDED_SERVICEDEF_ATLAS_NAME + " service-def");
        }
        logger.info("<== PatchAtlasForClassificationResource_J10047.execLoad()");
    }

    @Override
    public void printStats() {
        logger.info("PatchAtlasForClassificationResource_J10047 Logs");
    }

    private void addResourceClassificationsInServiceDef() throws Exception {
        RangerServiceDef ret = null;
        RangerServiceDef embeddedAtlasServiceDef = null;
        XXServiceDef xXServiceDefObj = null;
        RangerServiceDef dbAtlasServiceDef = null;


        embeddedAtlasServiceDef = EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(EMBEDDED_SERVICEDEF_ATLAS_NAME);
        if (embeddedAtlasServiceDef != null) {
            xXServiceDefObj = daoMgr.getXXServiceDef().findByName(EMBEDDED_SERVICEDEF_ATLAS_NAME);
            if (xXServiceDefObj == null) {
                logger.info(" service-def for "+ EMBEDDED_SERVICEDEF_ATLAS_NAME+" not found. No patching is needed");
                return;
            }

            dbAtlasServiceDef = svcDBStore.getServiceDefByName(EMBEDDED_SERVICEDEF_ATLAS_NAME);

            updateResourceInServiceDef(embeddedAtlasServiceDef, dbAtlasServiceDef);
            updateTypeResourceWithIgnoreCase(dbAtlasServiceDef.getResources());
            removeEntityResourceAccessTypeRestrictions(dbAtlasServiceDef.getResources());

            RangerServiceDefValidator validator = validatorFactory.getServiceDefValidator(svcStore);
            validator.validate(dbAtlasServiceDef, RangerValidator.Action.UPDATE);
            ret = svcStore.updateServiceDef(dbAtlasServiceDef);
            if (ret == null) {
                logger.error("Error while updating "+EMBEDDED_SERVICEDEF_ATLAS_NAME+"  service-def");
                throw new RuntimeException("Error while updating " + EMBEDDED_SERVICEDEF_ATLAS_NAME + " service-def");
            }
        }
    }

    private void updateResourceInServiceDef(RangerServiceDef embeddedAtlasServiceDef, RangerServiceDef dbAtlasServiceDef) {
        List<RangerServiceDef.RangerResourceDef> embeddedAtlasResourceDefs;
        List<RangerAccessTypeDef> embeddedAtlasAccessTypes;
        embeddedAtlasResourceDefs = embeddedAtlasServiceDef.getResources();
        embeddedAtlasAccessTypes = embeddedAtlasServiceDef.getAccessTypes();

        if (!checkResourcePresent(dbAtlasServiceDef.getResources()) && checkResourcePresent(embeddedAtlasResourceDefs)) {
            dbAtlasServiceDef.setResources(embeddedAtlasResourceDefs);
            if (checkAccessPresent(embeddedAtlasAccessTypes)) {
                dbAtlasServiceDef.setAccessTypes(embeddedAtlasAccessTypes);
            }
        } else {
            logger.info("resource already present");
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

    private boolean checkAccessPresent(List<RangerServiceDef.RangerAccessTypeDef> embeddedAtlasAccessTypes) {
        boolean ret = false;
        for (RangerServiceDef.RangerAccessTypeDef accessDef : embeddedAtlasAccessTypes) {
            if (ATLAS_ACCESS_TYPES.contains(accessDef.getName())) {
                ret = true;
                break;
            }
        }
        return ret;
    }

    private void updateTypeResourceWithIgnoreCase(List<RangerServiceDef.RangerResourceDef> dbSerresourceDefs) {
        for (RangerServiceDef.RangerResourceDef dbResourceDef : dbSerresourceDefs) {
            if (TYPES.contains(dbResourceDef.getName())) {
                dbResourceDef.getMatcherOptions().put("ignoreCase", "false");
            }
        }
    }

    private void removeEntityResourceAccessTypeRestrictions(List<RangerServiceDef.RangerResourceDef> dbSerresourceDefs) {
        for (RangerServiceDef.RangerResourceDef dbResourceDef : dbSerresourceDefs) {
            if (ENTITY.equals(dbResourceDef.getName())) {
                dbResourceDef.getAccessTypeRestrictions().removeAll(ATLAS_ACCESS_TYPES);
            }
        }
    }

    private void createAdditionalPolicyWithClassificationForExistingEntityClassificationPolicy() throws Exception {

        XXServiceDef xXServiceDefObj = daoMgr.getXXServiceDef().findByName(EMBEDDED_SERVICEDEF_ATLAS_NAME);

        if (xXServiceDefObj == null) {
            logger.debug("ServiceDef not found with name :" + EMBEDDED_SERVICEDEF_ATLAS_NAME);
            return;
        }

        Long xServiceDefId = xXServiceDefObj.getId();
        List<XXService> xxServices = daoMgr.getXXService().findByServiceDefId(xServiceDefId);

        for (XXService xxService : xxServices) {

            List<RangerPolicy> servicePolicies = svcStore.getServicePolicies(xxService.getId(), new SearchFilter());

            for (RangerPolicy policy : servicePolicies) {

                if(!isEntityResource(policy.getResources())){
                    continue;
                }

                List<RangerPolicyItem> policyItems = policy.getPolicyItems();
                List<RangerPolicyItem> denypolicyItems = policy.getDenyPolicyItems();

                boolean policyItemCheck = checkAndFilterNonClassificationAccessTypeFromPolicy(policyItems);
                boolean denyPolicyItemCheck =  checkAndFilterNonClassificationAccessTypeFromPolicy(denypolicyItems);

                if (policyItemCheck || denyPolicyItemCheck) {
                    policy.setName(policy.getName() + " - " + CLASSIFICATION);

                    Map<String, RangerPolicyResource> xPolResMap = policy.getResources();

                    RangerPolicyResource resource = xPolResMap.get(ENTITY_CLASSIFICATION);

                    xPolResMap.put(CLASSIFICATION, resource);
                    policy.setResources(xPolResMap);

                    policy.setVersion(1L);
                    policy.setGuid(null);
                    policy.setId(null);
                    policy.setCreateTime(new Date());
                    policy.setUpdateTime(new Date());

                    svcStore.createPolicy(policy);
                    logger.info("New Additional policy created");
                }
            }
        }
        logger.info("<== createAdditionalPolicyWithClassificationForExistingPolicy");
    }

    private boolean isEntityResource(Map<String, RangerPolicyResource> xPolResMap) {
        boolean ret = true;

        if (xPolResMap != null && xPolResMap.size() == ATLAS_RESOURCE_ENTITY.size()){
            for (String resourceName : ATLAS_RESOURCE_ENTITY) {
                if (xPolResMap.get(resourceName) == null) {
                    ret = false;
                    break;
                }
            }
        }else{
            ret = false;
        }

        return ret;
    }

    private boolean checkAndFilterNonClassificationAccessTypeFromPolicy(List<RangerPolicyItem> policyItems) {

        ListIterator<RangerPolicyItem> policyItemListIterator = policyItems.listIterator();
        boolean isClassificationAccessTypeExist = false;

        while (policyItemListIterator.hasNext()) {
            RangerPolicyItem policyItem = policyItemListIterator.next();
            ListIterator<RangerPolicyItemAccess> itemAccessListIterator = policyItem.getAccesses().listIterator();

            boolean accessPresent = false;
            while (itemAccessListIterator.hasNext()) {
                RangerPolicyItemAccess access = itemAccessListIterator.next();
                if (!ATLAS_ACCESS_TYPES.contains(access.getType())) {
                    itemAccessListIterator.remove();
                } else {
                    accessPresent = true;
                    isClassificationAccessTypeExist = true;
                }
            }
            if (!accessPresent) {
                policyItemListIterator.remove();
            }
        }

        return isClassificationAccessTypeExist;
    }

}
