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
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.plugin.model.UserInfo;
import org.apache.ranger.view.VXUser;
import org.apache.ranger.view.VXUserList;

public abstract class XUserServiceBase<T extends XXUser, V extends VXUser>
		extends AbstractBaseResourceService<T, V> {
	public static final String NAME = "XUser";
	private static final Gson gsonBuilder = new GsonBuilder().create();

	public XUserServiceBase() {

	}

	@SuppressWarnings("unchecked")
	@Override
	protected XXUser mapViewToEntityBean(VXUser vObj, XXUser mObj, int OPERATION_CONTEXT) {
		mObj.setName( vObj.getName());
		mObj.setIsVisible(vObj.getIsVisible());
		mObj.setDescription( vObj.getDescription());
		mObj.setCredStoreId( vObj.getCredStoreId());
		mObj.setOtherAttributes(vObj.getOtherAttributes());
		mObj.setSyncSource(vObj.getSyncSource());
		return mObj;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected VXUser mapEntityToViewBean(VXUser vObj, XXUser mObj) {
		vObj.setName( mObj.getName());
		vObj.setIsVisible(mObj.getIsVisible());
		vObj.setDescription( mObj.getDescription());
		vObj.setCredStoreId( mObj.getCredStoreId());
		vObj.setOtherAttributes(mObj.getOtherAttributes());
		vObj.setSyncSource(mObj.getSyncSource());
		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXUserList searchXUsers(SearchCriteria searchCriteria) {
		VXUserList returnList   = new VXUserList();
		List<VXUser> xUserList  = new ArrayList<VXUser>();

		@SuppressWarnings("unchecked")
		List<XXUser> resultList = (List<XXUser>)searchResources(searchCriteria, searchFields, sortFields, returnList);

		// Iterate over the result list and create the return list
		for (XXUser gjXUser : resultList) {
			@SuppressWarnings("unchecked")
			VXUser vXUser = populateViewBean((T)gjXUser);
			xUserList.add(vXUser);
		}

		returnList.setVXUsers(xUserList);
		return returnList;
	}

	public List<UserInfo> getUsers() {
		List<UserInfo> returnList = new ArrayList<>();

		@SuppressWarnings("unchecked")
		List<XXUser> resultList = daoManager.getXXUser().getAll();

		// Iterate over the result list and create the return list
		for (XXUser gjXUser : resultList) {
			UserInfo userInfo = new UserInfo(gjXUser.getName(), gjXUser.getDescription(), gsonBuilder.fromJson(gjXUser.getOtherAttributes(), Map.class));
			returnList.add(userInfo);
		}

		return returnList;
	}

}
