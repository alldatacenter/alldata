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
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;

import org.apache.ranger.common.AppConstants;
import org.apache.ranger.common.ContextUtil;
import org.apache.ranger.common.MessageEnums;
import org.apache.ranger.common.RESTErrorUtil;
import org.apache.ranger.common.RangerConstants;
import org.apache.ranger.common.StringUtil;
import org.apache.ranger.common.UserSessionBase;
import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXAssetDao;
import org.apache.ranger.db.XXPortalUserDao;
import org.apache.ranger.db.XXResourceDao;
import org.apache.ranger.db.XXUserDao;
import org.apache.ranger.entity.XXAsset;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.entity.XXResource;
import org.apache.ranger.entity.XXUser;
import org.apache.ranger.security.context.RangerContextHolder;
import org.apache.ranger.security.context.RangerSecurityContext;
import org.apache.ranger.view.VXPortalUser;
import org.apache.ranger.view.VXResource;
import org.apache.ranger.view.VXResponse;
import org.apache.ranger.view.VXUser;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class TestRangerBizUtil {
	
	private Long id = 1L;
	private String resourceName = "hadoopdev";
	
	@InjectMocks
	RangerBizUtil rangerBizUtil = new RangerBizUtil();
	
	@Mock
	RangerDaoManager daoManager;
	
	@Mock
	StringUtil stringUtil;
	
        @Mock
        VXUser vXUser;

        @Mock
        UserMgr userMgr;

        @Mock
        ContextUtil contextUtil;

        @Mock
        RangerSecurityContext context;

        @Mock
        UserSessionBase currentUserSession;

        @Mock
        RESTErrorUtil restErrorUtil;

        @Mock
        VXResponse vXResponse;

        @Rule
        public ExpectedException thrown = ExpectedException.none();

	@Before
	public void setup(){
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);

//		RESTErrorUtil restErrorUtil;
	}
	
	@Test
	public void testHasPermission_When_disableAccessControl(){
		VXResource vXResource = null;
		rangerBizUtil.enableResourceAccessControl = false;
		VXResponse resp = rangerBizUtil.hasPermission(vXResource, AppConstants.XA_PERM_TYPE_UNKNOWN);
		Assert.assertNotNull(resp);
	}
	
	@Test
	public void testHasPermission_When_NoResource(){
		VXResource vXResource = null;
		VXResponse resp = rangerBizUtil.hasPermission(vXResource, AppConstants.XA_PERM_TYPE_UNKNOWN);
		Assert.assertNotNull(resp);
		Assert.assertEquals(VXResponse.STATUS_ERROR, resp.getStatusCode());
		Assert.assertEquals("Please provide valid policy.", resp.getMsgDesc());
	}
	
	@Test
	public void testHasPermission_emptyResourceName(){
		VXResource vXResource = new VXResource();
		vXResource.setAssetId(12345L);
		XXPortalUser portalUser = new XXPortalUser();
		portalUser.setId(id);
		portalUser.setLoginId("12121");
		RangerContextHolder.getSecurityContext().getUserSession().setXXPortalUser(portalUser);

		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		XXPortalUserDao userDao = Mockito.mock(XXPortalUserDao.class);
		XXUser xxUser = new XXUser();
		XXAsset xxAsset = new XXAsset();
		List<XXResource> lst = new ArrayList<XXResource>();
		XXResourceDao xxResourceDao = Mockito.mock(XXResourceDao.class);
		XXAssetDao xxAssetDao = Mockito.mock(XXAssetDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(userDao);
		Mockito.when(userDao.getById(Mockito.anyLong())).thenReturn(portalUser);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByUserName(Mockito.anyString())).thenReturn(xxUser);
		Mockito.when(daoManager.getXXResource()).thenReturn(xxResourceDao);
		Mockito.when(xxResourceDao.findByAssetIdAndResourceStatus(Mockito.anyLong(),Mockito.anyInt())).thenReturn(lst);
		Mockito.when(daoManager.getXXAsset()).thenReturn(xxAssetDao);
		Mockito.when(xxAssetDao.getById(Mockito.anyLong())).thenReturn(xxAsset);
		VXResponse resp = rangerBizUtil.hasPermission(vXResource, AppConstants.XA_PERM_TYPE_UNKNOWN);
		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(userDao).getById(Mockito.anyLong());
		Mockito.verify(daoManager).getXXUser();
		Mockito.verify(xxUserDao).findByUserName(Mockito.anyString());
		Assert.assertNotNull(resp);
		Assert.assertEquals(VXResponse.STATUS_ERROR, resp.getStatusCode());
		Assert.assertEquals("Permission Denied !", resp.getMsgDesc());
	}
	
	@Test
	public void testHasPermission_isAdmin(){
		VXResource vXResource = new VXResource();
		vXResource.setName(resourceName);
		vXResource.setAssetId(id);
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(true);
		VXResponse resp = rangerBizUtil.hasPermission(vXResource, AppConstants.XA_PERM_TYPE_UNKNOWN);
		Assert.assertNotNull(resp);
		Assert.assertEquals(VXResponse.STATUS_SUCCESS, resp.getStatusCode());
	}
	
	@Test
	public void testIsNotAdmin(){
		boolean isAdminChk = rangerBizUtil.isAdmin();
		Assert.assertFalse(isAdminChk);
	}
	
	@Test
	public void testIsAdmin(){
		UserSessionBase currentUserSession = ContextUtil.getCurrentUserSession();
		currentUserSession.setUserAdmin(true);
		boolean isAdminChk = rangerBizUtil.isAdmin();
		Assert.assertTrue(isAdminChk);
	}
	
	@Test
	public void testUserSessionNull_forIsAdmin(){
		RangerContextHolder.setSecurityContext(null);
		boolean isAdminChk = rangerBizUtil.isAdmin();
		Assert.assertFalse(isAdminChk);
	}
	
	@Test
	public void testGetXUserId_NoUserSession(){
		RangerContextHolder.setSecurityContext(null);
		Long chk = rangerBizUtil.getXUserId();
		Assert.assertNull(chk);
	}
	
	@Test
	public void testGetXUserId_NoUser(){
		RangerSecurityContext context = new RangerSecurityContext();
		context.setUserSession(new UserSessionBase());
		RangerContextHolder.setSecurityContext(context);
		XXPortalUser xxPortalUser = new XXPortalUser();
		xxPortalUser.setId(id);
		xxPortalUser.setLoginId("12121");
		context.getUserSession().setXXPortalUser(xxPortalUser);

		XXUser xxUser = new XXUser();
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		XXPortalUserDao xxPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xxPortalUserDao);
		Mockito.when(xxPortalUserDao.getById(Mockito.anyLong())).thenReturn(xxPortalUser);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByUserName(Mockito.anyString())).thenReturn(xxUser);
		Long chk = rangerBizUtil.getXUserId();
		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(xxPortalUserDao).getById(Mockito.anyLong());
		Mockito.verify(daoManager).getXXUser();
		Mockito.verify(xxUserDao).findByUserName(Mockito.anyString());
		Assert.assertNull(chk);
	}
	
	@Test
	public void testGetXUserId(){
		XXPortalUser xxPortalUser = new XXPortalUser();
		xxPortalUser.setId(id);
		xxPortalUser.setLoginId("12121");
		XXUser xxUser = new XXUser();
		xxUser.setId(id);
		XXPortalUserDao xxPortalUserDao = Mockito.mock(XXPortalUserDao.class);
		XXUserDao xxUserDao = Mockito.mock(XXUserDao.class);
		RangerSecurityContext context = new RangerSecurityContext();
		UserSessionBase userSessionBase = new UserSessionBase();
		userSessionBase.setUserAdmin(true);
		context.setUserSession(userSessionBase);
		userSessionBase.setXXPortalUser(xxPortalUser);
		RangerContextHolder.setSecurityContext(context);
		Mockito.when(daoManager.getXXPortalUser()).thenReturn(xxPortalUserDao);
		Mockito.when(xxPortalUserDao.getById(Mockito.anyLong())).thenReturn(xxPortalUser);
		Mockito.when(daoManager.getXXUser()).thenReturn(xxUserDao);
		Mockito.when(xxUserDao.findByUserName(Mockito.anyString())).thenReturn(xxUser);
		Long chk = rangerBizUtil.getXUserId();
		Mockito.verify(daoManager).getXXPortalUser();
		Mockito.verify(xxPortalUserDao).getById(Mockito.anyLong());
		Mockito.verify(daoManager).getXXUser();
		Mockito.verify(xxUserDao).findByUserName(Mockito.anyString());
		Assert.assertEquals(chk, id);
	}
	
	@Test
	public void testReplaceMetaChars_PathEmpty(){
		String path = "";
		String pathChk = rangerBizUtil.replaceMetaChars(path);
		Assert.assertFalse(pathChk.contains("\\*"));
		Assert.assertFalse(pathChk.contains("\\?"));
	}
	
	@Test
	public void testReplaceMetaChars_NoMetaChars(){
		String path = "\\Demo\\Test";
		String pathChk = rangerBizUtil.replaceMetaChars(path);
		Assert.assertFalse(pathChk.contains("\\*"));
		Assert.assertFalse(pathChk.contains("\\?"));
	}
	
	@Test
	public void testReplaceMetaChars_PathNull(){
		String path = null;
		String pathChk = rangerBizUtil.replaceMetaChars(path);
		Assert.assertNull(pathChk);
	}
	
	@Test
	public void testReplaceMetaChars(){
		String path = "\\Demo\\Test\\*\\?";
		String pathChk = rangerBizUtil.replaceMetaChars(path);
		Assert.assertFalse(pathChk.contains("\\*"));
		Assert.assertFalse(pathChk.contains("\\?"));
	}
	
	@Test
	public void testGeneratePublicName(){
		String firstName = "Test123456789123456789";
		String lastName = "Unit";
		String publicNameChk = rangerBizUtil.generatePublicName(firstName, lastName);
		Assert.assertEquals("Test12345678... U.", publicNameChk);
	}
	
	@Test
	public void testGeneratePublicName_fNameLessThanMax(){
		String firstName = "Test";
		String lastName = "";
		String publicNameChk = rangerBizUtil.generatePublicName(firstName, lastName);
		Assert.assertNull(publicNameChk);
	}
	
	@Test
	public void testGeneratePublicName_withPortalUser(){
		VXPortalUser vXPortalUser = new VXPortalUser();
		vXPortalUser.setFirstName("Test");
		vXPortalUser.setLastName(null);
		String publicNameChk = rangerBizUtil.generatePublicName(vXPortalUser, null);
		Assert.assertNull(publicNameChk);
	}


	@Test
	public void testMatchHdfsPolicy_NoResourceName(){
		boolean bnlChk = rangerBizUtil.matchHbasePolicy(null, null, null, id, AppConstants.XA_PERM_TYPE_UNKNOWN);
		Assert.assertFalse(bnlChk);
	}
	
	@Test
	public void testMatchHdfsPolicy_NoResourceList(){
		boolean bnlChk = rangerBizUtil.matchHbasePolicy(resourceName, null, null, id, AppConstants.XA_PERM_TYPE_UNKNOWN);
		Assert.assertFalse(bnlChk);
	}
	
	@Test
	public void testMatchHdfsPolicy_NoUserId(){
		VXResponse vXResponse = new VXResponse();
		List<XXResource> xResourceList = new ArrayList<XXResource>();
		XXResource xXResource = new XXResource();
		xXResource.setId(id);
		xXResource.setName(resourceName);
		xXResource.setIsRecursive(AppConstants.BOOL_TRUE);
		xXResource.setResourceStatus(AppConstants.STATUS_ENABLED);
		xResourceList.add(xXResource);
		boolean bnlChk = rangerBizUtil.matchHbasePolicy(resourceName, xResourceList, vXResponse, null, AppConstants.XA_PERM_TYPE_UNKNOWN);
		Assert.assertFalse(bnlChk);
	}
	
	@Test
	public void testMatchHdfsPolicy_NoPremission(){
		VXResponse vXResponse = new VXResponse();
		List<XXResource> xResourceList = new ArrayList<XXResource>();
		XXResource xXResource = new XXResource();
		xXResource.setId(id);
		xXResource.setName(resourceName);
		xXResource.setIsRecursive(AppConstants.BOOL_TRUE);
		xXResource.setResourceStatus(AppConstants.STATUS_ENABLED);
		xResourceList.add(xXResource);
		Mockito.when(stringUtil.split(Mockito.anyString(), Mockito.anyString())).thenReturn(new String[0]);
		boolean bnlChk = rangerBizUtil.matchHbasePolicy("/*/*/*", xResourceList, vXResponse, id, AppConstants.XA_PERM_TYPE_UNKNOWN);
		Mockito.verify(stringUtil).split(Mockito.anyString(), Mockito.anyString());
		Assert.assertFalse(bnlChk);
	}
	
	@Test
	public void testMatchHivePolicy_NoResourceName(){
		boolean bnlChk = rangerBizUtil.matchHivePolicy(null, null, null, 0);
		Assert.assertFalse(bnlChk);
		
	}
	
	@Test
	public void testMatchHivePolicy_NoResourceList(){
		boolean bnlChk = rangerBizUtil.matchHivePolicy(resourceName, null, null, 0);
		Assert.assertFalse(bnlChk);
		
	}
	
	@Test
	public void testMatchHivePolicy_NoUserId(){
		List<XXResource> xResourceList = new ArrayList<XXResource>();
		XXResource xXResource = new XXResource();
		xXResource.setId(id);
		xXResource.setName(resourceName);
		xXResource.setIsRecursive(AppConstants.BOOL_TRUE);
		xXResource.setResourceStatus(AppConstants.STATUS_ENABLED);
		xResourceList.add(xXResource);
		boolean bnlChk = rangerBizUtil.matchHivePolicy(resourceName, xResourceList, null, 0);
		Assert.assertFalse(bnlChk);
		
	}
	
	@Test
	public void testMatchHivePolicy_NoPremission(){
		List<XXResource> xResourceList = new ArrayList<XXResource>();
		XXResource xXResource = new XXResource();
		xXResource.setId(id);
		xXResource.setName(resourceName);
		xXResource.setIsRecursive(AppConstants.BOOL_TRUE);
		xXResource.setResourceStatus(AppConstants.STATUS_ENABLED);
		xResourceList.add(xXResource);
		Mockito.when(stringUtil.split(Mockito.anyString(), Mockito.anyString())).thenReturn(new String[0]);
		boolean bnlChk = rangerBizUtil.matchHivePolicy("/*/*/*", xResourceList, id, 0);
		Assert.assertFalse(bnlChk);
	}
	
	@Test
	public void testMatchHivePolicy(){
		List<XXResource> xResourceList = new ArrayList<XXResource>();
		XXResource xXResource = new XXResource();
		xXResource.setId(5L);
		xXResource.setName(resourceName);
		xXResource.setIsRecursive(AppConstants.BOOL_TRUE);
		xXResource.setResourceStatus(AppConstants.STATUS_ENABLED);
		xResourceList.add(xXResource);
		Mockito.when(stringUtil.split(Mockito.anyString(), Mockito.anyString())).thenReturn(new String[0]);
		boolean bnlChk = rangerBizUtil.matchHivePolicy("/*/*/*", xResourceList, id, 17);
		Mockito.verify(stringUtil).split(Mockito.anyString(), Mockito.anyString());
		Assert.assertFalse(bnlChk);
	}

        @Test
        public void testCheckUserAccessibleThrowErrorForKeyAdminAndUserRoleSysAdmin()
                        throws Exception {

                Collection<String> roleList = new ArrayList<String>();
                roleList.add(RangerConstants.ROLE_SYS_ADMIN);
                Mockito.when(userMgr.getRolesByLoginId(vXUser.getName())).thenReturn(
                                roleList);
                Mockito.when(vXUser.getUserRoleList()).thenReturn(roleList);

                currentUserSession.setKeyAdmin(true);
                RangerSecurityContext context = new RangerSecurityContext();
                context.setUserSession(currentUserSession);
                RangerContextHolder.setSecurityContext(context);

                Mockito.when(currentUserSession.isKeyAdmin()).thenReturn(true);

                WebApplicationException webExp = new WebApplicationException();

                Mockito.when(
                                restErrorUtil.createRESTException(
                                                "Logged in user is not allowed to create/update user",
                                                MessageEnums.OPER_NO_PERMISSION)).thenReturn(webExp);

                thrown.expect(WebApplicationException.class);

                rangerBizUtil.checkUserAccessible(vXUser);

                Mockito.verify(restErrorUtil).createRESTException(
                                "Logged in user is not allowed to create/update user",
                                MessageEnums.OPER_NO_PERMISSION);

        }

        @Test
        public void testCheckUserAccessibleThrowErrorForKeyAdminAndUserRoleAdminAuditor()
                        throws Exception {

                Collection<String> roleList = new ArrayList<String>();
                roleList.add(RangerConstants.ROLE_ADMIN_AUDITOR);
                Mockito.when(userMgr.getRolesByLoginId(vXUser.getName())).thenReturn(
                                roleList);
                Mockito.when(vXUser.getUserRoleList()).thenReturn(roleList);

                currentUserSession.setKeyAdmin(true);
                RangerSecurityContext context = new RangerSecurityContext();
                context.setUserSession(currentUserSession);
                RangerContextHolder.setSecurityContext(context);

                Mockito.when(currentUserSession.isKeyAdmin()).thenReturn(true);

                WebApplicationException webExp = new WebApplicationException();

                Mockito.when(
                                restErrorUtil.createRESTException(
                                                "Logged in user is not allowed to create/update user",
                                                MessageEnums.OPER_NO_PERMISSION)).thenReturn(webExp);

                thrown.expect(WebApplicationException.class);

                rangerBizUtil.checkUserAccessible(vXUser);

                Mockito.verify(restErrorUtil).createRESTException(
                                "Logged in user is not allowed to create/update user",
                                MessageEnums.OPER_NO_PERMISSION);

        }

        @Test
        public void testCheckUserAccessibleSuccessForKeyAdmin(){
                Collection<String> roleList = new ArrayList<String>();
                roleList.add(RangerConstants.ROLE_KEY_ADMIN);
                roleList.add(RangerConstants.ROLE_KEY_ADMIN_AUDITOR);
                Mockito.when(userMgr.getRolesByLoginId(vXUser.getName())).thenReturn(
                                roleList);
                Mockito.when(vXUser.getUserRoleList()).thenReturn(roleList);

                currentUserSession.setKeyAdmin(true);

                RangerSecurityContext context = new RangerSecurityContext();
                context.setUserSession(currentUserSession);
                RangerContextHolder.setSecurityContext(context);

                Mockito.when(currentUserSession.isKeyAdmin()).thenReturn(true);

                boolean result = rangerBizUtil.checkUserAccessible(vXUser);
                Assert.assertTrue(result);

        }

        @Test
        public void testCheckUserAccessibleThrowErrorForAdminAndUserRoleKeyAdmin()
                        throws Exception {

                Collection<String> roleList = new ArrayList<String>();
                roleList.add(RangerConstants.ROLE_KEY_ADMIN);
                Mockito.when(userMgr.getRolesByLoginId(vXUser.getName())).thenReturn(
                                roleList);
                Mockito.when(vXUser.getUserRoleList()).thenReturn(roleList);

                currentUserSession.setUserAdmin(true);

                RangerSecurityContext context = new RangerSecurityContext();
                context.setUserSession(currentUserSession);
                RangerContextHolder.setSecurityContext(context);

                Mockito.when(currentUserSession.isUserAdmin()).thenReturn(true);

                WebApplicationException webExp = new WebApplicationException();

                Mockito.when(
                                restErrorUtil.createRESTException(
                                                "Logged in user is not allowed to create/update user",
                                                MessageEnums.OPER_NO_PERMISSION)).thenReturn(webExp);

                thrown.expect(WebApplicationException.class);

                rangerBizUtil.checkUserAccessible(vXUser);

                Mockito.verify(restErrorUtil).createRESTException(
                                "Logged in user is not allowed to create/update user",
                                MessageEnums.OPER_NO_PERMISSION);

        }

        @Test
        public void testCheckUserAccessibleThrowErrorForAdminAndUserRoleKeyAdminAuditor()
                        throws Exception {

                Collection<String> roleList = new ArrayList<String>();
                roleList.add(RangerConstants.ROLE_KEY_ADMIN_AUDITOR);
                Mockito.when(userMgr.getRolesByLoginId(vXUser.getName())).thenReturn(
                                roleList);
                Mockito.when(vXUser.getUserRoleList()).thenReturn(roleList);

                currentUserSession.setUserAdmin(true);

                RangerSecurityContext context = new RangerSecurityContext();
                context.setUserSession(currentUserSession);
                RangerContextHolder.setSecurityContext(context);

                Mockito.when(currentUserSession.isUserAdmin()).thenReturn(true);

                WebApplicationException webExp = new WebApplicationException();

                Mockito.when(
                                restErrorUtil.createRESTException(
                                                "Logged in user is not allowed to create/update user",
                                                MessageEnums.OPER_NO_PERMISSION)).thenReturn(webExp);

                thrown.expect(WebApplicationException.class);

                rangerBizUtil.checkUserAccessible(vXUser);

                Mockito.verify(restErrorUtil).createRESTException(
                                "Logged in user is not allowed to create/update user",
                                MessageEnums.OPER_NO_PERMISSION);

        }

        @Test
        public void testCheckUserAccessibleSuccessForAdmin(){
                Collection<String> roleList = new ArrayList<String>();
                roleList.add(RangerConstants.ROLE_SYS_ADMIN);
                Mockito.when(userMgr.getRolesByLoginId(vXUser.getName())).thenReturn(
                                roleList);
                Mockito.when(vXUser.getUserRoleList()).thenReturn(roleList);

                currentUserSession.setUserAdmin(true);

                RangerSecurityContext context = new RangerSecurityContext();
                context.setUserSession(currentUserSession);
                RangerContextHolder.setSecurityContext(context);

                Mockito.when(currentUserSession.isUserAdmin()).thenReturn(true);

                boolean result = rangerBizUtil.checkUserAccessible(vXUser);
                Assert.assertTrue(result);

        }

        @Test
        public void testBlockAuditorRoleUserThrowsErrorForAuditKeyAdmin(){
                RangerBizUtil rangerBizUtilMock = Mockito.mock(RangerBizUtil.class);
        vXResponse.setStatusCode(HttpServletResponse.SC_UNAUTHORIZED);
        vXResponse.setMsgDesc("Operation denied. LoggedInUser=1 ,isn't permitted to perform the action.");

        XXPortalUser xxPortalUser = new XXPortalUser();
        xxPortalUser.setId(1L);

                currentUserSession.setAuditKeyAdmin(true);

                RangerSecurityContext context = new RangerSecurityContext();
                context.setUserSession(currentUserSession);
                RangerContextHolder.setSecurityContext(context);

                Mockito.doThrow(new WebApplicationException()).when(rangerBizUtilMock).blockAuditorRoleUser();
                thrown.expect(WebApplicationException.class);

                rangerBizUtilMock.blockAuditorRoleUser();

        }

        @Test
        public void testBlockAuditorRoleUserThrowsErrorForAuditUserAdmin(){

                RangerBizUtil rangerBizUtilMock = Mockito.mock(RangerBizUtil.class);

        vXResponse.setStatusCode(HttpServletResponse.SC_UNAUTHORIZED);
        vXResponse.setMsgDesc("Operation denied. LoggedInUser=1 ,isn't permitted to perform the action.");

        XXPortalUser xxPortalUser = new XXPortalUser();
        xxPortalUser.setId(1L);

                currentUserSession.setAuditKeyAdmin(true);

                RangerSecurityContext context = new RangerSecurityContext();
                context.setUserSession(currentUserSession);
                RangerContextHolder.setSecurityContext(context);


                Mockito.doThrow(new WebApplicationException()).when(rangerBizUtilMock).blockAuditorRoleUser();

                thrown.expect(WebApplicationException.class);

                rangerBizUtilMock.blockAuditorRoleUser();
        }

        @Test
        public void testBlockAuditorRoleUserSuccess(){
                RangerBizUtil rangerBizUtilMock = Mockito.mock(RangerBizUtil.class);

        XXPortalUser xxPortalUser = new XXPortalUser();
        xxPortalUser.setId(1L);

                currentUserSession.setUserAdmin(true);

                RangerSecurityContext context = new RangerSecurityContext();
                context.setUserSession(currentUserSession);
                RangerContextHolder.setSecurityContext(context);


                Mockito.doNothing().when(rangerBizUtilMock).blockAuditorRoleUser();

                rangerBizUtilMock.blockAuditorRoleUser();
                Mockito.verify(rangerBizUtilMock).blockAuditorRoleUser();

        }

}