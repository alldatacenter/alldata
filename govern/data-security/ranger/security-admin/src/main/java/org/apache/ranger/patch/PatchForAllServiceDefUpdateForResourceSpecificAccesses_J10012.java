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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.validation.RangerServiceDefHelper;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.service.XPermMapService;
import org.apache.ranger.service.XPolicyService;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.apache.ranger.entity.XXServiceDef;

import java.util.List;
import java.util.Map;

@Component
public class PatchForAllServiceDefUpdateForResourceSpecificAccesses_J10012 extends BaseLoader {
    private static final Logger logger = LoggerFactory.getLogger(PatchForAllServiceDefUpdateForResourceSpecificAccesses_J10012.class);

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
            PatchForAllServiceDefUpdateForResourceSpecificAccesses_J10012 loader = (PatchForAllServiceDefUpdateForResourceSpecificAccesses_J10012) CLIUtil.getBean(PatchForAllServiceDefUpdateForResourceSpecificAccesses_J10012.class);
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
        logger.info("==> PatchForAllServiceDefUpdateForResourceSpecificAccesses_J10012.execLoad()");
        try {
            updateAllServiceDef();
        } catch (Exception e) {
            logger.error("Error in PatchForAllServiceDefUpdateForResourceSpecificAccesses_J10012.execLoad()", e);
        }
        logger.info("<== PatchForAllServiceDefUpdateForResourceSpecificAccesses_J10012.execLoad()");
    }

    @Override
    public void printStats() {
        logger.info("PatchForAllServiceDefUpdateForResourceSpecificAccesses_J10012 data ");
    }

	private void updateAllServiceDef() {

		List<XXServiceDef> allXXServiceDefs;
		allXXServiceDefs = daoMgr.getXXServiceDef().getAll();

		if (CollectionUtils.isNotEmpty(allXXServiceDefs)) {

			for (XXServiceDef xxServiceDef : allXXServiceDefs) {

				String serviceDefName = xxServiceDef.getName();

				try {
					String jsonStrPreUpdate = xxServiceDef.getDefOptions();
					Map<String, String> serviceDefOptionsPreUpdate = jsonUtil.jsonToMap(jsonStrPreUpdate);
					String valueBeforeUpdate = serviceDefOptionsPreUpdate.get(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES);

					RangerServiceDef serviceDef = svcDBStore.getServiceDefByName(serviceDefName);

					if (serviceDef != null) {
						logger.info("Started patching service-def:[" + serviceDefName + "]");

						RangerServiceDefHelper defHelper = new RangerServiceDefHelper(serviceDef, false);
						defHelper.patchServiceDefWithDefaultValues();

						svcStore.updateServiceDef(serviceDef);

						XXServiceDef dbServiceDef = daoMgr.getXXServiceDef().findByName(serviceDefName);

						if (dbServiceDef != null) {
							String jsonStrPostUpdate = dbServiceDef.getDefOptions();
							Map<String, String> serviceDefOptionsPostUpdate = jsonUtil.jsonToMap(jsonStrPostUpdate);
							String valueAfterUpdate = serviceDefOptionsPostUpdate.get(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES);

							if (!StringUtils.equals(valueBeforeUpdate, valueAfterUpdate)) {
								if (StringUtils.isEmpty(valueBeforeUpdate)) {
									serviceDefOptionsPostUpdate.remove(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES);
								} else {
									serviceDefOptionsPostUpdate.put(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES, valueBeforeUpdate);
								}
								dbServiceDef.setDefOptions(mapToJsonString(serviceDefOptionsPostUpdate));
								daoMgr.getXXServiceDef().update(dbServiceDef);
							}
						}
						logger.info("Completed patching service-def:[" + serviceDefName + "]");
					}
				} catch (Exception e) {
					logger.error("Error while patching service-def:[" + serviceDefName + "]", e);
				}
			}
		}
	}

    private String mapToJsonString(Map<String, String> map) throws Exception {
        String ret = null;
        if(map != null) {
            ret = jsonUtil.readMapToString(map);
        }
        return ret;
    }
}

