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

import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXPortalUserList;

public abstract class UserServiceBase<T extends XXPortalUser, V extends VXPortalUser>
		extends AbstractBaseResourceService<T, V> {
	public static final String NAME = "User";

	public UserServiceBase() {

	}

	@Override
	protected T mapViewToEntityBean(V vObj, T mObj, int OPERATION_CONTEXT) {
		return mObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T mObj) {
		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXPortalUserList searchUsers(SearchCriteria searchCriteria) {
		VXPortalUserList returnList = new VXPortalUserList();
		List<VXPortalUser> userList = new ArrayList<VXPortalUser>();

		List<T> resultList = searchResources(searchCriteria,
				searchFields, sortFields, returnList);

		// Iterate over the result list and create the return list
		for (T gjUser : resultList) {
			VXPortalUser vUser = populateViewBean(gjUser);
			userList.add(vUser);
		}

		returnList.setVXPortalUsers(userList);
		return returnList;
	}

}
