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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PatchForHiveServiceDefUpdate_J10007 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchForHiveServiceDefUpdate_J10007.class);
	public static final String SERVICEDBSTORE_SERVICEDEFBYNAME_HIVE_NAME  = "hive";
	public static final String URL_RESOURCE_NAME ="url";

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
			PatchForHiveServiceDefUpdate_J10007 loader = (PatchForHiveServiceDefUpdate_J10007) CLIUtil.getBean(PatchForHiveServiceDefUpdate_J10007.class);
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
		logger.info("==> PatchForHiveServiceDefUpdate.execLoad()");
		try {
			updateHiveServiceDef();
		} catch (Exception e) {
			logger.error("Error whille updateHiveServiceDef()data.", e);
		}
		logger.info("<== PatchForHiveServiceDefUpdate.execLoad()");
	}

	@Override
	public void printStats() {
		logger.info("PatchForHiveServiceDefUpdate data ");
	}

	private void updateHiveServiceDef(){
		RangerServiceDef ret  					= null;
		RangerServiceDef embeddedHiveServiceDef = null;
		RangerServiceDef dbHiveServiceDef 		= null;
		List<RangerServiceDef.RangerResourceDef> 	embeddedHiveResourceDefs  = null;
		List<RangerServiceDef.RangerAccessTypeDef> 	embeddedHiveAccessTypes   = null;
		XXServiceDef xXServiceDefObj			= null;
		try{
			embeddedHiveServiceDef=EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(SERVICEDBSTORE_SERVICEDEFBYNAME_HIVE_NAME);
			if(embeddedHiveServiceDef!=null){

				xXServiceDefObj = daoMgr.getXXServiceDef().findByName(SERVICEDBSTORE_SERVICEDEFBYNAME_HIVE_NAME);
				Map<String, String> serviceDefOptionsPreUpdate=null;
				String jsonStrPreUpdate=null;
				if(xXServiceDefObj!=null) {
					jsonStrPreUpdate=xXServiceDefObj.getDefOptions();
					serviceDefOptionsPreUpdate=jsonStringToMap(jsonStrPreUpdate);
					xXServiceDefObj=null;
				}
				dbHiveServiceDef=svcDBStore.getServiceDefByName(SERVICEDBSTORE_SERVICEDEFBYNAME_HIVE_NAME);
				
				if(dbHiveServiceDef!=null){
					embeddedHiveResourceDefs = embeddedHiveServiceDef.getResources();
					embeddedHiveAccessTypes  = embeddedHiveServiceDef.getAccessTypes();

					if (checkURLresourcePresent(embeddedHiveResourceDefs)) {
						// This is to check if URL def is added to the resource definition, if so update the resource def and accessType def
						if (embeddedHiveResourceDefs != null) {
							dbHiveServiceDef.setResources(embeddedHiveResourceDefs);
						}
						if (embeddedHiveAccessTypes != null) {
							if(!embeddedHiveAccessTypes.toString().equalsIgnoreCase(dbHiveServiceDef.getAccessTypes().toString())) {
								dbHiveServiceDef.setAccessTypes(embeddedHiveAccessTypes);
							}
						}
					}

					RangerServiceDefValidator validator = validatorFactory.getServiceDefValidator(svcStore);
					validator.validate(dbHiveServiceDef, Action.UPDATE);

					ret = svcStore.updateServiceDef(dbHiveServiceDef);
					if(ret==null){
						logger.error("Error while updating "+SERVICEDBSTORE_SERVICEDEFBYNAME_HIVE_NAME+"service-def");
						throw new RuntimeException("Error while updating "+SERVICEDBSTORE_SERVICEDEFBYNAME_HIVE_NAME+"service-def");
					}
					xXServiceDefObj = daoMgr.getXXServiceDef().findByName(SERVICEDBSTORE_SERVICEDEFBYNAME_HIVE_NAME);
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
					}
				}
			}
			}catch(Exception e)
			{
				logger.error("Error while updating "+SERVICEDBSTORE_SERVICEDEFBYNAME_HIVE_NAME+"service-def", e);
			}
	}

	private boolean checkURLresourcePresent(List<RangerServiceDef.RangerResourceDef> resourceDefs) {
		boolean ret = false;
		for(RangerServiceDef.RangerResourceDef resourceDef : resourceDefs) {
			if ( URL_RESOURCE_NAME.equals(resourceDef.getName()) ) {
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
}
