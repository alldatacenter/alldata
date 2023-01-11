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
import org.apache.ranger.entity.XXServiceDef;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PatchForKafkaServiceDefUpdate_J10015 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchForKafkaServiceDefUpdate_J10015.class);
	public static final String SERVICEDBSTORE_SERVICEDEFBYNAME_KAFKA_NAME  = "kafka";
	public static final String TRANSACTIONALID_RESOURCE_NAME ="transactionalid";

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
			PatchForKafkaServiceDefUpdate_J10015 loader = (PatchForKafkaServiceDefUpdate_J10015) CLIUtil.getBean(PatchForKafkaServiceDefUpdate_J10015.class);
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
		logger.info("==> PatchForKafkaServiceDefUpdate_J10015.execLoad()");
		try {
			updateHiveServiceDef();
		} catch (Exception e) {
			logger.error("Error while applying PatchForKafkaServiceDefUpdate_J10015...", e);
		}
		logger.info("<== PatchForKafkaServiceDefUpdate_J10015.execLoad()");
	}

	@Override
	public void printStats() {
		logger.info("PatchForKafkaServiceDefUpdate_J10015 ");
	}

	private void updateHiveServiceDef(){
		RangerServiceDef ret  					 = null;
		RangerServiceDef embeddedKafkaServiceDef = null;
		RangerServiceDef dbKafkaServiceDef 		 = null;
		List<RangerServiceDef.RangerResourceDef> 	embeddedKafkaResourceDefs  = null;
		List<RangerServiceDef.RangerAccessTypeDef> 	embeddedKafkaAccessTypes   = null;
		XXServiceDef xXServiceDefObj			= null;
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
						// This is to check if URL def is added to the resource definition, if so update the resource def and accessType def
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
			if (TRANSACTIONALID_RESOURCE_NAME.equals(resourceDef.getName()) ) {
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
