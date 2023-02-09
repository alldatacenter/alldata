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

package com.aliyun.oss.common.comm;

import java.util.Map.Entry;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.common.comm.io.ChunkedInputStreamEntity;
import com.aliyun.oss.common.utils.HttpHeaders;

class HttpRequestFactory {

    public HttpRequestBase createHttpRequest(ServiceClient.Request request, ExecutionContext context) {

        String uri = request.getUri();

        HttpRequestBase httpRequest;
        HttpMethod method = request.getMethod();
        if (method == HttpMethod.POST) {
            HttpPost postMethod = new HttpPost(uri);

            if (request.getContent() != null) {
                postMethod.setEntity(new RepeatableInputStreamEntity(request));
            }

            httpRequest = postMethod;
        } else if (method == HttpMethod.PUT) {
            HttpPut putMethod = new HttpPut(uri);

            if (request.getContent() != null) {
                if (request.isUseChunkEncoding()) {
                    putMethod.setEntity(buildChunkedInputStreamEntity(request));
                } else {
                    putMethod.setEntity(new RepeatableInputStreamEntity(request));
                }
            }

            httpRequest = putMethod;
        } else if (method == HttpMethod.GET) {
            httpRequest = new HttpGet(uri);
        } else if (method == HttpMethod.DELETE) {
            if (request.getContent() != null) {
                HttpDeleteWithBody deleteMethod = new HttpDeleteWithBody(uri);
                deleteMethod.setEntity(new RepeatableInputStreamEntity(request));
                httpRequest = deleteMethod;
            } else {
                httpRequest = new HttpDelete(uri);
            }
        } else if (method == HttpMethod.HEAD) {
            httpRequest = new HttpHead(uri);
        } else if (method == HttpMethod.OPTIONS) {
            httpRequest = new HttpOptions(uri);
        } else {
            throw new ClientException("Unknown HTTP method name: " + method.toString());
        }

        configureRequestHeaders(request, context, httpRequest);

        return httpRequest;
    }

    private HttpEntity buildChunkedInputStreamEntity(ServiceClient.Request request) {
        return new ChunkedInputStreamEntity(request);
    }

    private void configureRequestHeaders(ServiceClient.Request request, ExecutionContext context,
            HttpRequestBase httpRequest) {

        for (Entry<String, String> entry : request.getHeaders().entrySet()) {
            if (entry.getKey().equalsIgnoreCase(HttpHeaders.CONTENT_LENGTH)
                    || entry.getKey().equalsIgnoreCase(HttpHeaders.HOST)) {
                continue;
            }

            httpRequest.addHeader(entry.getKey(), entry.getValue());
        }
    }
}
