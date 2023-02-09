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
 * Configure lifecycle rules for a bucket.
 * 
 * @since 3.20.3
 */
public class SetBucketLifecycleRequest extends BaseBucketRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    private LifecycleConfiguration lifecycleConfig;

    public SetBucketLifecycleRequest(String bucketName, LifecycleConfiguration lifecycleConfig) {
        this.bucketName = bucketName;
        this.lifecycleConfig = lifecycleConfig;
    }

    public LifecycleConfiguration getLifecycleConfig() {
        return lifecycleConfig;
    }

    public void setLifecycleConfig(LifecycleConfiguration lifecycleConfig) {
        this.lifecycleConfig = lifecycleConfig;
    }

    @Override
    public String toString() {
        return "SetBucketLifecycleRequest [lifecycleConfig=" + lifecycleConfig + ", getBucketName()=" + getBucketName()
                + ", isRequesterPays()=" + isRequesterPays() + "]";
    }
}
