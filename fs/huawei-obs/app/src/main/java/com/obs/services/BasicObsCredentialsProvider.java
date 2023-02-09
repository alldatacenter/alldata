/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services;

import com.obs.services.internal.security.BasicSecurityKey;
import com.obs.services.model.ISecurityKey;

public class BasicObsCredentialsProvider implements IObsCredentialsProvider {

    private volatile ISecurityKey securityKey;

    public BasicObsCredentialsProvider(ISecurityKey securityKey) {
        checkISecurityKey(securityKey);
        this.securityKey = securityKey;
    }

    public BasicObsCredentialsProvider(String accessKey, String secretKey) {
        this(accessKey, secretKey, null);
    }

    public BasicObsCredentialsProvider(String accessKey, String secretKey, String securityToken) {
        checkSecurityKey(accessKey, secretKey);
        this.securityKey = new BasicSecurityKey(accessKey, secretKey, securityToken);
    }

    private static void checkSecurityKey(String accessKey, String secretKey) {
        if (accessKey == null) {
            throw new IllegalArgumentException("accessKey should not be null.");
        }

        if (secretKey == null) {
            throw new IllegalArgumentException("secretKey should not be null.");
        }
    }

    @Override
    public void setSecurityKey(ISecurityKey securityKey) {
        checkISecurityKey(securityKey);
        this.securityKey = securityKey;
    }

    private void checkISecurityKey(ISecurityKey securityKey) {
        if (securityKey == null) {
            throw new IllegalArgumentException("securityKey should not be null.");
        }
        checkSecurityKey(securityKey.getAccessKey(), securityKey.getSecretKey());
    }

    @Override
    public ISecurityKey getSecurityKey() {
        if (this.securityKey == null) {
            throw new IllegalArgumentException("Invalid securityKey");
        }

        return this.securityKey;
    }
}
