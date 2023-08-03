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

import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchField;
import org.apache.ranger.entity.XXGroupPermission;
import org.apache.ranger.entity.XXModuleDef;
import org.apache.ranger.entity.XXUserPermission;
import org.apache.ranger.view.VXGroupPermission;
import org.apache.ranger.view.VXModuleDef;
import org.apache.ranger.view.VXModuleDefList;
import org.apache.ranger.view.VXModulePermission;
import org.apache.ranger.view.VXModulePermissionList;
import org.apache.ranger.view.VXUserPermission;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@Scope("singleton")
public class XModuleDefService extends
		XModuleDefServiceBase<XXModuleDef, VXModuleDef> {


	@Autowired
	XUserPermissionService xUserPermService;

	@Autowired
	XGroupPermissionService xGrpPermService;

        @Autowired
        XUserService xUserService;

        @Autowired
        XGroupService xGroupService;
	public XModuleDefService() {
		searchFields.add(new SearchField("module", "obj.module",
				SearchField.DATA_TYPE.STRING, SearchField.SEARCH_TYPE.PARTIAL));
		searchFields
				.add(new SearchField(
						"userName",
						"portalUser.loginId",
						SearchField.DATA_TYPE.STRING,
						SearchField.SEARCH_TYPE.PARTIAL,
						" XXPortalUser portalUser,  XXUserPermission userPermission",
						"obj.id=userPermission.moduleId and portalUser.id=userPermission.userId and userPermission.isAllowed="
								+ RangerConstants.IS_ALLOWED));
		searchFields
				.add(new SearchField(
						"groupName",
						"group.name",
						SearchField.DATA_TYPE.STRING,
						SearchField.SEARCH_TYPE.PARTIAL,
						"XXGroup group,XXGroupPermission groupModulePermission",
						"obj.id=groupModulePermission.moduleId and groupModulePermission.groupId=group.id and groupModulePermission.isAllowed="
								+ RangerConstants.IS_ALLOWED));
	}

	@Override
	protected void validateForCreate(VXModuleDef vObj) {

	}

	@Override
	protected void validateForUpdate(VXModuleDef vObj, XXModuleDef mObj) {

	}

	@Override
	public VXModuleDef populateViewBean(XXModuleDef xObj) {
		VXModuleDef vModuleDef = super.populateViewBean(xObj);
		Map<Long, Object[]> xXPortalUserIdXXUserMap = xUserService.getXXPortalUserIdXXUserNameMap();
		Map<Long, String> xXGroupMap = xGroupService.getXXGroupIdNameMap();
		List<VXUserPermission> vXUserPermissionList = new ArrayList<VXUserPermission>();
		List<VXGroupPermission> vXGroupPermissionList = new ArrayList<VXGroupPermission>();
		List<XXUserPermission> xuserPermissionList = daoManager
				.getXXUserPermission().findByModuleId(xObj.getId(), false);
		List<XXGroupPermission> xgroupPermissionList = daoManager
				.getXXGroupPermission().findByModuleId(xObj.getId(), false);
                if(CollectionUtils.isEmpty(xXPortalUserIdXXUserMap)){
                        for (XXUserPermission xUserPerm : xuserPermissionList) {
                                VXUserPermission vXUserPerm = xUserPermService.populateViewBean(xUserPerm);
                                vXUserPermissionList.add(vXUserPerm);
                        }
                }else{
                        vXUserPermissionList=xUserPermService.getPopulatedVXUserPermissionList(xuserPermissionList,xXPortalUserIdXXUserMap,vModuleDef);
		}
                if(CollectionUtils.isEmpty(xXGroupMap)){
                        for (XXGroupPermission xGrpPerm : xgroupPermissionList) {
                                VXGroupPermission vXGrpPerm = xGrpPermService.populateViewBean(xGrpPerm);
                                vXGroupPermissionList.add(vXGrpPerm);
                        }
                }else{
                        vXGroupPermissionList=xGrpPermService.getPopulatedVXGroupPermissionList(xgroupPermissionList,xXGroupMap,vModuleDef);
		}
		vModuleDef.setUserPermList(vXUserPermissionList);
		vModuleDef.setGroupPermList(vXGroupPermissionList);
		return vModuleDef;
	}

	@Override
	public VXModuleDefList searchModuleDef(SearchCriteria searchCriteria) {
		VXModuleDefList returnList = new VXModuleDefList();
		List<VXModuleDef> vXModuleDefList = new ArrayList<VXModuleDef>();
		searchCriteria.setMaxRows(Integer.MAX_VALUE);
		searchCriteria.setDistinct(true);
		List<XXModuleDef> resultList = searchResources(searchCriteria, searchFields, sortFields, returnList);
		// Filter out duplicate values retrieved from database in case of user & group permission lookup
		Map<Long, XXModuleDef> matchModule = new HashMap<Long, XXModuleDef>();
		for (XXModuleDef moduleDef : resultList) {
			matchModule.put(moduleDef.getId(), moduleDef);
		}
		List<XXModuleDef> moduleDefList = new ArrayList<XXModuleDef>(matchModule.values());

		Map<Long, Object[]> xXPortalUserIdXXUserMap = xUserService.getXXPortalUserIdXXUserNameMap();
		Map<Long, String> xXGroupMap = xGroupService.getXXGroupIdNameMap();

		// Iterate over the result list and create the return list
		for (XXModuleDef gjXModuleDef : moduleDefList) {
			VXModuleDef vXModuleDef = populateViewBean(gjXModuleDef, xXPortalUserIdXXUserMap, xXGroupMap, false);
			vXModuleDefList.add(vXModuleDef);
		}
		returnList.setTotalCount(vXModuleDefList.size());
		returnList.setvXModuleDef(vXModuleDefList);
		return returnList;
	}

	public VXModuleDef populateViewBean(XXModuleDef xObj, Map<Long, Object[]> xXPortalUserIdXXUserMap,
			Map<Long, String> xXGroupMap, boolean isUpdate) {
		VXModuleDef vModuleDef = super.populateViewBean(xObj);
		List<VXUserPermission> vXUserPermissionList = new ArrayList<VXUserPermission>();
		List<VXGroupPermission> vXGroupPermissionList = new ArrayList<VXGroupPermission>();
		List<XXUserPermission> xuserPermissionList = daoManager.getXXUserPermission().findByModuleId(xObj.getId(),isUpdate);
		List<XXGroupPermission> xgroupPermissionList = daoManager.getXXGroupPermission().findByModuleId(xObj.getId(),isUpdate);
		if (CollectionUtils.isEmpty(xXPortalUserIdXXUserMap)) {
			for (XXUserPermission xUserPerm : xuserPermissionList) {
				VXUserPermission vXUserPerm = xUserPermService.populateViewBean(xUserPerm);
				vXUserPermissionList.add(vXUserPerm);
			}
		} else {
			vXUserPermissionList = xUserPermService.getPopulatedVXUserPermissionList(xuserPermissionList,xXPortalUserIdXXUserMap, vModuleDef);
		}
		if (CollectionUtils.isEmpty(xXGroupMap)) {
			for (XXGroupPermission xGrpPerm : xgroupPermissionList) {
				VXGroupPermission vXGrpPerm = xGrpPermService.populateViewBean(xGrpPerm);
				vXGroupPermissionList.add(vXGrpPerm);
			}
		} else {
			vXGroupPermissionList = xGrpPermService.getPopulatedVXGroupPermissionList(xgroupPermissionList, xXGroupMap,vModuleDef);
		}
		vModuleDef.setUserPermList(vXUserPermissionList);
		vModuleDef.setGroupPermList(vXGroupPermissionList);
		return vModuleDef;
	}

	public VXModulePermissionList searchModuleDefList(SearchCriteria searchCriteria) {
		VXModulePermissionList returnList = new VXModulePermissionList();
		List<VXModulePermission> vXModulePermissionList = new ArrayList<VXModulePermission>();
		searchCriteria.setMaxRows(Integer.MAX_VALUE);
		searchCriteria.setDistinct(true);
		List<XXModuleDef> moduleDefList = searchResources(searchCriteria, searchFields, sortFields, returnList);

		// Iterate over the result list and create the return list
		for (XXModuleDef gjXModuleDef : moduleDefList) {
			VXModulePermission obj = new VXModulePermission();
			obj.setId(gjXModuleDef.getId());
			obj.setModule(gjXModuleDef.getModule());
			List<String> userNameList = daoManager.getXXUserPermission().findModuleUsersByModuleId(gjXModuleDef.getId());
			List<String> groupNameList = daoManager.getXXGroupPermission().findModuleGroupsByModuleId(gjXModuleDef.getId());
			obj.setUserNameList(userNameList);
			obj.setGroupNameList(groupNameList);
			vXModulePermissionList.add(obj);
		}
		returnList.setTotalCount(vXModulePermissionList.size());
		returnList.setvXModulePermissionList(vXModulePermissionList);
		return returnList;
	}
}
