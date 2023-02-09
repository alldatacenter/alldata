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
import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyun.oss.common.utils.LogUtils;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.auth.KeyPairCredentials;
import com.aliyuncs.auth.sts.GetSessionAccessKeyRequest;
import com.aliyuncs.auth.sts.GenerateSessionAccessKeyResponse;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.ProtocolType;
import com.aliyuncs.profile.IClientProfile;

/**
 * STSKeyPairSessionCredentialsProvider implementation that uses the RSA key
 * pair to create temporary, short-lived sessions to use for authentication.
 */
public class STSKeyPairSessionCredentialsProvider implements CredentialsProvider {

    public STSKeyPairSessionCredentialsProvider(KeyPairCredentials keyPairCredentials, IClientProfile profile) {
        this.keyPairCredentials = keyPairCredentials;
        this.ramClient = new DefaultAcsClient(profile, keyPairCredentials);
    }

    public STSKeyPairSessionCredentialsProvider withExpiredDuration(long expiredDurationSeconds) {
        this.expiredDurationSeconds = expiredDurationSeconds;
        return this;
    }

    public STSKeyPairSessionCredentialsProvider withExpiredFactor(double expiredFactor) {
        this.expiredFactor = expiredFactor;
        return this;
    }

    @Override
    public void setCredentials(Credentials creds) {

    }

    @Override
    public Credentials getCredentials() {
        if (sessionCredentials == null || sessionCredentials.willSoonExpire()) {
            sessionCredentials = getNewSessionCredentials();
        }
        return sessionCredentials;
    }

    private BasicCredentials getNewSessionCredentials() {
        GetSessionAccessKeyRequest request = new GetSessionAccessKeyRequest();
        request.setPublicKeyId(keyPairCredentials.getAccessKeyId());
        request.setDurationSeconds((int) expiredDurationSeconds);
        request.setSysProtocol(ProtocolType.HTTPS);

        GenerateSessionAccessKeyResponse response = null;
        try {
            response = this.ramClient.getAcsResponse(request);
        } catch (ClientException e) {
            LogUtils.logException("RamClient.getAcsResponse Exception:", e);
            return null;
        }

        return new BasicCredentials(response.getSessionAccessKey().getSessionAccessKeyId(),
                response.getSessionAccessKey().getSessionAccessKeySecert(), null, expiredDurationSeconds)
                        .withExpiredFactor(expiredFactor);
    }

    private DefaultAcsClient ramClient;
    private KeyPairCredentials keyPairCredentials;
    private BasicCredentials sessionCredentials;

    private long expiredDurationSeconds = AuthUtils.DEFAULT_EXPIRED_DURATION_SECONDS;
    private double expiredFactor = AuthUtils.DEFAULT_EXPIRED_FACTOR;

}