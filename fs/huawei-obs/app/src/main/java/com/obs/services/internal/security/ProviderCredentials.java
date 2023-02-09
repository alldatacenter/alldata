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

package com.obs.services.internal.security;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.BasicObsCredentialsProvider;
import com.obs.services.IObsCredentialsProvider;
import com.obs.services.internal.ObsConstraint;
import com.obs.services.model.AuthTypeEnum;

import java.util.LinkedHashMap;
import java.util.Map;

public class ProviderCredentials {
    protected static final ILogger log = LoggerBuilder.getLogger(ProviderCredentials.class);

    protected AuthTypeEnum authType;

    private LinkedHashMap<String, AuthTypeEnum> localAuthType;

    private IObsCredentialsProvider obsCredentialsProvider;

    private boolean isAuthTypeNegotiation;

    public String getRegion() {
        return ObsConstraint.DEFAULT_BUCKET_LOCATION_VALUE;
    }

    public ProviderCredentials(String accessKey, String secretKey, String securityToken) {
        this.setObsCredentialsProvider(new BasicObsCredentialsProvider(accessKey, secretKey, securityToken));
    }

    public void setLocalAuthTypeCacheCapacity(int localAuthTypeCacheCapacity) {
        localAuthType = new LinkedHashMap<String, AuthTypeEnum>(localAuthTypeCacheCapacity, 0.7F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, AuthTypeEnum> entry) {
                return this.size() > localAuthTypeCacheCapacity;
            }
        };
    }

    public AuthTypeEnum getAuthType() {
        return authType;
    }

    public void setAuthType(AuthTypeEnum authType) {
        this.authType = authType;
    }

    public void setIsAuthTypeNegotiation(boolean isAuthTypeNegotiation) {
        this.isAuthTypeNegotiation = isAuthTypeNegotiation;
    }

    public boolean getIsAuthTypeNegotiation() {
        return isAuthTypeNegotiation;
    }

    public void setObsCredentialsProvider(IObsCredentialsProvider obsCredentialsProvider) {
        this.obsCredentialsProvider = obsCredentialsProvider;
    }

    public IObsCredentialsProvider getObsCredentialsProvider() {
        return this.obsCredentialsProvider;
    }

    public BasicSecurityKey getSecurityKey() {
        return (BasicSecurityKey) this.obsCredentialsProvider.getSecurityKey();
    }

    public AuthTypeEnum getLocalAuthType(String bucketName) {
        if (!isAuthTypeNegotiation) {
            return authType;
        }
        AuthTypeEnum authTypeEnum = localAuthType.get(bucketName);
        return authTypeEnum == null ? authType : authTypeEnum;
    }

    public void setLocalAuthType(String bucketName, AuthTypeEnum authType) {
        if (localAuthType == null || bucketName.isEmpty()) {
            return;
        }

        localAuthType.put(bucketName, authType);
    }

    public LinkedHashMap<String, AuthTypeEnum> getLocalAuthType() {
        return localAuthType;
    }

    public void setLocalAuthType(LinkedHashMap<String, AuthTypeEnum> localAuthType) {
        this.localAuthType = localAuthType;
    }
}
