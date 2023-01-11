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
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.entity.XXTagResourceMap;
import org.apache.ranger.plugin.model.RangerTagResourceMap;
import org.apache.ranger.plugin.store.PList;
import org.apache.ranger.plugin.util.SearchFilter;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class RangerTagResourceMapServiceBase<T extends XXTagResourceMap, V extends RangerTagResourceMap> extends RangerBaseModelService<T, V> {

	@Autowired
	GUIDUtil guidUtil;

	@Override
	protected T mapViewToEntityBean(V vObj, T xObj, int operationContext) {
		String guid = (StringUtils.isEmpty(vObj.getGuid())) ? guidUtil.genGUID() : vObj.getGuid();

		xObj.setGuid(guid);
		xObj.setTagId(vObj.getTagId());
		xObj.setResourceId(vObj.getResourceId());

		return xObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T xObj) {
		vObj.setGuid(xObj.getGuid());
		vObj.setTagId(xObj.getTagId());
		vObj.setResourceId(xObj.getResourceId());

		return vObj;
	}

	public PList<V> searchRangerTaggedResources(SearchFilter searchFilter) {
		PList<V> retList = new PList<V>();
		List<V> taggedResList = new ArrayList<V>();

		List<T> xTaggedResList = searchRangerObjects(searchFilter, searchFields, sortFields, retList);

		for (T xTaggedRes : xTaggedResList) {
			V taggedRes = populateViewBean(xTaggedRes);
			taggedResList.add(taggedRes);
		}
		retList.setList(taggedResList);
		return retList;
	}

}
