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


package com.obs.services.internal.service;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.internal.Constants;
import com.obs.services.internal.IHeaders;
import com.obs.services.internal.ObsConstraint;
import com.obs.services.internal.RestStorageService;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.trans.NewTransResult;
import com.obs.services.internal.utils.Mimetypes;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.AvailableZoneEnum;
import com.obs.services.model.BaseObjectRequest;
import com.obs.services.model.BucketTypeEnum;
import com.obs.services.model.GenericRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.SpecialParamEnum;
import com.obs.services.model.StorageClassEnum;
import com.obs.services.model.fs.FSStatusEnum;
import com.obs.services.model.fs.GetBucketFSStatusResult;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public abstract class AbstractRequestConvertor extends RestStorageService {
    private static final ILogger log = LoggerBuilder.getLogger("com.obs.services.ObsClient");

    protected static class TransResult {
        private Map<String, String> headers;

        private Map<String, String> params;

        // todo 临时手段，重构时要改掉
        private Map<String, String> userHeaders = new HashMap<>();

        private RequestBody body;

        TransResult(Map<String, String> headers) {
            this(headers, null, null);
        }

        TransResult(Map<String, String> headers, RequestBody body) {
            this(headers, null, body);
        }

        TransResult(Map<String, String> headers, Map<String, String> params, RequestBody body) {
            this.headers = headers;
            this.params = params;
            this.body = body;
        }

        public Map<String, String> getHeaders() {
            if (this.headers == null) {
                headers = new HashMap<>();
            }
            return this.headers;
        }

        public Map<String, String> getUserHeaders() {
            return this.userHeaders;
        }

        public void addUserHeaders(String key, String value) {
            userHeaders.put(key, value);
        }

        public Map<String, String> getParams() {
            if (this.params == null) {
                params = new HashMap<>();
            }
            return this.params;
        }

        public void setParams(Map<String, String> params) {
            this.params = params;
        }

        public void setBody(RequestBody body) {
            this.body = body;
        }

        public RequestBody getBody() {
            return body;
        }
    }

    /**
     * set requestHeader for requestPayment
     *
     * @param isRequesterPays
     * @param headers
     * @param iheaders
     * @throws ServiceException
     */
    protected Map<String, String> transRequestPaymentHeaders(boolean isRequesterPays, Map<String, String> headers,
                                                             IHeaders iheaders) throws ServiceException {
        if (isRequesterPays) {
            if (null == headers) {
                headers = new HashMap<>();
            }
            putHeader(headers, iheaders.requestPaymentHeader(), "requester");
        }

        return headers;
    }

    /**
     * set requestHeader for requestPayment
     *
     * @param request
     * @param headers
     * @param iheaders
     * @throws ServiceException
     */
    protected Map<String, String> transRequestPaymentHeaders(GenericRequest request, Map<String, String> headers,
                                                             IHeaders iheaders) throws ServiceException {
        if (null != request) {
            return transRequestPaymentHeaders(request.isRequesterPays(), headers, iheaders);
        }

        return null;
    }

    protected String getHeaderByMethodName(String bucketName, String code) {
        try {
            IHeaders iheaders = this.getIHeaders(bucketName);
            Method m = iheaders.getClass().getMethod(code);
            Object result = m.invoke(iheaders);
            return result == null ? "" : result.toString();
        } catch (Exception e) {
            if (log.isWarnEnabled()) {
                log.warn("Invoke getHeaderByMethodName error", e);
            }
        }
        return null;
    }

    protected void putHeader(Map<String, String> headers, String key, String value) {
        if (ServiceUtils.isValid(key)) {
            headers.put(key, value);
        }
    }

    protected HeaderResponse build(Response res) {
        HeaderResponse response = new HeaderResponse();
        setHeadersAndStatus(response, res);
        return response;
    }

    protected void setHeadersAndStatus(HeaderResponse response, Response res) {
        setHeadersAndStatus(response, res, true);
    }

    protected void setHeadersAndStatus(HeaderResponse response, Response res, boolean needDecode) {
        response.setStatusCode(res.code());
        Map<String, List<String>> headerMap = res.headers().toMultimap();
        Map<String, Object> originalHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, Object> responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (!(key == null || values == null)) {
                Object originalValue = values.size() == 1 ? values.get(0) : values;
                originalHeaders.put(key, originalValue);
                putCleanedKeyAndValues(responseHeaders, key, values, needDecode);
            }
        }

        response.setOriginalHeaders(originalHeaders);
        response.setResponseHeaders(responseHeaders);
    }

    protected void putCleanedKeyAndValues(Map<String, Object> responseHeaders,
                                          String key, List<String> values, boolean needDecode) {
        String cleanedKey = key.toLowerCase();
        List<String> cleanedValues = new ArrayList<>(values.size());
        Object finalValues;
        if ((Constants.CommonHeaders.DATE.equalsIgnoreCase(key)
                || Constants.CommonHeaders.LAST_MODIFIED.equalsIgnoreCase(key))) {
            finalValues = values.get(0);
            if (log.isDebugEnabled()) {
                log.debug("Parsing date string '" + finalValues + "' into Date object for key: " + key);
            }
            try {
                finalValues = ServiceUtils.parseRfc822Date(finalValues.toString());
            } catch (ParseException pe) {
                // Try ISO-8601
                try {
                    finalValues = ServiceUtils.parseIso8601Date(finalValues.toString());
                } catch (ParseException pe2) {
                    if (log.isWarnEnabled()) {
                        log.warn("Date string is not RFC 822 or ISO-8601 compliant for metadata field " + key, pe);
                    }
                }
            }
        } else {
            for (String prefix : Constants.NOT_NEED_HEADER_PREFIXES) {
                if (key.toLowerCase().startsWith(prefix)) {
                    cleanedKey = cleanedKey.replace(prefix, "");
                    break;
                }
            }
            for (String value : values) {
                if (needDecode) {
                    try {
                        cleanedValues.add(URLDecoder.decode(value, Constants.DEFAULT_ENCODING));
                    } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                        if (log.isDebugEnabled()) {
                            log.debug("Error to decode value of key:" + key);
                        }
                    }
                } else {
                    cleanedValues.add(value);
                }
            }
            if (needDecode) {
                try {
                    cleanedKey = URLDecoder.decode(cleanedKey, Constants.DEFAULT_ENCODING);
                } catch (UnsupportedEncodingException | IllegalArgumentException e) {
                    if (log.isWarnEnabled()) {
                        log.debug("Error to decode key:" + key);
                    }
                }
            }
            finalValues = cleanedValues.size() == 1 ? cleanedValues.get(0) : cleanedValues;
        }
        responseHeaders.put(cleanedKey, finalValues);
    }

    protected SpecialParamEnum getSpecialParamForStorageClass(String bucketName) {
        return this.getProviderCredentials().getLocalAuthType(bucketName) == AuthTypeEnum.OBS
                ? SpecialParamEnum.STORAGECLASS : SpecialParamEnum.STORAGEPOLICY;
    }

    protected RequestBody createRequestBody(String mimeType, String content) throws ServiceException {
        if (log.isTraceEnabled()) {
            log.trace("Entity Content:" + content);
        }
        return RequestBody.create(content.getBytes(StandardCharsets.UTF_8), MediaType.parse(mimeType));
    }

    protected GetBucketFSStatusResult getOptionInfoResult(String bucketName, Response response) {

        Headers headers = response.headers();

        Map<String, List<String>> map = headers.toMultimap();
        String maxAge = headers.get(Constants.CommonHeaders.ACCESS_CONTROL_MAX_AGE);

        IHeaders iheaders = this.getIHeaders(bucketName);
        FSStatusEnum status = FSStatusEnum.getValueFromCode(headers.get(iheaders.fsFileInterfaceHeader()));

        BucketTypeEnum bucketType = BucketTypeEnum.OBJECT;
        if (FSStatusEnum.ENABLED == status) {
            bucketType = BucketTypeEnum.PFS;
        }

        GetBucketFSStatusResult output = new GetBucketFSStatusResult.Builder()
                .allowOrigin(headers.get(Constants.CommonHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .allowHeaders(map.get(Constants.CommonHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .maxAge(maxAge == null ? 0 : Integer.parseInt(maxAge))
                .allowMethods(map.get(Constants.CommonHeaders.ACCESS_CONTROL_ALLOW_METHODS))
                .exposeHeaders(map.get(Constants.CommonHeaders.ACCESS_CONTROL_EXPOSE_HEADERS))
                .storageClass(StorageClassEnum.getValueFromCode(headers.get(iheaders.defaultStorageClassHeader())))
                .location(headers.get(iheaders.bucketRegionHeader()))
                .obsVersion(headers.get(iheaders.serverVersionHeader()))
                .status(status)
                .availableZone(AvailableZoneEnum.getValueFromCode(headers.get(iheaders.azRedundancyHeader())))
                .epid(headers.get(iheaders.epidHeader()))
                .bucketType(bucketType).build();

        setHeadersAndStatus(output, response);
        return output;
    }

    protected AuthTypeEnum getApiVersion(String bucketName) throws ServiceException {
        if (!ServiceUtils.isValid(bucketName)) {
            return parseAuthTypeInResponse("");
        }

        return parseAuthTypeInResponse(bucketName);
    }

    protected void verifyResponseContentType(Response response) throws ServiceException {
        if (this.obsProperties.getBoolProperty(ObsConstraint.VERIFY_RESPONSE_CONTENT_TYPE, true)) {
            String contentType = response.header(Constants.CommonHeaders.CONTENT_TYPE);
            if (!Mimetypes.MIMETYPE_XML.equalsIgnoreCase(contentType)
                    && !Mimetypes.MIMETYPE_TEXT_XML.equalsIgnoreCase(contentType)) {
                throw new ServiceException(
                        "Expected XML document response from OBS but received content type " + contentType);
            }
        }
    }

    protected void verifyResponseContentTypeForJson(Response response) throws ServiceException {
        if (this.obsProperties.getBoolProperty(ObsConstraint.VERIFY_RESPONSE_CONTENT_TYPE, true)) {
            String contentType = response.header(Constants.CommonHeaders.CONTENT_TYPE);
            if (null == contentType) {
                throw new ServiceException("Expected JSON document response  but received content type is null");
            } else if (!contentType.contains(Mimetypes.MIMETYPE_JSON)) {
                throw new ServiceException(
                        "Expected JSON document response  but received content type is " + contentType);
            }
        }
    }

    private AuthTypeEnum parseAuthTypeInResponse(String bucketName) throws ServiceException {
        Response response;
        try {
            response = getAuthTypeNegotiationResponseImpl(bucketName);
        } catch (ServiceException e) {
            if (e.getResponseCode() == 404 || e.getResponseCode() <= 0 || e.getResponseCode() == 408
                    || e.getResponseCode() >= 500) {
                throw e;
            } else {
                return AuthTypeEnum.V2;
            }
        }
        String apiVersion;
        return (response.code() == 200 && (apiVersion = response.headers().get("x-obs-api")) != null
                && apiVersion.compareTo("3.0") >= 0) ? AuthTypeEnum.OBS : AuthTypeEnum.V2;
    }

    private Response getAuthTypeNegotiationResponseImpl(String bucketName) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put("apiversion", "");
        return performRestForApiVersion(bucketName, null, requestParameters, null);
    }

    protected NewTransResult transRequestWithResult(TransResult result, GenericRequest request) {
        NewTransResult newResult = new NewTransResult();
        newResult.setHttpMethod(request.getHttpMethod());
        newResult.setBucketName(request.getBucketName());
        newResult.setHeaders(result.getHeaders());
        newResult.setUserHeaders(request.getUserHeaders());
        newResult.setBody(result.getBody());
        newResult.setParams(result.getParams());
        return newResult;
    }

    protected NewTransResult transObjectRequestWithResult(TransResult result, BaseObjectRequest request) {
        NewTransResult newTransResult = transRequestWithResult(result, request);
        newTransResult.setObjectKey(request.getObjectKey());
        newTransResult.setIsEncodeHeaders(request.isEncodeHeaders());
        return newTransResult;
    }

    protected NewTransResult transRequest(GenericRequest request) {
        NewTransResult newResult = new NewTransResult();
        newResult.setHttpMethod(request.getHttpMethod());
        newResult.setBucketName(request.getBucketName());
        newResult.setUserHeaders(request.getUserHeaders());
        return newResult;
    }

    protected NewTransResult transObjectRequest(BaseObjectRequest request) {
        NewTransResult newTransResult = transRequest(request);
        newTransResult.setObjectKey(request.getObjectKey());
        newTransResult.setIsEncodeHeaders(request.isEncodeHeaders());
        return newTransResult;
    }
}
