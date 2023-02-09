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


package com.qcloud.cos.internal;

import com.qcloud.cos.Headers;
import com.qcloud.cos.http.CosHttpResponse;
import com.qcloud.cos.model.BucketDomainConfiguration;
import com.qcloud.cos.model.CiServiceResult;
import com.qcloud.cos.model.CosServiceResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.Map;

public class COSXmlResponseHandler<T> extends AbstractCosResponseHandler<T> {

    /** The SAX unmarshaller to use when handling the response from COS */
    private Unmarshaller<T, InputStream> responseUnmarshaller;

    /** Shared logger for profiling information */
    private static final Logger log = LoggerFactory.getLogger(COSXmlResponseHandler.class);

    /** Response headers from the processed response */
    private Map<String, String> responseHeaders;

    /**
     * Constructs a new COS response handler that will use the specified SAX
     * unmarshaller to turn the response into an object.
     *
     * @param responseUnmarshaller
     *            The SAX unmarshaller to use on the response from COS.
     */
    public COSXmlResponseHandler(Unmarshaller<T, InputStream> responseUnmarshaller) {
        this.responseUnmarshaller = responseUnmarshaller;
    }

    @Override
    public CosServiceResponse<T> handle(CosHttpResponse response) throws Exception {
        CosServiceResponse<T> cosResponse = parseResponseMetadata(response);
        responseHeaders = response.getHeaders();
        if (responseUnmarshaller != null) {
            log.trace("Beginning to parse service response XML");
            T result = responseUnmarshaller.unmarshall(response.getContent());
            if(result instanceof BucketDomainConfiguration &&
                    responseHeaders.containsKey("x-cos-domain-txt-verification")) {
                ((BucketDomainConfiguration) result).setDomainTxtVerification(
                        responseHeaders.get("x-cos-domain-txt-verification"));
            }
            log.trace("Done parsing service response XML");
            cosResponse.setResult(result);
        }
        if(cosResponse.getResult() != null && cosResponse.getResult() instanceof CosServiceResult) {
            ((CosServiceResult) cosResponse.getResult()).setRequestId(responseHeaders.get(Headers.REQUEST_ID));
        }
        if (cosResponse.getResult() != null && cosResponse.getResult() instanceof CiServiceResult) {
            ((CiServiceResult) cosResponse.getResult()).setRequestId(responseHeaders.get(Headers.CI_REQUEST_ID));
        }
        return cosResponse;
    }

    /**
     * Returns the headers from the processed response. Will return null until a
     * response has been handled.
     *
     * @return the headers from the processed response.
     */
    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }
}
