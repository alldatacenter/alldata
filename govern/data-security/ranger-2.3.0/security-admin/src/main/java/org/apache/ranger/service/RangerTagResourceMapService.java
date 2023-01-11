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

import org.apache.commons.collections.CollectionUtils;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.common.SearchField.DATA_TYPE;
import org.apache.ranger.common.SearchField.SEARCH_TYPE;
import org.apache.ranger.entity.XXTagResourceMap;
import org.apache.ranger.plugin.model.RangerTagResourceMap;
import org.apache.ranger.plugin.util.SearchFilter;
import org.springframework.stereotype.Service;

@Service
public class RangerTagResourceMapService extends RangerTagResourceMapServiceBase<XXTagResourceMap, RangerTagResourceMap> {

	public RangerTagResourceMapService() {
		searchFields.add(new SearchField(SearchFilter.TAG_DEF_ID, "obj.id", DATA_TYPE.INTEGER, SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.TAG_RESOURCE_ID, "obj.resourceId", DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
		searchFields.add(new SearchField(SearchFilter.TAG_ID, "obj.tagId", DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL));
	}
	
	@Override
	protected void validateForCreate(RangerTagResourceMap vObj) {

	}

	@Override
	protected void validateForUpdate(RangerTagResourceMap vObj, XXTagResourceMap entityObj) {

	}

	@Override
	public RangerTagResourceMap postCreate(XXTagResourceMap tagResMap) {
		RangerTagResourceMap ret = super.postCreate(tagResMap);

		daoMgr.getXXServiceVersionInfo().updateServiceVersionInfoForTagResourceMapCreate(tagResMap.getResourceId(), tagResMap.getTagId());

		return ret;
	}

	@Override
	protected XXTagResourceMap preDelete(Long id) {
		XXTagResourceMap tagResMap = super.preDelete(id);

		if (tagResMap != null) {
			daoMgr.getXXServiceVersionInfo().updateServiceVersionInfoForTagResourceMapDelete(tagResMap.getResourceId(), tagResMap.getTagId());
		}

		return tagResMap;
	}

	public RangerTagResourceMap getPopulatedViewObject(XXTagResourceMap xObj) {
		return populateViewBean(xObj);
	}


	public List<RangerTagResourceMap> getByTagId(Long tagId) {
		List<RangerTagResourceMap> ret = new ArrayList<RangerTagResourceMap>();

		List<XXTagResourceMap> xxTagResourceMaps = daoMgr.getXXTagResourceMap().findByTagId(tagId);
		
		if(CollectionUtils.isNotEmpty(xxTagResourceMaps)) {
			for(XXTagResourceMap xxTagResourceMap : xxTagResourceMaps) {
				RangerTagResourceMap tagResourceMap = populateViewBean(xxTagResourceMap);

				ret.add(tagResourceMap);
			}
		}

		return ret;
	}

	public List<RangerTagResourceMap> getByTagGuid(String tagGuid) {
		List<RangerTagResourceMap> ret = new ArrayList<RangerTagResourceMap>();

		List<XXTagResourceMap> xxTagResourceMaps = daoMgr.getXXTagResourceMap().findByTagGuid(tagGuid);
		
		if(CollectionUtils.isNotEmpty(xxTagResourceMaps)) {
			for(XXTagResourceMap xxTagResourceMap : xxTagResourceMaps) {
				RangerTagResourceMap tagResourceMap = populateViewBean(xxTagResourceMap);

				ret.add(tagResourceMap);
			}
		}

		return ret;
	}

	public List<RangerTagResourceMap> getByResourceId(Long resourceId) {
		List<RangerTagResourceMap> ret = new ArrayList<RangerTagResourceMap>();

		List<XXTagResourceMap> xxTagResourceMaps = daoMgr.getXXTagResourceMap().findByResourceId(resourceId);
		
		if(CollectionUtils.isNotEmpty(xxTagResourceMaps)) {
			for(XXTagResourceMap xxTagResourceMap : xxTagResourceMaps) {
				RangerTagResourceMap tagResourceMap = populateViewBean(xxTagResourceMap);

				ret.add(tagResourceMap);
			}
		}

		return ret;
	}

	public List<Long> getTagIdsForResourceId(Long resourceId) {
		List<Long> ret = daoMgr.getXXTagResourceMap().findTagIdsForResourceId(resourceId);

		return ret;
	}

	public List<RangerTagResourceMap> getByResourceGuid(String resourceGuid) {
		List<RangerTagResourceMap> ret = new ArrayList<RangerTagResourceMap>();

		List<XXTagResourceMap> xxTagResourceMaps = daoMgr.getXXTagResourceMap().findByResourceGuid(resourceGuid);
		
		if(CollectionUtils.isNotEmpty(xxTagResourceMaps)) {
			for(XXTagResourceMap xxTagResourceMap : xxTagResourceMaps) {
				RangerTagResourceMap tagResourceMap = populateViewBean(xxTagResourceMap);

				ret.add(tagResourceMap);
			}
		}

		return ret;
	}
	
	public RangerTagResourceMap getByGuid(String guid) {
		RangerTagResourceMap ret = null;

		XXTagResourceMap xxTagResourceMap = daoMgr.getXXTagResourceMap().findByGuid(guid);

		if(xxTagResourceMap != null) {
			ret = populateViewBean(xxTagResourceMap);
		}

		return ret;
	}
	
	public RangerTagResourceMap getByTagAndResourceId(Long tagId, Long resourceId) {
		RangerTagResourceMap ret = null;

		XXTagResourceMap xxTagResourceMap = daoMgr.getXXTagResourceMap().findByTagAndResourceId(tagId, resourceId);

		if(xxTagResourceMap != null) {
			ret = populateViewBean(xxTagResourceMap);
		}

		return ret;
	}

	public RangerTagResourceMap getByTagAndResourceGuid(String tagGuid, String resourceGuid) {
		RangerTagResourceMap ret = null;

		XXTagResourceMap xxTagResourceMap = daoMgr.getXXTagResourceMap().findByTagAndResourceGuid(tagGuid, resourceGuid);

		if(xxTagResourceMap != null) {
			ret = populateViewBean(xxTagResourceMap);
		}

		return ret;
	}

	public List<RangerTagResourceMap> getTagResourceMapsByServiceId(Long serviceId) {
		List<RangerTagResourceMap> ret = new ArrayList<RangerTagResourceMap>();

		List<XXTagResourceMap> xxTagResourceMaps = daoMgr.getXXTagResourceMap().findByServiceId(serviceId);
		
		if(CollectionUtils.isNotEmpty(xxTagResourceMaps)) {
			for(XXTagResourceMap xxTagResourceMap : xxTagResourceMaps) {
				RangerTagResourceMap tagResourceMap = populateViewBean(xxTagResourceMap);

				ret.add(tagResourceMap);
			}
		}

		return ret;
	}
}
