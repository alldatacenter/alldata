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

import static com.aliyun.oss.common.utils.CodingUtils.assertParameterNotNull;
import static com.aliyun.oss.common.utils.LogUtils.getLog;
import static com.aliyun.oss.common.utils.LogUtils.logException;
import static com.aliyun.oss.internal.OSSUtils.COMMON_RESOURCE_MANAGER;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.http.HttpMessage;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.ClientException;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.ServiceException;
import com.aliyun.oss.common.auth.RequestSigner;
import com.aliyun.oss.common.utils.HttpUtil;
import com.aliyun.oss.common.utils.LogUtils;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.internal.OSSConstants;

/**
 * Abstract service client that provides interfaces to access OSS services.
 */
public abstract class ServiceClient {

    protected ClientConfiguration config;

    protected ServiceClient(ClientConfiguration config) {
        this.config = config;
    }

    public ClientConfiguration getClientConfiguration() {
        return this.config;
    }

    /**
     * Send HTTP request with specified context to OSS and wait for HTTP
     * response.
     *
     * @param request
     *            a request message.
     * @param context
     *            http request context.
     *
     * @return A {@link ResponseMessage} instance.
     *
     * @throws ClientException
     *             If any errors are encountered in the client while making the
     *             request or handling the response.
     * @throws ServiceException
     *             The base exception class to represent any expected or unexpected OSS
     *             server side errors.
     */
    public ResponseMessage sendRequest(RequestMessage request, ExecutionContext context)
            throws ServiceException, ClientException {

        assertParameterNotNull(request, "request");
        assertParameterNotNull(context, "context");

        try {
            return sendRequestImpl(request, context);
        } finally {
            // Close the request stream as well after the request is completed.
            try {
                request.close();
            } catch (IOException ex) {
                logException("Unexpected io exception when trying to close http request: ", ex);
                throw new ClientException("Unexpected io exception when trying to close http request: ", ex);
            }
        }
    }

    private ResponseMessage sendRequestImpl(RequestMessage request, ExecutionContext context)
            throws ClientException, ServiceException {

        RetryStrategy retryStrategy = context.getRetryStrategy() != null ? context.getRetryStrategy()
                : this.getDefaultRetryStrategy();

        // Sign the request if a signer provided.
        if (context.getSigner() != null && !request.isUseUrlSignature()) {
            context.getSigner().sign(request);
        }

        for (RequestSigner signer : context.getSignerHandlers()) {
            signer.sign(request);
        }

        InputStream requestContent = request.getContent();
        if (requestContent != null && requestContent.markSupported()) {
            requestContent.mark(OSSConstants.DEFAULT_STREAM_BUFFER_SIZE);
        }

        int retries = 0;
        ResponseMessage response = null;

        while (true) {
            try {
                if (retries > 0) {
                    pause(retries, retryStrategy);
                    if (requestContent != null && requestContent.markSupported()) {
                        try {
                            requestContent.reset();
                        } catch (IOException ex) {
                            logException("Failed to reset the request input stream: ", ex);
                            throw new ClientException("Failed to reset the request input stream: ", ex);
                        }
                    }
                }

                /*
                 * The key four steps to send HTTP requests and receive HTTP
                 * responses.
                 */

                // Step 1. Preprocess HTTP request.
                handleRequest(request, context.getResquestHandlers());

                // Step 2. Build HTTP request with specified request parameters
                // and context.
                Request httpRequest = buildRequest(request, context);

                // Step 3. Send HTTP request to OSS.
                String poolStatsInfo = config.isLogConnectionPoolStatsEnable()? "Connection pool stats " + getConnectionPoolStats():"";
                long startTime = System.currentTimeMillis();
                response = sendRequestCore(httpRequest, context);
                long duration = System.currentTimeMillis() - startTime;
                if (duration > config.getSlowRequestsThreshold()) {
                    LogUtils.getLog().warn(formatSlowRequestLog(request, response, duration) + poolStatsInfo);
                }

                // Step 4. Preprocess HTTP response.
                handleResponse(response, context.getResponseHandlers());

                return response;
            } catch (ServiceException sex) {
                logException("[Server]Unable to execute HTTP request: ", sex,
                        request.getOriginalRequest().isLogEnabled());

                // Notice that the response should not be closed in the
                // finally block because if the request is successful,
                // the response should be returned to the callers.
                closeResponseSilently(response);

                if (!shouldRetry(sex, request, response, retries, retryStrategy)) {
                    throw sex;
                }
            } catch (ClientException cex) {
                logException("[Client]Unable to execute HTTP request: ", cex,
                        request.getOriginalRequest().isLogEnabled());

                closeResponseSilently(response);

                if (!shouldRetry(cex, request, response, retries, retryStrategy)) {
                    throw cex;
                }
            } catch (Exception ex) {
                logException("[Unknown]Unable to execute HTTP request: ", ex,
                        request.getOriginalRequest().isLogEnabled());

                closeResponseSilently(response);

                throw new ClientException(
                        COMMON_RESOURCE_MANAGER.getFormattedString("ConnectionError", ex.getMessage()), ex);
            } finally {
                retries++;
            }
        }
    }

    /**
     * Implements the core logic to send requests to Aliyun OSS services.
     *
     * @param request
     *            a request message.
     * @param context
     *            http request context.
     *
     * @return A {@link ResponseMessage} instance.
     *
     * @throws IOException
     *             An IO errors are encountered in the client while making the
     *             request or handling the response.
     */
    protected abstract ResponseMessage sendRequestCore(Request request, ExecutionContext context) throws IOException;

    private Request buildRequest(RequestMessage requestMessage, ExecutionContext context) throws ClientException {

        Request request = new Request();
        request.setMethod(requestMessage.getMethod());
        request.setUseChunkEncoding(requestMessage.isUseChunkEncoding());

        if (requestMessage.isUseUrlSignature()) {
            request.setUrl(requestMessage.getAbsoluteUrl().toString());
            request.setUseUrlSignature(true);

            request.setContent(requestMessage.getContent());
            request.setContentLength(requestMessage.getContentLength());
            request.setHeaders(requestMessage.getHeaders());

            return request;
        }

        request.setHeaders(requestMessage.getHeaders());
        // The header must be converted after the request is signed,
        // otherwise the signature will be incorrect.
        if (request.getHeaders() != null) {
            HttpUtil.convertHeaderCharsetToIso88591(request.getHeaders());
        }

        final String delimiter = "/";
        String uri = requestMessage.getEndpoint().toString();
        if (!uri.endsWith(delimiter) && (requestMessage.getResourcePath() == null
                || !requestMessage.getResourcePath().startsWith(delimiter))) {
            uri += delimiter;
        }

        if (requestMessage.getResourcePath() != null) {
            uri += requestMessage.getResourcePath();
        }

        String paramString = HttpUtil.paramToQueryString(requestMessage.getParameters(), context.getCharset());

        /*
         * For all non-POST requests, and any POST requests that already have a
         * payload, we put the encoded params directly in the URI, otherwise,
         * we'll put them in the POST request's payload.
         */
        boolean requestHasNoPayload = requestMessage.getContent() != null;
        boolean requestIsPost = requestMessage.getMethod() == HttpMethod.POST;
        boolean putParamsInUri = !requestIsPost || requestHasNoPayload;
        if (paramString != null && putParamsInUri) {
            uri += "?" + paramString;
        }

        request.setUrl(uri);

        if (requestIsPost && requestMessage.getContent() == null && paramString != null) {
            // Put the param string to the request body if POSTing and
            // no content.
            try {
                byte[] buf = paramString.getBytes(context.getCharset());
                ByteArrayInputStream content = new ByteArrayInputStream(buf);
                request.setContent(content);
                request.setContentLength(buf.length);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(
                        COMMON_RESOURCE_MANAGER.getFormattedString("EncodingFailed", e.getMessage()));
            }
        } else {
            request.setContent(requestMessage.getContent());
            request.setContentLength(requestMessage.getContentLength());
        }

        return request;
    }

    private void handleResponse(ResponseMessage response, List<ResponseHandler> responseHandlers)
            throws ServiceException, ClientException {
        for (ResponseHandler h : responseHandlers) {
            h.handle(response);
        }
    }

    private void handleRequest(RequestMessage message, List<RequestHandler> resquestHandlers)
            throws ServiceException, ClientException {
        for (RequestHandler h : resquestHandlers) {
            h.handle(message);
        }
    }

    private void pause(int retries, RetryStrategy retryStrategy) throws ClientException {

        long delay = retryStrategy.getPauseDelay(retries);

        getLog().debug(
                "An retriable error request will be retried after " + delay + "(ms) with attempt times: " + retries);

        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            throw new ClientException(e.getMessage(), e);
        }
    }

    private boolean shouldRetry(Exception exception, RequestMessage request, ResponseMessage response, int retries,
            RetryStrategy retryStrategy) {

        if (retries >= config.getMaxErrorRetry()) {
            return false;
        }

        if (!request.isRepeatable()) {
            return false;
        }

        if (retryStrategy.shouldRetry(exception, request, response, retries)) {
            getLog().debug("Retrying on " + exception.getClass().getName() + ": " + exception.getMessage());
            return true;
        }
        return false;
    }

    private void closeResponseSilently(ResponseMessage response) {
        if (response != null) {
            try {
                response.close();
            } catch (IOException ioe) {
                /* silently close the response. */
            }
        }
    }

    private String formatSlowRequestLog(RequestMessage request, ResponseMessage response, long useTimesMs) {
        return String.format(
                "Request cost %d seconds, endpoint %s, resourcePath %s, " + "method %s, Date '%s', statusCode %d, requestId %s.",
                useTimesMs / 1000, request.getEndpoint(), request.getResourcePath(), request.getMethod(), request.getHeaders().get(HttpHeaders.DATE),
                response.getStatusCode(), response.getRequestId());
    }

    protected abstract RetryStrategy getDefaultRetryStrategy();

    public abstract void shutdown();

    public String getConnectionPoolStats() {
        return "";
    };

    /**
     * Wrapper class based on {@link HttpMessage} that represents HTTP request
     * message to OSS.
     */
    public static class Request extends HttpMesssage {
        private String uri;
        private HttpMethod method;
        private boolean useUrlSignature = false;
        private boolean useChunkEncoding = false;

        public String getUri() {
            return this.uri;
        }

        public void setUrl(String uri) {
            this.uri = uri;
        }

        public HttpMethod getMethod() {
            return method;
        }

        public void setMethod(HttpMethod method) {
            this.method = method;
        }

        public boolean isUseUrlSignature() {
            return useUrlSignature;
        }

        public void setUseUrlSignature(boolean useUrlSignature) {
            this.useUrlSignature = useUrlSignature;
        }

        public boolean isUseChunkEncoding() {
            return useChunkEncoding;
        }

        public void setUseChunkEncoding(boolean useChunkEncoding) {
            this.useChunkEncoding = useChunkEncoding;
        }
    }
}
