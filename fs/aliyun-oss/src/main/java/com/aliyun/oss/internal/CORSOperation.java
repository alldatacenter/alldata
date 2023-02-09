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

import static com.aliyun.oss.common.parser.RequestMarshallers.setBucketCORSRequestMarshaller;
import static com.aliyun.oss.common.utils.CodingUtils.assertListNotNullOrEmpty;
import static com.aliyun.oss.common.utils.CodingUtils.assertParameterNotNull;
import static com.aliyun.oss.common.utils.CodingUtils.assertStringNotNullOrEmpty;
import static com.aliyun.oss.internal.OSSUtils.ensureBucketNameValid;
import static com.aliyun.oss.internal.ResponseParsers.getBucketCorsResponseParser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.common.comm.ServiceClient;
import com.aliyun.oss.model.GenericRequest;
import com.aliyun.oss.model.CORSConfiguration;
import com.aliyun.oss.model.OptionsRequest;
import com.aliyun.oss.model.SetBucketCORSRequest;
import com.aliyun.oss.model.SetBucketCORSRequest.CORSRule;
import com.aliyun.oss.model.VoidResult;

/**
 * CORS operation.
 */
public class CORSOperation extends OSSOperation {

    private static final String SUBRESOURCE_CORS = "cors";

    public CORSOperation(ServiceClient client, CredentialsProvider credsProvider) {
        super(client, credsProvider);
    }

    /**
     * Set bucket cors.
     */
    public VoidResult setBucketCORS(SetBucketCORSRequest setBucketCORSRequest) {

        checkSetBucketCORSRequestValidity(setBucketCORSRequest);

        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put(SUBRESOURCE_CORS, null);

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(setBucketCORSRequest))
                .setMethod(HttpMethod.PUT).setBucket(setBucketCORSRequest.getBucketName()).setParameters(parameters)
                .setInputStreamWithLength(setBucketCORSRequestMarshaller.marshall(setBucketCORSRequest))
                .setOriginalRequest(setBucketCORSRequest).build();

        return doOperation(request, requestIdResponseParser, setBucketCORSRequest.getBucketName(), null);
    }

    /**
     * Return the CORS configuration of the specified bucket.
     */
    public CORSConfiguration getBucketCORS(GenericRequest genericRequest) {

        assertParameterNotNull(genericRequest, "genericRequest");

        String bucketName = genericRequest.getBucketName();
        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);

        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put(SUBRESOURCE_CORS, null);

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(genericRequest))
                .setMethod(HttpMethod.GET).setParameters(parameters).setBucket(bucketName)
                .setOriginalRequest(genericRequest).build();

        return doOperation(request, getBucketCorsResponseParser, bucketName, null, true);
    }

    /**
     * Delete bucket cors.
     */
    public VoidResult deleteBucketCORS(GenericRequest genericRequest) {

        assertParameterNotNull(genericRequest, "genericRequest");

        String bucketName = genericRequest.getBucketName();
        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);

        Map<String, String> parameters = new LinkedHashMap<String, String>();
        parameters.put(SUBRESOURCE_CORS, null);

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(genericRequest))
                .setMethod(HttpMethod.DELETE).setParameters(parameters).setBucket(bucketName)
                .setOriginalRequest(genericRequest).build();

        return doOperation(request, requestIdResponseParser, bucketName, null);
    }

    /**
     * Options object.
     */
    public ResponseMessage optionsObject(OptionsRequest optionsRequest) {

        assertParameterNotNull(optionsRequest, "optionsRequest");

        String bucketName = optionsRequest.getBucketName();
        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);

        @SuppressWarnings("deprecation")
        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(optionsRequest))
                .setMethod(HttpMethod.OPTIONS).setBucket(bucketName).setKey(optionsRequest.getObjectName())
                .addHeader(OSSHeaders.ORIGIN, optionsRequest.getOrigin())
                .addHeader(OSSHeaders.ACCESS_CONTROL_REQUEST_METHOD, optionsRequest.getRequestMethod().name())
                .addHeader(OSSHeaders.ACCESS_CONTROL_REQUEST_HEADER, optionsRequest.getRequestHeaders())
                .setOriginalRequest(optionsRequest).build();

        return doOperation(request, emptyResponseParser, bucketName, null);
    }

    private static void checkSetBucketCORSRequestValidity(SetBucketCORSRequest setBucketCORSRequest) {

        assertParameterNotNull(setBucketCORSRequest, "setBucketCORSRequest");

        String bucketName = setBucketCORSRequest.getBucketName();
        assertStringNotNullOrEmpty(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);

        List<CORSRule> corsRules = setBucketCORSRequest.getCorsRules();
        assertListNotNullOrEmpty(corsRules, "corsRules");
        for (CORSRule rule : setBucketCORSRequest.getCorsRules()) {
            assertListNotNullOrEmpty(rule.getAllowedOrigins(), "allowedOrigin");
            assertListNotNullOrEmpty(rule.getAllowedMethods(), "allowedMethod");
        }
    }

}
