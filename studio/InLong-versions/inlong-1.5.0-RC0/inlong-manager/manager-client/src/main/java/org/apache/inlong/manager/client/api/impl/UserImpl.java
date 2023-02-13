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

package org.apache.inlong.manager.client.api.impl;

import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.User;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.client.api.inner.client.UserClient;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.common.PageResult;
import org.apache.inlong.manager.pojo.user.UserInfo;
import org.apache.inlong.manager.pojo.user.UserRequest;

public class UserImpl implements User {

    private final UserClient userClient;

    public UserImpl(ClientConfiguration configuration) {
        ClientFactory clientFactory = ClientUtils.getClientFactory(configuration);
        this.userClient = clientFactory.getUserClient();
    }

    @Override
    public UserInfo currentUser() {
        return userClient.currentUser();
    }

    @Override
    public Integer register(UserRequest userInfo) {
        Preconditions.checkNotEmpty(userInfo.getName(), "username cannot be empty");
        Preconditions.checkNotEmpty(userInfo.getPassword(), "password cannot be empty");
        return userClient.register(userInfo);
    }

    @Override
    public UserInfo getById(Integer id) {
        Preconditions.checkNotNull(id, "user id cannot be null");
        return userClient.getById(id);
    }

    @Override
    public UserInfo getByName(String name) {
        Preconditions.checkNotNull(name, "username cannot be null");
        return userClient.getByName(name);
    }

    @Override
    public PageResult<UserInfo> list(UserRequest request) {
        Preconditions.checkNotNull(request, "request cannot be null");
        return userClient.list(request);
    }

    @Override
    public Integer update(UserRequest userInfo) {
        Preconditions.checkNotNull(userInfo, "userinfo cannot be null");
        Preconditions.checkNotNull(userInfo.getId(), "user id cannot be null");
        return userClient.update(userInfo);
    }

    @Override
    public Boolean delete(Integer id) {
        Preconditions.checkNotNull(id, "user id cannot be null");
        return userClient.delete(id);
    }
}
