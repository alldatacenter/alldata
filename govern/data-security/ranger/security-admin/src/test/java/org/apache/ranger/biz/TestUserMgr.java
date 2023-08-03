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
package org.apache.ranger.biz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerCommonEnums;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchUtil;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXGroupPermissionDao;
import org.apache.ranger.db.XXModuleDefDao;
import org.apache.ranger.db.XXPortalUserDao;
import org.apache.ranger.db.XXPortalUserRoleDao;
import org.apache.ranger.db.XXUserDao;
import org.apache.ranger.db.XXUserPermissionDao;
import org.apache.ranger.entity.XXGroupPermission;
import org.apache.ranger.entity.XXModuleDef;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXPortalUserRole;
import org.apache.ranger.entity.XXTrxLog;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.entity.XXUserPermission;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.apache.ranger.service.XGroupPermissionService;
import org.apache.ranger.service.XPortalUserService;
import org.apache.ranger.service.XUserPermissionService;
import org.apache.ranger.view.VXGroupPermission;
import org.apache.ranger.view.VXPasswordChange;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXPortalUserList;
import org.apache.ranger.view.VXResponse;
import org.apache.ranger.view.VXString;
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

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUserMgr {

	private static Long userId = 1L;
	private static String userLoginID = "testuser";

	@InjectMocks
	UserMgr userMgr = new UserMgr();

	@Mock
	VXPortalUser VXPortalUser;

	@Mock
	RangerDaoManager daoManager;

	@Mock
	RESTErrorUtil restErrorUtil;

	@Mock
	ContextUtil contextUtil;

	@Mock
	StringUtil stringUtil;

	@Mock
	SearchUtil searchUtil;

	@Mock
	RangerBizUtil rangerBizUtil;

	@Mock
	XUserPermissionService xUserPermissionService;

	@Mock
	XGroupPermissionService xGroupPermissionService;

	@Mock
	SessionMgr sessionMgr;

	@Mock
	XUserMgr xUserMgr;

	@Mock
	XPortalUserService xPortalUserService;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	public void setup() {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(true);
	}

	public void setupKeyAdmin() {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		XXPortalUser userKeyAdmin = new XXPortalUser();
		userKeyAdmin.setId(userProfile().getId());
		userKeyAdmin.setLoginId(userProfile().getLoginId());
		currentUserSession.setXXPortalUser(userKeyAdmin);
		currentUserSession.setKeyAdmin(true);
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
	}

	private VXPortalUser userProfile() {
		VXPortalUser userProfile = new VXPortalUser();
		userProfile.setEmailAddress("test@test.com");
		userProfile.setFirstName("user12");
		userProfile.setLastName("test12");
		userProfile.setLoginId(userLoginID);
		userProfile.setPassword("usertest12323");
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

	public void setupRangerUserSyncUser() {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		XXPortalUser user = new XXPortalUser();
		user.setId(1L);
		user.setLoginId("rangerusersync");
		user.setEmailAddress("test@test.com");
		currentUserSession.setXXPortalUser(user);
		currentUserSession.setUserAdmin(true);
	}

	@After
	public void destroySession() {
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(null);
		RangerContextHolder.setSecurityContext(context);
	}

	@Test
	public void test01CreateUser() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);

		VXPortalUser userProfile = userProfile();

		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");

		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setFirstName(userProfile.getFirstName());
		user.setLastName(userProfile.getLastName());
		user.setLoginId(userProfile.getLoginId());
		user.setPassword(userProfile.getPassword());
		user.setUserSource(userProfile.getUserSource());
		user.setPublicScreenName(userProfile.getPublicScreenName());
		user.setId(userProfile.getId());

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(user.getId());
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.create((XXPortalUser) Mockito.any())).thenReturn(user);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(roleDao.findByUserId(userId)).thenReturn(list);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(user);
		XXPortalUser dbxxPortalUser = userMgr.createUser(userProfile, 1, userRoleList);
		Assert.assertNotNull(dbxxPortalUser);
		userId = dbxxPortalUser.getId();

		Assert.assertEquals(userId, dbxxPortalUser.getId());
		Assert.assertEquals(userProfile.getFirstName(),dbxxPortalUser.getFirstName());
		Assert.assertEquals(userProfile.getFirstName(),dbxxPortalUser.getFirstName());
		Assert.assertEquals(userProfile.getLastName(),dbxxPortalUser.getLastName());
		Assert.assertEquals(userProfile.getLoginId(),dbxxPortalUser.getLoginId());
		Assert.assertEquals(userProfile.getEmailAddress(),dbxxPortalUser.getEmailAddress());
		Assert.assertEquals(userProfile.getPassword(),dbxxPortalUser.getPassword());

		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUser();
		Mockito.verify(daoManager).getXXPortalUserRole();
	}

	@Test
	public void test02CreateUser() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);

		VXPortalUser userProfile = userProfile();
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		userProfile.setUserRoleList(userRoleList);

		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setFirstName(userProfile.getFirstName());
		user.setLastName(userProfile.getLastName());
		user.setLoginId(userProfile.getLoginId());
		user.setPassword(userProfile.getPassword());
		user.setUserSource(userProfile.getUserSource());
		user.setPublicScreenName(userProfile.getPublicScreenName());
		user.setId(userProfile.getId());

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(user.getId());
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.create((XXPortalUser) Mockito.any())).thenReturn(user);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(roleDao.findByUserId(userId)).thenReturn(list);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(user);
		XXPortalUser dbxxPortalUser = userMgr.createUser(userProfile, 1);
		userId = dbxxPortalUser.getId();

		Assert.assertNotNull(dbxxPortalUser);
		Assert.assertEquals(userId, dbxxPortalUser.getId());
		Assert.assertEquals(userProfile.getFirstName(),dbxxPortalUser.getFirstName());
		Assert.assertEquals(userProfile.getFirstName(),dbxxPortalUser.getFirstName());
		Assert.assertEquals(userProfile.getLastName(),dbxxPortalUser.getLastName());
		Assert.assertEquals(userProfile.getLoginId(),dbxxPortalUser.getLoginId());
		Assert.assertEquals(userProfile.getEmailAddress(),dbxxPortalUser.getEmailAddress());
		Assert.assertEquals(userProfile.getPassword(),dbxxPortalUser.getPassword());

		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUser();
		Mockito.verify(daoManager).getXXPortalUserRole();
	}

	@Test
	public void test03ChangePasswordAsAdmin() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		VXPortalUser userProfile = userProfile();

		VXPasswordChange pwdChange = new VXPasswordChange();
		pwdChange.setId(userProfile.getId());
		pwdChange.setLoginId(userProfile.getLoginId());
		pwdChange.setOldPassword(userProfile.getPassword());
		pwdChange.setEmailAddress(userProfile.getEmailAddress());
		pwdChange.setUpdPassword(userProfile.getPassword());

		XXPortalUser user = new XXPortalUser();

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(Mockito.nullable(String.class))).thenReturn(user);
		Mockito.when(stringUtil.equals(Mockito.anyString(), Mockito.nullable(String.class))).thenReturn(true);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(stringUtil.validatePassword(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(true);
		VXResponse dbVXResponse = userMgr.changePassword(pwdChange);
		Assert.assertNotNull(dbVXResponse);
		Assert.assertEquals(userProfile.getStatus(),dbVXResponse.getStatusCode());

		Mockito.verify(stringUtil).equals(Mockito.anyString(),Mockito.nullable(String.class));
		Mockito.verify(stringUtil).validatePassword(Mockito.anyString(),Mockito.any(String[].class));

		XXPortalUser user2 = new XXPortalUser();
		user2.setId(userId);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(user2);
		VXPasswordChange invalidpwdChange = new VXPasswordChange();
		invalidpwdChange.setId(userProfile.getId());
		invalidpwdChange.setLoginId(userProfile.getLoginId());
		invalidpwdChange.setOldPassword("invalidOldPassword");
		invalidpwdChange.setEmailAddress(userProfile.getEmailAddress());
		invalidpwdChange.setUpdPassword(userProfile.getPassword());
		thrown.expect(WebApplicationException.class);
		userMgr.changePassword(invalidpwdChange);

		XXPortalUser externalUser = new XXPortalUser();
		externalUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(externalUser);
		VXResponse vXResponse = new VXResponse();
		vXResponse.setStatusCode(HttpServletResponse.SC_FORBIDDEN);
		vXResponse.setMsgDesc("SECURITY:changePassword().Ranger External Users cannot change password. LoginId=" + pwdChange.getLoginId());
		Mockito.when(restErrorUtil.generateRESTException((VXResponse) Mockito.any())).thenReturn(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.changePassword(pwdChange);
	}

	@Test
	public void test04ChangePasswordAsKeyAdmin() {
		setupKeyAdmin();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		VXPortalUser userProfile = userProfile();

		VXPasswordChange pwdChange = new VXPasswordChange();
		pwdChange.setId(userProfile.getId());
		pwdChange.setLoginId(userProfile.getLoginId());
		pwdChange.setOldPassword(userProfile.getPassword());
		pwdChange.setEmailAddress(userProfile.getEmailAddress());
		pwdChange.setUpdPassword(userProfile.getPassword());

		XXPortalUser userKeyAdmin = new XXPortalUser();
		userKeyAdmin.setId(userProfile.getId());
		userKeyAdmin.setLoginId(userProfile.getLoginId());
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(userKeyAdmin);
		Mockito.when(stringUtil.equals(Mockito.anyString(), Mockito.nullable(String.class))).thenReturn(true);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(stringUtil.validatePassword(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(true);

		VXResponse dbVXResponse = userMgr.changePassword(pwdChange);
		Assert.assertNotNull(dbVXResponse);
		Assert.assertEquals(userProfile.getStatus(),dbVXResponse.getStatusCode());

		Mockito.verify(stringUtil).equals(Mockito.anyString(), Mockito.nullable(String.class));
		Mockito.verify(stringUtil).validatePassword(Mockito.anyString(), Mockito.any(String[].class));
	}

	@Test
	public void test05ChangePasswordAsUser() {
		setupUser();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		VXPortalUser userProfile = userProfile();

		VXPasswordChange pwdChange = new VXPasswordChange();
		pwdChange.setId(userProfile.getId());
		pwdChange.setLoginId(userProfile.getLoginId());
		pwdChange.setOldPassword(userProfile.getPassword());
		pwdChange.setEmailAddress(userProfile.getEmailAddress());
		pwdChange.setUpdPassword(userProfile.getPassword());

		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile.getId());
		user.setLoginId(userProfile.getLoginId());
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(user);
		Mockito.when(stringUtil.equals(Mockito.anyString(), Mockito.nullable(String.class))).thenReturn(true);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(stringUtil.validatePassword(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(true);

		VXResponse dbVXResponse = userMgr.changePassword(pwdChange);
		Assert.assertNotNull(dbVXResponse);
		Assert.assertEquals(userProfile.getStatus(),dbVXResponse.getStatusCode());

		Mockito.verify(stringUtil).equals(Mockito.anyString(), Mockito.nullable(String.class));
		Mockito.verify(stringUtil).validatePassword(Mockito.anyString(),Mockito.any(String[].class));
	}

	@Test
	public void test06ChangeEmailAddressAsAdmin() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		XXGroupPermissionDao xGroupPermissionDao = Mockito.mock(XXGroupPermissionDao.class);
		XXModuleDefDao xModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXModuleDef xModuleDef = Mockito.mock(XXModuleDef.class);
		VXPortalUser userProfile = userProfile();

		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setFirstName(userProfile.getFirstName());
		user.setLastName(userProfile.getLastName());
		user.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		user.setPassword(encryptedPwd);
		user.setUserSource(userProfile.getUserSource());
		user.setPublicScreenName(userProfile.getPublicScreenName());
		user.setId(userProfile.getId());

		VXPasswordChange changeEmail = new VXPasswordChange();
		changeEmail.setEmailAddress("testuser@test.com");
		changeEmail.setId(user.getId());
		changeEmail.setLoginId(user.getLoginId());
		changeEmail.setOldPassword(userProfile.getPassword());

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

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

		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = new XXGroupPermission();
		xGroupPermissionObj.setAddedByUserId(userId);
		xGroupPermissionObj.setCreateTime(new Date());
		xGroupPermissionObj.setId(userId);
		xGroupPermissionObj.setIsAllowed(1);
		xGroupPermissionObj.setModuleId(1L);
		xGroupPermissionObj.setUpdatedByUserId(userId);
		xGroupPermissionObj.setUpdateTime(new Date());
		xGroupPermissionObj.setGroupId(userId);
		xGroupPermissionList.add(xGroupPermissionObj);

		VXUserPermission userPermission = new VXUserPermission();
		userPermission.setId(1L);
		userPermission.setIsAllowed(1);
		userPermission.setModuleId(1L);
		userPermission.setUserId(userId);
		userPermission.setUserName("xyz");
		userPermission.setOwner("admin");

		VXGroupPermission groupPermission = new VXGroupPermission();
		groupPermission.setId(1L);
		groupPermission.setIsAllowed(1);
		groupPermission.setModuleId(1L);
		groupPermission.setGroupId(userId);
		groupPermission.setGroupName("xyz");
		groupPermission.setOwner("admin");

		Mockito.when(stringUtil.validateEmail(Mockito.anyString())).thenReturn(true);
		Mockito.when(stringUtil.equals(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
		Mockito.when(stringUtil.normalizeEmail(Mockito.anyString())).thenReturn(changeEmail.getEmailAddress());
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(userDao.update(user)).thenReturn(user);
		Mockito.when(roleDao.findByParentId(Mockito.anyLong())).thenReturn(list);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xGroupPermissionDao);
		Mockito.when(xUserPermissionDao.findByUserPermissionIdAndIsAllowed(userProfile.getId())).thenReturn(xUserPermissionsList);
		Mockito.when(xGroupPermissionDao.findbyVXPortalUserId(userProfile.getId())).thenReturn(xGroupPermissionList);
		Mockito.when(xGroupPermissionService.populateViewBean(xGroupPermissionObj)).thenReturn(groupPermission);
		Mockito.when(xUserPermissionService.populateViewBean(xUserPermissionObj)).thenReturn(userPermission);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		Mockito.when(xModuleDefDao.findByModuleId(Mockito.anyLong())).thenReturn(xModuleDef);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		VXPortalUser dbVXPortalUser = userMgr.changeEmailAddress(user,changeEmail);
		Assert.assertNotNull(dbVXPortalUser);
		Assert.assertEquals(userId, dbVXPortalUser.getId());
		Assert.assertEquals(userProfile.getLastName(),dbVXPortalUser.getLastName());
		Assert.assertEquals(changeEmail.getLoginId(),dbVXPortalUser.getLoginId());
		Assert.assertEquals(changeEmail.getEmailAddress(),dbVXPortalUser.getEmailAddress());
		user.setUserSource(RangerCommonEnums.USER_APP);
		dbVXPortalUser = userMgr.changeEmailAddress(user,changeEmail);
		user.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		changeEmail.setEmailAddress("");
		dbVXPortalUser = userMgr.changeEmailAddress(user,changeEmail);

		Mockito.when(stringUtil.validateEmail(Mockito.anyString())).thenReturn(false);
		changeEmail.setEmailAddress("test@123.com");
		Mockito.when(restErrorUtil.createRESTException("serverMsg.userMgrInvalidEmail",MessageEnums.INVALID_INPUT_DATA, changeEmail.getId(), "emailAddress", changeEmail.toString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.changeEmailAddress(user,changeEmail);
	}

	@Test
	public void test07ChangeEmailAddressAsKeyAdmin() {
		setupKeyAdmin();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		XXGroupPermissionDao xGroupPermissionDao = Mockito.mock(XXGroupPermissionDao.class);
		XXModuleDefDao xModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXModuleDef xModuleDef = Mockito.mock(XXModuleDef.class);
		VXPortalUser userProfile = userProfile();

		XXPortalUser userKeyAdmin = new XXPortalUser();
		userKeyAdmin.setEmailAddress(userProfile.getEmailAddress());
		userKeyAdmin.setFirstName(userProfile.getFirstName());
		userKeyAdmin.setLastName(userProfile.getLastName());
		userKeyAdmin.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		userKeyAdmin.setPassword(encryptedPwd);
		userKeyAdmin.setUserSource(userProfile.getUserSource());
		userKeyAdmin.setPublicScreenName(userProfile.getPublicScreenName());
		userKeyAdmin.setId(userProfile.getId());

		VXPasswordChange changeEmail = new VXPasswordChange();
		changeEmail.setEmailAddress("testuser@test.com");
		changeEmail.setId(userKeyAdmin.getId());
		changeEmail.setLoginId(userKeyAdmin.getLoginId());
		changeEmail.setOldPassword(userProfile.getPassword());

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

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

		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = new XXGroupPermission();
		xGroupPermissionObj.setAddedByUserId(userId);
		xGroupPermissionObj.setCreateTime(new Date());
		xGroupPermissionObj.setId(userId);
		xGroupPermissionObj.setIsAllowed(1);
		xGroupPermissionObj.setModuleId(1L);
		xGroupPermissionObj.setUpdatedByUserId(userId);
		xGroupPermissionObj.setUpdateTime(new Date());
		xGroupPermissionObj.setGroupId(userId);
		xGroupPermissionList.add(xGroupPermissionObj);

		VXUserPermission userPermission = new VXUserPermission();
		userPermission.setId(1L);
		userPermission.setIsAllowed(1);
		userPermission.setModuleId(1L);
		userPermission.setUserId(userId);
		userPermission.setUserName("xyz");
		userPermission.setOwner("admin");

		VXGroupPermission groupPermission = new VXGroupPermission();
		groupPermission.setId(1L);
		groupPermission.setIsAllowed(1);
		groupPermission.setModuleId(1L);
		groupPermission.setGroupId(userId);
		groupPermission.setGroupName("xyz");
		groupPermission.setOwner("admin");

		Mockito.when(stringUtil.validateEmail(Mockito.anyString())).thenReturn(true);
		Mockito.when(stringUtil.equals(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
		Mockito.when(stringUtil.normalizeEmail(Mockito.anyString())).thenReturn(changeEmail.getEmailAddress());
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(roleDao.findByParentId(Mockito.anyLong())).thenReturn(list);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xGroupPermissionDao);
		Mockito.when(xUserPermissionDao.findByUserPermissionIdAndIsAllowed(userProfile.getId())).thenReturn(xUserPermissionsList);
		Mockito.when(xGroupPermissionDao.findbyVXPortalUserId(userProfile.getId())).thenReturn(xGroupPermissionList);
		Mockito.when(xGroupPermissionService.populateViewBean(xGroupPermissionObj)).thenReturn(groupPermission);
		Mockito.when(xUserPermissionService.populateViewBean(xUserPermissionObj)).thenReturn(userPermission);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		Mockito.when(xModuleDefDao.findByModuleId(Mockito.anyLong())).thenReturn(xModuleDef);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		VXPortalUser dbVXPortalUser = userMgr.changeEmailAddress(userKeyAdmin,changeEmail);
		Assert.assertNotNull(dbVXPortalUser);
		Assert.assertEquals(userId, dbVXPortalUser.getId());
		Assert.assertEquals(userProfile.getLastName(),dbVXPortalUser.getLastName());
		Assert.assertEquals(changeEmail.getLoginId(),dbVXPortalUser.getLoginId());
		Assert.assertEquals(changeEmail.getEmailAddress(),dbVXPortalUser.getEmailAddress());
	}


	@Test
	public void test08ChangeEmailAddressAsUser() {
		setupUser();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		XXGroupPermissionDao xGroupPermissionDao = Mockito.mock(XXGroupPermissionDao.class);
		XXModuleDefDao xModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXModuleDef xModuleDef = Mockito.mock(XXModuleDef.class);
		VXPortalUser userProfile = userProfile();

		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setFirstName(userProfile.getFirstName());
		user.setLastName(userProfile.getLastName());
		user.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		user.setPassword(encryptedPwd);
		user.setUserSource(userProfile.getUserSource());
		user.setPublicScreenName(userProfile.getPublicScreenName());
		user.setId(userProfile.getId());

		VXPasswordChange changeEmail = new VXPasswordChange();
		changeEmail.setEmailAddress("testuser@test.com");
		changeEmail.setId(user.getId());
		changeEmail.setLoginId(user.getLoginId());
		changeEmail.setOldPassword(userProfile.getPassword());

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

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

		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = new XXGroupPermission();
		xGroupPermissionObj.setAddedByUserId(userId);
		xGroupPermissionObj.setCreateTime(new Date());
		xGroupPermissionObj.setId(userId);
		xGroupPermissionObj.setIsAllowed(1);
		xGroupPermissionObj.setModuleId(1L);
		xGroupPermissionObj.setUpdatedByUserId(userId);
		xGroupPermissionObj.setUpdateTime(new Date());
		xGroupPermissionObj.setGroupId(userId);
		xGroupPermissionList.add(xGroupPermissionObj);

		VXUserPermission userPermission = new VXUserPermission();
		userPermission.setId(1L);
		userPermission.setIsAllowed(1);
		userPermission.setModuleId(1L);
		userPermission.setUserId(userId);
		userPermission.setUserName("xyz");
		userPermission.setOwner("admin");

		VXGroupPermission groupPermission = new VXGroupPermission();
		groupPermission.setId(1L);
		groupPermission.setIsAllowed(1);
		groupPermission.setModuleId(1L);
		groupPermission.setGroupId(userId);
		groupPermission.setGroupName("xyz");
		groupPermission.setOwner("admin");

		Mockito.when(stringUtil.validateEmail(Mockito.anyString())).thenReturn(true);
		Mockito.when(stringUtil.equals(Mockito.anyString(), Mockito.anyString())).thenReturn(true);
		Mockito.when(stringUtil.normalizeEmail(Mockito.anyString())).thenReturn(changeEmail.getEmailAddress());
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(roleDao.findByParentId(Mockito.anyLong())).thenReturn(list);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xGroupPermissionDao);
		Mockito.when(xUserPermissionDao.findByUserPermissionIdAndIsAllowed(userProfile.getId())).thenReturn(xUserPermissionsList);
		Mockito.when(xGroupPermissionDao.findbyVXPortalUserId(userProfile.getId())).thenReturn(xGroupPermissionList);
		Mockito.when(xGroupPermissionService.populateViewBean(xGroupPermissionObj)).thenReturn(groupPermission);
		Mockito.when(xUserPermissionService.populateViewBean(xUserPermissionObj)).thenReturn(userPermission);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		Mockito.when(xModuleDefDao.findByModuleId(Mockito.anyLong())).thenReturn(xModuleDef);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		VXPortalUser dbVXPortalUser = userMgr.changeEmailAddress(user,changeEmail);
		Assert.assertNotNull(dbVXPortalUser);
		Assert.assertEquals(userId, dbVXPortalUser.getId());
		Assert.assertEquals(userProfile.getLastName(),dbVXPortalUser.getLastName());
		Assert.assertEquals(changeEmail.getLoginId(),dbVXPortalUser.getLoginId());
		Assert.assertEquals(changeEmail.getEmailAddress(),dbVXPortalUser.getEmailAddress());

		user.setId(userProfile.getId());
		user.setLoginId("usertest123");
		String encryptCred = userMgr.encrypt(user.getLoginId(), userProfile.getPassword());
		user.setPassword(encryptCred);
		Mockito.when(stringUtil.equals(Mockito.anyString(), Mockito.nullable(String.class))).thenReturn(true);
		Mockito.when(stringUtil.equals(Mockito.anyString(), Mockito.anyString())).thenReturn(false);
		Mockito.when(restErrorUtil.createRESTException("serverMsg.userMgrWrongPassword",MessageEnums.OPER_NO_PERMISSION, null, null, changeEmail.toString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.changeEmailAddress(user, changeEmail);
	}

	@Test
	public void test09CreateUser() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		XXGroupPermissionDao xGroupPermissionDao = Mockito.mock(XXGroupPermissionDao.class);

		XXPortalUser user = new XXPortalUser();
		VXPortalUser userProfile = userProfile();

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

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

		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = new XXGroupPermission();
		xGroupPermissionObj.setAddedByUserId(userId);
		xGroupPermissionObj.setCreateTime(new Date());
		xGroupPermissionObj.setId(userId);
		xGroupPermissionObj.setIsAllowed(1);
		xGroupPermissionObj.setModuleId(1L);
		xGroupPermissionObj.setUpdatedByUserId(userId);
		xGroupPermissionObj.setUpdateTime(new Date());
		xGroupPermissionObj.setGroupId(userId);
		xGroupPermissionList.add(xGroupPermissionObj);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.create((XXPortalUser) Mockito.any())).thenReturn(user);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);

		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);

		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xGroupPermissionDao);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(user);
		VXPortalUser dbVXPortalUser = userMgr.createUser(userProfile);
		Assert.assertNotNull(dbVXPortalUser);
		Assert.assertEquals(user.getId(), dbVXPortalUser.getId());
		Assert.assertEquals(user.getFirstName(), dbVXPortalUser.getFirstName());
		Assert.assertEquals(user.getLastName(), dbVXPortalUser.getLastName());
		Assert.assertEquals(user.getLoginId(), dbVXPortalUser.getLoginId());
		Assert.assertEquals(user.getEmailAddress(),dbVXPortalUser.getEmailAddress());
		Assert.assertEquals(user.getPassword(), dbVXPortalUser.getPassword());

		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUser();
		Mockito.verify(daoManager).getXXUserPermission();
		Mockito.verify(daoManager).getXXGroupPermission();

		Collection<String> reqRoleList = new ArrayList<String>();
		reqRoleList.add(null);
		userProfile.setUserRoleList(reqRoleList);
		dbVXPortalUser = userMgr.createUser(userProfile);
	}

	@Test
	public void test10CreateDefaultAccountUser() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXPortalUser userProfile = userProfile();
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		userProfile.setUserRoleList(userRoleList);
		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");

		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(user);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		userProfile.setOtherAttributes("other1");
		VXPortalUser dbVXPortalUser = userMgr.createDefaultAccountUser(userProfile);
		Assert.assertNotNull(dbVXPortalUser);
		Assert.assertEquals(user.getId(), dbVXPortalUser.getId());
		Assert.assertEquals(user.getFirstName(), dbVXPortalUser.getFirstName());
		Assert.assertEquals(user.getLastName(), dbVXPortalUser.getLastName());
		Assert.assertEquals(user.getLoginId(), dbVXPortalUser.getLoginId());
		Assert.assertEquals(user.getEmailAddress(),dbVXPortalUser.getEmailAddress());
		Assert.assertEquals(user.getPassword(), dbVXPortalUser.getPassword());
		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUser();
		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUserRole();
	}

	@Test
	public void test11CreateDefaultAccountUser() {
		destroySession();
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXPortalUser userProfile = userProfile();
		userProfile.setStatus(RangerCommonEnums.USER_EXTERNAL);
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		userProfile.setUserRoleList(userRoleList);
		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");

		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(null, user);
		Mockito.when(userDao.findByEmailAddress(Mockito.anyString())).thenReturn(null);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(userDao.create((XXPortalUser) Mockito.any())).thenReturn(user);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		VXPortalUser dbVXPortalUser = userMgr.createDefaultAccountUser(userProfile);
		Assert.assertNotNull(dbVXPortalUser);
		Assert.assertEquals(user.getId(), dbVXPortalUser.getId());
		Assert.assertEquals(user.getFirstName(), dbVXPortalUser.getFirstName());
		Assert.assertEquals(user.getLastName(), dbVXPortalUser.getLastName());
		Assert.assertEquals(user.getLoginId(), dbVXPortalUser.getLoginId());
		Assert.assertEquals(user.getEmailAddress(),dbVXPortalUser.getEmailAddress());
		Assert.assertEquals(user.getPassword(), dbVXPortalUser.getPassword());
		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUser();
		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUserRole();
	}

	@Test
	public void test12CreateDefaultAccountUser() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXPortalUser userProfile = userProfile();
		userProfile.setStatus(RangerCommonEnums.USER_EXTERNAL);
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		userProfile.setUserRoleList(userRoleList);
		XXPortalUser xxPortalUser = new XXPortalUser();
		xxPortalUser.setEmailAddress(userProfile.getEmailAddress());
		xxPortalUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");

		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(xxPortalUser);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		userProfile.setPassword("");
		userProfile.setEmailAddress(null);
		VXPortalUser dbVXPortalUser = userMgr.createDefaultAccountUser(userProfile);
		Assert.assertNotNull(dbVXPortalUser);
		Assert.assertEquals(xxPortalUser.getId(), dbVXPortalUser.getId());
		Assert.assertEquals(xxPortalUser.getFirstName(), dbVXPortalUser.getFirstName());
		Assert.assertEquals(xxPortalUser.getLastName(), dbVXPortalUser.getLastName());
		Assert.assertEquals(xxPortalUser.getLoginId(), dbVXPortalUser.getLoginId());
		Assert.assertEquals(xxPortalUser.getEmailAddress(),dbVXPortalUser.getEmailAddress());
		Assert.assertEquals(xxPortalUser.getPassword(), dbVXPortalUser.getPassword());
		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUser();
		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUserRole();
	}

	@Test
	public void test13IsUserInRole() {
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(roleDao.findByRoleUserId(userId, "ROLE_USER")).thenReturn(XXPortalUserRole);
		boolean isValue = userMgr.isUserInRole(userId, "ROLE_USER");
		Assert.assertTrue(isValue);
		Mockito.when(roleDao.findByRoleUserId(userId, "ROLE_USER")).thenReturn(null);
		isValue = userMgr.isUserInRole(userId, "ROLE_USER");
		Assert.assertFalse(isValue);
	}

	@Test
	public void test14UpdateUserWithPass() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		VXPortalUser userProfile = userProfile();
		userProfile.setPassword("password1234");
		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile.getId());
		user.setLoginId(userProfile.getLoginId());
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		user.setPassword(encryptedPwd);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.getById(userProfile.getId())).thenReturn(user);
		Mockito.when(stringUtil.validateEmail(Mockito.anyString())).thenReturn(true);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		Mockito.when(stringUtil.validatePassword(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(true);
		Mockito.when(userDao.update(user)).thenReturn(user);
		XXPortalUser dbXXPortalUser = userMgr.updateUserWithPass(userProfile);
		Assert.assertNotNull(dbXXPortalUser);
		Assert.assertEquals(userId, dbXXPortalUser.getId());
		Assert.assertEquals(userProfile.getFirstName(),dbXXPortalUser.getFirstName());
		Assert.assertEquals(userProfile.getFirstName(),dbXXPortalUser.getFirstName());
		Assert.assertEquals(userProfile.getLastName(),dbXXPortalUser.getLastName());
		Assert.assertEquals(userProfile.getLoginId(),dbXXPortalUser.getLoginId());
		Assert.assertEquals(userProfile.getEmailAddress(),dbXXPortalUser.getEmailAddress());
		Assert.assertEquals(encryptedPwd, dbXXPortalUser.getPassword());
		Mockito.when(userDao.getById(userProfile.getId())).thenReturn(null);
		dbXXPortalUser = userMgr.updateUserWithPass(userProfile);
		Assert.assertNull(dbXXPortalUser);
	}

	@Test
	public void test15searchUsers() {
		Query query = Mockito.mock(Query.class);
		EntityManager entityManager = Mockito.mock(EntityManager.class);
		SearchCriteria searchCriteria = new SearchCriteria();
		searchCriteria.setDistinct(true);
		searchCriteria.setGetChildren(true);
		searchCriteria.setGetCount(true);
		searchCriteria.setMaxRows(12);
		searchCriteria.setOwnerId(userId);
		searchCriteria.setStartIndex(1);
		searchCriteria.setSortBy("userId");
		searchCriteria.setSortType("asc");
		Long count = 1l;
		Mockito.when(daoManager.getEntityManager()).thenReturn(entityManager);
		Mockito.when(entityManager.createQuery(Mockito.anyString())).thenReturn(query);
		Mockito.when(query.getSingleResult()).thenReturn(count);

		VXPortalUserList dbVXPortalUserList = userMgr.searchUsers(searchCriteria);
		Assert.assertNotNull(dbVXPortalUserList);
		searchCriteria.setSortBy("loginId");
		dbVXPortalUserList = userMgr.searchUsers(searchCriteria);
		Assert.assertNotNull(dbVXPortalUserList);
		searchCriteria.setSortBy("emailAddress");
		dbVXPortalUserList = userMgr.searchUsers(searchCriteria);
		Assert.assertNotNull(dbVXPortalUserList);
		searchCriteria.setSortBy("firstName");
		dbVXPortalUserList = userMgr.searchUsers(searchCriteria);
		Assert.assertNotNull(dbVXPortalUserList);
		searchCriteria.setSortBy("lastName");
		dbVXPortalUserList = userMgr.searchUsers(searchCriteria);
		Assert.assertNotNull(dbVXPortalUserList);
		searchCriteria.setSortBy("source");
		searchCriteria.setSortType("");
		dbVXPortalUserList = userMgr.searchUsers(searchCriteria);
		Assert.assertNotNull(dbVXPortalUserList);
		searchCriteria.setSortBy("");
		searchCriteria.setSortType("desc");
		dbVXPortalUserList = userMgr.searchUsers(searchCriteria);
		Assert.assertNotNull(dbVXPortalUserList);

		VXPortalUser userProfile = userProfile();
		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile.getId());
		user.setLoginId(userProfile.getLoginId());
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setLoginId(userProfile.getLoginId());
		List<XXPortalUser> resultList = new ArrayList<XXPortalUser>();
		resultList.add(user);
		Mockito.when(query.getResultList()).thenReturn(resultList);
		dbVXPortalUserList = userMgr.searchUsers(searchCriteria);
		Assert.assertNotNull(dbVXPortalUserList);

		count = 0l;
		Mockito.when(query.getSingleResult()).thenReturn(count);
		dbVXPortalUserList = userMgr.searchUsers(searchCriteria);
		Assert.assertNotNull(dbVXPortalUserList);
	}

	@Test
	public void test16FindByEmailAddress() {
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);

		XXPortalUser user = new XXPortalUser();

		String emailId = "test001user@apache.org";
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByEmailAddress(emailId)).thenReturn(user);

		XXPortalUser dbXXPortalUser = userMgr.findByEmailAddress(emailId);
		Assert.assertNotNull(dbXXPortalUser);
		Assert.assertNotEquals(emailId, dbXXPortalUser.getEmailAddress());

		Mockito.verify(daoManager).getXXPortalUser();
	}

	@Test
	public void test17GetRolesForUser() {
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXPortalUser userProfile = userProfile();

		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setFirstName(userProfile.getFirstName());
		user.setLastName(userProfile.getLastName());
		user.setLoginId(userProfile.getLoginId());
		user.setPassword(userProfile.getPassword());
		user.setUserSource(userProfile.getUserSource());
		user.setPublicScreenName(userProfile.getPublicScreenName());
		user.setId(userProfile.getId());

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(user.getId());
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(roleDao.findByUserId(userId)).thenReturn(list);

		Collection<String> stringReturn = userMgr.getRolesForUser(user);
		Assert.assertNotNull(stringReturn);

		Mockito.verify(daoManager).getXXPortalUserRole();
	}

	@Test
	public void test18DeleteUserRole() {
		setup();
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		String userRole = "ROLE_USER";
		XXPortalUser user = new XXPortalUser();
		XXPortalUserRole.setId(user.getId());
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(roleDao.findByUserId(userId)).thenReturn(list);

		boolean deleteValue = userMgr.deleteUserRole(userId, userRole);
		Assert.assertTrue(deleteValue);
	}

	@Test
	public void test19DeactivateUser() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		XXGroupPermissionDao xGroupPermissionDao = Mockito.mock(XXGroupPermissionDao.class);
		VXGroupPermission vXGroupPermission = Mockito.mock(VXGroupPermission.class);
		XXModuleDefDao xModuleDefDao = Mockito.mock(XXModuleDefDao.class);
		XXModuleDef xModuleDef = Mockito.mock(XXModuleDef.class);
		VXUserPermission vXUserPermission = Mockito.mock(VXUserPermission.class);

		VXPortalUser userProfile = userProfile();

		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setFirstName(userProfile.getFirstName());
		user.setLastName(userProfile.getLastName());
		user.setLoginId(userProfile.getLoginId());
		user.setPassword(userProfile.getPassword());
		user.setUserSource(userProfile.getUserSource());
		user.setPublicScreenName(userProfile.getPublicScreenName());
		user.setId(userProfile.getId());

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

		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = new XXGroupPermission();
		xGroupPermissionObj.setAddedByUserId(userId);
		xGroupPermissionObj.setCreateTime(new Date());
		xGroupPermissionObj.setId(userId);
		xGroupPermissionObj.setIsAllowed(1);
		xGroupPermissionObj.setModuleId(1L);
		xGroupPermissionObj.setUpdatedByUserId(userId);
		xGroupPermissionObj.setUpdateTime(new Date());
		xGroupPermissionObj.setGroupId(userId);
		xGroupPermissionList.add(xGroupPermissionObj);

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");

		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.update(user)).thenReturn(user);

		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(roleDao.findByParentId(Mockito.anyLong())).thenReturn(list);

		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		Mockito.when(xUserPermissionDao.findByUserPermissionIdAndIsAllowed(userProfile.getId())).thenReturn(xUserPermissionsList);

		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xGroupPermissionDao);
		Mockito.when(xGroupPermissionDao.findbyVXPortalUserId(userProfile.getId())).thenReturn(xGroupPermissionList);

		Mockito.when(xGroupPermissionService.populateViewBean(xGroupPermissionObj)).thenReturn(vXGroupPermission);

		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		Mockito.when(xModuleDefDao.findByModuleId(Mockito.anyLong())).thenReturn(xModuleDef);

		Mockito.when(xUserPermissionService.populateViewBean(xUserPermissionObj)).thenReturn(vXUserPermission);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		VXPortalUser dbVXPortalUser = userMgr.deactivateUser(null);
		Assert.assertNull(dbVXPortalUser);
		dbVXPortalUser = userMgr.deactivateUser(user);
		Assert.assertNotNull(dbVXPortalUser);
		Assert.assertEquals(user.getId(), dbVXPortalUser.getId());
		Assert.assertEquals(user.getFirstName(), dbVXPortalUser.getFirstName());
		Assert.assertEquals(user.getLastName(), dbVXPortalUser.getLastName());
		Assert.assertEquals(user.getLoginId(), dbVXPortalUser.getLoginId());

		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(daoManager).getXXUserPermission();
		Mockito.verify(daoManager).getXXGroupPermission();
		Mockito.verify(xUserPermissionService).populateViewBean(xUserPermissionObj);
		Mockito.verify(xGroupPermissionService).populateViewBean(xGroupPermissionObj);
	}

	@Test
	public void test20checkAccess() {
		setup();
		XXPortalUserDao xPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUser xPortalUser = Mockito.mock(XXPortalUser.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xPortalUserDao);
		Mockito.when(xPortalUserDao.getById(userId)).thenReturn(xPortalUser);
		userMgr.checkAccess(userId);

		Mockito.when(xPortalUserDao.getById(userId)).thenReturn(null);
		Mockito.when(restErrorUtil.create403RESTException("serverMsg.userMgrWrongUser: "+userId)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.checkAccess(userId);
	}

	@Test
	public void test21getUserProfile() {
		setup();
		XXPortalUserDao xPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUser xPortalUser = Mockito.mock(XXPortalUser.class);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		XXGroupPermissionDao xGroupPermissionDao = Mockito.mock(XXGroupPermissionDao.class);

		XXPortalUserRoleDao xPortalUserRoleDao = Mockito.mock(XXPortalUserRoleDao.class);

		List<XXPortalUserRole> xPortalUserRoleList = new ArrayList<XXPortalUserRole>();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		xPortalUserRoleList.add(XXPortalUserRole);

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

		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = new XXGroupPermission();
		xGroupPermissionObj.setAddedByUserId(userId);
		xGroupPermissionObj.setCreateTime(new Date());
		xGroupPermissionObj.setId(userId);
		xGroupPermissionObj.setIsAllowed(1);
		xGroupPermissionObj.setModuleId(1L);
		xGroupPermissionObj.setUpdatedByUserId(userId);
		xGroupPermissionObj.setUpdateTime(new Date());
		xGroupPermissionObj.setGroupId(userId);
		xGroupPermissionList.add(xGroupPermissionObj);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xPortalUserDao);
		Mockito.when(xPortalUserDao.getById(userId)).thenReturn(null);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xPortalUserRoleDao);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);

		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xGroupPermissionDao);
		VXPortalUser dbVXPortalUser = userMgr.getUserProfile(userId);
		Mockito.when(xPortalUserDao.getById(userId)).thenReturn(xPortalUser);
		dbVXPortalUser = userMgr.getUserProfile(userId);
		Assert.assertNotNull(dbVXPortalUser);
	}

	@Test
	public void test22getUserProfileByLoginId() {
		setup();
		XXPortalUserDao xPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xPortalUserDao);
		VXPortalUser userProfile = userProfile();
		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setFirstName(userProfile.getFirstName());
		user.setLastName(userProfile.getLastName());
		user.setLoginId(userProfile.getLoginId());
		user.setPassword(userProfile.getPassword());
		user.setUserSource(userProfile.getUserSource());
		user.setPublicScreenName(userProfile.getPublicScreenName());
		user.setId(userProfile.getId());
		VXPortalUser dbVXPortalUser = userMgr.getUserProfileByLoginId();
		Mockito.when(xPortalUserDao.findByLoginId(Mockito.anyString())).thenReturn(user);
		XXPortalUserRoleDao xPortalUserRoleDao = Mockito.mock(XXPortalUserRoleDao.class);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xPortalUserRoleDao);
		List<XXPortalUserRole> xPortalUserRoleList = new ArrayList<XXPortalUserRole>();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		xPortalUserRoleList.add(XXPortalUserRole);
		Mockito.when(xPortalUserRoleDao.findByParentId(Mockito.anyLong())).thenReturn(xPortalUserRoleList);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		XXGroupPermissionDao xGroupPermissionDao = Mockito.mock(XXGroupPermissionDao.class);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		List<XXUserPermission> xUserPermissionsList = new ArrayList<XXUserPermission>();
		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		Mockito.when(xUserPermissionDao.findByUserPermissionIdAndIsAllowed(userProfile.getId())).thenReturn(xUserPermissionsList);
		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xGroupPermissionDao);
		Mockito.when(xGroupPermissionDao.findbyVXPortalUserId(userProfile.getId())).thenReturn(xGroupPermissionList);
		dbVXPortalUser = userMgr.getUserProfileByLoginId(user.getLoginId());
		Assert.assertNotNull(dbVXPortalUser);
	}

	@Test
	public void test23setUserRoles() {
		setup();
		XXPortalUserRoleDao xPortalUserRoleDao = Mockito.mock(XXPortalUserRoleDao.class);
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		XXGroupPermissionDao xGroupPermissionDao = Mockito.mock(XXGroupPermissionDao.class);
		XXModuleDefDao xModuleDefDao = Mockito.mock(XXModuleDefDao.class);

		VXPortalUser userProfile = userProfile();
		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setFirstName(userProfile.getFirstName());
		user.setLastName(userProfile.getLastName());
		user.setLoginId(userProfile.getLoginId());
		user.setPassword(userProfile.getPassword());
		user.setUserSource(userProfile.getUserSource());
		user.setPublicScreenName(userProfile.getPublicScreenName());
		user.setId(userProfile.getId());

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

		List<XXGroupPermission> xGroupPermissionList = new ArrayList<XXGroupPermission>();
		XXGroupPermission xGroupPermissionObj = new XXGroupPermission();
		xGroupPermissionObj.setAddedByUserId(userId);
		xGroupPermissionObj.setCreateTime(new Date());
		xGroupPermissionObj.setId(userId);
		xGroupPermissionObj.setIsAllowed(1);
		xGroupPermissionObj.setModuleId(1L);
		xGroupPermissionObj.setUpdatedByUserId(userId);
		xGroupPermissionObj.setUpdateTime(new Date());
		xGroupPermissionObj.setGroupId(userId);
		xGroupPermissionList.add(xGroupPermissionObj);

		List<VXGroupPermission> groupPermList = new ArrayList<VXGroupPermission>();
		VXGroupPermission groupPermission = new VXGroupPermission();
		groupPermission.setId(1L);
		groupPermission.setIsAllowed(1);
		groupPermission.setModuleId(1L);
		groupPermission.setGroupId(userId);
		groupPermission.setGroupName("xyz");
		groupPermission.setOwner("admin");
		groupPermList.add(groupPermission);

		XXModuleDef xModuleDef = new XXModuleDef();
		xModuleDef.setUpdatedByUserId(userId);
		xModuleDef.setAddedByUserId(userId);
		xModuleDef.setCreateTime(new Date());
		xModuleDef.setId(userId);
		xModuleDef.setModule("Policy manager");
		xModuleDef.setUpdateTime(new Date());
		xModuleDef.setUrl("/policy manager");

		VXUserPermission userPermission = new VXUserPermission();
		userPermission.setId(1L);
		userPermission.setIsAllowed(1);
		userPermission.setModuleId(1L);
		userPermission.setUserId(userId);
		userPermission.setUserName("xyz");
		userPermission.setOwner("admin");

		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(xPortalUserRoleDao);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.getById(userId)).thenReturn(user);
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		Mockito.when(xUserPermissionDao.findByUserPermissionIdAndIsAllowed(userProfile.getId())).thenReturn(xUserPermissionsList);
		Mockito.when(daoManager.getXXGroupPermission()).thenReturn(xGroupPermissionDao);
		Mockito.when(xGroupPermissionDao.findbyVXPortalUserId(userProfile.getId())).thenReturn(xGroupPermissionList);
		Mockito.when(xGroupPermissionService.populateViewBean(xGroupPermissionObj)).thenReturn(groupPermission);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		Mockito.when(xModuleDefDao.findByModuleId(Mockito.anyLong())).thenReturn(xModuleDef);
		Mockito.when(xUserPermissionService.populateViewBean(xUserPermissionObj)).thenReturn(userPermission);
		Mockito.when(daoManager.getXXModuleDef()).thenReturn(xModuleDefDao);
		Mockito.when(xModuleDefDao.findByModuleId(Mockito.anyLong())).thenReturn(xModuleDef);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		userMgr.checkAccess(userId);
		userMgr.setUserRoles(userId, vStringRolesList);

		Mockito.verify(daoManager).getXXUserPermission();
		Mockito.verify(daoManager).getXXGroupPermission();
		Mockito.verify(xGroupPermissionService).populateViewBean(xGroupPermissionObj);
		Mockito.verify(xUserPermissionService).populateViewBean(xUserPermissionObj);
	}

	@Test
	public void test24updateRoles() {
		setup();
		Collection<String> rolesList = new ArrayList<String>();
		rolesList.add("ROLE_USER");
		rolesList.add("ROLE_SYS_ADMIN");
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);
		XXPortalUserRoleDao userDao = Mockito.mock(XXPortalUserRoleDao.class);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(userDao);
		Mockito.when(userDao.findByUserId(userId)).thenReturn(list);
		boolean isFound = userMgr.updateRoles(userId, rolesList);
		Assert.assertFalse(isFound);

		Mockito.when(restErrorUtil.createRESTException("Invalid user role, please provide valid user role.", MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		rolesList.clear();
		rolesList.add("INVALID_ROLE");
		isFound = userMgr.updateRoles(userId, rolesList);
	}

	@Test
	public void test25updatePasswordInSHA256() {
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		VXPortalUser userProfile = userProfile();
		String userName = userProfile.getFirstName();
		String userPassword = userProfile.getPassword();
		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setFirstName(userProfile.getFirstName());
		user.setLastName(userProfile.getLastName());
		user.setLoginId(userProfile.getLoginId());
		user.setPassword(userProfile.getPassword());
		user.setUserSource(RangerCommonEnums.USER_APP);
		user.setPublicScreenName(userProfile.getPublicScreenName());
		user.setId(userProfile.getId());
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.update(user)).thenReturn(user);
		XXPortalUser dbXXPortalUser = userMgr.updatePasswordInSHA256(null,userPassword,false);
		Assert.assertNull(dbXXPortalUser);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(null);
		dbXXPortalUser = userMgr.updatePasswordInSHA256(userName,userPassword,false);
		Assert.assertNull(dbXXPortalUser);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(user);
		dbXXPortalUser = userMgr.updatePasswordInSHA256(userName,userPassword,true);
		Assert.assertNotNull(dbXXPortalUser);
		dbXXPortalUser = userMgr.updatePasswordInSHA256(userName,"Secret",true);
		Assert.assertNotNull(dbXXPortalUser);

	 }

	@Test
	public void test26CreateUser() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);

		VXPortalUser userProfile = userProfile();

		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");

		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setFirstName(userProfile.getFirstName());
		user.setLastName(userProfile.getLastName());
		user.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		user.setPassword(encryptedPwd);
		user.setUserSource(userProfile.getUserSource());
		user.setPublicScreenName(userProfile.getPublicScreenName());
		user.setId(userProfile.getId());

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(user.getId());
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.create((XXPortalUser) Mockito.any())).thenReturn(user);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(roleDao.findByUserId(userId)).thenReturn(list);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(user);
		XXPortalUser dbxxPortalUser = userMgr.createUser(userProfile, 1,userRoleList);
		Assert.assertNotNull(dbxxPortalUser);
		userId = dbxxPortalUser.getId();
		Assert.assertEquals(userId, dbxxPortalUser.getId());
		Assert.assertEquals(userProfile.getFirstName(),dbxxPortalUser.getFirstName());
		Assert.assertEquals(userProfile.getFirstName(),dbxxPortalUser.getFirstName());
		Assert.assertEquals(userProfile.getLastName(),dbxxPortalUser.getLastName());
		Assert.assertEquals(userProfile.getLoginId(),dbxxPortalUser.getLoginId());
		Assert.assertEquals(userProfile.getEmailAddress(),dbxxPortalUser.getEmailAddress());
		Assert.assertEquals(encryptedPwd,dbxxPortalUser.getPassword());

		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUser();
		Mockito.verify(daoManager).getXXPortalUserRole();
	}

	@Test
	public void test27UpdateUser() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);

		VXPortalUser userProfile = userProfile();
		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile.getId());
		user.setLoginId(userProfile.getLoginId());
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		user.setPassword(encryptedPwd);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.getById(userProfile.getId())).thenReturn(user);
		Mockito.when(stringUtil.validateEmail(Mockito.anyString())).thenReturn(true);

		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		XXPortalUser dbXXPortalUser = userMgr.updateUser(userProfile);
		Assert.assertNotNull(dbXXPortalUser);
		Assert.assertEquals(userId, dbXXPortalUser.getId());
		Assert.assertEquals(userProfile.getFirstName(),dbXXPortalUser.getFirstName());
		Assert.assertEquals(userProfile.getFirstName(),dbXXPortalUser.getFirstName());
		Assert.assertEquals(userProfile.getLastName(),dbXXPortalUser.getLastName());
		Assert.assertEquals(userProfile.getLoginId(),dbXXPortalUser.getLoginId());
		Assert.assertEquals(userProfile.getEmailAddress(),dbXXPortalUser.getEmailAddress());
		Assert.assertEquals(encryptedPwd,dbXXPortalUser.getPassword());

		Mockito.when(restErrorUtil.createRESTException("Please provide valid email address.", MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		Mockito.when(stringUtil.validateEmail(Mockito.anyString())).thenReturn(false);
		userMgr.updateUser(userProfile);
	}

	@Test
	public void test28UpdateUser() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);

		VXPortalUser userProfile = userProfile();
		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile.getId());
		user.setLoginId(userProfile.getLoginId());
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		user.setPassword(encryptedPwd);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.getById(userProfile.getId())).thenReturn(null);
		XXPortalUser dbXXPortalUser = userMgr.updateUser(userProfile);
		Assert.assertNull(dbXXPortalUser);
		user.setStatus(RangerCommonEnums.USER_EXTERNAL);
		user.setFirstName("null");
		user.setLastName("null");
		Mockito.when(userDao.getById(userProfile.getId())).thenReturn(user);
		Mockito.when(stringUtil.validateEmail(Mockito.anyString())).thenReturn(true);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		Mockito.when(userDao.findByEmailAddress(Mockito.anyString())).thenReturn(user);
		dbXXPortalUser = userMgr.updateUser(userProfile);
		Assert.assertNotNull(dbXXPortalUser);
		Assert.assertEquals(userId, dbXXPortalUser.getId());
		Assert.assertEquals(userProfile.getLoginId(),dbXXPortalUser.getLoginId());
		Assert.assertEquals(userProfile.getEmailAddress(),dbXXPortalUser.getEmailAddress());
		Assert.assertEquals(encryptedPwd,dbXXPortalUser.getPassword());

		Mockito.when(restErrorUtil.createRESTException("Invalid user, please provide valid username.", MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userProfile.setLoginId(null);
		dbXXPortalUser = userMgr.updateUser(userProfile);

		Mockito.when(restErrorUtil.createRESTException("The email address you've provided already exists in system.", MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userProfile.setLoginId("test1234");
		user.setLoginId(null);
		Mockito.when(userDao.findByEmailAddress(Mockito.anyString())).thenReturn(user);
		dbXXPortalUser = userMgr.updateUser(userProfile);
	}

	@Test
	public void test29UpdateOldUserName() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXUserDao xXUserDao = Mockito.mock(XXUserDao.class);
		VXPortalUser userProfile = userProfile();
		String userLoginId = userProfile.getLoginId();
		String newUserName= "newUserName";
		String currentPassword = userProfile.getPassword();

		XXPortalUser xXPortalUser = new XXPortalUser();
		xXPortalUser.setEmailAddress(userProfile.getEmailAddress());
		xXPortalUser.setFirstName(userProfile.getFirstName());
		xXPortalUser.setLastName(userProfile.getLastName());
		xXPortalUser.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		xXPortalUser.setPassword(encryptedPwd);
		xXPortalUser.setUserSource(userProfile.getUserSource());
		xXPortalUser.setPublicScreenName(userProfile.getPublicScreenName());
		xXPortalUser.setId(userProfile.getId());
		xXPortalUser.setUserSource(RangerCommonEnums.USER_APP);

		XXUser xXUser = new XXUser();
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		Collection<String> groupNameList = new ArrayList<String>();
		groupNameList.add("Grp2");
		xXUser.setId(userId);
		xXUser.setDescription(userProfile.getPublicScreenName());
		xXUser.setName(userProfile.getLoginId());

		List<XXTrxLog> trxLogList = new ArrayList<XXTrxLog>();
		XXTrxLog xTrxLogObj = new XXTrxLog();
		xTrxLogObj.setAction("update");
		xTrxLogObj.setAddedByUserId(userId);
		xTrxLogObj.setAttributeName("User Name");
		xTrxLogObj.setCreateTime(new Date());
		xTrxLogObj.setId(userId);
		xTrxLogObj.setPreviousValue(userLoginId);
		xTrxLogObj.setNewValue(newUserName);
		xTrxLogObj.setObjectClassType(AppConstants.CLASS_TYPE_USER_PROFILE);
		xTrxLogObj.setObjectName(xXPortalUser.getLoginId());
		xTrxLogObj.setObjectId(userId);
		xTrxLogObj.setParentObjectClassType(AppConstants.CLASS_TYPE_USER_PROFILE);
		xTrxLogObj.setParentObjectId(userId);
		xTrxLogObj.setUpdatedByUserId(xXPortalUser.getId());
		trxLogList.add(xTrxLogObj);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(userProfile.getLoginId())).thenReturn(xXPortalUser);
		Mockito.when(daoManager.getXXUser()).thenReturn(xXUserDao);
		Mockito.when(xXUserDao.findByUserName(xXUser.getName())).thenReturn(xXUser);

		xXUser.setName(newUserName);
		Mockito.when(xXUserDao.update(xXUser)).thenReturn(xXUser);

		xXPortalUser.setLoginId(newUserName);
		Mockito.when(userDao.update(xXPortalUser)).thenReturn(xXPortalUser);

		xXPortalUser=userMgr.updateOldUserName(userLoginId, newUserName, currentPassword);

		Assert.assertNotNull(xXPortalUser);
		Assert.assertEquals(newUserName,xXPortalUser.getLoginId());
		xXPortalUser.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		Mockito.when(userDao.findByLoginId(userProfile.getLoginId())).thenReturn(xXPortalUser);
		xXPortalUser=userMgr.updateOldUserName(userLoginId, newUserName, currentPassword);
		xXPortalUser=userMgr.updateOldUserName(null, newUserName, currentPassword);
		Mockito.when(userDao.findByLoginId(userProfile.getLoginId())).thenReturn(null);
		xXPortalUser=userMgr.updateOldUserName(userLoginId, newUserName, currentPassword);
	}

	@Test
	public void test30getRolesByLoginId() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);

		VXPortalUser userProfile = userProfile();
		String userLoginId = userProfile.getLoginId();

		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");

		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setFirstName(userProfile.getFirstName());
		user.setLastName(userProfile.getLastName());
		user.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		user.setPassword(encryptedPwd);
		user.setUserSource(userProfile.getUserSource());
		user.setPublicScreenName(userProfile.getPublicScreenName());
		user.setId(userProfile.getId());

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(user.getId());
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(userProfile.getLoginId())).thenReturn(user);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(roleDao.findByUserId(userId)).thenReturn(list);
		Collection<String> roleList = userMgr.getRolesByLoginId(userLoginId);
		Assert.assertNotNull(roleList);
		Assert.assertEquals(userLoginId, user.getLoginId());
		Assert.assertEquals(userRoleList, roleList);
		roleList = userMgr.getRolesByLoginId(null);
		Mockito.when(roleDao.findByUserId(userId)).thenReturn(null);
		roleList = userMgr.getRolesByLoginId(userLoginId);
		Mockito.when(userDao.findByLoginId(userProfile.getLoginId())).thenReturn(null);
		roleList = userMgr.getRolesByLoginId(userLoginId);
		Assert.assertNotNull(roleList);
	}

	@Test
	public void test31checkAccess() {
		setup();
		XXPortalUser xPortalUser = Mockito.mock(XXPortalUser.class);
		userMgr.checkAccess(xPortalUser);
		destroySession();
		VXPortalUser userProfile = userProfile();
		xPortalUser = xxPortalUser(userProfile);
		xPortalUser.setId(userProfile.getId());
		setupUser();
		userMgr.checkAccess(xPortalUser);

		destroySession();
		Mockito.when(restErrorUtil.create403RESTException("User  access denied. loggedInUser=Not Logged In, accessing user=" + userProfile.getId())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.checkAccess(xPortalUser);

		Mockito.when(restErrorUtil.create403RESTException("serverMsg.userMgrWrongUser")).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		xPortalUser = null;
		userMgr.checkAccess(xPortalUser);
	}

	@Test
	public void test32checkAdminAccess() {
		setup();
		userMgr.checkAdminAccess();
		destroySession();
		Mockito.when(restErrorUtil.create403RESTException("Operation not allowed. loggedInUser=. Not Logged In.")).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.checkAdminAccess();
	}

	@Test
	public void test33checkAccessForUpdate() {
		setup();
		XXPortalUser xPortalUser = Mockito.mock(XXPortalUser.class);
		userMgr.checkAccessForUpdate(xPortalUser);

		destroySession();
		xPortalUser.setId(userId);
		VXResponse vXResponse = new VXResponse();
		vXResponse.setStatusCode(HttpServletResponse.SC_FORBIDDEN);
		vXResponse.setMsgDesc("User  access denied. loggedInUser=Not Logged In , accessing user="+ xPortalUser.getId());
		Mockito.when(restErrorUtil.generateRESTException((VXResponse) Mockito.any())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.checkAccessForUpdate(xPortalUser);
		xPortalUser = null;
		Mockito.when(restErrorUtil.create403RESTException("serverMsg.userMgrWrongUser")).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.checkAccessForUpdate(xPortalUser);
	}

	@Test
	public void test34updateRoleForExternalUsers() {
		setupRangerUserSyncUser();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		XXUserPermissionDao xUserPermissionDao = Mockito.mock(XXUserPermissionDao.class);
		Collection<String> existingRoleList = new ArrayList<String>();
		existingRoleList.add(RangerConstants.ROLE_USER);
		Collection<String> reqRoleList = new ArrayList<String>();
		reqRoleList.add(RangerConstants.ROLE_SYS_ADMIN);

		VXPortalUser userProfile = userProfile();
		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile.getId());
		user.setLoginId(userProfile.getLoginId());
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		user.setPassword(encryptedPwd);

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userProfile.getId());
		XXPortalUserRole.setUserRole(RangerConstants.ROLE_USER);
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

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
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(roleDao.findByUserId(userId)).thenReturn(list);
		Mockito.when(userDao.getById(userProfile.getId())).thenReturn(user);
		Mockito.when(stringUtil.validateEmail(Mockito.anyString())).thenReturn(true);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		Mockito.when(daoManager.getXXUserPermission()).thenReturn(xUserPermissionDao);
		Mockito.when(xUserPermissionDao.findByUserPermissionId(userProfile.getId())).thenReturn(xUserPermissionsList);
		VXPortalUser dbVXPortalUser = userMgr.updateRoleForExternalUsers(reqRoleList,existingRoleList,userProfile);
		Assert.assertNotNull(dbVXPortalUser);
		Assert.assertEquals(userId, dbVXPortalUser.getId());
		Assert.assertEquals(userProfile.getFirstName(),dbVXPortalUser.getFirstName());
		Assert.assertEquals(userProfile.getLastName(),dbVXPortalUser.getLastName());
		Assert.assertEquals(userProfile.getLoginId(),dbVXPortalUser.getLoginId());
		Assert.assertEquals(userProfile.getEmailAddress(),dbVXPortalUser.getEmailAddress());
	}

	@Test
	public void test35mapVXPortalUserToXXPortalUser() {
		setup();
		Collection<String> existingRoleList = new ArrayList<String>();
		existingRoleList.add(RangerConstants.ROLE_USER);
		Collection<String> reqRoleList = new ArrayList<String>();
		reqRoleList.add(RangerConstants.ROLE_SYS_ADMIN);

		VXPortalUser userProfile = userProfile();
		userProfile.setFirstName("null");
		userProfile.setLastName("null");
		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile.getId());
		user.setLoginId(userProfile.getLoginId());
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		user.setPassword(encryptedPwd);

		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userProfile.getId());
		XXPortalUserRole.setUserRole(RangerConstants.ROLE_USER);
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

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
		XXPortalUser dbVXPortalUser = userMgr.mapVXPortalUserToXXPortalUser(userProfile);
		Assert.assertNotNull(dbVXPortalUser);
		Assert.assertEquals(userProfile.getLoginId(),dbVXPortalUser.getLoginId());
		Assert.assertEquals(userProfile.getEmailAddress(),dbVXPortalUser.getEmailAddress());

		userProfile.setLoginId(null);
		Mockito.when(restErrorUtil.createRESTException("LoginId should not be null or blank, It is", MessageEnums.INVALID_INPUT_DATA)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.mapVXPortalUserToXXPortalUser(userProfile);
	}

	@Test
	public void test36UpdateUser() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		VXPortalUser userProfile = userProfile();
		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile.getId());
		user.setLoginId(userProfile.getLoginId());
		userProfile.setFirstName("User");
		userProfile.setLastName("User");
		Mockito.when(stringUtil.validateEmail(Mockito.anyString())).thenReturn(true);
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		user.setPassword(encryptedPwd);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.getById(userProfile.getId())).thenReturn(user);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		Mockito.when(stringUtil.toCamelCaseAllWords(Mockito.anyString())).thenReturn(userProfile.getFirstName());
		XXPortalUser dbXXPortalUser = userMgr.updateUser(userProfile);
		Assert.assertNotNull(dbXXPortalUser);
		Mockito.when(stringUtil.isEmpty(Mockito.anyString())).thenReturn(true);
		userProfile.setFirstName("null");
		userProfile.setLastName("null");
		userProfile.setEmailAddress("");
		dbXXPortalUser = userMgr.updateUser(userProfile);
	}

	@Test
	public void test37createUserSearchQuery() {
		EntityManager entityManager = Mockito.mock(EntityManager.class);
		String queryString="Select id,loginId,emailAddress,firstName,lastName,statusList,publicScreenName,status from XXPortalUser";
		Query query = Mockito.mock(Query.class);
		SearchCriteria searchCriteria = new SearchCriteria();
		searchCriteria.setDistinct(true);
		searchCriteria.setGetChildren(true);
		searchCriteria.setGetCount(true);
		searchCriteria.setMaxRows(12);
		searchCriteria.setOwnerId(userId);
		searchCriteria.setStartIndex(1);
		searchCriteria.setSortBy("asc");
		VXPortalUser vXPortalUser=userProfile();
		List<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		List<Integer> statusList = new ArrayList<Integer>();
		statusList.add(1);
		searchCriteria.addParam("roleList", userRoleList);
		searchCriteria.addParam("userId", vXPortalUser.getId());
		searchCriteria.addParam("loginId", vXPortalUser.getLoginId());
		searchCriteria.addParam("emailAddress", vXPortalUser.getEmailAddress());
		searchCriteria.addParam("firstName", vXPortalUser.getFirstName());
		searchCriteria.addParam("lastName", vXPortalUser.getLastName());
		searchCriteria.addParam("statusList", statusList);
		searchCriteria.addParam("publicScreenName", vXPortalUser.getPublicScreenName());
		searchCriteria.addParam("status", vXPortalUser.getStatus());
		searchCriteria.addParam("familyScreenName", vXPortalUser.getPublicScreenName());
		Mockito.when(daoManager.getEntityManager()).thenReturn(entityManager);
		Mockito.when(entityManager.createQuery(Mockito.anyString())).thenReturn(query);
		Query newQuery = userMgr.createUserSearchQuery(query.toString(),queryString,searchCriteria);
		Assert.assertNotNull(newQuery);
		userRoleList.add("ROLE_SYS_ADMIN");
		statusList.add(0);
		searchCriteria.addParam("statusList", statusList);
		searchCriteria.addParam("roleList", userRoleList);
		newQuery = userMgr.createUserSearchQuery(query.toString(),queryString,searchCriteria);
	}

	@Test
	public void test38mapVXPortalUserToXXPortalUser() {
		Collection<String> existingRoleList = new ArrayList<String>();
		existingRoleList.add(RangerConstants.ROLE_USER);
		VXPortalUser dbVXPortalUser = userMgr.mapXXPortalUserToVXPortalUser(null,existingRoleList);
		XXPortalUser user = new XXPortalUser();
		Assert.assertNull(dbVXPortalUser);
		dbVXPortalUser = userMgr.mapXXPortalUserToVXPortalUser(user,existingRoleList);
		Assert.assertNull(dbVXPortalUser);
	}

	@Test
	public void test39gjUserToUserProfile() {
		VXPortalUser vXPortalUser = new VXPortalUser();
		XXPortalUser xXPortalUser = new XXPortalUser();
		userMgr.gjUserToUserProfile(xXPortalUser,vXPortalUser);
	}

	@Test
	public void test40deleteUserRole() {
		XXPortalUserRole xXPortalUserRole = new XXPortalUserRole();
		userMgr.deleteUserRole(1L,xXPortalUserRole);
	}

	@Test
	public void test41mapXXPortalUserToVXPortalUserForDefaultAccount() {
		VXPortalUser vXPortalUser=userProfile();
		XXPortalUser xXPortalUser = xxPortalUser(vXPortalUser);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);
		Mockito.when(roleDao.findByParentId(xXPortalUser.getId())).thenReturn(list);
		VXPortalUser dbVXPortalUser = userMgr.mapXXPortalUserToVXPortalUserForDefaultAccount(xXPortalUser);
		Assert.assertNotNull(dbVXPortalUser);
	}

	@Test
	public void test42EncryptWithOlderAlgo() {
		VXPortalUser vXPortalUser = userProfile();
		String encodedpasswd = userMgr.encryptWithOlderAlgo(vXPortalUser.getLoginId(), vXPortalUser.getPassword());
		Assert.assertNotNull(encodedpasswd);
		encodedpasswd = userMgr.encryptWithOlderAlgo(null, vXPortalUser.getPassword());
		Assert.assertNotNull(encodedpasswd);
		encodedpasswd = userMgr.encryptWithOlderAlgo(vXPortalUser.getLoginId(), null);
		Assert.assertNotNull(encodedpasswd);
		encodedpasswd = userMgr.encryptWithOlderAlgo(null, null);
		Assert.assertNotNull(encodedpasswd);
	}

	@Test
	public void test43IsNewPasswordDifferent() {
		VXPortalUser vXPortalUser = userProfile();
		String newCred = "New5ecret4User21";
		boolean isDifferent = userMgr.isNewPasswordDifferent(vXPortalUser.getLoginId(), vXPortalUser.getPassword(), newCred);
		Assert.assertTrue(isDifferent);
		isDifferent = userMgr.isNewPasswordDifferent(vXPortalUser.getLoginId(), vXPortalUser.getPassword(), vXPortalUser.getPassword());
		Assert.assertFalse(isDifferent);
		isDifferent = userMgr.isNewPasswordDifferent(vXPortalUser.getLoginId(), null, newCred);
		Assert.assertTrue(isDifferent);
		isDifferent = userMgr.isNewPasswordDifferent(null, vXPortalUser.getPassword(), newCred);
		Assert.assertTrue(isDifferent);
		isDifferent = userMgr.isNewPasswordDifferent(null, null , newCred);
		Assert.assertTrue(isDifferent);
	}

	@Test
	public void test44IsPasswordValid() {
		VXPortalUser vXPortalUser = userProfile();
		boolean isValid = userMgr.isPasswordValid(vXPortalUser.getLoginId(), "ceb4f32325eda6142bd65215f4c0f371" , vXPortalUser.getPassword());
		Assert.assertFalse(isValid);
	}

	@Test
	public void test45ChangePassword() {
		destroySession();
		setupUser();
		VXPortalUser userProfile = userProfile();
		XXPortalUser user2 = new XXPortalUser();
		user2.setId(userId);

		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(daoManager.getXXPortalUser().findByLoginId(Mockito.anyString())).thenReturn(user2);
		VXPasswordChange invalidpwdChange = new VXPasswordChange();
		invalidpwdChange.setId(userProfile.getId());
		invalidpwdChange.setLoginId(userProfile.getLoginId());
		invalidpwdChange.setOldPassword("invalidOldPassword");
		invalidpwdChange.setEmailAddress(userProfile.getEmailAddress());
		invalidpwdChange.setUpdPassword(userProfile.getPassword());
		Mockito.when(restErrorUtil.createRESTException("serverMsg.userMgrOldPassword",MessageEnums.INVALID_INPUT_DATA, null, null, invalidpwdChange.getLoginId())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.changePassword(invalidpwdChange);
	}

	@Test
	public void test46ChangePassword() {
		destroySession();
		setupUser();
		VXPortalUser userProfile = userProfile();
		XXPortalUser user2 = new XXPortalUser();
		user2.setId(userId);
		VXPasswordChange invalidpwdChange = new VXPasswordChange();
		invalidpwdChange.setId(userProfile.getId());
		invalidpwdChange.setLoginId(userProfile.getLoginId()+1);
		invalidpwdChange.setOldPassword("invalidOldPassword");
		invalidpwdChange.setEmailAddress(userProfile.getEmailAddress());
		invalidpwdChange.setUpdPassword(userProfile.getPassword());

		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(userProfile.getLoginId())).thenReturn(user2);
		Mockito.when(userDao.findByLoginId(invalidpwdChange.getLoginId())).thenReturn(null);

		Mockito.when(restErrorUtil.createRESTException("serverMsg.userMgrInvalidUser",MessageEnums.DATA_NOT_FOUND, null, null, invalidpwdChange.getLoginId())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.changePassword(invalidpwdChange);
	}

	@Test
	public void test47ChangePasswordAsUser() {
		destroySession();
		setupUser();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		VXPortalUser userProfile = userProfile();

		VXPasswordChange pwdChange = new VXPasswordChange();
		pwdChange.setId(userProfile.getId());
		pwdChange.setLoginId(userProfile.getLoginId());
		pwdChange.setOldPassword(userProfile.getPassword());
		pwdChange.setEmailAddress(userProfile.getEmailAddress());
		pwdChange.setUpdPassword(userProfile.getPassword());

		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile.getId());
		user.setLoginId(userProfile.getLoginId());
		String encryptCred = userMgr.encrypt(userProfile.getLoginId(), userProfile.getPassword());
		user.setPassword(encryptCred);
		user.setOldPasswords(encryptCred);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(user);
		Mockito.when(stringUtil.equals(Mockito.anyString(), Mockito.nullable(String.class))).thenReturn(true);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(stringUtil.validatePassword(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(true);
		Mockito.when(restErrorUtil.createRESTException("serverMsg.userMgrOldPassword",MessageEnums.INVALID_INPUT_DATA, user.getId(), "password", user.toString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.changePassword(pwdChange);
	}

	@Test
	public void test48ChangePasswordAsUser() {
		destroySession();
		setupUser();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		VXPortalUser userProfile = userProfile();

		VXPasswordChange pwdChange = new VXPasswordChange();
		pwdChange.setId(userProfile.getId());
		pwdChange.setLoginId(userProfile.getLoginId());
		pwdChange.setOldPassword(userProfile.getPassword());
		pwdChange.setEmailAddress(userProfile.getEmailAddress());
		pwdChange.setUpdPassword(userProfile.getPassword());

		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile.getId());
		user.setLoginId(userProfile.getLoginId());
		String encryptCred = userMgr.encrypt(userProfile.getLoginId(), userProfile.getPassword());
		user.setPassword(encryptCred);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(user);
		Mockito.when(stringUtil.equals(Mockito.anyString(), Mockito.nullable(String.class))).thenReturn(true);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(stringUtil.validatePassword(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(false);
		Mockito.when(restErrorUtil.createRESTException("serverMsg.userMgrNewPassword",MessageEnums.INVALID_PASSWORD, null, null, pwdChange.getLoginId())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.changePassword(pwdChange);
	}

	@Test
	public void test49CreateDefaultAccountUser() {
		destroySession();
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXPortalUserRoleDao roleDao = Mockito.mock(XXPortalUserRoleDao.class);
		VXPortalUser userProfile = userProfile();
		userProfile.setStatus(RangerCommonEnums.USER_EXTERNAL);
		Collection<String> userRoleList = new ArrayList<String>();
		userRoleList.add("ROLE_USER");
		userProfile.setUserRoleList(userRoleList);
		XXPortalUser user = new XXPortalUser();
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setUserSource(RangerCommonEnums.USER_EXTERNAL);
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");

		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(null, user);
		Mockito.when(userDao.findByEmailAddress(Mockito.anyString())).thenReturn(null);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(roleDao);
		Mockito.when(userDao.create((XXPortalUser) Mockito.any())).thenReturn(user);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		userProfile.setEmailAddress(null);
		VXPortalUser dbVXPortalUser = userMgr.createDefaultAccountUser(userProfile);
		Assert.assertNotNull(dbVXPortalUser);
		Assert.assertEquals(user.getId(), dbVXPortalUser.getId());
		Assert.assertEquals(user.getFirstName(), dbVXPortalUser.getFirstName());
		Assert.assertEquals(user.getLastName(), dbVXPortalUser.getLastName());
		Assert.assertEquals(user.getLoginId(), dbVXPortalUser.getLoginId());
		Assert.assertEquals(user.getEmailAddress(),dbVXPortalUser.getEmailAddress());
		Assert.assertEquals(user.getPassword(), dbVXPortalUser.getPassword());
		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUser();
		Mockito.verify(daoManager, Mockito.atLeast(1)).getXXPortalUserRole();

		Mockito.when(userDao.findByLoginId(Mockito.anyString())).thenReturn(null);
		Mockito.when(userDao.findByEmailAddress(Mockito.anyString())).thenReturn(user);
		Mockito.when(restErrorUtil.createRESTException("The email address " + user.getEmailAddress() + " you've provided already exists. Please try again with different email address.", MessageEnums.OPER_NOT_ALLOWED_FOR_STATE)).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userProfile.setEmailAddress(user.getEmailAddress());
		userMgr.createDefaultAccountUser(userProfile);
	}

	@Test
	public void test50AddUserRole() {
		setupUser();
		XXPortalUserRole XXPortalUserRole = new XXPortalUserRole();
		XXPortalUserRole.setId(userId);
		XXPortalUserRole.setUserRole("ROLE_USER");
		List<XXPortalUserRole> list = new ArrayList<XXPortalUserRole>();
		list.add(XXPortalUserRole);
		XXPortalUserRoleDao userDao = Mockito.mock(XXPortalUserRoleDao.class);
		Mockito.when(daoManager.getXXPortalUserRole()).thenReturn(userDao);
		Mockito.when(userDao.findByUserId(userId)).thenReturn(list);
		try {
			userMgr.addUserRole(userId, "ROLE_SYS_ADMIN");
		} catch (Exception e) {
		}
		destroySession();
		userMgr.addUserRole(userId, "ROLE_SYS_ADMIN");
	}

	@Test
	public void test51UpdateUserWithPass() {
		setup();
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		VXPortalUser userProfile = userProfile();
		userProfile.setPassword("password1234");
		XXPortalUser user = new XXPortalUser();
		user.setId(userProfile.getId());
		user.setLoginId(userProfile.getLoginId());
		user.setEmailAddress(userProfile.getEmailAddress());
		user.setLoginId(userProfile.getLoginId());
		String encryptedPwd = userMgr.encrypt(userProfile.getLoginId(),userProfile.getPassword());
		user.setPassword(encryptedPwd);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.getById(userProfile.getId())).thenReturn(user);
		Mockito.when(stringUtil.validateEmail(Mockito.anyString())).thenReturn(true);
		Mockito.doNothing().when(rangerBizUtil).blockAuditorRoleUser();
		Mockito.when(stringUtil.validatePassword(Mockito.anyString(), Mockito.any(String[].class))).thenReturn(false);
		Mockito.when(restErrorUtil.createRESTException("serverMsg.userMgrNewPassword", MessageEnums.INVALID_PASSWORD, null, null, user.getId().toString())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		userMgr.updateUserWithPass(userProfile);
	}
}
