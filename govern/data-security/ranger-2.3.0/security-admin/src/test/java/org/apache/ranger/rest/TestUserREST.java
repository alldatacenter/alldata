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

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.WebApplicationException;

import org.apache.ranger.biz.UserMgr;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerConfigUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.SearchCriteria;
import org.apache.ranger.common.SearchUtil;
import org.apache.ranger.common.SortField;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXPortalUserDao;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.util.RangerRestUtil;
import org.apache.ranger.view.VXPasswordChange;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXPortalUserList;
import org.apache.ranger.view.VXResponse;
import org.apache.ranger.view.VXStringList;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestUserREST {

	@InjectMocks
	UserREST userREST = new UserREST();

	@Mock
	HttpServletRequest request;

	@Mock
	SearchUtil searchUtil;

	@Mock
	RangerConfigUtil configUtil;

	@Mock
	UserMgr userManager;

	@Mock
	RangerDaoManager daoManager;

	@Mock
	XUserMgr xUserMgr;

	@Mock
	RESTErrorUtil restErrorUtil;

	@Mock
	VXPortalUserList vXPUserExpList;

	@Mock
	RangerRestUtil msRestUtil;

	@Mock
	VXPortalUser vxPUserAct;

	@Mock
	VXPasswordChange changePassword;

	@Mock
	VXResponse responseExp;

	@Mock
	StringUtil stringUtil;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	Long userId = 10l;
	int pageSize = 100;
	String firstName = "abc";
	String lastName = "xyz";
	String loginId = "xyzId";
	String emailId = "abc@Example.com";

	@Test
	public void test1SearchUsers() {
		SearchCriteria searchCriteria = new SearchCriteria();
		vXPUserExpList = new VXPortalUserList();
		vXPUserExpList.setPageSize(pageSize);
		List<Integer> status = new ArrayList<Integer>();
		String publicScreenName = "nrp";
		List<String> roles = new ArrayList<String>();

		Mockito.when(searchUtil.extractCommonCriterias(Matchers.eq(request), Matchers.anyListOf(SortField.class))).thenReturn(searchCriteria);
		Mockito.when(searchUtil.extractLong(request, searchCriteria, "userId", "User Id")).thenReturn(userId);
		Mockito.when(searchUtil.extractString(request, searchCriteria, "loginId", "Login Id", null))
				.thenReturn(loginId);
		Mockito.when(searchUtil.extractString(request, searchCriteria, "emailAddress", "Email Address", null))
				.thenReturn(emailId);
		Mockito.when(searchUtil.extractString(request, searchCriteria, "firstName", "First Name",
				StringUtil.VALIDATION_NAME)).thenReturn(firstName);
		Mockito.when(
				searchUtil.extractString(request, searchCriteria, "lastName", "Last Name", StringUtil.VALIDATION_NAME))
				.thenReturn(lastName);
		Mockito.when(searchUtil.extractEnum(request, searchCriteria, "status", "Status", "statusList",
				RangerConstants.ActivationStatus_MAX)).thenReturn(status);
		Mockito.when(searchUtil.extractString(request, searchCriteria, "publicScreenName", "Public Screen Name",
				StringUtil.VALIDATION_NAME)).thenReturn(publicScreenName);
		Mockito.when(searchUtil.extractStringList(request, searchCriteria, "role", "Role", "roleList",
				configUtil.getRoles(), StringUtil.VALIDATION_NAME)).thenReturn(roles);
		Mockito.when(userManager.searchUsers(searchCriteria)).thenReturn(vXPUserExpList);

		VXPortalUserList vXPUserListAct = userREST.searchUsers(request);

		Assert.assertNotNull(vXPUserListAct);
		Assert.assertEquals(vXPUserExpList, vXPUserListAct);
		Assert.assertEquals(vXPUserExpList.getPageSize(), vXPUserListAct.getPageSize());

		Mockito.verify(searchUtil).extractCommonCriterias(Matchers.eq(request), Matchers.anyListOf(SortField.class));
		Mockito.verify(searchUtil).extractLong(request, searchCriteria, "userId", "User Id");
		Mockito.verify(searchUtil).extractString(request, searchCriteria, "loginId", "Login Id", null);
		Mockito.verify(searchUtil).extractString(request, searchCriteria, "emailAddress", "Email Address", null);
		Mockito.verify(searchUtil).extractString(request, searchCriteria, "firstName", "First Name",
				StringUtil.VALIDATION_NAME);
		Mockito.verify(searchUtil).extractString(request, searchCriteria, "lastName", "Last Name",
				StringUtil.VALIDATION_NAME);
		Mockito.verify(searchUtil).extractEnum(request, searchCriteria, "status", "Status", "statusList",
				RangerConstants.ActivationStatus_MAX);
		Mockito.verify(searchUtil).extractString(request, searchCriteria, "publicScreenName", "Public Screen Name",
				StringUtil.VALIDATION_NAME);
		Mockito.verify(searchUtil).extractStringList(request, searchCriteria, "role", "Role", "roleList",
				configUtil.getRoles(), StringUtil.VALIDATION_NAME);
		Mockito.verify(userManager).searchUsers(searchCriteria);
	}

	@Test
	public void test2GetUserProfileForUser() {
		VXPortalUser vxPUserExp = CreateVXPortalUser();

		Mockito.when(userManager.getUserProfile(userId)).thenReturn(vxPUserExp);

		VXPortalUser VXPUserAct = userREST.getUserProfileForUser(userId);

		Assert.assertNotNull(VXPUserAct);
		Assert.assertEquals(vxPUserExp, VXPUserAct);
		Assert.assertEquals(vxPUserExp.getLoginId(), VXPUserAct.getLoginId());
		Assert.assertEquals(vxPUserExp.getFirstName(), VXPUserAct.getFirstName());
		Assert.assertEquals(vxPUserExp.getEmailAddress(), VXPUserAct.getEmailAddress());
		Assert.assertEquals(vxPUserExp.getId(), VXPUserAct.getId());

		Mockito.verify(userManager).getUserProfile(userId);
	}

	@Test
	public void test3GetUserProfileForUser() {
		VXPortalUser vxPUserExp = new VXPortalUser();
		vxPUserExp = null;

		Mockito.when(userManager.getUserProfile(userId)).thenReturn(vxPUserExp);

		VXPortalUser VXPUserAct = userREST.getUserProfileForUser(userId);

		Assert.assertEquals(vxPUserExp, VXPUserAct);

		Mockito.verify(userManager).getUserProfile(userId);
	}

	@Test
	public void test6Create() {
		VXPortalUser vxPUserExp = CreateVXPortalUser();

		Mockito.when(userManager.createUser(vxPUserExp)).thenReturn(vxPUserExp);

		VXPortalUser VXPUserAct = userREST.create(vxPUserExp, request);

		Assert.assertNotNull(VXPUserAct);
		Assert.assertEquals(vxPUserExp.getLoginId(), VXPUserAct.getLoginId());
		Assert.assertEquals(vxPUserExp.getFirstName(), VXPUserAct.getFirstName());
		Assert.assertEquals(vxPUserExp.getLastName(), VXPUserAct.getLastName());
		Assert.assertEquals(vxPUserExp.getEmailAddress(), VXPUserAct.getEmailAddress());

		Mockito.verify(userManager).createUser(vxPUserExp);
	}

	@Test
	public void test7CreateDefaultAccountUser() {
		VXPortalUser vxPUserExp = new VXPortalUser();
		vxPUserExp = null;
		Mockito.when(userManager.createDefaultAccountUser(vxPUserExp)).thenReturn(vxPUserExp);

		VXPortalUser VXPUserAct = userREST.createDefaultAccountUser(vxPUserExp, request);

		Assert.assertNull(VXPUserAct);

		Mockito.verify(userManager).createDefaultAccountUser(vxPUserExp);
	}

	@Test
	public void test8CreateDefaultAccountUser() {
		VXPortalUser vxPUserExp = CreateVXPortalUser();

		Mockito.when(userManager.createDefaultAccountUser(vxPUserExp)).thenReturn(vxPUserExp);
		Mockito.doNothing().when(xUserMgr).assignPermissionToUser(vxPUserExp, true);

		VXPortalUser VXPUserAct = userREST.createDefaultAccountUser(vxPUserExp, request);

		Assert.assertNotNull(VXPUserAct);
		Assert.assertEquals(vxPUserExp, VXPUserAct);
		Assert.assertEquals(vxPUserExp.getLoginId(), VXPUserAct.getLoginId());
		Assert.assertEquals(vxPUserExp.getFirstName(), VXPUserAct.getFirstName());
		Assert.assertEquals(vxPUserExp.getLastName(), VXPUserAct.getLastName());
		Assert.assertEquals(vxPUserExp.getEmailAddress(), VXPUserAct.getEmailAddress());

		Mockito.verify(userManager).createDefaultAccountUser(vxPUserExp);
		Mockito.verify(xUserMgr).assignPermissionToUser(vxPUserExp, true);
	}

	@Test
	public void test8Update() {
		VXPortalUser vxPUserExp = CreateVXPortalUser();
		vxPUserExp.setLoginId(loginId);
		XXPortalUser xxPUserExp = new XXPortalUser();
		xxPUserExp.setLoginId(loginId);
		XXPortalUserDao xxPortalUserDao = Mockito.mock(XXPortalUserDao.class);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xxPortalUserDao);
		Mockito.when(xxPortalUserDao.getById(Mockito.anyLong())).thenReturn(xxPUserExp);
		Mockito.doNothing().when(userManager).checkAccess(xxPUserExp);
		Mockito.doNothing().when(msRestUtil).validateVUserProfileForUpdate(xxPUserExp, vxPUserExp);
		Mockito.when(userManager.updateUser(vxPUserExp)).thenReturn(xxPUserExp);
		Mockito.when(userManager.mapXXPortalUserVXPortalUser(xxPUserExp)).thenReturn(vxPUserExp);

		VXPortalUser vxPUserAct = userREST.update(vxPUserExp, request);

		Assert.assertNotNull(vxPUserAct);
		Assert.assertEquals(xxPUserExp.getLoginId(), vxPUserAct.getLoginId());
		Assert.assertEquals(vxPUserExp.getId(), vxPUserAct.getId());
		Assert.assertEquals(vxPUserExp.getFirstName(), vxPUserAct.getFirstName());

		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(xxPortalUserDao).getById(Mockito.anyLong());
		Mockito.verify(userManager).checkAccess(xxPUserExp);
		Mockito.verify(msRestUtil).validateVUserProfileForUpdate(xxPUserExp, vxPUserExp);
		Mockito.verify(userManager).updateUser(vxPUserExp);
		Mockito.verify(userManager).mapXXPortalUserVXPortalUser(xxPUserExp);
	}

	@Test
	public void test9Update() {
		VXPortalUser vxPUserExp = new VXPortalUser();
		XXPortalUser xxPUserExp = new XXPortalUser();
		xxPUserExp = null;
		XXPortalUserDao xxPortalUserDao = Mockito.mock(XXPortalUserDao.class);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xxPortalUserDao);
		Mockito.doNothing().when(userManager).checkAccess(xxPUserExp);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums) Mockito.any(),
				Mockito.nullable(Long.class), Mockito.nullable(String.class), Mockito.anyString())).thenReturn(new WebApplicationException());

		thrown.expect(WebApplicationException.class);

		userREST.update(vxPUserExp, request);

		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(xxPortalUserDao).getById(Mockito.anyLong());
		Mockito.verify(userManager).checkAccess(xxPUserExp);
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyString(), (MessageEnums) Mockito.any(),
				Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());
	}

	@Test
	public void test10SetUserRoles() {
		Long userId = 10L;
		VXResponse responseExp = new VXResponse();
		VXStringList roleList = new VXStringList();
		Mockito.doNothing().when(userManager).checkAccess(userId);
		Mockito.doNothing().when(userManager).setUserRoles(userId, roleList.getVXStrings());

		VXResponse responseAct = userREST.setUserRoles(userId, roleList);

		Assert.assertNotNull(responseAct);
		Assert.assertEquals(responseExp.getStatusCode(), responseAct.getStatusCode());

		Mockito.verify(userManager).checkAccess(userId);
		Mockito.verify(userManager).setUserRoles(userId, roleList.getVXStrings());
	}

	@Test
	public void test11DeactivateUser() {
		VXPortalUser vxPUserExp = CreateVXPortalUser();
		XXPortalUser xxPUserExp = new XXPortalUser();
		xxPUserExp.setLoginId(loginId);
		xxPUserExp.setStatus(1);
		vxPUserExp.setStatus(5);

		XXPortalUserDao xxPortalUserDao = Mockito.mock(XXPortalUserDao.class);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xxPortalUserDao);
		Mockito.when(xxPortalUserDao.getById(userId)).thenReturn(xxPUserExp);
		Mockito.when(userManager.deactivateUser(xxPUserExp)).thenReturn(vxPUserExp);

		VXPortalUser vxPUserAct = userREST.deactivateUser(userId);
		Assert.assertNotNull(vxPUserAct);
		Assert.assertEquals(xxPUserExp.getLoginId(), vxPUserAct.getLoginId());
		Assert.assertEquals(vxPUserExp.getStatus(), vxPUserAct.getStatus());
		Assert.assertEquals(vxPUserExp.getId(), vxPUserAct.getId());
		Assert.assertEquals(vxPUserExp.getFirstName(), vxPUserAct.getFirstName());

		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(xxPortalUserDao).getById(userId);
		Mockito.verify(userManager).deactivateUser(xxPUserExp);
	}

	@Test
	public void test12DeactivateUser() {
		XXPortalUser xxPUserExp = new XXPortalUser();
		xxPUserExp = null;
		XXPortalUserDao xxPortalUserDao = Mockito.mock(XXPortalUserDao.class);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xxPortalUserDao);
		Mockito.when(xxPortalUserDao.getById(userId)).thenReturn(xxPUserExp);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums) Mockito.any(),
				Mockito.nullable(Long.class), Mockito.nullable(String.class), Mockito.anyString())).thenReturn(new WebApplicationException());
		thrown.expect(WebApplicationException.class);

		userREST.deactivateUser(userId);

		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(xxPortalUserDao).getById(userId);
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyString(), (MessageEnums) Mockito.any(),
				Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());
	}

	@Test
	public void test13GetUserProfile() {
		HttpSession hs = Mockito.mock(HttpSession.class);
		VXPortalUser vxPUserExp = CreateVXPortalUser();
		Mockito.when(userManager.getUserProfileByLoginId()).thenReturn(vxPUserExp);
		Mockito.when(request.getSession()).thenReturn(hs);
		Mockito.when(hs.getId()).thenReturn("id");

		VXPortalUser vxPUserAct = userREST.getUserProfile(request);

		Assert.assertNotNull(vxPUserAct);
		Assert.assertEquals(vxPUserExp, vxPUserAct);
		Assert.assertEquals(vxPUserExp.getId(), vxPUserAct.getId());
		Assert.assertEquals(vxPUserExp.getFirstName(), vxPUserAct.getFirstName());

		Mockito.verify(userManager).getUserProfileByLoginId();
	}

	@Test
	public void test15SuggestUserFirstName() {
		String op = userREST.suggestUserFirstName(firstName, request);
		Assert.assertNull(op);
	}

	@Test
	public void test16ChangePassword() {
		XXPortalUser xxPUser = new XXPortalUser();
		VXResponse vxResponseExp = new VXResponse();
		vxResponseExp.setStatusCode(10);
		XXPortalUserDao xxPortalUserDao = Mockito.mock(XXPortalUserDao.class);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xxPortalUserDao);
		Mockito.when(restErrorUtil.createRESTException("serverMsg.userRestUser",MessageEnums.DATA_NOT_FOUND, null, null, changePassword.getLoginId())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		VXResponse vxResponseAct = userREST.changePassword(userId, changePassword);

		Assert.assertNotNull(vxResponseAct);
		Assert.assertEquals(vxResponseExp, vxResponseAct);
		Assert.assertEquals(vxResponseExp.getStatusCode(), vxResponseAct.getStatusCode());

		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(xxPortalUserDao).getById(userId);
		Mockito.verify(userManager).checkAccessForUpdate(xxPUser);
		Mockito.verify(changePassword).setId(userId);
		Mockito.verify(userManager).changePassword(changePassword);
	}

	@Test
	public void test17ChangePassword() {
		XXPortalUserDao xxPortalUserDao = Mockito.mock(XXPortalUserDao.class);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xxPortalUserDao);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums) Mockito.any(),
				Mockito.nullable(Long.class), Mockito.nullable(String.class), Mockito.nullable(String.class))).thenReturn(new WebApplicationException());
		thrown.expect(WebApplicationException.class);

		userREST.changePassword(userId, changePassword);

		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(xxPortalUserDao).getById(userId);
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyString(), (MessageEnums) Mockito.any(),
				Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());
	}

	@Test
	public void test18ChangeEmailAddress() {
		XXPortalUser xxPUser = new XXPortalUser();
		VXPortalUser vxPUserExp = CreateVXPortalUser();

		XXPortalUserDao xxPortalUserDao = Mockito.mock(XXPortalUserDao.class);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xxPortalUserDao);
		Mockito.when(restErrorUtil.createRESTException("serverMsg.userRestUser",MessageEnums.DATA_NOT_FOUND, null, null, changePassword.getLoginId())).thenThrow(new WebApplicationException());
		thrown.expect(WebApplicationException.class);
		VXPortalUser vxPortalUserAct = userREST.changeEmailAddress(userId, changePassword);

		Assert.assertNotNull(vxPortalUserAct);
		Assert.assertEquals(vxPUserExp, vxPortalUserAct);
		Assert.assertEquals(vxPUserExp.getId(), vxPortalUserAct.getId());
		Assert.assertEquals(vxPUserExp.getFirstName(), vxPortalUserAct.getFirstName());

		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(xxPortalUserDao).getById(userId);
		Mockito.verify(userManager).checkAccessForUpdate(xxPUser);
		Mockito.verify(changePassword).setId(userId);
		Mockito.verify(userManager).changeEmailAddress(xxPUser, changePassword);
	}

	@Test
	public void test19ChangeEmailAddress() {
		XXPortalUserDao xxPortalUserDao = Mockito.mock(XXPortalUserDao.class);

		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xxPortalUserDao);
		Mockito.when(restErrorUtil.createRESTException(Mockito.anyString(), (MessageEnums) Mockito.any(),
				Mockito.nullable(Long.class), Mockito.nullable(String.class), Mockito.nullable(String.class))).thenReturn(new WebApplicationException());
		thrown.expect(WebApplicationException.class);

		userREST.changeEmailAddress(userId, changePassword);

		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(xxPortalUserDao).getById(userId);
		Mockito.verify(restErrorUtil).createRESTException(Mockito.anyString(), (MessageEnums) Mockito.any(),
				Mockito.anyLong(), Mockito.anyString(), Mockito.anyString());
	}

	private VXPortalUser CreateVXPortalUser() {

		VXPortalUser vxPUserExp = new VXPortalUser();
		vxPUserExp.setId(userId);
		vxPUserExp.setFirstName(firstName);
		vxPUserExp.setLastName(lastName);
		vxPUserExp.setEmailAddress(emailId);
		vxPUserExp.setLoginId(loginId);
		return vxPUserExp;
	}
}
