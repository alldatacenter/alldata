/*
  * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.patch.cliutil;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import org.apache.ranger.biz.UserMgr;
import org.apache.ranger.biz.XUserMgr;
import org.apache.ranger.common.RangerConstants;

import org.apache.ranger.db.RangerDaoManager;
import org.apache.ranger.db.XXModuleDefDao;
import org.apache.ranger.db.XXPortalUserDao;
import org.apache.ranger.entity.XXPortalUser;
import org.apache.ranger.service.XUserService;
import org.apache.ranger.view.VXUser;
import org.apache.ranger.view.VXUserList;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRoleBasedUserSearchUtil {
    @Mock
    XUserService xUserService;

    @Mock
    RangerDaoManager daoMgr;

    @Mock
    UserMgr userMgr;

    @Mock
    XUserMgr xUserMgr;
    @Mock
    XXPortalUserDao xXPortalUserDao;


    @InjectMocks
    RoleBasedUserSearchUtil roleBasedUserSearchUtil = new RoleBasedUserSearchUtil();


    public TestRoleBasedUserSearchUtil() {

    }
    @Test
    public void TestGetUsersBasedOnRole() {
       try {
           XXPortalUser xXPortalUser = new XXPortalUser();
           xXPortalUser.setLoginId("testUser");
           xXPortalUser.setId(1L);
           xXPortalUser.setFirstName("testUser");
           xXPortalUser.setPublicScreenName("testUser");
           xXPortalUser.setPassword("testUserPassword");
           List<XXPortalUser> listXXPortalUser = new ArrayList<XXPortalUser>();
           listXXPortalUser.add(xXPortalUser);
           List<String> userRoleList = new ArrayList<String>();
           userRoleList.add("ROLE_SYS_ADMIN");

           Mockito.when(daoMgr.getXXPortalUser()).thenReturn(xXPortalUserDao);
           Mockito.when(xXPortalUserDao.findByRole(RangerConstants.ROLE_SYS_ADMIN)).thenReturn(listXXPortalUser);

           roleBasedUserSearchUtil.getUsersBasedOnRole(userRoleList);


           Mockito.verify(xXPortalUserDao).findByRole(RangerConstants.ROLE_SYS_ADMIN);

       } catch(Exception e) {
           fail("test failed due to: " + e.getMessage());
       }
    }
    @Test
    public void TestValidateUserAndFetchUserList() {
        List<String> permissionList = new ArrayList<String>();
        permissionList.add(RangerConstants.MODULE_USER_GROUPS);
        String currentEncryptedPassword = "testpassword";
        XXPortalUser xxPortalUser = new XXPortalUser();
        xxPortalUser.setId(1L);
        xxPortalUser.setLoginId("testUser");
        xxPortalUser.setPassword("testpassword");
        xxPortalUser.setFirstName("testUser");
        VXUser vxUser = new VXUser();
        vxUser.setId(1L);
        VXUserList vXUserList = new VXUserList();
        List<VXUser> vXUsers = new ArrayList<VXUser>();
        vXUsers.add(vxUser);
        vXUserList.setVXUsers(vXUsers );

        List<String> userRoleList = new ArrayList<String>();
        userRoleList.add("ROLE_SYS_ADMIN");
        List<XXPortalUser> listXXPortalUser = new ArrayList<XXPortalUser>();
                listXXPortalUser.add(xxPortalUser);
        vxUser.setUserRoleList(userRoleList);
        XXModuleDefDao xXModuleDefDao = Mockito.mock(XXModuleDefDao.class);

        Mockito.when(daoMgr.getXXPortalUser()).thenReturn(xXPortalUserDao);
        Mockito.when(xXPortalUserDao.findByLoginId(Mockito.anyString())).thenReturn(xxPortalUser);
        Mockito.when(xUserService.getXUserByUserName(xxPortalUser.getLoginId())).thenReturn(vxUser);
        Mockito.when(daoMgr.getXXModuleDef()).thenReturn(xXModuleDefDao);
        Mockito.when(xXModuleDefDao.findAccessibleModulesByUserId(Mockito.anyLong(), Mockito.anyLong())).thenReturn(permissionList);
        Mockito.when(userMgr.encrypt(Mockito.anyString(),Mockito.anyString())).thenReturn(currentEncryptedPassword);
        Mockito.when(xXPortalUserDao.findByRole(Mockito.anyString())).thenReturn(listXXPortalUser);

        roleBasedUserSearchUtil.validateUserAndFetchUserList();

        Mockito.verify(xXPortalUserDao).findByLoginId(Mockito.anyString());
        Mockito.verify(xUserService).getXUserByUserName(xxPortalUser.getLoginId());

        Mockito.verify(xXModuleDefDao).findAccessibleModulesByUserId(Mockito.anyLong(), Mockito.anyLong());
        Mockito.verify(userMgr).encrypt(Mockito.anyString(),Mockito.anyString());
        Mockito.verify(xXPortalUserDao, Mockito.atLeast(2)).findByRole(Mockito.anyString());

    }

}
