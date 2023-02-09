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

package com.obs.services.model;

import java.util.HashMap;

/**
 * Basic class of all requests, which encapsulates common parameters used by all requests.
 *
 * @since 3.20.3
 */
public class GenericRequest {
    protected String bucketName;
    /**
     * If the requester-pays function is enabled, the requester pays for his/her operations on the bucket.
     */
    private boolean isRequesterPays;

    protected HashMap<String, String> userHeaders = new HashMap<>();

    protected HttpMethodEnum httpMethod;

    public GenericRequest() {
    }

    public GenericRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    public HttpMethodEnum getHttpMethod() {
        return httpMethod;
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
     * Obtain the bucket name.
     *
     * @return Bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    public void addUserHeaders(String key, String value) {
        userHeaders.put(key, value);
    }

    public void setUserHeaders(HashMap<String, String> userHeaders) {
        this.userHeaders = userHeaders;
    }

    public HashMap<String, String> getUserHeaders() {
        return userHeaders;
    }

    /**
     * If the requester is allowed to pay, true is returned. Otherwise, false is returned.
     *
     * <p>
     * If the requester-pays function is enabled for a bucket, this attribute must be set to true when the bucket is
     *  requested by a requester other than the bucket owner. Otherwise, status code 403 is returned.
     *
     * <p>
     * After the requester-pays function is enabled, anonymous access to the bucket is not allowed.
     *
     * @return If the requester is allowed to pay, true is returned. Otherwise, false is returned.
     */
    public boolean isRequesterPays() {
        return isRequesterPays;
    }

    /**
     * Used to configure whether to enable the requester-pays function.
     *
     * <p>
     * If the requester-pays function is enabled for a bucket, this attribute must be set to true when the bucket is
     *  requested by a requester other than the bucket owner. Otherwise, status code 403 is returned.
     *
     * <p>
     * After the requester-pays function is enabled, anonymous access to the bucket is not allowed.
     *
     * @param isRequesterPays True indicates to enable the requester-pays function. 
     * False indicates to disable the requester-pays function.
     */
    public void setRequesterPays(boolean isRequesterPays) {
        this.isRequesterPays = isRequesterPays;
    }

    @Override
    public String toString() {
        return "GenericRequest [isRequesterPays=" + isRequesterPays + "]";
    }
}
