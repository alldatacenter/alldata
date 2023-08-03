
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

import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.entity.XXPolicyResource;
import org.apache.ranger.entity.XXResourceDef;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefValidator;
import org.apache.ranger.plugin.model.validation.RangerValidator.Action;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Disables the Nifi plugin's exclude toggle in Ranger UI.
 * After running this patch user wont be able to add exclude resource policies in NIFI.
 */
@Component
public class PatchForNifiResourceUpdateExclude_J10011 extends BaseLoader {
        private static final Logger logger = LoggerFactory.getLogger(PatchForNifiResourceUpdateExclude_J10011.class);
        @Autowired
        RangerDaoManager daoMgr;

        @Autowired
        ServiceDBStore svcDBStore;

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
                        PatchForNifiResourceUpdateExclude_J10011 loader = (PatchForNifiResourceUpdateExclude_J10011) CLIUtil.getBean(PatchForNifiResourceUpdateExclude_J10011.class);
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
                logger.info("==> PatchForNifiResourceUpdateExclude.execLoad()");
                try {
                        updateNifiServiceDef();
                } catch (Exception e) {
                        logger.error("Error whille updateNifiServiceDef()data.", e);
                }
                logger.info("<== PatchForNifiResourceUpdateExclude.execLoad()");
        }

        @Override
        public void printStats() {
                logger.info("updateNifiServiceDef data ");
        }

        private void updateNifiServiceDef(){
                RangerServiceDef ret = null;
                RangerServiceDef dbNifiServiceDef = null;
                try {
                        dbNifiServiceDef = svcDBStore.getServiceDefByName(EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_NIFI_NAME);
                        if (dbNifiServiceDef != null) {
                                List<RangerResourceDef> rRDefList = null;
                                rRDefList = dbNifiServiceDef.getResources();
                                if (CollectionUtils.isNotEmpty(rRDefList)) {
                                        for (RangerResourceDef rRDef : rRDefList) {

                                                if (rRDef.getExcludesSupported()) {
                                                        rRDef.setExcludesSupported(false);
                                                }

                                                XXResourceDef sdf=daoMgr.getXXResourceDef().findByNameAndServiceDefId(rRDef.getName(), dbNifiServiceDef.getId());
                                                long ResourceDefId=sdf.getId();
                                                List<XXPolicyResource> RangerPolicyResourceList=daoMgr.getXXPolicyResource().findByResDefId(ResourceDefId);
                                                if (CollectionUtils.isNotEmpty(RangerPolicyResourceList)){
                                                        for(XXPolicyResource RangerPolicyResource : RangerPolicyResourceList){
                                                                if(RangerPolicyResource.getIsexcludes()){
                                                                RangerPolicy rPolicy=svcDBStore.getPolicy(RangerPolicyResource.getPolicyid());
                                                                rPolicy.setIsEnabled(false);
                                                                svcStore.updatePolicy(rPolicy);
                                                                }
                                                        }
                                                }
                                        }
                                }
                                RangerServiceDefValidator validator = validatorFactory.getServiceDefValidator(svcStore);
                                validator.validate(dbNifiServiceDef, Action.UPDATE);
                                ret = svcStore.updateServiceDef(dbNifiServiceDef);
                        }
                        if (ret == null) {
                                logger.error("Error while updating " + EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_NIFI_NAME+ "service-def");
                        }
                } catch (Exception e) {
                        logger.error("Error while updating " + EmbeddedServiceDefsUtil.EMBEDDED_SERVICEDEF_NIFI_NAME + "service-def", e);
                }
        }

}
