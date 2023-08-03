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
import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.ServiceDBStore;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.RangerValidatorFactory;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
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
import org.apache.ranger.entity.XXServiceDef;
import java.util.List;
import java.util.Map;

@Component
public class PatchForTagServiceDefUpdate_J10008 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchForTagServiceDefUpdate_J10008.class);
	public static final String SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME  = "tag";
	public static final String SCRIPT_POLICY_CONDITION_NAME = "expression";

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
			PatchForTagServiceDefUpdate_J10008 loader = (PatchForTagServiceDefUpdate_J10008) CLIUtil.getBean(PatchForTagServiceDefUpdate_J10008.class);
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
		logger.info("==> PatchForTagServiceDefUpdate.execLoad()");
		try {
			updateTagServiceDef();
		} catch (Exception e) {
			logger.error("Error whille updateTagServiceDef()data.", e);
		}
		logger.info("<== PatchForTagServiceDefUpdate.execLoad()");
	}

	@Override
	public void printStats() {
		logger.info("PatchForTagServiceDefUpdate data ");
	}

	private void updateTagServiceDef(){
		RangerServiceDef embeddedTagServiceDef = null;
		RangerServiceDef dbTagServiceDef 		= null;
		List<RangerServiceDef.RangerPolicyConditionDef> 	embeddedTagPolicyConditionDefs  = null;
		XXServiceDef xXServiceDefObj			= null;
		try{
			embeddedTagServiceDef=EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME);
			if(embeddedTagServiceDef!=null){
				embeddedTagPolicyConditionDefs = embeddedTagServiceDef.getPolicyConditions();
				if (embeddedTagPolicyConditionDefs == null) {
					logger.error("Policy Conditions are empyt in tag service def json");
					return;
				}
				
				if (checkScriptPolicyCondPresent(embeddedTagPolicyConditionDefs) == false) {
					logger.error(SCRIPT_POLICY_CONDITION_NAME + "policy condition not found!!");
					return;
				}
				
				xXServiceDefObj = daoMgr.getXXServiceDef().findByName(SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME);
				if (xXServiceDefObj == null) {
					logger.error("Service def for " + SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME + " is not found!!");
					return;
				}
				
				Map<String, String> serviceDefOptionsPreUpdate=null;
				String jsonStrPreUpdate=null;
				jsonStrPreUpdate=xXServiceDefObj.getDefOptions();
				if (!StringUtils.isEmpty(jsonStrPreUpdate)) {
					serviceDefOptionsPreUpdate=jsonUtil.jsonToMap(jsonStrPreUpdate);
				}
				xXServiceDefObj=null;
				dbTagServiceDef=svcDBStore.getServiceDefByName(SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME);
				
				if(dbTagServiceDef!=null){				
					dbTagServiceDef.setPolicyConditions(embeddedTagPolicyConditionDefs);
					RangerServiceDefValidator validator = validatorFactory.getServiceDefValidator(svcStore);
					validator.validate(dbTagServiceDef, Action.UPDATE);

					svcStore.updateServiceDef(dbTagServiceDef);
					
					xXServiceDefObj = daoMgr.getXXServiceDef().findByName(SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME);
					if(xXServiceDefObj!=null) {
						String jsonStrPostUpdate=xXServiceDefObj.getDefOptions();
						Map<String, String> serviceDefOptionsPostUpdate = null;
						if (!StringUtils.isEmpty(jsonStrPostUpdate)) {
							serviceDefOptionsPostUpdate =jsonUtil.jsonToMap(jsonStrPostUpdate);
						}
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
					}
				}
			}
		}catch(Exception e)
		{
			logger.error("Error while updating "+SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME+"service-def", e);
		}
	}

	private boolean checkScriptPolicyCondPresent(List<RangerServiceDef.RangerPolicyConditionDef> policyCondDefs) {
		boolean ret = false;
		for(RangerServiceDef.RangerPolicyConditionDef policyCondDef : policyCondDefs) {
			if ( SCRIPT_POLICY_CONDITION_NAME.equals(policyCondDef.getName()) ) {
				ret = true ;
				break;
			}
		}
		return ret;
	}

	private String mapToJsonString(Map<String, String> map) throws Exception{
		String ret = null;
		if(map != null) {
			ret = jsonUtil.readMapToString(map);
		}
		return ret;
	}
}
