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

import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.entity.XXUserPermission;
import org.apache.ranger.view.VXUserPermission;
import org.apache.ranger.view.VXUserPermissionList;

public abstract class XUserPermissionServiceBase<T extends XXUserPermission, V extends VXUserPermission>
		extends AbstractBaseResourceService<T, V> {

	public static final String NAME = "XUserPermission";

	@Override
	protected T mapViewToEntityBean(V vObj, T mObj, int OPERATION_CONTEXT) {

		// Assuming that vObj.userId coming from UI/Client would be of XXUser, but in DB it should be of XXPortalUser so
		// have to map XXUser.ID to XXPortalUser.ID and if portalUser does not exist then not allowing to create/update

		XXPortalUser portalUser = daoManager.getXXPortalUser().findByXUserId(vObj.getUserId());
		if (portalUser == null) {
			throw restErrorUtil.createRESTException("Invalid UserId: [" + vObj.getUserId()
					+ "], Please make sure while create/update given userId should be of x_user",
					MessageEnums.INVALID_INPUT_DATA);
		}

		mObj.setUserId(portalUser.getId());
		mObj.setModuleId(vObj.getModuleId());
		mObj.setIsAllowed(vObj.getIsAllowed());

		if (OPERATION_CONTEXT == OPERATION_CREATE_CONTEXT) {
			validateXUserPermForCreate(mObj);
		} else if (OPERATION_CONTEXT == OPERATION_UPDATE_CONTEXT) {
			validateXUserPermForUpdate(mObj);
		}

		return mObj;
	}

	@Override
	protected V mapEntityToViewBean(V vObj, T mObj) {

		// As XXUserPermission.userID refers to XXPortalUser.ID, But UI/Client expects XXUser.ID so have to map
		// XXUserPermission.userID from XXPortalUser.ID to XXUser.ID
		XXUser xUser = daoManager.getXXUser().findByPortalUserId(mObj.getUserId());
		Long userId;
		if (xUser != null) {
			userId = xUser.getId();
		} else {
			// In this case rather throwing exception, send it as null
			userId = null;
		}
		vObj.setUserId(userId);
		vObj.setModuleId(mObj.getModuleId());
		vObj.setIsAllowed(mObj.getIsAllowed());
		return vObj;
	}

	/**
	 * @param searchCriteria
	 * @return
	 */
	public VXUserPermissionList searchXUserPermission(SearchCriteria searchCriteria) {
		VXUserPermissionList returnList = new VXUserPermissionList();
		List<VXUserPermission> vXUserPermissions = new ArrayList<VXUserPermission>();

		List<T> resultList = searchResources(
				searchCriteria, searchFields, sortFields, returnList);

		// Iterate over the result list and create the return list
		for (T gjXUser : resultList) {
			VXUserPermission vXUserPermission = populateViewBean(gjXUser);
			vXUserPermissions.add(vXUserPermission);
		}

		returnList.setvXModuleDef(vXUserPermissions);
		return returnList;
	}

	protected void validateXUserPermForCreate(XXUserPermission mObj) {
		XXUserPermission xUserPerm = daoManager.getXXUserPermission().findByModuleIdAndPortalUserId(mObj.getUserId(),
				mObj.getModuleId());
		if (xUserPerm != null) {
			throw restErrorUtil.createRESTException("User with ID [" + mObj.getUserId() + "] " + "is already "
					+ "assigned to the module with ID [" + mObj.getModuleId() + "]",
					MessageEnums.ERROR_DUPLICATE_OBJECT);
		}
	}

	protected void validateXUserPermForUpdate(XXUserPermission mObj) {

		XXUserPermission xUserPerm = daoManager.getXXUserPermission().findByModuleIdAndPortalUserId(mObj.getUserId(),
				mObj.getModuleId());
		if (xUserPerm != null && !xUserPerm.getId().equals(mObj.getId())) {
			throw restErrorUtil.createRESTException("User with ID [" + mObj.getUserId() + "] " + "is already "
					+ "assigned to the module with ID [" + mObj.getModuleId() + "]",
					MessageEnums.ERROR_DUPLICATE_OBJECT);
		}
	}

}