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

package com.obs.services.model;

/**
 * Request for setting the requester-pays function for a bucket
 * 
 * @since 3.20.3
 */
public class SetBucketRequestPaymentRequest extends BaseBucketRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    private RequestPaymentEnum payer;

    public SetBucketRequestPaymentRequest() {
    }

    public SetBucketRequestPaymentRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    public SetBucketRequestPaymentRequest(String bucketName, RequestPaymentEnum payer) {
        this.bucketName = bucketName;
        this.payer = payer;
    }

    /**
     * Obtain the status of the requester-pays function.
     * 
     * @return The status of the requester-pays function
     */
    public RequestPaymentEnum getPayer() {
        return payer;
    }

    /**
     * Set the status of the requester-pays function.
     * 
     * @param payer
     *            Status of the requester-pays function
     */
    public void setPayer(RequestPaymentEnum payer) {
        this.payer = payer;
    }

    @Override
    public String toString() {
        return "SetBucketRequestPaymentRequest [payer=" + payer + ", getBucketName()=" + getBucketName()
                + ", isRequesterPays()=" + isRequesterPays() + "]";
    }
}
