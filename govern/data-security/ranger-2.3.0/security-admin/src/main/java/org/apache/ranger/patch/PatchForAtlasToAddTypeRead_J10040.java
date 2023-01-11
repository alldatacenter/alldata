/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefValidator;
import org.apache.ranger.plugin.model.validation.RangerValidator.Action;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class PatchForAtlasToAddTypeRead_J10040 extends org.apache.ranger.patch.BaseLoader {
    private static final Logger logger = LoggerFactory.getLogger(PatchForAtlasToAddTypeRead_J10040.class);

    private static final List<String> ATLAS_RESOURCES = new ArrayList<>(
            Arrays.asList("type"));
    private static final List<String> ATLAS_ACCESS_TYPES = new ArrayList<>(
            Arrays.asList("type-read"));

    private static final String GROUP_PUBLIC = "public";
    private static final String TYPE_READ = "type-read";
    private static final String ALL_TYPE_RESOURCE_DEF_NAME = "all - type-category, type";


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
            PatchForAtlasToAddTypeRead_J10040 loader = (PatchForAtlasToAddTypeRead_J10040) CLIUtil
                    .getBean(PatchForAtlasToAddTypeRead_J10040.class);
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
        logger.info("==> PatchForAtlasToAddTypeRead_J10040.execLoad()");
        try {
            addTypeReadPermissionInServiceDef();
            updateDefaultPolicyForType();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error while updating " + EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_ATLAS_NAME + " service-def", e);
        }
        logger.info("<== PatchForAtlasToAddTypeRead_J10040.execLoad()");
    }

    @Override
    public void printStats() {
        logger.info("PatchForAtlasToAddTypeRead_J10040 Logs");
    }

    private void addTypeReadPermissionInServiceDef() throws Exception {

        logger.debug("==>> addTypeReadPermissionInServiceDef");
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
        logger.debug("<<== addTypeReadPermissionInServiceDef");
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

    private void updateDefaultPolicyForType() throws Exception {
        logger.info("==> updateDefaultPolicyForType() ");

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

            for (XXPolicy xxPolicy : xxPolicies) {
                if (xxPolicy.getName().equalsIgnoreCase(ALL_TYPE_RESOURCE_DEF_NAME)) {

                    RangerPolicy rPolicy = svcDBStore.getPolicy(xxPolicy.getId());
                    List<RangerPolicyItem> policyItems = rPolicy.getPolicyItems();

                    for (RangerPolicyItem item : policyItems) {
                        if (!checkIfTypeReadPermissionSet(item)) {
                            List<RangerPolicyItemAccess> itemAccesses = item.getAccesses();
                            itemAccesses.add(getTypeReadPolicyItemAccesses());
                            item.setAccesses(itemAccesses);
                        }
                    }

                    RangerPolicyItem rangerPolicyItemReadType = new RangerPolicyItem();
                    rangerPolicyItemReadType.setDelegateAdmin(Boolean.FALSE);
                    rangerPolicyItemReadType.setAccesses(Arrays.asList(getTypeReadPolicyItemAccesses()));
                    rangerPolicyItemReadType.setGroups(Arrays.asList(GROUP_PUBLIC));

                    policyItems.add(rangerPolicyItemReadType);

                    svcDBStore.updatePolicy(rPolicy);
                }

            }

        }
        logger.info("<== updateDefaultPolicyForType() ");
    }

    private RangerPolicyItemAccess getTypeReadPolicyItemAccesses() {

        RangerPolicyItemAccess policyItemAccess = new RangerPolicyItemAccess();
        policyItemAccess.setType(TYPE_READ);
        policyItemAccess.setIsAllowed(true);

        return policyItemAccess;
    }

    boolean checkIfTypeReadPermissionSet(RangerPolicyItem item) {
        boolean ret = false;
        for (RangerPolicyItemAccess itemAccess : item.getAccesses()) {
            if (ATLAS_ACCESS_TYPES.contains(itemAccess.getType())) {
                ret = true;
                break;
            }
        }
        return ret;
    }

}
