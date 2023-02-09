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

import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.common.auth.InvalidCredentialsException;
import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyun.oss.common.utils.StringUtils;

/**
 * {@link SystemPropertiesCredentialsProvider} implementation that provides
 * credentials by looking at the <code>oss.accessKeyId</code> and
 * <code>oss.accessKeySecret</code> Java system properties.
 */
public class SystemPropertiesCredentialsProvider implements CredentialsProvider {

    @Override
    public void setCredentials(Credentials creds) {
        
    }

    @Override
    public Credentials getCredentials() {
        String accessKeyId = StringUtils.trim(System.getProperty(AuthUtils.ACCESS_KEY_SYSTEM_PROPERTY));
        String secretAccessKey = StringUtils.trim(System.getProperty(AuthUtils.SECRET_KEY_SYSTEM_PROPERTY));
        String sessionToken = StringUtils.trim(System.getProperty(AuthUtils.SESSION_TOKEN_SYSTEM_PROPERTY));

        if (accessKeyId == null || accessKeyId.equals("")) {
            throw new InvalidCredentialsException("Access key id should not be null or empty.");
        }
        if (secretAccessKey == null || secretAccessKey.equals("")) {
            throw new InvalidCredentialsException("Secret access key should not be null or empty.");
        }

        return new DefaultCredentials(accessKeyId, secretAccessKey, sessionToken);
    }

}
