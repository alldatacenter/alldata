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

package org.apache.inlong.manager.client.api.inner;

import org.apache.inlong.manager.client.api.inner.client.NoAuthClient;
import org.apache.inlong.manager.common.enums.UserTypeEnum;
import org.apache.inlong.manager.common.util.JsonUtils;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.user.UserRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;

/**
 * Tests for {@link NoAuthClient}
 */
public class NoAuthClientTest extends ClientFactoryTest {

    private static final NoAuthClient NO_AUTH_CLIENT = clientFactory.getNoAuthClient();

    @Test
    void testRegister() {
        stubFor(
                post(urlMatching("/inlong/manager/api/anno/register.*"))
                        .willReturn(
                                okJson(JsonUtils.toJsonString(
                                        Response.success(1)))));

        UserRequest request = UserRequest.builder()
                .name("username")
                .password("pwd")
                .accountType(UserTypeEnum.ADMIN.getCode())
                .validDays(9999)
                .build();

        Integer userId = NO_AUTH_CLIENT.register(request);
        Assertions.assertEquals(1, userId);
    }
}
