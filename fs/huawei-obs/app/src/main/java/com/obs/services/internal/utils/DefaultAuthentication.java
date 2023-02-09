/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal.utils;

public class DefaultAuthentication implements IAuthentication {

    private String stringToSign;

    private String authorization;

    private String canonicalRequest;

    public DefaultAuthentication(String canonicalRequest, String stringToSign, String authorization) {
        this.canonicalRequest = canonicalRequest;
        this.stringToSign = stringToSign;
        this.authorization = authorization;
    }

    @Override
    public String getCanonicalRequest() {
        return this.canonicalRequest;
    }

    @Override
    public String getAuthorization() {
        return this.authorization;
    }

    @Override
    public String getStringToSign() {
        return this.stringToSign;
    }

}
