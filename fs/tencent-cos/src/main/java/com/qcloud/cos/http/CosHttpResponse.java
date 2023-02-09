/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 
 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos.http;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.methods.HttpRequestBase;

public class CosHttpResponse {

    private final CosHttpRequest<?> request;

    private final HttpRequestBase httpRequest;

    private String statusText;
    private int statusCode;
    private InputStream content;
    private Map<String, String> headers = new HashMap<String, String>();

    public CosHttpResponse(CosHttpRequest<?> request, HttpRequestBase httpRequest) {
        super();
        this.request = request;
        this.httpRequest = httpRequest;
    }

    public String getStatusText() {
        return statusText;
    }

    public void setStatusText(String statusText) {
        this.statusText = statusText;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public InputStream getContent() {
        return content;
    }

    public void setContent(InputStream content) {
        this.content = content;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void addHeader(String name, String value) {
        this.headers.put(name, value);
    }

    public CosHttpRequest<?> getRequest() {
        return request;
    }

    public HttpRequestBase getHttpRequest() {
        return httpRequest;
    }

}
