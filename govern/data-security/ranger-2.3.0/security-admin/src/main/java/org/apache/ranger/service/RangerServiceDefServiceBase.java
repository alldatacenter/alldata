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

package org.apache.ranger.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.JSONUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.SearchField.DATA_TYPE;
import org.apache.ranger.common.SearchField.SEARCH_TYPE;
import org.apache.ranger.entity.*;
import org.apache.ranger.plugin.model.RangerServiceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerAccessTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerContextEnricherDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerDataMaskDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerDataMaskTypeDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerEnumElementDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerPolicyConditionDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerResourceDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerRowFilterDef;
import org.apache.ranger.plugin.model.RangerServiceDef.RangerServiceConfigDef;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.plugin.util.ServiceDefUtil;
import org.apache.ranger.view.RangerServiceDefList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class RangerServiceDefServiceBase<T extends XXServiceDefBase, V extends RangerServiceDef>
		extends RangerBaseModelService<T, V> {
	private static final Logger LOG = LoggerFactory.getLogger(RangerServiceDefServiceBase.class);

	private static final String OPTION_RESOURCE_ACCESS_TYPE_RESTRICTIONS = "__accessTypeRestrictions";
	private static final String OPTION_RESOURCE_IS_VALID_LEAF            = "__isValidLeaf";

	@Autowired
	RangerAuditFields<?> rangerAuditFields;

	@Autowired
	JSONUtil jsonUtil;
	
	@Autowired
	GUIDUtil guidUtil;

	public RangerServiceDefServiceBase() {
		super();

		searchFields.add(new SearchField(SearchFilter.SERVICE_TYPE, "obj.name", DATA_TYPE.STRING, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.SERVICE_TYPE_DISPLAY_NAME, "obj.displayName", DATA_TYPE.STRING, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.SERVICE_TYPE_ID, "obj.id", DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.IS_ENABLED, "obj.isEnabled", DATA_TYPE.BOOLEAN, SEARCH_TYPE.FULL));

		sortFields.add(new SortField(SearchFilter.CREATE_TIME, "obj.createTime"));
		sortFields.add(new SortField(SearchFilter.UPDATE_TIME, "obj.updateTime"));
		sortFields.add(new SortField(SearchFilter.SERVICE_TYPE_ID, "obj.id"));
		sortFields.add(new SortField(SearchFilter.SERVICE_TYPE, "obj.name"));
		sortFields.add(new SortField(SearchFilter.SERVICE_TYPE_DISPLAY_NAME, "obj.displayName"));
	}

	@Override
	protected V populateViewBean(T xServiceDef) {
		V serviceDef = super.populateViewBean((T) xServiceDef);
		Long serviceDefId = xServiceDef.getId();

		List<XXServiceConfigDef> xConfigs = daoMgr.getXXServiceConfigDef().findByServiceDefId(serviceDefId);
		if (!stringUtil.isEmpty(xConfigs)) {
			List<RangerServiceConfigDef> configs = new ArrayList<RangerServiceConfigDef>();
			for (XXServiceConfigDef xConfig : xConfigs) {
				RangerServiceConfigDef config = populateXXToRangerServiceConfigDef(xConfig);
				configs.add(config);
			}
			serviceDef.setConfigs(configs);
		}

		List<XXResourceDef> xResources = daoMgr.getXXResourceDef().findByServiceDefId(serviceDefId);
		if (!stringUtil.isEmpty(xResources)) {
			List<RangerResourceDef> resources = new ArrayList<RangerResourceDef>();
			for (XXResourceDef xResource : xResources) {
				RangerResourceDef resource = populateXXToRangerResourceDef(xResource);
				resources.add(resource);
			}
			serviceDef.setResources(resources);
		}

		List<XXAccessTypeDef>     xAccessTypes  = daoMgr.getXXAccessTypeDef().findByServiceDefId(serviceDefId);
		Map<String, List<String>> impliedGrants = daoMgr.getXXAccessTypeDefGrants().findImpliedGrantsByServiceDefId(serviceDefId);
		if (!stringUtil.isEmpty(xAccessTypes)) {
			List<RangerAccessTypeDef> accessTypes = new ArrayList<RangerAccessTypeDef>();
			for (XXAccessTypeDef xAtd : xAccessTypes) {
				RangerAccessTypeDef accessType = populateXXToRangerAccessTypeDef(xAtd, impliedGrants.get(xAtd.getName()));
				accessTypes.add(accessType);
			}
			serviceDef.setAccessTypes(accessTypes);
		}

		List<XXPolicyConditionDef> xPolicyConditions = daoMgr.getXXPolicyConditionDef()
				.findByServiceDefId(serviceDefId);
		if (!stringUtil.isEmpty(xPolicyConditions)) {
			List<RangerPolicyConditionDef> policyConditions = new ArrayList<RangerPolicyConditionDef>();
			for (XXPolicyConditionDef xPolicyCondDef : xPolicyConditions) {
				RangerPolicyConditionDef policyCondition = populateXXToRangerPolicyConditionDef(xPolicyCondDef);
				policyConditions.add(policyCondition);
			}
			serviceDef.setPolicyConditions(policyConditions);
		}

		List<XXContextEnricherDef> xContextEnrichers = daoMgr.getXXContextEnricherDef()
				.findByServiceDefId(serviceDefId);
		if (!stringUtil.isEmpty(xContextEnrichers)) {
			List<RangerContextEnricherDef> contextEnrichers = new ArrayList<RangerContextEnricherDef>();
			for (XXContextEnricherDef xContextEnricherDef : xContextEnrichers) {
				RangerContextEnricherDef contextEnricher = populateXXToRangerContextEnricherDef(xContextEnricherDef);
				contextEnrichers.add(contextEnricher);
			}
			serviceDef.setContextEnrichers(contextEnrichers);
		}

		List<XXEnumDef> xEnumList = daoMgr.getXXEnumDef().findByServiceDefId(serviceDefId);
		if (!stringUtil.isEmpty(xEnumList)) {
			List<RangerEnumDef> enums = new ArrayList<RangerEnumDef>();
			for (XXEnumDef xEnum : xEnumList) {
				RangerEnumDef vEnum = populateXXToRangerEnumDef(xEnum);
				enums.add(vEnum);
			}
			serviceDef.setEnums(enums);
		}

		RangerDataMaskDef  dataMaskDef  = new RangerDataMaskDef();
		RangerRowFilterDef rowFilterDef = new RangerRowFilterDef();

		List<XXDataMaskTypeDef> xDataMaskTypes = daoMgr.getXXDataMaskTypeDef().findByServiceDefId(serviceDefId);
		if (!stringUtil.isEmpty(xDataMaskTypes)) {
			List<RangerDataMaskTypeDef> dataMaskTypes = new ArrayList<RangerDataMaskTypeDef>();
			for (XXDataMaskTypeDef xDataMaskType : xDataMaskTypes) {
				RangerDataMaskTypeDef dataMaskType = populateXXToRangerDataMaskTypeDef(xDataMaskType);
				dataMaskTypes.add(dataMaskType);
			}

			dataMaskDef.setMaskTypes(dataMaskTypes);
		}

		if (!stringUtil.isEmpty(xResources)) {
			for (XXResourceDef xResource : xResources) {
				if (StringUtils.isNotEmpty(xResource.getDataMaskOptions())) {
					RangerResourceDef dataMaskResource = jsonToObject(xResource.getDataMaskOptions(), RangerResourceDef.class);

					dataMaskDef.getResources().add(dataMaskResource);
				}

				if (StringUtils.isNotEmpty(xResource.getRowFilterOptions())) {
					RangerResourceDef resource = jsonToObject(xResource.getRowFilterOptions(), RangerResourceDef.class);

					rowFilterDef.getResources().add(resource);
				}
			}
		}

		if (!stringUtil.isEmpty(xAccessTypes)) {
			for (XXAccessTypeDef xAtd : xAccessTypes) {
				if(StringUtils.isNotEmpty(xAtd.getDataMaskOptions())) {
					RangerAccessTypeDef dataMaskAccessType = jsonToObject(xAtd.getDataMaskOptions(), RangerAccessTypeDef.class);

					dataMaskDef.getAccessTypes().add(dataMaskAccessType);
				}

				if(StringUtils.isNotEmpty(xAtd.getRowFilterOptions())) {
					RangerAccessTypeDef accessType = jsonToObject(xAtd.getRowFilterOptions(), RangerAccessTypeDef.class);

					rowFilterDef.getAccessTypes().add(accessType);
				}
			}
		}
		serviceDef.setDataMaskDef(dataMaskDef);
		serviceDef.setRowFilterDef(rowFilterDef);

		ServiceDefUtil.normalize(serviceDef);

		return serviceDef;
	}

	@Override
	protected T mapViewToEntityBean(V vObj, T xObj, int operationContext) {

		String guid = (StringUtils.isEmpty(vObj.getGuid())) ? guidUtil.genGUID() : vObj.getGuid();

		xObj.setGuid(guid);
		xObj.setName(vObj.getName());
		xObj.setDisplayName(vObj.getDisplayName());
		xObj.setImplclassname(vObj.getImplClass());
		xObj.setLabel(vObj.getLabel());
		xObj.setDescription(vObj.getDescription());
		xObj.setRbkeylabel(vObj.getRbKeyLabel());
		xObj.setRbkeydescription(vObj.getRbKeyDescription());
		xObj.setIsEnabled(vObj.getIsEnabled());

		xObj.setDefOptions(mapToJsonString(vObj.getOptions()));

		return xObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T xObj) {
		vObj.setGuid(xObj.getGuid());
		vObj.setVersion(xObj.getVersion());
		vObj.setName(xObj.getName());
		vObj.setImplClass(xObj.getImplclassname());
		vObj.setLabel(xObj.getLabel());
		vObj.setDescription(xObj.getDescription());
		vObj.setOptions(jsonStringToMap(xObj.getDefOptions()));
		vObj.setRbKeyLabel(xObj.getRbkeylabel());
		vObj.setRbKeyDescription(xObj.getRbkeydescription());
		vObj.setIsEnabled(xObj.getIsEnabled());
		vObj.setDisplayName(xObj.getDisplayName());
		return vObj;
	}

	public XXServiceConfigDef populateRangerServiceConfigDefToXX(RangerServiceConfigDef vObj, XXServiceConfigDef xObj,
			XXServiceDef serviceDef, int operationContext) {
		if(serviceDef == null) {
			LOG.error("RangerServiceDefServiceBase.populateRangerServiceConfigDefToXX, serviceDef can not be null");
			throw restErrorUtil.createRESTException("RangerServiceDef cannot be null.", MessageEnums.DATA_NOT_FOUND);
		}
		
		xObj = rangerAuditFields.populateAuditFields(xObj, serviceDef);
		xObj.setDefid(serviceDef.getId());
		xObj.setItemId(vObj.getItemId());
		xObj.setName(vObj.getName());
		xObj.setType(vObj.getType());
		xObj.setSubtype(vObj.getSubType());
		xObj.setIsMandatory(vObj.getMandatory());
		xObj.setDefaultvalue(vObj.getDefaultValue());
		xObj.setValidationRegEx(vObj.getValidationRegEx());
		xObj.setValidationMessage(vObj.getValidationMessage());
		xObj.setUiHint(vObj.getUiHint());
		xObj.setLabel(vObj.getLabel());
		xObj.setDescription(vObj.getDescription());
		xObj.setRbkeylabel(vObj.getRbKeyLabel());
		xObj.setRbkeydescription(vObj.getRbKeyDescription());
		xObj.setRbKeyValidationMessage(vObj.getRbKeyValidationMessage());
		xObj.setOrder(AppConstants.DEFAULT_SORT_ORDER);
		return xObj;
	}

	public RangerServiceConfigDef populateXXToRangerServiceConfigDef(XXServiceConfigDef xObj) {
		RangerServiceConfigDef vObj = new RangerServiceConfigDef();
		vObj.setItemId(xObj.getItemId());
		vObj.setName(xObj.getName());
		vObj.setType(xObj.getType());
		vObj.setSubType(xObj.getSubtype());
		vObj.setMandatory(xObj.getIsMandatory());
		vObj.setDefaultValue(xObj.getDefaultvalue());
		vObj.setValidationRegEx(xObj.getValidationRegEx());
		vObj.setValidationMessage(xObj.getValidationMessage());
		vObj.setUiHint(xObj.getUiHint());
		vObj.setLabel(xObj.getLabel());
		vObj.setDescription(xObj.getDescription());
		vObj.setRbKeyLabel(xObj.getRbkeylabel());
		vObj.setRbKeyDescription(xObj.getRbkeydescription());
		vObj.setRbKeyValidationMessage(xObj.getRbKeyValidationMessage());
		return vObj;
	}
	
	public XXResourceDef populateRangerResourceDefToXX(RangerResourceDef vObj, XXResourceDef xObj,
			XXServiceDef serviceDef, int operationContext) {
		if(serviceDef == null) {
			LOG.error("RangerServiceDefServiceBase.populateRangerResourceDefToXX, serviceDef can not be null");
			throw restErrorUtil.createRESTException("RangerServiceDef cannot be null.", MessageEnums.DATA_NOT_FOUND);
		}
		
		xObj = rangerAuditFields.populateAuditFields(xObj, serviceDef);
		xObj.setDefid(serviceDef.getId());
		xObj.setItemId(vObj.getItemId());
		xObj.setName(vObj.getName());
		xObj.setType(vObj.getType());
		xObj.setLevel(vObj.getLevel());
		xObj.setMandatory(vObj.getMandatory());
		xObj.setLookupsupported(vObj.getLookupSupported());
		xObj.setRecursivesupported(vObj.getRecursiveSupported());
		xObj.setExcludessupported(vObj.getExcludesSupported());
		xObj.setMatcher(vObj.getMatcher());

		String              accessTypeRestrictions = objectToJson((HashSet<String>)vObj.getAccessTypeRestrictions());
		String              isValidLeaf            = objectToJson(vObj.getIsValidLeaf());
		Map<String, String> matcherOptions         = vObj.getMatcherOptions();

		if (StringUtils.isNotBlank(accessTypeRestrictions)) {
			matcherOptions.put(OPTION_RESOURCE_ACCESS_TYPE_RESTRICTIONS, accessTypeRestrictions);
		} else {
			matcherOptions.remove(OPTION_RESOURCE_ACCESS_TYPE_RESTRICTIONS);
		}

		if (StringUtils.isNotBlank(isValidLeaf)) {
			matcherOptions.put(OPTION_RESOURCE_IS_VALID_LEAF, isValidLeaf);
		} else {
			matcherOptions.remove(OPTION_RESOURCE_IS_VALID_LEAF);
		}

        xObj.setMatcheroptions(mapToJsonString(matcherOptions));

		xObj.setValidationRegEx(vObj.getValidationRegEx());
		xObj.setValidationMessage(vObj.getValidationMessage());
		xObj.setUiHint(vObj.getUiHint());
		xObj.setLabel(vObj.getLabel());
		xObj.setDescription(vObj.getDescription());
		xObj.setRbkeylabel(vObj.getRbKeyLabel());
		xObj.setRbkeydescription(vObj.getRbKeyDescription());
		xObj.setRbKeyValidationMessage(vObj.getRbKeyValidationMessage());
		xObj.setOrder(AppConstants.DEFAULT_SORT_ORDER);
		return xObj;
	}
	
	public RangerResourceDef populateXXToRangerResourceDef(XXResourceDef xObj) {
		RangerResourceDef vObj = new RangerResourceDef();
		vObj.setItemId(xObj.getItemId());
		vObj.setName(xObj.getName());
		vObj.setType(xObj.getType());
		vObj.setLevel(xObj.getLevel());		
		vObj.setMandatory(xObj.getMandatory());
		vObj.setLookupSupported(xObj.getLookupsupported());
		vObj.setRecursiveSupported(xObj.getRecursivesupported());
		vObj.setExcludesSupported(xObj.getExcludessupported());
		vObj.setMatcher(xObj.getMatcher());

		Map<String, String> matcherOptions = jsonStringToMap(xObj.getMatcheroptions());

		if (MapUtils.isNotEmpty(matcherOptions)) {
			String optionAccessTypeRestrictions = matcherOptions.remove(OPTION_RESOURCE_ACCESS_TYPE_RESTRICTIONS);
			String optionIsValidLeaf            = matcherOptions.remove(OPTION_RESOURCE_IS_VALID_LEAF);

			if (StringUtils.isNotBlank(optionAccessTypeRestrictions)) {
				Set<String> accessTypeRestrictions = new HashSet<>();

				accessTypeRestrictions = jsonToObject(optionAccessTypeRestrictions, accessTypeRestrictions.getClass());

				vObj.setAccessTypeRestrictions(accessTypeRestrictions);
			}

			if (StringUtils.isNotBlank(optionIsValidLeaf)) {
				Boolean isValidLeaf = jsonToObject(optionIsValidLeaf, Boolean.class);

				vObj.setIsValidLeaf(isValidLeaf);
			}
		}

		vObj.setMatcherOptions(matcherOptions);

		vObj.setValidationRegEx(xObj.getValidationRegEx());
		vObj.setValidationMessage(xObj.getValidationMessage());
		vObj.setUiHint(xObj.getUiHint());
		vObj.setLabel(xObj.getLabel());
		vObj.setDescription(xObj.getDescription());
		vObj.setRbKeyLabel(xObj.getRbkeylabel());
		vObj.setRbKeyDescription(xObj.getRbkeydescription());
		vObj.setRbKeyValidationMessage(xObj.getRbKeyValidationMessage());

		XXResourceDef parent = daoMgr.getXXResourceDef().getById(xObj.getParent());
		String parentName = (parent != null) ? parent.getName() : null;
		vObj.setParent(parentName);
		
		return vObj;
	}
	
	public XXAccessTypeDef populateRangerAccessTypeDefToXX(RangerAccessTypeDef vObj, XXAccessTypeDef xObj,
			XXServiceDef serviceDef, int operationContext) {
		if(serviceDef == null) {
			LOG.error("RangerServiceDefServiceBase.populateRangerAccessTypeDefToXX, serviceDef can not be null");
			throw restErrorUtil.createRESTException("RangerServiceDef cannot be null.", MessageEnums.DATA_NOT_FOUND);
		}
		
		xObj = rangerAuditFields.populateAuditFields(xObj, serviceDef);
		xObj.setDefid(serviceDef.getId());
		xObj.setItemId(vObj.getItemId());
		xObj.setName(vObj.getName());
		xObj.setLabel(vObj.getLabel());
		xObj.setRbkeylabel(vObj.getRbKeyLabel());
		xObj.setOrder(AppConstants.DEFAULT_SORT_ORDER);
		return xObj;
	}
	
	public RangerAccessTypeDef populateXXToRangerAccessTypeDef(XXAccessTypeDef xObj) {
		List<String> impliedGrants = daoMgr.getXXAccessTypeDefGrants().findImpliedGrantsByATDId(xObj.getId());

		return populateXXToRangerAccessTypeDef(xObj, impliedGrants);
	}

	public RangerAccessTypeDef populateXXToRangerAccessTypeDef(XXAccessTypeDef xObj, List<String> impliedGrants) {
		RangerAccessTypeDef vObj = new RangerAccessTypeDef();

		if (impliedGrants == null) {
			impliedGrants = new ArrayList<>();
		}

		vObj.setItemId(xObj.getItemId());
		vObj.setName(xObj.getName());
		vObj.setLabel(xObj.getLabel());
		vObj.setRbKeyLabel(xObj.getRbkeylabel());
		vObj.setImpliedGrants(impliedGrants);

		return vObj;
	}

	public XXPolicyConditionDef populateRangerPolicyConditionDefToXX(RangerPolicyConditionDef vObj,
			XXPolicyConditionDef xObj, XXServiceDef serviceDef, int operationContext) {
		if(serviceDef == null) {
			LOG.error("RangerServiceDefServiceBase.populateRangerPolicyConditionDefToXX, serviceDef can not be null");
			throw restErrorUtil.createRESTException("RangerServiceDef cannot be null.", MessageEnums.DATA_NOT_FOUND);
		}
		
		xObj = rangerAuditFields.populateAuditFields(xObj, serviceDef);
		xObj.setDefid(serviceDef.getId());
		xObj.setItemId(vObj.getItemId());
		xObj.setName(vObj.getName());
		xObj.setEvaluator(vObj.getEvaluator());
		xObj.setEvaluatoroptions(mapToJsonString(vObj.getEvaluatorOptions()));
		xObj.setValidationRegEx(vObj.getValidationRegEx());
		xObj.setValidationMessage(vObj.getValidationMessage());
		xObj.setUiHint(vObj.getUiHint());
		xObj.setLabel(vObj.getLabel());
		xObj.setDescription(vObj.getDescription());
		xObj.setRbkeylabel(vObj.getRbKeyLabel());
		xObj.setRbkeydescription(vObj.getRbKeyDescription());
		xObj.setRbKeyValidationMessage(vObj.getRbKeyValidationMessage());
		xObj.setOrder(AppConstants.DEFAULT_SORT_ORDER);
		return xObj;
	}
	
	public RangerPolicyConditionDef populateXXToRangerPolicyConditionDef(XXPolicyConditionDef xObj) {
		RangerPolicyConditionDef vObj = new RangerPolicyConditionDef();
		vObj.setItemId(xObj.getItemId());
		vObj.setName(xObj.getName());
		vObj.setEvaluator(xObj.getEvaluator());
		vObj.setEvaluatorOptions(jsonStringToMap(xObj.getEvaluatoroptions()));
		vObj.setValidationRegEx(xObj.getValidationRegEx());
		vObj.setValidationMessage(xObj.getValidationMessage());
		vObj.setUiHint(xObj.getUiHint());
		vObj.setLabel(xObj.getLabel());
		vObj.setDescription(xObj.getDescription());
		vObj.setRbKeyLabel(xObj.getRbkeylabel());
		vObj.setRbKeyDescription(xObj.getRbkeydescription());
		vObj.setRbKeyValidationMessage(xObj.getRbKeyValidationMessage());
		return vObj;
	}
	
	public XXContextEnricherDef populateRangerContextEnricherDefToXX(RangerContextEnricherDef vObj,
			XXContextEnricherDef xObj, XXServiceDef serviceDef, int operationContext) {
		if(serviceDef == null) {
			LOG.error("RangerServiceDefServiceBase.populateRangerContextEnricherDefToXX, serviceDef can not be null");
			throw restErrorUtil.createRESTException("RangerServiceDef cannot be null.", MessageEnums.DATA_NOT_FOUND);
		}

		xObj = rangerAuditFields.populateAuditFields(xObj, serviceDef);
		xObj.setDefid(serviceDef.getId());
		xObj.setItemId(vObj.getItemId());
		xObj.setName(vObj.getName());
		xObj.setEnricher(vObj.getEnricher());
		xObj.setEnricherOptions(mapToJsonString(vObj.getEnricherOptions()));
		xObj.setOrder(AppConstants.DEFAULT_SORT_ORDER);
		return xObj;
	}
	
	public RangerContextEnricherDef populateXXToRangerContextEnricherDef(XXContextEnricherDef xObj) {
		RangerContextEnricherDef vObj = new RangerContextEnricherDef();
		vObj.setItemId(xObj.getItemId());
		vObj.setName(xObj.getName());
		vObj.setEnricher(xObj.getEnricher());
		vObj.setEnricherOptions(jsonStringToMap(xObj.getEnricherOptions()));
		return vObj;
	}
	
	public XXEnumDef populateRangerEnumDefToXX(RangerEnumDef vObj, XXEnumDef xObj, XXServiceDef serviceDef,
			int operationContext) {
		if(serviceDef == null) {
			LOG.error("RangerServiceDefServiceBase.populateRangerEnumDefToXX, serviceDef can not be null");
			throw restErrorUtil.createRESTException("RangerServiceDef cannot be null.", MessageEnums.DATA_NOT_FOUND);
		}

		xObj = rangerAuditFields.populateAuditFields(xObj, serviceDef);
		xObj.setDefid(serviceDef.getId());
		xObj.setItemId(vObj.getItemId());
		xObj.setName(vObj.getName());
		xObj.setDefaultindex(vObj.getDefaultIndex());
		return xObj;
	}
	
	public RangerEnumDef populateXXToRangerEnumDef(XXEnumDef xObj) {
		RangerEnumDef vObj = new RangerEnumDef();
		vObj.setItemId(xObj.getItemId());
		vObj.setName(xObj.getName());
		vObj.setDefaultIndex(xObj.getDefaultindex());
		
		List<XXEnumElementDef> xElements = daoMgr.getXXEnumElementDef().findByEnumDefId(xObj.getId());
		List<RangerEnumElementDef> elements = new ArrayList<RangerEnumElementDef>();
		
		for(XXEnumElementDef xEle : xElements) {
			RangerEnumElementDef element = populateXXToRangerEnumElementDef(xEle);
			elements.add(element);
		}
		vObj.setElements(elements);
		
		return vObj;
	}
	
	public XXEnumElementDef populateRangerEnumElementDefToXX(RangerEnumElementDef vObj, XXEnumElementDef xObj,
			XXEnumDef enumDef, int operationContext) {
		if(enumDef == null) {
			LOG.error("RangerServiceDefServiceBase.populateRangerEnumElementDefToXX, enumDef can not be null");
			throw restErrorUtil.createRESTException("enumDef cannot be null.", MessageEnums.DATA_NOT_FOUND);
		}

		xObj = rangerAuditFields.populateAuditFields(xObj, enumDef);
		xObj.setEnumdefid(enumDef.getId());
		xObj.setItemId(vObj.getItemId());
		xObj.setName(vObj.getName());
		xObj.setLabel(vObj.getLabel());
		xObj.setRbkeylabel(vObj.getRbKeyLabel());
		xObj.setOrder(AppConstants.DEFAULT_SORT_ORDER);
		return xObj;
	}
	
	public RangerEnumElementDef populateXXToRangerEnumElementDef(XXEnumElementDef xObj) {
		RangerEnumElementDef vObj = new RangerEnumElementDef();
		vObj.setItemId(xObj.getItemId());
		vObj.setName(xObj.getName());
		vObj.setLabel(xObj.getLabel());
		vObj.setRbKeyLabel(xObj.getRbkeylabel());
		return vObj;
	}

	public XXDataMaskTypeDef populateRangerDataMaskDefToXX(RangerDataMaskTypeDef vObj, XXDataMaskTypeDef xObj,
														   XXServiceDef serviceDef, int operationContext) {
		if(serviceDef == null) {
			LOG.error("RangerServiceDefServiceBase.populateRangerDataMaskDefToXX, serviceDef can not be null");
			throw restErrorUtil.createRESTException("RangerServiceDef cannot be null.", MessageEnums.DATA_NOT_FOUND);
		}

		xObj = rangerAuditFields.populateAuditFields(xObj, serviceDef);
		xObj.setDefid(serviceDef.getId());
		xObj.setItemId(vObj.getItemId());
		xObj.setName(vObj.getName());
		xObj.setLabel(vObj.getLabel());
		xObj.setDescription(vObj.getDescription());
		xObj.setTransformer(vObj.getTransformer());
		xObj.setDataMaskOptions(mapToJsonString(vObj.getDataMaskOptions()));
		xObj.setRbkeylabel(vObj.getRbKeyLabel());
		xObj.setRbKeyDescription(vObj.getRbKeyDescription());
		xObj.setOrder(AppConstants.DEFAULT_SORT_ORDER);
		return xObj;
	}

	public RangerDataMaskTypeDef populateXXToRangerDataMaskTypeDef(XXDataMaskTypeDef xObj) {
		RangerDataMaskTypeDef vObj = new RangerDataMaskTypeDef();
		vObj.setItemId(xObj.getItemId());
		vObj.setName(xObj.getName());
		vObj.setLabel(xObj.getLabel());
		vObj.setDescription(xObj.getDescription());
		vObj.setTransformer(xObj.getTransformer());
		vObj.setDataMaskOptions(jsonStringToMap(xObj.getDataMaskOptions()));
		vObj.setRbKeyLabel(xObj.getRbkeylabel());
		vObj.setRbKeyDescription(xObj.getRbKeyDescription());

		return vObj;
	}

	public RangerServiceDefList searchRangerServiceDefs(SearchFilter searchFilter) {
		RangerServiceDefList retList = new RangerServiceDefList();
		int startIndex = searchFilter.getStartIndex();
		int pageSize = searchFilter.getMaxRows();
		String denyCondition = searchFilter.getParam(SearchFilter.FETCH_DENY_CONDITION);
		searchFilter.setStartIndex(0);
		searchFilter.setMaxRows(Integer.MAX_VALUE);

		boolean isAuditPage=false;
		if(searchFilter.getParam("pageSource")!=null){
			isAuditPage=true;
		}
		List<T> xSvcDefList = searchResources(searchFilter, searchFields, sortFields,
				retList);
		List<T> permittedServiceDefs = new ArrayList<T>();
		for (T xSvcDef : xSvcDefList) {
			if ((bizUtil.hasAccess(xSvcDef, null) || (bizUtil.isAdmin() && isAuditPage)) || ("true".equals(denyCondition))) {
				permittedServiceDefs.add(xSvcDef);
			}
		}
		if (!permittedServiceDefs.isEmpty()) {
			populatePageList(permittedServiceDefs, startIndex, pageSize, retList);
		}
		return retList;

	}
	

	private void populatePageList(List<T> xxObjList, int startIndex, int pageSize,
			RangerServiceDefList retList) {
		List<RangerServiceDef> onePageList = new ArrayList<RangerServiceDef>();

		for (int i = startIndex; i < pageSize + startIndex && i < xxObjList.size(); i++) {
			onePageList.add(populateViewBean(xxObjList.get(i)));
		}
		retList.setServiceDefs(onePageList);
		retList.setStartIndex(startIndex);
		retList.setPageSize(pageSize);
		retList.setResultSize(onePageList.size());
		retList.setTotalCount(xxObjList.size());
	}

	private String mapToJsonString(Map<String, String> map) {
		String ret = null;

		if(map != null) {
			try {
				ret = jsonUtil.readMapToString(map);
			} catch(Exception excp) {
				LOG.warn("mapToJsonString() failed to convert map: " + map, excp);
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

	public String objectToJson(Serializable obj) {
		String ret = null;

		if(obj != null) {
			try {
				ret = jsonUtil.writeObjectAsString(obj);
			} catch(Exception excp) {
				LOG.warn("objectToJson() failed to convert object to json: " + obj, excp);
			}
		}

		return ret;
	}

	public <DST> DST jsonToObject(String jsonStr, Class<DST> clz) {
		DST ret = null;

		if(StringUtils.isNotEmpty(jsonStr)) {
			try {
				ret = jsonUtil.writeJsonToJavaObject(jsonStr, clz);
			} catch(Exception excp) {
				LOG.warn("jsonToObject() failed to convert json to object: " + jsonStr, excp);
			}
		}

		return ret;
	}

}
