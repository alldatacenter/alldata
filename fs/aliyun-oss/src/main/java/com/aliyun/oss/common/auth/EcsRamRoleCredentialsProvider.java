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
import com.aliyun.oss.common.utils.LogUtils;
import com.aliyuncs.exceptions.ClientException;

public class EcsRamRoleCredentialsProvider implements CredentialsProvider {

    public EcsRamRoleCredentialsProvider(String ossAuthServerHost) {
        this.fetcher = new EcsRamRoleCredentialsFetcher(ossAuthServerHost);
    }
    
    public EcsRamRoleCredentialsProvider withCredentialsFetcher(EcsRamRoleCredentialsFetcher fetcher) {
        this.fetcher = fetcher;
        return this;
    }

    @Override
    public void setCredentials(Credentials creds) {

    }

    @Override
    public Credentials getCredentials() {        
        if (credentials == null || credentials.willSoonExpire()) {
            synchronized (this) {
                if (credentials == null || credentials.willSoonExpire()) {
                    try {
                        credentials = (BasicCredentials) fetcher.fetch(maxRetryTimes);
                    } catch (ClientException e) {
                        LogUtils.logException("EcsRoleCredentialsProvider.fetch Exception:", e);
                        return null;
                    }
                }
            }
        }
        return credentials;
    }

    private BasicCredentials credentials;
    private EcsRamRoleCredentialsFetcher fetcher;

    private int maxRetryTimes = AuthUtils.MAX_ECS_METADATA_FETCH_RETRY_TIMES;

}
