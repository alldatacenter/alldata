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

package org.apache.inlong.manager.service.tenant;

import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.tenant.InlongTenantInfo;
import org.apache.inlong.manager.pojo.tenant.InlongTenantPageRequest;
import org.apache.inlong.manager.pojo.tenant.InlongTenantRequest;
import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.service.ServiceBaseTest;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class InlongTenantServiceTest extends ServiceBaseTest {

    @Autowired
    private InlongTenantService tenantService;

    @BeforeAll
    public static void initUser() {
        UserInfo userInfo = new UserInfo();
        userInfo.setName("admin");
        LoginUserUtils.setUserLoginInfo(userInfo);
    }

    @Test
    @Order(1)
    public void testSaveAndGet() {
        InlongTenantRequest request = new InlongTenantRequest();
        String name = "saveAndGet";
        request.setName(name);
        tenantService.save(request);
        InlongTenantInfo info = tenantService.getByName(request.getName());
        Assertions.assertNotNull(info);
        Assertions.assertEquals(request.getName(), info.getName());
    }

    @Test
    @Order(2)
    public void testSameName() {
        InlongTenantRequest request = new InlongTenantRequest();
        String name = "sameName";
        request.setName(name);
        tenantService.save(request);
        Assertions.assertThrows(BusinessException.class, () -> tenantService.save(request));
    }

    @Test
    @Order(3)
    public void testUpdate() {
        InlongTenantRequest request = new InlongTenantRequest();
        String name = "update";
        request.setName(name);
        tenantService.save(request);
        InlongTenantInfo info = tenantService.getByName(name);
        String desc = "update description";
        info.setDescription(desc);
        tenantService.update(info.genRequest());
        InlongTenantInfo info2 = tenantService.getByName(name);
        Assertions.assertEquals(desc, info2.getDescription());
    }

    @Test
    @Order(4)
    public void testPage() {
        InlongTenantRequest request = new InlongTenantRequest();
        int size = 5;
        for (int i = 0; i < size; i++) {
            request.setName("test" + i);
            tenantService.save(request);
        }
        InlongTenantPageRequest pageRequest = new InlongTenantPageRequest();
        pageRequest.setKeyword("test");
        PageResult<InlongTenantInfo> infoPage = tenantService.listByCondition(pageRequest,
                LoginUserUtils.getLoginUser());
        Assertions.assertEquals(size, infoPage.getList().size());
    }
}