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
package org.apache.ranger.rest;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.apache.ranger.biz.RangerBizUtil;
import org.apache.ranger.biz.SessionMgr;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchUtil;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXGroupDao;
import org.apache.ranger.entity.XXResource;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerDataMaskPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerRowFilterPolicyItem;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.apache.ranger.service.AuthSessionService;
import org.apache.ranger.service.XGroupGroupService;
import org.apache.ranger.service.XGroupPermissionService;
import org.apache.ranger.service.XGroupService;
import org.apache.ranger.service.XGroupUserService;
import org.apache.ranger.service.XModuleDefService;
import org.apache.ranger.service.XResourceService;
import org.apache.ranger.service.XUserPermissionService;
import org.apache.ranger.service.XUserService;
import org.apache.ranger.view.VXAuditMap;
import org.apache.ranger.view.VXAuditMapList;
import org.apache.ranger.view.VXAuthSession;
import org.apache.ranger.view.VXAuthSessionList;
import org.apache.ranger.view.VXGroup;
import org.apache.ranger.view.VXGroupGroup;
import org.apache.ranger.view.VXGroupGroupList;
import org.apache.ranger.view.VXGroupList;
import org.apache.ranger.view.VXGroupPermission;
import org.apache.ranger.view.VXGroupPermissionList;
import org.apache.ranger.view.VXGroupUser;
import org.apache.ranger.view.VXGroupUserList;
import org.apache.ranger.view.VXLong;
import org.apache.ranger.view.VXModuleDef;
import org.apache.ranger.view.VXModuleDefList;
import org.apache.ranger.view.VXPermMap;
import org.apache.ranger.view.VXPermMapList;
import org.apache.ranger.view.VXResponse;
import org.apache.ranger.view.VXString;
import org.apache.ranger.view.VXStringList;
import org.apache.ranger.view.VXUser;
import org.apache.ranger.view.VXUserGroupInfo;
import org.apache.ranger.view.VXUserList;
import org.apache.ranger.view.VXDataObject;
import org.apache.ranger.view.VXResource;
import org.apache.ranger.view.VXUserPermission;
import org.apache.ranger.view.VXUserPermissionList;
import org.apache.ranger.entity.XXAuditMap;
import org.apache.ranger.service.XAuditMapService;
import org.apache.ranger.entity.XXAsset;
import org.apache.ranger.entity.XXGroupGroup;
import org.apache.ranger.entity.XXGroupPermission;
import org.apache.ranger.entity.XXPermMap;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.service.XPermMapService;
import org.junit.After;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.db.XXGroupPermissionDao;
import org.apache.ranger.db.XXResourceDao;
import org.apache.ranger.db.XXPermMapDao;
import org.apache.ranger.db.XXPolicyDao;
import org.apache.ranger.db.XXGroupUserDao;
import org.apache.ranger.db.XXUserDao;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.db.XXAuditMapDao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestXUserREST {
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	
	@InjectMocks
	XUserREST xUserRest = new XUserREST();
	
	VXUser vxUser=createVXUser();
	Long id=1L;
	
	@Mock XUserMgr xUserMgr;
	@Mock VXGroup vxGroup;
	@Mock SearchCriteria searchCriteria;
	@Mock XGroupService xGroupService;
	@Mock SearchUtil searchUtil;
	@Mock StringUtil stringUtil;
	@Mock VXLong vXLong;
	@Mock HttpServletRequest request;
	@Mock VXUser vXUser1;
	@Mock VXUserGroupInfo vXUserGroupInfo;
	@Mock RangerBizUtil bizUtil;
	@Mock XUserService xUserService;
	@Mock VXUserList vXUserList;
	@Mock VXGroupUser vXGroupUser;
	@Mock XGroupUserService xGroupUserService;
	@Mock VXGroupUserList vXGroupUserList;
	@Mock VXGroupGroup vXGroupGroup;
	@Mock VXGroupGroupList vXGroupGroupList;
	@Mock XGroupGroupService xGroupGroupService;
	@Mock VXPermMap vXPermMap;
	@Mock RESTErrorUtil restErrorUtil;
	@Mock WebApplicationException webApplicationException;
	@Mock XResourceService xResourceService;
	@Mock VXDataObject VXDataObject;
	@Mock AppConstants AppConstants;
	@Mock RangerConstants RangerConstants;
	@Mock VXResource vXResource;
	@Mock VXResponse vXResponse;
	@Mock XXResource xXResource;
	@Mock XXAuditMap XXAuditMap;
	@Mock XAuditMapService xAuditMapService;
	@Mock XPermMapService xPermMapService;
	@Mock XXAsset XXAsset;
	@Mock RangerDaoManager rangerDaoManager;
	@Mock XXPermMap XXPermMap;
	@Mock Response response;
	@Mock VXPermMapList vXPermMapList;
	@Mock VXAuditMap vXAuditMap;
	@Mock VXAuditMapList vXAuditMapList;
	@Mock AuthSessionService authSessionService;
	@Mock SessionMgr sessionMgr;
	@Mock VXAuthSessionList vXAuthSessionList;
	@Mock VXModuleDef vXModuleDef;
	@Mock VXUserPermission vXUserPermission;
	@Mock VXUserPermissionList vXUserPermissionList;
	@Mock VXGroupPermission vXGroupPermission;
	@Mock XModuleDefService xModuleDefService;
	@Mock VXModuleDefList VXModuleDefList;
	@Mock XUserPermissionService xUserPermissionService;
	@Mock VXGroupPermissionList vXGroupPermissionList;
	@Mock XGroupPermissionService xGroupPermissionService;
	@Mock VXStringList vXStringList;
	@Mock VXString vXString;
	@Mock XXGroupDao xXGroupDao;
	@Mock XXGroup xXGroup;
	@Mock XXGroupGroup xXGroupGroup;
	@Mock XXGroupPermission xXGroupPermission;
	@Mock XXGroupPermissionDao xXGroupPermissionDao;
	@Mock XXPolicyDao xXPolicyDao;
	@Mock XXPolicy xXPolicy;
	@Mock XXGroupUserDao xXGroupUserDao;
	@Mock XXUserDao xXUserDao;
	@Mock XXUser xXUser;
	@Mock XXPermMapDao xXPermMapDao;
	@Mock XXResourceDao xXResourceDao;
	@Mock XXAuditMapDao xXAuditMapDao;
	@Mock RangerPolicy rangerPolicy;
	@Mock RangerPolicyItem rangerPolicyItem;
	@Mock RangerDataMaskPolicyItem rangerDataMaskPolicyItem;
	@Mock RangerRowFilterPolicyItem rangerRowFilterPolicyItem;
	
	@Test
	public void test1getXGroup() {
		VXGroup compareTestVXGroup=createVXGroup();
		
		Mockito.when(xUserMgr.getXGroup(id)).thenReturn(compareTestVXGroup);
		VXGroup retVxGroup= xUserRest.getXGroup(id);
		
		assertNotNull(retVxGroup);
		assertEquals(compareTestVXGroup.getId(),retVxGroup.getId());
		assertEquals(compareTestVXGroup.getName(),retVxGroup.getName());
		Mockito.verify(xUserMgr).getXGroup(id);
	}
	
	@Test
	public void test2secureGetXGroup() {
		VXGroup compareTestVXGroup=createVXGroup();
		
		Mockito.when(xUserMgr.getXGroup(id)).thenReturn(compareTestVXGroup);
		VXGroup retVxGroup=xUserRest.secureGetXGroup(id);
		
		assertNotNull(retVxGroup);
		assertEquals(compareTestVXGroup.getId(),retVxGroup.getId());
		assertEquals(compareTestVXGroup.getName(),retVxGroup.getName());
		Mockito.verify(xUserMgr).getXGroup(id);
	}
	
	@Test
	public void test3createXGroup() {
		VXGroup compareTestVXGroup=createVXGroup();
		
		Mockito.when(xUserMgr.createXGroupWithoutLogin(compareTestVXGroup)).thenReturn(compareTestVXGroup);
		VXGroup retVxGroup=xUserRest.createXGroup(compareTestVXGroup);
		
		assertNotNull(retVxGroup);
		assertEquals(compareTestVXGroup.getId(),retVxGroup.getId());
		assertEquals(compareTestVXGroup.getName(),retVxGroup.getName());
		Mockito.verify(xUserMgr).createXGroupWithoutLogin(compareTestVXGroup);
	}
	@Test
	public void test4secureCreateXGroup() {
		VXGroup compareTestVXGroup=createVXGroup();
		
		Mockito.when(xUserMgr.createXGroup(compareTestVXGroup)).thenReturn(compareTestVXGroup);
		VXGroup retVxGroup=xUserRest.secureCreateXGroup(compareTestVXGroup);
		
		assertNotNull(retVxGroup);
		assertEquals(compareTestVXGroup.getId(),retVxGroup.getId());
		assertEquals(compareTestVXGroup.getName(),retVxGroup.getName());
		Mockito.verify(xUserMgr).createXGroup(compareTestVXGroup);
	}
	@Test
	public void test5updateXGroup() {
		VXGroup compareTestVXGroup=createVXGroup();
		
		Mockito.when(xUserMgr.updateXGroup(compareTestVXGroup)).thenReturn(compareTestVXGroup);
		VXGroup retVxGroup=xUserRest.updateXGroup(compareTestVXGroup);
		
		assertNotNull(retVxGroup);
		assertEquals(compareTestVXGroup.getId(),retVxGroup.getId());
		assertEquals(compareTestVXGroup.getName(),retVxGroup.getName());
		Mockito.verify(xUserMgr).updateXGroup(compareTestVXGroup);
	}
	@Test
	public void test6secureUpdateXGroup() {
		VXGroup compareTestVXGroup=createVXGroup();
		
		Mockito.when(xUserMgr.updateXGroup(compareTestVXGroup)).thenReturn(compareTestVXGroup);
		VXGroup retVxGroup=xUserRest.secureUpdateXGroup(compareTestVXGroup);
		
		assertNotNull(retVxGroup);
		assertEquals(compareTestVXGroup.getId(),retVxGroup.getId());
		assertEquals(compareTestVXGroup.getName(),retVxGroup.getName());
		Mockito.verify(xUserMgr).updateXGroup(compareTestVXGroup);
	}
	@Test
	public void test7modifyGroupsVisibility() {
		HashMap<Long, Integer> groupVisibilityMap=creategroupVisibilityMap();
		xUserRest.modifyGroupsVisibility(groupVisibilityMap);
		
		Mockito.verify(xUserMgr).modifyGroupsVisibility(groupVisibilityMap);
	}
	@Test
	public void test8deleteXGroupTrue() {
			HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
			String TestforceDeleteStr="true";
			boolean forceDelete = false;
			Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
			
			forceDelete=true;
			Mockito.doNothing().when(xUserMgr).deleteXGroup(id, forceDelete);
			xUserRest.deleteXGroup(id,request);
			Mockito.verify(xUserMgr).deleteXGroup(id,forceDelete);
			Mockito.verify(request).getParameter("forceDelete");
	}
	@Test
	public void test9deleteXGroupFalse() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
		boolean forceDelete ;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
	
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXGroup(id, forceDelete);
		xUserRest.deleteXGroup(id,request);
		Mockito.verify(xUserMgr).deleteXGroup(id,forceDelete);
		Mockito.verify(request).getParameter("forceDelete");
	}
	@Test
	public void test10deleteXGroupNotEmpty() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr=null;
		boolean forceDelete ;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXGroup(id, forceDelete);
		xUserRest.deleteXGroup(id,request);
		Mockito.verify(xUserMgr).deleteXGroup(id,forceDelete);
		Mockito.verify(request).getParameter("forceDelete");
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test11searchXGroups() {

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
	
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "name", "group name", null)).thenReturn("");
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "isVisible", "Group Visibility")).thenReturn(1);
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "groupSource", "group source")).thenReturn(1);
		VXGroupList testvXGroupList=createxGroupList();
		Mockito.when(xUserMgr.searchXGroups(testSearchCriteria)).thenReturn(testvXGroupList);
		VXGroupList outputvXGroupList=xUserRest.searchXGroups(request);
		
		Mockito.verify(xUserMgr).searchXGroups(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "name", "group name", null);
		Mockito.verify(searchUtil).extractInt(request, testSearchCriteria, "isVisible", "Group Visibility");
		Mockito.verify(searchUtil).extractInt(request, testSearchCriteria, "groupSource", "group source");
		assertNotNull(outputvXGroupList);
		assertEquals(outputvXGroupList.getTotalCount(),testvXGroupList.getTotalCount());
		assertEquals(outputvXGroupList.getClass(),testvXGroupList.getClass());
		
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test12countXGroups() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
	
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
	
		vXLong.setValue(1);
		
		Mockito.when(xUserMgr.getXGroupSearchCount(testSearchCriteria)).thenReturn(vXLong);
		VXLong testvxLong=xUserRest.countXGroups(request);
		Mockito.verify(xUserMgr).getXGroupSearchCount(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		
		assertNotNull(testvxLong);
		assertEquals(testvxLong.getValue(),vXLong.getValue());
		assertEquals(testvxLong.getClass(),vXLong.getClass());
		
	}
	@Test
	public void test13getXUser() {
	
		Mockito.when(xUserMgr.getXUser(id)).thenReturn(vxUser);
		VXUser gotVXUser=xUserRest.getXUser(id);
		Mockito.verify(xUserMgr).getXUser(id);
		
		assertNotNull(gotVXUser);
		assertEquals(vxUser.getId(), gotVXUser.getId());
		assertEquals(vxUser.getName(), gotVXUser.getName());
	}
	@Test
	public void test14secureGetXUser() {
		
		Mockito.when(xUserMgr.getXUser(id)).thenReturn(vxUser);
		VXUser gotVXUser=xUserRest.secureGetXUser(id);
		Mockito.verify(xUserMgr).getXUser(id);
		
		assertNotNull(gotVXUser);
		assertEquals(vxUser.getId(), gotVXUser.getId());
		assertEquals(vxUser.getName(), gotVXUser.getName());
	}
	@Test
	public void test15createXUser() {
			
		Mockito.when(xUserMgr.createXUserWithOutLogin(vxUser)).thenReturn(vxUser);
		VXUser gotVXUser=xUserRest.createXUser(vxUser);
		Mockito.verify(xUserMgr).createXUserWithOutLogin(vxUser);
		
		assertNotNull(gotVXUser);
		assertEquals(vxUser.getId(), gotVXUser.getId());
		assertEquals(vxUser.getName(), gotVXUser.getName());
	}
	@Test
	public void test16createXUserGroupFromMap() {
		VXUserGroupInfo vXUserGroupInfo= new VXUserGroupInfo();
		vXUserGroupInfo.setXuserInfo(vxUser);
		
		Mockito.when(xUserMgr.createXUserGroupFromMap(vXUserGroupInfo)).thenReturn(vXUserGroupInfo);
		VXUserGroupInfo gotVXUserGroupInfo=xUserRest.createXUserGroupFromMap(vXUserGroupInfo);
		Mockito.verify(xUserMgr).createXUserGroupFromMap(vXUserGroupInfo);
		
		assertNotNull(gotVXUserGroupInfo);
		assertEquals(vXUserGroupInfo.getId(), gotVXUserGroupInfo.getId());
		assertEquals(vXUserGroupInfo.getOwner(), gotVXUserGroupInfo.getOwner());
	}
	@Test
	public void test17secureCreateXUser() {
		Boolean val= true;
		Mockito.when(bizUtil.checkUserAccessible(vxUser)).thenReturn(val);
		Mockito.when(xUserMgr.createXUser(vxUser)).thenReturn(vxUser);
		VXUser gotVXUser=xUserRest.secureCreateXUser(vxUser);
		Mockito.verify(xUserMgr).createXUser(vxUser);
		Mockito.verify(bizUtil).checkUserAccessible(vxUser);
		assertNotNull(gotVXUser);
		assertEquals(vxUser.getId(), gotVXUser.getId());
		assertEquals(vxUser.getName(), gotVXUser.getName());
	
		}
	@Test
	public void test18updateXUser() {
		Mockito.when(xUserMgr.updateXUser(vxUser)).thenReturn(vxUser);
		VXUser gotVXUser=xUserRest.updateXUser(vxUser);
		Mockito.verify(xUserMgr).updateXUser(vxUser);
		assertNotNull(gotVXUser);
		assertEquals(vxUser.getId(), gotVXUser.getId());
		assertEquals(vxUser.getName(), gotVXUser.getName());
	}
	@Test
	public void test19secureUpdateXUser() {
		
		Boolean val= true;
		Mockito.when(bizUtil.checkUserAccessible(vxUser)).thenReturn(val);
		Mockito.when(xUserMgr.updateXUser(vxUser)).thenReturn(vxUser);
		VXUser gotVXUser=xUserRest.secureUpdateXUser(vxUser);
		Mockito.verify(xUserMgr).updateXUser(vxUser);
		Mockito.verify(bizUtil).checkUserAccessible(vxUser);
		
		assertNotNull(gotVXUser);
		assertEquals(vxUser.getId(), gotVXUser.getId());
		assertEquals(vxUser.getName(), gotVXUser.getName());
	}
	@Test
	public void test20modifyUserVisibility() {
		HashMap<Long, Integer> testVisibilityMap= new HashMap<Long, Integer>();
		testVisibilityMap.put(1L,0);
		Mockito.doNothing().when(xUserMgr).modifyUserVisibility(testVisibilityMap);
		xUserRest.modifyUserVisibility(testVisibilityMap);
		Mockito.verify(xUserMgr).modifyUserVisibility(testVisibilityMap);
	}
	@Test
	public void test21deleteXUser() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		boolean forceDelete = false;
		String TestforceDeleteStr="true";
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		forceDelete = true;
		Mockito.doNothing().when(xUserMgr).deleteXUser(id, forceDelete);
		xUserRest.deleteXUser(id, request);
		Mockito.verify(xUserMgr).deleteXUser(id,forceDelete);
		Mockito.verify(request).getParameter("forceDelete");
	}
	@Test
	public void test22deleteXUserFalse() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
		boolean forceDelete ;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
	
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXUser(id, forceDelete);
		xUserRest.deleteXUser(id,request);
		Mockito.verify(xUserMgr).deleteXUser(id,forceDelete);
		Mockito.verify(request).getParameter("forceDelete");
	}
	@Test
	public void test23deleteXUserNotEmpty() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr=null;
		boolean forceDelete ;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXUser(id, forceDelete);
		xUserRest.deleteXUser(id,request);
		Mockito.verify(xUserMgr).deleteXUser(id,forceDelete);
		Mockito.verify(request).getParameter("forceDelete");
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test24searchXUsers() {
	
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
	
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any(), (List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
		
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "name", "User name", null)).thenReturn("");
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "emailAddress", "Email Address",null)).thenReturn("");
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "userSource", "User Source")).thenReturn(1);
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "isVisible", "User Visibility")).thenReturn(1);
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "status", "User Status")).thenReturn(1);
		Mockito.when(searchUtil.extractStringList(request, testSearchCriteria, "userRoleList", "User Role List", "userRoleList", null,null)).thenReturn(new ArrayList<String>());
		Mockito.when(searchUtil.extractRoleString(request, testSearchCriteria, "userRole", "Role", null)).thenReturn("");
		
		List<VXUser> vXUsersList= new ArrayList<VXUser>();
		vXUsersList.add(vxUser);
		VXUserList testVXUserList= new VXUserList();
		testVXUserList.setVXUsers(vXUsersList);
		
		Mockito.when(xUserMgr.searchXUsers(testSearchCriteria)).thenReturn(testVXUserList);
		VXUserList gotVXUserList=xUserRest.searchXUsers(request);
		
		Mockito.verify(xUserMgr).searchXUsers(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());

		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "name", "User name", null);
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "emailAddress", "Email Address",null);
		Mockito.verify(searchUtil).extractInt(request, testSearchCriteria, "userSource", "User Source");
		Mockito.verify(searchUtil).extractInt(request, testSearchCriteria, "isVisible", "User Visibility");
		Mockito.verify(searchUtil).extractInt(request, testSearchCriteria, "status", "User Status");
		Mockito.verify(searchUtil).extractStringList(request, testSearchCriteria, "userRoleList", "User Role List", "userRoleList", null,null);
		Mockito.verify(searchUtil).extractRoleString(request, testSearchCriteria, "userRole", "Role", null);
		assertNotNull(gotVXUserList);
		assertEquals(testVXUserList.getTotalCount(),gotVXUserList.getTotalCount());
		assertEquals(testVXUserList.getClass(),gotVXUserList.getClass());
	}
	
	
	
	@SuppressWarnings("unchecked")
	@Test
	public void test25countXUsers() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
	
		vXLong.setValue(1);
		
		Mockito.when(xUserMgr.getXUserSearchCount(testSearchCriteria)).thenReturn(vXLong);
		VXLong testvxLong=xUserRest.countXUsers(request);
		Mockito.verify(xUserMgr).getXUserSearchCount(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		
		assertNotNull(testvxLong);
		assertEquals(testvxLong.getValue(),vXLong.getValue());
		assertEquals(testvxLong.getClass(),vXLong.getClass());
	}
	@Test
	public void test26getXGroupUser() {
			VXGroupUser testVXGroupUser= createVXGroupUser();
			
			Mockito.when(xUserMgr.getXGroupUser(id)).thenReturn(testVXGroupUser);
			VXGroupUser retVxGroupUser= xUserRest.getXGroupUser(id);
			
			assertNotNull(retVxGroupUser);
			assertEquals(testVXGroupUser.getClass(),retVxGroupUser.getClass());
			assertEquals(testVXGroupUser.getId(),retVxGroupUser.getId());
			Mockito.verify(xUserMgr).getXGroupUser(id);
	}
	@Test
	public void test27createXGroupUser() {
		VXGroupUser testVXGroupUser= createVXGroupUser();
		
		Mockito.when(xUserMgr.createXGroupUser(testVXGroupUser)).thenReturn(testVXGroupUser);
		VXGroupUser retVxGroupUser= xUserRest.createXGroupUser(testVXGroupUser);
		
		assertNotNull(retVxGroupUser);
		assertEquals(testVXGroupUser.getClass(),retVxGroupUser.getClass());
		assertEquals(testVXGroupUser.getId(),retVxGroupUser.getId());
		Mockito.verify(xUserMgr).createXGroupUser(testVXGroupUser);
	}
	@Test
	public void test28updateXGroupUser() {
		VXGroupUser testVXGroupUser= createVXGroupUser();
		
		Mockito.when(xUserMgr.updateXGroupUser(testVXGroupUser)).thenReturn(testVXGroupUser);
		VXGroupUser retVxGroupUser= xUserRest.updateXGroupUser(testVXGroupUser);
		
		assertNotNull(retVxGroupUser);
		assertEquals(testVXGroupUser.getClass(),retVxGroupUser.getClass());
		assertEquals(testVXGroupUser.getId(),retVxGroupUser.getId());
		Mockito.verify(xUserMgr).updateXGroupUser(testVXGroupUser);
	}
		 
	@Test
	public void test29deleteXGroupUser() {
		boolean force = true;

		Mockito.doNothing().when(xUserMgr).deleteXGroupUser(id, force);
		xUserRest.deleteXGroupUser(id,request);
		Mockito.verify(xUserMgr).deleteXGroupUser(id,force);
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test30searchXGroupUsers() {
	
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
		
		VXGroupUserList testVXGroupUserList=new VXGroupUserList();
		VXGroupUser vXGroupUser = createVXGroupUser();
		List<VXGroupUser> vXGroupUsers= new ArrayList<VXGroupUser>();
		vXGroupUsers.add(vXGroupUser);
		testVXGroupUserList.setVXGroupUsers(vXGroupUsers);
		Mockito.when(xUserMgr.searchXGroupUsers(testSearchCriteria)).thenReturn(testVXGroupUserList);
		VXGroupUserList outputvXGroupList=xUserRest.searchXGroupUsers(request);
		
		Mockito.verify(xUserMgr).searchXGroupUsers(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		
		assertNotNull(outputvXGroupList);
		assertEquals(outputvXGroupList.getClass(),testVXGroupUserList.getClass());
		assertEquals(outputvXGroupList.getResultSize(),testVXGroupUserList.getResultSize());
		
	}	@SuppressWarnings("unchecked")
	@Test
	public void test31countXGroupUserst() {
	
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();

		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
	
		vXLong.setValue(1);
		
		Mockito.when(xUserMgr.getXGroupUserSearchCount(testSearchCriteria)).thenReturn(vXLong);
		VXLong testvxLong=xUserRest.countXGroupUsers(request);
		Mockito.verify(xUserMgr).getXGroupUserSearchCount(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		
		assertNotNull(testvxLong);
		assertEquals(testvxLong.getValue(),vXLong.getValue());
		assertEquals(testvxLong.getClass(),vXLong.getClass());
	}
	@Test
	public void test32getXGroupGroup() {
		VXGroupGroup compareTestVXGroup=createVXGroupGroup();
		
		Mockito.when(xUserMgr.getXGroupGroup(id)).thenReturn(compareTestVXGroup);
		VXGroupGroup retVxGroup= xUserRest.getXGroupGroup(id);
		
		assertNotNull(retVxGroup);
		assertEquals(compareTestVXGroup.getClass(),retVxGroup.getClass());
		assertEquals(compareTestVXGroup.getId(),retVxGroup.getId());
		Mockito.verify(xUserMgr).getXGroupGroup(id);
	}	@Test
	public void test33createXGroupGroup() {
		VXGroupGroup compareTestVXGroup=createVXGroupGroup();
		 
		Mockito.when(xUserMgr.createXGroupGroup(compareTestVXGroup)).thenReturn(compareTestVXGroup);
		VXGroupGroup retVxGroup= xUserRest.createXGroupGroup(compareTestVXGroup);
			
		assertNotNull(retVxGroup);
		assertEquals(compareTestVXGroup.getClass(),retVxGroup.getClass());
		assertEquals(compareTestVXGroup.getId(),retVxGroup.getId());
		Mockito.verify(xUserMgr).createXGroupGroup(compareTestVXGroup);
	}
	@Test
	public void test34updateXGroupGroup() {
		VXGroupGroup compareTestVXGroup=createVXGroupGroup();
		 
		Mockito.when(xUserMgr.updateXGroupGroup(compareTestVXGroup)).thenReturn(compareTestVXGroup);
		VXGroupGroup retVxGroup= xUserRest.updateXGroupGroup(compareTestVXGroup);
			
		assertNotNull(retVxGroup);
		assertEquals(compareTestVXGroup.getClass(),retVxGroup.getClass());
		assertEquals(compareTestVXGroup.getId(),retVxGroup.getId());
		Mockito.verify(xUserMgr).updateXGroupGroup(compareTestVXGroup);
	}
	@Test
	public void test35deleteXGroupGroup() {
		boolean forceDelete = false;
		
		Mockito.doNothing().when(xUserMgr).deleteXGroupGroup(id, forceDelete);
		xUserRest.deleteXGroupGroup(id,request);
		Mockito.verify(xUserMgr).deleteXGroupGroup(id,forceDelete);
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test36searchXGroupGroups() {
		VXGroupGroupList testvXGroupGroupList=new VXGroupGroupList();
		VXGroupGroup testVXGroup=createVXGroupGroup();
		List<VXGroupGroup> testVXGroupGroups= new ArrayList<VXGroupGroup>();
		testVXGroupGroups.add(testVXGroup);
		testvXGroupGroupList.setVXGroupGroups(testVXGroupGroups);
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);

		Mockito.when(xUserMgr.searchXGroupGroups(testSearchCriteria)).thenReturn(testvXGroupGroupList);
		VXGroupGroupList outputvXGroupGroupList=xUserRest.searchXGroupGroups(request);
		
		Mockito.verify(xUserMgr).searchXGroupGroups(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		
		assertNotNull(outputvXGroupGroupList);
		assertEquals(outputvXGroupGroupList.getClass(),testvXGroupGroupList.getClass());
		assertEquals(outputvXGroupGroupList.getResultSize(),testvXGroupGroupList.getResultSize());
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test37countXGroupGroups() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
				
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
		
		vXLong.setValue(1);
			
		Mockito.when(xUserMgr.getXGroupGroupSearchCount(testSearchCriteria)).thenReturn(vXLong);
		VXLong testvxLong=xUserRest.countXGroupGroups(request);
		Mockito.verify(xUserMgr).getXGroupGroupSearchCount(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
			
		assertNotNull(testvxLong);
		assertEquals(testvxLong.getClass(),vXLong.getClass());
		assertEquals(testvxLong.getValue(),vXLong.getValue());
	}
	@Test
	public void test38getXPermMapVXResourceNull() throws Exception{
		VXPermMap permMap = testcreateXPermMap();
		
		Mockito.when(xUserMgr.getXPermMap(id)).thenReturn(permMap);
		
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		
		VXPermMap retVxGroup= xUserRest.getXPermMap(id);
		
		Mockito.verify(xUserMgr).getXPermMap(id);
		Mockito.verify(xResourceService).readResource(null);
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyString(), (MessageEnums)Mockito.any());
		assertEquals(permMap.getId(),retVxGroup.getId());
		assertEquals(permMap.getClass(),retVxGroup.getClass());
		assertNotNull(retVxGroup);
		
	
	}
	@Test
	public void test39getXPermMapNotNull() throws Exception{
		VXPermMap permMap = testcreateXPermMap();
		
		Mockito.when(xUserMgr.getXPermMap(id)).thenReturn(permMap);
		VXResource testVxResource= new VXResource();
		Mockito.when(xResourceService.readResource(id)).thenReturn(testVxResource);
		
		VXPermMap retVxGroup=xUserRest.getXPermMap(id);
		assertEquals(permMap.getId(),retVxGroup.getId());
		assertEquals(permMap.getClass(),retVxGroup.getClass());
		assertNotNull(retVxGroup);
		Mockito.verify(xUserMgr).getXPermMap(id);
		Mockito.verify(xResourceService).readResource(id);
	}
	@Test
	public void test40getXPermMapNull() {
		
		Mockito.when(xUserMgr.getXPermMap(id)).thenReturn(null);
		VXPermMap retVxGroup=xUserRest.getXPermMap(id);
		assertNull(retVxGroup);
		Mockito.verify(xUserMgr).getXPermMap(id);
	}
	
	@Test
	public void test41createXPermMap() {

		VXPermMap permMap = testcreateXPermMap();
		permMap.setResourceId(null);
		Mockito.when(xResourceService.readResource(permMap.getResourceId())).thenReturn(null);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		
		VXPermMap retVxGroup=xUserRest.createXPermMap(permMap);
		
		assertEquals(permMap.getId(),retVxGroup.getId());
		assertEquals(permMap.getClass(),retVxGroup.getClass());
		assertNotNull(retVxGroup);
		
		Mockito.verify(xUserMgr).createXPermMap(permMap);
		Mockito.verify(xResourceService).readResource(permMap.getResourceId());
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyString(), (MessageEnums)Mockito.any());
	}
	
	@Test
	public void test42createXPermMapNull() {

		Mockito.when(xUserMgr.createXPermMap(null)).thenReturn(null);
		VXPermMap retVxGroup=xUserRest.createXPermMap(null);
		Mockito.verify(xUserMgr).createXPermMap(null);
		assertNull(retVxGroup);
		
	}
	
	@Test
	public void test43createXPermMapNullVXResource() {
		VXPermMap permMap = testcreateXPermMap();
		permMap.setResourceId(null);
		Mockito.when(xUserMgr.createXPermMap(permMap)).thenReturn(permMap);
		VXResource testVxResource= new VXResource();
		Mockito.when(xResourceService.readResource(permMap.getResourceId())).thenReturn(testVxResource);
		
		VXPermMap retVxGroup=xUserRest.createXPermMap(permMap);
		
		assertEquals(permMap.getId(),retVxGroup.getId());
		assertEquals(permMap.getClass(),retVxGroup.getClass());
		assertNotNull(retVxGroup);
		
		Mockito.verify(xUserMgr).createXPermMap(permMap);
		Mockito.verify(xResourceService).readResource(permMap.getResourceId());
	}

	@Test
	public void test44updateXPermMap() {

		VXPermMap permMap = testcreateXPermMap();
		
		VXResource testVxResource= new VXResource();
		Mockito.when(xResourceService.readResource(id)).thenReturn(testVxResource);
		Mockito.when(xUserMgr.updateXPermMap(permMap)).thenReturn(permMap);
		VXPermMap retVxGroup=xUserRest.updateXPermMap(permMap);
		
		assertEquals(permMap.getId(),retVxGroup.getId());
		assertEquals(permMap.getClass(),retVxGroup.getClass());
		assertNotNull(retVxGroup);
		
		Mockito.verify(xUserMgr).updateXPermMap(permMap);
		Mockito.verify(xResourceService).readResource(permMap.getResourceId());
	}
	@Test
	public void test45updateXPermMap() {
		VXPermMap vXPermMap=null ;
		VXPermMap retVxGroup=xUserRest.updateXPermMap(vXPermMap);
		assertNull(retVxGroup);
	}
	@Test
	public void test46updateXPermMap() {
		VXPermMap permMap = testcreateXPermMap();
		
		Mockito.when(xResourceService.readResource(permMap.getResourceId())).thenReturn(null);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		
		VXPermMap retVxGroup=xUserRest.updateXPermMap(permMap);
		
		assertEquals(permMap.getId(),retVxGroup.getId());
		assertEquals(permMap.getClass(),retVxGroup.getClass());
		assertNotNull(retVxGroup);
		
		Mockito.verify(xUserMgr).updateXPermMap(permMap);
		Mockito.verify(xResourceService).readResource(permMap.getResourceId());
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyString());
		
	}
	@Test
	public void test47deleteXPermMap() {
		Boolean	forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXPermMap(id, forceDelete);
		xUserRest.deleteXPermMap(id,request);
		Mockito.verify(xUserMgr).deleteXPermMap(id,forceDelete);
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test48searchXPermMaps() {
		VXPermMap permMap = testcreateXPermMap();

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
	
		List <VXPermMap> vXPermMaps= new ArrayList<VXPermMap>();
		vXPermMaps.add(permMap);
		VXPermMapList testvXGroupList=new VXPermMapList() ;
		testvXGroupList.setTotalCount(1);
		testvXGroupList.setVXPermMaps(vXPermMaps);
		Mockito.when(xUserMgr.searchXPermMaps(testSearchCriteria)).thenReturn(testvXGroupList);
		VXPermMapList outputvXGroupList=xUserRest.searchXPermMaps(request);
		
		Mockito.verify(xUserMgr).searchXPermMaps(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		
		assertNotNull(outputvXGroupList);
		assertEquals(outputvXGroupList.getClass(),testvXGroupList.getClass());
		assertEquals(outputvXGroupList.getTotalCount(),testvXGroupList.getTotalCount());
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test49countXPermMaps() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
	
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
			
		vXLong.setValue(1);
		Mockito.when(xUserMgr.getXPermMapSearchCount(testSearchCriteria)).thenReturn(vXLong);
		VXLong testvxLong=xUserRest.countXPermMaps(request);
		Mockito.verify(xUserMgr).getXPermMapSearchCount(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
				
		assertNotNull(testvxLong);
	}
	@Test
	public void test50getXAuditMapVXAuditMapNull() {
		VXAuditMap testvXAuditMap =  createVXAuditMapObj();
		Mockito.when(xUserMgr.getXAuditMap(testvXAuditMap.getResourceId())).thenReturn(testvXAuditMap);

		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		
		VXAuditMap retVXAuditMap=xUserRest.getXAuditMap(testvXAuditMap.getResourceId());
		
		assertEquals(testvXAuditMap.getId(),retVXAuditMap.getId());
		assertEquals(testvXAuditMap.getClass(),retVXAuditMap.getClass());
		assertNotNull(retVXAuditMap);
		
		Mockito.verify(xUserMgr).getXAuditMap(testvXAuditMap.getResourceId());
		Mockito.verify(xResourceService).readResource(null);
		Mockito.verify(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums)Mockito.any()));
		
	}
	@Test
	public void test51getXAuditMapNull() {
		VXAuditMap testvXAuditMap =  createVXAuditMapObj();
		Mockito.when(xUserMgr.getXAuditMap(testvXAuditMap.getResourceId())).thenReturn(null);
		VXAuditMap retVXAuditMap=xUserRest.getXAuditMap(testvXAuditMap.getResourceId());
		
		assertNull(retVXAuditMap);
		
		Mockito.verify(xUserMgr).getXAuditMap(testvXAuditMap.getResourceId());
	
		
	}
	@Test
	public void test52getXAuditMap() {
		VXAuditMap testvXAuditMap =  createVXAuditMapObj();
		
		
		Mockito.when(xUserMgr.getXAuditMap(id)).thenReturn(testvXAuditMap);
		VXResource testVxResource= createVXResource();
		Mockito.when(xResourceService.readResource(testvXAuditMap.getResourceId())).thenReturn(testVxResource);

		VXAuditMap retVXAuditMap=xUserRest.getXAuditMap(id);
		
		assertEquals(testvXAuditMap.getId(),retVXAuditMap.getId());
		assertEquals(testvXAuditMap.getClass(),retVXAuditMap.getClass());
		assertNotNull(retVXAuditMap);
		
		Mockito.verify(xUserMgr).getXAuditMap(id);
		Mockito.verify(xResourceService).readResource(testvXAuditMap.getResourceId());

		
	}

	@Test
	public void test53createXAuditMap() {
		VXAuditMap testvXAuditMap =  createVXAuditMapObj();
		
		Mockito.when(xUserMgr.createXAuditMap(testvXAuditMap)).thenReturn(testvXAuditMap);
		VXResource testVxResource= createVXResource();
		Mockito.when(xResourceService.readResource(testvXAuditMap.getResourceId())).thenReturn(testVxResource);
		VXAuditMap retvXAuditMap= xUserRest.createXAuditMap(testvXAuditMap);
		assertEquals(testvXAuditMap.getId(),retvXAuditMap.getId());
		assertEquals(testvXAuditMap.getClass(),retvXAuditMap.getClass());
		assertNotNull(retvXAuditMap);
		
		Mockito.verify(xUserMgr).createXAuditMap(testvXAuditMap);
		Mockito.verify(xResourceService).readResource(testvXAuditMap.getResourceId());
		
		
	}
	
	@Test
	public void test54createXAuditMapVxResourceNull() {

		VXAuditMap testvXAuditMap =  createVXAuditMapObj();
		testvXAuditMap.setResourceId(null);
		
		Mockito.when(xResourceService.readResource(testvXAuditMap.getResourceId())).thenReturn(null);

		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		
		VXAuditMap retvXAuditMap= xUserRest.createXAuditMap(testvXAuditMap);
		assertEquals(testvXAuditMap.getId(),retvXAuditMap.getId());
		assertEquals(testvXAuditMap.getClass(),retvXAuditMap.getClass());
		assertNotNull(retvXAuditMap);
		
		Mockito.verify(xUserMgr).createXAuditMap(testvXAuditMap);
		Mockito.verify(xResourceService).readResource(testvXAuditMap.getResourceId());
		Mockito.verify(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums)Mockito.any()));
		
	}@Test
	public void test55createXAuditMapNull() {
		VXAuditMap testvXAuditMap =  createVXAuditMapObj();
		testvXAuditMap.setResourceId(null);
		VXAuditMap retvXAuditMap=xUserRest.createXAuditMap(null);
		assertNull(retvXAuditMap);
		Mockito.verify(xUserMgr).createXAuditMap(null);
		
	}
	
	@Test
	public void test56updateXAuditMap() {
		VXAuditMap testvXAuditMap =  createVXAuditMapObj();
		testvXAuditMap.setResourceId(id);
		Mockito.when(xUserMgr.updateXAuditMap(testvXAuditMap)).thenReturn(testvXAuditMap);
		VXResource testVxResource= createVXResource();
		Mockito.when(xResourceService.readResource(testvXAuditMap.getResourceId())).thenReturn(testVxResource);

		VXAuditMap retvXAuditMap=xUserRest.updateXAuditMap(testvXAuditMap);
		assertEquals(testvXAuditMap.getId(),retvXAuditMap.getId());
		assertEquals(testvXAuditMap.getClass(),retvXAuditMap.getClass());
		assertNotNull(retvXAuditMap);
		
		Mockito.verify(xUserMgr).updateXAuditMap(testvXAuditMap);
		Mockito.verify(xResourceService).readResource(testvXAuditMap.getResourceId());
	
		
	}
	@Test
	public void test57updateXAuditMapNull() {
		VXAuditMap testvXAuditMap =  createVXAuditMapObj();
		
		
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		VXAuditMap retvXAuditMap=xUserRest.updateXAuditMap(testvXAuditMap);
		assertNull(retvXAuditMap);
		Mockito.verify(xUserMgr).updateXAuditMap(testvXAuditMap);
		Mockito.verify(xResourceService).readResource(null);
		Mockito.verify(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums)Mockito.any()));
	}
	@Test
	public void test58updateXAuditMapVXResourceNull() {
		VXAuditMap vXAuditMap =null;
		VXAuditMap retvXAuditMap=xUserRest.updateXAuditMap(vXAuditMap);
		assertNull(retvXAuditMap);
	}
	@Test
	public void test59deleteXAuditMap() {
		
		Boolean	forceDelete=false;
			Mockito.doNothing().when(xUserMgr).deleteXAuditMap(id, forceDelete);
			xUserRest.deleteXAuditMap(id,request);
			Mockito.verify(xUserMgr).deleteXAuditMap(id,forceDelete);
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test60searchXAuditMaps() {
		VXAuditMap testvXAuditMap =  createVXAuditMapObj();
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
	
		List <VXAuditMap> testvXAuditMaps= new ArrayList<VXAuditMap>();
		testvXAuditMaps.add(testvXAuditMap);
		VXAuditMapList testVXAuditMapList=new VXAuditMapList() ;
		testVXAuditMapList.setVXAuditMaps(testvXAuditMaps);
		Mockito.when(xUserMgr.searchXAuditMaps(testSearchCriteria)).thenReturn(testVXAuditMapList);
		VXAuditMapList outputVXAuditMapList=xUserRest.searchXAuditMaps(request);
		
		Mockito.verify(xUserMgr).searchXAuditMaps(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		
		assertNotNull(outputVXAuditMapList);
		assertEquals(outputVXAuditMapList.getClass(),testVXAuditMapList.getClass());
		assertEquals(outputVXAuditMapList.getResultSize(),testVXAuditMapList.getResultSize());
		
	
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test61countXAuditMaps() {
		

		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
			
		vXLong.setValue(1);
		Mockito.when(xUserMgr.getXAuditMapSearchCount(testSearchCriteria)).thenReturn(vXLong);
		VXLong testvxLong=xUserRest.countXAuditMaps(request);
		Mockito.verify(xUserMgr).getXAuditMapSearchCount(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		assertEquals(testvxLong.getClass(),vXLong.getClass());
		assertEquals(testvxLong.getValue(),vXLong.getValue());
		assertNotNull(testvxLong);
	}
	@Test
	public void test62getXUserByUserName() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		VXUser compareTestVxUser=createVXUser();
		
		Mockito.when(xUserMgr.getXUserByUserName("User1")).thenReturn(compareTestVxUser);
		VXUser retVXUser= xUserRest.getXUserByUserName(request,"User1");
		
		assertNotNull(retVXUser);
		assertEquals(compareTestVxUser.getClass(),retVXUser.getClass());
		assertEquals(compareTestVxUser.getId(),retVXUser.getId());
		Mockito.verify(xUserMgr).getXUserByUserName("User1");
	}
	@Test
	public void test63getXGroupByGroupName() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		VXGroup compareTestVXGroup=createVXGroup();
		
		Mockito.when(xGroupService.getGroupByGroupName(compareTestVXGroup.getName())).thenReturn(compareTestVXGroup);
		
		VXGroup retVxGroup= xUserRest.getXGroupByGroupName(request,compareTestVXGroup.getName());
		
		assertNotNull(retVxGroup);
		assertEquals(compareTestVXGroup.getClass(),compareTestVXGroup.getClass());
		assertEquals(compareTestVXGroup.getId(),compareTestVXGroup.getId());
		Mockito.verify(xGroupService).getGroupByGroupName(compareTestVXGroup.getName());
	}
	@Test
	public void test64deleteXUserByUserName() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="true";
		boolean forceDelete = false;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXUser testUser= createVXUser();
		Mockito.when(xUserService.getXUserByUserName(testUser.getName())).thenReturn(testUser);
		forceDelete=true;
		Mockito.doNothing().when(xUserMgr).deleteXUser(testUser.getId(), forceDelete);
		xUserRest.deleteXUserByUserName(testUser.getName(),request);
		Mockito.verify(xUserMgr).deleteXUser(testUser.getId(),forceDelete);
		Mockito.verify(xUserService).getXUserByUserName(testUser.getName());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test65deleteXUserByUserNametrue() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
		boolean forceDelete = true;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXUser testUser= createVXUser();
		Mockito.when(xUserService.getXUserByUserName(testUser.getName())).thenReturn(testUser);
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXUser(testUser.getId(), forceDelete);
		xUserRest.deleteXUserByUserName(testUser.getName(),request);
		Mockito.verify(xUserMgr).deleteXUser(testUser.getId(),forceDelete);
		Mockito.verify(xUserService).getXUserByUserName(testUser.getName());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test66deleteXUserByUserNameNull() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr=null;
		boolean forceDelete = true;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXUser testUser= createVXUser();
		Mockito.when(xUserService.getXUserByUserName(testUser.getName())).thenReturn(testUser);
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXUser(testUser.getId(), forceDelete);
		xUserRest.deleteXUserByUserName(testUser.getName(),request);
		Mockito.verify(xUserMgr).deleteXUser(testUser.getId(),forceDelete);
		Mockito.verify(xUserService).getXUserByUserName(testUser.getName());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test67deleteXGroupByGroupName() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
		boolean forceDelete = true;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXGroup testVXGroup= createVXGroup();
		Mockito.when(xGroupService.getGroupByGroupName(testVXGroup.getName())).thenReturn(testVXGroup);
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXGroup(testVXGroup.getId(), forceDelete);
		xUserRest.deleteXGroupByGroupName(testVXGroup.getName(),request);
		Mockito.verify(xUserMgr).deleteXGroup(testVXGroup.getId(),forceDelete);
		Mockito.verify(xGroupService).getGroupByGroupName(testVXGroup.getName());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test68deleteXGroupByGroupNameNull() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr=null;
		boolean forceDelete = true;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXGroup testVXGroup= createVXGroup();
		Mockito.when(xGroupService.getGroupByGroupName(testVXGroup.getName())).thenReturn(testVXGroup);
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXGroup(testVXGroup.getId(), forceDelete);
		xUserRest.deleteXGroupByGroupName(testVXGroup.getName(),request);
		Mockito.verify(xUserMgr).deleteXGroup(testVXGroup.getId(),forceDelete);
		Mockito.verify(xGroupService).getGroupByGroupName(testVXGroup.getName());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test69deleteXGroupByGroupNameflase() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="true";
		boolean forceDelete = false;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXGroup testVXGroup= createVXGroup();
		Mockito.when(xGroupService.getGroupByGroupName(testVXGroup.getName())).thenReturn(testVXGroup);
		forceDelete=true;
		Mockito.doNothing().when(xUserMgr).deleteXGroup(testVXGroup.getId(), forceDelete);
		xUserRest.deleteXGroupByGroupName(testVXGroup.getName(),request);
		Mockito.verify(xUserMgr).deleteXGroup(testVXGroup.getId(),forceDelete);
		Mockito.verify(xGroupService).getGroupByGroupName(testVXGroup.getName());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test70deleteXGroupAndXUser() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		VXGroup testVXGroup= createVXGroup();
		VXUser testVXuser= createVXUser();
		
		Mockito.doNothing().when(xUserMgr).deleteXGroupAndXUser(testVXGroup.getName(),testVXuser.getName());
		xUserRest.deleteXGroupAndXUser(testVXGroup.getName(),testVXuser.getName(),request);
		Mockito.verify(xUserMgr).deleteXGroupAndXUser(testVXGroup.getName(),testVXuser.getName());
	
		
	}
	@Test
	public void test71getXUserGroups() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
	
		VXGroupList groupList = createxGroupList();
		Mockito.when(xUserMgr.getXUserGroups(id)).thenReturn(groupList);
		VXGroupList retVxGroupList= xUserRest.getXUserGroups(request,id);
		
		assertNotNull(retVxGroupList);
		assertEquals(groupList.getClass(),retVxGroupList.getClass());
		assertEquals(groupList.getResultSize(),retVxGroupList.getResultSize());
		Mockito.verify(xUserMgr).getXUserGroups(id);
	}
	@Test
	public void test72getXGroupUsers() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		VXGroupList groupList = createxGroupList();
		Mockito.when(xUserMgr.getXUserGroups(id)).thenReturn(groupList);
		VXGroupList retVxGroupList= xUserRest.getXUserGroups(request,id);
		
		assertNotNull(retVxGroupList);
		assertEquals(groupList.getClass(),retVxGroupList.getClass());
		assertEquals(groupList.getResultSize(),retVxGroupList.getResultSize());
		Mockito.verify(xUserMgr).getXUserGroups(id);
	}
        @SuppressWarnings("unchecked")
	@Test
	public void test73getXGroupUsers() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
                SearchCriteria testSearchCriteria=createsearchCriteria();
                testSearchCriteria.addParam("xGroupId", id);

                Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
		
		VXUser testVXUser=createVXUser();
		VXUserList testVXUserList= new VXUserList();
		List<VXUser> testVXUsers = new ArrayList<VXUser>();
		testVXUsers.add(testVXUser);
		testVXUserList.setVXUsers(testVXUsers);
		testVXUserList.setStartIndex(1);
		testVXUserList.setTotalCount(1);
                Mockito.when(xUserMgr.getXGroupUsers(testSearchCriteria)).thenReturn(testVXUserList);
		VXUserList retVxGroupList= xUserRest.getXGroupUsers(request,id);
		
		assertNotNull(retVxGroupList);
		assertEquals(testVXUserList.getTotalCount(),retVxGroupList.getTotalCount());
		assertEquals(testVXUserList.getStartIndex(),retVxGroupList.getStartIndex());
                Mockito.verify(xUserMgr).getXGroupUsers(testSearchCriteria);
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test74getAuthSessions() {
	
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		

		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
	
		
		Mockito.when(searchUtil.extractLong(request, testSearchCriteria, "id", "Auth Session Id")).thenReturn(1L);
		Mockito.when(searchUtil.extractLong(request, testSearchCriteria, "userId", "User Id")).thenReturn(1L);
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "authStatus", "Auth Status")).thenReturn(1);
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "deviceType", "Device Type")).thenReturn(1);
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "firstName", "User First Name", StringUtil.VALIDATION_NAME)).thenReturn("");
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "lastName", "User Last Name", StringUtil.VALIDATION_NAME)).thenReturn("");
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "requestUserAgent", "User Agent", StringUtil.VALIDATION_TEXT)).thenReturn("");
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "requestIP", "Request IP Address", StringUtil.VALIDATION_IP_ADDRESS)).thenReturn("");
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "loginId", "Login ID", StringUtil.VALIDATION_TEXT)).thenReturn("");
		
		
		VXAuthSessionList testVXAuthSessionList=new VXAuthSessionList();
		testVXAuthSessionList.setTotalCount(1);
		testVXAuthSessionList.setStartIndex(1);
		VXAuthSession testVXAuthSession = createVXAuthSession();
		List<VXAuthSession> testvXAuthSessions = new ArrayList<VXAuthSession>();
		testvXAuthSessions.add(testVXAuthSession);
		
		testVXAuthSessionList.setVXAuthSessions(testvXAuthSessions);
		Mockito.when(sessionMgr.searchAuthSessions(testSearchCriteria)).thenReturn(testVXAuthSessionList);
		VXAuthSessionList outputvXGroupList=xUserRest.getAuthSessions(request);
		
		Mockito.verify(sessionMgr).searchAuthSessions(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		Mockito.verify(searchUtil).extractLong(request, testSearchCriteria, "id", "Auth Session Id");
		Mockito.verify(searchUtil).extractLong(request, testSearchCriteria, "userId", "User Id");
		Mockito.verify(searchUtil).extractInt(request, testSearchCriteria, "authStatus", "Auth Status");
                Mockito.verify(searchUtil).extractInt(request, testSearchCriteria, "authType", "Login Type");
		Mockito.verify(searchUtil).extractInt(request, testSearchCriteria, "deviceType", "Device Type");
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "firstName", "User First Name", StringUtil.VALIDATION_NAME);
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "lastName", "User Last Name", StringUtil.VALIDATION_NAME);
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "requestUserAgent", "User Agent", StringUtil.VALIDATION_TEXT);
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "requestIP", "Request IP Address", StringUtil.VALIDATION_IP_ADDRESS);
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "loginId", "Login ID", StringUtil.VALIDATION_TEXT);
                Mockito.verify(searchUtil).extractDate(request, testSearchCriteria, "startDate", "Start Date", null);
                Mockito.verify(searchUtil).extractDate(request, testSearchCriteria, "endDate", "End Date", null);
		assertNotNull(outputvXGroupList);
		assertEquals(outputvXGroupList.getStartIndex(),testVXAuthSessionList.getStartIndex());
		assertEquals(outputvXGroupList.getTotalCount(), testVXAuthSessionList.getTotalCount());
	}
	@Test
	public void test75getAuthSession() {
		String authSessionId ="testauthSessionId";
		VXAuthSession testVXAuthSession= createVXAuthSession();
	
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		Mockito.when(request.getParameter("extSessionId")).thenReturn(authSessionId);
		Mockito.when(sessionMgr.getAuthSessionBySessionId(authSessionId)).thenReturn(testVXAuthSession);
		VXAuthSession retVXAuthSession=xUserRest.getAuthSession(request);
		Mockito.verify(sessionMgr).getAuthSessionBySessionId(authSessionId);
		Mockito.verify(request).getParameter("extSessionId");
		assertEquals(testVXAuthSession.getId(), retVXAuthSession.getId());
		assertEquals(testVXAuthSession.getClass(), retVXAuthSession.getClass());
		assertNotNull(retVXAuthSession);
	}
	@Test
	public void test76createXModuleDefPermission() {

		VXModuleDef	testVXModuleDef = createVXModuleDef();
		
		Mockito.doNothing().when(xUserMgr).checkAdminAccess();
		
		Mockito.when(xUserMgr.createXModuleDefPermission(testVXModuleDef)).thenReturn(testVXModuleDef);
		VXModuleDef retVxModuleDef=xUserRest.createXModuleDefPermission(testVXModuleDef);
		
		assertNotNull(retVxModuleDef);
		assertEquals(testVXModuleDef.getId(),retVxModuleDef.getId());
		assertEquals(testVXModuleDef.getOwner(),retVxModuleDef.getOwner());
		Mockito.verify(xUserMgr).createXModuleDefPermission(testVXModuleDef);
		Mockito.verify(xUserMgr).checkAdminAccess();
		 
	}
	@Test
	public void test77getXModuleDefPermission() {
		VXModuleDef testVXModuleDef=createVXModuleDef();
		Mockito.when(xUserMgr.getXModuleDefPermission(testVXModuleDef.getId())).thenReturn(testVXModuleDef);
		VXModuleDef retVxModuleDef=xUserRest.getXModuleDefPermission(testVXModuleDef.getId());
			
		assertNotNull(retVxModuleDef);
		assertEquals(testVXModuleDef.getId(),retVxModuleDef.getId());
		assertEquals(testVXModuleDef.getOwner(),retVxModuleDef.getOwner());
			
		Mockito.verify(xUserMgr).getXModuleDefPermission(testVXModuleDef.getId());
		 
	}
	@Test
	public void test78updateXModuleDefPermission() {
	
		VXModuleDef	testVXModuleDef = createVXModuleDef();
		
		Mockito.doNothing().when(xUserMgr).checkAdminAccess();
		
		Mockito.when(xUserMgr.updateXModuleDefPermission(testVXModuleDef)).thenReturn(testVXModuleDef);
		VXModuleDef retVxModuleDef=xUserRest.updateXModuleDefPermission(testVXModuleDef);
		
		assertNotNull(retVxModuleDef);
		assertEquals(testVXModuleDef.getId(),retVxModuleDef.getId());
		assertEquals(testVXModuleDef.getOwner(),retVxModuleDef.getOwner());
		
		Mockito.verify(xUserMgr).updateXModuleDefPermission(testVXModuleDef);
		Mockito.verify(xUserMgr).checkAdminAccess();
	}
	@Test
	public void test79deleteXModuleDefPermission() {
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		
		boolean forceDelete = true;
		Mockito.doNothing().when(xUserMgr).checkAdminAccess();
		Mockito.doNothing().when(xUserMgr).deleteXModuleDefPermission(id, forceDelete);
		xUserRest.deleteXModuleDefPermission(id,request);
		Mockito.verify(xUserMgr).deleteXModuleDefPermission(id,forceDelete);
		Mockito.verify(xUserMgr).checkAdminAccess();
		
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test80searchXModuleDef() {
		VXModuleDefList testVXModuleDefList= new VXModuleDefList() ;
		VXModuleDef vXModuleDef=createVXModuleDef();
		List<VXModuleDef> VXModuleDefs= new ArrayList<VXModuleDef>();
		VXModuleDefs.add(vXModuleDef);
		testVXModuleDefList.setvXModuleDef(VXModuleDefs);
		testVXModuleDefList.setTotalCount(1);
		testVXModuleDefList.setStartIndex(1);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();

		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "module","modulename", null)).thenReturn("");
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "moduleDefList","id", null)).thenReturn("");
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "userName","userName", null)).thenReturn("");
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "groupName","groupName", null)).thenReturn("");
		
		Mockito.when(xUserMgr.searchXModuleDef(testSearchCriteria)).thenReturn(testVXModuleDefList);
		VXModuleDefList outputVXModuleDefList=xUserRest.searchXModuleDef(request);
		assertNotNull(outputVXModuleDefList);
		assertEquals(outputVXModuleDefList.getTotalCount(),testVXModuleDefList.getTotalCount());
		assertEquals(outputVXModuleDefList.getStartIndex(),testVXModuleDefList.getStartIndex());
		
		Mockito.verify(xUserMgr).searchXModuleDef(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "module","modulename", null);
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "moduleDefList","id", null);
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "userName","userName", null);
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "groupName","groupName", null);
		
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test81countXModuleDef() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		
	
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);

		vXLong.setValue(1);
		
		Mockito.when(xUserMgr.getXModuleDefSearchCount(testSearchCriteria)).thenReturn(vXLong);
		VXLong testvxLong=xUserRest.countXModuleDef(request);
		Mockito.verify(xUserMgr).getXModuleDefSearchCount(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		
		assertNotNull(testvxLong);
		assertEquals(testvxLong.getValue(),vXLong.getValue());
		assertEquals(testvxLong.getClass(),vXLong.getClass());
	}
	@Test
	public void test82createXUserPermission() {
		VXUserPermission testvXUserPermission = createVXUserPermission();
			
		Mockito.doNothing().when(xUserMgr).checkAdminAccess();
		Mockito.when(xUserMgr.createXUserPermission(testvXUserPermission)).thenReturn(testvXUserPermission);
		VXUserPermission retVXUserPermission=xUserRest.createXUserPermission(testvXUserPermission);
		Mockito.verify(xUserMgr).createXUserPermission(testvXUserPermission);
		Mockito.verify(xUserMgr).checkAdminAccess();
		assertNotNull(retVXUserPermission);
		assertEquals(retVXUserPermission.getId(), testvXUserPermission.getId());
		assertEquals(retVXUserPermission.getUserName(), testvXUserPermission.getUserName());
	}
	@Test
	public void test83getXUserPermission() {
		VXUserPermission testVXUserPermission=createVXUserPermission();
		Mockito.when(xUserMgr.getXUserPermission(testVXUserPermission.getId())).thenReturn(testVXUserPermission);
		VXUserPermission retVXUserPermission=xUserRest.getXUserPermission(testVXUserPermission.getId());
		Mockito.verify(xUserMgr).getXUserPermission(id);
		assertNotNull(retVXUserPermission);
		assertEquals(retVXUserPermission.getId(), testVXUserPermission.getId());
		assertEquals(retVXUserPermission.getUserName(), testVXUserPermission.getUserName());
	}
	@Test
	public void test84updateXUserPermission() {
		VXUserPermission testvXUserPermission = createVXUserPermission();
		Mockito.doNothing().when(xUserMgr).checkAdminAccess();
		Mockito.when(xUserMgr.updateXUserPermission(testvXUserPermission)).thenReturn(testvXUserPermission);
		VXUserPermission retVXUserPermission=xUserRest.updateXUserPermission(testvXUserPermission);
		Mockito.verify(xUserMgr).updateXUserPermission(testvXUserPermission);
		Mockito.verify(xUserMgr).checkAdminAccess();
		assertNotNull(retVXUserPermission);
		assertEquals(retVXUserPermission.getId(), testvXUserPermission.getId());
		assertEquals(retVXUserPermission.getUserName(), testvXUserPermission.getUserName());
	
	}
	@Test
	public void test85deleteXUserPermission() {
		boolean forceDelete = true;
		
		Mockito.doNothing().when(xUserMgr).checkAdminAccess();
		
		Mockito.doNothing().when(xUserMgr).deleteXUserPermission(id, forceDelete);
		xUserRest.deleteXUserPermission(id,request);
		Mockito.verify(xUserMgr).deleteXUserPermission(id,forceDelete);
		Mockito.verify(xUserMgr).checkAdminAccess();
		
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test86searchXUserPermission() {
		VXUserPermissionList testVXUserPermissionList= new VXUserPermissionList() ;
		testVXUserPermissionList.setTotalCount(1);
		testVXUserPermissionList.setStartIndex(1);
		VXUserPermission testVXUserPermission=createVXUserPermission();
		List<VXUserPermission> testVXUserPermissions= new ArrayList<VXUserPermission>();
		testVXUserPermissions.add(testVXUserPermission);
		testVXUserPermissionList.setvXModuleDef(testVXUserPermissions);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "id", "id",StringUtil.VALIDATION_NAME)).thenReturn("");
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "userPermissionList","userId", StringUtil.VALIDATION_NAME)).thenReturn("");
			
			
		Mockito.when(xUserMgr.searchXUserPermission(testSearchCriteria)).thenReturn(testVXUserPermissionList);
		VXUserPermissionList outputVXUserPermissionList=xUserRest.searchXUserPermission(request);
		assertNotNull(outputVXUserPermissionList);
		assertEquals(outputVXUserPermissionList.getStartIndex(),testVXUserPermissionList.getStartIndex());
		assertEquals(outputVXUserPermissionList.getTotalCount(),testVXUserPermissionList.getTotalCount());
		
		Mockito.verify(xUserMgr).searchXUserPermission(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "id", "id",StringUtil.VALIDATION_NAME);
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "userPermissionList","userId", StringUtil.VALIDATION_NAME);
			
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test87countXUserPermission() {
	
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		
		
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
	
		vXLong.setValue(1);
		
		Mockito.when(xUserMgr.getXUserPermissionSearchCount(testSearchCriteria)).thenReturn(vXLong);
		VXLong testvxLong=xUserRest.countXUserPermission(request);
		Mockito.verify(xUserMgr).getXUserPermissionSearchCount(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		
		assertNotNull(testvxLong);
		assertEquals(testvxLong.getValue(),vXLong.getValue());
		assertEquals(testvxLong.getClass(),vXLong.getClass());
		
	}
	@Test
	public void test88createXGroupPermission() {
		
		VXGroupPermission testVXGroupPermission = createVXGroupPermission();
		
		Mockito.doNothing().when(xUserMgr).checkAdminAccess();
		Mockito.when(xUserMgr.createXGroupPermission(testVXGroupPermission)).thenReturn(testVXGroupPermission);
		VXGroupPermission retVXGroupPermission=xUserRest.createXGroupPermission(testVXGroupPermission);
		Mockito.verify(xUserMgr).createXGroupPermission(testVXGroupPermission);
		Mockito.verify(xUserMgr).checkAdminAccess();
		assertNotNull(retVXGroupPermission);
		assertEquals(retVXGroupPermission.getId(), testVXGroupPermission.getId());
		assertEquals(retVXGroupPermission.getClass(), testVXGroupPermission.getClass());
		
	}
	@Test
	public void test89getXGroupPermission() {
		VXGroupPermission testVXGroupPermission =createVXGroupPermission();
		Mockito.when(xUserMgr.getXGroupPermission(testVXGroupPermission.getId())).thenReturn(testVXGroupPermission);
		VXGroupPermission retVXGroupPermission=xUserRest.getXGroupPermission(testVXGroupPermission.getId());
		Mockito.verify(xUserMgr).getXGroupPermission(testVXGroupPermission.getId());
		assertNotNull(retVXGroupPermission);
		assertEquals(retVXGroupPermission.getId(), testVXGroupPermission.getId());
		assertEquals(retVXGroupPermission.getClass(), testVXGroupPermission.getClass());
		
	}
	@Test
	public void test90updateXGroupPermission() {
	
		VXGroupPermission testVXGroupPermission = createVXGroupPermission();
		Mockito.doNothing().when(xUserMgr).checkAdminAccess();
		Mockito.when(xUserMgr.updateXGroupPermission(testVXGroupPermission)).thenReturn(testVXGroupPermission);
		VXGroupPermission retVXGroupPermission=xUserRest.updateXGroupPermission(testVXGroupPermission);
		Mockito.verify(xUserMgr).updateXGroupPermission(testVXGroupPermission);
		Mockito.verify(xUserMgr).checkAdminAccess();
		assertNotNull(retVXGroupPermission);
		assertEquals(retVXGroupPermission.getId(), testVXGroupPermission.getId());
		assertEquals(retVXGroupPermission.getClass(), testVXGroupPermission.getClass());
	
		
	}
	@Test
	public void test91deleteXGroupPermission() {
		
		boolean forceDelete = true;
		
		Mockito.doNothing().when(xUserMgr).checkAdminAccess();
		
		Mockito.doNothing().when(xUserMgr).deleteXGroupPermission(id, forceDelete);
		xUserRest.deleteXGroupPermission(id,request);
		Mockito.verify(xUserMgr).deleteXGroupPermission(id,forceDelete);
		Mockito.verify(xUserMgr).checkAdminAccess();
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test92searchXGroupPermission() {
		VXGroupPermissionList testVXGroupPermissionList= new VXGroupPermissionList() ;
		testVXGroupPermissionList.setTotalCount(1);
		VXGroupPermission testVXGroupPermission=createVXGroupPermission();
		List<VXGroupPermission> testVXGroupPermissions= new ArrayList<VXGroupPermission>();
		testVXGroupPermissions.add(testVXGroupPermission);
		testVXGroupPermissionList.setvXGroupPermission(testVXGroupPermissions);
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "id", "id",StringUtil.VALIDATION_NAME)).thenReturn("");
		Mockito.when(searchUtil.extractString(request, testSearchCriteria,"groupPermissionList", "groupId", StringUtil.VALIDATION_NAME)).thenReturn("");
		Mockito.when(xUserMgr.searchXGroupPermission(testSearchCriteria)).thenReturn(testVXGroupPermissionList);
		VXGroupPermissionList outputVXGroupPermissionList=xUserRest.searchXGroupPermission(request);
		assertNotNull(outputVXGroupPermissionList);
		assertEquals(outputVXGroupPermissionList.getClass(),testVXGroupPermissionList.getClass());
		assertEquals(outputVXGroupPermissionList.getTotalCount(),testVXGroupPermissionList.getTotalCount());
		
		Mockito.verify(xUserMgr).searchXGroupPermission(testSearchCriteria);
		
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria, "id", "id",StringUtil.VALIDATION_NAME);
		Mockito.verify(searchUtil).extractString(request, testSearchCriteria,"groupPermissionList", "groupId", StringUtil.VALIDATION_NAME);
	}
	@SuppressWarnings("unchecked")
	@Test
	public void test93countXGroupPermission() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();

		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
	
		vXLong.setValue(1);
		
		Mockito.when(xUserMgr.getXGroupPermissionSearchCount(testSearchCriteria)).thenReturn(vXLong);
		VXLong testvxLong=xUserRest.countXGroupPermission(request);
		Mockito.verify(xUserMgr).getXGroupPermissionSearchCount(testSearchCriteria);
		Mockito.verify(searchUtil).extractCommonCriterias((HttpServletRequest)Mockito.any() ,(List<SortField>)Mockito.any());
		
		assertNotNull(testvxLong);
		assertEquals(testvxLong.getValue(),vXLong.getValue());
		assertEquals(testvxLong.getClass(),vXLong.getClass());
		
	}
	@Test
	public void test94modifyUserActiveStatus() {
		HashMap<Long, Integer> statusMap= new HashMap<Long, Integer>();
		statusMap.put(id, 1);
		Mockito.doNothing().when(xUserMgr).modifyUserActiveStatus(statusMap);
		xUserRest.modifyUserActiveStatus(statusMap);
		Mockito.verify(xUserMgr).modifyUserActiveStatus(statusMap);
	}
	@Test
	public void test95setUserRolesByExternalID() {
		VXStringList testVXStringList= createVXStringList();
		Mockito.when(xUserMgr.setUserRolesByExternalID(id, testVXStringList.getVXStrings())).thenReturn(testVXStringList);
		VXStringList retVXStringList=xUserRest.setUserRolesByExternalID(id, testVXStringList);
		Mockito.verify(xUserMgr).setUserRolesByExternalID(id, testVXStringList.getVXStrings());
		
		assertNotNull(retVXStringList);
		assertEquals(testVXStringList.getTotalCount(), retVXStringList.getTotalCount());
		assertEquals(testVXStringList.getClass(), retVXStringList.getClass());
	}
	@Test
	public void test96setUserRolesByName() {
		VXStringList testVXStringList= createVXStringList();
		Mockito.when(xUserMgr.setUserRolesByName("Admin", testVXStringList.getVXStrings())).thenReturn(testVXStringList);
		VXStringList retVXStringList=xUserRest.setUserRolesByName("Admin", testVXStringList);
		Mockito.verify(xUserMgr).setUserRolesByName("Admin", testVXStringList.getVXStrings());
			
		assertNotNull(retVXStringList);
		assertEquals(testVXStringList.getTotalCount(), retVXStringList.getTotalCount());
		assertEquals(testVXStringList.getClass(), retVXStringList.getClass());
	}
	@Test
	public void test97getUserRolesByExternalID() {
		VXStringList testVXStringList=createVXStringList();
		
		Mockito.when(xUserMgr.getUserRolesByExternalID(id)).thenReturn(testVXStringList);
		VXStringList retVXStringList=xUserRest.getUserRolesByExternalID(id);
		Mockito.verify(xUserMgr).getUserRolesByExternalID(id);
		assertNotNull(retVXStringList);
		assertEquals(testVXStringList.getTotalCount(), retVXStringList.getTotalCount());
		assertEquals(testVXStringList.getClass(), retVXStringList.getClass());
		
	}
	@Test
	public void test98getUserRolesByName() {
		
		VXStringList testVXStringList=createVXStringList();
		
		Mockito.when(xUserMgr.getUserRolesByName("Admin")).thenReturn(testVXStringList);
		VXStringList retVXStringList=xUserRest.getUserRolesByName("Admin");
		Mockito.verify(xUserMgr).getUserRolesByName("Admin");
		assertNotNull(retVXStringList);
		assertEquals(testVXStringList.getTotalCount(), retVXStringList.getTotalCount());
		assertEquals(testVXStringList.getClass(), retVXStringList.getClass());
	}
	@Test
	public void test99deleteUsersByUserName() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="true";
		boolean forceDelete = false;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue("User1");
		VXUser testVXUser= createVXUser();
		VXStringList vxStringList=createVXStringList();
		
		Mockito.when(xUserService.getXUserByUserName(testVXString.getValue())).thenReturn(testVXUser);
		forceDelete=true;
		Mockito.doNothing().when(xUserMgr).deleteXUser(testVXUser.getId(), forceDelete);
		xUserRest.deleteUsersByUserName(request,vxStringList);
		Mockito.verify(xUserMgr).deleteXUser(testVXUser.getId(),forceDelete);
		Mockito.verify(xUserService).getXUserByUserName(testVXString.getValue());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test100deleteUsersByUserNameNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
		boolean forceDelete = true;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue("User1");
		VXUser testVXUser= createVXUser();
		VXStringList vxStringList=createVXStringList();
		
		Mockito.when(xUserService.getXUserByUserName(testVXString.getValue())).thenReturn(testVXUser);
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXUser(testVXUser.getId(), forceDelete);
		xUserRest.deleteUsersByUserName(request,vxStringList);
		Mockito.verify(xUserMgr).deleteXUser(testVXUser.getId(),forceDelete);
		Mockito.verify(xUserService).getXUserByUserName(testVXString.getValue());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test101deleteUsersByUserNameNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr=null;
		boolean forceDelete = true;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue("User1");
		VXUser testVXUser= createVXUser();
		VXStringList vxStringList=createVXStringList();
		
		Mockito.when(xUserService.getXUserByUserName(testVXString.getValue())).thenReturn(testVXUser);
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXUser(testVXUser.getId(), forceDelete);
		xUserRest.deleteUsersByUserName(request,vxStringList);
		Mockito.verify(xUserMgr).deleteXUser(testVXUser.getId(),forceDelete);
		Mockito.verify(xUserService).getXUserByUserName(testVXString.getValue());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test102deleteUsersByUserNameSetValueNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
		boolean forceDelete = true;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue("User1");
		VXUser testVXUser= createVXUser();
		VXStringList vxStringList=createVXStringList();
		
		Mockito.when(xUserService.getXUserByUserName(testVXString.getValue())).thenReturn(testVXUser);
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXUser(testVXUser.getId(), forceDelete);
		xUserRest.deleteUsersByUserName(request,vxStringList);
		Mockito.verify(xUserMgr).deleteXUser(testVXUser.getId(),forceDelete);
		Mockito.verify(xUserService).getXUserByUserName(testVXString.getValue());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test103deleteUsersByUserNameListNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue("User1");
		xUserRest.deleteUsersByUserName(request,null);
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test104deleteUsersByUserNameListGetListNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
	
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXStringList vxStringList=createVXStringList();
		vxStringList.setVXStrings(null);
		xUserRest.deleteUsersByUserName(request,vxStringList);
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test105deleteUsersByUserNameNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="true";
		
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue(null);
		
		VXStringList vxStringList=createVXStringList();
		List<VXString> testVXStrings=new ArrayList<VXString>();
		testVXStrings.add(testVXString);
		vxStringList.setVXStrings(testVXStrings);
		xUserRest.deleteUsersByUserName(request,vxStringList);
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	////////////////////////////////
	@Test
	public void test106deleteGroupsByGroupName() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="true";
		boolean forceDelete = false;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue("testVXGroup");
		VXGroup testVXGroup= createVXGroup();
		VXStringList vxStringList=createVXStringListGroup();
		
		Mockito.when(xGroupService.getGroupByGroupName(testVXString.getValue())).thenReturn(testVXGroup);
		forceDelete=true;
		Mockito.doNothing().when(xUserMgr).deleteXGroup(testVXGroup.getId(), forceDelete);
		xUserRest.deleteGroupsByGroupName(request,vxStringList);
		Mockito.verify(xUserMgr).deleteXGroup(testVXGroup.getId(),forceDelete);
		Mockito.verify(xGroupService).getGroupByGroupName(testVXString.getValue());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test107GroupsByGroupNameNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
		boolean forceDelete = true;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue("testVXGroup");
		VXGroup testVXGroup= createVXGroup();
		VXStringList vxStringList=createVXStringListGroup();
		
		Mockito.when(xGroupService.getGroupByGroupName(testVXString.getValue())).thenReturn(testVXGroup);
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXGroup(testVXGroup.getId(), forceDelete);
		xUserRest.deleteGroupsByGroupName(request,vxStringList);
		Mockito.verify(xUserMgr).deleteXGroup(testVXGroup.getId(),forceDelete);
		Mockito.verify(xGroupService).getGroupByGroupName(testVXString.getValue());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test108deleteGroupsByGroupNameNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr=null;
		boolean forceDelete = true;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue("testVXGroup");
		VXGroup testVXGroup= createVXGroup();
		VXStringList vxStringList=createVXStringListGroup();
		
		Mockito.when(xGroupService.getGroupByGroupName(testVXString.getValue())).thenReturn(testVXGroup);
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXGroup(testVXGroup.getId(), forceDelete);
		xUserRest.deleteGroupsByGroupName(request,vxStringList);
		Mockito.verify(xUserMgr).deleteXGroup(testVXGroup.getId(),forceDelete);
		Mockito.verify(xGroupService).getGroupByGroupName(testVXString.getValue());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test109deleteGroupsByGroupNameSetValueNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
		boolean forceDelete = true;
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue("testVXGroup");
		VXGroup testVXGroup= createVXGroup();
		VXStringList vxStringList=createVXStringListGroup();
		
		Mockito.when(xGroupService.getGroupByGroupName(testVXString.getValue())).thenReturn(testVXGroup);
		forceDelete=false;
		Mockito.doNothing().when(xUserMgr).deleteXGroup(testVXGroup.getId(), forceDelete);
		xUserRest.deleteGroupsByGroupName(request,vxStringList);
		Mockito.verify(xUserMgr).deleteXGroup(testVXGroup.getId(),forceDelete);
		Mockito.verify(xGroupService).getGroupByGroupName(testVXString.getValue());
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test110deleteGroupsByGroupNameListNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue("testVXGroup");
		xUserRest.deleteGroupsByGroupName(request,null);
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test111deleteUsersByUserNameListGetListNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="false";
	
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXStringList vxStringList=createVXStringList();
		vxStringList.setVXStrings(null);
		xUserRest.deleteGroupsByGroupName(request,vxStringList);
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	@Test
	public void test112deleteUsersByUserNameNull() {
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		String TestforceDeleteStr="true";
		
		Mockito.when(request.getParameter("forceDelete")).thenReturn(TestforceDeleteStr);
		VXString testVXString= new VXString();
		testVXString.setValue(null);
		
		VXStringList vxStringList=createVXStringListGroup();
		List<VXString> testVXStrings=new ArrayList<VXString>();
		testVXStrings.add(testVXString);
		vxStringList.setVXStrings(testVXStrings);
		xUserRest.deleteGroupsByGroupName(request,vxStringList);
		Mockito.verify(request).getParameter("forceDelete");
		
	}
	
	@SuppressWarnings({ "unchecked", "static-access" })
	@Test
	public void test113ErrorWhenRoleUserIsTryingToFetchAnotherUserDetails() {
	
		destroySession();
		String userLoginID = "testuser";
		Long userId = 8L;
		
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(false);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(userLoginID);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		
		VXUser loggedInUser = createVXUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_USER);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		testSearchCriteria.addParam("name", "admin");
		
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any(), (List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
		
		Mockito.when(searchUtil.extractCommonCriterias(request, xUserService.sortFields)).thenReturn(testSearchCriteria);
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "emailAddress", "Email Address",null)).thenReturn("");
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "userSource", "User Source")).thenReturn(1);
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "isVisible", "User Visibility")).thenReturn(1);
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "status", "User Status")).thenReturn(1);
		Mockito.when(searchUtil.extractStringList(request, testSearchCriteria, "userRoleList", "User Role List", "userRoleList", null,null)).thenReturn(new ArrayList<String>());
		Mockito.when(searchUtil.extractRoleString(request, testSearchCriteria, "userRole", "Role", null)).thenReturn("");
		Mockito.when(xUserService.getXUserByUserName("testuser")).thenReturn(loggedInUser);
		Mockito.when(restErrorUtil.create403RESTException("Logged-In user is not allowed to access requested user data.")).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		
		xUserRest.searchXUsers(request);
	}
	
	@SuppressWarnings({ "unchecked", "static-access" })
	@Test
	public void test114RoleUserWillGetOnlyHisOwnUserDetails() {
	
		destroySession();
		String userLoginID = "testuser";
		Long userId = 8L;
		
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(false);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(userLoginID);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		
		VXUser loggedInUser = createVXUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_USER);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		
		VXUserList expecteUserList = new VXUserList();
		VXUser expectedUser = new VXUser();
		expectedUser.setId(8L);
		expectedUser.setName("testuser");
		List<VXUser> userList = new ArrayList<VXUser>();
		userList.add(expectedUser);
		expecteUserList.setVXUsers(userList);
		
		HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
		SearchCriteria testSearchCriteria=createsearchCriteria();
		
		Mockito.when(searchUtil.extractCommonCriterias((HttpServletRequest)Mockito.any(), (List<SortField>)Mockito.any())).thenReturn(testSearchCriteria);
		
		Mockito.when(searchUtil.extractCommonCriterias(request, xUserService.sortFields)).thenReturn(testSearchCriteria);
		Mockito.when(searchUtil.extractString(request, testSearchCriteria, "emailAddress", "Email Address",null)).thenReturn("");
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "userSource", "User Source")).thenReturn(1);
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "isVisible", "User Visibility")).thenReturn(1);
		Mockito.when(searchUtil.extractInt(request, testSearchCriteria, "status", "User Status")).thenReturn(1);
		Mockito.when(searchUtil.extractStringList(request, testSearchCriteria, "userRoleList", "User Role List", "userRoleList", null,null)).thenReturn(new ArrayList<String>());
		Mockito.when(searchUtil.extractRoleString(request, testSearchCriteria, "userRole", "Role", null)).thenReturn("");
		Mockito.when(xUserService.getXUserByUserName("testuser")).thenReturn(loggedInUser);
		Mockito.when(xUserMgr.searchXUsers(testSearchCriteria)).thenReturn(expecteUserList);
		VXUserList gotVXUserList=xUserRest.searchXUsers(request);
		
		assertEquals(gotVXUserList.getList().size(), 1);
		assertEquals(gotVXUserList.getList().get(0).getId(), expectedUser.getId());
		assertEquals(gotVXUserList.getList().get(0).getName(), expectedUser.getName());
	}
	
	@After
	public void destroySession() {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(null);
		RangerContextHolder.setSecurityContext(context);
	}
	
	private HashMap<Long, Integer> creategroupVisibilityMap()
	{
		HashMap<Long, Integer> groupVisibilityMap=new HashMap<Long, Integer>();
		groupVisibilityMap.put(id, 1);
		return groupVisibilityMap;
	}
	private SearchCriteria createsearchCriteria(){
		SearchCriteria testsearchCriteria = new SearchCriteria();
		testsearchCriteria.setStartIndex(0);
		testsearchCriteria.setMaxRows(Integer.MAX_VALUE);
		testsearchCriteria.setSortBy("id");
		testsearchCriteria.setSortType("asc");
		testsearchCriteria.setGetCount(true);
		testsearchCriteria.setOwnerId(null);
		testsearchCriteria.setGetChildren(false);
		testsearchCriteria.setDistinct(false);
		
		return testsearchCriteria;
	}
	private VXGroupList createxGroupList() {
		
		VXGroupList testVXGroupList= new VXGroupList();
		VXGroup VXGroup1= createVXGroup();
		List<VXGroup> vXGroups = new ArrayList<VXGroup>();
		vXGroups.add(VXGroup1);
		testVXGroupList.setVXGroups(vXGroups);
		testVXGroupList.setStartIndex(0);
		testVXGroupList.setTotalCount(1);
		
		return testVXGroupList;
	}
	private VXUser createVXUser() {
		VXUser testVXUser= new VXUser();
		Collection<String>c = new ArrayList<String>();
		testVXUser.setId(id);
		testVXUser.setCreateDate(new Date());
		testVXUser.setUpdateDate(new Date());
		testVXUser.setOwner("Admin");
		testVXUser.setUpdatedBy("Admin");
		testVXUser.setName("User1");
		testVXUser.setFirstName("FnameUser1");
		testVXUser.setLastName("LnameUser1");
		testVXUser.setPassword("User1");
		testVXUser.setGroupIdList(null);
		testVXUser.setGroupNameList(null);
		testVXUser.setStatus(1);
		testVXUser.setIsVisible(1);
		testVXUser.setUserSource(0);
		c.add("ROLE_USER");
		testVXUser.setUserRoleList(c);
		
		return testVXUser;
		
	}
	private VXGroupUser createVXGroupUser(){
		VXGroupUser testVXGroupUser= new VXGroupUser();
		testVXGroupUser.setId(id);
		testVXGroupUser.setCreateDate(new Date());
		testVXGroupUser.setUpdateDate(new Date());
		testVXGroupUser.setOwner("Admin");
		testVXGroupUser.setUpdatedBy("Admin");
		testVXGroupUser.setName("finance");
		testVXGroupUser.setParentGroupId(id);
		testVXGroupUser.setUserId(id);
		return testVXGroupUser;
	}
	private VXGroupGroup createVXGroupGroup() {
		VXGroupGroup testVXGroupGroup= new VXGroupGroup();
		testVXGroupGroup.setName("testGroup");
		testVXGroupGroup.setCreateDate(new Date());
		testVXGroupGroup.setUpdateDate(new Date());
		testVXGroupGroup.setUpdatedBy("Admin");
		testVXGroupGroup.setOwner("Admin");
		testVXGroupGroup.setId(id);
		testVXGroupGroup.setParentGroupId(id);
		return testVXGroupGroup;
	}
	private VXPermMap testcreateXPermMap(){
		VXPermMap testVXPermMap= new VXPermMap();
		testVXPermMap.setCreateDate(new Date());
		testVXPermMap.setGroupId(id);
		testVXPermMap.setGroupName("testGroup");
		testVXPermMap.setId(id);
		testVXPermMap.setOwner("Admin");
		testVXPermMap.setPermGroup("testPermGroup");
		testVXPermMap.setPermType(1);
		testVXPermMap.setResourceId(id);
		testVXPermMap.setUpdateDate(new Date());
		testVXPermMap.setUpdatedBy("Admin");
		testVXPermMap.setUserId(id);
		testVXPermMap.setUserName("testUser");
		testVXPermMap.setPermFor(1);
		
		return testVXPermMap;
	}
	private VXAuditMap createVXAuditMapObj() {
		VXAuditMap testVXAuditMap=new VXAuditMap();
		testVXAuditMap.setAuditType(1);
		testVXAuditMap.setCreateDate(new Date());
		testVXAuditMap.setGroupId(id);
		testVXAuditMap.setId(id);
		testVXAuditMap.setResourceId(id);
		testVXAuditMap.setUpdateDate(new Date());
		testVXAuditMap.setOwner("Admin");
		testVXAuditMap.setUpdatedBy("Admin");
		testVXAuditMap.setUserId(id);
		return testVXAuditMap;
	}
	private VXResource createVXResource(){
		VXResource testVXResource= new VXResource();
		testVXResource.setAssetId(id);
		testVXResource.setAssetName("AdminAsset");
		testVXResource.setAssetType(1);
		testVXResource.setCreateDate(new Date());
		testVXResource.setOwner("Admin");
		testVXResource.setUpdateDate(new Date());
		testVXResource.setUpdatedBy("Admin");
		testVXResource.setParentId(id);
		testVXResource.setName("User");
		
		return testVXResource;
	}
	private VXGroup createVXGroup() {
		VXGroup testVXGroup= new VXGroup();
		testVXGroup.setName("testVXGroup");
		testVXGroup.setCreateDate(new Date());
		testVXGroup.setUpdateDate(new Date());
		testVXGroup.setUpdatedBy("Admin");
		testVXGroup.setOwner("Admin");
		testVXGroup.setId(id);
		testVXGroup.setGroupType(1);
		testVXGroup.setCredStoreId(1L);
		testVXGroup.setGroupSource(1);
		testVXGroup.setIsVisible(1);
		return testVXGroup;
	}
	private VXAuthSession createVXAuthSession() {
		VXAuthSession testVXAuthSession = new VXAuthSession();
		testVXAuthSession.setAuthProvider(1);
		testVXAuthSession.setAuthStatus(1);
		testVXAuthSession.setAuthTime(new Date());
		testVXAuthSession.setCityName("Mumbai");
		testVXAuthSession.setCountryName("India");
		testVXAuthSession.setCreateDate(new Date());
		testVXAuthSession.setDeviceType(1);
		testVXAuthSession.setEmailAddress("email@EXAMPLE.COM");
		testVXAuthSession.setFamilyScreenName("testfamilyScreenName");
		testVXAuthSession.setFirstName("testAuthSessionName");
		testVXAuthSession.setId(id);
		testVXAuthSession.setLoginId("Admin");
		testVXAuthSession.setOwner("Admin");
		testVXAuthSession.setPublicScreenName("Admin");
		testVXAuthSession.setUpdatedBy("Admin");
		testVXAuthSession.setUpdateDate(new Date());
		testVXAuthSession.setUserId(id);
		testVXAuthSession.setStateName("Maharashtra");
		return testVXAuthSession;
	}
	private VXUserPermission createVXUserPermission() {
		
		VXUserPermission testVXUserPermission= new VXUserPermission();
		
		testVXUserPermission.setCreateDate(new Date());
		testVXUserPermission.setId(id);
		testVXUserPermission.setIsAllowed(1);
		testVXUserPermission.setModuleId(id);
		testVXUserPermission.setModuleName("testModule");
		testVXUserPermission.setOwner("Admin");
		testVXUserPermission.setUpdateDate(new Date());
		testVXUserPermission.setUpdatedBy("Admin");
		testVXUserPermission.setUserId(id);
		testVXUserPermission.setUserName("testVXUser");
		
		return testVXUserPermission;
		
	}
	private VXGroupPermission createVXGroupPermission() {
		VXGroupPermission testVXGroupPermission = new VXGroupPermission();
		
		testVXGroupPermission.setCreateDate(new Date());
		testVXGroupPermission.setGroupId(id);
		testVXGroupPermission.setGroupName("testVXGroup");
		testVXGroupPermission.setId(id);
		testVXGroupPermission.setIsAllowed(1);
		testVXGroupPermission.setModuleId(id);
		testVXGroupPermission.setModuleName("testModule");
		testVXGroupPermission.setOwner("Admin");
		testVXGroupPermission.setUpdateDate(new Date());
		testVXGroupPermission.setUpdatedBy("Admin");
		
		return testVXGroupPermission;
		
	}
	private VXModuleDef createVXModuleDef() {
		VXModuleDef testVXModuleDef= new VXModuleDef();
		testVXModuleDef.setAddedById(id);
		testVXModuleDef.setCreateDate(new Date());
		testVXModuleDef.setCreateTime(new Date());
		
		VXGroupPermission testVXGroupPermission= createVXGroupPermission();
		List<VXGroupPermission>  groupPermList= new ArrayList<VXGroupPermission>();
		groupPermList.add(testVXGroupPermission);
		testVXModuleDef.setGroupPermList(groupPermList);
		
		testVXModuleDef.setId(id);
		testVXModuleDef.setModule("testModule");
		testVXModuleDef.setOwner("Admin");
		testVXModuleDef.setUpdateDate(new Date());
		testVXModuleDef.setUpdatedBy("Admin");
		testVXModuleDef.setUpdatedById(id);
		testVXModuleDef.setUpdateTime(new Date());
		testVXModuleDef.setUrl("testUrrl");
		
		List< VXUserPermission> userPermList= new ArrayList<VXUserPermission>();
		VXUserPermission testVXUserPermission= createVXUserPermission();
		userPermList.add(testVXUserPermission);
		testVXModuleDef.setUserPermList(userPermList);
		
		return testVXModuleDef;
	}
	private VXStringList createVXStringList() {
		VXStringList testVXStringList= new VXStringList();
		VXString testVXString= new VXString();
		testVXString.setValue("User1");
		List<VXString> testVXStrings=new ArrayList<VXString>();
		
		testVXStrings.add(testVXString);
		
		testVXStringList.setVXStrings(testVXStrings);
		testVXStringList.setResultSize(1);
		testVXStringList.setPageSize(1);
		testVXStringList.setSortBy("Id");
		testVXStringList.setStartIndex(1);
		testVXStringList.setTotalCount(1);
		return testVXStringList;
	}
	private VXStringList createVXStringListGroup() {
		VXStringList testVXStringList= new VXStringList();
		VXString testVXString= new VXString();
		testVXString.setValue("testVXGroup");
		List<VXString> testVXStrings=new ArrayList<VXString>();
		
		testVXStrings.add(testVXString);
		
		testVXStringList.setVXStrings(testVXStrings);
		testVXStringList.setResultSize(1);
		testVXStringList.setPageSize(1);
		testVXStringList.setSortBy("Id");
		testVXStringList.setStartIndex(1);
		testVXStringList.setTotalCount(1);
		return testVXStringList;
	}
}
