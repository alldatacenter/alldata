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

package com.aliyun.oss.model;

import com.aliyun.oss.HttpMethod;

public class OptionsRequest extends GenericRequest {

    private String origin;
    private HttpMethod requestMethod;
    private String requestHeaders;

    public OptionsRequest() {
    }

    public OptionsRequest(String bucketName, String key) {
        super(bucketName, key);
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public OptionsRequest withOrigin(String origin) {
        setOrigin(origin);
        return this;
    }

    public HttpMethod getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(HttpMethod requestMethod) {
        this.requestMethod = requestMethod;
    }

    public OptionsRequest withRequestMethod(HttpMethod requestMethod) {
        setRequestMethod(requestMethod);
        return this;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public void setRequestHeaders(String requestHeaders) {
        this.requestHeaders = requestHeaders;
    }

    public OptionsRequest withRequestHeaders(String requestHeaders) {
        setRequestHeaders(requestHeaders);
        return this;
    }

    @Deprecated
    public String getObjectName() {
        return this.getKey();
    }

    @Deprecated
    public void setObjectName(String objectName) {
        this.setKey(objectName);
    }
}
