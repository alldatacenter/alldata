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

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.SearchField.DATA_TYPE;
import org.apache.ranger.common.SearchField.SEARCH_TYPE;
import org.apache.ranger.common.SortField.SORT_ORDER;
import org.apache.ranger.entity.XXPolicyBase;
import org.apache.ranger.entity.XXSecurityZone;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.util.SearchFilter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public abstract class RangerPolicyServiceBase<T extends XXPolicyBase, V extends RangerPolicy> extends
		RangerBaseModelService<T, V> {

    public static final String OPTION_POLICY_VALIDITY_SCHEDULES = "POLICY_VALIDITY_SCHEDULES";

    @Autowired
	GUIDUtil guidUtil;

	public RangerPolicyServiceBase() {
		super();
		searchFields.add(new SearchField(SearchFilter.SERVICE_TYPE, "xSvcDef.name", DATA_TYPE.STRING, SEARCH_TYPE.FULL,
				"XXServiceDef xSvcDef, XXService xSvc", "xSvc.type = xSvcDef.id and xSvc.id = obj.service"));
		searchFields.add(new SearchField(SearchFilter.SERVICE_TYPE_ID, "xSvc.type", DATA_TYPE.INTEGER,
				SEARCH_TYPE.FULL, "XXService xSvc", "xSvc.id = obj.service"));
		searchFields.add(new SearchField(SearchFilter.SERVICE_NAME, "xSvc.name", DATA_TYPE.STRING, SEARCH_TYPE.FULL,
				"XXService xSvc", "xSvc.id = obj.service"));
		searchFields.add(new SearchField(SearchFilter.SERVICE_ID, "xSvc.id", DATA_TYPE.INTEGER, SEARCH_TYPE.FULL,
				"XXService xSvc", "xSvc.id = obj.service"));
		searchFields
				.add(new SearchField(SearchFilter.IS_ENABLED, "obj.isEnabled", DATA_TYPE.BOOLEAN, SEARCH_TYPE.FULL));
		//might need updation
		/*searchFields.add(new SearchField(SearchFilter.IS_RECURSIVE,"xPolRes.isRecursive",DATA_TYPE.BOOLEAN,SEARCH_TYPE.FULL,
				"XXPolicyResource xPolRes","obj.id=xPolRes.policyId"));*/
		searchFields.add(new SearchField(SearchFilter.POLICY_ID, "obj.id", DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.POLICY_NAME, "obj.name", DATA_TYPE.STRING, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.GUID, "obj.guid", DATA_TYPE.STRING, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.USER, "xUser.name", DATA_TYPE.STRING, SEARCH_TYPE.FULL,
				"XXUser xUser, XXPolicyRefUser refUser", "obj.id = refUser.policyId "
						+ "and xUser.id = refUser.userId"));
		searchFields.add(new SearchField(SearchFilter.GROUP, "xGrp.name", DATA_TYPE.STRING, SEARCH_TYPE.FULL,
				"XXGroup xGrp , XXPolicyRefGroup refGroup", "obj.id = refGroup.policyId "
						+ "and xGrp.id = refGroup.groupId"));
		searchFields.add(new SearchField(SearchFilter.ROLE, "xRole.name", DATA_TYPE.STRING, SEARCH_TYPE.FULL,
				"XXRole xRole , XXPolicyRefRole refRole", "obj.id = refRole.policyId "
				+ "and xRole.id = refRole.roleId"));
		//might need updation
		/*searchFields.add(new SearchField(SearchFilter.POL_RESOURCE, "resMap.value", DATA_TYPE.STRING,
				SEARCH_TYPE.PARTIAL, "XXPolicyResourceMap resMap, XXPolicyResource polRes",
				"resMap.resourceId = polRes.id and polRes.policyId = obj.id"));
                /*searchFields.add(new SearchField(SearchFilter.POLICY_LABELS_PARTIAL, "obj.label_name", DATA_TYPE.STRING,
                                SEARCH_TYPE.PARTIAL));*/
		searchFields.add(new SearchField(SearchFilter.POLICY_NAME_PARTIAL, "obj.name", DATA_TYPE.STRING,
				SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField(SearchFilter.POLICY_TYPE, "obj.policyType", DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.ZONE_NAME, "xZone.name", DATA_TYPE.STRING, SEARCH_TYPE.FULL,
				"XXSecurityZone xZone", "xZone.id = obj.zoneId"));
		searchFields.add(new SearchField(SearchFilter.ZONE_ID, "xZone.id", DATA_TYPE.INTEGER, SEARCH_TYPE.FULL,
				"XXSecurityZone xZone", "xZone.id = obj.zoneId"));

		sortFields.add(new SortField(SearchFilter.CREATE_TIME, "obj.createTime"));
		sortFields.add(new SortField(SearchFilter.UPDATE_TIME, "obj.updateTime"));
		sortFields.add(new SortField(SearchFilter.POLICY_ID, "obj.id", true, SORT_ORDER.ASC));
		sortFields.add(new SortField(SearchFilter.POLICY_NAME, "obj.name"));

	}

	@Override
	protected T mapViewToEntityBean(V vObj, T xObj, int OPERATION_CONTEXT) {
		XXService xService = daoMgr.getXXService().findByName(vObj.getService());
		if (xService == null) {
			throw restErrorUtil.createRESTException("No corresponding service found for policyName: " + vObj.getName()
					+ "Service Not Found : " + vObj.getService(), MessageEnums.INVALID_INPUT_DATA);
		}

		XXServiceDef xServiceDef = daoMgr.getXXServiceDef().getById(xService.getType());
		if (xServiceDef != null) {
			vObj.setServiceType(xServiceDef.getName());
		}

		String guid = vObj.getGuid();
		if (StringUtils.isEmpty(guid)) {
			guid = guidUtil.genGUID();
			vObj.setGuid(guid);
		}
		Integer policyPriority = vObj.getPolicyPriority();
		if (policyPriority == null) {
			policyPriority = RangerPolicy.POLICY_PRIORITY_NORMAL;
			vObj.setPolicyPriority(policyPriority);
		}
		Integer policyType = vObj.getPolicyType();
		if (policyType == null) {
			policyType = RangerPolicy.POLICY_TYPE_ACCESS;
			vObj.setPolicyType(policyType);
		}

		xObj.setGuid(guid);
		xObj.setVersion(vObj.getVersion());
		xObj.setService(xService.getId());
		xObj.setName(StringUtils.trim(vObj.getName()));
		xObj.setPolicyType(policyType);
		xObj.setPolicyPriority(policyPriority);
		xObj.setDescription(vObj.getDescription());
		xObj.setResourceSignature(vObj.getResourceSignature());
		xObj.setIsAuditEnabled(vObj.getIsAuditEnabled());
		xObj.setIsEnabled(vObj.getIsEnabled());
		Long zoneId = convertZoneNameToZoneId(vObj.getZoneName(), vObj);

		xObj.setZoneId(zoneId);

		String              validitySchedules = JsonUtils.listToJson(vObj.getValiditySchedules());
		Map<String, Object> options           = vObj.getOptions();

		if (options == null) {
			options = new HashMap<>();
		}

		if (StringUtils.isNotBlank(validitySchedules)) {
			options.put(OPTION_POLICY_VALIDITY_SCHEDULES, validitySchedules);
		} else {
			options.remove(OPTION_POLICY_VALIDITY_SCHEDULES);
		}

		xObj.setOptions(JsonUtils.mapToJson(options));

		xObj.setPolicyText(JsonUtils.objectToJson(vObj));

		return xObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T xObj) {
		XXService xService = daoMgr.getXXService().getById(xObj.getService());
		XXServiceDef xServiceDef = daoMgr.getXXServiceDef().getById(xService.getType());
		vObj.setGuid(xObj.getGuid());
		vObj.setVersion(xObj.getVersion());
		vObj.setService(xService.getName());
		vObj.setServiceType(xServiceDef.getName());
		vObj.setName(StringUtils.trim(xObj.getName()));
		vObj.setPolicyType(xObj.getPolicyType() == null ? RangerPolicy.POLICY_TYPE_ACCESS : xObj.getPolicyType());
		vObj.setPolicyPriority(xObj.getPolicyPriority() == null ? RangerPolicy.POLICY_PRIORITY_NORMAL : xObj.getPolicyPriority());
		vObj.setDescription(xObj.getDescription());
		vObj.setResourceSignature(xObj.getResourceSignature());
		vObj.setIsEnabled(xObj.getIsEnabled());
		vObj.setIsAuditEnabled(xObj.getIsAuditEnabled());
		String zoneName = convertZoneIdToZoneName(xObj.getZoneId(), vObj);
		vObj.setZoneName(zoneName);

		String policyText = xObj.getPolicyText();

		RangerPolicy ret = JsonUtils.jsonToObject(policyText, RangerPolicy.class);

		if (ret != null) {
			vObj.setOptions(ret.getOptions());
			vObj.setValiditySchedules(ret.getValiditySchedules());
			vObj.setPolicyLabels(ret.getPolicyLabels());
		}

		return vObj;
	}

	private Long convertZoneNameToZoneId(String zoneName, V vObj) {
		if (StringUtils.isEmpty(zoneName)) return RangerSecurityZone.RANGER_UNZONED_SECURITY_ZONE_ID;
		XXSecurityZone zone = daoMgr.getXXSecurityZoneDao().findByZoneName(zoneName);
		if (zone == null) {
			throw restErrorUtil.createRESTException("No corresponding zone found for policyName: " + vObj.getName()
					+ "Zone Not Found : " + zoneName, MessageEnums.INVALID_INPUT_DATA);
		}
		return zone.getId();
	}

	private String convertZoneIdToZoneName(Long zoneId, V vObj) {
		if (zoneId == null) {
			throw restErrorUtil.createRESTException("No corresponding zone found for policyName: " + vObj.getName()
					+ "Zone Not Found : " + zoneId, MessageEnums.INVALID_INPUT_DATA);
		}
		if (zoneId.equals(RangerSecurityZone.RANGER_UNZONED_SECURITY_ZONE_ID)) {
			return StringUtils.EMPTY;
		}
		XXSecurityZone zone = daoMgr.getXXSecurityZoneDao().findByZoneId(zoneId);
		if (zone == null) {
			throw restErrorUtil.createRESTException("No corresponding zone found for policyName: " + vObj.getName()
					+ "Zone Not Found : " + zoneId, MessageEnums.INVALID_INPUT_DATA);
		}
		return zone.getName();
	}
}
