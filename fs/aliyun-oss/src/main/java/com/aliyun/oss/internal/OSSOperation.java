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

package com.aliyun.oss.internal;

import static com.aliyun.oss.common.utils.CodingUtils.assertParameterNotNull;
import static com.aliyun.oss.common.utils.LogUtils.logException;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_CHARSET_NAME;
import static com.aliyun.oss.internal.OSSUtils.safeCloseResponse;

import java.net.URI;
import java.util.List;

import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.RequestSigner;
import com.aliyun.oss.common.comm.*;
import com.aliyun.oss.common.parser.ResponseParseException;
import com.aliyun.oss.common.parser.ResponseParser;
import com.aliyun.oss.common.utils.ExceptionFactory;
import com.aliyun.oss.internal.ResponseParsers.EmptyResponseParser;
import com.aliyun.oss.internal.ResponseParsers.RequestIdResponseParser;
import com.aliyun.oss.internal.signer.OSSSignerBase;
import com.aliyun.oss.internal.signer.OSSSignerParams;
import com.aliyun.oss.model.WebServiceRequest;

/**
 * Abstract base class that provides some common functionalities for OSS
 * operations (such as bucket/object/multipart/cors operations).
 */
public abstract class OSSOperation {
    protected String product;
    protected String region;
    protected volatile URI endpoint;
    protected CredentialsProvider credsProvider;
    protected ServiceClient client;
    protected String cloudBoxId;
    protected SignVersion signVersion;

    protected static OSSErrorResponseHandler errorResponseHandler = new OSSErrorResponseHandler();
    protected static EmptyResponseParser emptyResponseParser = new EmptyResponseParser();
    protected static RequestIdResponseParser requestIdResponseParser = new RequestIdResponseParser();
    protected static RetryStrategy noRetryStrategy = new NoRetryStrategy();

    protected OSSOperation(ServiceClient client, CredentialsProvider credsProvider) {
        this.client = client;
        this.credsProvider = credsProvider;
        this.product = OSSConstants.PRODUCT_DEFAULT;
        this.signVersion = null;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public URI getEndpoint() {
        return endpoint;
    }

    public URI getEndpoint(WebServiceRequest request) {
        String reqEndpoint = request.getEndpoint();
         if (reqEndpoint == null) {
             return getEndpoint();

        }
        String defaultProto = this.client.getClientConfiguration().getProtocol().toString();
        URI ret = OSSUtils.toEndpointURI(reqEndpoint, defaultProto);
        OSSUtils.ensureEndpointValid(ret.getHost());
         return ret;
    }

    public void setEndpoint(URI endpoint) {
        this.endpoint = URI.create(endpoint.toString());
    }

    public String getCloudBoxId() {
        return cloudBoxId;
    }

    public void setCloudBoxId(String cloudBoxId) {
        this.cloudBoxId = cloudBoxId;
    }

    public SignVersion getSignVersion() {
        return signVersion;
    }

    public void setSignVersion(SignVersion signVersion) {
        this.signVersion = signVersion;
    }

    protected ServiceClient getInnerClient() {
        return this.client;
    }

    protected ResponseMessage send(RequestMessage request, ExecutionContext context)
            throws OSSException, ClientException {
        return send(request, context, false);
    }

    protected ResponseMessage send(RequestMessage request, ExecutionContext context, boolean keepResponseOpen)
            throws OSSException, ClientException {
        ResponseMessage response = null;
        try {
            response = client.sendRequest(request, context);
            return response;
        } catch (ServiceException e) {
            assert (e instanceof OSSException);
            throw (OSSException) e;
        } finally {
            if (response != null && !keepResponseOpen) {
                safeCloseResponse(response);
            }
        }
    }

    protected <T> T doOperation(RequestMessage request, ResponseParser<T> parser, String bucketName, String key)
            throws OSSException, ClientException {
        return doOperation(request, parser, bucketName, key, false);
    }

    protected <T> T doOperation(RequestMessage request, ResponseParser<T> parser, String bucketName, String key,
            boolean keepResponseOpen) throws OSSException, ClientException {
        return doOperation(request, parser, bucketName, key, keepResponseOpen, null, null);
    }

    protected <T> T doOperation(RequestMessage request, ResponseParser<T> parser, String bucketName, String key,
            boolean keepResponseOpen, List<RequestHandler> requestHandlers, List<ResponseHandler> reponseHandlers)
            throws OSSException, ClientException {

        final WebServiceRequest originalRequest = request.getOriginalRequest();
        request.getHeaders().putAll(client.getClientConfiguration().getDefaultHeaders());
        request.getHeaders().putAll(originalRequest.getHeaders());
        request.getParameters().putAll(originalRequest.getParameters());

        ExecutionContext context = createDefaultContext(request.getMethod(), bucketName, key, originalRequest);

        context.addRequestHandler(new RequestProgressHanlder());
        if (requestHandlers != null) {
            for (RequestHandler handler : requestHandlers)
                context.addRequestHandler(handler);
        }
        if (client.getClientConfiguration().isCrcCheckEnabled()) {
            context.addRequestHandler(new RequestChecksumHanlder());
        }

        context.addResponseHandler(new ResponseProgressHandler(originalRequest));
        if (reponseHandlers != null) {
            for (ResponseHandler handler : reponseHandlers)
                context.addResponseHandler(handler);
        }
        if (client.getClientConfiguration().isCrcCheckEnabled()) {
            context.addResponseHandler(new ResponseChecksumHandler());
        }

        List<RequestSigner> signerHandlers = this.client.getClientConfiguration().getSignerHandlers();
        if (signerHandlers != null) {
            for (RequestSigner signer : signerHandlers) {
                context.addSignerHandler(signer);
            }
        }

        ResponseMessage response = send(request, context, keepResponseOpen);

        try {
            return parser.parse(response);
        } catch (ResponseParseException rpe) {
            OSSException oe = ExceptionFactory.createInvalidResponseException(response.getRequestId(), rpe.getMessage(),
                    rpe);
            logException("Unable to parse response error: ", rpe);
            throw oe;
        }
    }

    private RequestSigner createSigner(String bucketName, String key, Credentials creds, ClientConfiguration config) {
        String resourcePath = "/" + ((bucketName != null) ? bucketName + "/" : "") + ((key != null ? key : ""));

        OSSSignerParams params = new OSSSignerParams(resourcePath, creds);
        params.setProduct(product);
        params.setRegion(region);
        params.setCloudBoxId(cloudBoxId);
        params.setTickOffset(config.getTickOffset());
        return OSSSignerBase.createRequestSigner(signVersion != null ? signVersion : config.getSignatureVersion(), params);
    }

    protected ExecutionContext createDefaultContext(HttpMethod method, String bucketName, String key, WebServiceRequest originalRequest) {
        ExecutionContext context = new ExecutionContext();
		Credentials credentials = credsProvider.getCredentials();
        assertParameterNotNull(credentials, "credentials");
        context.setCharset(DEFAULT_CHARSET_NAME);
        context.setSigner(createSigner(bucketName, key, credentials, client.getClientConfiguration()));
        context.addResponseHandler(errorResponseHandler);
        if (method == HttpMethod.POST && !isRetryablePostRequest(originalRequest)) {
            context.setRetryStrategy(noRetryStrategy);
        }

        if (client.getClientConfiguration().getRetryStrategy() != null) {
            context.setRetryStrategy(client.getClientConfiguration().getRetryStrategy());
        }

        context.setCredentials(credentials);
        return context;
    }

    protected ExecutionContext createDefaultContext(HttpMethod method, String bucketName, String key) {
        return this.createDefaultContext(method, bucketName, key, null);
    }

    protected ExecutionContext createDefaultContext(HttpMethod method, String bucketName) {
        return this.createDefaultContext(method, bucketName, null, null);
    }

    protected ExecutionContext createDefaultContext(HttpMethod method) {
        return this.createDefaultContext(method, null, null, null);
    }

    protected boolean isRetryablePostRequest(WebServiceRequest request) {
        return false;
    }
}
