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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.ranger.common.GUIDUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SearchField.DATA_TYPE;
import org.apache.ranger.common.SearchField.SEARCH_TYPE;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.SortField.SORT_ORDER;
import org.apache.ranger.entity.XXService;
import org.apache.ranger.entity.XXServiceBase;
import org.apache.ranger.entity.XXServiceDef;
import org.apache.ranger.entity.XXServiceVersionInfo;
import org.apache.ranger.plugin.model.RangerService;
import org.apache.ranger.plugin.util.SearchFilter;
import org.apache.ranger.view.RangerServiceList;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class RangerServiceServiceBase<T extends XXServiceBase, V extends RangerService> extends RangerBaseModelService<T, V> {
	
	@Autowired
	GUIDUtil guidUtil;
	
	public RangerServiceServiceBase() {
		super();

		searchFields.add(new SearchField(SearchFilter.SERVICE_TYPE, "xSvcDef.name", DATA_TYPE.STRING,
				SEARCH_TYPE.FULL, "XXServiceDef xSvcDef", "obj.type = xSvcDef.id"));
		searchFields.add(new SearchField(SearchFilter.SERVICE_TYPE_ID, "obj.type", DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.SERVICE_NAME, "obj.name", DATA_TYPE.STRING, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.SERVICE_DISPLAY_NAME, "obj.displayName", DATA_TYPE.STRING, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.SERVICE_NAME_PARTIAL, "obj.name", DATA_TYPE.STRING, SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField(SearchFilter.SERVICE_DISPLAY_NAME_PARTIAL, "obj.displayName", DATA_TYPE.STRING, SEARCH_TYPE.PARTIAL));
		searchFields.add(new SearchField(SearchFilter.SERVICE_ID, "obj.id", DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.IS_ENABLED, "obj.isEnabled", DATA_TYPE.BOOLEAN, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.TAG_SERVICE_ID, "obj.tagService", DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.TAG_SERVICE_NAME, "xTagSvc.name", DATA_TYPE.STRING,
				SEARCH_TYPE.FULL, "XXServiceBase xTagSvc", "obj.tagService = xTagSvc.id"));

		sortFields.add(new SortField(SearchFilter.CREATE_TIME, "obj.createTime"));
		sortFields.add(new SortField(SearchFilter.UPDATE_TIME, "obj.updateTime"));
		sortFields.add(new SortField(SearchFilter.SERVICE_ID, "obj.id", true, SORT_ORDER.ASC));
		sortFields.add(new SortField(SearchFilter.SERVICE_NAME, "obj.name"));
		sortFields.add(new SortField(SearchFilter.SERVICE_DISPLAY_NAME, "obj.displayName"));
	}

	@Override
	protected T mapViewToEntityBean(V vObj, T xObj, int OPERATION_CONTEXT) {
		String guid = (StringUtils.isEmpty(vObj.getGuid())) ? guidUtil.genGUID() : vObj.getGuid();
		
		xObj.setGuid(guid);
		
		XXServiceDef xServiceDef = daoMgr.getXXServiceDef().findByName(vObj.getType());
		if(xServiceDef == null) {
			throw restErrorUtil.createRESTException(
					"No ServiceDefinition found with name :" + vObj.getType(),
					MessageEnums.INVALID_INPUT_DATA);
		}

		Long   tagServiceId   = null;
		String tagServiceName = vObj.getTagService();
		if(! StringUtils.isEmpty(tagServiceName)) {
			XXService xTagService = daoMgr.getXXService().findByName(tagServiceName);

			if(xTagService == null) {
				throw restErrorUtil.createRESTException(
						"No Service found with name :" + tagServiceName,
						MessageEnums.INVALID_INPUT_DATA);
			}

			tagServiceId = xTagService.getId();
		}

		xObj.setType(xServiceDef.getId());
		xObj.setName(vObj.getName());
		xObj.setDisplayName(vObj.getDisplayName());
		xObj.setTagService(tagServiceId);
		if (OPERATION_CONTEXT == OPERATION_CREATE_CONTEXT) {
			xObj.setTagVersion(vObj.getTagVersion());
		}
		xObj.setDescription(vObj.getDescription());
		xObj.setIsEnabled(vObj.getIsEnabled());
		return xObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T xObj) {
		XXServiceDef xServiceDef = daoMgr.getXXServiceDef().getById(xObj.getType());
		XXService    xTagService = xObj.getTagService() != null ? daoMgr.getXXService().getById(xObj.getTagService()) : null;
		vObj.setType(xServiceDef.getName());
		vObj.setGuid(xObj.getGuid());
		vObj.setVersion(xObj.getVersion());
		vObj.setName(xObj.getName());
		vObj.setDisplayName(xObj.getDisplayName());
		vObj.setDescription(xObj.getDescription());
		vObj.setTagService(xTagService != null ? xTagService.getName() : null);
		XXServiceVersionInfo versionInfoObj = daoMgr.getXXServiceVersionInfo().findByServiceId(xObj.getId());
		if (versionInfoObj != null) {
			vObj.setPolicyVersion(versionInfoObj.getPolicyVersion());
			vObj.setTagVersion(versionInfoObj.getTagVersion());
			vObj.setPolicyUpdateTime(versionInfoObj.getPolicyUpdateTime());
			vObj.setTagUpdateTime(versionInfoObj.getTagUpdateTime());
		} else {
			vObj.setPolicyVersion(xObj.getPolicyVersion());
			vObj.setTagVersion(xObj.getTagVersion());
			vObj.setPolicyUpdateTime(xObj.getPolicyUpdateTime());
			vObj.setTagUpdateTime(xObj.getTagUpdateTime());
		}
		vObj.setIsEnabled(xObj.getIsenabled());
		return vObj;
	}

	public RangerServiceList searchRangerServices(SearchFilter searchFilter) {
		RangerServiceList retList = new RangerServiceList();

		int startIndex = searchFilter.getStartIndex();
		int pageSize = searchFilter.getMaxRows();
		searchFilter.setStartIndex(0);
		searchFilter.setMaxRows(Integer.MAX_VALUE);

		List<T> xSvcList = searchResources(searchFilter, searchFields, sortFields, retList);
		List<T> permittedServices = new ArrayList<T>();

		for (T xSvc : xSvcList) {
			if(bizUtil.hasAccess(xSvc, null)){
				permittedServices.add(xSvc);
			}
		}

		if(!permittedServices.isEmpty()) {
			populatePageList(permittedServices, startIndex, pageSize, retList);
		}

		return retList;
	}

	private void populatePageList(List<T> xxObjList, int startIndex, int pageSize,
			RangerServiceList retList) {
		List<RangerService> onePageList = new ArrayList<RangerService>();

		for (int i = startIndex; i < pageSize + startIndex && i < xxObjList.size(); i++) {
			onePageList.add(populateViewBean(xxObjList.get(i)));
		}
		retList.setServices(onePageList);
		retList.setStartIndex(startIndex);
		retList.setPageSize(pageSize);
		retList.setResultSize(onePageList.size());
		retList.setTotalCount(xxObjList.size());
	}

}
