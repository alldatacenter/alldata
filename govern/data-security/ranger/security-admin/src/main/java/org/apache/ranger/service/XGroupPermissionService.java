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

import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXGroupPermission;
import org.apache.ranger.view.VXGroupPermission;
import org.apache.ranger.view.VXModuleDef;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("singleton")
public class XGroupPermissionService extends XGroupPermissionServiceBase<XXGroupPermission, VXGroupPermission>{

	public XGroupPermissionService() {
		searchFields.add(new SearchField("id", "obj.id",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.FULL));

		searchFields.add(new SearchField("groupPermissionList", "obj.groupId",
				SearchField.DATA_TYPE.INTEGER, SearchField.SEARCH_TYPE.FULL,
				"XXModuleDef xXModuleDef", "xXModuleDef.id = obj.groupId "));
	}

	@Override
	protected void validateForCreate(VXGroupPermission vObj) {
		XXGroupPermission xGroupPerm = daoManager.getXXGroupPermission().findByModuleIdAndGroupId(vObj.getGroupId(), vObj.getModuleId());
		if (xGroupPerm != null) {
			throw restErrorUtil.createRESTException("Group with ID [" + vObj.getGroupId() + "] " + "is already " + "assigned to the module with ID [" + vObj.getModuleId() + "]",
					MessageEnums.ERROR_DUPLICATE_OBJECT);
		}
	}

	@Override
	protected void validateForUpdate(VXGroupPermission vObj, XXGroupPermission mObj) {
		XXGroupPermission xGroupPerm = daoManager.getXXGroupPermission().findByModuleIdAndGroupId(vObj.getGroupId(), vObj.getModuleId());
		if (xGroupPerm != null && !xGroupPerm.getId().equals(vObj.getId())) {
			throw restErrorUtil.createRESTException("Group with ID [" + vObj.getGroupId() + "] " + "is already " + "assigned to the module with ID [" + vObj.getModuleId() + "]",
					MessageEnums.ERROR_DUPLICATE_OBJECT);
		}
	}

	@Override
	public VXGroupPermission populateViewBean(XXGroupPermission xObj) {
		VXGroupPermission vObj = super.populateViewBean(xObj);
		XXGroup xGroup = daoManager.getXXGroup().getById(
				xObj.getGroupId());

		if (xGroup == null) {
			throw restErrorUtil.createRESTException(xGroup + " is Not Found",
					MessageEnums.DATA_NOT_FOUND);
		}

		vObj.setGroupName(xGroup.getName());
		return vObj;
	}

	public List<VXGroupPermission> getPopulatedVXGroupPermissionList(List<XXGroupPermission> xgroupPermissionList,
			Map<Long, String> xXGroupMap, VXModuleDef vModuleDef) {
		List<VXGroupPermission> vXGroupPermissionList = new ArrayList<VXGroupPermission>();
		for (XXGroupPermission xgroupPermission : xgroupPermissionList) {
			if (xXGroupMap.containsKey(xgroupPermission.getGroupId())) {
				VXGroupPermission vXGrpPerm = new VXGroupPermission();
				vXGrpPerm.setId(xgroupPermission.getId());
				vXGrpPerm.setGroupId(xgroupPermission.getGroupId());
				vXGrpPerm.setModuleId(xgroupPermission.getModuleId());
				vXGrpPerm.setIsAllowed(xgroupPermission.getIsAllowed());
				vXGrpPerm.setCreateDate(xgroupPermission.getCreateTime());
				vXGrpPerm.setUpdateDate(xgroupPermission.getUpdateTime());
				vXGrpPerm.setGroupName(xXGroupMap.get(xgroupPermission.getGroupId()));
				vXGrpPerm.setModuleName(vModuleDef.getModule());
				vXGroupPermissionList.add(vXGrpPerm);
			}
		}
		return vXGroupPermissionList;
	}

	@Override
	public Map<Long, VXGroupPermission> convertVListToVMap(List<VXGroupPermission> vObjList) {
		Map<Long, VXGroupPermission> ret = new HashMap<Long, VXGroupPermission>();
		if (vObjList == null) {
			return ret;
		}
		for (VXGroupPermission vObj : vObjList) {
			ret.put(vObj.getGroupId(), vObj);
		}
		return ret;
	}
}
