/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.http.client.base;

import com.fasterxml.jackson.core.type.TypeReference;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class DataVinesAPI {
    private String path;
    private String method;
    private String consumerMediaType;
    private String providerMediaType;
    private Response.Status expectStatus;
    private TypeReference resultType;

    private static final String JSON_UTF8_MEDIA = MediaType.APPLICATION_JSON + "; charset=UTF-8";

    public DataVinesAPI(String path, String method, String consumerMediaType, String providerMediaType, Response.Status status, TypeReference resultType) {
        this.path = path;
        this.method = method;
        this.consumerMediaType = consumerMediaType;
        this.providerMediaType = providerMediaType;
        this.expectStatus = status;
        this.resultType = resultType;
    }
    public DataVinesAPI(String path, String method, Response.Status status, TypeReference resultType) {
        this(path, method, JSON_UTF8_MEDIA, MediaType.APPLICATION_JSON, status, resultType);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getConsumerMediaType() {
        return consumerMediaType;
    }

    public void setConsumerMediaType(String consumerMediaType) {
        this.consumerMediaType = consumerMediaType;
    }

    public String getProviderMediaType() {
        return providerMediaType;
    }

    public void setProviderMediaType(String providerMediaType) {
        this.providerMediaType = providerMediaType;
    }

    public Response.Status getExpectStatus() {
        return expectStatus;
    }

    public void setExpectStatus(Response.Status expectStatus) {
        this.expectStatus = expectStatus;
    }

    public TypeReference getResultType() {
        return resultType;
    }

    public void setResultType(TypeReference resultType) {
        this.resultType = resultType;
    }
}
