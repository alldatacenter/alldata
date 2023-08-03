/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.service.user;

import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.TenantRoleInfo;
import org.apache.inlong.manager.pojo.user.TenantRolePageRequest;
import org.apache.inlong.manager.pojo.user.TenantRoleRequest;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.user.UserRoleCode;
import org.apache.inlong.manager.service.ServiceBaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.apache.inlong.manager.common.enums.ErrorCodeEnum.TENANT_NOT_EXIST;

public class TenantRoleServiceTest extends ServiceBaseTest {

    @Autowired
    private TenantRoleService service;

    @BeforeAll
    public static void initUser() {
        UserInfo userInfo = new UserInfo();
        userInfo.setName("admin");
        LoginUserUtils.setUserLoginInfo(userInfo);
    }

    @Test
    @Order(1)
    public void testSaveWithoutTenant() {
        TenantRoleRequest request = new TenantRoleRequest();
        request.setTenant("not exist tenant");
        request.setRoleCode(UserRoleCode.TENANT_ADMIN);
        request.setUsername(LoginUserUtils.getLoginUser().getName());
        int code = -1;
        try {
            service.save(request, LoginUserUtils.getLoginUser().getName());
        } catch (BusinessException e) {
            code = e.getCode();
        }
        Assertions.assertEquals(TENANT_NOT_EXIST.getCode(), code);
    }

    @Test
    @Order(2)
    public void testSaveCorrect() {
        TenantRoleRequest request = new TenantRoleRequest();
        request.setTenant("public");
        request.setUsername(LoginUserUtils.getLoginUser().getName());
        request.setRoleCode(UserRoleCode.TENANT_ADMIN);
        service.save(request, LoginUserUtils.getLoginUser().getName());
        TenantRoleInfo info = service.getByUsernameAndTenant(request.getUsername(), request.getTenant());
        Assertions.assertEquals(request.getTenant(), info.getTenant());
        Assertions.assertEquals(request.getRoleCode(), info.getRoleCode());
        Assertions.assertEquals(request.getUsername(), info.getUsername());
    }

    @Test
    @Order(3)
    public void testUpdate() {
        String newUser = "new user";
        TenantRoleRequest request = new TenantRoleRequest();
        request.setTenant("public");
        request.setUsername(newUser);
        request.setRoleCode(UserRoleCode.TENANT_ADMIN);
        service.save(request, LoginUserUtils.getLoginUser().getName());
        TenantRoleInfo oldInfo = service.getByUsernameAndTenant(request.getUsername(), request.getTenant());
        oldInfo.setRoleCode(UserRoleCode.TENANT_OPERATOR);
        service.update(oldInfo.genRequest(), LoginUserUtils.getLoginUser().getName());
        TenantRoleInfo newInfo = service.getByUsernameAndTenant(request.getUsername(), request.getTenant());
        Assertions.assertEquals(UserRoleCode.TENANT_OPERATOR, newInfo.getRoleCode());
    }

    @Test
    @Order(4)
    public void testList() {
        int max = 5;
        TenantRoleRequest request = new TenantRoleRequest();
        request.setTenant("public");
        request.setRoleCode(UserRoleCode.TENANT_OPERATOR);
        for (int i = 0; i < max; i++) {
            request.setUsername("testName" + i);
            service.save(request, LoginUserUtils.getLoginUser().getName());
        }
        TenantRolePageRequest pageRequest = new TenantRolePageRequest();
        pageRequest.setKeyword("pub");
        PageResult<TenantRoleInfo> infos = service.listByCondition(pageRequest);
        Assertions.assertEquals(max, infos.getTotal());
    }

}