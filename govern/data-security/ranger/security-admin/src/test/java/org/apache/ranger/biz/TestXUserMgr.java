/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ranger.biz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.common.db.RangerTransactionSynchronizationAdapter;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXAuditMapDao;
import org.apache.ranger.db.XXAuthSessionDao;
import org.apache.ranger.db.XXGlobalStateDao;
import org.apache.ranger.db.XXGroupDao;
import org.apache.ranger.db.XXGroupGroupDao;
import org.apache.ranger.db.XXGroupPermissionDao;
import org.apache.ranger.db.XXGroupUserDao;
import org.apache.ranger.db.XXModuleDefDao;
import org.apache.ranger.db.XXPermMapDao;
import org.apache.ranger.db.XXPolicyDao;
import org.apache.ranger.db.XXPortalUserDao;
import org.apache.ranger.db.XXPortalUserRoleDao;
import org.apache.ranger.db.XXResourceDao;
import org.apache.ranger.db.XXRoleDao;
import org.apache.ranger.db.XXRoleRefGroupDao;
import org.apache.ranger.db.XXRoleRefUserDao;
import org.apache.ranger.db.XXSecurityZoneDao;
import org.apache.ranger.db.XXSecurityZoneRefGroupDao;
import org.apache.ranger.db.XXSecurityZoneRefUserDao;
import org.apache.ranger.db.XXUserDao;
import org.apache.ranger.db.XXUserPermissionDao;
import org.apache.ranger.entity.XXAuditMap;
import org.apache.ranger.entity.XXAuthSession;
import org.apache.ranger.entity.XXGroup;
import org.apache.ranger.entity.XXGroupGroup;
import org.apache.ranger.entity.XXGroupPermission;
import org.apache.ranger.entity.XXGroupUser;
import org.apache.ranger.entity.XXModuleDef;
import org.apache.ranger.entity.XXPermMap;
import org.apache.ranger.entity.XXPolicy;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXPortalUserRole;
import org.apache.ranger.entity.XXResource;
import org.apache.ranger.entity.XXRole;
import org.apache.ranger.entity.XXRoleRefGroup;
import org.apache.ranger.entity.XXRoleRefUser;
import org.apache.ranger.entity.XXSecurityZone;
import org.apache.ranger.entity.XXSecurityZoneRefGroup;
import org.apache.ranger.entity.XXSecurityZoneRefUser;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.entity.XXUserPermission;
import org.apache.ranger.plugin.model.RangerPolicy;
import org.apache.ranger.plugin.model.RangerSecurityZone;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItem;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemAccess;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyItemCondition;
import org.apache.ranger.plugin.model.RangerPolicy.RangerPolicyResource;
import org.apache.ranger.plugin.model.UserInfo;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.apache.ranger.service.RangerPolicyService;
import org.apache.ranger.service.XAuditMapService;
import org.apache.ranger.service.XGroupGroupService;
import org.apache.ranger.service.XGroupPermissionService;
import org.apache.ranger.service.XGroupService;
import org.apache.ranger.service.XGroupUserService;
import org.apache.ranger.service.XModuleDefService;
import org.apache.ranger.service.XPermMapService;
import org.apache.ranger.service.XPortalUserService;
import org.apache.ranger.service.XResourceService;
import org.apache.ranger.service.XUgsyncAuditInfoService;
import org.apache.ranger.service.XUserPermissionService;
import org.apache.ranger.service.XUserService;
import org.apache.ranger.ugsyncutil.model.GroupUserInfo;
import org.apache.ranger.ugsyncutil.model.UsersGroupRoleAssignments;
import org.apache.ranger.view.VXAuditMap;
import org.apache.ranger.view.VXAuditMapList;
import org.apache.ranger.view.VXGroup;
import org.apache.ranger.view.VXGroupGroup;
import org.apache.ranger.view.VXGroupList;
import org.apache.ranger.view.VXGroupPermission;
import org.apache.ranger.view.VXGroupUser;
import org.apache.ranger.view.VXGroupUserInfo;
import org.apache.ranger.view.VXGroupUserList;
import org.apache.ranger.view.VXLong;
import org.apache.ranger.view.VXModuleDef;
import org.apache.ranger.view.VXPermMap;
import org.apache.ranger.view.VXPermMapList;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXResource;
import org.apache.ranger.view.VXResponse;
import org.apache.ranger.view.VXString;
import org.apache.ranger.view.VXStringList;
import org.apache.ranger.view.VXUgsyncAuditInfo;
import org.apache.ranger.view.VXUser;
import org.apache.ranger.view.VXUserGroupInfo;
import org.apache.ranger.view.VXUserList;
import org.apache.ranger.view.VXUserPermission;
import org.junit.After;
import org.junit.Assert;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.PlatformTransactionManager;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestXUserMgr {

	private static Long userId = 8L;
	private static String adminLoginID = "admin";
	private static String keyadminLoginID = "keyadmin";
	private static String userLoginID = "testuser";
	private static String groupName = "public";
	private static final String RANGER_USER_GROUP_GLOBAL_STATE_NAME = "RangerUserStore";

	private static Integer emptyValue;

	@InjectMocks
	XUserMgr xUserMgr = new XUserMgr();

	@Mock
	XGroupService xGroupService;

	@Mock
	RangerDaoManager daoManager;

	@Mock
	RESTErrorUtil restErrorUtil;

	@Mock
	XGroupUserService xGroupUserService;

	@Mock
	StringUtil stringUtil;

	@Mock
	RangerBizUtil msBizUtil;

	@Mock
	UserMgr userMgr;

	@Mock
	RangerBizUtil xaBizUtil;

	@Mock
	XUserService xUserService;

	@Mock
	XModuleDefService xModuleDefService;

	@Mock
	XUserPermissionService xUserPermissionService;

	@Mock
	XGroupPermissionService xGroupPermissionService;

	@Mock
	ContextUtil contextUtil;

	@Mock
	RangerSecurityContext rangerSecurityContext;

	@Mock
	XPortalUserService xPortalUserService;

	@Mock
	SessionMgr sessionMgr;

	@Mock
	XPermMapService xPermMapService;

	@Mock
	XAuditMapService xAuditMapService;

	@Mock
	RangerPolicyService policyService;

	@Mock
	ServiceDBStore svcStore;

	@Mock
	XGroupGroupService xGroupGroupService;

	@Mock
	XResourceService xResourceService;

	@Mock
	XUgsyncAuditInfoService xUgsyncAuditInfoService;

	@Mock
	RangerTransactionSynchronizationAdapter transactionSynchronizationAdapter;

	@Mock
	XXGlobalStateDao xxGlobalStateDao;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	@Qualifier(value = "transactionManager")
	PlatformTransactionManager txManager;

	public void setup() {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(true);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(adminLoginID);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		Mockito.when(daoManager.getXXGlobalState()).thenReturn(xxGlobalStateDao);
	}

	@After
	public void destroySession() {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(null);
		RangerContextHolder.setSecurityContext(context);
	}

	private VXUser vxUser() {
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		Collection<String> groupNameList = new ArrayList<String>();
		groupNameList.add(groupName);
		VXUser vxUser = new VXUser();
		vxUser.setId(userId);
		vxUser.setDescription("group test working");
		vxUser.setName(userLoginID);
		vxUser.setUserRoleList(userRoleList);
		vxUser.setGroupNameList(groupNameList);
		vxUser.setPassword("Usertest123");
		vxUser.setEmailAddress("test@test.com");
		return vxUser;
	}

	private XXUser xxUser(VXUser vxUser) {
		XXUser xXUser = new XXUser();
		xXUser.setId(userId);
		xXUser.setName(vxUser.getName());
		xXUser.setStatus(vxUser.getStatus());
		xXUser.setIsVisible(vxUser.getIsVisible());
		xXUser.setDescription(vxUser.getDescription());
		return xXUser;
	}

	private VXGroup vxGroup() {
		VXGroup vXGroup = new VXGroup();
		vXGroup.setId(userId);
		vXGroup.setDescription("group test working");
		vXGroup.setName(groupName);
		vXGroup.setIsVisible(1);
		return vXGroup;
	}

	private VXModuleDef vxModuleDef() {
		VXUserPermission userPermission = vxUserPermission();
		List<VXUserPermission> userPermList = new ArrayList<VXUserPermission>();
		userPermList.add(userPermission);
		VXGroupPermission groupPermission = vxGroupPermission();
		List<VXGroupPermission> groupPermList = new ArrayList<VXGroupPermission>();
		groupPermList.add(groupPermission);
		VXModuleDef vxModuleDef = new VXModuleDef();
		vxModuleDef.setAddedById(userId);
		vxModuleDef.setCreateDate(new Date());
		vxModuleDef.setCreateTime(new Date());
		vxModuleDef.setId(userId);
		vxModuleDef.setModule("Policy manager");
		vxModuleDef.setOwner("admin");
		vxModuleDef.setUpdateDate(new Date());
		vxModuleDef.setUpdatedBy("admin");
		vxModuleDef.setUpdatedById(userId);
		vxModuleDef.setUpdateTime(new Date());
		vxModuleDef.setUrl("/policy manager");
		vxModuleDef.setUserPermList(userPermList);
		vxModuleDef.setGroupPermList(groupPermList);
		return vxModuleDef;
	}

	private VXUserPermission vxUserPermission() {
		VXUserPermission userPermission = new VXUserPermission();
		userPermission.setId(1L);
		userPermission.setIsAllowed(1);
		userPermission.setModuleId(1L);
		userPermission.setUserId(userId);
		userPermission.setUserName(userLoginID);
		userPermission.setOwner("admin");
		return userPermission;
	}

	private VXGroupPermission vxGroupPermission() {
		VXGroupPermission groupPermission = new VXGroupPermission();
		groupPermission.setId(1L);
		groupPermission.setIsAllowed(1);
		groupPermission.setModuleId(1L);
		groupPermission.setGroupId(userId);
		groupPermission.setGroupName(groupName);
		groupPermission.setOwner("admin");
		return groupPermission;
	}

	private VXPortalUser userProfile() {
		VXPortalUser userProfile = new VXPortalUser();
		userProfile.setEmailAddress("test@test.com");
		userProfile.setFirstName("user12");
		userProfile.setLastName("test12");
		userProfile.setLoginId(userLoginID);
		userProfile.setPassword("Usertest123");
		userProfile.setUserSource(1);
		userProfile.setPublicScreenName("testuser");
		userProfile.setId(userId);
		return userProfile;
	}

	private XXPortalUser xxPortalUser(VXPortalUser userProfile) {
		XXPortalUser xxPortalUser = new XXPortalUser();
		xxPortalUser.setEmailAddress(userProfile.getEmailAddress());
		xxPortalUser.setFirstName(userProfile.getFirstName());
		xxPortalUser.setLastName(userProfile.getLastName());
		xxPortalUser.setLoginId(userProfile.getLoginId());
		xxPortalUser.setPassword(userProfile.getPassword());
		xxPortalUser.setUserSource(userProfile.getUserSource());
		xxPortalUser.setPublicScreenName(userProfile.getPublicScreenName());
		return xxPortalUser;
	}

	public void setupKeyAdmin() {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		XXPortalUser userKeyAdmin = new XXPortalUser();
		userKeyAdmin.setId(userProfile().getId());
		userKeyAdmin.setLoginId(keyadminLoginID);
		currentUserSession.setXXPortalUser(userKeyAdmin);
		currentUserSession.setKeyAdmin(true);
	}

	private List<XXModuleDef> xxModuleDefs(){
		List<XXModuleDef> xXModuleDefs=new ArrayList<XXModuleDef>();
		XXModuleDef xXModuleDef1=xxModuleDef();
		XXModuleDef xXModuleDef2=xxModuleDef();
		XXModuleDef xXModuleDef3=xxModuleDef();
		XXModuleDef xXModuleDef4=xxModuleDef();
		XXModuleDef xXModuleDef5=xxModuleDef();
		xXModuleDef1.setId(1L);
		xXModuleDef1.setModule("Resource Based Policies");
		xXModuleDef1.setId(2L);
		xXModuleDef1.setModule("Users/Groups");
		xXModuleDef1.setId(3L);
		xXModuleDef1.setModule("Reports");
		xXModuleDef1.setId(4L);
		xXModuleDef1.setModule("Audit");
		xXModuleDef1.setId(5L);
		xXModuleDef1.setModule("Key Manager");
		xXModuleDefs.add(xXModuleDef1);
		xXModuleDefs.add(xXModuleDef2);
		xXModuleDefs.add(xXModuleDef3);
		xXModuleDefs.add(xXModuleDef4);
		xXModuleDefs.add(xXModuleDef5);
		return xXModuleDefs;
	}

	private VXGroupUser vxGroupUser(){
		VXUser vXUser = vxUser();
		VXGroupUser vxGroupUser = new VXGroupUser();
		vxGroupUser.setId(userId);
		vxGroupUser.setName(vXUser.getName());
		vxGroupUser.setOwner("Admin");
		vxGroupUser.setUserId(vXUser.getId());
		vxGroupUser.setUpdatedBy("User");
		vxGroupUser.setParentGroupId(userId);
		return vxGroupUser;
	}

	private VXGroupGroup vxGroupGroup(){
		VXGroupGroup vXGroupGroup = new VXGroupGroup();
		vXGroupGroup.setId(userId);
		vXGroupGroup.setName("group user test");
		vXGroupGroup.setOwner("Admin");
		vXGroupGroup.setUpdatedBy("User");
		return vXGroupGroup;
	}

	private XXGroupGroup xxGroupGroup(){
		XXGroupGroup xXGroupGroup = new XXGroupGroup();
		xXGroupGroup.setId(userId);
		xXGroupGroup.setName("group user test");
		return xXGroupGroup;
	}

	private XXPolicy getXXPolicy() {
		XXPolicy xxPolicy = new XXPolicy();
		xxPolicy.setId(userId);
		xxPolicy.setName("HDFS_1-1-20150316062453");
		xxPolicy.setAddedByUserId(userId);
		xxPolicy.setCreateTime(new Date());
		xxPolicy.setDescription("test");
		xxPolicy.setIsAuditEnabled(false);
		xxPolicy.setIsEnabled(false);
		xxPolicy.setService(1L);
		xxPolicy.setUpdatedByUserId(userId);
		xxPolicy.setUpdateTime(new Date());
		return xxPolicy;
	}

	private VXGroupUserList vxGroupUserList(){
		VXGroupUserList vxGroupUserList = new VXGroupUserList();
		List<VXGroupUser> vXGroupUsers =new ArrayList<VXGroupUser>();
		VXGroupUser vxGroupUser = vxGroupUser();
		vXGroupUsers.add(vxGroupUser);
		vxGroupUserList.setVXGroupUsers(vXGroupUsers);
		return vxGroupUserList;
	}

	private ArrayList<String> getRoleList() {
		ArrayList<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		return userRoleList;
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

	private XXUserPermission xxUserPermission(){
		XXUserPermission xUserPermissionObj = new XXUserPermission();
		xUserPermissionObj.setAddedByUserId(userId);
		xUserPermissionObj.setCreateTime(new Date());
		xUserPermissionObj.setId(userId);
		xUserPermissionObj.setIsAllowed(1);
		xUserPermissionObj.setModuleId(1L);
		xUserPermissionObj.setUpdatedByUserId(userId);
		xUserPermissionObj.setUpdateTime(new Date());
		xUserPermissionObj.setUserId(userId);
		return xUserPermissionObj;
	}

	private XXGroupPermission xxGroupPermission(){
		XXGroupPermission xGroupPermissionObj = new XXGroupPermission();
		xGroupPermissionObj.setAddedByUserId(userId);
		xGroupPermissionObj.setCreateTime(new Date());
		xGroupPermissionObj.setId(userId);
		xGroupPermissionObj.setIsAllowed(1);
		xGroupPermissionObj.setModuleId(1L);
		xGroupPermissionObj.setUpdatedByUserId(userId);
		xGroupPermissionObj.setUpdateTime(new Date());
		xGroupPermissionObj.setGroupId(userId);
		return xGroupPermissionObj;
	}

	private XXModuleDef xxModuleDef(){
		XXModuleDef xModuleDef = new XXModuleDef();
		xModuleDef.setUpdatedByUserId(userId);
		xModuleDef.setAddedByUserId(userId);
		xModuleDef.setCreateTime(new Date());
		xModuleDef.setId(userId);
		xModuleDef.setModule("Policy manager");
		xModuleDef.setUpdateTime(new Date());
		xModuleDef.setUrl("/policy manager");
		return xModuleDef;
	}

	private VXPermMap getVXPermMap(){
		VXPermMap testVXPermMap= new VXPermMap();
		testVXPermMap.setCreateDate(new Date());
		testVXPermMap.setGroupId(userId);
		testVXPermMap.setGroupName("testGroup");
		testVXPermMap.setId(userId);
		testVXPermMap.setOwner("Admin");
		testVXPermMap.setPermGroup("testPermGroup");
		testVXPermMap.setPermType(1);
		testVXPermMap.setResourceId(userId);
		testVXPermMap.setUpdateDate(new Date());
		testVXPermMap.setUpdatedBy("Admin");
		testVXPermMap.setUserId(userId);
		testVXPermMap.setUserName("testUser");
		testVXPermMap.setPermFor(1);
		return testVXPermMap;
	}

	private VXAuditMap getVXAuditMap() {
		VXAuditMap testVXAuditMap=new VXAuditMap();
		testVXAuditMap.setAuditType(1);
		testVXAuditMap.setCreateDate(new Date());
		testVXAuditMap.setGroupId(userId);
		testVXAuditMap.setId(userId);
		testVXAuditMap.setResourceId(userId);
		testVXAuditMap.setUpdateDate(new Date());
		testVXAuditMap.setOwner("Admin");
		testVXAuditMap.setUpdatedBy("Admin");
		testVXAuditMap.setUserId(userId);
		return testVXAuditMap;
	}

	private RangerPolicy rangerPolicy() {
		List<RangerPolicyItemAccess> accesses = new ArrayList<RangerPolicyItemAccess>();
		List<String> users = new ArrayList<String>();
		List<String> groups = new ArrayList<String>();
		List<String> policyLabels = new ArrayList<String>();
		List<RangerPolicyItemCondition> conditions = new ArrayList<RangerPolicyItemCondition>();
		List<RangerPolicyItem> policyItems = new ArrayList<RangerPolicyItem>();
		RangerPolicyItem rangerPolicyItem = new RangerPolicyItem();
		rangerPolicyItem.setAccesses(accesses);
		rangerPolicyItem.setConditions(conditions);
		rangerPolicyItem.setGroups(groups);
		rangerPolicyItem.setUsers(users);
		rangerPolicyItem.setDelegateAdmin(false);

		policyItems.add(rangerPolicyItem);

		Map<String, RangerPolicyResource> policyResource = new HashMap<String, RangerPolicyResource>();
		RangerPolicyResource rangerPolicyResource = new RangerPolicyResource();
		rangerPolicyResource.setIsExcludes(true);
		rangerPolicyResource.setIsRecursive(true);
		rangerPolicyResource.setValue("1");
		rangerPolicyResource.setValues(users);
		RangerPolicy policy = new RangerPolicy();
		policy.setId(userId);
		policy.setCreateTime(new Date());
		policy.setDescription("policy");
		policy.setGuid("policyguid");
		policy.setIsEnabled(true);
		policy.setName("HDFS_1-1-20150316062453");
		policy.setUpdatedBy("Admin");
		policy.setUpdateTime(new Date());
		policy.setService("HDFS_1-1-20150316062453");
		policy.setIsAuditEnabled(true);
		policy.setPolicyItems(policyItems);
		policy.setResources(policyResource);
		policy.setPolicyLabels(policyLabels);
		return policy;
	}

	public void setupUser() {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile().getId());
		user.setLoginId(userProfile().getLoginId());
		currentUserSession.setXXPortalUser(user);
		currentUserSession.setUserRoleList(getRoleList());
	}

	@Test
	public void test01CreateXUser() {
		setup();
		VXUser vxUser = vxUser();
		vxUser.setFirstName("user12");
		vxUser.setLastName("test12");
		Collection<Long> groupIdList = new ArrayList<Long>();
		groupIdList.add(userId);
		vxUser.setGroupIdList(groupIdList);
		VXGroup vxGroup = vxGroup();
		vxGroup.setName("user12Grp");
		VXGroupUser vXGroupUser = new VXGroupUser();
		vXGroupUser.setParentGroupId(userId);
		vXGroupUser.setUserId(userId);
		vXGroupUser.setName(vxGroup.getName());
		Mockito.when(xGroupService.readResource(userId)).thenReturn(vxGroup);
		Mockito.when(xGroupUserService.createResource((VXGroupUser) Mockito.any())).thenReturn(vXGroupUser);
		ArrayList<String> userRoleListVXPortaUser = getRoleList();
		VXPortalUser vXPortalUser = new VXPortalUser();
		vXPortalUser.setUserRoleList(userRoleListVXPortaUser);
		Mockito.when(xUserService.createResource(vxUser)).thenReturn(vxUser);
		XXModuleDefDao value = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(value);
		Mockito.when(userMgr.createDefaultAccountUser((VXPortalUser) Mockito.any())).thenReturn(vXPortalUser);
    Mockito.when(stringUtil.validateEmail("test@test.com")).thenReturn(true);
		VXUser dbUser = xUserMgr.createXUser(vxUser);
		Assert.assertNotNull(dbUser);
		userId = dbUser.getId();
		Assert.assertEquals(userId, dbUser.getId());
		Assert.assertEquals(dbUser.getDescription(), vxUser.getDescription());
		Assert.assertEquals(dbUser.getName(), vxUser.getName());
		Assert.assertEquals(dbUser.getUserRoleList(), vxUser.getUserRoleList());
		Assert.assertEquals(dbUser.getGroupNameList(),
		vxUser.getGroupNameList());
		Mockito.verify(xUserService).createResource(vxUser);
		Mockito.when(xUserService.readResourceWithOutLogin(userId)).thenReturn(vxUser);
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_ADMIN);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		Mockito.when(xUserService.getXUserByUserName("admin")).thenReturn(loggedInUser);
		
		VXUser dbvxUser = xUserMgr.getXUser(userId);
		Mockito.verify(userMgr).createDefaultAccountUser((VXPortalUser) Mockito.any());
		Assert.assertNotNull(dbvxUser);
		Assert.assertEquals(userId, dbvxUser.getId());
		Assert.assertEquals(dbvxUser.getDescription(), vxUser.getDescription());
		Assert.assertEquals(dbvxUser.getName(), vxUser.getName());
		Assert.assertEquals(dbvxUser.getUserRoleList(),vxUser.getUserRoleList());
		Assert.assertEquals(dbvxUser.getGroupIdList(),vxUser.getGroupIdList());
		Assert.assertEquals(dbvxUser.getGroupNameList(),vxUser.getGroupNameList());
		Mockito.verify(xUserService).readResourceWithOutLogin(userId);
	}

	@Test
	public void test02CreateXUser_WithBlankName() {
		setup();
		VXUser vxUser = vxUser();
		ArrayList<String> userRoleListVXPortaUser = getRoleList();
		VXPortalUser vXPortalUser = new VXPortalUser();
		vXPortalUser.setUserRoleList(userRoleListVXPortaUser);
		vxUser.setName(null);
		Mockito.when(restErrorUtil.createRESTException("Please provide a valid username.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.createXUser(vxUser);
	}

	@Test
	public void test03CreateXUser_WithBlankName() {
		destroySession();
		setup();
		VXUser vxUser = vxUser();
		ArrayList<String> userRoleListVXPortaUser = getRoleList();
		VXPortalUser vXPortalUser = new VXPortalUser();
		vXPortalUser.setUserRoleList(userRoleListVXPortaUser);
		vxUser.setName("");
		Mockito.when(restErrorUtil.createRESTException("Please provide a valid username.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.createXUser(vxUser);
	}

	@Test
	public void testCreateXUser_WithBlankFirstName() {
		destroySession();
		setup();
		VXUser vxUser = vxUser();
		vxUser.setName("test");
		vxUser.setFirstName(null);
		Mockito.when(restErrorUtil.createRESTException("Please provide a valid first name.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.createXUser(vxUser);
	}

	@Test
	public void test04CreateXUser_WithBlankValues() {
		destroySession();
		setup();
		VXUser vxUser = vxUser();
		vxUser.setDescription(null);
		vxUser.setFirstName("test");
		vxUser.setLastName("null");
		Mockito.when(restErrorUtil.createRESTException("Please provide valid email address.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.createXUser(vxUser);
	}

	@Test
	public void testUpdateXUser_WithBlankFirstName() {
		setup();
		VXUser vxUser = vxUser();
		ArrayList<String> userRoleListVXPortaUser = getRoleList();
		VXPortalUser vXPortalUser = new VXPortalUser();
		vXPortalUser.setUserRoleList(userRoleListVXPortaUser);
		vxUser.setDescription(null);
		vxUser.setFirstName("null");
		Mockito.when(restErrorUtil.createRESTException("Please provide a valid first name.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.updateXUser(vxUser);
	}

	@Test
	public void testUpdateXUser_WithBlankUserName() {
		setup();
		VXUser vxUser = vxUser();
		ArrayList<String> userRoleListVXPortaUser = getRoleList();
		VXPortalUser vXPortalUser = new VXPortalUser();
		vXPortalUser.setUserRoleList(userRoleListVXPortaUser);
		vxUser.setDescription(null);
		vxUser.setName("null");
		Mockito.when(restErrorUtil.createRESTException("Please provide a valid username.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.updateXUser(vxUser);
	}

	@Test
	public void test05UpdateXUser() {
		setup();
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		Collection<String> existingRoleList = new ArrayList<String>();
		existingRoleList.add(RangerConstants.ROLE_USER);
		Collection<String> reqRoleList = new ArrayList<String>();
		reqRoleList.add(RangerConstants.ROLE_SYS_ADMIN);
		Collection<Long> groupIdList = new ArrayList<Long>();
		groupIdList.add(userId);
		VXUser vxUser = vxUser();
		vxUser.setUserRoleList(reqRoleList);
		vxUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		vxUser.setGroupIdList(groupIdList);
		vxUser.setFirstName("user12");
		vxUser.setLastName("test12");
		Mockito.when(xUserService.updateResource(vxUser)).thenReturn(vxUser);
		XXModuleDefDao xModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		VXPortalUser vXPortalUser = userProfile();
		vXPortalUser.setUserRoleList(existingRoleList);
		Mockito.when(userMgr.getUserProfileByLoginId(vxUser.getName())).thenReturn(vXPortalUser);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = new XXUserPermission();
		xUserPermissionObj.setAddedByUserId(userId);
		xUserPermissionObj.setCreateTime(new Date());
		xUserPermissionObj.setId(userId);
		xUserPermissionObj.setIsAllowed(1);
		xUserPermissionObj.setModuleId(1L);
		xUserPermissionObj.setUpdatedByUserId(userId);
		xUserPermissionObj.setUpdateTime(new Date());
		xUserPermissionObj.setUserId(userId);
		xUserPermissionsList.add(xUserPermissionObj);
		List<XXModuleDef> xXModuleDefs = xxModuleDefs();
		Mockito.when(xUserPermissionDao.findByUserPermissionId(vXPortalUser.getId())).thenReturn(xUserPermissionsList);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		Mockito.when(xModuleDefDao.getAll()).thenReturn(xXModuleDefs);
		XXUser xXUser = xxUser(vxUser);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByPortalUserId(vXPortalUser.getId())).thenReturn(xXUser);
		VXGroupUserList vxGroupUserList = vxGroupUserList();
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		VXUserPermission vXUserPermission = vxUserPermission();
		Mockito.when(xUserPermissionService.createResource((VXUserPermission) Mockito.any())).thenReturn(vXUserPermission);
		Mockito.when(sessionMgr.getActiveUserSessionsForPortalUserId(userId)).thenReturn(userSessions);
		VXUser dbvxUser = xUserMgr.updateXUser(vxUser);
		Assert.assertNotNull(dbvxUser);
		Assert.assertEquals(dbvxUser.getId(), vxUser.getId());
		Assert.assertEquals(dbvxUser.getDescription(), vxUser.getDescription());
		Assert.assertEquals(dbvxUser.getName(), vxUser.getName());
		Mockito.verify(xUserService).updateResource(vxUser);
		groupIdList.clear();
		groupIdList.add(9L);
		vxUser.setGroupIdList(groupIdList);
		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		Mockito.when(xGroupUserService.getTransactionLog((VXGroupUser) Mockito.any(), Mockito.anyString())).thenReturn(trxLogList);
		VXGroup vXGroup = vxGroup();
		Mockito.when(xGroupService.readResource(Mockito.anyLong())).thenReturn(vXGroup);
		VXGroupUser vXGroupUser = vxGroupUser();
		Mockito.when(xGroupUserService.createResource((VXGroupUser) Mockito.any())).thenReturn(vXGroupUser);
		dbvxUser = xUserMgr.updateXUser(vxUser);
		Assert.assertNotNull(dbvxUser);
	}

	@Test
	public void test06ModifyUserVisibilitySetOne() {
		setup();
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		XXUser xxUser = Mockito.mock(XXUser.class);
		VXUser vxUser = vxUser();
		Mockito.when(xUserService.updateResource(vxUser)).thenReturn(vxUser);
		HashMap<Long, Integer> visibilityMap = new HashMap<Long, Integer>();
		Integer value = 1;
		visibilityMap.put(userId, value);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.getById(userId)).thenReturn(xxUser);
		Mockito.when(xUserService.populateViewBean(xxUser)).thenReturn(vxUser);
		xUserMgr.modifyUserVisibility(visibilityMap);
		Assert.assertEquals(value, vxUser.getIsVisible());
		Assert.assertEquals(userId, vxUser.getId());
		Mockito.verify(xUserService).updateResource(vxUser);
		Mockito.verify(daoManager).getXXUser();
		Mockito.verify(xUserService).populateViewBean(xxUser);
	}

	@Test
	public void test07ModifyUserVisibilitySetZero() {
		setup();
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		XXUser xxUser = Mockito.mock(XXUser.class);
		VXUser vxUser = vxUser();
		Mockito.when(xUserService.updateResource(vxUser)).thenReturn(vxUser);
		HashMap<Long, Integer> visibilityMap = new HashMap<Long, Integer>();
		Integer value = 0;
		visibilityMap.put(userId, value);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.getById(userId)).thenReturn(xxUser);
		Mockito.when(xUserService.populateViewBean(xxUser)).thenReturn(vxUser);
		xUserMgr.modifyUserVisibility(visibilityMap);
		Assert.assertEquals(value, vxUser.getIsVisible());
		Assert.assertEquals(userId, vxUser.getId());
		Mockito.verify(xUserService).updateResource(vxUser);
		Mockito.verify(daoManager).getXXUser();
		Mockito.verify(xUserService).populateViewBean(xxUser);
	}

	@Test
	public void test08ModifyUserVisibilitySetEmpty() {
		setup();
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		XXUser xxUser = Mockito.mock(XXUser.class);
		VXUser vxUser = vxUser();
		Mockito.when(xUserService.updateResource(vxUser)).thenReturn(vxUser);
		HashMap<Long, Integer> visibilityMap = new HashMap<Long, Integer>();
		visibilityMap.put(userId, emptyValue);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.getById(userId)).thenReturn(xxUser);
		Mockito.when(xUserService.populateViewBean(xxUser)).thenReturn(vxUser);
		xUserMgr.modifyUserVisibility(visibilityMap);
		Assert.assertEquals(emptyValue, vxUser.getIsVisible());
		Assert.assertEquals(userId, vxUser.getId());
		Mockito.verify(xUserService).updateResource(vxUser);
		Mockito.verify(daoManager).getXXUser();
		Mockito.verify(xUserService).populateViewBean(xxUser);
	}

	@Test
	public void test09CreateXGroup() {
		setup();
		VXGroup vXGroup = vxGroup();
		vXGroup.setDescription(null);
		Mockito.when(xGroupService.createResource(vXGroup)).thenReturn(vXGroup);
		VXGroup dbXGroup = xUserMgr.createXGroup(vXGroup);
		Assert.assertNotNull(dbXGroup);
		userId = dbXGroup.getId();
		Assert.assertEquals(userId, dbXGroup.getId());
		Assert.assertEquals(vXGroup.getName(), dbXGroup.getName());
		Mockito.verify(xGroupService).createResource(vXGroup);
		Mockito.when(xGroupService.readResourceWithOutLogin(userId)).thenReturn(vXGroup);
		VXGroup dbxGroup = xUserMgr.getXGroup(userId);
		Assert.assertNotNull(dbXGroup);
		Assert.assertEquals(userId, dbxGroup.getId());
		Assert.assertEquals(dbXGroup.getName(), dbxGroup.getName());
		Mockito.verify(xGroupService).readResourceWithOutLogin(userId);
	}

	@Test
	public void test10UpdateXGroup() {
		XXGroupDao xxGroupDao = Mockito.mock(XXGroupDao.class);
		XXGroupUserDao xxGroupUserDao = Mockito.mock(XXGroupUserDao.class);
		List<XXGroupUser> grpUsers =new ArrayList<XXGroupUser>();
		setup();
		VXGroup vXGroup = vxGroup();
		XXGroup xxGroup = new XXGroup();
		xxGroup.setName(groupName);
		Mockito.when(daoManager.getXXGroup()).thenReturn(xxGroupDao);
		Mockito.when(xxGroupDao.getById(vXGroup.getId())).thenReturn(xxGroup);
		Mockito.when(xGroupService.updateResource(vXGroup)).thenReturn(vXGroup);
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(xxGroupUserDao);
		Mockito.when(xxGroupUserDao.findByGroupId(vXGroup.getId())).thenReturn(grpUsers);
		VXGroup dbvxGroup = xUserMgr.updateXGroup(vXGroup);
		Assert.assertNotNull(dbvxGroup);
		userId = dbvxGroup.getId();
		Assert.assertEquals(userId, dbvxGroup.getId());
		Assert.assertEquals(vXGroup.getDescription(),dbvxGroup.getDescription());
		Assert.assertEquals(vXGroup.getName(), dbvxGroup.getName());
		Mockito.verify(daoManager).getXXGroup();
		Mockito.verify(daoManager).getXXGroupUser();
		Mockito.verify(xGroupService).updateResource(vXGroup);
		Mockito.verify(xxGroupUserDao).findByGroupId(vXGroup.getId());
		Mockito.when(restErrorUtil.createRESTException("group name updates are not allowed.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		vXGroup.setName("UnknownGroup");
		xUserMgr.updateXGroup(vXGroup);
	}

	@Test
	public void test11ModifyGroupsVisibilitySetOne() {
		setup();
		XXGroupDao xxGroupDao = Mockito.mock(XXGroupDao.class);
		VXGroup vXGroup = vxGroup();
		XXGroup xxGroup = new XXGroup();
		HashMap<Long, Integer> groupVisibilityMap = new HashMap<Long, Integer>();
		Integer value = 1;
		groupVisibilityMap.put(userId, value);
		Mockito.when(daoManager.getXXGroup()).thenReturn(xxGroupDao);
		Mockito.when(xxGroupDao.getById(vXGroup.getId())).thenReturn(xxGroup);
		Mockito.when(xGroupService.populateViewBean(xxGroup)).thenReturn(vXGroup);
		Mockito.when(xGroupService.updateResource(vXGroup)).thenReturn(vXGroup);
		xUserMgr.modifyGroupsVisibility(groupVisibilityMap);
		Assert.assertEquals(value, vXGroup.getIsVisible());
		Assert.assertEquals(userId, vXGroup.getId());
		Mockito.verify(daoManager).getXXGroup();
		Mockito.verify(xGroupService).populateViewBean(xxGroup);
		Mockito.verify(xGroupService).updateResource(vXGroup);
	}

	@Test
	public void test12ModifyGroupsVisibilitySetZero() {
		setup();
		XXGroupDao xxGroupDao = Mockito.mock(XXGroupDao.class);
		VXGroup vXGroup = vxGroup();
		XXGroup xxGroup = new XXGroup();
		HashMap<Long, Integer> groupVisibilityMap = new HashMap<Long, Integer>();
		Integer value = 0;
		groupVisibilityMap.put(userId, value);
		Mockito.when(daoManager.getXXGroup()).thenReturn(xxGroupDao);
		Mockito.when(xxGroupDao.getById(vXGroup.getId())).thenReturn(xxGroup);
		Mockito.when(xGroupService.populateViewBean(xxGroup)).thenReturn(vXGroup);
		Mockito.when(xGroupService.updateResource(vXGroup)).thenReturn(vXGroup);
		xUserMgr.modifyGroupsVisibility(groupVisibilityMap);
		Assert.assertEquals(value, vXGroup.getIsVisible());
		Assert.assertEquals(userId, vXGroup.getId());
		Mockito.verify(daoManager).getXXGroup();
		Mockito.verify(xGroupService).populateViewBean(xxGroup);
		Mockito.verify(xGroupService).updateResource(vXGroup);
	}

	@Test
	public void test13ModifyGroupsVisibilitySetEmpty() {
		setup();
		XXGroupDao xxGroupDao = Mockito.mock(XXGroupDao.class);
		VXGroup vXGroup = vxGroup();
		XXGroup xxGroup = new XXGroup();
		HashMap<Long, Integer> groupVisibilityMap = new HashMap<Long, Integer>();
		groupVisibilityMap.put(userId, emptyValue);
		Mockito.when(daoManager.getXXGroup()).thenReturn(xxGroupDao);
		Mockito.when(xxGroupDao.getById(vXGroup.getId())).thenReturn(xxGroup);
		Mockito.when(xGroupService.populateViewBean(xxGroup)).thenReturn(vXGroup);
		Mockito.when(xGroupService.updateResource(vXGroup)).thenReturn(vXGroup);
		xUserMgr.modifyGroupsVisibility(groupVisibilityMap);
		Assert.assertEquals(emptyValue, vXGroup.getIsVisible());
		Assert.assertEquals(userId, vXGroup.getId());
		Mockito.verify(daoManager).getXXGroup();
		Mockito.verify(xGroupService).populateViewBean(xxGroup);
		Mockito.verify(xGroupService).updateResource(vXGroup);
	}

	@Test
	public void test14createXGroupUser() {
		setup();
		VXGroupUser vxGroupUser = vxGroupUser();
		Mockito.when(xGroupUserService.createXGroupUserWithOutLogin(vxGroupUser)).thenReturn(vxGroupUser);
		VXGroupUser dbVXGroupUser = xUserMgr.createXGroupUser(vxGroupUser);
		Assert.assertNotNull(dbVXGroupUser);
		userId = dbVXGroupUser.getId();
		Assert.assertEquals(userId, dbVXGroupUser.getId());
		Assert.assertEquals(dbVXGroupUser.getOwner(), vxGroupUser.getOwner());
		Assert.assertEquals(dbVXGroupUser.getName(), vxGroupUser.getName());
		Assert.assertEquals(dbVXGroupUser.getUserId(), vxGroupUser.getUserId());
		Assert.assertEquals(dbVXGroupUser.getUpdatedBy(),vxGroupUser.getUpdatedBy());
		Mockito.verify(xGroupUserService).createXGroupUserWithOutLogin(vxGroupUser);
		Mockito.when(xGroupUserService.readResourceWithOutLogin(userId)).thenReturn(vxGroupUser);
		VXGroupUser dbvxGroupUser = xUserMgr.getXGroupUser(userId);
		Assert.assertNotNull(dbvxGroupUser);
		userId = dbvxGroupUser.getId();
		Assert.assertEquals(userId, dbvxGroupUser.getId());
		Assert.assertEquals(dbvxGroupUser.getOwner(), vxGroupUser.getOwner());
		Assert.assertEquals(dbvxGroupUser.getName(), vxGroupUser.getName());
		Assert.assertEquals(dbvxGroupUser.getUserId(), vxGroupUser.getUserId());
		Assert.assertEquals(dbvxGroupUser.getUpdatedBy(),vxGroupUser.getUpdatedBy());
		Mockito.verify(xGroupUserService).readResourceWithOutLogin(userId);
	}

	@Test
	public void test15GetXUserGroups() {
		List<VXGroup> vXGroupList = new ArrayList<VXGroup>();
		final VXGroup vXGroup1 = vxGroup();
		vXGroup1.setName("users");
		vXGroup1.setDescription("users -added for unit testing");
		vXGroupList.add(vXGroup1);
		SearchCriteria testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("xUserId", userId);
		VXGroupUserList vxGroupUserList = vxGroupUserList();
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		VXGroupList dbVXGroupList = xUserMgr.getXUserGroups(userId);
		Assert.assertNotNull(dbVXGroupList);
	}

	@Test
	public void test16GetXGroupUsers() {
		SearchCriteria testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("xGroupId", userId);
		VXGroupUserList vxGroupUserList = vxGroupUserList();
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		Mockito.when(msBizUtil.hasModuleAccess(RangerConstants.MODULE_USER_GROUPS)).thenReturn(true);
		VXUserList dbVXUserList = xUserMgr.getXGroupUsers(testSearchCriteria);
		Assert.assertNotNull(dbVXUserList);
		Mockito.when(msBizUtil.hasModuleAccess(Mockito.anyString())).thenReturn(false);
		Mockito.when(restErrorUtil.createRESTException(HttpServletResponse.SC_FORBIDDEN, "User is not having permissions on the "+RangerConstants.MODULE_USER_GROUPS+" module.", true)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.getXGroupUsers(testSearchCriteria);
	}

	@Test
	public void test17GetXUserByUserName() {
		setupUser();
		VXUser vxUser = vxUser();
		Mockito.when(xUserService.getXUserByUserName(vxUser.getName())).thenReturn(vxUser);
		XXModuleDefDao xxModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xxModuleDefDao);
		VXUser dbVXUser = xUserMgr.getXUserByUserName(vxUser.getName());
		Assert.assertNotNull(dbVXUser);
		userId = dbVXUser.getId();
		Assert.assertEquals(userId, dbVXUser.getId());
		Assert.assertEquals(dbVXUser.getName(), vxUser.getName());
		Assert.assertEquals(dbVXUser.getOwner(), vxUser.getOwner());
		Mockito.verify(xUserService, Mockito.atLeast(2)).getXUserByUserName(vxUser.getName());
	}

	@Test
	public void test18CreateXUserWithOutLogin() {
		setup();
		VXUser vxUser = vxUser();
		Mockito.when(xUserService.createXUserWithOutLogin(vxUser)).thenReturn(vxUser);
		VXUser dbUser = xUserMgr.createXUserWithOutLogin(vxUser);
		Assert.assertNotNull(dbUser);
		userId = dbUser.getId();
		Assert.assertEquals(userId, dbUser.getId());
		Assert.assertEquals(dbUser.getDescription(), vxUser.getDescription());
		Assert.assertEquals(dbUser.getName(), vxUser.getName());
		Assert.assertEquals(dbUser.getUserRoleList(), vxUser.getUserRoleList());
		Assert.assertEquals(dbUser.getGroupNameList(),vxUser.getGroupNameList());
		Mockito.verify(xUserService).createXUserWithOutLogin(vxUser);
	}

	@Test
	public void test19CreateXGroupWithoutLogin() {
		setup();
		VXGroup vXGroup = vxGroup();
		Mockito.when(xGroupService.createXGroupWithOutLogin(vXGroup)).thenReturn(vXGroup);
		VXGroup dbVXGroup = xUserMgr.createXGroupWithoutLogin(vXGroup);
		Assert.assertNotNull(dbVXGroup);
		userId = dbVXGroup.getId();
		Assert.assertEquals(userId, dbVXGroup.getId());
		Assert.assertEquals(vXGroup.getDescription(),dbVXGroup.getDescription());
		Assert.assertEquals(vXGroup.getName(), dbVXGroup.getName());
		Mockito.verify(xGroupService).createXGroupWithOutLogin(vXGroup);
	}

	@Test
	public void test20DeleteXGroup() {
		setup();
		boolean force = true;
		VXGroup vXGroup = vxGroup();
		XXGroupDao xXGroupDao = Mockito.mock(XXGroupDao.class);
		XXUserDao xXUserDao = Mockito.mock(XXUserDao.class);
		VXUser vxUser=vxUser();
		XXUser xXUser = xxUser(vxUser);
		Mockito.when(daoManager.getXXUser()).thenReturn(xXUserDao);
		Mockito.when(xXUserDao.getById(xXUser.getId())).thenReturn(xXUser);
		XXGroup xXGroup = new XXGroup();
		Mockito.when(daoManager.getXXGroup()).thenReturn(xXGroupDao);
		Mockito.when(xXGroupDao.getById(vXGroup.getId())).thenReturn(xXGroup);
		Mockito.when(xGroupService.populateViewBean(xXGroup)).thenReturn(vXGroup);
		VXGroupUserList vxGroupUserList = vxGroupUserList();
		XXGroupUserDao xGroupUserDao = Mockito.mock(XXGroupUserDao.class);
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(xGroupUserDao);
		VXPermMapList vXPermMapList = new VXPermMapList();
		VXPermMap vXPermMap1=getVXPermMap();
		List<VXPermMap> vXPermMaps=new ArrayList<VXPermMap>();
		vXPermMaps.add(vXPermMap1);
		vXPermMapList.setVXPermMaps(vXPermMaps);
		XXPermMapDao xXPermMapDao = Mockito.mock(XXPermMapDao.class);
		Mockito.when(xPermMapService.searchXPermMaps((SearchCriteria) Mockito.any())).thenReturn(vXPermMapList);
		Mockito.when(daoManager.getXXPermMap()).thenReturn(xXPermMapDao);
		VXAuditMapList vXAuditMapList = new VXAuditMapList();
		List<VXAuditMap> vXAuditMaps=new ArrayList<VXAuditMap>();
		VXAuditMap vXAuditMap=getVXAuditMap();
		vXAuditMaps.add(vXAuditMap);
		vXAuditMapList.setVXAuditMaps(vXAuditMaps);
		XXAuditMapDao xXAuditMapDao = Mockito.mock(XXAuditMapDao.class);
		Mockito.when(xAuditMapService.searchXAuditMaps((SearchCriteria) Mockito.any())).thenReturn(vXAuditMapList);
		Mockito.when(daoManager.getXXAuditMap()).thenReturn(xXAuditMapDao);
		XXGroupGroupDao xXGroupGroupDao = Mockito.mock(XXGroupGroupDao.class);
		List<XXGroupGroup> xXGroupGroups = new ArrayList<XXGroupGroup>();
		XXGroupGroup xXGroupGroup = xxGroupGroup();
		xXGroupGroups.add(xXGroupGroup);
		Mockito.when(daoManager.getXXGroupGroup()).thenReturn(xXGroupGroupDao);
		Mockito.when(xXGroupGroupDao.findByGroupId(userId)).thenReturn(xXGroupGroups);
		XXGroupPermissionDao xXGroupPermissionDao= Mockito.mock(XXGroupPermissionDao.class);
		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xXGroupPermissionDao);
		List<XXGroupPermission> xXGroupPermissions=new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xXGroupPermissions.add(xGroupPermissionObj);
		Mockito.when(xXGroupPermissionDao.findByGroupId(vXGroup.getId())).thenReturn(xXGroupPermissions);
		XXPolicyDao xXPolicyDao = Mockito.mock(XXPolicyDao.class);
		List<XXPolicy> xXPolicyList = new ArrayList<XXPolicy>();
		XXPolicy xXPolicy=getXXPolicy();
		xXPolicyList.add(xXPolicy);
		Mockito.when(daoManager.getXXPolicy()).thenReturn(xXPolicyDao);
		Mockito.when(xXPolicyDao.findByGroupId(userId)).thenReturn(xXPolicyList);
		List<XXResource> xResourceList = new ArrayList<XXResource>();
		XXResource xXResource = new XXResource();
		xXResource.setId(userId);
		xXResource.setName("hadoopdev");
		xXResource.setIsRecursive(AppConstants.BOOL_TRUE);
		xXResource.setResourceStatus(AppConstants.STATUS_ENABLED);
		xResourceList.add(xXResource);
		XXResourceDao xxResourceDao = Mockito.mock(XXResourceDao.class);
		Mockito.when(daoManager.getXXResource()).thenReturn(xxResourceDao);
		Mockito.when(xxResourceDao.getById(Mockito.anyLong())).thenReturn(xXResource);
		RangerPolicy rangerPolicy=rangerPolicy();
		Mockito.when(policyService.getPopulatedViewObject(xXPolicy)).thenReturn(rangerPolicy);
		XXModuleDefDao xModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		XXModuleDef xModuleDef=xxModuleDef();
		Mockito.when(xModuleDefDao.findByModuleId(Mockito.anyLong())).thenReturn(xModuleDef);
		List<XXSecurityZoneRefGroup> zoneSecRefGroup=new ArrayList<XXSecurityZoneRefGroup>();
	    XXSecurityZoneRefGroupDao zoneSecRefGroupDao=Mockito.mock(XXSecurityZoneRefGroupDao.class);
	    Mockito.when(daoManager.getXXSecurityZoneRefGroup()).thenReturn(zoneSecRefGroupDao);
	    Mockito.when(zoneSecRefGroupDao.findByGroupId(userId)).thenReturn(zoneSecRefGroup);
		List<XXRoleRefGroup> roleRefGroup = new ArrayList<XXRoleRefGroup>();
		XXRoleRefGroupDao roleRefGroupDao = Mockito.mock(XXRoleRefGroupDao.class);
		Mockito.when(daoManager.getXXRoleRefGroup()).thenReturn(roleRefGroupDao);
		Mockito.when(roleRefGroupDao.findByGroupId(userId)).thenReturn(roleRefGroup);
	    xUserMgr.deleteXGroup(vXGroup.getId(), force);
	}

	@Test
	public void test21DeleteXUser() {
		setup();
		boolean force = true;
		VXUser vXUser = vxUser();
		XXUser xXUser = new XXUser();
		XXUserDao xXUserDao = Mockito.mock(XXUserDao.class);
		Mockito.when(daoManager.getXXUser()).thenReturn(xXUserDao);
		Mockito.when(xXUserDao.getById(vXUser.getId())).thenReturn(xXUser);
		Mockito.when(xUserService.populateViewBean(xXUser)).thenReturn(vXUser);
		VXGroupUserList vxGroupUserList=vxGroupUserList();
		XXGroupUserDao xGroupUserDao = Mockito.mock(XXGroupUserDao.class);
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(xGroupUserDao);
		VXPermMapList vXPermMapList = new VXPermMapList();
		VXPermMap vXPermMap1=getVXPermMap();
		List<VXPermMap> vXPermMaps=new ArrayList<VXPermMap>();
		vXPermMaps.add(vXPermMap1);
		vXPermMapList.setVXPermMaps(vXPermMaps);
		XXPermMapDao xXPermMapDao = Mockito.mock(XXPermMapDao.class);
		Mockito.when(xPermMapService.searchXPermMaps((SearchCriteria) Mockito.any())).thenReturn(vXPermMapList);
		Mockito.when(daoManager.getXXPermMap()).thenReturn(xXPermMapDao);
		VXAuditMapList vXAuditMapList = new VXAuditMapList();
		List<VXAuditMap> vXAuditMaps=new ArrayList<VXAuditMap>();
		VXAuditMap vXAuditMap=getVXAuditMap();
		vXAuditMaps.add(vXAuditMap);
		vXAuditMapList.setVXAuditMaps(vXAuditMaps);
		XXAuditMapDao xXAuditMapDao = Mockito.mock(XXAuditMapDao.class);
		Mockito.when(xAuditMapService.searchXAuditMaps((SearchCriteria) Mockito.any())).thenReturn(vXAuditMapList);
		Mockito.when(daoManager.getXXAuditMap()).thenReturn(xXAuditMapDao);
		VXPortalUser vXPortalUser = userProfile();
		XXPortalUser xXPortalUser = xxPortalUser(vXPortalUser);
		XXPortalUserDao xXPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xXPortalUserDao);
		Mockito.when(xXPortalUserDao.findByLoginId(vXUser.getName().trim())).thenReturn(xXPortalUser);
		Mockito.when(xPortalUserService.populateViewBean(xXPortalUser)).thenReturn(vXPortalUser);
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		XXModuleDefDao xModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		XXModuleDef xModuleDef=xxModuleDef();
		Mockito.when(xModuleDefDao.findByModuleId(Mockito.anyLong())).thenReturn(xModuleDef);
		XXAuthSessionDao xXAuthSessionDao= Mockito.mock(XXAuthSessionDao.class);
		XXUserPermissionDao xXUserPermissionDao= Mockito.mock(XXUserPermissionDao.class);
		XXPortalUserRoleDao xXPortalUserRoleDao= Mockito.mock(XXPortalUserRoleDao.class);
		Mockito.when(daoManager.getXXAuthSession()).thenReturn(xXAuthSessionDao);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xXUserPermissionDao);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xXPortalUserRoleDao);
		List<XXAuthSession> xXAuthSessions=new ArrayList<XXAuthSession>();
		XXAuthSession xXAuthSession = new XXAuthSession();
		xXAuthSession.setId(userId);
		xXAuthSession.setLoginId(vXPortalUser.getLoginId());
		xXAuthSessions.add(xXAuthSession);
		List<XXUserPermission> xXUserPermissions=new ArrayList<XXUserPermission>();
		xXUserPermissions.add(xxUserPermission());
		List<XXPortalUserRole> xXPortalUserRoles=new ArrayList<XXPortalUserRole>();
		xXPortalUserRoles.add(XXPortalUserRole);
		Mockito.when(xXAuthSessionDao.getAuthSessionByUserId(vXPortalUser.getId())).thenReturn(xXAuthSessions);
		Mockito.when(xXUserPermissionDao.findByUserPermissionId(vXPortalUser.getId())).thenReturn(xXUserPermissions);
		Mockito.when(xXPortalUserRoleDao.findByUserId(vXPortalUser.getId())).thenReturn(xXPortalUserRoles);
		XXPolicyDao xXPolicyDao = Mockito.mock(XXPolicyDao.class);
		List<XXPolicy> xXPolicyList = new ArrayList<XXPolicy>();
		XXPolicy xXPolicy=getXXPolicy();
		xXPolicyList.add(xXPolicy);
		Mockito.when(daoManager.getXXPolicy()).thenReturn(xXPolicyDao);
		Mockito.when(xXPolicyDao.findByUserId(vXUser.getId())).thenReturn(xXPolicyList);
		RangerPolicy rangerPolicy=rangerPolicy();
		Mockito.when(policyService.getPopulatedViewObject(xXPolicy)).thenReturn(rangerPolicy);
		List<XXSecurityZoneRefUser> zoneSecRefUser=new ArrayList<XXSecurityZoneRefUser>();
	    XXSecurityZoneRefUserDao zoneSecRefUserDao=Mockito.mock(XXSecurityZoneRefUserDao.class);
	    Mockito.when(daoManager.getXXSecurityZoneRefUser()).thenReturn(zoneSecRefUserDao);
	    Mockito.when(zoneSecRefUserDao.findByUserId(userId)).thenReturn(zoneSecRefUser);
	    List<XXRoleRefUser> roleRefUser=new ArrayList<XXRoleRefUser>();
	    XXRoleRefUserDao roleRefUserDao=Mockito.mock(XXRoleRefUserDao.class);
	    Mockito.when(daoManager.getXXRoleRefUser()).thenReturn(roleRefUserDao);
	    Mockito.when(roleRefUserDao.findByUserId(userId)).thenReturn(roleRefUser);
		xUserMgr.deleteXUser(vXUser.getId(), force);
		force=false;
		xUserMgr.deleteXUser(vXUser.getId(), force);
	}

	@Test
	public void test22DeleteXGroupAndXUser() {
		setup();
		VXUser vxUser = vxUser();
		VXGroup vxGroup = vxGroup();
		VXGroupUserList vxGroupUserList = new VXGroupUserList();
		List<VXGroupUser> vXGroupUsers = new ArrayList<VXGroupUser>();
		VXGroupUser vxGroupUser = vxGroupUser();
		vXGroupUsers.add(vxGroupUser);
		vxGroupUserList.setVXGroupUsers(vXGroupUsers);
		Mockito.when(xGroupService.getGroupByGroupName(Mockito.anyString())).thenReturn(vxGroup);
		Mockito.when(xUserService.getXUserByUserName(Mockito.anyString())).thenReturn(vxUser);
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		XXGroupUserDao xGrpUserDao = Mockito.mock(XXGroupUserDao.class);
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(xGrpUserDao);
		Mockito.when(xGrpUserDao.remove(vxGroupUser.getId())).thenReturn(true);
		xUserMgr.deleteXGroupAndXUser(groupName, userLoginID);
		Mockito.verify(xGroupService).getGroupByGroupName(Mockito.anyString());
		Mockito.verify(xUserService).getXUserByUserName(Mockito.anyString());
		Mockito.verify(xGroupUserService).searchXGroupUsers((SearchCriteria) Mockito.any());
	}

	@Test
	public void test23CreateVXUserGroupInfo() {
		setup();
		VXUserGroupInfo vXUserGroupInfo = new VXUserGroupInfo();
		VXUser vXUser = vxUser();
		List<VXGroupUser> vXGroupUserList = new ArrayList<VXGroupUser>();
		List<VXGroup> vXGroupList = new ArrayList<VXGroup>();
		final VXGroup vXGroup1 = vxGroup();
		vXGroup1.setName("users");
		vXGroup1.setDescription("users -added for unit testing");
		vXGroupList.add(vXGroup1);
		VXGroupUser vXGroupUser1 = vxGroupUser();
		vXGroupUser1.setName("users");
		vXGroupUserList.add(vXGroupUser1);
		final VXGroup vXGroup2 = vxGroup();
		vXGroup2.setName("user1");
		vXGroup2.setDescription("user1 -added for unit testing");
		vXGroupList.add(vXGroup2);
		VXGroupUser vXGroupUser2 = vxGroupUser();
		vXGroupUser2.setName("user1");
		vXGroupUserList.add(vXGroupUser2);
		vXUserGroupInfo.setXuserInfo(vXUser);
		vXUserGroupInfo.setXgroupInfo(vXGroupList);
		Mockito.when(xUserService.createXUserWithOutLogin(vXUser)).thenReturn(vXUser);
		Mockito.when(xGroupService.createXGroupWithOutLogin(vXGroup1)).thenReturn(vXGroup1);
		Mockito.when(xGroupService.createXGroupWithOutLogin(vXGroup2)).thenReturn(vXGroup2);
		XXPortalUserDao portalUser = Mockito.mock(XXPortalUserDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(portalUser);
		XXPortalUser user = new XXPortalUser();
		user.setId(1L);
		user.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		Mockito.when(portalUser.findByLoginId(vXUser.getName())).thenReturn(user);
		XXPortalUserRoleDao userDao = Mockito.mock(XXPortalUserRoleDao.class);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(userDao);
		List<String> existingRole = new ArrayList<String>();
		existingRole.add(RangerConstants.ROLE_USER);
		List<String> reqRoleList = new ArrayList<String>();
		reqRoleList.add(RangerConstants.ROLE_SYS_ADMIN);
		Mockito.when(userDao.findXPortalUserRolebyXPortalUserId(Mockito.anyLong())).thenReturn(reqRoleList);
		VXPortalUser vXPortalUser = userProfile();
		Mockito.when(userMgr.getUserProfileByLoginId(Mockito.anyString())).thenReturn(vXPortalUser);
		Mockito.when(userMgr.updateRoleForExternalUsers(Mockito.any(), Mockito.any(), (VXPortalUser)Mockito.any())).thenReturn(vXPortalUser);
		XXModuleDefDao xXModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXUserPermissionDao xXUserPermissionDao= Mockito.mock(XXUserPermissionDao.class);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		vXPortalUser.setUserRoleList(userRoleList);
		VXUser vxUser = vxUser();
		XXUser xXUser = xxUser(vxUser);
		List<XXModuleDef> xXModuleDefs = xxModuleDefs();
		VXUserPermission userPermission = vxUserPermission();
		List<VXUserPermission> userPermList = new ArrayList<VXUserPermission>();
		userPermList.add(userPermission);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionObj.setModuleId(userPermission.getModuleId());
		xUserPermissionObj.setUserId(userPermission.getUserId());
		xUserPermissionsList.add(xUserPermissionObj);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xXModuleDefDao);
		Mockito.when(xXModuleDefDao.getAll()).thenReturn(xXModuleDefs);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xXUserPermissionDao);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByPortalUserId(vXPortalUser.getId())).thenReturn(xXUser);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		Mockito.when(xUserPermissionService.createResource((VXUserPermission) Mockito.any())).thenReturn(userPermission);
		Mockito.when(sessionMgr.getActiveUserSessionsForPortalUserId(userId)).thenReturn(userSessions);
		VXUserGroupInfo vxUserGroupTest = xUserMgr.createXUserGroupFromMap(vXUserGroupInfo);
		Assert.assertEquals(userLoginID, vxUserGroupTest.getXuserInfo().getName());
		List<VXGroup> result = vxUserGroupTest.getXgroupInfo();
		List<VXGroup> expected = new ArrayList<VXGroup>();
		expected.add(vXGroup1);
		expected.add(vXGroup2);
		Assert.assertTrue(result.containsAll(expected));
		Mockito.verify(portalUser).findByLoginId(vXUser.getName());
		Mockito.verify(userDao).findXPortalUserRolebyXPortalUserId(
		Mockito.anyLong());
	}

	@Test
	public void test24createXModuleDefPermission() {
		VXModuleDef vXModuleDef = vxModuleDef();
		Mockito.when(xModuleDefService.createResource(vXModuleDef)).thenReturn(vXModuleDef);
		XXModuleDefDao obj = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(obj);
		VXModuleDef dbMuduleDef = xUserMgr.createXModuleDefPermission(vXModuleDef);
		Assert.assertNotNull(dbMuduleDef);
		Assert.assertEquals(dbMuduleDef, vXModuleDef);
		Assert.assertEquals(dbMuduleDef.getId(), vXModuleDef.getId());
		Assert.assertEquals(dbMuduleDef.getOwner(), vXModuleDef.getOwner());
		Assert.assertEquals(dbMuduleDef.getUpdatedBy(),vXModuleDef.getUpdatedBy());
		Assert.assertEquals(dbMuduleDef.getUrl(), vXModuleDef.getUrl());
		Assert.assertEquals(dbMuduleDef.getAddedById(),vXModuleDef.getAddedById());
		Assert.assertEquals(dbMuduleDef.getCreateDate(),vXModuleDef.getCreateDate());
		Assert.assertEquals(dbMuduleDef.getCreateTime(),vXModuleDef.getCreateTime());
		Assert.assertEquals(dbMuduleDef.getUserPermList(),vXModuleDef.getUserPermList());
		Assert.assertEquals(dbMuduleDef.getGroupPermList(),vXModuleDef.getGroupPermList());
		Mockito.verify(xModuleDefService).createResource(vXModuleDef);
	}

	@Test
	public void test25getXModuleDefPermission() {
		VXModuleDef vXModuleDef = vxModuleDef();
		Mockito.when(xModuleDefService.readResource(1L)).thenReturn(vXModuleDef);
		VXModuleDef dbMuduleDef = xUserMgr.getXModuleDefPermission(1L);
		Assert.assertNotNull(dbMuduleDef);
		Assert.assertEquals(dbMuduleDef, vXModuleDef);
		Assert.assertEquals(dbMuduleDef.getId(), vXModuleDef.getId());
		Assert.assertEquals(dbMuduleDef.getOwner(), vXModuleDef.getOwner());
		Assert.assertEquals(dbMuduleDef.getUpdatedBy(),vXModuleDef.getUpdatedBy());
		Assert.assertEquals(dbMuduleDef.getUrl(), vXModuleDef.getUrl());
		Assert.assertEquals(dbMuduleDef.getAddedById(),vXModuleDef.getAddedById());
		Assert.assertEquals(dbMuduleDef.getCreateDate(),vXModuleDef.getCreateDate());
		Assert.assertEquals(dbMuduleDef.getCreateTime(),vXModuleDef.getCreateTime());
		Assert.assertEquals(dbMuduleDef.getUserPermList(),vXModuleDef.getUserPermList());
		Assert.assertEquals(dbMuduleDef.getGroupPermList(),vXModuleDef.getGroupPermList());
		Mockito.verify(xModuleDefService).readResource(1L);
	}

	@Test
	public void test26updateXModuleDefPermission() {
		XXModuleDefDao xModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXModuleDef xModuleDef = xxModuleDef();
		VXModuleDef vXModuleDef = vxModuleDef();
		Mockito.when(xModuleDefService.updateResource(vXModuleDef)).thenReturn(vXModuleDef);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		Mockito.when(xModuleDefDao.getById(userId)).thenReturn(xModuleDef);
		Map<Long, String> xXGroupNameMap = new HashMap<Long, String>();
		xXGroupNameMap.put(userId, groupName);
		Mockito.when(xGroupService.getXXGroupIdNameMap()).thenReturn(xXGroupNameMap);
		Object[] objArr = new Object[] {userId ,userId,userLoginID};
		Map<Long, Object[]> xXUserMap =new HashMap<Long, Object[]>();
		xXUserMap.put(userId, objArr);
		Mockito.when(xUserService.getXXPortalUserIdXXUserNameMap()).thenReturn(xXUserMap);
		Mockito.when(xModuleDefService.populateViewBean(xModuleDef,xXUserMap,xXGroupNameMap,true)).thenReturn(vXModuleDef);
		List<XXGroupPermission> xXGroupPermissions=new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xXGroupPermissions.add(xGroupPermissionObj);
		VXGroupPermission groupPermission=vxGroupPermission();
		List<XXUserPermission> xXUserPermissions=new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj=xxUserPermission();
		xXUserPermissions.add(xUserPermissionObj);
		VXUserPermission vxUserPermission=vxUserPermission();

		Map<Long, VXGroupPermission> groupPermMapOld = new HashMap<Long, VXGroupPermission>();
		groupPermMapOld.put(groupPermission.getGroupId(), groupPermission);
		Mockito.when(xGroupPermissionService.convertVListToVMap((List<VXGroupPermission>) Mockito.any())).thenReturn(groupPermMapOld);

		Map<Long, VXUserPermission> userPermMapOld = new HashMap<Long, VXUserPermission>();
		userPermMapOld.put(vxUserPermission.getUserId(), vxUserPermission);
		Mockito.when(xUserPermissionService.convertVListToVMap((List<VXUserPermission>) Mockito.any())).thenReturn(userPermMapOld);

		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		VXModuleDef dbMuduleDef = xUserMgr.updateXModuleDefPermission(vXModuleDef);
		Assert.assertEquals(dbMuduleDef, vXModuleDef);
		Assert.assertNotNull(dbMuduleDef);
		Assert.assertEquals(dbMuduleDef, vXModuleDef);
		Assert.assertEquals(dbMuduleDef.getId(), vXModuleDef.getId());
		Assert.assertEquals(dbMuduleDef.getOwner(), vXModuleDef.getOwner());
		Assert.assertEquals(dbMuduleDef.getUpdatedBy(),vXModuleDef.getUpdatedBy());
		Assert.assertEquals(dbMuduleDef.getUrl(), vXModuleDef.getUrl());
		Assert.assertEquals(dbMuduleDef.getAddedById(),vXModuleDef.getAddedById());
		Assert.assertEquals(dbMuduleDef.getCreateDate(),vXModuleDef.getCreateDate());
		Assert.assertEquals(dbMuduleDef.getCreateTime(),vXModuleDef.getCreateTime());
		Assert.assertEquals(dbMuduleDef.getUserPermList(),vXModuleDef.getUserPermList());
		Assert.assertEquals(dbMuduleDef.getGroupPermList(),vXModuleDef.getGroupPermList());
		Mockito.verify(xModuleDefService).updateResource(vXModuleDef);
		Mockito.verify(daoManager).getXXModuleDef();
		Mockito.verify(xModuleDefService).populateViewBean(xModuleDef,xXUserMap,xXGroupNameMap,true);
		vXModuleDef.setModule("UnknownModule");
		Mockito.when(xModuleDefDao.getById(userId)).thenReturn(xModuleDef);
		Mockito.when(restErrorUtil.createRESTException("Module name change is not allowed!",MessageEnums.DATA_NOT_UPDATABLE)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		dbMuduleDef = xUserMgr.updateXModuleDefPermission(vXModuleDef);
	}

	@Test
	public void test27deleteXModuleDefPermission() {
		Long moduleId=Long.valueOf(1);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		XXGroupPermissionDao xGroupPermissionDao = Mockito.mock(XXGroupPermissionDao.class);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xGroupPermissionDao);
		Mockito.doNothing().when(xUserPermissionDao).deleteByModuleId(moduleId);
		Mockito.doNothing().when(xGroupPermissionDao).deleteByModuleId(moduleId);
		Mockito.when(xModuleDefService.deleteResource(1L)).thenReturn(true);
		xUserMgr.deleteXModuleDefPermission(1L, true);
		Mockito.verify(xModuleDefService).deleteResource(1L);
	}

	@Test
	public void test28createXUserPermission() {
		VXUserPermission vXUserPermission = vxUserPermission();
		Mockito.when(xUserPermissionService.createResource(vXUserPermission)).thenReturn(vXUserPermission);
		VXUserPermission dbUserPermission = xUserMgr.createXUserPermission(vXUserPermission);
		Assert.assertNotNull(dbUserPermission);
		Assert.assertEquals(dbUserPermission, vXUserPermission);
		Assert.assertEquals(dbUserPermission.getId(), vXUserPermission.getId());
		Assert.assertEquals(dbUserPermission.getOwner(),vXUserPermission.getOwner());
		Assert.assertEquals(dbUserPermission.getUpdatedBy(),vXUserPermission.getUpdatedBy());
		Assert.assertEquals(dbUserPermission.getUserName(),vXUserPermission.getUserName());
		Assert.assertEquals(dbUserPermission.getCreateDate(),vXUserPermission.getCreateDate());
		Assert.assertEquals(dbUserPermission.getIsAllowed(),vXUserPermission.getIsAllowed());
		Assert.assertEquals(dbUserPermission.getModuleId(),vXUserPermission.getModuleId());
		Assert.assertEquals(dbUserPermission.getUpdateDate(),vXUserPermission.getUpdateDate());
		Assert.assertEquals(dbUserPermission.getUserId(),vXUserPermission.getUserId());
		Mockito.verify(xUserPermissionService).createResource(vXUserPermission);
	}

	@Test
	public void test29getXUserPermission() {
		VXUserPermission vXUserPermission = vxUserPermission();
		Mockito.when(xUserPermissionService.readResource(1L)).thenReturn(vXUserPermission);
		VXUserPermission dbUserPermission = xUserMgr.getXUserPermission(1L);
		Assert.assertNotNull(dbUserPermission);
		Assert.assertEquals(dbUserPermission, vXUserPermission);
		Assert.assertEquals(dbUserPermission.getId(), vXUserPermission.getId());
		Assert.assertEquals(dbUserPermission.getOwner(),vXUserPermission.getOwner());
		Assert.assertEquals(dbUserPermission.getUpdatedBy(),vXUserPermission.getUpdatedBy());
		Assert.assertEquals(dbUserPermission.getUserName(),vXUserPermission.getUserName());
		Assert.assertEquals(dbUserPermission.getCreateDate(),vXUserPermission.getCreateDate());
		Assert.assertEquals(dbUserPermission.getIsAllowed(),vXUserPermission.getIsAllowed());
		Assert.assertEquals(dbUserPermission.getModuleId(),vXUserPermission.getModuleId());
		Assert.assertEquals(dbUserPermission.getUpdateDate(),vXUserPermission.getUpdateDate());
		Assert.assertEquals(dbUserPermission.getUserId(),vXUserPermission.getUserId());
		Mockito.verify(xUserPermissionService).readResource(1L);
	}

	@Test
	public void test30updateXUserPermission() {
		VXUserPermission vXUserPermission = vxUserPermission();
		Mockito.when(xUserPermissionService.updateResource(vXUserPermission)).thenReturn(vXUserPermission);
		VXUserPermission dbUserPermission = xUserMgr.updateXUserPermission(vXUserPermission);
		Assert.assertNotNull(dbUserPermission);
		Assert.assertEquals(dbUserPermission, vXUserPermission);
		Assert.assertEquals(dbUserPermission.getId(), vXUserPermission.getId());
		Assert.assertEquals(dbUserPermission.getOwner(),vXUserPermission.getOwner());
		Assert.assertEquals(dbUserPermission.getUpdatedBy(),vXUserPermission.getUpdatedBy());
		Assert.assertEquals(dbUserPermission.getUserName(),vXUserPermission.getUserName());
		Assert.assertEquals(dbUserPermission.getCreateDate(),vXUserPermission.getCreateDate());
		Assert.assertEquals(dbUserPermission.getIsAllowed(),vXUserPermission.getIsAllowed());
		Assert.assertEquals(dbUserPermission.getModuleId(),vXUserPermission.getModuleId());
		Assert.assertEquals(dbUserPermission.getUpdateDate(),vXUserPermission.getUpdateDate());
		Assert.assertEquals(dbUserPermission.getUserId(),vXUserPermission.getUserId());
		Mockito.verify(xUserPermissionService).updateResource(vXUserPermission);
	}

	@Test
	public void test31deleteXUserPermission() {
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		XXUserPermission xUserPermissionObj = xxUserPermission();
		XXUserPermissionDao xUserPermDao = Mockito.mock(XXUserPermissionDao.class);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermDao);
		Mockito.when(xUserPermDao.getById(1L)).thenReturn(xUserPermissionObj);
		Mockito.when(xUserPermissionService.deleteResource(1L)).thenReturn(true);
		Mockito.when(sessionMgr.getActiveUserSessionsForPortalUserId(xUserPermissionObj.getUserId())).thenReturn(userSessions);
		xUserMgr.deleteXUserPermission(1L, true);
		Mockito.verify(xUserPermissionService).deleteResource(1L);
	}

	@Test
	public void test32createXGroupPermission() {
		VXGroupPermission vXGroupPermission = vxGroupPermission();
		XXGroupUserDao xGrpUserDao = Mockito.mock(XXGroupUserDao.class);
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(xGrpUserDao);
		Mockito.when(xGroupPermissionService.createResource(vXGroupPermission)).thenReturn(vXGroupPermission);
		List<XXGroupUser> xXGroupUserList = new ArrayList<XXGroupUser>();
		VXGroupUser vxGroupUser = vxGroupUser();
		XXGroupUser xXGroupUser =new XXGroupUser();
		xXGroupUser.setId(vxGroupUser.getId());
		xXGroupUser.setName(vxGroupUser.getName());
		xXGroupUser.setParentGroupId(vxGroupUser.getParentGroupId());
		xXGroupUser.setUserId(vxGroupUser.getUserId());
		xXGroupUserList.add(xXGroupUser);
		Mockito.when(xGrpUserDao.findByGroupId(vXGroupPermission.getGroupId())).thenReturn(xXGroupUserList);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		Mockito.when(sessionMgr.getActiveUserSessionsForXUserId(xXGroupUser.getUserId())).thenReturn(userSessions);
		VXGroupPermission dbGroupPermission = xUserMgr.createXGroupPermission(vXGroupPermission);
		Assert.assertNotNull(dbGroupPermission);
		Assert.assertEquals(dbGroupPermission, vXGroupPermission);
		Assert.assertEquals(dbGroupPermission.getId(),vXGroupPermission.getId());
		Assert.assertEquals(dbGroupPermission.getGroupName(),vXGroupPermission.getGroupName());
		Assert.assertEquals(dbGroupPermission.getOwner(),vXGroupPermission.getOwner());
		Assert.assertEquals(dbGroupPermission.getUpdatedBy(),vXGroupPermission.getUpdatedBy());
		Assert.assertEquals(dbGroupPermission.getCreateDate(),vXGroupPermission.getCreateDate());
		Assert.assertEquals(dbGroupPermission.getGroupId(),vXGroupPermission.getGroupId());
		Assert.assertEquals(dbGroupPermission.getIsAllowed(),vXGroupPermission.getIsAllowed());
		Assert.assertEquals(dbGroupPermission.getModuleId(),vXGroupPermission.getModuleId());
		Assert.assertEquals(dbGroupPermission.getUpdateDate(),vXGroupPermission.getUpdateDate());
		Mockito.verify(xGroupPermissionService).createResource(vXGroupPermission);
	}

	@Test
	public void test33getXGroupPermission() {
		VXGroupPermission vXGroupPermission = vxGroupPermission();
		Mockito.when(xGroupPermissionService.readResource(1L)).thenReturn(vXGroupPermission);
		VXGroupPermission dbGroupPermission = xUserMgr.getXGroupPermission(1L);
		Assert.assertNotNull(dbGroupPermission);
		Assert.assertEquals(dbGroupPermission, vXGroupPermission);
		Assert.assertEquals(dbGroupPermission.getId(),vXGroupPermission.getId());
		Assert.assertEquals(dbGroupPermission.getGroupName(),vXGroupPermission.getGroupName());
		Assert.assertEquals(dbGroupPermission.getOwner(),vXGroupPermission.getOwner());
		Assert.assertEquals(dbGroupPermission.getUpdatedBy(),vXGroupPermission.getUpdatedBy());
		Assert.assertEquals(dbGroupPermission.getCreateDate(),vXGroupPermission.getCreateDate());
		Assert.assertEquals(dbGroupPermission.getGroupId(),vXGroupPermission.getGroupId());
		Assert.assertEquals(dbGroupPermission.getIsAllowed(),vXGroupPermission.getIsAllowed());
		Assert.assertEquals(dbGroupPermission.getModuleId(),vXGroupPermission.getModuleId());
		Assert.assertEquals(dbGroupPermission.getUpdateDate(),vXGroupPermission.getUpdateDate());
		Mockito.verify(xGroupPermissionService).readResource(1L);
	}

	@Test
	public void test34updateXGroupPermission() {
		setup();
		VXGroupPermission vXGroupPermission = vxGroupPermission();
		XXGroupUserDao xGrpUserDao = Mockito.mock(XXGroupUserDao.class);
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(xGrpUserDao);
		Mockito.when(xGroupPermissionService.updateResource(vXGroupPermission)).thenReturn(vXGroupPermission);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		Mockito.when(sessionMgr.getActiveUserSessionsForXUserId(userId)).thenReturn(userSessions);
		List<XXGroupUser> xXGroupUserList = new ArrayList<XXGroupUser>();
		VXGroupUser vxGroupUser = vxGroupUser();
		XXGroupUser xXGroupUser =new XXGroupUser();
		xXGroupUser.setId(vxGroupUser.getId());
		xXGroupUser.setName(vxGroupUser.getName());
		xXGroupUser.setParentGroupId(vxGroupUser.getParentGroupId());
		xXGroupUser.setUserId(vxGroupUser.getUserId());
		xXGroupUserList.add(xXGroupUser);
		Mockito.when(xGrpUserDao.findByGroupId(vXGroupPermission.getGroupId())).thenReturn(xXGroupUserList);
		VXGroupPermission dbGroupPermission = xUserMgr.updateXGroupPermission(vXGroupPermission);
		Assert.assertNotNull(dbGroupPermission);
		Assert.assertEquals(dbGroupPermission, vXGroupPermission);
		Assert.assertEquals(dbGroupPermission.getId(),vXGroupPermission.getId());
		Assert.assertEquals(dbGroupPermission.getGroupName(),vXGroupPermission.getGroupName());
		Assert.assertEquals(dbGroupPermission.getOwner(),vXGroupPermission.getOwner());
		Assert.assertEquals(dbGroupPermission.getUpdatedBy(),vXGroupPermission.getUpdatedBy());
		Assert.assertEquals(dbGroupPermission.getCreateDate(),vXGroupPermission.getCreateDate());
		Assert.assertEquals(dbGroupPermission.getGroupId(),vXGroupPermission.getGroupId());
		Assert.assertEquals(dbGroupPermission.getIsAllowed(),vXGroupPermission.getIsAllowed());
		Assert.assertEquals(dbGroupPermission.getModuleId(),vXGroupPermission.getModuleId());
		Assert.assertEquals(dbGroupPermission.getUpdateDate(),vXGroupPermission.getUpdateDate());
		Mockito.verify(xGroupPermissionService).updateResource(vXGroupPermission);
	}

	@Test
	public void test35deleteXGroupPermission() {
		XXGroupPermissionDao xGrpPermDao = Mockito.mock(XXGroupPermissionDao.class);
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xGrpPermDao);
		Mockito.when(xGrpPermDao.getById(1L)).thenReturn(xGroupPermissionObj);
		XXGroupUserDao xGrpUserDao = Mockito.mock(XXGroupUserDao.class);
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(xGrpUserDao);
		List<XXGroupUser> xXGroupUserList = new ArrayList<XXGroupUser>();
		VXGroupUser vxGroupUser = vxGroupUser();
		XXGroupUser xXGroupUser =new XXGroupUser();
		xXGroupUser.setId(vxGroupUser.getId());
		xXGroupUser.setName(vxGroupUser.getName());
		xXGroupUser.setParentGroupId(vxGroupUser.getParentGroupId());
		xXGroupUser.setUserId(vxGroupUser.getUserId());
		xXGroupUserList.add(xXGroupUser);
		Mockito.when(xGrpUserDao.findByGroupId(xGroupPermissionObj.getGroupId())).thenReturn(xXGroupUserList);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		Mockito.when(sessionMgr.getActiveUserSessionsForXUserId(userId)).thenReturn(userSessions);
		Mockito.when(xGroupPermissionService.deleteResource(1L)).thenReturn(true);
		xUserMgr.deleteXGroupPermission(1L, true);
		Mockito.verify(xGroupPermissionService).deleteResource(1L);
	}

	@Test
	public void test36getGroupsForUser() {
		setupUser();
		VXUser vxUser = vxUser();
		VXGroup vxGroup=vxGroup();
		String userName = userLoginID;
		Mockito.when(xUserService.getXUserByUserName(userName)).thenReturn(vxUser);
		VXGroupUserList vxGroupUserList = vxGroupUserList();
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		Mockito.when(xGroupService.readResource(userId)).thenReturn(vxGroup);
		XXModuleDefDao modDef = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(modDef);
		List<String> lstModule = new ArrayList<String>();
		lstModule.add(RangerConstants.MODULE_USER_GROUPS);
		lstModule.add(RangerConstants.MODULE_RESOURCE_BASED_POLICIES);
		Mockito.when(modDef.findAccessibleModulesByUserId(Mockito.anyLong(),
		Mockito.anyLong())).thenReturn(lstModule);
		Set<String> list = xUserMgr.getGroupsForUser(userName);
		Assert.assertNotNull(list);
		Mockito.verify(xUserService, Mockito.atLeast(2)).getXUserByUserName(userName);
		Mockito.verify(modDef).findAccessibleModulesByUserId(Mockito.anyLong(),Mockito.anyLong());
		Mockito.when(xUserService.getXUserByUserName(userName)).thenReturn(null);
		list = xUserMgr.getGroupsForUser(userName);
		Assert.assertTrue(list.isEmpty());
		Mockito.verify(xUserService, Mockito.atLeast(2)).getXUserByUserName(userName);
		Mockito.verify(modDef).findAccessibleModulesByUserId(Mockito.anyLong(),Mockito.anyLong());
		Mockito.when(xUserService.getXUserByUserName(userName)).thenReturn(null);
		list = xUserMgr.getGroupsForUser(userName);
		Assert.assertTrue(list.isEmpty());
		Mockito.verify(xUserService, Mockito.atLeast(2)).getXUserByUserName(userName);
		Mockito.verify(modDef).findAccessibleModulesByUserId(Mockito.anyLong(),Mockito.anyLong());
	}

	@Test
	public void test37setUserRolesByExternalID() {
		setup();
		XXPortalUserRoleDao xPortalUserRoleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXUser vXUser = vxUser();
		VXPortalUser userProfile = userProfile();
		List<VXString> vStringRolesList = new ArrayList<VXString>();
		VXString vXStringObj = new VXString();
		vXStringObj.setValue("ROLE_USER");
		vStringRolesList.add(vXStringObj);
		List<XXPortalUserRole> xPortalUserRoleList = new ArrayList<XXPortalUserRole>();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		xPortalUserRoleList.add(XXPortalUserRole);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionsList.add(xUserPermissionObj);
		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xGroupPermissionList.add(xGroupPermissionObj);
		List<VXGroupPermission> groupPermList = new ArrayList<VXGroupPermission>();
		VXGroupPermission groupPermission = vxGroupPermission();
		groupPermList.add(groupPermission);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xPortalUserRoleDao);
		Mockito.when(xPortalUserRoleDao.findByUserId(userId)).thenReturn(xPortalUserRoleList);
		Mockito.when(xUserMgr.getXUser(userId)).thenReturn(vXUser);
		Mockito.when(userMgr.getUserProfileByLoginId(vXUser.getName())).thenReturn(userProfile);
		
		List<String> permissionList = new ArrayList<String>();
		permissionList.add(RangerConstants.MODULE_USER_GROUPS);
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_ADMIN);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		Mockito.when(xUserService.getXUserByUserName("admin")).thenReturn(loggedInUser);
		
		XXModuleDefDao mockxxModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(mockxxModuleDefDao);
		Mockito.when(mockxxModuleDefDao.findAccessibleModulesByUserId(8L, 8L)).thenReturn(permissionList);
		
		VXStringList vXStringList = xUserMgr.setUserRolesByExternalID(userId,vStringRolesList);
		Assert.assertNotNull(vXStringList);
	}

	@Test
	public void test38setUserRolesByExternalID() {
		destroySession();
		setup();
		VXUser vXUser = vxUser();
		List<VXString> vStringRolesList = new ArrayList<VXString>();
		VXString vXStringObj = new VXString();
		vXStringObj.setValue("ROLE_USER");
		vStringRolesList.add(vXStringObj);
		List<XXPortalUserRole> xPortalUserRoleList = new ArrayList<XXPortalUserRole>();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		xPortalUserRoleList.add(XXPortalUserRole);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionsList.add(xUserPermissionObj);
		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xGroupPermissionList.add(xGroupPermissionObj);
		List<VXGroupPermission> groupPermList = new ArrayList<VXGroupPermission>();
		VXGroupPermission groupPermission = vxGroupPermission();
		groupPermList.add(groupPermission);
		Mockito.when(xUserMgr.getXUser(userId)).thenReturn(vXUser);
		Mockito.when(userMgr.getUserProfileByLoginId(vXUser.getName())).thenReturn(null);
		
		List<String> permissionList = new ArrayList<String>();
		permissionList.add(RangerConstants.MODULE_USER_GROUPS);
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_ADMIN);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		Mockito.when(xUserService.getXUserByUserName("admin")).thenReturn(loggedInUser);
		
		XXModuleDefDao mockxxModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(mockxxModuleDefDao);
		Mockito.when(mockxxModuleDefDao.findAccessibleModulesByUserId(8L, 8L)).thenReturn(permissionList);
		
		Mockito.when(restErrorUtil.createRESTException("User ID doesn't exist.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.setUserRolesByExternalID(userId, vStringRolesList);
	}

	@Test
	public void test39setUserRolesByExternalID() {
		destroySession();
		setup();
		VXUser vXUser = vxUser();
		List<VXString> vStringRolesList = new ArrayList<VXString>();
		VXString vXStringObj = new VXString();
		vXStringObj.setValue("ROLE_USER");
		vStringRolesList.add(vXStringObj);
		List<XXPortalUserRole> xPortalUserRoleList = new ArrayList<XXPortalUserRole>();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		xPortalUserRoleList.add(XXPortalUserRole);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionsList.add(xUserPermissionObj);
		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xGroupPermissionList.add(xGroupPermissionObj);
		List<VXGroupPermission> groupPermList = new ArrayList<VXGroupPermission>();
		VXGroupPermission groupPermission = vxGroupPermission();
		groupPermList.add(groupPermission);
		Mockito.when(xUserMgr.getXUser(userId)).thenReturn(vXUser);
		Mockito.when(xUserMgr.getXUser(0L)).thenReturn(null);
		Mockito.when(restErrorUtil.createRESTException("User ID doesn't exist.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.setUserRolesByExternalID(0L, vStringRolesList);
	}

	@Test
	public void test40setUserRolesByName() {
		destroySession();
		setup();
		XXPortalUserRoleDao xPortalUserRoleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXPortalUser userProfile = userProfile();
		List<VXString> vStringRolesList = new ArrayList<VXString>();
		VXString vXStringObj = new VXString();
		vXStringObj.setValue("ROLE_USER");
		vStringRolesList.add(vXStringObj);
		List<XXPortalUserRole> xPortalUserRoleList = new ArrayList<XXPortalUserRole>();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		xPortalUserRoleList.add(XXPortalUserRole);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionsList.add(xUserPermissionObj);
		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xGroupPermissionList.add(xGroupPermissionObj);
		List<VXGroupPermission> groupPermList = new ArrayList<VXGroupPermission>();
		VXGroupPermission groupPermission = vxGroupPermission();
		groupPermList.add(groupPermission);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xPortalUserRoleDao);
		Mockito.when(xPortalUserRoleDao.findByUserId(userId)).thenReturn(xPortalUserRoleList);
		Mockito.when(userMgr.getUserProfileByLoginId(userProfile.getLoginId())).thenReturn(userProfile);
		VXStringList vXStringList = xUserMgr.setUserRolesByName(userProfile.getLoginId(), vStringRolesList);
		Assert.assertNotNull(vXStringList);
		Mockito.when(restErrorUtil.createRESTException("Login ID doesn't exist.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.setUserRolesByName(null, vStringRolesList);
	}

	@Test
	public void test41setUserRolesByName() {
		destroySession();
		setup();
		XXPortalUserRoleDao xPortalUserRoleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXPortalUser userProfile = userProfile();
		List<VXString> vStringRolesList = new ArrayList<VXString>();
		VXString vXStringObj = new VXString();
		vXStringObj.setValue("ROLE_USER");
		vStringRolesList.add(vXStringObj);
		List<XXPortalUserRole> xPortalUserRoleList = new ArrayList<XXPortalUserRole>();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		xPortalUserRoleList.add(XXPortalUserRole);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionsList.add(xUserPermissionObj);
		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xGroupPermissionList.add(xGroupPermissionObj);
		List<VXGroupPermission> groupPermList = new ArrayList<VXGroupPermission>();
		VXGroupPermission groupPermission = vxGroupPermission();
		groupPermList.add(groupPermission);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xPortalUserRoleDao);
		Mockito.when(xPortalUserRoleDao.findByUserId(userId)).thenReturn(xPortalUserRoleList);
		Mockito.when(userMgr.getUserProfileByLoginId(userProfile.getLoginId())).thenReturn(userProfile);
		VXStringList vXStringList = xUserMgr.setUserRolesByName(userProfile.getLoginId(), vStringRolesList);
		Assert.assertNotNull(vXStringList);
		Mockito.when(restErrorUtil.createRESTException("Login ID doesn't exist.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.setUserRolesByName(null, vStringRolesList);
	}

	@Test
	public void test42getUserRolesByExternalID() {
		destroySession();
		setup();
		XXPortalUserRoleDao xPortalUserRoleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXUser vXUser = vxUser();
		VXPortalUser userProfile = userProfile();
		List<VXString> vStringRolesList = new ArrayList<VXString>();
		VXString vXStringObj = new VXString();
		vXStringObj.setValue("ROLE_USER");
		vStringRolesList.add(vXStringObj);
		List<XXPortalUserRole> xPortalUserRoleList = new ArrayList<XXPortalUserRole>();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		xPortalUserRoleList.add(XXPortalUserRole);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionsList.add(xUserPermissionObj);
		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xGroupPermissionList.add(xGroupPermissionObj);
		List<VXGroupPermission> groupPermList = new ArrayList<VXGroupPermission>();
		VXGroupPermission groupPermission = vxGroupPermission();
		groupPermList.add(groupPermission);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xPortalUserRoleDao);
		Mockito.when(xPortalUserRoleDao.findByUserId(userId)).thenReturn(xPortalUserRoleList);
		Mockito.when(xUserMgr.getXUser(userId)).thenReturn(vXUser);
		Mockito.when(userMgr.getUserProfileByLoginId(vXUser.getName())).thenReturn(userProfile);
		
		List<String> permissionList = new ArrayList<String>();
		permissionList.add(RangerConstants.MODULE_USER_GROUPS);
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_ADMIN);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		Mockito.when(xUserService.getXUserByUserName("admin")).thenReturn(loggedInUser);
		
		XXModuleDefDao mockxxModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(mockxxModuleDefDao);
		Mockito.when(mockxxModuleDefDao.findAccessibleModulesByUserId(8L, 8L)).thenReturn(permissionList);
		
		VXStringList vXStringList = xUserMgr.getUserRolesByExternalID(userId);
		Assert.assertNotNull(vXStringList);
		Mockito.when(restErrorUtil.createRESTException("Please provide a valid ID",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		Mockito.when(xUserService.readResourceWithOutLogin((Long)Mockito.any())).thenReturn(null);
		xUserMgr.getUserRolesByExternalID(userId);
	}

	@Test
	public void test43getUserRolesByExternalID() {
		destroySession();
		setup();
		XXPortalUserRoleDao xPortalUserRoleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXUser vXUser = vxUser();
		VXPortalUser userProfile = userProfile();
		List<VXString> vStringRolesList = new ArrayList<VXString>();
		VXString vXStringObj = new VXString();
		vXStringObj.setValue("ROLE_USER");
		vStringRolesList.add(vXStringObj);
		List<XXPortalUserRole> xPortalUserRoleList = new ArrayList<XXPortalUserRole>();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		xPortalUserRoleList.add(XXPortalUserRole);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionsList.add(xUserPermissionObj);
		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xGroupPermissionList.add(xGroupPermissionObj);
		List<VXGroupPermission> groupPermList = new ArrayList<VXGroupPermission>();
		VXGroupPermission groupPermission = vxGroupPermission();
		groupPermList.add(groupPermission);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xPortalUserRoleDao);
		Mockito.when(xPortalUserRoleDao.findByUserId(userId)).thenReturn(xPortalUserRoleList);
		Mockito.when(xUserMgr.getXUser(userId)).thenReturn(vXUser);
		Mockito.when(userMgr.getUserProfileByLoginId(vXUser.getName())).thenReturn(userProfile);
		
		List<String> permissionList = new ArrayList<String>();
		permissionList.add(RangerConstants.MODULE_USER_GROUPS);
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_ADMIN);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		Mockito.when(xUserService.getXUserByUserName("admin")).thenReturn(loggedInUser);
		
		XXModuleDefDao mockxxModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(mockxxModuleDefDao);
		Mockito.when(mockxxModuleDefDao.findAccessibleModulesByUserId(8L, 8L)).thenReturn(permissionList);
		
		VXStringList vXStringList = xUserMgr.getUserRolesByExternalID(userId);
		Assert.assertNotNull(vXStringList);
		Mockito.when(restErrorUtil.createRESTException("User ID doesn't exist.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		Mockito.when(userMgr.getUserProfileByLoginId((String)Mockito.anyString())).thenReturn(null);
		xUserMgr.getUserRolesByExternalID(userId);
	}

	@Test
	public void test44getUserRolesByName() {
		destroySession();
		setup();
		XXPortalUserRoleDao xPortalUserRoleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXPortalUser userProfile = userProfile();
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		userProfile.setUserRoleList(userRoleList);
		List<VXString> vStringRolesList = new ArrayList<VXString>();
		VXString vXStringObj = new VXString();
		vXStringObj.setValue("ROLE_USER");
		vStringRolesList.add(vXStringObj);
		List<XXPortalUserRole> xPortalUserRoleList = new ArrayList<XXPortalUserRole>();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		xPortalUserRoleList.add(XXPortalUserRole);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionsList.add(xUserPermissionObj);
		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xGroupPermissionList.add(xGroupPermissionObj);
		List<VXGroupPermission> groupPermList = new ArrayList<VXGroupPermission>();
		VXGroupPermission groupPermission = vxGroupPermission();
		groupPermList.add(groupPermission);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xPortalUserRoleDao);
		Mockito.when(xPortalUserRoleDao.findByUserId(userId)).thenReturn(xPortalUserRoleList);
		Mockito.when(userMgr.getUserProfileByLoginId(userProfile.getLoginId())).thenReturn(userProfile);
		VXStringList vXStringList = xUserMgr.getUserRolesByName(userProfile.getLoginId());
		Assert.assertNotNull(vXStringList);
		Mockito.when(restErrorUtil.createRESTException("Please provide a valid userName",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		Mockito.when(userMgr.getUserProfileByLoginId((String) Mockito.anyString())).thenReturn(null);
		xUserMgr.getUserRolesByName(userProfile.getLoginId());
	}

	@Test
	public void test45getUserRolesByName() {
		destroySession();
		setup();
		XXPortalUserRoleDao xPortalUserRoleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXPortalUser userProfile = userProfile();
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		userProfile.setUserRoleList(userRoleList);
		List<VXString> vStringRolesList = new ArrayList<VXString>();
		VXString vXStringObj = new VXString();
		vXStringObj.setValue("ROLE_USER");
		vStringRolesList.add(vXStringObj);
		List<XXPortalUserRole> xPortalUserRoleList = new ArrayList<XXPortalUserRole>();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		xPortalUserRoleList.add(XXPortalUserRole);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionsList.add(xUserPermissionObj);
		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xGroupPermissionList.add(xGroupPermissionObj);
		List<VXGroupPermission> groupPermList = new ArrayList<VXGroupPermission>();
		VXGroupPermission groupPermission = vxGroupPermission();
		groupPermList.add(groupPermission);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xPortalUserRoleDao);
		Mockito.when(xPortalUserRoleDao.findByUserId(userId)).thenReturn(xPortalUserRoleList);
		Mockito.when(userMgr.getUserProfileByLoginId(userProfile.getLoginId())).thenReturn(userProfile);
		VXStringList vXStringList = xUserMgr.getUserRolesByName(userProfile.getLoginId());
		Assert.assertNotNull(vXStringList);
		Mockito.when(restErrorUtil.createRESTException("Please provide a valid userName",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.getUserRolesByName("");
	}

	@Test
	public void test46hasAccess() {
		setup();
		xUserMgr.hasAccess("test");
	}

	@Test
	public void test47searchXUsers() {
		VXUser vxUser = vxUser();
		vxUser.setStatus(1);
		vxUser.setUserSource(1);
		VXUserList vXUserListSort = new VXUserList();
		List<VXUser> vXUsers = new ArrayList<VXUser>();
		vXUsers.add(vxUser);
		vXUserListSort.setVXUsers(vXUsers);
		String userName = vxUser.getName();
		SearchCriteria testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", userName);
		Mockito.when(xUserService.getXUserByUserName(userName)).thenReturn(vxUser);
		Mockito.when(xUserService.searchXUsers(testSearchCriteria)).thenReturn(vXUserListSort);
		VXGroupUserList vxGroupUserList = vxGroupUserList();
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		VXGroup group = vxGroup();
		Mockito.when(xGroupService.readResource(Mockito.anyLong())).thenReturn(group);
		VXUserList dbVXUserList = xUserMgr.searchXUsers(testSearchCriteria);
		Assert.assertNotNull(dbVXUserList);
		testSearchCriteria.addParam("isvisible", "true");
		dbVXUserList = xUserMgr.searchXUsers(testSearchCriteria);
		Assert.assertNotNull(dbVXUserList);
		testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", userName);
		testSearchCriteria.addParam("usersource", vxUser.getUserSource());
		Mockito.when(xUserService.getXUserByUserName(userName)).thenReturn(vxUser);
		Mockito.when(xUserService.searchXUsers(testSearchCriteria)).thenReturn(vXUserListSort);
		List<VXGroup> vXGroupList = new ArrayList<VXGroup>();
		final VXGroup vXGroup1 = vxGroup();
		vXGroup1.setName("users");
		vXGroup1.setDescription("users -added for unit testing");
		vXGroupList.add(vXGroup1);
		testSearchCriteria.addParam("xUserId", userId);
		dbVXUserList = xUserMgr.searchXUsers(testSearchCriteria);
		Assert.assertNotNull(dbVXUserList);
		testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", userName);
		testSearchCriteria.addParam("emailaddress", vxUser.getEmailAddress());
		Mockito.when(xUserService.getXUserByUserName(userName)).thenReturn(vxUser);
		Mockito.when(xUserService.searchXUsers(testSearchCriteria)).thenReturn(vXUserListSort);
		dbVXUserList = xUserMgr.searchXUsers(testSearchCriteria);
	}

	@Test
	public void test48searchXGroups() {
		setupUser();
		VXGroup vXGroup = vxGroup();
		VXGroupList vXGroupListSort = new VXGroupList();
		List<VXGroup> vXGroups = new ArrayList<VXGroup>();
		vXGroups.add(vXGroup);
		vXGroupListSort.setVXGroups(vXGroups);
		String groupName = vXGroup.getName();
		SearchCriteria testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", groupName);
		Mockito.when(xGroupService.getGroupByGroupName(groupName)).thenReturn(vXGroup);
		Mockito.when(xGroupService.searchXGroups(testSearchCriteria)).thenReturn(vXGroupListSort);
		VXGroupList vXGroupList = xUserMgr.searchXGroups(testSearchCriteria);
		testSearchCriteria.addParam("isvisible", "true");
		vXGroupList = xUserMgr.searchXGroups(testSearchCriteria);
		Assert.assertNotNull(vXGroupList);
		testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", groupName);
		testSearchCriteria.addParam("groupsource", 1L);
		Mockito.when(xGroupService.searchXGroups(testSearchCriteria)).thenReturn(vXGroupListSort);
		vXGroupList = xUserMgr.searchXGroups(testSearchCriteria);
		Assert.assertNotNull(vXGroupList);
		testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", groupName);
		testSearchCriteria.addParam("userid", userId);
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_USER);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		Mockito.when(xUserService.getXUserByUserName(userLoginID)).thenReturn(loggedInUser);
		Mockito.when(xGroupService.searchXGroups(testSearchCriteria)).thenReturn(vXGroupListSort);

		List<Long> groupIdList = new ArrayList<Long>();
		groupIdList.add(2L);
		XXGroupUserDao mockxxGroupUserDao = Mockito.mock(XXGroupUserDao.class);
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(mockxxGroupUserDao);
		Mockito.when(mockxxGroupUserDao.findGroupIdListByUserId(loggedInUser.getId())).thenReturn(groupIdList);
		XXModuleDefDao modDef = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(modDef);
		List<String> lstModule = new ArrayList<String>();
		lstModule.add(RangerConstants.MODULE_USER_GROUPS);
		Mockito.when(modDef.findAccessibleModulesByUserId(Mockito.anyLong(),
		Mockito.anyLong())).thenReturn(lstModule);
		xUserMgr.searchXGroups(testSearchCriteria);
	}

	@Test
	public void test49createServiceConfigUser() {
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		VXUser vxUser = vxUser();
		XXUser xXUser = xxUser(vxUser);
		VXPortalUser userProfile = userProfile();
		Collection<String> userRoleList =getRoleList();
		VXUserPermission vXUserPermission=vxUserPermission();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionObj.setModuleId(vXUserPermission.getModuleId());
		xUserPermissionObj.setUserId(vXUserPermission.getUserId());
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByUserName(vxUser.getName())).thenReturn(xXUser);
		Mockito.when(xUserService.populateViewBean(xXUser)).thenReturn(vxUser);
		VXUser serviceConfigUser=xUserMgr.createServiceConfigUser(vxUser.getName());
		Assert.assertNotNull(serviceConfigUser);
		Assert.assertEquals(xXUser.getName(), serviceConfigUser.getName());
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByUserName(vxUser.getName())).thenReturn(null);
		Mockito.when(xxUserDao.findByUserName(vxUser.getName())).thenReturn(null, xXUser);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);

		userProfile.setUserRoleList(userRoleList);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj2 = new XXUserPermission();
		xUserPermissionObj2.setAddedByUserId(userId);
		xUserPermissionObj2.setCreateTime(new Date());
		xUserPermissionObj2.setId(userId);
		xUserPermissionObj2.setIsAllowed(1);
		xUserPermissionObj2.setModuleId(1L);
		xUserPermissionObj2.setUpdatedByUserId(userId);
		xUserPermissionObj2.setUpdateTime(new Date());
		xUserPermissionObj2.setUserId(userId);
		xUserPermissionsList.add(xUserPermissionObj2);

		serviceConfigUser=xUserMgr.createServiceConfigUser(vxUser.getName());
		Assert.assertNotNull(serviceConfigUser);
		Assert.assertEquals(xXUser.getName(), serviceConfigUser.getName());
	}

	@Test
	public void test50createServiceConfigUser_WithBlankName() {
		destroySession();
		setup();
		Mockito.when(restErrorUtil.createRESTException("Please provide a valid username.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.createServiceConfigUser(null);
	}

	@Test
	public void test51assignPermissionToUser() {
		XXModuleDefDao xXModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXUserPermissionDao xXUserPermissionDao= Mockito.mock(XXUserPermissionDao.class);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		VXPortalUser vXPortalUser = userProfile();
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		vXPortalUser.setUserRoleList(userRoleList);
		VXUser vxUser = vxUser();
		XXUser xXUser = xxUser(vxUser);
		List<XXModuleDef> xXModuleDefs = xxModuleDefs();
		VXUserPermission userPermission = vxUserPermission();
		List<VXUserPermission> userPermList = new ArrayList<VXUserPermission>();
		userPermList.add(userPermission);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionObj.setModuleId(userPermission.getModuleId());
		xUserPermissionObj.setUserId(userPermission.getUserId());
		xUserPermissionsList.add(xUserPermissionObj);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xXModuleDefDao);
		Mockito.when(xXModuleDefDao.getAll()).thenReturn(xXModuleDefs);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xXUserPermissionDao);
		Mockito.when(xXUserPermissionDao.findByModuleIdAndPortalUserId(vXPortalUser.getId(),xXModuleDefs.get(0).getId())).thenReturn(xUserPermissionObj);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByPortalUserId(vXPortalUser.getId())).thenReturn(xXUser);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		Mockito.when(xUserPermissionService.createResource((VXUserPermission) Mockito.any())).thenReturn(userPermission);
		Mockito.when(xUserPermissionService.populateViewBean(xUserPermissionObj)).thenReturn(userPermission);
		Mockito.when(xUserPermissionService.updateResource((VXUserPermission) Mockito.any())).thenReturn(userPermission);
		Mockito.when(sessionMgr.getActiveUserSessionsForPortalUserId(userId)).thenReturn(userSessions);
		xUserMgr.assignPermissionToUser(vXPortalUser,true);
		userRoleList.clear();
		userRoleList.add("ROLE_SYS_ADMIN");
		vXPortalUser.setUserRoleList(userRoleList);
		xUserMgr.assignPermissionToUser(vXPortalUser,true);
		userRoleList.clear();
		userRoleList.add("ROLE_KEY_ADMIN");
		vXPortalUser.setUserRoleList(userRoleList);
		xUserMgr.assignPermissionToUser(vXPortalUser,true);
		userRoleList.clear();
		userRoleList.add("ROLE_KEY_ADMIN_AUDITOR");
		vXPortalUser.setUserRoleList(userRoleList);
		xUserMgr.assignPermissionToUser(vXPortalUser,true);
		userRoleList.clear();
		userRoleList.add("ROLE_ADMIN_AUDITOR");
		vXPortalUser.setUserRoleList(userRoleList);
		xUserMgr.assignPermissionToUser(vXPortalUser,true);
		Assert.assertNotNull(xXModuleDefs);
	}

	@Test
	public void test52createXGroupUserFromMap() {
		setup();
		VXGroup vxGroup=vxGroup();
		VXUser vxUser = vxUser();
		List<VXUser> vXUserList=new ArrayList<VXUser>();
		vXUserList.add(vxUser);
		VXGroupUserInfo vxGUInfo = new VXGroupUserInfo();
		vxGUInfo.setXgroupInfo(vxGroup);
		vxGUInfo.setXuserInfo(vXUserList);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao userRoleDao = Mockito.mock(XXPortalUserRoleDao.class);
		XXModuleDefDao xXModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXUser xXUser = xxUser(vxUser);
		VXPortalUser userProfile = userProfile();
		XXPortalUser xXPortalUser = xxPortalUser(userProfile);
		xXPortalUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		List<String> lstRole = new ArrayList<String>();
		lstRole.add(RangerConstants.ROLE_SYS_ADMIN);
		List<XXModuleDef> xXModuleDefs=new ArrayList<XXModuleDef>();
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByUserName(vxUser.getName())).thenReturn(xXUser);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(vxUser.getName())).thenReturn(xXPortalUser);
		Mockito.when(xGroupService.createXGroupWithOutLogin(vxGroup)).thenReturn(vxGroup);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(userRoleDao);
		Mockito.when(userMgr.mapXXPortalUserToVXPortalUserForDefaultAccount(xXPortalUser)).thenReturn(userProfile);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xXModuleDefDao);
		Mockito.when(xXModuleDefDao.getAll()).thenReturn(xXModuleDefs);
		VXGroupUserInfo vxGUInfoObj=xUserMgr.createXGroupUserFromMap(vxGUInfo);
		Assert.assertNotNull(vxGUInfoObj);
	}

	@Test
	public void test53getXGroupUserFromMap() {
		setup();
		VXGroup vxGroup=vxGroup();
		VXUser vxUser = vxUser();
		List<VXUser> vXUserList=new ArrayList<VXUser>();
		vXUserList.add(vxUser);
		VXGroupUserInfo vxGUInfo = new VXGroupUserInfo();
		vxGUInfo.setXgroupInfo(vxGroup);
		vxGUInfo.setXuserInfo(vXUserList);
		XXGroupDao xxGroupDao = Mockito.mock(XXGroupDao.class);
		XXGroup xxGroup = new XXGroup();
		xxGroup.setId(vxGroup.getId());
		xxGroup.setName(vxGroup.getName());
		xxGroup.setDescription(vxGroup.getDescription());
		xxGroup.setIsVisible(vxGroup.getIsVisible());
		VXPortalUser userProfile = userProfile();
		XXPortalUser xXPortalUser = xxPortalUser(userProfile);
		xXPortalUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		VXGroupUserList vxGroupUserList = new VXGroupUserList();
		List<VXGroupUser> vXGroupUsers = new ArrayList<VXGroupUser>();
		VXGroupUser vxGroupUser = vxGroupUser();
		vXGroupUsers.add(vxGroupUser);
		vxGroupUserList.setVXGroupUsers(vXGroupUsers);
		List<String> lstRole = new ArrayList<String>();
		lstRole.add(RangerConstants.ROLE_USER);
		Mockito.when(daoManager.getXXGroup()).thenReturn(xxGroupDao);
		SearchCriteria searchCriteria = createsearchCriteria();
		searchCriteria.addParam("xGroupId", xxGroup.getId());
		Mockito.when(xxGroupDao.findByGroupName("")).thenReturn(null);
		VXGroupUserInfo vxGUInfoObjNull=xUserMgr.getXGroupUserFromMap("");
		Assert.assertNull(vxGUInfoObjNull.getXgroupInfo());
		Mockito.when(xxGroupDao.findByGroupName(Mockito.anyString())).thenReturn(xxGroup);
		Mockito.when(xGroupService.populateViewBean(xxGroup)).thenReturn(vxGroup);
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		XXUser xXUser = xxUser(vxUser);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.getById(userId)).thenReturn(xXUser);
		XXPortalUserDao xXPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xXPortalUserDao);
		Mockito.when(xXPortalUserDao.findByLoginId(xXUser.getName().trim())).thenReturn(xXPortalUser);
		XXPortalUserRoleDao xXPortalUserRoleDao = Mockito.mock(XXPortalUserRoleDao.class);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xXPortalUserRoleDao);
		vxGUInfoObjNull=xUserMgr.getXGroupUserFromMap(xxGroup.getName());
	}

	@Test
	public void test54modifyUserActiveStatus() {
		setup();
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		VXPortalUser userProfile = userProfile();
		VXUser vxUser = vxUser();
		XXUser xXUser = xxUser(vxUser);
		XXPortalUser xXPortalUser = xxPortalUser(userProfile);
		xXPortalUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.getById(xXUser.getId())).thenReturn(xXUser);
		Mockito.when(userMgr.updateUser(userProfile)).thenReturn(xXPortalUser);
		HashMap<Long, Integer> statusMap= new HashMap<Long, Integer>();
		statusMap.put(xXUser.getId(), 1);
		Mockito.when(userMgr.getUserProfileByLoginId(vxUser.getName())).thenReturn(userProfile);
		xUserMgr.modifyUserActiveStatus(statusMap);
	}

	@Test
	public void test55updateXGroupUser() {
		setup();
		VXUser vxUser = vxUser();
		vxUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		VXGroupUser vxGroupUser = vxGroupUser();
		Mockito.when(xGroupUserService.updateResource((VXGroupUser) Mockito.any())).thenReturn(vxGroupUser);
		VXGroupUser dbvxUser = xUserMgr.updateXGroupUser(vxGroupUser);
		Assert.assertNotNull(dbvxUser);
		Assert.assertEquals(dbvxUser.getId(), vxGroupUser.getId());
		Assert.assertEquals(dbvxUser.getName(), vxGroupUser.getName());
		Mockito.verify(xGroupUserService).updateResource((VXGroupUser) Mockito.any());
	}

	@Test
	public void test56createXGroupGroup() {
		setup();
		VXUser vxUser = vxUser();
		vxUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		VXGroupGroup vXGroupGroup = vxGroupGroup();
		Mockito.when(xGroupGroupService.createResource((VXGroupGroup) Mockito.any())).thenReturn(vXGroupGroup);
		VXGroupGroup dbvXGroupGroup = xUserMgr.createXGroupGroup(vXGroupGroup);
		Assert.assertNotNull(dbvXGroupGroup);
		Assert.assertEquals(dbvXGroupGroup.getId(), vXGroupGroup.getId());
		Assert.assertEquals(dbvXGroupGroup.getName(), vXGroupGroup.getName());
		Mockito.verify(xGroupGroupService).createResource((VXGroupGroup) Mockito.any());
	}

	@Test
	public void test57updateXGroupGroup() {
		setup();
		VXUser vxUser = vxUser();
		vxUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		VXGroupGroup vXGroupGroup = vxGroupGroup();
		Mockito.when(xGroupGroupService.updateResource((VXGroupGroup) Mockito.any())).thenReturn(vXGroupGroup);
		VXGroupGroup dbvXGroupGroup = xUserMgr.updateXGroupGroup(vXGroupGroup);
		Assert.assertNotNull(dbvXGroupGroup);
		Assert.assertEquals(dbvXGroupGroup.getId(), vXGroupGroup.getId());
		Assert.assertEquals(dbvXGroupGroup.getName(), vXGroupGroup.getName());
		Mockito.verify(xGroupGroupService).updateResource((VXGroupGroup) Mockito.any());
	}

	@Test
	public void test58deleteXGroupGroup() {
		setup();
		VXUser vxUser = vxUser();
		vxUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		VXGroupGroup vXGroupGroup = vxGroupGroup();
		Mockito.when(xGroupGroupService.deleteResource((Long) Mockito.any())).thenReturn(true);
		xUserMgr.deleteXGroupGroup(vXGroupGroup.getId(),true);
		Mockito.verify(xGroupGroupService).deleteResource((Long) Mockito.any());
	}

	@Test
	public void test59deleteXGroupUser() {
		setup();
		VXUser vxUser = vxUser();
		vxUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		VXGroupUser vXGroupUser = vxGroupUser();
		Mockito.when(xGroupUserService.deleteResource((Long) Mockito.any())).thenReturn(true);
		xUserMgr.deleteXGroupUser(vXGroupUser.getId(),true);
		Mockito.verify(xGroupUserService).deleteResource((Long) Mockito.any());
	}

	@Test
	public void test60postUserGroupAuditInfo() {
		setup();
		VXUgsyncAuditInfo vxUgsyncAuditInfo=new VXUgsyncAuditInfo();
		vxUgsyncAuditInfo.setId(userId);
		Mockito.when(xUgsyncAuditInfoService.createUgsyncAuditInfo((VXUgsyncAuditInfo) Mockito.any())).thenReturn(vxUgsyncAuditInfo);
		VXUgsyncAuditInfo dbVXUgsyncAuditInfo = xUserMgr.postUserGroupAuditInfo(vxUgsyncAuditInfo);
		Assert.assertNotNull(dbVXUgsyncAuditInfo);
		Assert.assertEquals(dbVXUgsyncAuditInfo.getId(), vxUgsyncAuditInfo.getId());
		Mockito.verify(xUgsyncAuditInfoService).createUgsyncAuditInfo((VXUgsyncAuditInfo) Mockito.any());
	}

	@Test
	public void test61createXGroupUser() {
		setup();
		VXUser vxUser = vxUser();
		vxUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		VXGroupUser vxGroupUser = vxGroupUser();
		Mockito.when(xGroupUserService.createXGroupUserWithOutLogin((VXGroupUser) Mockito.any())).thenReturn(vxGroupUser);
		VXGroupUser dbvxUser = xUserMgr.createXGroupUser(vxGroupUser);
		Assert.assertNotNull(dbvxUser);
		Assert.assertEquals(dbvxUser.getId(), vxGroupUser.getId());
		Assert.assertEquals(dbvxUser.getName(), vxGroupUser.getName());
		Mockito.verify(xGroupUserService).createXGroupUserWithOutLogin((VXGroupUser) Mockito.any());
	}

	@Test
	public void test62createXGroupUser() {
		setup();
		VXGroupUser vXGroupUser = vxGroupUser();
		VXGroup vxGroup=vxGroup();
		Mockito.when(xGroupService.readResource(userId)).thenReturn(vxGroup);
		Mockito.when(xGroupUserService.createResource((VXGroupUser) Mockito.any())).thenReturn(vXGroupUser);
		VXGroupUser dbVXGroupUser = xUserMgr.createXGroupUser(userId,vxGroup.getId());
		Assert.assertNotNull(dbVXGroupUser);
		Assert.assertEquals(userId, dbVXGroupUser.getId());
		Assert.assertEquals(dbVXGroupUser.getOwner(), vXGroupUser.getOwner());
		Assert.assertEquals(dbVXGroupUser.getName(), vXGroupUser.getName());
		Assert.assertEquals(dbVXGroupUser.getUserId(), vXGroupUser.getUserId());
		Assert.assertEquals(dbVXGroupUser.getUpdatedBy(),vXGroupUser.getUpdatedBy());
	}

	@Test
	public void test63searchXUsers_Cases() {
		VXUser vxUser = vxUser();
		VXUserList vXUserListSort = new VXUserList();
		List<VXUser> vXUsers = new ArrayList<VXUser>();
		vXUsers.add(vxUser);
		vXUserListSort.setVXUsers(vXUsers);
		String userName = vxUser.getName();
		SearchCriteria testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", userName);
		Mockito.when(xUserService.getXUserByUserName(userName)).thenReturn(vxUser);
		Mockito.when(xUserService.searchXUsers(testSearchCriteria)).thenReturn(vXUserListSort);
		VXGroupUserList vxGroupUserList = vxGroupUserList();
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		VXGroup vXGroup = vxGroup();
		Mockito.when(xGroupService.readResource(Mockito.anyLong())).thenReturn(vXGroup);
		VXUserList dbVXUserList = xUserMgr.searchXUsers(testSearchCriteria);
		Assert.assertNotNull(dbVXUserList);
		testSearchCriteria.addParam("isvisible", "true");
		dbVXUserList = xUserMgr.searchXUsers(testSearchCriteria);
		Assert.assertNotNull(dbVXUserList);
		testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", userName);
		testSearchCriteria.addParam("status", RangerCommonEnums.USER_EXTERNAL);
		Mockito.when(xUserService.searchXUsers(testSearchCriteria)).thenReturn(vXUserListSort);
		dbVXUserList = xUserMgr.searchXUsers(testSearchCriteria);
		Assert.assertNotNull(dbVXUserList);
		testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", userName);
		testSearchCriteria.addParam("usersource", 1L);
		Mockito.when(xUserService.searchXUsers(testSearchCriteria)).thenReturn(vXUserListSort);
		dbVXUserList = xUserMgr.searchXUsers(testSearchCriteria);
		Assert.assertNotNull(dbVXUserList);
		testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", userName);
		testSearchCriteria.addParam("emailaddress", "new"+vxUser.getEmailAddress());
		Mockito.when(xUserService.searchXUsers(testSearchCriteria)).thenReturn(vXUserListSort);
		dbVXUserList = xUserMgr.searchXUsers(testSearchCriteria);
		Assert.assertNotNull(dbVXUserList);
		testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", userName);
		testSearchCriteria.addParam("userrole", RangerConstants.ROLE_USER);
		Mockito.when(xUserService.searchXUsers(testSearchCriteria)).thenReturn(vXUserListSort);
		dbVXUserList = xUserMgr.searchXUsers(testSearchCriteria);
		Assert.assertNotNull(dbVXUserList);
		testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", userName);
		testSearchCriteria.addParam("userrolelist", vxUser.getUserRoleList());
		Mockito.when(xUserService.searchXUsers(testSearchCriteria)).thenReturn(vXUserListSort);
		dbVXUserList = xUserMgr.searchXUsers(testSearchCriteria);
		Assert.assertNotNull(dbVXUserList);
	}

	@Test
	public void test64checkAccessRolesAdmin() {
		destroySession();
		setup();
		List<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_KEY_ADMIN");
		Mockito.when(restErrorUtil.create403RESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.checkAccessRoles(userRoleList);
	}

	@Test
	public void test65checkAccessRolesKeyAdmin() {
		destroySession();
		List<String> userRoleList = new ArrayList<String>();
		setupKeyAdmin();
		userRoleList.add("ROLE_SYS_ADMIN");
		Mockito.when(restErrorUtil.create403RESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.checkAccessRoles(userRoleList);
	}

	@Test
	public void test66checkAccessRolesUser() {
		destroySession();
		setupUser();
		List<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		Mockito.when(restErrorUtil.create403RESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.checkAccessRoles(userRoleList);
	}

	@Test
	public void test67checkAccessRolesUser() {
		destroySession();
		List<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		VXResponse vXResponse = new VXResponse();
		vXResponse.setStatusCode(HttpServletResponse.SC_UNAUTHORIZED);
		vXResponse.setMsgDesc("Bad Credentials");
		Mockito.when(restErrorUtil.generateRESTException((VXResponse) Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.checkAccessRoles(userRoleList);
	}

	@Test
	public void test68getGroupByGroupName() {
		destroySession();
		VXGroup vxGroup=vxGroup();
		Mockito.when(xGroupService.getGroupByGroupName(vxGroup.getName())).thenReturn(vxGroup);
		VXGroup vxGroup1=xUserMgr.getGroupByGroupName(vxGroup.getName());
		Assert.assertNotNull(vxGroup1);
		Mockito.when(xGroupService.getGroupByGroupName(Mockito.anyString())).thenReturn(null);
		Mockito.when(restErrorUtil.createRESTException(vxGroup.getName() + " is Not Found", MessageEnums.DATA_NOT_FOUND)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		VXGroup vxGroup2=xUserMgr.getGroupByGroupName(vxGroup.getName());
		Assert.assertNull(vxGroup2);
	}

	@Test
	public void test69denySelfRoleChange() {
		destroySession();
		setupUser();
		Mockito.when(restErrorUtil.create403RESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.denySelfRoleChange(userProfile().getLoginId());
	}

	@Test
	public void test70denySelfRoleChange() {
		destroySession();
		setup();
		Mockito.when(restErrorUtil.create403RESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.denySelfRoleChange(adminLoginID);
	}

	@Test
	public void test71denySelfRoleChange() {
		destroySession();
		setupKeyAdmin();
		Mockito.when(restErrorUtil.create403RESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.denySelfRoleChange(keyadminLoginID);
	}

	@Test
	public void test72UpdateXUser() {
		destroySession();
		setup();
		Collection<String> existingRoleList = new ArrayList<String>();
		existingRoleList.add(RangerConstants.ROLE_USER);
		Collection<String> reqRoleList = new ArrayList<String>();
		reqRoleList.add(RangerConstants.ROLE_SYS_ADMIN);
		Collection<Long> groupIdList = new ArrayList<Long>();
		groupIdList.add(userId);
		VXUser vxUser = vxUser();
		vxUser.setUserRoleList(reqRoleList);
		vxUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		vxUser.setGroupIdList(groupIdList);
		vxUser.setFirstName("user1");
		vxUser.setLastName("null");
		vxUser.setPassword("*****");
		Mockito.when(xUserService.updateResource(vxUser)).thenReturn(vxUser);
		VXPortalUser oldUserProfile = userProfile();
		oldUserProfile.setUserSource(RangerCommonEnums.USER_APP);
		oldUserProfile.setPassword(vxUser.getPassword());
		VXPortalUser vXPortalUser = userProfile();
		vXPortalUser.setUserRoleList(existingRoleList);
		Mockito.when(userMgr.getUserProfileByLoginId(vxUser.getName())).thenReturn(oldUserProfile);
		XXPortalUser xXPortalUser = xxPortalUser(vXPortalUser);
		Mockito.when(userMgr.updateUserWithPass((VXPortalUser) Mockito.any())).thenReturn(xXPortalUser);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = new XXUserPermission();
		xUserPermissionObj.setAddedByUserId(userId);
		xUserPermissionObj.setCreateTime(new Date());
		xUserPermissionObj.setId(userId);
		xUserPermissionObj.setIsAllowed(1);
		xUserPermissionObj.setModuleId(1L);
		xUserPermissionObj.setUpdatedByUserId(userId);
		xUserPermissionObj.setUpdateTime(new Date());
		xUserPermissionObj.setUserId(userId);
		xUserPermissionsList.add(xUserPermissionObj);
		VXGroupUserList vxGroupUserList = vxGroupUserList();
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		VXUser dbvxUser = xUserMgr.updateXUser(vxUser);
		Assert.assertNotNull(dbvxUser);
		Assert.assertEquals(dbvxUser.getId(), vxUser.getId());
		Assert.assertEquals(dbvxUser.getDescription(), vxUser.getDescription());
		Assert.assertEquals(dbvxUser.getName(), vxUser.getName());
		Mockito.verify(xUserService).updateResource(vxUser);

		groupIdList.clear();
		groupIdList.add(9L);
		vxUser.setGroupIdList(groupIdList);
		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		Mockito.when(xGroupUserService.getTransactionLog((VXGroupUser) Mockito.any(), Mockito.anyString())).thenReturn(trxLogList);
		vxUser.setPassword("TestUser@1234");
		oldUserProfile.setPassword(vxUser.getPassword());
		vxGroupUserList.setVXGroupUsers(null);
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		VXGroup vXGroup = vxGroup();
		Mockito.when(xGroupService.readResource(Mockito.anyLong())).thenReturn(vXGroup);
		VXGroupUser vXGroupUser = vxGroupUser();
		Mockito.when(xGroupUserService.createResource((VXGroupUser) Mockito.any())).thenReturn(vXGroupUser);
		dbvxUser = xUserMgr.updateXUser(vxUser);
		Assert.assertNotNull(dbvxUser);

		Mockito.when(userMgr.getUserProfileByLoginId(Mockito.anyString())).thenReturn(null);
		Mockito.when(restErrorUtil.createRESTException("user " + vxUser.getName() + " does not exist.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		vxUser=xUserMgr.updateXUser(vxUser);
		Assert.assertNull(vxUser);
	}

	@Test
	public void test73restrictSelfAccountDeletion() {
		destroySession();
		setupUser();
		Mockito.when(restErrorUtil.generateRESTException((VXResponse)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.restrictSelfAccountDeletion(userProfile().getLoginId());
	}

	@Test
	public void test74restrictSelfAccountDeletion() {
		destroySession();
		setup();
		Mockito.when(restErrorUtil.generateRESTException((VXResponse)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.restrictSelfAccountDeletion(adminLoginID);
	}

	@Test
	public void test75restrictSelfAccountDeletion() {
		destroySession();
		setupKeyAdmin();
		Mockito.when(restErrorUtil.generateRESTException((VXResponse)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.restrictSelfAccountDeletion(keyadminLoginID);
	}

	@Test
	public void test76restrictSelfAccountDeletion() {
		destroySession();
		Mockito.when(restErrorUtil.generateRESTException((VXResponse)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.restrictSelfAccountDeletion(userProfile().getLoginId());
	}

	@Test
	public void test77updateUserRolesPermissions() {
		setup();
		List<String> existingRoleList = new ArrayList<String>();
		existingRoleList.add(RangerConstants.ROLE_USER);
		List<String> reqRoleList = new ArrayList<String>();
		reqRoleList.add(RangerConstants.ROLE_SYS_ADMIN);
		Collection<Long> groupIdList = new ArrayList<Long>();
		groupIdList.add(userId);
		VXUser vxUser = vxUser();
		vxUser.setUserRoleList(reqRoleList);
		vxUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		vxUser.setGroupIdList(groupIdList);
		vxUser.setFirstName("null");
		vxUser.setLastName("null");
		vxUser.setPassword("*****");
		VXPortalUser oldUserProfile = userProfile();
		oldUserProfile.setUserSource(RangerCommonEnums.USER_APP);
		oldUserProfile.setPassword(vxUser.getPassword());
		oldUserProfile.setUserRoleList(existingRoleList);
		VXPortalUser vXPortalUser = userProfile();
		vXPortalUser.setUserRoleList(reqRoleList);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = new XXUserPermission();
		xUserPermissionObj.setAddedByUserId(userId);
		xUserPermissionObj.setCreateTime(new Date());
		xUserPermissionObj.setId(userId);
		xUserPermissionObj.setIsAllowed(1);
		xUserPermissionObj.setModuleId(1L);
		xUserPermissionObj.setUpdatedByUserId(userId);
		xUserPermissionObj.setUpdateTime(new Date());
		xUserPermissionObj.setUserId(userId);
		xUserPermissionsList.add(xUserPermissionObj);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		XXModuleDefDao xXModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXUserPermissionDao xXUserPermissionDao= Mockito.mock(XXUserPermissionDao.class);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		vXPortalUser.setUserRoleList(userRoleList);
		XXUser xXUser = xxUser(vxUser);
		List<XXModuleDef> xXModuleDefs = xxModuleDefs();
		VXUserPermission userPermission = vxUserPermission();
		List<VXUserPermission> userPermList = new ArrayList<VXUserPermission>();
		userPermList.add(userPermission);
		xUserPermissionObj.setModuleId(userPermission.getModuleId());
		xUserPermissionObj.setUserId(userPermission.getUserId());
		xUserPermissionsList.add(xUserPermissionObj);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xXModuleDefDao);
		Mockito.when(xXModuleDefDao.getAll()).thenReturn(xXModuleDefs);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xXUserPermissionDao);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByPortalUserId(vXPortalUser.getId())).thenReturn(xXUser);
		Mockito.when(xXUserPermissionDao.findByUserPermissionId(vXPortalUser.getId())).thenReturn(xUserPermissionsList);
		Mockito.when(xUserPermissionService.createResource((VXUserPermission) Mockito.any())).thenReturn(userPermission);
		Mockito.when(sessionMgr.getActiveUserSessionsForPortalUserId(userId)).thenReturn(userSessions);
		xUserMgr.updateUserRolesPermissions(oldUserProfile,reqRoleList);
	}

	@Test
	public void test78checkAccess() {
		destroySession();
		setupUser();
		Mockito.when(restErrorUtil.create403RESTException(Mockito.anyString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.checkAccess("testuser2");
	}

	@Test
	public void test79checkAccess() {
		destroySession();
		Mockito.when(restErrorUtil.generateRESTException((VXResponse)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		VXPortalUser vXPortalUser = userProfile();
		xUserMgr.checkAccess(vXPortalUser.getLoginId());
	}

	@Test
	public void test80checkAdminAccess() {
		destroySession();
		setupUser();
		Mockito.when(restErrorUtil.generateRESTException((VXResponse)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.checkAdminAccess();
	}

	@Test
	public void test81checkAdminAccess() {
		destroySession();
		Mockito.when(restErrorUtil.generateRESTException((VXResponse)Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.checkAdminAccess();
	}

	@Test
	public void test82updateXgroupUserForGroupUpdate() {
		setup();
		XXGroupUserDao xxGroupUserDao = Mockito.mock(XXGroupUserDao.class);
		VXGroup vXGroup = vxGroup();
		List<XXGroupUser> xXGroupUserList = new ArrayList<XXGroupUser>();
		VXGroupUser vxGroupUser = vxGroupUser();
		XXGroupUser xXGroupUser =new XXGroupUser();
		xXGroupUser.setId(vxGroupUser.getId());
		xXGroupUser.setName(vxGroupUser.getName());
		xXGroupUser.setParentGroupId(vxGroupUser.getParentGroupId());
		xXGroupUser.setUserId(vxGroupUser.getUserId());
		xXGroupUserList.add(xXGroupUser);
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(xxGroupUserDao);
		Mockito.when(xxGroupUserDao.findByGroupId(vXGroup.getId())).thenReturn(xXGroupUserList);
		Mockito.when(xGroupUserService.populateViewBean(xXGroupUser)).thenReturn(vxGroupUser);
		xUserMgr.updateXgroupUserForGroupUpdate(vXGroup);
		Mockito.verify(daoManager).getXXGroupUser();
		Mockito.verify(xxGroupUserDao).findByGroupId(vXGroup.getId());
	}

	@Test
	public void test83validatePassword() {
		destroySession();
		setup();
		VXUser vxUser = vxUser();
		vxUser.setPassword(null);
		Mockito.when(restErrorUtil.createRESTException("serverMsg.xuserMgrValidatePassword", MessageEnums.INVALID_PASSWORD, null, "Password cannot be blank/null", null)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.validatePassword(vxUser);
	}

	@Test
	public void test84validatePassword() {
		setup();
		VXUser vxUser = vxUser();
		xUserMgr.validatePassword(vxUser);
	}

	@Test
	public void test85validatePassword() {
		destroySession();
		setup();
		VXUser vxUser = vxUser();
		vxUser.setPassword("password");
		Mockito.when(restErrorUtil.createRESTException("serverMsg.xuserMgrValidatePassword", MessageEnums.INVALID_PASSWORD, null, "Password should be minimum 8 characters, at least one uppercase letter, one lowercase letter and one numeric.", null)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.validatePassword(vxUser);
	}

	@Test
	public void test86deleteXPermMap() {
		setup();
		VXResource vxresource=new VXResource();
		XXPermMapDao xXPermMapDao = Mockito.mock(XXPermMapDao.class);
		Mockito.when(daoManager.getXXPermMap()).thenReturn(xXPermMapDao);
		VXPermMap vXPermMap1=getVXPermMap();
		XXPermMap xXPermMap1=new XXPermMap();
		xXPermMap1.setId(vXPermMap1.getId());
		xXPermMap1.setResourceId(vXPermMap1.getResourceId());
		Mockito.when(xXPermMapDao.getById(xXPermMap1.getId())).thenReturn(xXPermMap1);
		Mockito.when(xResourceService.readResource(xXPermMap1.getResourceId())).thenReturn(vxresource);
		Mockito.when(xPermMapService.deleteResource(Mockito.anyLong())).thenReturn(true);
		xUserMgr.deleteXPermMap(vXPermMap1.getId(),true);
	}

	@Test
	public void test87deleteXPermMap() {
		destroySession();
		setup();
		VXResource vxresource=new VXResource();
		XXPermMapDao xXPermMapDao = Mockito.mock(XXPermMapDao.class);
		Mockito.when(daoManager.getXXPermMap()).thenReturn(xXPermMapDao);
		VXPermMap vXPermMap1=getVXPermMap();
		XXPermMap xXPermMap1=new XXPermMap();
		xXPermMap1.setId(vXPermMap1.getId());
		xXPermMap1.setResourceId(vXPermMap1.getResourceId());
		Mockito.when(xXPermMapDao.getById(xXPermMap1.getId())).thenReturn(xXPermMap1);
		Mockito.when(xResourceService.readResource(xXPermMap1.getResourceId())).thenReturn(vxresource);
		Mockito.when(xResourceService.readResource(xXPermMap1.getResourceId())).thenReturn(null);
		Mockito.when(restErrorUtil.createRESTException("Invalid Input Data - No resource found with Id: " + xXPermMap1.getResourceId(), MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.deleteXPermMap(vXPermMap1.getId(),true);
	}

	@Test
	public void test88deleteXPermMap() {
		destroySession();
		setup();
		VXPermMap vXPermMap1=getVXPermMap();
		XXPermMap xXPermMap1=new XXPermMap();
		xXPermMap1.setId(vXPermMap1.getId());
		xXPermMap1.setResourceId(vXPermMap1.getResourceId());
		Mockito.when(restErrorUtil.createRESTException("serverMsg.modelMgrBaseDeleteModel", MessageEnums.OPER_NOT_ALLOWED_FOR_ENTITY)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.deleteXPermMap(vXPermMap1.getId(),false);
	}

	@Test
	public void test89deleteXAuditMap() {
		destroySession();
		setup();
		VXResource vxresource=new VXResource();
		XXAuditMapDao xXAuditMapDao = Mockito.mock(XXAuditMapDao.class);
		Mockito.when(daoManager.getXXAuditMap()).thenReturn(xXAuditMapDao);
		VXAuditMap vXAuditMap=getVXAuditMap();
		XXAuditMap xXAuditMap=new XXAuditMap();
		xXAuditMap.setId(vXAuditMap.getId());
		xXAuditMap.setResourceId(vXAuditMap.getResourceId());
		Mockito.when(xXAuditMapDao.getById(vXAuditMap.getId())).thenReturn(xXAuditMap);
		Mockito.when(xResourceService.readResource(xXAuditMap.getResourceId())).thenReturn(vxresource);
		Mockito.when(xAuditMapService.deleteResource(Mockito.anyLong())).thenReturn(true);
		xUserMgr.deleteXAuditMap(vXAuditMap.getId(),true);
		Mockito.when(xResourceService.readResource(xXAuditMap.getResourceId())).thenReturn(null);
		Mockito.when(restErrorUtil.createRESTException("Invalid Input Data - No resource found with Id: " + xXAuditMap.getResourceId(), MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.deleteXAuditMap(vXAuditMap.getId(),true);
	}

	@Test
	public void test90getXPermMapSearchCount() {
		SearchCriteria testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("xUserId", userId);
		VXPermMap vXPermMap=getVXPermMap();
		List<VXPermMap> vXPermMapList=new ArrayList<VXPermMap>();
		vXPermMapList.add(vXPermMap);
		VXPermMapList permMapList=new VXPermMapList();
		permMapList.setVXPermMaps(vXPermMapList);
		Mockito.when(xPermMapService.searchXPermMaps((SearchCriteria) Mockito.any())).thenReturn(permMapList);
		VXLong vXLong = new VXLong();
		vXLong.setValue(permMapList.getListSize());
		VXLong vXLong1=xUserMgr.getXPermMapSearchCount(testSearchCriteria);
		Assert.assertEquals(vXLong.getValue(), vXLong1.getValue());
	}

	@Test
	public void test91getXAuditMapSearchCount() {
		SearchCriteria testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("xUserId", userId);
		VXAuditMap vXAuditMap=getVXAuditMap();
		List<VXAuditMap> vXAuditMapList=new ArrayList<VXAuditMap>();
		vXAuditMapList.add(vXAuditMap);
		VXAuditMapList auditMapList=new VXAuditMapList();
		auditMapList.setVXAuditMaps(vXAuditMapList);
		Mockito.when(xAuditMapService.searchXAuditMaps((SearchCriteria) Mockito.any())).thenReturn(auditMapList);
		VXLong vXLong = new VXLong();
		vXLong.setValue(auditMapList.getListSize());
		VXLong vXLong1=xUserMgr.getXAuditMapSearchCount(testSearchCriteria);
		Assert.assertEquals(vXLong.getValue(), vXLong1.getValue());
	}

	@Test
	public void test92searchXPermMap() {
		SearchCriteria testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("xUserId", userId);
		VXResource vxresource=new VXResource();
		VXPermMap vXPermMap=getVXPermMap();
		List<VXPermMap> vXPermMapList=new ArrayList<VXPermMap>();
		vXPermMapList.add(vXPermMap);
		VXPermMapList permMapList=new VXPermMapList();
		permMapList.setVXPermMaps(vXPermMapList);
		Mockito.when(xPermMapService.searchXPermMaps((SearchCriteria) Mockito.any())).thenReturn(permMapList);
		List<XXResource> xResourceList = new ArrayList<XXResource>();
		XXResource xRes = new XXResource();
		xRes.setId(userId);
		xRes.setName("hadoopdev");
		xRes.setIsRecursive(AppConstants.BOOL_TRUE);
		xRes.setResourceStatus(AppConstants.STATUS_ENABLED);
		xResourceList.add(xRes);
		XXResourceDao xxResourceDao = Mockito.mock(XXResourceDao.class);
		Mockito.when(daoManager.getXXResource()).thenReturn(xxResourceDao);
		Mockito.when(xxResourceDao.getById(Mockito.anyLong())).thenReturn(xRes);
		Mockito.when(xResourceService.populateViewBean(xRes)).thenReturn(vxresource);
		VXResponse vXResponse = new VXResponse();
		vXResponse.setStatusCode(VXResponse.STATUS_SUCCESS);
		Mockito.when(msBizUtil.hasPermission(vxresource, AppConstants.XA_PERM_TYPE_ADMIN)).thenReturn(vXResponse);
		VXPermMapList returnList=xUserMgr.searchXPermMaps(testSearchCriteria);
		Assert.assertNotNull(returnList);
		Assert.assertEquals(permMapList.getListSize(), returnList.getListSize());
	}

	@Test
	public void test93searchXAuditMap() {
		SearchCriteria testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("xUserId", userId);
		VXResource vxresource=new VXResource();
		VXAuditMap vXAuditMap=getVXAuditMap();
		List<VXAuditMap> vXAuditMapList=new ArrayList<VXAuditMap>();
		vXAuditMapList.add(vXAuditMap);
		VXAuditMapList auditMapList=new VXAuditMapList();
		auditMapList.setVXAuditMaps(vXAuditMapList);
		Mockito.when(xAuditMapService.searchXAuditMaps((SearchCriteria) Mockito.any())).thenReturn(auditMapList);
		List<XXResource> xResourceList = new ArrayList<XXResource>();
		XXResource xRes = new XXResource();
		xRes.setId(userId);
		xRes.setName("hadoopdev");
		xRes.setIsRecursive(AppConstants.BOOL_TRUE);
		xRes.setResourceStatus(AppConstants.STATUS_ENABLED);
		xResourceList.add(xRes);
		XXResourceDao xxResourceDao = Mockito.mock(XXResourceDao.class);
		Mockito.when(daoManager.getXXResource()).thenReturn(xxResourceDao);
		Mockito.when(xxResourceDao.getById(Mockito.anyLong())).thenReturn(xRes);
		Mockito.when(xResourceService.populateViewBean(xRes)).thenReturn(vxresource);
		VXResponse vXResponse = new VXResponse();
		vXResponse.setStatusCode(VXResponse.STATUS_SUCCESS);
		Mockito.when(msBizUtil.hasPermission(vxresource, AppConstants.XA_PERM_TYPE_ADMIN)).thenReturn(vXResponse);
		VXAuditMapList returnList=xUserMgr.searchXAuditMaps(testSearchCriteria);
		Assert.assertNotNull(returnList);
		Assert.assertEquals(auditMapList.getListSize(), returnList.getListSize());
	}

	@Test
	public void test94DeleteXUser() {
		setup();
		boolean force = false;
		VXUser vXUser = vxUser();
		XXUser xXUser = new XXUser();
		XXUserDao xXUserDao = Mockito.mock(XXUserDao.class);
		Mockito.when(daoManager.getXXUser()).thenReturn(xXUserDao);
		Mockito.when(xXUserDao.getById(vXUser.getId())).thenReturn(xXUser);
		Mockito.when(xUserService.populateViewBean(xXUser)).thenReturn(vXUser);
		VXGroupUserList vxGroupUserList=new VXGroupUserList();
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		VXAuditMapList vXAuditMapList = new VXAuditMapList();
		Mockito.when(xAuditMapService.searchXAuditMaps((SearchCriteria) Mockito.any())).thenReturn(vXAuditMapList);
		VXPortalUser vXPortalUser = userProfile();
		XXPortalUser xXPortalUser = xxPortalUser(vXPortalUser);
		XXPortalUserDao xXPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xXPortalUserDao);
		Mockito.when(xXPortalUserDao.findByLoginId(vXUser.getName().trim())).thenReturn(xXPortalUser);
		Mockito.when(xPortalUserService.populateViewBean(xXPortalUser)).thenReturn(vXPortalUser);
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		XXAuthSessionDao xXAuthSessionDao= Mockito.mock(XXAuthSessionDao.class);
		XXUserPermissionDao xXUserPermissionDao= Mockito.mock(XXUserPermissionDao.class);
		XXPortalUserRoleDao xXPortalUserRoleDao= Mockito.mock(XXPortalUserRoleDao.class);
		Mockito.when(daoManager.getXXAuthSession()).thenReturn(xXAuthSessionDao);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xXUserPermissionDao);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xXPortalUserRoleDao);
		List<XXAuthSession> xXAuthSessions=new ArrayList<XXAuthSession>();
		XXAuthSession xXAuthSession = new XXAuthSession();
		xXAuthSession.setId(userId);
		xXAuthSession.setLoginId(vXPortalUser.getLoginId());
		List<XXUserPermission> xXUserPermissions=new ArrayList<XXUserPermission>();
		List<XXPortalUserRole> xXPortalUserRoles=new ArrayList<XXPortalUserRole>();
		Mockito.when(xXAuthSessionDao.getAuthSessionByUserId(vXPortalUser.getId())).thenReturn(xXAuthSessions);
		Mockito.when(xXUserPermissionDao.findByUserPermissionId(vXPortalUser.getId())).thenReturn(xXUserPermissions);
		Mockito.when(xXPortalUserRoleDao.findByUserId(vXPortalUser.getId())).thenReturn(xXPortalUserRoles);
		XXPolicyDao xXPolicyDao = Mockito.mock(XXPolicyDao.class);
		List<XXPolicy> xXPolicyList = new ArrayList<XXPolicy>();
		Mockito.when(daoManager.getXXPolicy()).thenReturn(xXPolicyDao);
		Mockito.when(xXPolicyDao.findByUserId(vXUser.getId())).thenReturn(xXPolicyList);
		List<XXSecurityZoneRefUser> zoneSecRefUser=new ArrayList<XXSecurityZoneRefUser>();
	    XXSecurityZoneRefUserDao zoneSecRefUserDao=Mockito.mock(XXSecurityZoneRefUserDao.class);
	    Mockito.when(daoManager.getXXSecurityZoneRefUser()).thenReturn(zoneSecRefUserDao);
	    Mockito.when(zoneSecRefUserDao.findByUserId(userId)).thenReturn(zoneSecRefUser);
	    List<XXRoleRefUser> roleRefUser=new ArrayList<XXRoleRefUser>();
	    XXRoleRefUserDao roleRefUserDao=Mockito.mock(XXRoleRefUserDao.class);
	    Mockito.when(daoManager.getXXRoleRefUser()).thenReturn(roleRefUserDao);
	    Mockito.when(roleRefUserDao.findByUserId(userId)).thenReturn(roleRefUser);
		xUserMgr.deleteXUser(vXUser.getId(), force);
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(new VXGroupUserList());
		XXPolicy xXPolicy=getXXPolicy();
		xXPolicyList.add(xXPolicy);
		Mockito.when(xXPolicyDao.findByUserId(userId)).thenReturn(xXPolicyList);
		xUserMgr.deleteXUser(vXUser.getId(), force);
		Mockito.when(xXPolicyDao.findByUserId(userId)).thenReturn(new ArrayList<XXPolicy>());
		VXPermMapList vXPermMapList = new VXPermMapList();
		VXPermMap vXPermMap1=getVXPermMap();
		List<VXPermMap> vXPermMaps=new ArrayList<VXPermMap>();
		vXPermMaps.add(vXPermMap1);
		vXPermMapList.setVXPermMaps(vXPermMaps);
		Mockito.when(xPermMapService.searchXPermMaps((SearchCriteria) Mockito.any())).thenReturn(vXPermMapList);
		xUserMgr.deleteXUser(vXUser.getId(), force);
		Mockito.when(xPermMapService.searchXPermMaps((SearchCriteria) Mockito.any())).thenReturn(new VXPermMapList());
		List<VXAuditMap> vXAuditMaps=new ArrayList<VXAuditMap>();
		VXAuditMap vXAuditMap=getVXAuditMap();
		vXAuditMaps.add(vXAuditMap);
		vXAuditMapList.setVXAuditMaps(vXAuditMaps);
		Mockito.when(xAuditMapService.searchXAuditMaps((SearchCriteria) Mockito.any())).thenReturn(vXAuditMapList);
		xUserMgr.deleteXUser(vXUser.getId(), force);
		Mockito.when(xAuditMapService.searchXAuditMaps((SearchCriteria) Mockito.any())).thenReturn(new VXAuditMapList());
		xXAuthSessions.add(xXAuthSession);
		Mockito.when(xXAuthSessionDao.getAuthSessionByUserId(vXPortalUser.getId())).thenReturn(xXAuthSessions);
		xUserMgr.deleteXUser(vXUser.getId(), force);
		Mockito.when(xXAuthSessionDao.getAuthSessionByUserId(vXPortalUser.getId())).thenReturn(new ArrayList<XXAuthSession>());
		XXUserPermission xUserPermissionObj=xxUserPermission();
		xXUserPermissions.add(xUserPermissionObj);
		Mockito.when(xXUserPermissionDao.findByUserPermissionId(vXPortalUser.getId())).thenReturn(xXUserPermissions);
		xUserMgr.deleteXUser(vXUser.getId(), force);
		Mockito.when(xXUserPermissionDao.findByUserPermissionId(vXPortalUser.getId())).thenReturn(new ArrayList<XXUserPermission>());
		xXPortalUserRoles.add(XXPortalUserRole);
		Mockito.when(xXPortalUserRoleDao.findByUserId(vXPortalUser.getId())).thenReturn(xXPortalUserRoles);
		xUserMgr.deleteXUser(vXUser.getId(), force);
		Mockito.when(xXPortalUserRoleDao.findByUserId(vXPortalUser.getId())).thenReturn(new ArrayList<XXPortalUserRole>());
		xUserMgr.deleteXUser(vXUser.getId(), force);

		vXUser.setName("");
		Mockito.when(xXUserDao.getById(vXUser.getId())).thenReturn(xXUser);
		Mockito.when(xUserService.populateViewBean(xXUser)).thenReturn(vXUser);
		thrown.expect(NullPointerException.class);
		xUserMgr.deleteXUser(vXUser.getId(), force);
	}

	@Test
	public void test95DeleteXGroup() {
		setup();
		boolean force = false;
		VXGroup vXGroup = vxGroup();
		XXGroupDao xXGroupDao = Mockito.mock(XXGroupDao.class);
		XXGroup xXGroup = new XXGroup();
		Mockito.when(daoManager.getXXGroup()).thenReturn(xXGroupDao);
		Mockito.when(xXGroupDao.getById(vXGroup.getId())).thenReturn(xXGroup);
		Mockito.when(xGroupService.populateViewBean(xXGroup)).thenReturn(vXGroup);
		VXGroupUserList vxGroupUserList =vxGroupUserList();
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(vxGroupUserList);
		VXPermMapList vXPermMapList = new VXPermMapList();
		VXPermMap vXPermMap1=getVXPermMap();
		List<VXPermMap> vXPermMaps=new ArrayList<VXPermMap>();
		vXPermMaps.add(vXPermMap1);
		VXAuditMapList vXAuditMapList = new VXAuditMapList();
		List<VXAuditMap> vXAuditMaps=new ArrayList<VXAuditMap>();
		VXAuditMap vXAuditMap=getVXAuditMap();
		vXAuditMaps.add(vXAuditMap);
		XXGroupGroupDao xXGroupGroupDao = Mockito.mock(XXGroupGroupDao.class);
		List<XXGroupGroup> xXGroupGroups = new ArrayList<XXGroupGroup>();
		Mockito.when(daoManager.getXXGroupGroup()).thenReturn(xXGroupGroupDao);
		Mockito.when(xXGroupGroupDao.findByGroupId(userId)).thenReturn(xXGroupGroups);
		XXGroupPermissionDao xXGroupPermissionDao= Mockito.mock(XXGroupPermissionDao.class);
		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xXGroupPermissionDao);
		List<XXGroupPermission> xXGroupPermissions=new ArrayList<XXGroupPermission>();
		Mockito.when(xXGroupPermissionDao.findByGroupId(vXGroup.getId())).thenReturn(xXGroupPermissions);
		XXPolicyDao xXPolicyDao = Mockito.mock(XXPolicyDao.class);
		List<XXPolicy> xXPolicyList = new ArrayList<XXPolicy>();
		Mockito.when(daoManager.getXXPolicy()).thenReturn(xXPolicyDao);
		List<XXResource> xResourceList = new ArrayList<XXResource>();
		List<XXSecurityZoneRefGroup> zoneSecRefGroup=new ArrayList<XXSecurityZoneRefGroup>();
	    XXSecurityZoneRefGroupDao zoneSecRefGroupDao=Mockito.mock(XXSecurityZoneRefGroupDao.class);
	    Mockito.when(daoManager.getXXSecurityZoneRefGroup()).thenReturn(zoneSecRefGroupDao);
	    List<XXRoleRefGroup> roleRefGroup=new ArrayList<XXRoleRefGroup>();
	    XXRoleRefGroupDao roleRefGroupDao = Mockito.mock(XXRoleRefGroupDao.class);
	    Mockito.when(daoManager.getXXRoleRefGroup()).thenReturn(roleRefGroupDao);
	    Mockito.when(zoneSecRefGroupDao.findByGroupId(userId)).thenReturn(zoneSecRefGroup);
	    Mockito.when(roleRefGroupDao.findByGroupId(userId)).thenReturn(roleRefGroup);
		XXResource xXResource = new XXResource();
		xXResource.setId(userId);
		xXResource.setName("hadoopdev");
		xXResource.setIsRecursive(AppConstants.BOOL_TRUE);
		xXResource.setResourceStatus(AppConstants.STATUS_ENABLED);
		xResourceList.add(xXResource);
		xUserMgr.deleteXGroup(vXGroup.getId(), force);
		Mockito.when(xGroupUserService.searchXGroupUsers((SearchCriteria) Mockito.any())).thenReturn(new VXGroupUserList());
		XXPolicy xXPolicy=getXXPolicy();
		xXPolicyList.add(xXPolicy);
		Mockito.when(xXPolicyDao.findByGroupId(userId)).thenReturn(xXPolicyList);
		xUserMgr.deleteXGroup(vXGroup.getId(), force);
		Mockito.when(xXPolicyDao.findByGroupId(userId)).thenReturn(new ArrayList<XXPolicy>());
		vXPermMapList.setVXPermMaps(vXPermMaps);
		Mockito.when(xPermMapService.searchXPermMaps((SearchCriteria) Mockito.any())).thenReturn(vXPermMapList);
		xUserMgr.deleteXGroup(vXGroup.getId(), force);
		Mockito.when(xPermMapService.searchXPermMaps((SearchCriteria) Mockito.any())).thenReturn(new VXPermMapList());
		vXAuditMapList.setVXAuditMaps(vXAuditMaps);
		Mockito.when(xAuditMapService.searchXAuditMaps((SearchCriteria) Mockito.any())).thenReturn(vXAuditMapList);
		xUserMgr.deleteXGroup(vXGroup.getId(), force);
		Mockito.when(xAuditMapService.searchXAuditMaps((SearchCriteria) Mockito.any())).thenReturn(new VXAuditMapList());
		XXGroupGroup xXGroupGroup = xxGroupGroup();
		xXGroupGroups.add(xXGroupGroup);
		Mockito.when(xXGroupGroupDao.findByGroupId(userId)).thenReturn(xXGroupGroups);
		xUserMgr.deleteXGroup(vXGroup.getId(), force);
		Mockito.when(xXGroupGroupDao.findByGroupId(userId)).thenReturn(new ArrayList<XXGroupGroup>());
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xXGroupPermissions.add(xGroupPermissionObj);
		Mockito.when(xXGroupPermissionDao.findByGroupId(vXGroup.getId())).thenReturn(xXGroupPermissions);
		xUserMgr.deleteXGroup(vXGroup.getId(), force);
		Mockito.when(xXGroupPermissionDao.findByGroupId(vXGroup.getId())).thenReturn(new ArrayList<XXGroupPermission>());
		xUserMgr.deleteXGroup(vXGroup.getId(), force);
		Mockito.when(xGroupService.populateViewBean(xXGroup)).thenReturn(null);
		Mockito.when(restErrorUtil.createRESTException("Group ID doesn't exist.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.deleteXGroup(vXGroup.getId(), force);
	}

	@Test
	public void test96updateXModuleDefPermission() {
		XXModuleDefDao xModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXModuleDef xModuleDef = xxModuleDef();
		VXModuleDef vXModuleDef = vxModuleDef();
		Mockito.when(xModuleDefService.updateResource(vXModuleDef)).thenReturn(vXModuleDef);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		Mockito.when(xModuleDefDao.getById(userId)).thenReturn(xModuleDef);

		Map<Long, String> xXGroupNameMap = new HashMap<Long, String>();
		xXGroupNameMap.put(userId, groupName);
		Mockito.when(xGroupService.getXXGroupIdNameMap()).thenReturn(xXGroupNameMap);

		Object[] objArr = new Object[] {userId ,userId,userLoginID};
		Map<Long, Object[]> xXUserMap =new HashMap<Long, Object[]>();
		xXUserMap.put(userId, objArr);
		Mockito.when(xUserService.getXXPortalUserIdXXUserNameMap()).thenReturn(xXUserMap);

		Mockito.when(xModuleDefService.populateViewBean(xModuleDef,xXUserMap,xXGroupNameMap,true)).thenReturn(vXModuleDef);
		List<XXGroupPermission> xXGroupPermissions=new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xXGroupPermissions.add(xGroupPermissionObj);
		List<VXGroupPermission> vXGroupPermissions=new ArrayList<VXGroupPermission>();
		VXGroupPermission vXGroupPermission=vxGroupPermission();
		vXGroupPermission.setIsAllowed(0);
		vXGroupPermissions.add(vXGroupPermission);
		List<XXUserPermission> xXUserPermissions=new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj=xxUserPermission();
		xXUserPermissions.add(xUserPermissionObj);
		VXUserPermission vxUserPermission=vxUserPermission();
		vxUserPermission.setIsAllowed(0);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		Map<Long, VXGroupPermission> groupPermMapOld = new HashMap<Long, VXGroupPermission>();
		groupPermMapOld.put(vXGroupPermission.getGroupId(), vXGroupPermission);
		Mockito.when(xGroupPermissionService.convertVListToVMap((List<VXGroupPermission>) Mockito.any())).thenReturn(groupPermMapOld);
		Mockito.when(xGroupPermissionService.updateResource(vXGroupPermission)).thenReturn(vXGroupPermission);
		XXGroupUserDao xxGroupUserDao = Mockito.mock(XXGroupUserDao.class);
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(xxGroupUserDao);
		List<XXGroupUser> grpUsers =new ArrayList<XXGroupUser>();
		Mockito.when(xxGroupUserDao.findByGroupId(vXGroupPermission.getGroupId())).thenReturn(grpUsers);
		List<VXUserPermission> userPermListOld = new ArrayList<VXUserPermission>();
		userPermListOld.add(vxUserPermission);
		Map<Long, VXUserPermission> userPermMapOld = new HashMap<Long, VXUserPermission>();
		userPermMapOld.put(vxUserPermission.getUserId(), vxUserPermission);
		Mockito.when(xUserPermissionService.convertVListToVMap((List<VXUserPermission>) Mockito.any())).thenReturn(userPermMapOld);
		Mockito.when(xUserPermissionService.updateResource(vxUserPermission)).thenReturn(vxUserPermission);
		Mockito.when(sessionMgr.getActiveUserSessionsForPortalUserId(vxUserPermission.getUserId())).thenReturn(userSessions);
		VXModuleDef dbMuduleDef = xUserMgr.updateXModuleDefPermission(vXModuleDef);
		Assert.assertEquals(dbMuduleDef, vXModuleDef);
		Assert.assertNotNull(dbMuduleDef);
		Assert.assertEquals(dbMuduleDef, vXModuleDef);
		Assert.assertEquals(dbMuduleDef.getId(), vXModuleDef.getId());
		Assert.assertEquals(dbMuduleDef.getOwner(), vXModuleDef.getOwner());
		Assert.assertEquals(dbMuduleDef.getUpdatedBy(),vXModuleDef.getUpdatedBy());
		Assert.assertEquals(dbMuduleDef.getUrl(), vXModuleDef.getUrl());
		Assert.assertEquals(dbMuduleDef.getAddedById(),vXModuleDef.getAddedById());
		Assert.assertEquals(dbMuduleDef.getCreateDate(),vXModuleDef.getCreateDate());
		Assert.assertEquals(dbMuduleDef.getCreateTime(),vXModuleDef.getCreateTime());
		Assert.assertEquals(dbMuduleDef.getUserPermList(),vXModuleDef.getUserPermList());
		Assert.assertEquals(dbMuduleDef.getGroupPermList(),vXModuleDef.getGroupPermList());
		Mockito.verify(xModuleDefService).populateViewBean(xModuleDef,xXUserMap,xXGroupNameMap,true);
		Mockito.verify(xModuleDefService).updateResource(vXModuleDef);
		Mockito.verify(daoManager).getXXModuleDef();
	}

	@Test
	public void test97updateXModuleDefPermission() {
		XXModuleDefDao xModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXModuleDef xModuleDef = xxModuleDef();
		VXModuleDef vXModuleDef = vxModuleDef();
		Mockito.when(xModuleDefService.updateResource(vXModuleDef)).thenReturn(vXModuleDef);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		Mockito.when(xModuleDefDao.getById(userId)).thenReturn(xModuleDef);

		Map<Long, String> xXGroupNameMap = new HashMap<Long, String>();
		xXGroupNameMap.put(userId, groupName);
		Mockito.when(xGroupService.getXXGroupIdNameMap()).thenReturn(xXGroupNameMap);

		Object[] objArr = new Object[] {userId ,userId,userLoginID};
		Map<Long, Object[]> xXUserMap =new HashMap<Long, Object[]>();
		xXUserMap.put(userId, objArr);
		Mockito.when(xUserService.getXXPortalUserIdXXUserNameMap()).thenReturn(xXUserMap);

		Mockito.when(xModuleDefService.populateViewBean(xModuleDef,xXUserMap,xXGroupNameMap,true)).thenReturn(vXModuleDef);
		List<XXGroupPermission> xXGroupPermissions=new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xXGroupPermissions.add(xGroupPermissionObj);
		VXGroupPermission vXGroupPermission=vxGroupPermission();
		vXGroupPermission.setIsAllowed(0);
		List<XXUserPermission> xXUserPermissions=new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj=xxUserPermission();
		xXUserPermissions.add(xUserPermissionObj);
		VXUserPermission vxUserPermission=vxUserPermission();
		vxUserPermission.setIsAllowed(0);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		XXGroupUserDao xxGroupUserDao = Mockito.mock(XXGroupUserDao.class);
		Mockito.when(daoManager.getXXGroupUser()).thenReturn(xxGroupUserDao);
		List<XXGroupUser> grpUsers =new ArrayList<XXGroupUser>();
		Mockito.when(xxGroupUserDao.findByGroupId(vXGroupPermission.getGroupId())).thenReturn(grpUsers);
		Mockito.when(sessionMgr.getActiveUserSessionsForPortalUserId(vxUserPermission.getUserId())).thenReturn(userSessions);
		Mockito.when(xGroupPermissionService.createResource((VXGroupPermission) Mockito.any())).thenReturn(vXGroupPermission);
		Mockito.when(xUserPermissionService.createResource((VXUserPermission) Mockito.any())).thenReturn(vxUserPermission);
		VXModuleDef dbMuduleDef = xUserMgr.updateXModuleDefPermission(vXModuleDef);
		Assert.assertEquals(dbMuduleDef, vXModuleDef);
		Assert.assertNotNull(dbMuduleDef);
		Assert.assertEquals(dbMuduleDef, vXModuleDef);
		Assert.assertEquals(dbMuduleDef.getId(), vXModuleDef.getId());
		Assert.assertEquals(dbMuduleDef.getOwner(), vXModuleDef.getOwner());
		Assert.assertEquals(dbMuduleDef.getUpdatedBy(),vXModuleDef.getUpdatedBy());
		Assert.assertEquals(dbMuduleDef.getUrl(), vXModuleDef.getUrl());
		Assert.assertEquals(dbMuduleDef.getAddedById(),vXModuleDef.getAddedById());
		Assert.assertEquals(dbMuduleDef.getCreateDate(),vXModuleDef.getCreateDate());
		Assert.assertEquals(dbMuduleDef.getCreateTime(),vXModuleDef.getCreateTime());
		Assert.assertEquals(dbMuduleDef.getUserPermList(),vXModuleDef.getUserPermList());
		Assert.assertEquals(dbMuduleDef.getGroupPermList(),vXModuleDef.getGroupPermList());
		Mockito.verify(xModuleDefService).updateResource(vXModuleDef);
		Mockito.verify(daoManager).getXXModuleDef();
		Mockito.verify(xModuleDefService).populateViewBean(xModuleDef,xXUserMap,xXGroupNameMap,true);
		Mockito.verify(xGroupService).getXXGroupIdNameMap();
		Mockito.verify(xUserService).getXXPortalUserIdXXUserNameMap();
	}

	@Test
	public void test98modifyUserActiveStatus() {
		VXUser vxUser = vxUser();
		XXUser xXUser = xxUser(vxUser);
		HashMap<Long, Integer> statusMap= new HashMap<Long, Integer>();
		statusMap.put(xXUser.getId(), 1);
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(true);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(null);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		xUserMgr.modifyUserActiveStatus(statusMap);
	}

	@Test
	public void test99createServiceConfigUser() {
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		VXUser vxUser = vxUser();
		XXUser xXUser = xxUser(vxUser);
		VXUserPermission vXUserPermission=vxUserPermission();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionObj.setModuleId(vXUserPermission.getModuleId());
		xUserPermissionObj.setUserId(vXUserPermission.getUserId());
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByUserName(vxUser.getName())).thenReturn(xXUser);
		Mockito.when(xUserService.populateViewBean(xXUser)).thenReturn(vxUser);
		VXUser serviceConfigUser=xUserMgr.createServiceConfigUser(vxUser.getName());
		Assert.assertNotNull(serviceConfigUser);
		Assert.assertEquals(xXUser.getName(), serviceConfigUser.getName());
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByUserName(vxUser.getName())).thenReturn(null);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		serviceConfigUser=xUserMgr.createServiceConfigUser(vxUser.getName());
		Assert.assertNull(serviceConfigUser);
	}

	@Test
	public void test100getStringListFromUserRoleList() {
		destroySession();
		VXStringList vXStringList=xUserMgr.getStringListFromUserRoleList(null);
		Assert.assertNull(vXStringList);
	}
	
	
	@Test
	public void test101getAdminUserDetailsWithUserHavingUSER_ROLE() {
		destroySession();
		
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(false);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(userLoginID);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_USER);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		
		VXUser vxUser = vxUser();
		List<String> userRole = new ArrayList<String>();
		userRole.add(RangerConstants.ROLE_ADMIN);
		vxUser.setId(5L);
		vxUser.setName("test3");
		vxUser.setUserRoleList(userRole);
		vxUser.setUserSource(RangerCommonEnums.USER_UNIX);
		Mockito.when(xUserService.readResourceWithOutLogin(5L)).thenReturn(vxUser);
		Mockito.when(xUserService.getXUserByUserName("testuser")).thenReturn(loggedInUser);
		Mockito.when(restErrorUtil.create403RESTException("Logged-In user is not allowed to access requested user data.")).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.getXUser(5L);
	}
	
	@Test
	public void test102getKeyAdminUserDetailsWithUserHavingUSER_ROLE() {
		destroySession();
		
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(false);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(userLoginID);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_USER);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		
		VXUser vxUser = vxUser();
		List<String> userRole = new ArrayList<String>();
		userRole.add(RangerConstants.ROLE_KEY_ADMIN);
		vxUser.setId(5L);
		vxUser.setName("test3");
		vxUser.setUserRoleList(userRole);
		vxUser.setUserSource(RangerCommonEnums.USER_UNIX);
		Mockito.when(xUserService.readResourceWithOutLogin(5L)).thenReturn(vxUser);
		Mockito.when(xUserService.getXUserByUserName("testuser")).thenReturn(loggedInUser);
		Mockito.when(restErrorUtil.create403RESTException("Logged-In user is not allowed to access requested user data.")).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.getXUser(5L);
	}
	
	@Test
	public void test103getAdminAuditorUserDetailsWithUserHavingUSER_ROLE() {
		destroySession();
		
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(false);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(userLoginID);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_USER);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		
		VXUser vxUser = vxUser();
		List<String> userRole = new ArrayList<String>();
		userRole.add(RangerConstants.ROLE_ADMIN_AUDITOR);
		vxUser.setId(5L);
		vxUser.setName("test3");
		vxUser.setUserRoleList(userRole);
		vxUser.setUserSource(RangerCommonEnums.USER_UNIX);
		Mockito.when(xUserService.readResourceWithOutLogin(5L)).thenReturn(vxUser);
		Mockito.when(xUserService.getXUserByUserName("testuser")).thenReturn(loggedInUser);
		Mockito.when(restErrorUtil.create403RESTException("Logged-In user is not allowed to access requested user data.")).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.getXUser(5L);
	}

	@Test
	public void test104getKeyAdminAuditorUserDetailsWithUserHavingUSER_ROLE() {
		destroySession();
		
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(false);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(userLoginID);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_USER);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		
		VXUser vxUser = vxUser();
		List<String> userRole = new ArrayList<String>();
		userRole.add(RangerConstants.ROLE_KEY_ADMIN_AUDITOR);
		vxUser.setId(5L);
		vxUser.setName("test3");
		vxUser.setUserRoleList(userRole);
		vxUser.setUserSource(RangerCommonEnums.USER_UNIX);
		Mockito.when(xUserService.readResourceWithOutLogin(5L)).thenReturn(vxUser);
		Mockito.when(xUserService.getXUserByUserName("testuser")).thenReturn(loggedInUser);
		Mockito.when(restErrorUtil.create403RESTException("Logged-In user is not allowed to access requested user data.")).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.getXUser(5L);
	}

	@Test
	public void test105getUserDetailsOfItsOwn() {
		destroySession();
		
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(false);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(userLoginID);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		List<String> permissionList = new ArrayList<String>();
		permissionList.add(RangerConstants.MODULE_USER_GROUPS);
		
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_USER);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		
		VXUser vxUser = vxUser();
		List<String> userRole = new ArrayList<String>();
		userRole.add(RangerConstants.ROLE_USER);
		vxUser.setId(8L);
		vxUser.setName("test3");
		vxUser.setUserRoleList(userRole);
		vxUser.setUserSource(RangerCommonEnums.USER_UNIX);
		Mockito.when(xUserService.readResourceWithOutLogin(8L)).thenReturn(vxUser);
		Mockito.when(xUserService.getXUserByUserName("testuser")).thenReturn(loggedInUser);
		XXModuleDefDao mockxxModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(mockxxModuleDefDao);
		Mockito.when(mockxxModuleDefDao.findAccessibleModulesByUserId(8L, 8L)).thenReturn(permissionList);
		VXUser expectedVXUser = xUserMgr.getXUser(8L);
		Assert.assertNotNull(expectedVXUser);
		Assert.assertEquals(expectedVXUser.getName(), vxUser.getName());
		destroySession();
		Mockito.when(restErrorUtil.create403RESTException("Logged-In user is not allowed to access requested user data.")).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.getXUser(8L);
	}
	
	@Test
	public void test106getErrorWhenRoleUserFetchAnotherUserGroupInfo() {
		destroySession();
		
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(false);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(userLoginID);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		List<String> permissionList = new ArrayList<String>();
		permissionList.add(RangerConstants.MODULE_USER_GROUPS);
		
		List<Long> groupIdList = new ArrayList<Long>();
		groupIdList.add(2L);
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_USER);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		loggedInUser.setGroupIdList(groupIdList);
		
		VXUser vxUser = vxUser();
		List<String> userRole = new ArrayList<String>();
		userRole.add(RangerConstants.ROLE_USER);
		vxUser.setId(8L);
		vxUser.setName("test3");
		vxUser.setUserRoleList(userRole);
		vxUser.setUserSource(RangerCommonEnums.USER_UNIX);

		Mockito.when(xUserService.getXUserByUserName("testuser")).thenReturn(loggedInUser);

		XXGroupUserDao mockxxGroupUserDao = Mockito.mock(XXGroupUserDao.class);

		Mockito.when(daoManager.getXXGroupUser()).thenReturn(mockxxGroupUserDao);
		Mockito.when(mockxxGroupUserDao.findGroupIdListByUserId(loggedInUser.getId())).thenReturn(groupIdList);

		Mockito.when(restErrorUtil.create403RESTException("Logged-In user is not allowed to access requested group data.")).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xUserMgr.getXGroup(5L);
	}
	
	@Test
	public void test107RoleUserWillFetchOnlyHisOwnGroupDetails() {
		destroySession();
		
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(false);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(userLoginID);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		List<String> permissionList = new ArrayList<String>();
		permissionList.add(RangerConstants.MODULE_USER_GROUPS);
		
		List<Long> groupIdList = new ArrayList<Long>();
		groupIdList.add(5L);
		
		VXGroup expectedVXGroup = new VXGroup();
		expectedVXGroup.setId(5L);
		expectedVXGroup.setName("testGroup");
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_USER);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		loggedInUser.setGroupIdList(groupIdList);
		
		VXUser vxUser = vxUser();
		List<String> userRole = new ArrayList<String>();
		userRole.add(RangerConstants.ROLE_USER);
		vxUser.setId(8L);
		vxUser.setName("test3");
		vxUser.setUserRoleList(userRole);
		vxUser.setUserSource(RangerCommonEnums.USER_UNIX);
		Mockito.when(xGroupService.readResourceWithOutLogin(5L)).thenReturn(expectedVXGroup);

		VXGroup rcvVXGroup = xUserMgr.getXGroup(5L);
		Assert.assertNotNull(rcvVXGroup);
		Assert.assertEquals(expectedVXGroup.getId(), rcvVXGroup.getId());
		Assert.assertEquals(expectedVXGroup.getName(), rcvVXGroup.getName());
	}
	
	@Test
	public void test108RoleUserWillSearchOnlyHisOwnGroupDetails() {
		destroySession();
		
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(false);
		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setLoginId(userLoginID);
		xXPortalUser.setId(userId);
		currentUserSession.setXXPortalUser(xXPortalUser);
		List<String> permissionList = new ArrayList<String>();
		permissionList.add(RangerConstants.MODULE_USER_GROUPS);
		
		SearchCriteria testSearchCriteria = createsearchCriteria();
		
		List<Long> groupIdList = new ArrayList<Long>();
		groupIdList.add(5L);
		
		VXGroup expectedVXGroup = new VXGroup();
		expectedVXGroup.setId(5L);
		expectedVXGroup.setName("testGroup");
		
		List<VXGroup> grpList = new ArrayList<VXGroup>();
		grpList.add(expectedVXGroup);
		
		
		VXGroupList expectedVXGroupList = new VXGroupList();
		expectedVXGroupList.setVXGroups(grpList);
		
		VXUser loggedInUser = vxUser();
		List<String> loggedInUserRole = new ArrayList<String>();
		loggedInUserRole.add(RangerConstants.ROLE_USER);
		loggedInUser.setId(8L);
		loggedInUser.setName("testuser");
		loggedInUser.setUserRoleList(loggedInUserRole);
		loggedInUser.setGroupIdList(groupIdList);
		
		VXUser vxUser = vxUser();
		List<String> userRole = new ArrayList<String>();
		userRole.add(RangerConstants.ROLE_USER);
		vxUser.setId(8L);
		vxUser.setName("test3");
		vxUser.setUserRoleList(userRole);
		vxUser.setUserSource(RangerCommonEnums.USER_UNIX);
		Mockito.when(xUserService.getXUserByUserName("testuser")).thenReturn(loggedInUser);
		Mockito.when(xGroupService.searchXGroups(testSearchCriteria)).thenReturn(expectedVXGroupList);
		XXModuleDefDao mockxxModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(mockxxModuleDefDao);
		Mockito.when(mockxxModuleDefDao.findAccessibleModulesByUserId(8L, 8L)).thenReturn(permissionList);

		VXGroupList rcvVXGroupList = xUserMgr.searchXGroups(testSearchCriteria);
		Assert.assertNotNull(rcvVXGroupList);
		
		Assert.assertEquals(rcvVXGroupList.getList().get(0).getId(),expectedVXGroup.getId());
		Assert.assertEquals(rcvVXGroupList.getList().get(0).getName(),expectedVXGroup.getName());
	}

	@Test
	public void test109AssignPermissionToUser() {
		destroySession();
		setup();
		VXPortalUser vXPortalUser = userProfile();
		List<XXModuleDef> xXModuleDefs = xxModuleDefs();
		XXModuleDefDao xXModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xXModuleDefDao);
		Mockito.when(daoManager.getXXModuleDef().getAll()).thenReturn(xXModuleDefs);

		VXUserPermission userPermission = vxUserPermission();
		List<VXUserPermission> userPermList = new ArrayList<VXUserPermission>();
		userPermList.add(userPermission);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermission = xxUserPermission();
		xUserPermission.setModuleId(userPermission.getModuleId());
		xUserPermission.setUserId(userPermission.getUserId());
		xUserPermissionsList.add(xUserPermission);

		XXUserPermissionDao xXUserPermissionDao= Mockito.mock(XXUserPermissionDao.class);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xXUserPermissionDao);
		Mockito.when(xXUserPermissionDao.findByModuleIdAndPortalUserId(vXPortalUser.getId(),xXModuleDefs.get(0).getId())).thenReturn(xUserPermission);

		VXUser vxUser = vxUser();
		XXUser xXUser = xxUser(vxUser);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByPortalUserId(vXPortalUser.getId())).thenReturn(xXUser);

		Mockito.when(xUserPermissionService.populateViewBean(xUserPermission)).thenReturn(userPermission);

		Mockito.when(xUserPermissionService.createResource((VXUserPermission) Mockito.any())).thenReturn(userPermission);
		Mockito.when(xUserPermissionService.updateResource((VXUserPermission) Mockito.any())).thenReturn(userPermission);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		Mockito.when(sessionMgr.getActiveUserSessionsForPortalUserId(userId)).thenReturn(userSessions);

		Collection<String> existingRoleList = new ArrayList<String>();
		existingRoleList.add(RangerConstants.ROLE_SYS_ADMIN);
		vXPortalUser.setUserRoleList(existingRoleList);
		xUserMgr.assignPermissionToUser(vXPortalUser, true);
		existingRoleList.clear();
		existingRoleList.add(RangerConstants.ROLE_KEY_ADMIN);
		vXPortalUser.setUserRoleList(existingRoleList);
		xUserMgr.assignPermissionToUser(vXPortalUser, true);
		existingRoleList.clear();
		existingRoleList.add(RangerConstants.ROLE_KEY_ADMIN_AUDITOR);
		vXPortalUser.setUserRoleList(existingRoleList);
		xUserMgr.assignPermissionToUser(vXPortalUser, true);
		existingRoleList.clear();
		existingRoleList.add(RangerConstants.ROLE_ADMIN_AUDITOR);
		vXPortalUser.setUserRoleList(existingRoleList);
		xUserMgr.assignPermissionToUser(vXPortalUser, true);
	}

	@Test
	public void test110CreateOrDeleteXGroupUserList() {
		destroySession();
		setup();
		GroupUserInfo groupUserInfo = new GroupUserInfo();
		groupUserInfo.setGroupName("public");
		Set<String> addUsers = new HashSet<String>();
		Set<String> delUsers = new HashSet<String>();
		addUsers.add("testuser1");
		addUsers.add("testuser2");
		delUsers.add("testuser3");
		groupUserInfo.setAddUsers(addUsers);
		groupUserInfo.setDelUsers(delUsers);
		List<GroupUserInfo> groupUserInfoList = new ArrayList<GroupUserInfo>();
		groupUserInfoList.add(groupUserInfo);
		Map<String, Long> usersFromDB = new HashMap<String, Long>();
		usersFromDB.put("testuser1", 1L);
		usersFromDB.put("testuser2", 2L);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.getAllUserIds()).thenReturn(usersFromDB);
		xUserMgr.createOrDeleteXGroupUserList(groupUserInfoList);
	}

	@Test
	public void test111CreateOrUpdateXUsers() {
		destroySession();
		setup();
		List<VXUser> vXUserList=new ArrayList<VXUser>();
		VXUser vXUser = vxUser();
		VXUser vXUser1 = vxUser();
		VXUser vXUser2 = vxUser();
		vXUser2.setFirstName("user12");
		vXUser2.setEmailAddress(null);
		vXUser.setFirstName("null");
		vXUser.setLastName("null");
		vXUser.setEmailAddress("");
		vXUser1.setName("null");
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add(RangerConstants.ROLE_USER);
		userRoleList.add(RangerConstants.ROLE_SYS_ADMIN);
		userRoleList.add(RangerConstants.ROLE_KEY_ADMIN);
		userRoleList.add(RangerConstants.ROLE_KEY_ADMIN_AUDITOR);
		userRoleList.add(RangerConstants.ROLE_ADMIN_AUDITOR);
		vXUser.setUserRoleList(userRoleList);
		vXUser1.setUserRoleList(userRoleList);
		vXUser2.setUserRoleList(userRoleList);
		vXUserList.add(vXUser);
		vXUserList.add(vXUser1);
		vXUserList.add(vXUser2);
		VXUserList users = new VXUserList(vXUserList);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXModuleDefDao xXModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXUser xXUser = xxUser(vXUser);
		VXPortalUser vXPortalUser = userProfile();
		vXPortalUser.setFirstName("null");
		vXPortalUser.setLastName("null");
		XXPortalUser xXPortalUser = xxPortalUser(vXPortalUser);
		xXPortalUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		List<String> lstRole = new ArrayList<String>();
		lstRole.add(RangerConstants.ROLE_SYS_ADMIN);
		List<XXModuleDef> xXModuleDefs=xxModuleDefs();

		vXPortalUser.setUserRoleList(lstRole);
		Mockito.when(userMgr.getUserProfileByLoginId(vXUser.getName())).thenReturn(null);

		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByUserName(vXUser.getName())).thenReturn(xXUser);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xXModuleDefDao);
		Mockito.when(xXModuleDefDao.getAll()).thenReturn(xXModuleDefs);

		Mockito.when(userMgr.mapVXPortalUserToXXPortalUser((VXPortalUser) Mockito.any())).thenReturn(xXPortalUser);
		XXPortalUserDao xXPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xXPortalUserDao);
		Mockito.when(daoManager.getXXPortalUser().create((XXPortalUser) Mockito.any())).thenReturn(xXPortalUser);
		XXUser xUser = xxUser(vXUser);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(daoManager.getXXUser().findByUserName(vXUser.getName())).thenReturn(xUser);
		Mockito.when(xUserService.populateViewBean(xUser)).thenReturn(vXUser);

		VXUserPermission userPermission = vxUserPermission();
		List<VXUserPermission> userPermList = new ArrayList<VXUserPermission>();
		userPermList.add(userPermission);

		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		xUserPermissionObj.setModuleId(userPermission.getModuleId());
		xUserPermissionObj.setUserId(userPermission.getUserId());
		xUserPermissionsList.add(xUserPermissionObj);

		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);

		Mockito.when(xUserPermissionService.createResource((VXUserPermission) Mockito.any())).thenReturn(userPermission);
		Mockito.when(sessionMgr.getActiveUserSessionsForPortalUserId(userId)).thenReturn(userSessions);
		Mockito.when(xUserPermissionDao.findByModuleIdAndPortalUserId(null, null)).thenReturn(xUserPermissionObj);
		Mockito.when(xUserPermissionService.populateViewBean(xUserPermissionObj)).thenReturn(userPermission);
		Mockito.when(xUserPermissionService.updateResource((VXUserPermission) Mockito.any())).thenReturn(userPermission);
		int createdOrUpdatedUserCount = xUserMgr.createOrUpdateXUsers(users);
		Assert.assertEquals(createdOrUpdatedUserCount, 1);
	}

	@Test
	public void test112CreateOrUpdateXUsers() {
		destroySession();
		setup();
		List<VXUser> vXUserList=new ArrayList<VXUser>();
		VXUser vXUser = vxUser();
		vXUser.setFirstName("testuser");
		vXUser.setLastName("testuser");
		vXUser.setPassword("TestPassword@123");
		vXUser.setEmailAddress("");
		vXUser.setUserSource(RangerCommonEnums.USER_APP);
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add(RangerConstants.ROLE_USER);
		userRoleList.add(RangerConstants.ROLE_SYS_ADMIN);
		userRoleList.add(RangerConstants.ROLE_KEY_ADMIN);
		userRoleList.add(RangerConstants.ROLE_KEY_ADMIN_AUDITOR);
		userRoleList.add(RangerConstants.ROLE_ADMIN_AUDITOR);
		vXUser.setUserRoleList(userRoleList);
		vXUserList.add(vXUser);
		VXUserList users = new VXUserList(vXUserList);

		VXPortalUser vXPortalUser = userProfile();
		vXPortalUser.setFirstName("testuser");
		vXPortalUser.setLastName("testuser");
		vXPortalUser.setPassword("TestPassword@123");
		XXPortalUser xXPortalUser = xxPortalUser(vXPortalUser);
		List<XXModuleDef> xXModuleDefs=xxModuleDefs();
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = xxUserPermission();
		VXUserPermission userPermission = vxUserPermission();
		List<VXUserPermission> userPermList = new ArrayList<VXUserPermission>();
		userPermList.add(userPermission);
		xUserPermissionObj.setModuleId(userPermission.getModuleId());
		xUserPermissionObj.setUserId(userPermission.getUserId());
		xUserPermissionsList.add(xUserPermissionObj);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);

		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXModuleDefDao xXModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);

		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(daoManager.getXXPortalUser().create((XXPortalUser) Mockito.any())).thenReturn(xXPortalUser);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xXModuleDefDao);
		Mockito.when(xXModuleDefDao.getAll()).thenReturn(xXModuleDefs);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		Mockito.when(xUserPermissionDao.findByModuleIdAndPortalUserId(null, null)).thenReturn(xUserPermissionObj);
		Mockito.when(xUserPermissionService.createResource((VXUserPermission) Mockito.any())).thenReturn(userPermission);
		Mockito.when(sessionMgr.getActiveUserSessionsForPortalUserId(userId)).thenReturn(userSessions);
		Mockito.when(xUserService.createResource((VXUser) Mockito.any())).thenReturn(vXUser);
		Mockito.when(xUserPermissionService.populateViewBean(xUserPermissionObj)).thenReturn(userPermission);
		Mockito.when(xUserPermissionService.updateResource((VXUserPermission) Mockito.any())).thenReturn(userPermission);
		xUserMgr.createOrUpdateXUsers(users);

		vXUser.setPassword("*****");
		xUserMgr.createOrUpdateXUsers(users);
	}

	@Test
	public void test113CreateOrUpdateXUsers() {
		destroySession();
		setup();
		VXUser vXUser = vxUser();
		vXUser.setFirstName("null");
		vXUser.setLastName("null");
		List<VXUser> vXUserList=new ArrayList<VXUser>();
		vXUserList.add(vXUser);
		VXUserList users = new VXUserList(vXUserList);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		XXModuleDefDao xXModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXUser xXUser = xxUser(vXUser);
		VXPortalUser vXPortalUser = userProfile();
		vXPortalUser.setFirstName("null");
		vXPortalUser.setLastName("null");
		XXPortalUser xXPortalUser = xxPortalUser(vXPortalUser);
		xXPortalUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		List<String> lstRole = new ArrayList<String>();
		lstRole.add(RangerConstants.ROLE_SYS_ADMIN);
		List<XXModuleDef> xXModuleDefs=new ArrayList<XXModuleDef>();

		vXPortalUser.setUserRoleList(lstRole);
		Mockito.when(userMgr.getUserProfileByLoginId(vXUser.getName())).thenReturn(vXPortalUser);

		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByUserName(vXUser.getName())).thenReturn(xXUser);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xXModuleDefDao);
		Mockito.when(xXModuleDefDao.getAll()).thenReturn(xXModuleDefs);
		Mockito.when(xUserService.updateResource(vXUser)).thenReturn(vXUser);

		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = new XXUserPermission();
		xUserPermissionObj.setAddedByUserId(userId);
		xUserPermissionObj.setCreateTime(new Date());
		xUserPermissionObj.setId(userId);
		xUserPermissionObj.setIsAllowed(1);
		xUserPermissionObj.setModuleId(1L);
		xUserPermissionObj.setUpdatedByUserId(userId);
		xUserPermissionObj.setUpdateTime(new Date());
		xUserPermissionObj.setUserId(userId);
		xUserPermissionsList.add(xUserPermissionObj);
		Mockito.when(xUserPermissionDao.findByUserPermissionId(vXPortalUser.getId())).thenReturn(xUserPermissionsList);
		xUserMgr.createOrUpdateXUsers(users);
		vXUserList.clear();
		vXUser.setUserSource(RangerCommonEnums.USER_APP);
		vXUser.setFirstName("testuser");
		vXUser.setLastName("testuser");
		vXUser.setPassword("TestPassword@123");
		vXUserList.add(vXUser);
		users = new VXUserList(vXUserList);
		vXPortalUser = userProfile();
		vXPortalUser.setUserSource(RangerCommonEnums.USER_APP);
		vXPortalUser.setFirstName("testuser");
		vXPortalUser.setLastName("testuser");
		vXPortalUser.setPassword("TestPassword@123");
		vXPortalUser.setUserRoleList(lstRole);
		Mockito.when(userMgr.getUserProfileByLoginId(vXUser.getName())).thenReturn(vXPortalUser);
		Mockito.when(userMgr.updateUserWithPass((VXPortalUser) Mockito.any())).thenReturn(xXPortalUser);
		xUserMgr.createOrUpdateXUsers(users);
		vXUser.setPassword("*****");
		xUserMgr.createOrUpdateXUsers(users);
	}

	@Test
	public void test114CreateOrUpdateXGroups() {
		destroySession();
		setup();
		VXGroup vXGroup = vxGroup();
		VXGroupList vXGroupListSort = new VXGroupList();
		List<VXGroup> vXGroups = new ArrayList<VXGroup>();
		vXGroups.add(vXGroup);
		VXGroup vXGroup1 = vxGroup();
		vXGroup1.setName("null");
		vXGroups.add(vXGroup1);
		vXGroupListSort.setVXGroups(vXGroups);

		VXUser vXUser = vxUser();
		List<VXUser> vXUserList=new ArrayList<VXUser>();
		vXUserList.add(vXUser);
		VXPortalUser vXPortalUser = userProfile();
		XXPortalUser xXPortalUser = xxPortalUser(vXPortalUser);
		xXPortalUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		List<String> lstRole = new ArrayList<String>();
		lstRole.add(RangerConstants.ROLE_SYS_ADMIN);

		vXPortalUser.setUserRoleList(lstRole);

		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = new XXUserPermission();
		xUserPermissionObj.setAddedByUserId(userId);
		xUserPermissionObj.setCreateTime(new Date());
		xUserPermissionObj.setId(userId);
		xUserPermissionObj.setIsAllowed(1);
		xUserPermissionObj.setModuleId(1L);
		xUserPermissionObj.setUpdatedByUserId(userId);
		xUserPermissionObj.setUpdateTime(new Date());
		xUserPermissionObj.setUserId(userId);
		xUserPermissionsList.add(xUserPermissionObj);
		xUserMgr.createOrUpdateXGroups(vXGroupListSort);
	}

	@Test
	public void test115UpdateUserRoleAssignments() {
		destroySession();
		setup();
		UsersGroupRoleAssignments ugRoleAssignments = new UsersGroupRoleAssignments();
		Set<String> addUsers = new HashSet<String>();
		Set<String> delUsers = new HashSet<String>();
		addUsers.add("testuser");
		addUsers.add("testuser2");
		delUsers.add("testuser2");
		Map<String, String> userMap = new HashMap<String, String>();
		Map<String, String> groupMap = new HashMap<>();
		List<String> allUsers = new ArrayList<>(addUsers);
		userMap.put("testuser", "role1");
		userMap.put("testuser2", "role2");
		groupMap.put("testgroup1", "role1");
		groupMap.put("testgroup2", "role2");
		ugRoleAssignments.setUsers(allUsers);
		ugRoleAssignments.setGroupRoleAssignments(groupMap);
		ugRoleAssignments.setUserRoleAssignments(userMap);
		ugRoleAssignments.setWhiteListUserRoleAssignments(new HashMap<>());
		ugRoleAssignments.setWhiteListGroupRoleAssignments(new HashMap<>());
		VXUser vXUser = vxUser();
		List<VXUser> vXUserList=new ArrayList<VXUser>();
		vXUserList.add(vXUser);
		VXPortalUser vXPortalUser = userProfile();
		XXPortalUser xXPortalUser = xxPortalUser(vXPortalUser);
		xXPortalUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		List<String> lstRole = new ArrayList<String>();
		lstRole.add(RangerConstants.ROLE_SYS_ADMIN);
		vXPortalUser.setUserRoleList(lstRole);
		Mockito.when(userMgr.getUserProfileByLoginId(vXUser.getName())).thenReturn(vXPortalUser);

		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = new XXUserPermission();
		xUserPermissionObj.setAddedByUserId(userId);
		xUserPermissionObj.setCreateTime(new Date());
		xUserPermissionObj.setId(userId);
		xUserPermissionObj.setIsAllowed(1);
		xUserPermissionObj.setModuleId(1L);
		xUserPermissionObj.setUpdatedByUserId(userId);
		xUserPermissionObj.setUpdateTime(new Date());
		xUserPermissionObj.setUserId(userId);
		xUserPermissionsList.add(xUserPermissionObj);
		Mockito.when(xUserPermissionDao.findByUserPermissionId(vXPortalUser.getId())).thenReturn(xUserPermissionsList);

		List<XXModuleDef> xXModuleDefs = xxModuleDefs();
		XXModuleDefDao xXModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xXModuleDefDao);
		Mockito.when(daoManager.getXXModuleDef().getAll()).thenReturn(xXModuleDefs);
		xUserMgr.updateUserRoleAssignments(ugRoleAssignments);

		allUsers.clear();
		allUsers.add("UnMappedUser");
		ugRoleAssignments.setUsers(allUsers);
		ugRoleAssignments.setGroupRoleAssignments(groupMap);
		ugRoleAssignments.setUserRoleAssignments(userMap);

		VXUserPermission userPermission = vxUserPermission();
		List<VXUserPermission> userPermList = new ArrayList<VXUserPermission>();
		userPermList.add(userPermission);
		List<XXUserPermission> xUserPermissionsList1 = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj1 = xxUserPermission();
		xUserPermissionObj1.setModuleId(userPermission.getModuleId());
		xUserPermissionObj1.setUserId(userPermission.getUserId());
		xUserPermissionsList1.add(xUserPermissionObj1);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		xUserMgr.updateUserRoleAssignments(ugRoleAssignments);

		vXPortalUser.setUserSource(RangerCommonEnums.USER_APP);
		Mockito.when(userMgr.getUserProfileByLoginId(Mockito.anyString())).thenReturn(vXPortalUser);
		xUserMgr.updateUserRoleAssignments(ugRoleAssignments);
	}

	@Test
	public void test116GetGroups() {
		destroySession();
		setup();
		VXGroup vXGroup = vxGroup();
		XXGroup xxGroup = new XXGroup();
		xxGroup.setId(vXGroup.getId());
		xxGroup.setName(vXGroup.getName());
		xxGroup.setDescription(vXGroup.getDescription());
		xxGroup.setIsVisible(vXGroup.getIsVisible());
		List<XXGroup> resultList = new ArrayList<XXGroup>();
		resultList.add(xxGroup);
		xUserMgr.getGroups();
	}

	@Test
	public void test117GetUserGroups() {
		destroySession();
		setup();
		String user = "testuser1";
		Set<String> userGroups = new HashSet<String>();
		userGroups.add("group1");
		Map<String, Set<String>> userGroupMap = new HashMap<String, Set<String>>();
		userGroupMap.put(user, userGroups);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findGroupsByUserIds()).thenReturn(userGroupMap);
		Map<String, Set<String>> userGroupMap1 = xUserMgr.getUserGroups();
		Assert.assertNotNull(userGroupMap1);
		Assert.assertEquals(userGroupMap, userGroupMap1);
	}

	@Test
	public void test118GetUsers() {
		destroySession();
		setup();
		VXUser vXUser = vxUser();
		UserInfo userInfo = new UserInfo(vXUser.getName(), vXUser.getDescription(), null);
		Set<UserInfo> userInfoSet = new HashSet<UserInfo>();
		userInfoSet.add(userInfo);
		List<UserInfo> userInfoList = new ArrayList<UserInfo>();
		userInfoList.add(userInfo);
		XXUser xxUser = xxUser(vXUser);
		List<XXUser> resultList = new ArrayList<XXUser>();
		resultList.add(xxUser);
		Set<UserInfo> userInfoSet1 = xUserMgr.getUsers();
		Assert.assertNotNull(userInfoSet1);
		Mockito.when(xUserService.getUsers()).thenReturn(userInfoList);
		Set<UserInfo> userInfoSet2 = xUserMgr.getUsers();
		Assert.assertNotNull(userInfoSet2);
		Assert.assertEquals(userInfoSet, userInfoSet2);
	}

	@Test
	public void test119GetRangerUserStore() throws Exception {
		destroySession();
		setup();
		Long lastKnownUserStoreVersion=Long.valueOf(1);
		Mockito.when(xxGlobalStateDao.getAppDataVersion(RANGER_USER_GROUP_GLOBAL_STATE_NAME)).thenReturn(lastKnownUserStoreVersion);
		Map<String, Set<String>> userGroupMap = new HashMap<String, Set<String>>();
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findGroupsByUserIds()).thenReturn(userGroupMap);
		xUserMgr.getRangerUserStoreIfUpdated(lastKnownUserStoreVersion);
	}

	@Test
	public void test120GetUserStoreVersion() throws Exception {
		destroySession();
		setup();
		Long lastKnownUserStoreVersion=Long.valueOf(1);
		Mockito.when(xxGlobalStateDao.getAppDataVersion(RANGER_USER_GROUP_GLOBAL_STATE_NAME)).thenReturn(lastKnownUserStoreVersion);
		Long userStoreVersion = xUserMgr.getUserStoreVersion();
		Assert.assertNotNull(userStoreVersion);
		Assert.assertEquals(lastKnownUserStoreVersion, userStoreVersion);
	}

	@Test
	public void test121UpdateDeletedUsers() {
		destroySession();
		setup();
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		VXUser vxUser = vxUser();
		XXUser xXUser = xxUser(vxUser);
		Set<String> delUsers = new HashSet<String>();
		delUsers.add(vxUser.getName());
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByUserName(vxUser.getName())).thenReturn(xXUser);
		Mockito.when(xUserService.populateViewBean(xXUser)).thenReturn(vxUser);
		Mockito.when(xUserService.updateResource(vxUser)).thenReturn(vxUser);
		int count = xUserMgr.updateDeletedUsers(delUsers);
		Assert.assertNotNull(count);
		Assert.assertEquals(count, 1);
	}

	@Test
	public void test122UpdateDeletedGroups() {
		destroySession();
		setup();
		XXGroupDao xxGroupDao = Mockito.mock(XXGroupDao.class);
		VXGroup vxGroup = vxGroup();
		XXGroup xxGroup = new XXGroup();
		xxGroup.setId(vxGroup.getId());
		xxGroup.setName(vxGroup.getName());
		xxGroup.setDescription(vxGroup.getDescription());
		xxGroup.setIsVisible(vxGroup.getIsVisible());
		Set<String> delGroups = new HashSet<String>();
		delGroups.add(vxGroup.getName());
		Mockito.when(daoManager.getXXGroup()).thenReturn(xxGroupDao);
		Mockito.when(xxGroupDao.findByGroupName(vxGroup.getName())).thenReturn(xxGroup);
		Mockito.when(xGroupService.populateViewBean(xxGroup)).thenReturn(vxGroup);
		Mockito.when(xGroupService.updateResource(vxGroup)).thenReturn(vxGroup);
		int count = xUserMgr.updateDeletedGroups(delGroups);
		Assert.assertNotNull(count);
		Assert.assertEquals(count, 1);
	}

	@Test
	public void test123LookupXGroups() {
		destroySession();
		setup();
		VXGroup vXGroup = vxGroup();
		VXGroupList vXGroupListSort = new VXGroupList();
		List<VXGroup> vXGroups = new ArrayList<VXGroup>();
		vXGroups.add(vXGroup);
		vXGroupListSort.setVXGroups(vXGroups);
		String groupName = vXGroup.getName();
		SearchCriteria testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", groupName);
		Mockito.when(xGroupService.getGroupByGroupName(groupName)).thenReturn(vXGroup);
		Mockito.when(xGroupService.searchXGroups((SearchCriteria) Mockito.any())).thenReturn(vXGroupListSort);
		VXGroupList vXGroupList = xUserMgr.searchXGroups(testSearchCriteria);
		testSearchCriteria.addParam("isvisible", "true");
		vXGroupList = xUserMgr.lookupXGroups(testSearchCriteria);
		Assert.assertNotNull(vXGroupList);
		testSearchCriteria = createsearchCriteria();
		testSearchCriteria.addParam("name", groupName);
		testSearchCriteria.addParam("groupsource", 1L);
		vXGroupList = xUserMgr.lookupXGroups(testSearchCriteria);
		Assert.assertNotNull(vXGroupList);
		testSearchCriteria = createsearchCriteria();
		testSearchCriteria.setSortBy("");
		testSearchCriteria.addParam("name", groupName);
		Mockito.when(xGroupService.getGroupByGroupName(Mockito.anyString())).thenReturn(vXGroup);
		vXGroupList = xUserMgr.lookupXGroups(testSearchCriteria);
		Assert.assertNotNull(vXGroupList);

		SearchCriteria emptyCriteria = new SearchCriteria();
		Mockito.when(xGroupService.searchXGroups((SearchCriteria) Mockito.any())).thenReturn(null);
		vXGroupList = xUserMgr.lookupXGroups(emptyCriteria);
		Assert.assertNull(vXGroupList);
	}

	@Test
	public void test124LookupXUsers() {
		destroySession();
		setup();
		VXUser vXUser = vxUser();
		VXUserList vXUserList1 = new VXUserList();
		List<VXUser> vXUsers = new ArrayList<VXUser>();
		vXUsers.add(vXUser);
		vXUserList1.setVXUsers(vXUsers);
		String groupName = vXUser.getName();
		SearchCriteria searchCriteria = createsearchCriteria();
		searchCriteria.addParam("name", groupName);
		searchCriteria.addParam("isvisible", "true");
		Mockito.when(xUserService.lookupXUsers((SearchCriteria) Mockito.any(), (VXUserList) Mockito.any())).thenReturn(vXUserList1);
		VXUserList vXUserList2 = xUserMgr.lookupXUsers(searchCriteria);
		Assert.assertNotNull(vXUserList2);
		Assert.assertEquals(vXUserList1, vXUserList2);
		searchCriteria.setSortBy("");
		vXUserList2 = xUserMgr.lookupXUsers(searchCriteria);
		Assert.assertNotNull(vXUserList2);
		Assert.assertEquals(vXUserList1, vXUserList2);
	}

	@Test
	public void test125DeleteXUser() {
		destroySession();
		setup();
		boolean force = true;
		VXUser vXUser = vxUser();
		XXUser xXUser = new XXUser();
		XXUserDao xXUserDao = Mockito.mock(XXUserDao.class);
		Mockito.when(daoManager.getXXUser()).thenReturn(xXUserDao);
		Mockito.when(xXUserDao.getById(vXUser.getId())).thenReturn(xXUser);
		Mockito.when(xUserService.populateViewBean(xXUser)).thenReturn(vXUser);
		VXPermMapList vXPermMapList = new VXPermMapList();
		VXPermMap vXPermMap1=getVXPermMap();
		List<VXPermMap> vXPermMaps=new ArrayList<VXPermMap>();
		vXPermMaps.add(vXPermMap1);
		vXPermMapList.setVXPermMaps(vXPermMaps);
		VXAuditMapList vXAuditMapList = new VXAuditMapList();
		List<VXAuditMap> vXAuditMaps=new ArrayList<VXAuditMap>();
		VXAuditMap vXAuditMap=getVXAuditMap();
		vXAuditMaps.add(vXAuditMap);
		vXAuditMapList.setVXAuditMaps(vXAuditMaps);
		VXPortalUser vXPortalUser = userProfile();
		XXPortalUser xXPortalUser = xxPortalUser(vXPortalUser);
		XXPortalUserDao xXPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xXPortalUserDao);
		Mockito.when(xXPortalUserDao.findByLoginId(vXUser.getName().trim())).thenReturn(xXPortalUser);
		Mockito.when(xPortalUserService.populateViewBean(xXPortalUser)).thenReturn(vXPortalUser);
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXAuthSession> xXAuthSessions=new ArrayList<XXAuthSession>();
		XXAuthSession xXAuthSession = new XXAuthSession();
		xXAuthSession.setId(userId);
		xXAuthSession.setLoginId(vXPortalUser.getLoginId());
		xXAuthSessions.add(xXAuthSession);
		List<XXUserPermission> xXUserPermissions=new ArrayList<XXUserPermission>();
		xXUserPermissions.add(xxUserPermission());
		List<XXPortalUserRole> xXPortalUserRoles=new ArrayList<XXPortalUserRole>();
		xXPortalUserRoles.add(XXPortalUserRole);
		List<XXPolicy> xXPolicyList = new ArrayList<XXPolicy>();
		XXPolicy xXPolicy=getXXPolicy();
		xXPolicyList.add(xXPolicy);

		XXSecurityZoneRefUser xZoneAdminUser = new XXSecurityZoneRefUser();
		xZoneAdminUser.setZoneId(2L);
		xZoneAdminUser.setUserId(userId);
		xZoneAdminUser.setUserName(vXUser.getName());
		xZoneAdminUser.setUserType(1);
		List<XXSecurityZoneRefUser> zoneSecRefUser=new ArrayList<XXSecurityZoneRefUser>();
		zoneSecRefUser.add(xZoneAdminUser);
		XXSecurityZoneRefUserDao zoneSecRefUserDao=Mockito.mock(XXSecurityZoneRefUserDao.class);
		Mockito.when(daoManager.getXXSecurityZoneRefUser()).thenReturn(zoneSecRefUserDao);
		Mockito.when(zoneSecRefUserDao.findByUserId(userId)).thenReturn(zoneSecRefUser);

		RangerSecurityZone securityZone = new RangerSecurityZone();
		securityZone.setId(2L);
		securityZone.setName("sz1");
		XXSecurityZone xxSecurityZone = new XXSecurityZone();
		xxSecurityZone.setId(2L);
		xxSecurityZone.setName("sz1");

		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);
		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.getById(xZoneAdminUser.getZoneId())).thenReturn(xxSecurityZone);

		List<XXRoleRefUser> roleRefUser=new ArrayList<XXRoleRefUser>();
		XXRoleRefUser xRoleRefUser = new XXRoleRefUser();
		xRoleRefUser.setRoleId(userId);
		xRoleRefUser.setUserId(userId);
		xRoleRefUser.setUserName(vXUser.getName().trim());
		xRoleRefUser.setUserType(0);
		roleRefUser.add(xRoleRefUser);
		XXRole xRole = new XXRole();
		xRole.setId(userId);
		xRole.setName("Role1");

		VXResponse vXResponse = new VXResponse();
		vXResponse.setStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		vXResponse.setMsgDesc("Can Not Delete User '" + vXUser.getName().trim() + "' as its present in " + RangerConstants.ROLE_FIELD);
		Mockito.when(restErrorUtil.generateRESTException((VXResponse) Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);

		xUserMgr.deleteXUser(vXUser.getId(), force);
		force=false;
		xUserMgr.deleteXUser(vXUser.getId(), force);
	}

	@Test
	public void test126DeleteXGroup() {
		destroySession();
		setup();
		boolean force = true;
		VXGroup vXGroup = vxGroup();
		VXPermMapList vXPermMapList = new VXPermMapList();
		VXPermMap vXPermMap1=getVXPermMap();
		List<VXPermMap> vXPermMaps=new ArrayList<VXPermMap>();
		vXPermMaps.add(vXPermMap1);
		vXPermMapList.setVXPermMaps(vXPermMaps);
		VXAuditMapList vXAuditMapList = new VXAuditMapList();
		List<VXAuditMap> vXAuditMaps=new ArrayList<VXAuditMap>();
		VXAuditMap vXAuditMap=getVXAuditMap();
		vXAuditMaps.add(vXAuditMap);
		vXAuditMapList.setVXAuditMaps(vXAuditMaps);
		List<XXGroupGroup> xXGroupGroups = new ArrayList<XXGroupGroup>();
		XXGroupGroup xXGroupGroup = xxGroupGroup();
		xXGroupGroups.add(xXGroupGroup);
		List<XXGroupPermission> xXGroupPermissions=new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xXGroupPermissions.add(xGroupPermissionObj);
		List<XXPolicy> xXPolicyList = new ArrayList<XXPolicy>();
		XXPolicy xXPolicy=getXXPolicy();
		xXPolicyList.add(xXPolicy);
		List<XXResource> xResourceList = new ArrayList<XXResource>();
		XXResource xXResource = new XXResource();
		xXResource.setId(userId);
		xXResource.setName("hadoopdev");
		xXResource.setIsRecursive(AppConstants.BOOL_TRUE);
		xXResource.setResourceStatus(AppConstants.STATUS_ENABLED);
		xResourceList.add(xXResource);

		XXSecurityZoneRefGroup xZoneAdminGroup = new XXSecurityZoneRefGroup();
		xZoneAdminGroup.setZoneId(2L);
		xZoneAdminGroup.setGroupId(vXGroup.getId());
		xZoneAdminGroup.setGroupName(vXGroup.getName());
		xZoneAdminGroup.setGroupType(1);
		List<XXSecurityZoneRefGroup> zoneSecRefGroup=new ArrayList<XXSecurityZoneRefGroup>();
		zoneSecRefGroup.add(xZoneAdminGroup);
		XXSecurityZoneRefGroupDao zoneSecRefGroupDao=Mockito.mock(XXSecurityZoneRefGroupDao.class);
		Mockito.when(daoManager.getXXSecurityZoneRefGroup()).thenReturn(zoneSecRefGroupDao);
		Mockito.when(zoneSecRefGroupDao.findByGroupId(userId)).thenReturn(zoneSecRefGroup);

		RangerSecurityZone securityZone = new RangerSecurityZone();
		securityZone.setId(2L);
		securityZone.setName("sz1");
		XXSecurityZone xxSecurityZone = new XXSecurityZone();
		xxSecurityZone.setId(2L);
		xxSecurityZone.setName("sz1");

		XXSecurityZoneDao xXSecurityZoneDao = Mockito.mock(XXSecurityZoneDao.class);
		Mockito.when(daoManager.getXXSecurityZoneDao()).thenReturn(xXSecurityZoneDao);
		Mockito.when(xXSecurityZoneDao.getById(xZoneAdminGroup.getZoneId())).thenReturn(xxSecurityZone);

		List<XXRoleRefGroup> roleRefGroup = new ArrayList<XXRoleRefGroup>();
		XXRoleRefGroup xRoleRefGroup = new XXRoleRefGroup();
		xRoleRefGroup.setRoleId(userId);
		xRoleRefGroup.setGroupId(userId);
		xRoleRefGroup.setGroupName(groupName);
		xRoleRefGroup.setGroupType(0);
		roleRefGroup.add(xRoleRefGroup);

		XXRole xRole = new XXRole();
		xRole.setId(userId);
		xRole.setName("Role1");

		VXResponse vXResponse = new VXResponse();
		vXResponse.setStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		vXResponse.setMsgDesc("Can Not Delete Group '" + vXGroup.getName().trim() + "' as its present in " + RangerConstants.ROLE_FIELD);
		Mockito.when(restErrorUtil.generateRESTException((VXResponse) Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);

		xUserMgr.deleteXGroup(vXGroup.getId(), force);
	}

	@Test
	public void test127DeleteXUser() {
		destroySession();
		setup();
		boolean force = true;
		VXUser vXUser = vxUser();
		XXUser xXUser = new XXUser();
		XXUserDao xXUserDao = Mockito.mock(XXUserDao.class);
		Mockito.when(daoManager.getXXUser()).thenReturn(xXUserDao);
		Mockito.when(xXUserDao.getById(vXUser.getId())).thenReturn(xXUser);
		Mockito.when(xUserService.populateViewBean(xXUser)).thenReturn(vXUser);
		VXPermMapList vXPermMapList = new VXPermMapList();
		VXPermMap vXPermMap1=getVXPermMap();
		List<VXPermMap> vXPermMaps=new ArrayList<VXPermMap>();
		vXPermMaps.add(vXPermMap1);
		vXPermMapList.setVXPermMaps(vXPermMaps);
		VXAuditMapList vXAuditMapList = new VXAuditMapList();
		List<VXAuditMap> vXAuditMaps=new ArrayList<VXAuditMap>();
		VXAuditMap vXAuditMap=getVXAuditMap();
		vXAuditMaps.add(vXAuditMap);
		vXAuditMapList.setVXAuditMaps(vXAuditMaps);
		VXPortalUser vXPortalUser = userProfile();
		XXPortalUser xXPortalUser = xxPortalUser(vXPortalUser);
		XXPortalUserDao xXPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xXPortalUserDao);
		Mockito.when(xXPortalUserDao.findByLoginId(vXUser.getName().trim())).thenReturn(xXPortalUser);
		Mockito.when(xPortalUserService.populateViewBean(xXPortalUser)).thenReturn(vXPortalUser);
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXAuthSession> xXAuthSessions=new ArrayList<XXAuthSession>();
		XXAuthSession xXAuthSession = new XXAuthSession();
		xXAuthSession.setId(userId);
		xXAuthSession.setLoginId(vXPortalUser.getLoginId());
		xXAuthSessions.add(xXAuthSession);
		List<XXUserPermission> xXUserPermissions=new ArrayList<XXUserPermission>();
		xXUserPermissions.add(xxUserPermission());
		List<XXPortalUserRole> xXPortalUserRoles=new ArrayList<XXPortalUserRole>();
		xXPortalUserRoles.add(XXPortalUserRole);
		List<XXPolicy> xXPolicyList = new ArrayList<XXPolicy>();
		XXPolicy xXPolicy=getXXPolicy();
		xXPolicyList.add(xXPolicy);

		List<XXSecurityZoneRefUser> zoneSecRefUser=new ArrayList<XXSecurityZoneRefUser>();
		XXSecurityZoneRefUserDao zoneSecRefUserDao=Mockito.mock(XXSecurityZoneRefUserDao.class);
		Mockito.when(daoManager.getXXSecurityZoneRefUser()).thenReturn(zoneSecRefUserDao);
		Mockito.when(zoneSecRefUserDao.findByUserId(userId)).thenReturn(zoneSecRefUser);

		List<XXRoleRefUser> roleRefUser=new ArrayList<XXRoleRefUser>();
		XXRoleRefUser xRoleRefUser = new XXRoleRefUser();
		xRoleRefUser.setRoleId(userId);
		xRoleRefUser.setUserId(userId);
		xRoleRefUser.setUserName(vXUser.getName().trim());
		xRoleRefUser.setUserType(0);
		roleRefUser.add(xRoleRefUser);
		XXRoleRefUserDao roleRefUserDao=Mockito.mock(XXRoleRefUserDao.class);
		Mockito.when(daoManager.getXXRoleRefUser()).thenReturn(roleRefUserDao);
		Mockito.when(roleRefUserDao.findByUserId(userId)).thenReturn(roleRefUser);
		XXRole xRole = new XXRole();
		xRole.setId(userId);
		xRole.setName("Role1");
		XXRoleDao roleDao=Mockito.mock(XXRoleDao.class);
		Mockito.when(daoManager.getXXRole()).thenReturn(roleDao);
		Mockito.when(roleDao.getById(xRoleRefUser.getRoleId())).thenReturn(xRole);

		VXResponse vXResponse = new VXResponse();
		vXResponse.setStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		vXResponse.setMsgDesc("Can Not Delete User '" + vXUser.getName().trim() + "' as its present in " + RangerConstants.ROLE_FIELD);
		Mockito.when(restErrorUtil.generateRESTException((VXResponse) Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);

		xUserMgr.deleteXUser(vXUser.getId(), force);
		force=false;
		xUserMgr.deleteXUser(vXUser.getId(), force);
	}

	@Test
	public void test128DeleteXGroup() {
		destroySession();
		setup();
		boolean force = true;
		VXGroup vXGroup = vxGroup();
		VXPermMapList vXPermMapList = new VXPermMapList();
		VXPermMap vXPermMap1=getVXPermMap();
		List<VXPermMap> vXPermMaps=new ArrayList<VXPermMap>();
		vXPermMaps.add(vXPermMap1);
		vXPermMapList.setVXPermMaps(vXPermMaps);
		VXAuditMapList vXAuditMapList = new VXAuditMapList();
		List<VXAuditMap> vXAuditMaps=new ArrayList<VXAuditMap>();
		VXAuditMap vXAuditMap=getVXAuditMap();
		vXAuditMaps.add(vXAuditMap);
		vXAuditMapList.setVXAuditMaps(vXAuditMaps);
		List<XXGroupGroup> xXGroupGroups = new ArrayList<XXGroupGroup>();
		XXGroupGroup xXGroupGroup = xxGroupGroup();
		xXGroupGroups.add(xXGroupGroup);
		List<XXGroupPermission> xXGroupPermissions=new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = xxGroupPermission();
		xXGroupPermissions.add(xGroupPermissionObj);
		List<XXPolicy> xXPolicyList = new ArrayList<XXPolicy>();
		XXPolicy xXPolicy=getXXPolicy();
		xXPolicyList.add(xXPolicy);
		List<XXResource> xResourceList = new ArrayList<XXResource>();
		XXResource xXResource = new XXResource();
		xXResource.setId(userId);
		xXResource.setName("hadoopdev");
		xXResource.setIsRecursive(AppConstants.BOOL_TRUE);
		xXResource.setResourceStatus(AppConstants.STATUS_ENABLED);
		xResourceList.add(xXResource);

		List<XXSecurityZoneRefGroup> zoneSecRefGroup=new ArrayList<XXSecurityZoneRefGroup>();
		XXSecurityZoneRefGroupDao zoneSecRefGroupDao=Mockito.mock(XXSecurityZoneRefGroupDao.class);
		Mockito.when(daoManager.getXXSecurityZoneRefGroup()).thenReturn(zoneSecRefGroupDao);
		Mockito.when(zoneSecRefGroupDao.findByGroupId(userId)).thenReturn(zoneSecRefGroup);

		List<XXRoleRefGroup> roleRefGroup = new ArrayList<XXRoleRefGroup>();
		XXRoleRefGroup xRoleRefGroup = new XXRoleRefGroup();
		xRoleRefGroup.setRoleId(userId);
		xRoleRefGroup.setGroupId(userId);
		xRoleRefGroup.setGroupName(groupName);
		xRoleRefGroup.setGroupType(0);
		roleRefGroup.add(xRoleRefGroup);
		XXRoleRefGroupDao roleRefGroupDao = Mockito.mock(XXRoleRefGroupDao.class);
		Mockito.when(daoManager.getXXRoleRefGroup()).thenReturn(roleRefGroupDao);
		Mockito.when(roleRefGroupDao.findByGroupId(userId)).thenReturn(roleRefGroup);

		XXRole xRole = new XXRole();
		xRole.setId(userId);
		xRole.setName("Role1");
		XXRoleDao roleDao=Mockito.mock(XXRoleDao.class);
		Mockito.when(daoManager.getXXRole()).thenReturn(roleDao);
		Mockito.when(roleDao.getById(xRoleRefGroup.getRoleId())).thenReturn(xRole);

		VXResponse vXResponse = new VXResponse();
		vXResponse.setStatusCode(HttpServletResponse.SC_BAD_REQUEST);
		vXResponse.setMsgDesc("Can Not Delete Group '" + vXGroup.getName().trim() + "' as its present in " + RangerConstants.ROLE_FIELD);
		Mockito.when(restErrorUtil.generateRESTException((VXResponse) Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);

		xUserMgr.deleteXGroup(vXGroup.getId(), force);
	}

	@Test
	public void test129CreateOrUpdateUserPermisson() {
		destroySession();
		setup();
		VXPortalUser vXPortalUser = userProfile();
		List<XXModuleDef> xXModuleDefs = xxModuleDefs();

		VXUserPermission userPermission = vxUserPermission();
		List<VXUserPermission> userPermList = new ArrayList<VXUserPermission>();
		userPermList.add(userPermission);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermission = xxUserPermission();
		xUserPermission.setModuleId(userPermission.getModuleId());
		xUserPermission.setUserId(userPermission.getUserId());
		xUserPermissionsList.add(xUserPermission);

		XXUserPermissionDao xXUserPermissionDao= Mockito.mock(XXUserPermissionDao.class);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xXUserPermissionDao);
		Mockito.when(xXUserPermissionDao.findByModuleIdAndPortalUserId(vXPortalUser.getId(),xXModuleDefs.get(0).getId())).thenReturn(xUserPermission);

		VXUser vxUser = vxUser();
		XXUser xXUser = xxUser(vxUser);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByPortalUserId(vXPortalUser.getId())).thenReturn(xXUser);

		Mockito.when(xUserPermissionService.populateViewBean(xUserPermission)).thenReturn(userPermission);

		Mockito.when(xUserPermissionService.updateResource((VXUserPermission) Mockito.any())).thenReturn(userPermission);
		UserSessionBase userSession = Mockito.mock(UserSessionBase.class);
		Set<UserSessionBase> userSessions = new HashSet<UserSessionBase>();
		userSessions.add(userSession);
		Mockito.when(sessionMgr.getActiveUserSessionsForPortalUserId(userId)).thenReturn(userSessions);

		Collection<String> existingRoleList = new ArrayList<String>();
		existingRoleList.add(RangerConstants.ROLE_SYS_ADMIN);
		existingRoleList.add(RangerConstants.ROLE_KEY_ADMIN);
		existingRoleList.add(RangerConstants.ROLE_KEY_ADMIN_AUDITOR);
		existingRoleList.add(RangerConstants.ROLE_ADMIN_AUDITOR);
		vXPortalUser.setUserRoleList(existingRoleList);
		xUserMgr.createOrUpdateUserPermisson(vXPortalUser, xXModuleDefs.get(0).getId(), true);
		Mockito.when(xXUserPermissionDao.findByModuleIdAndPortalUserId(vXPortalUser.getId(),xXModuleDefs.get(0).getId())).thenReturn(null);
		Mockito.when(xxUserDao.findByPortalUserId(vXPortalUser.getId())).thenReturn(null);
		xUserMgr.createOrUpdateUserPermisson(vXPortalUser, xXModuleDefs.get(0).getId(), true);
	}

	@Test
	public void test130UpdateXUser() {
		destroySession();
		setup();
		VXUser vxUser = vxUser();
		Mockito.when(restErrorUtil.createRESTException("Please provide a valid username.",MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		vxUser = xUserMgr.updateXUser(null);
		Assert.assertNull(vxUser);
	}

	@Test
	public void test131hasAccess() {
		destroySession();
		setup();
		destroySession();
		boolean access = xUserMgr.hasAccess("test");
		Assert.assertEquals(access, false);
	}

	@Test
	public void test132CreateExternalUser() {
		destroySession();
		setup();
		ArrayList<String> roleList = new ArrayList<String>();
		roleList.add(RangerConstants.ROLE_USER);
		VXPortalUser vXPortalUser = userProfile();
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		VXUser vXUser = vxUser();
		VXUser createdXUser = vxUser();
		XXUser xXUser = xxUser(vXUser);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		
		Mockito.when(xxUserDao.findByUserName(vXUser.getName())).thenReturn(null, xXUser);
		Mockito.when(xUserService.populateViewBean(xXUser)).thenReturn(vXUser);

		vXPortalUser.setUserRoleList(roleList);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		XXUserPermission xUserPermissionObj = new XXUserPermission();
		xUserPermissionObj.setAddedByUserId(userId);
		xUserPermissionObj.setCreateTime(new Date());
		xUserPermissionObj.setId(userId);
		xUserPermissionObj.setIsAllowed(1);
		xUserPermissionObj.setModuleId(1L);
		xUserPermissionObj.setUpdatedByUserId(userId);
		xUserPermissionObj.setUpdateTime(new Date());
		xUserPermissionObj.setUserId(userId);
		xUserPermissionsList.add(xUserPermissionObj);

		createdXUser = xUserMgr.createExternalUser(vXUser.getName());
		Assert.assertNotNull(createdXUser);
		Assert.assertEquals(createdXUser.getName(), vXUser.getName());
	}
}
