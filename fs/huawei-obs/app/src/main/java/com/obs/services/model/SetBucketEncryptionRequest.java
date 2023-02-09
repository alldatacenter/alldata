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
 * Configure encryption for a bucket.
 * 
 * @since 3.20.3
 */
public class SetBucketEncryptionRequest extends BaseBucketRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    private BucketEncryption bucketEncryption;

    public SetBucketEncryptionRequest(String bucketName, BucketEncryption bucketEncryption) {
        this.bucketName = bucketName;
        this.bucketEncryption = bucketEncryption;
    }

    public BucketEncryption getBucketEncryption() {
        return bucketEncryption;
    }

    public void setBucketEncryption(BucketEncryption bucketEncryption) {
        this.bucketEncryption = bucketEncryption;
    }

    @Override
    public String toString() {
        return "SetBucketEncryptionRequest [bucketEncryption=" + bucketEncryption + ", getBucketName()="
                + getBucketName() + ", isRequesterPays()=" + isRequesterPays() + "]";
    }
}
