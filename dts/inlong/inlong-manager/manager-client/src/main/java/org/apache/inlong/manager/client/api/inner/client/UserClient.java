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

package org.apache.inlong.manager.client.api.inner.client;

import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.service.UserApi;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.common.Response;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.user.UserRequest;

/**
 * Client for {@link UserApi}.
 */
public class UserClient {

    private final UserApi userApi;

    public UserClient(ClientConfiguration configuration) {
        userApi = ClientUtils.createRetrofit(configuration).create(UserApi.class);
    }

    /**
     * get current user
     *
     * @return user info
     */
    public UserInfo currentUser() {
        Response<UserInfo> response = ClientUtils.executeHttpCall(userApi.currentUser());
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Save user info
     *
     * @param userInfo user info request
     * @return user id after saving
     */
    public Integer register(UserRequest userInfo) {
        Preconditions.expectNotBlank(userInfo.getName(), ErrorCodeEnum.INVALID_PARAMETER, "username cannot be empty");
        Preconditions.expectNotBlank(userInfo.getPassword(), ErrorCodeEnum.INVALID_PARAMETER,
                "password cannot be empty");

        Response<Integer> response = ClientUtils.executeHttpCall(userApi.register(userInfo));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Get user info by user id
     *
     * @param id user id
     * @return user info
     */
    public UserInfo getById(Integer id) {
        Preconditions.expectNotNull(id, "user id cannot be null");

        Response<UserInfo> response = ClientUtils.executeHttpCall(userApi.getById(id));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Get user info by username
     *
     * @param name username
     * @return user info
     */
    public UserInfo getByName(String name) {
        Preconditions.expectNotBlank(name, ErrorCodeEnum.INVALID_PARAMETER, "username cannot be null");

        Response<UserInfo> response = ClientUtils.executeHttpCall(userApi.getByName(name));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * List all users basic info by request condition
     *
     * @param request request
     * @return user info list
     */
    public PageResult<UserInfo> list(UserRequest request) {
        Preconditions.expectNotNull(request, "request cannot be null");

        Response<PageResult<UserInfo>> response = ClientUtils.executeHttpCall(userApi.list(request));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Update user info
     *
     * @param userInfo user info request
     * @return user id
     */
    public Integer update(UserRequest userInfo) {
        Preconditions.expectNotNull(userInfo, "userinfo cannot be null");
        Preconditions.expectNotNull(userInfo.getId(), "user id cannot be null");

        Response<Integer> response = ClientUtils.executeHttpCall(userApi.update(userInfo));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }

    /**
     * Delete user by id
     *
     * @param id user id
     * @return whether succeed
     */
    public Boolean delete(Integer id) {
        Preconditions.expectNotNull(id, "user id cannot be null");

        Response<Boolean> response = ClientUtils.executeHttpCall(userApi.delete(id));
        ClientUtils.assertRespSuccess(response);
        return response.getData();
    }
}
