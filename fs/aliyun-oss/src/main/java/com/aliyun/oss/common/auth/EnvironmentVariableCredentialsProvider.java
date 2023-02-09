/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.auth;

import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyun.oss.common.utils.StringUtils;

/**
 * {@link EnvironmentVariableCredentialsProvider} implementation that provides
 * credentials by looking at the: <code>OSS_ACCESS_KEY_ID</code> and
 * <code>OSS_ACCESS_KEY_SECRET</code> environment variables.
 */
public class EnvironmentVariableCredentialsProvider implements CredentialsProvider {

    @Override
    public void setCredentials(Credentials creds) {
        
    }

    @Override
    public Credentials getCredentials() {
        String accessKeyId = StringUtils.trim(System.getenv(AuthUtils.ACCESS_KEY_ENV_VAR));
        String secretAccessKey = StringUtils.trim(System.getenv(AuthUtils.SECRET_KEY_ENV_VAR));
        String sessionToken = StringUtils.trim(System.getenv(AuthUtils.SESSION_TOKEN_ENV_VAR));

        if (accessKeyId == null || accessKeyId.equals("")) {
            throw new InvalidCredentialsException("Access key id should not be null or empty.");
        }
        if (secretAccessKey == null || secretAccessKey.equals("")) {
            throw new InvalidCredentialsException("Secret access key should not be null or empty.");
        }
        
        return new DefaultCredentials(accessKeyId, secretAccessKey, sessionToken);
    }

}
