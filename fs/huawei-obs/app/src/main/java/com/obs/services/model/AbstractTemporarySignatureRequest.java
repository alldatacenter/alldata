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
**/

package com.obs.services.model;

import java.util.Map;
import java.util.TreeMap;

/**
 * Abstract class for request parameters of signature-carrying queries
 *
 */
public abstract class AbstractTemporarySignatureRequest {

    protected HttpMethodEnum method;

    protected String bucketName;

    protected String objectKey;

    protected SpecialParamEnum specialParam;

    protected Map<String, String> headers;

    protected Map<String, Object> queryParams;

    public AbstractTemporarySignatureRequest() {
    }

    /**
     * Constructor
     * 
     * @param method
     *            HTTP/HTTPS request method
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     */
    public AbstractTemporarySignatureRequest(HttpMethodEnum method, String bucketName, String objectKey) {
        this.method = method;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    /**
     * Obtain the HTTP/HTTPS request method.
     * 
     * @return HTTP/HTTPS request method
     */
    public HttpMethodEnum getMethod() {
        return method;
    }

    /**
     * Set the HTTP/HTTP request method.
     * 
     * @param method
     *            HTTP/HTTPS request method
     */
    public void setMethod(HttpMethodEnum method) {
        this.method = method;
    }

    /**
     * Obtain the bucket name.
     * 
     * @return Bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Set the bucket name.
     * 
     * @param bucketName
     *            Bucket name
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Obtain the object name.
     * 
     * @return Object name
     */
    public String getObjectKey() {
        return objectKey;
    }

    /**
     * Set the object name.
     * 
     * @param objectKey
     *            Object name
     */
    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    /**
     * Obtain the request header information.
     * 
     * @return Request header information
     */
    public Map<String, String> getHeaders() {
        if (headers == null) {
            headers = new TreeMap<String, String>();
        }
        return headers;
    }

    /**
     * Obtain the query parameters of the request.
     * 
     * @return Query parameter information
     */
    public Map<String, Object> getQueryParams() {
        if (queryParams == null) {
            queryParams = new TreeMap<String, Object>();
        }
        return queryParams;
    }

    /**
     * Obtain the special operator.
     * 
     * @return Special operator
     */
    public SpecialParamEnum getSpecialParam() {
        return specialParam;
    }

    /**
     * Set the special operator.
     * 
     * @param specialParam
     *            Special operator
     */
    public void setSpecialParam(SpecialParamEnum specialParam) {
        this.specialParam = specialParam;
    }

    /**
     * Set request header information.
     * 
     * @param headers
     *            Request header information
     */
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    /**
     * Set request query parameters.
     * 
     * @param queryParams
     *            Request query parameter
     */
    public void setQueryParams(Map<String, Object> queryParams) {
        this.queryParams = queryParams;
    }
}
