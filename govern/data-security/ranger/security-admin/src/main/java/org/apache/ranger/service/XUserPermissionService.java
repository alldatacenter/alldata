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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ranger.common.SearchField;
import org.apache.ranger.entity.XXModuleDef;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXUserPermission;
import org.apache.ranger.view.VXModuleDef;
import org.apache.ranger.view.VXUserPermission;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
@Service
@Scope("singleton")
public class XUserPermissionService extends XUserPermissionServiceBase<XXUserPermission, VXUserPermission>{

	public XUserPermissionService() {
		searchFields.add(new SearchField("id", "obj.id",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));

		searchFields.add(new SearchField("userPermissionList", "obj.userId",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL,
				"XXModuleDef xXModuleDef", "xXModuleDef.id = obj.userId "));
	}

	@Override
	protected void validateForCreate(VXUserPermission vObj) {

	}

	@Override
	protected void validateForUpdate(VXUserPermission vObj, XXUserPermission mObj) {

	}

	@Override
	public VXUserPermission populateViewBean(XXUserPermission xObj) {
		VXUserPermission vObj = super.populateViewBean(xObj);

		XXPortalUser xPortalUser = daoManager.getXXPortalUser().getById(xObj.getUserId());
		if (xPortalUser != null) {
			vObj.setUserName(xPortalUser.getLoginId());
		}

		XXModuleDef xModuleDef = daoManager.getXXModuleDef().getById(xObj.getModuleId());
		if (xModuleDef != null) {
			vObj.setModuleName(xModuleDef.getModule());
		}

		return vObj;
	}

	public List<VXUserPermission> getPopulatedVXUserPermissionList(List<XXUserPermission> xuserPermissionList,
			Map<Long, Object[]> xXPortalUserIdXXUserMap, VXModuleDef vModuleDef) {
		List<VXUserPermission> vXUserPermissionList = new ArrayList<VXUserPermission>();
		Object[] xXUser = null;
		for (XXUserPermission xuserPermission : xuserPermissionList) {
			if (xXPortalUserIdXXUserMap.containsKey(xuserPermission.getUserId())) {
				xXUser = xXPortalUserIdXXUserMap.get(xuserPermission.getUserId());
				VXUserPermission vXUserPerm = new VXUserPermission();
				vXUserPerm.setId(xuserPermission.getId());
				vXUserPerm.setUserId((Long) xXUser[1]);
				vXUserPerm.setModuleId(xuserPermission.getModuleId());
				vXUserPerm.setIsAllowed(xuserPermission.getIsAllowed());
				vXUserPerm.setCreateDate(xuserPermission.getCreateTime());
				vXUserPerm.setUpdateDate(xuserPermission.getUpdateTime());
				vXUserPerm.setModuleName(vModuleDef.getModule());
				vXUserPerm.setLoginId((String) xXUser[2]);
				vXUserPerm.setUserName((String) xXUser[2]);
				vXUserPermissionList.add(vXUserPerm);
			}
		}
		return vXUserPermissionList;
	}

	@Override
	public Map<Long, VXUserPermission> convertVListToVMap(List<VXUserPermission> vObjList) {
		Map<Long,VXUserPermission> ret = new HashMap<Long,VXUserPermission>();
		if (vObjList == null) {
			return ret;
		}
		for (VXUserPermission vObj : vObjList) {
			ret.put(vObj.getUserId(), vObj);
		}
		return ret;
	}
}