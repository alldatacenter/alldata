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

/**
 *
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.google.gson.Gson;

import com.google.gson.GsonBuilder;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.plugin.model.GroupInfo;
import org.apache.ranger.view.VXGroup;
import org.apache.ranger.view.VXGroupList;

public abstract class XGroupServiceBase<T extends XXGroup, V extends VXGroup>
		extends AbstractBaseResourceService<T, V> {
	public static final String NAME = "XGroup";
	private static final Gson gsonBuilder = new GsonBuilder().create();

	public XGroupServiceBase() {

	}

	@Override
	protected T mapViewToEntityBean(V vObj, T mObj, int OPERATION_CONTEXT) {
		mObj.setName( vObj.getName());
		mObj.setIsVisible(vObj.getIsVisible());
		mObj.setDescription( vObj.getDescription());
		mObj.setGroupType( vObj.getGroupType());
		mObj.setCredStoreId( vObj.getCredStoreId());
		mObj.setGroupSource(vObj.getGroupSource());
		mObj.setOtherAttributes(vObj.getOtherAttributes());
		mObj.setSyncSource(vObj.getSyncSource());
		return mObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T mObj) {
		vObj.setName( mObj.getName());
		vObj.setIsVisible( mObj.getIsVisible());
		vObj.setDescription( mObj.getDescription());
		vObj.setGroupType( mObj.getGroupType());
		vObj.setCredStoreId( mObj.getCredStoreId());
		vObj.setGroupSource(mObj.getGroupSource());
		vObj.setOtherAttributes(mObj.getOtherAttributes());
		vObj.setSyncSource(mObj.getSyncSource());
		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXGroupList searchXGroups(SearchCriteria searchCriteria) {
		VXGroupList returnList   = new VXGroupList();
		List<VXGroup> xGroupList = new ArrayList<VXGroup>();
		List<T> resultList       = searchResources(searchCriteria, searchFields, sortFields, returnList);

		// Iterate over the result list and create the return list
		for (T gjXGroup : resultList) {
			VXGroup vXGroup = populateViewBean(gjXGroup);
			xGroupList.add(vXGroup);
		}

		returnList.setVXGroups(xGroupList);
		return returnList;
	}

	public List<GroupInfo> getGroups() {
		List<GroupInfo> returnList = new ArrayList<>();

		@SuppressWarnings("unchecked")
		List<XXGroup> resultList = daoManager.getXXGroup().getAll();

		// Iterate over the result list and create the return list
		for (XXGroup gjXGroup : resultList) {
			GroupInfo groupInfo = new GroupInfo(gjXGroup.getName(), gjXGroup.getDescription(), gsonBuilder.fromJson(gjXGroup.getOtherAttributes(), Map.class));
			returnList.add(groupInfo);
		}

		return returnList;
	}

}
