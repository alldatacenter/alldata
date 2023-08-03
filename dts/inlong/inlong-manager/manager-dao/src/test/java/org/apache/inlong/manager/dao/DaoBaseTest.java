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

package org.apache.inlong.manager.dao;

import org.apache.inlong.manager.pojo.user.LoginUserUtils;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.test.BaseTest;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Rollback
@SpringBootApplication
@SpringBootTest(classes = DaoBaseTest.class)
public abstract class DaoBaseTest extends BaseTest {

    public static final String PUBLIC_TENANT = "public";
    public static final String ANOTHER_TENANT = "another";
    public static final String ADMIN = "admin";

    @BeforeEach
    public void login() {
        UserInfo userInfo = new UserInfo();
        userInfo.setTenant(PUBLIC_TENANT);
        userInfo.setName(ADMIN);
        LoginUserUtils.setUserLoginInfo(userInfo);
    }

    public void setOtherUser(String newUser) {
        UserInfo info = LoginUserUtils.getLoginUser();
        info.setName(newUser);
    }

    public void setOtherTenant(String newTenant) {
        UserInfo info = LoginUserUtils.getLoginUser();
        info.setTenant(newTenant);
    }

}
