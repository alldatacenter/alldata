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
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.AlibabaCloudCredentials;
import com.aliyuncs.auth.AlibabaCloudCredentialsProvider;
import com.aliyuncs.auth.StaticCredentialsProvider;
import com.aliyuncs.auth.sts.AssumeRoleRequest;
import com.aliyuncs.auth.sts.AssumeRoleResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.profile.IClientProfile;

/**
 * STSAssumeRoleSessionCredentialsProvider implementation that uses the STS
 * Security Token Service to assume a Role and create temporary, short-lived
 * sessions to use for authentication.
 */
public class STSAssumeRoleSessionCredentialsProvider implements CredentialsProvider {

    public STSAssumeRoleSessionCredentialsProvider(AlibabaCloudCredentials longLivedCredentials, String roleArn,
            IClientProfile clientProfile) {
        this(new StaticCredentialsProvider(longLivedCredentials), roleArn, clientProfile);
    }

    public STSAssumeRoleSessionCredentialsProvider(AlibabaCloudCredentialsProvider longLivedCredentialsProvider,
            String roleArn, IClientProfile clientProfile) {
        if (roleArn == null) {
            throw new NullPointerException("You must specify a value for roleArn.");
        }
        this.roleArn = roleArn;
        this.roleSessionName = getNewRoleSessionName();
        this.ramClient = new DefaultAcsClient(clientProfile, longLivedCredentialsProvider);
    }

    public STSAssumeRoleSessionCredentialsProvider withRoleSessionName(String roleSessionName) {
        this.roleSessionName = roleSessionName;
        return this;
    }

    public STSAssumeRoleSessionCredentialsProvider withExpiredFactor(double expiredFactor) {
        this.expiredFactor = expiredFactor;
        return this;
    }

    public STSAssumeRoleSessionCredentialsProvider withExpiredDuration(long expiredDurationSeconds) {
        if (expiredDurationSeconds < 900 || expiredDurationSeconds > 3600) {
            throw new IllegalArgumentException("Assume Role session duration should be in the range of 15min - 1hour");
        }
        this.expiredDurationSeconds = expiredDurationSeconds;
        return this;
    }

    public static String getNewRoleSessionName() {
        return "aliyun-java-sdk-" + System.currentTimeMillis();
    }

    @Override
    public void setCredentials(Credentials creds) {

    }

    @Override
    public Credentials getCredentials() {
        if (credentials == null || credentials.willSoonExpire()) {
            synchronized (this) {
                if (credentials == null || credentials.willSoonExpire()) {
                    credentials = getNewSessionCredentials();
                }
            }
        }
        return credentials;
    }

    private BasicCredentials getNewSessionCredentials() {
        AssumeRoleRequest assumeRoleRequest = new AssumeRoleRequest();
        assumeRoleRequest.setRoleArn(roleArn);
        assumeRoleRequest.setRoleSessionName(roleSessionName);
        assumeRoleRequest.setDurationSeconds(expiredDurationSeconds);

        AssumeRoleResponse response = null;
        try {
            response = ramClient.getAcsResponse(assumeRoleRequest);
        } catch (ClientException e) {
            LogUtils.logException("RamClient.getAcsResponse Exception:", e);
            return null;
        }

        return new BasicCredentials(response.getCredentials().getAccessKeyId(),
                response.getCredentials().getAccessKeySecret(), response.getCredentials().getSecurityToken(),
                expiredDurationSeconds).withExpiredFactor(expiredFactor);
    }

    private final DefaultAcsClient ramClient;
    private final String roleArn;
    private String roleSessionName;
    private volatile BasicCredentials credentials;

    private long expiredDurationSeconds = AuthUtils.DEFAULT_EXPIRED_DURATION_SECONDS;
    private double expiredFactor = AuthUtils.DEFAULT_EXPIRED_FACTOR;

}
