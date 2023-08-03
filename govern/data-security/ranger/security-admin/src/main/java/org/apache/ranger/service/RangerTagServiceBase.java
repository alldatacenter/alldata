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

package org.apache.ranger.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ranger.authorization.utils.JsonUtils;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RangerConfigUtil;
import org.apache.ranger.entity.XXTagAttribute;
import org.apache.ranger.entity.XXTag;
import org.apache.ranger.entity.XXTagDef;
import org.apache.ranger.plugin.model.RangerTag;
import org.apache.ranger.plugin.model.RangerValiditySchedule;
import org.apache.ranger.plugin.store.PList;
import org.apache.ranger.plugin.util.SearchFilter;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class RangerTagServiceBase<T extends XXTag, V extends RangerTag> extends
		RangerBaseModelService<T, V> {

	@Autowired
	GUIDUtil guidUtil;

	@Autowired
	RangerAuditFields rangerAuditFields;
	
	@Autowired
	RangerConfigUtil configUtil;

	@Override
	protected T mapViewToEntityBean(V vObj, T xObj, int OPERATION_CONTEXT) {
		String guid = (StringUtils.isEmpty(vObj.getGuid())) ? guidUtil.genGUID() : vObj.getGuid();

		XXTagDef xTagDef = daoMgr.getXXTagDef().findByName(vObj.getType());
		if(xTagDef == null) {
			throw restErrorUtil.createRESTException(
					"No TagDefinition found with name :" + vObj.getType(),
					MessageEnums.INVALID_INPUT_DATA);
		}

		xObj.setGuid(guid);
		xObj.setType(xTagDef.getId());
		xObj.setOwner(vObj.getOwner());

		String              validityPeriods = JsonUtils.listToJson(vObj.getValidityPeriods());
		Map<String, Object> options         = vObj.getOptions();

		if (options == null) {
			options = new HashMap<>();
		}

		if (StringUtils.isNotBlank(validityPeriods)) {
			options.put(RangerTag.OPTION_TAG_VALIDITY_PERIODS, validityPeriods);
		} else {
			options.remove(RangerTag.OPTION_TAG_VALIDITY_PERIODS);
		}

		xObj.setOptions(JsonUtils.mapToJson(options));
		return xObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T xObj) {
		XXTagDef xTagDef = daoMgr.getXXTagDef().getById(xObj.getType());
		if(xTagDef == null) {
			throw restErrorUtil.createRESTException(
					"No TagDefinition found with name :" + xObj.getType(),
					MessageEnums.INVALID_INPUT_DATA);
		}

		vObj.setGuid(xObj.getGuid());
		vObj.setType(xTagDef.getName());
		vObj.setOwner(xObj.getOwner());

		Map<String, Object> options = JsonUtils.jsonToObject(xObj.getOptions(), Map.class);

		if (MapUtils.isNotEmpty(options)) {
			String optionTagValidityPeriod = (String)options.remove(RangerTag.OPTION_TAG_VALIDITY_PERIODS);

			if (StringUtils.isNotBlank(optionTagValidityPeriod)) {
				List<RangerValiditySchedule> validityPeriods = JsonUtils.jsonToRangerValiditySchedule(optionTagValidityPeriod);

				vObj.setValidityPeriods(validityPeriods);
			}
		}

		vObj.setOptions(options);

		Map<String, String> attributes = getAttributesForTag(xObj);
		vObj.setAttributes(attributes);

		return vObj;
	}

	public Map<String, String> getAttributesForTag(XXTag xtag) {
		List<XXTagAttribute> tagAttrList = daoMgr.getXXTagAttribute().findByTagId(xtag.getId());
		Map<String, String>  ret         = new HashMap<String, String>();

		if(CollectionUtils.isNotEmpty(tagAttrList)) {
			for (XXTagAttribute tagAttr : tagAttrList) {
				ret.put(tagAttr.getName(), tagAttr.getValue());
			}
		}

		return ret;
	}

	public PList<V> searchRangerTags(SearchFilter searchFilter) {
		PList<V> retList = new PList<V>();
		List<V> tagList = new ArrayList<V>();

		List<T> xTagList = searchRangerObjects(searchFilter, searchFields, sortFields, retList);

		for (T xTag : xTagList) {
			V tag = populateViewBean(xTag);
			tagList.add(tag);
		}

		retList.setList(tagList);

		return retList;
	}
}
