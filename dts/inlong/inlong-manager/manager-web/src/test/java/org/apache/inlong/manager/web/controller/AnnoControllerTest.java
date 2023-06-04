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

package org.apache.inlong.manager.web.controller;

import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.pojo.user.UserLoginRequest;
import org.apache.inlong.manager.pojo.user.UserRequest;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.web.WebBaseTest;
import org.apache.shiro.SecurityUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@TestInstance(Lifecycle.PER_CLASS)
class AnnoControllerTest extends WebBaseTest {

    // Password contains uppercase and lowercase numeric special characters
    private static final String TEST_PWD = "test_#$%%Y@UI$123";

    @BeforeAll
    void setup() {
        logout();
    }

    @Test
    void testLogin() throws Exception {
        adminLogin();
    }

    @Test
    void testLoginFailByWrongPwd() throws Exception {
        UserLoginRequest loginUser = new UserLoginRequest();
        loginUser.setUsername("admin");
        // Wrong pwd
        loginUser.setPassword("test_wrong_pwd");

        MvcResult mvcResult = mockMvc.perform(
                post("/api/anno/login")
                        .content(JsonUtils.toJsonString(loginUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Response<String> response = getResBody(mvcResult, String.class);
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertTrue(response.getErrMsg().contains("incorrect"));
    }

    /**
     * It will throw many error logs, so keep it in the local test
     */
    @Disabled
    void testLoginFailByWrongPwdAndLockAccount() throws Exception {
        UserLoginRequest loginUser = new UserLoginRequest();
        loginUser.setUsername("test_lock_account");
        // Wrong pwd
        loginUser.setPassword("test_wrong_pwd");

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(
                    post("/api/anno/login")
                            .content(JsonUtils.toJsonString(loginUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();
        }

        // account is locked
        MvcResult mvcResult = mockMvc.perform(
                post("/api/anno/login")
                        .content(JsonUtils.toJsonString(loginUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        Response<String> response = getResBody(mvcResult, String.class);
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertTrue(response.getErrMsg().contains("account has been locked"));
    }

    /**
     * It will throw many error logs, so keep it in the local test
     */
    @Disabled
    void testLoginSuccessfulAndClearErrorCount() throws Exception {
        UserLoginRequest loginUser = new UserLoginRequest();
        loginUser.setUsername("admin");
        // Wrong pwd
        loginUser.setPassword("test_wrong_pwd");

        MvcResult mvcResult = null;
        for (int i = 0; i < 19; i++) {
            // Before locking account, input correct pwd to clear error count
            if (i == 9) {
                loginUser.setPassword("inlong");
            } else {
                loginUser.setPassword("test_wrong_pwd");
            }
            mvcResult = mockMvc.perform(
                    post("/api/anno/login")
                            .content(JsonUtils.toJsonString(loginUser))
                            .contentType(MediaType.APPLICATION_JSON)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andReturn();
        }

        Response<String> response = getResBody(mvcResult, String.class);
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertFalse(response.getErrMsg().contains("account has been locked"));
    }

    @Test
    void testRegister() throws Exception {
        UserRequest userInfo = UserRequest.builder()
                .name("test_name")
                .password(TEST_PWD)
                .accountType(UserTypeEnum.ADMIN.getCode())
                .validDays(88888)
                .build();

        MvcResult mvcResult = mockMvc.perform(
                post("/api/anno/register")
                        .content(JsonUtils.toJsonString(userInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Response<Integer> resBody = getResBody(mvcResult, Integer.class);
        Assertions.assertTrue(resBody.isSuccess() && resBody.getData() > 0);
    }

    @Test
    void testRegisterFailByExistName() throws Exception {
        UserRequest userInfo = UserRequest.builder()
                // Username already exists in the init sql
                .name("admin")
                .password(TEST_PWD)
                .accountType(UserTypeEnum.ADMIN.getCode())
                .validDays(88888)
                .build();

        MvcResult mvcResult = mockMvc.perform(
                post("/api/anno/register")
                        .content(JsonUtils.toJsonString(userInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Response<Integer> resBody = getResBody(mvcResult, Integer.class);
        Assertions.assertFalse(resBody.isSuccess());
        Assertions.assertTrue(resBody.getErrMsg().contains("already exists"));
    }

    @Test
    void testLogout() throws Exception {
        testLogin();

        MvcResult mvcResult = mockMvc.perform(
                get("/api/anno/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Response<String> resBody = getResBody(mvcResult, String.class);
        Assertions.assertTrue(resBody.isSuccess());
        Assertions.assertFalse(SecurityUtils.getSubject().isAuthenticated());
    }

    @Test
    void testRegisterFailByInvalidType() throws Exception {
        UserRequest userInfo = UserRequest.builder()
                .name("admin11")
                .password(TEST_PWD)
                // invalidType
                .accountType(3)
                .validDays(88888)
                .build();

        MvcResult mvcResult = mockMvc.perform(
                post("/api/anno/register")
                        .content(JsonUtils.toJsonString(userInfo))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        Response<Integer> resBody = getResBody(mvcResult, Integer.class);
        Assertions.assertFalse(resBody.isSuccess());
        Assertions.assertTrue(resBody.getErrMsg().contains("must in 0,1"));
    }

}
