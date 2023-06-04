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

package org.apache.inlong.manager.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.user.UserLoginRequest;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.test.BaseTest;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.SecurityManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@TestInstance(Lifecycle.PER_CLASS)
@SpringBootTest(classes = InlongManagerMain.class)
public abstract class WebBaseTest extends BaseTest {

    public MockMvc mockMvc;
    @Resource
    private ObjectMapper objectMapper;
    @Resource
    private WebApplicationContext context;

    @BeforeAll
    void baseSetup() {
        SecurityUtils.setSecurityManager(context.getBean(SecurityManager.class));

        mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .alwaysDo(print())
                .build();
    }

    @BeforeEach
    void baseBeforeEach() throws Exception {
        logout();
        adminLogin();
    }

    protected void logout() {
        SecurityUtils.getSubject().logout();
    }

    protected void adminLogin() throws Exception {
        UserLoginRequest loginUser = new UserLoginRequest();
        loginUser.setUsername("admin");
        loginUser.setPassword("inlong");

        MvcResult mvcResult = mockMvc.perform(
                post("/api/anno/login")
                        .content(JsonUtils.toJsonString(loginUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String resBodyObj = getResBodyObj(mvcResult, String.class);
        Assertions.assertNotNull(resBodyObj);

        Assertions.assertTrue(SecurityUtils.getSubject().isAuthenticated());
    }

    @SneakyThrows
    protected MvcResult postForSuccessMvcResult(String url, Object body) {
        return mockMvc.perform(
                post(url)
                        .content(JsonUtils.toJsonString(body))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    @SneakyThrows
    protected MvcResult getForSuccessMvcResult(String url, Object... pathVariable) {
        return mockMvc.perform(
                get(url, pathVariable)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    @SneakyThrows
    protected MvcResult deleteForSuccessMvcResult(String url, Object... pathVariable) {
        return mockMvc.perform(
                delete(url, pathVariable)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    protected void operatorLogin() throws Exception {
        UserLoginRequest loginUser = new UserLoginRequest();
        loginUser.setUsername("operator");
        loginUser.setPassword("inlong");

        MvcResult mvcResult = mockMvc.perform(
                post("/api/anno/login")
                        .content(JsonUtils.toJsonString(loginUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        String resBodyObj = getResBodyObj(mvcResult, String.class);
        Assertions.assertNotNull(resBodyObj);

        Assertions.assertTrue(SecurityUtils.getSubject().isAuthenticated());
    }

    public <T> Response<T> getResBody(MvcResult mvcResult, Class<T> t) throws Exception {
        return objectMapper
                .readValue(
                        mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8),
                        objectMapper.getTypeFactory().constructParametricType(Response.class, t));
    }

    public <T> T getResBodyObj(MvcResult mvcResult, Class<T> t) throws Exception {
        Response<T> resBody = getResBody(mvcResult, t);
        Assertions.assertTrue(resBody.isSuccess());
        return resBody.getData();
    }

    public <T> List<T> getResBodyList(MvcResult mvcResult, Class<T> t) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return objectMapper
                .readValue(
                        jsonNode.get("data").toString(),
                        objectMapper.getTypeFactory().constructParametricType(List.class, t));
    }

    public <T, R> Map<T, R> getResBodyMap(MvcResult mvcResult, Class<T> keyType, Class<R> valueType) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return objectMapper
                .readValue(
                        jsonNode.get("data").toString(),
                        this.objectMapper.getTypeFactory().constructParametricType(HashMap.class, keyType, valueType));
    }

    public <T, R> Map<T, R> getResBodyListMap(MvcResult mvcResult, Class<T> keyType, Class<R> valueType)
            throws Exception {
        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return objectMapper
                .readValue(
                        jsonNode.get("data").toString(),
                        this.objectMapper.getTypeFactory().constructParametricType(HashMap.class, keyType, valueType));
    }

    public <T> List<T> getResBodyPageList(MvcResult mvcResult, Class<T> t) throws Exception {
        JsonNode jsonNode = objectMapper.readTree(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8));
        return objectMapper
                .readValue(
                        jsonNode.get("data").get("records").toString(),
                        objectMapper.getTypeFactory().constructParametricType(List.class, t));
    }
}
