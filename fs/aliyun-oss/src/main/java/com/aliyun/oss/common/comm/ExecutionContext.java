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

import java.util.LinkedList;
import java.util.List;

import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.RequestSigner;
import com.aliyun.oss.internal.OSSConstants;

/**
 * HTTP request context.
 */
public class ExecutionContext {

    /* Request signer */
    private RequestSigner signer;

    /* The request handlers that handle request content in as a pipeline. */
    private List<RequestHandler> requestHandlers = new LinkedList<RequestHandler>();

    /* The response handlers that handle response message in as a pipeline. */
    private List<ResponseHandler> responseHandlers = new LinkedList<ResponseHandler>();

    /* The signer handlers that handle sign request in as a pipeline. */
    private List<RequestSigner> signerHandlers = new LinkedList<RequestSigner>();

    private String charset = OSSConstants.DEFAULT_CHARSET_NAME;

    /* Retry strategy when HTTP request fails. */
    private RetryStrategy retryStrategy;

    private Credentials credentials;

    public RetryStrategy getRetryStrategy() {
        return retryStrategy;
    }

    public void setRetryStrategy(RetryStrategy retryStrategy) {
        this.retryStrategy = retryStrategy;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String defaultEncoding) {
        this.charset = defaultEncoding;
    }

    public RequestSigner getSigner() {
        return signer;
    }

    public void setSigner(RequestSigner signer) {
        this.signer = signer;
    }

    public List<ResponseHandler> getResponseHandlers() {
        return responseHandlers;
    }

    public void addResponseHandler(ResponseHandler handler) {
        responseHandlers.add(handler);
    }

    public void insertResponseHandler(int position, ResponseHandler handler) {
        responseHandlers.add(position, handler);
    }

    public void removeResponseHandler(ResponseHandler handler) {
        responseHandlers.remove(handler);
    }

    public List<RequestHandler> getResquestHandlers() {
        return requestHandlers;
    }

    public void addRequestHandler(RequestHandler handler) {
        requestHandlers.add(handler);
    }

    public void insertRequestHandler(int position, RequestHandler handler) {
        requestHandlers.add(position, handler);
    }

    public void removeRequestHandler(RequestHandler handler) {
        requestHandlers.remove(handler);
    }

    public List<RequestSigner> getSignerHandlers() {
        return signerHandlers;
    }

    public void addSignerHandler(RequestSigner handler) {
        signerHandlers.add(handler);
    }

    public void insertSignerHandler(int position, RequestSigner handler) {
        signerHandlers.add(position, handler);
    }

    public void removeSignerHandler(RequestSigner handler) {
        signerHandlers.remove(handler);
    }

    public Credentials getCredentials() {
        return credentials;
    }

    public void setCredentials(Credentials credentials) {
        this.credentials = credentials;
    }

}
