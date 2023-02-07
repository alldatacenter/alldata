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

import org.apache.inlong.manager.client.api.NoAuth;
import org.apache.inlong.manager.client.api.ClientConfiguration;
import org.apache.inlong.manager.client.api.inner.client.NoAuthClient;
import org.apache.inlong.manager.client.api.inner.client.ClientFactory;
import org.apache.inlong.manager.client.api.util.ClientUtils;
import org.apache.inlong.manager.common.util.Preconditions;
import org.apache.inlong.manager.pojo.user.UserRequest;

/**
 * No auth interface implementation
 */
public class NoAuthImpl implements NoAuth {

    private final NoAuthClient noAuthClient;

    public NoAuthImpl(ClientConfiguration configuration) {
        ClientFactory clientFactory = ClientUtils.getClientFactory(configuration);
        this.noAuthClient = clientFactory.getNoAuthClient();
    }

    @Override
    public Integer register(UserRequest request) {
        Preconditions.checkNotEmpty(request.getName(), "username cannot be empty");
        Preconditions.checkNotEmpty(request.getPassword(), "password cannot be empty");
        Preconditions.checkNotNull(request.getAccountType(), "accountType cannot be null");
        Preconditions.checkNotNull(request.getValidDays(), "validDays cannot be null");

        return noAuthClient.register(request);
    }
}
