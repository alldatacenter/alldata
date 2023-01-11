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
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.entity.XXGroupPermission;
import org.apache.ranger.view.VXGroupPermission;
import org.apache.ranger.view.VXGroupPermissionList;

public abstract class XGroupPermissionServiceBase<T extends XXGroupPermission, V extends VXGroupPermission>
		extends AbstractBaseResourceService<T, V> {

	public static final String NAME = "XGroupPermission";

	public XGroupPermissionServiceBase() {

	}

	@Override
	protected T mapViewToEntityBean(V vObj,
			T mObj, int OPERATION_CONTEXT) {
		mObj.setGroupId(vObj.getGroupId());
		mObj.setModuleId(vObj.getModuleId());
		mObj.setIsAllowed(vObj.getIsAllowed());
		return mObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T mObj) {
		vObj.setGroupId(mObj.getGroupId());
		vObj.setModuleId(mObj.getModuleId());
		vObj.setIsAllowed(mObj.getIsAllowed());
		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXGroupPermissionList searchXGroupPermission(SearchCriteria searchCriteria) {
		VXGroupPermissionList returnList = new VXGroupPermissionList();
		List<VXGroupPermission> vXGroupPermissions = new ArrayList<VXGroupPermission>();

		List<T> resultList = searchResources(
				searchCriteria, searchFields, sortFields, returnList);

		// Iterate over the result list and create the return list
		for (T gjXUser : resultList) {
			VXGroupPermission vXGroupPermission = populateViewBean(gjXUser);
			vXGroupPermissions.add(vXGroupPermission);
		}

		returnList.setvXGroupPermission(vXGroupPermissions);
		return returnList;
	}
}
