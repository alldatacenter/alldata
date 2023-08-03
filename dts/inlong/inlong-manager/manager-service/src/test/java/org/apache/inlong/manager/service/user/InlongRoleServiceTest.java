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

import org.apache.inlong.manager.pojo.user.InlongRoleInfo;
import org.apache.inlong.manager.pojo.user.InlongRolePageRequest;
import org.apache.inlong.manager.pojo.user.InlongRoleRequest;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.user.UserRoleCode;
import org.apache.inlong.manager.service.ServiceBaseTest;

import com.github.pagehelper.PageInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InlongRoleServiceTest extends ServiceBaseTest {

    @Autowired
    private InlongRoleService inlongRoleService;

    @BeforeAll
    public static void initUser() {
        UserInfo userInfo = new UserInfo();
        userInfo.setName("admin");
        LoginUserUtils.setUserLoginInfo(userInfo);
    }

    @Test
    @Order(1)
    public void testSave() {
        InlongRoleRequest request = new InlongRoleRequest();
        request.setUsername("new admin");
        request.setRoleCode(UserRoleCode.INLONG_ADMIN);
        inlongRoleService.save(request, LoginUserUtils.getLoginUser().getName());
        InlongRoleInfo info = inlongRoleService.getByUsername(request.getUsername());
        Assertions.assertEquals(request.getRoleCode(), info.getRoleCode());
        Assertions.assertEquals(request.getUsername(), info.getUsername());
    }

    @Test
    @Order(2)
    public void testUpdate() {
        InlongRoleRequest request = new InlongRoleRequest();
        request.setUsername("admin2");
        request.setRoleCode(UserRoleCode.INLONG_ADMIN);
        inlongRoleService.save(request, LoginUserUtils.getLoginUser().getName());
        InlongRoleInfo info = inlongRoleService.getByUsername(request.getUsername());
        info.setRoleCode(UserRoleCode.INLONG_OPERATOR);
        inlongRoleService.update(info.genRequest(), LoginUserUtils.getLoginUser().getName());
        InlongRoleInfo updatedInfo = inlongRoleService.getByUsername(request.getUsername());
        Assertions.assertEquals(UserRoleCode.INLONG_OPERATOR, updatedInfo.getRoleCode());
    }

    @Test
    @Order(3)
    public void testList() {
        int max = 6;
        InlongRoleRequest request = new InlongRoleRequest();
        request.setRoleCode(UserRoleCode.INLONG_OPERATOR);
        for (int i = 0; i < max; i++) {
            request.setUsername("test" + i);
            inlongRoleService.save(request, LoginUserUtils.getLoginUser().getName());
        }
        InlongRolePageRequest pageRequest = new InlongRolePageRequest();
        pageRequest.setRoleCode(UserRoleCode.INLONG_OPERATOR);
        PageInfo<InlongRoleInfo> infos = inlongRoleService.listByCondition(pageRequest);
        Assertions.assertEquals(max, infos.getSize());

        pageRequest.setRoleCode(UserRoleCode.INLONG_ADMIN);
        infos = inlongRoleService.listByCondition(pageRequest);
        Assertions.assertEquals(1, infos.getSize());
    }
}