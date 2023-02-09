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

import com.qcloud.cos.event.ProgressListener;
import com.qcloud.cos.internal.CosServiceRequest;

public class CosHttpRequest<T extends CosServiceRequest> {

    /** The resource path being requested */
    private String resourcePath;


    private Map<String, String> parameters = new HashMap<String, String>();

    /** Map of the headers included in this request */
    private Map<String, String> headers = new HashMap<String, String>();
    // HTTP protocol
    private HttpProtocol protocol;
    /** The service endpoint to which this request should be sent */
    private String endpoint;

    /** The HTTP method to use when sending this request. */
    private HttpMethodName httpMethod = HttpMethodName.POST;

    /** An optional stream from which to read the request payload. */
    private InputStream content;

    private T originRequest;
    
    private ProgressListener progressListener;

    public CosHttpRequest(T originRequest) {
        this.originRequest = originRequest;
    }

    public void addHeader(String name, String value) {
        headers.put(name, value);
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void addParameter(String name, String value) {
        parameters.put(name, value);
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public CosHttpRequest<T> withParameter(String name, String value) {
        addParameter(name, value);
        return this;
    }
    
    public HttpProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(HttpProtocol protocol) {
        this.protocol = protocol;
    }

    public HttpMethodName getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(HttpMethodName httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers.clear();
        this.headers.putAll(headers);
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters.clear();
        this.parameters.putAll(parameters);
    }

    public InputStream getContent() {
        return content;
    }

    public void setContent(InputStream content) {
        this.content = content;
    }

    public T getOriginalRequest() {
        return originRequest;
    }
    
    public ProgressListener getProgressListener() {
        return progressListener;
    }

    public void setProgressListener(ProgressListener progressListener) {
        this.progressListener = progressListener;
    }

    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("endpoint: ").append(this.endpoint).append(", resourcepath: ")
                .append(this.resourcePath).append(", httpMethod: ").append(this.httpMethod);
        
        strBuilder.append(", headers { ");
        for (String headerKey : this.headers.keySet()) {
            String headerValue = this.headers.get(headerKey);
            strBuilder.append(headerKey).append(" : ").append(headerValue).append(", ");
        }
        strBuilder.append("}, params: { ");
        for (String paramKey : this.parameters.keySet()) {
            String paramValue = this.parameters.get(paramKey);
            strBuilder.append(paramKey).append(" : ").append(paramValue).append(", ");
        }
        strBuilder.append("}");
        return strBuilder.toString();
    }
}
