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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.http.HttpRequest;
import com.aliyuncs.http.HttpResponse;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.http.clients.CompatibleUrlConnClient;

public abstract class HttpCredentialsFetcher implements CredentialsFetcher {

    public abstract URL buildUrl() throws ClientException;

    public abstract Credentials parse(HttpResponse response) throws ClientException;

    @Override
    public Credentials fetch() throws ClientException {
        URL url = buildUrl();
        HttpRequest request = new HttpRequest(url.toString());
        request.setSysMethod(MethodType.GET);
        request.setSysConnectTimeout(AuthUtils.DEFAULT_HTTP_SOCKET_TIMEOUT_IN_MILLISECONDS);
        request.setSysReadTimeout(AuthUtils.DEFAULT_HTTP_SOCKET_TIMEOUT_IN_MILLISECONDS);
        
        HttpResponse response = null;
        try {
            response = send(request);
        } catch (IOException e) {
            throw new ClientException("CredentialsFetcher.fetch exception: " + e);
        }
        
        return parse(response);
    }

    @Override
    public HttpResponse send(HttpRequest request) throws IOException {

        HttpResponse response = null;
        try {
            response = CompatibleUrlConnClient.compatibleGetResponse(request);
        } catch (ClientException e) {
            throw new IOException(e);
        }

        if (response.getStatus() != HttpURLConnection.HTTP_OK) {
            throw new IOException("HttpCode=" + response.getStatus());
        }
        return response;
    }

    @Override
    public Credentials fetch(int retryTimes) throws ClientException {
        for (int i = 0; i <= retryTimes; i++) {
            try {
                return fetch();
            } catch (ClientException e) {
                if (i == retryTimes) {
                    throw e;
                }
            }
        }
        throw new ClientException("Failed to connect ECS Metadata Service: Max retry times exceeded.");
    }

}
