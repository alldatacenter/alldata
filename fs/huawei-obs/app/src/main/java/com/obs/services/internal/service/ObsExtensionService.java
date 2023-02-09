/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */


package com.obs.services.internal.service;

import com.obs.services.internal.Constants;
import com.obs.services.internal.Constants.CommonHeaders;
import com.obs.services.internal.Constants.ObsRequestParams;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.trans.NewTransResult;
import com.obs.services.internal.utils.JSONChange;
import com.obs.services.internal.utils.Mimetypes;
import com.obs.services.internal.utils.RestUtils;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.ReadAheadQueryResult;
import com.obs.services.model.ReadAheadRequest;
import com.obs.services.model.ReadAheadResult;
import com.oef.services.model.CreateAsynchFetchJobsResult;
import com.oef.services.model.QueryAsynchFetchJobsResult;
import com.oef.services.model.QueryExtensionPolicyResult;
import com.oef.services.model.RequestParamEnum;
import okhttp3.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class ObsExtensionService extends ObsFileService {
    protected HeaderResponse setExtensionPolicyImpl(String bucketName, String policyDocument) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(RequestParamEnum.EXTENSION_POLICY.getOriginalStringCode(), "");

        Map<String, String> headers = new HashMap<>();
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_JSON);
        headers.put((this.getProviderCredentials().getLocalAuthType(bucketName) != AuthTypeEnum.OBS
                ? Constants.V2_HEADER_PREFIX : Constants.OBS_HEADER_PREFIX) + Constants.OEF_MARKER, Constants.YES);
        NewTransResult result = new NewTransResult();
        result.setParams(requestParams);
        result.setHttpMethod(HttpMethodEnum.PUT);
        result.setBucketName(bucketName);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_JSON, policyDocument));
        return build(performRequest(result, true, false, true, false));
    }

    protected QueryExtensionPolicyResult queryExtensionPolicyImpl(String bucketName) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(RequestParamEnum.EXTENSION_POLICY.getOriginalStringCode(), "");

        Map<String, String> metadata = new HashMap<>();
        Response response = performRestGet(bucketName, requestParams, metadata);

        String body = RestUtils.readBodyFromResponse(response);

        QueryExtensionPolicyResult ret = (QueryExtensionPolicyResult) JSONChange
                .jsonToObj(new QueryExtensionPolicyResult(), body);

        setHeadersAndStatus(ret, response);
        return ret;
    }

    private Response performRestGet(String bucketName, Map<String, String> requestParams,
                                    Map<String, String> metadata) {
        metadata.put((this.getProviderCredentials().getLocalAuthType(bucketName) != AuthTypeEnum.OBS
                ? Constants.V2_HEADER_PREFIX : Constants.OBS_HEADER_PREFIX) + Constants.OEF_MARKER, Constants.YES);

        Response response = performRestGet(bucketName, null, requestParams, metadata, null, true);

        this.verifyResponseContentTypeForJson(response);
        return response;
    }

    protected HeaderResponse deleteExtensionPolicyImpl(String bucketName) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(RequestParamEnum.EXTENSION_POLICY.getOriginalStringCode(), "");

        Map<String, String> metadata = new HashMap<>();
        metadata.put((this.getProviderCredentials().getLocalAuthType(bucketName) != AuthTypeEnum.OBS
                ? Constants.V2_HEADER_PREFIX : Constants.OBS_HEADER_PREFIX) + Constants.OEF_MARKER, Constants.YES);

        Response response = performRestDelete(bucketName, null, requestParams, metadata, null, true, true);
        return this.build(response);
    }

    protected CreateAsynchFetchJobsResult createFetchJobImpl(String bucketName, String policyDocument)
            throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(RequestParamEnum.ASYNC_FETCH_JOBS.getOriginalStringCode(), "");

        Map<String, String> headers = new HashMap<>();
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_JSON);
        headers.put((this.getProviderCredentials().getLocalAuthType(bucketName) != AuthTypeEnum.OBS
                ? Constants.V2_HEADER_PREFIX : Constants.OBS_HEADER_PREFIX) + Constants.OEF_MARKER, Constants.YES);

        NewTransResult result = new NewTransResult();
        result.setBucketName(bucketName);
        result.setHttpMethod(HttpMethodEnum.POST);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(this.createRequestBody(Mimetypes.MIMETYPE_JSON, policyDocument));
        Response response = performRequest(result, true, false, true, false);

        this.verifyResponseContentTypeForJson(response);

        String body = RestUtils.readBodyFromResponse(response);

        CreateAsynchFetchJobsResult ret = (CreateAsynchFetchJobsResult) JSONChange
                .jsonToObj(new CreateAsynchFetchJobsResult(), body);

        setHeadersAndStatus(ret, response);
        return ret;
    }

    protected QueryAsynchFetchJobsResult queryFetchJobImpl(String bucketName, String jobId) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(RequestParamEnum.ASYNC_FETCH_JOBS.getOriginalStringCode() + "/" + jobId, "");

        Map<String, String> metadata = new HashMap<>();
        metadata.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_JSON);
        Response response = performRestGet(bucketName, requestParams, metadata);

        String body = RestUtils.readBodyFromResponse(response);

        QueryAsynchFetchJobsResult ret = (QueryAsynchFetchJobsResult) JSONChange
                .jsonToObj(new QueryAsynchFetchJobsResult(), body);

        setHeadersAndStatus(ret, response);

        return ret;
    }

    protected ReadAheadResult readAheadObjectsImpl(ReadAheadRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(Constants.ObsRequestParams.READAHEAD, "");
        requestParams.put(Constants.ObsRequestParams.PREFIX, request.getPrefix());

        Map<String, String> headers = new HashMap<>();
        String cacheControl = request.getCacheOption().getCode() + ", ttl=" + request.getTtl();
        headers.put(ObsRequestParams.X_CACHE_CONTROL, cacheControl);
        NewTransResult transResult = transRequest(request);
        transResult.setHeaders(headers);
        transResult.setParams(requestParams);

        Response response = performRequest(transResult);

        this.verifyResponseContentTypeForJson(response);

        String body = RestUtils.readBodyFromResponse(response);

        ReadAheadResult result = (ReadAheadResult) JSONChange.jsonToObj(new ReadAheadResult(), body);

        setHeadersAndStatus(result, response);

        return result;
    }

    protected ReadAheadResult deleteReadAheadObjectsImpl(String bucketName, String prefix) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(Constants.ObsRequestParams.READAHEAD, "");
        requestParameters.put(Constants.ObsRequestParams.PREFIX, prefix);

        Response response = performRestDelete(bucketName, null, requestParameters, null, false);

        this.verifyResponseContentTypeForJson(response);

        String body = RestUtils.readBodyFromResponse(response);

        ReadAheadResult result = (ReadAheadResult) JSONChange.jsonToObj(new ReadAheadResult(), body);

        setHeadersAndStatus(result, response);

        return result;
    }

    protected ReadAheadQueryResult queryReadAheadObjectsTaskImpl(String bucketName, String taskId)
            throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(Constants.ObsRequestParams.READAHEAD, "");
        requestParameters.put(Constants.ObsRequestParams.TASKID, taskId);

        Response response = performRestGet(bucketName, null, requestParameters, null, null);

        this.verifyResponseContentTypeForJson(response);

        String body = RestUtils.readBodyFromResponse(response);

        ReadAheadQueryResult result = (ReadAheadQueryResult) JSONChange.jsonToObj(new ReadAheadQueryResult(), body);

        setHeadersAndStatus(result, response);

        return result;
    }
}
