/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.internal;

import com.obs.log.ILogger;
import com.obs.log.InterfaceLogBean;
import com.obs.log.LoggerBuilder;
import com.obs.services.internal.Constants.CommonHeaders;
import com.obs.services.internal.ext.ExtObsConstraint;
import com.obs.services.internal.handler.XmlResponsesSaxParser;
import com.obs.services.internal.io.UnrecoverableIOException;
import com.obs.services.internal.security.BasicSecurityKey;
import com.obs.services.internal.security.ProviderCredentialThreadContext;
import com.obs.services.internal.security.ProviderCredentials;
import com.obs.services.internal.trans.NewTransResult;
import com.obs.services.internal.utils.IAuthentication;
import com.obs.services.internal.utils.JSONChange;
import com.obs.services.internal.utils.Mimetypes;
import com.obs.services.internal.utils.RestUtils;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.internal.utils.V4Authentication;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.HttpMethodEnum;
import okhttp3.Call;
import okhttp3.Headers;
import okhttp3.Request;
import okhttp3.Response;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public abstract class RestStorageService extends RestConnectionService {
    private static final ILogger log = LoggerBuilder.getLogger(RestStorageService.class);

    private static final Set<Class<? extends IOException>> NON_RETRIABLE_CLASSES =
            new HashSet<>();

    private static final String REQUEST_TIMEOUT_CODE = "RequestTimeout";

    // for example:Caused by: java.io.IOException: unexpected end of stream on
    // Connection{...}
    private static final String UNEXPECTED_END_OF_STREAM_EXCEPTION = "unexpected end of stream";

    static {
        NON_RETRIABLE_CLASSES.add(UnknownHostException.class);
        NON_RETRIABLE_CLASSES.add(SSLException.class);
        NON_RETRIABLE_CLASSES.add(ConnectException.class);
    }

    private static ThreadLocal<HashMap<String, String>> userHeaders = new ThreadLocal<>();

    // switch of using standard http headers
    protected static final ThreadLocal<Boolean> CAN_USE_STANDARD_HTTP_HEADERS = new ThreadLocal<>();

    protected RestStorageService() {

    }

    /**
     * set switch of using standard http headers
     *
     * @param canUseStandardHTTPHeadersMap A Boolean variable to control switch of using http standard
     *                                     headers
     */
    public void setCanUseStandardHTTPHeaders(Boolean canUseStandardHTTPHeadersMap) {
        CAN_USE_STANDARD_HTTP_HEADERS.set(canUseStandardHTTPHeadersMap);
    }

    /**
     * set user headers
     *
     * @param userHeadersMap A HashMap<String,String></String,String>
     */
    public void setUserHeaders(HashMap<String, String> userHeadersMap) {
        userHeaders.set(userHeadersMap);
    }

    /**
     * add user headers to Request
     *
     * @param builder Request in OKHttp3.0
     */
    private void addUserHeaderToRequest(String bucketName, Request.Builder builder, boolean needEncode) {
        Map<String, String> userHeaderMap = formatMetadataAndHeader(bucketName, userHeaders.get(), needEncode);
        for (Map.Entry<String, String> entry : userHeaderMap.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
            if (log.isDebugEnabled()) {
                log.debug("Added request header to connection: " + entry.getKey() + "=" + entry.getValue());
            }
        }
    }

    private Map<String, String> formatMetadataAndHeader(String bucketName, Map<String, String> metadataAndHeader,
                                                        boolean needEncode) {
        Map<String, String> format = new HashMap<>();
        if (metadataAndHeader != null) {
            for (Map.Entry<String, String> entry : metadataAndHeader.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!ServiceUtils.isValid(key)) {
                    continue;
                }
                key = key.trim();
                if (!key.startsWith(this.getRestHeaderPrefix(bucketName))
                        && !key.startsWith(Constants.OBS_HEADER_PREFIX)
                        && !Constants.ALLOWED_REQUEST_HTTP_HEADER_METADATA_NAMES
                        .contains(key.toLowerCase(Locale.getDefault()))) {
                    key = this.getRestMetadataPrefix(bucketName) + key;
                }
                try {
                    if (key.startsWith(this.getRestMetadataPrefix(bucketName))) {
                        if (needEncode) {
                            key = RestUtils.uriEncode(key, true);
                        }
                    }
                    if (needEncode) {
                        format.put(key, RestUtils.uriEncode(value == null ? "" : value, true));
                    } else {
                        format.put(key, value);
                    }
                } catch (ServiceException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Ignore key:" + key);
                    }
                }
            }
        }

        return format;
    }


    protected boolean retryRequest(IOException exception, RetryCounter retryCounter, Request request,
                                   Call call) {
        retryCounter.addErrorCount();

        if (retryCounter.getErrorCount() > retryCounter.getRetryMaxCount()) {
            return false;
        }
        if (NON_RETRIABLE_CLASSES.contains(exception.getClass())) {
            return false;
        } else {
            for (final Class<? extends IOException> rejectException : NON_RETRIABLE_CLASSES) {
                if (rejectException.isInstance(exception)) {
                    return false;
                }
            }
        }

        return !call.isCanceled();
    }

    private boolean retryRequestForUnexpectedException(IOException exception, RetryCounter retryCounter,
                                                       Call call) {
        retryCounter.addErrorCount();

        if (null == exception || retryCounter.getErrorCount() > retryCounter.getRetryMaxCount()) {
            return false;
        }

        if (!exception.getMessage().contains(UNEXPECTED_END_OF_STREAM_EXCEPTION)) {
            return false;
        }

        return !call.isCanceled();
    }

    private ServiceException handleThrowable(String bucketName, Request request, Response response, Call call,
                                             Throwable t, boolean needEncode) {
        ServiceException serviceException = (t instanceof ServiceException) ? (ServiceException) t
                : new ServiceException("Request Error: " + t, t);
        serviceException.setRequestHost(request.header(CommonHeaders.HOST));
        serviceException.setRequestVerb(request.method());
        serviceException.setRequestPath(request.url().toString());

        if (response != null) {
            ServiceUtils.closeStream(response);
            serviceException.setResponseCode(response.code());
            serviceException.setResponseStatus(response.message());
            serviceException.setResponseDate(response.header(CommonHeaders.DATE));
            serviceException.setErrorIndicator(response.header(CommonHeaders.X_RESERVED_INDICATOR));
            serviceException.setResponseHeaders(ServiceUtils.cleanRestMetadataMapV2(
                    convertHeadersToMap(response.headers()), getRestHeaderPrefix(bucketName),
                    getRestMetadataPrefix(bucketName), needEncode));
            if (!ServiceUtils.isValid(serviceException.getErrorRequestId())) {
                serviceException.setRequestAndHostIds(response.header(getIHeaders(bucketName).requestIdHeader()),
                        response.header(getIHeaders(bucketName).requestId2Header()));
            }
        }

        if (log.isWarnEnabled()) {
            log.warn("Request failed, Response code: " + serviceException.getResponseCode()
                    + "; Request ID: " + serviceException.getErrorRequestId()
                    + "; Request path: " + serviceException.getRequestPath());
        }
        if (log.isDebugEnabled()) {
            log.debug("Exception detail.", serviceException);
        }

        if (call != null) {
            call.cancel();
        }
        return serviceException;
    }

    private boolean isLocationHostOnly(String location) {
        boolean isOnlyHost = false;

        URI uri;
        uri = URI.create(location);
        String path = uri.getPath();

        if (!location.contains("?")) {
            if (path == null || path.isEmpty() || path.equals("/")) {
                isOnlyHost = true;
            }
        }

        return isOnlyHost;
    }

    protected Response performRequest(Request request, Map<String, String> requestParameters, String bucketName)
            throws ServiceException {
        return performRequest(request, requestParameters, bucketName, true);
    }

    protected Response performRequestWithoutSignature(Request request, Map<String, String> requestParameters,
                                                      String bucketName) throws ServiceException {
        return performRequest(request, requestParameters, bucketName, false);
    }

    protected Response performRequest(Request request, Map<String, String> requestParameters, String bucketName,
                                      boolean doSignature) throws ServiceException {
        return this.performRequest(request, requestParameters, bucketName, doSignature, false);
    }

    private static final class RetryCounter {
        private int errorCount = 0;
        private int retryMaxCount;

        public RetryCounter(int retryMaxCount) {
            super();
            this.retryMaxCount = retryMaxCount;
        }

        public int getErrorCount() {
            return errorCount;
        }

        public void addErrorCount() {
            this.errorCount++;
        }

        public int getRetryMaxCount() {
            return retryMaxCount;
        }
    }

    private static final class RetryController {
        private RetryCounter errorRetryCounter;
        private RetryCounter unexpectedErrorRetryCounter;
        private Exception lastException = null;
        private boolean wasRecentlyRedirected;

        public RetryController(RetryCounter errorRetryCounter, RetryCounter unexpectedErrorRetryCounter,
                               boolean wasRecentlyRedirected) {
            super();
            this.errorRetryCounter = errorRetryCounter;
            this.unexpectedErrorRetryCounter = unexpectedErrorRetryCounter;
            this.wasRecentlyRedirected = wasRecentlyRedirected;
        }

        public Exception getLastException() {
            return lastException;
        }

        public void setLastException(Exception lastException) {
            this.lastException = lastException;
        }

        public RetryCounter getErrorRetryCounter() {
            return errorRetryCounter;
        }

        public RetryCounter getUnexpectedErrorRetryCounter() {
            return unexpectedErrorRetryCounter;
        }

        public boolean isWasRecentlyRedirected() {
            return wasRecentlyRedirected;
        }

        public void setWasRecentlyRedirected(boolean wasRecentlyRedirected) {
            this.wasRecentlyRedirected = wasRecentlyRedirected;
        }
    }

    private static final class RequestInfo {
        private Request request;
        private Response response = null;
        private Call call = null;
        private InterfaceLogBean reqBean;

        public RequestInfo(Request request, InterfaceLogBean reqBean) {
            super();
            this.request = request;
            this.reqBean = reqBean;
        }

        public Request getRequest() {
            return request;
        }

        public void setRequest(Request request) {
            this.request = request;
        }

        public Response getResponse() {
            return response;
        }

        public void setResponse(Response response) {
            this.response = response;
        }

        public Call getCall() {
            return call;
        }

        public void setCall(Call call) {
            this.call = call;
        }

        public InterfaceLogBean getReqBean() {
            return reqBean;
        }
    }

    protected Response performRequest(Request request, Map<String, String> requestParameters, String bucketName,
                                      boolean doSignature, boolean isOEF) throws ServiceException {
        return performRequest(request, requestParameters, bucketName, doSignature, isOEF, true);
    }

    protected Response performRequest(Request request, Map<String, String> requestParameters, String bucketName,
                                      boolean doSignature, boolean isOEF, boolean needEncode) throws ServiceException {
        RequestInfo requestInfo = new RequestInfo(request, new InterfaceLogBean("performRequest", "", ""));
        try {
            tryRequest(requestParameters, bucketName, doSignature, isOEF, requestInfo, needEncode);
        } catch (Throwable t) {
            throw this.handleThrowable(bucketName, requestInfo.getRequest(),
                    requestInfo.getResponse(), requestInfo.getCall(), t, needEncode);
        }

        if (log.isInfoEnabled()) {
            requestInfo.getReqBean().setRespTime(new Date());
            requestInfo.getReqBean().setResultCode(Constants.RESULTCODE_SUCCESS);
            log.info(requestInfo.getReqBean());
        }
        return requestInfo.getResponse();
    }

    protected Response performRequest(NewTransResult result) {
        return performRequest(result, true, true, false, false);
    }

    // todo 重构后仅保留这一个 performRequest，将其他的均合并至这里
    // todo 重构时需要评估下 needSignature, autoRelease, isOEF 这三个参数是否要合入到 transResult 里
    protected Response performRequest(NewTransResult result, boolean needSignature, boolean autoRelease, boolean isOEF,
                                      boolean isNotNeedBucket) {
        Request.Builder builder = setupConnection(result, isOEF, isNotNeedBucket);
        renameMetadataKeys(result.getBucketName(), builder, result.getHeaders(), result.isEncodeHeaders());
        if (result.getUserHeaders() != null) {
            result.getUserHeaders().forEach(builder::addHeader);
        }
        Request request = builder.build();
        RequestInfo requestInfo = new RequestInfo(request, new InterfaceLogBean("performRequest", "", ""));
        try {
            tryRequest(result.getParams(), result.getBucketName(), needSignature,
                    isOEF, requestInfo, result.isEncodeHeaders());
        } catch (Throwable t) {
            throw this.handleThrowable(result.getBucketName(), requestInfo.getRequest(),
                    requestInfo.getResponse(), requestInfo.getCall(), t, result.isEncodeHeaders());
        }

        if (log.isInfoEnabled()) {
            requestInfo.getReqBean().setRespTime(new Date());
            requestInfo.getReqBean().setResultCode(Constants.RESULTCODE_SUCCESS);
            log.info(requestInfo.getReqBean());
        }
        Response response = requestInfo.getResponse();
        if (autoRelease) {
            response.close();
        }
        return response;
    }

    private void tryRequest(Map<String, String> requestParameters, String bucketName, boolean doSignature,
                            boolean isOEF, RequestInfo requestInfo, boolean needEncode) throws Exception {
        requestInfo.setRequest(initRequest(bucketName, requestInfo.getRequest(), needEncode));

        log.debug("Performing " + requestInfo.getRequest().method()
                + " request for '" + requestInfo.getRequest().url());
        log.debug("Headers: " + requestInfo.getRequest().headers());

        RetryController retryController = new RetryController(new RetryCounter(
                obsProperties.getIntProperty(ObsConstraint.HTTP_RETRY_MAX,
                        ObsConstraint.HTTP_RETRY_MAX_VALUE)),
                new RetryCounter(obsProperties.getIntProperty(
                        ExtObsConstraint.HTTP_MAX_RETRY_ON_UNEXPECTED_END_EXCEPTION,
                        ExtObsConstraint.DEFAULT_MAX_RETRY_ON_UNEXPECTED_END_EXCEPTION)),
                false);

        do {
            if (!retryController.isWasRecentlyRedirected()) {
                requestInfo.setRequest(addBaseHeaders(requestInfo.getRequest(), bucketName, doSignature));
            } else {
                retryController.setWasRecentlyRedirected(false);
            }

            requestInfo.setCall(httpClient.newCall(requestInfo.getRequest()));
            requestInfo.setResponse(executeRequest(requestInfo.getCall(),
                    requestInfo.getRequest(), retryController));
            if (null == requestInfo.getResponse()) {
                continue;
            }

            int responseCode = requestInfo.getResponse().code();
            requestInfo.getReqBean().setRespParams("[responseCode: " + responseCode + "][request-id: "
                    + requestInfo.getResponse().header(this.getIHeaders(bucketName).requestIdHeader(), "") + "]");

            String contentType = requestInfo.getResponse().header(CommonHeaders.CONTENT_TYPE);
            if (log.isDebugEnabled()) {
                log.debug("Response for '" + requestInfo.getRequest().method() + "'. Content-Type: " + contentType
                        + ", ResponseCode:" + responseCode
                        + ", Headers: " + requestInfo.getResponse().headers());
            }
            if (log.isTraceEnabled()) {
                if (requestInfo.getResponse().body() != null) {
                    log.trace("Entity length: " + requestInfo.getResponse().body().contentLength());
                }
            }

            if (isOEF && Mimetypes.MIMETYPE_JSON.equalsIgnoreCase(contentType)) {
                transOEFResponse(requestInfo.getResponse(), requestInfo.getReqBean(),
                        retryController.getErrorRetryCounter().getErrorCount(), responseCode);
                break;
            } else {
                if (responseCode >= 300 && responseCode < 400 && responseCode != 304) {
                    requestInfo.setRequest(handleRedirectResponse(requestInfo.getRequest(),
                            requestParameters, bucketName, doSignature, isOEF,
                            requestInfo.getReqBean(), requestInfo.getResponse(), retryController));
                } else if ((responseCode >= 400 && responseCode < 500) || responseCode == 304) {
                    handleRequestErrorResponse(requestInfo.getResponse(), retryController);
                } else if (responseCode >= 500) {
                    handleServerErrorResponse(requestInfo.getReqBean(),
                            requestInfo.getResponse(), retryController, responseCode);
                } else {
                    break;
                }
            }

        } while (true);
    }

    private void handleRequestErrorResponse(Response response, RetryController retryController) {
        ServiceException exception = createServiceException("Request Error.", response);

        if (!REQUEST_TIMEOUT_CODE.equals(exception.getErrorCode())) {
            throw exception;
        }

        retryController.getErrorRetryCounter().addErrorCount();
        if (retryController.getErrorRetryCounter().getErrorCount()
                < retryController.getErrorRetryCounter().getRetryMaxCount()) {
            if (log.isWarnEnabled()) {
                log.warn("Retrying connection that failed with RequestTimeout error"
                        + ", attempt number " + retryController.getErrorRetryCounter().getErrorCount()
                        + " of " + retryController.getErrorRetryCounter().getRetryMaxCount());
            }
        } else {
            if (log.isErrorEnabled()) {
                log.error("Exceeded maximum number of retries for RequestTimeout errors: "
                        + retryController.getErrorRetryCounter().getRetryMaxCount());
            }

            throw exception;
        }
    }

    private void handleServerErrorResponse(InterfaceLogBean reqBean, Response response, RetryController retryController,
                                           int responseCode) {
        reqBean.setResponseInfo("Internal Server error(s).", String.valueOf(responseCode));
        if (log.isErrorEnabled()) {
            log.error(reqBean);
        }

        doRetry(response,
                "Encountered too many 5xx errors ("
                        + retryController.getErrorRetryCounter().getErrorCount()
                        + "), aborting request.",
                retryController.getErrorRetryCounter());

        sleepBeforeRetry(retryController.getErrorRetryCounter().getErrorCount());
    }

    private Request handleRedirectResponse(Request request, Map<String, String> requestParameters, String bucketName,
                                           boolean doSignature, boolean isOEF, InterfaceLogBean reqBean,
                                           Response response, RetryController retryController) {
        int responseCode = response.code();
        String location = response.header(CommonHeaders.LOCATION);
        if (!ServiceUtils.isValid(location)) {
            ServiceException exception = new ServiceException("Try to redirect, but location is null!");
            reqBean.setResponseInfo("Request Error:" + exception.getMessage(),
                    "|" + responseCode + "|" + response.message() + "|");
            throw exception;
        }

        request = createRedirectRequest(request, requestParameters, bucketName, doSignature, isOEF,
                responseCode, location);

        retryController.setWasRecentlyRedirected(true);

        doRetry(response,
                "Exceeded 3xx redirect limit ("
                        + retryController.getErrorRetryCounter().getRetryMaxCount()
                        + ").",
                retryController.getErrorRetryCounter());
        return request;
    }

    private Response executeRequest(Call call,
                                    Request request,
                                    RetryController retryController) throws Exception {
        long start = System.currentTimeMillis();

        try {
            semaphore.acquire();
            return call.execute();
        } catch (IOException e) {
            if (e instanceof UnrecoverableIOException) {
                if (retryController.getLastException() != null) {
                    throw retryController.getLastException();
                } else {
                    throw e;
                }
            }
            retryController.setLastException(e);

            retryOnIOException(e,
                    request,
                    retryController,
                    call);

            if (log.isWarnEnabled()) {
                log.warn("Retrying connection that failed with error"
                        + ", attempt number " + retryController.getErrorRetryCounter().getErrorCount()
                        + " of " + retryController.getErrorRetryCounter().getRetryMaxCount());
            }
            return null;
        } finally {
            semaphore.release();
            if (log.isInfoEnabled()) {
                log.info("OkHttp cost " + (System.currentTimeMillis() - start) + " ms to apply http request");
            }
        }
    }

    private void retryOnIOException(IOException e,
                                    Request request,
                                    RetryController retryController,
                                    Call call) throws Exception {

        // for example:Caused by: java.io.IOException: unexpected
        // end of stream on Connection{...}
        if (retryRequestForUnexpectedException(e, retryController.getUnexpectedErrorRetryCounter(), call)) {
            if (log.isErrorEnabled()) {
                log.error("unexpected end of stream excepiton.");
            }
            return;
        }

        if (retryRequest(e, retryController.getErrorRetryCounter(), request, call)) {
            sleepBeforeRetry(retryController.getErrorRetryCounter().getErrorCount());
            return;
        }

        if ((e instanceof ConnectException) || (e instanceof InterruptedIOException)) {
            ServiceException se = new ServiceException("Request error. ", e);
            se.setResponseCode(408);
            se.setErrorCode("RequestTimeOut");
            se.setErrorMessage(e.getMessage());
            se.setResponseStatus("Request error. ");
            throw se;
        }
        throw e;
    }

    private void doRetry(Response response, String message, RetryCounter retryCounter) {
        retryCounter.addErrorCount();
        if (retryCounter.getErrorCount() > retryCounter.getRetryMaxCount()) {
            throw createServiceException(message, response);
        }
        ServiceUtils.closeStream(response);
    }

    private ServiceException createServiceException(String message, Response response) {
        String xmlMessage = readResponseMessage(response);
        return new ServiceException(message, xmlMessage);
    }

    private String readResponseMessage(Response response) {
        String xmlMessage = null;
        try {
            if (response.body() != null) {
                xmlMessage = response.body().string();
            }
        } catch (IOException e) {
            log.warn("read response body failed.", e);
        }
        return xmlMessage;
    }

    private Request createRedirectRequest(Request request, Map<String, String> requestParameters, String bucketName,
                                          boolean doSignature, boolean isOEF, int responseCode, String location) {
        if (!location.contains("?")) {
            location = addRequestParametersToUrlPath(location, requestParameters, isOEF);
        }

        if (doSignature && isLocationHostOnly(location)) {
            request = authorizeHttpRequest(request, bucketName, location);
        } else {
            Request.Builder builder = request.newBuilder();

            if (responseCode == 302
                    && HttpMethodEnum.GET.getOperationType().equalsIgnoreCase(request.method())) {
                Headers headers = request.headers().newBuilder().removeAll(CommonHeaders.AUTHORIZATION)
                        .build();
                builder.headers(headers);
            }

            this.setHost(builder, request, location);

            request = builder.build();
        }
        return request;
    }

    private void transOEFResponse(Response response, InterfaceLogBean reqBean,
                                  int internalErrorCount, int responseCode) {
        if ((responseCode >= 400 && responseCode < 500)) {
            String xmlMessage = readResponseMessage(response);

            OefExceptionMessage oefException = (OefExceptionMessage) JSONChange
                    .jsonToObj(new OefExceptionMessage(), ServiceUtils.toValid(xmlMessage));
            ServiceException exception = new ServiceException(
                    "Request Error." + ServiceUtils.toValid(xmlMessage));
            exception.setErrorMessage(oefException.getMessage());
            exception.setErrorCode(oefException.getCode());
            exception.setErrorRequestId(oefException.getRequestId());

            throw exception;
        } else if (responseCode >= 500) {
            reqBean.setResponseInfo("Internal Server error(s).", String.valueOf(responseCode));
            if (log.isErrorEnabled()) {
                log.error(reqBean);
            }
            throw createServiceException("Encountered too many 5xx errors (" + internalErrorCount
                            + "), aborting request.",
                    response);
        }
    }

    private Request addBaseHeaders(Request request, String bucketName, boolean doSignature) {
        if (doSignature) {
            request = authorizeHttpRequest(request, bucketName, null);
        } else {
            Request.Builder builder = request.newBuilder();
            builder.headers(request.headers().newBuilder().removeAll(CommonHeaders.AUTHORIZATION).build());
            this.setHost(builder, request, null);
            builder.header(CommonHeaders.USER_AGENT, Constants.USER_AGENT_VALUE);
            request = builder.build();
        }
        log.debug("Request Headers: " + request.headers().toString().replace("\n", "|"));
        return request;
    }

    private Request initRequest(String bucketName, Request request, boolean needEncode) {
        if (userHeaders.get() != null && userHeaders.get().size() > 0) {
            // create a new Builder from current Request
            Request.Builder builderTmp = request.newBuilder();
            // add user header
            addUserHeaderToRequest(bucketName, builderTmp, needEncode);
            // build new request
            request = builderTmp.build();
        }
        return request;
    }

    protected String getRestMetadataPrefix(String bucketName) {
        return this.getIHeaders(bucketName).headerMetaPrefix();
    }

    protected String getRestHeaderPrefix(String bucketName) {
        return this.getIHeaders(bucketName).headerPrefix();
    }

    private boolean isProviderCredentialsInValid(ProviderCredentials providerCredentials) {
        return providerCredentials == null || providerCredentials.getObsCredentialsProvider().getSecurityKey() == null
                || !ServiceUtils.isValid(providerCredentials.getSecurityKey().getAccessKey())
                || !ServiceUtils.isValid(providerCredentials.getSecurityKey().getSecretKey());
    }

    private URI setHost(Request.Builder builder, Request request, String url) {
        URI uri;
        if (url == null) {
            uri = request.url().uri();
        } else {
            uri = URI.create(url);
            builder.url(url);
        }

        String portStr;
        if (getHttpsOnly()) {
            int securePort = this.getHttpsPort();
            portStr = securePort == 443 ? "" : ":" + securePort;
        } else {
            int insecurePort = this.getHttpPort();
            portStr = insecurePort == 80 ? "" : ":" + insecurePort;
        }

        builder.header(CommonHeaders.HOST, uri.getHost() + portStr);
        return uri;
    }

    private Date parseDate(String bucketName, Request request, boolean isV4) {
        String dateHeader = this.getIHeaders(bucketName).dateHeader();
        String date = request.header(dateHeader);

        if (date != null) {
            try {
                return isV4 ? ServiceUtils.getLongDateFormat().parse(date) : ServiceUtils.parseRfc822Date(date);
            } catch (ParseException e) {
                throw new ServiceException(dateHeader + " is not well-format", e);
            }
        } else {
            return new Date();
        }
    }

    protected Request authorizeHttpRequest(Request request, String bucketName, String url) throws ServiceException {

        Headers headers = request.headers().newBuilder().removeAll(CommonHeaders.AUTHORIZATION).build();
        Request.Builder builder = request.newBuilder();
        builder.headers(headers);

        URI uri = this.setHost(builder, request, url);
        String hostname = uri.getHost();

        ProviderCredentials providerCredentials = ProviderCredentialThreadContext.getInstance()
                .getProviderCredentials();
        if (isProviderCredentialsInValid(providerCredentials)) {
            providerCredentials = this.getProviderCredentials();
        } else {
            providerCredentials.setAuthType(this.getProviderCredentials().getLocalAuthType(bucketName));
        }
        if (isProviderCredentialsInValid(providerCredentials)) {
            if (log.isInfoEnabled()) {
                log.info("Service has no Credential and is un-authenticated, skipping authorization");
            }
            return request;
        }

        boolean isV4 = providerCredentials.getLocalAuthType(bucketName) == AuthTypeEnum.V4;

        Date now = parseDate(bucketName, request, isV4);

        builder.header(CommonHeaders.DATE, ServiceUtils.formatRfc822Date(now));

        BasicSecurityKey securityKey = providerCredentials.getSecurityKey();
        String securityToken = securityKey.getSecurityToken();
        if (ServiceUtils.isValid(securityToken)) {
            builder.header(this.getIHeaders(bucketName).securityTokenHeader(), securityToken);
        }

        String fullUrl = uri.getRawPath();
        String endpoint = this.getEndpoint();

        if ((!this.isPathStyle() || isCname()) && hostname != null && !isV4) {
            if (isCname()) {
                fullUrl = "/" + hostname + fullUrl;
            } else if (ServiceUtils.isValid(bucketName) && !endpoint.equals(hostname)
                    && hostname.contains(bucketName)) {
                fullUrl = "/" + bucketName + fullUrl;
            }
        }

        String queryString = uri.getRawQuery();
        if (queryString != null && queryString.length() > 0) {
            fullUrl += "?" + queryString;
        }

        if (log.isDebugEnabled()) {
            log.debug("For creating canonical string, using uri: " + fullUrl);
        }

        IAuthentication iauthentication;
        if (isV4) {
            builder.header(this.getIHeaders(bucketName).contentSha256Header(), V4Authentication.CONTENT_SHA256);
            iauthentication = V4Authentication.makeServiceCanonicalString(request.method(),
                    convertHeadersToMap(builder.build().headers()), fullUrl, providerCredentials, now, securityKey);
            if (log.isDebugEnabled()) {
                log.debug("CanonicalRequest:" + iauthentication.getCanonicalRequest());
            }
        } else {
            iauthentication = Constants.AUTHTICATION_MAP.get(providerCredentials.getLocalAuthType(bucketName))
                    .makeAuthorizationString(request.method(), convertHeadersToMap(builder.build().headers()), fullUrl,
                            Constants.ALLOWED_RESOURCE_PARAMTER_NAMES, securityKey);
        }
        log.debug("StringToSign ('|' is a newline): "
                + iauthentication.getStringToSign().replace('\n', '|'));

        String authorizationString = iauthentication.getAuthorization();
        builder.header(CommonHeaders.AUTHORIZATION, authorizationString);
        builder.header(CommonHeaders.USER_AGENT, Constants.USER_AGENT_VALUE);
        return builder.build();
    }

    protected Response performRestHead(String bucketName, String objectKey, Map<String, String> requestParameters,
                                       Map<String, String> requestHeaders, Map<String, String> userHeaders,
                                       boolean needEncode) throws ServiceException {
        NewTransResult transResult = new NewTransResult();
        transResult.setHttpMethod(HttpMethodEnum.HEAD);
        transResult.setBucketName(bucketName);
        transResult.setObjectKey(objectKey);
        transResult.setParams(requestParameters);
        Request.Builder builder = setupConnection(transResult, false, false);
        addRequestHeadersToConnection(bucketName, builder, requestHeaders);
        if (userHeaders != null) {
            userHeaders.forEach(builder::addHeader);
        }
        return performRequest(builder.build(), requestParameters, bucketName, true, false, needEncode);
    }

    protected Response performRestGet(String bucketName, String objectKey, Map<String, String> requestParameters,
                                      Map<String, String> requestHeaders, Map<String, String> userHeaders)
            throws ServiceException {
        return this.performRestGet(bucketName, objectKey, requestParameters, requestHeaders, userHeaders, false);
    }

    protected Response performRestGetForListBuckets(String bucketName, String objectKey,
                                                    Map<String, String> requestParameters,
                                                    Map<String, String> requestHeaders) throws ServiceException {

        // no bucket name required for listBuckets
        NewTransResult transResult = new NewTransResult();
        transResult.setHttpMethod(HttpMethodEnum.GET);
        transResult.setBucketName(bucketName);
        transResult.setObjectKey(objectKey);
        transResult.setParams(requestParameters);
        Request.Builder builder = setupConnection(transResult, false, true);

        addRequestHeadersToConnection(bucketName, builder, requestHeaders);
        return performRequest(builder.build(), requestParameters, bucketName, true, false);
    }

    protected Response performRestGet(String bucketName, String objectKey, Map<String, String> requestParameters,
                                      Map<String, String> requestHeaders, Map<String, String> userHeaders,
                                      boolean isOEF) throws ServiceException {

        return performRestGet(bucketName, objectKey, requestParameters, requestHeaders, userHeaders, isOEF, true);
    }

    protected Response performRestGet(String bucketName, String objectKey, Map<String, String> requestParameters,
                                      Map<String, String> requestHeaders, Map<String, String> userHeaders,
                                      boolean isOEF, boolean needEncode) throws ServiceException {
        NewTransResult transResult = new NewTransResult();
        transResult.setHttpMethod(HttpMethodEnum.GET);
        transResult.setBucketName(bucketName);
        transResult.setObjectKey(objectKey);
        transResult.setParams(requestParameters);
        Request.Builder builder = setupConnection(transResult, isOEF, false);

        addRequestHeadersToConnection(bucketName, builder, requestHeaders);
        if (userHeaders != null) {
            userHeaders.forEach(builder::addHeader);
        }
        return performRequest(builder.build(), requestParameters, bucketName, true, isOEF, needEncode);
    }

    protected Response performRestDelete(String bucketName, String objectKey, Map<String, String> requestParameters,
                                         Map<String, String> metadata, Map<String, String> userHeaders)
            throws ServiceException {
        return this.performRestDelete(bucketName, objectKey, requestParameters, metadata, userHeaders, true, false);
    }

    protected Response performRestDelete(String bucketName, String objectKey, Map<String, String> requestParameters,
                                         Map<String, String> metadata, Map<String, String> userHeaders,
                                         boolean autoRelease, boolean isOEF)
            throws ServiceException {
        NewTransResult transResult = new NewTransResult();
        transResult.setHttpMethod(HttpMethodEnum.DELETE);
        transResult.setBucketName(bucketName);
        transResult.setObjectKey(objectKey);
        transResult.setParams(requestParameters);
        Request.Builder builder = setupConnection(transResult, isOEF, false);

        renameMetadataKeys(bucketName, builder, metadata);
        if (userHeaders != null) {
            userHeaders.forEach(builder::addHeader);
        }
        Response result = performRequest(builder.build(), requestParameters, bucketName, true, isOEF);

        if (autoRelease) {
            result.close();
        }

        return result;
    }

    protected Response performRestDelete(String bucketName, String objectKey, Map<String, String> requestParameters,
                                         Map<String, String> userHeaders, boolean autoRelease)
            throws ServiceException {
        return performRestDelete(bucketName, objectKey, requestParameters, new HashMap<>(),
                userHeaders, autoRelease, false);
    }

    protected Response performRestOptions(String bucketName, String objectKey, Map<String, String> metadata,
                                          Map<String, String> requestParameters, boolean autoRelease)
            throws ServiceException {
        NewTransResult transResult = new NewTransResult();
        transResult.setHttpMethod(HttpMethodEnum.OPTIONS);
        transResult.setBucketName(bucketName);
        transResult.setObjectKey(objectKey);
        transResult.setParams(requestParameters);
        Request.Builder builder = setupConnection(transResult, false, false);

        addRequestHeadersToConnection(bucketName, builder, metadata);

        Response result = performRequest(builder.build(), requestParameters, bucketName);

        if (autoRelease) {
            result.close();
        }
        return result;
    }

    protected Response performRestForApiVersion(String bucketName, String objectKey,
                                                Map<String, String> requestParameters,
                                                Map<String, String> requestHeaders) throws ServiceException {
        boolean isListBuckets = true;
        if (null != bucketName && !"".equals(bucketName.trim())) {
            isListBuckets = false;
        }
        NewTransResult transResult = new NewTransResult();
        transResult.setHttpMethod(HttpMethodEnum.HEAD);
        transResult.setBucketName(bucketName);
        transResult.setObjectKey(objectKey);
        transResult.setParams(requestParameters);
        Request.Builder builder = setupConnection(transResult, false, isListBuckets);

        addRequestHeadersToConnection(bucketName, builder, requestHeaders);

        return performRequestWithoutSignature(builder.build(), requestParameters, bucketName);
    }

    private void sleepBeforeRetry(int internalErrorCount) {
        long delayMs = 50L * (int) Math.pow(2, internalErrorCount);
        if (log.isWarnEnabled()) {
            log.warn("Encountered " + internalErrorCount + " Internal Server error(s), will retry in " + delayMs
                    + "ms");
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            log.warn("thread sleep failed.", e);
        }
    }

    protected Map<String, String> convertHeadersToMap(Headers headers) {
        Map<String, String> map = new IdentityHashMap<>();
        for (Map.Entry<String, List<String>> entry : headers.toMultimap().entrySet()) {
            List<String> values = entry.getValue();
            for (String value : values) {
                // map.put(new String(entry.getKey()), value);
                // fix FindBug
                map.put(new StringBuilder(entry.getKey()).toString(), value);
            }
        }
        return map;
    }

    protected ProviderCredentials getProviderCredentials() {
        return credentials;
    }

    protected void setProviderCredentials(ProviderCredentials credentials) {
        this.credentials = credentials;
    }

    protected void renameMetadataKeys(String bucketName, Request.Builder builder, Map<String, String> metadata,
                                      boolean needEncode) {
        Map<String, String> convertedMetadata = formatMetadataAndHeader(bucketName, metadata, needEncode);
        for (Map.Entry<String, String> entry : convertedMetadata.entrySet()) {
            builder.addHeader(entry.getKey(), entry.getValue());
            if (log.isDebugEnabled()) {
                log.debug("Added metadata to connection: " + entry.getKey() + "=" + entry.getValue());
            }
        }
    }

    protected void renameMetadataKeys(String bucketName, Request.Builder builder, Map<String, String> metadata) {
        renameMetadataKeys(bucketName, builder, metadata, true);
    }

    protected XmlResponsesSaxParser getXmlResponseSaxParser() throws ServiceException {
        return new XmlResponsesSaxParser();
    }

    protected void addRequestHeadersToConnection(String bucketName, Request.Builder builder,
                                                 Map<String, String> requestHeaders) {
        if (requestHeaders != null) {
            for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (!ServiceUtils.isValid(key) || value == null) {
                    continue;
                }
                key = key.trim();

                if (!Constants.ALLOWED_REQUEST_HTTP_HEADER_METADATA_NAMES.contains(key.toLowerCase(Locale.getDefault()))
                        && !key.startsWith(this.getRestHeaderPrefix(bucketName))) {
                    continue;
                }
                builder.addHeader(key, value);
                if (log.isDebugEnabled()) {
                    log.debug("Added request header to connection: " + key + "=" + value);
                }
            }
        }
    }

    protected IHeaders getIHeaders(String bucketName) {
        return Constants.HEADERS_MAP.get(this.getProviderCredentials().getLocalAuthType(bucketName));
    }

    protected IConvertor getIConvertor(String bucketName) {
        return Constants.CONVERTOR_MAP.get(this.getProviderCredentials().getLocalAuthType(bucketName));
    }

    protected boolean isAuthTypeNegotiation() {
        return this.obsProperties.getBoolProperty(ObsConstraint.AUTH_TYPE_NEGOTIATION, true);
    }

    protected String getFileSystemDelimiter() {
        return this.obsProperties.getStringProperty(ObsConstraint.FS_DELIMITER, "/");
    }

}
