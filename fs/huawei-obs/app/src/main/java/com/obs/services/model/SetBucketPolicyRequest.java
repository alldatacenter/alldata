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
 * 
 * Request parameters for setting a bucket policy
 * 
 * @since 3.20.3
 */
public class SetBucketPolicyRequest extends BaseBucketRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    private String policy;

    public SetBucketPolicyRequest() {
    }

    public SetBucketPolicyRequest(String bucketName) {
        this.bucketName = bucketName;
    }

    public SetBucketPolicyRequest(String bucketName, String policy) {
        this.bucketName = bucketName;
        this.policy = policy;
    }

    public String getPolicy() {
        return policy;
    }

    public void setPolicy(String policy) {
        this.policy = policy;
    }

    @Override
    public String toString() {
        return "SetBucketPolicyRequest [policy=" + policy + ", getBucketName()=" + getBucketName()
                + ", isRequesterPays()=" + isRequesterPays() + "]";
    }
}
