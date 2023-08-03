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
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.store.AbstractServiceStore;
import org.apache.ranger.plugin.store.EmbeddedServiceDefsUtil;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.service.XPermMapService;
import org.apache.ranger.service.XPolicyService;
import org.apache.ranger.services.tag.RangerServiceTag;
import org.apache.ranger.util.CLIUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PatchForTagServiceDefUpdate_J10028 extends BaseLoader {
	private static final Logger logger = LoggerFactory.getLogger(PatchForTagServiceDefUpdate_J10028.class);
	private static final String SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME = "tag";

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
			PatchForTagServiceDefUpdate_J10028 loader = (PatchForTagServiceDefUpdate_J10028) CLIUtil.getBean(PatchForTagServiceDefUpdate_J10028.class);
			loader.init();
			while (loader.isMoreToProcess()) {
				loader.load();
			}
			logger.info("Load complete. Exiting.");
			System.exit(0);
		} catch (Exception e) {
			logger.error("Error loading", e);
			System.exit(1);
		}
	}

	@Override
	public void printStats() {
		logger.info("PatchForTagServiceDefUpdate data ");
	}

	@Override
	public void execLoad() {
		logger.info("==> PatchForTagServiceDefUpdate.execLoad()");
		try {
			if (!updateTagServiceDef()) {
				logger.error("Failed to apply the patch.");
				System.exit(1);
			}
		} catch (Exception e) {
			logger.error("Error while updateTagServiceDef()data.", e);
			System.exit(1);
		}
		logger.info("<== PatchForTagServiceDefUpdate.execLoad()");
	}

	@Override
	public void init() throws Exception {
		// Do Nothing
	}

	private boolean updateTagServiceDef() throws Exception {
		RangerServiceDef ret;
		RangerServiceDef embeddedTagServiceDef;
		RangerServiceDef dbTagServiceDef;
		XXServiceDef xXServiceDefObj;

		embeddedTagServiceDef = EmbeddedServiceDefsUtil.instance().getEmbeddedServiceDef(SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME);

		if (embeddedTagServiceDef != null) {
			xXServiceDefObj = daoMgr.getXXServiceDef().findByName(SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME);
			Map<String, String> serviceDefOptionsPreUpdate;
			String jsonPreUpdate;

			if (xXServiceDefObj != null) {
				jsonPreUpdate = xXServiceDefObj.getDefOptions();
				serviceDefOptionsPreUpdate = jsonStringToMap(jsonPreUpdate);
			} else {
				logger.error("Tag service-definition does not exist in the Ranger DAO.");
				return false;
			}
			dbTagServiceDef = svcDBStore.getServiceDefByName(SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME);

			boolean isTagServiceUpdated;
			if (dbTagServiceDef != null) {
				isTagServiceUpdated = updateResourceInTagServiceDef(dbTagServiceDef);
			} else {
				logger.error("Tag service-definition does not exist in the db store.");
				return false;
			}
			if (isTagServiceUpdated) {
				ret = svcStore.updateServiceDef(dbTagServiceDef);
				if (ret == null) {
					throw new RuntimeException("Error while updating " + SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME + " service-def");
				}
				xXServiceDefObj = daoMgr.getXXServiceDef().findByName(SERVICEDBSTORE_SERVICEDEFBYNAME_TAG_NAME);
				if (xXServiceDefObj != null) {
					String jsonStrPostUpdate = xXServiceDefObj.getDefOptions();
					Map<String, String> serviceDefOptionsPostUpdate = jsonStringToMap(jsonStrPostUpdate);
					if (serviceDefOptionsPostUpdate != null && serviceDefOptionsPostUpdate.containsKey(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES)) {
						if (serviceDefOptionsPreUpdate == null || !serviceDefOptionsPreUpdate.containsKey(RangerServiceDef.OPTION_ENABLE_DENY_AND_EXCEPTIONS_IN_POLICIES)) {
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
				} else {
					logger.error("Tag service-definition does not exist in the Ranger DAO.");
					return false;
				}
			}
		} else {
			logger.error("The embedded Tag service-definition does not exist.");
			return false;
		}
		return true;
	}

	private String mapToJsonString(Map<String, String> map) {
		String ret = null;
		if (map != null) {
			try {
				ret = jsonUtil.readMapToString(map);
			} catch (Exception ex) {
				logger.warn("mapToJsonString() failed to convert map: " + map, ex);
			}
		}
		return ret;
	}

	private Map<String, String> jsonStringToMap(String jsonStr) {
		Map<String, String> ret = null;
		if (!StringUtils.isEmpty(jsonStr)) {
			try {
				ret = jsonUtil.jsonToMap(jsonStr);
			} catch (Exception ex) {
				// fallback to earlier format: "name1=value1;name2=value2"
				for (String optionString : jsonStr.split(";")) {
					if (StringUtils.isEmpty(optionString)) {
						continue;
					}
					String[] nvArr = optionString.split("=");
					String name = (nvArr.length > 0) ? nvArr[0].trim() : null;
					String value = (nvArr.length > 1) ? nvArr[1].trim() : null;
					if (StringUtils.isEmpty(name)) {
						continue;
					}
					if (ret == null) {
						ret = new HashMap<>();
					}
					ret.put(name, value);
				}
			}
		}
		return ret;
	}

	private boolean updateResourceInTagServiceDef(RangerServiceDef tagServiceDef) {
		if (logger.isDebugEnabled()) {
			logger.debug("==> PatchForTagServiceDefUpdate_J10028.updateResourceInTagServiceDef(" + tagServiceDef + ")");
		}
		boolean ret = false;

		final RangerServiceDef.RangerResourceDef accessPolicyTagResource = getResourceDefForTagResource(tagServiceDef.getResources());

		if (accessPolicyTagResource != null) {

			RangerServiceDef.RangerDataMaskDef dataMaskDef = tagServiceDef.getDataMaskDef();

			if (dataMaskDef != null) {
				if (CollectionUtils.isNotEmpty(dataMaskDef.getAccessTypes())) {
					addOrUpdateResourceDefForTagResource(dataMaskDef.getResources(), accessPolicyTagResource);
					ret = true;
				} else {
					if (CollectionUtils.isNotEmpty(dataMaskDef.getResources())) {
						dataMaskDef.setResources(null);
						ret = true;
					}
				}
			}

			RangerServiceDef.RangerRowFilterDef rowFilterDef = tagServiceDef.getRowFilterDef();

			if (rowFilterDef != null) {
				boolean autopropagateRowfilterdefToTag = config.getBoolean(AbstractServiceStore.AUTOPROPAGATE_ROWFILTERDEF_TO_TAG_PROP, AbstractServiceStore.AUTOPROPAGATE_ROWFILTERDEF_TO_TAG_PROP_DEFAULT);
				if (autopropagateRowfilterdefToTag) {
					if (CollectionUtils.isNotEmpty(rowFilterDef.getAccessTypes())) {
						addOrUpdateResourceDefForTagResource(rowFilterDef.getResources(), accessPolicyTagResource);
						ret = true;
					} else {
						if (CollectionUtils.isNotEmpty(rowFilterDef.getResources())) {
							rowFilterDef.setResources(null);
							ret = true;
						}
					}
				}
			}
		} else {
			logger.warn("Resource with name :[" + RangerServiceTag.TAG_RESOURCE_NAME + "] not found in  tag-service-definition!!");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("<== PatchForTagServiceDefUpdate_J10028.updateResourceInTagServiceDef(" + tagServiceDef + ") : " + ret);
		}

		return ret;
	}

	private RangerServiceDef.RangerResourceDef getResourceDefForTagResource(List<RangerServiceDef.RangerResourceDef> resourceDefs) {

		RangerServiceDef.RangerResourceDef ret = null;

		if (CollectionUtils.isNotEmpty(resourceDefs)) {
			for (RangerServiceDef.RangerResourceDef resourceDef : resourceDefs) {
				if (resourceDef.getName().equals(RangerServiceTag.TAG_RESOURCE_NAME)) {
					ret = resourceDef;
					break;
				}
			}
		}

		return ret;
	}

	private void addOrUpdateResourceDefForTagResource(List<RangerServiceDef.RangerResourceDef> resourceDefs, RangerServiceDef.RangerResourceDef tagResourceDef) {

		RangerServiceDef.RangerResourceDef tagResourceDefInResourceDefs = getResourceDefForTagResource(resourceDefs);

		if (tagResourceDefInResourceDefs == null) {
			resourceDefs.add(tagResourceDef);
		} else {
			tagResourceDefInResourceDefs.setDescription(tagResourceDef.getDescription());
			tagResourceDefInResourceDefs.setLabel(tagResourceDef.getLabel());
			tagResourceDefInResourceDefs.setValidationMessage(tagResourceDef.getValidationMessage());
			tagResourceDefInResourceDefs.setValidationRegEx(tagResourceDef.getValidationRegEx());
			tagResourceDefInResourceDefs.setRbKeyDescription(tagResourceDef.getRbKeyDescription());
			tagResourceDefInResourceDefs.setRbKeyLabel(tagResourceDef.getRbKeyLabel());
			tagResourceDefInResourceDefs.setRbKeyValidationMessage(tagResourceDef.getRbKeyValidationMessage());
			tagResourceDefInResourceDefs.setUiHint(tagResourceDef.getUiHint());
			tagResourceDefInResourceDefs.setMatcher(tagResourceDef.getMatcher());
			tagResourceDefInResourceDefs.setMatcherOptions(tagResourceDef.getMatcherOptions());
			tagResourceDefInResourceDefs.setLookupSupported(tagResourceDef.getLookupSupported());
			tagResourceDefInResourceDefs.setExcludesSupported(tagResourceDef.getExcludesSupported());
			tagResourceDefInResourceDefs.setRecursiveSupported(tagResourceDef.getRecursiveSupported());
			tagResourceDefInResourceDefs.setMandatory(tagResourceDef.getMandatory());
			tagResourceDefInResourceDefs.setLevel(tagResourceDef.getLevel());
			tagResourceDefInResourceDefs.setIsValidLeaf(tagResourceDef.getIsValidLeaf());
			tagResourceDefInResourceDefs.setParent(tagResourceDef.getParent());
		}
	}
}
