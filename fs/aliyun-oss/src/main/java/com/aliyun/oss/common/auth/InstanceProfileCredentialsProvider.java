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

import java.util.concurrent.locks.ReentrantLock;

/**
 * Credentials provider implementation that loads credentials from the Ali Cloud
 * ECS Instance Metadata Service.
 */
public class InstanceProfileCredentialsProvider implements CredentialsProvider {

    public InstanceProfileCredentialsProvider(String roleName) {
        if (null == roleName) {
            throw new NullPointerException("You must specifiy a valid role name.");
        }
        this.roleName = roleName;
        this.fetcher = new InstanceProfileCredentialsFetcher();
        this.fetcher.setRoleName(this.roleName);
    }
    
    public InstanceProfileCredentialsProvider withCredentialsFetcher(InstanceProfileCredentialsFetcher fetcher) {
        this.fetcher = fetcher;
        return this;
    }

    @Override
    public void setCredentials(Credentials creds) {

    }

    @Override
    public InstanceProfileCredentials getCredentials() {
        if (credentials == null || credentials.isExpired()) {
            try {
                lock.lock();
                if (credentials == null || credentials.isExpired()) {
                    try {
                        credentials = (InstanceProfileCredentials) fetcher.fetch(maxRetryTimes);
                    } catch (ClientException e) {
                        LogUtils.logException("EcsInstanceCredentialsFetcher.fetch Exception:", e);
                        return null;
                    }
                }
            } finally {
                lock.unlock();
            }
        } else if (credentials.willSoonExpire() && credentials.shouldRefresh()) {
            try {
                lock.lock();
                if (credentials.willSoonExpire() && credentials.shouldRefresh()) {
                    try {
                        credentials = (InstanceProfileCredentials) fetcher.fetch();
                    } catch (ClientException e) {
                        // Use the current expiring session token and wait for next round
                        credentials.setLastFailedRefreshTime();
                        LogUtils.logException("EcsInstanceCredentialsFetcher.fetch Exception:", e);
                    }
                }
            } finally {
                lock.unlock();
            }
        }

        return credentials;
    }

    private final String roleName;
    private InstanceProfileCredentials credentials;
    private InstanceProfileCredentialsFetcher fetcher;

    private int maxRetryTimes = AuthUtils.MAX_ECS_METADATA_FETCH_RETRY_TIMES;
    private ReentrantLock lock = new ReentrantLock();

}
